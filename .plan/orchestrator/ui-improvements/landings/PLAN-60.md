# Landing Analysis: PLAN-60 — Web settings in one place

epic: ui-improvements
workstream: WS-03
pr: #457 (https://github.com/plan-marshall/pm-mcp-clients/pull/457)

> Landing record, written by `analyze` on 2026-11-16 from the inbox message
> `rb-60-web-settings-one-place-001.md` (revision 1, amended 17:15:21Z; a lead, not a fact), the lane's progress file
> `.plan/temp/rb-60-web-settings-one-place/progress.md`, git, the provider, and a read-only three-layer leaf.
> The orchestrator re-read the points marked "re-read" on `66050343`.

Merged as `6605034343c5f3a8e63da135b4b62b4406d9a8df`, merge head `8530ad09`, 38 files, +2931/-1886. Sender `rb-60-web-settings-one-place`. Lane: runbook. Harness `opencode`, model `deepseek-v4.1-flash/high` (as stated, not corroborated).

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 — one settings view, redirect, every tab | shipped | `routes.ts:705-708` maps `/settings/machine` to `/settings?tab=setup`; `onboarding.spec.js:146-175` and `devices.spec.js:22-25` fail on the old code (no tab roles, no `settings-panel-*` ids) |
| D2 — forms: Save disabled until change, reset | shipped, two re-grounding clauses missing | `SettingsView.scenarios.test.ts:671-712` assert Save disabled, enabled after a change, and the changed mark; **no leave guard** (the re-grounding's "a guard when the operator leaves a changed form"; no `beforeNavigate`, no confirm, `SettingsView.svelte:1052-1553`); the device rename and the replace-kind input are raw `<input>` (`:1249`, `:1622-1628`), not the field system; "Use the inherited value" reverts to the last-read record (`:759-764,837-843`) while machine settings have no inherited default |
| D3 — conflict notice | shipped; Done-when `test-first` was unmeetable as written | the notice predates the plan (PLAN-101; the spec's own re-grounding says "done, carried"), so `mutation-after` is the only form: `SettingsView.scenarios.test.ts:241-264` would fail if the notice were reverted |
| D4 — the cleanup record's conflict, save, texts | shipped; the lane's "not fully met" is wrong for the added-key test | `scenarios.test.ts:515-657` assert the harm (non-record `current_value` keeps the stored values, the retention minimum sentence, the added key `custom_after_days` survives `saveCleanup`, a store refresh is not read as a change). The added-key test **was** meetable: at the base `saveCleanup` builds the record from a five-key list (`MachineSettingsView.svelte:220-227`), so it fails there; the missing failing run came from the lane's commit order (D2's spread first), not from the spec |
| D5 — the device list's reads | shipped, with a source-text pin | six tests, not the five counted: revoked row stays gone (`Devices.scenarios.test.ts:209`, reverting `loadSequence += 1` at `SettingsView.svelte:313`), event during a load (`eventEpoch`), status abort, unmount aborts both reads, the bounded status read by fake timers; the destroy-guard leftover is pinned by `fs.readFileSync` of the component source plus `toContain` (`:281-289`), a text pin that stays green if the guard is moved or left in dead code |

`deliverables_done=3` of 5 understates what shipped: by this reading D1, D3, D4 and D5 are delivered and the two Done-when conditions the lane called unmet are the spec's (D3) and the lane's commit order (D4). The lane's own count is the one the ledger stamps; the difference is recorded here.

**Files the diff touches that the Expected Surface does not list:** `AppRouter.svelte` and its test, `OnboardingView.svelte`, `history.ts`, `nav.test.ts`, `tokens.test.ts`, `routes.test.ts`, `runtime.test.ts`, `pm-web-app/README.md`, `text-guard-baseline.json`, eight e2e specs beyond onboarding and devices, two new test files; and the Expected Surface lists `common.*` and `devices.*.properties`, which the diff does not touch. All trace to the tab move and the merge of the two views.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf per landing — reason: the Dispatch Decision Rule; 38 files across Svelte, TypeScript, e2e specs, catalogues and design documents. The orchestrator re-read four points on `66050343`.
- **Verdict:** layer 1 findings (two deliverable lists disagree; D3 unmeetable as written); layer 2 findings (leave guard missing, partial orphan cleanup, a source-text pin, a stale design table); layer 3 findings — no blocker, **1 major (re-read)**, 11 minor, 3 info.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | D3 can be demonstrated test-first | checked | refutes | the notice exists at the base (spec lines 170 and 181) |
| L1-2 | D4's added-key Done-when is unmeetable | checked | refutes | it fails at the base (`MachineSettingsView.svelte:220-227`); the lane's "no failing run of its own" is its commit order |
| L1-3 | one list of deliverables | checked | refutes | the first list has five (D3 a separate conflict notice), the 2026-11-10 re-grounding four and renames "Reset to inherited" and "Apply mine again" to "Use the inherited value" and "Apply yours again"; the lane followed the five-list and the renamed texts |
| L1-4 | the cited lines are current | checked | confirms (premises) | `saveCleanup ~216-224` is `199-227`, `runtime.ts 1086-1099` is about `1486-1506`, `texts-and-languages.adoc:224` is `:300/301`; the factual premises (a) at `MachineSettingsView:244` and the missing German tokens key hold |
| L1-5 | the Expected Surface lists what the plan touches | checked | refutes | see the list above |

**Layer 1 verdict:** findings — a spec with two inconsistent deliverable lists and one Done-when no run could meet.

### Layer 2 — Implemented

| Item | Verdict | Evidence |
|------|---------|----------|
| Redirect, tab derivation, unknown `?tab=` falls back to setup | borne out | `routes.ts:705-708`, `SettingsView.svelte:106-110`, `routes.ts:601-606`; tab changes are replaces (`runtime.ts:1583`) |
| the "orphaned catalogue keys removed" claim | **partly false** (re-read) | `web.settings.devices.manage` is gone from both packs, but `web.settings.devices.label` and `.count` remain in both (`settings.en/de.properties:4,82`) with no reference in code; the design's Texts table still lists the deleted `manage` (`machine-settings.adoc:364`) and none of the new keys |
| the contrast audit | narrowed | `theme-readability.spec.js:502,844,848,976` cover Setup and Access only, by positional `nth()` selectors; Credentials, Consents and Retention (the marks, Save, the rotate dialog) are not audited |
| untested behaviours the design states | missing tests | the `conflict.no_record` refusal (`SettingsView.svelte:861-866`), `move` disabled off loopback (`:565-572`), the listener-off confirmation (`:1320-1324`), `summary_unknown` |
| stale captures | borne out | `system-view.txt`, `system-printed.txt`, `system-view.svg`, `texts-and-languages.adoc:301` read "tokens 2.02 M (1.90 M in · 0.12 M out)"; `TokenFormat.formatSplit` prints each part in its own unit (`120 k`), so the captures differ from what `pm-operator` prints, as the lane admits |
| `AppRouter.test.ts:26` | stale | a `vi.mock` of the deleted `MachineSettingsView.svelte`; `theme-readability.adoc:208` still lists it |
| design `machine-settings.adoc:211-214` "Changed Meanwhile" | borne out, one divergence | it states the wire form, the changed set, the record the page writes and the minimum; code also treats `invalid_arguments` as a value refusal (`SettingsView.svelte:738`) and always passes the retention minimum even when the refused field was the reminder (`:813-818`) |
| `text-guard-baseline.json` | shrank only | the `MachineSettingsView` entry removed, nothing added |

Not shown by reading: the failing-run logs, the 16 baseline and 48 after pairs and the redirect cells (artifacts untracked under `.plan/temp/playwright-mcp/`), the four `data-table.spec.js` failures and `ConsoleSettingsTranscriptTest.machineSettings` failing "on a blank line" locally (the diff touches no `pm-operator` file), Sonar (the Sonar MCP search for pull request 457 returns no issues).

**Layer 2 verdict:** findings — no major; the leave guard, the orphan cleanup, the text pin, the audit and the design table are the gaps.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `SettingsView.svelte:928-943,960-983` (re-read: `readRevision` is assigned at `:627`, `:753`, `:822` only; the success path of `withdraw()` and of `saveToken()` never refreshes it) | a consent withdrawal or a token replace, then a documents or cleanup Save or a second withdraw | the write goes out at the revision the page read first; the leaf states that the credential write bumps the revision, so the next write gets a spurious 409 and the dialogue shows identical current and own values (the revision bump itself was not re-read; the absence of the refresh was); the only test (`scenarios.test.ts:134-156`) checks the first call | **major** |
| L3-2 | `SettingsView.svelte:1176-1183,1541,1461` | double click on Save (documents, cleanup) or Withdraw | Save stays enabled while the write is in flight (the changed flag clears after the await, `:753-756,821-824`); the second click goes out at the same revision and conflicts with its own change | minor |
| L3-3 | `SettingsView.svelte:338-341,427` | a read already in flight (the retry after a timed-out revoke, `LanListenerModal onChanged`) when a rename or the listener-off completes | the late read reverts the new label or the "off" state (the class D5 fixed for revoke); `saveRename` also reads `renameValue` after the await | minor |
| L3-4 | `SettingsView.svelte:887-891,727,932` | a consent conflict with a missing or empty `current_value` | shown as "Remembered in the other client", a statement the page does not know | minor |
| L3-5 | `SettingsView.svelte:1492-1500` (re-read: the `NumberField` has `allowNever` and no `min`) | `-1` typed with Never unchecked | passes the pre-check (`:777`, `days === NEVER`) and saves `plans_after_days = -1` (never) while the field shows `-1`; contradicts the comment at `:620`; `7.5` is refused with the minimum sentence, which names the wrong rule | minor |
| L3-6 | `SettingsView.svelte:1117-1119,1349,1436,1486`; `runtime.ts:1486-1495` | a failed settings read | shown as "No aspect is recorded yet." with no error state and no retry | minor |
| L3-7 | `SettingsView.svelte:1079-1084,915-923,877` | any tab switch; Keep current or Apply yours again | `error` and `notice` are page-wide and survive a tab switch (a Retention refusal shows on Setup); `applyLocal` discards unsaved edits in the unrelated form | minor |
| L3-8 | `Tabs.svelte:125-136`; `SettingsView.svelte:1107-1112` | screen reader | tabs carry no id, panels use `aria-label` not `aria-labelledby`, `aria-controls` of the four inactive tabs names panels that are not rendered | minor |
| L3-9 | `SettingsView.svelte:1702-1704` (re-read) | — | `max-width: 64rem; margin: 0 auto` against the Standing Constraint "no own page width" (VD-S-06) | minor |
| L3-10 | `SettingsView.svelte:1019-1022,985-1003,1048,979,1151` | an unknown kind, step, channel or a failed token write | raw words reach the page (the kind, the aspect key, the channel, the runtime's raw cause); the loopback hint "Move is not offered on the network" is always shown, even on a loopback session | minor |
| L3-11 | `SettingsView.svelte:794` | — | dead local `recorded` in `saveCleanup` | info |
| L3-12 | `SettingsView.svelte:565-572,846-879`; `routes.ts:498-500` | — | confirmed sound: `move` disabled off loopback (client side only), revision-bound writes, the conflict resend, unknown `?tab=` dropped | info |
| L3-13 | `SettingsView.svelte:1648-1654` | — | confirmed: `SecretField` is `type=password`, the secret is cleared on success and on close; no `innerHTML`, `{@html}` or sleep-style timer in any changed view or test | info |

**Layer 3 verdict:** findings — no blocker; 1 major (a stale revision after a non-form write), 9 minor, 3 info.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Web | no `innerHTML`/`{@html}`; no timer waits; ES modules, flat ESLint | conforms | no hit in the changed views or tests; `waitFor`, `findBy`, `advanceTimersByTimeAsync` only |
| spec Standing | text-guard baseline only shrinks | conforms | `text-guard-baseline.json:47-50` |
| spec Standing | design system v2: no own page width | deviates | L3-9 |
| spec Standing | the design document is updated in the same PR | deviates | `machine-settings.adoc:211-214` updated; `:364` lists a removed key; the Texts table omits the new keys; the Layout sketch `19-57` unchanged |
| spec Write-Boundary | no write under `.plan/orchestrator/` | conforms | re-read: `git diff --stat 66050343^1 66050343 -- .plan` is empty |
| CLAUDE.md Code (Java) | — | not shown by reading | no Java file in the diff |

## Metrics and Anomalies

- Tokens: `unknown` (unobserved, not zero); `plugin_version` is `0.1.2135` here, `total_tokens` the one missing key. Executor `0.1.2135`. The message is revision 1 (amended 17:15:21Z); the first revision is not kept, only the amended body was read.
- Three `pm-web-e2e` runs went in-process on the operator's answer (ports 20600/20601); the routed daemon environment carried another plan's port block. One whole-tree `verify -Ppre-commit` hit its 960 s bound and was replaced by the per-module form.
- Shell-rule breaches the lane owns: repeated `grep`/`sed`/`ls`/`cat`/`head`/`wc`, two chained commands, one `git add` from the main checkout (staged nothing).
- The resolve's `inbox_read_command` carries `--plan-id 60-web-settings-one-place`, which the orchestrator refuses (`invalid_target_plan`); the lane used `plan-60-web-settings-one-place`. The same plan-id-starting-with-a-digit defect PLAN-24's lane reported.
- The base-sha tree for the D3 and D4 evidence could not be created (`executor_in_worktree: absent`).

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `7fcbc286` (16 inline findings and 1 nitpick, 17 body findings); the lane records 15 inline plus the nitpick fixed in `8479807f` and two answered with a reason. Re-read: `review-gate --pr-number 457` shows `unresolved_review_threads=0` and `review_body_findings_total=17`, the same as the landing; 17 `finding:` dispositions in the progress file. The merge conforms to the standing rule.
- Sonar: `sonar_open_new_issues=0`, gate `ok`; one `typescript:S5906` at `8479807f` fixed in `d449ea2f`; the Sonar MCP search for pull request 457 returns no issues.
- Landing check: `complete: false`, `missing_keys` `total_tokens`; recorded as an Open Defect.
- `.plan`: #457 carries no file under `.plan`.

## Reconciliation Actions

- [x] row `status` → `shipped` — `queue --transition PLAN-60 --status shipped`
- [x] row `pr` stamped — `457`
- [x] row `landing` stamped — `landings/PLAN-60.md`
- [x] row `plan_marshall_plan_id` stamped — `rb-60-web-settings-one-place`
- [x] epic.md: Open Defect `PLAN-60 landed`
- [x] resume anchor and `queue-view.md`
- [x] the inbox message archived on consume

## Follow-Ups

- **A PLAN-60 residue plan** (named, NOT staged): L3-1 (refresh `readRevision` after a withdraw and a token write, with a test on the second call), L3-2 and L3-3 (double submit, late reads after rename and listener-off), the retention `min`, the failed-read state, page-wide error and notice, the tab ids, the own page width, the raw words, the leave guard, the orphaned keys and the design Texts table, the stale `AppRouter.test.ts` mock, the missing tests.
- **PLAN-64** (`Emit after PLAN-60 and PLAN-150`) and **PLAN-85** (`Emit after PLAN-60, PLAN-144 and PLAN-150`) lose the PLAN-60 gate; PLAN-85 still waits for PLAN-144.
- PLAN-52, PLAN-49 and PLAN-64 share catalogues and `runtime.ts` with this view; the residue plan should come before another plan touches `SettingsView.svelte`.
- The CLI token format (`TokenFormat.formatSplit`) still differs from the design's sentence: left to the tokens change in `pm-api`, named by the lane.
