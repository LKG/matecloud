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
package vip.mate.system.admin.trigger.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vip.mate.system.admin.infrastructure.dao.OperationLogDao;

/**
 * Async listener — persists the operation log in a separate thread so the
 * request returns immediately.
 *
 * <p>Uses {@link TransactionalEventListener} with {@code AFTER_COMMIT} so that
 * if the annotated operation runs inside a transaction, the log is written only
 * after that transaction commits (no log rows for rolled-back operations).
 * {@code fallbackExecution = true} keeps it working when there is no active
 * transaction (e.g. the aspect publishes after a controller-level call).
 *
 * @author mateaix
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogListener {

    private final OperationLogDao operationLogDao;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOperationLog(OperationLogEvent event) {
        try {
            operationLogDao.insert(event.getLogPO());
        } catch (Exception e) {
            log.error("[OperationLog] Failed to persist operation log asynchronously", e);
        }
    }
}
