# Completion Report: `mock-runtime-fully-onboarded-scenario`

## 1. Executive Summary

The `fully-onboarded` showroom scenario is delivered as a sixth canonical `pm-mock-runtime` scenario. One file, `fully-onboarded.json`, starts a machine that is fully onboarded and whose operator domains are all populated at once: three enrolled projects, running work with plans and an epic, three pending decisions, five unread reports (plus one read), the system and cleanup views, machine settings, a start entry with every request source and a refinement chat, the process designer, and the SSE stream.

The runtime now seeds reports, enrolled projects, and being-added projects from a scenario's `facts.runtime` fixture (all optional, so the existing scenarios are unchanged) and recognizes `fully-onboarded` in the system and decision domain guards. Two `pm-api` codecs now guard the new fixture data at scenario load, so malformed showroom data fails by naming the fixture path rather than half-switching the runtime.

The showroom is verified twice:

- **`FullyOnboardedScenarioTest`** boots the scenario on the Unix socket and asserts that every domain route answers HTTP 200 and decodes with the `pm-api` codec of its contract, including the event streams.
- **`pm-web-e2e/tests/showroom.spec.js`** walks the web client through the populated header badges, projects and file tree, overview, six-phase plan view, epic view, decisions, reports and completion report, system and cleanup, settings and paired devices, and the process designer. The terminal client is covered by the runtime contract matrix, not by Playwright.

## 2. Delivered Artifacts

### pm-mock-runtime

| File | Change |
|---|---|
| `src/main/resources/scenarios/fully-onboarded.json` | New showroom fixture (one `showroom.active` state, 20 routes, the shared control/decision/decomposition fixtures, two paired devices, an open web listener on 7420, five unread and one read report, three enrolled projects, a being-added project, the designer fixtures, and a full start entry). |
| `src/main/java/de/planmarshall/mock/StandardScenarios.java` | New `FULLY_ONBOARDED` constant, included in `ALL`; the primary count rises from five to six. |
| `src/main/resources/scenarios/primary.txt` | `fully-onboarded` added to the launcher index. |
| `src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` | `declaresSystem()` and `declaresDecisions()` recognize `fully-onboarded`; `initDefaultReports()` and `initDefaultProjects()` seed from optional `facts.runtime.reports`, `facts.runtime.projects`, and `facts.runtime.being_added` fixtures; `validateRuntimeFixture()` validates the new fixtures plus devices before a scenario switch. |
| `src/main/java/de/planmarshall/mock/scenario/ScenarioRegistry.java` | Documents that `fully-onboarded` resolves directly by name and needs no legacy alias. |
| `src/test/java/de/planmarshall/mock/FullyOnboardedScenarioTest.java` | New 10-test suite: loading, onboarded status, a 42-endpoint matrix with codec decoding, reports, decisions, projects and devices, file tree, designer, start entry/recipes/intake, epic workstreams/tokens, and SSE. |
| `src/test/java/de/planmarshall/mock/StandardScenariosTest.java` | Asserts the six canonical scenarios and the new constant. |
| `start.sh` | Launcher comment updated; the menu itself is driven by `primary.txt`. |
| `README.adoc` | Documents the showroom and the canonical launcher list. |

### pm-web-e2e

| File | Change |
|---|---|
| `tests/showroom.spec.js` | New 10-test Playwright walkthrough of the single showroom scenario. |

### Gate-stabilizing fixes (pre-existing failures exposed by the gate)

| File | Change |
|---|---|
| `pm-operator/src/test/java/de/planmarshall/operator/planview/PlanCompletionLevelTest.java` | The "minimal report" fixture used a fixed `ended_at` and asserted a fixed `2 d ago`; after 20 October it reads `3 d ago` and failed the whole reactor. The fixture now names an instant two days back and asserts the same `ReportItem.formatTime` result. |
| `pm-web-app/package.json` | `npm run check` now runs `svelte-kit sync` first. SvelteKit 3 writes `$app/tsconfig` into `node_modules`; the pre-commit profile's second `npm ci` removes it after the build, so a clean checkout failed typecheck. |

## 3. Test & Verification Results

### pm-mock-runtime (JUnit 5)

```
pm-mock-runtime tests 384 failures 0 errors 0 skipped 0
FullyOnboardedScenarioTest: 10 tests, 0 failures
```

The showroom matrix covers:

- `status`, `status/connection`, `web/devices`, `machine/settings`
- `projects` (overview and explorer), project detail, ended work, confined file tree
- `overview`, `plans`, `epics`, plan and epic process trees, plan task activity
- plan intent/outline, epic intent, over-size distribution
- decisions list and detail; reports list, single report, completion report
- system snapshot, cleanup job, release notes
- recipes, intake draft, start entry, unclaimed lessons
- start decomposition and control decomposition, plan actions/holds/settings/tokens, epic resource/tokens/archive check/review/chat
- designer units and catalogue
- runtime-wide and plan-scoped SSE streams

### pm-web-e2e (Playwright, Chromium)

```
Fully onboarded showroom: 10 passed
```

The full `pm-web-e2e` suite was also executed. The showroom suite is green; the remaining legacy specs exercise the other consolidated scenarios and report pre-existing fixture expectations that are outside this plan (for example, they expect fixture strings that `running-work` no longer contains, and the Cleanup view reads a `projects` member while the runtime sends `project_items`). No legacy spec failure is caused by the new scenario, by the optional fixture seeding, or by the domain guards, because those paths apply only when the new fixtures are present.

### Pre-commit gate

```
./mvnw verify -Ppre-commit
BUILD SUCCESS
[INFO] pm-api ............ SUCCESS
[INFO] pm-exec ........... SUCCESS
[INFO] pm-mock-runtime ... SUCCESS
[INFO] pm-relay .......... SUCCESS
[INFO] pm-operator ....... SUCCESS
[INFO] pm-web-app ........ SUCCESS
[INFO] pm-web-e2e ........ SUCCESS
```

OpenRewrite, license headers, checkstyle, enforcer rules, Javadoc, ESLint, `svelte-check`, and the unit suites completed. The gate reformatted unrelated `pm-api`/`pm-operator` sources as a side effect of the repository-wide recipe run; those files were reverted so the change stays scoped to the showroom.

## 4. Domain Invariants

| Invariant | Status |
|---|---|
| Java 25 toolchain, no uncatalogued strings | Preserved; no new message keys. |
| Bilingual catalogue (EN & DE) | Untouched; the showroom uses existing message keys. |
| Closed scenario schema | New fixture loads through `ScenarioLoader`; both new `facts.runtime` shapes are validated with their codecs. |
| Mock runtime completeness | Every domain route used by `pm-web-app` and `pm-operator` answers 200 in the showroom. |
| Output safety | Reuses sanitized activity data and the existing `OutputSafety` rendering path. |
| Existing scenarios | Unchanged: the new fixture seeding is opt-in and the delivered defaults still apply when no fixture is present. |

## 5. Scope Notes

- `pm-api`, `pm-operator` production code, and `pm-web-app` production code were not changed.
- The two gate-stabilizing fixes above are test/build-configuration changes, not client behavior changes.
- The orchestrator retains Step 8: move this report to `doc/agy/verified/` after the PR lands and post-landing verification passes.
