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

/**
 * 灰度请求上下文持有器 —— 由 {@link GrayServletFilter} 在请求入口填充，
 * 请求结束统一清理。
 *
 * <p>与 {@link GrayContext}（单版本 ThreadLocal，供 Dubbo 透传/业务读取）不同，
 * 本持有器保存的是决策<b>输入</b>（feature / userId），供 {@code @GrayFeature}
 * 切面在同一请求线程内复用，避免重复解析身份。</p>
 *
 * <p>注意：跨线程（@Async、线程池）不会自动传递，异步分支请显式取值传递。</p>
 *
 * @author mateaix
 */
public final class GrayContextHolder {

    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    private GrayContextHolder() {
    }

    record Context(String feature, String userId) {
    }

    public static void set(String feature, String userId) {
        HOLDER.set(new Context(feature, userId));
    }

    public static String getFeature() {
        Context c = HOLDER.get();
        return c == null ? null : c.feature();
    }

    public static String getUserId() {
        Context c = HOLDER.get();
        return c == null ? null : c.userId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
