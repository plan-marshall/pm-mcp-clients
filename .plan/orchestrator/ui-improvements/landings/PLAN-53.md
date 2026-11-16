# Landing Analysis: PLAN-53 — Web start wizard with a working gate

epic: ui-improvements
workstream: WS-03
pr: #455 (https://github.com/plan-marshall/pm-mcp-clients/pull/455)

> Landing record, written by `analyze` on 2026-11-16 from the inbox message
> `rb-53-web-start-wizard-001.md` (a lead, not a fact), the lane's progress file
> `.plan/temp/rb-53-web-start-wizard/progress.md`, git, the provider, and a read-only three-layer leaf.
> The orchestrator re-read the points marked "re-read" on `b2ac6204`.

Merged as `b2ac620499aa96904769165cfd5a1658abd9a550` (parent `8f9e8cf4`), merge head `941fe759`, 21 files, +1112/-666.
Sender `rb-53-web-start-wizard`. Lane: runbook. Harness `opencode`, model `deepseek-v4.1-flash/high` (as stated, not corroborated).

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 — the gate: a gate that does not answer shows its cause after 20 s; `/start` without a project | shipped, with a weak pin | `StartView.svelte:47,282-288` (`GATE_BOUND_MS`, `AbortSignal.timeout` plus `timeoutMs` to `openEntry`), spinner `:961-967`, cause and retry `:968-980`; no-project pattern tested at `StartView.test.ts:89-100` and `start.spec.js:148-157`. The 20 s value itself is pinned only as the argument passed (`StartView.test.ts:75-86`), and the e2e waits up to 30 s without asserting the cause is absent before 20 s (`start.spec.js:133-146`) |
| D2 — steps 1 to 3: one primary per step, Back works, Abandon asks first, one Add menu | partly shipped | menu, footer primary, Back from ready, recipe and parameters, and the Abandon confirmation are delivered; "one primary per step" fails at the recipe step (and at the adopt step, which the lane admits); Back is absent at three steps; `Field` and `RadioGroup` were not adopted |
| D3 — lazy entry | dropped by its own condition | re-read: `EntryOpenRequest.java:37` has no dry-run field, `StartRegistry.java:542-565` creates the entry in `open()`, `openEntry` is the client's only route (`client.ts:3102` at the merge commit, 3096 at the base) |

`deliverables_done=2` of 3 is borne out. Every file of the diff traces to a deliverable or a standing constraint (RecipeReadyView, EpicAdoptView, DecompositionReviewView, `errors.ts`, `pairing.js`, `showroom.spec.js`, `common.*.properties` are all named in the PR body); `text-guard-baseline.json` is untouched.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf per landing — reason: the Dispatch Decision Rule; the change spans Svelte, the design documents, the catalogues and the e2e specs. The orchestrator re-read seven points on `b2ac6204`.
- **Verdict:** layer 1 findings (two internal spec contradictions, an Objective the contract cannot meet); layer 2 findings (one major: "one primary per step" is not true; one test gone vacuous); layer 3 findings — no blocker, no major; 13 minor, 2 info, 2 not shown by reading.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | D3 is unconditional in "Deliverables" but conditional in the 2026-11-10 re-grounding | checked | refutes | spec; the Done-when set itself is consistent |
| L1-2 | "`stores/start.test.ts` is absent; created by this plan" | checked | refutes | the file exists at the base; Claim 5 says so |
| L1-3 | the lane's three D3 citations | checked | confirms | re-read, see above |
| L1-4 | "a client bound with a signal passed to `request()`" makes the 20 s gate | checked | refutes | the client's own 15 s deadline (`client.ts:4023-4024`) would fire first; the lane had to use the `timeoutMs` parameter (`client.ts:3105-3106`) |
| L1-5 | Objective "Back on every step" and Done-when "Back works" | checked | refutes | the contract offers no Back at `start.sources`; the spec was not amended |
| L1-6 | "Discard (overflow)" vs the re-grounding "Discard is Abandon"; "Abandon asks first" is in the re-grounding only | checked | refutes | spec |

