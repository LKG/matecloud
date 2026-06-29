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

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import vip.mate.cli.config.CliConfig;
import vip.mate.cli.render.Ansi;
import vip.mate.cli.render.Table;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code mate cache keys|get|evict} — Redis cache operations.
 *
 * <p>Uses raw Redis RESP protocol over a plain socket, so no Jedis/Lettuce
 * dependency is needed. Connection details are read from {@code ~/.matecloud/config}
 * or environment variables REDIS_HOST, REDIS_PORT, REDIS_PASSWORD.
 */
@Command(name = "cache", description = "Cache operations (Redis)",
        subcommands = {CacheCommand.KeysSub.class, CacheCommand.GetSub.class, CacheCommand.EvictSub.class})
public class CacheCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Usage: mate cache <keys|get|evict>");
    }

    // ----------------------------------------------------------------
    // Shared Redis config helpers
    // ----------------------------------------------------------------

    static String redisHost() {
        return CliConfig.get("redis.host", "REDIS_HOST", "127.0.0.1");
    }

    static int redisPort() {
        String p = CliConfig.get("redis.port", "REDIS_PORT", "6379");
        try {
            return Integer.parseInt(p.trim());
        } catch (NumberFormatException e) {
            return 6379;
        }
    }

    static String redisPassword() {
        return CliConfig.get("redis.password", "REDIS_PASSWORD", "");
    }

    static int redisDb() {
        String d = CliConfig.get("redis.db", "REDIS_DB", "0");
        try {
            return Integer.parseInt(d.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ----------------------------------------------------------------
    // Minimal RESP client (no external dependency)
    // ----------------------------------------------------------------

    /**
     * A tiny Redis client that speaks RESP over a raw TCP socket.
     */
    static class RedisConnection implements AutoCloseable {
        private final Socket socket;
        private final OutputStream out;
        private final BufferedInputStream in;

        RedisConnection() throws IOException {
            String host = redisHost();
            int port = redisPort();
            try {
                socket = new Socket(host, port);
                socket.setSoTimeout(10_000);
                out = socket.getOutputStream();
                in = new BufferedInputStream(socket.getInputStream());
            } catch (IOException e) {
                throw new IOException("Cannot connect to Redis at " + host + ":" + port
                        + " — " + e.getMessage()
                        + "\n\nHint: ensure Redis is running (make infra-up) and check config:"
                        + "\n  ~/.matecloud/config   redis.host / redis.port / redis.password"
                        + "\n  Or env vars:          REDIS_HOST / REDIS_PORT / REDIS_PASSWORD", e);
            }

            // AUTH if password is set
            String pw = redisPassword();
            if (pw != null && !pw.isBlank()) {
                sendCommand("AUTH", pw);
                String reply = readLine();
                if (reply != null && reply.startsWith("-")) {
                    throw new IOException("Redis AUTH failed: " + reply);
                }
            }

            // SELECT db
            int db = redisDb();
            if (db != 0) {
                sendCommand("SELECT", String.valueOf(db));
                readLine(); // +OK
            }
        }

        void sendCommand(String... parts) throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append('*').append(parts.length).append("\r\n");
            for (String p : parts) {
                byte[] bytes = p.getBytes(StandardCharsets.UTF_8);
                sb.append('$').append(bytes.length).append("\r\n");
                sb.append(p).append("\r\n");
            }
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();
        }

        /** Read one RESP line (up to CR LF). */
        String readLine() throws IOException {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            int prev = -1;
            while (true) {
                int b = in.read();
                if (b == -1) return buf.size() > 0 ? buf.toString(StandardCharsets.UTF_8) : null;
                if (prev == '\r' && b == '\n') {
                    // strip trailing CR
                    byte[] data = buf.toByteArray();
                    return new String(data, 0, data.length - 1, StandardCharsets.UTF_8);
                }
                buf.write(b);
                prev = b;
            }
        }

        /** Read a RESP reply: simple string, error, integer, bulk string, or array. */
        Object readReply() throws IOException {
            String line = readLine();
            if (line == null || line.isEmpty()) return null;
            char type = line.charAt(0);
            String payload = line.substring(1);
            return switch (type) {
                case '+' -> payload;                         // simple string
                case '-' -> "ERROR: " + payload;             // error
                case ':' -> Long.parseLong(payload);         // integer
                case '$' -> readBulkString(Integer.parseInt(payload));
                case '*' -> readArray(Integer.parseInt(payload));
                default -> line;
            };
        }

        private String readBulkString(int len) throws IOException {
            if (len < 0) return null; // $-1 means nil
            byte[] data = in.readNBytes(len);
            in.read(); // CR
            in.read(); // LF
            return new String(data, StandardCharsets.UTF_8);
        }

        private List<Object> readArray(int count) throws IOException {
            if (count < 0) return null;
            List<Object> list = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                list.add(readReply());
            }
            return list;
        }

        @Override
        public void close() {
            try { socket.close(); } catch (IOException ignored) { }
        }
    }

    // ----------------------------------------------------------------
    // Sub-commands
    // ----------------------------------------------------------------

    @Command(name = "keys", description = "List cache keys by pattern (uses SCAN, max 100)")
    public static class KeysSub implements Runnable {
        @Parameters(index = "0", description = "Pattern (e.g., mate:user:*)", defaultValue = "*")
        String pattern;

        @Option(names = "--limit", description = "Max keys to return", defaultValue = "100")
        int limit;

        @Override
        public void run() {
            System.out.println("Redis: " + redisHost() + ":" + redisPort() + "  db=" + redisDb());
            System.out.println("Pattern: " + pattern);
            System.out.println();

            try (RedisConnection redis = new RedisConnection()) {
                List<String> allKeys = new ArrayList<>();
                String cursor = "0";

                do {
                    redis.sendCommand("SCAN", cursor, "MATCH", pattern, "COUNT", "100");
                    Object reply = redis.readReply();
                    if (reply instanceof List<?> arr && arr.size() == 2) {
                        cursor = String.valueOf(arr.get(0));
                        if (arr.get(1) instanceof List<?> keys) {
                            for (Object k : keys) {
                                allKeys.add(String.valueOf(k));
                                if (allKeys.size() >= limit) break;
                            }
                        }
                    } else {
                        break;
                    }
                } while (!"0".equals(cursor) && allKeys.size() < limit);

                if (allKeys.isEmpty()) {
                    System.out.println(Ansi.muted("(no keys matching '" + pattern + "')"));
                    return;
                }

                // Get type for each key
                Table table = Table.of("KEY", "TYPE").maxWidth(64);
                for (String key : allKeys) {
                    redis.sendCommand("TYPE", key);
                    Object typeReply = redis.readReply();
                    String type = typeReply != null ? String.valueOf(typeReply) : "?";
                    table.row(key, type);
                }
                table.styler((c, raw, padded) -> c == 1 ? Ansi.cyan(padded) : padded).print(System.out);
                System.out.println();
                System.out.println(Ansi.muted(allKeys.size() + " key(s)"
                        + (allKeys.size() >= limit ? " (limit reached)" : "")));
            } catch (IOException e) {
                System.err.println("Redis error: " + e.getMessage());
            }
        }
    }

    @Command(name = "get", description = "Get cache value by key")
    public static class GetSub implements Runnable {
        @Parameters(index = "0", description = "Cache key")
        String key;

        @Override
        public void run() {
            try (RedisConnection redis = new RedisConnection()) {
                // Detect key type first
                redis.sendCommand("TYPE", key);
                Object typeReply = redis.readReply();
                String type = typeReply != null ? String.valueOf(typeReply) : "none";

                System.out.println("Key:  " + key);
                System.out.println("Type: " + type);

                // Get TTL
                redis.sendCommand("TTL", key);
                Object ttlReply = redis.readReply();
                long ttl = ttlReply instanceof Long l ? l : -1;
                System.out.println("TTL:  " + (ttl == -1 ? "no expiry" : ttl == -2 ? "(key not found)" : ttl + "s"));
                System.out.println();

                switch (type) {
                    case "string" -> {
                        redis.sendCommand("GET", key);
                        Object val = redis.readReply();
                        System.out.println("Value:");
                        System.out.println(val != null ? val : "(nil)");
                    }
                    case "hash" -> {
                        redis.sendCommand("HGETALL", key);
                        Object val = redis.readReply();
                        if (val instanceof List<?> list) {
                            System.out.println("Hash entries:");
                            for (int i = 0; i + 1 < list.size(); i += 2) {
                                System.out.printf("  %s = %s%n", list.get(i), list.get(i + 1));
                            }
                        }
                    }
                    case "list" -> {
                        redis.sendCommand("LLEN", key);
                        Object lenReply = redis.readReply();
                        long len = lenReply instanceof Long l ? l : 0;
                        System.out.println("List length: " + len);

                        // Show first 20 elements
                        int show = (int) Math.min(len, 20);
                        redis.sendCommand("LRANGE", key, "0", String.valueOf(show - 1));
                        Object val = redis.readReply();
                        if (val instanceof List<?> list) {
                            System.out.println("Elements (first " + show + "):");
                            for (int i = 0; i < list.size(); i++) {
                                System.out.printf("  [%d] %s%n", i, list.get(i));
                            }
                        }
                    }
                    case "set" -> {
                        redis.sendCommand("SCARD", key);
                        Object cardReply = redis.readReply();
                        long card = cardReply instanceof Long l ? l : 0;
                        System.out.println("Set cardinality: " + card);

                        redis.sendCommand("SMEMBERS", key);
                        Object val = redis.readReply();
                        if (val instanceof List<?> list) {
                            System.out.println("Members (max 100):");
                            int i = 0;
                            for (Object m : list) {
                                System.out.println("  " + m);
                                if (++i >= 100) break;
                            }
                        }
                    }
                    case "zset" -> {
                        redis.sendCommand("ZCARD", key);
                        Object cardReply = redis.readReply();
                        long card = cardReply instanceof Long l ? l : 0;
                        System.out.println("Sorted set cardinality: " + card);

                        int show = (int) Math.min(card, 20);
                        redis.sendCommand("ZRANGE", key, "0", String.valueOf(show - 1), "WITHSCORES");
                        Object val = redis.readReply();
                        if (val instanceof List<?> list) {
                            System.out.println("Members (first " + show + "):");
                            for (int i = 0; i + 1 < list.size(); i += 2) {
                                System.out.printf("  %s  (score=%s)%n", list.get(i), list.get(i + 1));
                            }
                        }
                    }
                    case "none" -> System.out.println("Key does not exist.");
                    default -> System.out.println("Unsupported type: " + type);
                }
            } catch (IOException e) {
                System.err.println("Redis error: " + e.getMessage());
            }
        }
    }

    @Command(name = "evict", description = "Evict (delete) cache key(s) by exact key or pattern")
    public static class EvictSub implements Runnable {
        @Parameters(index = "0", description = "Key or pattern (use --pattern for glob delete)")
        String key;

        @Option(names = "--pattern", description = "Treat argument as glob pattern and delete all matching keys")
        boolean usePattern;

        @Option(names = "--dry-run", description = "Show keys that would be deleted without actually deleting")
        boolean dryRun;

        @Override
        public void run() {
            try (RedisConnection redis = new RedisConnection()) {
                if (!usePattern) {
                    // Single key delete
                    if (dryRun) {
                        System.out.println("[dry-run] Would delete: " + key);
                        return;
                    }
                    redis.sendCommand("DEL", key);
                    Object reply = redis.readReply();
                    long deleted = reply instanceof Long l ? l : 0;
                    if (deleted > 0) {
                        System.out.println("Deleted: " + key);
                    } else {
                        System.out.println("Key not found: " + key);
                    }
                } else {
                    // Pattern-based delete using SCAN
                    List<String> toDelete = new ArrayList<>();
                    String cursor = "0";
                    do {
                        redis.sendCommand("SCAN", cursor, "MATCH", key, "COUNT", "100");
                        Object reply = redis.readReply();
                        if (reply instanceof List<?> arr && arr.size() == 2) {
                            cursor = String.valueOf(arr.get(0));
                            if (arr.get(1) instanceof List<?> keys) {
                                for (Object k : keys) {
                                    toDelete.add(String.valueOf(k));
                                }
                            }
                        } else {
                            break;
                        }
                    } while (!"0".equals(cursor));

                    if (toDelete.isEmpty()) {
                        System.out.println("No keys matching pattern: " + key);
                        return;
                    }

                    if (dryRun) {
                        System.out.println("[dry-run] Would delete " + toDelete.size() + " key(s):");
                        toDelete.forEach(k -> System.out.println("  " + k));
                        return;
                    }

                    int deleted = 0;
                    for (String k : toDelete) {
                        redis.sendCommand("DEL", k);
                        Object reply = redis.readReply();
                        if (reply instanceof Long l && l > 0) deleted++;
                    }
                    System.out.println("Deleted " + deleted + " key(s) matching '" + key + "'");
                }
            } catch (IOException e) {
                System.err.println("Redis error: " + e.getMessage());
            }
        }
    }
}
