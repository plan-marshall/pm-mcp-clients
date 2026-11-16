envelope_version=1
sender_type=plan
sender_id=rb-53-web-start-wizard
epic=ui-improvements
kind=landing
created=2026-11-16T14:45:11Z

```landing-facts
schema=landing-facts/1
plan_id=rb-53-web-start-wizard
epic=ui-improvements
pr=#455
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=2
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2135
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=5
review_body_findings_handled=5
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

# PLAN-53 — the start as a wizard with a working gate

Guard: none on this harness (unenforced). Merge commit `b2ac620499aa96904769165cfd5a1658abd9a550`.

## Deliverables

1. **Gate — met.**
   - "a gate that does not answer shows its cause after 20 s; `/start` without a project" — **met**, order `test-first`. Failing run: base `8f9e8cf4`, `.plan/local/plans/NO_PLAN/build-results/pm-web-app,pm-web-e2e/maven-2026-11-16-084048.log:6900-6945` (`element(s) not found: getByTestId('start-gate-checking')`, `…('start-no-project')`). Passing: `start.spec.js` "shows the setup gate with its cause and its retry after 20 s without an answer" (22.9 s in the run at `44fbf4c9`) and "shows the error pattern with its act for a start without a project"; the bound itself is pinned by `StartView.test.ts` "bounds the setup gate at 20 seconds" (`signal` + `timeoutMs === 20_000`). Commits `398372ed`, `88a37627`, `0bdb38a8`, `6307cb8c`, `17a7976f`, `44fbf4c9`.
   - Browser check — **met**, order `none`. Playwright MCP on `projects-and-start`, mock 20530 / app 20531: `start.sources`, `start.refused`, `/start` without a project, each in en/de, dark/light, 1440×900 and 390×844; baselines from the base tree and after from the head, every capture verified on `http://localhost:20531` before and after (a concurrent session shares the MCP browser; the operator chose "retry now with URL checks"). Screenshots `.plan/temp/playwright-mcp/53-web-start-wizard-{start.sources,start.refused,start.no-project}-{before,after}.png`; facts `.plan/temp/53-web-start-wizard-browser-check.md`.
