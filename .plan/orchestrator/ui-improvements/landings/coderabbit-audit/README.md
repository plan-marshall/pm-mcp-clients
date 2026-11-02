# CodeRabbit audit of the ledger's history (2026-11-02)

The operator's rule (2026-11-02): a result from CodeRabbit is always handled, a late review included, and a finding in a review BODY counts as much as an inline thread. The landing checks until then looked at threads only (PR #303 merged with six open findings that sat in a review body). This audit re-read, for all 39 pull requests the ledger names (PR 231 to 304, minus the ledger lands), every `coderabbitai` review body and every review thread with its resolution state and replies, and compared each finding with the code at the merge commit and on `main` at `94bbefe1`. Three read-only sub-agents did the reading (the operator's rule: analysis by sub-agent); the orchestrator read their detail files, spot-checked the PR #303 and PR #275 items, and routed the findings. Nothing was built or run. Detail with file and line per finding: `batch-A-prs-231-259.md`, `batch-B-prs-260-283.md`, `batch-C-prs-284-304.md`.

"Handled" means fixed in a pushed commit, or answered with a stated reason in a reply or a PR comment. Findings that CodeRabbit itself marked "Addressed" were spot-checked, not all re-derived.

## Result: unhandled findings still present on `main`

| PR | Plan | Unhandled | Most important | Owner |
|---|---|---|---|---|
| 232 | PLAN-02 | 1 | `OnboardingDialogueIT` re-ask assertion counts a frame title | PLAN-94 D4 |
| 237 | PLAN-01 | 1 | `theme-readability.spec.js` records EpicView, CleanupView, MachineSettingsView while loading (unresolved thread) | PLAN-93 D5 (f) |
| 243 | port assignment | 4 | `strictPort`, `Number(env) \|\| 5173`, duplicated port guard, repeated `webPort > 0 ? webPort : 7420` | PLAN-93 D5 (g), PLAN-95 D5 |
| 253 | mock goto | 5 | `setScenario` does not validate start-state facts first (Major) | PLAN-95 D2 |
| 260 | PLAN-25 | 1 | no build-output check for the dev page | PLAN-93 D5 (h) |
| 269 | mock plan details | 2 | `startedPlans` never cleared; start-entry `create` plans not recorded | PLAN-95 D1 |
| 270 | PLAN-13 | 1 | `EntryFlow.badges` omits open project onboardings | PLAN-94 D2 |
| 275 | PLAN-22 | 13 (12 + 1 partial) | one blanket "Fixed" comment pasted into five threads; CodeRabbit replied each was still open | PLAN-95 D1/D4, PLAN-94 D2 |
| 284 | PLAN-82 | 10 | four unresolved threads (unlocked readers, `startedPlans`, plan location); `NeedsOperatorItem` has no item id | PLAN-95 D1/D3 |
| 287 | PLAN-09 | 2 | Major: the machine settings reminder interval is never saved | PLAN-93 D5 (a) |
| 291 | PLAN-16 | 2 | `formatTokens(0, true)` reads `0`, not `≥ 0`; catalogue eviction | PLAN-93 D5 (e) |
| 292 | PLAN-17 | 1 | no read deadline on the snapshot request of the runtime acts | PLAN-94 D3 |
| 296 | PLAN-46 | 3 (body only) | `CommandPalette` passes only `match.id`; two test gaps (the six inline findings were fixed by PR #300 but their threads stayed unresolved) | PLAN-93 D5 (d) |
| 297 | PLAN-37 | 6 | Major: `DecisionView.answerChatProposal` writes into another decision's panel; the last review came after the last code commit | PLAN-93 D5 (b, c); the `ControlActionsBar` item went to PLAN-47's mailbox |
| 299 | PLAN-26 | 6 | draining line kept on upgrading/stopping; `stateLabel` unsanitised; interactive device acts swallow failures | PLAN-94 D1 |
| 303 | PLAN-91 | 10 | the six known (PLAN-92) plus `04-console.adoc:195`, `dev-2`/`dev-3` literals, failed-snapshot test | PLAN-92 (running; the three extra went to its mailbox) |

Clean: PR 231, 238, 246, 247, 249, 255, 256, 257, 259, 262, 266, 273, 274, 277, 281, 282, 283, 288, 293, 301, 304. PR 298, 300 and 302 carried `skip-bot-review` (CodeRabbit posted "Review skipped"); PR 302, the data components of PLAN-35, never had a bot review. PR 276 never got a review either (only "rate limited" notices). Both are the operator's earlier skips, not findings.

## Routing

- PLAN-92 and PLAN-47 are running: their specs were not edited; the extra findings were delivered to their mailboxes (`inbox/to/rb-92-cli-interrupt-completion/`, `inbox/to/rb-47-web-actions-by-state/`).
- PLAN-93 (staged) took deliverable 5 (the web findings).
- PLAN-94 `cli-coderabbit-residue` and PLAN-95 `mock-runtime-coderabbit-residue` are new, staged.

## Limits

Not checked line by line: threads CodeRabbit marked "Addressed in commit" (spot-checked). Two answers rest on a judgement (PR 262 "filed as a follow-up", PR 273 a coverage gap accepted); whether the follow-ups were filed was not checked. PR 8 sibling `plan-marshall-documentation` PR #128 was not read.
