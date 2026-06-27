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
package vip.mate.system.admin.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.system.admin.infrastructure.dao.OperationLogDao;

import java.util.List;

/**
 * Write side (CQRS) for operation logs. Keeps the write off the controller so the
 * trigger layer never touches the DAO directly and the deletion runs inside a
 * transactional boundary.
 *
 * @author mateaix
 */
@Service
@RequiredArgsConstructor
public class OperationLogCommandService {

    private final OperationLogDao operationLogDao;

    /** Batch-delete operation logs by id; no-op for an empty selection. */
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<String> ids) {
        if (ids != null && !ids.isEmpty()) {
            operationLogDao.deleteBatchIds(ids);
        }
    }
}
