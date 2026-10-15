# Implementation Plan: PM-Operator Terminal Client & CLI Infrastructure

Status: Ready for Implementation
Target Module: `pm-operator` (and evaluation of dedicated `pm-operator-it`)
Reference Specifications:
- `plan-marshall-documentation/doc/specification/cli-and-security/01-cli-and-installer.adoc`
- `plan-marshall-documentation/doc/specification/cli-and-security/02-credentials-and-operator.adoc`
- `plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc`
- `plan-marshall-documentation/doc/specification/module-structure.adoc`
- `pm-mcp-clients/doc/design/terminal-client/` (01-commands to 26-cleanup)

---

## 1. Operational & Governance Discipline

### Git & Worktree Workflow
- **Isolated Worktree**: All implementation work is conducted on a dedicated git worktree (e.g., `/Users/oliver/git/worktrees/pm-mcp-clients-operator` on feature branch `feat/pm-operator`) branched from `main`.
- **Build Tool Invariant**: **Always use the `./mvnw` wrapper** (never naked `mvn` or global installations).

### PR, Quality Gate & Landing Cycle
1. **Branch & PR**: Commit incremental, conventional changes and push to origin on the feature branch; open a GitHub Pull Request.
2. **Review & Observation Window**: Wait 5 minutes for CI workflows, review bots (CodeRabbit, cuioss-review-bot), and SonarCloud analysis to complete.
3. **Findings Resolution**:
   - Triage and resolve all review comments (implement fixes or provide technical refutations).
   - Resolve all SonarCloud findings (0 new issues, 0 security hotspots, 0% duplication, >= 80% test coverage).
4. **Merge Queue & Landing**:
   - Add the PR to the GitHub merge queue (`gh pr merge <pr-number> --squash` or `--auto`).
   - Await merge queue CI validation and automatic landing onto `main`.
   - Switch back to `main` and synchronize local repository (`git checkout main && git pull`).
   - Clean up the worktree and feature branch.

---

## 2. Applicable Standards & Skills Matrix

The implementation and verification strictly adhere to the following skillset:

| Skill | Focus & Key Rules |
|---|---|
| **pm-dev-java:java-core** | Java 21/25 modern language features (records, sealed hierarchies, pattern matching switch, sequenced collections). Clean package structure, Command-Query Separation (CQS), no leaked streams/resources. |
| **pm-dev-java:java-cdi** | Constructor injection, immutable dependencies (`final`), clean separation of concerns, decoupling terminal/runtime clients for testability. |
| **pm-dev-java:java-lombok** | Use `@Builder`, `@UtilityClass` for static helpers; prefer Java records for immutable data carriers; avoid `@Value` or `@Data` on domain models. |
| **pm-dev-java:java-null-safety** | Package-level `@NullMarked` in `package-info.java` using JSpecify; return `Optional<T>` instead of `@Nullable`; defensive `Objects.requireNonNull()` at public API boundaries. |
| **pm-dev-java:java-quarkus** | GraalVM native image metadata, reflection hints for Picocli & JLine 4, strictly respecting the reactor ban on Quarkus runtime dependencies in client modules. |
| **pm-dev-java:java-security** | Trust boundary input validation (`jakarta.validation`), secure token masking in logs/console, directory/file mode verification (`0700`/`0600`), and agent environment rejection (`PM-SEC-3`). |
| **pm-dev-java:junit-core** | AAA pattern without phase comments, grouped assertions (`assertAll`), `@Nested` test classes, minimum 80% line/branch coverage, at least one test class per production class. |
| **pm-dev-java:junit-integration** | Maven Failsafe (`*IT.java`) integration testing separated from Surefire (`*Test.java`), `-Pintegration-tests` lifecycle management. |
| **pm-dev-java-cui:cui-http** | Use CUI `HttpHandler` and `HttpResult<T>` sealed interface with exhaustive pattern matching for all runtime REST communication (never raw `HttpClient` or unhandled exceptions). |
| **pm-dev-java-cui:cui-http-testing**| `@EnableMockWebServer` with `@MockResponseConfig` and `URIBuilder` injection for HTTP client verification without hardcoded ports. |
| **pm-dev-java-cui:cui-logging** | `CuiLogger` with DSL-style `LogMessages` / `LogRecord` constants; exception parameter first; `%s` format specifiers; verified in tests via `LogAsserts`. |
| **pm-dev-java-cui:cui-testing** | Mandatory `Generators.*` for test data (`@EnableGeneratorController`); no manual test strings or `UUID.randomUUID()`; **no Mockito** (use stubs, DI, or EasyMock); JUL log testing via `@EnableTestLogger`. |

