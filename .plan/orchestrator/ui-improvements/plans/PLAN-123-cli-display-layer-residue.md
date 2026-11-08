# PLAN-123: Terminal: What the Display Layer Left Open (PLAN-34 Residue)

epic: ui-improvements
workstream: WS-05

> Staged plan spec. One shippable unit of work, ready for hand-off on the `runbook` lane. Staged 2026-11-08 on the
> operator's word, from `landings/PLAN-34.md`. This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer
> and carries no brief.

## Objective

PLAN-34 (#371, `40dc879e`) added `pm-api` `Display` (`duration`, `time` with `TimeStyle`, `word`, `title`), the served
order and the change size. It merged with only one of its four deliverables fully met. The merge rule was also broken:
two CodeRabbit threads and the whole second review body were never answered.

Four kinds of work are still open:

- **The terminal still does not speak through one layer.** Several duration formatters remain, some twenty lone `-`
  placeholders remain, raw ids and wire tokens are still printed, and `Display.word` / `Display.title` have no caller.
- **Two clocks and two zones are still used.**
- **A German regression:** dates have lost the dot after the day.
- **No screen evidence exists** for any of it.

Close these, and answer the open review items on the threads where they stand.

## Deliverables

1. **The regressions and the unanswered review items**
 - What:
   - (a) `Display.java:247` formats `d MMM` for every language. German needs `d. MMM`, as the removed
     `OverviewFormatting.SHORT_DATE_DE` did, and as the design shows (`12. Sep.`, `16. Okt., 11:25`).
   - (b) `StatusCommand.java:461,473` and `ProjectsCommand.java:314` take `Instant.now()` / `Clock.systemUTC()`. They
     must use `OperatorContext.clock()` (`OperatorContext.java:218`), as `WebCommand` does. This is CodeRabbit thread
     `PRRT_kwDOVBOtss61Blxm` on #371. Check `PlanCommand:238`, `SystemCommand:154` and `OverviewFullScreenApp:220` for
     the same.
   - (c) A unit-kind landing without a unit renders an empty slot (`ProjectWorkLevel.java:306-312`, `landing  ·
     running`). Show a catalogue placeholder instead. `StatusCommand:435-441` prints the raw kind word `landing unit`
     and needs the same placeholder. This is thread `PRRT_kwDOVBOtss61Blxh`.
   - (d) A null ended time or `last_used_at` prints `just now` / `gerade eben` (`StatusCommand:467`,
     `WebCommand:622-624`). Missing data must not claim recency: use the catalogue's "unknown" or "never" form.
   - (e) `EpicViewFormatting.eventTime` (`:247-257`) formats in the system zone. Move it onto `Display` with the clock's
     zone.
   - (f) The second review body's nitpicks. `PlanCompletionLevelTest:77` and `PlanViewLevelTest` assert the German
     labels `Änderung` and `Änderung bisher`. `ProcessTreeTest:123` asserts that an absent `change` is omitted on
     serialization and parses back to null.
   - Reply on #371 to both threads and to the second review body, each with the commit that fixes it, and resolve the
     threads.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Each of (a) to (e) has a test that fails on a revert of its line. Evidence order: `test-first`.
   - (a)'s test asserts `12. Sep.` (or the JDK's CLDR month form, quoted from the run) in `de`.
   - (b)'s test runs `status` and `projects` at a fixed context clock and asserts the age.
   - The #371 replies exist, and both threads are resolved, read back through `ci pr comments --pr-number 371`.
     Evidence order: `none`.
2. **One formatter for durations and times**
 - What: move every remaining duration or time formatter onto `Display.duration` / `Display.time`, and delete the
   copies:
   - `PlanViewFormatting.java:50-63` (callers `PlanViewLevel:465,616,646`, `PlanCompletionLevel:81`,
     `WorkerActivityLevel:135`, `PlanViewFormatting:140`);
   - `OverviewFormatting.java:247-261` (2 days prints `48 h` today);
   - `SystemFormatting.java:57-68` (a literal `d` in German);
   - `PlanCommand.java:473-486` and `:1186-1193`.

   The same span reads the same in every view.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `git grep -n -E "formatSeconds|formatPlanDuration|uptime\(|static String duration\(" -- pm-operator/src/main`
     names none of the removed formatters. Evidence order: `none`.
   - A test shows 2 days as `2 d` / `2 T` in the overview, the plan view and the system view, in both languages.
     Evidence order: `test-first`.
