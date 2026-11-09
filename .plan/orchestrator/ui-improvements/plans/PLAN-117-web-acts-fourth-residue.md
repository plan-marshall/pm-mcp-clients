# PLAN-117: Web: What the Acts Third Residue Plan Left Open

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane (operator rule 2026-11-07).
> This spec is SELF-SUFFICIENT: the prompt below is a one-line pointer and carries no brief.
> Staged 2026-11-07 from the landing analysis of PLAN-106 (`landings/PLAN-106.md`: PR #360 merged with 0 unresolved threads and
> 0 Sonar issues, but two of its own review fixes are pinned by no test, the unknown running count was decided without a check
> or a design text, and a reply on PR 331 lost its code spans). One pull request on purpose. Keep it under 30 files.
> **Starts after PLAN-110 and PLAN-109 have landed** (they touch `runtime.ts`, `work-actions.adoc`, `+layout.svelte`).

## Objective

PLAN-106 derived the act ids from the tables, made the unknown running count and the unknown withdraw states real, and pinned
the stores' count-only drain. It left the review fixes of its own pull request without a test that fails when they are
reverted, the unknown count without a stated rule, and three tests that prove less than their name. This plan closes them so
that the fixes cannot be reverted green and the design says when the count is unknown.

## Deliverables

1. **Two review fixes pinned by tests that fail when reverted**
   - What: (a) the lifecycle epoch advance in `SystemView.svelte` (~281, ~289, `advanceLifecycleEpoch()`): a stale status
     read that resolves after a successful act in the system view does not overwrite the result. (b) `isStopped: true` after a
     successful stop-now in `+layout.svelte` (`confirmStopNow`): `layout.test.ts` ~768 passes through the rejected-`stopRuntime`
     refresh path; add the success path. (c) The omitted-`running` test of `EpicView.test.ts` (`delete resource.running`)
     asserts the badge only: assert the raise dialogue too. (d) The act-table test drives the epic subject through the panel
     like the plan subject (an injected act in the epic table appears in the overflow menu and is applied), and
     `handledActIds` is compared with the table with the exclusion only (the second assertion is true only because the epic
     table holds no `decision`).
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - Each test of (a)-(d) fails when the code it covers is reverted (the epoch advance removed, `isStopped` not set on success, the dialogue reading another text, an epic act not handled), the reverted code and the failing assertion quoted in the pull request body (evidence order: `mutation-after`)
     - No test waits on an arbitrary timer (evidence order: `none`)
2. **The unknown running count: checked against the daemon, stated in the design**
   - What: PLAN-106 kept `running?: number | null` (`client.ts` ~598) and the `?` badge (`EpicView.svelte` ~583) without checking
     whether the real `pm-mcpd` omits `running` and without saying so in the design. Read the daemon's epic resource
     (`plan-marshall/plan-marshall-mcp` and `plan-marshall/pm-mcp-core` specifications, read-only; if the repositories are not
     checked out, record `not checkable` and the owner) and state in `doc/design/shared/epic-view.adoc` (~47 the head bullet,
     ~81 the `at_once` row) and `doc/design/shared/work-actions.adoc` (~222, "where the count is not known") when the count
     is unknown and what the client shows (`? of N at once`), in the German pack too where a text is named.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The documents pass `pm-documents:ref-documentation` docs review and `pm-documents:ref-asciidoc` `validate` and
       `verify-links`; the daemon check or `not checkable` with its owner is in the pull request body (evidence order: `none`)
3. **The replies and the wording**
   - What: (a) the in-thread reply `4493497437` on PR 331 thread `4484094188` lost its code spans ("because  was typed as ,"):
     post a clean reply in the thread (code spans intact) naming `running?: number | null`, the dialogue text and the test.
     (b) German "Epik" remains in `pm-api/src/main/resources/messages/epic_view.de.properties:32,33,35` ("Der Epik-Prozess",
     "Epik-Ansicht"): "Epic", with the catalogue tests green.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - `git grep -n "Epik" -- pm-api/src/main/resources/messages` names none (evidence order: `none`)
     - The pull request lists every thread and body item it answers, none of its own unresolved (list the threads through
       the GraphQL `reviewThreads` read for the own pull request and PR 331), and the open-issue read at the head is
       recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a
       confirmed `0` (evidence order: `none`)

## Standing Constraints

- Build and test through npm scripts and Maven only (CLAUDE.md); `./mvnw -pl pm-web-app verify` and, for the catalogue,
  `./mvnw -pl pm-api verify` pass AT THE FINAL HEAD (state the head the gate ran at; PLAN-106 cited a log four commits old).
