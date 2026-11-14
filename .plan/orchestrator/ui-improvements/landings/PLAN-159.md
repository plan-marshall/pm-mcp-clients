# Landing Analysis: PLAN-159 — The Contract Carries What the Engine Records (Intent Delta)

epic: ui-improvements
workstream: WS-01
pr: #417 (https://github.com/plan-marshall/pm-mcp-clients/pull/417), squash `835e7024` (11 files, +798/-94), merge head `4185ff8e`

> Written by the `analyze` verb (2026-11-12) from `rb-159-contract-intent-delta-001.md`, verified against git, the PR reviews,
> the PR body and the engine checkout (`../pm-mcp-core` at `a2edd0f2`). Runbook lane, opencode `deepseek-v4.1-flash/high`;
> executor `0.1.2106`.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 ADR, evidence, `role` | shipped-as-specified | `PlanIntentResource.java:90-156` (one validator per rule: `requireEvidence`, `requireOperatorAdr`, `requireQuestion`), `:219` `AdrReference`, `:265` `AdrRelation`, `IntentDecider.java:31-32` `ROLE("role")`, `:532` `roleCalls()`; rules equal the engine's `IntentRecord.java:539-552` |
| D2 Non-goals `{id, text}` | shipped-as-specified | `:330` `NonGoal`, `NG-\d{1,4}` as the engine, unique ids `:458`, order kept; the epic's non-goals stay strings (as the engine) |
| D3 Builder `reason`, distribution without proposal | shipped-as-specified | `BuilderCheckResource.java:115-122` (reason exactly when `not_run`), `OverSizeDistributionResource` `@Nullable Double proposedMultiple`, omitted on write |
| D4 Mock and design | shipped (coverage clause not met, pre-existing) | both scenarios grown (`fully-onboarded.json:892-975, 1120-1155`, `running-work.json:1133-1200`); `ScenarioLoader` decodes every fixture through pm-api (mutation `model` → 431 failures); `-Pcoverage` branch 0.72 at base, 0.73 on main, red before the change |

Added-unplanned: `IntentJson.optionalRawText`/`optionalDouble`/`optionalObject`; `AdrRelation` in `everyMemberResolves`
(CodeRabbit); DC-005 with `question_id` Q-009 in the fixtures; duplicated fully-onboarded assertions in two test classes. No consumer
outside `pm-mock-runtime` reads the intent records (pm-operator, pm-relay, pm-web-app checked), so nothing else breaks.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`) — reason: 11 files of contract, fixtures and tests plus the engine's
  schema and codec. No write. It loaded only the base and orchestrator personas, not the listed Java skills, and did not read
  `three-layer-analysis.md` (it followed the brief's Produce list) — disclosed. Re-read by the orchestrator: `PlanIntentResource.java:450-460`,
  `BuilderCheckResource.java` (Javadoc `:32-59`, rules `:115-122`), pm-mcp-core `IntentRecord.java:93,557-565`, `PLAN-V2-123` D1
  (queue `staged`), the PR body (no review-count facts). Confirmed.
- **Verdict:** no blocker. Layer 1 — the spec's three HYPOTHESES settled (see epic.md Decisions D6 note): the engine allows zero
  non-goals on the light lane against the client's ≥1 (a real disagreement, operator: keep ≥1); the over-size route is one role
  (`MockRuntimeEngine.java:5459-5465`, PLAN-146's); `reason` and `proposed_multiple` rest on the design alone. Layer 2 — D1–D3 met
  (test-first as compile refusals; the blank-evidence and blank-reason refusals do fail on a revert of `optionalRawText`); D4 met
  except the pre-existing coverage gate. Layer 3 — **major (cross-repo):** the engine still writes the decider `model`
  (`IntentCodec.java:268`) and the client refuses it; owner `PLAN-V2-123` (staged), not named by the landing. **major (latent):**
  an empty non-goal list refuses the whole record (`PlanIntentResource.java:455`). **major (process):** the PR body carries no
  `unresolved_review_threads=`/`review_body_findings_*` and the outside-diff finding got a fix but no reply. Minor: the readers'
  explicit-null, wrong-type and non-object `adr` branches untested; tests assert exception types only; `not_run` accepts questions
  against its own Javadoc; the `reason` is untrusted text for the web; no `@NullMarked` in any pm-api package (pre-existing).
  Nits: duplicated assertions, redundant `containsKey`, public `FIELD_*` on `NonGoal`/`AdrReference`.
- Confirmed sound: the wire matches the engine's schema (decider set, `adr {adr_id, relation}`, `evidence`, `NG-n`,
  `contradicts` only by the operator); fail-closed parsing (`requireOnlyKnown`, unknown enums, string non-goal, duplicate id); no
  generic exception; jackson-core only; `@EnableGeneratorController` on `IntentContractTest`.

Routing (operator, 2026-11-12, AskUserQuestion): non-goals ≥1 kept, the disagreement → Open Defects; the pm-api minors (reader tests,
the `not_run` rule, the `decisions`/`choices` note, the `reason` as untrusted text) → **PLAN-145** deliverable 5 (surface +3). The
engine sequencing, the red `pm-api` coverage gate and the lane lessons → Open Defects.

## Metrics and Anomalies

- Tokens and plugin version `unknown` (opencode) — the standing Open Defect.
- Two operator questions in the run: an empty commit to re-trigger CI on a head with no run (`4185ff8e`); the merge.
- Shell reads and one `python3 -c` probe recorded by the lane itself.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `dacc204a` (2 inline + 1 outside-diff): inline-1 replied and withdrawn, inline-2 fixed in `2c2eb9d1`
  and replied, outside-diff fixed without a reply. Threads 0 unresolved. Sourcery approved `4185ff8e`. Merge rule met.
- CI green at `4185ff8e`; merged via the queue on the operator's answer in the lane (`835e7024`, 23:21Z).
- Sonar: `0` confirmed by the lane at `4185ff8e` (8 issues at `3b10b742` fixed in `dacc204a`). Not re-read (Sonar MCP down).

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 417; `landing` `landings/PLAN-159.md`
- [x] PLAN-145 deliverable 5 and the landing amendment (operator)
- [x] epic.md: Open Defects (non-goals, engine `model`, coverage gate, lessons); the D6 HYPOTHESES marked settled
- [x] message 001 archived; resume anchor; `queue-view.md` regenerated

## Follow-Ups

- PLAN-145 and PLAN-146 are unblocked by the contract; both still wait on the web foundation chain (PLAN-147 → 148).
- No daemon intent route before pm-mcp-core `PLAN-V2-123` lands.
