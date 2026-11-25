# PLAN-184: Terminal: `plan settings` Names the Cause the Runtime Sent

epic: ui-improvements
workstream: WS-05

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of 2026-11-23 (the question about the one layer-3 major of the PLAN-174 landing, `landings/PLAN-174.md` L3-1: "stage a small separate plan"). This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief. **It adds no dependency.** Emit it after PLAN-42 has landed (see Dependencies and Sequencing).

## Objective

`pm-operator plan settings <plan> --set <aspect>=<value>` is refused by the runtime with a code, and the operator should read the cause. Today the command shows a cause for one case only: the status `422` with the code `invalid_value` (`PlanCommand.java:1698`, constant `:111`), which neither `ApiErrorCode` nor the Runtime Model specification defines and which the mock never sends; the only other cause it names is "it is not one of {choices}", re-read from the settings. A conforming runtime answers `400 invalid_arguments` (the body violates the endpoint's schema) or a coded `409` (`team_owned`: a value the project's team owns; `base_branch_fixed`: a change of a plan's base branch after its worktree was created; `plan_ended`; `runtime-model/01-processes-and-transport.adoc:234,237`), and the operator then sees only the neutral line "{aspect} = {value} is refused; nothing changed." with exit 1, although the runtime named a cause the catalogue can render. The tests pin the invented pair. The result: the refusal names the cause of every code the specification lists for the settings write, the invented pair is gone, the `--yes` retry is covered, and the one code the specification leaves open (what a runtime answers for a value it refuses itself) is reported, not decided.

## Deliverables

Read first: `landings/PLAN-174.md` (L3-1, with its orchestrator note on the coded 409), `PlanCommand.java` `SettingsCommand` (class `:1563`, `--yes` `:1579`, `apply` `:1640-1668`, `put` `:1681-1700`, `refused` `:1702-1710`, `refusalLine` `:1729-1757`, `rereadChoices` `:1762-1776`), `PlanSettingsDialogue.java` (the interactive dialogue; it already names `control.settings.too_late` at `:181`, the one-shot follows the same rule where it fits), `OperatorRefusedException.java` (the refusal parser and `message(locale)` of PLAN-174 D4: reuse it, do not parse a body again), `PlanSettingsUpdateResult.java` and the Runtime Model error list (`01-processes-and-transport.adoc:233-241`).

1. **The refusal names the cause the runtime sent, and the invented pair is gone**
 - What:
   - Remove the `422` + `invalid_value` arm: `valueRefused` (`PlanCommand.java:1698`), the constant (`:111`), the `PutOutcome.valueRefused` member and the "does not fit" branch of `refusalLine` (`:1745`), with the key `control.settings.refused.does_not_fit` in both `control.*` packs.
   - `put` treats EVERY `409` as a `PlanSettingsUpdateResult` (`:1693`). Read first what a coded `409` body (`team_owned`, `base_branch_fixed`, `plan_ended`) yields there (HYPOTHESIS below), and separate a revision `conflict` (the existing conflict path, unchanged) from the coded `409` refusals: the latter are refused with their cause.
   - The cause comes from the runtime's own code through the catalogue: one text per code the specification lists for the settings write (`invalid_arguments`, `team_owned`, `base_branch_fixed`, `plan_ended`, and `act_not_available` where the plan's state does not offer the change), in English and German, keyed `control.settings.refused.<code>`; a code with no key falls back to the neutral line as today. Where the body carries a detail (a message key with arguments), `OperatorRefusedException` renders it; do not print the runtime's English on a German screen (PLAN-174 L3-7).
   - The refusal goes to stderr with exit 1, as every one-shot refusal does since PLAN-174. The `--no-input` conflict stop on stdout (`PlanCommand.java:1655-1666`) is NOT touched here beyond what the coded `409` split needs: where PLAN-42 deliverable 6 left it, it stays.
   - The "not one of {choices}" reason, re-read from the settings, stays and comes first where it applies.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `PlanCommandTest.java` (the lines that pin `422 invalid_value`, `:1192,1235-1240`, are rewritten; Generators, `@EnableGeneratorController`; the expected catalogue text stays literal): a `400 invalid_arguments`, a `409 team_owned`, a `409 base_branch_fixed` and a `409 plan_ended` answer to the write each show their cause on stderr with exit 1, in English and German; a revision `409 conflict` still follows the conflict path; a `422` with any code is no longer special; each fails with its production line reverted. Evidence order: `test-first`.
   - `ControlCommandTest.java` (`:420-429`): the test that pins "names a value that does not fit only on the runtime's code" is replaced by tests of the same coded answers; the `--yes` retry has a test where the retried write is refused with a coded `409`, and it fails with the retry's `refused(...)` call changed back. Evidence order: `mutation-after`.
   - `./mvnw -pl pm-api,pm-operator verify` passes, the number of tests is higher than before by the added tests, and no key of the catalogue pair is left without its counterpart (`OperatorCatalogueHygieneTest`, `EnrolCatalogueTest`-style parity). Evidence order: `none`.