---

## 3. Analysis: JLine 4 Testing Architecture & Helper Libraries

### Problem & Challenge
Testing interactive console applications (JLine 4 + Picocli) often encounters hurdles:
- Automated CI environments lack an interactive TTY (`isatty` returns false).
- Native PTY (pseudo-terminal) libraries (`pty4j`, `jline-terminal-jansi`) can be flaky, OS-dependent, or extract native binaries at test time.
- Standard I/O (`System.in`, `System.out`, `System.exit`) is global, causing test concurrency issues and JVM exit traps.

### Evaluation of JLine Testing Approaches & Libraries
1. **JLine Built-in Virtual/Dumb Terminal (`TerminalBuilder.builder().streams(in, out).dumb(true).build()`) [CORE STRATEGY]**:
   - JLine 3 and 4 have first-class, built-in support for non-system terminals backed by arbitrary `InputStream` and `OutputStream`.
   - Allows feeding pre-scripted user inputs (keystrokes, tab completion requests, multi-line entries) via `ByteArrayInputStream` or `PipedInputStream`, and capturing ANSI/plain output in `ByteArrayOutputStream`.
   - **Advantage**: Zero external dependencies, 100% deterministic, instant execution, runs in any CI container without native PTY allocation.

2. **Dependency Injection of Terminal & Console Engine**:
   - Rather than invoking static `TerminalBuilder.terminal()` inside commands, design `PmConsole` and interactive prompt handlers with a `Terminal` / `LineReader` provider or context injection.
   - Production uses the native FFM-backed system terminal.
   - Unit tests inject an in-memory virtual terminal.

3. **Picocli Built-in Testing (`CommandLine.execute(...)`)**:
   - Picocli provides built-in stream redirection:
     ```java
     var out = new StringWriter();
     var err = new StringWriter();
     int exitCode = new CommandLine(new PmOperatorCommand())
         .setOut(new PrintWriter(out))
         .setErr(new PrintWriter(err))
         .execute(args);
     ```
   - Avoids intercepting `System.out` or calling `System.exit`.

4. **Environment & TTY Context Decoupling (`OperatorContext`)**:
   - Testing `PM-SEC-3` (agent detection) and `PM-SEC-4` (terminal gates) requires varying environment variables and terminal states.
   - Java 25 enforces strong encapsulation, making reflective mutation of `System.getenv()` illegal and fragile.
   - **Solution**: `OperatorContext` provides functional hooks (`Predicate<String> hasEnv`, `Function<String, String> getEnv`, `BooleanSupplier isInteractiveTty`).
   - Production delegates to `System::getenv` and `System.console() != null && System.console().isTerminal()`.
   - Unit tests pass simple maps or lambdas without bytecode manipulation.

5. **Helper Libraries**:
   - **`org.junit-pioneer:junit-pioneer`**: Offers JUnit 5 extensions (`@StdIo`, `@SetEnvironmentVariable`, `@ClearSystemProperty`) if end-to-end process boundaries need testing in unit tests.
   - **`cui-test-generator` & `cui-test-juli-logger`**: Supply test data and verify log records during CLI operations.

---

## 4. Architectural Analysis: Dedicated IT Module vs In-Module Integration Tests

### Problem Statement
The user asked to consider whether we need a separate integration testing (IT) module for the CLI (similar to `pm-web-e2e` for `pm-web-app`).

### Evaluation
1. **Module Structure Specification (`module-structure.adoc` lines 209 & 575):**
   - *Normative specification*:
     - `pm-operator` depends **only** on `pm-api`.
     - `pm-mock-runtime` depends on `pm-api` only and is a **test-scoped dependency** of `pm-operator`.
     - *"Its `*IT` tests run operator verbs against the mock runtime with its scenarios; the verbs against the real `pm-mcpd` are covered by `pm-e2e`."*
   - Sibling comparison: `pm-relay` (the worker relay CLI) keeps its `PmRelayIT` tests directly inside `pm-relay/src/test`.

2. **Why `pm-web-app` needed `pm-web-e2e`:**
   - `pm-web-app` produces a packaged web resource JAR (`META-INF/resources`) built via `frontend-maven-plugin`.
   - Playwright requires downloading external browser engines (Chromium, WebKit, Firefox) and managing long-running Node/browser processes that do not belong in the reactor artifact build.

