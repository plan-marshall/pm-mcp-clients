# Landing Analysis: PLAN-156 — Web Views: The Modals and Pickers on the Design Vocabulary (and the Residue of PLAN-172)

epic: ui-improvements
workstream: WS-03
pr: #536 (merged as b4663e27, `feat(web-app): the modals and pickers on the design vocabulary, and the residue of PLAN-172 (PLAN-156)`; 43 files, +1711/-1478)

> Landing record for one shipped plan. Written by `analyze` (inbox message `rb-156-web-views-modals-and-pickers-001.md`, runbook lane, harness `opencode`, model `deepseek-v4.1-flash/high`, effort `high`, executor `0.1.2183`; analysed 2026-11-23). `landing-check`: `total_tokens` and `plugin_version` are `unknown`. The message was read as a lead; one read-only leaf read the squash commit, the cited build logs and five D5 screenshots; the orchestrator re-read the Sonar gate.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D6 dialog and interaction components (PLAN-172 residue) | met, with the evidence ORDER `mutation-after` for clause 2 (the base tree answered `executor_in_worktree: absent`; reverting the committed fix in the plan worktree is equivalent for the three behavioural tests); **one sentence of "What" unmet:** the values of `web.confirm.destructive.title` and `.consequence` (en and de) are asserted by no test; the lint clauses cannot fail on the six components (`vocabulary-lint.mjs` scans only files outside `components/`) | `Dialog.test.ts:253,272,288,308`; `RowMenu.test.ts:790`; logs `171528`, `171658`, `172001` |
| D1 `HoldResolutionModal` | met, **test-first** (11 of 18 cases failed at base `a4fa36fa`) | `HoldResolutionModal.test.ts:66`; log `172310` |
| D2 `FindingsReviewModal` (restyle) | met in substance, evidence `mutation-after` where the spec says test-first | `FindingsReviewModal.test.ts:86` |
| D3 `ConfinedFilePicker`, `PrAdoptPanel` | met in substance, `mutation-after`; stale premise: the spec's disabled _Move_ entry is not in the picker (Move lives in `SettingsView.svelte:566`) | `ConfinedFilePicker.test.ts:147`; `PrAdoptPanel.test.ts:44` |
| D4 `GrantModal`, `LanListenerModal`, dead `StartWorkModal` | met in substance, `mutation-after`; the removal leaves two components with no production importer (L3-4) | `GrantModal.test.ts:26`; `LanListenerModal.test.ts:50` |
| D5 browser evidence | **not met as written (the lane's own account), plus a gap the lane did not report:** the grant question never opened in a browser (and `GrantModal` has no e2e at all); the picker shots come from `fully-onboarded`; the baseline has 16 files, all at 1440×900, no phone set (the landing's section 1 says "both sizes", section 9 says "no phone set"); **the after-shots predate the self-review fixes** (mtimes 19:03–19:11, fixes `68c16abb` 20:18 and `104356b2` 20:32; the picker shot still shows the pre-fix secondary "Add documents" and no directory checkbox); a contrast audit only for the review modal, a keyboard walk only for the hold dialog | leaf (screenshot mtimes and content) |
| added-unplanned | `loopback.ts` (+25, no test; 80/50 branch coverage), `runtime.ts`, `TopBar.svelte`, `CredentialEntry.svelte`, `SettingsView.svelte` (the CodeRabbit de-duplication, no browser behaviour change); `tokens.css`/`tokens.test.ts`/README (required by D6(d)); fixtures and test ids | leaf |

**"5 of 6 met" is the lane's count;** by the spec's rule the D2, D3 and D4 clauses are `mutation-after` and not `test-first`, and D5 is not met. No ledger defect depends on D5; the risk is a visual regression in grant and in the picker after round 2.

## Three-Layer Analysis

By reading, no build, no test run, no write.