2. **The code the specification leaves open is reported, and the design says what the client does**
 - What:
   - The Runtime Model specification lists no code for "the runtime refuses a submitted value itself": `invalid_value` on a `400` is named only in this repository's web design (`doc/design/web-app/machine-settings.adoc:214`, "read as its wire code by the client"), and the terminal design's `plan settings` row (`doc/design/terminal-client/01-commands.adoc:62`) names none. The plan does not choose a code. It reports the question in the pull request and the landing message for the owners of the catalogue and the Runtime Model specification (`plan-marshall-documentation`, `pm-mcp-core`) and states the client's rule in the design: a refusal is shown by the code the runtime sent through the catalogue, and a code the catalogue does not know shows the neutral line.
   - If the terminal design has a place that names the refusal of a settings value (the `plan settings` section of the terminal client's design documents), it is aligned to the rule above; if it has none, no design file is added.
 - Persona: plan-marshall:persona-documenter
 - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
 - Done when:
   - The pull request body and the landing message name the open question (the code for a value the runtime refuses) with the three places that mention it, and state that no code was chosen. Evidence order: `none`.
   - The design lines changed pass `pm-documents:ref-asciidoc` `validate` and `verify-links` (under the runbook lane `trace.py` does not run: `pm-documents:ref-documentation` `docs review` stands in, clean). Evidence order: `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api,pm-operator verify`, then the full `./mvnw verify` and `./mvnw verify -Ppre-commit` (review every rewrite) at the head; `-Pcoverage` at the head (minimum 80 % per module).
- No new dependency or plugin. Code standards of `CLAUDE.md`: Java 25, no generic `Exception` in production code, `CuiLogger` through `LogMessages`, JUnit 5 only (no Mockito, PowerMock or Hamcrest), input test data from `Generators` with `@EnableGeneratorController` (the expected value of a contract, a catalogue text or a wire name, stays literal). Every new text is in the catalogue in English and German; `LiteralTextGuardTest` stays green. Pre-1.0: no shim.
- A test is fed with the values the wire sends and the engine's records, never with a pair the contract does not define; every new or rewritten assertion is shown to fail with the production line reverted, the failing run cited.
- A finding that needs an engine or catalogue decision (the code for a refused value) is reported in the pull request and the landing message, never decided silently.
- The merge rule: the run merges only when every review thread has a reply and is resolved and every review body is answered, and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and review-body items from `ci pr comments`, never from memory. The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer. If CodeRabbit refuses on quota, stop and ask the operator.
- ZERO Sonar findings on the new code (the operator, 2026-11-05); state `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`).
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch. No browser check, no port.

## Claim Labels

- OBSERVED: `PlanCommand.put` computes `valueRefused` from status `422` and the code `invalid_value`, and `refusalLine` has one "does not fit" arm for it — read at `pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java:1698,111,1745`
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: PlanCommand.java:1742 valueRefused, :115 constant, :1789-1790 arm, :1722 PutOutcome (moved +44 since fcf87728) (3dea04b2)
- OBSERVED: the Runtime Model specification lists `400 invalid_arguments` and, among the `409` codes, `team_owned`, `base_branch_fixed` and `plan_ended`, and lists no `422` and no `invalid_value` — read at `../plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc:233-241`
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: plan-marshall-documentation runtime-model/01-processes-and-transport.adoc:248 (400) and :251 (409 list); no 422 or invalid_value; ApiErrorCode has no base_branch_fixed (3dea04b2)
- OBSERVED: `invalid_value` on a `400` is named only in this repository's web design — read at `doc/design/web-app/machine-settings.adoc:214`
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: machine-settings.adoc:214 is the only design hit; code PlanCommand.java:115, SettingsView.svelte:735 (3dea04b2)
- OBSERVED: the tests pin the invented pair — read at `pm-operator/src/test/java/de/planmarshall/operator/PlanCommandTest.java:1192,1235-1240` and `pm-operator/src/test/java/de/planmarshall/operator/ControlCommandTest.java:420-429`
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: ControlCommandTest.java:420-424; PlanCommandTest.java:1192,1235-1240 (3dea04b2)
- HYPOTHESIS: a coded `409` body (`team_owned`, `base_branch_fixed`, `plan_ended`) parsed by `PlanSettingsUpdateResult.fromJson` (`PlanCommand.java:1693`) ends on the neutral line with exit 1 and no cause — confirm/refute by running that parse on such a body in a test (verify-at-outline)
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: by reading: PlanCommand.java:1736-1737 parses every 200/409 body with PlanSettingsUpdateResult.fromJson; a coded body reaches refused() :1712 -> control.settings.refused_set; same hole in WorkActionsClient.java:244-245 (3dea04b2)
- HYPOTHESIS: `PlanSettingsDialogue` (`:181`) and the one-shot can share one text per code — confirm/refute by reading `PlanSettingsDialogue.java` (verify-at-outline)
  - verdict: contradicted | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: yes | evidence: dialogue and one-shot share no text today: PlanSettingsDialogue.java:223-230 prints control.settings.refused with the 400 body's reason (WorkActionsClient.java:246-251), no code (3dea04b2)
