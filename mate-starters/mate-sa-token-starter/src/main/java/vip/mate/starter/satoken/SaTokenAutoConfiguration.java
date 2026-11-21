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
package vip.mate.starter.satoken;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.stp.StpInterface;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Auto-configuration for Sa-Token.
 * <p>
 * SaTokenWebConfig is discovered via ComponentScan (not @Import) so that its
 * class definition is only loaded when the Servlet web environment is present.
 * Using @Import would force-load WebMvcConfigurer which doesn't exist in WebFlux (Gateway).
 *
 * @author mateaix
 */
@AutoConfiguration
@ConditionalOnClass(SaManager.class)
@ComponentScan(basePackageClasses = SaTokenAutoConfiguration.class)
public class SaTokenAutoConfiguration {

    @Bean
    public StpUserUtil stpUserUtil() {
        return new StpUserUtil();
    }

    /**
     * Safe {@link SaTokenDao} that avoids the Redisson {@code KEEPTTL} emulation
     * which otherwise breaks {@code StpUtil.login()} (login {@code SYSE001}) — see
     * {@link SafeKeepTtlSaTokenDao}. Takes precedence over the library's own
     * {@code SaTokenDaoForRedisTemplate} via {@link Primary}; the DAO builds its
     * {@code StringRedisTemplate} from the same {@link RedisConnectionFactory} the
     * rest of the app uses (Redisson), so wiring is otherwise unchanged.
     */
    @Bean
    @Primary
    @ConditionalOnBean(RedisConnectionFactory.class)
    public SaTokenDao safeKeepTtlSaTokenDao(RedisConnectionFactory redisConnectionFactory) {
        SafeKeepTtlSaTokenDao dao = new SafeKeepTtlSaTokenDao();
        dao.init(redisConnectionFactory);
        return dao;
    }

    /**
     * Default Redis-backed {@link StpInterface}. Lets {@code @SaCheckPermission}
     * / {@code @SaCheckRole} resolve permission codes in any service that
     * pulls this starter in. Skipped automatically if the consumer already
     * declares its own {@link StpInterface} bean (e.g. mate-gateway's
     * reactive variant).
     */
    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(StpInterface.class)
    public StpInterface redisStpInterface(StringRedisTemplate stringRedisTemplate) {
        return new RedisStpInterface(stringRedisTemplate);
    }
}
