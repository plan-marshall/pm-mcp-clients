# PLAN-76: Shared: Ports Passed by the Caller, One Port Block per Plan

epic: ui-improvements
workstream: WS-01

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Let several plans run their mock, web app and browser tests at the same time on one machine. Today every
piece assumes the mock web port `7420` and the app port `5173`. That holds for the mock scripts, the Vite
proxy, the Playwright config, the e2e global setup, about thirty e2e specs, and the URLs the mock itself
reports. So two plans block each other's ports. Worse, they reuse each other's servers: the Playwright
`reuseExistingServer` and the global setup's "mock already running on 7420" check mean one plan can run its
browser tests against another plan's app and mock. And `pm-mock-runtime/stop.sh` kills whatever mock listens
on the port, even one started by another worktree.

After this plan a caller passes the ports. The defaults stay `7420` and `5173`, so the operator's own loop
does not change. Every staged plan of the epic names its own port block (see Standing Constraints → Port
convention), and the orchestrator assigns the blocks, so concurrent plans never share a port.

Source of this plan (read before scoping):

- Operator request of 2026-10-27: plans block each other's ports for the mock and the app (and the terminal
  client when it connects to the mock); the caller must be able to pass the ports, and the ports are set in
  the plans so the orchestrator can run several plans at once.
- `AGENTS.md` § Browser checks (the procedure the plans follow today)
- `pm-mock-runtime/start.sh`, `pm-mock-runtime/stop.sh`, `pm-web-app/start.sh`, `pm-web-app/stop.sh`,
  `pm-web-app/vite.config.ts`, `pm-web-e2e/playwright.config.js`, `pm-web-e2e/global-setup.js`

## Deliverables

1. **Mock: the caller's port, reported truthfully**
   - What: `pm-mock-runtime` takes its web port from `--web-port`, or else from `PM_MOCK_WEB_PORT`, or else
     the default `7420`. The default applies to `start.sh` and to `exec:java`. The engine reports the port it
     actually listens on everywhere it now writes the constant `7420`:
     - the loopback URL;
     - the pairing URL of `POST /mock/pairing`;
     - the web-access port;
     - the loopback listener of the system resources;
     - the "port held by the loopback listener" refusal.
     The scenario files keep their data. `start.sh` with an assigned port (flag or variable) that is in use
     fails with an error naming the port and the process holding it: no fallback to the next free port, and
     no stopping of a process another worktree started. The fallback stays only for the unassigned default.
     `stop.sh` stops only the process its own worktree's PID file names, and never kills by port a mock that
     another worktree started.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:junit-core
   - Done when:
     - A mock test on an ephemeral port (`webPort(0)`) asserts that the pairing URL, the loopback URL and
       the listener port the engine reports name the live port, not `7420` (evidence order: `test-first`)
     - A shell test of `start.sh` with an assigned port that another process holds fails, names the holder
       and leaves that process running. A second shell test shows that `stop.sh` in one worktree leaves a
       mock started from another worktree running (evidence order: `test-first`)
     - Browser check (Playwright MCP) on `fully-onboarded`, with this plan's ports (mock `20760`, app
       `20761`) while a second mock runs on `7420`: pairing through a fresh code works, and the system view
       shows the loopback address `127.0.0.1:20760`. Snapshots and screenshots recorded (evidence order:
       `none`)
2. **Web app and browser tests: ports from the caller**
   - What: Make the ports configurable on every part of the web side:
     - `pm-web-app/vite.config.ts` takes its proxy target from `PM_MOCK_WEB_PORT` and its server port from
       `PM_WEB_PORT` (defaults `7420` and `5173`).
     - `pm-web-app/start.sh` uses `PM_WEB_PORT` when no `-p`/`--port` is given and still records the port in
       `target/pm-web-app.port`; `stop.sh` stops only its own worktree's server.
     - `pm-web-e2e` reads both ports from one helper, `tests/helpers/ports.js` (`MOCK_URL`, `APP_URL`). The
       helper is used by `playwright.config.js` (`baseURL`, `webServer`), `global-setup.js` (start of and
       wait for the mock) and every spec and helper. No other file carries a literal `7420` or `5173`.
     - With assigned ports, a server already holding the port is never reused: `reuseExistingServer` and the
       global setup's "already running" path apply only to the defaults. Otherwise an occupied assigned port
       is an error naming the port.
     - `pm-web-e2e/pom.xml` passes the Maven properties `pm.mock.webPort` and `pm.web.port` to the npm calls
       as the two variables, defaulting to the environment.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - A unit test of the Vite config's port resolution covers: no variable gives the defaults, set
       variables are used, and an invalid value is an error, never a silent default (evidence order:
       `test-first`)
     - A guard test in `pm-web-e2e` fails on a literal `7420` or `5173` in any file below `tests/` except
       `tests/helpers/ports.js`, and in `playwright.config.js` and `global-setup.js` (evidence order:
       `test-first`)
     - Concurrency proof: while a mock runs on `7420` and an app on `5173` from the main checkout,
       `PM_MOCK_WEB_PORT=20760 PM_WEB_PORT=20761 ./mvnw -pl pm-web-e2e verify -Pe2e` passes in the plan's
       worktree, and the processes on `7420` and `5173` are still the main checkout's afterwards. The run
       log and the before and after process lists are recorded (evidence order: `none`)
     - `./mvnw -pl pm-web-e2e verify -Pe2e` with no variables still passes on the defaults (evidence order:
       `none`)
