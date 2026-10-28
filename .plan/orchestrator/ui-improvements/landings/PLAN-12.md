# Landing Analysis: PLAN-12 — Web: Design Tokens v2 and the Inter Typeface

epic: ui-improvements
workstream: WS-02
pr: #247 (https://github.com/plan-marshall/pm-mcp-clients/pull/247), merged as `09a0725f`

> Landing record for one shipped plan. Written by `analyze` (inbox scan, message
> `rb-12-web-design-tokens-v2-001.md`). The message was checked against the PR and the merge diff
> `f2379809..09a0725f`.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Operator decisions recorded | shipped-as-specified | `doc/design/web-app/README.adoc` § Decisions of the Operator (2026-10-25): Inter self-hosted, own SVG icons, axe-core, stylelint |
| D2 Tokens v2 | shipped-as-specified | `pm-web-app/src/styles/tokens.css` (rewritten, +/−461), `base.css`, `tokens.test.ts` (+552): every token of VD-P-02/03/04 in both themes plus the aliases, proven by a test that failed first (59 failed). Browser matrix: 8 combinations × 10 routes, before and after, under `.plan/temp/playwright-mcp/12-web-design-tokens-v2-*` |
| D3 Inter, self-hosted | shipped-as-specified | `static/fonts/InterVariable.woff2` (352 kB) with `OFL.txt`, `fonts.test.ts`, `pm-web-e2e/tests/fonts.spec.js` (+71): no external font request, the served bytes are WOFF2 |
| D4 Contrast audit reset | shipped-as-specified | `theme-readability.spec.js` (+39/−), `theme-readability.adoc`. E2e with the plan's ports (`20120`/`20121`): 295 passed, twice. Sonar `0` at the merge head |

Beyond the declared surface: `pm-web-e2e/tests/fonts.spec.js` (new) and `onboarding.spec.js` (one line). No in-flight plan declares either. The font file is the self-hosted Inter that the operator approved on 2026-10-25, under the SIL Open Font License (`OFL.txt`): an asset, not a dependency.

## Metrics and Anomalies

- Tokens: `unknown`; `plugin_version` `unknown`. Harness opencode, `deepseek-v4.1-flash/high`.
- Duration: from about 16:20Z on 2026-10-27 to the merge at 06:25Z on 2026-10-28.
- Anomalies:
  - **Build server regressed for this run**: every gate build fell back to in-process (`maven_not_on_daemon_path`: the daemon shells a bare `mvn`). PLAN-06 and PLAN-07, running over the same hours, built on the daemon. One in-process `verify` also hit the client's learned 787 s bound.
  - **The Playwright MCP browser was shared with a parallel session.** Captures whose tab another session had taken over were retried after re-checking port, theme and font. One e2e failure (`reports.spec`) came from a tab this run had left open on the app port. The port blocks separate the servers, not the browser.
  - Two Maven runs overlapped once in the worktree and gave a false failure; re-run alone, green.
  - **The Sonar credential cannot transition an issue** (HTTP 400). Every dismissal the lane would transmit is therefore a blocker; this run fixed its four `css:S9423` findings in code instead.
  - The executor skew was cleared by a parallel plan's regeneration (operator: "continue").
  - Shell reads and one mistyped executor call are disclosed by the run.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed three heads (3 findings, all fixed and resolved). It was then quota-refused or silent, and the standing authorization was applied by operator choice. Sourcery was out of budget.
- Self-review: three rounds plus an operator-authorized fourth, ending with nothing open.
- CI/merge: green. Merged through the merge queue with `main` merged in. No conflicts with PLAN-06, which landed hours earlier and touched `common.*.properties`, not the styles.
- Sonar: `0` confirmed at `b014b979`.

## Reconciliation Actions

- [x] row `status` → `shipped`
- [x] row `pr` stamped — `247`
- [x] row `landing` stamped — `landings/PLAN-12.md`
- [x] row `plan_marshall_plan_id` stamped — `12-web-design-tokens-v2`
- [x] epic.md: Open Defect recurrence; build-server watch (the bare-`mvn` fallback is back for this run); new watches on the shared Playwright browser and on the Sonar transition credential
- [x] resume anchor updated, `queue-view.md` regenerated

## Follow-Ups

- Emoji glyphs in the header rail: PLAN-19 (`web-ui-icon-set`), which is emitted with this analysis.
- Fifteen transitions with hard-coded durations: the motion tokens are applied view by view under VD-S-06, a standing constraint of every view plan.
- `/plans/{id}` scrolls horizontally at 390 px (498 px measured at the base): PLAN-55 (`web-plan-page`). Recorded as a watch, to check when PLAN-55 is emitted.
- Sonar transition credential: recorded as a watch, because the Sonar clause of every spec allows a dismissal the lane cannot transmit.
