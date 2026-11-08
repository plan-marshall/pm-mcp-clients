# PLAN-121: Terminal: `project add` Onboards, Release and Unenrol Confirm

epic: ui-improvements
workstream: WS-04

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane (a small plan, per the operator
> rule of 2026-11-07). Staged 2026-11-08 on the operator's decisions about the two questions PR #369 (PLAN-118) asked. This
> spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

PLAN-118 recorded two gaps as open items in `doc/design/terminal-client/17-projects.adoc` (`project-add-onboarding`,
`release-confirmation-prompt`) and asked the operator about them. The operator decided both on 2026-11-08, in favour of the
design and the direct-verb rule of `01-commands.adoc` § A Direct Verb Runs a Sub-Workflow. That rule says the arguments
are answers given in advance, a choice state without an answer is asked, and with `--no-input` the client stops with exit
code `3`.

1. **`project add` runs the project onboarding.** It starts the onboarding of a project from the identity step through
   the enrolment (`project.identity` … `project.enrol`). The working directory is the proposed checkout, and `--pilot`
   answers the pilot line of the identity. A missing answer is asked; with `--no-input` the verb stops with exit `3` and
   names the missing answer. Today `ProjectCommand.AddCommand` (`ProjectCommand.java:164-222`) sends `POST /api/v1/enrol`
   and asks nothing.
2. **`project release` and `project unenrol` always confirm.** Without `--yes` they show the confirmation of
   `17-projects.adoc` § Release, in the shell and in the console alike. `project release` also asks the reason when
   `--reason` is missing. `--yes` answers the confirmation in advance. `--no-input` without `--yes` (or, for release,
   without `--reason`) keeps exit `3`. Today both send the act at once unless `--no-input` is given
   (`ProjectCommand.java:361-371`, `:436`).

## Deliverables

1. **`project add` is the direct verb of the project onboarding**
 - What:
   - Replace the direct enrolment with the onboarding run the terminal already drives: `EntryFlow`, `ProjectOnboardingSource`,
     `StateDialogue`. `project setup --entry` and the being-added rows use the same run.
   - Find from the runtime contract how a project onboarding is started: read `plan-marshall-documentation`
     `doc/specification/runtime-model/01-processes-and-transport.adoc` and the `project-onboardings` resource. Today
     `EntryFlow.java:641` reads `GET /api/v1/project-onboardings`. Do not guess the start request.
   - Arguments answer their states: the working directory or `<path>` answers the checkout, and `--pilot` answers the
     pilot line. An argument no state asked for is reported after the run, as the direct-verb rule says.
   - Close the open item `project-add-onboarding` in `17-projects.adoc`, and make the verb line and the onboarding section
     say the same.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing,
   pm-documents:ref-asciidoc
 - Done when:
   - `ProjectCommandTest` shows the following, against the stub daemon:
     - an interactive `project add` reaches the identity state and asks;
     - `project add --no-input` without a required answer exits `3` and names it;
     - with every answer given, it runs through to the enrolment without a prompt;
     - no `POST /api/v1/enrol` is sent outside the onboarding's own enrol step.

     Each test fails on a revert to the direct enrolment. Evidence order: `test-first`.
   - The transcript harness (`ProjectSetupDialogueIT` or a new `ProjectAddDialogueIT` beside it) captures one
     interactive `project add` through the identity step. Evidence order: `none`.
2. **`project release` and `project unenrol` confirm on every path**
 - What: without `--yes`, both verbs show the confirmation (the texts of `17-projects.adoc` § Release; reuse
   `operator.projects.release.confirm` and the existing keys before adding any). `project release` asks the reason when
   `--reason` is missing. This holds in the shell and in the console. Keep the `--no-input` exits. Close the open item
   `release-confirmation-prompt` in `17-projects.adoc`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
 - Done when:
   - `ProjectCommandTest` shows the following for each verb:
     - without `--yes`, the confirmation is asked, and declining sends nothing;
     - `--yes` sends without asking;
     - `--no-input` without `--yes` exits `3`;
     - `project release` without `--reason`, interactive, asks the reason.

     Each test fails on a revert of its branch. Evidence order: `test-first`.
   - `17-projects.adoc` names no open item `project-add-onboarding` or `release-confirmation-prompt`, and its verb lines
     match the behaviour. Evidence order: `none`.
   - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open
     (`unknown` for a failed read, never `0`). It is a confirmed `0`, or every open issue is dismissed. Evidence order:
     `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`). Besides the session's own checks, `./mvnw -pl pm-api,pm-operator verify`
  passes at the head, and so does `./mvnw -pl pm-api,pm-operator verify -Ppre-commit` with every rewrite committed.
