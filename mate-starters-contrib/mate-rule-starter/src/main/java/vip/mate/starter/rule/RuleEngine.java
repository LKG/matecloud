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
package vip.mate.starter.rule;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import lombok.extern.slf4j.Slf4j;
import vip.mate.base.exception.BizException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple Aviator-based rule engine.
 *
 * <pre>
 *   RuleEngine.eval("age >= 18 && credit > 700", Map.of("age", 25, "credit", 750))
 *   // returns true
 * </pre>
 *
 * @author mateaix
 */
@Slf4j
public class RuleEngine {

    /** Compiled expression cache. */
    private final Map<String, Expression> cache = new ConcurrentHashMap<>();

    /**
     * Evaluate the expression against the given variables.
     */
    public Object eval(String expression, Map<String, Object> variables) {
        if (expression == null || expression.isBlank()) {
            throw new BizException("RULE_EXPR_EMPTY", "Rule expression is empty");
        }
        try {
            Expression expr = cache.computeIfAbsent(expression, AviatorEvaluator::compile);
            return expr.execute(variables);
        } catch (Exception e) {
            log.error("[mate-rule] Failed to evaluate expression: {}", expression, e);
            throw new BizException("RULE_EVAL_ERROR", "Rule evaluation failed: " + e.getMessage());
        }
    }

    /**
     * Evaluate a boolean rule; returns false on any error.
     */
    public boolean evalBoolean(String expression, Map<String, Object> variables) {
        Object result = eval(expression, variables);
        return result instanceof Boolean ? (Boolean) result : Boolean.parseBoolean(String.valueOf(result));
    }

    /**
     * Clear the compiled expression cache.
     */
    public void clearCache() {
        cache.clear();
    }
}
