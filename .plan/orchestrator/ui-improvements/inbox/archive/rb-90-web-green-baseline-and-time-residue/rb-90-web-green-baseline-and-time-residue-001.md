envelope_version=1
sender_type=plan
sender_id=rb-90-web-green-baseline-and-time-residue
epic=ui-improvements
kind=landing
created=2026-11-02T10:09:37Z

```landing-facts
schema=landing-facts/1
plan_id=rb-90-web-green-baseline-and-time-residue
epic=ui-improvements
pr=#304
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=3
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2036
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

## Deliverables

### D1 — the e2e suite on `main` is green (met)

| Done-when | Verdict | Order | Test | Failing run | Commit |
|---|---|---|---|---|---|
| the base failing names recorded first with the log path, each fault classified code-or-test wrong with the design sentence | met | `test-first` | the seven e2e names | `pm-web-e2e/maven-2026-11-01-194722.log` — `7 failed`, `308 passed`, the seven names at lines 1168-1175 (base `be4995e5`) | 5bcf13a |
| `PM_MOCK_WEB_PORT=20900 PM_WEB_PORT=20901 ./mvnw -pl pm-web-e2e verify -Pe2e` reports no failure | met | `none` | the e2e suite | — | green at the merge head `c5469b41`: `maven-2026-11-02-093830.log:873`, `333 passed` |
| browser check (`running-work`): epic token split and plan-view process tree, `de`/`en`, baseline and after | met | `none` | Playwright MCP | — | before: `waiting_for_operator`; after: `waiting for you` / `wartet auf dich`; token split `≥ 3.86 M (≥ 3.52 M in · 0.34 M out)` |

Classification: the epic token split and the three plan-view journeys and the showroom journey were stale tests (`texts-and-languages.adoc:221` — the split takes its total's unit; `plan-overview.adoc:103` — the runtime serves those that need the operator first and a client does not sort again). The two text-guard failures were code wrong: `getNodeStateText` printed the runtime's `waiting_for_operator` raw (`plan-view.adoc:52`, the display layer's rule).

### D2 — the time and unit residue of PLAN-16 (met)

| Done-when | Verdict | Order | Test | Failing run | Commit |
|---|---|---|---|---|---|
| `format.test.ts` one case per form (framing word, unit mark, `since`, day header), each failing before | met | `test-first` | `format.test.ts` | `maven-2026-11-01-201135.log:261-269` — 6 failed (since form, framing word, since word, day words, clock day header, unit mark) | bb41043 |
| browser check (`running-work`): overview, plan view and activity list, `de`/`en`, no raw key, no ISO time | met | `none` | Playwright MCP | — | one `today` / `heute` header per day; no key, no ISO |
| open-issue read at the pull-request head recorded | met | `none` | SonarCloud | — | `0` confirmed at the merge head `c5469b41` (`sonar-gate merge_allowed: true`) |

### D3 — the words of the system view's model column and its load failure (met)

| Done-when | Verdict | Order | Test | Failing run | Commit |
|---|---|---|---|---|---|
| `SystemView.test.ts` one case per word (retry, error, ok in `de`) and one for the load failure, each failing before | met | `test-first` (load failure) / `mutation-after` (the three words) | `SystemView.test.ts` | load failure: `maven-2026-11-01-202101.log:477`; the three words against the reverted markup: `maven-2026-11-02-220552.log:285,488,691` | 1ec49a3, d63dc11 |
| browser check (`fully-onboarded`): `/system`, models table, `de`/`en`, baseline and after | met | `none` | Playwright MCP | — | `1 retry`/`ok`/`1 error` in `en`, `1 Wiederholung`/`ok`/`1 Fehler` in `de` (baseline: English literals on the German page) |

The `ok` case asserts a changed pack (`system.models.ok=einverstanden`), because the literal and the pack carry the same word: only a changed pack proves the view reads it.

## Personas and skills

- D1: `plan-marshall:persona-integration-tester`; skills `pm-dev-frontend:javascript`, `css`, `jest-testing`, `javascript-security`.
- D2 and D3: `plan-marshall:persona-implementer`; the same four domain skills.
- Self-review rounds: `plan-marshall:persona-code-reviewer` (three rounds, general agent, report-only).
- skills added: none — the surface stayed inside the spec's Expected Surface plus `lib/utils/processNode.ts` (the fix for one of the seven named failures).

## Hypotheses

- "the `epic-view.spec.js` mismatch is PLAN-16's formatter choosing `M` for 340000, so the test or the number rule is stale" — confirmed: `format.ts` (formatSplit takes the total's unit) and `texts-and-languages.adoc:221`; the test was stale.
- "the `plan-view.spec.js`, `showroom.spec.js` and text-guard failures are older breadcrumb and text drift, not one cause" — confirmed as two causes: positional plan selection (`plan-overview.adoc:103`) and the raw node state.
- "`project.time.*` exist and are unused" — confirmed (no reader in either client); removed.

## Findings

Self-review round 1: duplicate day header for a clock-only row followed by a dated row of the same day (fixed, `dayOf`); three comments that had become false (fixed); German cross-midnight ages unasserted (restored); zero-duration case read the wrong pack (fixed); one case per word (split).
Round 2: the duration comment still implied a shared token mechanism (fixed); three wrapper JSDoc lines (fixed); the `ok` case could not fail before (fixed with the pack override + a mutation-after run).
Round 3: an undated live row reopened the day header (fixed); the clock-only rule had no design sentence (added to `texts-and-languages.adoc`); the German models-table screenshot missed the table (re-captured); the `since` wording on completed-point cells — the operator kept the design's wording.
CodeRabbit (3 reviews): future instant took the `since` form (fixed); clock rows dated by the render clock (fixed, then refined); `ok` fallback (refuted: the catalogue guard forbids an inline fallback, thread answered and resolved); row-scoped status assertion (fixed); language-switch status test (fixed); streamed clock row after midnight (fixed); dated rows measured on the frozen clock (fixed); clock not moved with an arriving row (fixed); plural word untested (fixed); the streamed-row test could not distinguish the read day (fixed).
Sourcery: refused on its own weekly budget (optional reviewer, not gating).

## Reviewers

| Reviewer | Required | State | Head reviewed | Trigger and wait |
|---|---|---|---|---|
| coderabbit | yes (quota_limited) | reviewed | `c41df5c`, `dc52c617`, `88846137` — each with the `final_review_risk_coverage` marker (`kind=reviewed`) | triggered at 06:14, 06:37, 07:22, 08:03, 08:24, 09:5x; refusals on quota recorded at 06:38, 08:03, 08:28; the qualifying review for the standing authorization is `88846137`, every finding of it handled and every thread resolved |
| sourcery | no (optional) | refused | — | triggered at 06:14; refused on its own weekly budget (next 15 h 28 min) |

**Standing review authorization:** the merge was enqueued on a qualifying review of `88846137` (a head of this PR), with every finding of it fixed or answered, every one of its threads resolved, a re-trigger after each later push (refusals recorded), and every other gate condition green on the merge head `c5469b41`.

## Operator questions

1. "How should the `since` ripple be handled?" — options: keep the design's wording / confine `since` to start cells / leave it to the orchestrator. Selected: **keep the design's wording**.
2. "The self-review reached its third (last) round with items open — continue?" — options: proceed to the PR / run a fourth round. Selected: **proceed to the PR**.
No free-text operator messages.

## Rule check

guard: none on this harness (unenforced) — the run's own actions are counted below, not a guard log.

- **A file written outside the worktree (and reverted).** The four message packs (`common.{en,de}.properties`, `projects.{en,de}.properties`) were first edited in the main checkout instead of the worktree. The `git restore` family is forbidden here, so they were reverted by hand to their exact bytes (`git status` empty again) and the edits redone inside the worktree. This is the run's one repository-write rule break; nothing reached a commit from the main checkout.
- **A gate and an e2e run in parallel.** The routed `verify -Ppre-commit` and the in-process e2e suite ran at the same time once; the gate's `npm run build` and recompile broke the e2e dev server, so that e2e run failed at 08:53 (`maven-2026-11-02-085327.log`). The run was repeated alone and is green (`maven-2026-11-02-090717.log:867`). Sequencing, not code — recorded because the run issued both.
- **Shell file operations issued by the run:** the two `lsof`/`curl` checks (ports and the mock's scenario switch) and the background `start.sh`/`stop.sh` calls; no `cat`/`ls`/`grep`/`find`/`head`/`sed` on repository files.
- **Routed builds:** all of them were waited on in-call; none backgrounded; one at a time except the case above.
- **Every operator message:** the two questions above and the model switch (none).
- **Spec read:** `spec_source: ledger_worktree`, `spec_differ` not reported.
- **executor-skew:** `match` (executor 0.1.2036 / lane 0.1.2036) — nothing asked, nothing downgraded.
- **Restarts / re-dispatches:** three review rounds (new agents), no killed sub-agent, no harness restart. The plain `verify` (the resolved `build.verify`) ran twice, both green: at `c73d9c4` (job `c6987eb44c4341fe82d6816f602c2af0`) and at `c41df5c` (job `df55fd7d50514b7eba85309cbdc13ea3`). At the later heads, including the merge head `c5469b41`, the **pre-commit gate** ran instead — the project's own quality-gate canonical, `verify -Ppre-commit`, which runs the same lifecycle with the quality plugins added; it is what the resolve result's `build.quality-gate` names. The plain `verify` was therefore not re-run separately at the final head (see Residue).

## Build server

- setup preflight: `ready`; routing `auto`, `executor_in_worktree: present`.
- Every gate ran routed (`mechanism=daemon_longpoll`), waited in-call: 654 s, 518 s, 1167 s, 989 s, 826 s, 1022 s, 1096 s, 906 s — all success. One routed submission first failed instantly with a relative `--project-dir` (`build_failed`, no structured errors); the same command with an absolute path ran normally.
- `runtime_stamp_reason`: none; `plugin_version` resolves as `unknown` (nothing measured it), `executor_version` 0.1.2036.

## Residue

- The `since 14:02` form now renders on cells that name a completed point (Ended, last used, last active, created) for a value of today. The operator kept the design's wording (`texts-and-languages.adoc:149-150`, `:180`); if the epic wants different words for those cells, it is a design change.
- The token marks and split labels of `format.ts` are still literals (`k`, `M`, `Tsd.`, `Mio.`, `in`/`out`); the design names `common.tokens.*` for them. D2's sentence is scoped to `common.time.*`.
- The formatters' `locale` argument does not select the catalogue words (the active store does); every production caller passes `$currentLocale`, so it is latent.
- `project.explorer.last_active={duration} ago` and `project.time.*`-style keys that no view reads remain in the packs; only the ones this plan named were removed.
- `origin/main` moved three times during the run (PLAN-35, PLAN-43, the ledger); each was merged and the gate re-run before the enqueue.
- The merge head carries the pre-commit quality gate green (`verify -Ppre-commit`, job `26d5dc3b85eb4cf1bef0eeea781239a5`, 906 s) and the e2e suite green, but not a separate plain `verify` run; the profile runs the same lifecycle with the quality plugins added, so nothing of `verify` was skipped.
