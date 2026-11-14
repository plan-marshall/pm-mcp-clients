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
3. **What PLAN-131 left (from the landing of PLAN-131, folded 2026-11-13 on the operator's decision)**
 - What: read `landings/PLAN-131.md`; lines are as read at `6e8c29af`. This deliverable changes behaviour where named (the restyle constraint does not apply to it).
   - **One reconnect.** `UnreachableView.svelte:24-49` and `ListenerOffView.svelte:24-51` copy the same interval, `isReconnecting` and `isMounted`
     code, and `ListenerOffView.svelte:47` calls `initRuntime()` directly while `UnreachableView` uses `retryRuntime`: two initializations can run
     side by side. Extract one reconnect helper in `lib/stores/runtime.ts`, used by both views through `retryRuntime`; one shared figure-height
     constant replaces `FIGURE_HEIGHT = 180` in `RevokedView.svelte:14`, `ListenerOffView.svelte:22`, `UnreachableView.svelte:22`.
   - **`shellGone`.** `+layout.svelte:236` guards a retry after the shell is gone during the catalogue wait (`Promise.race`, `:234`); no test
     unmounts inside that window. Add the test (unmount before the wait ends; `initRuntime` is never called).
   - **The held figure.** `heldFigure.ts:32-41` sets `requested = true` before the fetch, so one failed fetch at start disables the figure for the
     session: retry it on the next request.
   - **Rail bear size.** `NavRail.svelte:379` `aspect-ratio: 258 / 200` repeats `mascotFiles.ts:50`; a `NavRail.test.ts` guard compares the rule
     with the bust's size.
   - **Test tree.** `BrandMark.test.ts:227` reads every file under `SOURCE_DIR` as UTF-8, now including the `.webp` files (`uiIcons.test.ts:155`
     skips them via `RASTER_IMAGE`); `walk` is copied in `BrandMark.test.ts`, `uiIcons.test.ts`, `requestLimit.test.ts`: one shared "text source
     files" helper. `MascotFigure.test.ts:68-73` checks only size: assert the WebP signature of every mascot file and that no source or build
     output file carries a `data:` URL (the D1 Done-when of PLAN-131). `routes/dev/components/+page.test.ts:202-225` takes the first three
     `empty-state` nodes by page order: select them by test id.
   - **BrandMark.** PLAN-131 dropped the runtime refusal of a colour `BrandMark` at an undelivered size (`BrandMark.svelte:~35`) for a
     compile-time type; the spec said keep it: restore it beside the type.
   - **Pairing hero.** The `eager` hero (`PairingView.svelte:183-185`) is fetched on phones inside a hidden wrapper: load it only where it shows.
   - **Flaky operator test.** `NeedsItemCommandsTest.java:111` draws the epic title from `Generators.nonEmptyStrings()`, which can yield a blank
     string; `OverviewFormatting.nameOf` (`:395-398`) then prints the id while the test expects the cleaned title. Draw a non-blank title and add a
     deterministic blank-title case.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Tests: one reconnect path (a test fails if `ListenerOffView` starts its own `initRuntime`); the `shellGone` window; a held-figure retry after
     one failed fetch; the rail bear guard; the WebP signature and `data:` scan; the BrandMark runtime refusal; the dev-page fixtures by test id.
     Each fails on a revert. Evidence order: `test-first`.
   - `NeedsItemCommandsTest` passes with a blank-title case and a non-blank generator (evidence order: `none`).
 - Implements: `entry.adoc` § `#unreachable`, `#listener-off`; `brand-mark-mascot.adoc` § assets.
4. **Browser evidence**
 - What: the four pages (pairing, revoked, listener-off, unreachable) in a browser, desktop and phone, both locales and themes, with a revoked device, with the network listener off and with the machine unreachable.
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
- OBSERVED (re-grounded 2026-11-13 at `6e8c29af`): PLAN-131 created `ListenerOffView.test.ts` (132 lines) and a new `UnreachableView.svelte` (+ test), which
  covers the hidden open view (`+layout.svelte:364-371`) with hand-styled `h1`/`p` as `entry.adoc` draws "a page of its own". Deliverable 2's
  "a page that says the machine is not reachable uses the `ErrorState` pattern" applies to `UnreachableView` too: decide at outline whether
  `ErrorState` can carry the page-of-its-own layout; if not, record it as a finding. `lint:vocabulary` covers `UnreachableView.svelte`.
- Verify-first clause: re-read the three views after PLAN-45, PLAN-130, PLAN-131, PLAN-150 and PLAN-151 landed; take the PLAN-151 lint list as the work list.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/PairingView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/PairingView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/PairingView.scenarios.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/RevokedView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/RevokedView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/ListenerOffView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/ListenerOffView.test.ts` — present at `6e8c29af` (PLAN-131)
- OBSERVED: `pm-web-app/src/lib/views/UnreachableView.svelte` — present at `6e8c29af` (PLAN-131)
- OBSERVED: `pm-web-app/src/lib/views/UnreachableView.test.ts` — present at `6e8c29af` (PLAN-131)
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at `6e8c29af` (PLAN-131 residue)
- OBSERVED: `pm-web-app/src/routes/+layout.svelte` — present at `6e8c29af` (PLAN-131 residue)
- OBSERVED: `pm-web-app/src/routes/layout.test.ts` — present at `6e8c29af` (PLAN-131 residue; the `shellGone` test)
- OBSERVED: `pm-web-app/src/lib/stores/heldFigure.ts` — present at `6e8c29af` (PLAN-131 residue)
- OBSERVED: `pm-web-app/src/lib/stores/heldFigure.test.ts` — present at `6e8c29af` (PLAN-131 residue)
- OBSERVED: `pm-web-app/src/lib/components/NavRail.svelte` — present at `6e8c29af` (PLAN-131 residue)
- OBSERVED: `pm-web-app/src/lib/components/NavRail.test.ts` — present at `6e8c29af` (PLAN-131 residue)
- OBSERVED: `pm-web-app/src/lib/components/BrandMark.svelte` — present at `6e8c29af` (PLAN-131 residue)
- OBSERVED: `pm-web-app/src/lib/components/BrandMark.test.ts` — present at `6e8c29af` (PLAN-131 residue)
- OBSERVED: `pm-web-app/src/lib/components/MascotFigure.test.ts` — present at `6e8c29af` (PLAN-131 residue)
- OBSERVED: `pm-web-app/src/lib/components/uiIcons.test.ts` — present at `6e8c29af` (PLAN-131 residue)
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.test.ts` — present at `6e8c29af` (PLAN-131 residue)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/NeedsItemCommandsTest.java` — present at `6e8c29af` (PLAN-131 residue)
- OBSERVED: `pm-web-e2e/tests/pairing.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/entry.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/pairing.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/pairing.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/entry.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/entry.adoc` — present at HEAD 1cc0e10e

## Dependencies and Sequencing

- Depends on: `PLAN-45` landed; `PLAN-131` landed (same three views); `PLAN-150` (`CodeField`, `Chip` via PLAN-148), `PLAN-151` (the count).
- Overlaps with:
  - `PLAN-130`, `PLAN-131` (shipped, #414 / #422): `PairingView`, `RevokedView`, `ListenerOffView`, `UnreachableView`. This plan follows both.
  - `PLAN-45` (running): `ListenerOffView`.
  - `PLAN-85` (parked): the pairing page and the theme controls; re-grounded on this plan (and on option A).
- Adjacent to: `PairingQr`, `BrandMark`, `MascotFigure` (unchanged here); the terminal's entry flow.
- Unblocks: PLAN-85; the gate "views package implemented" of PLAN-29, 41 and 65. May be merged into `PLAN-156` at staging; it is kept apart because its overlaps (PLAN-131, 85) differ.

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD. Run `corpus surfaces` against what landed since. Settle each HYPOTHESIS. Re-check the placement decision of the theme and language controls.

## PLAN-133 Landing Residue (Amendment 2026-11-13)

- Premise: since `a139ec62` the web chat (entry, plan, epic, review, ended work) was broken against the mock until `PLAN-161`
  (chat bridge). RESOLVED 2026-11-14: `PLAN-161` (#429, `dc811fb3`) landed and removed `PLAN-148`'s `test.fixme`; no `fixme` remains in
  `pm-web-e2e/tests`. Still open: a chat that is not seeded is never OPENED by the clients (GET only) — `PLAN-135`/`PLAN-138` own that;
  an e2e failure on an unseeded chat is theirs, name it in the pull request. An e2e failure at this plan's head that traces
  to the chat contract is not this plan's to fix: name it in the pull request with `PLAN-161` as owner.
- The scenario fixtures are validated at load against the B1 catalogue (unknown chat keys fail closed): a fixture edit in a `chats` block
  must satisfy it.

## PLAN-148 Landing Residue (Amendment 2026-11-14)

- `Chip` renders nothing for an empty value, is named `Copy {value}`, and truncates at `max-inline-size: 100%`: a long command to copy would be cut. Re-check the pairing page's two commands against that (wrap, or a copy button beside full text).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-157-web-views-entry-pages.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message.
The orchestrator owns every other ledger write (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

## PLAN-131 Residue Fold 2026-11-13 (Amendment)

From `landings/PLAN-131.md` (operator, AskUserQuestion — "All into PLAN-157"): deliverable 3 takes the reconnect duplication (and
`ListenerOffView`'s direct `initRuntime`), the untested `shellGone` window, the held-figure retry, the rail bear guard, the test-tree items, the
BrandMark runtime refusal, the eager pairing hero and the flaky `NeedsItemCommandsTest`; the browser evidence is now deliverable 4 and covers
the unreachable page. Surface +16 paths. Four deliverables across `pm-web-app` and one `pm-operator` test; runbook lane (operator 2026-11-13).
