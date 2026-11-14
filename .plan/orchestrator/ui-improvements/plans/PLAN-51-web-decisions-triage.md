# PLAN-51: Web: Decisions as Triage

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Turn the decisions page into a triage list with the detail beside it, keyboard decisions, auto-advance and a toast (UX-S-08; UX-P-06).

Source of this plan (read before scoping):

- `doc/claude_review/ux/plan.adoc (UX-S-08)`
- `doc/claude_review/ux/proposals.adoc#UX-P-06`
- `doc/claude_review/ux/ux-decision-triage.svg`
- `doc/claude_review/ux/findings.adoc#UX-F-11`

## Deliverables

1. **Triage list and detail**
   - What: The page sits on the `TriagePage` template of `PLAN-43` (this plan moves it there: `PLAN-43` no longer does). Rows: title, plan path, blocks mark, reversible mark, since, recommended option; blocking first then oldest; group by project as a toggle; detail without the repeated description; the layout toggle is removed for decisions.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `decisions.spec.js` on `running-work` → `decisions.open`: eight decisions, blocking first (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `decisions.open`: triage list, `1` and `Enter` decide, next selected, toast; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
2. **Keyboard and advance**
   - What: `1`–`9` choose, `Enter` decides, `c` chat, `l` later; after deciding a toast "Decided: <option>" and the next decision is selected; phone: list then detail.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - e2e: `1`, `Enter` decide; the next is selected; the toast names the option (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `decisions.open`: triage list, `1` and `Enter` decide, next selected, toast; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
3. **Recommendation and evidence** (from the former `PLAN-57`, superseded: one view, one plan)
   - What: "Recommended: Rebase onto main (by implement)"; evidence as fields with plain labels, `file`/`lines` as a link where the project offers one, unknown structures as a folded JSON block (`EvidenceView.svelte`).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - Unit tests: label, fields, the fallback (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `decisions.open` and `decisions.answered-elsewhere` (plus a `POST /mock/broadcast` from `cli`); baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
4. **Own answer confirmed** (from the former `PLAN-57`)
   - What: The answer request carries a client request id; the `decision.answered` event with that id confirms "Decided: <option>"; other events produce "answered elsewhere" with channel, device and option label.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `decisions.spec.js`: own answer; a `POST /mock/broadcast` from `cli` shows "answered in the terminal" (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `decisions.open` and `decisions.answered-elsewhere` (plus a `POST /mock/broadcast` from `cli`); baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
5. **One notification for a burst, none after a reconnect** (from the package `web-moving-between-plans`, moved here from `PLAN-83`)
   - What: a notification fires only for a decision the client has not announced before (`doc/design/shared/decisions.adoc#announce`, merged with PR #278): the decision announcer keeps the set of decisions it has announced; the first read after a start, a reload or a reconnect primes that set silently; decisions that arrive within one second make one notification (several: `decision.notify.several`, `{count} decisions wait for you` / `{count} Entscheidungen warten auf dich`, which opens the list of open decisions; one: its own, which opens that decision); a decision answered before the notification shows is dropped from it. The key `decision.notify.several` goes into `decisions.{en,de}.properties`; a notification never carries a decision's content.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - Unit tests with a fake clock and a fake notification: a reconnect raises none, a burst of three raises one that opens the list, a single decision raises one that opens it, an answered decision is not announced (evidence order: `test-first`)
     - An e2e test through the mock's decision events: the same two cases in the browser (evidence order: `test-first`)
     - The row of the package `web-moving-between-plans` in `doc/plans/README.adoc` is set to `done` if `PLAN-83` and `PLAN-59` D4 have landed (evidence order: `none`)

6. **The chat guards are pinned, and two replies are true** (PLAN-101 residue, `landings/PLAN-101.md`)
   - What: (a) `DecisionView.svelte` ~64-66 keeps one `chatBusyId` slot: B finishing first clears A's busy marker while A's request is in flight; busy is tracked per decision (the PR 319 thread asked for it), the operation token stays. (b) No test pins the guards of `11be8390`: the `chatBusyToken` clear, the `takeProposedAnswer` revision guard (a late answer to an earlier revision is dropped; the operator is told or the design says it is dropped silently) and the `endTheChat` generation guard. (c) Two replies in the PR 319 threads are false and are corrected in the thread: `format.ts:363` ("handled in `7c7ea71c`": that commit changes `DecisionView.test.ts` only; the finding was answered, not changed, and its behaviour is decided: a first paint without the unit key keeps the numeric figure or the design says it does not) and `DecisionView.svelte:60` (the fix is `e0140658`/`11be8390` of PR 335, then this deliverable).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - Tests that fail when each guard is reverted: the per-decision busy marker survives another decision's operation; a late proposal answer after a revision applies nothing; `endTheChat` of an earlier generation closes nothing of the reopened panel (evidence order: `mutation-after`)
     - The two PR 319 threads carry a reply that states the real commit or the reason, and the `format.ts` first-paint behaviour is decided in the design (evidence order: `none`)
     - The landing message states `unresolved_review_threads=N` (read through GraphQL `reviewThreads` at the merge head, path:line of each open thread) and `sonar_open_new_issues=N` as lines of its facts block (evidence order: `none`)

## Standing Constraints

- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project `plan-marshall_pm-mcp-clients`), read at the pull request head after the push (`unknown` for a failed read, never `0`); every open issue is fixed in a commit of THIS pull request, and a dismissal is made only on the operator's selection. State `sonar_open_new_issues=N` in the landing message. This is a merge gate, not a deliverable's Done-when.
- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20510 PM_WEB_PORT=20511 ./mvnw -pl pm-web-e2e verify -Pe2e`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20510`, app port `20511` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20510"`), then `PM_MOCK_WEB_PORT=20510 ./pm-web-app/start.sh -p 20511` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20510/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/ux/plan.adoc (UX-S-08)`, `doc/claude_review/ux/proposals.adoc#UX-P-06` — read at `doc/claude_review/ux/plan.adoc`
  - verdict: corroborated | checked_at: 1cc0e10ec2ca617853b99a23dcfed8e5e2fed1bb | by: ui-improvements/cleanup | rescoped: n/a | evidence: `doc/claude_review/ux/plan.adoc` UX-S-08 and `ux/proposals.adoc:269` UX-P-06 exist.; re-grounded vs D1-D5 2026-11-10 (Amendment)
- Re-grounded note at HEAD 07104678 (2026-10-30): PR #278 added a rule to `doc/design/shared/decisions.adoc` (#announce): a notification fires only for a decision the client has not announced before (none after a reconnect or a reload), decisions arriving within one second make one notification, several open the list of open decisions (`decision.notify.several`) and one opens that decision. this plan builds it as deliverable 5 (moved here from `PLAN-83`); the triage view must not re-announce on its own re-read.
- Re-grounded note at HEAD 94bbefe1 (cleanup 2026-11-02): PLAN-43 shipped (#301): templates live in pm-web-app/src/lib/templates/ (TriagePage, ObjectPage, WizardPage, DashboardPage) and its D3 guard lists views sitting on a template; DecisionView does not use TriagePage yet and this plan must add itself to that list. Key decision.notify.several is not yet in decisions.en.properties (to be created).
- RESCOPE at outline (design audit 2026-11-02): Done-when for a package row is 'the row stays done, no link' (Z2).
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.
- HYPOTHESIS: the answer endpoint accepts a client-chosen request id that the `decision.answered` event echoes — confirm/refute at `pm-api/src/main/java/de/planmarshall/api` § decision answer resource and `pm-web-app/src/lib/api/client.ts` § `answerDecision` (verify-at-outline)
- HYPOTHESIS: `PremiseEvidenceView.svelte` already renders the evidence list and can carry the plain labels without a new component — confirm/refute at `pm-web-app/src/lib/components/PremiseEvidenceView.svelte` § `evidence` (verify-at-outline)

## Expected Surface


- OBSERVED: `pm-web-app/src/lib/views/DecisionView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/PremiseEvidenceView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/stores/runtime.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/decisions.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/decisions.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/decisions.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/decisions.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/decisions-texts.adoc` — present at HEAD 1cc0e10e

## Dependencies and Sequencing

- Depends on: `PLAN-43` (`web-page-templates`), `PLAN-76` (`shared-port-assignment`, shipped)
- Overlaps with: `PLAN-83` (package `web-moving-between-plans`), `PLAN-21` (the terminal answer dialogue; separate client)
- Adjacent to: none

## Design Review 2026-11-06 (Amendment)

Source: `inbox/archive/design-review/design-review-010.md` § Per-plan instructions (messages 003, 004, 006, 007, 008, 009).
**Gate: D1 and D2 merged** (documents `AGREED`) and the packages that build what this plan uses; its decision-chat part
waits for the D4 web chat package. **`PLAN-111` (drop the layout modes) lands first**: this plan starts from Decisions
without layout modes. Queue status: `parked`; the orchestrator re-grounds this spec when it is restarted. The Expected Surface
above is NOT widened now (a parked row is not in the disjointness gate population). When restarted:

- Layout: list → one decision; the next decision after deciding is a `replaceState`; `Enter` decides a decision with a
  selected option (003).
- Tags: subject as `Chip`, phase in list and detail, reversibility and "blocks the plan" as detail text, "3 offen" as a
  `CountBadge` in the group head (006).
- Interaction: options as `RadioGroup` cards and one primary "Entscheide"; the waiver's agreed answers "Erteile den Waiver"
  and "Lehne ab" both secondary, no preselection, `Enter` answers nothing, "Später" ghost (`decisions.adoc:174`); rows
  `RowLink` + `aria-current`; one "Later" key (007).
- Fields: "Notiz (optional)", own words as `Compose`, waiver reason `TextArea` with the one required text, "Entscheide"
  disabled with its reason, its "Entschieden: <Option>" line as the form's `FormStatus` (008).
- Chat: the decision chat in the new `Chat` with the agreed texts ("Antworte mit diesen Worten") (009).
- The "layout parts" of its scope (removing the toggle for Decisions) are already done by `PLAN-111`.

## Amendment 2026-11-07 (PLAN-111 landing)

PLAN-111 merged (`bb30d71e`) and deleted `pm-web-app/src/lib/stores/viewLayout.ts` and `pm-web-app/src/lib/components/ViewLayoutToggle.svelte` (struck from the Expected Surface); its "layout toggle is removed for decisions" clause is done. `DecisionView.svelte` has no detail pane now: `selectedDecisionId` only marks a card (`aria-current="true"`, `.decision-card--selected`) after the palette deep-link clears a hiding filter and scrolls to it (guarded while `isLoading`); the plan's "next decision selected after deciding" and `RowLink` work reuses or replaces that mark and adds no second one. `decisions.split.select` and `common.layout.*` no longer exist. Re-ground every line number (the file lost about 225 lines). Include the pre-existing prettier drift of `DecisionView.svelte` (~833, ~1068) and `DecisionView.test.ts` (~187, ~526, ~591) and the browser check of `/decisions` (mark and scroll at 390×844, de/en, light/dark) that PLAN-111 could not run.

## PLAN-148 Landing Residue (Amendment 2026-11-14)

- `PhaseLabel`'s role words: the six `role.name.*` come from the mock scenarios; the runtime's `askingRole` is a free string
  (`DecisionEnvelope.java:57`), and `PhaseLabel.svelte:90` drops an unknown role silently. Settle the role set with the runtime docs (or
  show an unknown role with the catalogue's unknown word); list consumers of `RowLink` and `StatusLabel` as in `PLAN-49`'s amendment.

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-51-web-decisions-triage.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

## Re-grounding 2026-11-10 against the agreed D1–D5 designs (Amendment)

Source: `doc/design/shared/decisions.adoc` (#order, #recommendation, #parts, #own-words, #chat, #announce, #waiver, #outcomes), `doc/design/web-app/navigation.adoc` (#lenses, #url, #keys, #missing), `doc/design/web-app/components.adoc` (#hierarchy, #row-link), `doc/design/web-app/components-fields.adoc` (#submit, #choice, #compose), `doc/design/web-app/chat.adoc` (#placement). HEAD 1cc0e10e. `PLAN-111` has landed (the layout clause is done). **Split proposed** (the spec would hold six deliverables):

- `PLAN-51` keeps D1–D3 (list, answer form, recommendation and evidence).
- D4 and D5 become one plan `web-decision-announcer` with no D1/D2 dependency (stageable at once, after `PLAN-45` which holds `views/`).
- D6 (a) and (b) and the chat adoption move to the D4/D5 web chat package.

1. **The list and one decision.** `DecisionView.svelte` becomes the Decisions lens: the list of open decisions (rows are `RowLink` with `aria-current`; the subject a `Chip`; the phase in list and detail; reversibility and "blocks the plan" as detail text; the group head with a `CountBadge`), and the decision as the object column at `/decisions/{id}`; blocking first, then oldest, as the runtime serves; a selection a filter hides is a pinned row. The description is not repeated in the detail.
   - Done when: `decisions.spec.js` on `running-work` → `decisions.open`: eight decisions, blocking first, one selected beside the list (evidence order: `test-first`); browser check of `/decisions` at 390×844 and 1440×900 in `de`/`en`, both themes, baseline and after (evidence order: `none`); the prettier drift of `DecisionView.svelte` and `DecisionView.test.ts` named by the PLAN-111 amendment is cleared (evidence order: `none`).
2. **Answering.** Options are `RadioGroup` cards (`CheckGroup` for a part that accepts several), the one primary is _Decide_ (`decision.confirm.action`), `Enter` decides only a decision with a chosen option; the success line is the form's `FormStatus` `decision.outcome.answered` ("Decided: {answer}"), not a toast; after deciding, the next decision is selected by a `replaceState`. Own words use `Compose`, the waiver reason a `TextArea` with its one required text; _Decide_ disabled shows its reason in `FormStatus`. The waiver keeps both answers secondary, no preselection, `Enter` answers nothing, _Later_ ghost. A phone shows the list, then the decision (one column).
   - Done when: `DecisionView.test.ts` and `decisions.spec.js`: choose, `Enter`, the `FormStatus` line, the next decision selected, the waiver has no preselection (evidence order: `test-first`).
3. **Recommendation and evidence.** The recommendation stands as `decisions.adoc#recommendation` says (marked and preselected only for a reversible decision with unanimous, clear-majority or no-quorum support; behind a disclosure for a costly or irreversible one); "Recommended by outline: …" is a sentence, never a label suffix; evidence is the description, then the evidence it names as fields with plain labels, `file`/`lines` as a link where the project offers one, an unknown structure as a folded block, shown expanded. If a new component is needed it is added to `doc/design/web-app/components.adoc` first (`#new-component`).
   - Done when: unit tests of label, fields and the fallback (evidence order: `test-first`); browser check on `running-work` → `decisions.open` (evidence order: `none`).

Removed or replaced: the keys `1`–`9`, `c` and `l` (not in the design; a design line in `navigation.adoc#keys` and the operator's word first, then they may return as a deliverable); the toast "Decided: <option>" (it is the `FormStatus` line); the layout toggle and the blocks/reversible marks of a row; D4 and D5 move to `web-decision-announcer` (below); D6 moves to the web chat package.

**Content of the carved plan `web-decision-announcer` (draft for the orchestrator)**
- D4 own answer confirmed: the answer request carries a client request id and `decision.answered` with that id confirms; other events produce "answered elsewhere" with channel, device and option label (`decisions.adoc#outcomes`).
  - Done when: `decisions.spec.js` own answer, and a `POST /mock/broadcast` from `cli` shows the "answered in the terminal" line (evidence order: `test-first`).
- D5 one notification for a burst, none after a reconnect: an announcer in a store, not in the view (`DecisionView.svelte:289` does `new Notification` inline today), keeps the announced set, primes it silently at start, reload and reconnect, makes one notification for decisions within one second (`decision.notify.several`, `{count} decisions wait for you`, opens the list; one opens that decision), drops a decision answered before the notification shows, and never carries content. Add `decision.notify.several` to `decisions.{en,de}.properties`.
  - Done when: unit tests with a fake clock and notification; e2e through the mock's decision events; the row `web-moving-between-plans` in `doc/plans/README.adoc` is set to `done` by the last of the D2 navigation engine, this deliverable and `PLAN-59` D4 (evidence order: `none`).

Implements: `doc/design/shared/decisions.adoc` § Where the Recommendation Stands, Decisions in Parts, Announcing a Decision, When It Ends Elsewhere; `doc/design/web-app/navigation.adoc` § Lenses and the Rail, Addresses and History, Keys and Focus; `doc/design/web-app/components-fields.adoc` § Submitting.

Dependencies: D1 foundation (RowLink, Chip, CountBadge, RadioGroup/CheckGroup, Compose, TextArea, FormStatus, Button hierarchy), D2 navigation engine (column engine, `/decisions/{id}`, `replaceState` fill, `Enter`/`j`/`k` keys). The chat part waits for the D4 web chat package, which also takes the `chatBusyId` and revision-guard tests (D6 a and b: per-decision busy marker, late proposal answer after a revision, `endTheChat` generation guard) as Done-when of its decision-chat adoption.

New claim bullets:

New claims of this re-grounding are in `## Claim Labels`.

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- **Chat adoption:** `PLAN-137` (web chat places) replaces every `ChatPanel` call site and the project-onboarding chat drawer and deletes `ChatPanel`. This plan drops its own chat deliverable or chat lines; it only places the `Chat` side column in its redesigned layout where the design shows it.
