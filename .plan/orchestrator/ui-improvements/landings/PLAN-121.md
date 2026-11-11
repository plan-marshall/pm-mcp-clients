# Landing Analysis: PLAN-121 — Terminal: `project add` Onboards, Release and Unenrol Confirm

epic: ui-improvements
workstream: WS-04
pr: #390 (https://github.com/plan-marshall/pm-mcp-clients/pull/390), merged as `06d155c3` (squash through the merge queue, 2026-11-09; parent `0cc6a66d`)

> Runbook lane (harness antigravity, model unknown, executor 0.1.2090). Inbox message
> `rb-121-cli-project-add-and-confirm-001.md`. Checked against git, the PR (`ci pr comments`), SonarCloud and a read-only
> leaf; the key points re-read by the orchestrator at `06d155c3`. Analysed 2026-11-10.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 `project add` runs the project onboarding (no direct `POST /enrol`) | met, with defects | `ProjectCommand.java:198-231` posts `/api/v1/project-onboardings` and drives `StateDialogue`; direct enrol removed (`ProjectCommandTest` NeverSendsDirectEnrol); transcript `ProjectSetupDialogueIT:96-108` |
| D1 `--no-input` without an answer exits 3 and names it | partly | only the missing `--pilot` flag exits 3 (`:198-202`); at any later choice state `ChoicePrompt` throws `StartPrompt.MissingInputException` (a `RuntimeException`, `dialogue/ChoicePrompt.java:280`) which `AddCommand` does not catch → `PmOperator` handler (`PmOperator.java:176-189`) prints a raw message and exits **1** (L3-1) |
| D1 `--pilot` answers `project.identity` | partly | implemented as "press Enter on the identity frame's preselection" (`:215-217`) plus an undocumented `pilot` field in the start body (`:204`) |
| D1 every argument answer → runs to enrolment; unasked argument reported | not met as written | the verb has only `<path>` and `--pilot`; later states still ask; the "argument no state asked for" report (`01-commands.adoc:172`) is not implemented |
| D1 open item `project-add-onboarding` closed, line and section agree | met | `17-projects.adoc:36`, `01-commands.adoc:154` |
| D2 release/unenrol confirm without `--yes`; declining sends nothing; `--yes` sends | met, with a design gap | `:451-455`, `:542-550`, `confirm()` `:606-620`; tests `:736`, `:801` |
| D2 release asks the reason when missing | met | `:484-492`; but asked BEFORE the confirmation, design puts it after the block |
| D2 open item `release-confirmation-prompt` closed | met | `17-projects.adoc:38,297,354` |

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`, `orchestrator.analyze`).
- **Verdict:** layer 1 findings — 3 (1 major); layer 2 findings — 4 major/minor groups; layer 3 findings — 2 major, 5 minor.
- **Re-read by the orchestrator at `06d155c3`:** `ProjectCommand.java:196-232` (add path, catches), `:262-300`
  (`resolveSource`, basename guess), `:484-498`, `:606-620` (release prompt, `confirm`); `PmOperator.java:174-190`;
  `dialogue/ChoicePrompt.java:276-280`, `StartPrompt.java:254` (`MissingInputException extends RuntimeException`). All confirmed.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 (major) | The spec's hypothesis covered only the endpoint, not the contract: the start answers an ENTRY (`/api/v1/entries/{id}`), the run has no project id until `project.enrol`, the body carries the checkout path plus the terminal client's build environment | checked | refutes | pm-mcp-core `credentials-and-enrolment/02-enrolment-and-project-lifecycle.adoc:51`; docs `cli-and-installer.adoc:132`; runtime-model `01:271` |
| L1-2 | "every answer given → runs to the enrolment" is not reachable with `<path>`/`--pilot` | checked | refutes | spec D1 Done-when; `17-projects.adoc:130-160` |
| L1-3 | `17-projects.adoc:36` ("answers of `project.identity` to `project.enrol`") vs Javadoc "checkout and identity" | checked | refutes | `ProjectCommand.java:162-163` |

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained / unborne | Verdict |
|-------------|-----------|--------------------|-----------------------|---------|
| D1 | `:198-231`, `resolveSource :266-300` | `ProjectCommandTest:364` (fails on revert; "asks" unproven), `:402-426` (missing-pilot only), `:432` (no no-prompt assertion), IT | later-state `--no-input`; unasked-argument report; source selection mock-shaped (mock given `project_id` in `MockRuntimeEngine`, outside the Expected Surface) | findings |
| D2 | `:445-457`, `:484-498`, `:536-551` | `:736`, `:759`, `:801`; `:771` passes on the parent (`0cc6a66d` already had `!yes && noInput → 3`) — its "test-first, failing run `c4d139de`" claim cannot be true | the "What ends / What stays / Afterwards" block of `17-projects.adoc` § Release not shown although its keys exist (`projects.en.properties:43-62`) | findings |

**Layer 2 verdict:** findings — D1 partly, D2 met with the confirmation block missing; one false test-first claim.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `ProjectCommand.java:221-231`; `PmOperator.java:176-189` | `project add --no-input --pilot` against a real onboarding with any choice state after identity | `MissingInputException` escapes → raw `project.<state> needs <label>`, exit 1 not 3, not from the catalogue; the started entry is left open (other dialogue verbs catch it: `HoldDialogue:88`, `StartDialogue:122`) | major |
| L3-2 | `ProjectCommand.java:204`, `:266-300` | `project add` (no argument), or a 201 whose body names only the entry | start body `{path: ".", pilot}` — no build environment, `pilot` not a contract field, `.` unresolved for another process; `resolveSource` prefers a `project_id` the contract does not return and falls back to a project id GUESSED from the path basename; a malformed 201 is swallowed into that guess | major (daemon behaviour not shown by reading) |
| L3-3 | `:484-498`, `:606-620` | `project release` / `unenrol` without `--yes` | one bare line "Release the project? y) … n) keep it ? ›": nothing of what ends is named (design § Release); `?` offered but declines; Enter/typo/EOF decline silently with exit 0 instead of re-asking "Type y or n."; `"de".equals(locale)` misses `de_DE`/`de-AT` | major (design) / minor |
| L3-4 | `:225-228`; `ProjectSetupDialogueIT:96-108` | `q` or Ctrl+D in `project add` | exit 0, table says 3 (`01-commands.adoc:199-200`); the IT pins 0 (pre-existing in `SetupCommand:353`) | minor |
| L3-5 | `:490`, `:457` | Enter at the reason prompt | untranslated literal "Released by operator" recorded as the operator's reason | minor |
| L3-6 | pm-api `EnrolRequest`, `EnrolResult`, `enrolment.enrol.*` texts | — | now dead in production; not removed (pre-1.0, no shims) | minor |
| L3-7 | `EnrolCommandTest:146-165` | — | `enrolStripsControlSequences` is vacuous (path no longer printed) | minor |

**Layer 3 verdict:** findings — 3 major, 4 minor.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md § Code Standards | Generators for input data | deviates | new tests use literals (`cui-parent`, `/git/cui-parent`, `my-reason`) where the removed tests used Generators; `EnrolCommandTest` lacks `@EnableGeneratorController` |
| CLAUDE.md § Code Standards | `@ParameterizedTest` for 3+ variants | deviates | four start-response variants as four near-identical tests |
| CLAUDE.md § Code Standards | no generic Exception; no System.out | conforms | `ProjectCommand` |
| CLAUDE.md | every text en and de | conforms | `operator.project.add.pilot_required` |
| Spec Expected Surface | — | deviates (justified in part) | `MockRuntimeEngine` (+`project_id`, +`continue_link`), `EnrolCommandTest`, `01-commands.adoc` |

## Metrics and Anomalies

- Tokens/model/effort: `unknown` (antigravity reports none) → landing-check `complete: false`; recurrence of the incomplete-facts Open Defect.
- Anomalies: none reported.

## Routing and Merge Behavior

- **Review (re-read from `ci pr comments`):** CodeRabbit 1 review body (8 actionable, no nitpick/outside-diff), 8 threads,
  0 unresolved, all replied. Re-triggers refused on quota. **Merge rule met.** The lane's counts hold.
- **CI/merge:** green; merge queue under standing authorization.
- **Sonar:** PR #390 0 issues, gate OK, new coverage 83.0%.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 390; `landing`; `plan_marshall_plan_id` `rb-121-cli-project-add-and-confirm`
- [x] epic.md Watch for PLAN-121; Open Defect (incomplete facts) recurrence
- [x] resume anchor updated; `queue-view.md` regenerated

## Follow-Ups

- Residue plan (operator's word): L3-1 (catch `MissingInputException`, exit 3 from the catalogue, close the entry), L3-2
  (start body and source per the contract; drop the basename guess and the mock's `project_id`), L3-3 (the § Release
  block, re-ask on anything but y/n, locale prefix), L3-4/L3-5, dead pm-api enrol types, the weak tests and the
  Generators rule. L1-1 needs the daemon contract read first (body with the build environment; entry addressing).
