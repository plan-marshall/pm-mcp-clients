envelope_version=1
sender_type=plan
sender_id=rb-152-web-foundation-catalogue-cleanup
epic=ui-improvements
kind=landing
created=2026-11-17T23:58:51Z
revision=1
amended=2026-11-17T23:59:43Z

```landing-facts
schema=landing-facts/1
plan_id=rb-152-web-foundation-catalogue-cleanup
epic=ui-improvements
pr=#468
merge_state=merged
cleanup_owed=false
deliverables_total=5
deliverables_done=5
total_tokens=0
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=0.1.2149
executor_version=0.1.2149
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=1
review_body_findings_handled=1
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```
guard: none on this harness (unenforced)

# PLAN-152 — Web Foundation: The Catalogue Clean-Up (D1 Foundation, Part 6)

Merged as **#468**, merge commit `e9aef9a6` (squash). One review finding of that pull request landed afterwards in the follow-up **#476**, merge commit `5cb2a3e4` — see section 9 for why the merge ran ahead of the last review read.

## 1. Deliverables

| Deliverable | Done-when | Verdict | Order | Test / artifact | Commit |
|---|---|---|---|---|---|
| D1 Shared acts | grep over `pm-web-app/src` + `pm-api/src/main` empty, catalogue test excluded | met | none | `pm-api/src/main` no hit; `pm-web-app/src` names the retired keys only in `catalogue.test.ts:29-45` (absence assertion) | `b8c16083` |
| D1 | catalogue tests pass; the web test fails on a put-back key | met | mutation-after | mutation `web.common.back` put back → `catalogue.test.ts` "retires every duplicate act key…" fails `expected [ 'web.common.back' ] to deeply equal []`, `.../build-results/pm-web-app/maven-2026-11-17-150057.log`; `MessageCatalogueTest.shouldRetireTheDuplicateActKeys` fails on the same key, `.../default/maven-2026-11-17-145841.log`; reverted in `b8c16083`; green in every later gate | `b8c16083` |
| D2 Kinds, filters, reasons, states | each removed key has no reader in web/api/operator | met | none | the retired keys (`overview.find.kind.*`, `overview.needs.operator_wait`, `decisions.filter.label`, `report.filter.label`, `decision.waiver.later`, `overview.kind.project`) appear only in the absence assertions of `catalogue.test.ts`, `MessageCatalogueTest`, `OverviewCatalogueTest`, `ReportCatalogueTest`, `DecisionCatalogueTest` | `b8c16083`, `5bc74c72` |
| D2 | Java catalogue tests pass, both locales equal | met | test-first | failing at `f7da05c7`: 9 failures — `shouldCarryTheSharedActs[1..4]`, `shouldRetireTheDuplicateActKeys`, `OverviewCatalogueTest.shouldNameTheKindsAndTheOneWaitWordOnly`, `ReportCatalogueTest.shouldUseTheSharedFilterLabel`, `DecisionCatalogueTest.sharedLaterAct`, `.sharedFilterLabel`, `.../default/maven-2026-11-17-140723.log`; passing in the green gate runs | `b8c16083` |
| D2 | key list for the terminal package in PR body and inbox message | met | none | PR #468 § "Keys that stay for the terminal package", repeated in this message | `b8c16083` |
| D3 Deferred reload | `index.test.ts`: no request, locale kept, applied on release, cached locale at once | met | test-first | failing at `f7da05c7`: `expected "getMessages" to not be called at all, but actually been called 1 times` for `["de","all"]` and for the area pack, `.../default/maven-2026-11-17-133909.log`; passing in the green gate runs | `54e4ec40`, `a00cedfe`, `d6a4f41e`, `282ab891`, `5cb2a3e4` |
| D3 | `SystemView.test.ts`: state set while waiting, cleared when sent or released | met | test-first | same failing run (both store tests fail at base); passing in the green gate runs | `54e4ec40` |
| D4 Documents | no removed key under `doc/design` | met | none | grep over `doc/design` empty for every retired key | `9689cde9`, `5bc74c72`, `d6a4f41e` |
| D5 Browser evidence | `reports-and-system` and `running-work`, 1440×900 and 390×844, `en` and `de`, both themes, screenshots kept | met | none | 33 PNGs under `.plan/temp/rb-152-web-foundation-catalogue-cleanup/screens/` (16 `reports-and-system_*_{reports,system}`, 16 `running-work_*_{running,palette}`, plus `reports-and-system_1440x900_en_light_deferred_reload.png`); text-guard baseline untouched by this plan | no code change |
| Amendment 2026-11-17 `<html lang>` | unit test pins `de` → `en`, pairing first load carries `de` | met | test-first | failing at `f7da05c7`: `expected '' to be 'de'` (`index.test.ts:572`) and the pairing-page test, same web log; passing in the green gate runs; live: `documentElement.lang` = `de` on the German pages | `54e4ec40`, `d6a4f41e` |

