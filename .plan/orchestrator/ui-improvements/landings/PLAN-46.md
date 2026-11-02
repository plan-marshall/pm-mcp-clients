# Landing Analysis: PLAN-46 — Web: Command Palette, Go to Work by Name

epic: ui-improvements
workstream: WS-03
pr: #296 (merge commit `d7f7ffc83c7733c3236e14802b24bd8a24eaa197`, merge-queue merge onto `66c1495a`)

> Landing record for one shipped plan. Source: inbox message `rb-46-web-command-palette-002.md` (runbook lane, harness
> `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2036`, written 2026-11-01T13:55Z; it revises the open-state
> observation `-001`, archived on 2026-11-01), analysed 2026-11-01 against the merged PR (`ci pr view`), the merge diff
> (`git show d7f7ffc8`, 17 files, +1332/−7), the PR's review comments (`ci pr comments`) and the Sonar read. The per-head
> test runs, the two self-review rounds and the Playwright artifacts are the run's account and are not re-run here.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Search index | shipped-as-specified (the run's account for the tests) | `stores/searchIndex.ts` and `searchIndex.test.ts` are in the merge diff, new. The failing run of the test-first order (`ec6afd87^`) and the browser check are the run's account. |
| D2 Palette panel | shipped-as-specified in scope, with open review findings (below) | `shell/CommandPalette.svelte` (new), `TopBar.svelte` (button enabled), `AppShell.svelte` and `chrome.spec.js` are in the diff. The palette tests (305 passed, 7 pre-existing failures) are the run's account. |

`deliverables_done=2` of 2 is consistent with the diff.

**Surface delta** (`inbox landing-check`, declared = the 9 backticked paths of the spec's Expected Surface, realized = the 17
merge-diff files): `expansion_detected`, 8 added, 0 declared and untouched.

- **Added, never declared (8):** `doc/design/web-app/needs-and-find.adoc`, `lib/api/client.ts` and `client.test.ts`,
  `lib/routes.ts` and `routes.test.ts`, `lib/shell/AppShell.svelte`, `lib/stores/runtime.ts`,
  `lib/views/DecisionView.svelte` (the jump to a decision the palette opens). The message calls them "undeclared but
  load-bearing". With PLAN-16, 17 and 82 this is the fourth plan in a row to under-declare its surface.
