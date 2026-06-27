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

import java.nio.file.Path;
import java.util.List;

/**
 * {@code mate down} — stop the MateCloud stack (docker-compose down).
 *
 * @author mateaix
 */
@Command(name = "down", description = "Stop the MateCloud stack (docker-compose down)")
public class DownCommand implements Runnable {

    @Override
    public void run() {
        Path root = UpCommand.findProjectRoot();
        if (root == null) {
            System.err.println("Could not locate MateCloud project root.");
            return;
        }
        // Compose v1 binary (hyphenated). See UpCommand for rationale.
        String compose = System.getProperty("os.name").toLowerCase().contains("win") ? "docker-compose.exe" : "docker-compose";
        UpCommand.exec(List.of(compose, "down"), root);
    }
}
