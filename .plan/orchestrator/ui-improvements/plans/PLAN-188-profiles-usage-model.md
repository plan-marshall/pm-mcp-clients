# PLAN-188: The Shipped Harness Profiles Declare `usage.model`

epic: ui-improvements
workstream: WS-01

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on 2026-11-24 from the brief
> `inbox/pm-mcp-core-usage-model-profiles-001.md` (sender: the orchestrator of the pm-mcp-core epic `pm-mcp-core-m2-m4-v2`, on
> the operator's word "draft the brief for the clients repo"). It is a **hard cross-repo predecessor**: pm-mcp-core PR #594
> (PLAN-V2-135, head `79b6a5a8`) stays unmerged until this plan is merged here and its `SNAPSHOT` is deployed.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief. **It adds no dependency.**

## Objective

pm-mcp-core makes `headless.usage.model` a required member of a harness profile: the pointer into the harness's turn output
that names the model which answered, or `null` when the harness reports none. Every profile this repository ships in `pm-api`
lacks the member, so pm-mcp-core's validator refuses all five and five of its test classes are red for that one cause. This
plan adds the member to the five shipped profiles, with the values the engine's specification names, and pins them with a test
of this repository that fails on a profile without it.

## Deliverables

Read first: the brief `inbox/pm-mcp-core-usage-model-profiles-001.md` is the ledger's record; its facts are restated here, and
the lane does not need the ledger. The validator's rules (pm-mcp-core `doc/specification/harness-profiles/01-profile-schema.adoc`
on PR #594's branch `feature/v2-135-served-model-recorded`, the schema block near `:324-328`, the bullet `[[served-model]]` near
`:862`, the shipped-declarations sentence near `:877`; not on pm-mcp-core `main` yet): the member is **required**; its type is
`["string","null"]`; `additionalProperties: false` holds for the `usage` object; a string is an RFC 6901 pointer that starts with
`/`, is at most 256 characters and uses only the `~0` and `~1` escapes; `null` means "this harness reports no model"; the word
`not_applicable` (which the seven numeric figures use) is **refused** for `model`. If the lane can read the pm-mcp-core
checkout (`../pm-mcp-core`, `git show origin/feature/v2-135-served-model-recorded:doc/specification/harness-profiles/01-profile-schema.adoc`)
it re-reads these rules there first; a rule that differs from this list is reported and the specification's text wins.

1. **`usage.model` in the five shipped profiles, pinned by a test**
 - What: in `pm-api/src/main/resources/profiles/`, inside each profile's existing `headless.usage` object, beside the seven
   figures (`input_tokens`, `output_tokens`, `cache_read`, `cache_creation`, `reasoning`, `cost_usd`, `tool_uses`), one new member
   `"model"` with exactly these values:

   | Profile | `headless.usage.model` |
   |---|---|
   | `claude.json` | `"/message/model"` |
   | `antigravity.json` | `null` |
   | `codex.json` | `null` |
   | `neutral.json` | `null` |
   | `opencode.json` | `null` |

   No other member of any profile changes, and no figure's value changes. A test in `pm-api` reads each shipped profile
   document (the shipped catalogue the reader uses: `META-INF/pm-api/profiles.index` and `profiles/*.json`, as
   `ProfileDocumentReaderTest.ShippedDocuments` already reads them) and asserts, per profile, that `headless.usage` carries the
   member `model` with the value of the table (a JSON `null` for the four, the literal pointer for `claude`), and that a string
   value starts with `/` and is at most 256 characters. The test reads JSON with the streaming API of `jackson-core` only (the
   dependency rule of `pm-api`: never `jackson-databind`); whether the production `ProfileDocumentReader` should read
   `usage.model` itself is the lane's decision at outline — no client of this repository reads the `usage` block today, so the
   default is a test-only reader and no production change; a production change is made only if the lane names its consumer.
   The table's values are the expected values of a contract and stay literal in the test (not generated).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - The new test fails at `81e2c1fc` (no profile carries the member) and passes at the head: for each of the five shipped
     profiles it asserts the member's presence and the value of the table; the test fails when the member is removed from any
     one profile, and when `claude.json`'s value is changed to `null`. Evidence order: test-first.
   - `git diff origin/main --stat` names only the five profile files, the test file(s) and, if the lane made one, the production
     reader with its named consumer; no profile member other than `headless.usage.model` changes (diff read). Evidence order:
     `none`.
   - `./mvnw -pl pm-api -am verify` and the full `./mvnw verify` pass. Evidence order: `none`.
   - The landing message states that the `SNAPSHOT` deploy ran on the merge to `main` (the CI deploy job of the merge commit,
     its run id and conclusion, read through `ci`), or `unknown` when it cannot be read — never inferred from the merge alone.
     Evidence order: `none`.

