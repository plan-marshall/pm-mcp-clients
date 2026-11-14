# PLAN-160: Terminal: `config` Made to Do What the Design Says (PLAN-32 Residue)

epic: ui-improvements
workstream: WS-04

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's decision of 2026-11-12
> from `landings/PLAN-32.md` (PR #415, squash `948c51a1`). The cleanup row and durations went to `PLAN-128`; the texts and the error exit went to
> `PLAN-30`. This spec is SELF-SUFFICIENT.

## Objective

PLAN-32 moved `config` onto the agreed transport (`GET /api/v1/config`, `PUT|DELETE /api/v1/config/{level}/{key}`, the aspect verbs through
the onboarding sub-workflow). The move left behaviour the design does not have:

- a change is sent without the revision it was made against, and a `409 conflict` prints an HTTP status;
- `credentials rotate` no longer checks the providers;
- the overview shows wire tokens and one row per consent, and has no `Paired devices` row;
- the mock answers a plan-only read without the project's values, marks a provider verified because the request asked for it, and cannot
  reach `config <aspect>` on `fully-onboarded`.

Make each match the design, with tests that can fail, and answer the review items #415 left without a reply.

## Deliverables

The design is the specification. Read first: `doc/design/terminal-client/08-transcripts-later.adoc` (the overview `:160-182`, the
documents and "Changed meanwhile" `:236-241`, the credential rotation `:255-276`), `doc/design/terminal-client/01-commands.adoc` (the `config`
rows), `doc/design/terminal-client/02-levels.adoc` (`:86`, `:108`: the nearest level wins), `doc/design/shared/work-actions.adoc#conflict`, and
the transport row `runtime-model/01-processes-and-transport.adoc:202` in `../plan-marshall-documentation`. Lines are as read at `948c51a1`.

1. **A change carries its revision, and `conflict` is answered as the design says**
 - What:
   - `config set` and `config unset` (`ConfigCommand.java:646-659`, `:713-723`) send no revision and treat every non-2xx as
     `operator.config.set_failed` with the HTTP status. The transport row says each change carries the revision it was made against.
     `08-transcripts-later.adoc:241` says every setting of `config` answers a `conflict` with `Changed meanwhile: it is now <current>. Yours was not
     applied.` and asks `Apply yours again? [Enter]   k) keep the current value`. Send the revision read with the value. On `409 conflict`, print
     the line with the current value from the answer and ask. `--no-input` exits 3 on a conflict.
   - Verify first: read where the revision travels (body member or header) in the specification row and `ConfigValueResource`. If the
     specification names no form, stop and ask.
   - The mock (`MockConfigRoutes.java:140-160`) keeps a revision per value, refuses a stale one with `409 conflict` and the current value, and
     answers a DELETE of an aspect-owned key with `409 aspect_owned` (today 404), as the PUT does.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `ConfigCommandTest`: a set and an unset send the revision; a `409 conflict` prints the "Changed meanwhile" line with the current value and
     no HTTP status; `k` keeps it; Enter sends again with the new revision; `--no-input` exits 3. `MockConfigRoutesTest`: a stale revision
     answers 409 with the current value; a DELETE of an aspect-owned key answers `409 aspect_owned`. Each fails on a revert. Evidence order:
     `test-first`.
2. **`credentials rotate` checks the providers again**
 - What:
   - `CredentialsCommand.java:215-223` prints only a hint to run `config providers --verify`. `08-transcripts-later.adoc:262-273` runs the check
     on the providers that use the key after the store (`providers.verify-consent`, `providers.verify`). It prints each provider's lines, and
     the last line names the held plans that go on (`✓ github verified. 1 plan that waited for it goes on: fix-relay`). On `providers.failed`
     the hold stays. Run the check through the same onboarding sub-workflow the aspect verbs use (`ConfigAspectRun`, `AspectRunSource`). Do not
     add a new route.
   - Verify first: the sub-workflow offers the verify after `settings.store-credential` in the onboarding state catalogue and in the mock's
     `onboarding.json`. If it does not, stop and ask.
   - Remove the key `operator.credentials.verify_with` if nothing else uses it.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `CredentialsCommandTest`: a rotate runs the check and prints the verified line with the plans that go on; a failed check prints the
     failure and keeps the hold. Each fails on a revert. Evidence order: `test-first`.
   - Terminal check of `credentials rotate github` on a scenario with a held plan, en and de. Evidence order: `none`.
3. **The overview as the design draws it**
 - What:
   - `ConfigCommand.java:225-232` prints wire tokens as values: documents `ask`, consent kind `scan_harnesses`. It prints one row per consent
     with label and value swapped. The design (`08-transcripts-later.adoc:172-175`) has one `Remembered consents   6` row (opening `config
     consents`), `Documents   ask each time` (the catalogue word of the mode) and `Paired devices   3 devices · network listener off`
     (opening the devices view, `<<t-devices>>`). Build the rows from the catalogue; no wire token on screen. Add the devices row from the
     read the web devices view already uses; if that read is not reachable from `config`, stop and ask.
   - The Cleanup row (the last row) is `PLAN-128`'s (it runs `config cleanup`); this plan keeps the row and its numbering. Only the rows above
     it change.
   - The prompt line (`:279-282`, `:503`): the design's input sits on the prompt line after `›` (`:176`). Read the input on that line.
   - Runtime-served strings printed raw: a config `key`, a credential `key`, `provider` and `kind` (`:221`, `:255`, `:262`, `:408`, `:413`)
     go through `TerminalSanitizer` as the values do.
   - Tests to remove or replace (rule 2): the assertions of the wire values in `ConfigCommandTest.java:466-469`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `ConfigCommandTest`: the consents are one row with their count; documents shows the catalogue word; the devices row is present; a
     control sequence in a key is stripped; the input is read on the prompt line. Each fails on a revert. Evidence order: `test-first`.
   - Terminal check of the overview on `fully-onboarded` at 120 × 30 and 80 × 24, en and de, compared row by row with
     `08-transcripts-later.adoc:160-176` (rule 3: by the rendered capture, not by reading the source). Evidence order: `none`.
4. **What a key belongs to**
 - What:
   - `AspectOwnedKeys.java:28` owns the whole `credentials.` family; the specification (`01-provider-credentials.adoc:140`) and
     `01-commands.adoc:148` own only `credentials.store`. `ConfigCommand.getOwningAspect` (`:526-537`) repeats the table, and its
     `startsWith(ROLES)` has no dot. Keep one table (`AspectOwnedKeys`), owning exactly what the specification says, and make the command read
     it. `AspectOwnedKeys.normalise()` (`:74`) has no caller: use it or remove it.
   - `AspectOwnedKeys` has no test: add `AspectOwnedKeysTest` in `pm-api` (each owned key, a key of the family the aspect does not own, a
     prefix without its dot).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `AspectOwnedKeysTest` and a `ConfigCommandTest` case `config set credentials.<other>` (accepted) and `config set roleset` (not
     refused as `roles`) fail on a revert. Evidence order: `test-first`.
5. **The mock serves what the runtime would**
 - What:
   - Plan-only read: `MockConfigRoutes.levelScope` (`:216-222`) gives the project level no scope when `project` is absent. A plan-only read
     therefore misses the plan's project values, against its own Javadoc (`:209-215`) and `02-levels.adoc:86,108`. Resolve the plan's project
     from the scenario's plan records and inherit its values. If a scenario has a plan without a project, stop and ask.
   - Providers verify: `MockRuntimeEngine.java:7360`, `:7412` record `github (verified)` from the request flag alone, and
     `ConfigRoutesTest.java:206` pins it (CodeRabbit "Unverified finding", review `5865570352`, unanswered). Record "verified" only from the
     scenario's provider state; a flag without a ready provider records the check's failure.
   - `fully-onboarded` rests at `showroom.active`, so `config <aspect>` reports the machine settings unreachable
     (`AspectRunSource.java:219-233`). Make the fixture offer the `settings` link as the runtime does after `onboard.completed`.
     `PLAN-133` migrates the same file: rebase on whichever lands first.
   - Tests to remove or replace (rule 2): `ConfigRoutesTest.java:206` (the "verified" assertion).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `MockConfigRoutesTest`: a plan-only read shows the project's value with origin `project`. `ConfigRoutesTest`: a verify of a provider
     that is not ready records the failure. A `ConfigSettingsTest` case runs `config language` on `fully-onboarded`. Each fails on a revert.
     Evidence order: `test-first`.
6. **The tests, the commands document, and the review items left open**
 - What:
   - `MockConfigRoutesTest` and `ConfigRoutesTest` (no `@EnableGeneratorController`) take hand-written input (`"machine"`, `"custom.label"`,
     `"60"`, `"p-1"`): draw it from `Generators`; the expected wire stays literal. `ConfigCommandTest.aRefusedSettingsReadLeavesWithOne`
     draws a reason it never asserts: assert it or drop it.
   - `ReadVerbExitCodesTest.java:117` builds an `OperatorContext` with no terminal supplier and runs `config`. It is safe only while the read
     is refused. Give it the `DumbTerminal` supplier the other config tests use.
   - `01-commands.adoc`: `config harnesses --path` (not implemented) and the positional `[<level>]` of `show|set|unset` (implemented as
     `--machine|--project|--plan`). Correct the document to the implementation, or the implementation to the document where the design is
     AGREED; say which in the PR. The document names the revision rule of D1 and the overview rows of D3.
   - #415: the three review-body items without a reply (outside-diff of review `5864209056`, the `HarnessesCommand` `--no-input` guard,
     fixed; "Unverified finding" of `5865570352`, fixed by D5; the nitpick of `5868097420` on `MockConfigRoutesTest:159`, fixed by this
     deliverable) and thread `4543426060` (resolved without a reply). Reply to each with the commit. The reply on thread `4545310611` ("a
     project value is inherited by a plan read") was only true for a read that names both: correct it with D5's commit.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
 - Done when:
   - `git grep -n '@EnableGeneratorController' -- pm-mock-runtime/src/test/java/de/planmarshall/mock/ConfigRoutesTest.java
     pm-mock-runtime/src/test/java/de/planmarshall/mock/http/MockConfigRoutesTest.java` names both classes. Evidence order: `none`.
   - Landing obligation (rule 4, not a deliverable criterion): `ci pr comments --pr-number 415` reads every thread replied to and no
     unanswered review body. Report it in the landing message.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`). Besides the session's own checks, `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify`
  and `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify -Pintegration-tests` pass at the head, and so does
  `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify -Ppre-commit`, with every rewrite committed.
- No new dependency or plugin. Input test data comes from `Generators` with `@EnableGeneratorController`; expected catalogue
  text stays literal. Every new text is in en and de.
- Never catch or throw generic `Exception`/`RuntimeException` in production code; package-private over public; no compatibility shims.
- Terminal check through the screen capture harness (`ScreenCaptureIT`; command line in `doc/design/terminal-client/27-tui-testing.adoc`),
  saved as `pm-operator/src/test/resources/screen-captures/PLAN-160.txt`. Never run `pm-operator` in the agent's own shell. Read every
  capture with the file tools: no raw id, wire token, HTTP status or ISO time on screen.
- Ports of this plan: per `AGENTS.md` § Ports formula (`20000 + 10 × 160`): mock web port 21600, app port 21601. Other plans run at the same
  time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- **The merge rule:**
  - The pull request is merged only after at least one CodeRabbit review of some head with its findings handled. A lane's
    "standing review authorization" does not replace it. If CodeRabbit refuses on quota, stop and ask the operator.
  - The run merges only when every review thread has a reply and is resolved, and every review body item (outside-diff,
    nitpick, "additional comments") has a reply on the PR, not only a fix.
  - The pull request body states the thread count, the resolved count and the review-body items with their count
    (`unresolved_review_threads=`, `review_body_findings_total=`, `review_body_findings_handled=`), read through `ci pr comments`, never
    from memory.
  - The run merges the pull request itself once these gates hold, without a further word of the operator (`CLAUDE.md`, Git Workflow, #423). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`.
    The pull request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project
  `plan-marshall_pm-mcp-clients`), every open issue fixed in a commit of THIS pull request. State `sonar_open_new_issues=N` in the
  landing message. A dismissal needs the operator's word.

