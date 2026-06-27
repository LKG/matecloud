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
package vip.mate.system.domain.model.aggregate;

import org.junit.jupiter.api.Test;
import vip.mate.base.exception.BizException;
import vip.mate.system.domain.constant.UserOperateType;
import vip.mate.system.domain.model.entity.UserOperateStream;
import vip.mate.system.domain.model.valobj.UserStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserAggregateTest {

    private UserAggregate createAggregate() {
        return UserAggregate.createNew("testuser", "encoded_pwd", "13800138000",
                "test@example.com", "Test User");
    }

    @Test
    void createNew_validParams_createsAggregateWithCreateStream() {
        UserAggregate agg = createAggregate();
        assertNotNull(agg.getUser());
        assertEquals("testuser", agg.getUser().getUsername());
        assertEquals(UserStatus.ACTIVE, agg.getUser().getStatus());
        assertEquals(1, agg.getOperateStreams().size());
        assertEquals(UserOperateType.CREATE, agg.getOperateStreams().get(0).getOperateType());
    }

    @Test
    void create_nullUser_throwsException() {
        assertThrows(BizException.class, () -> UserAggregate.create(null));
    }

    @Test
    void getId_delegatesToUser() {
        UserAggregate agg = createAggregate();
        assertEquals(agg.getUser().getId(), agg.getId());
    }

    @Test
    void freeze_activeAggregate_statusFrozenAndStreamRecorded() {
        UserAggregate agg = createAggregate();
        agg.freeze();
        assertTrue(agg.getUser().isFrozen());
        assertEquals(2, agg.getOperateStreams().size()); // CREATE + FREEZE
        assertEquals(UserOperateType.FREEZE, agg.getOperateStreams().get(1).getOperateType());
    }

    @Test
    void unfreeze_frozenAggregate_statusActiveAndStreamRecorded() {
        UserAggregate agg = createAggregate();
        agg.freeze();
        agg.unfreeze();
        assertTrue(agg.getUser().isActive());
        assertEquals(3, agg.getOperateStreams().size()); // CREATE + FREEZE + UNFREEZE
    }

    @Test
    void delete_activeAggregate_statusDeletedAndStreamRecorded() {
        UserAggregate agg = createAggregate();
        agg.delete();
        assertTrue(agg.getUser().isDomainDeleted());
        assertEquals(2, agg.getOperateStreams().size());
        assertEquals(UserOperateType.DELETE, agg.getOperateStreams().get(1).getOperateType());
    }

    @Test
    void changeRealName_updatesAndRecordsStream() {
        UserAggregate agg = createAggregate();
        agg.changeRealName("New Name");
        assertEquals("New Name", agg.getUser().getRealName());
        assertEquals(2, agg.getOperateStreams().size());
        assertEquals(UserOperateType.CHANGE_NICKNAME, agg.getOperateStreams().get(1).getOperateType());
    }

    @Test
    void changePassword_updatesAndRecordsStream() {
        UserAggregate agg = createAggregate();
        agg.changePassword("new_encoded_pwd");
        assertEquals("new_encoded_pwd", agg.getUser().getPassword());
        assertEquals(UserOperateType.CHANGE_PASSWORD, agg.getOperateStreams().get(1).getOperateType());
    }

    @Test
    void resetPassword_adminDriven_recordsResetStream() {
        UserAggregate agg = createAggregate();
        agg.freeze(); // frozen user
        agg.resetPassword("admin_reset_pwd");
        assertEquals("admin_reset_pwd", agg.getUser().getPassword());
        assertEquals(UserOperateType.RESET_PASSWORD, agg.getOperateStreams().get(2).getOperateType());
    }

    @Test
    void getAndClearOperateStreams_returnsAndClears() {
        UserAggregate agg = createAggregate();
        agg.freeze();
        List<UserOperateStream> streams = agg.getAndClearOperateStreams();
        assertEquals(2, streams.size());
        assertTrue(agg.getOperateStreams().isEmpty()); // cleared
    }

    @Test
    void updateProfile_recordsProfileUpdateStream() {
        UserAggregate agg = createAggregate();
        agg.updateProfile("New Name", "13900139000", "new@test.com", null, 1);
        assertEquals("New Name", agg.getUser().getRealName());
        assertEquals("13900139000", agg.getUser().getMobile());
        assertEquals(UserOperateType.UPDATE_PROFILE, agg.getOperateStreams().get(1).getOperateType());
    }
}
