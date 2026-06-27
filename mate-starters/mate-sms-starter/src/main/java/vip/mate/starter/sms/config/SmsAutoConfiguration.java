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
package vip.mate.starter.sms.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import java.util.List;
import vip.mate.base.channel.ChannelConfigStore;
import vip.mate.starter.sms.core.DefaultSmsProviderResolver;
import vip.mate.starter.sms.core.SmsProviderResolver;
import vip.mate.starter.sms.core.SmsSenderRegistry;
import vip.mate.starter.sms.core.SmsTemplate;
import vip.mate.starter.sms.provider.AliyunSmsSender;
import vip.mate.starter.sms.provider.LogSmsSender;
import vip.mate.starter.sms.provider.TencentSmsSender;
import vip.mate.starter.sms.spi.SmsSender;

/**
 * Wires the pluggable SMS layer: bundled Log + Aliyun providers (both
 * dependency-light), the active-provider resolver, the registry (collecting all
 * {@link SmsSender} beans), and the {@link SmsTemplate} facade.
 *
 * <p>Extra gateways (Tencent, …) just add their own {@code @SmsProvider} bean.
 *
 * @author mateaix
 */
@AutoConfiguration
@EnableConfigurationProperties(SmsProperties.class)
public class SmsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LogSmsSender.class)
    public LogSmsSender logSmsSender() {
        return new LogSmsSender();
    }

    @Bean
    @ConditionalOnMissingBean(AliyunSmsSender.class)
    public AliyunSmsSender aliyunSmsSender(SmsProperties properties,
                                           ObjectProvider<ChannelConfigStore> configStore) {
        return new AliyunSmsSender(properties, configStore);
    }

    @Bean
    @ConditionalOnMissingBean(TencentSmsSender.class)
    public TencentSmsSender tencentSmsSender(SmsProperties properties,
                                             ObjectProvider<ChannelConfigStore> configStore) {
        return new TencentSmsSender(properties, configStore);
    }

    @Bean
    @ConditionalOnMissingBean(SmsProviderResolver.class)
    public SmsProviderResolver smsProviderResolver(SmsProperties properties,
                                                   ObjectProvider<ChannelConfigStore> configStore) {
        return new DefaultSmsProviderResolver(properties, configStore);
    }

    @Bean
    @ConditionalOnMissingBean(SmsSenderRegistry.class)
    public SmsSenderRegistry smsSenderRegistry(List<SmsSender> senders) {
        return new SmsSenderRegistry(senders);
    }

    @Bean
    @ConditionalOnMissingBean(SmsTemplate.class)
    public SmsTemplate smsTemplate(SmsSenderRegistry registry, SmsProviderResolver resolver) {
        return new SmsTemplate(registry, resolver);
    }
}
