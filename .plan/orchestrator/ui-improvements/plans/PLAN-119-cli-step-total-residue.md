# PLAN-119: Terminal: One Rule for the Step Total (PLAN-116 Residue)

epic: ui-improvements
workstream: WS-04

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane (small plan, operator rule
> 2026-11-07). Staged 2026-11-08 on the operator's word from the layer-3 findings of `landings/PLAN-116.md`.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

PLAN-116 (#368, `6c105a86`) made the onboarding dialogue take its step total from the runtime's `total_steps`, but left the
rule inconsistent. A project-setup payload without a usable total now falls back to 0, so the dialogue drops its `[n/m]` step
line, where it used to show `[n/10]`. The being-added badge (`BeingAddedProject`) still assumes a fixed 10. The step overview
still lists every `OnboardingAspect`, whatever list the source was given. Fractional and over-range values are accepted.
Settle one rule for the step total, from the runtime contract, and apply it wherever a total is read: badge, project
listing, machine and project dialogue. Pin the rule with tests that fail on a revert.

## Deliverables

1. **One rule for a missing or invalid total**
 - What: decide, from the client contract for the being-added and onboarding records (read the record classes in `pm-api`
   and the runtime contract they cite; do not guess), what a payload without a usable `total_steps` shows. The choice is
   either a stated fallback or the bare title without `[n/m]`. Apply that one rule in `ProjectOnboardingSource.java:80,94`
   (now fallback `0`), `OnboardingSource`, `StateDialogue.fromJson` (`:705-711`) and `pm-api`
   `BeingAddedProject.java:49-51,121` (now a fixed `10`, read by the badge at `EntryFlow.java:674` and the listing at
   `ProjectsCommand.java:226`). A usable total is a positive whole number within `int` range. `3.9` and `4294967297` are not
   usable; today they read as 3 and 1 (`JsonTree.integer` truncates and wraps). The badge and the dialogue of one run show
   the same total.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Tests at both reading sites (the dialogue's `fromJson` and `BeingAddedProject`) cover a missing, zero, negative,
     fractional, over-range and non-numeric `total_steps`, plus one valid total other than 10, and each test fails on a
     revert of its site (evidence order: `test-first`)
   - A project payload without `total_steps` renders whatever the rule decides, and a test asserts the rendered step line
     (evidence order: `test-first`)
2. **The overview follows the injected aspect list**
 - What: `StateDialogue.java:411` (`addStepOverview`) and `:648` (`machineStepOf`) iterate `OnboardingAspect.values()`.
   The 2-arg `OnboardingSource` constructor (`:47`) and `EntryFlow.java:78` default to `List.of(OnboardingAspect.values())`.
   A dialogue built over an aspect list of another size says N steps in its intro but lists every aspect in its overview.
   Make the overview and the machine-step mapping use the list the dialogue was given. Keep the all-aspects default only
   where no list is supplied. Make `public OnboardingSource(RuntimeClient, OperatorGate, int)` package-private or private:
   it has no outside caller.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - A dialogue-level test over a two-aspect list shows "2" in the intro and exactly those two aspects in the overview. It
     fails if `:411` or `:648` goes back to `values()` (evidence order: `test-first`)
   - `git grep -n "OnboardingAspect.values()" -- pm-operator/src/main` lists only the documented no-list defaults (evidence
     order: `none`)
   - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open
     (`unknown` for a failed read, never `0`). It is a confirmed `0`, or every open issue is dismissed (evidence order:
     `none`)

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`). Beside the session's own checks, `./mvnw -pl pm-api,pm-operator verify`
  passes at the head, and so does `./mvnw -pl pm-api,pm-operator verify -Ppre-commit`, with every rewrite it makes committed.
  PLAN-116's D3 formatting of `ConsoleBadge.java:28-33` and `PmConsole.java:238` did not land. If the gate rewrites those
  lines, commit the rewrite and say so.
- No new dependency or plugin. Test input data comes from `Generators` with `@EnableGeneratorController`; expected
  catalogue text stays literal.
- Never catch or throw generic `Exception`/`RuntimeException` in production code; package-private over public.
- Terminal check of the rendered step line through the screen capture harness of `cli-pty-dialogue-harness`
  (`ScreenCaptureIT`, command line in `doc/design/terminal-client/27-tui-testing.adoc`) for a being-added project with and
  without `total_steps`.
- Ports of this plan: mock web port `21190`, app port `21191` (`AGENTS.md` § Ports formula `20000 + 10 × 119`). Other plans
  run at the same time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The pull request is merged only on the operator's word. Every review body is answered where the reviewer reads it,
  nitpicks included. Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no
  generator footer.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`). An open Sonar issue is
  fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` and transmitted through
  `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`.
  The merge gate reads the open-issue count at the head: `0` confirmed, or every remaining issue dismissed.

## Claim Labels

- OBSERVED: `ProjectOnboardingSource.java:80,94` call `StateDialogue.fromJson(body, 0, true)`. The parent of `6c105a86`
  passed `TOTAL_STEPS = 10` (`:37,81,95`). `StateDialogue.stepLine` (`:275-277`) returns the bare title when the total is
  below 1. Read at `6c105a86` by the orchestrator on 2026-11-08.
- OBSERVED: `BeingAddedProject.java:49-51` replaces a total ≤ 0 with 10, and `:121` reads `total_steps` (then `total`)
  with default 10. Read at `6c105a86`.
- OBSERVED: `OnboardingAspect.values()` is iterated at `StateDialogue.java:411,648` and is the default list at
  `OnboardingSource.java:47` and `EntryFlow.java:78`. Read at `6c105a86`.
- HYPOTHESIS: `JsonTree.integer` truncates a fraction and wraps an over-range long through `intValue`, so `3.9` reads as 3
  and `4294967297` reads as 1. From a read-only agent's reading of `JsonTree.java:313-340`, not run. Confirm or refute at
  `JsonTree.integer`/`number` (verify-at-outline).
- HYPOTHESIS: the runtime contract states whether `total_steps` is always present on a being-added or onboarding record.
  Confirm or refute at the `pm-api` record classes and the contract they cite (verify-at-outline). The answer decides
  deliverable 1's rule.
- Verify-first clause: re-read the named lines at the current HEAD before editing. PLAN-21 and PLAN-34 (running, if
  launched) touch the terminal's catalogue and transcript tests, not these files.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/dialogue/ProjectOnboardingSource.java` — present at `6c105a86`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/dialogue/OnboardingSource.java` — present at `6c105a86`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/dialogue/StateDialogue.java` — present at `6c105a86`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EntryFlow.java` — present at `6c105a86`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/project/BeingAddedProject.java` — present at `6c105a86`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/project/ProjectRecordsTest.java` — present at `6c105a86`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/DialogueSourceTest.java` — present at `6c105a86`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/dialogue/StateDialogueTest.java` — present at `6c105a86`

## Dependencies and Sequencing

- Depends on: `PLAN-116` (shipped, #368)
- Overlaps with: `PLAN-79` (rewrites every `pm-operator` test, including `DialogueSourceTest` and `StateDialogueTest`; whichever starts second rebases), `PLAN-118` (`EntryFlow.java` is not in its surface; check at outline)
- Adjacent to: `PLAN-84` (console find; `04-console.adoc` only)

## Hand-Off Command

Lane: `runbook` — a small plan.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-119-cli-step-total-residue.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under
`.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The
session reports its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned
write mechanism are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
