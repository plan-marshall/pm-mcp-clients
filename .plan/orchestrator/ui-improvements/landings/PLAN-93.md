# Landing analysis: PLAN-93 web-format-and-token-residue

PR #319 `4dcf77b1` (squash-merged 2026-11-05T09:57Z; head `e34987d5`; 43 files). Lane `rb-93-web-format-and-token-residue`: landing message filed 2026-11-05T09:59Z. Analysis by a read-only sub-agent (code read at the merge commit, nothing built or run); the unresolved-thread list, the Sonar bot comment, the Sonar rule and the lines named below were re-read by the orchestrator.

## Merge rule and Sonar policy

- CodeRabbit: review `5780908254` on `65aa5c73` (20 findings: 8 inline, 4 follow-ups, 8 nitpicks), answered in PR comment `6651918991`; the 8 inline threads carry no reply inside the thread. Sourcery: 2 reviews of `5d9a4de`, 5 findings handled. Unresolved threads of PR 319 (V: GraphQL): **0 of 13**. The lane's message does not state a thread count (the rule was set later the same day). Two of the answers do not hold (generation counter, the onboarding test: below).
- **SONAR POLICY (zero findings, the operator, 2026-11-05) NOT MET**: the SonarCloud bot comment at 09:44Z says "Quality Gate passed" and **"3 New issues"** (V); the Sonar MCP search for PR 319 returned "No issues were found" (not reconciled: read after the merge). The lane left the fix on a side branch `fix/93-sonar-residue` (`8f3ca376`, one commit on top of the PR head, 14 PR commits, not rebased) and listed the issues as residue. The lane's own account called the Sonar read "undecidable" and merged on recorded evidence; CI runs exist (run `42002587449`) and the "no run for the last three heads" claim was resolved by `b4c09e54` before the merge.
- **The side-branch fix does not close `java:S6213`** (V): the rule is "Restricted Identifiers should not be used as Identifiers" (`record`), not near-identical tests as the lane wrote. At `4dcf77b1` `var record` stands at `OnboardingScenarioTest.java:373` and `:398`; the fix keeps one and adds a parameter named `record`. `java:S1192` (the `remind_after_days` literal, `MockRuntimeEngine.java` ~8702) is closed by it (I; two literals stay at ~8722/8729).

## Three layers

| Done-when | Implemented | Correct |
|---|---|---|
| D1 marks from the catalogue | MET in code and tests (`format.ts` ~314-392, one case per form in `de` and `en` with a pack override); the browser check "text unchanged" not run, and the text changed (`412 k tokens (…)`) | the word "tokens" now doubles with the labels beside it (plan view, epic view, system models) |
| D2 formatters select by `locale` | MET (`i18n/index.ts` `resolveTemplate`) | works only if that locale's catalogue is in the session history, else silent English (I); latent, callers pass the active locale |
| D3 edges | MET; `mutation-after` unverifiable (logs gone) | fine |
| D4 e2e selectors, Sonar clause | selectors MET (`helpers/plans.js`, `openAnyPlan` keeps `.first()` with comments); the open-issue clause NOT MET | |
| D5 (a) reminder record | MET | cleanup save sends a JSON record that only the mock accepts (the lane says `pm-mcpd` must accept it); conflict dialog prints raw JSON (`MachineSettingsView.svelte` ~281-284), "apply yours again" resends the stale record (~127), a refusal shows `update_failed` not the minimum, plans "never" blocks the reminder |
| D5 (b) chat | PARTLY: the generation counter CodeRabbit asked for does not exist (V: no `generation` in `DecisionView.svelte`; guards compare `chatDecisionId`) | the lane's reply ("sets chat = null first, so a superseded load fails that check") is false: open A, open B, reopen A makes the id equal again, a stale load or error wins (~611-651) |
| D5 (c) step line | code MET | the test cannot fail on the locale part: the `clientWidth` spy is evaluated at `box === 0` (I) |
| D5 (d), (f), (g), (h) | MET | `check-build-output.mjs` fails open on a read error; `DEV_MARKERS` hand-written; `strictPortIsBound` a regex over config text |
| D5 (e) | zero floor MET; rollback not built as decided | the reason given cites "CC-P-05", which here is CLI-CC-P-05 (the operator gate), not the operator's reason |
| D5 (i) palette | `ArrowDown` and Tab-walk cases MET | **mailbox 002 ignored** (V): the `focusable` selector at `CommandPalette.test.ts:76-77` has no `iframe`, `object`, `summary`; the PR 314 thread stays unresolved and the message does not mention it |

Files outside the Expected Surface: `MockRuntimeEngine.java`, `OnboardingScenarioTest.java`, four web test files, `OnboardingView.svelte` (test id), `ports.ts` and test, `check-build-output.mjs`, `buildOutput.test.ts`, `package.json`, `global-setup.js`, `helpers/plans.js`, `mobile.spec.js`, `port-guard.spec.js`, `guard-ports.js`, two design files. Cited `maven-2026-11-0*.log` files are not available (lane worktree gone): evidence orders are unverifiable by reading. The lane's claim that `console-settings.transcript` was regenerated in `65aa5c7` is moot (not in the net diff; the file was last touched by `159c310c`); the `<ago>` rows still cross 56 days around 2026-12-08 to 12-11 (PLAN-98 deliverable 1).

## Unverifiable lane claims

CodeRabbit was "re-triggered once after `7c7ea71` and refused at 07:02" (no trace; the only refusal found is 2026-11-04 22:48); the browser checks of the reminder and the step line (no artifact); the decision chats (the mock answers 404).

## Routing

- Sonar (3 issues, S6213 correctly understood, the compat path of the mock): PLAN-95 new deliverable 7 (the mock) and mailbox message to `rb-95`.
- Everything else: new staged PLAN-101 `web-format-and-token-second-residue` (WS-03, 21010/21011): the cleanup record (conflict, refusal, resend, "never", wire form and the open daemon item), the chat generation guard, the palette selector and the PR 314 thread, the weak tests, the tokens word, the wrong reason of D5 (e), the thread replies.
- Open outside this repository: `pm-mcpd` must accept the `cleanup_durations` record (`plan-marshall/plan-marshall-mcp`).
