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
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import vip.mate.system.admin.infrastructure.dao.po.DictTypePO;

@Mapper
public interface DictTypeDao extends BaseMapper<DictTypePO> {

    @Select("SELECT * FROM mate_dict_type WHERE dict_type = #{dictType} AND deleted = 0 LIMIT 1")
    DictTypePO selectByCode(@Param("dictType") String dictType);

    @Select("SELECT COUNT(1) > 0 FROM mate_dict_type WHERE dict_type = #{dictType} AND deleted = 0")
    boolean existsByCode(@Param("dictType") String dictType);
}
