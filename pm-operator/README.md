# pm-operator

`pm-operator` is the terminal operator command-line interface and interactive console of [plan-marshall-mcp](https://github.com/plan-marshall/plan-marshall-mcp). It compiles both as a standard executable JAR and as an ahead-of-time (AOT) GraalVM native binary.

---

## Capabilities & Architecture

- **Interactive Console (`pm-operator`)**: Built on JLine 4 with terminal capability detection, command completion, history persistence, styled prompts (`pm ›`, `pm [setup 3/7] ›`), and asynchronous notifications (`ConsoleNotice`).
- **Direct CLI Verbs**: Command tree implemented via Picocli (`status`, `runtime`, `config`, `project`, `projects`, `plan`, `epic`, `recipe`, `questions`, `answer`, `reports`, `web`, `client`, `drain`, `abort`).
- **Operator Gate (`PM-SEC-3`)**: Verifies interactive terminal attachment (`System.console() != null && System.console().isTerminal()`) and refuses execution (exit code 4) if coding-agent host signals (`ANTIGRAVITY_AGENT`, `CLAUDE_CODE`, `OPENCODE_CLIENT`, `CODEX_*`) are present.
- **Unix Domain Socket Client**: Communicates with the daemon exclusively over local Unix domain sockets via `pm-api` (`UnixSocketHttpClient`), authenticated with `run/runtime.token`.
- **QR Code Pairing**: Generates ASCII and ANSI terminal QR codes (`QrCodeRenderer`) for pairing browsers and mobile devices with the runtime's web listener.

---

## Prerequisites

- **Java**: OpenJDK 25 or GraalVM JDK 25.
- **Maven**: Maven wrapper `./mvnw` provided in the repository root.
- **OS**: macOS or Linux (POSIX Unix domain socket support).

---

## Building

### Standard Executable JAR
```bash
./mvnw clean package -pl pm-operator
```
The output JAR is produced at `pm-operator/target/pm-operator-0.1.0-SNAPSHOT.jar`.

### GraalVM Native Image
To build the native binary using the `native` profile:
```bash
./mvnw package -Pnative -DskipTests -pl pm-operator
```
The resulting standalone native executable is created at `pm-operator/target/pm-operator`.

---

## Manual Testing & Local Execution

To test `pm-operator` manually without a running production daemon, use the scenario-driven `pm-mock-runtime` server.

### Step 1: Start the Mock Runtime
In a dedicated terminal, launch `pm-mock-runtime` with a target scenario:
```bash
./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario onboarding-happy --web-port 7420"
```
The mock server logs its runtime base directory and token:
```text
Mock runtime started successfully:
  Base (PM_MCP_BASE): /tmp/pm-mock-xyz/b
  Unix Socket:        /tmp/pm-mock-xyz/b/run/runtime.sock
  Loopback Web Port:  7420 (http://127.0.0.1:7420)
  Scenario:           onboarding-happy
```

### Step 2: Point `pm-operator` to the Mock Runtime
In another terminal, export `PM_MCP_BASE`:
```bash
export PM_MCP_BASE=/tmp/pm-mock-xyz/b
```
*(Note: `./pm-operator/start.sh` automatically detects `PM_MCP_BASE` when `pm-mock-runtime` was started in the background with `./pm-mock-runtime/start.sh`; the direct `exec:java` command above does not create the PID and log files used for detection).*

### Step 3: Run Verbs and Console

### Running with Lifecycle Scripts (`start.sh` & `stop.sh`)

`pm-operator` includes smart lifecycle scripts that perform incremental builds (`./mvnw -am` without `clean`) automatically:

```bash
# Run verbs directly (foreground)
./pm-operator/start.sh status
./pm-operator/start.sh config show

# Launch interactive JLine console (foreground)
./pm-operator/start.sh

# Run in background / daemon mode (stores PID in target/pm-operator.pid)
./pm-operator/start.sh -d status

# Stop background instance
./pm-operator/stop.sh
```

#### Running Individual Verbs (Direct)
```bash
# Via Maven Wrapper
./mvnw -pl pm-operator exec:java -Dexec.args="status"
./mvnw -pl pm-operator exec:java -Dexec.args="config show"
./mvnw -pl pm-operator exec:java -Dexec.args="web pair"
./mvnw -pl pm-operator exec:java -Dexec.args="project add ."

# Or via the packaged JAR
java -jar pm-operator/target/pm-operator-0.1.0-SNAPSHOT.jar status
java -jar pm-operator/target/pm-operator-0.1.0-SNAPSHOT.jar config show

# Or via the native binary
./pm-operator/target/pm-operator status
```

#### Running the Interactive Console (Direct)
Launching `pm-operator` without arguments opens the interactive JLine console:
```bash
java -jar pm-operator/target/pm-operator-0.1.0-SNAPSHOT.jar
# or
./pm-operator/target/pm-operator
```
Inside the console, commands can be executed interactively:
```text
pm › help
pm › status
pm › plans
pm › questions
pm › exit
```

### Manual Check: The Full-Screen Overview from the Console

A copy-paste check of the hand-off (it opens the same view as the direct `plans` command):

1. Start the mock runtime with the showroom scenario:
   ```bash
   ./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario fully-onboarded --web-port 7420"
   ```
2. In another terminal, point `pm-operator` at the base the mock printed
   (`Base (PM_MCP_BASE): /tmp/pm-mock-xyz/b`):
   ```bash
   export PM_MCP_BASE=/tmp/pm-mock-xyz/b
   ```
3. Start the console:
   ```bash
   ./pm-operator/start.sh
   ```
4. At `pm ›` type `plans` (or its alias `tui`). The terminal enters the alternate screen and draws
   `Running work · projects`.
5. Move with the arrow keys, open a row with `Enter`, and leave with `q`.
6. The prompt returns and the console stays usable; `exit` leaves with code `0`.

The same view opens directly with `./pm-operator/start.sh plans` or `./pm-operator/start.sh --tui`.

Notes:

- The VS Code integrated terminal is a real TTY and is supported (`TERM=xterm-256color`,
  `TERM_PROGRAM=vscode`).
- The IDE's "Run" button or output panel is not a TTY: there the view cannot open, and the command
  prints one line naming the reason before the status tree.
- A terminal narrower than 60 columns, `TERM=dumb`, and `console.full_screen=false` in
  `machine-config.json` are the other documented fallbacks; each names its reason.

---

## Troubleshooting & Operator Gate Notes

### Exit Code 4: Operator Refused (Coding-Agent Detected)
If you run `pm-operator` from an environment where AI assistant signals are exported (e.g. within an IDE terminal session or agent sandbox), the Operator Gate (`OperatorGate`) refuses the execution:
```text
pm-operator: operator refused: coding-agent environment detected (antigravity)
```
To run tests manually from such a terminal, launch the command in a clean environment:
```bash
env -u ANTIGRAVITY_AGENT -u GEMINI_CLI -u CLAUDE_CODE -u OPENCODE_CLIENT java -jar pm-operator/target/pm-operator-0.1.0-SNAPSHOT.jar status
```

### Interactive Terminal Required
`pm-operator` verbs require an interactive terminal (TTY). If standard input is piped or redirected (e.g. `echo "status" | pm-operator`), execution is refused unless explicitly supported by batch-oriented options.

---

## Running Automated Tests

```bash
# Run unit tests and pre-commit checks
./mvnw clean verify -Ppre-commit -pl pm-operator

# Run integration tests (process execution ITs)
./mvnw verify -Pintegration-tests -pl pm-operator
```
