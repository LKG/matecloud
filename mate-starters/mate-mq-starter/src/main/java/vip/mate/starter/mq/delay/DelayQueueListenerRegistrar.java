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
package vip.mate.starter.mq.delay;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * BeanPostProcessor that scans for @DelayQueueListener methods and registers
 * delay queue consumers at bean init time.
 *
 * @author mateaix
 */
@Slf4j
@RequiredArgsConstructor
public class DelayQueueListenerRegistrar implements BeanPostProcessor {

    private final DelayQueueService delayQueueService;

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        Method[] methods = ReflectionUtils.getDeclaredMethods(targetClass);

        for (Method method : methods) {
            DelayQueueListener annotation = AnnotationUtils.findAnnotation(method, DelayQueueListener.class);
            if (annotation == null) {
                continue;
            }
            String topic = annotation.topic();
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1) {
                throw new IllegalStateException(
                        "@DelayQueueListener method must have exactly one parameter: "
                                + targetClass.getName() + "#" + method.getName());
            }
            log.info("[DelayQueue] Registering listener: topic={}, handler={}.{}",
                    topic, targetClass.getSimpleName(), method.getName());

            delayQueueService.consume(topic, data -> {
                try {
                    method.setAccessible(true);
                    method.invoke(bean, data);
                } catch (Exception e) {
                    log.error("[DelayQueue] Error invoking listener: topic={}, handler={}.{}",
                            topic, targetClass.getSimpleName(), method.getName(), e);
                }
            });
        }
        return bean;
    }
}
