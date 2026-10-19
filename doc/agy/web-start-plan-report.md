# Completion Report: `web-start` (Starting Work in the Web App)

Plan: [`doc/agy/implemented/web-start-plan.md`](implemented/web-start-plan.md)
Work package: [`doc/plans/implemented/web-start.adoc`](../plans/implemented/web-start.adoc)
Branch: `feat/web-start` · Module: `pm-web-app`

---

## 1. Executive Summary

The web app now renders the complete start workflow of plans, epics, and recipes from the
operator-interaction state representations served by `pm-mock-runtime`:

- the three addressable entry routes (`/start`, `/projects/{id}/start`, `/entries/{id}`);
- the sources step with dynamic source management, confined file picking, copy-or-move, issue/PR
  intake with forge detection and the grant dialogue, flagged-span rendering, rate-limit display,
  unscreened acceptance, lessons, local changes, and the source-kind combination rules enforced by
  the runtime's offers;
- the ready step for plans, epics, and recipes, including the read-only derived recipe plan id and
  the epic's `epic.auto_start` in force with its origin;
- epic adoption and its failure screen;
- the reusable chat panel with transcript, message input, proposal read-back, accept/reject, and
  clean termination before create or abandon;
- the decomposition review with structured rows, automated checks, instruction input, read-back,
  and agree/commit;
- responsive desktop/phone layouts and light/dark styling through the existing design tokens.

Every user-facing string comes from the `start.*` catalogues in
`pm-api/src/main/resources/messages/start.{en,de}.properties`; a catalogue guard now also scans
every new screen for literal copy.

## 2. Delivered Artifacts

### `pm-web-app` — API surface

| File | Change |
|---|---|
| `src/lib/api/client.ts` | Added the entry/start records (`StartEntryResource`, `StartStateResource`, `StartReadyResource`, `StartChatResource`, `StartSource`, `SourceKindOffer`, `AdoptableEpic`, `AdoptionFailure`, `ConfinedFile*`, `FollowUpProposal`), the entry verbs (`openEntry`, `getEntry`, `actEntry`), the chat verbs (`chatTurn`, `endEntryChat`, `answerChatProposal`), the confined listing (`listProjectFiles`), the follow-up verbs, and `subscribeEntryEvents`; refactored the SSE reader into one `subscribeStream`. |

### `pm-web-app` — Routing and state

| File | Change |
|---|---|
| `src/lib/routes.ts` | `'start'` route; `matchPath` for `/start`, `/projects/{id}/start`, `/entries/{id}`; `pathForRoute` takes the start context. |
| `src/lib/stores/runtime.ts` | `startProjectId`, `startEntryId`, `startKind`; `navigateTo` and `adoptRouteFromLocation` carry the start context. |
| `src/lib/stores/start.ts` | `openStartWork` / `openStartEntry` open the dedicated route instead of a modal. |
| `src/lib/views/AppRouter.svelte` | Dispatches the `start` route to `StartView`, keyed by project/entry/kind. |
| `src/routes/start/+page.svelte` | `/start` page. |
| `src/routes/projects/[id]/start/+page.svelte` | `/projects/{id}/start` page. |
| `src/routes/entries/[id]/+page.svelte` | `/entries/{id}` page. |
| `src/routes/+layout.svelte` | Removes the superseded start modal mount; the address bar keeps the start path. |
| `src/lib/views/OverviewView.svelte` | Renders *Continue starting* for every open entry a project detail reports. |

### `pm-web-app` — Components and views

| File | Responsibility |
|---|---|
| `src/lib/views/StartView.svelte` | The start flow: project choice, gate refusal, sources, ready, recipe choose/parameters/create, epic adoption, started, and decomposition hand-off. |
| `src/lib/components/SourceList.svelte` | Source lines with reactive state marks, trust marks, failure causes, flagged spans, PR changed files, accept/retry/remove, the runtime's kind offers, Continue and Chat. |
| `src/lib/components/ConfinedFilePicker.svelte` | Browses `/api/v1/projects/{project_id}/files` over the checkout and (loopback only) the home directory; multi-file and directory picking. |
| `src/lib/components/GrantModal.svelte` | The read-issue grant dialogue (`this time`, `for this project`, `No`). |
| `src/lib/components/ChatPanel.svelte` | The one reusable chat panel: transcript, untrusted turns, message input, proposal card with accept/reject, ending. |
| `src/lib/components/MarkdownPreview.svelte` | A plain-text Markdown preview that renders no HTML. |
| `src/lib/views/RecipeReadyView.svelte` | Recipe parameter form and the ready step (sources, title, id, derived read-only id, epic auto-start, run/start). |
| `src/lib/views/EpicAdoptView.svelte` | Adoptable epics, and the failure screen with file and retry/back/abandon. |
| `src/lib/views/DecompositionReviewView.svelte` | Workstreams, plan rows, automated checks, instruction, read-back, draft-again, agree/commit. |
| `src/lib/start/sourceLabels.ts` | Forge/repository parsing, failure message selection, rate-limit reset text, offer reasons. |

