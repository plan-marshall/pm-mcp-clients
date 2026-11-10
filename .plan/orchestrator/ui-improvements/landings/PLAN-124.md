# Landing Analysis: PLAN-124 — Terminal: the Answer Dialogue, Made Correct (PLAN-21 Residue)

epic: ui-improvements
workstream: WS-04
pr: #385 (https://github.com/plan-marshall/pm-mcp-clients/pull/385), merged as `be8c8403` (squash through the merge queue, 2026-11-09; parent `36fb27d9`); review answers on PLAN-21's #372

> Runbook lane (harness opencode, model deepseek-v4.1-flash/high, executor 0.1.2090). Inbox message
> `rb-124-cli-answer-dialogue-residue-001.md`. Checked against git, the PRs (`ci pr comments` of #385 and #372),
> SonarCloud and a read-only leaf; key points re-read by the orchestrator at `be8c8403`. Analysed 2026-11-10.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 every decision text cleaned before it is printed | met (core), tests partly | one `SanitizingPrintWriter` wraps the verb's out (`AnswerCommand.java:134`); no print path outside it found; `AnswerSanitizationTest` dialogue/`--no-input`/waiver fail on a sink revert, the direct-path test cannot (its line already used `stripControlSequences`); order `mutation-after`, spec asked `test-first` (disclosed) |
| D2 every part asked with its question; keys per design | met | `printPartPosition :886-898`, `askRemaining :750-776`, one POST with `part_answers` (`submitPartAnswers :1312-1334`); direct key on a multi-part decision exits 3 (`:391-401`); costly `?r`, split no preselect, `?<n>`, `w)`/`m)` per part, no chat key — `AnswerMultipartTest`, `AnswerDialogueIT.threePartDecisionAsksEveryPart` |
| D3 responses checked, texts from the catalogue, nouns from the envelope | partly | every act's response is checked and a refusal exits non-zero, BUT the refusal text is `OperatorRefusedException`'s English literal "refused by the runtime (HTTP n)" (`OperatorRefusedException.java:33-36`), not `decision.refused.*`, and not "printed with the decision as it stands" (`14-transcripts-decisions.adoc:190`); German nouns met (`AnswerRefusalTest.germanNouns`); residue grep empty |
| D4 tests that can fail; #372's 20 threads answered | met | #372: 20 threads, 0 unresolved, all replied, body answered (re-read); `AnswerDialogueIT` asserts the posted bodies; Sonar read recorded |

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`, `orchestrator.analyze`).
- **Verdict:** layer 1 findings — 2 major (spec vs design on refusals; `OperatorRefusedException` outside the catalogue rule); layer 2 findings — 1 major, 8 minor; layer 3 findings — 9 minor, 3 info.
- **Re-read by the orchestrator at `be8c8403`:** `OperatorRefusedException.java:28-36`; `AnswerCommand.java:560-572` (refusal rethrown), `:983-998` (`dissentForSummary`); `14-transcripts-decisions.adoc:190`; the three new test classes carry no `Generators`/`@EnableGeneratorController` (grep count 0). All confirmed.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 (major) | The spec's "a refusal prints the refused-answer line and exits non-zero" contradicts the design: refused answers (`conflict`, `already_answered`, `invalid_answer`, `dependency_unanswered`) are printed with the decision as it stands, never as an error; `invalid_answer`/`dependency_unanswered` keep the decision open at its prompt | checked | refutes | `14-transcripts-decisions.adoc:190`, `04-console.adoc:138` |
| L1-2 (major) | D3's "every operator text from the catalogue" did not name the shared `OperatorRefusedException` | checked | refutes | `OperatorRefusedException.java:33-36` |
| L1-3 | Multi-part `part_answers` contract hypothesis | checked | confirms | `DecisionAnswerRequest.java:56` |
| L1-4 | whether pm-mcpd sends `project` on a decision (new `DecisionEnvelope.project`); the mock never does | not shown by reading | — | `DecisionEnvelope`; mock scenarios unchanged |

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained / unborne | Verdict |
|-------------|-----------|--------------------|-----------------------|---------|
| D1 | `SanitizingPrintWriter`, `:134` | 3 of 4 `AnswerSanitizationTest` | direct-path test vacuous; LF/TAB collapse and exception-message sanitizing untested; helper allows LF/CR/TAB | checked, findings |
| D2 | `:750-1001` | `AnswerMultipartTest`, `AnswerDialogueIT` | multi-part confirmation title ignores costly/irreversible (`:1264-1266`) | checked |
| D3 | `:341,564-566,1368,1469,1498` | `AnswerRefusalTest` pins the English literal | catalogue `decision.refused.invalid_answer`/`dependency_unanswered` unused; transport `IOException` still reads as `input_error` (`:567-571`) | finding (major) |
| D4 | IT posted-body asserts; mock `recordActBodies` | yes | mock README (`pm-mock-runtime/README.adoc:137`) not updated; flag untested | checked |

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `AnswerCommand.java:983-998` | dissenters roleX (r1), roleY (r2), summary r2 | "Against it (roleX): r2" — the first disagreeing voter is taken before the reason match | minor |
| L3-2 | `:391-401` | envelope read fails (non-200/IOException) | multi-part guard fails open; direct single key sent | minor |
| L3-3 | `:427-435` | `answer Q --plan X` and 404 on the plan path | retries the unscoped `/api/v1/questions/{id}/answers` | minor |
| L3-4 | `:146-152` | 401/500 on the first GET | body parsed as an empty decision | minor |
| L3-5 | `:1125-1147` | option key of 10+ digits | `NumberFormatException` aborts every view (`\d{1,9}` fixed only at `:1251`) | minor |
| L3-6 | `decisions.de.properties` `decision.project.fallback` | German, no `project` on the envelope (always on the mock) | "das Setup von das Projekt" | minor |
| L3-7 | `:1437-1457`, `:1428-1434` | UNCLEAR with no clarification question; a typo at the read-back | posted as understood; typo silently drops the reading | minor |
| L3-8 | `TerminalSanitizer.java:49-70` | bidi (U+202E), zero-width, tag characters in model text | pass through; meets "remove control sequences", not "text only" | minor |
| L3-9 | `:1417-1435`; `AnswerDialogueIT.ownWordsAnswers` | own words | posts the interpreter's read-back as `words`, not the operator's text; contract not shown | info |
| L3-10 | `:783-785`, `:1228-1230`, `:591-593` | EOF at a prompt | exit 0, no "Left" line | info |

**Layer 3 verdict:** findings — 8 minor, 2 info (no blocker).

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md § Code Standards | Generators + `@EnableGeneratorController` | deviates | `AnswerMultipartTest`, `AnswerRefusalTest`, `AnswerSanitizationTest`, `AnswerDialogueIT` none; the PR body claims it |
| CLAUDE.md § Code Standards | `@ParameterizedTest` for 3+ variants | deviates | four sanitization paths, three nouns as separate tests/loops |
| CLAUDE.md § Code Standards | no generic Exception; no System.out; no new dependency | conforms | — |
| Spec D1/D2 | evidence order `test-first` | deviates (disclosed) | `mutation-after` |
| Spec Expected Surface | — | deviates, justified | `DecisionEnvelope`, `PmOperator`, `OperatorRefusedException`, pm-mock-runtime (3 files), `27-tui-testing.adoc`, `decisions-texts.adoc` |

## Metrics and Anomalies

- Tokens/plugin version: `unknown`; the facts block lacks `unresolved_review_threads`, `review_body_findings_*`, `sonar_open_new_issues` → landing-check `complete: false` (opencode incomplete-facts recurrence).
- Anomalies: integration profile run with `pm-mock-runtime` added to `-pl` (stale snapshot); shell `tail`/`grep` and a `fetch` of the public GitHub API (deviations recorded by the lane).

## Routing and Merge Behavior

- **Review (re-read from `ci pr comments`):** #385 — CodeRabbit 5 bodies (13, 3, 1 + 1 outside-diff, 1, 0 actionable), 18 threads, 0 unresolved; 16 replied, 2 resolved by CodeRabbit as addressed without a reply (`MockRuntimeEngine`, `OperatorRefusedException`) — the lane's "every thread replied" is off by those two, not material; the bodies with non-inline items answered by PR comments (three findings; outside-diff). #372 — 20 threads, 0 unresolved, all replied, body answered. **Merge rule met.** Merged on the operator's word on a standing review of `228b9179` (head `c8768f1e` skipped by CodeRabbit).
- **CI/merge:** green; merge queue.
- **Sonar:** PR #385 0 issues, gate OK, new coverage 81.9% (two new-code issues fixed before merge).

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 385; `landing`; `plan_marshall_plan_id` `rb-124-cli-answer-dialogue-residue`
- [x] epic.md Watch for PLAN-124; Open Defect (incomplete facts) recurrence
- [x] resume anchor updated; `queue-view.md` regenerated

## Follow-Ups

- Residue plan (operator's word): refusals per the design (printed with the decision, from `decision.refused.*`, the
  open decision kept at its prompt; `OperatorRefusedException` text into the catalogue), L3-1 … L3-8, the vacuous
  direct-path test, sink unit tests, the multi-part confirmation title, the mock README, the Generators rule.
- Unblocks PLAN-123 and PLAN-33.
