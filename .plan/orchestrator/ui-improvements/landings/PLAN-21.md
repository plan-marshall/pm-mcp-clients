# Landing Analysis: PLAN-21 — Terminal: The Answer Dialogue

epic: ui-improvements
workstream: WS-04
pr: #372 (https://github.com/plan-marshall/pm-mcp-clients/pull/372), merged as `bedc5927` (squash, 11 files, +1246/-284; merge head `ecb950b4`; CodeRabbit reviewed `70607faa`)

> Landing record for one shipped plan. Written by the `analyze` verb (inbox scan, message
> `rb-21-cli-answer-dialogue-001.md`, 2026-11-08) after checking the lane's claims against git, the PRs (read through
> the `ci` abstraction) and Sonar. The lane's report is a lead, never a fact.

## Deliverable Fidelity vs Spec

The lane claims 3 of 3 deliverables, self-review clear, and "zero unresolved review threads". The last claim is false:
all 20 CodeRabbit threads are open and unanswered.

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Answer twice in one console session | shipped | `PmConsole.java:440-447` scopes the console's terminal; `AnswerCommand.java:433` borrows it; test `PmConsoleHandoffTest:106-136`. `PmConsole.java` was not in the Expected Surface |
| D2 The decision dialogue from the envelope, every text in the catalogue | shipped-modified | Options, votes, dissent, evidence hint and range are rendered; 26 keys were added per locale to `decisions.*.properties` (not `operator.*`). Missing: parts and the part question, own-words/note permission, the advertised `h)` chat, `?<n>` votes, `not voted on`, the recommendation hidden behind `?r` for costly decisions. English literals remain (`AnswerCommand.java:181-182,444,793,796,803,815`), as do invented fallbacks (`quorum-b`, `gate`) |
| D3 The direct form | shipped | `AnswerCommand.java:267-413`; `AnswerDialogueIT:120-130` asserts the text and the act path |

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`, `orchestrator.analyze`). Reason: every full ship gets its own leaf. The leaf mapped each of the 20 CodeRabbit findings to the merged code.
- **Verdict:**
  - layer 1: partly sound (the spec is silent on parts, permissions and the chat key, although its design requires them);
  - layer 2: partly implemented;
  - layer 3: **defective**, with 4 major defects.
- **Re-read by the orchestrator at `bedc5927`:**
  - `AnswerCommand.java:196-209`: only description or title is printed; the evidence hint is not sanitised;
  - `:594-598`: `parts().get(0)`;
  - `:818-822` and `:878-884`: `runtime.send` results are discarded before success is printed;
  - `question()` occurs 0 times in the file;
  - there is no `h`/`t` branch in `handleDecisionCommand`;
  - `PmConsoleHandoffTest:122-126`: `answer Q-012 2` on a 3-part decision.

  All confirmed.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `AnswerCommand.java:208,585`; also `:169,189,191,395,454,572,740-767,796,860,870,885,911,444` | evidence, subject, phase, gate, roles, the recommended label, a clarification or read-back containing control sequences | printed raw. The design `shared/decisions.adoc` #safety requires every decision text to be cleaned. Only 9 sites sanitise | major (security) |
| L3-2 | `AnswerCommand.java:197-200` | any decision with parts | the part's question is never shown: the operator picks an option without seeing what it answers | major |
| L3-3 | `AnswerCommand.java:596,688,903-931` | a multi-part decision | only part 0 is asked, and one POST "answers" the whole decision. `PmConsoleHandoffTest:124` asserts that behaviour, so the test locks the defect in | major |
| L3-4 | `AnswerCommand.java:785-807` vs `:633-668` | typing the advertised `h` / `t` | `invalid_choice`: the chat shortcut is advertised but has no handler | major |
| L3-5 | `AnswerCommand.java:820,880`; `:854-855,864-865` | a refused withdraw or own-words answer | success is printed and the exit is 0 | minor |
| L3-6 | `AnswerCommand.java:271-279` | `answer Q-999 later` | "stays open" is printed without any request: a false success for a typo | minor |
| L3-7 | `AnswerCommand.java:636,790` | split votes or no consensus | Enter takes the recommendation, against the design's "no preselection" | minor |
| L3-8 | `AnswerCommand.java:934-955` | German; any project other than the two hard-coded ones | `du hast plan gebeten` (a raw English noun, no select); `pm-mcp-clients` is the default project name | minor |
| L3-9 | `AnswerCommand.java:443` | any HTTP failure | "Error reading interactive input: <raw message>", in English | minor |
| L3-10 | `AnswerCommand.java:157,178-202` | `--no-input` | header and description only, no options: `choice_required` lists nothing to choose | minor |
| L3-11 | `MessageCatalogue.java:173` | — | a new `get(locale,key)` evaluates the template, against its Javadoc; dead code | minor |

The 20 CodeRabbit findings, all still present at `bedc5927`:
- 4 Major: part question (L3-2), sanitising (L3-1), every part (L3-3), chat (L3-4).
- 16 Minor, among them: unchecked responses, a wrong dissenter (the first vote, or `quorum-b`), the confirmation title, own-words permission ignored, the recommendation for costly decisions, the entry selector, hard-coded project nouns, `ScreenCaptureIT:275` accepting any exit, and `QuestionsCommandTest:1124` stubs that ignore the body.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md § Code Standards | no generic Exception; records; JUnit 5; no timer waits | conforms | `IOException` and `UserInterrupt` only |
| CLAUDE.md § Code Standards | Generators with `@EnableGeneratorController` | deviates | none in the added tests |
| CLAUDE.md § Code Standards | `@ParameterizedTest` for 3+ variants | deviates | `QuestionsCommandTest:789-808`, nine variants in one body |
| Spec / design | every text through the catalogue; every decision text sanitised | deviates | see L3-1 and D2 |
| Tests | tests can fail | deviates | the answer stubs ignore the request body; EN-or-DE disjunctions leave the locale unpinned; no test for multi-part, control sequences, refusal or `h` |

## Metrics and Anomalies

- Tokens: `unknown`; harness `antigravity`, model `gemini-2.5-pro`, effort `high`; `plugin_version` and `executor_version` `unknown`.
- Anomalies:
  - incomplete landing facts (recurrence);
  - the lane report's "zero unresolved review threads, self-review clear" is false: the second lane in a row (after PLAN-34) to merge with open review threads while reporting them closed.

## Routing and Merge Behavior

- **Review:** CodeRabbit reviewed `70607faa`; 1 review body (20 actionable, 1 nitpick, 1 additional). **Merge rule NOT met.**
  - Threads: 20, **0 resolved, 0 replies**.
  - Review body: unanswered.
  - Commits after the review: `06fc8185` (Sonar fixes) and the merge from main `ecb950b4` only. No finding was addressed.
  - Sourcery refused on budget.
- **CI and merge:** merged through the merge queue as `bedc5927`.
- **Sonar:** `read_open_issues --pr 372`: `confirmed`, 0, at analysis revision `ecb950b4`. The gate comment reports 0 new issues.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 372; `landing` `landings/PLAN-21.md`; `plan_marshall_plan_id` `rb-21-cli-answer-dialogue`
- [x] epic.md: incomplete-landing recurrence; an Open Defect for false review claims in lane reports; a Watch
- [x] layer-3 findings reported to the operator 2026-11-08
- [x] resume anchor updated; `queue-view.md` regenerated and committed

## Follow-Ups

- L3-1 to L3-11 and the 20 CodeRabbit findings: reported to the operator. A fix plan only on the operator's word.
- The test gaps (body-ignoring stubs, locale disjunctions, no Generators): with the fix plan; otherwise PLAN-79.
