envelope_version=1
sender_type=plan
sender_id=rb-25-web-core-components-controls
epic=ui-improvements
kind=landing
created=2026-10-29T06:53:09Z

```landing-facts
schema=landing-facts/1
plan_id=rb-25-web-core-components-controls
epic=ui-improvements
pr=#260
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=4
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2028
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# PLAN-25 — Web: Core Components, Controls

Landed as #260, merge commit `8b19a4a7592ed07d4c62430e2b8a8137c4374ef7` (merge queue).

## 1. Deliverables

Four deliverables, every Done-when met. `order` is `test-first` where the test was seen failing against the unfixed code.

**D1 — Button and IconButton** (commit `a0f80a3`, later `f77815f`, `11d0ba1`, `097be20`, `7d47877`, `fe33489`)
- *Unit tests per variant, size and the disabled reason* — **met**, `order: test-first`. `Button.test.ts`: four variants, three sizes, the `small` alias, the icon at 16/20 px, the reason as title and as `aria-describedby`, the caller's description kept, the existing props. Failing run `a4be10a1`, `maven-2026-10-28-210352.log`: `Tests 11 failed | 798 passed (809)` (sizes, icon, reason, description, loading). Green at `a0f80a3`.
- *Browser check, both themes, baseline and after* — **met**, `order: none`. Baseline: the pre-change checkout at `a4be10a1` served on the same ports answered **HTTP 404** at `/dev/components`. After: `de`/`en` at 1440×900 and 390×844 with the light and dark `data-theme` blocks, at `df9fb08` and re-captured at the final head. Files: `.plan/temp/playwright-mcp/25-dev-components-{de,en}-{1440,390}-{before,after}.{png,yml}`.

**D2 — StatusLabel, CountBadge, Chip** (commit `f4f1730`, later `f77815f`, `11d0ba1`, `a99f4bf`, `7d47877`, `fe33489`)
- *Unit tests per state colour and the copy act* — **met**, `order: test-first`. `StatusLabel.test.ts` (five tones, dot `aria-hidden`), `CountBadge.test.ts` (neutral/attention, the name read instead of the number), `Chip.test.ts` (copy, refused clipboard, rejection fallback, both-fail, clearing confirmation, the live region standing before its text). Failing run `a0f80a3`, `maven-2026-10-28-211057.log`: `Test Files 3 failed | 69 passed (72)`. Green at `f4f1730`.
- *Browser check* — **met**, `order: none`. Same page and shots; the five state words, the attention badge and the chip read correctly in both locales and themes.

**D3 — ActionBar style** (commit `36e3fc5`, later `f77815f`, `11d0ba1`, `a99f4bf`)
- *Five acts → one primary, three secondary, one in the overflow* — **met**, `order: test-first`. `ActionBar.test.ts`: exactly one `.btn--primary`, three `.btn--secondary`, the fifth act only inside `role=menu`; plus focus on open, arrows, Home/End, Tab, Escape, the skipped disabled entry, and the advisory on the primary act. Failing run `f4f1730`, `maven-2026-10-28-211539.log` (`Failed to resolve import ./ActionBar.svelte`). Green at `36e3fc5`.
- *Browser check* — **met**, `order: none`. The overflow opens with the fifth act and the first *enabled* entry focused, `document.activeElement` read as `Retry` while the disabled `Archive` stays unreachable; Escape closes it. `25-dev-components-en-1440-overflow-after.{png,yml}`.

**D4 — Development page** (commit `bd88beb`, later `097be20`, `1d494f9`, `df9fb08`)
- *The production build holds no `/dev` route* — **met**, `order: test-first`. `devRouteExclusion.test.ts`: the route leaves the generated manifest, a node only it used is dropped, a shared node stays at its index, `/dev` and `/dev/x` both count, the node's import is substituted, the plugin is build-only, the manifest matcher covers the build layout, the hooks answer, the config registers the plugin. Failing run `36e3fc5`, `maven-2026-10-28-211942.log` (`Failed to resolve import ./devRouteExclusion`). Green at `bd88beb`; the build-level fact re-checked on a removed `build/` at `097be20` and `7d47877`: no `/dev/components`, no `dev.*` page key, no `action-bar` style, `() => Promise.resolve({})` at the development node's index.
- *Browser check* — **met**, `order: none`; before/after shots as in D1.
- *Open-issue read at the PR head* — **met**, `order: none`. At `fa8c30e8`: `count_status: confirmed`, `open_issue_count: 0`, `analysis_revision` = head; `sonar-gate` answered `merge_allowed: true` (`no_open_issues`). Earlier heads carried two findings (`S8786` super-linear expression, `S7770` redundant arrow), both fixed in `097be20` and `1d494f9`.

Per-head test runs: `-pl pm-web-app verify` after every deliverable (`a0f80a3`, `f4f1730`, `36e3fc5`, `bd88beb`) and after each review fix; the full `verify -Ppre-commit` on `bd88beb` (2177 tests), `df9fb08` (2177), `1d494f9` (2177), `fe33489` (2208) and the merge head `fa8c30e8` (2208), each with no rewritten files. The project publishes no `module-tests` canonical, so the gate canonical is the test run.

## 2. Personas and skills

- **Persona**, every deliverable: `plan-marshall:persona-implementer` (the spec's own line). Self-review rounds: `plan-marshall:persona-code-reviewer` in a sub-agent, report-only.
- **Domain skills**, every deliverable: `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing` (declared, all three loaded as standards).
- **Surface drift**: `pm-api` (the two catalogue packs) and `src/routes/+layout.svelte` were touched beyond the spec's Expected Surface. No extra skill was needed for them: the change is resource text and a six-line guard, no Java and no new pattern. Recorded as `skills added: none, reason: catalogue resources and a layout guard, no domain rule beyond the frontend skills already loaded`.

## 3. Hypotheses

- Spec HYPOTHESIS: "today's `Button` is used by few views and has no variants of VD-P-05" — **refuted** at `pm-web-app/src/lib/components/Button.svelte:4` (all four variants present) and by the 18 views importing it; added discovery: ten call sites in three views already pass `size="small"`, a prop the component ignored (`ReportsView.svelte:763`, `DecisionView.svelte:862` and five more, `ProjectExplorerView.svelte:686`).

## 4. Findings

**Self-review round 1** (12 findings, all triaged): fixed the CountBadge attention contrast (white on `#B86E00` = 3.99:1, replaced by the enforced `--color-attention-text` on `--color-attention-subtle`), the `/dev` prefix matching `/device`, the menu's missing focus contract, the advisory dropped for the primary act, the Chip confirmation never clearing, `aria-label` on a generic span, a duplicate `min-height`, an inert class, the double substitution path, and the missing wiring test. Kept with reasons: `size="small"` now draws 28 px; `dev.*` in the common pack; the build-level exclusion guarded by tests plus a manual grep.
**Round 2** (three defects in my own fix): focus stolen from non-menu acts, Tab dropping focus, a disabled first menu entry blocking the keyboard — all fixed, with tests that fail on the old code.
**Round 3**: a vacuous N3 test and a fixture that could not show the skip; the Chip live region removed from the accessibility tree by `:empty` — all fixed.
**PR review (CodeRabbit, `df9fb08`)**: `devRoute` not gated on the build → fixed (`import.meta.env.DEV`); matcher asymmetry between the manifest and the node patterns → fixed; clipboard unavailable on the plain-HTTP LAN origin → the selection-based fallback added; `dev.*` keys in the shared pack → answered, thread resolved, CodeRabbit withdrew the finding. Review of `1d494f9`: the rejection-fallback path untested → test added (`fe33489`).
**Sonar**: `S8786` (super-linear expression) → the dictionary entry is parsed without a regex (`097be20`); `S7770` (redundant arrow) → `.map(Number)` (`1d494f9`). Final head: 0 open.

