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
package vip.mate.system.domain.service.impl;

import lombok.RequiredArgsConstructor;
import vip.mate.system.domain.adapter.port.PasswordEncoderPort;
import vip.mate.system.domain.adapter.repository.UserRepository;
import vip.mate.system.domain.model.aggregate.UserAggregate;
import vip.mate.system.domain.service.IUserDomainService;
import vip.mate.system.types.exception.UserErrorCode;
import vip.mate.system.types.exception.UserException;

/**
 * Framework-free domain service (no Spring annotations) — registered as a bean by
 * {@code DomainServiceConfiguration} in the infrastructure layer. Password hashing
 * goes through {@link PasswordEncoderPort} so the domain stays crypto-agnostic.
 *
 * @author mateaix
 */
@RequiredArgsConstructor
public class UserDomainServiceImpl implements IUserDomainService {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;

    @Override
    public UserAggregate createUser(String username, String password, String mobile,
                                    String email, String realName) {
        if (username == null || username.isBlank()) {
            throw UserException.of(UserErrorCode.REQUEST_PARAM_NULL);
        }
        if (userRepository.existsByUsername(username)) {
            throw UserException.of(UserErrorCode.DUPLICATE_USERNAME);
        }
        if (mobile != null && userRepository.existsByMobile(mobile)) {
            throw UserException.of(UserErrorCode.DUPLICATE_MOBILE);
        }
        String realNameResolved = (realName == null || realName.isBlank()) ? username : realName;
        String hashedPassword = passwordEncoder.encode(password);
        return UserAggregate.createNew(username, hashedPassword, mobile, email, realNameResolved);
    }

    @Override
    public UserAggregate findByIdOrThrow(String userId) {
        UserAggregate aggregate = userRepository.findById(userId);
        if (aggregate == null) {
            throw UserException.of(UserErrorCode.USER_NOT_EXIST);
        }
        return aggregate;
    }

    @Override
    public UserAggregate changeRealName(String userId, String realName) {
        UserAggregate aggregate = findByIdOrThrow(userId);
        aggregate.changeRealName(realName);
        return aggregate;
    }

    @Override
    public UserAggregate updateProfile(String userId, String realName, String mobile,
                                        String email, String avatar, Integer gender) {
        UserAggregate aggregate = findByIdOrThrow(userId);
        // If the mobile is being changed, ensure no other user owns it.
        String currentMobile = aggregate.getUser().getMobile();
        if (mobile != null && !mobile.equals(currentMobile)
                && userRepository.existsByMobile(mobile)) {
            throw UserException.of(UserErrorCode.DUPLICATE_MOBILE);
        }
        aggregate.updateProfile(realName, mobile, email, avatar, gender);
        return aggregate;
    }

    @Override
    public UserAggregate changePassword(String userId, String rawOldPassword, String rawNewPassword) {
        if (rawNewPassword == null || rawNewPassword.length() < 6) {
            throw UserException.of(UserErrorCode.PASSWORD_TOO_SHORT);
        }
        UserAggregate aggregate = findByIdOrThrow(userId);
        if (!passwordEncoder.matches(rawOldPassword, aggregate.getUser().getPassword())) {
            throw UserException.of(UserErrorCode.OLD_PASSWORD_INCORRECT);
        }
        aggregate.changePassword(passwordEncoder.encode(rawNewPassword));
        return aggregate;
    }

    @Override
    public UserAggregate resetPassword(String userId, String rawNewPassword) {
        if (rawNewPassword == null || rawNewPassword.length() < 6) {
            throw UserException.of(UserErrorCode.PASSWORD_TOO_SHORT);
        }
        UserAggregate aggregate = findByIdOrThrow(userId);
        aggregate.resetPassword(passwordEncoder.encode(rawNewPassword));
        return aggregate;
    }

    @Override
    public UserAggregate freezeUser(String userId) {
        UserAggregate aggregate = findByIdOrThrow(userId);
        aggregate.freeze();
        return aggregate;
    }

    @Override
    public UserAggregate unfreezeUser(String userId) {
        UserAggregate aggregate = findByIdOrThrow(userId);
        aggregate.unfreeze();
        return aggregate;
    }

    @Override
    public UserAggregate deleteUser(String userId) {
        UserAggregate aggregate = findByIdOrThrow(userId);
        aggregate.delete();
        return aggregate;
    }
}
