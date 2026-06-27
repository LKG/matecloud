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
import vip.mate.cli.template.GenCodeTemplate;

import java.nio.file.Path;

@Command(name = "gen", description = "Code generation",
        subcommands = {GenCommand.CodeSub.class})
public class GenCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Usage: mate gen code --table <table> --module <module>");
    }

    @Command(name = "code", description = "Reverse-generate DDD code from a DB table")
    public static class CodeSub implements Runnable {
        @Option(names = "--table", required = true, description = "Database table name (e.g. mate_order)")
        String table;

        @Option(names = "--module", required = true, description = "Target module (e.g. mate-order)")
        String module;

        @Option(names = "--service", description = "Service for DB connection", defaultValue = "mate-admin")
        String service;

        @Override
        public void run() {
            try {
                Path projectRoot = Path.of(System.getProperty("user.dir"));
                GenCodeTemplate template = new GenCodeTemplate(table, module, service, projectRoot);
                template.generate();
            } catch (Exception e) {
                System.err.println("Code generation failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