3. **Trade-offs for `pm-operator`:**
   - **Option A: In-Module Integration Tests (`pm-operator/src/test/java/.../*IT.java`) [RECOMMENDED]**
     - **Pros**:
       - 100% compliant with normative `module-structure.adoc` specification.
       - Maven Failsafe plugin with `-Pintegration-tests` cleanly separates fast unit tests (`mvn test`) from integration tests (`mvn verify -Pintegration-tests`).
       - `pm-operator` already has `PmOperatorProcessIT.java` measuring startup budget (< 50ms JVM) against `FakeRuntimeServer` / `MockRuntimeServer`.
       - No extra module overhead or POM boilerplate in reactor.
     - **Cons**:
       - Long scenario test suites run in the same module directory, though gated behind `skipITs=true` by default unless `-Pintegration-tests` is active.

   - **Option B: Separate `pm-operator-it` Module**
     - **Pros**:
       - Isolates heavyweight terminal simulation (PTY testing, JLine interactive transcript verification, native binary execution against multi-step mock scenarios).
       - Allows building the native binary in `pm-operator` and testing the packaged executable from `pm-operator-it` without circular dependencies.
     - **Cons**:
       - Diverges from `module-structure.adoc` (requires updating specification to allow `pm-operator-it` in reactor).
       - Adds another module to reactor build.

### Decision & Recommendation
- **Phase 1-4**: Implement unit tests and in-module Failsafe integration tests in `pm-operator` conforming directly to `module-structure.adoc`.
- **Phase 5**: If pseudo-terminal transcript testing or multi-scenario native image benchmarking requires dedicated execution isolation without polluting `pm-operator`, extract/add `pm-operator-it` as an opt-in test harness module.

---

## 5. Implementation Tasks & Checklist

### Phase 1: Security Gates & Process Context (`OperatorGate` & `OperatorContext`)
- [ ] **Task 1.1: Interactive Terminal Gate**
  - Verify attached interactive terminal (`System.console() != null && System.console().isTerminal()`).
  - Implement bypass for test contexts via `OperatorContext`.
- [ ] **Task 1.2: Host Integration Agent-Environment Signal Detection (`PM-SEC-3`)**
  - Read environment signals from `pm-api` Host Integration Table (Claude Code: `CLAUDE_CODE`, `CLAUDE_PROJECT_ROOT`; Gemini/Antigravity: `GEMINI_CLI`; OpenCode: `OPENCODE_CLIENT`; Codex: `CODEX_*`).
  - Reject execution with exit code `4` and diagnostic message naming detected host if any agent signal is present.
- [ ] **Task 1.3: Client Environment Capture (`01-cli-and-installer.adoc#client-environment`)**
  - Capture terminal locale: `LC_ALL` -> `LC_MESSAGES` -> `LANG` -> default `en` (sent as `Accept-Language`).
  - Detect SSH session: check `SSH_CONNECTION` and `SSH_TTY`.
  - Capture login `PATH` and non-secret build environment variables (filtering out credential patterns).
- [ ] **Task 1.4: Direct File Read & Permissions Verification (`PM-SEC-4`)**
  - Restrict direct file reads strictly to: `state/runtime.json`, `run/runtime.token`, `state/install-records.json`, `machine-config.json`.
  - Verify `<PM_MCP_BASE>/run/` mode `0700`, owner = current user, not a symbolic link.
  - Verify `<PM_MCP_BASE>/run/runtime.token` mode `0600`, owner = current user, not a symbolic link.
  - Ensure token is sent as `Authorization: Bearer <token>`; on `401 Unauthorized`, re-read token once and retry once.
  - Ensure `$XDG_STATE_HOME/pm-operator/` (default `~/.local/state/pm-operator/`) directory permissions `0700` and files `0600`.
- [ ] **Task 1.5: Unit Tests for Phase 1**
  - Test terminal gate pass/fail.
  - Test agent environment rejection (exit code 4).
  - Test token permission and symlink rejections.

---

### Phase 2: Interactive JLine 4 Console & Entry Flow (`pm-operator`)
- [ ] **Task 2.1: JLine 4 Dependency & Terminal Setup**
  - Add JLine 4 dependencies (`jline-reader`, `jline-terminal`, `jline-terminal-ffm`) to `pm-operator/pom.xml`.
  - Configure JLine terminal with FFM provider (avoiding runtime JNI extraction).
  - Configure line reader with history (`$XDG_STATE_HOME/pm-operator/history`) and tab completion.
