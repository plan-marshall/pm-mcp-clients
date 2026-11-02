envelope_version=1
sender_type=plan
sender_id=rb-87-cui-testing-mock-runtime
epic=ui-improvements
kind=landing
created=2026-11-01T05:30:46Z

```landing-facts
schema=landing-facts/1
plan_id=rb-87-cui-testing-mock-runtime
epic=ui-improvements
pr=#293
merge_state=merged
cleanup_owed=false
deliverables_total=1
deliverables_done=1
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2036
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# Landing — PLAN-87: pm-mock-runtime tests on generated input

Merged as #293 (merge commit `418579a0`), branch `chore/87-cui-testing-mock-runtime` removed.

## 1. Deliverables

**D1 — pm-mock-runtime tests on generated input** (all three Done-when met, each with evidence order `none`, so no failing-run evidence applies)

| Done-when | Verdict | Test / evidence | Failing run | Commit |
|---|---|---|---|---|
| Count recorded before and after (annotated classes, input-free classes, number of tests) | met | Before (`6372185a`): 45 test classes, 0 annotated, 483 tests. After: 40 annotated, 5 input-free (each with its reason below), 483 tests, 484 after the base merge added one | none required (order `none`) | `f7b0d275` … `7f7249fc` |
| `./mvnw -pl pm-mock-runtime verify` passes, tests not lower | met | Module verify green at the head: `pm-mock-runtime` 484 tests, 0 failures/errors. No test method removed by the diff | none required (order `none`) | `7f7249fc` |
| Open-issue read at the PR head recorded (count + rules, never `0` on a failed read); confirmed `0` or every issue dismissed | met | Read at `7f7249fc`: `count_status: confirmed`, `open_issue_count: 0`, no rule left open; earlier read at `d8cfceaa` showed 7 `java:S1117` issues, all fixed in `5f7792e2` | none required (order `none`) | `7f7249fc` |

Per-head test runs against pushed heads: `483` at `f7b0d275`/`4fefb10f`/`31f0db1f`/`a50539ad`/`d8cfceaa`/`5f7792e2`, `484` at `80634078` and `7f7249fc` (module `test` after each push, full `verify -Ppre-commit` at `d8cfceaa`, `80634078` and `7f7249fc`).

**Input-free classes (5), each with the reason the spec asks for:** `StandardScenariosTest` (scenario constants and resource files are the contract), `scenario/StateVocabularyScenarioTest` (fixture units and catalogue), `junit5/MockRuntimeExtensionTest` (annotation values are compile-time constants and are exactly what the test asserts), `junit5/MockRuntimeExtensionLoggingTest` (the `LogAsserts` test, kept as written), `cli/StartStopScriptsTest` (OS-assigned ports and PIDs).

## 2. Personas and skills

- Persona applied throughout: `plan-marshall:persona-module-tester` (the deliverable's own `Persona:` line), plus `plan-marshall:persona-code-reviewer` for the three self-review rounds.
- Domain skills named by the spec and loaded: `pm-dev-java:junit-core`, `pm-dev-java:junit-integration`, `pm-dev-java-cui:cui-testing`.
- No surface drift: every changed file is a `pm-mock-runtime` test class; `skills added: none`.

## 3. Hypotheses

- `HYPOTHESIS: most mock tests assert scenario fixtures and wire texts, so the share of classes that can use generated input is well below 43` — **refuted**. 40 of 45 classes supply at least one arbitrary value (unknown ids, tokens, labels, ports, delays, free text); only 5 are fixture-only. Reported, and the conversion was not forced past the discriminator: the values that the production resolves or echoes are generated, and every contract value (scenario fixtures, wire names, route paths, error codes, golden texts) stays literal.
- Verify-first: `pm-mock-runtime/pom.xml` still declares `cui-test-generator` and `cui-test-juli-logger` (PLAN-77 landed, no blocker); the test tree re-counted at 45 classes, matching the spec's re-grounding; `NeedsAndFindScenarioTest` (PLAN-82) counts like any other class and was converted.

## 4. Findings

- **Self-review round 1** (7): weakened equal-body coverage in `MockHttpRequestTest` (shared array instance) — fixed with `bytes.clone()`; stale `new-service` comment — fixed; `FixtureConsistencyTest` wrongly listed input-free — converted; generated values in field initialisers — moved to `@BeforeEach`; `pmw_` prefix dropped — restored; collision-prone unknown values — prefixed; `Path.of("/")` — moved under `/tmp`.
- **Self-review round 2** (8): field initialisers re-introduced in `FixtureConsistencyTest` — fixed; stale counts in the record — fixed; gate not run at HEAD — re-run; stale `/unknown` comment — fixed; `pmw_` prefix in the short-flag test — fixed; unprefixed link ids — prefixed; absent-header lookup could collide — made impossible; delay slack — narrowed to the original tolerance.
- **Self-review round 3**: no code defect; `FullyOnboardedScenarioTest` was converted too (its broadcast total was incidental), counts refreshed, gate re-run at the head.
- **CI/Sonar**: 7 `java:S1117` shadowing issues at `d8cfceaa` — fixed in `5f7792e2` (seven tests redeclared the authorization header locally); re-read 0.
- **CodeRabbit** (review of `5f7792e2`, 12 Minor): every finding was a sentinel value that could coincide with a real one (unknown scenario name, legacy aliases, no-match query, check ids, echo/error routes, unknown file-picker root, wrong web bearer, two generated ports against the default 7420) — all 12 fixed in `5965c0d3`, threads replied/resolved.
- **CodeRabbit** (review of `80634078`, 3 Nitpick): scenario/state name draws and runtime-token/device-secret draws could coincide — prefixed in `7f7249fc`; the web listener is now also checked to refuse the runtime token (a new assertion, 484 tests).
- **Sourcery** (optional): refused every trigger, "pull request is larger than the review limit of 150,000 diff characters" — not gate-bearing.

## 5. Reviewers

| Reviewer | Required | Result | Head reviewed | History |
|---|---|---|---|---|
| CodeRabbit | yes | reviewed | `5f7792e2` (03:36Z), `80634078` (04:40Z) — `kind=reviewed`, marker `coveredCommitId` on those heads | triggered 21:18Z, 22:51Z, 00:23Z (escalated `@coderabbitai full review`), 01:55Z — all refused on quota; accepted 03:28Z; re-triggered 04:32Z — accepted, reviewed `80634078`; final re-trigger 04:59Z on `7f7249fc` — refused on quota ("Review rate limited"), 90-minute waits observed between every refusal |
| Sourcery | no | refused | — | triggered 21:18Z; refused: PR larger than 150,000 diff characters |

**Standing review authorization** (CodeRabbit is `quota_limited`): a qualifying review exists on `80634078` (the head immediately before the merged head); every finding from both reviews is fixed (`5965c0d3`, `7f7249fc`) and the 12 threads are resolved; the reviewer was re-triggered once after the last push (`7f7249fc`) and refused on quota; every other gate condition holds on `7f7249fc` (CI green, full `verify -Ppre-commit` green, Sonar 0 confirmed).

## 6. Operator questions

1. Asked: how to handle the `-Ppre-commit` gate's rewrite of 178 unrelated main sources in this worktree (the lane forbids `git checkout -- <path>`; the spec's claim label says to restore them). Options: restore from `origin/main` / leave them in the PR / stop. **Selected (free text): "land that changes in a separate pr with skip-bot-review as pr label"** — executed as #294 (`chore/pre-commit-import-grouping`, label `skip-bot-review`), with the rewrite restored out of #293's worktree after each gate run.

No other question was asked; no free-text message other than that answer.

## 7. Rule check

- **Shell / tools**: no `&&`, `;`, pipes, command substitution or loops in a command; file reads and searches went through the file and search tools; temp output only under `.plan/temp/`.
- **Executor**: every script through `python3 .plan/execute-script.py …`, every build through the resolved `maven run` command, every CI action through `plan-marshall:tools-integration-ci:ci`; no `gh`/`glab`, no `ruff`/`mypy`/`pytest`, no `python3 -c`, no `sleep` (waits used `ci pr wait-for-comments`, `ci checks wait`, `build_server wait`).
- **Guard**: `guard: none on this harness (unenforced)`; nothing was enforced, so no guard log line exists for this run.
- **Git**: staging by path only; no `stash`, `rebase`, forced push or `git restore`. Two sanctioned exceptions: the gate's rewrite was restored with `git checkout HEAD -- pm-api/src/main pm-mock-runtime/src/main/java` (`git checkout -- <path>` is forbidden by the lane) and the same paths were once restored from the rewrite branch — both under the operator's explicit instruction to keep that rewrite out of this PR in favour of #294.
- **A gate-refusal-adjacent action**: the second restore (`git checkout origin/chore/pre-commit-import-grouping -- …`) staged the rewrite into the index and needed a `git checkout HEAD -- …` to undo; recorded here as the run's one self-inflicted detour.
- **Spec read**: `spec_source: ledger_worktree`, `spec_differ: true` — the main checkout's copy is the older one (it lacks the 2026-10-31 re-grounding lines); the ledger worktree's copy was read.
- **Executor skew**: `match` (executor `0.1.2036` = lane `0.1.2036`); recorded, not asked about.
- **Sub-agents**: three self-review sub-agents (general), each read-only; no restart, no killed sub-agent, no re-dispatch.
- **Routed builds**: all builds were submitted with an absolute `--project-dir`; the first attempt used a relative one and failed at daemon spawn (empty log, exit -1) — recorded, then corrected.

## 8. Build server

- Setup preflight: `ready` (project registered, daemon reachable); no fallback was needed.
- Every build was `resolved=routed` (`mechanism=daemon_longpoll`) except the base-branch comparison run, which used `--execution-mode in_process` intentionally (against the main checkout).
- One bound expiry: the first full `verify -Ppre-commit` hit the daemon's 520 s bound (`timeout_used_seconds: 520`, `timeout_source: explicit`); re-submitted once with `--timeout 1500`, which finished in 579–611 s. No `killed` job.
- `runtime_stamp_reason`: not reported; `plugin_version` is `unknown` in the resolve result.

## 9. Residue

- **PR #294** (`chore/pre-commit-import-grouping`, label `skip-bot-review`) is open and unmerged: the `pre-commit` import-grouping rewrite of 177 `pm-api` main sources and one `pm-mock-runtime` main source, landed separately by the operator's instruction. It needs the operator's word to land, and every future `-Ppre-commit` run will keep producing the same rewrite until it does.
- `pm-operator verify -Pintegration-tests` still shows three transcript-golden mismatches (`status.transcript`, `start-plan.transcript`, `start-plan-no-input.transcript`); they reproduce identically at the untouched `main` checkout, so they are pre-existing and outside this plan.
