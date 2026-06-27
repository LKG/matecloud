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
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证锁类型选择 + 超时策略 —— 纯 Mockito, 不需要真实 Redis。
 *
 * @author mateaix
 */
class DistributedLockTypeTest {

    // ---- LockType 选对了 Redisson 的锁变体 ----

    @Test
    void lockTypeSelectsCorrectRedissonLock() throws Exception {
        RedissonClient client = mock(RedissonClient.class);
        RLock reentrant = mock(RLock.class);
        RLock fair = mock(RLock.class);
        RReadWriteLock rw = mock(RReadWriteLock.class);
        RLock read = mock(RLock.class);
        RLock write = mock(RLock.class);
        when(client.getLock("k")).thenReturn(reentrant);
        when(client.getFairLock("k")).thenReturn(fair);
        when(client.getReadWriteLock("k")).thenReturn(rw);
        when(rw.readLock()).thenReturn(read);
        when(rw.writeLock()).thenReturn(write);

        assertSame(reentrant, LockType.REENTRANT.getLock(client, "k"));
        assertSame(fair, LockType.FAIR.getLock(client, "k"));
        assertSame(read, LockType.READ.getLock(client, "k"));
        assertSame(write, LockType.WRITE.getLock(client, "k"));
    }

    // ---- 超时策略 ----

    @Test
    void returnNullStrategySkipsMethodWhenLockNotAcquired() throws Throwable {
        ProceedingJoinPoint jp = jp();
        RedissonClient client = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(client.getLock("mate:lock:k")).thenReturn(lock);
        when(lock.tryLock(anyLong(), org.mockito.ArgumentMatchers.any())).thenReturn(false);

        Object result = new DistributedLockAspect(client)
                .around(jp, ann("k", LockType.REENTRANT, LockTimeoutStrategy.RETURN_NULL));

        assertNull(result, "RETURN_NULL 策略下未拿到锁应返回 null");
        verify(jp, never()).proceed();   // 目标方法不执行
    }

    @Test
    void failStrategyThrowsWhenLockNotAcquired() throws Throwable {
        ProceedingJoinPoint jp = jp();
        RedissonClient client = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(client.getFairLock("mate:lock:k")).thenReturn(lock);
        when(lock.tryLock(anyLong(), org.mockito.ArgumentMatchers.any())).thenReturn(false);

        assertThrows(DistributedLockException.class, () -> new DistributedLockAspect(client)
                .around(jp, ann("k", LockType.FAIR, LockTimeoutStrategy.FAIL)));
        verify(jp, never()).proceed();
    }

    /** join point: 无参方法签名 → resolveLockName 直接用原始 name。 */
    private static ProceedingJoinPoint jp() throws Exception {
        ProceedingJoinPoint jp = mock(ProceedingJoinPoint.class);
        MethodSignature sig = mock(MethodSignature.class);
        when(jp.getSignature()).thenReturn(sig);
        when(sig.getMethod()).thenReturn(Object.class.getMethod("toString"));
        return jp;
    }

    /** 造一个注解实例, 让切面跑到 tryLock 分支。 */
    private static DistributedLock ann(String name, LockType type, LockTimeoutStrategy strategy) throws Exception {
        DistributedLock a = mock(DistributedLock.class);
        when(a.name()).thenReturn(name);
        when(a.lockType()).thenReturn(type);
        when(a.timeoutStrategy()).thenReturn(strategy);
        lenient().when(a.waitTime()).thenReturn(1L);
        lenient().when(a.leaseTime()).thenReturn(-1L);
        when(a.timeUnit()).thenReturn(TimeUnit.SECONDS);
        return a;
    }
}
