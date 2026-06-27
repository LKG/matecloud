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
package vip.mate.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import vip.mate.cli.command.AiCommand;
import vip.mate.cli.command.CacheCommand;
import vip.mate.cli.command.ConfigCommand;
import vip.mate.cli.command.DbCommand;
import vip.mate.cli.command.DownCommand;
import vip.mate.cli.command.GenCommand;
import vip.mate.cli.command.LogsCommand;
import vip.mate.cli.command.NewCommand;
import vip.mate.cli.command.RpcCommand;
import vip.mate.cli.command.ServiceCommand;
import vip.mate.cli.command.StatusCommand;
import vip.mate.cli.command.UpCommand;
import vip.mate.cli.mcp.McpServerMode;

/**
 * MateCloud CLI entry point.
 *
 * Usage:
 *   mate new module &lt;name&gt; [--port 9050]        Scaffold a new business module
 *   mate new aggregate &lt;name&gt; --module X        Scaffold an aggregate inside an existing module
 *   mate up [--mvn]                             Start the whole stack (docker-compose up -d)
 *   mate down                                   Stop the whole stack
 *   mate status                                 One-screen cluster overview (Nacos + /actuator/health)
 *   mate logs &lt;service&gt;                         Tail docker-compose logs
 *   mate service list|info|health               Query services registered in Nacos
 *   mate config init|get|push|delete            Manage Nacos config center
 *   mate ai tools|chat|providers                Inspect or call AI features
 *   mate --mcp                                  Run as MCP stdio server for Claude Code/Desktop
 */
@Command(
        name = "mate",
        description = "MateCloud CLI — scaffold, orchestrator, config manager, AI agent",
        mixinStandardHelpOptions = true,
        version = "mate-cli 1.0.0",
        subcommands = {
                NewCommand.class,
                UpCommand.class,
                DownCommand.class,
                StatusCommand.class,
                LogsCommand.class,
                ServiceCommand.class,
                ConfigCommand.class,
                AiCommand.class,
                RpcCommand.class,
                DbCommand.class,
                CacheCommand.class,
                GenCommand.class
        }
)
public class MateCliApplication implements Runnable {

    @CommandLine.Option(names = "--mcp", description = "Run as MCP stdio server")
    private boolean mcpMode;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MateCliApplication()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        if (mcpMode) {
            new McpServerMode().start();
            return;
        }
        CommandLine.usage(this, System.out);
    }
}
