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
package vip.mate.cli.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.*;
import java.util.*;

import vip.mate.cli.config.DbConfig;
import vip.mate.cli.config.DbConfigLoader;
import vip.mate.cli.nacos.NacosClient;

/**
 * MCP stdio server implementing the Model Context Protocol for Claude Code/Desktop.
 *
 * <p>Reads JSON-RPC 2.0 requests from stdin (one per line) and writes responses
 * to stdout. Supports the MCP handshake (initialize, notifications/initialized)
 * and tool execution (tools/list, tools/call).
 *
 * <p>Available tools:
 * <ul>
 *   <li>{@code db_describe} — describe a database table structure</li>
 *   <li>{@code db_query} — execute a read-only SQL query</li>
 *   <li>{@code service_list} — list services registered in Nacos</li>
 * </ul>
 *
 * @author mateaix
 */
public class McpServerMode {

    private final ObjectMapper json = new ObjectMapper();
    private PrintStream out;

    public void start() {
        out = System.out;
        // Redirect System.out so normal print statements don't corrupt the JSON-RPC stream
        System.setOut(System.err);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        String line;
        try {
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    Map<String, Object> request = json.readValue(line, new TypeReference<>() {});
                    handleRequest(request);
                } catch (Exception e) {
                    sendError(null, -32700, "Parse error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("MCP server IO error: " + e.getMessage());
        }
    }

    private void handleRequest(Map<String, Object> request) {
        String method = (String) request.get("method");
        Object id = request.get("id");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = request.get("params") instanceof Map
                ? (Map<String, Object>) request.get("params") : Collections.emptyMap();

        if (method == null) {
            sendError(id, -32600, "Missing method");
            return;
        }

        switch (method) {
            case "initialize" -> handleInitialize(id, params);
            case "notifications/initialized" -> { /* no response needed for notifications */ }
            case "tools/list" -> handleToolsList(id);
            case "tools/call" -> handleToolsCall(id, params);
            case "ping" -> sendResult(id, Map.of());
            default -> sendError(id, -32601, "Method not found: " + method);
        }
    }

    // ----------------------------------------------------------------
    // initialize
    // ----------------------------------------------------------------

    private void handleInitialize(Object id, Map<String, Object> params) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", "2024-11-05");

        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("tools", Map.of("listChanged", false));
        result.put("capabilities", capabilities);

        Map<String, Object> serverInfo = new LinkedHashMap<>();
        serverInfo.put("name", "matecloud-cli");
        serverInfo.put("version", "1.0.0");
        result.put("serverInfo", serverInfo);

        sendResult(id, result);
    }

    // ----------------------------------------------------------------
    // tools/list
    // ----------------------------------------------------------------

