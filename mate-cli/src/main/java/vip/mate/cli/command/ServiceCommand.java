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
import picocli.CommandLine.Parameters;
import vip.mate.cli.http.JsonHttpClient;
import vip.mate.cli.nacos.NacosClient;

import java.util.List;
import java.util.Map;

/**
 * {@code mate service list|info|health} — query Nacos-registered services.
 */
@Command(name = "service", description = "List and inspect services registered in Nacos",
        subcommands = {ServiceCommand.ListSub.class, ServiceCommand.InfoSub.class, ServiceCommand.HealthSub.class})
public class ServiceCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Usage: mate service <list|info|health>");
    }

    @Command(name = "list", description = "List services registered in Nacos")
    public static class ListSub implements Runnable {
        @Override
        public void run() {
            NacosClient nacos = new NacosClient();
            System.out.println("Nacos: " + nacos.getServerAddr()
                    + "  namespace=" + nacos.getNamespace()
                    + "  group=" + nacos.getGroup());
            try {
                List<String> services = nacos.listServices();
                if (services.isEmpty()) {
                    System.out.println("(no services registered)");
                    return;
                }
                System.out.println();
                System.out.printf("%-24s %s%n", "SERVICE", "INSTANCES");
                System.out.println("-".repeat(60));
                for (String name : services) {
                    int count = nacos.listInstances(name).size();
                    System.out.printf("%-24s %d%n", name, count);
                }
            } catch (Exception e) {
                System.err.println("Failed to query Nacos: " + e.getMessage());
            }
        }
    }

    @Command(name = "info", description = "Show instance details for one service")
    public static class InfoSub implements Runnable {
        @Parameters(index = "0", description = "Service name")
        String name;

        @Override
        public void run() {
            NacosClient nacos = new NacosClient();
            try {
                List<Map<String, Object>> instances = nacos.listInstances(name);
                if (instances.isEmpty()) {
                    System.out.println("No instances for " + name);
                    return;
                }
                System.out.println("Service: " + name);
                for (Map<String, Object> inst : instances) {
                    System.out.printf("  - %s:%s  healthy=%s  weight=%s%n",
                            inst.get("ip"), inst.get("port"),
                            inst.get("healthy"), inst.get("weight"));
                }
            } catch (Exception e) {
                System.err.println("Failed: " + e.getMessage());
            }
        }
    }

    @Command(name = "health", description = "Hit /actuator/health on every registered instance")
    public static class HealthSub implements Runnable {
        @Override
        public void run() {
            NacosClient nacos = new NacosClient();
            JsonHttpClient http = new JsonHttpClient();
            try {
                List<String> services = nacos.listServices();
                System.out.printf("%-24s %-22s %s%n", "SERVICE", "ENDPOINT", "STATUS");
                System.out.println("-".repeat(70));
                for (String svc : services) {
                    List<Map<String, Object>> instances = nacos.listInstances(svc);
                    if (instances.isEmpty()) {
                        System.out.printf("%-24s %-22s %s%n", svc, "(none)", "-");
                        continue;
                    }
                    for (Map<String, Object> inst : instances) {
                        String ep = inst.get("ip") + ":" + inst.get("port");
                        String url = "http://" + ep + "/actuator/health";
                        String body = http.tryGetString(url);
                        String status = body == null
                                ? "UNREACHABLE"
                                : body.contains("\"UP\"") ? "UP" : "DOWN";
                        System.out.printf("%-24s %-22s %s%n", svc, ep, status);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed: " + e.getMessage());
            }
        }
    }
}
