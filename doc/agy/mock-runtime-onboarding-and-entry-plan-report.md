# Completion Report: `mock-runtime-onboarding-and-entry`

**Plan**: [`mock-runtime-onboarding-and-entry-plan.md`](implemented/mock-runtime-onboarding-and-entry-plan.md)
**Branch**: `feat/mock-runtime-onboarding-and-entry`
**Worktree**: `/Users/oliver/git/worktrees/pm-mcp-clients-mock-onboarding-entry`
**Runbook**: [`runbook.md`](runbook.md)
**Date**: 2026-10-20

---

## 1. Executive Summary

Scenario 1 of the five-scenario mock-runtime architecture is consolidated. The 23 fragmented
`onboarding-*`, `entry-*`, `paired-devices`, `web-pairing`, `operator-interaction`, and `status-only`
scenario files are gone; every one of their cases now lives as a state in a single
`pm-mock-runtime/src/main/resources/scenarios/onboarding.json` with **80 states**.

Every state carries a complete `OperatorStateResource` decoded by the `pm-api` codecs, every state
link is an `OperatorLink` the representation advertises, and every scheduled event is a complete
`EventEnvelope`. The loader's strict operator validation (previously bypassed for the legacy flat
representations) now runs over the whole file, so the scenario cannot describe a state, link, or
event the contract does not allow.

The legacy scenario constants and `POST /mock/scenario` names keep working through the
`ScenarioRegistry` alias table, now mapped to the specific consolidated state of each case. The
onboarding endpoint keeps the flat member names the terminal and web clients read while preserving
the operator envelope under `operator`.

---

## 2. Delivered Artifacts

### 2.1 Scenario data (`pm-mock-runtime`)

| Artifact | Change |
|---|---|
| `src/main/resources/scenarios/onboarding.json` | Rewritten: 80 `OperatorStateResource` states, one status route, alias-target states for every legacy case. |
| `src/main/resources/scenarios/onboarding-*.json` (9 files) | Deleted. |
| `src/main/resources/scenarios/entry*.json` (11 files) | Deleted. |
| `src/main/resources/scenarios/paired-devices.json` | Deleted. |
| `src/main/resources/scenarios/web-pairing.json` | Deleted. |
| `src/main/resources/scenarios/operator-interaction.json` | Deleted. |
| `src/main/resources/scenarios/status-only.json` | Deleted. |

The consolidated state families are:

- **Guided onboarding**: `onboard.mode`, `auto.consent`, `auto.scan`, `auto.result`,
  `language.choose`, `harness.consent`, `harness.choose`, `harness.not-logged-in`,
  `harness.tokens-complete`, `harness.tokens-partial`, `harness.tokens-not-reported`,
  `harness.probe-consent`, `harness.probe`, `harness.blocked`, `harness.all-failed`,
  `harness.probe-failed`, `harness.none`, `harness.confirm`, `harness.enrol`, `harness.unknown`,
  `harness.add`, `harness.add-consent`, `harness.verify-profile`, `harness.add-confirm`,
  `harness.install-profile`.
- **Models, roles, store, providers, defaults**: `catalogue.*` (6), `roles.*` (3), `store.*` (5),
  `providers.*` (7), `github.consent`, `github.check`, `sonar.consent`, `sonar.key`,
  `defaults.*` (3), `onboard.summary`, `onboard.completed`.
- **Machine settings**: `settings.choose`, `settings.conflict`, `settings.withdraw`,
  `settings.documents-mode`, `settings.plan-defaults`, `settings.cleanup-durations`,
  `settings.record`.
- **Entry, pairing, and devices**: `entry.ready`, `entry.paired`, `entry.lan-open`,
  `entry.unonboarded`, `entry.onboarding`, `entry.web-app`, `entry.paired-browser`,
  `entry.ssh-session`, `entry.fresh-code`, `entry.lan-conflict`, `entry.lan-credentials`,
  `entry.device-lifecycle`, `entry.stream-close`.
- **Web pairing, shared interaction, status**: `web.pair`, `web.paired`, `wait.choose`,
  `wait.check`, `wait.moved`, `wait.cancelled`, `status.only`.

### 2.2 Registry and engine (`pm-mock-runtime`)

| Artifact | Change |
|---|---|
| `scenario/ScenarioRegistry.java` | Every legacy alias now targets the specific consolidated state (`status-only → onboarding#status.only`, `entry → onboarding#entry.ready`, `paired-devices/entry-device-lifecycle → onboarding#entry.device-lifecycle`, `operator-interaction → onboarding#wait.choose`, …). A `.json` suffix on an alias name resolves too. |
| `runtime/MockRuntimeEngine.java` | `mergeRepresentation` flattens the operator state's `facts` and keeps the full envelope under `operator`; `applyStateRuntimeToStatus` derives the status, onboarding progress, `onboarded`, and listener fixture from the current state; devices and the LAN listener seed from the start state's fixture; the LAN-conflict and `loopback_only` credential refusals are state-conditioned. |

The engine reads state-scoped runtime fixtures from
`representation.facts.runtime` (`status`, `version`, `machine`, `onboarded`, `onboard`, `web`,
`devices`, `lan_conflict`, `loopback_only`). This is how one scenario serves the per-case runtime
status of `entry-unonboarded`, `entry-onboarding`, `entry-lan-conflict`, and the device lists of
`entry-device-lifecycle` and `entry-stream-close` without a separate scenario file.

**Codec mapping note**: the plan names `summary`, `detail`, `data`, and `label_key`. The closed
`pm-api` codec defines none of those members. The consolidated states therefore carry:

- the summary and all domain values under `facts` (`facts.summary`, `facts.harnesses`,
  `facts.results`, `facts.checks`, …);
