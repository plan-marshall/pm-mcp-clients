# PLAN-91: Terminal: Ctrl+C Status in Every Handler and the Transcript Goldens

epic: ui-improvements
workstream: WS-04

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Staged 2026-11-01 from the open findings of the landing analyses. One pull request on purpose: each pull request costs a
> CodeRabbit review (quota-limited), so the open aspects of one area are collected here. Keep the pull request under 100 files
> (CodeRabbit refuses more); if it would exceed that, split it by deliverable and say so in the progress record.

## Objective

Finish the interrupt work of `PLAN-17` and clear the red baseline of `pm-operator`: Ctrl+C still ends with exit `0` in eight handlers that PLAN-17 reported and did not change, and three transcript goldens fail on an untouched `main`. One pull request, because each pull request costs a CodeRabbit review.

## Deliverables

1. **Ctrl+C leaves with the interrupt status in every handler**
   - What: `PLAN-17` gave the prompts an interrupt (`ChoicePrompt.InterruptException`, `StartPrompt.EXIT_INTERRUPTED`, 130) and used it in the runtime acts. The run reported that Ctrl+C still ends with `0` in eight other handlers: `WorkActionDialogue`, `ReviewDialogue`, `HoldDialogue`, `ProjectCommand`, `PlanCommand`, `OnboardCommand` and two paths of `ActionSupport`. Each one reproduced is made to leave through the same mechanism, with nothing sent to the daemon after the interrupt. A handler that does not reproduce is reported and left alone.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - One test per reproduced handler (the PTY harness of `cli-pty-dialogue-harness` where a prompt is involved, a unit test where not) sends the interrupt at a prompt and asserts exit `130` and no call to the daemon; each fails before the change (evidence order: `test-first`)
     - Terminal check (screen capture) shows this deliverable's change as specified on: `fully-onboarded`: `plan` hold and review dialogues and `project` setup, each interrupted at its first prompt; before and after captures recorded (evidence order: `none`)
