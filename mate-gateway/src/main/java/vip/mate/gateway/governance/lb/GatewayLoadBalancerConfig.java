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
package vip.mate.gateway.governance.lb;

import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Configuration;

/**
 * 把 {@link GrayLoadBalancerConfiguration} 设为<b>所有</b> {@code lb://} 服务的默认负载均衡配置,
 * 从而用 {@link GrayLoadBalancer} 全局替换官方的 {@code RoundRobinLoadBalancer}。
 *
 * <p>关闭灰度的服务(默认)行为与官方轮询一致,故全局替换是安全的、纯增量的。</p>
 *
 * @author mateaix
 */
@Configuration
@LoadBalancerClients(defaultConfiguration = GrayLoadBalancerConfiguration.class)
public class GatewayLoadBalancerConfig {
}
