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

import org.jline.console.SystemRegistry;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Parser;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.shell.jline3.PicocliCommands;
import picocli.shell.jline3.PicocliCommands.PicocliCommandsFactory;
import vip.mate.cli.MateCliApplication;
import vip.mate.cli.render.Ansi;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.function.Supplier;

/**
 * {@code mate shell} — interactive REPL over the whole command tree.
 * <p>
 * One JVM (no per-command startup cost), with Tab-completion of every command +
 * option (via picocli-shell-jline3), persistent history ({@code ~/.mate/shell_history}),
 * and JLine built-ins ({@code help}, {@code clear}, {@code history}). Type any
 * normal command without the {@code mate} prefix, e.g. {@code status},
 * {@code service list}, {@code cache get <key>}, {@code ai chat "..."}.
 *
 * @author mateaix
 */
@Command(name = "shell", description = "Interactive REPL — all commands in one JVM, with Tab-completion + history")
public class ShellCommand implements Runnable {

    private static final Set<String> QUIT = Set.of("exit", "quit", "/exit", "/quit", ":q");

    @Override
    public void run() {
        Supplier<Path> workDir = () -> Paths.get(System.getProperty("user.dir"));
        PicocliCommandsFactory factory = new PicocliCommandsFactory();
        // Build the REPL over a fresh root so every subcommand is reachable.
        CommandLine cmd = new CommandLine(new MateCliApplication(), factory);
        PicocliCommands picocliCommands = new PicocliCommands(cmd);
        Parser parser = new DefaultParser();

        try (Terminal terminal = TerminalBuilder.builder().build()) {
            SystemRegistry registry = new SystemRegistryImpl(parser, terminal, workDir, null);
            registry.setCommandRegistries(picocliCommands);

            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(registry.completer())
                    .parser(parser)
                    .variable(LineReader.LIST_MAX, 50)
                    .variable(LineReader.HISTORY_FILE, historyFile())
                    .build();
            factory.setTerminal(terminal);

            printBanner(terminal);
            String prompt = Ansi.enabled() ? Ansi.cyan("mate") + Ansi.dim(" ❯ ") : "mate> ";
            while (true) {
                try {
                    registry.cleanUp();
                    String line = reader.readLine(prompt);
                    if (line == null) {
                        break;
                    }
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    if (QUIT.contains(trimmed)) {
                        break;
                    }
                    registry.execute(line);
                } catch (UserInterruptException e) {
                    // Ctrl-C: abandon the current line, keep the shell alive.
                } catch (EndOfFileException e) {
                    break; // Ctrl-D
                } catch (Exception e) {
                    registry.trace(e); // print the error, keep going
                }
            }
        } catch (Exception e) {
            System.err.println(Ansi.fail("Shell failed to start: " + e.getMessage()));
            return;
        }
        System.out.println(Ansi.muted("bye 👋"));
    }

    private Path historyFile() {
        File dir = new File(System.getProperty("user.home"), ".mate");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        return new File(dir, "shell_history").toPath();
    }

    private void printBanner(Terminal terminal) {
        terminal.writer().println(Ansi.heading("MateCloud shell")
                + Ansi.muted("   Tab 补全 · ↑↓ 历史 · <命令> -h 帮助 · exit 退出"));
        terminal.writer().flush();
    }
}
