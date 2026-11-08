# PLAN-120: Terminal: An Unknown Running Count Is Not Zero (PLAN-117 Residue)

epic: ui-improvements
workstream: WS-04

> Staged plan spec. One shippable unit of work, ready for hand-off on the `runbook` lane (a small plan, per the operator rule
> of 2026-11-07). Staged 2026-11-08 on the operator's word, from the layer-3 finding L3-1 of `landings/PLAN-117.md`.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

PLAN-106 and PLAN-117 settled, for both clients, what happens when the epic resource gives no running count. The shared
design says so in two places:

- `doc/design/shared/epic-view.adoc:82`: `? of N at once` / `? von N gleichzeitig`.
- `doc/design/shared/work-actions.adoc:222`: the client asks before *Next plan* and prints `control.epic.scope_unknown`,
  "so no screen reads an absent count as none".

The web app does this. The terminal client does not:

- `pm-api` `EpicResource` holds `int running` (`:59`) and reads an absent or `null` field as `0` (`:164`, `:188-191`).
- The epic line therefore shows `0 of N at once` (`EpicViewFormatting.java:86-89`, called at `EpicCommand.java:155` and
  `epicview/EpicViewLevel.java:629`).
- The wait texts use 0 (`EpicCommand.java:191`, `EpicViewLevel.java:450`).
- *Next plan* (`WorkActionDialogue.java:293`) never asks: it posts `raise_scope: false` and then reports `{running}+1 of
  {scope}` (`:322`).

The specified epic resource carries no running count at all (Open Defect of 2026-11-08 in `epic.md`), so this is the
normal case today, not an edge case. Make the terminal treat an absent count as unknown everywhere it reads one, as the
shared design says, and pin that with tests.

## Deliverables

1. **The client record and the terminal read an unknown count as unknown**
 - What:
   - `EpicResource` keeps an absent or `null` `running` distinct from 0, as a nullable `Integer` or an `OptionalInt`.
     Settle which from the record conventions in `pm-api` `control/`. A negative or non-integer value is also unknown;
     read `ControlJson.optInteger` first and do not guess.
   - Every terminal reader follows the shared design:
     - the epic line shows `? of N at once` / `? von N gleichzeitig` (`overview.epic.at_once` with `?` as the running
       parameter, as `EpicView.svelte:583` does);
     - the wait texts never claim a free place they cannot know;
     - *Next plan* asks before it starts, with `control.epic.scope_unknown` in place of `control.epic.scope_full`, and
       sends `raise_scope` only on the operator's answer;
     - the "started" line does not print `{running}+1` from an unknown count.
   - Update the terminal design in the same pull request: `19-actions.adoc` (the epic line and the *Next plan* step) and
     `22-texts-actions.adoc:117-118` (the started text, and any new text in en and de).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing,
   pm-documents:ref-asciidoc
 - Done when:
   - `ControlResourcesTest` covers each of: `running` absent, `null`, negative, fractional and a valid value. Each test
     fails on a revert of the record's read. Evidence order: `test-first`.
   - `EpicCommandTest` and `EpicViewLevelTest` assert the rendered `? of 3 at once` line for an absent count and
     `2 of 3 at once` for a known one. `ControlDialoguesTest` asserts that *Next plan* over an absent count prints the
     `control.epic.scope_unknown` text and sends nothing until answered. Each fails if its call site goes back to 0.
     Evidence order: `test-first`.
   - `git grep -n "running()" -- pm-operator/src/main` lists no call site that formats or compares the count without
     handling the unknown case. Evidence order: `none`.
2. **The German and English texts of the unknown case are pinned**
 - What:
   - In `pm-api`, `EpicViewCatalogueTest` formats `overview.epic.at_once` with `?` in en and de (`? of 3 at once`,
     `? von 3 gleichzeitig`).
   - `ControlCatalogueTest` asserts the full en and de text of `control.epic.scope_unknown`, word for word against
     `doc/design/shared/work-actions-texts.adoc:193-195`. Today no test names this key (PLAN-117's answer to the #370
     nitpick said otherwise; see `landings/PLAN-117.md`).
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Both catalogue tests fail when the de or en value of either key is changed. Evidence order: `mutation-after`.
   - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open
     (`unknown` for a failed read, never `0`). It is a confirmed `0`, or every open issue is dismissed. Evidence order:
     `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`). Besides the session's own checks, `./mvnw -pl pm-api,pm-operator
  verify` passes at the head, and so does `./mvnw -pl pm-api,pm-operator verify -Ppre-commit`, with every rewrite it
  makes committed.
- No new dependency or plugin. Test input data comes from `Generators`, with `@EnableGeneratorController`. Expected
  catalogue text stays literal.
- Never catch or throw a generic `Exception`/`RuntimeException` in production code. Prefer package-private to public.
  `@Nullable` from JSpecify where a value may be absent.
- Pre-1.0: change the record's accessor outright, with no compatibility shim. Fix every caller in this pull request.
  `OperatorActions`, `WorkActionsClient` and `OverviewFullScreenApp` also read `EpicResource`: re-check them at outline.
- The web app is out of scope: it already shows `?` (PLAN-106, PLAN-117).
- Check the rendered epic line in the terminal through the screen capture harness (`ScreenCaptureIT`, command line in
  `doc/design/terminal-client/27-tui-testing.adoc`), for an epic payload with and without `running`.
- Ports of this plan: mock web port `21200`, app port `21201` (`AGENTS.md` § Ports formula `20000 + 10 × 120`). Other
  plans run at the same time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited. Work only in the session's own worktree and branch.
- The pull request is merged only on the operator's word. Every review body is answered where the reviewer reads it,
  nitpicks included, and the pull request body states the threads and the review-body items with their count. Commits
  end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`). An open Sonar issue
  is either fixed in a pushed commit or dismissed:
  - A dismissal carries a stated reason, recorded as `{issue-key}: {reason}`.
  - It is transmitted through `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key}
    --transition {falsepositive|wontfix}`.
  - The merge gate reads the open-issue count at the head: `0` confirmed, or every remaining issue dismissed.