3. **No raw text reaches the operator**
 - What:
   - Replace the lone `-` placeholders with catalogue text (a reason, a "none" or "unknown" word, or nothing where the
     design shows nothing). The sites:
     - `CredentialsCommand:138`;
     - `PlanCommand:932,1058`;
     - `PlanSettingsDialogue:120`;
     - `RolesCommand:173,250,278,410,419,510,512,521`; `:419` compares against `"-"` as a sentinel, so replace the
       sentinel too;
     - `StatusCommand:254,538`;
     - `PlanCompletionLevel:75,190,261`;
     - `PlanViewFormatting:131`;
     - `PlanViewLevel:642`;
     - `WorkerActivityLevel:142`.
   - The printed epic (`EpicCommand:275-313`) shows titles and state words, not raw plan ids, `phase · phaseState`
     wire tokens or English literals (`running plans:`, `none`, `completion review: ready`).
   - The waiver gate is printed through its catalogue word (`state.gate.*`, e.g. `state.gate.ci_check` at
     `common.en.properties:287`), never as `ci_check:integration-tests`. The sites are `QuestionsCommand:125` and
     `AnswerCommand:126-128,254-256,319`.
   - The four copied unit-title lookups (`EpicCommand:326-333`, `StatusCommand:448-454`, `ProjectWorkLevel:306-310`,
     `StateDialogue:438`) become `Display.title`, and state words use `Display.word`. Neither may fall back to the raw
     id.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java:junit-integration
 - Done when:
   - `TuiCapabilityIT` (or a guard test beside it) asserts that the captured screens and transcripts of the overview,
     plan view, printed epic, status, questions and projects contain none of: `remediation.`, `ci_check:`,
     `(step current)`, a lone `-` cell, or an ISO instant. The test fails when one is reintroduced. Evidence order:
     `mutation-after`.
   - `git grep -n "Display\.\(word\|title\)(" -- pm-operator/src/main` names at least the four former lookups.
     Evidence order: `none`.
4. **The screen evidence PLAN-34 owed**
 - What: screen captures through `cli-pty-dialogue-harness` (`ScreenCaptureIT`; command line in
   `doc/design/terminal-client/27-tui-testing.adoc`), saved as `pm-operator/src/test/resources/screen-captures/PLAN-123.txt`.
   They cover:
   - the plan view with and without a change size;
   - the completion level;
   - `status` and `projects` ages;
   - the printed epic.

   Capture each in `en` and `de`, at 120×30 and 80×24.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing
 - Done when:
   - The capture file exists, and each screen in it matches the design forms of `doc/design/terminal-client`.
     Evidence order: `none`.
   - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open
     (`unknown` for a failed read, never `0`). It is a confirmed `0`, or every open issue is dismissed. Evidence order:
     `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`). Besides the session's own checks, `./mvnw -pl pm-api,pm-operator
  verify` and `./mvnw -pl pm-api,pm-operator verify -Pintegration-tests` pass at the head, and so does
  `./mvnw -pl pm-api,pm-operator verify -Ppre-commit`, with every rewrite committed.
- No new dependency or plugin. Test input data comes from `Generators` with `@EnableGeneratorController`. Expected
  catalogue text stays literal. Every new text is in en and de.
- Never catch or throw a generic `Exception`/`RuntimeException` in production code. Prefer package-private to public.
  No compatibility shims: delete the old formatters.
- The web app is out of scope. If a shared catalogue value changes (e.g. `common.*`), run the web unit tests
  (`pm-web-app` `npm run test:ci`) and say so.
- Ports of this plan: mock web port `21230`, app port `21231` (`AGENTS.md` § Ports formula `20000 + 10 × 123`). Other
  plans run at the same time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited. Work only in the session's own worktree and branch.
- The pull request is merged only on the operator's word. Every review body is answered where the reviewer reads it,
  nitpicks included. Every thread is answered and resolved before the merge question is asked. The pull request body
  states the threads and the review-body items with their count. Commits end with `Co-Authored-By: plan-marshall
  <noreply@cuioss.de>`. The pull request body carries no generator footer.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`). An open Sonar issue
  is either fixed in a pushed commit, or dismissed:
  - The dismissal carries a stated reason, recorded as `{issue-key}: {reason}`.
  - It is transmitted through `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key}
    --transition {falsepositive|wontfix}`.
  - The merge gate reads the open-issue count at the head: `0` confirmed, or every remaining issue dismissed.

