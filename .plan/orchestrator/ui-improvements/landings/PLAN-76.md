# Landing Analysis: PLAN-76 — Shared: Ports Passed by the Caller, One Port Block per Plan

epic: ui-improvements
workstream: WS-01
pr: #243 (https://github.com/plan-marshall/pm-mcp-clients/pull/243), merged as `15b7780c`. PR #241 was closed unmerged: the lane re-opened the change after waiting out the reviewer quota.

> Landing record for one shipped plan. Written by `analyze` (inbox scan, message
> `rb-76-shared-port-assignment-001.md`). The message was checked against the PR, the merge diff
> `9c1f259c..15b7780c`, the code on `main` and the lane's progress file
> `.plan/temp/rb-76-shared-port-assignment/progress.md`.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Mock: the caller's port, reported truthfully | shipped-as-specified (browser clause unverified) | `MockRuntimeEngine.java` reports `webPort` (the live port) wherever the constant stood; `7420` stays only as the fallback when no listener is bound. `MockRuntimeCli` validates the port and reads `PM_MOCK_WEB_PORT`. `start.sh` refuses an occupied assigned port; `stop.sh` lost its port-kill branch (−37). Tests: `MockRuntimeTest` (+83), `MockRuntimeCliTest`, `StartStopScriptsTest` (+204). **The browser-check clause is not evidenced**: there is no `76-*` file under `.plan/temp/playwright-mcp/`, and neither the message nor the progress file mentions a Playwright run. |
| D2 Web app and browser tests | shipped-as-specified | `pm-web-app/src/lib/ports.ts` (+41) with `ports.test.ts` (+34) resolves both variables and rejects invalid values. `vite.config.ts`, `start.sh` and `stop.sh` use it. `pm-web-e2e/tests/helpers/ports.js` is used by all 19 specs; `port-guard.spec.js` and `scripts/guard-ports.js` enforce it, and `git grep` finds no literal outside the guard itself and `ports.js`. `pom.xml` passes the properties. The concurrency proof is recorded: 254 tests on 20760/20761 while the main checkout's mock and app on 7420/5173 stayed alive. The default run passed too |
| D3 Terminal client and Java tests | shipped-as-specified | `WebCommand.java` prints the runtime's URL and falls back to `7420` only without one. `PmOperatorTest` (+58) has both cases, the fallback found failing first. `ScreenCaptureIT` binds `webPort(0)`. `pm-operator` ITs (80) passed with `7420` held. Screen captures under `.plan/temp/terminal/76-shared-port-assignment/{before,after}` |
| D4 Port convention documented | shipped-as-specified | `AGENTS.md` § Ports (the formula, the defaults, the reserved rest of the block, the variables), `README.md`, `pm-mock-runtime/README.adoc`, `27-tui-testing.adoc`. Sonar: `0` |

Beyond the declared surface: `pm-web-app/src/lib/views/CleanupView.svelte` (two `--color-success` → `--color-success-text`), `pm-web-e2e/package.json` (a `test:ports-guard` script) and `pm-mock-runtime/.../MockRuntime.java`. The `CleanupView` change is presumably what lets the concurrency run's contrast case pass. It touches the watch on the failing light-narrow contrast case. It is not stated in the message.

## Metrics and Anomalies

- Tokens: `unknown`; **model and effort `unknown` too**. The run was on the `antigravity` harness, not opencode, the first landing from that harness. The landing check reports `total_tokens`, `model` and `effort` missing.
- Versions: `plugin_version` `0.1.2021`, `executor_version` `0.1.2017`. The skew question in the progress file names an executor version `0.1.0`, which disagrees with the landing facts.
- Duration: from the emit at about 09:45Z to the merge at 15:38Z on 2026-10-27.
- Anomalies:
  - The landing message is thinner than the lane contract asks: no operator-question list, no build-server section, no rule-break account beyond "maintained", no browser evidence for D1.
  - Guard `none`.

## Routing and Merge Behavior

- Review: on #243, CodeRabbit gave one finding (pipe deadlock in `StartStopScriptsTest`), fixed in `cffb5d14` and resolved. It was then rate limited, and the standing authorization was applied. Sourcery was quota-limited throughout.
- CI/merge: CI run 40528828029 green. Merged with `origin/main` merged in (`35b61897`), so no conflict with PLAN-01 or PLAN-05, which had landed meanwhile and touch `pm-web-e2e/tests/` and `pm-operator`.
- Sonar: `open_issue_count=0`, `open_issue_rules=none` at `35b61897`; new-code coverage 90.3 %.

## Reconciliation Actions

- [x] row `status` → `shipped`
- [x] row `pr` stamped — `243`
- [x] row `landing` stamped — `landings/PLAN-76.md`
- [x] row `plan_marshall_plan_id` stamped — `76-shared-port-assignment`
- [x] epic.md: the port gate is lifted; Open Defect recurrence (now also `model`/`effort` on antigravity); watches for the unevidenced D1 browser check and the first antigravity landing; the contrast watch notes the `CleanupView` token change
- [x] resume anchor updated, `queue-view.md` regenerated

## Follow-Ups

- The D1 browser check (system view at `127.0.0.1:20760` while a second mock runs on `7420`) is unverified. It is now a check for PLAN-18 (`verify-shared-foundation`), recorded as a watch, not reopened: the behaviour is covered by `MockRuntimeTest` and the concurrency run.
- PLAN-03 is still running on the default ports. It rebases over these changes (`MockRuntimeEngine.java`, `pm-mock-runtime/README.adoc`).