3. **Terminal client and Java tests: no fixed port**
   - What: Make the Java side independent of the fixed port:
     - Every Java test or IT that starts a mock web listener binds an ephemeral port (`webPort(0)`) and reads
       the port from the runtime: the `pm-operator` tests and ITs that name `7420` today
       (`PmOperatorProcessIT` among them) and the `pm-mock-runtime` tests.
     - `pm-operator` builds no web URL from its default port when the runtime reported one. The
       `WebCommand` fallback URL is used only when the runtime's answer carries no URL.
     - The operator's default `7420` (`EntryFlow.DEFAULT_WEB_PORT`, `WebCommand.DEFAULT_PORT`) stays the
       product default.
     - A transcript or screen-capture fixture that shows the port shows the port of its scenario, never a
       port taken from the machine.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-java:junit-integration, pm-dev-java:junit-core
   - Done when:
     - `./mvnw -pl pm-operator verify -Pintegration-tests` passes while another process holds `7420` (evidence
       order: `none`)
     - A test proves that `pm-operator web pair` prints the URL the runtime returned (a mock on an ephemeral
       port), not one built from `7420` (evidence order: `test-first`)
     - Terminal check (screen capture) on `fully-onboarded`: `status` and `web` at 120 × 30 in
       `en_US.UTF-8`, with the mock on an ephemeral port, show the runtime's address. Before and after
       captures recorded (evidence order: `none`)
