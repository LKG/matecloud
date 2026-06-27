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
package vip.mate.starter.idempotent.controller;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.Result;
import vip.mate.starter.idempotent.config.IdempotentProperties;

import java.time.Duration;
import java.util.UUID;

/**
 * Endpoint for TOKEN-mode idempotency.
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/idempotent")
@RequiredArgsConstructor
public class IdempotentTokenController {

    private final RedissonClient redissonClient;
    private final IdempotentProperties properties;

    @GetMapping("/token")
    public Result<String> getToken() {
        String token = UUID.randomUUID().toString().replace("-", "");
        String redisKey = properties.getKeyPrefix() + "token:" + token;
        RBucket<String> bucket = redissonClient.getBucket(redisKey);
        bucket.set("1", Duration.ofSeconds(properties.getTokenExpireSeconds()));
        return Result.ok(token);
    }
}
