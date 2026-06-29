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
import vip.mate.cli.nacos.NacosClient;
import vip.mate.cli.render.Ansi;
import vip.mate.cli.render.Table;

import java.util.List;
import java.util.Map;

@Command(name = "rpc", description = "Dubbo RPC debug commands",
        subcommands = {RpcCommand.ListSub.class, RpcCommand.InvokeSub.class, RpcCommand.DescribeSub.class})
public class RpcCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Usage: mate rpc <list|invoke|describe>");
    }

    @Command(name = "list", description = "List all Dubbo RPC interfaces registered in Nacos")
    public static class ListSub implements Runnable {
        @Override
        public void run() {
            NacosClient nacos = new NacosClient();
            System.out.println("Querying Nacos: " + nacos.getServerAddr()
                    + "  namespace=" + nacos.getNamespace()
                    + "  group=" + nacos.getGroup());
            System.out.println();

            try {
                List<String> services = nacos.listServices();
                if (services.isEmpty()) {
                    System.out.println("(no services registered in Nacos)");
                    System.out.println();
                    System.out.println("Hint: Make sure Nacos is running and Dubbo services are started.");
                    System.out.println("  - Start infra:  make infra-up");
                    System.out.println("  - Check Nacos:  curl http://127.0.0.1:8848/nacos/");
                    return;
                }

                // Nacos Dubbo services typically register with the interface name directly.
                Table table = Table.of("SERVICE NAME", "GROUP", "VERSION", "INSTANCES").maxWidth(50);
                int count = 0;
                for (String name : services) {
                    List<Map<String, Object>> instances = nacos.listInstances(name);
                    // Extract Dubbo metadata if available
                    String group = "-";
                    String version = "-";
                    if (!instances.isEmpty()) {
                        Map<String, Object> first = instances.get(0);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> metadata = first.get("metadata") instanceof Map
                                ? (Map<String, Object>) first.get("metadata") : null;
                        if (metadata != null) {
                            if (metadata.containsKey("dubbo.tag")) {
                                group = String.valueOf(metadata.get("dubbo.tag"));
                            } else if (metadata.containsKey("group")) {
                                group = String.valueOf(metadata.get("group"));
                            }
                            if (metadata.containsKey("version")) {
                                version = String.valueOf(metadata.get("version"));
                            }
                        }
                    }
                    table.row(name, group, version, instances.size());
                    count++;
                }
                table.print(System.out);
                System.out.println();
                System.out.println(Ansi.muted(count + " service(s) found."));
            } catch (Exception e) {
                System.err.println("Failed to query Nacos: " + e.getMessage());
                System.err.println();
                System.err.println("Troubleshooting:");
                System.err.println("  1. Ensure Nacos is running: make infra-up");
                System.err.println("  2. Check connection: curl http://127.0.0.1:8848/nacos/");
                System.err.println("  3. Verify config in ~/.matecloud/config:");
                System.err.println("       nacos.server-addr = 127.0.0.1:8848");
                System.err.println("       nacos.namespace   = dev");
                System.err.println("  4. Or set env: NACOS_SERVER_ADDR=127.0.0.1:8848");
            }
        }

        private static String truncate(String s, int max) {
            return s.length() <= max ? s : s.substring(0, max - 3) + "...";
        }
    }

    @Command(name = "invoke", description = "Invoke a Dubbo RPC method (generic invocation)")
    public static class InvokeSub implements Runnable {
        @Parameters(index = "0", description = "Interface.method (e.g., vip.mate.api.user.IRpcUserService.getById)")
        String target;

        @Option(names = "--args", description = "JSON args array (e.g., '[1]' or '[\"hello\", 42]')")
        String args;

        @Option(names = "--version", description = "Dubbo service version", defaultValue = "1.0.0")
        String version;

        @Option(names = "--group", description = "Dubbo service group")
        String group;

        @Override
        public void run() {
            System.out.println("Dubbo Generic Invocation");
            System.out.println("========================");
            System.out.println();
            System.out.println("Target:  " + target);
            System.out.println("Args:    " + (args != null ? args : "(none)"));
            System.out.println("Version: " + version);
            if (group != null) System.out.println("Group:   " + group);
            System.out.println();
            System.out.println("[Planned Feature] Interactive generic invocation requires a Dubbo");
            System.out.println("generic client runtime, which is not bundled with mate-cli to keep");
            System.out.println("the CLI lightweight.");
            System.out.println();
            System.out.println("Alternatives:");
            System.out.println();
            System.out.println("  1. Use Dubbo Admin UI (if deployed):");
            System.out.println("     http://127.0.0.1:8080 -> Test tab -> Generic Invoke");
            System.out.println();
            System.out.println("  2. Call via the Gateway REST endpoint:");

            // Parse interface and method from target
            int lastDot = target.lastIndexOf('.');
            String method = lastDot > 0 ? target.substring(lastDot + 1) : target;
            String iface = lastDot > 0 ? target.substring(0, lastDot) : target;

            System.out.println("     curl -X POST http://127.0.0.1:9010/api/v1/" + method + " \\");
            System.out.println("       -H 'Content-Type: application/json' \\");
            System.out.println("       -d '" + (args != null ? args : "{}") + "'");
            System.out.println();
            System.out.println("  3. Write a unit test with Dubbo GenericService:");
            System.out.println("     ReferenceConfig<GenericService> ref = new ReferenceConfig<>();");
            System.out.println("     ref.setInterface(\"" + iface + "\");");
            System.out.println("     ref.setGeneric(\"true\");");
            System.out.println("     ref.setVersion(\"" + version + "\");");
            System.out.println("     GenericService svc = ref.get();");
            System.out.println("     Object result = svc.$invoke(\"" + method + "\", paramTypes, paramValues);");
        }
    }

    @Command(name = "describe", description = "Show RPC interface metadata")
    public static class DescribeSub implements Runnable {
        @Parameters(index = "0", description = "Interface name (e.g., vip.mate.api.user.IRpcUserService)")
        String interfaceName;

        @Override
        public void run() {
            System.out.println("RPC Interface: " + interfaceName);
            System.out.println();
            System.out.println("To view interface documentation, use one of these approaches:");
            System.out.println();
            System.out.println("  1. Smart-Doc (recommended):");
            System.out.println("     mvn smart-doc:html -pl mate-biz/mate-system");
            System.out.println("     Then open target/doc/index.html");
            System.out.println();
            System.out.println("  2. Check the source directly:");

            // Convert interface name to likely file path
            String path = interfaceName.replace('.', '/') + ".java";
            System.out.println("     find . -path '*/" + path + "'");
            System.out.println();
            System.out.println("  3. Query Nacos for instance metadata:");
            try {
                NacosClient nacos = new NacosClient();
                // Try to find a service matching the interface name
                List<String> services = nacos.listServices();
                boolean found = false;
                for (String svc : services) {
                    if (svc.contains(interfaceName) || interfaceName.contains(svc)) {
                        System.out.println("     Found matching service: " + svc);
                        List<Map<String, Object>> instances = nacos.listInstances(svc);
                        for (Map<String, Object> inst : instances) {
                            System.out.printf("       - %s:%s  metadata=%s%n",
                                    inst.get("ip"), inst.get("port"), inst.get("metadata"));
                        }
                        found = true;
                    }
                }
                if (!found) {
                    System.out.println("     No Nacos service found matching '" + interfaceName + "'");
                    System.out.println("     (Nacos may be offline or the service may not be running)");
                }
            } catch (Exception e) {
                System.out.println("     Could not query Nacos: " + e.getMessage());
            }
        }
    }
}
