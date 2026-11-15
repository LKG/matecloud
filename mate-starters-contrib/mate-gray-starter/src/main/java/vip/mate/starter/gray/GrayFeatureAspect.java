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
package vip.mate.starter.gray;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;

/**
 * {@link GrayFeature} 分流切面 —— 单体路线 B 的方法级灰度骨架。
 *
 * <p>命中灰度执行原方法；未命中且配置了兜底 bean/方法则反射转发到稳定版实现；
 * 未命中且无兜底则原样执行（零行为改变）。兜底转发失败时记录并回落到原方法，
 * 宁可暴露新版也绝不让请求 500。</p>
 *
 * @author mateaix
 */
@Slf4j
@Aspect
public class GrayFeatureAspect {

    private final GrayDecider decider;
    private final GrayProperties props;
    private final ApplicationContext applicationContext;

    public GrayFeatureAspect(GrayDecider decider, GrayProperties props, ApplicationContext applicationContext) {
        this.decider = decider;
        this.props = props;
        this.applicationContext = applicationContext;
    }

    @Around("@annotation(grayFeature)")
    public Object route(ProceedingJoinPoint pjp, GrayFeature grayFeature) throws Throwable {
        if (hit(grayFeature)) {
            return pjp.proceed();
        }
        // 未命中灰度：有兜底则转发稳定版，无兜底则原样执行。
        if (!grayFeature.fallbackBean().isBlank() && !grayFeature.fallbackMethod().isBlank()) {
            Object forwarded = forward(pjp, grayFeature);
            if (forwarded != FORWARD_FAILED) {
                return forwarded;
            }
        }
        return pjp.proceed();
    }

    private boolean hit(GrayFeature grayFeature) {
        String feature = grayFeature.feature().isBlank() ? props.getDefaultFeature() : grayFeature.feature();
        if (feature.equals(props.getDefaultFeature())) {
            // 默认特性已由 GrayServletFilter 解析进上下文。
            return GrayContext.isGray(grayFeature.grayVersion());
        }
        // 其它特性：按当前线程 userId 现算（决策器自带 TTL 缓存）。
        String userId = GrayContextHolder.getUserId();
        return grayFeature.grayVersion().equals(decider.decide(feature, userId));
    }

    private static final Object FORWARD_FAILED = new Object();

    private Object forward(ProceedingJoinPoint pjp, GrayFeature grayFeature) {
        try {
            Object bean = applicationContext.getBean(grayFeature.fallbackBean());
            MethodSignature sig = (MethodSignature) pjp.getSignature();
            Method method = bean.getClass().getMethod(grayFeature.fallbackMethod(), sig.getParameterTypes());
            method.setAccessible(true);
            return method.invoke(bean, pjp.getArgs());
        } catch (NoSuchMethodException e) {
            log.warn("[mate-gray] fallback {}.{}#{} not found, running gray method instead",
                    grayFeature.fallbackBean(), grayFeature.fallbackMethod(), e.getMessage());
            return FORWARD_FAILED;
        } catch (Exception e) {
            log.error("[mate-gray] fallback invocation failed, running gray method instead", e);
            return FORWARD_FAILED;
        }
    }
}
