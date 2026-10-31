envelope_version=1
sender_type=plan
sender_id=rb-82-needs-and-find-contract
epic=ui-improvements
kind=landing
created=2026-10-31T17:59:29Z

```landing-facts
schema=landing-facts/1
plan_id=rb-82-needs-and-find-contract
epic=ui-improvements
pr=#284
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
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

## Deliverables

**D1 — the stable order and the size of change — met.**
- *A mock test: activity changes no row; a row moves when its item starts, ends, or starts or stops needing the operator* — `RunningWorkScenarioTest.TheStableOrder`: `activityMovesNoRow`, `startedRowMoves`, `endedRowMoves`, `needsChangeMovesTheRow`, `projectsInStableOrder`. Order `test-first`; failing run `pm-mock-runtime/maven-2026-10-30-204030.log` (6 failures) at base `07104678` + working tree; green at `pm-mock-runtime` verify (470 → 483 tests). `startedRowMoves` is a review regression test and says so.
- *A contract test and a catalogue test* — `ChangeSizeContractTest`, `PlanViewCatalogueTest.shouldFormatTheChangeSize`. Order `test-first`; failing runs `pm-api/maven-2026-10-30-203954.log` (`expected: not <null>`) and `...-204013.log` (`Missing English key plan.change.size`).
- *`./mvnw -pl pm-api verify` and `-pl pm-mock-runtime verify` pass* — order `none`; green (680 / 483 tests on their own runs; whole-repo `verify` 2496).

**D2 — the two views in the design, accepted — met.**
- *The documents exist, link from `doc/design/README.adoc`, and the operator's acceptance is recorded verbatim* — order `none`; `doc/design/shared/needs-and-find.adoc` and `doc/design/web-app/needs-and-find.adoc`, both linked, and the answers "Accept both views" / "Yes — '/' is the list" recorded in the progress record.
- *The start-page decision is recorded and the four plans listed with the lines they rely on* — order `none`; `web-app/needs-and-find.adoc` § *The Start Page Is What Waits* and § *What the Staged Plans Rely On* (PLAN-36, 46, 49, 84).

**D3 — the what-waits and find contract and the mock — met.**
- *Contract tests* — `NeedsAndFindContractTest`. Order `test-first`; failing run `pm-api/maven-2026-10-31-083720.log` (compilation: the types did not exist) and the behavioural reds `084104`/`084125`.
- *Mock tests* — `NeedsAndFindScenarioTest` (9 tests: order across projects, nothing waits, the resolved item leaving with the event, id/title/PR matches, running first, ended marked, no match, empty refused). Order `test-first`; failing runs `084104`, `084125`, `084220`.
- *Both module verifies pass; the pm-api signatures unchanged or listed* — order `none`; additive only (new classes; two enum constants; the event's canonical constructor gained the nullable machine summary, constructed only in `pm-api`).
- *The package rows stay `open`* — order `none`; `doc/plans/README.adoc` untouched for both rows.
- *The open-issue read at the head is recorded* — order `none`; `sonar read_open_issues` at `038d4a1e`: **0 confirmed**, the SonarCloud quality gate green.

## Personas and skills

- Per step: `persona-implementer` for D1 and D3, `persona-documenter` for D2, `persona-code-reviewer` for every self-review round and every PR comment triage.
- D1 and D3 named `pm-dev-java:java-core`, `java-lombok`, `javadoc`, `java-null-safety`, `junit-core`, `pm-dev-java-cui:cui-testing`, `cui-logging`; D2 named `pm-documents:ref-asciidoc`, `ref-documentation`.
- `skills added`: none — the surface stayed inside the modules the Expected Surface named (`pm-api`, `pm-mock-runtime`, `doc/design`).

## Hypotheses

- *"MachinePaths is where the new resource paths belong, and the mock's machine resource has no needs_operator field today"* — **refuted** for the first half: `MachinePaths` (`pm-api/src/main/java/de/planmarshall/api/MachinePaths.java:31`) is the filesystem machine root (socket, token, runtime record, locks) and holds no HTTP path; the paths became `MachineApiPaths.java` (new). The second half held for the first part of the run.
- *"NeedsOperatorKind lists decision, waiver, waits_for_you, stalled, condition, integrity_pause, held, landing_failed, findings, setup and no credential or cleanup_due"* — **confirmed** (`NeedsOperatorKind.java:18-27` at the base).
- *"The closed list's order puts credential and cleanup_due after setup"* — **confirmed** at `runtime-model/01-processes-and-transport.adoc#needs-operator` (items 7 and 8 after `setup`).

## Findings

