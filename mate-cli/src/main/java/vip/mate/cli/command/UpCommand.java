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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code mate up} — start the whole MateCloud stack with a single command.
 * <p>
 * By default runs {@code docker-compose up -d} (Compose v1, hyphenated binary).
 * Use {@code --mvn} to spawn each service via {@code mvn spring-boot:run} in a
 * background process — handy when iterating on a single service without Docker.
 *
 * @author mateaix
 */
@Command(name = "up", description = "Start the MateCloud stack (docker-compose up -d or mvn spring-boot:run)")
public class UpCommand implements Runnable {

    @Option(names = "--mvn", description = "Use Maven (mvn spring-boot:run) instead of docker-compose")
    boolean useMvn;

    @Option(names = "--services", description = "Comma-separated subset (default: all)")
    String services;

    @Override
    public void run() {
        Path projectRoot = findProjectRoot();
        if (projectRoot == null) {
            System.err.println("Could not locate MateCloud project root. Run from inside the repo.");
            return;
        }
        List<String> cmd = new ArrayList<>();
        if (useMvn) {
            System.out.println("[!] --mvn mode: will start services one-by-one. Use Ctrl+C to stop.");
            cmd.add("mvn");
            cmd.add("-q");
            cmd.add("spring-boot:run");
            cmd.add("-pl");
            cmd.add(services != null ? services : "mate-gateway,mate-auth,mate-biz/mate-system,mate-biz/mate-notice");
            cmd.add("-am");
        } else {
            // Compose v1 binary (hyphenated). NOTE: the v2 plugin form
            // {@code docker compose} is intentionally NOT used — this project
            // standardises on v1 to match deploy.sh / Makefile / RFC-023.
            cmd.add(System.getProperty("os.name").toLowerCase().contains("win") ? "docker-compose.exe" : "docker-compose");
            cmd.add("up");
            cmd.add("-d");
            if (services != null && !services.isBlank()) {
                for (String s : services.split(",")) cmd.add(s.trim());
            }
        }
        exec(cmd, projectRoot);
    }

    static Path findProjectRoot() {
        Path cwd = Path.of(".").toAbsolutePath().normalize();
        for (Path p = cwd; p != null; p = p.getParent()) {
            if (Files.exists(p.resolve("docker-compose.yml")) && Files.exists(p.resolve("pom.xml"))) {
                return p;
            }
        }
        return null;
    }

    static int exec(List<String> command, Path workDir) {
        System.out.println("$ " + String.join(" ", command));
        try {
            ProcessBuilder pb = new ProcessBuilder(command).directory(workDir.toFile()).inheritIO();
            Process p = pb.start();
            return p.waitFor();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            System.err.println("Command failed: " + e.getMessage());
            return -1;
        }
    }
}
