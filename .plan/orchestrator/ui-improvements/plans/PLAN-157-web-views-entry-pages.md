# PLAN-157: Web Views: The Entry Pages on the D1 Vocabulary (Views Package, Part 5)

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Drafted 2026-11-10 from the AGREED D1 documents and `web-app/entry.adoc`. SELF-SUFFICIENT: the emitted command is a one-line pointer.

## Objective

The pairing page, the revoked page and the page of a turned-off network listener are the first screens a person sees. `PairingView` still has a raw `<input>` for the code; the commands to copy and the acts of the other two pages follow the old pattern.
This plan moves the three pages onto `CodeField`, `Chip`, `Button` and `ErrorState` by the agreed rules, without touching what they show or do. The brand block and the mascot figures on these pages are PLAN-130's and PLAN-131's; the theme and language controls are NOT added
(the README keeps them in the rail foot and the More sheet).

## Deliverables

Read first: `doc/design/web-app/entry.adoc` (§ `#pairing`, `#revoked`, `#listener-off`), `components-fields.adoc` (§ `#special-fields`, `#validation`), `components.adoc` § `#hierarchy`, `shared/paired-devices.adoc` (§ `#revoked-page`, `#listener-off`).

Not in this plan: `BrandMark`, `MascotFigure`, the hero figure and its poses (PLAN-130, PLAN-131); a theme or language control on these pages (README option A; PLAN-85 is re-grounded on it); the pairing protocol.

1. **The pairing page**
 - What:
   - The code `<input>` becomes `CodeField` (monospace and large, `autocomplete="one-time-code"`, any case, with or without the dash); _Pair this browser_ is the one primary, enabled once eight characters are typed.
   - The line under the field for a code that does not work is its `FieldError` (`This code does not work. Get a new one in a terminal of the machine.`); `Pairing this browser …` stays a `Spinner` line.
   - The two commands to get a code are `Chip`s (they copy on click); the certificate fingerprint on the network listener and its _Show in full_ keep their behaviour; the QR code stays `PairingQr`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `PairingView.test.ts`, `PairingView.scenarios.test.ts` and `pm-web-e2e/tests/pairing.spec.js` show: the code accepted in lower and upper case, with and without the dash; the button disabled below eight characters; the error under the field; the command chips copy. Fail on a revert. Evidence order: `test-first`.
   - `npm run lint:vocabulary` reports 0 for `PairingView.svelte` (evidence order: `none`).
 - Implements: `components-fields.adoc` § `#special-fields`; `entry.adoc` § `#pairing`.
2. **The revoked page and the listener-off page**
 - What: `RevokedView.svelte` and `ListenerOffView.svelte` keep their reason lines (the revoker, the time, the idle expiry); their acts follow the hierarchy (one primary, none dangerous); the commands to copy are `Chip`s; a page that says the machine is not reachable
   uses the `ErrorState` pattern of PLAN-45. No form of the revoked page survives a revocation (the page is replaced, not covered).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `RevokedView.test.ts` shows each reason line of the table in `entry.adoc` § `#revoked`; a new `ListenerOffView.test.ts` shows the reason line, the way back and the reconnect. Fail on a revert. Evidence order: `test-first`.
   - `npm run lint:vocabulary` reports 0 for both files (evidence order: `none`).
 - Implements: `components.adoc` § `#hierarchy`; `entry.adoc` § `#revoked`, `#listener-off`.
3. **Browser evidence**
 - What: the three pages in a browser, desktop and phone, both locales and themes, with a revoked device and with the network listener off.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - Browser check (Playwright MCP) of the pairing page, a revoked page and the listener-off page of the mock (the scenarios `pairing.spec.js` and `entry.spec.js` use) at 1440×900 and 390×844, `de` and `en`, both themes; baseline and after screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-web-app verify`, `-Ppre-commit` (review every rewrite), and at the head `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin. Web standards of `CLAUDE.md`: ES modules, flat ESLint, no `innerHTML`/`outerHTML`/`insertAdjacentHTML`/`{@html}` with runtime text, no timer waits in tests, the npm scripts stay; the pairing code is never logged or echoed outside its field.
