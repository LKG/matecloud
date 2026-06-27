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
package vip.mate.starter.sso.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import javax.sql.DataSource;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import vip.mate.base.channel.ChannelConfigStore;
import vip.mate.starter.sso.core.AccessTokenCache;
import vip.mate.starter.sso.core.ProviderRegistry;
import vip.mate.starter.sso.core.SsoConfigStore;
import vip.mate.starter.sso.core.SsoTemplate;
import vip.mate.starter.sso.core.http.HttpExecutor;
import vip.mate.starter.sso.core.http.JdkHttpExecutor;
import vip.mate.starter.sso.core.redis.RedisAccessTokenCache;
import vip.mate.starter.sso.core.redis.RedissonSyncLock;
import vip.mate.starter.sso.core.sync.SyncLock;
import vip.mate.starter.sso.core.sync.SyncPipeline;
import vip.mate.starter.sso.web.SsoCallbackController;
import vip.mate.starter.sso.web.SsoJsConfigController;
import vip.mate.starter.sso.port.IdentityMappingPort;
import vip.mate.starter.sso.port.OrgProvisionPort;
import vip.mate.starter.sso.port.impl.JdbcIdentityMappingAdapter;
import vip.mate.starter.sso.provider.DingTalkProvider;
import vip.mate.starter.sso.provider.FeishuProvider;
import vip.mate.starter.sso.provider.LdapProvider;
import vip.mate.starter.sso.provider.WechatWorkProvider;
import vip.mate.starter.sso.spi.IdentityProvider;

/**
 * Wires the pluggable SSO layer. Master switch follows the platform feature-flag
 * convention: {@code mate.feature.sso.enabled=true} (coding-standards §12.3).
 *
 * <p>Every bean is {@code @ConditionalOnMissingBean} so a consumer can override
 * the token cache / sync lock / identity mapping with a clustered (Redis) impl.
 * The bundled providers are registered as beans (the {@code @IdentityProvider}
 * annotation is a pure marker — same approach as mate-file-starter).
 *
 * <p>{@link OrgProvisionPort} intentionally has <b>no default bean</b>: a service
 * that wants organization sync must supply one (where to write its dept/user).
 *
 * @author mateaix
 */
@AutoConfiguration
@EnableConfigurationProperties(SsoProperties.class)
@ConditionalOnProperty(prefix = "mate.feature.sso", name = "enabled", havingValue = "true")
public class SsoAutoConfiguration {

    // ---- infrastructure ----

    @Bean
    @ConditionalOnMissingBean(HttpExecutor.class)
    public HttpExecutor ssoHttpExecutor(ObjectProvider<ObjectMapper> mapper) {
        return new JdkHttpExecutor(mapper.getIfAvailable(ObjectMapper::new));
    }

    // Clustered Redis impls take precedence when a RedissonClient is present;
    // declared before the in-memory/no-op fallbacks so @ConditionalOnMissingBean works.
    @Bean
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(AccessTokenCache.class)
    public AccessTokenCache ssoRedisAccessTokenCache(RedissonClient redisson) {
        return new RedisAccessTokenCache(redisson);
    }

    @Bean
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(SyncLock.class)
    public SyncLock ssoRedissonSyncLock(RedissonClient redisson) {
        return new RedissonSyncLock(redisson);
    }

    @Bean
    @ConditionalOnMissingBean(AccessTokenCache.class)
    public AccessTokenCache ssoAccessTokenCache() {
        return new AccessTokenCache.InMemory();
    }

    @Bean
    @ConditionalOnMissingBean(SyncLock.class)
    public SyncLock ssoSyncLock() {
        return new SyncLock.NoOp();
    }

    @Bean
    @ConditionalOnMissingBean(SsoConfigStore.class)
    public SsoConfigStore ssoConfigStore(ObjectProvider<ChannelConfigStore> channelConfigStore) {
        return new SsoConfigStore(channelConfigStore);
    }

    @Bean
    @ConditionalOnMissingBean(IdentityMappingPort.class)
    @ConditionalOnBean(DataSource.class)
    public IdentityMappingPort ssoIdentityMappingPort(DataSource dataSource) {
        return new JdbcIdentityMappingAdapter(new JdbcTemplate(dataSource));
    }

    // ---- bundled providers (add a channel = add a @Bean) ----

    @Bean
    @ConditionalOnMissingBean(WechatWorkProvider.class)
    public WechatWorkProvider wechatWorkProvider(HttpExecutor http, AccessTokenCache cache) {
        return new WechatWorkProvider(http, cache);
    }

    @Bean
    @ConditionalOnMissingBean(DingTalkProvider.class)
    public DingTalkProvider dingTalkProvider(HttpExecutor http, AccessTokenCache cache) {
        return new DingTalkProvider(http, cache);
    }

    @Bean
    @ConditionalOnMissingBean(FeishuProvider.class)
    public FeishuProvider feishuProvider(HttpExecutor http, AccessTokenCache cache) {
        return new FeishuProvider(http, cache);
    }

    @Bean
    @ConditionalOnMissingBean(LdapProvider.class)
    public LdapProvider ldapProvider() {
        return new LdapProvider();
    }

    // ---- core ----

    @Bean
    @ConditionalOnMissingBean(ProviderRegistry.class)
    public ProviderRegistry ssoProviderRegistry(List<IdentityProvider> providers) {
        return new ProviderRegistry(providers);
    }

    @Bean
    @ConditionalOnMissingBean(SyncPipeline.class)
    public SyncPipeline ssoSyncPipeline(ProviderRegistry registry,
                                        SsoConfigStore configStore,
                                        ObjectProvider<IdentityMappingPort> mappingPort,
                                        ObjectProvider<OrgProvisionPort> provisionPort,
                                        SyncLock lock,
                                        ApplicationEventPublisher events,
                                        SsoProperties properties) {
        return new SyncPipeline(registry, configStore,
                mappingPort.getIfAvailable(), provisionPort, lock, events, properties);
    }

    @Bean
    @ConditionalOnMissingBean(SsoTemplate.class)
    public SsoTemplate ssoTemplate(ProviderRegistry registry,
                                   SsoConfigStore configStore,
                                   ObjectProvider<IdentityMappingPort> mappingPort,
                                   SyncPipeline pipeline) {
        return new SsoTemplate(registry, configStore, mappingPort.getIfAvailable(), pipeline);
    }

    // ---- web (only on a servlet app; spring-web is an optional dependency) ----

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = "org.springframework.web.bind.annotation.RestController")
    @ConditionalOnMissingBean(SsoJsConfigController.class)
    public SsoJsConfigController ssoJsConfigController(SsoTemplate ssoTemplate) {
        return new SsoJsConfigController(ssoTemplate);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = "org.springframework.web.bind.annotation.RestController")
    @ConditionalOnBean(OrgProvisionPort.class) // only where org sync can actually land (e.g. mate-system)
    @ConditionalOnMissingBean(SsoCallbackController.class)
    public SsoCallbackController ssoCallbackController(SsoTemplate ssoTemplate, SsoConfigStore configStore) {
        return new SsoCallbackController(ssoTemplate, configStore);
    }
}
