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
 * 灰度路由模式。
 *
 * <ul>
 *   <li>{@link #OFF} —— 不做版本路由,标准轮询全部实例(默认)。</li>
 *   <li>{@link #HEADER} —— 按请求头隔离:仅命中 {@code metadata.version} 与
 *       {@code X-Service-Version} 完全相等的实例;无匹配且配了兜底则降级。</li>
 *   <li>{@link #WEIGHTED} —— 按版本权重灰度:在各版本间按权重随机加权分流。</li>
 * </ul>
 *
 * @author mateaix
 */
public enum RouteMode {
    OFF,
    HEADER,
    WEIGHTED
}
