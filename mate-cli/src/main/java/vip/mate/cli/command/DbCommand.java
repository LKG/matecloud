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
package vip.mate.cli.command;

import org.flywaydb.core.Flyway;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import vip.mate.cli.config.DbConfig;
import vip.mate.cli.config.DbConfigLoader;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Command(name = "db", description = "Database operations",
        subcommands = {DbCommand.QuerySub.class, DbCommand.DescribeSub.class, DbCommand.MigrateSub.class})
public class DbCommand implements Runnable {
    private static final String IDENTIFIER_PATTERN = "[a-zA-Z0-9_]+";

    @Override
    public void run() {
        System.out.println("Usage: mate db <query|describe|migrate>");
    }

    @Command(name = "query", description = "Execute a SQL query")
    public static class QuerySub implements Runnable {
        @Parameters(index = "0", description = "SQL query")
        String sql;

        @Option(names = "--service", description = "Target service", defaultValue = "mate-admin")
        String service;

        @Override
        public void run() {
            DbConfig.ServiceDb db = DbConfigLoader.getServiceDb(service);
            try (Connection conn = DriverManager.getConnection(db.getUrl(), db.getUsername(), db.getPassword());
                 Statement stmt = conn.createStatement()) {
                if (sql.trim().toUpperCase().startsWith("SELECT") || sql.trim().toUpperCase().startsWith("SHOW")) {
                    ResultSet rs = stmt.executeQuery(sql);
                    printResultSet(rs);
                } else {
                    int affected = stmt.executeUpdate(sql);
                    System.out.println("Affected rows: " + affected);
                }
            } catch (SQLException e) {
                System.err.println("SQL error: " + e.getMessage());
            }
        }
    }

    @Command(name = "describe", description = "Show table structure")
    public static class DescribeSub implements Runnable {
        @Parameters(index = "0", description = "Table name")
        String table;

        @Option(names = "--service", description = "Target service", defaultValue = "mate-admin")
        String service;

        @Override
        public void run() {
            String quotedTable = quoteIdentifier(table);
            DbConfig.ServiceDb db = DbConfigLoader.getServiceDb(service);
            try (Connection conn = DriverManager.getConnection(db.getUrl(), db.getUsername(), db.getPassword())) {
                System.out.println("=== Columns ===");
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SHOW FULL COLUMNS FROM " + quotedTable)) {
                    printResultSet(rs);
                }
                System.out.println("\n=== Indexes ===");
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SHOW INDEX FROM " + quotedTable)) {
                    printResultSet(rs);
                }
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS row_count FROM " + quotedTable)) {
                    if (rs.next()) {
                        System.out.println("\nRow count: " + rs.getLong("row_count"));
                    }
                }
            } catch (SQLException e) {
                System.err.println("SQL error: " + e.getMessage());
            }
        }
    }

    @Command(name = "migrate", description = "Run Flyway migrations for a module")
    public static class MigrateSub implements Runnable {
        @Option(names = "--module", description = "Target module (e.g. mate-admin)", required = true)
        String module;

        @Override
        public void run() {
            DbConfig.ServiceDb db = DbConfigLoader.getServiceDb(module);
            try {
                Flyway flyway = Flyway.configure()
                        .dataSource(db.getUrl(), db.getUsername(), db.getPassword())
                        .locations("filesystem:" + findMigrationDir(module))
                        .baselineOnMigrate(true)
                        .load();
                var result = flyway.migrate();
                System.out.println("Applied " + result.migrationsExecuted + " migration(s)");
                result.migrations.forEach(m ->
                        System.out.println("  " + m.version + " - " + m.description));
            } catch (Exception e) {
                System.err.println("Migration failed: " + e.getMessage());
            }
        }

        private String findMigrationDir(String module) {
            String[] candidates = {
                    module + "/src/main/resources/db/migration",
                    "mate-biz/" + module + "/src/main/resources/db/migration"
            };
            for (String candidate : candidates) {
                if (new File(candidate).isDirectory()) return candidate;
            }
            System.err.println("Migration directory not found for module: " + module);
            System.exit(1);
            return null;
        }
    }

    static String quoteIdentifier(String identifier) {
        if (identifier == null || !identifier.matches(IDENTIFIER_PATTERN)) {
            throw new IllegalArgumentException("Invalid identifier: " + identifier);
        }
        return "`" + identifier + "`";
    }

    static void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        List<String[]> rows = new ArrayList<>();
        String[] headers = new String[colCount];
        int[] widths = new int[colCount];
        for (int i = 1; i <= colCount; i++) {
            headers[i - 1] = meta.getColumnLabel(i);
            widths[i - 1] = headers[i - 1].length();
        }
        while (rs.next()) {
            String[] row = new String[colCount];
            for (int i = 1; i <= colCount; i++) {
                String val = rs.getString(i);
                row[i - 1] = val != null ? val : "NULL";
                widths[i - 1] = Math.max(widths[i - 1], Math.min(row[i - 1].length(), 50));
            }
            rows.add(row);
        }
        StringBuilder sep = new StringBuilder("+");
        StringBuilder hdr = new StringBuilder("|");
        for (int i = 0; i < colCount; i++) {
            sep.append("-".repeat(widths[i] + 2)).append("+");
            hdr.append(" ").append(String.format("%-" + widths[i] + "s", headers[i])).append(" |");
        }
        System.out.println(sep);
        System.out.println(hdr);
        System.out.println(sep);
        for (String[] row : rows) {
            StringBuilder line = new StringBuilder("|");
            for (int i = 0; i < colCount; i++) {
                String val = row[i].length() > 50 ? row[i].substring(0, 47) + "..." : row[i];
                line.append(" ").append(String.format("%-" + widths[i] + "s", val)).append(" |");
            }
            System.out.println(line);
        }
        System.out.println(sep);
        System.out.println(rows.size() + " row(s)");
    }
}
