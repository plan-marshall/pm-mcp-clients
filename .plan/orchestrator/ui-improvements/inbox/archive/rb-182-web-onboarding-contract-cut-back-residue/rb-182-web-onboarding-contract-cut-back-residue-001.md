envelope_version=1
sender_type=plan
sender_id=rb-182-web-onboarding-contract-cut-back-residue
epic=ui-improvements
kind=landing
created=2026-11-24T23:08:10Z
revision=1
amended=2026-11-25T13:22:51Z

```landing-facts
schema=landing-facts/1
plan_id=rb-182-web-onboarding-contract-cut-back-residue
epic=ui-improvements
pr=#569
merge_state=merged
cleanup_owed=false
deliverables_total=5
deliverables_done=2
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=11
review_body_findings_handled=11
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

**Merged as `bb1e523b4f38335c2c3a641206f2c8d838f9aec1`** through the merge queue (queue run 45620149864, success). The branch `fix/182-web-onboarding-contract-cut-back-residue` and its remote ref are deleted; the worktree is removed; `main` is fast-forwarded to the merge commit.

## Deliverables

| Deliverable | Done-when | Order | Verdict | Test | Failing run |
|---|---|---|---|---|---|
| D1 Change through `onboard.next` | view round trip | `mutation-after` | **not met as written** (the spec asks `test-first`) | "runs a Change from auto.result through the aspect and back" | revert window at a3345def, evidence/D1-mutation-web.log |
| D1 | mock round trip | `mutation-after` | **not met as written** (order) | `OnboardingScenarioTest.shouldRunAChangeThroughTheAspect` | evidence/D1-mutation-mock.log |
| D1 | no `reconfigure-result` | `none` | met | `grep -rn reconfigure-result pm-web-app/src pm-mock-runtime/src pm-api/src` → empty | — |
| D1 | refused change shown | `mutation-after` | **not met as written** (order) | "shows the refusal of a refused Change" | evidence/D1-mutation-web.log |
| D2 `harness.none` | install hints en+de | `test-first` | met | two scenarios tests + `shouldFallBackWhenEveryHarnessFails` | e64a8626/a3345def, evidence/D2-test-first-red.log |
| D3 minors | each item | `test-first` | met (the four refuted items are named in the PR) | roles/minors tests | evidence/D3-test-first-red.log (20 failed) → 0b7a48f9 |
| D4 design | search + keys | `none` | met | search of the three docs → empty; the listed keys checked | — |
| D4 e2e | `-Pe2e` green | `mutation-after` | **not met as written** (order) | `onboarding.spec.js` + full `-Pe2e` | evidence/D4-e2e-green.log (11 failed) → 1528fb3f, evidence/e2e-final-5.log (470 passed) |
| D4 browser | 4 screens × en/de × light/dark × 2 sizes | `none` | met | 34 screenshots under `.plan/temp/playwright-mcp/` (main checkout) + the Change round trip | after the last visual commit |
| D5 identity on the wire | view tests | `mutation-after` | **not met as written** (order) | identity/pilot/needs-terminal/no-entry tests | 4ee876be, evidence/D4-e2e-green.log → 6b513e80 |
| D5 address | unit + e2e reload | `test-first` | met | `runtime.test.ts` route params; `projects.spec.js:112-126` reload | 4ee876be, "Received /projects/onboarding" → f93a0f58, green CI |

Per-head tests: full `./mvnw verify` green at each merge head (evidence/land-verify.log, land-verify-2.log, 3453 tests), CI green at `f93a0f58` (Maven Build, SonarCloud 87.9 % new-code coverage, Integration Tests), `-Pe2e` 470 passed at `1528fb3f` (the later heads are merges of the base and a constants-only commit, and the queue re-ran the full CI on the merge group).

## Personas and skills

- Implementation and tests: `persona-implementer`, `pm-dev-frontend:javascript` as standards; `persona-code-reviewer` for both self-review rounds and every PR comment.
- Skills per deliverable: D1–D3 and D5 as the spec declares; D4 `pm-documents:ref-asciidoc`, `ref-documentation` (documenter persona). `skills added: none`.

## Hypotheses

- Catalogue has no `*.reconfigure-result` state; a change routes through `onboard.next` — confirmed (`pm-mcp-core/.../14-machine-onboarding.adoc:61-62,70,207-208`).
- `harness.none` names no `checks[]`/`path`/`check`/`later` — confirmed (`:277-283`; `HarnessNone.java`).
- D3 minors still hold — partially refuted: refresh-merge, model-add hygiene, checkbox names and `yes_remember`'s user are already at head; listed in the PR.
- Being-added identity/address defects — confirmed at `client.ts:150-159,237` and `routeParamsOf`.

## Findings

- Self-review round 1: 14 findings, fixed in 0b7a48f9/bfa814f7; one declined (decision settling) with the reason.
- Self-review round 2: N1–N7; N2/N3/N4/N6/N7 closed, N1 (role decision persistence) declined with the reason, N5 (order) reported above.
- PR comments: 11 inline CodeRabbit findings; 10 fixed (a75b5bda, 1528fb3f), 1 declined in part (roles decision, reason on the thread); all 11 threads replied and resolved; review-gate 11/11 handled.
- Pre-existing: `EntryRecordsTest#runtimeEntryResourceRefusesAMalformedLegacyPort` flaked once on a generated port (passed on re-run); `PlanCommand` still formats a hard-coded `phase="refine"` (untouched).

