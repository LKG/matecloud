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
package vip.mate.gateway.governance.model;

/**
 * 限流维度 —— 令牌桶按哪个粒度切分。
 *
 * <ul>
 *   <li>{@link #IP} —— 按客户端 IP(默认,防单点刷接口)。</li>
 *   <li>{@link #USER} —— 按登录用户(sa-token loginId,缺失回退到 IP)。</li>
 *   <li>{@link #PATH} —— 按请求路径(保护单个慢接口)。</li>
 *   <li>{@link #SERVICE} —— 按目标服务整体(服务级总闸)。</li>
 * </ul>
 *
 * @author mateaix
 */
public enum RateLimitKey {
    IP,
    USER,
    PATH,
    SERVICE
}
