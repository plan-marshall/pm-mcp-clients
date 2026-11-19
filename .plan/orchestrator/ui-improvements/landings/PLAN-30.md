# Landing Analysis: PLAN-30 — Terminal texts, one error pattern, exit codes and dialogue residue

epic: ui-improvements
workstream: WS-05
pr: #438 (merged as 2efcaa40, 2026-11-19)

> Landing record for one shipped plan, the resumed run of the first run of 2026-11-14. Written by `analyze` (inbox scan, message `rb-30-cli-texts-and-errors-001`, `landing-check` complete: false, missing `total_tokens` and `plugin_version`, both `unknown`) after verifying claims against ground truth. The lane's report grades itself: D1, D3, D9 met; D2, D4, D5, D6, D7, D8 "not met as written" or "not evidenced"; `deliverables_done=3` of 9.

## Deliverable Fidelity vs Spec

90 files, +4765/-707. The lane's own grades are accurate and, for two deliverables, understated; one grade is inconsistent with its table (L2-3).

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 strings | shipped-as-specified (guard blind outside its list) | `LiteralTextGuardTest.java:66-75` lists the three commands and seven sub-package files, `:197` walks the tree (re-read); blind to ternaries, `String.format`, and files outside the list (PlanCommand, EpicCommand, StatusCommand, RolesCommand, DecomposeCommand, ConfigCleanupCommand, DrainCommand) |
| D2 renderer | shipped-modified | `RefusalRendererTest.java:32-153` fails if the renderer is reverted; both D2 table rows are graded met by the lane, yet the lane's heading lists D2 as not met (L2-3); the renderer has three major defects (L3-2, L3-3, L3-4) |
| D3 picocli | shipped-modified | `PicocliLanguageTest.java:73-134`: `bogus` and top-level `-h` only; about 370 option and subcommand descriptions stay English (L3-12) |
| D4 exit codes | shipped-modified, **not met as written, understated** | `ExitCodesTest.java:109-115` usage for every verb, `:122-149` 24 hand-listed refusals asserting the exit code only, `:179-201` success is ONE verb (`status`) plus `-h`/`-V` (the lane says five), 130 not covered (re-read) |
| D5 display residue | **not met as written, confirmed** | `PlanCommand.java:619,623` `get(key)` with no locale (the lane says 620/624), more no-locale callers (L2-2), English `plan report` literals at about 1044-1305; `PlanCommand` is not in the guard list |
| D6 config residue | not evidenced (confirmed: tests exist, no failing-run record) | `ConfigCommandTest`, `CleanupCommandTest`; `ConfigCommand.java:1291` still prints "HTTP {status}" (L3-13) |
| D7 PLAN-160 residue | **not met as written, confirmed** | no test asserts an unready provider is not recorded (`OnboardingScenarioTest.java:654` tests the positive case only); the #415 thread replies are not on record |
| D8 PLAN-128 residue | partly met, **one grade corrected** | `--project .` IS tested and fixed (`CleanupCommandTest`: the dot behaves as the unfiltered view, and the 25-item `--project` test), contrary to the lane's "neither tested nor fixed"; "remind offers no never" still has no test and the refusal text is wrong (L3-8); the capture set holds no choice bar or delete confirmation |
| D9 PLAN-127 residue | shipped-modified | the tests fail on a revert except the credential-path act body (L2-4) |

## Three-Layer Analysis

Read on three layers by one read-only leaf (level 3, `execution-context-level-3`, 30 findings, 10 conformance rows); the orchestrator re-read the points marked "re-read" at `2efcaa40`. By reading: no build, no test run, no write. **The leaf did not read** the bulk of the `MockRuntimeEngine` diff (+377), the two scenario JSONs (+547, +397), `WebCommand`, `DecomposeCommand`, `OperatorRefusedExceptionTest`, `PlanCommandTest`, `ProjectCommandTest`, `QuestionsCommandTest`, `WebDevicesCommandTest`, `DecomposeCommandTest` and the coverage numbers; those are **not shown by reading**, not clean. The capture sets, CI job logs and thread replies are under `.plan/` and were never opened.

