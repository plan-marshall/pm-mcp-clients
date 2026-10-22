# Completion Report: UC-PLN-5 Follow an Epic (Epic View)

**Plan**: [`uc-pln-5-plan.md`](implemented/uc-pln-5-plan.md) (moved from `doc/agy/uc-pln-5-plan.md`)
**Branch**: `feat/uc-pln-5-follow-epic`
**Use case**: UC-PLN-5 (design [`doc/design/shared/epic-view.adoc`](../design/shared/epic-view.adoc))
**Runbook**: [`doc/agy/runbook.md`](runbook.md), Steps 2–6

---

## 1. Executive Summary

The epic view now fulfils the shared design end to end. The work package started from a
substantially implemented epic view in all five modules; this session audited it against
`epic-view.adoc#texts`, the epic-resource specification, and the two canonical mock scenarios, then
closed the gaps the audit found:

- **One contract gap** — a shipped row carried its pull request number but no address, so neither
  client could offer the design's link. `WorkRow` now carries an optional `pull_request_url`,
  populated in `running-work` and `fully-onboarded`.
- **Catalogue drift** — `overview.epic.event.landed_staged` was named against the design's
  `overview.epic.event.landed`; the web app borrowed the plan vocabulary for an empty epic source
  list. Both are corrected, and two missing keys (`epic.views.no_sources`,
  `epic.views.next_plan`) are added in English and German.
- **Web view gaps** — the head now states the localized epic state, the running/scope figure, the
  end of an ended epic, and the next startable plan; workstreams show their id, their per-status
  counts, and fold/unfold; landings are listed with their pull requests as external links; events
  are written at their wall time; a sources-empty state has its own text; the layout stays readable
  at 320 px.
- **A silent live-update defect** — the view subscribed to `epic.event.landed` and
  `epic.event.overlap`, names that exist in no event catalogue, while ignoring the real
  `epic.row_changed`, `epic.auto_changed`, `epic.register_changed`, and `epic.decomposition_changed`.
  A row change therefore never refreshed the view. The subscription now follows the closed
  catalogue.
- **Missing verification assets** — a dedicated catalogue test, an epic-scenario contract test
  (resource + epic-scoped SSE), and the required `pm-web-e2e/tests/epic-view.spec.js` suite were
  authored. The Playwright suite covers all seven cases the plan names, plus token formatting, the
  showroom scenario, and the 320 px layout.

No production stub, placeholder, or `TODO` was introduced.

---

## 2. Delivered Artifacts

### `pm-api`

| File | Change |
|---|---|
| `src/main/java/de/planmarshall/api/control/WorkRow.java` | Added optional `pullRequestUrl` to the record, Javadoc, `toMap()`, and `fromMap()` (backward compatible: absent means `null`). |
| `src/main/resources/messages/epic_view.en.properties` | Renamed `overview.epic.event.landed_staged` → `overview.epic.event.landed` (design name); added `epic.views.no_sources`, `epic.views.next_plan`. |
| `src/main/resources/messages/epic_view.de.properties` | The same three changes in German. |
| `src/test/java/de/planmarshall/api/control/ControlResourcesTest.java` | Updated the `WorkRow` fixtures for the new component; the shipped row asserts the URL round-trip. |
| `src/test/java/de/planmarshall/api/message/EpicViewCatalogueTest.java` | **New.** 47 required keys across the `overview` and `epic_view` areas, exact EN/DE key parity, ICU formatting for the head, the closed status/state lists, all wait reasons, the event lines, the registers, the landings, the token scope, and the PR label. |

### `pm-mock-runtime`

| File | Change |
|---|---|
| `src/main/resources/scenarios/running-work.json` | `pull_request_url` on the shipped row `relay-reconnect-backoff` and the handed-back `relay-docs`. |
| `src/main/resources/scenarios/fully-onboarded.json` | The same two rows in the showroom epic. |
| `src/test/java/de/planmarshall/mock/EpicViewScenarioTest.java` | **New.** Five tests: the whole running-work epic (header, two workstreams, four rows, the three wait kinds, sources, landings, findings/watches, token floor and coverage, newest-first events); a repeat-read + codec round-trip identity; the showroom's third workstream; the 404 for an unknown epic; and the epic-scoped SSE stream receiving its own `epic.row_changed` and not the other epic's. |

### `pm-operator` (verified, no production change required)