## 5. Reviewers

- **coderabbit** (required, quota-limited): reviewed `df9fb08` (22:59) and `1d494f9` (00:05, `kind=reviewed` marker naming that head). Both inline threads answered and resolved. The review-body finding was fixed. After the last push (`fa8c30e8`, the base merge) it was re-triggered once and returned no review — its check stayed `NEUTRAL`/skipping. **Standing review authorization applied**: a qualifying review on one of this PR's heads (`1d494f9`), every finding handled, one re-trigger after the last push with the silence recorded, and every other gate condition met on `fa8c30e8`.
- **sourcery** (optional): refused on its own budget — "request another review in 4 days and 23 hours" (22:51, re-stated 22:52). Not required, so the merge did not wait on it.

## 6. Operator questions

1. "The executor-skew check reports `skew` (executor 0.1.2021 vs lane text 0.1.2028). The lane is blocked until the executor is regenerated. How should I proceed?" — options: *Regenerate the executor, then continue (Recommended)*, *Stop the run as blocked*. The answer was free text, **"already regenerated"** — no option selected. It was a factual claim, verified by re-running `executor-skew` (`match`, 0.1.2028 = 0.1.2028) before any work started.
2. "Merge #260 into main?" with the reviewer, CI, verify and Sonar facts read in that step — options: *Merge #260 now (Recommended)*, *Hold — do not merge*. Selected **"Merge #260 now"**.