- **Dispatch:** one read-only leaf per landing — reason: 90 files and a lane report that grades itself needed the merged code read against the spec, separately from PLAN-151's read.
- **Verdict:** layer 1 findings (2, one major spec premise); layer 2 findings (4, three major in the tests); layer 3 findings (7 major, 17 minor), reported to the operator; large parts of the diff not shown by reading.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 (major) | D2's "The runtime refused: <message>" over `error.unknown.*` cannot be met from catalogue keys: `error.unknown.*` has no message slot (`error.en.properties:185-187`), so the prefix went into code (`RefusalRenderer.java:84`, re-read) | checked | refutes | spec D2 |
| L1-2 (major) | The lane's claim that the three design documents now match the code is partly refuted: `03-dialogues.adoc:269` promises `audit_id` and the runtime's detail on `internal_fault`, the code reads no `audit_id` (grep of `pm-operator` main empty, re-read); `doc/design/shared/work-actions.adoc:222` (not touched) still says an unknown running count "asks as well", contradicting D9's launch without asking and the spec's own "if the document is silent, stop and ask"; `26-cleanup.adoc`'s text table lists `operator.cleanup.choose.remind` as "ask me again" while the pack holds `{days}` | checked | refutes | the three documents |
| L1-3 (minor) | The D5 clause "`MessageCatalogue.get(` names no call without a locale" no longer discriminates once `get(locale, key)` exists; the argument order is easy to swap and `get(key, locale)` silently returns the key (`MessageCatalogue.java:196`) | checked | refutes | spec D5 |
| L1-4 | Resume Amendment steps 1 to 9 are consistent with D1 to D9; "do not redo D1-D9" means D4 to D8's open Done-whens could not be closed by the resume and are carried as residue; unexplained by any deliverable: `reports-and-system.json` (+397, not in the Expected Surface, plausibly the D8 captures) and the large `MockRuntimeEngine` hunks | checked | confirms | |

**Layer 1 verdict:** findings — 3 (2 major, 1 minor).

### Layer 2 — Implemented

- L2-1 (major, confirmed and understated) D4: `ExitCodesTest.java:179-201` success covers one verb, not five; exit 3 only via `project add --no-input`, exit 4 only via `runtime stop`; the refusal cases assert the exit code on a 500, so the renderer path cannot be told from `PmOperator`'s generic failure handler (`:242`); `RANDOM_ID` (`:54`) is a class-load static.
- L2-2 (major, confirmed with corrections) D5: no-locale `get(key)` at `PlanCommand.java:619,623` (the lane: 620/624) and, on touched or related screens, `EntryFlow.java:435`, `ReportsCommand.java:83`, `LessonSourceCommand.java:113,160`, `EnrolHarnessCommand.java:116-240`; the English report literals are more than the lane's list.
- L2-3 (minor) the lane's heading lists D2 among the "not met" while both D2 table rows say met, so `deliverables_done=3` excludes a deliverable the table grades met; the expected exits of `PLAN-30.txt` are 1/1/2/0 in file order, not "1/2/1/0".
- L2-4 (major) D9 "a credential-path act body not recorded" is partly vacuous: the asserted path `/api/v1/credentials/github` (`MockRuntimeEngineTest.java:2622`, re-read) carries no `decisions` segment and no `/api/v1/questions` prefix, so the old guard `carriesDecisionBody` (defined at `MockRuntimeEngine.java:8329` of the parent commit; its body was not re-read) would also have skipped it; only the chat-route assertion fails on a revert; the new code's credential route with a decision segment (`MockRuntimeEngine:8360-8363`) is untested. The other D9 tests hold (`EpicCommandTest:441-454,486-497`, `ControlDialoguesTest:852-866`, `TerminalSanitizerTest:134-143`); `StatusCommandTest:81-138` asserts "unbekannt" and another unknown word would satisfy it.
- L2-5 (minor) `PicocliLanguageTest` has no `@EnableGeneratorController` and uses the literal `bogus`; the 25-phrase deny-list mirrors strings and cannot see annotation descriptions.

