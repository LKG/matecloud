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
import vip.mate.cli.http.JsonHttpClient;
import vip.mate.cli.nacos.NacosClient;

import java.util.List;
import java.util.Map;

/**
 * {@code mate status} — one-screen overview of every service registered in
 * Nacos along with its /actuator/health and /actuator/info response.
 *
 * @author mateaix
 */
@Command(name = "status", description = "Show a one-screen overview of every MateCloud service")
public class StatusCommand implements Runnable {

    @Override
    public void run() {
        NacosClient nacos = new NacosClient();
        JsonHttpClient http = new JsonHttpClient();
        System.out.println("MateCloud cluster status");
        System.out.println("Nacos: " + nacos.getServerAddr()
                + "  namespace=" + nacos.getNamespace());
        System.out.println();

        List<String> services;
        try {
            services = nacos.listServices();
        } catch (Exception e) {
            System.err.println("Failed to reach Nacos: " + e.getMessage());
            return;
        }
        if (services.isEmpty()) {
            System.out.println("(no services registered)");
            return;
        }

        System.out.printf("%-18s %-22s %-8s %s%n",
                "SERVICE", "ENDPOINT", "HEALTH", "INFO");
        System.out.println("-".repeat(80));
        for (String svc : services) {
            List<Map<String, Object>> instances = nacos.listInstances(svc);
            if (instances.isEmpty()) {
                System.out.printf("%-18s %-22s %-8s %s%n", svc, "(none)", "-", "-");
                continue;
            }
            for (Map<String, Object> inst : instances) {
                String ep = inst.get("ip") + ":" + inst.get("port");
                String health = probe(http, "http://" + ep + "/actuator/health");
                String info = probeVersion(http, "http://" + ep + "/actuator/info");
                System.out.printf("%-18s %-22s %-8s %s%n", svc, ep, health, info);
            }
        }
    }

    private String probe(JsonHttpClient http, String url) {
        String body = http.tryGetString(url);
        if (body == null) return "DOWN";
        return body.contains("\"UP\"") ? "UP" : "DOWN";
    }

    private String probeVersion(JsonHttpClient http, String url) {
        String body = http.tryGetString(url);
        if (body == null) return "";
        // Cheap JSON field extraction without full parse
        int idx = body.indexOf("\"startupTime\"");
        if (idx > 0) {
            int colon = body.indexOf(':', idx);
            int end = body.indexOf('"', colon + 3);
            if (colon > 0 && end > 0) {
                return "up-since=" + body.substring(colon + 2, end);
            }
        }
        return body.length() > 50 ? body.substring(0, 50) + "..." : body;
    }
}