Free-text operator messages apart from those answers: none.

## 7. Rule check

- **A rule I broke, and corrected.** I created `Button.test.ts` and `lib/utils/uniqueId.ts` in the **main checkout** before realising the edit tools resolve against it; "repository files are created and edited only inside the worktree" was broken for those two files. I moved both into the worktree, removed them from the main checkout, and verified with `git status --porcelain` that the main checkout was clean before any commit (and it stayed clean; the merge cleanup confirms it at `8b19a4a7`).
- **A workaround worth naming.** The first routed build with `--project-dir .plan/local/worktrees/…` failed (`exit_code: -1`, empty maven log): the daemon resolved the relative path inside the worktree, so its wrapper detection fell back to a `mvn` that is not installed. Every later build passed the worktree's **absolute** path.
- **Shell.** One command per call; no `&&`, `;`, pipes, substitution, loops or heredocs; no `sleep`. Files were read and searched with the file tools. `rm` was used five times: the two misplaced files above, `src/lib/dev/EmptyDevPage.svelte` (superseded by the virtual stand-in), and `pm-web-app/build` three times before a clean-build check.
- **Executor.** Every script ran as `python3 .plan/execute-script.py …`; every build through the resolved `plan-marshall:build-maven:maven` command; every CI action through `plan-marshall:tools-integration-ci:ci`; no `gh`, no `./mvnw`, no test/lint binary, no `python3 -c`. The absolute `--project-dir` noted above is an argument value, never the executor path.
- **Guard.** `guard: none on this harness (unenforced)` — no line exists to read, and no refusal came from one. The counts above are the run's own account.
- **Spec read.** `spec_source: main_checkout`, no `spec_differ` marker was reported (the ledger copy was not read; `--spec-from-ledger` was not used).
- **executor-skew.** `skew` at session start (0.1.2021 vs 0.1.2028), answered, then `match`; re-checked once after the operator's answer and never again. Not `not_applicable`: the executor is a generated flat file, not a source checkout.
- Browsers: the mock and the app were started and stopped by this run on the plan's own ports (20250/20251); the pre-change baseline was served from the main checkout on the same ports after the worktree servers were stopped.

## 8. Build server

Setup preflight: `ready` (registered). Every build ran routed (`mechanism: daemon_longpoll`), with the daemon's own serialization; two of them returned `running` at the 300 s client window and were re-attached by `build_server wait` — no job was backgrounded and none was `killed`. No degradation happened, so no fallback reason applies.

## 9. Residue

- **`size="small"` now renders** at ten call sites in three views (`DecisionView` 6, `ReportsView` 2, `ProjectExplorerView` 2): 28 px and `--text-xs` where the inert attribute drew `md`. Deliberate, named in the PR; those views were not otherwise touched.
- **The `dev.*` copy stays in the shared catalogue pack**, dormant without the page — answered to CodeRabbit, which withdrew the finding.
- **The build exclusion is guarded by unit tests plus a manual clean-build grep**, not by an assertion on the built artefact; the repository's antrun check does that for `index.html` and the immutable assets only.
- **A menu Tab costs two presses** (close and return the focus, then move on) — a documented trade in the component.
- **sourcery** is unavailable to this repository for about five days on its own budget; it is optional.
- `plugin_version` came back `unknown` from the runtime stamp, so the facts block says so rather than guessing.
