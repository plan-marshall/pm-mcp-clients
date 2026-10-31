# Landing Analysis: PLAN-80 — Web: Frontend Standards Conformance

epic: ui-improvements
workstream: WS-02
pr: #281 (merge commit `bf8ddb31557192b50325ee58b71fb2f517ab86fb`, squash merge)

> Landing record for one shipped plan. Source: inbox message `rb-80-web-standards-conformance-001.md` (runbook lane,
> harness `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2036`), analysed 2026-10-31 against the merged PR,
> the CI checks, the merge diff and the Sonar gate. The per-head build logs and the three self-review rounds are the run's
> account and are not re-run here.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Domain configuration and the rule (`javascript` domain matches `.ts` and `.svelte`; `pm-dev-frontend-cui` configured; a Web Standards paragraph) | shipped-as-specified, one departure | `.plan/marshal.json` changed (+19/−6 lines): `skill-domains get --domain javascript` lists `**/*.ts` and `**/*.svelte`; the additive domain is named `javascript-cui` and carries the bundle `pm-dev-frontend-cui` with the same six globs. The spec said "a Web standards paragraph under § Code Standards"; the re-grounded wording (a section of its own) was followed: `CLAUDE.md` § Web Standards stands directly after § Code Standards (+20 lines) and its last bullet names the `src/` Svelte layout as standing in for the standard project types. The executor was regenerated (`unchanged`, 334 scripts). The run undid a reseeded `default:verify:coverage` step through `manage-config` after a self-review finding. |
| D2 Scripts and Node version | met | `pm-web-e2e/package.json` gained `quality` and `test:ci-strict` (the latter runs `test:ports-guard`; the departure is written next to it); both ran green. Node: `frontend.node.version` = `v22.23.1`, `engines.node` `>=22.17` (`pm-web-app`) and `>=22` (`pm-web-e2e`) agree, so no `package.json` change in the app. |
| D3 Test hygiene | met, one hypothesis refuted | No `setTimeout` in the unit tests, no `waitForTimeout` in `pm-web-e2e/tests`; `npm run test:ci` of `pm-web-app` passes with 86 files and 952 tests, and the diff adds or removes no `it(` or `test(` line. Both lints are silent. The `any` hypothesis was REFUTED: `Chip.test.ts` has none, while `OverviewView.svelte` held 28 real `as any` casts for snake_case and camelCase fallbacks; they were typed (`CamelCaseAliases`, `withAliases()`, `planIdOf()`, `epicIdOf()`), not annotated. |
| D3 open-issue read | met | `open_issue_count=0`, `open_issue_rules=none`, analysis revision = head revision; corroborated below. |

`deliverables_done=3` of 3 is consistent with the diff. All three deliverables carry evidence order `none`, so no
failing-run commit is reported, as the spec states.

**Surface delta** (computed here from `git diff bf8ddb31^1 bf8ddb31`, because `inbox landing-check` answers
`unmeasured`): declared 10 entries, realized 11 files. Eight declared entries were changed (`.plan/marshal.json`,
`CLAUDE.md`, `pm-web-e2e/package.json`, `advisoryTitle.test.ts`, `EpicView.test.ts`, `OverviewView.svelte`,
`helpers/textGuard.js`, `helpers/contrast.js`). Two declared entries are untouched: `pm-web-app/package.json` (the Node
versions agree) and `Chip.test.ts` (no `any` there). Three files are undeclared: `chrome.spec.js`, `overview.spec.js` and
`theme-readability.spec.js` (the helper changes reached into the specs that call them).

## Metrics and Anomalies

- Tokens: `unknown` (not measured on this harness, recorded as unobserved, not as zero). `plugin_version` `unknown`.
  `executor_version` `0.1.2036`.