## Claim Labels

- OBSERVED: `EpicResource.java:59` declares `int running`; `:164` reads it with `integer(map, "running")`; `:188-191`
  returns 0 when `ControlJson.optInteger` gives null. Read at `bf24091c` by the orchestrator on 2026-11-08.
- OBSERVED: `WorkActionDialogue.java:293` asks only when `epic.running() >= epic.scope()`, and `:322` formats
  `epic.running() + 1`. `EpicCommand.java:155,191` and `EpicViewLevel.java:450,629` format `epic.running()`. Read at
  `bf24091c`.
- OBSERVED: `git grep scope_unknown -- '*/src/test/*'` matches nothing at `bf24091c`. `EpicViewCatalogueTest:134-136`
  formats `overview.epic.at_once` with numbers only.
- OBSERVED: the API specification's epic resource (`plan-marshall-documentation`
  `doc/specification/runtime-model/01-processes-and-transport.adoc:346`) lists no running count. Read on 2026-11-08.
- HYPOTHESIS: `ControlJson.optInteger` returns null for JSON `null` and for a missing key. Not read. Confirm or refute at
  `ControlJson` (verify-at-outline).
- Verify-first clause: re-read the named lines at the current HEAD before editing. PLAN-34 (the display layer) rewrites
  `pm-operator/.../epicview/`: this plan starts after PLAN-34 has landed and re-grounds every line number against it.

## Expected Surface

- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/EpicResource.java` — present at `bf24091c`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/control/ControlResourcesTest.java` — present at `bf24091c`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/EpicViewCatalogueTest.java` — present at `bf24091c`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/ControlCatalogueTest.java` — present at `bf24091c`
- OBSERVED: `pm-api/src/main/resources/messages/control.en.properties` — present at `bf24091c` (only if a new started text is needed)
- OBSERVED: `pm-api/src/main/resources/messages/control.de.properties` — present at `bf24091c` (only if a new started text is needed)
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EpicCommand.java` — present at `bf24091c`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/WorkActionDialogue.java` — present at `bf24091c`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/epicview/EpicViewFormatting.java` — present at `bf24091c`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/epicview/EpicViewLevel.java` — present at `bf24091c`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ControlDialoguesTest.java` — present at `bf24091c`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/EpicCommandTest.java` — present at `bf24091c`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/epicview/EpicViewLevelTest.java` — present at `bf24091c`
- OBSERVED: `doc/design/terminal-client/19-actions.adoc` — present at `bf24091c`
- OBSERVED: `doc/design/terminal-client/22-texts-actions.adoc` — present at `bf24091c`

## Dependencies and Sequencing

- Depends on: `PLAN-117` (shipped, #370), `PLAN-34` (running; rewrites `epicview/`, so this plan starts after it lands)
- Overlaps with:
  - `PLAN-34` (`epicview/EpicViewFormatting.java`, `EpicViewLevel.java`): sequenced after it.
  - `PLAN-44` and `PLAN-50` (staged; `epicview/`): whichever starts second rebases.
  - `PLAN-79` (rewrites every `pm-operator` test).
- Adjacent to: the Open Defect "the epic resource carries no running count" (contract owner `plan-marshall-documentation`
  / `pm-mcpd`). If the contract gains the count, this plan's unknown branch stays as the fallback.

## Hand-Off Command

Lane: `runbook`, a small plan.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-120-cli-unknown-running-count.md
```

## Write-Boundary

The session implementing this spec touches only its own repository's source and tests. It creates and edits NO file
under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger
write. The session reports its outcome through its PR and its inbox message. The qualifiers of the inbox exception, and
the only sanctioned way to write, are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger
Write-Boundary.
