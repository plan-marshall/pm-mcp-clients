# Landing Analysis: PLAN-23 — Terminal: the Cleanup Decision Dialogue

epic: ui-improvements
workstream: WS-04
pr: #381 (https://github.com/plan-marshall/pm-mcp-clients/pull/381), merged as `fb27d4aa` (squash through the merge queue, 10 files, 2026-11-09T08:55Z; parent `354098bf`)

> Runbook lane (harness opencode, model deepseek-v4.1-flash/high, executor 0.1.2080). Inbox message
> `rb-23-cli-cleanup-dialogue-001.md`. Checked against git, the PR (gh) and a read-only leaf; key points re-read by the
> orchestrator at `fb27d4aa`.

## Deliverable Fidelity vs Spec

| Done-when (D1 Dialogue) | Verdict | Evidence |
|-------------------------|---------|----------|
| `CleanupDialogueIT` enabled, `q` deletes nothing | met | 6 PTY cases, no `@Disabled`, all wait on `awaitText`; the report says 5 and names a case `consoleNoSendsNoDelete` that does not exist (the unit test `dialogNoSendsNoDelete` does) |
| Terminal captures before/after | claimed met | `PLAN-23.txt` lists 8 captures (120×30, 80×24, en/de); the files under `.plan/temp` were not opened |
| Browser regression check | met, before waived | the operator waived the before set (no web file in the diff) |
| Open-issue read at the head | met | SonarCloud on #381: Quality Gate passed, 0 new issues |
| Operator scope decision: all six keys wired (`d r i x o q`) | met | `CleanupCommand.decide` `:547-571`, `Decision` enum `:578-620`. Tests: `d`, `x`, `q` through the bar (unit + IT); `r`, `i` only through the `--remind`/`--ignore` options; `o` only its refusal for a logs item |

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`, `orchestrator.analyze`), code and tests, no build.
- **Verdict:** layer 1 met for the code clauses; layer 2 mostly (catalogue complete in both locales — 17 keys each; `ChoicePrompt.java` +171 and its test +84 outside the Expected Surface, opt-in; an AGREED design document edited, partly drift); layer 3 no major — a delete is sent only after `y`/`j` from `askConsent`.
- **Re-read by the orchestrator at `fb27d4aa`:** `CleanupCommand.java:470-481` (the non-interactive fall-through into `sendDelete`); `CleanupCommandTest` (0 `Generators` / `@EnableGeneratorController`); `CleanupDialogueIT` (6 `@Test`); the `26-cleanup.adoc` confirmation change. All confirmed.

### Seams

| Id | File:line | Seam | Severity |
|----|-----------|------|----------|
| S1 | `CleanupCommand.java:688-694` | the leave-out mark wraps at 80 columns (92 en, 96 de); titles over 18 characters overflow too | minor |
| S2 | `CleanupCommand.java:855` | `{items}` filled with `""`; matches the design transcript, but the design's text table (`26-cleanup.adoc:309-311`) disagrees with the catalogue (older drift) | conformance |
| S3 | `CleanupCommand.java:801-830` | grouped plans/epics of a left-out project are not discounted; the preview over-reports (documented in the AGREED doc by this PR) | minor |
| S4 | `CleanupCommand.java:335`, `OperatorContext.java:105` | the reminder day renders in the context clock's zone, always UTC in production: off by one for the operator | minor |
| S5 | `CleanupCommand.java:294,381,777` | no handling of an already-decided job; a delete 409 always reads "runtime is stopping" (design `26-cleanup.adoc:174` wants the decision shown); CodeRabbit thread `:423` only partly fixed | minor |
| S6 | `CleanupCommand.java:985-990` | `glyphForKind` still on legacy kinds: `unenrolled_project`, `read_reports`, `logs` get no glyph | minor |
| S7 | `CleanupCommand.java:382,453-461` | `--exclude` ids not validated against the job (design `:106` refuses with `not_in_job`); `--exclude` strips `kind:` while the dialogue sends the raw id; without `--delete`, `--exclude` is ignored and `i` returns 1 | minor |
| S8 | `CleanupCommand.java:477-481` | a non-interactive path prints the preview and sends the delete unconfirmed; unreachable today behind `OperatorGate.verify` (non-TTY acts refused), but unsafe if the gate is relaxed; also the old behaviour | minor |
| S9 | `CleanupCommand.java:641-649` | the `… N more` line (`operator.cleanup.more`, design `:59,:84`) is not printed; items beyond the page cannot be addressed by `x`/`o` | conformance |
| S10 | `CleanupCommandTest` | no `Generators` / `@EnableGeneratorController`; every id, title and size a literal (CLAUDE.md); not reported by the lane | conformance |
| S11 | `CleanupCommandTest` | untested: `r`/`i` through `decide` and the posted job id; `o` on plan/epic (exit code ignored); the whole-job note in the console path; an uppercase prefix; `deletePreview` beyond a plan item; a real narrow terminal | minor |
| S12 | `doc/design/terminal-client/26-cleanup.adoc` | AGREED document edited without a recorded operator approval: two fixes (no `{days}`, two-line bar), drift (confirmation lost "back" and `?`; 12 keys replace one but 7 new keys miss the text table; the S3 limit written as design) | conformance |
| S13 | `printJob`, `previewText`, `sendDelete` | job and preview body strings stay hard-coded English, bypassing `MessageCatalogue` (older strings, moved or reused) | conformance |
| S14 | report §1 | "CleanupDialogueIT 5/5" and the case `consoleNoSendsNoDelete`: false; the file has 6 cases | minor (report accuracy) |

## Routing and Merge Behavior

- **Review:** CodeRabbit 5 bodies (9 + 4 + 1 + 6 + 3 actionable, 0 nitpicks, no outside-diff items) on `3f0da9b`, `dff4acb`, `86c5f53`, `89acbf8`, `0585c2c`; then quota refusals. Threads 22, all resolved; 7 replied; **15 resolved without a reply** (13 by the lane, 2 by CodeRabbit). The leaf checked all 15 at `fb27d4aa`: 14 fixed, 1 partly (`:423`, S5); test backing thin for several. Sourcery refused (budget, optional). Merge rule met on the letter (a review exists, every body item is a thread, all resolved); the silent resolutions are recorded.
- **Merge:** on the lane's standing review authorization (`quota_limited` CodeRabbit re-triggered after the last push and refused); merge-queue build green.
- **Sonar:** 0 new issues (confirmed from the SonarCloud comment).
- **Rule check (self-reported):** shell heredoc / `perl -pi` edits of a properties file and `CleanupCommand`, shell `grep`/`sed`/`cat` instead of the file tools, `python3 -` heredocs outside the executor; `guard: none` on opencode. `self-review: budget-exhausted` with low items open (S1, S2, S3).

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 381; `landing` `landings/PLAN-23.md`
- [x] epic.md Watch
- [ ] seams to the operator; fix route to be decided (a residue runbook, or folds into the next cleanup/terminal plan)

## Follow-Ups

- Residue candidates: S5, S6, S7, S9 (behaviour), S10, S11 (tests), S12/S2 (design text table and the confirmation wording, decided by the operator), S13 (catalogue), S4 (zone), S1 (80 columns), S8 (make the non-interactive path refuse).
- Lane follow-ups not verified: the mock turning a decided job back to `POSTPONED` on a late reminder (`MockRuntimeEngine`); `o` opening the plan view without ended-work context.