Per-head test runs: every head pushed to #468 was covered — `mvn test` at `8c4c89f4` (Java tests only, planned red), the base-code tree runs for the test-first evidence, and green `verify -Ppre-commit` runs at the content of `b8c16083` (before the PR), of `d6a4f41e` (`.../default/maven-2026-11-17-161338.log`), and of `b1d4f4bc` (`.../default/maven-2026-11-17-212604.log`); `verify -Ppre-commit` green at `6c37bb8b` and `908b7a54` on #476; Playwright `-Pe2e` green at the content of `b1d4f4bc` (`.../pm-web-e2e/maven-2026-11-17-205240.log`).
Base-sha evidence: `worktree-create --detach` returned `executor_in_worktree: absent`, so the base-sha recipe was not usable; the test-first runs above come from a scratch branch worktree at `f7da05c7` with the plan's committed tests copied in (removed after use, tracked files restored first).

## 2. Personas and skills

- D1 and D2: `plan-marshall:persona-implementer`; `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`, `pm-dev-java:junit-core` (declared).
- D3: `plan-marshall:persona-implementer`; `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`.
- D4: `plan-marshall:persona-documenter`; `pm-documents:ref-asciidoc`, `pm-documents:ref-documentation`.
- D5: `plan-marshall:persona-integration-tester`; `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`.
- Every self-review round and the PR triage: `plan-marshall:persona-code-reviewer`.
- Skills added for surface drift: none — every touched file is a web reader, a message pack, a catalogue test or one of the spec's documents; `pm-operator` was not edited.

## 3. Hypotheses

