# Completion Report: `control-contract` (Controlling Work: Contract and Mock Runtime)

> Implementation plan: [`doc/agy/implemented/control-contract-plan.md`](implemented/control-contract-plan.md) (archived)
> Work package: [`doc/plans/implemented/control-contract.adoc`](../plans/implemented/control-contract.adoc)
> Runbook: [`doc/agy/runbook.md`](runbook.md)
> Branch: `feat/control-contract`

---

## 1. Executive Summary

This package delivers the client-side **contract for controlling work** in `pm-api`
(`de.planmarshall.api.control`) together with the mock-runtime scenarios that serve it in
`pm-mock-runtime`. It serves use cases `UC-CTL-1`..`UC-CTL-5`, `UC-STA-6`, and `UC-PLN-5`.

The work extends the control contract that the earlier `uc-ctl` package (PR #72) established —
plan and epic actions, holds, and a plan's settings — with the records the *Controlling work:
contract and mock runtime* specification additionally names:

* the **epic as a resource** (header, workstreams, plan rows with their status and waiting causes,
  sources, landings, registers, history) and its **tokens** (per plan, the epic's own tasks, and the
  total, with a floor and its coverage, `not_measured` where model calls happened and no task was
  measured — never a budget and never money);
* the **decomposition draft** as the control contract shows it (workstreams, plans with objective,
  deliverables, surface, dependencies, and order, the checks of a run, and the change marker of a
  later run);
* the **chat** with the planning role (transcript, routed role, scope, states, and the one read-back
  proposal, incl. the chat about ended work);
* the **findings and watches review** (items, dispositions, the drafted plan of a promotion, and the
  read-backs of an edit or a redraft);
* the **archive check** (its lines, the completion review that runs first, and the ledger checks).

All new records are Jackson-`core`-streaming codecs (`toMap`/`fromMap`/`toJson`/`fromJson`) with a
round-trip test each, and each is served by a mock-runtime scenario named `control-<case>`.

### Plan-name mapping

The plan's Phase 2 lists a few record file names that the earlier `uc-ctl` package had already
realised under the names the code now uses; the new work keeps those established names rather than
introducing duplicates:

| Plan name | Existing/implemented name |
|---|---|
| `HoldResource.java`, `HoldResolution.java` | `HoldRecord`, `HoldResolutionRequest`, `HoldResolutionResult` |
| `PlanControlResource.java` | `WorkActions` |
| `PlanSettingsResource.java` | `PlanSettingsRecord` |

## 2. Delivered Artifacts

### 2.1 `pm-api` — new control records (`de.planmarshall.api.control`)

**Closed catalogues (enumerations):**

* `PausePoint.java` — `now`, `task`, `phase`
* `PlanOutcome.java` — `merged`, `no_change`, `handed_back`, `returned`, `cancelled`, `abandoned`,
  `released`, `aborted`
* `EpicTerminalReason.java` — `completed`, `cancelled`, `released`
* `EpicWaitReason.java` — `none`, `dependency`, `lock`, `scope`, `overlap`, `stale`, `held`
* `ReviewDisposition.java` — `open`, `kept`, `watch`, `dropped`, `promoted`
* `ChatRole.java` — `replan`, `decompose`, `refine`
* `ChatScope.java` — `plan`, `epic`, `decomposition`, `register_item`, `register`, `archive`, `request`
* `ChatState.java` — `chat.open`, `chat.turn`, `chat.read-back`, `chat.apply`, `chat.end`

**Records:**

* Epic resource: `EpicResource.java`, `EpicWorkstream.java`, `WorkRow.java`, `EpicEvent.java`,
  `RegisterSummary.java`
* Tokens: `EpicTokens.java`, `TokenFigure.java`, `PlanTokenFigure.java`
* Decomposition: `DecompositionResource.java`, `DecompositionWorkstream.java`,
  `DecompositionPlan.java`, `DecompositionCheck.java`
* Chat: `ChatReadBackResource.java`, `ChatTurnResource.java`, `ChatProposalResource.java`
* Review: `ReviewResource.java`, `ReviewItemResource.java`, `DraftedPlan.java`,
  `ReviewReadBackResource.java`
* Archive: `ArchiveCheck.java`, `ArchiveCheckLine.java`
* Shared codec helper: `ControlJson.java`

**Modified:** `package-info.java` (scope broadened to the new records).

### 2.2 `pm-api` — tests (`de.planmarshall.api.control`)

* `ControlCataloguesTest.java` — verbatim tests of every closed catalogue (each value pinned to its
  exact wire string) and unknown-value rejection.
* `ControlResourcesTest.java` — round-trip tests of every new record (epic resource, tokens and
  their floor/coverage/`not_measured`, decomposition, chat read-back and its proposal, review with
  promotion and read-back, archive check), plus malformed-input rejection.

### 2.3 `pm-mock-runtime` — scenarios (`src/main/resources/scenarios/`)

* `control-pause.json` — pause/resume/cancel of a plan.
* `control-holds.json` — the hold kinds and the acts that resolve them.
* `control-settings.json` — a plan's settings and their revision guard (conflict).
* `control-epic.json` — the epic as a resource, its automatic mode, and its archive check.
* `control-decomposition.json` — the decomposition draft via the control contract.
* `control-chat.json` — a plan chat, an epic chat, a chat about ended work, turns, accept, reject.
* `control-review.json` — the findings and watches review.
* `control-tokens.json` — an epic's tokens (a floor) and a plan's tokens.

**Modified:** `control.json` (unchanged routes kept), `StandardScenarios.java` (eight new
constants and their entries in `ALL`).

### 2.4 `pm-mock-runtime` — engine (`runtime/MockRuntimeEngine.java`)

* Seeded control fixtures in `initDefaultControl()`: the epic resources, the chats, the reviews, and
  the archive checks.
* New routes inside `handleControlRoutes(...)`:
  * `GET /api/v1/epics/{epic_id}` → `EpicResource`
  * `GET /api/v1/epics/{epic_id}/tokens` and `GET /api/v1/plans/{plan_id}/tokens` → tokens
  * `GET /api/v1/epics/{epic_id}/archive-check` → `ArchiveCheck`
  * `GET /api/v1/epics/{epic_id}/decomposition` → `DecompositionResource`
  * `GET /api/v1/epics/{epic_id}/review?register=findings|watches` → `ReviewResource`
  * `GET /api/v1/plans/{plan_id}/chat` and `GET /api/v1/epics/{epic_id}/chat` → `ChatReadBackResource`
  * `POST /api/v1/plans/{plan_id}/chat/turns` (a dialogue turn)
  * `POST /api/v1/plans/{plan_id}/chat/proposal/accept|reject` (read-back resolution)

### 2.5 `pm-mock-runtime` — tests

* `ControlScenarioTest.java` — extended with tests that load each new scenario and assert the
  representation a client receives (epic resource rows and waits, archive check failing on open
  findings, tokens as a floor and `not_measured`, the decomposition draft, the chats and their
  read-back, and the review with every disposition).

### 2.6 Documentation

* `doc/plans/implemented/control-contract.adoc` — the work package, moved on completion.
* `doc/plans/README.adoc` — the package marked `done` and all its links repointed to `implemented/`.
* `doc/plans/cli-actions.adoc`, `doc/plans/web-actions.adoc` — dependency links repointed.
* `doc/agy/implemented/control-contract-plan.md` — the implementation plan, archived.
* `doc/agy/implemented/README.md` — the archive index entry added.

## 3. Test & Verification Results

* `pm-api`: `ControlRecordsTest` (existing), `ControlCataloguesTest`, `ControlResourcesTest` — all
  green. Every new record has a codec round-trip; every closed catalogue is pinned verbatim.
* `pm-mock-runtime`: `ControlScenarioTest` — all green. Each `control-<case>` scenario loads and the
  test asserts the representation a client receives; `StandardScenariosTest` — green with the newly
  registered scenario names.

## 4. Domain Invariants

* **Java 25** baseline; records, `var`, final collections; no Quarkus/CDI.
* **`jackson-core` streaming only** — no `jackson-databind`; every codec builds and reads a
  `Map<String, Object>` tree through `JsonTree`.
* **Closed catalogues** — hold kinds, pause points, outcomes, epic terminal reasons, wait reasons,
  dispositions, chat roles/scopes/states are enumerated and pinned by tests.
* **Tokens, not money** — no budget, limit, or money figure on an epic; `not_measured` and floor are
  modelled explicitly.
* **Boundary rules** — all new `pm-api` types live strictly in `de.planmarshall.api.control`; all new
  mock-runtime scenarios follow `control-*.json`; tests live in `ControlScenarioTest.java`.
* **Bilingual catalogue** — no new message keys are introduced by this package (the existing
  `control.*` keys already cover the actions, settings, holds, review, and archive texts); the new
  records carry message *keys* the clients resolve, never client-facing prose.

## 5. Build & Quality Evidence

```
./mvnw verify -Ppre-commit

pm-mcp-clients ..................................... SUCCESS
pm-api ............................................. SUCCESS
pm-exec ............................................ SUCCESS
pm-relay ........................................... SUCCESS
pm-mock-runtime .................................... SUCCESS
pm-operator ........................................ SUCCESS
pm-web-app ......................................... SUCCESS
pm-web-e2e ......................................... SUCCESS

BUILD SUCCESS
```

OpenRewrite recipes, license headers, Checkstyle, the enforcer rules
(`clients-depend-on-pm-api-only`, `jackson-core-only`, …), Javadoc, and the full unit suite all
pass; the working tree holds only the intended changes.
