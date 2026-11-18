# Landing Analysis: PLAN-51 — Web: Decisions as a Triage List

epic: ui-improvements
workstream: WS-03
pr: #469 (https://github.com/plan-marshall/pm-mcp-clients/pull/469)

> Landing record, written by `analyze` on 2026-11-17 from the inbox message
> `rb-51-web-decisions-triage-001.md` (revision 1: the lane replaced its earlier stopped-run report with the real landing; a lead, not a fact), git, the provider, and a read-only three-layer leaf.
> The orchestrator re-read the points marked "re-read" on `deee9290`.

Merged as `deee9290356ee30eb18a38a47dca830d0850c6b4` (2026-11-17T19:53:30Z), 29 files, +3675/-2408. Sender `rb-51-web-decisions-triage`. Lane: runbook. Harness `opencode`, model `deepseek-v4.1-flash/high` (as stated, not corroborated). The lane reports `deliverables_done=6` of 6 and `self-review: budget-exhausted` (two rounds; the residue was left open by design, not by a session budget). The run spanned two sessions: the first stopped at its context limit with D1 to D5 in code and no pull request.

The operator's two answers settle the scope: the amendment plus the design documents bind D1 and D2 (the Deliverables text's row marks, group toggle, `1` to `9`/`c`/`l` keys and toast are superseded, not required), and all six deliverables were authorised, including the pm-api request-id change beyond the Expected Surface.

## Deliverable Fidelity vs Spec

| Deliverable (spec, amended) | Verdict | Evidence |
|-----------------------------|---------|----------|
| D1 — triage list and detail | shipped | blocking first, grouped by project (`DecisionView.svelte:149-183`), subject chip, phase and detail line (`:1434-1450,283-294`), detail without the repeated description (`:1580`), page on `TriagePage` (`:1869`); `decisions.spec.js:71-106`, `DecisionView.test.ts:334,354,370`. In the browser the mock's only non-blocking decision is also its newest, so the e2e alone cannot separate the two orders; the unit fixture does |
| D2 — keyboard and advance | shipped, **one clause partly** | `Enter` decides a chosen option (`:1276-1292`), the line names it (`:597,1295-1299`), the next decision is selected by `replaceState` read from the displayed order (`:934-948,1223-1242`). **Partly:** the handler returns when the target is inside `button, a, input, textarea, select, summary` (re-read: `:1280-1286`), and a chosen radio holds focus on its `input`, so `Enter` does nothing from the option itself; the e2e moves focus to `.decision-title` by hand and says so (re-read: `decisions.spec.js:139-140`, "`Enter` belongs to the decision's own surface, not to the option it was chosen on") and the unit test fires `keyDown` on the legend text (re-read: `DecisionView.test.ts:634`) |
| D3 — recommendation and evidence | shipped, preselection not delivered | recommendation sentence with the option label (`:1126-1131,1634-1656`), `EvidenceView.svelte` (plain labels, file and lines, folded fallback, text only; `EvidenceView.test.ts`), the design entry (`components.adoc:455-461` and the SVG). **Not delivered, reported by the lane:** preselection for a reversible decision with unanimous, clear-majority or no-quorum support (`decisions.adoc:84-86`; the re-grounding of 2026-11-10 asks for it; the D3 Done-when does not name it) |
| D4 — own answer confirmed | shipped, weakly pinned | `request_id` on `DecisionAnswerRequest` and `DecisionAnsweredEvent`, echoed by the mock, sent by the client; own, console and elsewhere lines (`:543-549,596-610`); `cli` rejected before the id fallback (`:545`); `DecisionRecordsTest.requestIdRoundTrips`. The mock's echo has no test; no test sends a mismatched id for a pending web answer |
| D5 — one notification for a burst | shipped, wiring unpinned | `createDecisionAnnouncer` in `stores/runtime.ts:1031-1099` (silent prime, one-second non-sliding burst, several or one text, dropped when answered first, foreground-tab gate `:1039`); `decision.notify.several` en and de; the `doc/plans/README.adoc:367` row stays `open` with the notification part recorded. The prime wiring (`:341,726-733`) has no unit test and no reconnect e2e |
| D6 — chat guards pinned, two replies true | shipped, one guard test doubtful | `takeProposedAnswer` revision guard and `endTheChat` generation guard (`:1076,1089-1110`; tests `:1239,1285`, with a positive control); late load and late send (`:972-1025`; `:1261,1200`); the two PR-319 threads `PRRT_kwDOVBOtss6z9Ddl` and `PRRT_kwDOVBOtss6z9Dds` are resolved and carry replies of 2026-11-17 (the leaf verified them by a read-only GraphQL query); the `format.ts` first-paint decision recorded (`texts-and-languages.adoc:302`) and implemented. **Doubtful:** the per-decision busy-marker test (see layer 2) |

