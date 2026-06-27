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

import lombok.Builder;
import lombok.Data;
import vip.mate.system.domain.constant.UserOperateType;
import vip.mate.system.domain.model.entity.User;
import vip.mate.system.domain.model.entity.UserOperateStream;
import vip.mate.system.types.exception.UserErrorCode;
import vip.mate.system.types.exception.UserException;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class UserAggregate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private User user;

    @Builder.Default
    private List<UserOperateStream> operateStreams = new ArrayList<>();

    public static UserAggregate create(User user) {
        if (user == null) {
            throw UserException.of(UserErrorCode.REQUEST_PARAM_NULL);
        }
        return UserAggregate.builder().user(user).build();
    }

    public static UserAggregate createNew(String username, String password, String mobile,
                                          String email, String realName) {
        User user = User.create(username, password, mobile, email, realName);
        UserAggregate aggregate = create(user);
        aggregate.addOperateStream(UserOperateType.CREATE,
                String.format("User created, username: %s, mobile: %s", username, mobile));
        return aggregate;
    }

    public String getId() {
        return user.getId();
    }

    public void changeRealName(String newRealName) {
        String oldRealName = user.getRealName();
        user.changeRealName(newRealName);
        addOperateStream(UserOperateType.CHANGE_NICKNAME,
                String.format("RealName: %s -> %s", oldRealName, newRealName));
    }

    public void updateProfile(String realName, String mobile, String email,
                              String avatar, Integer gender) {
        user.updateProfile(realName, mobile, email, avatar, gender);
        addOperateStream(UserOperateType.UPDATE_PROFILE,
                String.format("Profile updated: realName=%s, mobile=%s, email=%s",
                        realName, mobile, email));
    }

    /** Self-service password change. {@code encodedPassword} is BCrypt-hashed. */
    public void changePassword(String encodedPassword) {
        user.changePassword(encodedPassword);
        addOperateStream(UserOperateType.CHANGE_PASSWORD, "Password changed by user");
    }

    /**
     * Admin-driven password reset. Goes through {@code User#adminResetPassword}
     * which intentionally skips the active check — letting admins recover
     * frozen accounts. {@code encodedPassword} is BCrypt-hashed.
     */
    public void resetPassword(String encodedPassword) {
        user.adminResetPassword(encodedPassword);
        addOperateStream(UserOperateType.RESET_PASSWORD, "Password reset by admin");
    }

    public void freeze() {
        user.freeze();
        addOperateStream(UserOperateType.FREEZE, "User frozen");
    }

    public void unfreeze() {
        user.unfreeze();
        addOperateStream(UserOperateType.UNFREEZE, "User unfrozen");
    }

    public void delete() {
        user.delete();
        addOperateStream(UserOperateType.DELETE, "User deleted");
    }

    private void addOperateStream(UserOperateType type, String detail) {
        operateStreams.add(UserOperateStream.create(user.getId(), type, detail));
    }

    public List<UserOperateStream> getAndClearOperateStreams() {
        List<UserOperateStream> streams = new ArrayList<>(operateStreams);
        operateStreams.clear();
        return streams;
    }
}