**Layer 1 verdict:** findings — a spec whose Objective is partly unmeetable and whose premises needed correcting by the lane.

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained file / unborne claim | Verdict |
|-------------|------------|--------------------|----------------------------------|---------|
| D1 gate | `StartView.svelte:47,282-288,961-980` | `StartView.test.ts:89-100` and `start.spec.js:148-157` fail at the base; the 20 s pin is weak (`:75-86`, `start.spec.js:133-146`) | none | checked |
| D2 menu, Abandon | `SourceList.svelte`, `StartView.svelte:1414-1427,1509-1518` | `StartView.test.ts:111` (dialog appears), scenarios `~745-750` (act not called on the adopt route); **no test that confirming calls `act('abandon')` and navigates**, no e2e | none | checked |
| D2 "one primary per step" | recipe step `StartView.svelte:1023-1059` (re-read: rows are `variant="secondary"`, no footer primary); adopt step has none either | the e2e counts the primary at choose, sources, ready and parameters only | the lane's "one primary per step" is **not borne out** | refutes |
| D2 Back | Back at ready, recipe and parameters only (`StartView.svelte:230-233,523-527`) | `start.spec.js:59-60` pins its absence at sources | "Back on every step" is not delivered (the lane concedes ①) | refutes |
| D2 description card | `StartView.svelte:1208`: the editor shows only with no sources or after picking Description | none | "the description as a card of its own with one Add" is not standing when sources exist | refutes |
| D2 Fields | raw `input`/`textarea`/radio remain (`:1237,1268,1291-1315`); no Enter-to-submit | none | `Field`/`RadioGroup` of the re-grounding not done and **not listed as residue** | refutes |
| tests | `StartView.scenarios.test.ts:649-650` assert that `add-pull_request` and `add-local_changes` are not offered — those testids exist nowhere after the menu change (re-read) | the "kinds that do not combine are not offered" rule lost its test | vacuous | refutes |

Standing constraint "no plain text button": `ConfinedFilePicker.svelte:111-209` and `GrantModal.svelte:22-28`, both rendered by `StartView`, keep plain `<button>`s. Four start-flow components in the Expected Surface (`SourceIntakePanel`, `RecipeSelector`, `GitSourceIntakeModal`, `LessonSourceModal`) are imported by no view, only by their tests: dead code, untouched.

Catalogue: all eight new `start.*` keys and the three `common.error.no_project_to_start.*` keys exist in both packs; `start.gate.slow` hard-codes "20 seconds" in a catalogue shared by both clients while the design says "20 s in the web app".

Not shown by reading: the Sonar read (the Sonar MCP search for pull request 455 returns no issues), the build and e2e runs, the browser check and its screenshots, the visual placement of the Add menu, the text-guard result for the new strings.

**Layer 2 verdict:** findings — 1 major (one primary per step), 4 minor traces, 1 vacuous test.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `StartView.svelte:282-288,1441-1449` | the retry after the 20 s bound, or leaving the view while the gate waits | a second `openEntry` creates a second entry; the request is not aborted on destroy; the late answer is dropped by the `destroyed` check, so the entry exists server-side and no client addresses it; the design admits only the retry case; D3 was dropped so there is no mitigation (re-read the call site at `:282-288`) | minor |
| L3-2 | `StartView.svelte:764-771` | `act('abandon')` fails | re-read: `navigateTo('projects')` runs after `await act('abandon')` whatever it returned; the dialog said "The entry and its sources are deleted"; the error is dropped by the navigation | minor |
| L3-3 | `StartView.svelte:270,961-974` | resume path: `getEntry(entryId)` times out at its 15 s client bound | the gate text "did not answer within 20 seconds" and "Checking the project's setup…" — the wrong number and the wrong subject | minor |
| L3-4 | `StartView.svelte:1414-1427` | — (re-read) | the overflow `RowMenu` is labelled "Abandon" and holds one item "Abandon"; the trigger is not an act, contradicting "no two acts with the same label" | minor |
| L3-5 | `StartView.svelte:1047-1055,1018` | every recipe row | each row's button is labelled "Choose a recipe", with no per-row `aria-label` (the Add buttons got one) | minor |
| L3-6 | `StartView.svelte:937-953` | a pending step | the third step mark is a constant ✓ (`:947`), even while the step is pending; hard-coded glyphs outside the catalogue | minor |
| L3-7 | `StartView.svelte:1431-1491,970-980` | any step change; the timeout | no focus management: the focused primary is destroyed and focus falls to the body; the timeout draws a `role=status` paragraph beside `ErrorState`'s `role=alert`: two live regions for one event | minor |
| L3-8 | `StartView.svelte:1452-1459` | Continue without sources | native `disabled`, no `disabledReason` (Chat in the same list uses it) | minor |
| L3-9 | `StartView.svelte:299,556-581` | an issue submitted before the explorer answers | `resolveProjectRepository` is no longer awaited (the CodeRabbit fix); the fallback `repository.split('/').pop() === currentProject` can raise the grant dialog for the project's own repository | minor |
| L3-10 | `doc/design/shared/starting-work.adoc:24` | — (re-read: a lone `+` sits between the setup-gate bullet and the new bullet) | a stray list continuation; its rendering was not shown by reading | minor |
| L3-11 | `StartView.svelte:1102-1112`; `start.spec.js:49,62`; `DecompositionReviewView.svelte:102` | `npm run format:check` (not part of the Maven build, not run) | formatting drift: an unindented `{#if epic}` block, double-quoted strings, lines over 100 characters | info |
| L3-12 | `SourceList.svelte:30-36`, `ActMenu.svelte:389-391` | touch | the disabled item's reason is in an `aria-hidden` span shown on focus plus `title`; may never show on touch; the menu may open far from the trigger | minor, not shown by reading |
| L3-13 | `Button.svelte` `handleClick`, `StartView.svelte:1009-1020` | click on a disabled "Choose a recipe" or on Chat while unavailable | `stopPropagation` is called but the forwarded `on:click` is on the same element, so the parent handler may still fire; the Button test covers only `onclick` as an attribute | minor, not shown by reading |