- A pure restyle: no change to what a page shows, sends or stores; where a rule of the design cannot be met without a behaviour change, record it as a finding in the pull request.
- Every new text comes from the catalogue in English and German; the text guard baseline does not grow.
- Ports of this plan: mock web port 21570, app port 21571 (`20000 + 10 × 157`) (`AGENTS.md` § Ports). Never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- Every `test-first` clause cites a FAILING and a PASSING run; every `mutation-after` clause cites the reverted code and the failing assertion from a log quoted in the pull request body.
- **The merge rule:**
  - At least one CodeRabbit review of some head with its findings handled; every review BODY answered where the reviewer reads it; every thread replied and resolved. If CodeRabbit refuses on quota, stop and ask the operator.
  - The pull request body states `unresolved_review_threads=N` and `review_body_findings_total/handled`, read through `ci pr comments`.
  - The run merges the pull request itself once these gates hold, without a further word of the operator (`CLAUDE.md`, Git Workflow, #423). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): 0 open new-code issues, every open issue fixed in a commit of THIS pull request; state `sonar_open_new_issues=N` in the landing message.
  A dismissal is made only on the operator's selection through `sonar_rest transition`; the lane never records one of its own.

## Claim Labels

- OBSERVED: `PairingView.svelte` (371 lines) has one raw `<input>`, uses `BrandMark`, `Button`, `PairingQr`, `Spinner`; `RevokedView.svelte` (95) and `ListenerOffView.svelte` (113) use `Button` and have no raw tag — read at the files and by `git grep -c`
- OBSERVED: `PairingView.test.ts`, `PairingView.scenarios.test.ts` and `RevokedView.test.ts` exist; `ListenerOffView` has no test; `pairing.spec.js` and `entry.spec.js` exist — read in the directories
- OBSERVED: `PLAN-130` edits `PairingView.svelte` (the brand mark), `PLAN-131` edits `PairingView`, `RevokedView`, `ListenerOffView` (the figures), `PLAN-45` edits `ListenerOffView.svelte` — read by `corpus surfaces`
- OBSERVED: `entry.adoc` shows no theme or language control on the pairing page; `README.adoc` § themes keeps the choice in the rail foot and the More sheet (option A), while `PLAN-85`'s spec places it on these pages — read in both documents and the batch reports
- HYPOTHESIS: the two commands to copy are rendered by a hand-written copy button today, not by `Chip` — confirm/refute at `PairingView.svelte` § commands markup (verify-at-outline)
- HYPOTHESIS: `ListenerOffView` already follows the `ErrorState` pattern after PLAN-45 — confirm/refute at `ListenerOffView.svelte` after PLAN-45 lands (verify-at-outline)
- Verify-first clause: re-read the three views after PLAN-45, PLAN-130, PLAN-131, PLAN-150 and PLAN-151 landed; take the PLAN-151 lint list as the work list.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/PairingView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/PairingView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/PairingView.scenarios.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/RevokedView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/RevokedView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/ListenerOffView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/ListenerOffView.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-e2e/tests/pairing.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/entry.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/pairing.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/pairing.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/entry.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/entry.adoc` — present at HEAD 1cc0e10e

## Dependencies and Sequencing

- Depends on: `PLAN-45` landed; `PLAN-131` landed (same three views); `PLAN-150` (`CodeField`, `Chip` via PLAN-148), `PLAN-151` (the count).
- Overlaps with:
  - `PLAN-130`, `PLAN-131` (staged): `PairingView`, `RevokedView`, `ListenerOffView`. This plan follows both.
  - `PLAN-45` (running): `ListenerOffView`.
  - `PLAN-85` (parked): the pairing page and the theme controls; re-grounded on this plan (and on option A).
- Adjacent to: `PairingQr`, `BrandMark`, `MascotFigure` (unchanged here); the terminal's entry flow.
- Unblocks: PLAN-85; the gate "views package implemented" of PLAN-29, 41 and 65. May be merged into `PLAN-156` at staging; it is kept apart because its overlaps (PLAN-131, 85) differ.

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD. Run `corpus surfaces` against what landed since. Settle each HYPOTHESIS. Re-check the placement decision of the theme and language controls.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-157-web-views-entry-pages.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message.
The orchestrator owns every other ledger write (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).
