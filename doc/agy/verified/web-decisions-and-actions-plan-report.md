# Completion Report: `web-decisions-and-actions`

**Plan:** [`doc/agy/implemented/web-decisions-and-actions-plan.md`](implemented/web-decisions-and-actions-plan.md)
**Work package:** `web-actions` (`doc/plans/implemented/web-actions.adoc`), plus the remediation of `web-decisions`
**Branch:** `feat/web-decisions-actions`
**Pull request:** [#151](https://github.com/plan-marshall/pm-mcp-clients/pull/151)
**Worktree:** `/Users/oliver/git/worktrees/pm-mcp-clients-web-decisions-actions`

## 1. Executive Summary

The web app now carries the whole control surface of the clients epic: decisions announce themselves and
offer a one-step recommendation and a chat; plan and epic actions are mounted in the plan view, the epic
view, and Running Work; holds and settings conflicts are resolvable without leaving the view; the epic
view shows its workstreams, sources, landings, registers, token split, and events; and a findings/watches
review is available over the epic's registers.

Every user-facing text is a catalogue key in English and German, and `npm run check` reports zero errors
and zero warnings. No file in `pm-operator` or `pm-mock-runtime` was changed.

## 2. Delivered Artifacts

### 2.1 `pm-web-app`

| File | Change |
|---|---|
| `src/lib/views/DecisionView.svelte` | Counted tab title through the runtime store; one-time notification banner and background desktop notifications; one-step recommendation reveal for costly/irreversible decisions; per-part answers with dependency gating and automatic reopening; `answered_elsewhere`, `revised`, and `withdrawn` lines; docked `ChatPanel` with proposed-answer read-back and chat closure when answered elsewhere. |
| `src/lib/components/ControlActionsBar.svelte` | Pause dialogue with point (`now`/`task`/`phase`); end/abandon/abort confirmation with required reason and pull-request close choice; epic `Next plan` with the raise-scope dialogue; named `Automatic` value toggle; common-files dialogue; archive check with carried findings/watches; integrity-pause abort naming. |
| `src/lib/components/HoldResolutionModal.svelte` | Holds of plans, epics, and landings with runtime-offered acts; parameterised grant/PR labels; irreversible-act confirmations with what follows; released/still-held outcome after an act. |
| `src/lib/components/PlanSettingsModal.svelte` | `conflict` dialogue with the current value and the operator's value, `Keep the current value`, and `Apply mine again` against the current revision; refusal/too-late notice. |
| `src/lib/components/FindingsReviewModal.svelte` | **New.** Register list and per-item statement, rules, origin, trust, proposal; keep as finding/watch, move between registers, drop with reason, promote, edit with read-back, stage the drafted plan, chat, accept remaining, and commit. |
| `src/lib/components/WorkControlPanel.svelte` | **New.** Shared host that fetches actions, holds, settings, chat, and the archive check, applies acts, resolves holds, resolves settings conflicts, and opens the plan/epic/ended-work chat. |
| `src/lib/views/PlanView.svelte` | Mounts `WorkControlPanel`; ended plans offer the read-only chat and follow-up start. |
| `src/lib/views/EpicView.svelte` | Epic resource sections: workstreams and member plans with status and wait reasons, sources, landings, findings/watches with review entry points, per-plan and epic-own tokens with the `≥` floor and unmeasured count, and all events; mounts control actions and review chat. |
| `src/lib/views/OverviewView.svelte` | Per-plan and per-epic `Actions` entry points that mount the same control panel beside Running Work. |
| `src/lib/components/ChatPanel.svelte` | Optional accept/reject labels, so an ended-work follow-up names `Start a plan with it`. |
| `src/routes/+layout.svelte` | Route title with the `({count})` decision prefix, so another tab sees the open decisions. |
| `src/lib/api/client.ts` | Epic resource, archive check, review, chat, ended-work chat/follow-up, decision chat, and review read-back/stage methods and their types. |
| Tests | `DecisionView.test.ts`, `ControlActionsBar.test.ts`, `HoldResolutionModal.test.ts`, `PlanSettingsModal.test.ts`, `EpicView.test.ts`, and new `FindingsReviewModal.test.ts` and `WorkControlPanel.test.ts`. |

### 2.2 `pm-api` message catalogue

New keys were added in both locales for the one-step recommendation, the decision chat, notification
feedback, pause/end/archive/epic dialogues, review dispositions and read-back, hold reasons, and the epic
view sections (`control.*`, `decision.*`, `epic.views.*`, `start.chat.start_follow_up`).

### 2.3 `pm-web-e2e`

`tests/decisions.spec.js` and `tests/control.spec.js` were expanded to drive the views: counted title,
recommendation reveal, multi-part gating, answered-elsewhere notice, decision chat fallback, notification
banner, plan actions and pause dialogue, end confirmation, integrity-pause resolution, settings refusal,
epic workstreams/registers/tokens/events, automatic toggle, archive check, findings review, actions from
Running Work, and the ended-plan follow-up. `plan-view.spec.js` and `showroom.spec.js` were updated for
the epic resource sections.

## 3. Test & Verification Results

| Gate | Result |
|---|---|
| `cd pm-web-app && npm run check` | 0 errors, 0 warnings |
| `cd pm-web-app && npm test` | 44 files, **347 tests passed** |
| `cd pm-web-app && npm run lint` | clean |
| `cd pm-web-e2e && npm run lint` | clean |
| `npx playwright test tests/decisions.spec.js tests/control.spec.js` | **23 tests passed** |
| `npx playwright test tests/plan-view.spec.js` | 4 tests passed |
| `npx playwright test tests/showroom.spec.js` | 10 tests passed |
| `./mvnw verify -Ppre-commit` | **BUILD SUCCESS** |
| `./mvnw test -pl pm-api` | passed (catalogue parity) |

The repository-wide Playwright suite still contains suites outside this plan's surface (decomposition,
start, system, reports, lesson-source, designer, onboarding) whose fixtures/pairing codes predate the
consolidated scenarios; they were not touched here. The suites this plan owns pass.

## 4. Domain Invariants

- **Readable-label shield:** every rendered label is a `$t(...)` lookup; the catalogue guard confirms no
  fallback literals, no raw translation keys, and EN/DE parity for all new keys.
- **Module boundary:** no file under `pm-operator` or `pm-mock-runtime` was modified.
- **Responsive layout:** the action dialogues, settings, review, and chat panels collapse to one column
  and full-height panels at 768/480 px; no fixed width exceeds the viewport.
- **Untrusted text:** review items marked `untrusted` are shown as text with an explicit marker; no HTML
  rendering is used.
- **No money, no budget:** epic tokens use the existing `formatSplit`/`formatTokens` helpers and report
  the `≥` floor and the unmeasured task count only.

## 5. Build & Quality Evidence

`./mvnw verify -Ppre-commit` completed with `BUILD SUCCESS` after the
web-app build, lint, type check, unit tests, and the e2e module's lint. The working tree contains only
the intended source, catalogue, test, and documentation changes.