- **Dispatch:** one read-only leaf (`execution-context-level-3`, plan-less), 43 files over six modals and a dialog layer.
- **Verdict:** layer 1 findings (8, minor); layer 2 findings (5, minor); layer 3 findings (9 minor, 1 info, **no confirmed major**). No blocker.
- **Re-read by the orchestrator:** the Sonar gate for #536 (OK, new coverage 90.0, duplication 0.0, hotspots reviewed 100). The leaf's code points are leads of the orchestrator, except those it states as run logs.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity |
|---------|-------|---------|----------|
| L1-1 (minor) | "5 of 6" counts D2, D3, D4 although the lane itself labels them `mutation-after` | leaf | refutes |
| L1-2 (minor) | D5 gaps (a)–(f) above | leaf | refutes "met as written" |
| L1-3 (info) | D6's revert-based evidence is acceptable; the four `portal.test.ts` failures in log `172001` are import-level and prove nothing; the nested-Escape test (added after CodeRabbit) was never run against a revert | leaf | accept |
| L1-4 (minor) | D6(c) second half unmet: no test asserts the confirm texts | leaf (grep of tests) | refutes |
| L1-5 (minor) | the `lint:vocabulary` clauses cannot fail on `components/`; no raw `<button>/<input>/<textarea>` remains in the six components (by reading) | leaf | refutes the instrument |
| L1-6 (minor) | D3's disabled _Move_ entry is a stale premise, unreported | leaf | refutes |
| L1-7 (minor) | the portal target (nearest themed ancestor) is a decision not in the landing message | leaf | none |
| L1-8 (minor) | `shared/work-actions.adoc:312`: _Accept the change_ confirms "after a confirmation that shows the same facts"; the new `ConfirmDialog` shows only the explanation and the reason field; the hold acts sit in the dialog body, not the footer slot | leaf | deviates |

### Layer 2 — Implemented

`Dialog.test.ts` (+96: themed section, nested Tab, nested Escape, autofocus child, Escape on a focused menu item) and `HoldResolutionModal.test.ts` assert behaviour; the hold kinds match `HoldKind.java`. Gaps: of the five irreversible hold acts only `accept` and `cancel_epic` are driven; no test for Escape on the open confirmation or for the CodeRabbit fix (the pending confirmation resets when the owner closes); **L2-3** the `FindingsReviewModal` "holds the focus … walks its acts with Tab" test passes with no trap at all (`contains(self)` is true while focus sits on the dialog box, jsdom never moves focus on a synthetic Tab), so only `Dialog.test.ts` pins the trap; **L2-4** `loopback.ts` has no test file (the picker test mocks it away); **L2-5** the Escape yield is pinned only against removing it, not against its focus-keying (no test has a menu open in the dialog with focus elsewhere, which is what D6 asks); the round-2 "a directory is pickable again" fix and the LAN modal's non-danger turn-off are untested.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `Dialog.svelte` `takeFocus`; `HoldResolutionModal.svelte` | the owner closes or the runtime drops the hold while the confirmation is open | the hold `Dialog` and the `ConfirmDialog` are sibling blocks and go in one flush, outer first; the inner stored `overflow: hidden`, so the page may stay scroll-locked; no test covers the order | minor, suspected |
| L3-2 | `Dialog.svelte` capture handler; `ActMenu.svelte` `onWindowKeydown` | a menu open in the dialog with focus elsewhere | Escape closes the dialog and the menu's own handler never runs | minor |
| L3-3 | `Dialog.svelte` `onKeydown` | an Escape that cancels an IME candidate | `event.isComposing` is ignored, the dialog closes; this is the open PLAN-155 IME minor, **not retired** | minor |
| L3-4 | `SourceIntakePanel.svelte`, `RecipeSelector.svelte` | none | after the `StartWorkModal` removal they have no production importer (only their tests); the keys `start.modal.title`, `start.action.start`, `start.action.cancel`, `start.field.title`, `lesson_source.start_from_lesson` are orphaned; both components are on PLAN-53's surface | minor |
| L3-5 | `LanListenerModal` | a LAN browser turns the listener off | the turn-off dropped from `danger` to `secondary`; a LAN browser loses its own access, and the modal is reachable from there (`SettingsView.svelte:1308`) with no confirmation (a pre-existing hole; the restyle removed the only visual signal) | minor |
| L3-6 | `ConfinedFilePicker.svelte` | the two root buttons | both `secondary` with only `aria-pressed`: the selected root has no visible state (screenshot-verified) | minor |
| L3-7 | the review dialog | any review | the stock 560 px `Dialog` (was 72 rem) leaves a ~300 px detail column; the accept-remaining sentence renders in the scroll pane while its acts sit in the footer; the focused button is replaced on toggle and focus drops to the body | minor |
| L3-8 | `Edit`/`Promote`, Compose | the panels; an empty send | `Edit` and `Promote` now close each other (round-1 exclusivity); a Compose send on empty text is enabled but inert; disclosed by the lane | minor |
| L3-9 | `HoldResolutionModal` | Safari or Firefox on macOS | focus return relies on `document.activeElement` at mount and the modal does not forward `returnFocusElement`; those browsers do not focus a clicked button, so focus falls to the body | minor, suspected |
| L3-10 | the hold confirmation | none | title and confirming act share one label with a trailing "…" | info |

