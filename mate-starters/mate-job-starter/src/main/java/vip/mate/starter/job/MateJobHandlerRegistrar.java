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
package vip.mate.starter.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;

/**
 * BeanPostProcessor that logs discovered @MateJobHandler methods on startup.
 * Note: @MateJobHandler is a documentation/discovery aid — actual job registration
 * still uses XXL-Job's native @XxlJob annotation on the same method.
 *
 * @author mateaix
 */
@Slf4j
public class MateJobHandlerRegistrar implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        for (Method method : bean.getClass().getDeclaredMethods()) {
            MateJobHandler mateJob = AnnotationUtils.findAnnotation(method, MateJobHandler.class);
            if (mateJob != null) {
                log.info("[mate-job] Discovered job handler: {} -> {}.{}() — {}",
                        mateJob.value(), bean.getClass().getSimpleName(), method.getName(),
                        mateJob.description().isEmpty() ? "(no description)" : mateJob.description());
            }
        }
        return bean;
    }
}
