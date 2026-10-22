# Completion Report: `client-profile-schema`

Plan: [`implemented/client-profile-schema-plan.md`](implemented/client-profile-schema-plan.md)
Schema landing: [PR #161](https://github.com/plan-marshall/pm-mcp-clients/pull/161) (`feature/client-profile-schema`, merged as `725f62a`)
Follow-up: this change on `fix/profiles-probe-and-mcp-slot`
Operational SOP: [`doc/agy/runbook.md`](runbook.md)

---

## 1. Executive Summary

PR #161 ships the complete client profile schema across all five shipped profile documents — `antigravity.json`, `claude.json`, `codex.json`, `neutral.json`, and `opencode.json` — and closes the work package that unblocks `pm-mcp-core PLAN-19` (profile loader).

The plan was executed in four phases: alignment on latest `main`, remediation of the CodeRabbit findings, alignment of the operator's harness discovery with the shipped antigravity profile, and full schema/build validation. All four phases are complete.

The schema itself landed as PR #161 (`725f62a`). Two contract gaps remained in that merge and are closed by this follow-up: the antigravity identity probe had been reverted, and the OpenCode profile declared `mcp_config.delivery: "slot"` without a `{{mcp_config}}` slot. The completion report and the plan archival are delivered here as well.

The plan was taken over mid-flight. A second, duplicate plan-marshall worker (agent identity `plan-marshall-oliver`) was executing the same plan on the same branch from a different environment and had pushed four commits between 08:16 and 09:46. The executing session reset onto that head (`54bbc73`), adopted its six launch-vector fixes, then completed the plan's own resolution: restored the antigravity version probe (reverted by the duplicate worker), realigned the discovery tests, and added the still-missing `{{mcp_config}}` slot for OpenCode. Section 6 records this precisely.

## 2. Delivered Artifacts

| File | Change |
|---|---|
| `pm-api/src/main/resources/profiles/claude.json` | Full schema: headless launch (`-p` prompt, `--model`, `--output-format stream-json`, `--mcp-config {{mcp_config}}`, `--strict-mcp-config`, `--agents`), classes, usage and event pointers, detection and identity. Shell-command target corrected to the RFC 6901 pointer `/input/command`. |
| `pm-api/src/main/resources/profiles/opencode.json` | Full schema: `run` launch with `--format json`, inline configuration in `OPENCODE_CONFIG_CONTENT` carrying both the rendered agent and the job-relay MCP configuration, usage pointers from the `step_finish` token object (`/part/tokens/...`), detection and identity. |
| `pm-api/src/main/resources/profiles/codex.json` | Full schema: `exec` launch, classes, output/usage, detection and identity. |
| `pm-api/src/main/resources/profiles/antigravity.json` | Full schema: `agy` launch, inline `agent_mode`, `.agents/mcp_config.json` file delivery, detection, and the `agy --version` identity probe with its version pattern. |
| `pm-api/src/main/resources/profiles/neutral.json` | Full schema for the neutral fallback profile (no host, no probe). |
| `pm-api/src/main/java/de/planmarshall/api/host/ProfileDocumentReader.java` | Reads the current schema: `schema_version` is accepted, `detection` keeps its closed members, and `identity` requires the declared probe/pattern pair (`null` declared where absent). The legacy `session_id_variable`, `model_source`, and `provisional` members are still read when present, defaulting to no session variable, `declared`, and `false`. |
| `pm-api/src/test/java/de/planmarshall/api/host/ProfileDocumentReaderTest.java` | Fixtures moved to the current closed schema; the legacy-identity tolerance and the required-probe refusal are covered. |
| `pm-api/src/test/java/de/planmarshall/api/host/HostDetectionTest.java` | The session-ID test now derives its client population from `HostDetection.clientIds()` minus `HostDetection.NEUTRAL`; the probe assertion follows the shipped antigravity probe. |
| `pm-operator/src/test/java/de/planmarshall/operator/HarnessDiscoveryTest.java` | The antigravity case now asserts the profile's probe is honoured (`agy`, parsed version), and the no-probe path is covered through the neutral profile. |
| `pm-operator/src/main/java/de/planmarshall/operator/HarnessDiscovery.java` | Documentation corrected: the no-probe fallback is stated generically instead of naming antigravity. |

## 3. Test & Verification Results

| Layer | Command | Result |
|---|---|---|
| `pm-api` unit tests | `./mvnw test -pl pm-api,pm-operator -am` | **649 tests, 0 failures, 0 errors, 0 skipped** |
| `pm-operator` unit and transcript tests | `./mvnw test -pl pm-api,pm-operator -am` | **884 tests, 0 failures, 0 errors, 0 skipped** (includes `HarnessDiscovery` 9 tests) |
| Profile documents against the upstream schema | `ajv-cli validate --spec=draft2020` with the Draft 2020-12 schema extracted from `pm-mcp-core` `origin/main` (`01-profile-schema.adoc`, `03-endpoint-harness.adoc`) | **all 5 documents valid** |
| JSON well-formedness | `python3 -m json.tool` over every shipped profile | **all 5 parse** |

The schema validation was run against `pm-mcp-core` `origin/main` rather than the sibling working checkout, which was 12 commits behind and still carried the pre-`672d746c` `turn` definition without `stop_reason`. Validating against the stale checkout produced false positives on `claude.json` and `opencode.json`; against the current schema both are valid.

## 4. Domain Invariants

| Invariant | Status |
|---|---|
| Java 25 build target | Preserved; no compiler-release change. |
| Host-detection data is read only from the shipped profile documents | Preserved: `HostDetection` still derives every row from `ProfileDocumentReader`, with no host named in code. |
| Closed profile schema (`additionalProperties: false`) | Preserved and now verified: all five documents validate against the Draft 2020-12 schema. |
| `version_probe`/`version_pattern` pair (`null` iff `null`) | Preserved and enforced by the reader; antigravity carries `agy --version` with its dotted-version pattern. |
| `mcp_config.delivery: slot` requires a `{{mcp_config}}` slot in `argv` or `env` | Enforced in every slot-delivery profile: `claude.json` (`--mcp-config {{mcp_config}}`), `opencode.json` (inline `OPENCODE_CONFIG_CONTENT`), `codex.json`. `antigravity.json` uses file delivery. |
| Module boundary discipline (`clients-depend-on-pm-api-only`) | Preserved; changes stay inside `pm-api` and `pm-operator`. |
| Bilingual catalogue strings | Not touched; no user-facing text was added or changed. |

## 5. Build & Quality Evidence

```text
./mvnw verify -Ppre-commit
...
[INFO] Reactor Summary for pm-mcp-clients 0.1.0-SNAPSHOT:
[INFO] pm-mcp-clients ..................................... SUCCESS
[INFO] pm-api ............................................. SUCCESS
[INFO] pm-exec ............................................ SUCCESS
[INFO] pm-mock-runtime .................................... SUCCESS
[INFO] pm-relay ........................................... SUCCESS
[INFO] pm-operator ........................................ SUCCESS
[INFO] pm-web-app ......................................... SUCCESS
[INFO] pm-web-e2e ......................................... SUCCESS
[INFO] BUILD SUCCESS
```

The OpenRewrite recipe set of the current parent POM reformats pre-existing sources repository-wide (import grouping and record-header continuation indentation). Those reformat-only changes were reverted rather than committed: the plan names this session the sole owner of the profile documents and their two test classes, and committing 300+ unrelated files would collide with the parallel plans. The gate itself passed with the tree reduced to this plan's files.

Final run: all eight reactor modules `SUCCESS`, `BUILD SUCCESS`, and `git status` reduced to this plan's files after reverting the repository-wide OpenRewrite reformat described above.

## 6. Findings Resolution & Concurrency Record

### CodeRabbit / Sonar findings

| # | Finding | Disposition |
|---|---|---|
| 1 | Malformed JSON pointer `"/input.command"` in `claude.json` | **Fixed**: `"/input/command"`. |
| 2 | Hardcoded client list in `HostDetectionTest` | **Fixed**: derived from `HostDetection.clientIds()` minus `NEUTRAL`. |
| 3 | Claude `-p` receives `{{prompt_file}}` (a path) | **Refuted**: the closed slot set has no other prompt slot, and all four harness profiles hand the composed prompt over through `{{prompt_file}}`; the renderer (pm-mcp-core `PLAN-19`, not yet implemented) owns the slot's value. Replied on the thread with the reasoning. |
| 4 | Claude launch declares no `{{mcp_config}}` slot | **Fixed**: `--mcp-config {{mcp_config}}`. |
| 5 | OpenCode inline agent not rendered | **Fixed**: `OPENCODE_CONFIG_CONTENT` carries the rendered agent. |
| 6 | OpenCode declares no `{{mcp_config}}` slot | **Fixed**: the inline configuration now carries both slots, `{"agent": {{agent}}, "mcp": {{mcp_config}}}`. |
| 7 | OpenCode launch omits `--format json` | **Fixed**: `--format json` with `output.format: jsonl`. |
| 8 | OpenCode usage pointers do not match `step_finish` | **Fixed**: `/part/tokens/{input,output,cache/read,cache/write,reasoning}`. |
| 9 | Antigravity probe assertion stale | **Fixed**: the probe is restored and the assertion follows it. |

### Concurrency record

A duplicate plan-marshall worker was executing the same plan on `feature/client-profile-schema` from another environment (commits authored and committed as `plan-marshall-oliver`, not this workstation's identity). Its commits:

- `1c374f8` — reverted `antigravity.json` `version_probe` to `null`;
- `0dd2059` — merged this worktree's Step-1 commit into its branch;
- `f376b75` — changed the probe assertion to `List.of()`;
- `54bbc73` — fixed findings 4, 5, 7, 8 and finding 2.

This session adopted `54bbc73` as its base and completed the plan. That head was then squash-merged as `725f62a` (PR #161) before the session's commit could be pushed, so the remaining contract fixes and the archival landed as this follow-up. The antigravity probe is restored because it is the profile's contract (`headless.executable: "agy"`, and `agy --version` reports `1.4.5` on the host); the no-probe fallback in `HarnessDiscovery` resolves the *client id* as an executable name, so declaring the probe is what makes discovery resolve the real binary. `HarnessDiscoveryTest` covers both paths.

## 7. Roadmap & Archival

- `doc/plans/README.adoc`: no active package covers this follow-up; `profile-documents` (the roadmap package this plan traces to) is already archived as `done` under `doc/plans/implemented/`. No roadmap status change was required.
- The plan is archived as `doc/agy/implemented/client-profile-schema-plan.md`; this report stays in `doc/agy/` for the orchestrator's Step 8 verification and later move to `doc/agy/verified/`.