2. **Steps 1 to 3 — met.**
   - "`start.spec.js`, `git-sources.spec.js`, `lesson-source.spec.js`: one primary per step; Back works" — **met**, order `test-first`. Failing run: base `8f9e8cf4`, same log `:6759-6900` (`.btn--primary` expected 1, received 0; `source-add-menu` absent). Passing: `start.spec.js` "keeps one primary act per step and goes back from the ready step to the sources", "goes back from the recipe choice to the sources", "goes back from a recipe's parameters to the sources"; `git-sources.spec.js` and `lesson-source.spec.js` assert the one primary and Back on their flows. Abandon asks first (the amendment's clause) by `ConfirmDialog`, held by two unit tests. Commits `398372ed`, `88a37627`, `0bdb38a8`, `6307cb8c`, `44fbf4c9`.
   - Browser check — **met**, order `none`, the same 24 variants.
3. **Lazy entry — dropped by its own condition, not implemented.** The spec's re-grounding makes it conditional on the contract checking the gate without an entry. It cannot: `pm-api/src/main/java/de/planmarshall/api/start/EntryOpenRequest.java:37` has no dry-run field and is the only request that names a project; the mock's `open()` creates the entry in the same call and returns `null` only for an unseeded project (`pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/StartRegistry.java:542-565`); `openEntry` is the client's only route (`pm-web-app/src/lib/api/client.ts:3096`). `doc/design/shared/starting-work.adoc#steps` now records the consequence: a retry can leave an entry no client addresses.

Per-head test runs: `398372ed` — pm-api 1580, pm-web-app, e2e 424 passed / 4 failed (the four reproduce at base); `a019c4c6` — gate 5102; `17a7976f` — gate 5102, verify 5102, e2e all plan specs green; `44fbf4c9` — gate 5102, e2e all plan specs green; `941fe759` (the merge head) — gate 5102. The merge head differs from `44fbf4c9` only in `.plan/orchestrator` ledger files, which are no module input, so the e2e suite was not re-run there.

## Personas and skills

- D1, D2: `plan-marshall:persona-integration-tester` (declared) with `pm-dev-frontend:javascript` and `pm-dev-frontend:javascript-security` as standards; `plan-marshall:persona-code-reviewer` for both self-review rounds and for triaging the PR comments; `plan-marshall:persona-implementer` folded in for the view and the catalogue work D1/D2 required beyond tests. D3: dropped, its persona and skills never loaded.
- Skills added for surface drift: none beyond the declared set — the touched files are `pm-web-app`, `pm-web-e2e`, the `start`/`common` catalogues and `doc/design/shared`, the surface the spec names.

## Hypotheses

- "The start hangs on 'checking' because the gate has no time limit" — **confirmed as the gap**: `client.ts:2871` bounded every request at 15 s and the gate carried no bound, no spinner and no cause; the bound is now the gate's own 20 s.
- "The entry is created by `openEntry` before any source exists" — **confirmed**: `StartRegistry.open` creates it in the opening call; this is what makes D3 impossible.
- "No `StartView.test.ts` / `start.test.ts` exists" — **refuted**: all three test files exist at HEAD.

## Findings

- Self-review round 1 (14 findings): fixed the nested card, Back's contract links, the step bar's semantics, the 20 s pin, `aria-haspopup`, the Add accessible names, the page's `h1`, the gate announcement, two doc drifts, the formatter line, the unused import and the duplicated keys; left open by decision: the adopt step's footer primary, the bar CSS duplication.
- Self-review round 2 (8 findings): fixed the `h1` outside the gate guard, the plan's third mark, the one-column divider, the cold-failure announcement and the recipe-Back e2e; the §4 files are declared in the PR body; formatting of pre-existing long lines is left.
- CodeRabbit (5 body findings, all handled): `inline-1` refuted (the page-width list carries per-view root selectors that no import discovery yields; membership is already derived by the completeness test) — answered on the thread; `inline-2`, `inline-3`, `inline-4`, `inline-5` fixed in `44fbf4c9` (the explorer read beside the gate, the retry announcement, the adoption-failure confirmation, the entry-change reset) — each answered and its thread resolved.

## Reviewers

- `coderabbit` (required, quota-limited, `review_complete_after_handling`): reviewed `17a7976f` (marker `coveredCommitId=17a7976f`, `kind=reviewed`), all five findings handled, its threads resolved; the later heads raised no re-trigger under the done exemption. One trigger posted at `12:19Z` (`@coderabbitai review`); its reply said it had already reviewed the last commit.
- `sourcery` (optional): refused on its own quota ("used your own review budget … request another review in 2 days"), recorded, no gate.

## Operator questions

1. "The Playwright MCP browser is being driven by another session at the same time… How should I proceed with the browser check?" — options: wait then retry / retry now with URL checks / report as not met. Selected: **"Retry now with URL checks"**.

## Rule check

- `spec_source: ledger_worktree`, no `spec_differ` marker; the spec was read from the shared ledger worktree.
- `executor-skew: match` (`0.1.2135` both).
- Builds: every build through `plan-marshall:build-maven:maven`; one `verify -Ppre-commit` hit the daemon's bound at 960 s (`timeout_source: explicit`) and was re-run raised to 1800/2400 s; no launch-fault retry was needed; one in-process fallback in the base tree (`executor_absent_from_worktree`, the sanctioned degraded route).
- Shell rule breaches I have to own: in the base-evidence recipe the prescribed copy-back of the test files used `cp`, and one early command read a log with `grep`/`sed`/`tail` before I switched to the file tools; one `awk` measured line lengths. No `&&`/`;`/pipe chains were issued, no `sleep`, no inline Python, no `gh`/`glab`, no direct build wrapper or test/lint binary.
- `.plan/` state: writes only under `.plan/temp/`, one `ci` body slot per prepared body, and nothing else; the spec and `.plan/orchestrator` were never edited.
- Git: no `stash`/`restore`/`checkout --`/`rebase`/force-push; the base branch came in by merge; every commit carries the trailer.
- The four `data-table` e2e failures are not this plan's: they fail identically at base `8f9e8cf4` (same log), and no file of theirs is in the diff.

## Build server

Setup preflight `ready`; builds: `daemon_longpoll` 20, `in_process_fallback` 1 (the base tree, `executor_absent_from_worktree`); fallbacks: one `timeout` at the daemon's explicit bound, re-run raised; no `killed` job; `runtime_stamp_reason` absent (the stamp's `plugin_version` reports `unknown` — the resolve seam returned it so).

## Residue

- The Objective's "Back on every step" is contract-limited: `start.sources` offers no Back link, so ① has none; recorded in the PR body and unchanged in the specification.
- The adopt step draws no footer primary (its acts are per epic inside `EpicAdoptView`).
- `npm run format:check` in `pm-web-app` is not part of the Maven build and was not run; the gate's own lint and `svelte-check` are green.
