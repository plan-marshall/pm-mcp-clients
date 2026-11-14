# PLAN-49: Web: Attention Inbox as Home

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Answer "what needs me now" in one screen with zero clicks: the attention inbox of UX-P-01 as the home page, derived on the client from the stores that exist (UX-S-02; serves UC-PLN-7).

Source of this plan (read before scoping):

- `doc/claude_review/ux/plan.adoc (UX-S-02)`
- `doc/claude_review/ux/proposals.adoc#UX-P-01`
- `doc/claude_review/use-cases/proposals.adoc#UC-P-09`

## Deliverables

1. **Attention store**
   - What: `lib/stores/attention.ts` holds the runtime's list of what waits (decisions, holds, failed landings, setup questions, findings to review, credentials, due cleanup, any other operator wait) as `GET /api/v1/machine/needs-operator` serves it, in the order served (blocking first, then oldest), and updates on `needs_operator.changed`; an item resolved in another client leaves with a quiet notice; reports are never items.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - `attention.test.ts`: each kind becomes an item with its act; order; an answered decision leaves (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: `/` lists the waiting items blocking first; each act opens its place; an answered decision leaves; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
2. **Inbox view and route**
   - What: `lib/views/InboxView.svelte` on the triage template at `/`; `openStartPage()` opens it; empty state "Nothing waits for you." with the running work per project; the rail item and the tab title carry the total.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `inbox.spec.js` on `fully-onboarded`: the five items of UX-V-02, blocking first; each act opens its place (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: `/` lists the waiting items blocking first; each act opens its place; an answered decision leaves; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)

## Standing Constraints

- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project `plan-marshall_pm-mcp-clients`), read at the pull request head after the push (`unknown` for a failed read, never `0`); every open issue is fixed in a commit of THIS pull request, and a dismissal is made only on the operator's selection. State `sonar_open_new_issues=N` in the landing message. This is a merge gate, not a deliverable's Done-when.
- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20490 PM_WEB_PORT=20491 ./mvnw -pl pm-web-e2e verify -Pe2e`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20490`, app port `20491` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- UC-PLN-7 is `AGREED` (`plan-marshall-documentation` PR #135, 2026-10-30). The list is the runtime's (`GET /api/v1/machine/needs-operator`, built by `PLAN-82`: blocking first, then oldest; the machine's own `credential` and `cleanup_due` items included), kept in the order served; this plan derives nothing from the stores. Whether the view is also the start page `/` is settled by the design `PLAN-82` writes and the operator accepts: where it differs from this spec, the design wins and the spec's route text is corrected in the same pull request. If `PLAN-82` has not landed, this plan stops as a blocker. The phone's Inbox item that `PLAN-39` deferred (PR #274) is added here.
- Re-grounded after `web-brand` (PLAN-27, HEAD 4c39c0f8): the tab icon is switched by `src/lib/favicon.ts` from `runtimeStore.openDecisionsCount` (wired in `+layout.svelte`), the same count the header badge and the tab title carry. When the attention store exists, point the badge, the tab title and the tab icon at its item total in this plan, so the three never disagree; `favicon.test.ts` is the test to extend.
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20490"`), then `PM_MOCK_WEB_PORT=20490 ./pm-web-app/start.sh -p 20491` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20490/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/ux/plan.adoc (UX-S-02)`, `doc/claude_review/ux/proposals.adoc#UX-P-01` — read at `doc/claude_review/ux/plan.adoc`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: UX-S-02 at ux/plan.adoc:24, UX-P-01 at ux/proposals.adoc:12, UC-P-09 at use-cases/proposals.adoc:176; all present at HEAD
- Re-grounded at HEAD 07104678 (2026-10-30) after PR #278, PLAN-39 (PR #274) and `plan-marshall-documentation` PR #135: UC-PLN-7 is `AGREED` and the runtime serves the list, so "derived on the client from the stores that exist" no longer holds. The operator decided two views against one combined entry (what waits; go to work by name), and the web "what waits" view is "a route of its own, a destination of the rail with its count" per the package `needs-and-find`; the rail from `PLAN-39` exists and the rail item and tab title carry the total. `PLAN-83` and `PLAN-52` share the running-work rows with this view.
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: MachineApiPaths.java:26 serves /api/v1/machine/needs-operator via NeedsOperatorPage; rail exists (nav.ts, NavRail, nav-inbox.svg); client.ts has no getNeedsOperator yet
- Re-grounded note at HEAD 94bbefe1 (cleanup 2026-11-02): attention.ts, InboxView.svelte and inbox.spec.js are still absent; the claim of the spec narrative that the rail exists holds (nav-inbox.svg icon and routes.ts present); routes.ts has no inbox/attention/needs route yet; OverviewView.svelte is still home.
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: attention.ts, InboxView.svelte, inbox.spec.js absent; nav-inbox.svg present; AppRoute (routes.ts:20) has no inbox route; landingRoute (runtime.ts:380) lands on overview
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: procedural verify-first instruction; no code fact to settle by reading
- HYPOTHESIS: `fully-onboarded.json` serves no `needs-operator` resource (only `running-work.json` does), so the browser check names `running-work` or extends the fixture — confirm/refute at `pm-mock-runtime/src/main/resources/scenarios/fully-onboarded.json` § `needs-operator` (verify-at-outline)
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: fully-onboarded.json has 0 needs-operator resources; running-work.json:84,3125,3497,3806 serve it
- HYPOTHESIS: the Event `needs_operator.changed` reaches the web stream with the machine summary in its payload — confirm/refute at `pm-api/src/main/java/de/planmarshall/api/overview/NeedsOperatorChangedEvent.java` § `EVENT_NAME` (verify-at-outline)
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: NeedsOperatorChangedEvent.java:41 EVENT_NAME needs_operator.changed; toMap puts summary under needs_operator; OverviewView.svelte:242 handles it from the web stream
- HYPOTHESIS: `web.rail.needs` and `web.inbox.title` are absent from the catalogue and belong in `common.{en,de}.properties` — confirm/refute at `pm-api/src/main/resources/messages/common.en.properties` § `web.rail` (verify-at-outline)
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: web.rail.needs and web.inbox.title appear in no messages file; only common has web.frame.rail.*; keys are named in doc/design/web-app/needs-and-find.adoc:27,70,82

## Expected Surface


- OBSERVED: `pm-web-app/src/lib/stores/attention.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/stores/attention.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/api/client.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/InboxView.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/views/InboxView.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/views/AppRouter.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/routes.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/routes.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/routeTitles.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/shell/nav.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/shell/nav.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/shell/NavRail.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/shell/NavRail.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/shell/BottomBar.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/favicon.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/favicon.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/routes/+layout.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/routes/+page.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/inbox.spec.js` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-e2e/tests/chrome.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/fully-onboarded.json` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/overview.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/overview.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/needs-and-find.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/RowLink.svelte` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/components/RowLink.test.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/stores/runtime.test.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/running-work.json` — present at `dc811fb3` (added by the cleanup of 2026-11-14)

## Dependencies and Sequencing

- Depends on: `PLAN-43` (`web-page-templates`), `PLAN-08` (`use-case-documents`, shipped), `PLAN-76` (`shared-port-assignment`, shipped), `PLAN-82` (`needs-and-find-contract`)
- Overlaps with: none known at staging
- Adjacent to: none

## Design Review 2026-11-06 (Amendment)

Source: `inbox/archive/design-review/design-review-010.md` § Per-plan instructions (messages 002, 003, 005, 006, 007).
**Gate: D1, D2 and D3 merged** (documents `AGREED`) and the D1 foundation / D2 navigation engine / D3 brand assets packages
that build what this plan uses. Queue status: `parked`; the orchestrator re-grounds this spec (Expected Surface, Claim Labels,
deliverables) when it is restarted. The Expected Surface above is NOT widened now: the new files and component names are the
design sessions' output, and a parked row is not in the disjointness gate population. When restarted:

- `pm_start_route` is gone (`PLAN-110` removes the toggle), so this plan does not take the toggle over; it starts from a
  `landingRoute` without the `remembered` branch.
- What waits is a launcher at `/`, which becomes an `AppRoute` (003).
- Rows: `KindMark` + `ReasonBadge` on the `overview.needs.*` keys, `decision` items as Decision rows (003, 006); each row a
  `RowLink` (007).
- The empty state uses the mascot "nothing waits" pose or the source bust (005).
- The bottom bar becomes What waits · Running work · Decisions · More; Reports moves into More with its unread count (003).

## PLAN-148 Landing Residue (Amendment 2026-11-14)

- First adopter of `RowLink` and `StatusLabel`'s list slot (after `PLAN-149` D6 renames the prop; never write `<StatusLabel slot>`).
- `RowLink` contract at `25137445`: `title` is a plain string on one line with ellipsis; slots `kind`/`tags`/`acts`; only `acts` and a `.chip`
  in `tags` sit above the stretched link (the `kind` slot and any other control in `tags` are covered, `RowLink.svelte:100-103`, `:137`,
  `:146`). The second detail line this plan needs (path / since / blocks, `:111-112`) is not in `RowLink`: add it to `RowLink` here, with
  its stacking.
- a11y to settle as first adopter: `aria-current` sits on the wrapper `<div>` (`RowLink.svelte:47`), not the anchor — move it to the anchor
  (`aria-current="page"`/`true`); `.row-link:focus-within` (`:87-90`) rings the row on a mouse click into a nested control and doubles the
  anchor's own ring — use `:has(a:focus-visible)`. Test in a browser, not by CSS strings.

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-49-web-attention-inbox.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

## Re-grounding 2026-11-10 against the agreed D1–D5 designs (Amendment)

Source: `doc/design/web-app/needs-and-find.adoc` (#start-page, #route, #list-layout), `doc/design/shared/needs-and-find.adoc#waits`, `doc/design/web-app/navigation.adoc` (#lenses, #kinds, #phone), `doc/design/web-app/components-tags.adoc` (#reason-badge, #kind-mark, #tag-places), `doc/design/web-app/brand-mark-mascot.adoc#surfaces`. HEAD 1cc0e10e. The three deliverables below replace the two above.

1. **Attention store.** `pm-web-app/src/lib/stores/attention.ts` holds the runtime's list as `GET /api/v1/machine/needs-operator` serves it, in the order served, kept in step by `needs_operator.changed`; reports are never items; an item resolved elsewhere leaves with the quiet notice `overview.needs.left`. A `getNeedsOperator` method is added to `pm-web-app/src/lib/api/client.ts`.
   - Done when: `attention.test.ts` shows each kind as an item with its act, the served order, and an answered decision leaving (evidence order: `test-first`); `client.test.ts` pins the call and the page (`Show more`, `overview.needs.more`) (evidence order: `test-first`).
2. **What waits at `/`.** `pm-web-app/src/lib/views/InboxView.svelte` is the list column of the lens What waits on the column engine, no object column of its own. Each row is a `RowLink`: the `KindMark` of the object the item belongs to, its name, the item kind as `ReasonBadge` (`overview.needs.*`; none for a `decision` item), path, since, what it blocks, and the act as a button that opens the place (the list performs no act). A due cleanup opens the cleanup job; a credential opens the settings' providers. Empty: `overview.needs.empty`, the mascot pose "nothing waits" (D3 brand), and the running projects (`overview.needs.empty.running`).
   - Done when: `inbox.spec.js` on `running-work` (the scenario that serves `needs-operator`; or `fully-onboarded` once its fixture serves it): blocking first, each act opens its place, an answered decision leaves, nothing waits shows the empty state (evidence order: `test-first`); the Playwright browser check at 1440×900 and 390×844 in `de` and `en`, both themes (evidence order: `none`); the open-issue read at the head (evidence order: `none`).
3. **The shell shows What waits.** `nav.ts` gets the entry `web.rail.needs` as the first rail entry with the total (`! 4`), the phone's bottom bar becomes What waits · Running work · Decisions · More (Reports moves into More and its unread count shows on More), the tab title carries `web.inbox.title`, and the tab icon, the header badge and the title read the same total (`+layout.svelte`, `favicon.ts`).
   - Done when: `nav.test.ts`, `NavRail.test.ts`, `favicon.test.ts` and `routes.test.ts` show the entry, the total and the three consumers agreeing (evidence order: `test-first`); `chrome.spec.js` shows the bottom bar on the phone (evidence order: `test-first`).

Removed or replaced: "derived on the client from the stores" (already struck); the toggle-less `landingRoute` (nothing to take over); a rail entry that disappears when nothing waits (it stays visible, without a count).

Implements: `doc/design/web-app/needs-and-find.adoc` § The Start Page Is What Waits, The Route and the Rail, The List in the Frame; `doc/design/web-app/navigation.adoc` § Lenses and the Rail, The Phone.

Dependencies: D1 foundation (KindMark, ReasonBadge, RowLink, CountBadge), D2 navigation engine (column engine, the route `/` as an `AppRoute`, the remembered-place fill at a bare start), D3 brand assets for the mascot pose (the figure may follow in a later plan if D3 is late; then the empty state ships text and project rows only). Sequence after `PLAN-45` (it holds `routes/+layout.svelte` and `views/`).

New claim bullets:

New claims of this re-grounding are in `## Claim Labels`.
