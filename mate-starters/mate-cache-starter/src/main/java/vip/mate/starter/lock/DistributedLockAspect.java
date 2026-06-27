/*
 * Copyright (c) 2024-2026 Beijing Daotiandi Technology Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package vip.mate.starter.lock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * AOP aspect for {@link DistributedLock}.
 *
 * @author mateaix
 */
@Aspect
public class DistributedLockAspect {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockAspect.class);

    private static final String LOCK_KEY_PREFIX = "mate:lock:";

    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    public DistributedLockAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockKey = LOCK_KEY_PREFIX + resolveLockName(joinPoint, distributedLock.name());
        long waitTime = distributedLock.waitTime();
        long leaseTime = distributedLock.leaseTime();

        // 锁类型自己知道怎么从 Redisson 取锁 (可重入/公平/读/写)。
        RLock lock = distributedLock.lockType().getLock(redissonClient, lockKey);
        boolean acquired = false;

        try {
            // leaseTime <= 0 -> Redisson watchdog (auto-renewed lease) so the lock
            // can't expire while the guarded method is still running.
            acquired = leaseTime <= 0
                    ? lock.tryLock(waitTime, distributedLock.timeUnit())
                    : lock.tryLock(waitTime, leaseTime, distributedLock.timeUnit());
            if (!acquired) {
                return switch (distributedLock.timeoutStrategy()) {
                    case RETURN_NULL -> {
                        log.warn("Lock [{}] not acquired within {} {}, skipping per RETURN_NULL strategy",
                                lockKey, waitTime, distributedLock.timeUnit());
                        yield null;
                    }
                    case FAIL -> throw new DistributedLockException(
                            "Failed to acquire distributed lock [" + lockKey
                                    + "] within " + waitTime + " " + distributedLock.timeUnit());
                };
            }
            log.debug("Acquired distributed lock [{}]", lockKey);
            return joinPoint.proceed();
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Released distributed lock [{}]", lockKey);
            }
        }
    }

    private String resolveLockName(ProceedingJoinPoint joinPoint, String expression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = nameDiscoverer.getParameterNames(method);
        Object[] args = joinPoint.getArgs();

        if (paramNames == null || paramNames.length == 0) {
            return expression;
        }

        try {
            EvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < paramNames.length; i++) {
                ((StandardEvaluationContext) context).setVariable(paramNames[i], args[i]);
            }
            Object value = parser.parseExpression(expression).getValue(context);
            return value != null ? value.toString() : expression;
        } catch (Exception e) {
            log.warn("Failed to parse SpEL expression [{}], using raw value", expression, e);
            return expression;
        }
    }
}
