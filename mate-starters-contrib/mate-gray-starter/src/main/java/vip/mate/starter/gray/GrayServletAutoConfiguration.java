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

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 进程内特性灰度（单体路线 B）自动装配 —— 提供 Redis 规则决策器 +
 * {@link GrayServletFilter} 注册 + {@link GrayFeature} 分流切面。
 *
 * <p>四重门槛，缺任何一条整套都不生效，现有行为零改变：</p>
 * <ul>
 *   <li>servlet web 应用（网关 reactive 天然排除）；</li>
 *   <li>classpath 有 {@code OncePerRequestFilter} / {@code RedissonClient}；</li>
 *   <li>容器里存在 {@code RedissonClient} bean（规则源可用）；</li>
 *   <li>{@code mate.gray.enabled=true}（默认关）。</li>
 * </ul>
 *
 * <p><b>顺序契约</b>：过滤器必须晚于身份注入运行（读 {@code X-User-Id}）。
 * 单体侧身份注入见 {@code MonolithHeaderRelayFilter}
 * （order = {@code HIGHEST_PRECEDENCE + 20}），本过滤器取 + 30。</p>
 *
 * @author mateaix
 */
@AutoConfiguration(afterName = "vip.mate.starter.cache.CacheAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({OncePerRequestFilter.class, RedissonClient.class})
@ConditionalOnBean(RedissonClient.class)
@ConditionalOnProperty(prefix = "mate.gray", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(GrayProperties.class)
public class GrayServletAutoConfiguration {

    /** 身份注入（relay）之后、业务过滤器之前。 */
    public static final int FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 30;

    @Bean
    @ConditionalOnMissingBean(GrayDecider.class)
    public GrayDecider grayDecider(RedissonClient redisson, GrayProperties props) {
        return new RedissonGrayDecider(redisson, props);
    }

    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<GrayServletFilter> grayServletFilter(
            GrayDecider decider, GrayProperties props) {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(
                new GrayServletFilter(decider, props));
        registration.addUrlPatterns("/*");
        registration.setOrder(FILTER_ORDER);
        registration.setName("grayServletFilter");
        return registration;
    }

    /** 切面单独门槛：classpath 无 AspectJ 时只保留过滤器能力。 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    public static class GrayFeatureAspectConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public GrayFeatureAspect grayFeatureAspect(GrayDecider decider, GrayProperties props,
                                                   ApplicationContext applicationContext) {
            return new GrayFeatureAspect(decider, props, applicationContext);
        }
    }
}
