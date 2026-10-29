# Landing Analysis: PLAN-27 — Web: Brand Mark, Pairing Page, Favicon

epic: ui-improvements
workstream: WS-02
pr: #262 (merge commit `4c39c0f8a2b6bb52b98a05a110ad5a64de7ccb7a`, branch `feature/27-web-brand`)

> Landing record for one shipped plan. Source: inbox message `rb-27-web-brand-001.md` (runbook lane, harness
> `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2032`), analysed 2026-10-29 against the merged PR, the CI
> checks, the merge diff and the Sonar gate. The browser artifacts, the per-head build logs and the self-review rounds are
> the run's account and are not re-run here.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 BrandMark (24 px in a 28 px square, 64 px) | shipped-as-specified, evidence order substituted | `BrandMark.svelte` (+85/-) and `BrandMark.test.ts` (+27) are in the merge diff. The `test-first` clause holds for the plate/rail test; the 64 px half passed against unfixed code and rests on a mutation-after run. The operator accepted the substitution (question 3). |
| D2 Pairing page (mark, wordmark, code at `--text-3xl`, QR) | shipped-as-specified, one operator-decided departure | `PairingView.svelte` and `pairing.spec.js` (+27) are in the diff. The QR now shows on every listener and the caption says the truth (`pairing.{en,de}.properties`); the operator chose this (question 2). |
| D3 Favicon and design | shipped-as-specified | `favicon.ts` (+45), `favicon.test.ts` (+144), `static/brand-mark-attention.svg`, the `+layout.svelte` wiring and `brand-mark.adoc` (+100) are in the diff. |
| D3b Open-issue read | met | `open_issue_count=0`, `open_issue_rules=none` in the landing; corroborated below. |

`deliverables_done=3` of 3 is consistent with the diff.

**Limitations the lane records, accepted as stated:** the 28 px plate is visible in no artifact, because the rail head
arrives with `web-app-shell` (PLAN-39); the square is covered by `BrandMark.test.ts` only. The favicon dot cannot show
in a page screenshot; it is evidenced by the icon `href` read in the browser and by `favicon.test.ts`. Empty states
with the mark (named in the Objective, in no deliverable) are left to PLAN-35 and recorded in `brand-mark.adoc`.

**Surface delta** (computed here from `git diff 4c39c0f8^1 4c39c0f8`, because `inbox landing-check` answers
`unmeasured`): declared 7, realized 18. Six declared files were changed (`BrandMark.svelte`, `BrandMark.test.ts`,
`PairingView.svelte`, `static/brand-mark.svg`, `pairing.spec.js`, `brand-mark.adoc`). One declared file is untouched:
`src/app.html` (the lane reports `<html lang>` stays `en` on the German pairing page, pre-existing). Twelve files are
undeclared: `favicon.ts`, `favicon.test.ts`, `static/brand-mark-attention.svg`, `+layout.svelte`,
`PairingView.scenarios.test.ts`, `pairing.{en,de}.properties`, `doc/design/web-app/{README,entry,theme-readability}.adoc`
and the two brand diagrams under `doc/resources/diagrams/`. All twelve are named in the message (§2 surface drift) and
sit in domains the run already loaded; the favicon files are the wiring D3 needs, the rest are the consistency repairs
of the design pages. No file outside the web app, the pairing messages and the design pages.

## Metrics and Anomalies

- Tokens: `unknown` (not measured on this harness, recorded as unobserved, not as zero). `plugin_version` `unknown`.
  `executor_version` `0.1.2032`.
- Anomalies:
  - Executor skew at the start (0.1.2021 against 0.1.2028), regenerated with the operator's agreement, re-check
    `match`. The plugin moved to 0.1.2032 during the run; no skew check ran against that version.
  - The one routed build at the start answered `execution_failed` with an empty log; every build then ran
    `--execution-mode in_process` (the build server was `ready` at preflight). First `verify` at `916198a` hit the
    856 s bound and passed on the re-run with `--timeout 1800`.
  - `gh` authentication was lost once (~13:05Z); the operator restored it. The free-text "retry" answers were not
    treated as a merge authorization (correct).
  - One earlier `sonar-build` run failed with "Organization is not allowed to access data from non main branches"
    (provider permission); every later run passed. Seen before on other PRs of this epic.
  - Two pre-existing e2e/unit flakes (`GitSourceIntakeModal` 5 s timeout, one `designer.spec.js` draft test), each
    green on the re-run.
  - Shell file reads: guard `none` on this harness, recorded only.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `a7b07d0`, `23403bc`, `b294111` and `916198a` (the merged head) and refused four times
  on quota. All trivial nitpicks were fixed except the last three, answered in PR comment `6500029686` and carried as a
  follow-up. No thread was ever opened. Sourcery refused on its hard quota (optional).
- CI/merge: `ci checks status --pr-number 262` is `success`, 30 checks, all `pass` or `skipping`. `build / build (25)`,
  `build (26)`, `sonar-build`, `quarkus-alignment`, `integration-tests / test` and `dependency-review` all passed.
  `origin/main` carries `4c39c0f8` after #265 (a CI-exclusion fix for the ledger branch).
- Sonar: the landing reports `open_issue_count=0`, `open_issue_rules=none` at the merge head. `sonar_rest gate-status
  --project plan-marshall_pm-mcp-clients --pr 262` answers `gate_status: OK`, five conditions OK
  (`new_duplicated_lines_density` 0.0, hotspots reviewed 100). Corroborated; this is a confirmed `0`, not the docs-only
  exception.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-27 --status shipped`
- [x] row `pr` stamped `262`
- [x] row `landing` stamped `landings/PLAN-27.md`
- [x] row `plan_marshall_plan_id` stamped `27-web-brand` (the sender's derived name)
- [x] epic.md narrative reconciled against the queue rows
- [x] defects and watches opened, updated or retired (see Follow-Ups)
- [x] resume anchor updated in `resume_anchor.md`
- [x] `queue-view.md` regenerated
- [x] inbox message `rb-27-web-brand-001.md` archived after this record

## Follow-Ups

- **Incomplete landing facts** (`total_tokens`, `plugin_version` unknown): recurrence on the Open Defect.
- **Favicon test tightening** (lane residue, three CodeRabbit nitpicks `cr-comment:v1:b6726aef…`, `…bf2be272…`,
  `…427de384…`): stroke half-width in the dot bounds, an exact palette set, the ring asserted on the dot circle. No
  staged plan owns it; low value, each assertion already proves the same about the shipped assets. Recorded as a Watch.
- **PLAN-49 dependency:** the favicon's attention count is `runtimeStore.openDecisionsCount`; when the attention inbox
  (`lib/stores/attention.ts`) lands with PLAN-49, the header badge, the tab title and the tab icon should all read the
  inbox total. Check PLAN-49's spec at emit.
- **PLAN-39 dependency:** the rail head must render `<BrandMark size={24} plate />` for the 28 px square to appear.
  Check PLAN-39's spec at emit.
- **PLAN-35 dependency:** the empty states carry the mark in `--color-text-muted`; `brand-mark.adoc` says so.
- **`<html lang>`** stays `en` on the German pairing page (`src/app.html`); pre-existing, nobody owns it. Watch.
- **Branch and worktree:** no `feature/27*` branch remains on the remote and no `27-` worktree is listed.
