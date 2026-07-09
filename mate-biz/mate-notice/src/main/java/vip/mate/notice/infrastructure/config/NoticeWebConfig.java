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
package vip.mate.notice.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
public class NoticeWebConfig implements WebMvcConfigurer {

    @Value("${mate.gateway.internal.secret:}")
    private String gatewaySecret;
    @Value("${mate.gateway.internal.signature-required:true}")
    private boolean signatureRequired;
    @Value("${mate.gateway.internal.timestamp-skew-ms:300000}")
    private long timestampSkewMs;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (signatureRequired && (gatewaySecret == null || gatewaySecret.isBlank())) {
            log.error("[notice][security] 网关签名校验已开启但 mate.gateway.internal.secret 未配置, "
                    + "所有 /api/v1/notice/** 请求都将被拒绝; 请配置 MATE_GATEWAY_INTERNAL_SECRET 与网关一致");
        }
        registry.addInterceptor(new NoticeAuthInterceptor(gatewaySecret, timestampSkewMs, signatureRequired))
                .addPathPatterns("/api/v1/notice/**");
    }
}
