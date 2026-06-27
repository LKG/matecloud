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
package vip.mate.starter.monitor;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import vip.mate.base.trace.MateThreadContext;
import vip.mate.base.trace.MateTrace;

/**
 * Auto-configuration for monitoring.
 *
 * @author mateaix
 */
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
public class MonitorAutoConfiguration {

    @Bean
    public MonitorInfoContributor monitorInfoContributor() {
        return new MonitorInfoContributor();
    }

    /**
     * 给线程池统一装上 trace 装饰器:
     * <ul>
     *   <li>{@link ThreadPoolTaskExecutor}({@code @Async}/业务线程池):{@link MateThreadContext#decorate}
     *       —— 从<b>提交线程</b>透传 MDC(traceId)+ 租户, 续上发起请求的同一条链路;</li>
     *   <li>{@link ThreadPoolTaskScheduler}({@code @Scheduled} 定时任务):{@link #bindFreshTraceId}
     *       —— 定时任务<b>无上游</b>(调度线程本就无 traceId), 故每次执行<b>新生成</b>一个 id,
     *       让一次定时执行的日志归在同一个可 grep 的 traceId 下(等价 XXL-Job 的 JobTraceAspect)。</li>
     * </ul>
     * 指向同一登记处, 与租户 starter 的同名设置等价、不冲突;租户未启用的服务由本处兜底。
     */
    @Bean
    public static BeanPostProcessor traceTaskExecutorDecoratorPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof ThreadPoolTaskExecutor executor) {
                    executor.setTaskDecorator(MateThreadContext::decorate);
                } else if (bean instanceof ThreadPoolTaskScheduler scheduler) {
                    scheduler.setTaskDecorator(MonitorAutoConfiguration::bindFreshTraceId);
                }
                return bean;
            }
        };
    }

    /**
     * 定时任务装饰器:执行前若当前线程无 traceId 则生成一个, 跑完清除(池化线程不串号)。
     * 不从提交线程透传 —— 调度线程没有有意义的上下文。
     */
    private static Runnable bindFreshTraceId(Runnable runnable) {
        return () -> {
            boolean bound = !MateTrace.hasCurrent();
            if (bound) {
                MateTrace.bindOrNew();
            }
            try {
                runnable.run();
            } finally {
                if (bound) {
                    MateTrace.clear();
                }
            }
        };
    }
}
