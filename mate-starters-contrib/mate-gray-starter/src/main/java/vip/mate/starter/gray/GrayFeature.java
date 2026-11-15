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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个方法为某特性的"灰度/新版"实现。运行期由 {@link GrayFeatureAspect} 依据
 * 当前请求线程的灰度版本（{@link GrayContext} / {@link GrayDecider}）决定是否执行：
 *
 * <ul>
 *   <li><b>命中灰度</b>（版本 == {@link #grayVersion()}）—— 执行被标注方法本身；</li>
 *   <li><b>未命中 + 配了 {@link #fallbackBean()}/{@link #fallbackMethod()}</b> ——
 *       转发到同签名的稳定版 bean 方法；</li>
 *   <li><b>未命中 + 未配兜底</b> —— 仍执行被标注方法（骨架期零行为改变，方法体可自行
 *       用 {@link GrayContext#getVersion()} 分支）。</li>
 * </ul>
 *
 * <p>仅在 {@code mate.gray.enabled=true} 且过滤器链已填充上下文时生效；否则
 * {@link GrayContext#getVersion()} 为 null，永远走稳定分支。</p>
 *
 * @author mateaix
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GrayFeature {

    /** 特性 key；留空表示用 {@code mate.gray.default-feature}。 */
    String feature() default "";

    /** 命中此版本视为进入灰度。 */
    String grayVersion();

    /** 未命中时转发的稳定版 Spring bean 名；空表示不转发。 */
    String fallbackBean() default "";

    /** 未命中时转发的稳定版方法名（需与被标注方法同签名）。 */
    String fallbackMethod() default "";
}