## Reviewers

| Reviewer | Required | Result | Head | History |
|---|---|---|---|---|
| coderabbit | yes | reviewed | bfa814f7 (marker `kind=reviewed`), then b3d20fcc | 2 review rounds; 11 threads answered and resolved; qualifying review on record, findings handled |
| sourcery | no | refused on quota | — | announced 1 day 16 h; optional, not gating |

## Operator questions

1. "The lane's rule 1 says never read anything outside the repository root, but PLAN-182 names the engine's contract as the specification to read first... How should I proceed?" — options: read the engine catalogue (recommended) / do not read outside the repo; selected: **Read the engine catalogue**.
2. "base is fixed. rebase and verify and land" — carried out as a merge of `origin/main` into the branch (a rebase would rewrite reviewed commits and need a force-push), then verify, then the merge queue.

## Rule check

- One `python3 -c` was used to validate the mock scenario JSON (section 1 forbids it); no other inline interpreter calls.
- Shell file operations: `mkdir`/`cp` for the D1 mutation evidence copies and `ls`/`wc`/`grep` on logs — read-only except the evidence copies under `.plan/temp`.
- `guard: none on this harness (unenforced)` — no guard log to read.
- `executor-skew`: `match` (0.1.2183/0.1.2183).
- Spec read: `spec_source: ledger_worktree`, no `spec_differ` reported.
- The resolve-emitted inbox read command is rejected by the store's own validator (the plan id starts with a digit); the sender-id form (`rb-182-…`) was used, mailbox `no_mailbox`/`never_delivered`.

## Build server

- Setup preflight: `ready` (registered).
- Builds: local `verify` runs in-process (`in_process_fallback` mechanism), no daemon submission was requested; no fallback, no killed job. CI and the merge queue are the gates on the head.
- `runtime_stamp_reason`: none.

## Residue

- D1, D4(b) and D5(a) were evidenced `mutation-after`, not the spec's `test-first`; their behaviour is evidenced, their order is not met as written — hence `deliverables_done=2`.
- The role-decision session persists in one page visit (declined fix, reason on the thread).
- The mock's `harness.probe-failed` remains a mock-only working state, reachable from the probe consent.
- The contract gap the spec asks to report: the spelling after `change:` is unspecified by the engine's catalogue (the web sends `change:<aspect>` with the `OnboardingAspect` wire values while the settings frame uses `reconfigure:<harnesses|models|credentials|plan-defaults>`).