- **Self-review round 1** (sub-agent, `persona-code-reviewer`) — 24 findings: F1 (closed-list spellings) fixed by aliases plus the fixture serving the enum's values; F2 (design claimed an item title) fixed in the design; F3 (no list total) fixed by `NeedsOperatorPage.total`; F4 (`blocks` prose vs closed word) fixed in the design; F5 (state-blind find) fixed by `mergedRoutes()`; F8, F-l2, F-l3, F-l4, F-l6 fixed; F6, F7, F-l5 accepted as mock-fixture residue; the "starts" test and the title-match test added.
- **Round 2** — confirmed the fixes; the sample rows, the `overview.kind.project` word, the empty state's machine summary and the parse-through-the-record coverage fixed in `4ea5d39a`/`3b5e96e`; `F-l7` (no typed machine body) accepted by design.
- **Round 3** — no merge blocker; the two pre-landing completions (this record, the open-issue read) carried out; residue listed below.
- **CI and PR comments** — CodeRabbit's inline findings all fixed or answered: ended rows in the project key, malformed `q`, non-2xx route status, `ChangeSize` partial maps, live project store, started plans, ended-work pages, the `needs-changed` list, row locations, `FindKind` order, and the "list in step with acts" note answered as fixture scope; every thread resolved. Dispositions comments `6544163981` and `6544322072`.

## Reviewers

- **coderabbit** (required, `quota_limited: true`) — reviewed head `2caad811` (`final_review_risk_coverage` kind=reviewed, low risk); re-triggered after the last push at 17:40:23Z and refused on quota ("Review rate limited"). Standing review authorization applied: qualifying review exists, every finding handled and every thread resolved, re-trigger after the last push recorded, every other gate green on the current head. **Reviewed** (2caad811).
- **sourcery** (optional, `quota_limited: true`) — quota refusal at 10:54 ("2 days and 12 hours"); never gated the merge. **Refused**.

## Operator questions

1. "The lane reports `verdict: skew` — executor 0.1.2032, lane text 0.1.2036. How should I proceed?" — options {regenerate, continue anyway, stop}; answer (free text): "already regenerated"; verified by re-running `executor-skew` (match).
2. "The contract lives outside this repository (the sibling specification) — read it or work from the plan text?" — options {read the four sections, work from the plan text}; selected "Read those four spec sections".
3. "Do you accept both views as designed?" — options {accept both, accept with changes, reject}; selected "Accept both views".
4. "Is the 'what waits' view also the web app's start page (`/`), or a route of its own?" — options {yes `/` is the list, no `/` stays running work}; selected "Yes — '/' is the list".
5. "The Sonar state at head `4150b8bb` has 4 open S1192 findings in pre-existing cleanup fixtures, and the dismissal verb answers HTTP 400 — how should I proceed?" — options {merge and record, stop blocked, refactor the file's literals}; selected "Refactor the whole file's literals".

Free-text operator messages, separate: "already regenerated" (answer 1).

## Rule check

- **Guard.** `guard: none on this harness (unenforced)` — the resolve result reported `guard: none` for OpenCode, so no guard log line can attest anything; the check below counts the run's own actions.
- **Rule bent, on the operator's selection** (question 2): the run read four sections of `plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc` (`#overview-resource`, `#needs-operator`, `#needs-operator-list`, `#find-work`) and `pm-mcp-core/doc/specification/store-schemas/13-leases-integrity-testing.adoc#change-size` — outside this repository root, which the lane forbids by default.
- **Bash misuse by the run**: two file-operation commands — `tail -n 60` on a build log (to read the build result, which the lane itself sanctions) and `wc -l` on four files (a file listing/size check) — and one piped `python3 -c` that returned only a broken-pipe error and changed nothing. Every other script call went through `python3 .plan/execute-script.py`.
- **Spec read**: `spec_source=ledger_worktree` (the shared ledger worktree's copy), no `spec_differ` marker.
- **executor-skew**: `match` (0.1.2036 / 0.1.2036) after the operator's regeneration; `not_applicable` did not apply.
- **Build gate**: the routed build failed with an empty log on the first attempt; every build ran `in_process` (`mechanism=in_process_fallback`) and the canonical `verify -Ppre-commit` / `verify` were green. One bound expiry at 802 s (`timeout_source=explicit`, 2486 tests passed, the reactor tail cut off) was continued with the gate canonical re-run at a raised timeout, as the lane prescribes.
- **Restarts**: one server restart mid-run (recorded; lane text re-read at 09:05Z) and no killed sub-agent.
- **Commits**: every commit ends with the resolved `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; one commit was written with a wrong trailer by mistake and amended before it was pushed (never a forced push).
- **Git**: no `git stash`, no `restore`, no `checkout -- <path>`, no rebase, no forced push; the base was brought in by `git merge`; the main checkout was moved only by `switch-and-pull`.

## Build server

- Setup preflight on the worktree: `ready` (registered, version 1). Every build reported `requested=in_process, resolved=in_process, mechanism=in_process_fallback` because the routed attempt failed; no `killed` job.

## Residue

- The mock's needs list is fixture-driven: a real act (answering a decision, resolving a hold) does not remove its item; the two states `overview.needs-changed` and `overview.needs-resolved` demonstrate that transition instead. Reported, not fixed (the Done-when asks for a state change).
- `MachineApiPaths` names the machine resource's path but `pm-api` carries no typed body record for it; the summary has its record and the tests, which was accepted as design in review round 2.
- `overview.needs-resolved` carries the same list minus the answered decision; its rows keep their places after review round 2.
- The order key is `created_at` with a `running_since` fallback; the fixtures carry `running_since` only, so the fallback is what they exercise — to be settled where PLAN-34 consumes the order.
- The machine's summary counts the machine's own items (2 in the fixture) while the list totals 12; the design's "as one of their sources" reconciles the badges when PLAN-36 implements them.