**Layer 2 verdict:** findings — 5 (3 major, 2 minor).

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `RuntimeCommand.java:376,388-392` (362-368) | `runtime upgrade --check` or `--withdraw`, or a release-notes read, when the runtime refuses | prints "Version check failed: HTTP 500" / "Failed to withdraw upgrade: HTTP n" and **exits 0**: breaks the exit-code table (a refusal is 1) and D2's "never an HTTP status", in a file D1 names; `ExitCodesTest` does not cover the verb; the lane does not mention it (re-read) | major |
| L3-2 | `RefusalRenderer.java:81-90`, callers `ConfigAspectRun.java:190`, `AspectRunSource.java:232,238`, `CleanupCommand.java:568,586`, `OperatorGate.java:123` | a local refusal (`OperatorRefusedException(String)`, code null), e.g. "settings unreachable" | rendered "✗ The runtime refused: <local text>", cause "The refusal carries a code this client does not know.", fix "Update the client and try again": a local condition shown as an unknown runtime code with a wrong fix; `RolesCommandTest:488` pins "The runtime refused: internal" (re-read) | major |
| L3-3 | `RefusalRenderer.java:125`, `error.en.properties:123` | any runtime refusal with code `enrolment_tampered` | `render(out, locale, exception)` always passes `Map.of()`, no caller passes arguments, the exception parses no project id: the operator is shown the literal `{project_id}` in the fix line; `RefusalRendererTest:109-117` tests only the arguments overload (re-read) | major |
| L3-4 | `OperatorRefusedException.java:129-138` | a proxy 502 HTML page or a plain-text stack trace on a 500; a 404 without a code | the whole body becomes the message (newlines flattened, no length cap, no exception-class filter): contrary to D2 "never a JSON body or an exception class"; any 404 without a code becomes `scope_not_found` ("This entry does not exist"), also for a missing route on an older runtime (re-read) | major |
| L3-5 | `OperatorRefusedException.java:157-181`, `RefusalRenderer.java:78-80` | `internal_fault` | `audit_id` is never read; `error.internal_fault.cause/fix` tell the operator to "look the audit id up"; for a known code `titleLine` drops the server message; the design line at `03-dialogues.adoc:269` is not what the code does (re-read: no `audit_id` in main) | major |
| L3-6 | `AnswerCommand.java:856`, `:1746` | `dependency_unanswered` for any part but the first | `n` hard-coded to 1: "Answer part 1 first." (re-read); `AnswerRefusalTest` pins the text; `invalid_answer` with no message renders "This answer does not fit the part: " with an empty reason; the interactive retry (810-827) is bounded (Ctrl-C, EOF via `readLineSafe:569-577`); a persistent server `invalid_answer` with scripted non-EOF input loops by design | major |
| L3-7 | `WorkActionDialogue.java:342` | an operator who declines the raise question | "Left epic.raise-scope. Nothing was sent.": the raw internal state id on the screen, against the spec's own screen rule; `EpicCommandTest` (~:493) and `ControlDialoguesTest` (~:866) pin the raw id; the design names no leave line (re-read) | major |
| L3-8 | `ConfigCleanupCommand.java:233` | `config cleanup --remind never` | refused with the generic minimum text "At least 3 days, or never." (offers "never" for the one kind that refuses it); the remind-specific key is used only in `askDays` (368-378); no test pins it | minor |
| L3-9 | `PlanCommand.java:173,200,293,981,1426`, `EpicCommand.java:121` vs `PmOperator.java:235`, `ProjectCommand.java:127` | the same `scope_not_found` | rendered to stdout for plan/epic and to stderr elsewhere; decision refusals and `ConfigCleanupCommand:307/423` to stdout; tests pin both; D6 says a refusal goes to err | minor |
| L3-10 | `PmOperator.java:230` | exit code 4 through `OperatorRefusedException` | dead branch (nothing passes 4; exit 4 comes from `OperatorSecurityException` 222-226) | minor |
| L3-11 | `PmOperator.java:193`, `operator.en/de.properties:791-792` | `answer` without id, a bad option value, in German | only `UnmatchedArgumentException` is localised; any other `ParameterException` prints picocli's English text, without the ✗/→ pattern; the lines end with "help lists them" (the console-only word) | minor |
| L3-12 | `CredentialsCommand.java:357-361`, `PlanCommand` 53, `ConfigCommand` 57 | `LC_ALL=de_DE pm-operator plan -h` (or `config -h`, `credentials rotate -h`) | about 370 `description = "…"` literals stay English; the pack holds 34 usage keys | minor |
| L3-13 | `ConfigCommand.java:1291-1296` | a refused settings read in the machine-settings run | prints "Unable to load settings: HTTP {status}" ("HTTP unreachable" for 0); D6 says every non-404 refusal prints the refusal; `operator.config.settings_failed` (`:323`) duplicates `operator.settings.list_failed` (`:192`); about 20 "HTTP {status}" keys remain | minor |
| L3-14 | `operator.en.properties:771-782` (en and de) | none (dead keys) | `operator.cleanup.footer`, `footer_sweep`, `kept_header`, `none`, `none_header`, `released`, `residue_header`, `residue.*` are unreferenced and still freeze invented figures ("214 · 340 MB"); `released` has no singular | minor |
| L3-15 | `Display.java:78,122-124,140`, `ConsolePrompt.java:165,172` | `Display.word` of an unknown value; `Display.gate` of free text | stale javadoc (`@throws` no longer true, "smoke tests"); a free-text unknown gate with a space, `<`, `>` or `=` passes through unchanged (wire text unsanitised, against the "no raw token" rule); an unknown non-blank need kind still maps to `overview.needs.other`, so D9's "one rule for an unknown value" is half done; the hyphen change alters four pinned strings including `ci_check` scopes | minor |
| L3-16 | `RuntimeCommand.java:135` | a runtime status carrying no `pid` | `Map.of("pid", status.get("pid"))` throws a `NullPointerException`; the old concatenation printed "pid null"; the generic handler prints "runtime: null" and exits 1 | minor |
| L3-17 | `MessageCatalogue.java:196`, `:80` | `get(key, locale)` argument swap; a key present in en but missing in de | `get(key)` changed meaning (raw template, no select or plural evaluation); the bundle has no parent, so a missing de key falls back to English in picocli; today parity is complete (operator 815/815, error 129/129, decisions 192/192, plan_view 148/148, projects 207/207), so latent; the cache is thread-safe (`computeIfAbsent`, no recursion) | minor |
| L3-18 | `operator.en.properties:761`, `:320`, `:723`, `:786` | two cleanup sites | `operator.common.needs_choice` equals `operator.config.needs_choice`; `delete_preview` and the frees lines are not pluralised ("1 plans") | minor |
| L3-19 | `ConfigCleanupCommand.java:303`, `:491` | `config cleanup d)` on a project with no own values; a later DELETE or write fails | prints nothing and exits 0; stops at the first failed DELETE leaving earlier ones applied without reporting them; the machine path (`recordMachine` 410-440, `stopAtConflict` 455) holds | minor |
| L3-20 | `RefusalRenderer.java:84`, `OperatorRefusedException.java:112` | none (rule) | the "The runtime refused: " prefix is a hard-coded literal with a hand-rolled `startsWith("de")`, duplicated; neither file is in `LiteralTextGuardTest.GUARDED` | minor |

