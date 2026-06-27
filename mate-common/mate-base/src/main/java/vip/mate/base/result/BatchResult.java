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
import java.util.ArrayList;
import java.util.List;

/**
 * Per-id result envelope for bulk-action endpoints (batch-delete, batch-freeze,
 * batch-disable, etc.).
 *
 * <p>Different from {@code ImportResult} on purpose: bulk actions deal with
 * <em>existing</em> entity ids (not Excel rows), so failures carry a
 * {@code (id, message)} pair the UI can join back against the user's
 * selection. The two types intentionally don't share a base class — they
 * answer different questions (\u201Cwhich row in my upload was bad\u201D vs.
 * \u201Cwhich of the items I had selected couldn't be processed\u201D) and
 * collapsing them would make both sides lose precision.
 *
 * <p>Wire shape (mirrors the TypeScript {@code BatchResult} interface in
 * {@code packages/core}):
 * <pre>
 * { "successCount": 8, "failCount": 2,
 *   "failures": [ { "id": "u-12", "message": "User is deleted" }, ... ] }
 * </pre>
 *
 * @author mateaix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int successCount;
    private int failCount;
    private List<BatchFailure> failures;

    public static BatchResult empty() {
        return new BatchResult(0, 0, new ArrayList<>());
    }

    public void incrementSuccess() {
        this.successCount++;
    }

    public void addFailure(String id, String message) {
        if (this.failures == null) this.failures = new ArrayList<>();
        this.failures.add(new BatchFailure(id, message));
        this.failCount++;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchFailure implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String id;
        private String message;
    }
}