- Anomalies:
  - **Executor skew at the start, answered by the operator:** the generated executor was `0.1.2032` and the lane text
    `0.1.2036` (verdict `skew`); the operator answered in free text "already regenerated" and the re-check read `match`.
    The cause is known: the plugin was updated to `0.1.2036` after the lanes of PLAN-77, 80, 82 and 86 were prepared on
    `0.1.2032`; the other three lanes may report the same.
  - **Config verbs run with the shell in the worktree:** `skill-domains configure`, `set-inclusion`,
    `phase-5-execute remove-step` and `generate_executor generate --force` ran from the plan's worktree, because
    `.plan/marshal.json` is a tracked file the deliverable ships; running them in the main checkout would have kept the
    change out of the PR. The run records this as a known deviation.
  - **Self-review, three rounds** (16, then 2, then none; `self-review: clear`): real findings fixed in `dfe1f44` and
    `e1d2892` (a false claim in `CLAUDE.md` that the Maven build runs all five npm scripts, a sticky quiet-frame flag, a
    contrast wait that could hang on infinite animations, an unbounded text-guard wait, the reseeded coverage step).
  - **CI:** `build / quarkus-alignment` failed once on a Maven Central `HTTP 429`, infrastructure, and passed on
    `ci checks rerun`.
  - **Build routing:** every gate run used `--execution-mode in_process`, because the routed job could not spawn `mvn`
    (`exit_code -1`, empty log, three submissions). PLAN-15, 19, 22, 14 and 20 reported the same failure (open
    Build-server watch).
  - **Shell file operations by the run itself:** `ls`, `grep` and `cat`/`tail` over skill directories, POMs and build
    logs where the rules name the file tools; the repository searches used the grep tool. Skill texts under
    `~/.config/opencode/skills/` and the daemon's job logs were read; no credential file was. Guard `none` on this
    harness, so the account is the run's own.
  - `spec_source=ledger_worktree`, `spec_differ=true`; the run implemented the ledger copy (the "Web Standards section of
    its own" wording).

## Routing and Merge Behavior

- Review: CodeRabbit (required, quota-limited) reviewed head `e1d2892` (three findings: a monotonic clock for the quiet
  window, polling on a timer instead of animation frames, naming `test:ports-guard` in a note), all fixed in `640e4c9`
  with both inline threads answered and resolved. Its re-triggers on the later heads were refused on quota. The merge
  head `c57576aa` therefore differs from the reviewed head by those three fixes and the `origin/main` merge; the merge ran
  under the standing review authorization. Sourcery (optional) refused on quota.
- CI/merge: `ci checks status --pr-number 281` is `success`, 20 checks. `origin/main` carries `bf8ddb31`.
- Sonar: `sonar_rest gate-status --project plan-marshall_pm-mcp-clients --pr 281` answers `gate_status: OK`; the five
  conditions it lists are OK (no new-coverage condition appears: the change adds no measured lines). A confirmed `0`,
  corroborated.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-80 --status shipped`
- [x] row `pr` stamped `281`
- [x] row `landing` stamped `landings/PLAN-80.md`
- [x] row `plan_marshall_plan_id` stamped `80-web-standards-conformance` (the sender's derived name)
- [x] epic.md narrative reconciled against the queue rows
- [x] defects and watches opened, updated or retired (see Follow-Ups)
- [x] re-grounded notes added to PLAN-17, PLAN-70 and PLAN-85
- [x] resume anchor updated in `resume_anchor.md`
- [x] `queue-view.md` regenerated
- [x] inbox message `rb-80-web-standards-conformance-001.md` archived after this record

## Follow-Ups

- **Incomplete landing facts** (`total_tokens`, `plugin_version` unknown): recurrence on the Open Defect.
- **The dark contrast matrix may audit the light theme** (run's residue, not verified here): `theme-readability.spec.js`
  calls `setTheme` before `page.goto`, and `app.html` re-derives `data-theme` from a `localStorage.pm_theme` that nothing
  writes. If so, the "dark" results of the audit are not dark results. It concerns `PLAN-85` (which changes the helper's
  storage and the theme control) and `PLAN-70` (the light-theme pass): both specs get a note that the audit must assert
  the theme it claims (`data-theme` equals the requested theme) before its verdict counts. Recorded as a Watch.
- **`format:check` fails on 42 files of `pm-web-app`** at HEAD (pre-existing; the Maven build does not run it).
  `OverviewView.svelte` is one of them. No plan owns it. Recorded as a Watch for the operator: a formatting pass would
  touch most views and collide with the view plans, so it is best taken by the last web plan (`PLAN-75` territory) or not
  at all.
- **Standards still left to the operator:** `stylelint` (approved for `PLAN-72`), `dependency-cruiser` and a sanitizer stay
  unadded, as the spec required.
- **Specs whose declared files this merge changed** (re-read at HEAD before emit): `OverviewView.svelte` (97 lines changed:
  PLAN-09, 52, 83), `helpers/contrast.js` and `theme-readability.spec.js` (PLAN-70, 85), `helpers/textGuard.js` (PLAN-73),
  `chrome.spec.js` (PLAN-45, 46, 85), `overview.spec.js` (PLAN-52).
- **Residue (accepted):** `planIdOf` and `epicIdOf` share an unreachable `|| ''` fallback; the text guard's `.catch`
  also swallows a destroyed execution context (the next `countChanges` surfaces it).
- **Branch and worktree:** no `feature/80*` branch remains on the remote (`git ls-remote`, checked after this record). The
  lane's local worktree is gone too (`git worktree list`).
