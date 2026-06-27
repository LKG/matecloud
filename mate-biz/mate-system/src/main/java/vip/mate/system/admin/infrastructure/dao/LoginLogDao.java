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
import vip.mate.system.admin.infrastructure.dao.po.LoginLogPO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface LoginLogDao extends BaseMapper<LoginLogPO> {

    /** Count rows with {@code created_at >= since}. Used by today-stats. */
    @Select("SELECT COUNT(*) FROM mate_login_log WHERE created_at >= #{since}")
    long countSince(@Param("since") LocalDateTime since);

    /**
     * Group by calendar date for the trend chart. Returns rows shaped
     * {@code [{ date: "2026-04-09", cnt: 23 }, ...]}.
     *
     * <p>{@code DATE(created_at)} is portable across MySQL/MariaDB; if you
     * swap to PostgreSQL change to {@code DATE_TRUNC('day', created_at)}.
     */
    @Select("""
            SELECT DATE(created_at) AS date, COUNT(*) AS cnt
              FROM mate_login_log
             WHERE created_at >= #{since}
             GROUP BY DATE(created_at)
             ORDER BY date ASC
            """)
    List<Map<String, Object>> countByDateSince(@Param("since") LocalDateTime since);
}
