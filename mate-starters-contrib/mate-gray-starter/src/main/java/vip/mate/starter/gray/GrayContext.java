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
 * Thread-local gray version context.
 *
 * @author mateaix
 */
public final class GrayContext {

    public static final String HEADER_NAME = "X-Gray-Version";

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private GrayContext() {
    }

    public static void setVersion(String version) {
        CONTEXT.set(version);
    }

    public static String getVersion() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