`EpicViewLevel`, `EpicViewFormatting`, and `OverviewFullScreenApp` already implement the design:
the `project › epic` path, the head with state/mode/running-of-scope, the workstream trees with
per-status counts and wait reasons, staged-plan details (objective, deliverables, surface,
dependencies), sources, landings with analysis, findings/watches review entry, the token split with
`--tokens`, and the newest-first events. Every runtime text passes through
`EpicViewFormatting.clean` → `OutputSafety.stripControlSequences`. The keys `p`, `a`, `Enter`,
`Esc`, `e` (expand all) and `←` (close row) are wired in `OverviewFullScreenApp`.

### `pm-web-app`

| File | Change |
|---|---|
| `src/lib/api/client.ts` | `EpicWorkRow.pull_request_url?: string \| null`. |
| `src/lib/views/EpicView.svelte` | Localized state (resource is authoritative over the process); `running of scope at once`; ended-epic end marker; next-plan marker; workstream id, per-status counts, and fold/unfold honouring the resource's `open`; status glyphs (`✓ ▶ ⏸ ! ✕ ○`); landings list with `<a target="_blank" rel="noopener noreferrer">` only for `http(s)` addresses; wall-time events; `no sources` text; closed-catalogue SSE subscription; responsive rules down to 320 px and no fixed 320 px grid track. |
| `src/lib/views/EpicView.test.ts` | 17 tests (was 10): added counts/fold, next plan, landing link, localized end, empty sources, wall-time events, closed-catalogue reload, an SSE-driven event-list update, and the two-word status normalization. |
| `src/lib/views/epicEvents.ts` + `epicEvents.test.ts` | **New.** The epic view's reload set as a named constant, with a test that reads `pm-api`'s `EventName.java` and refuses any name the closed catalogue does not hold. |

### `pm-web-e2e`

| File | Change |
|---|---|
| `tests/epic-view.spec.js` | **New.** Ten tests against `running-work` and `fully-onboarded`: open from Running Work; one-step project jump and return; fold/unfold plus `dependency` and `overlap` wait lines; plan row → plan view → back; landings with external PR link; findings and watches review; token floor/split/coverage; a live `epic.row_changed` broadcast growing the event list; the showroom's third workstream; and a 320 px no-horizontal-scroll check. |

### Documentation

| File | Change |
|---|---|
| `doc/agy/implemented/uc-pln-5-plan.md` | The executed plan, moved per Step 3. |
| `doc/agy/implemented/README.md` | Archive index row for this plan and its report. |
| `doc/agy/uc-pln-5-plan-report.md` | This report. |

---

## 3. Test & Verification Results

| Suite | Result |
|---|---|
| `pm-api` (all) | **644 tests, 0 failures** |
| `EpicViewCatalogueTest` (new) | 6 tests, 0 failures — 47-key presence, EN/DE parity, ICU formatting |
| `ControlResourcesTest` | pass, including the new `pull_request_url` round-trip |
| `pm-mock-runtime` (all) | **400 tests, 0 failures** |
| `EpicViewScenarioTest` (new) | 6 tests, 0 failures |
| `pm-operator` (all) | **850 tests, 0 failures** |
| `EpicViewLevelTest` / `EpicViewFullScreenTest` / `ControlEpicTranscriptTest` / `EpicCommandTest` | 9 / 10 / 1 / 12 tests, 0 failures |
| `pm-relay` (all) | 26 tests, 0 failures |
| `pm-exec` (all) | 87 tests, 0 failures, 3 pre-existing skips |
| `pm-web-app` Vitest | **403 tests across 49 files, 0 failures** |
| `EpicView.test.ts` | 17 tests, 0 failures |
| `epicEvents.test.ts` (new) | 2 tests, 0 failures — every subscribed event is a member of `EventName` |
| `pm-web-app` `npm run check` | 0 errors, 0 warnings |
| `pm-web-app` / `pm-web-e2e` `npm run lint` | clean |
| `pm-web-e2e/tests/epic-view.spec.js` | **10 passed** |
| `pm-web-e2e` full suite | 112 passed, 17 failed — **all 17 reproduce on the base commit** (see §6) |

### Baseline comparison

A detached worktree at `f5480b4` was built and run for the failing Playwright suites without any
change from this branch. The same 17 tests fail there (`start.spec.js` 4,
`decomposition.spec.js` 3, `git-sources.spec.js` 4, `lesson-source.spec.js` 5,
`onboarding.spec.js` 1), so they are pre-existing and unrelated to UC-PLN-5. Two of those files
even name pairing codes (`DCMP-2606`, `GTSR-CMTR`) that no scenario in the repository defines.

