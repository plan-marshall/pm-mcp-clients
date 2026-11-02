# PLAN-91 — Terminal: Ctrl+C Status in Every Handler and the Transcript Goldens — landing

PR #303, merged into `main` as `94bbefe1` (30 files, +1131/−127) through the merge queue. Lane message `rb-91-cli-interrupt-and-transcripts-001`
(the lane reports `deliverables_done=1` of 3: D3 fully met; D1 and D2 not met as written).
Analysed in three layers (specified / implemented / correct) by a read-only sub-agent that read the merge diff before the lane's report; the orchestrator
verified the findings it records against `main` and the PR. Nothing was built or run.

## Verdict

| Layer | Verdict |
|---|---|
| 1 Specified | D1 PARTLY: all eight named handlers return 130, but the title says "every handler" and several still map Ctrl+C to 0. D2 NOT MET, no subject: its premise is refuted. D3 MET by reading. |
| 2 Implemented | Every changed file is explained. The three goldens and their tests are untouched, consistent with D2 having no subject. Docs: `03-dialogues`, `04-console` and `27-tui-testing` changed; two of their sentences are wrong or unproven (findings 1, 2). |
| 3 Correct | The D1 and D3 tests would fail if their code were reverted. One major lead: a real Ctrl+C may not reach a dialogue a full-screen view opened, and the cause the lane gives is unproven. Handlers outside the eight still return 0. |

## Layer 1 and 2 — what was specified, what the diff holds

