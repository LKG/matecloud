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
package vip.mate.starter.monitor;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.Map;

/**
 * Contributes MateCloud framework info to /actuator/info.
 *
 * @author mateaix
 */
public class MonitorInfoContributor implements InfoContributor {

    private final Instant startupTime = Instant.now();

    @Override
    public void contribute(@NonNull Info.Builder builder) {
        builder.withDetail("matecloud", Map.of(
                "framework", "MateCloud DDD Scaffold",
                "startupTime", startupTime.toString()
        ));
    }
}