- No new dependency or plugin. Test input data comes from `Generators` with `@EnableGeneratorController`; expected
  catalogue text stays literal. Every new text is in en and de.
- Never catch or throw a generic `Exception` or `RuntimeException` in production code. Prefer package-private to public.
  Pre-1.0: no flag that keeps the old direct enrolment (the operator rejected that variant).
- Ports of this plan: mock web port `21210`, app port `21211` (`AGENTS.md` § Ports formula `20000 + 10 × 121`). Other plans
  run at the same time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The pull request is merged only on the operator's word. Every review body is answered where the reviewer reads it,
  nitpicks included, and the pull request body states the threads and review-body items with their count. Commits end
  with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`). An open Sonar issue is
  either fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` and transmitted
  through `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition
  {falsepositive|wontfix}`. The merge gate reads the open-issue count at the head: `0` confirmed, or every remaining issue
  dismissed.

## Claim Labels

- OBSERVED: `ProjectCommand.AddCommand` (`ProjectCommand.java:164-222`) sends `POST /api/v1/enrol` with no dialogue.
  `ReleaseCommand` (`:326`) and `UnenrolCommand` (`:409`) require `--yes` only with `--no-input` (`:371`, `:436`). Read
  at `origin/main` `ef7d2389` on 2026-11-08, matching PLAN-118's confirmed hypothesis.
- OBSERVED: the open items `project-add-onboarding` and `release-confirmation-prompt` stand in `17-projects.adoc:36,38`.
- HYPOTHESIS: the runtime starts a project onboarding through the `project-onboardings` resource (the terminal reads it at
  `EntryFlow.java:641`, and `OverviewFullScreenApp.java:3181` addresses one by id). Confirm or refute against the API
  specification (verify-at-outline). If no start request exists, report it before building and stop for the operator.
- Verify-first clause: re-read the named lines at the current HEAD before editing. PLAN-119 (running) changes
  `EntryFlow`, `ProjectOnboardingSource` and `StateDialogue`. This plan starts after PLAN-119 has landed and builds on its
  step-total rule.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ProjectCommand.java` — present at `ef7d2389`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ProjectCommandTest.java` — present at `ef7d2389`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EntryFlow.java` — present at `ef7d2389` (only if the start of an onboarding needs a new entry point)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/transcript/ProjectSetupDialogueIT.java` — present at `ef7d2389`
- OBSERVED: `pm-api/src/main/resources/messages/operator.en.properties` — present at `ef7d2389`
- OBSERVED: `pm-api/src/main/resources/messages/operator.de.properties` — present at `ef7d2389`
- OBSERVED: `doc/design/terminal-client/17-projects.adoc` — present at `ef7d2389`

## Dependencies and Sequencing

- Depends on: `PLAN-118` (shipped, #369), `PLAN-119` (running; `EntryFlow` and the onboarding sources)
- Overlaps with:
  - `PLAN-34` (running; `17-projects.adoc`, `operator.*.properties`);
  - `PLAN-21` and `PLAN-23` (running; `operator.*.properties`);
  - `PLAN-79` (rewrites every `pm-operator` test);
  - `PLAN-122` (Status line of `17-projects.adoc`).

  Whichever starts second rebases.
- Adjacent to: `PLAN-22` (shipped; the onboarding dialogue)

## Hand-Off Command

Lane: `runbook`, a small plan.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-121-cli-project-add-and-confirm.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under
`.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The
session reports its outcome through its PR and its inbox message. The qualifiers of the inbox exception, and the sole
sanctioned write mechanism, are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger
Write-Boundary.