`text-guard-baseline.json`: the only change removes `ci_check` for `DecisionView` (de and en): no list grew. Browser checks: not shown by reading.

**Files outside the Expected Surface:** `pm-api` (`DecisionAnswerRequest`, `DecisionAnsweredEvent`, `DecisionRecordsTest`), `pm-mock-runtime` (`MockRuntimeEngine`, `DecisionsScenarioTest`), `format.ts`/`format.test.ts`, `chrome`, `navigation`, `showroom` and `theme-readability` specs, `pageWidth.test.ts`, `catalogue.test.ts`: the operator's authorisation covers the pm-api change; the rest follow from the layout change.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf — reason: the Dispatch Decision Rule; 29 files; `DecisionView.svelte` +1161/−952 (1991 → 2200 lines) and `DecisionView.test.ts` +1189/−1230 are real change (about 10% re-indentation), not reformatting. Build logs and screenshots were not opened. The orchestrator re-read the points marked below on `deee9290`.
- **Verdict:** layer 1 minor (preselection, one partly); layer 2 four weakened lane claims and **one contract finding**; layer 3: no blocker, **4 major**, 8 minor, 4 leads.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | `Enter` decides a chosen option | checked | partly | re-read: not from the focused option (see D2); `navigation.adoc:252` and the "every form submits on Enter" rule (`components-fields.adoc:85`) say it should |
| L1-2 | a reversible decision with unanimous or clear-majority support is preselected | checked | refutes | `answerOf` starts at `''`; no initial selection anywhere in `DecisionView.svelte` (the leaf read the whole file); "Enter takes the recommendation" can never happen; the lane's reason is that it "would change agreed behaviour": the design row says preselected |
| L1-3 | VD-S-06: no plain text button | checked | refutes | `DecisionView.svelte:1348-1356` a plain `<button class="notice-dismiss">` with "×": pre-existing, in a touched view |
| L1-4 | the design document is updated in the same PR | checked | partly | `components.adoc`, `decisions-texts.adoc`, `texts-and-languages.adoc`, `plans/README.adoc` updated; stale lines below |
| L1-5 | the three HYPOTHESIS lines | checked | confirms | the answer endpoint carried no request id (refuted at the re-grounding, built as the D4 change); `PremiseEvidenceView` could not carry the evidence (`EvidenceView` built); `DecisionView` not on `TriagePage` (now is) |

### Layer 2 — Implemented

| Item | Verdict | Evidence |
|------|---------|----------|
| "D6 per-decision marker pinned by a test that fails on a revert" | **weakened** | re-read: `DecisionView.test.ts:1159-1198` finishes A's turn first (`releases[0]`) while B stays in flight and asserts B stays busy; the old code's single `chatBusyId` with a token compare (`deee9290^1:DecisionView.svelte:788-798`) already satisfies that, because B's call bumps `chatBusyToken` so A's `finally` clears nothing. The spec's case, B finishing first while A's marker survives, is not exercised. Read by comparison, not run. The second PR-319 reply ("each test failing when its guard is reverted") is therefore doubtful for this guard |
| "D4 echoed by the mock … proven against the mock" | **not pinned** | `MockRuntimeEngine.java:11047-11159` echoes the id; `DecisionsScenarioTest` only had `null` appended to its constructors; the own-answer e2e passes with or without the echo because of the fallback; the lane's mutation inverted the attribution, not the echo |
| "a reconnect raises none" | true at `announcer.prime()` only | the wiring (`runtime.ts:341,726-733`) is untested |
| "`Enter` decides, met" | depends on the manual focus | see D2 |
| preselection residue | correctly reported | confirmed by reading all of `DecisionView.svelte` |
| `cli` rejected before the id fallback; successor from the displayed order; reconciliation after a successful answer (`loadDecisions(true)`, `:802,863,902`); keyed each; the pending-own-answer fallback; background-tab gate; silent prime; one-second non-sliding burst | borne out | `:545,935,802,548`, `EvidenceView.svelte:176`, `runtime.ts:1031-1099` |

