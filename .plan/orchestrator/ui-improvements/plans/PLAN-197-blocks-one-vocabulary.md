# PLAN-197: What a Decision or a Waiting Item Blocks: One Vocabulary

epic: ui-improvements
workstream: WS-01

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Cut from the inbox message
> `doc-review-2026-11-25-001` (clients #586; the third doc-review round's client follow-up: core #617, clients #585 `12281d41`,
> docs #203), whose one ask is "cut § 1 as one small plan".
> This spec is SELF-SUFFICIENT. **It adds no dependency.**

## Objective

The merged design and specifications have the server write what a decision blocks, from what waits on it, in one closed
vocabulary used by the decision and by the needs-operator list alike: `plan`, `epic`, `setup`, `entry`, `task`, `nothing`
(`doc/design/shared/needs-and-find.adoc#blocks` "What It Blocks", `doc/design/shared/texts-and-languages.adoc:137`,
`doc/design/web-app/components-tags.adoc:203`, `doc/design/shared/decisions-texts.adoc`). The words are the decision's words,
`decision.blocks.<value>`, with the wire value `nothing` shown through the key `decision.blocks.none` (the design keeps that key).

The client still speaks the old vocabulary in places: the needs-operator item takes `plan`/`epic`/`project` and is absent when it
blocks no work; the decision's documented `none` is the wire value `nothing`; the needs list words its blocker with its own keys
`overview.needs.blocks` / `overview.needs.blocks_nothing` and the kind words `common.kind.*`; the mock sends `project` and
`none`. PLAN-191 (#582) and PLAN-195 (#588) already moved the chat line `control.chat.line.decision_opened` and the mock's
`ChatRegistry.blocksOf` onto the vocabulary (verify, do not redo); its fallback for an absent value is still `no`.

Line numbers are at `origin/main` `9011e6d1`.

## Deliverables

1. **The contract and the mock carry the one vocabulary**
 - What:
   - `NeedsOperatorItem` (`pm-api/.../overview/NeedsOperatorItem.java:35-36,45,73-75`): `blocks` is one value of the vocabulary and never absent; the old `project` is `setup`. The record documents the six values; a missing or unknown value read from the wire is handled as the contract's reader handles an unknown closed value elsewhere in `pm-api` (state which one in the PR), never silently as "blocks nothing".
   - `DecisionEnvelope` (`pm-api/.../decision/DecisionEnvelope.java:36`): the value list names `nothing`, not `none`.
   - The mock runtime sends the vocabulary: `running-work.json:154` `"blocks": "project"` → `"setup"`; every decision `"blocks": "none"` (`running-work.json:2975,7325,7723` and any other scenario) → `"nothing"`; every needs-operator item carries `blocks` (`nothing` for the machine's own items and a setup that holds nothing). `ChatRegistry.blocksOf` (`ChatRegistry.java:1227-1233`) falls back to `nothing`, not `no`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:javadoc, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `NeedsAndFindContractTest` (or the item's own contract test) round-trips each of the six values and fails on an item without `blocks`; it fails with the production change reverted (evidence order: `test-first`).
   - A mock scenario test asserts that no scenario of `pm-mock-runtime/src/main/resources/scenarios/` carries a `blocks` value outside the vocabulary on a decision or a needs-operator item, and that every needs-operator item carries one (evidence order: `test-first`).
   - `git grep -n -E '"blocks": "(none|project|yes|no)"' pm-mock-runtime` has no hit, and `ChatRegistry.blocksOf`'s fallback is `nothing` with a test (evidence order: `none`).
2. **Both clients word a blocker with the decision's words**
 - What:
   - Delete `overview.needs.blocks` and `overview.needs.blocks_nothing` (`overview.{en,de}.properties:34-35`) and their rows in the design text table that lists them, if any.
   - The terminal's needs list (`OverviewFormatting.formatNeedsItemBlocks`, `:405-420`) and the web's (`InboxView.svelte:46-47,125-134`, `BLOCK_KINDS`, `KIND_WORD_KEYS`) show `decision.blocks.<value>`, `nothing` through `decision.blocks.none`. The kind words `common.kind.*` no longer serve "what an item blocks".
   - Every reader of `decision.blocks.<value>` maps the wire `nothing` to the key `decision.blocks.none`, through ONE helper per client, not per call site: the terminal's `AnswerCommand.java:234-239`, `ConsoleNotice.java:584`, `QuestionsCommand.java:139-140`; the web's `DecisionView.svelte:284,317,1503`. `DecisionView.isBlocking` (`:240`) tests `nothing`, not `none`. A value outside the vocabulary shows the client's unknown-value word, never a raw key and never "blocks the plan" (today `AnswerCommand:239` and `QuestionsCommand:140` fall back to `decision.blocks.plan`).
   - `client.ts:113` (`AttentionItem.blocks`) documents the six values and is no longer optional.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
 - Done when:
   - `OverviewFormattingTest` (en and de, literal catalogue text) shows `blocks the project's setup` for `setup` and `blocks nothing` for `nothing`; fails with the change reverted (evidence order: `test-first`).
   - The terminal decision readers' tests show `blocks nothing` for `nothing` and the unknown-value word for a value outside the vocabulary, not `blocks the plan` (evidence order: `test-first`).
   - `InboxView` and `DecisionView` tests (`InboxView.test.ts` or `attention.test.ts`, `DecisionView.test.ts:356` moved to `nothing`) show the decision's words for each value and `blocks nothing` for `nothing` (evidence order: `test-first`).
   - `git grep -n 'overview.needs.blocks' pm-api pm-operator pm-web-app doc/design` has no hit, the catalogue key guards (`ApiContractTest`, the web text guard) pass, and en and de stay in parity (evidence order: `none`).
   - Browser check: the needs list (`/` or its lens) and an open decision on `running-work`, en and de, 1440 × 900, light: before and after screenshots `197-{needs|decision}-{en|de}-{before|after}.png` show the decision's words (evidence order: `none`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`). At the head: `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify`, `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=21970 PM_WEB_PORT=21971 ./mvnw -pl pm-web-e2e verify -Pe2e -am`, the full `./mvnw verify` and `./mvnw verify -Ppre-commit`. main's own Playwright red (`designer.spec.js:264`/`:272`) is named and not counted unless this plan turns it.
- Ports of this plan: mock web port `21970`, app port `21971` (`AGENTS.md` § Ports). An assigned port that is in use is a blocker, never a reason to take another port.
- **No new dependency or plugin.** Pre-1.0: no shim, no alias accepting the old values.
- Java: Java 25, JUnit 5, input from `Generators` with `@EnableGeneratorController`; the wire values and catalogue texts are contract expectations and stay literal. `@NullMarked`; no generic exception catch.
- Web: ES modules; text only through text-treating sinks (no `{@html}`); no timer wait in a test; the text-guard baseline (`pm-web-app/scripts/vocabulary-baseline.json`) may only shrink.
- The design text tables are the source: catalogue keys follow `doc/design/shared/decisions-texts.adoc` and the needs-and-find design word for word; a design document this plan changes is updated in the same pull request.
- Browser check: `AGENTS.md` § Browser checks — mock on `21970`, app on `21971`, a fresh single-use pairing code, states by `POST /mock/goto/<state>` and a reload, read through `browser_snapshot`, write only to `.plan/temp/playwright-mcp/`, stop what you started. No Playwright MCP is a blocker for that Done-when, never replaced.
- ZERO Sonar findings on the new code of the pull request (SonarCloud `plan-marshall_pm-mcp-clients`), read at the head after the push; `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`). No `NOSONAR`.
- The merge rule (`CLAUDE.md`, Git Workflow): every review thread replied and resolved, every review-body item answered, one CodeRabbit review on some head, build and Sonar green; the PR body states the counts from `ci pr comments`. Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; no generator footer. If CodeRabbit refuses on quota, wait out the window and trigger once more; ask the operator only if it still refuses.
- The landing message states every claim against the merged tree (file names, counts of tests and screenshots).
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch. PLAN-194 runs at the same time and edits `ChatRegistry.java`, `running-work.json`, `fully-onboarded.json` and `ChatContextNarrativeTest.java`: whichever merges second merges `main` and keeps both changes; never touch its branch.

## Claim Labels

- OBSERVED: `NeedsOperatorItem.java:35-36` documents `plan`, `epic`, `project`, absent when it blocks no work; `:45` `@Nullable String blocks`; `:73-75` writes `blocks` only when non-null — read at `9011e6d1`
  - verdict: corroborated | checked_at: 9011e6d1 | by: ui-improvements/analyze | rescoped: n/a | evidence: NeedsOperatorItem.java:35-36,45,73-75 (orchestrator read at 9011e6d1)
- OBSERVED: `DecisionEnvelope.java:36` lists `none`; the design's wire value is `nothing` (`needs-and-find.adoc:54-56`) — read at `9011e6d1`
  - verdict: corroborated | checked_at: 9011e6d1 | by: ui-improvements/analyze | rescoped: n/a | evidence: DecisionEnvelope.java:36; needs-and-find.adoc:54-56 (orchestrator read at 9011e6d1)
- OBSERVED: `overview.{en,de}.properties:34-35` hold `overview.needs.blocks`, `overview.needs.blocks_nothing`; read by `OverviewFormatting.java:410,419` and `InboxView.svelte:131,133` (`BLOCK_KINDS` = plan/epic/project at `:47`) — read at `9011e6d1`
  - verdict: corroborated | checked_at: 9011e6d1 | by: ui-improvements/analyze | rescoped: n/a | evidence: overview.en.properties:34-35; OverviewFormatting.java:410,419; InboxView.svelte:47,131,133 (orchestrator read at 9011e6d1)
- OBSERVED: `control.{en,de}.properties:307` `decision_opened` already selects over plan/epic/setup/entry/task with `other {}`; `ChatRegistry.java:1227-1233` passes the payload's `blocks` through and falls back to `"no"` — read at `9011e6d1`
  - verdict: corroborated | checked_at: 9011e6d1 | by: ui-improvements/analyze | rescoped: n/a | evidence: control.en.properties:307; ChatRegistry.java:1227-1233 fallback "no" (orchestrator read at 9011e6d1)
- OBSERVED: the readers of `decision.blocks.<value>`: `AnswerCommand.java:234-239` (fallback `decision.blocks.plan`), `ConsoleNotice.java:584`, `QuestionsCommand.java:139-140` (fallback `decision.blocks.plan`), `DecisionView.svelte:284,317,1503`; `DecisionView.svelte:240` `isBlocking` tests `'none'` — read at `9011e6d1`
  - verdict: corroborated | checked_at: 9011e6d1 | by: ui-improvements/analyze | rescoped: n/a | evidence: git grep decision.blocks. in pm-operator/src/main and pm-web-app/src/lib; DecisionView.svelte:240 (orchestrator read at 9011e6d1)
- OBSERVED: the mock sends `"blocks": "project"` (`running-work.json:154`) and `"blocks": "none"` (`running-work.json:2975,7325,7723`); `client.ts:113` documents plan/epic/project, optional — read at `9011e6d1`
  - verdict: corroborated | checked_at: 9011e6d1 | by: ui-improvements/analyze | rescoped: n/a | evidence: running-work.json:154,2975,7325,7723; client.ts:113-114 (orchestrator read at 9011e6d1)
- HYPOTHESIS: the engine (pm-mcp-core #617) already sends `nothing` and `setup` and never omits `blocks` on a needs-operator item — confirm against pm-mcp-core `origin/main` at the outline; if the engine still sends the old values, report it and keep the client on the design
  - verdict: unverifiable | checked_at: 9011e6d1 | by: ui-improvements/analyze | rescoped: n/a | evidence: hypothesis for the outline; the engine repository was not read
- Verify-first clause: re-read the named lines at the then-current HEAD before each deliverable (PLAN-194 runs in the same mock files). A refuted claim re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: 9011e6d1 | by: ui-improvements/analyze | rescoped: n/a | evidence: procedural verify-first clause

## Expected Surface

- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/overview/NeedsOperatorItem.java` — present at HEAD 9011e6d1
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/decision/DecisionEnvelope.java` — present at HEAD 9011e6d1
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/overview/NeedsAndFindContractTest.java` — present at HEAD 9011e6d1
- OBSERVED: `pm-api/src/main/resources/messages/overview.en.properties` — present at HEAD 9011e6d1
- OBSERVED: `pm-api/src/main/resources/messages/overview.de.properties` — present at HEAD 9011e6d1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFormatting.java` — present at HEAD 9011e6d1
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/overview/OverviewFormattingTest.java` — present at HEAD 9011e6d1
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/overview/ProjectsOverviewLevelTest.java` — present at HEAD 9011e6d1
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/NeedsItemCommandsTest.java` — present at HEAD 9011e6d1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/AnswerCommand.java` — present at HEAD 9011e6d1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConsoleNotice.java` — present at HEAD 9011e6d1
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/QuestionsCommand.java` — present at HEAD 9011e6d1
- OBSERVED: `pm-web-app/src/lib/views/InboxView.svelte` — present at HEAD 9011e6d1
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.svelte` — present at HEAD 9011e6d1
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.test.ts` — present at HEAD 9011e6d1
- OBSERVED: `pm-web-app/src/lib/stores/attention.test.ts` — present at HEAD 9011e6d1
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD 9011e6d1
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/ChatRegistry.java` — present at HEAD 9011e6d1
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/running-work.json` — present at HEAD 9011e6d1
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/fully-onboarded.json` — present at HEAD 9011e6d1
- OBSERVED: `doc/design/shared/needs-and-find.adoc` — present at HEAD 9011e6d1

## Dependencies and Sequencing

- Depends on: nothing unshipped. PLAN-191 (#582) and PLAN-195 (#588) moved the chat line onto the vocabulary.
- Overlaps with:
  - running `PLAN-194`: `ChatRegistry.java`, `running-work.json`, `fully-onboarded.json` (3 code files; under the overlap rule's 5). Whichever merges second merges `main`.
  - staged `PLAN-196`: `client.ts` is not in PLAN-196's surface; none.
- Adjacent to: the needs list and decision views of WS-03.

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-197-blocks-one-vocabulary.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source, tests and design documents. It creates and edits NO file
under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The
session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` §
Ledger Write-Boundary).
