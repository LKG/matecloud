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
package vip.mate.system.admin.trigger.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.Result;

import java.util.Map;

/**
 * Scheduled-job admin glue (RFC-050 G6, plan A). MateCloud delegates job
 * scheduling to XXL-Job; the actual UI lives in the XXL-Job admin server.
 * This endpoint hands the admin URL to the frontend so it can {@code iframe}
 * the XXL-Job console under our menu.
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/admin/jobs")
@RequiredArgsConstructor
public class JobController {

    @Value("${xxl.job.admin-addresses:}")
    private String xxlJobAdminUrl;

    @Value("${xxl.job.access-token:}")
    private String xxlJobAccessToken;

    @GetMapping("/admin-url")
    public Result<Map<String, Object>> adminUrl() {
        boolean configured = xxlJobAdminUrl != null && !xxlJobAdminUrl.isBlank();
        return Result.ok(Map.of(
                "url", configured ? xxlJobAdminUrl : "",
                "configured", configured,
                "tokenRequired", xxlJobAccessToken != null && !xxlJobAccessToken.isBlank()
        ));
    }
}