**Verified clean:** the hold acts and kinds against `HoldKind.java`, the mock scenarios and `WorkActionDialogue.java:51`; the irreversible set against `work-actions.adoc:314`; `confirmTitle`/`confirmConsequences` have no caller; `StartWorkModal` has no importer; every used key exists in en and de; no numeric `z-index` in the six files; no `{@html}`; nested dialogs answer only on the top (`openPanels` stack).

**Not shown by reading:** the e2e result (458 passed, 75 skipped) and `theme-readability.spec.js`; the Sonar open count (gate OK); only five of the screenshots were opened.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md § Web Standards | no `{@html}`; no timer wait | conforms | leaf |
| CLAUDE.md § Dependencies | no dependency without asking | conforms | no POM or package file in the diff |
| spec | evidence order test-first | deviates | D2, D3, D4 `mutation-after`; D6 clause 2 by revert |
| spec | re-take screenshots after later visual fixes | deviates | after-shots predate `68c16abb` and `104356b2` |
| CLAUDE.md | zero Sonar findings | conforms | `sonar_rest gate-status --pr 536` OK; the lane states `0` |

## Metrics and Anomalies

- Tokens `unknown`, `plugin_version` `unknown`; model `deepseek-v4.1-flash/high`; executor `0.1.2183`. `guard: none on this harness (unenforced)`.
- Review: `review_body_findings_total=3`, `handled=3`, `unresolved_review_threads=0`.
- Per-head runs green at all pushed heads; `-Pe2e` 458 passed, 75 skipped at `2a8d671d` and `a865fb59`; `-Pcoverage` pm-web-app 80.86 % branch (the margin over the minimum is 0.86 pt).

## Routing and Merge Behavior

- Merge `b4663e27` (merge queue). Rebased over #535 (`a865fb59`, the dev-gallery files and `EmptyState.svelte`).
- Overlap observed: none with a staged spec's declared surface by `declaration-currency`.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 536; `landing` `landings/PLAN-156.md`; `plan_marshall_plan_id` `rb-156-web-views-modals-and-pickers`
- [x] epic.md: the PLAN-172 entry closed in substance (D6 landed), the PLAN-58 modal minor retired as moot, an Open Defect for this landing (D5 evidence, layer-3 minors, the orphans); the PLAN-155 IME, PLAN-153 focus and PLAN-88 Dialog width entries stay open (not touched)
- [x] resume anchor and `queue-view.md` regenerated

## Follow-Ups

- No confirmed layer-3 major. The minors and the unmet D5/D6 sentences ride the web carrier decision (decision 2); the orphaned `SourceIntakePanel`/`RecipeSelector` and their keys are for PLAN-53's surface owner or a sweep.
- The PLAN-155 IME item for `Dialog.svelte` is still open.
