# Landing Analysis: PLAN-84 — Terminal: Go to Work by Name (the `find` Command)

epic: ui-improvements
workstream: WS-04
pr: #397 (https://github.com/plan-marshall/pm-mcp-clients/pull/397), squash `1b938f37`

> Landing record for one shipped plan. Written by the `analyze` verb (session 3, 2026-11-10) from the inbox message
> `rb-84-cli-find-001.md`, verified against git, `ci pr reviews`, `runbook_lane review-gate` and SonarCloud. Runbook lane,
> harness opencode, model `deepseek-v4.1-flash/high`. The lane's own follow-up PR #405 (`fix/84-find-kind-words`, 4 files,
> +63 −14) is OPEN with no CodeRabbit review.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 the command | shipped-modified | `FindCommand.java` (registered `PmOperator.java:65-66`), console `/` (`PmConsole.java:386-389,422-445`), `FindCommandTest` (34 `@Test` + a 5-value `@EnumSource` = 39 cases; the lane said 40), `FindIT` (2), `PLAN-84.txt` 20 captures. Evidence order `mutation-after`, not the spec's `test-first` (the lane says so). The German kind word prints the wire value; fixed only in #405. |
| D2 texts and design | shipped-modified | `operator.find.*` en/de (`operator.*.properties:126-128`), `LiteralTextGuardTest.java:65`; the catalogue test covers `operator.find.*` only (#405 widens it); `doc/plans/README.adoc:360-364` row left open as the amendment orders; the `project setup --entry --` fold done in `01-commands`, `04-console`, `17-projects`. |

Added-unplanned, explained: `ReadVerbExitCodesTest.java:96` (a new read verb belongs in its hand list).

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`, `orchestrator.analyze`) — reason: 13-file merged diff
  (+1339 −8) with terminal input handling; one landing per leaf. The leaf wrote nothing and opened no `.plan/` path.
- **Re-read by the orchestrator at `1b938f37`:** `FindCommand.java:194-197,308-323,366-373,463-469`,
  `OperatorContext.java:236-251` (and `useTerminal` at `:260`), `PmConsole.java:428-436`, `FindCommandTest.java:529-541`.
  Confirmed. The remaining points are recorded as the leaf's reading.
- **Verdict:** layer 1 findings (1 minor); layer 2 findings (2 minor); layer 3 findings (6 minor checked; 1 likely-major and
  2 minor not shown by reading).

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence (file:line) |
|---------|-------|---------|----------|----------------------|
| L1-1 | Sound after the amendment, with seams the lane had to resolve: "opens the project" vs `projects --filter` lists; D2's README row vs the amendment; `overview.find.kind.*` (amendment) vs `common.kind.<kind>` (`shared/needs-and-find.adoc:175`); no real-tty clause for the typed field and Ctrl+C; no exit-code rule for an empty text or a left choice. PLAN-82 premises hold. | checked | confirms | spec D1/D2, amendment |

**Layer 1 verdict:** findings — 1 (no staged spec's claim settled; no `corpus set-verdict`)

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained file / unborne claim | Verdict |
|-------------|------------|--------------------|---------------------------------|---------|
| D1 | `FindCommand.java`, `PmConsole.java:386-389,422-445` | `FindIT` console path; `FindCommandTest.java:529-541` "opens the one selected" cannot fail if the open fails (re-read: asserts text already in the list, exit code unchecked) — #405 fixes; no PTY test of the shell find-then-select | 39 cases, not 40 | checked |
| D2 | catalogue keys | catalogue test sees `operator.find.*` only, not `overview.find.*` / kind words (#405 widens) | `operator.console.help.body` names neither `find` nor `/` (PLAN-42 owns help) | checked |
| evidence order | — | — | `test-first` not shown (squash) | not_shown_by_reading |

**Layer 2 verdict:** findings — 2

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 (re-read) | `FindCommand.java:196` | German locale | kind column prints `project`/`decision` (wire value); `%-9s` too narrow for `Entscheidung` — #405 fixes | minor |
| L3-2 | `FindCommand.java:295`, Javadoc `:53-54`, `01-commands.adoc:156` | select a project match | `projects --filter <id>` lists (substring, possibly several), the docs claim "opens the explorer filtered" — #405 fixes the text; behaviour is the spec's | minor |
| L3-3 (re-read) | `FindCommand.java:467-468` + `PmConsole.java:432` | Esc in the `/` field | two line ends, a blank line — #405 fixes | minor |
| L3-4 | `FindCommand.java:196` | a match with a blank / control-only / null title | prints an empty cell or the bare id; location titles go through Display (`:255-264`), the match title does not | minor |
| L3-5 | `FindCommand.java:456-463` | Backspace after a supplementary character | lone high surrogate in `q=` (encoded `?`); one column of junk for a wide character | minor |
| L3-6 | `FindCommand.java:180-187` | a runtime that omits/normalises the echo | header and "Nothing matches “”." show the echo, not the typed text | minor |
| L3-7 | `FindCommand.java:130-134,317-319` | empty text; `q` at the choice | exit 0, where `01-commands.adoc` § Exit Codes gives 3 for a left dialogue | info |
| L3-8 | `FindCommand.java:371` | an id with a space or leading `-` | split into arguments without a `--` guard (ids are runtime slugs) | info |

**Layer 3 not shown by reading:**
- L3-NS1 (likely **major**, re-read): `choose()` (`:310-322`) holds a lease from `context.openTerminal()`, `open()` (`:371-372`)
  runs `PmOperator.execute` without `context.useTerminal(lease.terminal())`; `OperatorContext.createTerminal()` (`:247-250`)
  then builds a second terminal, which its own Javadoc (`:236-246`) says JLine falls back to dumb for. `pm-operator find x`
  then a number may get a refused or dumb view. The console path is scoped (FindIT passes); the unit test hands every caller
  one DumbTerminal. Needs a PTY run. Not in #405.
- L3-NS2: Ctrl+C in the `/` field is read as char 3 after `enterRawMode()` (`:385`), which keeps ISIG; on a real tty it may be a
  SIGINT that ends the console (exit 130). Same in `SecretReader.java:56-66`. Not in #405.
- L3-NS3: only the first page of `find` is read (`FindPage` carries paging; the mock answers one page).

Confirmed sound by reading: every runtime string through `TerminalSanitizer.sanitizeLine` (`:180-297`); `q=` URL-encoded
UTF-8; the typed text guarded by `--`; no gate on a read.

**Layer 3 verdict:** findings — 8 checked (6 minor, 2 info) + 3 not shown (1 likely major); reported to the operator at
once (AskUserQuestion 2026-11-10): **fold into PLAN-126** (new deliverable 5, Expected Surface +5 in the same edit).

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Code Standards | no generic Exception/RuntimeException | conforms | catches `IOException`, `EndOfFileException`, `UserInterruptException`, `NumberFormatException` |
| CLAUDE.md Code Standards | no System.out except the CLI's output | conforms | picocli writer, terminal writer |
| CLAUDE.md Code Standards | `@NullMarked` | conforms | package-info; `@Nullable` at `:346,355,383` |
| CLAUDE.md Code Standards | JUnit 5 only, no Mockito/Hamcrest | conforms | `FindCommandTest`, `FindIT` |
| CLAUDE.md Code Standards | Generators for input data | conforms | `@EnableGeneratorController` (`:58`), no-match text from Generators (`:216`) |
| Spec | find is a read, no gate | conforms | no gate call; `answer`/`reports open` gate themselves |
| Spec amendment | kind word from the catalogue; blank title through Display | deviates | `:196` (kind fixed in #405; title not) |
| Spec | no new dependency | conforms | no pom change |
| 04-console output safety | runtime text sanitized | conforms | see above |
| Spec evidence order | `test-first` | not_shown_by_reading | lane: `mutation-after` |

## Metrics and Anomalies

- Tokens: `unknown` (opencode) — `landing-check` `complete: false`, `missing_keys` `total_tokens`, `plugin_version`; folded
  into the standing Open Defect. `executor_version=0.1.2090`.
- Steps: `setup:done, implement:done, build-gate:done, self-review:clear, pr:done, reviews:done, merge:done`. The lane admits
  the self-review ran AFTER the merge (step missed); its four findings are real and sit in #405. `self-review:clear` is
  therefore not a pre-merge fact.
- Anomalies: one inline `python3 - <<EOF` (rule break, read-only); two `rm` of scratch files; one session restart; a CI
  flake `TuiScreenTest.levelChangeLeavesNoResidue` re-run green.

## Routing and Merge Behavior

- Review: `ci pr reviews --pr-number 397` shows CodeRabbit reviews (first `09:38:37Z`), so the merge rule (one CodeRabbit
  review of some head) is met; later re-triggers were rate-limited. `runbook_lane review-gate --pr-number 397`:
  `unresolved_review_threads=0` (landing 0), `review_body_findings_total=6` (landing 6) — agree.
  `review_body_findings_handled=6`: the progress file carries no `finding:` lines → **not re-verified** (the landing lists a
  disposition with a commit or a reason for each of the 6 inline items and 3 nitpicks).
- CI/merge: green after one flake re-run; merged via the queue. #405 (post-merge self-review fixes) has no CodeRabbit review
  yet (3 rate-limit refusals) — owed before its merge.
- Sonar: claimed `0`; SonarCloud search for PR 397 → no issues; quality gate OK (new coverage 92.4 %). Measured `0`, agrees.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 397; `landing` `landings/PLAN-84.md`; `plan_marshall_plan_id` `rb-84-cli-find`
- [x] epic.md: incomplete-landing recurrence; Watch for PLAN-84 and the open #405
- [x] PLAN-126 deliverable 5 folded (find residue), Expected Surface +5, verified via `corpus surfaces` (32 entries)
- [x] resume anchor updated; `queue-view.md` regenerated

## Follow-Ups

- #405 → needs one CodeRabbit review and the operator's merge word; PLAN-126 deliverable 5 starts from what it leaves.
- L3-4…L3-8, L3-NS1, L3-NS2 → PLAN-126 deliverable 5. L3-NS3 (paging) → recorded only (the contract has no page parameter).
- `operator.console.help.body` lacks `find` and `/` → PLAN-42 (help text owner).
- `shared/project-onboarding.adoc:81` stale `project setup --entry <entry_id>` form → noted; no plan owns the file now.
