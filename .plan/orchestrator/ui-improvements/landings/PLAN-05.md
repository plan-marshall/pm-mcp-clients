# Landing Analysis: PLAN-05 — Terminal: Summary Line After `q`, PID File Only for `-d`

epic: ui-improvements
workstream: WS-05
pr: #238 (https://github.com/plan-marshall/pm-mcp-clients/pull/238), merged as `a1a72483`

> Landing record for one shipped plan. Written by `analyze` (inbox scan, message
> `rb-05-cli-summary-line-and-start-script-001.md`). The message was checked against the PR, the merge
> diff `394c627b..a1a72483` and the code on `main`.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Summary line | shipped-as-specified | `OverviewFullScreenApp.java` (+132/−), `ConsoleNoticeBuffer.java`, the `overview.*` catalogue (de/en), `SummaryLineIT.java` (+156), capture set `screen-captures/05-cli-summary-line-and-start-script.txt` |
| D2 PID file only for `-d` | shipped-modified | `pm-operator/start.sh`, `stop.sh`, `StartScriptTest.java` (+185). Its terminal-check clause (D2.2) could not be met as written: see Follow-Ups. The browser regression and the Sonar read are recorded |

`deliverables_done=1` is the lane's own grading. The code of both deliverables is on `main`. Only the D2.2 clause, a spec defect, is unmet.

Beyond the declared surface: `pm-operator/src/test/java/.../transcript/PtySession.java` and `ScreenCaptureIT.java` (the reader-race fix the review asked for), `OverviewCatalogueTest.java` in `pm-api`, and `doc/design/terminal-client/04-console.adoc`/`27-tui-testing.adoc`. None of these is declared by PLAN-01 or PLAN-03. PLAN-76 declares `doc/design/terminal-client/27-tui-testing.adoc` and the `pm-operator` test tree, so PLAN-76 rebases over these changes.

## Metrics and Anomalies

- Tokens: `unknown` (opencode measures none). `plugin_version` is `0.1.2017` this time: read at landing time, the first opencode landing that carries it.
- Duration: from the failing run at about 06:32Z to the landing message at 12:02Z on 2026-10-27.
- Model: `deepseek-v4.1-flash/high` on opencode; executor 0.1.2017.
- Anomalies:
  - `executor-skew` `could_not_check` (operator: proceed). Guard `none`.
  - Build server: the routed build failed again because the daemon invoked a bare `mvn` (job log `8d16de87…`). This is the third recurrence of that watch. Every build ran in-process.
  - One gate timeout at 900 s while `npm run lint` ran; re-run at 1800 s, green.
  - Port 5173 was reported in use (a Chrome helper socket), so the run started the app on 5273. This is the port problem PLAN-76 addresses.
  - Shell reads, `rm -rf` on temp captures and one stray executor call are disclosed by the run.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `e119f1cf` (5 findings, all fixed in `353813a`, follow-up fixed in `f016c11a`). It was then rate limited and did not see the final three commits (`abc15f57`, `f016c11a`, `c257bcca`). The operator chose to merge on the standing authorization. Sourcery approved `353813a`.
- CI/merge: one transient `sonar-build` failure (non-main branch access), green on re-run. Merged through the merge queue (merge_group run 40499192678). No conflicts.
- Sonar: `open_issue_count=0`, `open_issue_rules=none` at `c257bcca`, after `java:S1192` and `java:S3776` were fixed in `abc15f57`.

## Reconciliation Actions

- [x] row `status` → `shipped`
- [x] row `pr` stamped — `238`
- [x] row `landing` stamped — `landings/PLAN-05.md`
- [x] row `plan_marshall_plan_id` stamped — `05-cli-summary-line-and-start-script`
- [x] epic.md: Open Defect recurrence (only `total_tokens` missing now); build-server watch recurrence; new watch on the terminal-check targets of the staged terminal specs
- [x] resume anchor updated, `queue-view.md` regenerated

## Follow-Ups

- **Spec defect, D2.2.** The generator gave every output-changing terminal deliverable the plan's one terminal-check target. D2 changes `start.sh`, which no screen capture can show. The same pattern can repeat in the staged terminal specs: recorded as a watch, to check each terminal target against its deliverable before emitting the spec.
- The web defects the browser regression brushed against are owned already: "10 d ago" untranslated in the German overview (PLAN-16), and the icon glyphs on the header buttons (PLAN-19).
- `stop.sh`'s fall-through by process name has no test, because a test could stop another session's operator. PLAN-76's "stop only what this worktree started" rule covers the mock and the app, not the operator. Noted, no new plan.
