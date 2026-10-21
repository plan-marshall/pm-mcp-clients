# pm-mcp-clients

The client contract of [plan-marshall-mcp](https://github.com/plan-marshall/plan-marshall-mcp) and everything that runs outside the daemon:

| Module | What it is |
|---|---|
| [`pm-api`](pm-api/) | Client contract of the daemon's API: socket client, on-demand start, runtime-token validation, and host detection |
| [`pm-exec`](pm-exec/) | Job confinement launcher, native binary `pm-exec` |
| [`pm-relay`](pm-relay/) | Harness- and model-facing CLI with the `serve` relay, native binary `pm-mcp` |
| [`pm-operator`](pm-operator/) | Terminal operator CLI and interactive JLine console, native binary `pm-operator` |
| [`pm-mock-runtime`](pm-mock-runtime/) | Scenario-driven in-memory mock server providing Unix domain socket and loopback HTTP API |
| [`pm-web-app`](pm-web-app/) | Browser-based operator web interface (SvelteKit + Vite) |
| [`pm-web-e2e`](pm-web-e2e/) | End-to-end Playwright tests for the web interface |

The modules are proprietary software; see [LICENSE.md](LICENSE.md). They are deployed as `SNAPSHOT` versions to the GitHub Packages registry of the organisation `plan-marshall` on every merge to `main` and are never released; the product consumes them from there.

A machine that builds needs a token for the registry; the setup is described in the developer documentation of plan-marshall-mcp (`doc/developer/registry-setup.adoc`). The Maven CLI reads the registry profile from [`.mvn/settings.xml`](.mvn/settings.xml) through `.mvn/maven.config`; the Java extension's m2e importer resolves the same file differently, so keep the same `plan-marshall` profile and repositories in `~/.m2/settings.xml` beside the token as well. Without it the editor can report the parent POM as unresolvable while `./mvnw` builds. Keep `java.autobuild.enabled` off in the editor: Maven is the build, and a background JDT build competes with `./mvnw` for CPU and output folders.

---

## Manual Testing & Local Development

For local development and manual verification of the operator interfaces, the repository provides [`pm-mock-runtime`](pm-mock-runtime/README.adoc), an in-memory, scenario-driven mock server. It provides both the Unix domain socket for CLI tools and loopback HTTP for web interfaces without requiring a live daemon.

### 1. Starting the Mock Runtime

Launch the mock runtime using its lifecycle script (with smart incremental build and dynamic scenario selection):

```bash
# Interactively choose scenario from dynamic menu:
./pm-mock-runtime/start.sh

# Or specify scenario and web port directly:
./pm-mock-runtime/start.sh --scenario onboarding-happy --web-port 7420

# Stop mock runtime:
./pm-mock-runtime/stop.sh
```

Or via the Maven wrapper:
```bash
./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario onboarding-happy --web-port 7420"
```

The mock runtime logs its connection coordinates:
```text
Mock runtime started successfully:
  Base (PM_MCP_BASE): /tmp/pm-mock-xyz/b
  Unix Socket:        /tmp/pm-mock-xyz/b/run/pm-mcpd.sock
  Loopback Web Port:  7420 (http://127.0.0.1:7420)
  Scenario:           onboarding-happy
  Runtime Token:      mock-token-abcde-12345
  Pairing Code:       MOCKMOCK
  Web App URL:        http://localhost:5173/pair#code=MOCKMOCK
```

---

### 2. Manually Testing the CLI (`pm-operator`)

Detailed instructions and architecture details are available in [`pm-operator/README.md`](pm-operator/README.md).

#### Pointing to the Mock Runtime
In another terminal, export `PM_MCP_BASE` using the path logged by the mock server:
```bash
export PM_MCP_BASE=/tmp/pm-mock-xyz/b
```

#### Running Commands
You can run verbs directly via the lifecycle script (with implicit incremental build), via the Maven wrapper, via the executable JAR, or as a native binary:

```bash
# 1. Via Lifecycle Scripts (start.sh & stop.sh)
./pm-operator/start.sh status
./pm-operator/start.sh config show
./pm-operator/start.sh                 # opens interactive console
./pm-operator/start.sh -d status       # background mode (target/pm-operator.pid)
./pm-operator/stop.sh                  # stops background instance

# 2. Via Maven Wrapper
./mvnw -pl pm-operator exec:java -Dexec.args="status"
./mvnw -pl pm-operator exec:java -Dexec.args="config show"
./mvnw -pl pm-operator exec:java -Dexec.args="web pair"
./mvnw -pl pm-operator exec:java -Dexec.args="project add ."

# 3. Via Standalone Executable JAR
./mvnw package -DskipTests -pl pm-operator
java -jar pm-operator/target/pm-operator-0.1.0-SNAPSHOT.jar status
java -jar pm-operator/target/pm-operator-0.1.0-SNAPSHOT.jar config show

# 4. Interactive Console (JLine)
# Running pm-operator without arguments launches the interactive terminal console:
java -jar pm-operator/target/pm-operator-0.1.0-SNAPSHOT.jar

# 5. GraalVM Native Image
./mvnw package -Pnative -DskipTests -pl pm-operator
./pm-operator/target/pm-operator status
./pm-operator/target/pm-operator # opens console
```

#### Operator Security Preconditions (`PM-SEC-3`)
- `pm-operator` requires an **interactive terminal** (`System.console() != null`).
- It refuses execution (exit code `4`) if coding-agent host signals (`ANTIGRAVITY_AGENT`, `CLAUDE_CODE`, `OPENCODE_CLIENT`, `CODEX_*`) are present in the environment.
- If testing inside an IDE or assistant terminal session where these variables are set, clear them:
  ```bash
  env -u ANTIGRAVITY_AGENT -u GEMINI_CLI -u CLAUDE_CODE -u OPENCODE_CLIENT java -jar pm-operator/target/pm-operator-0.1.0-SNAPSHOT.jar status
  ```

---

### 3. Manually Testing the Web Interface (`pm-web-app`)

Detailed instructions are available in [`pm-web-app/README.md`](pm-web-app/README.md).

#### Running the Development Server
With `pm-mock-runtime` running on port 7420:
```bash
# Start Vite in background (default, target/pm-web-app.pid):
./pm-web-app/start.sh

# Or start in foreground:
./pm-web-app/start.sh --foreground

# Stop Vite background instance:
./pm-web-app/stop.sh

# Or manually via npm:
cd pm-web-app
npm install
npm run dev
```

Open `http://localhost:5173` in your browser. Vite automatically proxies `/api` and `/mock` endpoints to `http://127.0.0.1:7420`.

#### Testing User Flows
- **Pairing Flow**: Navigate to `http://localhost:5173/pair#code=TEST-CODE` to verify browser pairing.
- **Onboarding Setup**: Test the multi-step configuration wizard.
- **Mock State Transitions**: Switch scenarios or inspect state via `/mock/` endpoints while the web app is running.

#### Running Component and End-to-End Tests
```bash
# Unit & Component Tests (Vitest)
cd pm-web-app
npm test

# End-to-End Tests (Playwright)
cd ../pm-web-e2e
npm install
npx playwright install --with-deps
npm test            # headless
npm run test:headed # visible browser window
npm run test:ui     # interactive Playwright UI
```
