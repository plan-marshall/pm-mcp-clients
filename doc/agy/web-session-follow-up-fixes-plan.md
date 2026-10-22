# Implementation Plan: `web-session-follow-up-fixes` (Close the Dev-Host Loopback Output Gap, Reconcile the Designer Route with a Dirty Draft, and Retire the Dead Pre-Pairing Status Probe)

This implementation plan resolves the post-landing findings recorded during the Step 8 verification of [`web-session-pairing-and-dev-host`](verified/web-session-pairing-and-dev-host-plan-report.md) (PR #173). The original plan landed the eight web-client findings; this follow-up closes two user-facing gaps and one dead code path that the post-landing audit found.

The pairing contract, the mock runtime's device-secret lifecycle, and the Vite network binding are **not** changed. The dev-host fix only makes the printed output honour the operator's explicit loopback choice; the designer fix only reconciles the URL with the project the operator actually keeps; the pairing-page cleanup removes a probe that cannot run on the only page that renders it.

It operationalizes [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) under the execution discipline of [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Findings Source** | Step 8 verification of [`web-session-pairing-and-dev-host-plan.md`](implemented/web-session-pairing-and-dev-host-plan.md); verified report [`web-session-pairing-and-dev-host-plan-report.md`](verified/web-session-pairing-and-dev-host-plan-report.md) |
| **Use Cases** | [`UC-ENT-1`](../../../plan-marshall-documentation/doc/use-cases/02-entry.adoc#UC-ENT-1) (Enter and Connect), [`UC-PLN-3`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-3) (Design a Plan's Process) |
| **Specifications** | [`03-web-server.adoc#two-listeners`](../../../plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc#two-listeners), [`#web-listener`](../../../plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc#web-listener) |
| **Local Client Design** | [`doc/design/web-app/README.adoc`](../design/web-app/README.adoc) |
| **Predecessor Plans** | [`implemented/web-session-pairing-and-dev-host-plan.md`](implemented/web-session-pairing-and-dev-host-plan.md), [`implemented/web-reports-system-and-designer-plan.md`](implemented/web-reports-system-and-designer-plan.md) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

### 1.1 Finding Analysis (post-landing finding -> root cause -> correction)

| # | Finding | Root cause in the landed code | Correction in this plan |
|---|---|---|---|
| **F1** | With `PM_WEB_HOST=127.0.0.1`, `pm-web-app/start.sh` still prints `Network (IP)` and a network `Paired:` URL, although the README and the predecessor plan promise only the local URL. | `print_connection_urls` prints `Network (IP)` whenever a primary IPv4 exists, independent of the resolved host; `print_paired_url_if_available` replaces a loopback host with that IPv4. | Render `Network`/`Network (IP)` only for a non-loopback host. Keep `Paired:` on the resolved host; never substitute the primary IPv4 for a loopback host. Extract the rendering into a sourceable helper and test the matrix. |
| **F2** | A route change from `/designer?project=A` to `/designer?project=B` with an unsaved draft can leave the URL on B while the designer keeps A. Choosing “Stay” resets only the select value, not the route; a later B route is then ignored. A local project selection also does not update the URL. | `ProcessDesigner` sets `appliedRouteProjectId = next` **before** the leave guard resolves; `stayInDesigner` restores `projectId` but not the route/applied value; `onProjectChange` loads the project without replacing the route. | Adopt a route project only when the guard commits; on “Stay”, replace the URL/store back to `loadedProjectId`; on a local selection, replace the designer query after the guard commits. Cover dirty stay/discard, `/designer` without a project, and local selection in Vitest. |
| **F3** | The pre-pairing `getConnectionStatus` probe and the certificate-fingerprint block in `PairingView` are unreachable in the running app. | `PairingView` renders only when `!$runtimeStore.isPaired`, but its `onMount` probes only when `apiClient.isPaired()`; the unit test renders the impossible paired-pairing state directly. | Remove the dead probe, `status` state, fingerprint markup/CSS, and the impossible-state test. The browser's TLS warning remains the pre-pairing fingerprint source, as the predecessor plan's risk note states. The LAN fingerprint in Settings stays. |
| **F4** | The dev-host output and the designer route had no regression coverage for the failing matrix. | `start.sh` has no sourceable test seam; `ProcessDesigner` tests cover only `null -> project`, not dirty `A -> B -> Stay` or local selection. | Add a Bash test for the pure URL renderer and the missing Vitest cases. Run both in Phase 4. |

> [!NOTE]
> The single-use pairing rule, the device-secret persistence, the authenticated-only `401` handling, and the real runtime's listener security are **not** modified. This plan only corrects how the development coordinates are printed and how the designer keeps its route and selection consistent.

---

## 2. Scope & Objectives

### Part A: Make the Dev-Host Output Honour the Loopback Choice

1. **Extract the URL rendering into a sourceable helper**:
   - Add `pm-web-app/scripts/connection-urls.sh` with the existing `primary_ipv4`, `reachable_host`, and `is_loopback_host` functions plus one pure `render_connection_urls <web-port> <pairing-code>`.
   - `pm-web-app/start.sh` sources the helper and prints through it in both the already-running and the start branches.
   - Keep `current_pairing_code` in `start.sh` (it needs `REPO_ROOT` and the mock logs) and pass the code into the renderer.
2. **Correct the loopback matrix**:
   - Always print `Local:`.
   - Print `Network:` and `Network (IP):` only when the resolved host is not loopback.
   - Print `Paired:` on the resolved host, never on a substituted primary IPv4; omit it when no code is available.
   - When `PM_WEB_HOST` is `127.0.0.1`, `localhost`, `::1`, or `[::1]`, the output contains only local coordinates.
3. **Keep the normal network output**:
   - A real host name still prints `Network: http://<host>:<port>`; an IP that differs from the host still prints `Network (IP):`; the `Paired:` URL uses the host name.
   - `PM_WEB_ALLOWED_HOSTS` and the Vite bind are untouched.
4. **Add the regression test**:
   - Add `pm-web-app/scripts/connection-urls.test.sh`, source the helper, stub `reachable_host`/`primary_ipv4`, and assert the loopback/name/IP/no-code matrix.
   - Add a `test:urls` script to `pm-web-app/package.json` and document the command.

### Part B: Reconcile the Designer Route with the Draft Guard

1. **Add one route-replacement helper**:
   - In `pm-web-app/src/lib/stores/runtime.ts`, add `replaceDesignerProject(projectId: string | null)`: update `designerProjectId` and write `/designer?project=<id>` with `syncUrl(path, true)` (replace, not push), so a selection or a refused route change does not add a history entry.
2. **Defer the route adoption until the guard commits**:
   - In `ProcessDesigner.svelte`, keep the route project separate from the local selection.
   - If the designer is clean, adopt the new route project immediately and load its units.
   - If the designer is dirty, set a pending route action; on “Discard”, adopt the new project; on “Stay”, keep `loadedProjectId` and replace the URL/store back to it.
   - Remove the `next === null` early return; a plain `/designer` means machine scope, not “keep the old project”.
3. **Keep a local selection in the URL**:
   - After `onProjectChange` commits (clean, or after the guard), call `replaceDesignerProject(loadedProjectId)` so reload, bookmark, and back see the selected project.
4. **Keep browse navigation coherent**:
   - The route-driven effect, the local selector, and `stayInDesigner` must all converge on the same `appliedRouteProjectId`; a later route to the same project must not be ignored after a refused earlier attempt.
5. **Tests**:
   - Dirty `A -> B -> Stay`: URL and select stay on A; a later B route asks again.
   - Dirty `A -> B -> Discard`: URL and select become B.
   - Dirty `A -> /designer -> Discard`: URL becomes `/designer` and the select is machine scope.
   - Clean local selection B: URL becomes `/designer?project=B`; a rerender/reload keeps B.
   - Clean route change still follows.

### Part C: Retire the Dead Pre-Pairing Probe

1. In `PairingView.svelte`, remove `ConnectionStatus` imports, the `status` state, the `onMount` probe, the fingerprint markup, and the fingerprint CSS.
2. Remove the `shows the network certificate fingerprint on the network listener` test from `PairingView.scenarios.test.ts`; keep the no-probe and error-mapping tests.
3. Leave the LAN fingerprint in `SettingsView` and its E2E coverage untouched; it is a paired-browser surface and still works.
4. Update the `PairingView` comments and the `pm-web-app/README.md` wording if either still implies an in-page pre-pairing fingerprint.

### Non-Goals (Explicitly Out of Scope)

- Changing the single-use pairing contract, the failed-redemption limit, the device-secret format, or the mock runtime's stable base.
- Changing `vite.config.ts`, the Vite bind, the allowed-host list, or the `/mock` loopback proxy.
- Adding an unauthenticated fingerprint endpoint; the TLS warning remains the pre-pairing source.
- Fixing the pre-existing onboarding `?step=` Playwright failure; that remains a separate remediation worktree/plan.
- Redesigning the designer beyond route/selection reconciliation.

---

## 3. Concurrency & Isolation

- **Branch**: `fix/web-session-follow-up-fixes` off latest `main`.
- **Worktree**: `/Users/oliver/git/worktrees/pm-mcp-clients-web-session-follow-up-fixes`.
- **Parallelizable with**: nothing currently active; the plan touches the web app shell and the dev launch script only.

### Boundary Rules

- **Sole Owner of**:
  - `pm-web-app/start.sh`
  - `pm-web-app/scripts/connection-urls.sh` (new)
  - `pm-web-app/scripts/connection-urls.test.sh` (new)
  - `pm-web-app/package.json`
  - `pm-web-app/src/lib/stores/runtime.ts` and `runtime.test.ts`
  - `pm-web-app/src/lib/views/ProcessDesigner.svelte` and `ProcessDesigner.test.ts`
  - `pm-web-app/src/lib/views/PairingView.svelte` and `PairingView.scenarios.test.ts`
  - `pm-web-app/README.md`
  - `doc/agy/web-session-follow-up-fixes-plan.md`, its completion report, and the `doc/agy` index/dashboard rows for this plan
- **No Touch**:
  - `pm-mock-runtime`, `pm-api` messages, `pm-operator`, `pm-relay`, `pm-exec`
  - `pm-web-app/vite.config.ts`, the pairing/session behavior, the message catalogues
  - `pm-web-e2e` (including the onboarding spec)
  - other web views and the existing Settings fingerprint surface

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [ ] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-web-session-follow-up-fixes` on branch `fix/web-session-follow-up-fixes` off latest `main`; Step 1 committed and pushed.
- [ ] 2. Domain skills & engineering personas loaded where applicable:
  - `plan-marshall-persona-implementer` & `pm-dev-frontend:javascript` for the start-script helper, store helper, and designer reconciliation.
  - `plan-marshall-persona-module-tester` & `pm-dev-frontend:jest-testing` for the Bash/Vitest regression cases.
  - `plan-marshall-persona-documenter` for the README, report, and link-rebase correction.
  - `plan-marshall-persona-code-reviewer` for the adversarial review and review-bot triage.
- [ ] 3. Incremental implementation with tests following the phased technical recipe.
- [ ] 4. Final completion report written (`doc/agy/web-session-follow-up-fixes-plan-report.md`), initial plan moved to `doc/agy/implemented/web-session-follow-up-fixes-plan.md`, **all relative links rebased for the new location**, and the dashboard/implemented index updated. (Do NOT move the report to `verified/` — Step 8 is reserved for the orchestrator.)
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`; required because this plan changes TypeScript, shell scripts, and `package.json`).
- [ ] 6. PR opened (NEVER use `skip-bot-review` on a worker PR), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/web-session-follow-up-fixes-plan-report.md` by the orchestrator session.

> [!IMPORTANT]
> **Plan archival link rule**: this plan is written at `doc/agy/`, so its links use `../meta`, `../design`, `../findings-web.txt`, `../../../plan-marshall-documentation`, and sibling `implemented/<name>.md` paths. When Step 4 moves it to `doc/agy/implemented/`, rebase every relative link and re-run the internal-link check. The predecessor plan's 27 broken links after its move are the regression this rule prevents.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Dev-host output** | `plan-marshall-persona-implementer` | `pm-dev-frontend:javascript`<br>`plan-marshall-persona-module-tester` | Sourceable URL renderer, loopback matrix, Bash 3.2-safe test, README accuracy. |
| **Phase 2: Designer reconciliation** | `plan-marshall-persona-implementer` | `pm-dev-frontend:javascript`<br>`plan-marshall-persona-module-tester` | Deferred route adoption, stay/discard, local selection, URL/store consistency, tests. |
| **Phase 3: Dead pairing UI** | `plan-marshall-persona-implementer` | `pm-dev-frontend:javascript`<br>`plan-marshall-persona-code-reviewer` | Remove unreachable probe/fingerprint; preserve Settings fingerprint and error mapping. |
| **Phase 4: Verification** | `plan-marshall-persona-module-tester` | `pm-dev-frontend:jest-testing`<br>`plan-marshall-persona-integration-tester` | Bash test, Vitest, `npm run check`/`lint`/`format:check`, full pre-commit. |
| **Phase 5: Report & archive** | `plan-marshall-persona-documenter` | `plan-marshall-persona-code-reviewer` | Report evidence, link rebase, index/dashboard, review triage. |

---

## 6. Phased Technical Recipe

### Phase 1: Dev-Host Output (F1)

- [ ] **1.1 Extract the sourceable URL helper**:
  - Create `pm-web-app/scripts/connection-urls.sh` with the standard license header and:
    - `primary_ipv4()`: keep the macOS `en0`/`en1` and Linux `hostname -I` resolution.
    - `reachable_host()`: keep `PM_WEB_HOST` -> macOS `LocalHostName.local` -> `hostname -f` -> `hostname` -> IPv4 -> `localhost`.
    - `is_loopback_host()`: keep the loopback set.
    - `render_connection_urls <web_port> <pairing_code>`: print `Local:` always; print `Network:`/`Network (IP):` only for a non-loopback host; print `Paired:` from the resolved host and the supplied code, never from a substituted IP.
  - In `pm-web-app/start.sh`, source `"$SCRIPT_DIR/scripts/connection-urls.sh"` after `SCRIPT_DIR` is set, delete the moved functions, and replace both `print_connection_urls` call sites with `render_connection_urls "$port" "$(current_pairing_code)"`.
  - Keep `current_pairing_code` in `start.sh`; it owns `REPO_ROOT`, `/mock/state`, `POST /mock/pairing`, and the log fallback.
- [ ] **1.2 Add the Bash regression test**:
  - Create executable `pm-web-app/scripts/connection-urls.test.sh` with `set -euo pipefail`.
  - Source the helper and override `reachable_host`/`primary_ipv4` per case through a small `run_case` helper.
  - Cases:
    1. `PM_WEB_HOST`-equivalent host `127.0.0.1` with IPv4 `192.168.1.20`: no `Network`, no `Network (IP)`, local `Paired`.
    2. `localhost` with an IPv4: same local-only result.
    3. `Mac-Mini.local` with IPv4 `192.168.1.20`: `Network` on the name, `Network (IP)` on the IP, `Paired` on the name.
    4. Host equal to the IPv4: one `Network`, no duplicate `Network (IP)`.
    5. No pairing code: no `Paired`.
    6. Empty IPv4: no `Network (IP)`.
  - Add `"test:urls": "bash scripts/connection-urls.test.sh"` to `pm-web-app/package.json`.
- [ ] **1.3 Update the documentation**:
  - Keep `pm-web-app/README.md`'s statement accurate: with `PM_WEB_HOST=127.0.0.1`, only local coordinates are printed.
  - Add a short note to the README's phone-reaching section that `Paired:` uses the resolved host and never swaps in the IP.

### Phase 2: Designer Route Reconciliation (F2)

- [ ] **2.1 Add `replaceDesignerProject`**:
  - In `runtime.ts`, add and export:
    ```ts
    export function replaceDesignerProject(projectId: string | null): void {
      runtimeStore.update((s) => ({ ...s, designerProjectId: projectId }));
      const path = pathForRoute('designer', { projectId: projectId ?? undefined });
      if (path) syncUrl(path, true);
    }
    ```
  - Guard against non-designer state only if the call would surprise another route; the designer is the only caller.
- [ ] **2.2 Rework the route effect and guard**:
  - Replace `appliedRouteProjectId` assignment-before-guard with an explicit adoption function:
    - first render/onMount seeds `appliedRouteProjectId = routeProjectId` and `projectId = routeProjectId`;
    - if the next route equals the applied value, do nothing;
    - if clean, set applied + project and `loadAll()`;
    - if dirty, store the pending route project and a stay callback; on discard adopt the new project; on stay use `replaceDesignerProject(loadedProjectId)` and set `appliedRouteProjectId = loadedProjectId`.
  - Remove the `next === null` early return so `/designer` is a real machine-scope route.
  - Keep `beforeunload` and the existing leave modal; do not introduce a second modal.
- [ ] **2.3 Keep local selection in the URL**:
  - In `onProjectChange`, after the guard commits and `loadAll()` has set `loadedProjectId`, call `replaceDesignerProject(loadedProjectId)`.
  - Use replace semantics (`syncUrl(..., true)`), so changing the selector does not add a history entry.
- [ ] **2.4 Add Vitest cases** in `ProcessDesigner.test.ts`:
  - dirty route A -> B -> Stay: URL remains `/designer?project=A`, select A, `appliedRouteProjectId` A; a subsequent B route opens the guard again.
  - dirty route A -> B -> Discard: URL and select B.
  - dirty route A -> `/designer` -> Discard: URL `/designer`, select machine scope.
  - clean local selection B: URL `/designer?project=B`; rerender/reload keeps B.
  - clean route change still follows.
  - Use the existing fixture project ids; remove the draft from `localStorage` where the test needs a clean state.

### Phase 3: Dead Pre-Pairing Probe (F3)

- [ ] **3.1 Remove the dead code**:
  - In `PairingView.svelte`, remove the `ConnectionStatus`/`onMount` imports, `status`, the `onMount` probe, the `fingerprint` markup, and the fingerprint CSS.
  - Keep `pairingErrorText`, single-use/offline/revoked copy, form, QR, and command copy.
- [ ] **3.2 Update tests**:
  - Remove the fingerprint test that arranges `isPaired: true` on the pairing page.
  - Keep the no-probe test, the invalid-code test, the offline test, and the revoked-reason test.
  - Do not touch `SettingsView`'s LAN fingerprint or its E2E test.
- [ ] **3.3 Documentation**:
  - State in the report that the TLS warning is the pre-pairing fingerprint source; the in-page block was unreachable.
  - Remove any claim that the pairing page itself shows the fingerprint before pairing.

### Phase 4: Verification & Archival

- [ ] **4.1 Targeted tests**:
  - `bash pm-web-app/scripts/connection-urls.test.sh`
  - `cd pm-web-app && npm test`
  - `cd pm-web-app && npm run check && npm run lint && npm run format:check`
- [ ] **4.2 Full pre-commit gate**:
  - `./mvnw verify -Ppre-commit` with `BUILD SUCCESS` and a clean `git status`.
  - Do not revert unrelated OpenRewrite reformatting; commit what the gate writes, per `doc/agy/runbook.md` Step 4.
- [ ] **4.3 Manual evidence**:
  - `PM_WEB_HOST=127.0.0.1 pm-web-app/start.sh` (or the already-running branch) prints only `Local:` and a loopback `Paired:`; no `Network` lines.
  - Without the override, prints `Local`, `Network` on the machine name, `Network (IP)` when the IP differs, and `Paired` on the name.
  - Open a plan's `Design process`, edit to make the draft dirty, navigate to another plan's designer, choose Stay: the URL stays on the first plan and the select matches. Repeat with Discard: URL/select move to the second plan.
  - Change the designer selector: the URL query updates; reload keeps the selection.
- [ ] **4.4 Report and archive**:
  - Write `doc/agy/web-session-follow-up-fixes-plan-report.md` with the findings, artifacts, test evidence, manual checks, and the link-rebase result.
  - Move the plan to `doc/agy/implemented/web-session-follow-up-fixes-plan.md` and **rebase all relative links**; run an internal-link check.
  - Update `doc/agy/implemented/README.md` and the dashboard.
  - Mark Steps 1-7 in the plan as complete as they are executed; leave Step 8 for the orchestrator.

---

## 7. Acceptance Criteria

| Finding | Acceptance criterion | Evidence |
|---|---|---|
| **F1** | With `PM_WEB_HOST=127.0.0.1` (or `localhost`, `::1`, `[::1]`), the start script prints no `Network`/`Network (IP)` line and the `Paired:` URL is on the loopback host. | `connection-urls.test.sh` loopback cases + manual start-script output. |
| **F1** | With a real host name and an IPv4, the script prints `Local`, `Network` on the name, `Network (IP)` when the IP differs, and `Paired` on the name; a host equal to the IP does not duplicate the IP line. | `connection-urls.test.sh` name/IP cases + manual output. |
| **F2** | Dirty route A -> B -> Stay leaves the URL and select on A; a later B route asks again. | `ProcessDesigner.test.ts` dirty-stay case. |
| **F2** | Dirty route A -> B -> Discard moves URL and select to B; dirty A -> `/designer` -> Discard moves to machine scope. | `ProcessDesigner.test.ts` dirty-discard cases. |
| **F2** | A local selection updates `/designer?project=...`; rerender/reload keeps it. | `ProcessDesigner.test.ts` local-selection case + manual reload. |
| **F3** | `PairingView` has no authenticated pre-pairing status call, no unreachable `status` state, and no fingerprint block; the no-probe test and error mapping still pass. | `grep` + `PairingView.scenarios.test.ts` + `npm test`. |
| **F4** | The new Bash test and Vitest cases pass; `npm run check`, `npm run lint`, `npm run format:check`, and `./mvnw verify -Ppre-commit` succeed. | Phase 4 command output; completion report. |
| **F5** | No pairing-contract, mock-runtime, Vite-bind, message-catalogue, or onboarding-E2E change is included. | Diff review and module boundary check. |

---

## 8. Risk & Rollback Notes

- **Start-script refactor**: Moving the URL functions into `scripts/connection-urls.sh` must not change the `set -euo pipefail` behavior, the Bash 3.2 empty-array idiom, or the `PM_WEB_HOST` resolution. The helper's test sources it directly; `start.sh` keeps the launch flow unchanged apart from the call.
- **Loopback output**: The rule is deliberately conservative: when the resolved host is loopback, no network URL is printed, even if a primary IPv4 exists. This is the operator's explicit no-exposure choice. If a phone needs the app, set a real host name (the normal path).
- **Designer route**: Use `replaceState` (not `pushState`) for the stay/local-selection reconciliation, so the back button does not loop through refused route changes. The guard must run once per route change; do not add a second modal.
- **Dead fingerprint removal**: The Settings view still shows the LAN fingerprint after pairing; only the unreachable pre-pairing block is removed. The TLS warning remains the pre-pairing source; adding an unauthenticated fingerprint endpoint remains a separate specification change.
- **Link rebase**: The predecessor plan's 27 broken links after its move are the known failure mode. The Step 4 checklist makes the rebase and internal-link check part of the deliverable, not a post-landing fix.
- **Rollback**: Revert the single follow-up commit; no data migration, no scenario change, and no persisted state format change is involved.
