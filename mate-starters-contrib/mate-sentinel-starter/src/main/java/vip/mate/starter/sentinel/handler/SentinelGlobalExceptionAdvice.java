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
package vip.mate.starter.sentinel.handler;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for Sentinel BlockException.
 *
 * @author mateaix
 */
@Slf4j
@Order(-1)
@RestControllerAdvice
@ConditionalOnClass(BlockException.class)
public class SentinelGlobalExceptionAdvice {

    @ExceptionHandler(BlockException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Map<String, Object> handleBlockException(BlockException ex) {
        log.warn("[mate-sentinel] BlockException caught: rule={}, type={}",
                ex.getRule(), ex.getClass().getSimpleName());

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("timestamp", System.currentTimeMillis());

        if (ex instanceof FlowException) {
            result.put("code", "FLOW_LIMIT");
            result.put("msg", "Request rate limit exceeded");
        } else if (ex instanceof DegradeException) {
            result.put("code", "CIRCUIT_BREAK");
            result.put("msg", "Service circuit breaker is open");
        } else {
            result.put("code", "BLOCKED");
            result.put("msg", "Request blocked by Sentinel");
        }
        return result;
    }
}