- [ ] **Task 2.2: Entry Flow & State Evaluation (`01-cli-and-installer.adoc#start`)**
  - When `pm-operator` is invoked without a verb:
    - Run operator gate checks.
    - Connect / start runtime on-demand.
    - Query `GET /api/v1/status`:
      - **Not fully onboarded** (`onboard.completed` not reached): render `entry.where-to-onboard` prompt (*Here* or *In the web app*).
      - **Onboarded**: connect web app automatically (enable loopback listener if closed; desktop: open browser with pairing code if unpaired; SSH: print SSH port forward command and URL).
- [ ] **Task 2.3: Interactive REPL & Badged Prompt (`04-console.adoc`)**
  - Render prompt badging:
    - Incomplete onboarding: `pm [setup 3/7] › `
    - Normal prompt: `pm › `
  - Support push event updates above the input prompt via `LineReader.printAbove(...)`.
  - Handle masked secret entry for credentials without echo.
- [ ] **Task 2.4: Console Built-in Commands**
  - Implement console-only commands: `plans`, `plan <id>`, `epic <id>`, `questions`, `answer <id> [<option>]`, `web [qr|copy [ssh]]`, `help [<verb>]`, `exit`.
- [ ] **Task 2.5: Unit & Mock Tests for Phase 2**
  - Test entry flow branching (onboarded vs incomplete).
  - Test SSH vs Desktop browser pairing detection.
  - Test console command dispatch.

---

### Phase 3: Core Operator Verbs (Config, Project, Client, Lifecycle, Web)
- [ ] **Task 3.1: Machine Settings & Aspects (`pm-operator config`)**
  - `pm-operator config`: render `settings.choose` machine settings overview.
  - `pm-operator config <aspect>`: run onboarding aspect as sub-workflow (`language`, `harnesses`, `models`, `roles`, `credentials`, `providers`, `plan-defaults`, `consents`, `documents`).
  - `pm-operator config show [<aspect>|<key>] [<level>]`
  - `pm-operator config set <key> <value> [<level>]`: reject aspect-owned keys with `aspect_owned` (HTTP 409).
  - `pm-operator config unset <key> [<level>]`
  - `pm-operator config cleanup [<level>] [<duration options>]`
- [ ] **Task 3.2: Project Lifecycle (`pm-operator projects` / `project`)**
  - `pm-operator projects [--list]`: project explorer.
  - `pm-operator project add [<path>]`: onboard new project.
  - `pm-operator project setup <project_id> [<step>]`: continue/re-run project onboarding.
  - `pm-operator project favourite <project_id> [--off]`: toggle favourite flag.
  - `pm-operator project release <project_id> [--reason <text>]`: release project.
  - `pm-operator project unenrol <project_id>`: unenrol project (refuse if active work runs).
- [ ] **Task 3.3: Client Bundle Installer & Drift Notices (`pm-operator client`)**
  - `pm-operator client install <client> [--scope user|project] [--yes]`
  - `pm-operator client update [<client>] [--scope user|project] [--force] [--yes]`
  - `pm-operator client remove <client> [--scope user|project] [--force]`
  - Operator Gate: enforce interactive confirmation for removal/weakening of `enforcement` rules (reject `--yes` if rules weakened).
  - `pm-operator client status`: evaluate drift checks (`bundle_outdated`, `bundle_modified`) directly from `state/install-records.json` without runtime.
- [ ] **Task 3.4: Runtime Lifecycle & Drain (`pm-operator runtime`, `drain`)**
  - `pm-operator drain`: signal runtime to drain (timeout bounded by `machine-config.json`).
  - `pm-operator runtime start|install|uninstall`: on-demand start / service unit generation.
  - `pm-operator runtime stop|restart [--drain|--now] [--yes]`
  - `pm-operator runtime stop --withdraw`: cancel pending drain.
  - `pm-operator runtime upgrade [<version>|--check|--withdraw|--notes]`
- [ ] **Task 3.5: Web Access & Pairing (`pm-operator web`)**
  - `pm-operator web enable|disable [--lan]`
  - `pm-operator web pair [--label <label>]`: issue pairing code and print URL.
  - Render terminal QR code via `io.nayuki:qrcodegen` (Unicode half blocks, 4 modules quiet zone, forced black on white).
  - `pm-operator web open [--pair]`: open browser on local machine.
  - `pm-operator web devices [list|rename|revoke|listener-off]`
