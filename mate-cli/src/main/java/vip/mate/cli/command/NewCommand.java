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
import vip.mate.cli.template.AggregateTemplate;
import vip.mate.cli.template.ModuleTemplate;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

@Command(
        name = "new",
        description = "Create new module or aggregate scaffold",
        subcommands = {NewCommand.ModuleSub.class, NewCommand.AggregateSub.class}
)
public class NewCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Usage: mate new <module|aggregate> <name> [options]");
    }

    @Command(name = "module", description = "Create a new DDD business module skeleton")
    public static class ModuleSub implements Callable<Integer> {

        @Parameters(index = "0", description = "Module name (e.g., mate-order)")
        String name;

        @Option(names = "--port", description = "Service port", defaultValue = "9050")
        int port;

        @Option(names = "--package", description = "Base package (default derived from name)")
        String pkg;

        @Option(names = "--root", description = "Project root directory", defaultValue = ".")
        String root;

        @Override
        public Integer call() {
            Path projectRoot = Paths.get(root).toAbsolutePath().normalize();
            String basePackage = pkg != null ? pkg : "vip.mate." + name.replace("mate-", "").replace("-", "");
            try {
                new ModuleTemplate(projectRoot, name, basePackage, port).generate();
                String code = name.replace("mate-", "");
                System.out.println("Created module: " + name + " at port " + port);
                System.out.println("  ✔ registered <module> in mate-biz/pom.xml");
                System.out.println("  ✔ mate.module.code=" + code + "  → Flyway history flyway_history_" + code
                        + " (independent version line)");
                System.out.println("  ✔ gateway-path /api/v1/" + code + "/**  → mate-gateway auto-routes (no gateway change)");
                System.out.println("  ✔ menu-manifest.yml + mate-menu-starter  → menus self-register to mate-system");
                System.out.println("  ✔ V1__" + code + "_schema.sql demo migration");
                System.out.println();
                System.out.println("Next:");
                System.out.println("  1) replace the demo table / menu / controllers with real content");
                System.out.println("  2) build:  mvn -q -o install -pl mate-biz/" + name + " -am -DskipTests");
                System.out.println("  3) run:    cd mate-biz/" + name + " && mvn spring-boot:run");
                System.out.println("  (no mate-system / mate-gateway edits needed — see docs/conventions/pluggable-module-guide.md)");
                return 0;
            } catch (Exception e) {
                System.err.println("Failed to create module: " + e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "aggregate", description = "Create a new aggregate in an existing module")
    public static class AggregateSub implements Callable<Integer> {

        @Parameters(index = "0", description = "Aggregate name (e.g., Order)")
        String name;

        @Option(names = "--module", description = "Target module", required = true)
        String module;

        @Option(names = "--root", description = "Project root directory", defaultValue = ".")
        String root;

        @Override
        public Integer call() {
            Path projectRoot = Paths.get(root).toAbsolutePath().normalize();
            try {
                new AggregateTemplate(projectRoot, module, name).generate();
                System.out.println("Created aggregate: " + name + " in module " + module);
                return 0;
            } catch (Exception e) {
                System.err.println("Failed to create aggregate: " + e.getMessage());
                return 1;
            }
        }
    }
}