## Standing Constraints

- **No new dependency or plugin.** `pm-api` uses the streaming API of `jackson-core` only, never `jackson-databind` (enforced by
  the build). Java 25, JUnit 5 only (no Mockito, PowerMock or Hamcrest), `@DisplayName`, AAA; input test data from `Generators`
  with `@EnableGeneratorController` where a test takes input data; the table's values are contract expectations and stay
  literal. Every public type documented. Pre-1.0: no shim.
- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api -am verify`, then the full `./mvnw verify` and
  `./mvnw verify -Ppre-commit` at the head (review every rewrite; it is known not to be idempotent and may touch files outside
  this plan: revert those). No e2e run is needed (`pm-web-app` and `pm-web-e2e` are not touched).
- The values are the specification's own. The four `null` values were not verified against those harnesses' output by the
  sender; this plan does not investigate them and does not replace a `null` by a pointer (pm-mcp-core's watch item
  PM-WATCH-CLIENT-20 keeps that open). A doubt about a value is reported in the landing message, not acted on.
- Where the project analyses its pull requests with Sonar (`sonar: configured`): an open Sonar finding is fixed in a pushed
  commit, or dismissed only on the operator's selection, recorded as `operator-selected: {issue-key}: {reason}` and transmitted
  through `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`;
  that transmission is known to answer HTTP 400 (PLAN-181): when it does, the lane names the key, rule and reason in the PR body
  and the landing message and does not count it as fixed. ZERO Sonar findings on the new code of the pull request; state
  `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`).
- The merge rule: the run merges only when every review thread has a reply and is resolved and every review body is answered,
  and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and review-body items
  from `ci pr comments`, never from memory. The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git
  Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator
  footer. If CodeRabbit refuses on quota, stop and ask the operator.
- Other plans run at the same time: never stop or reuse a process this worktree did not start. The main checkout is never
  edited; work only in the session's own worktree and branch. The branch of the parked PLAN-42 is never touched.
- Nothing is written to the pm-mcp-core repository and nothing is posted to its PR: the sender's orchestrator reads this
  repository's landing through its own channel.

## Claim Labels

- OBSERVED: the five shipped profiles `claude`, `antigravity`, `codex`, `neutral`, `opencode` each carry `headless.usage` with
  the seven members `input_tokens`, `output_tokens`, `cache_read`, `cache_creation`, `reasoning`, `cost_usd`, `tool_uses` and no
  `model` — read at `origin/main` `81e2c1fc` by parsing each `pm-api/src/main/resources/profiles/{id}.json`
  - verdict: corroborated | checked_at: 81e2c1fc59389c0e484b66b25abb5d6aaa287558 | by: ui-improvements/analyze | rescoped: n/a | evidence: each profile parsed at 81e2c1fc: headless.usage has the seven figures, no model
- OBSERVED: `claude.json`'s `usage` block sits at `:55-63`, with `"output": { "format": "stream_json", "result_pointer": "/result" }` at `:54` — read at `81e2c1fc`
  - verdict: corroborated | checked_at: 81e2c1fc59389c0e484b66b25abb5d6aaa287558 | by: ui-improvements/analyze | rescoped: n/a | evidence: claude.json :55-63 usage block, :54 output result_pointer /result, read at 81e2c1fc
- OBSERVED: no production code of this repository reads the `usage` block: `ProfileDocumentReader` parses `detection` and `identity` only (`:226-256`), and `git grep tool_uses` outside the profile files finds nothing — read at `81e2c1fc`
  - verdict: corroborated | checked_at: 81e2c1fc59389c0e484b66b25abb5d6aaa287558 | by: ui-improvements/analyze | rescoped: n/a | evidence: ProfileDocumentReader parses detection and identity only (parseProfile :226, parseDetection :256); git grep tool_uses outside profiles empty at 81e2c1fc
- OBSERVED: `ProfileDocumentReaderTest.ShippedDocuments` reads the shipped documents through `ProfileDocumentReader.readShippedProfiles()` and the profiles directory (`:143-170`) — read at `81e2c1fc`
  - verdict: corroborated | checked_at: 81e2c1fc59389c0e484b66b25abb5d6aaa287558 | by: ui-improvements/analyze | rescoped: n/a | evidence: ProfileDocumentReaderTest.ShippedDocuments calls readShippedProfiles() at :147,:159 and lists the profiles directory, read at 81e2c1fc
- OBSERVED: the `model_differs` display (the brief's optional D2) is already shipped: the CLI's `PlanCommand.printActivity` names both models (`PlanCommandTest.modelDiffersNamesBothModels`, PLAN-163, `landings/PLAN-163.md`) and the web view shows both (`PlanView.svelte`, PLAN-164, `landings/PLAN-164.md`); D2 is therefore not part of this plan
  - verdict: corroborated | checked_at: 81e2c1fc59389c0e484b66b25abb5d6aaa287558 | by: ui-improvements/analyze | rescoped: n/a | evidence: PlanCommandTest.modelDiffersNamesBothModels :245; PlanView.svelte :507,:1316 render served_model, read at 81e2c1fc
- HYPOTHESIS: pm-mcp-core's validator rules for `usage.model` are as listed under Deliverables — confirm/refute at pm-mcp-core `origin/feature/v2-135-served-model-recorded:doc/specification/harness-profiles/01-profile-schema.adoc` § `served-model` and the `usage` schema block (verify-at-outline; not on pm-mcp-core `main` at the staging read)
  - verdict: unverifiable | checked_at: 81e2c1fc59389c0e484b66b25abb5d6aaa287558 | by: ui-improvements/analyze | rescoped: n/a | evidence: the rules live on pm-mcp-core PR #594's branch, not on its main; the lane re-reads them at outline
- HYPOTHESIS: `/message/model` is where the `claude` harness's stream-json turn line names the answering model — the specification's own value; not verified against a recorded harness output by the sender or here (report a doubt, do not act on it)
  - verdict: unverifiable | checked_at: 81e2c1fc59389c0e484b66b25abb5d6aaa287558 | by: ui-improvements/analyze | rescoped: n/a | evidence: no recorded claude stream-json turn line read; the specification's own value
- Verify-first clause: before scoping, re-read the five profiles and `ProfileDocumentReaderTest` at the then-current HEAD; a refuted claim re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: 81e2c1fc59389c0e484b66b25abb5d6aaa287558 | by: ui-improvements/analyze | rescoped: n/a | evidence: procedural: the lane's verify-first step

## Expected Surface

- OBSERVED: `pm-api/src/main/resources/profiles/claude.json` — present at HEAD 81e2c1fc
- OBSERVED: `pm-api/src/main/resources/profiles/antigravity.json` — present at HEAD 81e2c1fc
- OBSERVED: `pm-api/src/main/resources/profiles/codex.json` — present at HEAD 81e2c1fc
- OBSERVED: `pm-api/src/main/resources/profiles/neutral.json` — present at HEAD 81e2c1fc
- OBSERVED: `pm-api/src/main/resources/profiles/opencode.json` — present at HEAD 81e2c1fc
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/host/ProfileDocumentReaderTest.java` — present at HEAD 81e2c1fc

## Dependencies and Sequencing

- Depends on: nothing in this epic.
- Blocks: pm-mcp-core PR #594 (PLAN-V2-135), cross-repo; its lane re-runs its red classes against the deployed `SNAPSHOT`.
- Overlaps with: none of the running plans (PLAN-178, PLAN-185, PLAN-187 touch `pm-web-app`; read by `git diff --name-only
  origin/main...origin/{branch}` on 2026-11-24, none names a file under `pm-api/src/main/resources/profiles/` or
  `pm-api/src/test/java/de/planmarshall/api/host/`).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-188-profiles-usage-model.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under
`.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The
session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md`
§ Ledger Write-Boundary).