- Verify-first clause: before each deliverable re-read the named files at the then-current HEAD; PLAN-42 (deliverables 1, 2 and 6 edit `PlanCommand`, `PmOperator`, `ConfigCommand`, `ExitCodesTest`) lands first. A refuted claim re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: procedural; PLAN-42 not landed (3dea04b2)

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java` — present at HEAD fcf87728
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlanSettingsDialogue.java` — present at HEAD fcf87728
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/OperatorRefusedException.java` — present at HEAD fcf87728
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/PlanSettingsUpdateResult.java` — present at HEAD fcf87728
- OBSERVED: `pm-api/src/main/resources/messages/control.en.properties` — present at HEAD fcf87728
- OBSERVED: `pm-api/src/main/resources/messages/control.de.properties` — present at HEAD fcf87728
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PlanCommandTest.java` — present at HEAD fcf87728
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ControlCommandTest.java` — present at HEAD fcf87728
- OBSERVED: `doc/design/terminal-client/01-commands.adoc` — present at HEAD fcf87728
- OBSERVED: `doc/design/shared/work-actions-texts.adoc` — present at HEAD fcf87728
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/actions/WorkActionsClient.java` — present at HEAD 3dea04b2 (cleanup 2026-11-25, declared surface understated)
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/resource/ApiErrorCode.java` — present at HEAD 3dea04b2 (cleanup 2026-11-25, declared surface understated)
- OBSERVED: `pm-api/src/main/resources/messages/error.en.properties` — present at HEAD 3dea04b2 (cleanup 2026-11-25, declared surface understated)
- OBSERVED: `pm-api/src/main/resources/messages/error.de.properties` — present at HEAD 3dea04b2 (cleanup 2026-11-25, declared surface understated)
- OBSERVED: `pm-web-app/src/lib/views/SettingsView.svelte` — present at HEAD 3dea04b2 (cleanup 2026-11-25, declared surface understated)

## Dependencies and Sequencing

- Depends on: `PLAN-174` (shipped, #533: the refusal parser and the stderr rule this plan builds on).
- Overlaps with: `PLAN-42` (staged, emitted first: its deliverables 1, 2 and 6 edit `PlanCommand` regions more than 1200 lines away, `ConfigCommand`, `ExitCodesTest` and `PmOperator`; no textual conflict is expected, but the plan runs after it so that `PlanCommand` is read at the then-current HEAD), `PLAN-44`, `PLAN-48`, `PLAN-50`, `PLAN-63` (the CLI chain, staged: they also edit `PlanCommand`).
- Adjacent to: the Runtime Model specification and the catalogue (`plan-marshall-documentation`, `pm-mcp-core`), the owners of the open code.


## Re-Grounding 2026-11-25

Re-grounded by the cleanup of 2026-11-25 (read-only sub-agent, contradictions spot-checked by the orchestrator) at `3dea04b2`, against the code and the design and specification changes since 2026-11-15: #570 (`20f971fe`: `over_size`, `epic.overlap_found`, `stale`→`unreadable`, the phase `sanity-check`), pm-mcp-core #607/#614 and plan-marshall-documentation #201/#202 ("observe facts instead of predicting them", two rounds), clients #578 (protected project configuration waits for the operator's decision) and the inbox messages `doc-review-2026-11-17-001` and `doc-review-2026-11-24-001`. **This section binds over the body where they differ.**

- Claim 5 contradicted: the dialogue and the one-shot share no text today (`PlanSettingsDialogue.java:223-230` prints `control.settings.refused` with the 400 body's reason; `WorkActionsClient.java:246-251` carries no code). Sharing needs `WorkActionsClient` to carry the code.
- **D1**: per-code texts already exist in the error catalogue (`error.invalid_arguments/team_owned/plan_ended/act_not_available.*`, rendered by `RefusalRenderer` through `ErrorTexts`): route a coded 400/409 through `RefusalRenderer.render` after the "not one of" reason instead of five new `control.settings.refused.<code>` keys; add `BASE_BRANCH_FIXED` to `ApiErrorCode` with `error.base_branch_fixed.*` texts; fix the same coded-409 hole in `WorkActionsClient.java:244-250`. The specification lines moved to runtime-model `01-processes-and-transport.adoc:248,251`. Already done: stderr and exit 1 (PLAN-174 #533). Re-anchor (`PlanCommand.java` +44 lines).
- The PLAN-42 ordering is soft (disjoint regions).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-184-cli-plan-settings-refusal-causes.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).
