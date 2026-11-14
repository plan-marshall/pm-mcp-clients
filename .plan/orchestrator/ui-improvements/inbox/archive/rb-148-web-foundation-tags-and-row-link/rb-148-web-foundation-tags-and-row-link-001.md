envelope_version=1
sender_type=plan
sender_id=rb-148-web-foundation-tags-and-row-link
epic=ui-improvements
kind=landing
created=2026-11-13T22:15:40Z

```landing-facts
schema=landing-facts/1
plan_id=rb-148-web-foundation-tags-and-row-link
epic=ui-improvements
pr=#427
merge_state=merged
cleanup_owed=false
deliverables_total=6
deliverables_done=5
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2118
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=8
review_body_findings_handled=8
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
```

# PLAN-148 — Web Foundation: ReasonBadge, PhaseLabel, Marker, RowLink and the tag rules

Merged as #427 into `main` at `251374456ae470dc0dc8c41e55290b560b3faa21` (merge-queue merge of
`8f926e04`), after the branch was merged onto `a139ec62` (PLAN-133) and `98cea294` (#426). Guard:
none on this harness (unenforced).

## 1. Deliverables

Six deliverables: D1–D5 of the spec and D6 from the 2026-11-13 amendment. Five are fully met; D4 is not,
because one Done-when carries the wrong evidence order.

**D1 — `ReasonBadge`** — met.
| Done-when | Order | Verdict | Evidence |
|---|---|---|---|
| the 13 reasons in en/de, the count form, both tones, an unknown reason | `test-first` | met | red at `6e8c29af`, `build-results/pm-web-app/maven-2026-11-13-094303.log` (import unresolvable); green at the final head, `ReasonBadge.test.ts` (14 tests) |
| the two contrast pairs in `tokens.test.ts`, both themes | `test-first` | met | same red; green `tokens.test.ts` (90 tests) |
| `OverviewCatalogueTest` with the new key in both locales | `test-first` | met | red `build-results/pm-api/maven-2026-11-13-094103.log`; green at the final head |

**D2 — `PhaseLabel` and `Marker`** — met.
| Done-when | Order | Verdict | Evidence |
|---|---|---|---|
| six phases, role form, no wire token for an unknown phase, three marker kinds, no brand colour | `test-first` | met | red `build-results/pm-web-app/maven-2026-11-13-095547.log` (components absent); green at the final head (`PhaseLabel.test.ts` 11, `Marker.test.ts` 6) |
| the Java catalogue tests with the new keys; `StateWordsCompletenessTest` still passes | `test-first` | met | red `build-results/pm-api/maven-2026-11-13-094839.log` (13 failures); green at the final head |

**D3 — `RowLink`** — met.
| Done-when | Order | Verdict | Evidence |
|---|---|---|---|
| one anchor, controls outside it, `aria-current`, ring/hover on the row, title navigates, nested button does not | `test-first` | met | red `maven-2026-11-13-095547.log`; green (`RowLink.test.ts` 34) |
| the authority table of `Link.test.ts` draws no anchor | `test-first` | met | same red; both tests import one table, `src/test/appPathAuthorityForms.ts` |
| `ExternalLink.test.ts` refuses the added forms, the existing table still passes | `mutation-after` | met | pre-fix red `maven-2026-11-13-095416.log`; fix `7fd88249`; reverted-guard run job `1ce868bc025c4973a67131a3bbd158da`; restored green |
| the row fixture's menu opens and does not navigate | `none` | met | `screens/d3-row-menu-open-1440-en.png`, path stayed `/dev/components` |

**D4 — the agreed rules** — not fully met.
| Done-when | Order | Verdict | Evidence |
|---|---|---|---|
| every listed edge asserted, each fails on a revert | `mutation-after` | met | reverted-component run job `e114f483045f4274a9530382027a82a4`, `maven-2026-11-13-100949.log` (11 failures) |
| `tokens.test.ts` asserts the new values and the three contrast rules, both themes | `test-first` | **not met as written** | the assertion and the value were written together; the only red is the reverted-value run (mutation-after) |
| the `--color-success-subtle` places looked at in a browser, both themes, none loses contrast | `none` | met | `/projects` (7), `/overview` (2), `/decisions` (4), `/plans/fix-relay` (1): min 4.78 light / 5.95 dark; 8 screenshots `screens/d4-*-1440-{light,dark}.png` |

**D5 — fixtures and browser evidence** — met.
| Done-when | Order | Verdict | Evidence |
|---|---|---|---|
| the PLAN-147 groups and the mascot row; `Link`/`ExternalLink` 44 px at 390 and 24 px at 1440; console from load | `none` | met | measured at both viewports; console 0 errors from load; 8 screenshots `screens/d5-dev-*`, `screens/d5-tags-*` |
| the page at 1440×900 and 390×844, de and en, both themes, screenshots kept | `none` | met | the same eight, re-taken after the last visual fix |

**D6 — the e2e chat window (amendment)** — met.
| Done-when | Order | Verdict | Evidence |
|---|---|---|---|
| `-Pe2e` passes at the head and the PR lists every fixme with its owner | `none` | met | `build-results/pm-web-e2e/maven-2026-11-13-154005.log` (394 passed, 76 skipped, 0 failed) and again at `8f926e04`; the fixme is `showroom.spec.js` "opens the start entry's refinement chat" → PLAN-161 |

Per-head test runs at the pushed heads: `8fd6482d` (pm-web-app 1888, pm-api 1077), `5816320d`
(pm-web-app 1893, pm-api, e2e 394), `8f926e04` (whole reactor green, e2e 394).

## 2. Personas and skills

- `plan-marshall:persona-implementer` for D1–D4 (production components), `plan-marshall:persona-integration-tester` for D5 and D6, `plan-marshall:persona-code-reviewer` for the three self-review rounds and the triage of the review findings.
- Per deliverable, the declared skills were loaded as standards: `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing` (D1–D5), `pm-dev-java:junit-core` (D1, D2). D6 names no skills; `skills: none named`.
- No skill was added for surface drift.

## 3. Hypotheses

- "the roles the runtime sends as asking role are a closed list that an enum or `OperatorRolesCatalogueTest` already names" — **refuted**: `DecisionEnvelope.java:57` carries `askingRole` as a free string and `OperatorRolesCatalogueTest` names no asking-role enum; the shipped scenarios send six values (`running-work.json:2343,2428,2519,2607,2695,2737`).
- "the phase icons are drawn by `WorkflowIcon` from the ids `phase.<phase>`" — **confirmed**: `WorkflowIcon.svelte:5-16` and `workflow-visual-language.adoc:430-488`.

## 4. Findings

- Self-review round 1: 11 findings, all fixed in `c4d9d89a` (pointer/hover only on an opening row; `Marker` naming; the superseded spelling no longer painted red; two false guard comments; a mangled test line; unused `data-reason`; two design surface lines).
- Round 2: 16 findings, fixed in `5bc49a61` and `2f500c01` (one-line title with ellipsis; hover never replaces the selected ground; the stretched target only on the anchor; an unknown reason stays in the attention tone even with an explicit tone; the `ExternalLink` comment corrected; the authority table shared; the row fixtures take the theme block's width).
- Round 3: the two new items fixed in `8fd6482d`; **F-01 stays open** (D4's token Done-when evidence order) and the state is `budget-exhausted`, never clear.
- CodeRabbit: 8 findings on `0204d229`, each replied and resolved — 5 fixed in `ee17e45e` (leading C0 controls refused before trimming; `MARKER_WORDS`, `PHASE_WORDS`/`ROLE_WORDS` and the reason list derived or test-guarded; the tags-slot chip lifted), 3 declined with the stated reason that a contract expectation stays literal. The replies were read back on the PR.
- CI: nothing failed. The merge queue refused the PR for ~1.5 h (auto-merge armed, no queue entry) until a new head was pushed after the second base merge; the queue then admitted it at position 1 and merged it.

## 5. Reviewers

| Reviewer | Required | Outcome | Head | History |
|---|---|---|---|---|
| coderabbit | yes | reviewed | `0204d229` (qualifying review, merge risk Low, `kind=reviewed`) | triggered at creation (rate-limited), reviewed `0204d229`; re-triggered after each push (rate-limited or "already reviewed"); its 8 findings all handled, `review-gate` `all_handled` |
| sourcery | no | refused on quota | — | triggered once; refused ("review budget of 250,000 diff characters for the last 7 days"); optional, so not gating |

Standing review authorization: coderabbit is marked `review_complete_after_handling`; its review of
`0204d229` is on record, every finding is handled, and the later heads were re-triggered.

## 6. Operator questions

1. "The self-review has reached its 3-round limit with one item still open that no further round can close: … How should the run proceed?" — options "Continue and report D4 not fully met" / "Stop before the pull request"; selected **"Continue and report D4 not fully met"**.
2. "Every gate the lane names holds on the head 5816320d … the pull request does not enter the merge queue … How should the run proceed?" — options "Merge directly with `ci pr merge`" / "Stop and leave the pull request open"; selected **"Merge directly with `ci pr merge`"** (the provider then refused: the branch carries a required merge queue).
3. "The direct merge you chose is refused by the platform … How should the run proceed?" — options "Stop and leave the pull request open" / "Keep polling the merge queue"; selected **"Keep polling the merge queue"**.
4. "Polling cannot converge … How should the run proceed?" — options "Update the branch, then poll again" / "Stop and leave the pull request open" / "Keep polling anyway"; selected **"Update the branch, then poll again"**.

No free-text operator messages beyond these answers.

## 7. Rule check

- Every script went through `python3 .plan/execute-script.py …`; every build through the resolved `build` commands; every CI action through `ci`. One command whose `--command-args` names `pm-web-e2e` and the plan's ports (`-Pe2e`, `-Dpm.mock.webPort`, `-Dpm.web.port`) went through the same Maven verb.
- Shell file operations: none for reading or searching (the file tools were used); the only shell writes were `mkdir` for the screenshot directory and one `mv` of three component files during the unfixed-tree evidence run, both under `.plan/temp/`. No `&&`, `;`, pipes or heredocs.
- Guard: **none on this harness (unenforced)** — `resolve` reported `guard: none`, so no `{guard_log}` line exists for this run and this report does not claim one.
- The main checkout was edited once in error (two design pages) and restored to clean with the file tool; every other write was inside the worktree.
- The pre-commit profile rewrites `pm-mock-runtime/src/main/java/de/planmarshall/mock/cli/MockRuntimeCli.java` (arrived on `main` with PLAN-133) into worse indentation; the run reverted that unrelated rewrite each time and recorded it. `f7bfd459` took the one rewrite it produced in this plan's own test source.
- Two merges of `origin/main` (`a139ec62`, then `98cea294`); the second had one conflict in `OverviewCatalogueTest.java`, resolved by keeping the main-side rewrite (keys derived from the document tables) and re-adding this plan's two keys to the explicit value test.
- Spec read: `spec_source=ledger_worktree`, `spec_differ=true` (the main checkout's copy differs) — recorded here because a silent read of the older copy is the failure the worktree-first default exists to remove.
- `executor-skew`: `match` (executor 0.1.2106 and lane 0.1.2106 at the start; the executor later read 0.1.2118).
- One build hit the daemon's supervisory bound: `verify -Ppre-commit` on the merged head at a 757 s timeout came back `timeout` with `timeout_used_seconds: 757, timeout_source: explicit`; it was re-run with a 1500 s bound and succeeded. No launch fault, no degraded routing.

## 8. Build server

- Setup preflight (`ready`), project enrolled. Every build was routed and ran `daemon_longpoll`; no `in_process_fallback`, no degraded routing, no killed job.
- Counts: `verify -Ppre-commit` 6, `test` 5, `verify` 7, `-Pe2e` 2, module-scoped `pm-api`/`pm-web-app` runs 12 — all green except the one bound expiry above.
- `runtime_stamp_reason`: none. `plugin_version` is `unknown` in the stamp.

## 9. Residue

- D4's token Done-when: the assertion was written with the value, so no `test-first` artifact exists; the operator chose to report it rather than rework. The product and both themes are measured correct.
- `doc/design/shared/work-actions.adoc`, `plan-overview.adoc`, `terminal-client/04-console.adoc` and `work-actions-review-texts.adoc` still spell the hold `integrity_hold`; the planned work package `doc/plans/harness-decision-log-hold-names.adoc` owns that rename, and the two documents D-2 named are corrected.
- `StatusLabel`'s `slot` is opt-in; the lists that adopt it are PLAN-49, 51, 52, 55 and 62.
- `showroom.spec.js`'s chat step stays marked for PLAN-161.
- `/reports`, `/designer` and `/system` draw no `--color-success-subtle` block under the `running-work` scenario (the System call answers 404 from the mock), so their places are covered by the measured text pairs rather than a screenshot.
- The merge queue took ~1.5 h to admit the PR; the low-risk explanation is that auto-merge was armed on a head whose checks had already completed, and the queue only admitted the PR after a fresh head arrived.