**Contract finding (re-read in `pm-mcp-core`).** `request_id` on an answer request and on `decision.answered` exists only in this repository's `pm-api` records, the web client's types and the mock. The engine's published specification (`../pm-mcp-core/doc/specification/`) has no such field: its only `request_id` is the waiver-request record counter (`identifiers/02-registry-stores-to-web.adoc:25`; `phase-workflows/07-…:68`), a different thing that the client-side name collides with in vocabulary (lead). The lane says the daemon does not echo it. Against a daemon that never echoes: every own answer takes the fallback (a pending request with no id counts as own); two answers to different decisions in one tab are fine (`ownRequests` keyed by decision id); another tab of the same client shows "answered in the web app"; another client answering the same decision while this tab's own request is in flight, with a web-channel event, shows "Decided: <their option>" (narrow race; `already_answered` then reaches the loser). The engine's channel values are `cli`, `web` and `quorum` (`store-schemas/14-plan-instance-waivers-questions.adoc:149`); the client matches `cli` and `web` and calls the rest "another client", so a quorum answer reads wrongly (lead). The runtime records the channel from the transport, never from a client field. The event shape the client reads (`id`, `answer` as the option key, `device`) matches `pm-api`, not the catalogue text at `plan-marshall-documentation/.../06-event-catalogue.adoc:87` (address, revision, channel): if the real event carries no `answer` the label is empty (lead).

Not shown by reading: the failing-run logs, the browser captures, the CI runs, whether the daemon accepts or ignores an unknown `request_id` on the answer endpoint.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `DecisionView.svelte:1280-1286` (re-read), masked by `decisions.spec.js:139-140` and `DecisionView.test.ts:634` | click or arrow to an option, press Enter | nothing happens; it works only from the heading or the container; the e2e and the unit test never put focus where the operator's focus is | major |
| L3-2 | `DecisionView.test.ts:1159-1198` (re-read) | revert the per-decision marker to one slot | the test still passes: D6(a) is implemented, not pinned | major (test) |
| L3-3 | `decisions.de.properties` `decision.outcome.answered_elsewhere=beantwortet in {channel}, {device}: {answer}` with `decision.channel.web=die Web-App`, `.cli=die Konsole`, `.other=ein anderer Client` (re-read) | an answer elsewhere, German UI | "beantwortet in die Konsole", "in ein anderer Client": needs the dative ("in der Konsole", "in einem anderen Client"); the lane's own evidence quotes the wrong form; the default-language operator sees it on every answered-elsewhere | major |
| L3-4 | `DecisionView.svelte:586-615,802,387-393` (lead, depends on the wire order) | `loadDecisions(true)` completes before `decision.answered` | the effect calls `closeDetail()`; the event then finds no decision, so `optionLabelOf` returns the raw option key ("Decided: rebase") and `removeDecision` no longer advances; the comment at `:592` assumes the event comes first; with the stream down, the reload alone removes the decision with no outcome line and no advance | major (lead) |
| L3-5 | `DecisionView.svelte:674-691,1381,1245,1238-1241` | every post-answer reload | `isLoading=true` swaps the whole list and the filter input for a skeleton, and focus drops to the page (`navigation.adoc:259`: focus never falls to the page) | minor |
| L3-6 | `DecisionView.svelte:71,596-610` | decision B open, an answer about C arrives | one outcome slot, not tied to a decision: "answered in the console" about C shows under B without naming C | minor |
| L3-7 | `DecisionView.svelte:907` | a second Enter or Decide before the event or reload removes the decision | `isSubmitting` has cleared: a second post, refused `already_answered`, shows the failure banner | minor |
| L3-8 | `runtime.ts:1066-1075,341,726-733`, `decisions.adoc:148,150` | events replayed right after a reconnect, before the async prime resolves (lead); a non-blocking or reversible decision; `decision.changed` | announced; the announcer ignores urgency (design: interrupts only for blocking or irreversible decisions) and `decision.changed` ("announced again as changed") | minor (leads) |
| L3-9 | `DecisionView.svelte:89,554,1318` (lead) | a first decision that arrives while another view is open; a phone with the detail open | the permission banner exists only in the list column and only after a mounted `DecisionView` set `sawDecisionEvent`: the ask never happens, or is not visible | minor (lead) |
| L3-10 | `EvidenceView.svelte:175-185`, `:29-36,112-143`, `DecisionView.svelte:1305,1485` | the folded block; heading order | `<div><details>` directly inside `<dl>` is invalid structure; the list `h2` comes before the detail `h1`; the control-sequence stripping test proves nothing for the folded block, because `JSON.stringify` has already escaped ESC to the text `\u001b` before `stripControlSequences` runs | minor |
| L3-11 | `theme-readability.spec.js:345` | the contrast guard | walks only the list for `DecisionView`: the detail is no longer covered (the other e2e rewrites read the page and are not weakened; `showroom.spec.js:149` replaced the `.recommended-tag` check by sentence text) | minor |
| L3-12 | `DecisionView.svelte:843-849,887-899` | own words on a multi-part decision | Decide submits only the option key (`own_words`), not the words; pre-existing | minor (noted) |
| L3-13 | the residue the lane reports | — | own words use `TextArea`, not `Compose`; a disabled Decide shows no reason in `FormStatus`; "waiting since" shows the clock where the design says how long (`decisions.adoc:150` against `:162`, inconsistent in the design itself); the `value` answer type has no control (the producer contract defines no value input) | minor |

