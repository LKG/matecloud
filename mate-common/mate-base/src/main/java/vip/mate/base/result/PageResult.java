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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Generic paged response carrier.
 * <p>
 * Mirrors the {@code PageResult<T>} TypeScript interface used by the frontend
 * ({@code packages/core/src/types/result.ts}) so a paginated controller can
 * return {@code Result<PageResult<T>>} and the JSON shape on the wire is
 * always {@code { list: [...], total: N }}.
 * <p>
 * Built deliberately as a simple data carrier rather than wrapping
 * MyBatis-Plus's {@code IPage} so it can be consumed by application services
 * that don't depend on MyBatis-Plus (e.g. when results come from RPC,
 * Elasticsearch, or in-memory aggregation).
 *
 * @param <T> element type
 *
 * @author mateaix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<T> list;
    private Long total;

    public static <T> PageResult<T> of(List<T> list, long total) {
        return new PageResult<>(list, total);
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(Collections.emptyList(), 0L);
    }
}
