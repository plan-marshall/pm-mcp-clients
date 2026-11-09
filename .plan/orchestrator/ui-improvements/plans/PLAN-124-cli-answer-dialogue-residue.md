# PLAN-124: Terminal: The Answer Dialogue, Made Correct (PLAN-21 Residue)

epic: ui-improvements
workstream: WS-04

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged 2026-11-08 on the
> operator's word ("Fix plan, emit now") from `landings/PLAN-21.md`. This spec is SELF-SUFFICIENT: the emitted command is a
> one-line pointer and carries no brief.

## Objective

PLAN-21 (#372, `bedc5927`) gave the terminal its decision dialogue. It merged with all 20 of CodeRabbit's findings
unanswered (threads on head `70607faa`), and the landing analysis confirmed every one of them at the merge commit.

Four of the findings are major:

- **Sanitising.** Model- and runtime-supplied text reaches the terminal without control-sequence cleaning. The design
  forbids this in `doc/design/shared/decisions.adoc` (safety: every decision text is shown after removing control
  sequences).
- **The part question.** A part's question is never shown.
- **Multi-part decisions.** A decision with several parts counts as answered after its first part, and a test asserts
  that behaviour.
- **The chat key.** The advertised `h)` / `t)` chat key has no handler.

Make the dialogue do what the design says (`doc/design/shared/decisions.adoc`,
`doc/design/terminal-client/14-transcripts-decisions.adoc`), test it with tests that can fail, and answer the 20 threads
on #372.

## Deliverables

1. **Every decision text is cleaned before it is printed**
 - What:
   - Route every runtime- or model-supplied string through one sanitising path before it reaches the terminal. These are
     the sites that currently print raw text (line numbers at `bedc5927`):
     - evidence: the hint at `AnswerCommand.java:208` and the details at `:585`, plus the raw concatenation in
       `formatEvidenceHint` (`:213-258`);
     - subject, phase and gate: `:169,189,191,395,454,572`;
     - roles: `:740-767`;
     - the recommended label in the JLine prompt: `:796`;
     - the success echoes: `:885,911`;
     - the clarification question: `:860`;
     - the read-back: `:870`;
     - the error message: `:444`.
   - Prefer one sanitising writer around the dialogue's output over call-site patches. A new sink must not be able to
     bypass it.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-security, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - A test feeds an envelope whose evidence, subject, gate, role names, option labels, clarification and read-back all
     contain ESC/CSI/OSC sequences and C1 controls. It asserts that none reaches the output, on the dialogue, the
     `--no-input` and the direct paths. It fails on a revert of the cleaning. Evidence order: `test-first`.
2. **Every part, with its question; the answer keys the design defines**
 - What:
   - **Parts and their questions.** Show each part's question (`DecisionPart.question`) before its options; today only
     the description or title is printed (`:197-200`).
   - **Every part asked.** Ask every part in order. The decision completes only when every part is answered: one POST
     per the runtime contract, or one per part, whichever `pm-api` / the API specification states (read it, do not
     guess). Today only `parts.get(0)` is asked (`:596,688`).
   - **The direct form on a multi-part decision.** `answer <id> <key>` either takes one key per part, or refuses with
     the catalogue text and exit `3`. It never "answers" after one part.
   - **The test that locks the defect in.** Correct `PmConsoleHandoffTest:124`, which asserts the defect.
   - **Per-part permissions.** Honour each part's `allowOwnWords` and `allowNote` (`:652-658,792-798`); offer `w` / `n`
     only where they are allowed.
   - **The recommendation.**
     - For a costly or irreversible decision, show the recommendation, rationale and dissent only behind `?r`
       (`shared/decisions.adoc` #recommendation). Today they are always printed (`:715-720`).
     - On split votes or no consensus, nothing is preselected. Today Enter takes the recommendation (`:636,790`).
   - **The vote views.** `?<n>` shows the votes on option n. An option without a quorum reads `not voted on`
     (`decision.support.not_voted` exists).
   - **The chat key.** `h)` / `t)` opens the decision chat the design describes (`shared/decisions.adoc` #chat). If the
     terminal has no chat surface yet, the key is not advertised; the design and the code say the same either way. Today
     the key is advertised and falls through to `invalid_choice`.
   - **The dissenter.** Name the dissenter from the votes that disagree with the recommendation. Never use the first vote
     or the literal `quorum-b` (`:762-765`).
   - **The range.** Built from the actual option keys (`:601-602`).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Over the mock's 3-part decision (`running-work.json`, Q-012), the dialogue asks three questions with their texts,
     and the posted body carries three answers. The direct `answer Q-012 2` does not report success. Each test fails on
     a revert. Evidence order: `test-first`.
   - Tests cover a costly decision (recommendation hidden until `?r`), split votes (no preselection), a part without
     own-words (`w` not offered), `?2`, and the chat key. Evidence order: `test-first`.
3. **Responses checked, texts from the catalogue, nouns from the envelope**
 - What:
   - **Check every response before success is printed:**
     - withdraw (`:820`);
     - own-words answer (`:880`);
     - interpret (`:854-855,864-865`);
     - the direct `later` (`:271-279`), which today prints "stays open" without any request or existence check.

     A refusal prints the design's refused-answer line and exits non-zero.
   - **Texts from the catalogue.** Every operator text comes from the catalogue: replace the English literals at
     `:181-182,444,793,796,803,815`, and the invented fallbacks (`quorum-b`, `gate`).
   - **Nouns from the envelope.** Take the scope and project nouns from the envelope (`scope`, `blocks`, project), never
     from the subject text or hard-coded names (`:271-276,934-955`). The German templates select the noun's case
     (`du hast den Plan …`). `entry` maps to the catalogue's `entry` selector.
   - **The confirmation title** follows the decision's reversibility (`:693-694,835`). Costly is not "cannot be undone".
   - **Read-back and clarify.** An unclear interpretation returns to the options with the clarify bound. The read-back
     handles `e)` edit and `o)` choose option (`:857-874`).
   - **`--no-input`** without an answer lists the options and the range with `choice_required` (`:157,178-202`).
   - **Error reporting.** An HTTP failure is reported as a refusal with its catalogue text, not as "Error reading
     interactive input" (`:443-444`).
   - **`MessageCatalogue.get(locale, key)`** (`MessageCatalogue.java:173`) returns the unevaluated template as its
     Javadoc says, or is removed. It has no caller.
   - **The design text table.** Update the text table of `14-transcripts-decisions.adoc` with the keys the dialogue
     uses.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
 - Done when:
   - Tests for a refused withdraw, a refused own-words answer and `answer Q-999 later` each assert a non-zero exit and
     the refusal text. Evidence order: `test-first`.
   - `git grep -n -E '"(Error reading|quorum-b|pm-mcp-clients|pm-mcp-core)' -- pm-operator/src/main/java/de/planmarshall/operator/AnswerCommand.java`
     is empty. Evidence order: `none`.
   - A German-session test asserts the `none_fits_done` and `later_done` sentences for a plan, an epic and an entry.
     Evidence order: `test-first`.
4. **Tests that can fail, and the 20 threads answered**
 - What:
   - **Stubs that check what was sent.** The answer stubs of `QuestionsCommandTest` (`:670-745,1084-1136`, `:1124`)
     assert the posted body: option key, words, note, clarified answer.
   - **One locale per assertion.** Replace the EN-or-DE disjunctions (`:1076-1078,1143`) with one locale per
     assertion. The German arm currently checks `Ein Grund ist erforderlich`, which the catalogue does not contain.
   - **Generated input and parameterised variants.** Input data comes from `Generators` with
     `@EnableGeneratorController`. `formatEvidenceHintCoverage` (`:789-808`) becomes a `@ParameterizedTest`.
   - **`ScreenCaptureIT`.** At `:275-277` and `:163` it no longer accepts an unexpected exit as a capture: it asserts the
     exit code, or that an early exit was expected.
   - **`AnswerDialogueIT`.** It asserts the posted body for own words, waiver, later and direct, and the votes, dissent
     and evidence on the PTY screen.
   - **The 20 threads on #372.** Reply to each with the commit that fixes it, or the reason it does not apply, and
     resolve it. Answer the review body (nitpick and additional item) in one comment on #372.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing
 - Done when:
   - `ci pr comments --pr-number 372` reads 20 threads, each with a reply, 0 unresolved. Evidence order: `none`.
   - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open
     (`unknown` for a failed read, never `0`). It is a confirmed `0`, or every open issue is dismissed. Evidence order:
     `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`). Besides the session's own checks, `./mvnw -pl pm-api,pm-operator
  verify` and `./mvnw -pl pm-api,pm-operator verify -Pintegration-tests` pass at the head, and so does
  `./mvnw -pl pm-api,pm-operator verify -Ppre-commit`, with every rewrite committed.
- No new dependency or plugin. Test input data comes from `Generators` with `@EnableGeneratorController`; expected
  catalogue text stays literal. Every new text is in en and de.
- Never catch or throw generic `Exception`/`RuntimeException` in production code; package-private over public; no
  compatibility shims.
- Terminal check through the screen capture harness (`ScreenCaptureIT`; command line in
  `doc/design/terminal-client/27-tui-testing.adoc`), saved as `pm-operator/src/test/resources/screen-captures/PLAN-124.txt`:
  a 3-part decision, a costly decision before and after `?r`, and an evidence line with an injected escape sequence
  (shown cleaned). Capture each in `en` and `de`.
- Ports of this plan: mock web port `21240`, app port `21241` (`AGENTS.md` § Ports formula `20000 + 10 × 124`). Other
  plans run at the same time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- **The merge rule, stated hard because PLAN-21 broke it:**
  - The merge question is asked only when every review thread on this pull request has a reply and is resolved, and
    every review body is answered.
  - The pull request body states the thread count, the resolved count and the review-body items with their count, read
    through `ci pr comments`, never written from memory.
  - A landing message whose review counts disagree with the PR is a defect of the run.
  - The pull request is merged only on the operator's word.
  - Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`.
  - The pull request body carries no generator footer.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`). An open Sonar issue
  is either fixed in a pushed commit, or dismissed:
  - with a stated reason, recorded as `{issue-key}: {reason}`;
  - transmitted through `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition
    {falsepositive|wontfix}`.

  The merge gate reads the open-issue count at the head: `0` confirmed, or every remaining issue dismissed.

## Claim Labels

- OBSERVED, all read by the orchestrator at `bedc5927` on 2026-11-08:
  - `AnswerCommand.java:196-209` prints description or title only, and the evidence hint without sanitising.
  - `:594-598` asks `envelope.parts().get(0)` only.
  - `:818-822` and `:878-884` discard `runtime.send(...)` results before printing success.
  - `question()` occurs 0 times in the file.
  - `handleDecisionCommand` has no `h` / `t` branch.
  - `PmConsoleHandoffTest:122-126` answers a 3-part decision with one key.
- OBSERVED: all 20 CodeRabbit findings of #372 are present at `bedc5927`, from the read-only landing leaf of
  `landings/PLAN-21.md`. That record has the per-finding table.
- HYPOTHESIS: the runtime contract answers a multi-part decision in one POST carrying every part's answer. It is not
  read. Confirm or refute it at `pm-api` (`DecisionEnvelope`, `Answers`) and the API specification (verify-at-outline).
  The answer shapes deliverable 2.
- HYPOTHESIS: the terminal has no decision-chat surface yet. Confirm or refute it at the terminal design and
  `pm-operator` (verify-at-outline). It decides between handling the chat key and not advertising it.
- Verify-first clause: re-read the named lines at the current HEAD before editing.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/AnswerCommand.java` — present at `bedc5927`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/Answers.java` — present at `bedc5927`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/message/MessageCatalogue.java` — present at `bedc5927`
- OBSERVED: `pm-api/src/main/resources/messages/decisions.en.properties` — present at `bedc5927`
- OBSERVED: `pm-api/src/main/resources/messages/decisions.de.properties` — present at `bedc5927`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/AnswersTest.java` — present at `bedc5927`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/QuestionsCommandTest.java` — present at `bedc5927`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PmConsoleHandoffTest.java` — present at `bedc5927`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/AnswerDialogueIT.java` — present at `bedc5927`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/ScreenCaptureIT.java` — present at `bedc5927`
- OBSERVED: `pm-operator/src/test/resources/screen-captures/PLAN-124.txt` — absent; created by this plan
- OBSERVED: `doc/design/terminal-client/14-transcripts-decisions.adoc` — present at `bedc5927`

## Dependencies and Sequencing

- Depends on: `PLAN-21` (shipped, #372)
- Overlaps with:
  - `PLAN-123` (staged): the waiver gate in `AnswerCommand`. PLAN-123 now starts after this plan.
  - `PLAN-79`: rewrites every `pm-operator` test.
  - `PLAN-23` (running): `ScreenCaptureIT` is shared harness; its own capture files are separate.
- Adjacent to: `PLAN-115` (D4 chat): the decision chat.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-124-cli-answer-dialogue-residue.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under
`.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write.
The session reports its outcome through its PR and its inbox message. The qualifiers of the inbox exception, and the
sole sanctioned write mechanism, are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger
Write-Boundary.
