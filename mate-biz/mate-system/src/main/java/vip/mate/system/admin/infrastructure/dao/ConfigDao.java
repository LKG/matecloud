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
package vip.mate.system.admin.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import vip.mate.system.admin.infrastructure.dao.po.ConfigPO;

@Mapper
public interface ConfigDao extends BaseMapper<ConfigPO> {
    @Select("SELECT * FROM mate_config WHERE config_key = #{configKey} AND deleted = 0")
    ConfigPO selectByKey(String configKey);

    @Select("SELECT COUNT(*) > 0 FROM mate_config WHERE config_key = #{configKey} AND deleted = 0")
    boolean existsByKey(String configKey);
}
