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
package vip.mate.auth.infrastructure.captcha;

import com.xingyuv.captcha.model.common.ResponseModel;
import com.xingyuv.captcha.model.vo.CaptchaVO;
import com.xingyuv.captcha.properties.AjCaptchaProperties;
import com.xingyuv.captcha.service.CaptchaCacheService;
import com.xingyuv.captcha.service.CaptchaService;
import com.xingyuv.captcha.service.impl.BlockPuzzleCaptchaServiceImpl;
import com.xingyuv.captcha.service.impl.CaptchaServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Manual captcha-plus configuration for Spring Boot 4.x compatibility.
 * <p>
 * The upstream auto-config ({@code AjCaptchaAutoConfiguration}) was compiled
 * against Spring Boot 2.7 and may fail on Boot 4.x. This class provides the
 * same beans directly: a Redis-backed {@link CaptchaCacheService} and the
 * {@link CaptchaService} (block-puzzle or click-word, depending on config).
 * <p>
 * It also registers the two HTTP endpoints that the captcha-plus frontend
 * expects ({@code GET /captcha/get} and {@code POST /captcha/check}).
 *
 * @author mateaix
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AjCaptchaProperties.class)
public class CaptchaPlusConfig {

    // ── Redis-backed CaptchaCacheService ────────────────────────────────

    @Bean
    @ConditionalOnMissingBean(CaptchaCacheService.class)
    public CaptchaCacheService captchaCacheService(StringRedisTemplate redis) {
        return new RedisCaptchaCacheService(redis);
    }

    // ── CaptchaService (block-puzzle / click-word) ──────────────────────

    @Bean
    @ConditionalOnMissingBean(CaptchaService.class)
    public CaptchaService captchaPlusService(AjCaptchaProperties props,
                                              CaptchaCacheService cacheService) {
        Properties p = new Properties();
        p.setProperty("captcha.captchaType", "blockPuzzle");
        p.setProperty("captcha.water.mark",
                props.getWaterMark() != null ? props.getWaterMark() : "MateCloud");
        p.setProperty("captcha.interference.options",
                String.valueOf(props.getInterferenceOptions()));
        p.setProperty("captcha.slip.offset",
                props.getSlipOffset() != null ? props.getSlipOffset() : "5");
        p.setProperty("captcha.aes.status",
                String.valueOf(props.getAesStatus()));
        p.setProperty("captcha.cache.type", "redis");

        // Register our Redis cache impl so the factory finds it by name
        CaptchaServiceFactory.cacheService.put("redis", cacheService);
        CaptchaServiceFactory.cacheService.put("local", cacheService);

        // Directly instantiate BlockPuzzleCaptchaServiceImpl to avoid
        // DefaultCaptchaServiceImpl initialising ALL types (clickWord
        // NPE's on font loading under Spring Boot 4.x classloader).
        log.info("[captcha-plus] initialising BlockPuzzleCaptchaServiceImpl");
        BlockPuzzleCaptchaServiceImpl service = new BlockPuzzleCaptchaServiceImpl();
        service.init(p);
        return service;
    }

    // ── REST controller (replaces auto-scanned controller) ──────────────

    @RestController
    @RequestMapping("/api/v1/auth/captcha")
    static class CaptchaController {

        private final CaptchaService captchaService;

        CaptchaController(CaptchaService captchaService) {
            this.captchaService = captchaService;
        }

        @RequestMapping("/get")
        public ResponseModel get(@RequestParam(value = "captchaType", required = false) String type) {
            CaptchaVO vo = new CaptchaVO();
            if (type != null) vo.setCaptchaType(type);
            return captchaService.get(vo);
        }

        @PostMapping("/check")
        public ResponseModel check(@RequestBody CaptchaVO captchaVO) {
            return captchaService.check(captchaVO);
        }
    }

    // ── Redis cache implementation ──────────────────────────────────────

    static class RedisCaptchaCacheService implements CaptchaCacheService {

        private static final String PREFIX = "captcha:";
        private final StringRedisTemplate redis;

        RedisCaptchaCacheService(StringRedisTemplate redis) {
            this.redis = redis;
        }

        @Override
        public void set(String key, String value, long expiresInSeconds) {
            redis.opsForValue().set(PREFIX + key, value, expiresInSeconds, TimeUnit.SECONDS);
        }

        @Override
        public boolean exists(String key) {
            return Boolean.TRUE.equals(redis.hasKey(PREFIX + key));
        }

        @Override
        public void delete(String key) {
            redis.delete(PREFIX + key);
        }

        @Override
        public String get(String key) {
            return redis.opsForValue().get(PREFIX + key);
        }

        @Override
        public String type() {
            return "redis";
        }

        @Override
        public Long increment(String key, long val) {
            return redis.opsForValue().increment(PREFIX + key, val);
        }
    }
}