**Layer 3 not shown by reading:** the bulk of the `MockRuntimeEngine` diff, the two scenario JSONs, `WebCommand`, `DecomposeCommand`, five test classes and the coverage figures (above); whether `ScreenCaptureIT` captures show what the lane says (under `.plan/temp/terminal/`, not opened); the CI logs.

**Layer 3 verdict:** findings — 20 (7 major, 13 minor); large parts not shown by reading.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Code Standards | no generic `Exception`/`RuntimeException` | conforms | grep of the changed main files empty |
| CLAUDE.md Code Standards | no `System.out`/`System.err` | conforms | output through `PrintWriter`/`commandLine().getOut/getErr` |
| CLAUDE.md Code Standards | `@NullMarked`, documented types | conforms | `package-info.java:15`; no new package |
| CLAUDE.md Code Standards | input data from `Generators` with `@EnableGeneratorController` | deviates | `PicocliLanguageTest` (no controller, literal `bogus` `:76,:87`); `ExitCodesTest:105` literal, `:54` class-load static |
| CLAUDE.md Code Standards | `CuiLogger`/`LogRecord` | not_shown_by_reading | huge hunks not read |
| spec Standing Constraints | every new operator text through `MessageCatalogue`, en and de | deviates | L3-20, L3-12, `PlanCommand` 1044-1305; pack parity clean |
| spec Standing Constraints | no raw id, state id or HTTP status on screen | deviates | L3-7, L3-1, L3-13 |
| pm-dev-java:javadoc | javadoc matches behaviour | deviates | L3-15 |
| pm-dev-java:junit-core | a test stays green on a revert, assertion anchored | deviates | L2-4, `PicocliLanguageTest` deny-list, `ExitCodesTest` refusal asserts the code only |
| pm-dev-java:java-core | resource, Optional, check-then-act, empty catch | conforms | cache thread-safe; the interactive refusal loop has EOF and Ctrl-C exits |