### `pm-web-app` — Tests

| File | Coverage |
|---|---|
| `src/lib/views/StartView.scenarios.test.ts` | One test (21 in total) per `start-contract` scenario: sources, documents, issue failures, rate limits, unscreened, local changes and `landing.files`, pull request, lesson, epic, epic adoption and failure, refusal, recipe, chat, follow-up, and entry continuation. |
| `src/lib/views/DecompositionReviewView.test.ts` | Checks, failing/agreeable cut, instruction and read-back, drop. |
| `src/lib/start/sourceLabels.test.ts` | Forge parsing, bare numbers, unknown forges, failure messages, rate-limit credential variants. |
| `src/lib/routes.test.ts` | The three start paths and their project/entry parameters. |
| `src/lib/i18n/catalogue.test.ts` | The new screens are part of the zero-literal guard; the start keys are required in both catalogues. |

## 3. Test and Verification Results

| Gate | Result |
|---|---|
| `npm run lint` (`eslint .`) | No errors, no warnings. |
| `npm test` (`vitest run`) | **41 files, 237 tests, all passed**. |
| `npm run build` (`vite build` + static adapter) | Succeeded; the three new routes build. |
| `./mvnw verify -Ppre-commit` | **BUILD SUCCESS** across `pm-api`, `pm-exec`, `pm-relay`, `pm-mock-runtime`, `pm-operator`, `pm-web-app`, and `pm-web-e2e` (7:06 min). |
| `git status` after the gate | Only the intended source and documentation changes; no generated artifacts. |

The view tests mock `apiClient` with representations shaped exactly as `pm-mock-runtime` serves
them (`StartStateResource.toMap`); the mock-runtime scenarios remain the source of truth for the
records, states, and offers.

## 4. Domain Invariants

| Invariant | Status |
|---|---|
| No user-facing literal in the new screens | Verified by the extended catalogue guard in `catalogue.test.ts`. |
| Bilingual catalogue (`start.en` / `start.de`) | The guard asserts identical key sets; all keys used by the new views exist in both. |
| Untrusted content never rendered as markup | Flagged spans render as text with a plain-text tooltip; the Markdown preview emits text nodes only; no `{@html}`. |
| Rate limits are never auto-retried | The line shows the reset and only the operator's *Try again* acts. |
| PR changed files stay untrusted | Rendered with the count and the untrusted mark unless the operator authored them. |
| Local changes withheld under `landing.files` | The offer is disabled with the reason naming the project's setting. |
| Copy is preselected; the mode is asked only while `sources.documents_mode` is `ask` | The mode dialogue appears only when the runtime leaves a document source `checking`; Copy is checked. |
| No parallelization scope is asked for epics | The ready step shows only `epic.auto_start` with its origin. |
| Recipe plan id is derived and read-only | `id_editable=false` renders the field read-only; create omits the id. |
| Chat is closed before create/abandon | `create()` and `abandon()` end an open chat before acting. |
| Responsive layout | Sources/chat collapse to one column at ≤1024px; the picker and panels become full-height at ≤480px; controls keep 44px targets. |
| Light and dark themes | Every new surface uses the `tokens.css` variables; no literal colour outside fallbacks. |

## 5. Notes and Boundaries

- The start flow is now the dedicated route, not the former `StartWorkModal`. The component and its
  unit test remain in the tree for callers that instantiate a dialogue directly, but the app mounts
  `StartView`; the layout no longer mounts the modal.
- Resumption is addressed by `/entries/{id}`: an entry opened by either client is rendered from its
  current state. The overview renders *Continue starting* for every open entry a project detail
  reports (`open_entries`); the mock runtime does not yet carry that optional summary, so the
  action appears as soon as the runtime reports it.
- The decomposition review reuses `control-contract`'s decomposition endpoints. It offers Keep,
  Edit, Add, instructions, read-back, draft-again (`rewrite`), and agree/commit; a persisted *Drop*
  is not offered because the contract's `InstructAction` has no drop verb. The shared start chat is
  the component the later `web-actions`/`web-plan-view` work can mount unchanged.
- The package's Done When names both clients. The web view tests are complete here; the terminal
  half of the same scenarios is the sibling Wave 10 `cli-start` package, still open, and the
  package index now records it as a dependency rather than claiming terminal parity from this PR.
- No file in `pm-api`, `pm-operator`, or `pm-mock-runtime` was modified; the package consumed the
  contract read-only, as its boundary requires.