- **D1.** `WorkActionDialogue:93`, `ReviewDialogue:79`, `HoldDialogue:90`, `ProjectCommand:194`, `PlanCommand:618` (pause), `OnboardCommand:114` and `ActionSupport:208` (end reason) and `:243` (consent) each catch `StartPrompt.InterruptException` and return 130; the five view verbs return 130 from `app.interrupted()`. The new marker lives in `StartPrompt`, with `ChoicePrompt`, `RuntimeActDialogue`, `PlansCommand`, `ProjectsCommand`, `SystemCommand`, `PmOperator` and `TuiScreenDriver.stop()` following it (all justified, none in the declaration).
- **D2 premise refuted, agreed.** The last commit touching `status.transcript` is `7d7b4d71` (PLAN-82); the two start transcripts were last touched by `a1fe2d72` (#118). `7d7b4d71` is an ancestor of both bases. The premise came from PLAN-87's landing, which says "Not verified here". The PR touches no golden and none of the three test classes. "Green" is the lane's account.
- **D3.** `SystemFullScreenTest.LifecycleLine` (replace, leave, clear, failed read), the three device refusals, the config refusal (drives `config show`, `ShowCommand`) and `ReadVerbExitCodesTest` for 401/404 over the 19 read verbs. The four lifecycle cases depend on different branches of `systemBannerFromEvent` (`OverviewFullScreenApp:1739-1745`). The `Generators` input is in place.
- **Merge rule.** CodeRabbit reviewed `78148600` (7052 chars, one nitpick: the config fixture, fixed: it now refuses only `/api/v1/config`, verified in `ConfigCommandTest:66,86`) and the empty 08:26 acknowledgement. **It also reviewed `0c92ada1` at 09:39 with six follow-up comments, and no commit after it except a merge (`bcfd4297`)** — so those six were not handled in the PR. **The merge is OUTSIDE the merge rule**: every CodeRabbit result is handled, a late one included, and this one was not. The lane's check ("no thread is open") cannot see findings that sit in a review body; see "Operator note".
- **Sonar.** Lane: `0` at the merge head `bcfd4297`. My read after the merge is `undecidable` (analysis revision `bcfd4297`): an unverified lead.

## Layer 3 — read, not run

1. **[lead, major] The residue is real in structure, its mechanism is unproven, and it is wider than two dialogues.** `StartPrompt.reader()` (`StartPrompt.java:211`) calls `context.createTerminal()`; `PlansCommand:70`, `PlanCommand:226`, `SystemCommand:140`, `PmOperator:120` and `ProjectsCommand:88` do not lend their terminal (`useTerminal`); only `PmConsole:366` does. So a dialogue a view opens on a direct verb gets a second system terminal that is never closed, and this holds for every dialogue opened through `runDialogue` (review, the runtime acts, project add and setup), not only work action and hold. The tests pass one terminal for both (`InterruptExitCodeTest:88`, `ControlFixture:285-289`), so the real topology is untested. The committed evidence never types Ctrl+C into a view-suspended dialogue: the hold capture uses keys `q\nq`, and the lane's `pause()/resume()` experiment has no log in the diff. The lane's explanation (the view's reader consumes the byte) fits ordinary bytes; under raw mode that keeps ISIG, Ctrl+C normally arrives as SIGINT, which the dialogue's `LineReader` handles. Not settled by reading. The design sentence (`03-dialogues:44`, `04-console:197`) stating 130 for action, hold and review is **unverified, not false**.
2. **[defect, doc]** `04-console:197` lists only "an action, a hold and a review" as ending the view with 130; `runDialogue` ends the view for any task that returns 130, so the runtime acts and project add/setup do too. `03-dialogues:44` has its cross-reference in backticks, so it renders as text.
3. **[defect against the D1 title]** Handlers outside the eight still turn Ctrl+C into 0, null or "no": `StartDialogue:125` (`start.entry.left`, exit 0 — verified; `StartPrompt.read` now throws the new marker and this catch swallows it), `PlanSettingsDialogue:77` (0 — verified, it catches the parent `StartLeftException`), `WorkChatDialogue:75` (null), `ProjectSelector:299`, `AnswerCommand:222`, the confirms in `CredentialsCommand:232`, `EnrolHarnessCommand:241` and `RolesCommand:388`, `OverviewFullScreenApp.askYes:3197`. This contradicts `01-commands.adoc:184` and `03-dialogues:255` ("a verb run as one shell command exits with 3 or 130"). CodeRabbit named the first two in its 09:39 review. Chat and start nested inside review and work action swallow the interrupt and return to the parent prompt.
4. **[lead]** `TuiScreenDriver.suspendFor/stop` (`:514-539`): control flow is correct, but `exit_ca_mode` is sent twice (`suspend()` does not set `restored`), which on some terminals could overwrite the last dialogue line; the view tests never assert `alternateScreenActive()` is false or that the cursor and attributes are restored.
5. **[nit]** `refreshReports`, the SSE paths and `openPendingDecision` (a read-only `questions --plan`) are not guarded after an interrupt.
6. **[nit]** Tests: `projects`, `system` and bare `pm` have no verb-level 130 test (`plans` and `plan <id>` do); `InterruptDialogueIT` settles on 700 ms of quiet time (a timing heuristic); `ReadVerbExitCodesTest.refusedReads()` omits `plans` and `lesson check` although it claims every read verb (CodeRabbit); `InterruptExitCodeTest:81` keeps the literal `"test-token"`; the token-unit tests do not tie a split to its model row and do not assert the summary.
7. **[ok by reading]** The D3 tests fail if their code is reverted; the D1 tests fail on the pre-fix code and assert "no act" as "no non-GET request" (not "nothing read after the interrupt").

## Operator note

PR #303 was merged with six open findings of CodeRabbit's review of `0c92ada1` (09:39), two of them functional and in D1's own subject (Ctrl+C at the start and settings prompts exits 0). The operator's rule (2026-11-02: a result from CodeRabbit is always handled) makes this merge non-conforming. I first wrote it down as conforming to the letter; the operator corrected that. The six are PLAN-92's. The probable cause is the lane's thread-only check, which does not see review-body findings.

## Evidence checked

- `git show --stat 94bbefe1`; the code at the cited lines on `main`; `git log` of the goldens; `ConfigCommandTest:66,86`; `StartDialogue:125` and `PlanSettingsDialogue:77`.
- PR #303: reviews `78148600` (01:59), `2586a83a` (08:26, empty), `0c92ada1` (09:39, six follow-ups); the last commits are `0c92ada1` and the merge `bcfd4297`; no label.
- Not checked (lane account): every maven log, the 2609 and 578 green counts, the PTY captures, the lane's Sonar `0`.

## Follow-ups

All collected into PLAN-92 (one pull request): the PTY proof and the one-terminal fix (1), the remaining handlers (3), the docs (2), the tests (4-6). The drain question the spec left open is answered: `shared/system.adoc:199` says the drain asks nothing (design-audit message).