- the scope and state under `scope` / `state` (`state_id`, `kind`, `level`);
- the acts under `links` as `OperatorLink` objects (`id`, `rel`, `method`, `href`, `preselected`,
  `confirm`);
- `detail`-style text would use the codec's `texts`/`title`; neither is required by a state and
  neither is invented here.

### 2.3 Tests (`pm-mock-runtime`)

| Artifact | Change |
|---|---|
| `OnboardingScenarioTest.java` | Added `shouldDecodeEveryStateAsAnOperatorState` (all 80 states decode, ids match, advertised links equal the scenario links); refusal chain updated to the consolidated flow; settings-conflict and all-failed expectations updated. |
| `EntryScenarioTest.java` | Every entry case now resolves to the consolidated scenario; pairing, LAN conflict, credential refusal, and device lifecycle assertions retained. |
| `PairedDevicesScenarioTest.java` | Resolves the consolidated `entry.device-lifecycle` state; canonical `paired_from: lan`. |
| `OperatorInteractionScenarioTest.java` | Loads and validates the consolidated file; `wait.check` representation. |
| `ScenarioLoaderTest.java`, `ScenarioRegistryTest.java`, `MockRuntimeTest.java`, `MockRuntimeEngineTest.java`, `MockRuntimeExtensionTest.java`, `MockRuntimeCliTest.java` | Alias-era expectations updated (resolved scenario name `onboarding`, `status.only`, `entry.ready`, `KXQB-MRTN` pairing code, `act_not_available` on a stale state link, `check.result` event). |

---

## 3. Test & Verification Results

| Gate | Result |
|---|---|
| `pm-mock-runtime` unit suite | **342 tests, 0 failures, 0 errors** |
| `OnboardingScenarioTest` | 19 tests (guided run, machine settings, state view, all case scenarios, all-state operator decode) |
| `EntryScenarioTest` | 27 tests (all nine entry cases, LAN open/conflict, credentials, pairing, device revocation, stream close) |
| `PairedDevicesScenarioTest` | 7 tests |
| `OperatorInteractionScenarioTest` | 3 tests (operator links, `check.result` envelopes, stale-link refusal) |
| `ScenarioLoaderTest` / `ScenarioRegistryTest` | 17 / 13 tests |
| `pm-operator` transcript suites | `console-status`, `console-settings`, `status`, `web-pair` transcripts match |
| `pm-web-app` Vitest | Passed in the reactor pre-commit run |
| `pm-web-e2e` (build/install stage in pre-commit) | Passed |
| `./mvnw verify -Ppre-commit` | **BUILD SUCCESS** across all eight reactor modules (pm-mock-runtime, pm-api, pm-exec, pm-relay, pm-operator, pm-web-app, pm-web-e2e) |

One unrelated TUI timing test (`TuiScreenDriverTest.filterHandoffRedraws`) flaked once during an
intermediate run and passed in isolation and in the final full gate.

> Note on footprint: the `pre-commit` OpenRewrite profile applies `OrderImports` with an
> import-layout that also inserts blank lines between import groups in files this plan does not
> touch. The gate was run to `BUILD SUCCESS`; the resulting repo-wide import-layout rewrite of
> `pm-api` and `pm-operator` is **not** part of this change, to preserve the plan's module
> boundaries and avoid colliding with the parallel waves. The working tree is clean for the plan's
> own footprint.

---

## 4. Domain Invariants

| Invariant | Status |
|---|---|
| Java 25 / no new source level | Unchanged; module compiles under the reactor's Java 25 release. |
| Closed scenario schema | Every state representation, link, event, and `next` is validated by `ScenarioLoader` at load; no bypass remains for this scenario. |
| `OperatorStateResource` closure | All 80 states decode via `OperatorStateResource.fromMap`; link ids are unique per state; hrefs are absolute `/api/v1/` paths; methods and relations are catalogue members. |
| No uncatalogued event names | Scheduled events use `check.result` only, with complete `EventEnvelope` data (`event`, `at`, `scope`). |
| No uncatalogued strings in Java | No new message keys or literals beyond the fixture field names. |
| One scenario file for the flow | `scenarios/onboarding.json` is the only onboarding/entry scenario; the legacy names resolve through aliases. |
| No `pm-operator` / `pm-web-app` / `pm-api` behavior change | None: the flat onboarding response shape, status shape, and all client code paths are unchanged. |
| No `skip-bot-review` label | Not applied; the PR is a worker implementation PR. |

---

## 5. Build & Quality Evidence

```
[INFO] Reactor Summary for pm-mcp-clients 0.1.0-SNAPSHOT:
[INFO] pm-mcp-clients ..................................... SUCCESS [ 11.529 s]
[INFO] pm-api ............................................. SUCCESS [01:47 min]
[INFO] pm-exec ............................................ SUCCESS [ 15.618 s]
[INFO] pm-mock-runtime .................................... SUCCESS [01:08 min]
[INFO] pm-relay ........................................... SUCCESS [ 16.574 s]
[INFO] pm-operator ........................................ SUCCESS [03:30 min]
[INFO] pm-web-app ......................................... SUCCESS [01:22 min]
[INFO] pm-web-e2e ......................................... SUCCESS [ 27.432 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  09:00 min
```

`pm-mock-runtime` suite:

```
[INFO] Tests run: 342, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 6. Follow-ups (not in this plan)

- The remaining four scenarios migrate onto `OperatorStateResource` in their own waves; the
  `ScenarioLoader` bypass for states without `state.state_id` is removed by
  `mock-runtime-reports-system-designer`.
- `StandardScenarios` still lists the legacy constants by design; they all resolve through the
  alias table. The catalog can be trimmed once every wave has landed.
