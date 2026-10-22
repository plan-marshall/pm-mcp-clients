# Implementation Plan: `client-profile-schema` (Ship Full Client Profile Schema & Resolve PR #161)

This plan coordinates the resolution of CI failures, CodeRabbit review findings, and the final merge of pull request [#161](https://github.com/plan-marshall/pm-mcp-clients/pull/161) (`feat(profiles): ship the full client profile schema`), shipping the complete client profile schema across the five shipped profiles.

It unblocks `pm-mcp-core PLAN-19 profile-loader` and adheres strictly to [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) and [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Roadmap & Core Dependency** | Prerequisite for `pm-mcp-core PLAN-19 profile-loader` (see [`doc/plans/epic-clients.adoc#parallel`](../plans/epic-clients.adoc#parallel) line 22) |
| **Profile Schema Spec** | [`pm-mcp-core/doc/specification/client-profiles/01-profile-schema.adoc`](../../../pm-mcp-core/doc/specification/client-profiles/01-profile-schema.adoc) |
| **Roadmap Package** | [`doc/plans/implemented/profile-documents.adoc`](../plans/implemented/profile-documents.adoc) |
| **Pull Request** | [PR #161](https://github.com/plan-marshall/pm-mcp-clients/pull/161) (`feature/client-profile-schema`) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

---

## 2. Scope & Objectives

1. **Rebase on Latest `main`**:
   - Rebase branch `feature/client-profile-schema` onto latest `main` (incorporating PR #158, PR #159, and PR #160).
2. **Resolve CI Test Failure in `pm-operator`**:
   - `HarnessDiscoveryTest.antigravityHasNoVersionProbe` failed in CI:
     - `antigravity.json` added `identity.version_probe: [ "agy", "--version" ]`.
     - `HarnessDiscoveryTest` asserted `discovery.version("antigravity")` returns `Optional.empty()` because previously antigravity had no probe.
     - Update `HarnessDiscoveryTest` to reflect the current schema where `antigravity` carries a version probe, or ensure discovery handles missing harness on PATH cleanly without throwing `HarnessNotFound`.
3. **Resolve CodeRabbit Actionable Findings**:
   - In `pm-api/src/main/resources/profiles/claude.json`: fix malformed JSON pointer in `tool_call.target` from `"/input.command"` to `"/input/command"`.
   - In `pm-api/src/test/java/de/planmarshall/api/host/HostDetectionTest.java`: replace the hardcoded list `List.of("claude", "opencode", "antigravity", "codex")` with dynamic derivation from `HostDetection.clientIds()`, excluding `HostDetection.NEUTRAL`.
4. **Validate Full Profile Schema**:
   - Verify all 5 profile files (`antigravity.json`, `claude.json`, `codex.json`, `neutral.json`, `opencode.json`) validate completely.
   - Run `./mvnw verify -Ppre-commit` and ensure all 29 GitHub Actions checks turn green on PR #161.
5. **Merge PR #161**:
   - Enqueue in merge queue and land on `main`.

---

## 3. Concurrency & Isolation

- **Branch**: `feature/client-profile-schema` (PR #161).
- **Sole Owner of**: `pm-api/src/main/resources/profiles/*.json`, `ProfileDocumentReader.java`, `ProfileDocumentReaderTest.java`, `HostDetectionTest.java`, and `HarnessDiscoveryTest.java`.
- **Parallelizable with**: `cli-fullscreen-reports-system` and `completion-own-calls-change-map`.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-client-profile-schema` on branch `feature/client-profile-schema` rebased on latest `main`.
- [ ] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-code-reviewer` for CodeRabbit finding remediation
- [ ] 3. Fixes implemented, local tests passing, all profiles valid.
- [ ] 4. Final completion report written (`doc/agy/client-profile-schema-plan-report.md`) and initial plan moved to `doc/agy/implemented/client-profile-schema-plan.md`.
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [ ] 6. Push to `feature/client-profile-schema`, all 29 CI checks green, CodeRabbit review verified resolved.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/client-profile-schema-plan-report.md` by orchestrator session.

---

## 5. Phased Technical Recipe

### Phase 1: Rebase & Alignment
- Rebase `feature/client-profile-schema` onto latest `main`.

### Phase 2: CodeRabbit Finding Remediation
- Fix `tool_call.target` in `claude.json` (`"/input/command"`).
- Make client ID testing in `HostDetectionTest.java` dynamic using `HostDetection.clientIds()`.

### Phase 3: HarnessDiscoveryTest Alignment
- Fix `HarnessDiscoveryTest.java` in `pm-operator` to correctly test version probe handling when antigravity defines a version probe.

### Phase 4: Full Validation & Gate
- Run `./mvnw -q test -pl pm-api,pm-operator`.
- Run `./mvnw verify -Ppre-commit`.
- Push to PR #161 and monitor CI checks to completion.