**Layer 3 verdict:** findings — no blocker; four major (L3-1, L3-2, L3-3, and L3-4 as a lead); 8 minor, 4 leads.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Web | no `innerHTML`/`{@html}`; no arbitrary timer wait in a test | conforms | none in the added lines; the lane's one `waitForTimeout(2000)` was a manual browser check, not a test |
| CLAUDE.md Dependencies | none added | conforms | no `pom.xml` or `package.json` in the file list |
| CLAUDE.md Java | JUnit 5 only, generators for input data | not shown by reading | `DecisionRecordsTest`, `DecisionsScenarioTest` |
| spec Standing | VD-S-06 v2 components | partly | L1-3 |
| spec Standing | text guard baseline only shrinks | conforms | one entry removed |
| spec Standing | en/de pairs with the same placeholders | conforms in form | placeholders match; the German wording is wrong (L3-3) |
| spec Standing | the design document is updated in the same PR | partly | stale lines below |
| spec Write-Boundary | no write under `.plan/orchestrator/` | conforms | the lane states none; #469's file list has no `.plan/orchestrator` path (re-read: `git show --stat deee9290`) |
| the engine's contract (`pm-mcp-core`) | the client follows the published event and request shapes | **deviates** | the contract finding |

**Stale or missing design lines:** `doc/design/shared/decisions.adoc:148,152` (neither mentions the background-tab-only rule, the notification of non-urgent decisions, or the foreground gate; the text still says it fires for every unannounced decision); `decisions.adoc:84-86` (preselected, "Enter takes the recommendation": not implemented, not annotated); `decisions-texts.adoc` omits `decisions.list.since` and `decisions.list.other_group` (no design document names them); no design document describes `request_id` or the fallback (`decisions.adoc#outcomes`, `:179-185`); `decisions.adoc:162` shows a clock ("waiting since 14:02") while `:150` says how long. `doc/plans/README.adoc:367` is consistent.

## Metrics and Anomalies