    private void handleToolsList(Object id) {
        List<Map<String, Object>> tools = new ArrayList<>();

        // db_describe
        tools.add(toolDef("db_describe",
                "Describe a database table structure (columns, indexes, row count)",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "table", Map.of("type", "string", "description", "Table name (e.g., mate_user)"),
                                "service", Map.of("type", "string", "description", "Service name for DB lookup (default: mate-admin)", "default", "mate-admin")
                        ),
                        "required", List.of("table")
                )));

        // db_query
        tools.add(toolDef("db_query",
                "Execute a read-only SQL query (SELECT/SHOW only)",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "sql", Map.of("type", "string", "description", "SQL query (SELECT or SHOW only)"),
                                "service", Map.of("type", "string", "description", "Service name for DB lookup (default: mate-admin)", "default", "mate-admin")
                        ),
                        "required", List.of("sql")
                )));

        // service_list
        tools.add(toolDef("service_list",
                "List running MateCloud services registered in Nacos",
                Map.of(
                        "type", "object",
                        "properties", Map.of()
                )));

        sendResult(id, Map.of("tools", tools));
    }

    private Map<String, Object> toolDef(String name, String description, Map<String, Object> inputSchema) {
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("name", name);
        tool.put("description", description);
        tool.put("inputSchema", inputSchema);
        return tool;
    }

    // ----------------------------------------------------------------
    // tools/call
    // ----------------------------------------------------------------

    private void handleToolsCall(Object id, Map<String, Object> params) {
        String toolName = (String) params.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = params.get("arguments") instanceof Map
                ? (Map<String, Object>) params.get("arguments") : Collections.emptyMap();

        if (toolName == null) {
            sendError(id, -32602, "Missing tool name in params.name");
            return;
        }

        try {
            String result = switch (toolName) {
                case "db_describe" -> executeDbDescribe(arguments);
                case "db_query" -> executeDbQuery(arguments);
                case "service_list" -> executeServiceList();
                default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
            };
            sendToolResult(id, result, false);
        } catch (Exception e) {
            sendToolResult(id, "Error: " + e.getMessage(), true);
        }
    }

    // ----------------------------------------------------------------
    // Tool implementations
    // ----------------------------------------------------------------

    private String executeDbDescribe(Map<String, Object> args) {
        String table = (String) args.get("table");
        String service = args.getOrDefault("service", "mate-admin").toString();

        if (table == null || table.isBlank()) {
            throw new IllegalArgumentException("'table' parameter is required");
        }
        // Basic SQL injection guard
        if (!table.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Invalid table name: " + table);
        }

        DbConfig.ServiceDb db = DbConfigLoader.getServiceDb(service);
        StringBuilder sb = new StringBuilder();

        try (Connection conn = DriverManager.getConnection(db.getUrl(), db.getUsername(), db.getPassword())) {
            // Columns
            sb.append("=== Columns ===\n");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SHOW FULL COLUMNS FROM `" + table + "`")) {
                sb.append(formatResultSet(rs));
            }

            // Indexes
            sb.append("\n=== Indexes ===\n");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SHOW INDEX FROM `" + table + "`")) {
                sb.append(formatResultSet(rs));
            }

            // Row count
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS row_count FROM `" + table + "`")) {
                if (rs.next()) {
                    sb.append("\nRow count: ").append(rs.getLong("row_count"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("SQL error: " + e.getMessage());
        }

        return sb.toString();
    }

    private String executeDbQuery(Map<String, Object> args) {
        String sql = (String) args.get("sql");
        String service = args.getOrDefault("service", "mate-admin").toString();

        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("'sql' parameter is required");
        }

        // Safety: only allow SELECT and SHOW
        String trimmed = sql.trim().toUpperCase();
        if (!trimmed.startsWith("SELECT") && !trimmed.startsWith("SHOW") && !trimmed.startsWith("DESCRIBE") && !trimmed.startsWith("EXPLAIN")) {
            throw new IllegalArgumentException("Only SELECT, SHOW, DESCRIBE, and EXPLAIN queries are allowed (read-only)");
        }

        DbConfig.ServiceDb db = DbConfigLoader.getServiceDb(service);

        try (Connection conn = DriverManager.getConnection(db.getUrl(), db.getUsername(), db.getPassword());
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            return formatResultSet(rs);
        } catch (SQLException e) {
            throw new RuntimeException("SQL error: " + e.getMessage());
        }
    }

    private String executeServiceList() {
        NacosClient nacos = new NacosClient();
        StringBuilder sb = new StringBuilder();
        sb.append("Nacos: ").append(nacos.getServerAddr())
                .append("  namespace=").append(nacos.getNamespace())
                .append("  group=").append(nacos.getGroup()).append("\n\n");

        try {
            List<String> services = nacos.listServices();
            if (services.isEmpty()) {
                sb.append("(no services registered)");
                return sb.toString();
            }

            sb.append(String.format("%-24s %s%n", "SERVICE", "INSTANCES"));
            sb.append("-".repeat(40)).append("\n");
            for (String name : services) {
                int count = nacos.listInstances(name).size();
                sb.append(String.format("%-24s %d%n", name, count));
            }
        } catch (Exception e) {
            sb.append("Failed to query Nacos: ").append(e.getMessage());
        }

        return sb.toString();
    }

    // ----------------------------------------------------------------
    // ResultSet formatter (replicates DbCommand logic)
    // ----------------------------------------------------------------

    private String formatResultSet(ResultSet rs) throws SQLException {
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
        StringBuilder sb = new StringBuilder();
        StringBuilder sep = new StringBuilder("+");
        StringBuilder hdr = new StringBuilder("|");
        for (int i = 0; i < colCount; i++) {
            sep.append("-".repeat(widths[i] + 2)).append("+");
            hdr.append(" ").append(String.format("%-" + widths[i] + "s", headers[i])).append(" |");
        }
        sb.append(sep).append("\n");
        sb.append(hdr).append("\n");
        sb.append(sep).append("\n");
        for (String[] row : rows) {
            StringBuilder line = new StringBuilder("|");
            for (int i = 0; i < colCount; i++) {
                String val = row[i].length() > 50 ? row[i].substring(0, 47) + "..." : row[i];
                line.append(" ").append(String.format("%-" + widths[i] + "s", val)).append(" |");
            }
            sb.append(line).append("\n");
        }
        sb.append(sep).append("\n");
        sb.append(rows.size()).append(" row(s)\n");
        return sb.toString();
    }

    // ----------------------------------------------------------------
    // JSON-RPC response helpers
    // ----------------------------------------------------------------

    private void sendResult(Object id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        writeLine(response);
    }

    private void sendToolResult(Object id, String text, boolean isError) {
        List<Map<String, Object>> content = List.of(Map.of(
                "type", "text",
                "text", text
        ));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        if (isError) result.put("isError", true);
        sendResult(id, result);
    }

    private void sendError(Object id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", Map.of("code", code, "message", message));
        writeLine(response);
    }

    private void writeLine(Object obj) {
        try {
            String line = json.writeValueAsString(obj);
            out.println(line);
            out.flush();
        } catch (Exception e) {
            System.err.println("MCP write error: " + e.getMessage());
        }
    }
}