- The `overview.find.*` rows and `web.frame.search.title` the spec gap note assigned to this plan are in `overview.{en,de}.properties`
  and `common.{en,de}.properties` of the diff (whether each key is present is the run's account; not read key by key here).

## Review findings: the lane's account is contradicted

The message says CodeRabbit's formal review of `42aee47d` (11:23:46Z) was "summary only, no findings, no threads" and that "no inline
review threads exist". `ci pr comments` for PR #296 reads otherwise: **six inline comments by `coderabbitai`, created
11:23:43Z to 11:23:44Z, on the same head**, all `resolved: false`, with no reply from the lane or the operator in the PR:

| File:line | Severity | Finding (CodeRabbit's wording, shortened) |
|-----------|----------|-------------------------------------------|
| `CommandPalette.svelte:129` | Minor | `rows` lists every project act before the matches, so `Enter` on a query that returns a project opens that project's start route, not the matched project |
| `CommandPalette.svelte:350` | **Major** | no touch-accessible close control: on a phone without Escape the panel has no way out when no row is chosen |
| `CommandPalette.svelte:365` | **Major** | the arrow-key selection is a CSS class and `aria-current` on a button that has no focus; a screen reader misses it (suggests a combobox with `aria-activedescendant`) |
| `DecisionView.svelte:105` | Minor | `requestedDecisionId` is not cleared when the route names no decision |
| `DecisionView.svelte:112` | Minor | a decision the local filter hides is selected and the detail pane stays empty |
| `DecisionView.svelte:120` | Minor | the scroll to the requested card runs before the DOM updates (suggests `tick()`) |

What the merged tree shows (read with `git show d7f7ffc8:…` and `git diff 42aee47d d7f7ffc8`): `CommandPalette.svelte` has **no
change** after the reviewed head; at the merge `rows` still reads `...acts, ...matches` (line 126-127), the file has no
`aria-activedescendant` and no `role="combobox"`, and its only `Button` is the retry one (line 437). The change to
`DecisionView.svelte` after the reviewed head is the display-format merge-forward of PLAN-16 (`formatTime`), not these findings. So the
three `CommandPalette` findings are open on `main` as far as the code shows; the three `DecisionView` ones I did not check
line by line. Whether the threads are resolved I could not establish: the `ci` resolved flag is unreliable for threads
(PLAN-17's lane reported the same and found GraphQL disagreed), and no reply exists.

Consequences: (1) the standing review authorization needs "every finding handled"; the message says it was not relied on
("a qualifying review exists, every finding is handled (there were none)"), and the premise was wrong. The merge happened on a
qualifying review that carried unhandled findings. (2) The earlier Watch "PLAN-46 is blocked on the required reviewer" is closed
(a review came), but it is replaced by an open finding list. (3) Two Major findings are accessibility defects in a plan whose
neighbour PLAN-37 is the accessibility baseline.

## Metrics and Anomalies

- Tokens: `unknown`; `plugin_version` `unknown` (completeness check: both missing, the known opencode gap, recurrence recorded).
- Anomalies, the run's account: (1) the CodeRabbit review needed eight quota refusals and one accepted trigger over about 12 hours
  (23:45Z to 11:23Z); (2) a routed build failed instantly twice (`execution_failed`, empty log), every build then ran in process,
  the same bare-mvn failure as PLAN-17; (3) the lane lists its read-class shell breaks (`ls`, `cat`, `grep`, `tail`, `find | wc`,
  `;`-chains) and one commit subject carrying a literal backtick pair; (4) the baseline snapshots carry the new advisory text, so
  they are not a true before.
- Operator interaction: one question ("dirty worktree after merge", 171 files of pre-commit rewrite), answered in free text
  "Commit / land the changes on a new branch, use skip-bot-review as pr label to skip", carried out as PR #298.
- Size: 17 files, under the 100-file limit of the required reviewer.

## Routing and Merge Behavior

- Review: CodeRabbit (required, quota-limited) reviewed `42aee47d` only (formal review plus the six comments above); Sourcery
  (optional) refused on its own budget.
- CI/merge: PR #296 reads `merged`, merge commit `d7f7ffc8`, on `origin/main`; its single parent is `66c1495a` (the PLAN-17 merge).
  The merge-queue run id (41307855664) is the run's account.
- Sonar: the landing claims `open_issue_count=0`, `open_issue_rules=none`, read at `f4b746d3` (two issues at the first head were
  fixed in `42aee47d`). **Not re-corroborated:** `read_open_issues` for PR 296 answers `count_status: undecidable`, reason
  `head_revision_unavailable`, analysis revision `f4b746d3b6fe513a4dc8f86b73ed93ae8e300c53`, the revision the lane read.
  Recorded as an unverified lead, not as a `0`.

## The pre-commit follow-up (PR #298)

The operator asked the lane for it; it is not a plan of this epic. PR #298 (`chore(pm-api): apply the pre-commit profile's
import order`, label `skip-bot-review`) reads `merged`, merge commit `f5de8449` (parent `d7f7ffc8`, on `origin/main`): 178 files,
+1/−178 (`git show --shortstat`), 177 under `pm-api` and 1 under `pm-mock-runtime`. Those are the **same 178 paths as PR #294**
(the operator's earlier route, still `open`); #294 is therefore redundant. The lane's claim that `verify -Ppre-commit` leaves
`git status` clean on `main` now is its own run on PR #298's head; not re-run here. This removes the rewrite that PLAN-35, 37 and 26
would have had to restore, for runs that start from `f5de8449` or merge it forward.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-46 --status shipped`
- [x] row `pr` stamped — `296` (already set while running)
- [x] row `landing` stamped — `landings/PLAN-46.md`
- [x] row `plan_marshall_plan_id` stamped — `46-web-command-palette` (from the sender `rb-46-web-command-palette`)
- [x] epic.md narrative reconciled (Open Defect recurrence, Decisions, Watches; the Watch on the blocked reviewer closed)
- [x] resume anchor updated; `queue-view.md` regenerated

## Follow-Ups

- **Open review findings (above): fixed by PR #300 (see "Follow-up landing" below).** The operator chose a fix through the lane
  session, not a staged plan; no queue row was added.
- **Re-read at HEAD `f5de8449` before emit** (path-level match of the 17 files against each staged spec's Expected Surface;
  directory declarations such as `lib/shell/` or `lib/views/` are not matched by it): PLAN-34 and 73 (`common.*`), PLAN-36, 52 and 84
  (`overview.*`), PLAN-45 (`common.*`, `client.ts`, `client.test.ts`, `chrome.spec.js`), PLAN-49 (`overview.*`, `routes.ts`),
  PLAN-51 (`client.ts`, `runtime.ts`, `DecisionView.svelte`), PLAN-60 (`routes.ts`), PLAN-85 (`routes.ts`, `AppShell.svelte`,
  `chrome.spec.js`). PLAN-84 (`cli-find`) is no longer sequenced behind PLAN-46: both declared `overview.{en,de}.properties` and
  PLAN-46 has landed; PLAN-84 reads `overview.find.*` and adds only what is missing.
- **Pre-existing red e2e on `main`, the run's account:** seven failures reproduce on `66c1495a` (`epic-view.spec.js` tokens
  floor, `plan-view.spec.js` ×3, `showroom.spec.js`, the text guard `plan` de/en). The `epic-view.spec.js` cause is named as
  PLAN-16's formatter (`≥ 0.34 M` where the spec expects `340 k`); PLAN-16's own message reported 308 e2e passed. Not verified; the
  two reports disagree. PLAN-37 records the failing names at its own base: compare.
- **Residue the run named, carried as a Watch:** the browser's own clear action (no labelled button, no catalogue key); `/` now
  belongs to the palette; `overview.find.plain` still has no Texts row and no catalogue key.

## Follow-up landing: PR #300, the six review findings (analysed 2026-11-01)

Source: inbox message `rb-46-web-command-palette-003.md` (same sender, written 2026-11-01T15:42Z, `merge_state=merged`,
`deliverables 6/6` counts the six findings, not plan deliverables). It is the report of the follow-up the operator pasted into the
PLAN-46 lane session; it is not a new queue row, and the PLAN-46 row stays `shipped` with `pr` 296. Analysed against `ci pr view`,
`gh pr view 300` (read side), the merge diff (`git show 1ce32d80`) and the merged tree.

| Check | Result |
|-------|--------|
| PR #300 | `merged` 2026-11-01T15:40:44Z, merge commit `1ce32d80`, single parent `f5de8449`, on `origin/main` |
| Diff | 4 files, +239/−41: `CommandPalette.svelte` (+111/−…), `DecisionView.svelte`, `DecisionView.test.ts` (+70), `chrome.spec.js` (+71); nothing else |
| Label / reviewers | label `skip-bot-review`; the `CodeRabbit` and `Sourcery review` checks read `SKIPPED`; no review exists on the PR. The merge rests on the label and on the operator's pasted word, as the message says |
| CI | `build` (25, 26, quarkus-alignment, sonar-build, gate), `integration-tests`, `dependency-review`, `SonarCloud Code Analysis`: success; `deploy-snapshot`, `auto-merge` skipped |
| Sonar | the lane claims `count_status: confirmed`, 0 open, at `8e47dd0e`. **Not re-corroborated:** `read_open_issues --pr 300` answers `undecidable`, reason `head_revision_unavailable`, analysis revision `8e47dd0e`. An unverified lead, not a `0`; the Sonar bot comment on the PR reads "Quality Gate Passed" |

The six findings against the merged tree (`git show 1ce32d80:…`):

| # | Finding | In the merged code |
|---|---------|--------------------|
| 1 | acts listed before the matches | `rows` is `...matches` then `...acts` (`CommandPalette.svelte:126-127`); test `keeps the matched project ahead of its start act` |
| 2 | no touch close control | `Button` with `data-testid="command-palette-close"` and `web.common.close` (:368-371); the key exists in `common.{en,de}.properties:64` ("Close"/"Schließen"); test `closes the panel from a labelled control on a phone` |
| 3 | selection not exposed | `role="combobox"` (:378), `aria-activedescendant` (:382), `role="listbox"` (:401), `role="option"` (:415, :450); test `exposes the arrow-key selection as a combobox` |
| 4 | `requestedDecisionId` survives `null` | reset in an `else` branch of the first effect; test `drops a pending decision when the route stops naming one` |
| 5 | a filter hides the requested decision | `query = ''` when `matchingDecisions` lacks the requested id; test `clears the local filter when it hides the requested decision` |
| 6 | scroll before the DOM updates | the query runs in `void tick().then(…)`; test `scrolls to a decision whose card mounts after the request` |

All six are present in code with a test of the matching name. Not re-run here: the failing-first logs, the 2527/1027 test counts,
the 308 e2e passes and the Playwright artifacts are the run's account. Whether the six PR #296 threads were marked resolved on
GitHub was not established (the `ci` resolved flag is unreliable); the code, not the flag, is the evidence.

**What the fix did not get:** a review. The two Major findings are accessibility changes (a combobox with `aria-activedescendant`, a
dialog close control), reviewed by no bot and by the lane's own self-review only. PLAN-37's axe-core audit (when it merges) is the
first independent check on the palette's markup; PLAN-37's audit covers the 22 declared views, and the palette is a panel on top of
them, so whether it is audited is not established.

**Residue carried from the message (unchanged, a Watch):** the seven pre-existing red e2e tests on `main` (same names as before:
`epic-view.spec.js` tokens floor, `plan-view.spec.js` ×3, `showroom.spec.js`, the text guard `plan` de/en), measured on `f5de8449`
both before and after the change; the palette's clear action is still the browser's own (`type="search"`), no catalogue key; `/`
belongs to the palette. Shell rule breaks listed by the lane: read-class `grep` and `tail` through the shell.

**Re-read before emit (path-level match against the staged specs):** PLAN-51 and PLAN-57 name `DecisionView.svelte` (PLAN-51 also
`DecisionView.test.ts`); PLAN-04, 39, 45 and 85 name `chrome.spec.js`. PLAN-51, 45 and 85 were already on the PLAN-46 re-read list.

### Three-layer analysis of PR #300 (2026-11-01, by reading; nothing built or run)

Method: the epic's rule "Landing analysis in three layers" (epic.md, Queue annotations). Files read at `1ce32d80`: the whole
`CommandPalette.svelte` diff and its script and markup, the `DecisionView.svelte` effects, the three added unit tests and three
added e2e tests, the PLAN-46 spec, `doc/design/web-app/needs-and-find.adoc`.

**Layer 1, specified. Verdict: met, with one gap.**
- The follow-up's brief is the six findings (the table above). Each is answered.
- PLAN-46's own Done-when for D2 reads "`⌘K`, "relay", `Enter` opens the first match". With the acts ahead of the matches that clause was
  not true for a query that returns a project: the first row was the act. PR #300 is what makes the shipped plan meet its spec (the
  e2e `keeps the matched project ahead of its start act` is the first test of that clause for a project).
- The design says "an act **beside** the matches" (`needs-and-find.adoc:51`); acts after the matches fits it.
- **Gap: the design document was not updated.** The plan's Standing Constraints say the design document the plan changes is updated
  in the same pull request (UX-S-13, VD-S-09). PR #300 changes the panel's behaviour (a visible close control at every width, a
  combobox/listbox model with an active descendant, Enter default on the first match) and touches no file under `doc/`. The design
  names `Esc` as the only way to close (`:47`) and says nothing of the close control or the keyboard model.

