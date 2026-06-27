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
package vip.mate.starter.ds.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Auto-configuration for DataSource and MyBatis Plus.
 *
 * <p>MyBatis-Plus 3.5.16 auto-config references Spring Boot 3.x's
 * DataSourceAutoConfiguration (Boot 4.x has migrated), so we must
 * manually create SqlSessionFactory and wire GlobalConfig + MetaObjectHandler.
 *
 * <p>Druid's own auto-config ({@code DruidDataSourceAutoConfigure}) also
 * references the old Boot 3.x class and is excluded globally in
 * mate-defaults.yml. We create the DruidDataSource bean here instead,
 * bound to standard {@code spring.datasource.*} properties.</p>
 *
 * @author mateaix
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({SqlSessionFactory.class, MybatisSqlSessionFactoryBean.class})
@MapperScan({"vip.mate.**.infrastructure.dao", "vip.mate.**.dao"})
public class DataSourceAutoConfiguration {

    /** The legacy single shared Flyway history table (pre per-service split). */
    private static final String LEGACY_FLYWAY_TABLE = "flyway_schema_history";

    /**
     * Create a single Druid DataSource bound to spring.datasource.* properties.
     * Replaces the excluded DruidDataSourceAutoConfigure.
     *
     * <p>Backs off when baomidou dynamic-datasource is enabled
     * ({@code spring.datasource.dynamic.enabled=true}), in which case
     * {@code DynamicRoutingDataSource} becomes THE datasource — this is how
     * tenant SCHEMA / DATASOURCE isolation modes plug in (RFC-012 Part 5).
     */
    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    @ConditionalOnClass(name = "DruidDataSource")
    @ConditionalOnProperty(
            prefix = "spring.datasource.dynamic", name = "enabled",
            havingValue = "false", matchIfMissing = true)
    @ConfigurationProperties(prefix = "spring.datasource.druid")
    public DruidDataSource dataSource(
            Environment env) {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl(env.getProperty("spring.datasource.url"));
        ds.setUsername(env.getProperty("spring.datasource.username"));
        ds.setPassword(env.getProperty("spring.datasource.password"));
        ds.setDriverClassName(env.getProperty("spring.datasource.driver-class-name",
                "com.mysql.cj.jdbc.Driver"));
        return ds;
    }