- No new dependency or plugin. Frontend standards (CLAUDE.md, PLAN-80): no `innerHTML` / `{@html}`; a test waits on a
  condition, never on a timer.
- Ports of this plan: `21170` and `21171` where a deliverable starts a process; never stop a process this worktree did not
  start.
- Every `test-first` clause cites a FAILING and a PASSING run with the assertion quoted in the pull request body (the logs
  are git-ignored; a reader of the pull request cannot open them); every `mutation-after` clause cites the reverted code.
- ZERO Sonar findings (the operator, 2026-11-05): `sonar: configured` (project `plan-marshall_pm-mcp-clients`); 0 open
  new-code issues at the merge head, fixed in a commit of this pull request, none dismissed without the operator's word;
  state `sonar_open_new_issues=N` by that name.
- Merge rule (the operator, 2026-11-02): at least one CodeRabbit review of some head with its findings handled, the findings
  of EVERY review handled, the review BODIES (nitpicks included) answered in the pull request; reply IN the thread; state
  `unresolved_review_threads=N` and `review_body_findings_total/handled` counting every actionable item and nitpick. The
  pull request is merged only on the operator's word; commits end with `Co-Authored-By: plan-marshall
  <noreply@cuioss.de>`; the pull request body carries no generator footer.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.

## Claim Labels

- OBSERVED: PR 360 has 3 threads, 0 unresolved; PR 331 10 and PR 320 16, 0 unresolved; the reply `4493497437` on PR 331 is
  garbled; the Sonar gate passed with 0 new issues — read by the orchestrator through GraphQL and REST on 2026-11-07
- HYPOTHESIS: `SystemView.svelte:281,289` and the stop-now success path are pinned by no test (`git grep lifecycleEpoch` in
  tests names none), `epic-view.adoc` and `work-actions.adoc` never say when the count is unknown, "Epik" stands in
  `epic_view.de.properties:32,33,35` — by a read-only agent's reading of `3bdd02a2`, not run; confirm/refute at the files named
  - verdict: corroborated | checked_at: 2fc2c214097e0359ac0e9b9eba48771bee7eaa96 | by: ui-improvements/analyze | rescoped: n/a | evidence: at bf24091c^ (=2fc2c214): layout.test.ts:246-290 asserted only the banner (no isStopped pin); epic-view.adoc had no unknown-count rule; epic_view.de.properties:32,33,35 read Epik
- Source: `landings/PLAN-106.md`.
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD (PLAN-109, 110, 111 land first)
  and confirm or refute every HYPOTHESIS; a refuted cause re-scopes the deliverable and is reported.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/SystemView.svelte` — present at HEAD 3bdd02a2
- OBSERVED: `pm-web-app/src/lib/views/SystemView.test.ts` — present at HEAD 3bdd02a2
- OBSERVED: `pm-web-app/src/routes/+layout.svelte` — present at HEAD 3bdd02a2
- OBSERVED: `pm-web-app/src/routes/layout.test.ts` — present at HEAD 3bdd02a2
- OBSERVED: `pm-web-app/src/lib/views/EpicView.test.ts` — present at HEAD 3bdd02a2
- OBSERVED: `pm-web-app/src/lib/actions/byState.test.ts` — present at HEAD 3bdd02a2
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.test.ts` — present at HEAD 3bdd02a2
- OBSERVED: `doc/design/shared/epic-view.adoc` — present at HEAD 3bdd02a2
- OBSERVED: `doc/design/shared/work-actions.adoc` — present at HEAD 3bdd02a2
- OBSERVED: `pm-api/src/main/resources/messages/epic_view.de.properties` — present at HEAD 3bdd02a2

## Dependencies and Sequencing

- Depends on: `PLAN-106` (shipped); starts after `PLAN-110` and `PLAN-109` have landed (`runtime.ts`, `work-actions.adoc`,
  `+layout.svelte`)
- Overlaps with: `PLAN-109` (`work-actions.adoc`, `+layout.svelte`, `control.*.properties`), `PLAN-110` (`runtime.ts`),
  `PLAN-111` (`+layout.svelte`, `layout.test.ts`), `PLAN-113` (D2 rewrites `work-actions.adoc` and `epic-view.adoc` text)
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode) — a small plan (operator rule 2026-11-07). Start it after `PLAN-110` and `PLAN-109` have
landed.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-117-web-acts-fourth-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source, tests and design documents. It creates and edits NO
file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message — the orchestrator owns every other ledger
write — and reports its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned
write mechanism are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