**Layer 2, implemented. Verdict: the six fixes are present; the PR body overstates "exactly those six and nothing else".**
- Present: see the table above (code lines and test names).
- Beyond the six, in the same files: every result row changed from `<button>` to `<div role="option" tabindex="-1">`, so rows left the
  Tab walk (`cycleFocus` selects `input, button:not([disabled])`, `CommandPalette.svelte:272`); the group list and items became
  `role="presentation"`; each group wrapper became `role="group"` with an `aria-label` that repeats the visible `h3`. The body says
  "The Tab trap and `aria-modal` are unchanged": the trap is, the walk is not (before: field, rows, Retry; now: Close, field, Retry).
  That is the combobox pattern and probably right, but it was not stated.
- Stale comments left by the change: `:251` "A button in the panel (a row, Retry) keeps its own Enter" (a row is no longer a button) and
  `:266` "the field, the rows, and Retry" (rows are no longer stops).
- Not in the diff: no catalogue key (the existing `web.common.close` serves), no design document (above).

**Layer 3, correct. Verdict: the two Major findings are not fully closed; one new defect, three weak tests.** All are read, not run.

| # | Where | Observation | Severity |
|---|-------|-------------|----------|
| C1 | `CommandPalette.svelte:401-480` | The results container is now `role="listbox"` and still holds the empty-state `<p class="command-palette__empty">` (:468) and, on a failed read, the Retry `Button` (:478). A listbox may own only options (or groups of options); a paragraph and a button inside it are invalid owned content and a button inside a listbox is an interactive control nested in a composite widget. This is new in PR #300 (the container was a plain `div` before). The fix for the Major finding 3 introduces an ARIA error in exactly the "no match" and "failed" states the design names (`needs-and-find.adoc:49`). Axe has rules for required and allowed children of a role and would probably flag it; that is a guess, not run, and whether PLAN-37's audit opens the palette is not established. | Minor to Major (a11y) |
| C2 | `:405` | `h3` inside `role="group"` inside the listbox, plus an `aria-label` on the group equal to the `h3` text: the kind is named twice; a heading is not an allowed child of a listbox group. | Minor |
| C3 | `:379` | `aria-expanded="true"` is constant, also with no rows (empty field, empty answer); the e2e test pins it (`toHaveAttribute('aria-expanded','true')`). | Minor |
| C4 | `:134-143` | `rowOptionId` is `rows.findIndex` per row (quadratic in rows) and returns `command-palette-option--1` for a key the new answer dropped, so `aria-activedescendant` can name a missing id until the effect at :146 resets the selection. Transient. | Minor |
| C5 | palette | The selected row is never scrolled into view (`scrollIntoView` appears nowhere in the file): with a long answer in a 70vh panel ArrowDown moves the active descendant to an invisible row. Not a regression and not in the six findings; now that the selection is announced as the active descendant it is more visible. | Minor, unspecified |
| T1 | `DecisionView.test.ts:759` | `clears the local filter when it hides the requested decision` asserts that the filter input is empty and that the other decision is visible again. It does not assert that `dec-101` is selected or that the detail pane is not empty, which is the harm finding 5 names. A change that cleared the filter and still lost the selection would pass. | Weak test |
| T2 | `DecisionView.test.ts:782` | `Element.prototype.scrollIntoView = vi.fn(…)` is assigned and never restored (the file's `beforeEach` at :74 resets the runtime store; the file has no `afterEach` and no `restoreAllMocks` by grep); every later test in the file runs with a `scrollIntoView` that jsdom lacks, so `typeof card?.scrollIntoView === 'function'` is true for them. | Test hygiene |
| T3 | `chrome.spec.js` (added) | The three palette tests cover the happy paths only: no test opens the palette on a query with no match or with a failed read, which is where C1 sits; no test presses Tab to show the new walk. | Gap |

Checked and found sound: the order of `rows` (matches in served order, then acts) equals the rendered order, because `groups` is
`groupFindMatches(matches)` in served order, so `Enter` on the selection opens what the screen shows; the Enter guard
(`tagName !== 'BUTTON'`) still lets the Close and Retry buttons keep their own Enter; the effect pair in `DecisionView` converges (the
request effect writes `query` and `requestedDecisionId`, both guarded, and the second pass returns at `!requested`); `tick()` is imported
and the scroll query now runs after the DOM update; `matchingDecisions` is derived from `queryTokens` (the query) alone (read at :77-82), so clearing `query` is enough to
un-hide the decision. One behaviour choice not in the spec: the request silently discards the operator's typed
filter; the finding's own suggestion is not quoted here, so whether this is what CodeRabbit asked for is not established.

**Net:** all six findings are addressed in intent; findings 2 and 3 (the two Major) are closed for the case with results, and C1 opens a
fresh ARIA problem in the empty and failed states. The follow-up, like the original, merged with no independent review.

**Owner needed (operator's call):** C1 (and C2, C3) as a second small fix, ideally on the same lane session; the design document gap of layer 1;
T1 and T2 with it. They are one PR's worth, not a queue row of their own; PLAN-37 meets the palette's markup only if its audit opens it.