## Claim Labels

- OBSERVED: `Display.java:247` formats `d MMM` for every language. At `40dc879e^`, `OverviewFormatting.java:49,373`
  used `SHORT_DATE_DE = "d. MMM"`. Read by the orchestrator at `40dc879e` on 2026-11-08.
- OBSERVED: `StatusCommand.java:461` uses `Instant.now()` and `:473` uses `Clock.systemUTC()`. Read at `40dc879e`.
- OBSERVED: at `ProjectWorkLevel.java:306-312`, a null unit gives `unit=""` and an empty title. Read at `40dc879e`.
- OBSERVED: `git grep 'Display.(word|title)('` over `src/main` matches nothing at `40dc879e`.
- OBSERVED: `QuestionsCommand.java:125` prints `waiver request: {gate}` raw. Read at `40dc879e`.
- OBSERVED: the duration formatters, `-` placeholders and raw-id sites listed in the deliverables. From the read-only
  landing leaf of `landings/PLAN-34.md`, at `40dc879e`. Re-read each before editing.
- HYPOTHESIS: the real daemon sends `running_since` / `started_at` as ISO instants that reach the screen through
  `StatusCommand:254,532`. Not shown. Confirm or refute against the API specification (verify-at-outline).
- Verify-first clause: re-read the named lines at the current HEAD before editing.
  - PLAN-120 changes `EpicViewFormatting`, `EpicViewLevel` and `EpicCommand`.
  - PLAN-21 changes `AnswerCommand`.
  - PLAN-119 changes `StateDialogue`.
  - This plan starts after all three have landed.

## Expected Surface

- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/display/Display.java` — present at `40dc879e`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/display/DisplayTest.java` — present at `40dc879e`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/process/ProcessTreeTest.java` — present at `40dc879e`
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at `40dc879e`
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at `40dc879e`
- OBSERVED: `pm-api/src/main/resources/messages/operator.en.properties` — present at `40dc879e`
- OBSERVED: `pm-api/src/main/resources/messages/operator.de.properties` — present at `40dc879e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/StatusCommand.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ProjectsCommand.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WebCommand.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EpicCommand.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/QuestionsCommand.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/AnswerCommand.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/CredentialsCommand.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlanSettingsDialogue.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/RolesCommand.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/OverviewFormatting.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/overview/ProjectWorkLevel.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/planview/PlanViewFormatting.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/planview/PlanViewLevel.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/planview/PlanCompletionLevel.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/planview/WorkerActivityLevel.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/system/SystemFormatting.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/epicview/EpicViewFormatting.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/dialogue/StateDialogue.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/overview/OverviewFormattingTest.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/overview/ProjectWorkLevelTest.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/planview/PlanCompletionLevelTest.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/TuiCapabilityIT.java` — present at `40dc879e`
- OBSERVED: `pm-operator/src/test/resources/screen-captures/PLAN-123.txt` — absent; created by this plan

## Dependencies and Sequencing

- Depends on: `PLAN-34` (shipped, #371), `PLAN-120` (staged; `epicview/`, `EpicCommand`), `PLAN-21` (running;
  `AnswerCommand`), `PLAN-119` (running; `StateDialogue`)
- Overlaps with:
  - `PLAN-44` (staged; `planview/`, `overview/`, `system/`). This plan lands BEFORE PLAN-44, so PLAN-44 colours one
    formatter instead of six.
  - `PLAN-30` (staged; texts and errors).
  - `PLAN-79` (rewrites every `pm-operator` test).
  - `PLAN-23` (running; `operator.*.properties`).
- Adjacent to: `PLAN-36` (needs verb; `StatusCommand`)

## Hand-Off Command

Lane: `runbook`, a small-to-normal plan with mechanical breadth.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-123-cli-display-layer-residue.md
```

## Write-Boundary

The session implementing this spec touches only its own repository's source and tests. It creates and edits NO file
under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger
write. The session reports its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger
Write-Boundary.
