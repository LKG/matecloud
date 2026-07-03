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
import vip.mate.cli.render.Ansi;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import vip.mate.cli.nacos.NacosClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@code mate config init|get|push|delete} — manage Nacos config center.
 * <p>
 * {@code init} publishes the packaged {@code mate-infra-${profile}.yml}
 * template so a fresh Nacos instance can be bootstrapped with one command.
 */
@Command(name = "config", description = "Manage Nacos config center",
        subcommands = {
                ConfigCommand.InitSub.class,
                ConfigCommand.GetSub.class,
                ConfigCommand.PushSub.class,
                ConfigCommand.DeleteSub.class
        })
public class ConfigCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Usage: mate config <init|get|push|delete>");
    }

    @Command(name = "init", description = "Publish the shared mate-infra-${profile}.yml template to Nacos")
    public static class InitSub implements Runnable {
        @Option(names = "--profile", description = "Spring profile", defaultValue = "dev")
        String profile;

        @Option(names = "--overwrite", description = "Overwrite if already exists")
        boolean overwrite;

        @Override
        public void run() {
            NacosClient nacos = new NacosClient();
            String dataId = "mate-infra-" + profile + ".yml";
            try {
                String existing = nacos.getConfig(dataId, null);
                if (existing != null && !existing.isBlank() && !overwrite) {
                    System.out.println("Config already exists: " + dataId
                            + " (use --overwrite to replace)");
                    return;
                }
            } catch (Exception ignored) {
                // Not found is fine
            }
            String template = loadTemplate(profile);
            boolean ok = nacos.publishConfig(dataId, null, template);
            System.out.println((ok ? "[OK] " : "[FAIL] ") + "Published " + dataId
                    + " to " + nacos.getServerAddr() + " namespace=" + nacos.getNamespace());
        }

        private String loadTemplate(String profile) {
            // Try profile-specific template first, fall back to dev
            String[] candidates = {
                    "/mate-infra-" + profile + "-template.yml",
                    "/mate-infra-dev-template.yml"
            };
            for (String path : candidates) {
                try (InputStream in = getClass().getResourceAsStream(path)) {
                    if (in != null) {
                        return new String(in.readAllBytes());
                    }
                } catch (IOException ignored) {
                }
            }
            throw new IllegalStateException(
                    "No packaged template for profile '" + profile
                    + "'. Add mate-infra-" + profile + "-template.yml to mate-cli resources.");
        }
    }

    @Command(name = "get", description = "Fetch a Nacos config by dataId")
    public static class GetSub implements Runnable {
        @Parameters(index = "0", description = "dataId (e.g. mate-infra-dev.yml)")
        String dataId;

        @Option(names = "--group", description = "Nacos group (default: DEFAULT_GROUP)")
        String group;

        @Override
        public void run() {
            try {
                System.out.println(new NacosClient().getConfig(dataId, group));
            } catch (Exception e) {
                System.err.println(Ansi.fail("Failed: " + e.getMessage()));
            }
        }
    }

    @Command(name = "push", description = "Upload a local YAML file to Nacos as a config")
    public static class PushSub implements Runnable {
        @Parameters(index = "0", description = "dataId")
        String dataId;

        @Option(names = "--file", description = "Local file to upload", required = true)
        String file;

        @Option(names = "--group")
        String group;

        @Override
        public void run() {
            try {
                String content = Files.readString(Path.of(file));
                boolean ok = new NacosClient().publishConfig(dataId, group, content);
                System.out.println((ok ? "[OK] " : "[FAIL] ") + "Pushed " + dataId);
            } catch (Exception e) {
                System.err.println(Ansi.fail("Failed: " + e.getMessage()));
            }
        }
    }

    @Command(name = "delete", description = "Delete a Nacos config")
    public static class DeleteSub implements Runnable {
        @Parameters(index = "0", description = "dataId")
        String dataId;

        @Option(names = "--group")
        String group;

        @Override
        public void run() {
            try {
                boolean ok = new NacosClient().deleteConfig(dataId, group);
                System.out.println((ok ? "[OK] " : "[FAIL] ") + "Deleted " + dataId);
            } catch (Exception e) {
                System.err.println(Ansi.fail("Failed: " + e.getMessage()));
            }
        }
    }
}