- Tokens `unknown`; `plugin_version` `unknown`; executor `0.1.2135`. `landing-check`: `complete: false`, `missing_keys` `total_tokens` and `plugin_version`; `surface_delta` `unmeasured`.
- Two sessions: the first reached its context limit with D1 to D5 in code and no pull request, and wrote into the main checkout, restoring it with a scoped `git checkout --` and `git clean` (both on the lane's forbidden list, disclosed; the main checkout was verified clean afterwards). The restart was the operator's, with the corrective resume paste.
- Self-review: two rounds (15 and 5 items), all actionable ones fixed; the second round found the restored revision test vacuous and fixed it.
- CodeRabbit: 9 inline findings plus one outside the diff, 8 fixed, 2 answered with a stated limit (the id-less own-answer fallback, a `value` part without options). One generator-driven `pm-api EntryRecordsTest` failure in `sonar-build` on an earlier head (the known flaky case, also recorded by PLAN-66); the rerun was green.
- Builds: gate and unit runs routed (`daemon_longpoll`); the e2e and the scoped runs that need the plan's ports ran `in_process_fallback` (the daemon takes its own environment, which cannot carry `PM_MOCK_WEB_PORT` and `PM_WEB_PORT`), the same cause the PLAN-49 lane reported for the routed e2e. No `killed` job; builds queued up to about 530 s behind another session's.
- Merged `origin/main` twice (PLAN-52, then PLAN-66 and PLAN-163); both auto-resolved; the second merge exposed two e2e assertions that read fixture ids and a hidden-by-substring option, rewritten to read the page (`45ca23a4`).
- The lane's remaining rule breaks, by its own count: shell file listings (the Glob tool does not traverse `.plan`), one `git add -A pm-web-app/src/lib doc`, `curl` to the mock's control route outside the executor. No guard enforced on this harness.
- The mailbox read with a digit-leading plan id is refused (`invalid_target_plan`): the fifth lane to report it (PLAN-24, 60, 64, 49, 51).

## Routing and Merge Behavior

- Review: CodeRabbit (required, `review_complete_after_handling`), one review at `4b01a` with the findings above; later heads were not re-triggered; re-read: `review-gate --pr-number 469 --required-reviewer coderabbit` shows `unresolved_review_threads=0`, `review_body_findings_total=9` (its `handled:0` is the gate lacking `--disposition`). Sourcery refused (150,000 diff characters), optional.
- Sonar: `sonar_open_new_issues=0`; the Sonar MCP search for pull request 469 returns no issues.
- Merged through the queue.

## Reconciliation Actions

- [x] row `status` → `shipped` — `queue --transition PLAN-51 --status shipped`
- [x] row `pr` stamped — `469`
- [x] row `landing` stamped — `landings/PLAN-51.md`
- [x] row `plan_marshall_plan_id` stamped — `rb-51-web-decisions-triage`
- [x] epic.md: Open Defect `PLAN-51 landed`
- [x] resume anchor and `queue-view.md`
- [x] the inbox message archived on consume

## Follow-Ups

- **A PLAN-51 residue plan** (named, NOT staged): `Enter` from the chosen option (L3-1) with an e2e that starts from the focus the operator has, the busy-marker test for the spec's case (L3-2), the German dative (L3-3; a one-line catalogue change, a trivial-change candidate), the reload-versus-event order (L3-4), preselection (L1-2: the design row, or the design amended), the outcome slot per decision, the notification banner's reach, the `EvidenceView` structure, the reconnect wiring test, the mock echo test, the stale design lines.
- **The `request_id` contract (operator decision):** the engine's specification and the daemon carry no `request_id` on the answer request or on `decision.answered`; the field exists only in `pm-api`, the client and the mock. Either the engine specification and the daemon grow it (and a channel value for a quorum answer), or the client drops the id and keeps the fallback. Same family as the PLAN-66 onboarding state facts: nothing staged.
- **Running overlaps:** PLAN-152 shares four files with this plan (`decisions.en.properties`, `decisions.de.properties`, `DecisionView.svelte`, `decisions-texts.adoc`) and PLAN-164 two (`client.ts`, `client.test.ts`): each merges `main` into its branch and keeps both sides, as their specs say; the German key `decision.outcome.answered_elsewhere` and the `decision.channel.*` keys now live in `main`.
