envelope_version=1
sender_type=plan
sender_id=rb-31-web-core-components-containers
epic=ui-improvements
kind=landing
created=2026-10-29T17:58:23Z

```landing-facts
schema=landing-facts/1
plan_id=rb-31-web-core-components-containers
epic=ui-improvements
pr=#266
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=3
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2032
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# PLAN-31 — Web: Core Components, Containers

Merged as PR #266 (merge commit `0351ce78e2717ee5ab04bd51aeaed4a59aa8e1b0`). Seven container components of design system v2 (`Card`, `KeyValueList`, `Tabs`, `Breadcrumb`, `Dialog`, `Toast`, `AttentionStrip`), their unit tests, their fixtures on `/dev/components`, and their copy in the `dev.*` keys of the `common` pack in English and German.

## 1. Deliverables

| Deliverable | Done-when | Verdict | Order | Test | Failing run | Commit |
|---|---|---|---|---|---|---|
| D1 Card and KeyValueList | unit: head with actions; nested card refused by a dev warning or a divider | met | test-first | `Card.test.ts` (7), `KeyValueList.test.ts` (2) | `maven-2026-10-29-112117.log` — `Failed to resolve import "./Card.svelte"` | `82064b7f` |
| D1 | browser check on `/dev/components`, dark and light, snapshots and screenshots | met | none | `31-{desktop,phone}-{de,en}-final-after.{yml,png}`, baseline `31-*-before.{yml,png}`; the nested card renders a `separator` and its `Detail` head | — | `82064b7f` |
| D2 Tabs and Breadcrumb | unit: keyboard on tabs; middle shortening at phone width | met | test-first | `Tabs.test.ts` (13), `Breadcrumb.test.ts` (9), `shortenMiddle.test.ts` (5) | `maven-2026-10-29-115451.log` — `Failed to resolve import "./Breadcrumb.svelte"` | `6a802bdd` |
| D2 | browser check | met | none | `31-{desktop,phone}-{de,en}-final-after.{yml,png}`; at 390 px the id reads `plan-marshal…ay@36e3fc5c`; `31-tabs-per-theme-final.yml` shows the arrow key moving one list only | — | `6a802bdd` |
| D3 Dialog, Toast, AttentionStrip | unit: Escape and focus return of `Dialog`; the live region of `Toast` | met | test-first | `Dialog.test.ts` (12), `Toast.test.ts` (8), `AttentionStrip.test.ts` (3) | `maven-2026-10-29-123435.log` — `Failed to resolve import "./AttentionStrip.svelte"` | `0e949b7e` |
| D3 | browser check, Escape closes the dialog | met | none | `31-desktop-{en,de}-final-dialog-open.{yml,png}`, `31-desktop-{en,de}-final-after-escape.yml` (opener `[active]` = focus returned), `31-desktop-en-final-toast.yml` | — | `0e949b7e` |
| D3 | open-issue read at the PR head recorded | met | none | read at `acbeae9b`: 1 confirmed (`typescript:S5906`) → fixed in `d04ffe00`; read at `acbeae9b`: 0 confirmed, `sonar-gate` `merge_allowed: true` | — | `d04ffe00` |

Per-head test runs on pushed heads (the project publishes no `module-tests`/`test-compile`, so the gate canonical is the run): `82064b7f` 861 web tests; `6a802bdd` 878; `0e949b7e` 893; `c0bdb495`+`3f304502` 904; `9b111159` 909; `0012f6af` 910; `2b16afe` 915; full `verify -Ppre-commit` green at `0e949b7e` (2208), `c0bdb495` (2208), `0012f6af` (2208), `d04ffe00` (2208), `2b16afe` (2208) and at the merge head `acbeae9b` (2208, `maven-2026-10-29-165202.log`).

The Standing Constraint `./mvnw -pl pm-web-app verify` is covered by the same gate, which builds and verifies `pm-web-app` inside the reactor.

## 2. Personas and skills

- Persona for all three deliverables: `plan-marshall:persona-implementer` (declared by the spec), applied as a standard. The self-review rounds and the PR-comment triage applied `plan-marshall:persona-code-reviewer`.
- Domain skills, per deliverable as declared: `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`.
- No surface drift: every changed module (`pm-web-app`, `pm-api` messages) is named by the spec's Expected Surface and Standing Constraints. `lib/utils/shortenMiddle.ts` and the four test fixtures are new files inside those modules.

## 3. Hypotheses

The spec carries no `HYPOTHESIS` labels; its `OBSERVED` claims were re-read at HEAD before each deliverable and all confirmed:

- `pm-web-app/src/lib/components/{Card,KeyValueList,Tabs,Breadcrumb,Dialog,Toast,AttentionStrip}.svelte` absent — confirmed by search; each was created by this plan.
- `pm-web-app/src/routes/dev/components/+page.svelte` present and development-only — confirmed (176 lines, `lib/dev/devRouteExclusion.ts` untouched).

## 4. Findings

**Self-review round 1 (20 open).** 5 major: Card's nesting check keyed on a shared `.card` class (now the component's own `data-card`); Tabs' selection not derived from `tabs` (a late list left every tab unreachable); Dialog's `aria-modal` without focus containment; Toast's live region created populated; Breadcrumb's phone row clipped its text. 9 minor: duplicate `{#each}` keys, a nested card dropping its acts, a parentless card reported nested, an unnamed tab count, a toast countdown restarting on re-render, an act inside the live region, Escape reaching a view's handler, an optional trail name, a missing clamp in `shortenMiddle`. 6 nits: test assertions and the fixture's shared tab state. All fixed in `c0bdb495`/`3f304502`; two accepted with reasons (focus loss when a 4-second toast removes the focused act — inherent to the specified duration; a danger dialog with no consequences draws no list — an empty list says less than nothing).

**Self-review round 2 (10 open).** Dialog had no asserted accessible name (it had one; now asserted), a focus that had left the dialog escaped the trap (containment widened), Tabs discarded a view's selection when the list arrived after the fetch, the toast countdown re-armed after firing, an unused import, an unexercised nested-card branch, a stale test docblock, missing `de` evidence at the fixed head, an unnamed page tab count and an unnamed tab list. All fixed in `9b111159`; `de` desktop and phone re-captured.

**Self-review round 3 (2 open).** A regression from round 2: Shift-Tab from the dialog box itself left the modal — fixed in `0012f6af` with a test; the dialog/toast captures were stale — re-captured at the head (`31-desktop-{en,de}-final-*`). Clear after round 3.

**CodeRabbit (review on `d04ffe00`).** 4 inline findings — classify crumb labels before shortening (only `id` segments are shortened now, `id: true` on the fixture crumb), let callers set Card's heading level (`titleLevel`), exclude fieldset-disabled controls from the dialog's tab order (`.filter(matches(':disabled'))`), clamp the dev grid track (`minmax(min(260px, 100%), 1fr)`); 8 follow-ups — attention-strip wrap, key-value label column, dialog scroll lock, `contenteditable` in the focus walk, native tab order sorting, Tabs' `followed` reset, the full crumb label as accessible name (`aria-label` on a shortened link, sr-only text on the current segment), a break opportunity on crumb items; 4 nits — three test assertions and a final-crumb `href` case. All fixed in `2b16afe`; CodeRabbit marked each inline thread "Addressed in commit 2b16afe" and the four threads were resolved.

**Sonar.** `typescript:S5906` on `shortenMiddle.test.ts:19` (prefer `toHaveLength`) — fixed in `d04ffe00`; no open issue at the merge head.

**CI.** One flake in the first merge-head run: `pm-operator` `TuiScreenDriverTest.shutdownHookRestoresOnce` (a module this plan does not touch). The run was re-run at the operator's lane-vetted action and passed.

**PR comments.** No review comment raised an unsupported finding; the two bot rate-limit notices are refusals, not findings.

## 5. Reviewers

| Reviewer | Required | Result | Head reviewed | History |
|---|---|---|---|---|
| coderabbit | yes (quota-limited) | reviewed | `d04ffe00` (formal review, marker `final_review_risk_coverage` `kind=reviewed`, `coveredCommitId=d04ffe0`) | refused on quota at 14:48Z ("53 minutes"); waited 90 minutes (two `wait-for-comments` windows) and re-triggered at 16:20Z; reviewed at 16:33Z; refused again on `2b16afe` ("34 minutes"); merged under the standing authorization with the refusal on the merge head `acbeae9b` recorded (17:39Z) |
| sourcery | no | refused | — | hard quota (4 days) at 14:49Z; optional, does not gate |

Standing review authorization: the qualifying review on `d04ffe00` (one of this PR's heads) covers the branch's own files; every finding it raised is fixed in `2b16afe` and every thread is resolved; the reviewer was re-triggered once after the last push and refused; and every other condition (build, CI, Sonar, comments) holds on the merge head `acbeae9b`.

## 6. Operator questions

1. "The executor is 4 patch versions older than the lane text (0.1.2028 vs 0.1.2032). Regenerating `.plan/execute-script.py` is the skill's named remedy, and the file is gitignored so no tracked file changes. How should I proceed?" — options: "Regenerate the executor now (Recommended)", "Stop as blocked", "Continue on the stale executor". Selected: **"already regenerated in parallel session"** (free text); the re-check then reported `match` at 0.1.2032.

No other question-tool call was made. Free text: that one answer only.

## 7. Rule check

- **Guard: none on this harness (unenforced)** — OpenCode installs no pre-tool-use hook, so nothing was enforced and no guard log exists for this run.
- **A rule was broken and repaired:** the two `common.*.properties` edits were first applied in the **main checkout** by mistake; both were reverted by hand (leaving the tree clean) and re-applied inside the worktree. The main checkout was verified clean afterwards.
- **A shell rule was broken once:** one command used a pipe (`… | tail -c 2500`) while reading the review output. No file was written or read through it.
- **Build routing:** the daemon accepted the job twice and failed it with `execution_failed` (exit −1, 0 s, empty inner log); subsequent builds ran `--execution-mode in_process` with that reason recorded. The setup preflight itself answered `ready`.
- **Spec read:** `spec_source: ledger_worktree`, no `spec_differ` marker.
- **`executor-skew`:** `match` (0.1.2032) after the operator's regeneration; the initial `skew` is the first entry of this run.
- Everything else was kept: every script through `python3 .plan/execute-script.py`, every build through the resolved `maven` canonical, every CI action through `ci`, one command per call apart from the pipe noted above, files under `.plan/temp/` (`playwright-mcp/`, `rb-31-…/`, the landing), the worktree removed without force, the main checkout moved only by `switch-and-pull`.

## 8. Build server

Setup preflight: `ready`. The first two routed builds returned `execution_failed` with an empty inner log (the job never started); from then on builds ran in-process (`in_process`, reason `daemon_execution_failed`), so the gate ran either way. No `killed` job. No `runtime_stamp_reason` was returned; `plugin_version` came back `unknown` from the runtime-info seam and is reported as such.

## 9. Residue

- CodeRabbit has no review of the merge head `acbeae9b`; the merge rests on the standing authorization (quota-limited reviewer, review on `d04ffe00`, all findings fixed, re-trigger refused).
- Sourcery is optional and under a 4-day quota; it reviewed nothing.
- Two accepted findings: a focused act inside a 4-second toast is removed by the timer (inherent to the specified duration); a danger dialog with an empty `consequences` list draws no list (the caller's responsibility).
- The `pm-operator` `TuiScreenDriverTest.shutdownHookRestoresOnce` flake is unrelated to this plan and passed on re-run.