4. **The port convention, documented**
   - What: Document the ports and the per-plan convention:
     - `AGENTS.md`: § Browser checks and the start commands take the plan's ports, and a new § Ports states
       the convention. Each plan uses the block its spec names: mock web port `20000 + 10 × NN`, app port
       `20000 + 10 × NN + 1`, where NN is the plan number; the rest of the block is reserved. Without a block
       the defaults `7420`/`5173` apply, which is the operator's own loop. An assigned port that is in use is
       a blocker, never a reason to take another port.
     - `README.md` and `pm-mock-runtime/README.adoc`: the two variables and the flag.
     - `doc/design/terminal-client/27-tui-testing.adoc`: the Java tests and ITs bind ephemeral ports.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - `AGENTS.md` § Ports states the formula, the defaults and the "in use is a blocker" rule, and every
       command in § Browser checks carries the port variables or flags (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules
       left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is
       dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at
  the head: `./mvnw -pl pm-mock-runtime verify`, `./mvnw -pl pm-web-app verify`,
  `./mvnw -pl pm-operator verify -Pintegration-tests`, and
  `PM_MOCK_WEB_PORT=20760 PM_WEB_PORT=20761 ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Ports of this plan: mock web port `20760`, app port `20761`. Other plans may be running at the same time
  on `7420`/`5173` or on their own blocks; never stop or reuse a process this worktree did not start.
- The defaults stay `7420` and `5173`. An operator who starts the mock and the app without variables sees
  no change.
- Existing e2e specs and `pm-operator` PTY tests keep passing. A test that asserted `7420` as a reported
  value asserts the live port instead, never a weaker check.
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following
  `AGENTS.md` § Browser checks.
  - Start the mock with this plan's port:
    `./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20760"`.
  - Start the app with `PM_MOCK_WEB_PORT=20760 ./pm-web-app/start.sh -p 20761` (read the port from
    `pm-web-app/target/pm-web-app.port`). Until deliverable 2 works, this plan's own first browser check
    starts Vite with the proxy target it has at that point and records how.
  - The MCP browser starts with an empty profile, so pair again every session with a fresh single-use code:
    `POST http://127.0.0.1:20760/mock/pairing`, then open `<app>/pair#code=<CODE>`, never through the
    `Network:` URL. Reach a state with `POST /mock/goto/<state>` and a reload.
  - Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`).
  - Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and
    structure) and keep one `browser_take_screenshot` per target. The server writes to its `--output-dir`
    `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and
    never write to the repository root.
  - Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress
    record.
  - Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards.
  - If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met. That
    is a blocker reported as such, never replaced by a unit test or skipped.
- Terminal check of the rendered output through the screen capture harness of `cli-pty-dialogue-harness`
  (`ScreenCaptureIT`, command line in `doc/design/terminal-client/27-tui-testing.adoc`).
  - Write this plan's captures to `pm-operator/src/test/resources/screen-captures/{plan}.txt`.
  - Run the set BEFORE the change and copy `pm-operator/target/screens/{plan}/` to
    `.plan/temp/terminal/{plan}/before/`, then run it AFTER and copy it to `.../after/`. Read every `.txt`
    and `.attrs.txt` with the file tools.
  - Never run `pm-operator` in the agent's own shell: the operator gate refuses a coding agent by design
    (`PM-SEC-3`) and is never worked around. Only the harness, which clears the environment like every PTY
    test, runs it.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`,
  `.github/project.yml`; `runbook_lane resolve` reports `sonar: configured`).
  - An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason. Record the
    dismissal as `{issue-key}: {reason}` in the run's progress record and transmit it with
    `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`.
  - A dismissal the run cannot transmit is a blocker, never a silent pass.
  - The merge gate reads the open-issue count at the head: `0` confirmed, or every remaining issue
    dismissed. An `undecidable` read blocks.

## Claim Labels

- OBSERVED: `pm-mock-runtime/start.sh` falls back to the next free port when the requested one is held, and
  notes that the app still proxies to `7420`. Read at `pm-mock-runtime/start.sh:248-279`.
- OBSERVED: `pm-mock-runtime/stop.sh` stops a mock found listening on the port, not only its own. Read at
  `pm-mock-runtime/stop.sh:52-70`.
- OBSERVED: `pm-web-app/vite.config.ts` fixes `port: 5173` and the proxy target `http://127.0.0.1:7420`
  (lines 130-137).
- OBSERVED: `pm-web-e2e/playwright.config.js` fixes `baseURL` and `webServer` to `5173` with
  `reuseExistingServer: !process.env.CI`; `global-setup.js` reuses any mock reachable on `7420`.
- OBSERVED: 19 files below `pm-web-e2e/tests/` carry the literal `http://127.0.0.1:7420`.
- OBSERVED: `MockRuntimeEngine.java` writes the constant `7420` in the loopback URL, the pairing URL, the
  web-access port and the listener resources (lines 297, 1771, 1965, 1993, 2006, 2108).
- HYPOTHESIS: the in-JVM mock of the `pm-operator` PTY harness (`DialogueHarness`, `ScreenCaptureIT`) runs
  with the web listener disabled and the socket under its own base, so it collides with nothing. Confirm or
  refute at `pm-operator/src/test/java/de/planmarshall/operator/transcript/DialogueHarness.java`
  (verify-at-outline).
- HYPOTHESIS: `PmOperatorProcessIT` binds `7420` today. Confirm or refute at
  `pm-operator/src/test/java/de/planmarshall/operator/PmOperatorProcessIT.java` (verify-at-outline).
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or
  refute every HYPOTHESIS above. A refuted cause re-scopes the deliverable and is reported, never silently
  worked around.

## Expected Surface

- OBSERVED: `pm-mock-runtime/start.sh` — present at HEAD b37557b2
- OBSERVED: `pm-mock-runtime/stop.sh` — present at HEAD b37557b2
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/cli/MockRuntimeCli.java` — present at HEAD b37557b2
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at HEAD b37557b2
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/` — present at HEAD b37557b2
- OBSERVED: `pm-mock-runtime/scripts/` — present at HEAD b37557b2
- OBSERVED: `pm-mock-runtime/README.adoc` — present at HEAD b37557b2
- OBSERVED: `pm-web-app/vite.config.ts` — present at HEAD b37557b2
- OBSERVED: `pm-web-app/start.sh` — present at HEAD b37557b2
- OBSERVED: `pm-web-app/stop.sh` — present at HEAD b37557b2
- OBSERVED: `pm-web-app/scripts/` — present at HEAD b37557b2
- OBSERVED: `pm-web-e2e/playwright.config.js` — present at HEAD b37557b2
- OBSERVED: `pm-web-e2e/global-setup.js` — present at HEAD b37557b2
- OBSERVED: `pm-web-e2e/pom.xml` — present at HEAD b37557b2
- OBSERVED: `pm-web-e2e/tests/` — present at HEAD b37557b2
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WebCommand.java` — present at HEAD b37557b2
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/` — present at HEAD b37557b2
- OBSERVED: `pm-operator/src/test/resources/transcript/` — present at HEAD b37557b2
- OBSERVED: `AGENTS.md` — present at HEAD b37557b2
- OBSERVED: `README.md` — present at HEAD b37557b2
- OBSERVED: `doc/design/terminal-client/27-tui-testing.adoc` — present at HEAD b37557b2

## Dependencies and Sequencing

- Depends on: none
- Overlaps with: `web-text-guard-report` (PLAN-01, `pm-web-e2e/tests/`), `mock-scenario-states` (PLAN-03,
  `MockRuntimeEngine.java`, `pm-mock-runtime/README.adoc`) and `cli-summary-line-and-start-script` (PLAN-05,
  `pm-operator`), all three in flight at staging; whichever lands later rebases.
- Gates: every staged plan whose spec names a port block (Standing Constraints → "Ports of this plan")
  passes its ports only once this plan has landed; the orchestrator emits those plans after it.

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-76-shared-port-assignment.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests. It creates and edits NO
file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message — the orchestrator owns
every other ledger write — and reports its outcome through its PR and its inbox message. The inbox
exception's qualifiers and the sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