2. **The three transcript goldens match**
   - What: `status.transcript`, `start-plan.transcript` and `start-plan-no-input.transcript` (`pm-operator/src/test/resources/transcript/`) mismatch in `pm-operator verify -Pintegration-tests` on an untouched `main`, per the run of `PLAN-87`. Re-measure on the current `main` first. Each mismatch is diagnosed as the code wrong or the golden stale (`PLAN-82` and `PLAN-09` changed the status and start texts), and the right side is fixed; a golden is never regenerated to match without the design sentence that decides it.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-java:junit-integration, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - The failing names at the base are recorded first with the log path; each is classified as code wrong or golden stale with the deciding design sentence (evidence order: `test-first`)
     - `./mvnw -pl pm-operator -am verify -Pintegration-tests` reports no transcript failure (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

3. **The tests PLAN-26 left out**
   - What: `PLAN-26` (merged `be4995e5`) shipped code whose tests are missing, found by reading its merge (`landings/PLAN-26.md`, layer 3). (a) The full-screen system view reads `/api/v1/status` on entry and on refresh and keeps `systemBannerFromEvent` in `OverviewFullScreenApp` so that a status read replaces a line the status wrote, leaves a progress event's line alone and clears it once the runtime runs again: no unit test drives it (`SystemLifecycleLineTest` drives only the printed `system` verb). (b) `WebCommand` rename, revoke and listener-off and `ConfigCommand` set-elines now return exit `1` on a refused request: no test fails on a refusal (`WebDevicesCommandTest` covers success). (c) `ReadVerbExitCodesTest` answers HTTP 500 only; add a 401 and a 404 case. (d) `SystemLifecycleLineTest`, `SystemViewConsistencyTest` and `ReadVerbExitCodesTest` use hand-written snapshot values and ids where the standing constraint says `Generators` with `@EnableGeneratorController` (two carry the annotation and never call `Generators`; the third carries neither): input data moves to `Generators`, expected catalogue text and wire names stay literal. The first-line text (`draining · 2 left`) stays as built unless the operator has restored the specified line by then; this plan does not decide it.
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - One test per case of (a) (replace, leave, clear, a failed status read leaves the line) and of (b) (four refusals), each fails when the code it covers is reverted (evidence order: `mutation-after`: the failing output comes from reverting the committed code)
     - The three test classes of (d) draw their input from `Generators` and every `@EnableGeneratorController` is used (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-operator -am verify -Pintegration-tests`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- CUI standards (`CLAUDE.md`, `PLAN-77`): a test that takes input data draws it from `Generators` with `@EnableGeneratorController` (the expected value of a contract stays literal); a log call at INFO or above goes through a `LogRecord` of the module's own `LogMessages` class (one per module that logs, never one for the repository) with a `LogAsserts` test; `@NullMarked` applies in a module that declares JSpecify (`pm-operator`, `pm-mock-runtime`); until `PLAN-77` has landed the test-scope CUI dependencies are missing in `pm-api`, `pm-exec`, `pm-relay` and `pm-mock-runtime`, and a test there follows plain JUnit and reports the departure, never adds the dependency itself.
- Ports of this plan: mock web port `20910`, app port `20911` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Java 25, Lombok, records; no generic `Exception`/`RuntimeException`; JUnit 5 only, no Mockito (CLAUDE.md).
- Every new operator text goes through `MessageCatalogue` into `operator.en.properties` and `operator.de.properties`.
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20910"`), then `PM_MOCK_WEB_PORT=20910 ./pm-web-app/start.sh -p 20911` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20910/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- Terminal check of the rendered output through the screen capture harness of `cli-pty-dialogue-harness` (`ScreenCaptureIT`, command line in `doc/design/terminal-client/27-tui-testing.adoc`): write this plan's captures to `pm-operator/src/test/resources/screen-captures/{plan}.txt`, run the set BEFORE the change and copy `pm-operator/target/screens/{plan}/` to `.plan/temp/terminal/{plan}/before/`, then run it AFTER and copy to `.../after/`. Capture at 120 × 30 and 80 × 24 in `en_US.UTF-8` and `de_DE.UTF-8` unless the target names other sizes or environments. Read every `.txt` and `.attrs.txt` with the file tools and compare them with the deliverable; the screen text shows no raw id, link name, state id, `-` placeholder, ISO time or HTTP status, and no text of a previous level. Never run `pm-operator` in the agent's own shell: the operator gate refuses a coding agent by design (`PM-SEC-3`) and is never worked around; only the harness, which clears the environment like every PTY test, runs it. If the harness is not available, the terminal Done-when clauses are NOT met — a blocker, never skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: `PLAN-17` added `ChoicePrompt.InterruptException` and `StartPrompt.EXIT_INTERRUPTED` (130) — read in `landings/PLAN-17.md` and the merge diff of `66c1495a`
- HYPOTHESIS: Ctrl+C ends with exit `0` in `WorkActionDialogue`, `ReviewDialogue`, `HoldDialogue`, `ProjectCommand`, `PlanCommand`, `OnboardCommand` and two paths of `ActionSupport` — this is the PLAN-17 run's account, not read by the orchestrator; confirm/refute per handler at `pm-operator/src/main/java/de/planmarshall/operator/` (verify-at-outline)
- OBSERVED: `PmConsole`, `EntryFlow`, `AnswerCommand`, `CredentialsCommand` and `EnrolHarnessCommand` already catch `UserInterruptException` — read by `git grep` at `1ce32d80`; how each maps it to an exit status is to be read before reusing the pattern
- HYPOTHESIS: the three goldens fail on `main` because `PLAN-82` (`status`) and `PLAN-09` (start texts) changed texts the goldens still carry — confirm/refute at `StatusDialogueTranscriptTest` and `ConsoleDialogueTranscriptTest` (verify-at-outline)
- Source: `landings/PLAN-17.md` (follow-ups), `landings/PLAN-87.md` (the three goldens), Watches of `epic.md`.
- Not in this plan, the operator's call: whether `drain` asks for confirmation as the runtime acts do (`DrainCommand` was declared by `PLAN-17` and untouched).
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WorkActionDialogue.java` — present at HEAD 1ce32d80
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ReviewDialogue.java` — present at HEAD 1ce32d80
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/HoldDialogue.java` — present at HEAD 1ce32d80
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ProjectCommand.java` — present at HEAD 1ce32d80
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java` — present at HEAD 1ce32d80
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OnboardCommand.java` — present at HEAD 1ce32d80
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ActionSupport.java` — present at HEAD 1ce32d80
- OBSERVED: `pm-operator/src/test/resources/transcript/status.transcript` — present at HEAD 1ce32d80
- OBSERVED: `pm-operator/src/test/resources/transcript/start-plan.transcript` — present at HEAD 1ce32d80
- OBSERVED: `pm-operator/src/test/resources/transcript/start-plan-no-input.transcript` — present at HEAD 1ce32d80
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/StatusDialogueTranscriptTest.java` — present at HEAD 1ce32d80
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/ConsoleDialogueTranscriptTest.java` — present at HEAD 1ce32d80
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFullScreenApp.java` — present at HEAD be4995e5
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/SystemLifecycleLineTest.java` — present at HEAD be4995e5
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/SystemViewConsistencyTest.java` — present at HEAD be4995e5
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ReadVerbExitCodesTest.java` — present at HEAD be4995e5
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/WebDevicesCommandTest.java` — present at HEAD be4995e5

## Dependencies and Sequencing

- Depends on: `PLAN-17` (shipped), `PLAN-02` (shipped, the PTY harness)
- Overlaps with: `PLAN-26` (running; `status` and the system texts, check `status.transcript` at the outline), `PLAN-21`, `PLAN-23`, `PLAN-30`, `PLAN-36`, `PLAN-42` (name `PlanCommand`, the operator catalogue pair or `ChoicePrompt`)
- Adjacent to: `DrainCommand` (untouched; the operator decides whether it asks)

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-91-cli-interrupt-and-transcripts.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
