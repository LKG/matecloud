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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import vip.mate.gateway.governance.store.GovernanceRuleStore;

/**
 * 每个被负载均衡的服务在其 LoadBalancer 子上下文里据此装配一个 {@link GrayLoadBalancer}。
 *
 * <p>注意:本类<b>故意不加</b> {@code @Configuration}/{@code @Component} —— 它由
 * {@code @LoadBalancerClients(defaultConfiguration = ...)} 注入各子上下文,若被主上下文组件扫描会重复注册。
 * {@link GovernanceRuleStore} 是主上下文 bean,子上下文以主上下文为 parent 可正常注入。</p>
 *
 * @author mateaix
 */
public class GrayLoadBalancerConfiguration {

    @Bean
    public ReactorLoadBalancer<ServiceInstance> grayLoadBalancer(Environment environment,
                                                                 LoadBalancerClientFactory factory,
                                                                 GovernanceRuleStore ruleStore) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        ObjectProvider<ServiceInstanceListSupplier> supplier =
                factory.getLazyProvider(name, ServiceInstanceListSupplier.class);
        return new GrayLoadBalancer(supplier, name, ruleStore);
    }
}
