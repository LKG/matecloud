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
package vip.mate.base.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import vip.mate.base.exception.ErrorCode;
import vip.mate.base.response.ResponseCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * Unified API response wrapper.
 *
 * <p>Instances are <b>built only through the static factory methods</b>
 * ({@code ok}/{@code fail}/{@code condition}) — there are intentionally no public
 * setters, so {@code success}, {@code code} and {@code msg} can never drift into a
 * contradictory state (e.g. {@code success=true} with an error {@code code}). The
 * no-arg constructor exists solely for JSON / Hessian (Dubbo) deserialization,
 * which populate fields reflectively.
 *
 * <p>{@code success} is kept as an explicit wire field because the frontend
 * response interceptor keys failure detection off {@code data.success === false}
 * (see {@code packages/core/src/api/client.ts}) rather than parsing {@code code}.
 *
 * @param <T> the type of response data
 *
 * @author mateaix
 */
@Getter
@ToString
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String code;
    private String msg;
    private Boolean success;
    private T data;
    /**
     * 链路追踪 ID。出错时给前端展示/复制, 报障可直接关联后端日志。
     * 由 HTTP 出口的 {@code TraceResponseBodyAdvice} 自动盖章 (取当前 MDC traceId),
     * 业务代码无需关心。{@code @JsonInclude(NON_NULL)} 保证未设置时不出现在响应里。
     */
    private String traceId;

    private Result(String code, String msg, Boolean success, T data) {
        this.code = code;
        this.msg = msg;
        this.success = success;
        this.data = data;
    }

    /**
     * 盖上链路追踪 ID 并返回自身 (流式)。
     *
     * <p>这是本类<b>唯一</b>的字段修改入口, 且<b>只</b>动 {@code traceId} —— 与本类守护的
     * 「{@code success}/{@code code}/{@code msg} 三者一致」不变量正交, 故不破坏不可变设计意图。
     */
    public Result<T> traceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMessage(), true, data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMessage(), true, null);
    }

    public static <T> Result<T> ok(T data, String msg) {
        return new Result<>(ResponseCode.SUCCESS.getCode(), msg, true, data);
    }

    public static <T> Result<T> fail(ResponseCode responseCode) {
        return new Result<>(responseCode.getCode(), responseCode.getMessage(), false, null);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(ResponseCode.FAILURE.getCode(), msg, false, null);
    }

    public static <T> Result<T> fail(String code, String msg) {
        return new Result<>(code, msg, false, null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), false, null);
    }

    public static <T> Result<T> condition(boolean flag) {
        return flag ? ok() : fail(ResponseCode.FAILURE);
    }
}
