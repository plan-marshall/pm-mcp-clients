# Landing Analysis: PLAN-157 — Web Views: The Entry Pages on the D1 Vocabulary (Views Package, Part 5)

epic: ui-improvements
workstream: WS-03
pr: #535 (merged as fe44e6a7, `feat(web): modernize entry pages onto D1 vocabulary and resolve PLAN-131 residue (PLAN-157)`; 27 files, +677/-280 against the first parent `a4fa36fa`)

> Landing record for one shipped plan. Written by `analyze` (inbox message `rb-157-web-views-entry-pages-001.md`, runbook lane, harness `opencode`, model `deepseek-v4.1-flash/high`, effort `high`, executor `0.1.2183`; analysed 2026-11-22). `landing-check` is **incomplete**: `total_tokens` and `plugin_version` are `unknown`. The message was read as a lead; the PR, the merge commit, the review gate and Sonar were re-read, and the lane's progress file `.plan/temp/rb-157-web-views-entry-pages/progress.md` was read as the runbook lane's evidence set.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 The pairing page | shipped-as-specified, with one unstated behaviour change: `CodeField` (`bind:complete`), the error under the field, the two commands as `Chip`s, `PairingView.svelte` out of `vocabulary-baseline.json` (3 lines removed). **`CodeField` is exact-8 (`complete` false at nine characters); the old rule was "at least eight"**, and no test asserts what is sent to `apiClient.pair` (the client sends `code.trim()` raw, lowercase included, `client.ts:3128`, leaf) | `PairingView.svelte:138-158` (leaf); `git diff --stat` |
| D2 Revoked and listener-off pages | shipped-modified: `ListenerOffView` reconnect is the one primary, no `ErrorState` (a recorded finding, see L1-3); **`RevokedView` maps reason keys the wire never sends (L3-1)**; the "commands are Chips" clause has nothing to apply to (L1-3) | `RevokedView.svelte:25-75` (re-read); `ListenerOffView.svelte:22-24,56` (leaf) |
| D3 What PLAN-131 left | shipped-as-specified in code for ten of eleven items, weak guards for three: one reconnect (`createReconnectLoop`, `runtime.ts:317-362`), one figure-height constant, the `shellGone` test (**likely does not fail on revert**, L3-2), the held-figure retry (**unbounded**, L3-3), the rail-bear guard, the shared `textFiles.ts` walk with the WebP signature test and a `data:image/webp` scan (narrower than the spec's "no `data:` URL"), the BrandMark runtime refusal, the hero loaded only where it shows, the dev-page fixtures by test id, the blank-title case of `NeedsItemCommandsTest` (a pin, no bug existed: L1-2) | see Layer 2 |
| D4 Browser evidence | shipped by presence: 32 PNG files (4 views × 2 viewports × 2 locales × 2 themes) under `.plan/temp/rb-157-web-views-entry-pages/screenshots/` (counted by `ls`); untracked, not viewed by this analysis, **no baseline set** although the spec asks for "baseline and after" | `ls` (re-read) |
| added-unplanned | `EmptyState.svelte` (+ a `testid` prop, for the dev-page fixtures), `MascotFigure.svelte` (+6: lazy and eager) | `git diff --stat a4fa36fa fe44e6a7` |

**"4 of 4 met" is the lane's count;** by the spec's own rule D2 is not fully met (its reason-line table is satisfied by tests over invented inputs) and D3's guards are partly vacuous.

## Three-Layer Analysis

The landing was read on three layers by one read-only leaf, and the orchestrator re-read the points recorded as `checked` below at `fe44e6a7`. By reading, no build, no test run, no write.

- **Dispatch:** one read-only leaf (`execution-context-level-3`, plan-less) — reason: every full ship gets its own leaf; four views, a new store helper and eleven residue items are the reading depth the dispatch rule exists for.
- **Verdict:** layer 1 findings (6, minor); layer 2 findings (6, minor) and not shown by reading (3); layer 3 findings (1 major confirmed, 1 major suspected, 6 minor). No blocker.
- **Re-read by the orchestrator** (`file:line` at `fe44e6a7`): `RevokedView.svelte:25-80`; `runtime.ts:745-755` and `client.ts:5200-5208` (the wire reasons); `runtime.ts:315-362`; `layout.test.ts:45-60,100-125`; `+layout.svelte:247-257,308-320`; `heldFigure.ts:25-45`; the `initRuntime` grep of `lib/views` (empty); `NeedsItemCommandsTest.java:205-238`; `RevokedInfo` (`runtime.ts:55-60`); `entry.adoc:59`; the catalogue keys `web.revoked.reason.*` (all six exist in both locales, none new); `progress.md:5-14`. The remaining leaf points (the pairing client call, the hero constant, the MascotFigure and runtime test internals) are **unverified leads** of the orchestrator.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence (file:line) |
|---------|-------|---------|----------|----------------------|
| L1-1 (minor) | D2's Done-when "each reason line of the table in `entry.adoc` § `#revoked`" is not satisfiable from the wire: the runtime ends the stream with `device_revoked` (with channel, device and time) or `device_expired`; `RevokedInfo` carries `reason`, `revoked_by`, `revoked_at`, `details` only | checked | refutes | `entry.adoc:59`; `runtime.ts:55-60`; `client.ts:5205` (re-read) |
| L1-2 (minor) | D3's flaky-test item is stale: the title draw of `NeedsItemCommandsTest` is already `Generators.letterStrings(4, 12)`; the lane added the blank case, which passes at the base | checked | refutes | `NeedsItemCommandsTest.java:211,224-236` (re-read) |
| L1-3 (minor) | D2's "the commands to copy are Chips" and "`ErrorState` for the unreachable page": `RevokedView` has no command, the others interpolate the command into a catalogue sentence; the lane recorded the `ErrorState` finding **only in `progress.md` (line 8)**, the spec says "record it as a finding in the pull request" | checked | confirms the spec's own decide-at-outline clause, refutes the recording | `progress.md:8` (re-read); PR body (the finding is in the PR body's D2 section, not in the landing message) |
| L1-4 (minor) | D3's "no `data:` URL" against the implementation: `data:image/webp` only | not_shown_by_reading beyond the leaf's read | refutes | `MascotFigure.test.ts` (leaf) |
| L1-5 (minor) | D4 "baseline and after screenshots": one set exists | checked | refutes | `ls` of the screenshots (re-read: 32 files, one naming set) |
| L1-6 (info) | The PLAN-148 residue (a long command not cut by `Chip`'s `max-inline-size: 100%`) is covered by no test and no finding | checked | none | leaf |

**Layer 1 verdict:** findings — 6.

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained file / unborne claim | Verdict |
|-------------|------------|--------------------|---------------------------------|---------|
| D1 | `PairingView.svelte:138-158` (leaf) | `PairingView.test.ts` (case and dash cases, below eight, the error, the chips), `PairingView.scenarios.test.ts` (`it.each`), `pairing.spec.js` | no assertion on what `pair` is sent; exact-8 against the old ">= 8" | findings (minor) |
| D2 | `RevokedView.svelte:25-75` (re-read), `ListenerOffView.svelte:22-24,56` | `RevokedView.test.ts` (one case per reason, **over invented inputs**); the `ListenerOffView` test spies `retryRuntime` and checks the class `btn--primary` ("exactly one primary" is asserted as one button's class) | the periodic reconnect is asserted by no test | findings (minor) |
| D3 one reconnect | `runtime.ts:317-362`; `lib/views` has no `initRuntime` (grep, re-read) | the `ListenerOffView` click test (fails on a revert to `initRuntime`); `runtime.test.ts:1018-1036` is weak (`toBeDefined`; no `stop()`, in-flight guard or swallowed error); `vi.useRealTimers()` not in a `finally` | `start()` is not idempotent: a second call overwrites `interval` (L3-5) | findings (minor) |
| D3 figure constant | `MascotFigure.svelte:9-10` | the test asserts `FIGURE_HEIGHT \|\| ENTRY_FIGURE_HEIGHT` is 180; `ENTRY_FIGURE_HEIGHT` is dead; no test fails if a view re-declares 180 | | findings (minor) |
| D3 shellGone | `layout.test.ts:105-123` (re-read) | **probably vacuous** (L3-2) | | findings (major, suspected) |
| D3 heldFigure | `heldFigure.ts:30-43` (re-read) | a retry-after-failure test; the `!ok` path is not asserted | | done, unbounded (L3-3) |
| D3 rail bear | `NavRail.test.ts` guard | yes (compares the rule with `mascotFiles('welcome','bust')`) | `NavRail.svelte` still hardcodes the ratio | done |
| D3 test tree | `src/test/textFiles.ts` shared `walk` and `RASTER_IMAGE`; WebP signature test; dev page by test id (`EmptyState testid`) | yes | the build-dir scan is skipped silently when `build/` is absent (no non-vacuity guard) | done, narrowed |
| D3 BrandMark | `BrandMark.svelte:42-44` throws at init | "refuses at runtime" | a later `size` change is not checked | done |
| D3 hero | `PairingView.svelte:23-51,178` (leaf) | a narrow-viewport test | the initial value reads `innerWidth`, the frame is the CSS container | done |
| D3 flaky test | `NeedsItemCommandsTest.java:224-236` (re-read) | passes at the base | pins behaviour; no draw changed | done |
| D4 | 32 files | n/a | not in the diff; no baseline | by presence |

**Claims of the landing message the diff does not bear out:** the numbered list "items 1–11 resolved" is **not the spec's item list**: its items 3 ("size-32 test strengthened"), 7, 9, 10 and 11 ("clean generated random input for pair scenarios") have no counterpart in the diff, and it omits the shellGone test, the NavRail guard, the dev-page test ids and the data-URL scan. "5425 tests" is the gate at `b71bea73` (`progress.md:13`, re-read); four later commits changed tests (`b8fc5e3f`, `e9743997`, `2d37d9bd`, `f27db4d7`) and the file records no re-run. **No `-Pe2e` run is recorded** although `pairing.spec.js` changed and the spec's Standing Constraints require one at the head.

**Layer 2 verdict:** findings — 6 (minor), not shown by reading — 3 (the 5425 figure at the head; the e2e run; the screenshots' content).

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `RevokedView.svelte:25-75`, `runtime.ts:745-755`, `client.ts:5200-5208` (all re-read) | the real wire reasons `device_revoked` and `device_expired` | the branches `device`, `terminal`, `others`, `self`, `idle` match reason values the runtime never sends (the wire carries `device_revoked` plus channel, device and time: `entry.adoc:59`; `RevokedInfo` has no channel field), so the table's lines and their five tests cover invented inputs and a real revocation never shows `web.revoked.reason.terminal`, `.others`, `.self` or `.idle`; a `device_expired` shows the pre-existing `devices.revoked.reason` line, not the table's idle line. The reading of `device`/`time` from the object goes through `as unknown as Record<string, string>` (not on the wire type), and `idle` passes an unused `days: 30` (the catalogue hardcodes 30) | major |
| L3-2 | `layout.test.ts:50-57,105-123`; `+layout.svelte:247-257`; `runtime.ts:737` (all re-read) | the `beforeEach` already sets `messagesStore` to `loadMessages('en')`, so `waitFor(get(messagesStore)['web.frame.brand'])` is satisfied at once, before the pending catalogue resolves; `initRuntime` does not call `getConnectionStatus` (grep of `runtime.ts` finds only the declaration of `initRuntime`), so `expect(getConnSpy).not.toHaveBeenCalled()` is vacuous; `isInitialized false` can be read before the continuation runs | the test can pass with `if (shellGone) return` (`:257`) removed: the guard is **not pinned** | major (suspected; not shown by reading) |
| L3-3 | `+layout.svelte:312-318`, `heldFigure.ts:30-43` (re-read) | a figure that fails to load (404 or a lost connection) while the shell is up | the `$effect` reads the whole `$runtimeStore`; a failure now resets `requested`, so **each store update refetches the figure, with no backoff or cap**; how often the store updates is not shown by reading | minor (potentially major) |
| L3-4 | `RevokedView.svelte:25-75` (re-read) vs the base `RevokedView.svelte:18,40` | `device_revoked` with an empty `revoked_by` | before: "…by the terminal client" (hardcoded English fallback); now: "Its pairing is no longer accepted." — a displayed-text change in a "pure restyle" | minor |
| L3-5 | `runtime.ts:343` (re-read) | `start()` called twice | the first interval leaks; both views call it once in `onMount`, so it is latent; the helper has no return-type JSDoc | minor |
| L3-6 | `PairingView.svelte:23-31` vs the CSS container query (leaf, unverified lead) | viewport over 800 px with the frame under 800 px (the rail present) | the initial `eager` is true and the hero is fetched, then flips to lazy; the 800 and 520 constants are hardcoded where the CSS uses `50rem` | minor |
| L3-7 | `MascotFigure.test.ts` data-URL test (leaf, unverified lead) | `build/` absent | the scan covers `src/` only and only `data:image/webp` | minor |
| L3-8 | `runtime.test.ts:1018-1036` (leaf) | a failing assertion | `vi.useRealTimers()` is not in a `finally` | minor |

**Layer 3 not shown by reading:** whether `pairing.spec.js` passes (its new "Copied" assertion depends on the clipboard permission or the `execCommand` fallback in headless Chromium); whether the server accepts a lowercase code (the client sends it raw); the visual quality of the screenshots; whether any of the above defects pre-dates the diff.

**Layer 3 verdict:** findings — 8 (1 major confirmed, 1 major suspected, 6 minor), not shown by reading — 4.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md § Dependencies | no dependency or plugin without asking | conforms | no `package.json`, lock file or POM in the 27-file diff (`git diff --stat`) |
| CLAUDE.md § Web Standards | no `innerHTML`/`{@html}` with runtime text; no timer wait in a test | conforms | leaf grep of the diff; the lane's self-review replaced one timer wait in `layout.test.ts` |
| spec | every new text from the catalogue in en and de; the text-guard baseline does not grow | conforms | no new key (`pm-api` untouched); `text-guard-baseline.json` not in the diff; `vocabulary-baseline.json` shrank by 3 lines |
| spec | the pairing code is never logged or echoed outside its field | conforms | leaf: no log, no echo; the QR encodes it by design (pre-existing) |
| spec | a pure restyle: no change to what a page shows | deviates | the revoked-by fallback line (L3-4); exact-8 (D1) |
| spec | "record it as a finding in the pull request" | deviates | the `ErrorState` finding is in the PR body's D2 section and `progress.md`, not in the landing message (partially conforms) |
| CLAUDE.md | zero Sonar findings; full verify; e2e at the head | not_shown_by_reading | Sonar gate OK; no e2e run recorded |

## Metrics and Anomalies

- Tokens: `unknown` (harness `opencode` measured none); `plugin_version` `unknown`; executor `0.1.2183`. Gate duration 716 s (the progress file).
- Anomalies: the landing's "items 1–11" list does not match the spec's items; the build gate ran at `b71bea73`, four commits before the head; a self-review round of five items; `guard: none on this harness (unenforced)`, so nothing is reported as "all rules kept".

## Routing and Merge Behavior

- Review: CodeRabbit reviewed with four findings, all handled in `f27db4d7` (dynamic WebP asset discovery, the localized actor fallback, the reactive `$t` dependency in `RevokedView`, the `shellGone` unmount test). `runbook_lane review-gate --pr-number 535` re-read on 2026-11-22: `unresolved_review_threads=0` (the landing says 0, equal), `review_body_findings_total=4` (the landing says 4, equal), `handled=0` from the bare call (`merge_allowed=false, body_findings_undisposed`): the known artefact of the bare call; `review_body_findings_handled` is **re-verified at 4 of 4** from the four `finding:` lines of the progress file.
- CI/merge: merge commit `fe44e6a7` (`ci pr view`: merged, base `main`).
- Sonar: the landing states `sonar_open_new_issues=0` and `quality_gate_status=ok`. Re-read now: `sonar_rest gate-status --pr 535` is `OK` (new coverage 91.4, duplication 0.0, hotspots reviewed 100); the open count of a merged PR cannot be re-measured; nothing contradicts the lane's `0`.
- Overlap observed: PLAN-157 touched files on the surfaces of the running **PLAN-156** (`routes/dev/components/+page.svelte`, `+page.test.ts`, `EmptyState.svelte`; its PR #536 is open and must rebase), of the staged **PLAN-85** (`PairingView.svelte`, `RevokedView.svelte`, `ListenerOffView.svelte`: its spec must be re-read at `fe44e6a7`), and shared files `runtime.ts`, `textFiles.ts`, `layout.test.ts`, `vocabulary-baseline.json` (a conflict-prone JSON). The overlap was predicted by the gate (the corpus lists these surfaces), so no declared surface was wrong.

## Reconciliation Actions

- [ ] row `status` → `shipped` — `orchestrator queue --transition PLAN-157 --status shipped`
- [ ] row `pr` stamped `535`
- [ ] row `landing` stamped `landings/PLAN-157.md`
- [ ] row `plan_marshall_plan_id` stamped `rb-157-web-views-entry-pages` (the sender's `{plan}`)
- [ ] epic.md: Open Defect for the incomplete landing and the layer-3 findings (PLAN-157 entry)
- [ ] resume anchor and `queue-view.md` regenerated

## Follow-Ups

- **Layer-3 major L3-1 (and the suspected major L3-2) are reported to the operator at once and are not fixed inline;** a fix plan is staged only on the operator's word. The minors ride the same decision (the unbounded figure retry L3-3 should be read with it).
- PLAN-85 (staged) must re-read `PairingView.svelte`, `RevokedView.svelte` and `ListenerOffView.svelte` at `fe44e6a7`: its spec predates this landing.
- PLAN-156 (running, #536) rebases onto `fe44e6a7`; the dev-gallery files and `EmptyState.svelte` are the expected conflict surface.
