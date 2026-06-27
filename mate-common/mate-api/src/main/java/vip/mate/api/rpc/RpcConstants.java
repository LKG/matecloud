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
package vip.mate.api.rpc;

/**
 * Shared RPC constants for Dubbo service declarations.
 *
 * @author mateaix
 */
public final class RpcConstants {

    private RpcConstants() {
    }

    public static final String VERSION = "1.0.0";
    public static final int DEFAULT_TIMEOUT = 5000;
    public static final int LONG_TIMEOUT = 15000;
    public static final int DEFAULT_RETRIES = 2;
    public static final int NO_RETRY = 0;
    public static final String GROUP_SYSTEM = "system";
    public static final String GROUP_ADMIN = "admin";
    public static final String GROUP_NOTICE = "notice";
}