---

## 4. Domain Invariants

| Invariant | Status |
|---|---|
| Java 25 records, immutable, Jackson-free streaming via `JsonTree` | Kept — `WorkRow` is still an immutable record; the new component is an optional `String`. |
| Clients depend only on `pm-api` | Kept — the enforcer rule `clients-depend-on-pm-api-only` passed in the reactor gate. |
| No uncatalogued strings | Kept — every new web label is a catalogue key in both languages; the client tests read the real properties files. |
| Bilingual catalogue (EN & DE) | Kept and tightened — a new test asserts key-set identity and the design's required keys for the epic view. |
| Mock-runtime completeness | Extended — the epic resource route now carries PR addresses; the new scenario test decodes every answer with the `pm-api` codec. |
| Responsive layout down to 320 px | Verified — the 320 px Playwright test asserts `scrollWidth - clientWidth <= 1`. |
| Terminal output sanitization | Kept — `EpicViewLevel` passes every runtime string through `OutputSafety`; the transcript test still passes. |
| No stubs, placeholders, or `fallbackMessages` | Kept — the previous `href="#pr-N"` placeholder link for a pull request is replaced by a real external link or a plain label. |
| Closed event catalogue | Corrected — the web subscription no longer names two non-existent events and now listens to `epic.row_changed`, `epic.auto_changed`, `epic.register_changed`, `epic.decomposition_changed`, `scope.started`, `scope.ended`, `activity.changed`, `needs_operator.changed`, and `tokens.changed`. |

---

## 5. Build & Quality Evidence

```text
./mvnw verify -Ppre-commit
...
[INFO] Reactor Summary for pm-mcp-clients 0.1.0-SNAPSHOT:
[INFO] pm-mcp-clients ..................................... SUCCESS [ 18.519 s]
[INFO] pm-api ............................................. SUCCESS [03:27 min]
[INFO] pm-exec ............................................ SUCCESS [ 31.427 s]
[INFO] pm-mock-runtime .................................... SUCCESS [02:56 min]
[INFO] pm-relay ........................................... SUCCESS [ 45.650 s]
[INFO] pm-operator ........................................ SUCCESS [07:46 min]
[INFO] pm-web-app ......................................... SUCCESS [03:30 min]
[INFO] pm-web-e2e ......................................... SUCCESS [ 24.573 s]
[INFO] BUILD SUCCESS
[INFO] Total time:  19:41 min
```

The gate ran OpenRewrite, license headers, Checkstyle, the enforcer rules, Javadoc validation, and
the full unit suite. After it, the affected Java files were restored to their hand-written layout
and re-verified with `-Drewrite.skip=true`; see §6 for the OpenRewrite churn finding.

---

## 6. Known Issues Observed (Pre-existing, Not Introduced)

1. **OpenRewrite rewrites the whole tree on every build.** The parent POM (`pm-mcp-parent:0.1.1`)
   ships a recipe set that adds or removes the blank line between the `java.*` import group and the
   next one, and flattens some record component indentation. A clean `./mvnw verify` therefore
   leaves roughly 310 unrelated files modified. Those changes were reverted so this PR stays scoped;
   the two files this plan owns were kept in their hand-written layout and their tests re-run with
   `-Drewrite.skip=true`. This is a repository/tooling issue for the orchestrator, not a UC-PLN-5
   defect. (It also means `./mvnw verify -Ppre-commit` ends with a dirty tree on `main` today.)

2. **Seventeen stale Playwright suites.** `start.spec.js`, `decomposition.spec.js`,
   `git-sources.spec.js`, `lesson-source.spec.js`, and one `onboarding.spec.js` test fail on the
   base commit: they expect older routes/modals, and two use pairing codes that exist in no
   scenario. They are outside this plan's scope (Stream B/C remediation) and were left untouched.

3. **`epic.views.wait.lock` and `epic.views.wait.scope` word the wait differently.** The web keys
   read "waits for a free slot/place"; the design's `overview.epic.waits.*` texts are used by the
   terminal. The web view uses its own `epic.views.wait.*` family consistently for all six closed
   reasons; aligning them with the `overview` family is a wording decision for the design owner.

---

## 7. Follow-ups for the Orchestrator

- Run the Step 8 four-pillar verification on `main` and move this report to
  `doc/agy/verified/uc-pln-5-plan-report.md`.
- Decide whether the OpenRewrite recipe set should be pinned to the version the tree was last
  formatted with, or whether a repository-wide formatting commit is intended.
