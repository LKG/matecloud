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
package vip.mate.system.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import vip.mate.system.infrastructure.dao.po.UserPO;

import java.util.List;

@Mapper
public interface UserDao extends BaseMapper<UserPO> {

    @Select("SELECT * FROM mate_user WHERE mobile = #{mobile} AND deleted = 0 LIMIT 1")
    UserPO selectByMobile(@Param("mobile") String mobile);

    @Select("SELECT * FROM mate_user WHERE username = #{username} AND deleted = 0 LIMIT 1")
    UserPO selectByUsername(@Param("username") String username);

    @Select("SELECT COUNT(1) FROM mate_user WHERE mobile = #{mobile} AND deleted = 0")
    long countByMobile(@Param("mobile") String mobile);

    @Select("SELECT COUNT(1) FROM mate_user WHERE username = #{username} AND deleted = 0")
    long countByUsername(@Param("username") String username);

    @Select("SELECT COUNT(1) FROM mate_user WHERE tenant_id = #{tenantId} AND deleted = 0")
    long countByTenantId(@Param("tenantId") String tenantId);

    @Select("SELECT * FROM mate_user WHERE deleted = 0 ORDER BY created_at DESC LIMIT #{offset}, #{pageSize}")
    List<UserPO> pageQuery(@Param("offset") int offset, @Param("pageSize") int pageSize);
}
