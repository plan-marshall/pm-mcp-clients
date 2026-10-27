envelope_version=1
sender_type=plan
sender_id=rb-76-shared-port-assignment
epic=ui-improvements
kind=landing
created=2026-10-27T15:40:48Z

```landing-facts
schema=landing-facts/1
plan_id=rb-76-shared-port-assignment
epic=ui-improvements
pr=#243
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=4
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.2021
executor_version=0.1.2017
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```
guard: none on this harness (unenforced)

## 1. Deliverables
- **Deliverable 1:** Met. Mock: the caller's port, reported truthfully.
  - Done-when: `MockRuntimeTest#ephemeralWebPortReportedTruthfully`, order: test-first, failing run commit `5b76f1f8`, passed at commit `de88c1a`.
  - Done-when (scripts): `StartStopScriptsTest` (occupied assigned port failure, PID isolation), order: test-first, failing run commit `5b76f1f8`, passed at commit `de88c1a`.
- **Deliverable 2:** Met. Web app and browser tests: ports from the caller.
  - Done-when (ports unit): `ports.test.ts`, order: test-first, failing run commit `de88c1a`, passed at commit `24280286`.
  - Done-when (port guard): `guard-ports.js` / `port-guard.spec.js`, order: test-first, failing run commit `de88c1a` (34 violations), passed at commit `24280286`.
  - Done-when (concurrency proof): Ran 254 E2E tests on ports 20760/20761 while main checkout mock (7420) and web app (5173) were running; main processes remained healthy (HTTP 200).
- **Deliverable 3:** Met. Terminal client and Java tests: no fixed port.
  - Done-when: `PmOperatorTest$Web#pairFallsBackToDefaultPortWhenRuntimeCarriesNoUrl` and `pairPrintsUrlReturnedByRuntimeOnEphemeralPort`, order: test-first, failing run commit `24280286`, passed at commit `4733e92d`.
  - Done-when (IT concurrency): `ScreenCaptureIT` binds ephemeral port 0; ran operator integration tests while port 7420 was held by background process; all 80 tests passed at commit `4733e92d`.
  - Terminal check captures recorded in `.plan/temp/terminal/76-shared-port-assignment/` (before and after).
- **Deliverable 4:** Met. The port convention, documented.
  - Done-when: Documented in `AGENTS.md`, `README.md`, `pm-mock-runtime/README.adoc`, `doc/design/terminal-client/27-tui-testing.adoc`, order: none, commit `afaab088`.

Per-head test runs against pushed heads:
- Head `35b61897`: pre-commit build gate (`verify -Ppre-commit`) passed cleanly across all 4 projects; GitHub Actions CI run `40528828029` passed all checks.
- Merge commit: `15b7780ccf53daa4d8646adf318d785c98ff907e`.

## 2. Personas and Skills
- **Deliverable 1:** `plan-marshall:persona-implementer` with `pm-dev-java:java-core`, `pm-dev-java:java-lombok`, `pm-dev-java:junit-core`.
- **Deliverable 2:** `plan-marshall:persona-implementer` with `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`.
- **Deliverable 3:** `plan-marshall:persona-integration-tester` with `pm-dev-java:junit-integration`, `pm-dev-java:junit-core`.
- **Deliverable 4:** `plan-marshall:persona-documenter` with `pm-documents:ref-asciidoc`, `pm-documents:ref-documentation`.
- **Self-Review:** `plan-marshall:persona-code-reviewer` and `plan-marshall:persona-auditor`.

## 3. Review Cycle
- **CodeRabbit (`@coderabbitai`):**
  - PR #241: Closed unmerged per reviewer quota policy (§ 6.7 step 2) after rate-limit wait.
  - PR #243: Triggered review. CodeRabbit completed a formal review with 1 finding on `StartStopScriptsTest.java:72` (avoid pipe deadlock when draining `start.sh` output in tests).
  - Triage disposition: Fixed in commit `cffb5d14` (async drainage via `CompletableFuture` with `redirectErrorStream(true)` and port range validation in `MockRuntimeCli`).
  - Thread `PRRT_kwDOVBOtss6wvEJ6` replied and resolved. Re-triggered after merge commit `35b61897`; CodeRabbit responded with rate-limit notice. Standing review authorization criteria satisfied.
- **Sourcery (`@sourcery-ai`):**
  - Quota limit reached on account; rate limit notice received. Re-triggered on each push; silence/refusal recorded. Standing review authorization applies.
- **SonarCloud:**
  - Automated analysis passed on commit `35b61897`: 0 open new-code issues, 90.3% new code coverage, Quality Gate passed.

## 4. Rule Checks
- All repository code standards (CLAUDE.md, AGENTS.md) maintained.
- Java 25 standards, modern records, null safety, and error handling preserved.
- Port convention followed: mock web port `20000 + 10 * NN` (20760), app port `20000 + 10 * NN + 1` (20761).
- PID-isolated start/stop scripts ensure no foreign processes are terminated.
- Co-Authored-By commit trailer included on all commits and merges.
- Merge gate: clean pass under standing review authorization.
- Guard state: `guard: none on this harness (unenforced)`.

## 5. Changes
- Deliverable 1 (`c9fa6c4a`): Mock runtime bound live port reporting, assigned port conflict failure, worktree-scoped PID termination.
- Deliverable 2 (`24280286`): Dynamic web port and mock port resolution in web app, Vite proxy configuration, centralized test ports and forbidden literal guard in E2E tests.
- Deliverable 3 (`4733e92d`): Operator WebCommand live runtime URL resolution, ephemeral port binding in ScreenCaptureIT.
- Deliverable 4 (`afaab088`): Port configuration and isolation documented in AGENTS.md, README.md, mock runtime README, and design docs.
- Review Fix (`cffb5d14`): Asynchronous child process stream drainage in StartStopScriptsTest, CLI and env port range validation in MockRuntimeCli.
- Merge Base (`35b61897`): Merged origin/main cleanly into feature branch.
