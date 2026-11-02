# Landing Analysis: PLAN-16 — Web: One Time Format and a Full Locale Switch

epic: ui-improvements
workstream: WS-02
pr: #291 (merge commit `9890d74bd296f8906a222486488d305e8d75625d`, merge-queue merge onto `ae0cb6b8`)

> Landing record for one shipped plan. Source: inbox message `rb-16-web-time-format-and-locale-001.md` (runbook lane,
> harness `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2036`, written 2026-10-31T23:09Z), analysed
> 2026-11-01 against the merged PR (`ci pr view`), the merge diff (`git show 9890d74b`, 38 files, +1483/−448) and the
> Sonar read. The per-head build logs, the Playwright screenshots under `.plan/temp/playwright-mcp/plan-16-*` and the
> two self-review rounds are the run's account and are not re-run here.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Formatter | shipped-as-specified (the run's account) | `pm-web-app/src/lib/display/format.ts` (317 lines, new) and `format.test.ts` (164 lines, new) are in the merge diff. The message names the failing run of the test-first order (job `71bc0e88…`, `./format` absent). The browser-check clause is the run's account (screenshots not opened here). |
| D2 Every view formatted | shipped-as-specified (the run's account) | 34 `pm-web-app` files and 2 `pm-web-e2e` files changed; the message lists 22 views under the text guard in both locales and says the committed baseline lost every `iso-time` entry. `text-guard-baseline.json` is in the diff. |
| D3 Locale switch and recorded language | shipped-as-specified (the run's account) | `lib/i18n/index.ts` (+335/−) and `index.test.ts` (+141) changed; `onboarding.spec.js` is in the diff. The message names the failing runs of the test-first order. |

`deliverables_done=3` of 3 is consistent with the diff. The test counts, the green gates and the e2e run (308 passed) are
the run's account, not re-run here.

**Surface delta** (`inbox landing-check`, declared = the backticked paths of the spec's Expected Surface, realized = the 38
merge-diff files): declared 10 entries, realized 38, `expansion_detected`, 5 added, 0 missing.

- **Added, never declared (5):** `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` (a
  production file in a module the spec did not name: the message says the mock now records and serves the interface language
  chosen at onboarding and discloses it as "skills added … surface drift"), `pm-mock-runtime/src/test/.../OnboardingScenarioTest.java`,
  `pm-web-app/src/lib/api/client.ts`, `lib/start/sourceLabels.ts`, `lib/stores/runtime.ts`.
- **Declared, untouched (0).**
- The other 33 realized files fall under directory declarations of the spec.

## Metrics and Anomalies

- Tokens: `unknown`; `plugin_version` `unknown` (completeness check reports both missing: the known opencode gap, recurrence
  recorded).
- Anomalies: (1) a fourth base merge brought five commits from `main` and the pre-commit recipes rewrote 177 unrelated
  `pm-api` files; the run reversed them before committing (`git apply -R`), so the PR carries only its own files; (2) one red
  head `12bf4612` from four Sonar findings and one from new-code coverage (64.4 %), both fixed (coverage 91.3 % at the head);
  (3) one routed gate hit the daemon's learned bound at 607 s and the gate and e2e then ran in process with a raised bound;
  (4) the lane lists its own shell-rule breaks (a `for` loop, two piping calls on tool output, one `curl -o /tmp/none.json`
  deleted at once). Recorded as the run's account; not independently verified.
- **Merge without a separate operator word:** the message says the merge "was taken under the lane's merge gate (standing
  authorization), not on a separate operator word" and that the run read the project's "do not merge without the user's word"
  as overridden by the lane's own contract. The epic's earlier landings record the same standing authorization (PLAN-77, 80,
  86, 09), some after an explicit "Merge now" from the operator (PLAN-78). Recorded here as the lane's own reading, not as
  an anomaly of the ledger; the operator may want to confirm that the authorization covers a merge with no question asked.
- Size: 38 files, under the 100-file limit of the required reviewer.

## Routing and Merge Behavior

- Review: CodeRabbit (required, quota-limited) reviewed heads `12bf4612`, `bd70e3a9` and `79af7e40`; the merge head
  `0ccea877` and the final head `1f10f52b` were refused on quota. All threads of the reviews are fixed or answered; two
  findings were answered rather than applied (Generators in `OnboardingScenarioTest`; the disk-measurement year). Sourcery
  (optional) refused on its own budget.
- CI/merge: PR #291 reads `merged`, merge commit `9890d74b`, which is on `origin/main` (checked with `git branch -r
  --contains`); its single parent is `ae0cb6b8` (the ledger land of 2026-10-31).
- Sonar: the landing claims `open_issue_count=0`, `open_issue_rules=none`, read at `1f10f52b`. **Not re-corroborated:**
  `read_open_issues` for PR 291 answers `count_status: undecidable`, reason `head_revision_unavailable`, analysis revision
  `1f10f52b0dffbad8b120bb1997941290d0481efa` — the revision the lane read. Recorded as an unverified lead, not as a `0`.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-16 --status shipped`
- [x] row `pr` stamped — `291`
- [x] row `landing` stamped — `landings/PLAN-16.md`
- [x] row `plan_marshall_plan_id` stamped — `16-web-time-format-and-locale` (from the sender `rb-16-web-time-format-and-locale`)
- [x] epic.md narrative reconciled (Open Defect recurrence, Decisions, Watches)
- [x] resume anchor updated; `queue-view.md` regenerated

## Follow-Ups

- **PLAN-35 is unblocked** (its dependencies PLAN-31, 16 and 76 have shipped); PLAN-45 follows PLAN-35. The web chain
  PLAN-16 → 35 → 45 continues.
- **Re-read at HEAD `418579a0` before emit** every staged spec that names a file this plan changed. A path-level match
  (`git diff` of the merge against the Expected Surface lines) finds: PLAN-43 (`SystemView.svelte`), PLAN-45 (`client.ts`),
  PLAN-47 (`SystemView`, `SettingsView`), PLAN-51 (`DecisionView`, `client.ts`, `runtime.ts`), PLAN-52 (`OverviewView`),
  PLAN-53 (`SourceList`), PLAN-54 (`ReportsView` and its test, `CompletionReportView`), PLAN-55 (`PlanView` and its test,
  `NodeNarrative`), PLAN-58 (`EpicView`), PLAN-59 (`ProjectExplorerView`, `ProjectOnboardingView`), PLAN-60 (`SettingsView`,
  `MachineSettingsView`, `onboarding.spec.js`), PLAN-62 (`PlanView`, `NodeNarrative`), PLAN-64 (`OnboardingView`,
  `SettingsView`, `onboarding.spec.js`), PLAN-66 (`OnboardingView`, `ModelsChoice`, `onboarding.spec.js`), PLAN-73
  (`text-guard-baseline.json`), PLAN-83 (`PlanView`, `EpicView`, `OverviewView`), PLAN-85 (`index.ts`, `RevokedView`),
  PLAN-88 (`ProcessDesigner`); and by directory declaration PLAN-45, 72 and 73 (`lib/views/`), PLAN-72 (`lib/components/`).
  Every view plan therefore formats its server values through `display/format.ts` and reads `i18n/index.ts` as changed.
- **Residue the run named, carried as a Watch:** the framing words and unit marks of `format.ts` live in code, not in the
  catalogue (a follow-up adds `common.time.*` keys); `relative` renders the age only (the design's `since 14:02` row has no
  rendering); the activity day header exists only where the wire dates a row (the mock's `running-work` activity sends a bare
  clock); `project.time.*` keys are unused in both clients; `PrAdoptPanel`'s `reset_at` renders as it arrives;
  `formatTokens` returns `0`, not `≥ 0`, for a zero floor.