## Claim Labels

- OBSERVED, read by the landing analysis at `948c51a1` (2026-11-12) and re-read by the orchestrator:
  - `ConfigCommand.java:646-659` sends `{value}` without a revision and prints `set_failed` with the status for every non-2xx.
  - `ConfigCommand.java:225-232` prints `consent.kind().wireValue()` and `settings.documentsMode().wireValue()` as values.
  - `CredentialsCommand.java:215-223` `verifyProviders` prints `operator.credentials.verify_with` only.
  - `08-transcripts-later.adoc:160-182` (overview), `:241` (every setting answers `conflict`), `:262-273` (rotate checks).
- OBSERVED, from the analysis and not re-read by the orchestrator: `MockConfigRoutes.java:140-160`, `:216-222`; `MockRuntimeEngine.java:7360`,
  `:7412`; `AspectOwnedKeys.java:28,74`; `ConfigCommand.java:526-537`; `ReadVerbExitCodesTest.java:117`; the #415 review ids.
- HYPOTHESIS: the revision of a configuration value travels in the request body, as a decision's does. Confirm or refute at the transport row
  and the CLI specification § Concurrent Changes of a Setting (verify-at-outline).
- HYPOTHESIS: the onboarding sub-workflow offers `providers.verify` after `settings.store-credential` (verify-at-outline).
- HYPOTHESIS: the plans of `fully-onboarded` and `running-work` each carry their project id (verify-at-outline).
- Verify-first clause: re-read each named line at the current HEAD before editing. `PLAN-126` (#416) and `PLAN-133` change
  `MockRuntimeEngine.java`; `PLAN-133` migrates `fully-onboarded.json`.

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConfigCommand.java` — present at `948c51a1`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/CredentialsCommand.java` — present at `948c51a1`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ConfigAspectRun.java` — present at `948c51a1`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/dialogue/AspectRunSource.java` — present at `948c51a1`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/config/AspectOwnedKeys.java` — present at `948c51a1`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/config/AspectOwnedKeysTest.java` — absent at `948c51a1`; created by this plan
- OBSERVED: `pm-api/src/main/resources/messages/operator.en.properties` — present at `948c51a1`
- OBSERVED: `pm-api/src/main/resources/messages/operator.de.properties` — present at `948c51a1`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/http/MockConfigRoutes.java` — present at `948c51a1`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at `948c51a1`
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/fully-onboarded.json` — present at `948c51a1`
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/onboarding.json` — present at `948c51a1` (read; changed only if D2's verify needs it)
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/ConfigRoutesTest.java` — present at `948c51a1`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/http/MockConfigRoutesTest.java` — present at `948c51a1`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ConfigCommandTest.java` — present at `948c51a1`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ConfigSettingsTest.java` — present at `948c51a1`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/CredentialsCommandTest.java` — present at `948c51a1`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ReadVerbExitCodesTest.java` — present at `948c51a1`
- OBSERVED: `doc/design/terminal-client/01-commands.adoc` — present at `948c51a1`
- OBSERVED: `pm-operator/src/test/resources/screen-captures/PLAN-160.txt` — absent; created by this plan

## Dependencies and Sequencing

- Depends on: `PLAN-32` (shipped, #415, `948c51a1`).
- Overlaps with:
  - `PLAN-126` (#416, open): `MockRuntimeEngine.java`; the second to land rebases.
  - `PLAN-133` (running): `MockRuntimeEngine.java` and `fully-onboarded.json`; the second to land rebases.
  - `PLAN-128` (staged, after this plan): the overview's Cleanup row and `config cleanup` in `ConfigCommand.java`.
  - `PLAN-30` (staged, after this plan): the `config` texts and the refused-read exit in `ConfigCommand.java` and `operator.*.properties`.
  - The operator's non-epic plan `analyze-fix-build-time` (pm-operator tests and build configuration).
- Adjacent to: `PLAN-79` (rewrites every `pm-operator` test: last).

## Hand-Off Command

Lane: `runbook`.

/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-160-cli-config-residue.md

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under
`.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The qualifiers of the
inbox exception, and the sole sanctioned write mechanism, are stated in `persona-plan-orchestrator/standards/orchestration-model.md`
§ Ledger Write-Boundary.

## PLAN-134 Landing Residue (Amendment 2026-11-14)

PLAN-134 (#428, `7adb56c2`) moved `MockRuntimeEngine.java` line refs:
- refs past line ~5000 shift by about +430 (5386→5813, 5407→5834, 5459→5886);
- the ref at 491 shifts by +31 (→522).

`ChatRegistry.java` is now 2428 lines, with route parsing at :2054-2145. Re-ground every engine and registry line
ref in this spec at restart.