- "The fetch of a missing pack during a deferred reload is reachable through `setLocale` and the first open of a view, and through nothing else" — **refuted** for the view path: no view calls `loadMessages(area)`; the fetch is reachable through `setLocale` (`pm-web-app/src/lib/i18n/index.ts:543`), `adoptRecordedLocale` (`:651`) and `initI18n` (`:741`). The rule therefore guards `loadMessages`, `setLocale` and `loadCatalogueFor`.
- "The states part of deliverable 2 touches fewer than ten keys once the `pm-operator` readers are excluded" — **confirmed**: no web reader of `overview.state.*` exists, so nothing moved and the six keys stay (`overview/OverviewFormatting.java:224-232`, `planview/PlanViewFormatting.java:85-87`, `StatusCommand.java:366-368`, `system/SystemView.java:322`).
- Claim 0 (the act keys' readers) — **confirmed at HEAD**: 20 web files, no `pm-api`/`pm-operator` reader; `web.common.retry` was already merged before this plan.
- Claim 1 (kinds) — **confirmed with a correction**: `EpicView`/`OverviewView` no longer read `overview.kind.*`, so only the find list moved; four `pm-operator` readers keep `overview.kind.*`, and `overview.kind.project` had none and was removed.

## 4. Findings

- Self-review round 1 (sub-agent, persona-code-reviewer): 9 findings — 2 should-fix (`overview.kind.project` orphaned; doc example contradicting the shipped filter label) and 7 nits (wording, stale docstrings, pending-choice supersede, deferral release order, area handling). All fixed in `5bc74c72` and `a00cedfe`.
- Self-review round 2: all round-1 findings closed, no regression; 3 new nits (two behaviours unpinned by tests, a test title, one doc wording) — fixed in `d6a4f41e`; round 2 reported nothing else open.
- CodeRabbit review of `d6a4f41e`: inline — `loadMessages` could write a response that resolved during the deferral (`coderabbit inline inline-1`) → fixed in `282ab891`, thread replied and resolved. Outside the diff — `loadCatalogueFor` fetched a cached pack when the version was unknown (`coderabbit outside-diff outside-diff-1`) → fixed in `282ab891`.
- CodeRabbit review of `b1d4f4bc`: outside the diff — a locale switch started before the deferral could publish and cache the newer pack (`coderabbit outside-diff outside-diff-2`) → fixed in #476, merge commit `5cb2a3e4`; answered on #468.
- CodeRabbit review of the follow-up #476 (`6c37bb8b`): no actionable comments.
- CI: one failing job on #476 — `EntryRecordsTest.runtimeEntryResourceRefusesAMalformedLegacyPort` failed on a generator seed in a module this plan does not touch; the job was re-run and passed (flake, not this plan's).

## 5. Reviewers

- **coderabbit** (required, quota-limited, review-complete-after-handling): refused on review quota at 2026-11-17T16:26Z (no qualifying review then). Re-triggered after the 90-minute window (2026-11-17T18:44Z, comment 6957512154); reviewed `d6a4f41e` (COMMENTED, marker `kind=reviewed`, findings handled), then reviewed `b1d4f4bc` automatically (1 finding, handled in #476), then reviewed `6c37bb8b` on #476 (no findings, marker `kind=reviewed`, merge risk minimal). Head reviewed by the last qualifying review: `6c37bb8b`; the #468 line is the done-exemption case.
- **sourcery** (optional): refused on quota both times (1 day 19 hours, then 21 hours) — not gating.

## 6. Operator questions

1. "CodeRabbit refused on quota — how should I proceed?" — options {wait 90 minutes then re-trigger, stop with the PR open}; **selected: wait 90 minutes, then re-trigger**.
2. "PR #468 merged a few minutes earlier than my gate check; one finding of the last review is not on main — how should I proceed?" — options {land the follow-up fix now, report it as residue}; **selected: land the follow-up fix now**.

No free-text operator messages were exchanged.

## 7. Rule check

- `guard: none on this harness (unenforced)` — the harness has no tool-call guard, so nothing enforced the lane's shell and file rules; this section counts the run's own actions.
- Shell file operations the run issued (read class included): `ls` 6× (worktree listing, scenarios, routes, screens, package dirs), `cat` 1× (`pm-api/.../NeedsOperatorKind.java`), `grep -n` 2× (mock runtime control endpoints, the `PATH_BROADCAST` constant), `cp` 8× (four Java tests, two web tests and the store into the evidence tree; the PR body into the edit slot), `rm` 2× (the evidence tree's untracked store copy and owner marker), and one redirect (`git show … > /dev/null`, a no-op read). Each is a break of the "file tools, never shell" rule; none changed a repository file outside `.plan/temp`.
- Git: `git add` by path, `git commit`, `git push`, `git fetch`, `git merge` (twice per base move), `git rev-list`, `git merge-tree`, `git merge-base`, and `git switch -c fix/152-deferred-reload-race origin/main` for the follow-up. Also `git merge --abort` once (to re-run the merge with `-X theirs` after the decisions view conflict). No `stash`, `restore`, `checkout -- <path>`, `rebase`, forced push or `branch -D`.
- Builds: all through `plan-marshall:build-maven:maven run`; every executor call in the form `python3 .plan/execute-script.py …` from the main checkout.
- `.plan/` state: only `.plan/temp/` (progress, screenshots, landing, PR-body draft), the `ci` body slots under `.plan/local/plans/NO_PLAN/work/ci-bodies/`, and the evidence tree's own `.plan/local/base-evidence-owner` marker were written; the spec and the ledger were not edited.
- Outside the repository root the run read the daemon's build logs under `~/.plan-marshall/marshalld/job-logs/` (the paths the build results name) — no credentials, no config dumps.
- Restarts: none. Killed sub-agents: none. Re-dispatches: the self-review sub-agent ran two rounds (round 2 as a follow-up on the same session).
- Spec read: `spec_source=ledger_worktree`, no `spec_differ` marker at setup and at landing time.
- `executor-skew`: `match` (executor 0.1.2135, lane 0.1.2135). At landing time the executor reports 0.1.2149 — the executor file was regenerated by something outside this run during the day; every call succeeded.
- Retired rule break worth naming: the operator's screenshot matrix was captured with a textarea appended through the page's own JS to reach the deferred-reload state, and the language-toggle check in that state was read from the network panel — both recorded as live evidence, neither is a substitute for the unit tests.

## 8. Build server

- Setup preflight: `ready` (project enrolled, daemon reachable), version "1".
- Mechanisms: every build ran `daemon_longpoll` (routed), no `in_process_fallback`.
- Fallbacks and retries, one entry per build that did not finish first time:
  - quality gate at `c44a2f1`-era tree: daemon bound expiry `timeout` (1018 s, source `learned`), re-submitted with `--timeout 1093` (green, 790 s).
  - quality gate at `d6a4f41e`: bound expiry `timeout` (1093 s, source `explicit`), re-submitted with `--timeout 1500` (green, 1043 s).
  - `killed` jobs: none.
- `runtime_stamp_reason`: none. `plugin_version` is the version the executor's install stamp carries for every installed harness (`0.1.2149`, `.plan/execute-script.py:119-123`), the same version the landing-time `resolve` reported as `executor_version`; the setup-time resolve reported no plugin version. `total_tokens=0` is the measured-subset total: no phase of this run measured tokens, so the sum of what was measured is zero.

## 9. Residue

- **The merge ran ahead of the last review read.** `ci pr merge-queue` answered `enqueued: indeterminate — auto_merge_armed_awaiting_checks`, so GitHub's auto-merge fired as soon as CI went green on `b1d4f4bc`; CodeRabbit's third review (published 21:14 on that head) carried one finding that was therefore not handled before the merge. The operator chose to land it as the follow-up #476 (`5cb2a3e4`), which is green and merged. A later lane run should treat an `indeterminate` enqueue as "armed auto-merge", re-read the review surfaces after it, and not treat the enqueue answer as the gate.
- The amendment's instruction "add `pm-web-app/src/app.html` and its test to the Expected Surface" is a spec edit; the lane's Write-Boundary forbids it. The orchestrator owns that row.
- `pm-operator` still reads `overview.kind.epic|plan`, `overview.state.*` and `overview.needs.waiver|waits_for_you`; the terminal package inherits them (the list is in section 1 and the PR body).
- The `chore/152-evidence` scratch branch was pruned; its worktree was removed before the landing. No worktree remains.