- [ ] **Task 3.6: Unit & Mock Tests for Phase 3**
  - Test config aspect dispatch and key mutation validation.
  - Test client status drift reporting.
  - Test web pairing & QR code rendering.

---

### Phase 4: Work Control & Decision Verbs (Plans, Epics, Recipes, Decisions)
- [ ] **Task 4.1: Plan Operations (`pm-operator plan`)**
  - `plan add [--project <id>|.] [<source>...] [--title <text>] [--id <plan_id>]`
  - `plan pause <plan_id> [--now|--after-task|--after-phase]`
  - `plan start <plan_id>`
  - `plan cancel|abandon <plan_id> [--reason <text>] [--close-pr|--keep-pr]`
  - `plan chat <plan_id>`: interactive chat with planning role.
  - `plan report <plan_id> [--tokens [--components]]`: completion report.
- [ ] **Task 4.2: Epic Operations (`pm-operator epic`)**
  - `epic add [--project <id>|.] [<source>...] [--title <text>] [--id <epic_id>] | --adopt <epic_id>`
  - `epic next <epic_id> [--raise-scope]`
  - `epic auto <epic_id> on|off | --common-files <n>`
  - `epic decompose <epic_id> [--instruction <text>]`
  - `epic archive|findings|watches|chat <epic_id>`
  - `epic cancel <epic_id> [--reason <text>] [--with-plans|--without-plans]`
- [ ] **Task 4.3: Abort, Recipes, Waivers & Overrides**
  - `abort plan|epic <id> --reason <text>`
  - `recipe run [<recipe_key>] [--project <id>|.] [<source>...] [--param <k>=<v>]...`
  - `waive <plan_id> <gate> --reason <text> [--reason-class <class>]`
  - `waive <plan_id> --raise <budget>=<N> --reason <text>`
  - `decline <plan_id> <request_id> --reason <text>`
  - `unwaive <plan_id> <waiver_id> --reason <text>`
  - `waivers <plan_id> [--pending]`
- [ ] **Task 4.4: Additional Operator Verbs**
  - `roles list|show|qualify`
  - `credentials set|status|verify|remove|rotate`, `credentials migrate os|file`
  - `decisions export [--project <p>] --out <dir>`
  - `grant-access <plan_id> <project_id> read|read-write --reason <text>`
  - `reports [--project <p>] [read|open]`
  - `cleanup [--delete|--remind|--ignore] [--yes]`
  - `accept-edit plan|epic|workspace|lesson|enrolment`
  - `sweep [<project_id>] [--machine] [--apply]`
  - `drop-capture <plan_id>`, `discharge-owed`, `delete-plan <plan_id>`
  - `audit verify`, `upstream allow|deny`, `quality-report <plan_id>|--corpus`
- [ ] **Task 4.5: Unit & Mock Tests for Phase 4**
  - Test plan pause/start/cancel dispatch.
  - Test waiver and override submissions.
  - Test roles and decision export commands.

---

### Phase 5: Integration Testing & Verification
- [ ] **Task 5.1: Mock Runtime Scenario Coverage**
  - Extend `pm-mock-runtime` scenarios to support full CLI interaction tests:
    - Scenario: `cli-entry-unonboarded`
    - Scenario: `cli-entry-onboarded-ssh`
    - Scenario: `cli-plan-lifecycle`
    - Scenario: `cli-waiver-dialogue`
    - Scenario: `cli-client-drift`
- [ ] **Task 5.2: In-Module Process ITs (`PmOperatorProcessIT.java`)**
  - Verify startup to first request median <= 50ms (JVM) / <= 20ms (native) (`PM-TECH-3`).
  - Test full CLI execution against mock runtime scenarios using `ProcessBuilder` or in-process `CommandLine`.
- [ ] **Task 5.3: IT Module Decision Checkpoint**
  - If PTY transcript tests or native binary validation require a decoupled lifecycle, establish `pm-operator-it` profile or module.

---

### Phase 6: Native Image Build & Reflection Metadata
- [ ] **Task 6.1: Picocli-Codegen & Native Image Verification**
  - Ensure `picocli-codegen` generates complete reflection metadata in `target/classes/META-INF/native-image/`.
  - Register JLine 4 FFM / signal classes in native image configuration.
  - Verify `--enable-native-access=ALL-UNNAMED` and headless properties.
- [ ] **Task 6.2: Build and Test Native Binary**
  - Execute `mvn clean package -Pnative`.
  - Validate native executable `target/pm-operator` execution, memory usage, and zero runtime library extractions.
