# PLAN-110: Web: Remove "Set as Browser Start Page" and Every Part of It

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, handed off as a direct Claude Code session (operator instruction
> 2026-11-06; not the runbook lane, not `/plan-marshall`). Code-only: no design session.
> This spec is SELF-SUFFICIENT: the prompt below is a one-line pointer and carries no brief.

## Objective

Operator design review of 2026-11-06 (`design-review-002.md`, archived under `inbox/archive/design-review/`): remove the
button "Als Startseite des Browsers festlegen" ("Set as browser start page") from Running work together with the whole
function — every artifact, not only the button. The toggle is the only writer of the `localStorage` key `pm_start_route`;
after the removal nothing reads or writes it, and `landingRoute` / `openStartPage` land by the rule alone. Pre-1.0: no shim;
a stale `pm_start_route` in a browser is harmless. This matches the design
(`doc/design/web-app/needs-and-find.adoc:17-21`: the list of what waits at `/` becomes the start page; "a remembered place
still wins" means where the operator left it, not this toggle). No design document names the toggle, so no design text
changes.

## Deliverables

1. **Remove the toggle, its store and its texts**
   - What: `pm-web-app/src/lib/views/OverviewView.svelte`: the imports `rememberedStartRoute` / `setRememberedStartRoute`
     (:29-30), `startPageIsOverview` (:43, its read in `onMount` :193-195 and its uses :591, :596), `toggleStartPage()`
     (:365-368), the button `data-testid="start-page-toggle"` (~589-599) and its CSS (`.start-page-toggle`, `:hover`,
     `--active`, ~1244-1265); the wrapper `.sidebar-header__actions` stays (it also holds the loading Spinner), and the
     heading block "Laufende Arbeit" is checked for layout (the title does not stay squeezed to two lines).
     `pm-web-app/src/lib/stores/runtime.ts`: `rememberedStartRoute()` (:323) and `setRememberedStartRoute()` (:336), the
     branch `remembered` in `landingRoute()` (:308-311) and the doc comments that name `pm_start_route` or the "remembered
     start route" (:282-295 the `landingRoute` comment, :286, :366-368 at `openStartPage`) are removed or reworded;
     `landingRoute`, `openStartPage` and the brand click in `NavRail.svelte` stay. `pm-api/src/main/resources/messages/overview.{de,en}.properties`
     (:75-76): `overview.start_page.set` and `overview.start_page.is_default` are removed (no other consumer);
     `OverviewView.test.ts` (:354-368, the toggle test) and `pm-web-e2e/tests/overview.spec.js` (:172-185, "remembers Running
     Work as the browser start page across a reload") are removed. `runtime.test.ts` stays unchanged (its `openStartPage` /
     `landingRoute` cases test the rule).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - `git grep -n "pm_start_route\|rememberedStartRoute\|setRememberedStartRoute\|start_page\.\(set\|is_default\)\|start-page-toggle\|startPageIsOverview"`
       finds nothing in `pm-web-app`, `pm-web-e2e` and `pm-api` (evidence order: `none`)
     - A unit test shows `landingRoute` with a stale `pm_start_route` in `localStorage` lands by the rule alone, and that a
       fresh paired browser and the brand click land by the rule (evidence order: `test-first`)
     - `npm run quality` in `pm-web-app` and the overview e2e spec pass (evidence order: `none`)
     - Browser check shows the heading of Running work in `de` and `en`, light and dark, at 1440 × 900 and 390 × 844 without the
       button and with the title on its natural lines; baseline and after screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open
       (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order:
       `none`)

## Standing Constraints

- Build and test through Maven and npm only (`CLAUDE.md`); beside the session's own checks these builds pass at the head:
  `./mvnw -pl pm-web-app verify`, `./mvnw -pl pm-web-e2e verify`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin. Frontend standards (`CLAUDE.md`, `PLAN-80`) apply; a test waits on a condition, never on a
  timer. Both language packs lose both keys (catalogue parity).
- Ports of this plan: mock web port `21090`, app port `21091` (`AGENTS.md` § Ports, `PLAN-76`). Never stop or reuse a process
  this worktree did not start.
- Browser check following `AGENTS.md` § Browser checks (the session's browser tool; none available means the browser clause
  is NOT met — a blocker, never replaced by a unit test).
- **Sequencing:** `runtime.ts` is also listed by the running `PLAN-106` (a different region: settings reads / drain), so merge
  `main` and rebase before landing; start after `PLAN-106` has landed. `PLAN-49` (staged, gated on design) lists
  `overview.{de,en}.properties` and rewrites the landing rule: its spec says `pm_start_route` is gone, and it must not take
  the toggle over.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The pull request is merged only on the operator's word; commits end with `Co-Authored-By: plan-marshall
  <noreply@cuioss.de>`; the pull request body carries no generator footer.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`): an open Sonar issue is
  fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` and transmitted through
  `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`.
  The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed.

## Claim Labels

- OBSERVED: the only `setItem` / `removeItem` of `pm_start_route` is `runtime.ts:341,343`, reached only through
  `setRememberedStartRoute`, called only at `OverviewView.svelte:367`; `pm-web-e2e` only reads it
  (`overview.spec.js:178`); no writer in `pm-operator`, `pm-api` or `doc/` — verified by a read-only repository-wide search at
  HEAD 60b4bfa5 on 2026-11-06
- OBSERVED: the line references of the Objective and Deliverable 1 were checked at HEAD 60b4bfa5 on 2026-11-06 (the button
  block is 589-599, `landingRoute` doc comment 282-295); `overview.start_page.*` has no consumer in `pm-api` tests or docs
- OBSERVED: no design document names the toggle or `pm_start_route`; the "remembered place" prose in
  `needs-and-find.adoc:21`, `README.adoc:56` and `entry.adoc:126` is a different concept — verified at HEAD 60b4bfa5
- HYPOTHESIS: the heading block `.sidebar-header` (an `h2.sidebar-title` plus the Spinner) renders the title on one line once
  the button is gone — confirm/refute in the browser check (verify-at-outline)
- Verify-first clause: re-read the named ranges at the current HEAD; `PLAN-106` (running) edits `runtime.ts`.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/OverviewView.svelte` — present at HEAD 60b4bfa5
- OBSERVED: `pm-web-app/src/lib/views/OverviewView.test.ts` — present at HEAD 60b4bfa5
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at HEAD 60b4bfa5
- OBSERVED: `pm-web-app/src/lib/stores/runtime.test.ts` — present at HEAD 60b4bfa5 (a test for the stale-key case is added here or in `OverviewView.test.ts`)
- OBSERVED: `pm-api/src/main/resources/messages/overview.de.properties` — present at HEAD 60b4bfa5
- OBSERVED: `pm-api/src/main/resources/messages/overview.en.properties` — present at HEAD 60b4bfa5
- OBSERVED: `pm-web-e2e/tests/overview.spec.js` — present at HEAD 60b4bfa5

## Dependencies and Sequencing

- Depends on: `PLAN-106` (running; `runtime.ts`)
- Overlaps with: `PLAN-49` (`overview.*.properties`, landing rule; staged and gated), `PLAN-51` (`runtime.ts`; staged and
  gated), `PLAN-70` (the overview screenshots; the button leaves its evidence)
- Adjacent to: none

## Hand-Off Command

Lane: none — a direct Claude Code session (operator instruction 2026-11-06). Start it in its own worktree and branch of
this repository, after `PLAN-106` has landed, and give it exactly this prompt (the orchestrator prints the absolute spec path
at emit time):

```text
Implement the staged plan PLAN-110 (remove the browser start page toggle) of the epic ui-improvements. Read the whole spec
first: <absolute path of this file>. Follow its deliverable, Done-when clauses and Standing Constraints exactly, working
test-first where a clause says so, through Maven and npm only. Run the browser check it names. Open one pull request, answer
its review comments, and report. Do not edit anything under .plan/orchestrator/ except your own inbox message
(orchestrator inbox write), and do not merge.
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under
`.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write —
and reports its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned write
mechanism are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