**Layer 3 verdict:** findings — no blocker, no major; 11 minor, 1 info, 2 not shown by reading.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Web | no `innerHTML`/`{@html}` with untrusted text | conforms | no such sink in the diff |
| CLAUDE.md Web | no arbitrary timer wait in tests | conforms | only `test.setTimeout(60000)` as a budget; waits are `expect`, `vi.waitFor` |
| CLAUDE.md Web | ES modules, flat ESLint | conforms | no config change |
| CLAUDE.md Code (Java) | Generators, `@NullMarked`, `package-info`, no generic exception | conforms | Java untouched; four `.properties` files only |
| spec Standing | text-guard baseline only shrinks | conforms | file not in the diff |
| spec Standing | design system v2 views, no plain text button | deviates | `ConfinedFilePicker`, `GrantModal` |
| spec Standing | the design document is updated in the same PR | conforms | `starting-work.adoc`, `starting-work-texts.adoc`; `components.adoc` not touched |

## Metrics and Anomalies

- Tokens: `unknown` (unobserved, not zero). `plugin_version` `unknown`; executor `0.1.2135`.
- Test runs as stated by the lane (not re-run): `941fe759` gate 5102; e2e all plan specs green at `44fbf4c9`; four `data-table.spec.js` failures reproduce at the base (`8f9e8cf4`).
- The lane reports no Playwright MCP contention beyond the one operator question (browser shared with another session; "retry now with URL checks").
- Shell-rule breaches the lane owns: a `cp` copy-back, `grep`/`sed`/`tail` on a log, one `awk`.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `17a7976f` (5 body findings, all dispositioned in the progress file: 5 `finding:` lines; four fixed in `44fbf4c9`, `inline-1` refuted and answered). Re-read: `review-gate --pr-number 455` shows `unresolved_review_threads=0` and `review_body_findings_total=5`, the same as the landing (its `handled=0` is the gate not being given the dispositions, as at PLAN-146). The merge conforms to the standing rule (one CodeRabbit review on record, later unreviewed heads are fine).
- Sonar: `sonar_open_new_issues=0`, gate `ok`; the Sonar MCP search for pull request 455 returns no issues; `read_open_issues` is `undecidable` on the merged PR (`head_revision_unavailable`), its `analysis_revision` equals the merge head `941fe759`.
- Landing check: `complete: false`, `missing_keys` `total_tokens` and `plugin_version`, both `unknown`; recorded as an Open Defect.
- `.plan/orchestrator`: re-read — the #455 diff (`b2ac6204^1..b2ac6204`) carries no file under `.plan`; the write-boundary held here. The next landing, PLAN-58 (#456), did carry six ledger files; see its report.

## Reconciliation Actions

- [x] row `status` → `shipped` — `queue --transition PLAN-53 --status shipped`
- [x] row `pr` stamped — `455`
- [x] row `landing` stamped — `landings/PLAN-53.md`
- [x] row `plan_marshall_plan_id` stamped — `rb-53-web-start-wizard`
- [x] epic.md: Open Defect `PLAN-53 landed`
- [x] resume anchor and `queue-view.md`
- [x] the inbox message archived on consume

## Follow-Ups

- **A PLAN-53 residue plan** (named, NOT staged) after the field-system D0 of decision (1): the recipe step's primary, Back, the description card, the abandon-on-failed-act, the gate text on the resume path, the stale gate entry on retry or leave, the `Field`/`RadioGroup` adoption, the vacuous test at `StartView.scenarios.test.ts:649-650`, the weak 20 s pin, the stray `+` in `starting-work.adoc:24`.
- PLAN-88, 151, 153, 155, 156 and 157 build on the same field system and views; the residue plan should come before them where they touch `StartView.svelte`.