    @Bean
    @ConditionalOnMissingBean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource,
                                               MybatisPlusInterceptor mybatisPlusInterceptor,
                                               MetaObjectHandler metaObjectHandler) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/**/*.xml"));

        // MybatisConfiguration — mirrors mybatis-plus.configuration in mate-defaults.yml
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setCacheEnabled(false);
        configuration.setCallSettersOnNulls(true);
        configuration.setJdbcTypeForNull(JdbcType.NULL);
        factory.setConfiguration(configuration);

        factory.setPlugins(mybatisPlusInterceptor);
        factory.setTypeAliasesPackage("vip.mate.**.infrastructure.dao.po");

        // GlobalConfig — mirrors mybatis-plus.global-config in mate-defaults.yml
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setBanner(false);
        globalConfig.setMetaObjectHandler(metaObjectHandler);

        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        dbConfig.setIdType(IdType.ASSIGN_ID);
        dbConfig.setLogicDeleteField("deleted");
        dbConfig.setLogicDeleteValue("1");
        dbConfig.setLogicNotDeleteValue("0");
        dbConfig.setInsertStrategy(FieldStrategy.NOT_NULL);
        dbConfig.setUpdateStrategy(FieldStrategy.NOT_NULL);
        dbConfig.setTableUnderline(true);
        globalConfig.setDbConfig(dbConfig);

        factory.setGlobalConfig(globalConfig);

        return factory.getObject();
    }

    /**
     * Assembles the single MyBatis-Plus interceptor chain.
     *
     * <p>InnerInterceptor ordering is significant: tenant-line and
     * data-permission interceptors must run BEFORE pagination so the rewritten
     * WHERE clause is reflected in both the page and the count SQL. Other
     * starters (tenant, security) contribute their {@link InnerInterceptor}
     * beans; they are collected here, ordered by {@code @Order}, and inserted
     * ahead of the framework's own optimistic-lock / block-attack / pagination
     * interceptors (pagination always last).
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(
            ObjectProvider<InnerInterceptor> contributedInterceptors) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        List<InnerInterceptor> contributed = contributedInterceptors.orderedStream().toList();
        contributed.forEach(interceptor::addInnerInterceptor);

        // NOTE: optimistic-locking interceptor intentionally NOT registered.
        // Real OCC requires the load-time version to travel back on update, which
        // means the aggregate root must carry it. The domain layer is kept
        // framework-free (no version field), so the `lock_version` columns are
        // reserved-but-inert. To enable OCC later: add a version to the aggregate
        // root, map it through the repository convertor, re-add @Version on the
        // PO field, and register OptimisticLockerInnerInterceptor here.
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(500L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean
    public MetaObjectHandler myMetaObjectHandler() {
        return new MyMetaObjectHandler();
    }

    /**
     * Repair-then-migrate Flyway strategy that makes the documented
     * {@code spring.flyway.repair-on-migrate} flag actually do something (plain
     * Spring Boot has no such property — it was previously silently ignored).
     *
     * <p>When present, Spring Boot calls THIS instead of the default
     * {@code flyway.migrate()}. {@link Flyway#repair()} first
     * (1) realigns checksums / descriptions of applied migrations to the resolved
     * scripts, (2) removes failed migration entries, and (3) marks applied-but-
     * missing migrations as deleted — so a multi-statement DDL that half-failed,
     * or a per-service history split (see docs/conventions/pluggable-module-guide.md
     * + scripts/flyway-split-history.sql), does not brick boot under
     * {@code validate-on-migrate=true}. Successful, matching versions are never
     * re-run.
     *
     * <p>Toggle off with {@code spring.flyway.repair-on-migrate=false} to get the
     * strict default (migrate only).
     */
    @Bean
    @ConditionalOnMissingBean(FlywayMigrationStrategy.class)
    @ConditionalOnClass(name = "Flyway")
    @ConditionalOnProperty(
            prefix = "spring.flyway", name = "repair-on-migrate",
            havingValue = "true", matchIfMissing = true)
    public FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            autoSeedPerServiceHistory(flyway);
            flyway.repair();
            flyway.migrate();
        };
    }

    /**
     * One-time, automatic transition from the legacy single shared
     * {@code flyway_schema_history} to this service's per-service history table
     * (e.g. {@code flyway_history_system}). Runs on MySQL only (H2 dev is always fresh).
     *
     * <p>Without this, an existing DB upgraded to per-service history would find its
     * new table empty, baseline at version 0, and RE-RUN every migration from V1 —
     * which fails on the first non-idempotent {@code ALTER ... ADD COLUMN} (e.g.
     * "Duplicate column name"). Here we pre-seed the new table with exactly the rows
     * for THIS service's resolved scripts, copied from the legacy table, so already-
     * applied migrations are recognised and never re-run. Idempotent: once the new
     * table has rows, this is a no-op. Equivalent to scripts/flyway-split-history.sql
     * but built into the boot path so no manual step is needed.
     */
    private void autoSeedPerServiceHistory(Flyway flyway) {
        String table = flyway.getConfiguration().getTable();
        if (table == null || LEGACY_FLYWAY_TABLE.equalsIgnoreCase(table)) {
            return; // not using a per-service history table — nothing to transition
        }
        try (Connection conn = flyway.getConfiguration().getDataSource().getConnection()) {
            String product = conn.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("mysql")) {
                return; // only persistent MySQL carries legacy history; H2 is fresh
            }
            if (!tableExists(conn, LEGACY_FLYWAY_TABLE)) {
                return; // nothing to migrate from (fresh DB)
            }
            boolean newExists = tableExists(conn, table);
            if (newExists && rowCount(conn, table) > 0) {
                return; // already seeded / migrated — no-op
            }
            List<String> scripts = new ArrayList<>();
            for (MigrationInfo mi : flyway.info().all()) {
                if (mi.getScript() != null && !mi.getScript().isBlank()) {
                    scripts.add(mi.getScript());
                }
            }
            if (scripts.isEmpty()) {
                return;
            }
            if (!newExists) {
                try (Statement st = conn.createStatement()) {
                    st.execute("CREATE TABLE `" + table + "` LIKE `" + LEGACY_FLYWAY_TABLE + "`");
                }
            }
            String placeholders = String.join(",", Collections.nCopies(scripts.size(), "?"));
            String sql = "INSERT IGNORE INTO `" + table + "` SELECT * FROM `" + LEGACY_FLYWAY_TABLE
                    + "` WHERE script IN (" + placeholders + ")";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < scripts.size(); i++) {
                    ps.setString(i + 1, scripts.get(i));
                }
                int n = ps.executeUpdate();
                log.info("[flyway] per-service history transition: seeded {} applied row(s) into `{}` "
                        + "from `{}` (this service's scripts)", n, table, LEGACY_FLYWAY_TABLE);
            }
        } catch (Exception e) {
            // Never block boot on the seed itself; repair+migrate still run. If the seed
            // could not complete, migrate may fail loudly (same as before this fix).
            log.warn("[flyway] per-service history auto-seed skipped for `{}`: {}", table, e.toString());
        }
    }

    private static boolean tableExists(Connection conn, String name) throws SQLException {
        try (ResultSet rs = conn.getMetaData()
                .getTables(conn.getCatalog(), null, name, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private static long rowCount(Connection conn, String table) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM `" + table + "`")) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }
}