## Metrics and Anomalies

- Tokens: `unknown` (harness `opencode`, model `deepseek-v4.1-flash/high`, effort high, plugin version `unknown`, executor 0.1.2149): recorded as unobserved, not zero.
- Anomalies: a resumed run, the first run having ended without a landing; a 901 s `timeout` build (a hung interactive test, fixed in `2e4b3271`); the self-review reported `budget-exhausted` for the residue; CodeRabbit refused on quota at 17:03Z and was re-triggered 90 minutes later; the mailbox was never readable (`invalid_target_plan` for a plan id with a leading digit, a known plan-marshall gap).

## Routing and Merge Behavior

- Review: CodeRabbit reviewed four heads, 29 body findings, 26 fixed and 3 answered with the reason; Sourcery refused structurally (150,000 characters). Re-read with `runbook_lane review-gate --pr-number 438`: `unresolved_review_threads=0` (claimed 0, **same**), `review_body_findings_total=29` (claimed 29, **same**); `review_body_findings_handled` reads 0 without a disposition record and the run's progress file is gone, so the claimed 29 is **not re-verified**.
- CI/merge: merged through the merge queue as `2efcaa40`; the gate at the merged head `213b0b02` green by the lane's account (logs not opened).
- Sonar: claimed `sonar_open_new_issues=0`, quality gate ok; re-read through the Sonar search for pull request 438: no issues, **same**; new-code coverage lifted 78.3 → 83.8-84.6 % by the lane's account (not re-measured).
- Collisions: three conflicts when merging `origin/main` (`MessageCatalogueTest`, `MockRuntimeEngine`, `PlanCommand`), both sides kept.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-30 --status shipped` (the run ended and the PR is merged; **the operator may instead want the row back to `staged` for the residue; the grade is the operator's**)
- [x] row `pr` stamped 438
- [x] row `landing` stamped `landings/PLAN-30.md`
- [x] row `plan_marshall_plan_id` stamped `30-cli-texts-and-errors`
- [x] epic.md: Open Defect entry for this landing and for the incomplete landing facts
- [x] resume anchor, `queue-view.md`, decision log

## Follow-Ups

- Layer 3 reported to the operator at once, none staged: the seven majors. L3-1 (a refused upgrade check exits 0) and L3-2/L3-3 (a local refusal shown as an unknown runtime code; a literal `{project_id}`) are the ones an operator sees on a normal failure.
- The residue plan (named, NOT staged): D4's test (success and refusal for every verb with the renderer path distinguishable, 130), D5 (the no-locale callers, the English `plan report`), D7 (the unready-provider test and the #415 replies), D8 (the choice bar and delete-confirmation captures, "remind offers no never"), the layer-3 majors, the picocli option descriptions, the dead cleanup keys, the guard's inversion, the design lines (`03-dialogues.adoc:269`, `work-actions.adoc:222`, `26-cleanup.adoc`).
- For plan-marshall: the mailbox read refuses a plan id with a leading digit (reported again); `landing-check` complete: false for `total_tokens` and `plugin_version` `unknown`; a lane report that grades its own deliverables (`deliverables_done=3`) is the honest form and the one to keep.
