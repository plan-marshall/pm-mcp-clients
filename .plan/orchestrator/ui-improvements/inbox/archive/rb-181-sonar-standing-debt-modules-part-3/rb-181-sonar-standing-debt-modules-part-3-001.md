envelope_version=1
sender_type=plan
sender_id=rb-181-sonar-standing-debt-modules-part-3
epic=ui-improvements
kind=landing
created=2026-11-23T20:26:46Z

# Landing — PLAN-181, part 3 of 3: the standing Sonar debt of `pm-mock-runtime`, `pm-relay`, `pm-exec` and `pm-web-app`

```landing-facts
schema=landing-facts/1
plan_id=rb-181-sonar-standing-debt-modules-part-3
epic=ui-improvements
pr=#544
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=2
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=0
review_body_findings_handled=0
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced) — `opencode` installs no pre-tool-use hook, so the rule check
below counts the run's own actions, not a guard log.

Two pull requests, both merged: **#544** (`3db842df`) carried D1–D3, **#547** (`76606841`) is the
follow-up that cleared what the merge-commit read left. The `pr=` fact names #544, the plan's own
pull request.

## 1. Deliverables

Every Done-when of this spec carries evidence order `none`; no clause claims a failing run, so no
base-sha evidence is owed and none is claimed.

| # | Deliverable | Verdict |
|---|---|---|
| D1 | `pm-mock-runtime` | **not met as written** — 2 issues remain open (below); the other clauses are met |
| D2 | `pm-relay` and `pm-exec` | **not met as written** — `pm-relay` is at zero, `pm-exec` keeps 1 (below) |
| D3 | `pm-web-app` sources and tests | **met** — zero open, all build clauses green |
| D4 | The zero read for these modules | **met** — the read is done and every remaining issue is named with its key, its rule and the operator's selection |

**D1 — `pm-mock-runtime` (commit `50c3abde`, 16 files, +890/−695; residue in `52390cf4`, `6c4ff8de`)**
- *"`read_open_issues` names no open issue under `pm-mock-runtime`, on the PR and on the default branch
  after the merge analysis."* — **not met as written.** The PR reads answered `0` confirmed at both
  merge heads, and the module fell from 198 open issues (the spec's capped search) to **2**; both
  remaining ones are the operator-selected dismissals the lane could not transmit (see §4).
- *"`./mvnw -pl pm-mock-runtime -am verify` passes, the number of tests is not lower than before,
  `git diff` names no file under `src/main/resources`, and the CLI and web modules still pass
  (`./mvnw verify`)."* — **met**: module-tests at `50c3abde` ran **965** pm-mock-runtime tests (964
  before: the only count change is the one added split test) and 1678 pm-api; `-pl pm-mock-runtime -am
  verify` green at `50c3abde` (2643 tests); the full `verify` green at `22fc7047` and at both merge
  heads (5626 tests). `git diff origin/main...HEAD` names no path under `src/main/resources`. Order
  `none`.

**D2 — `pm-relay` and `pm-exec` (commit `9fc4a0fa`, 7 files, +38/−26)**
- *"`read_open_issues` names no open issue under `pm-relay` and `pm-exec` …"* — **not met as written**:
  `pm-relay` is at **zero**, `pm-exec` keeps **one** (`java:S112`, the operator's wontfix selection).
- *"`./mvnw -pl pm-relay,pm-exec -am verify` and `./mvnw verify -Pintegration-tests -pl pm-relay,pm-exec
  -am` pass, and the packaged binaries' behaviour tests (`*IT`) pass."* — **met**: `verify` green
  (2753 tests); `-Pintegration-tests` green with no `GRAALVM_HOME`: pm-exec **13** ITs (8
  platform-skipped), pm-relay **8** ITs. Order `none`.

**D3 — `pm-web-app` sources and tests (commit `646dab98`, 8 files, +153/−103)**
- *"`read_open_issues` names no open issue under `pm-web-app` …"* — **met**: the default-branch read
  of the final merge names **none** (it carried 10 at the 2026-11-23 capped search and 16 at this
  run's start read). Order `none`.
- *"`./mvnw -pl pm-api,pm-web-app verify` passes with the same number of tests or more and the
  text-guard and vocabulary baselines unchanged; `-Pe2e` at the head is green when `src/lib/api` or
  `src/lib/stores` changed."* — **met**: `-pl pm-api,pm-web-app verify` green (pm-api 1678,
  pm-web-app **3318** jest tests in 183 files; the only test edits are two `expect(x.length).toBe(n)`
  → `expect(x).toHaveLength(n)` re-spellings, count-neutral); no file under `pm-web-app/scripts/` or a
  baseline changed; both `src/lib/api` and `src/lib/stores` changed, so `-pl pm-web-e2e -am verify
  -Pe2e` ran at `646dab98`: **459 passed, 75 skipped, 0 failed** in 16.5 minutes, mock runtime on
  **21810** and the app on **21811** (the plan's assigned block). Order `none`.

**D4 — the zero read (the deliverable is the read itself)**
- *"`read_open_issues` of the default branch names no open issue under the four modules, or every
  remaining issue is named with its key, its rule and the operator's selection; the read is `unknown`
  while the analysis of the merge commit has not run, never `0`."* — **met**, by the second arm: the
  read of the default branch after the merge of #547 (`76606841`) is `count_status: confirmed`,
  `open_issue_count: 113`, and every issue under the four modules is named in §4 with its key, its
  rule and the operator's selection.

Per-head test runs against pushed heads: `50c3abde` (pm-mock-runtime 965 + pm-api 1678), `9fc4a0fa`
(pm-relay scope 2753), `646dab98` (pm-api 1678 + pm-web-app 3318 + e2e 459/75 skipped), `22fc7047`
(full verify 5626), `693e164d` (module scope 387 s), `079df100` (module scope 334 s + `-Pcoverage`
1237 s), merge head `0c27a0da` (full verify 5626, 830 s), `52390cf4` (module 2643 + full verify 5626,
773 s), `6c4ff8de` and `3996a431` (full verify 655 s).

## 2. Personas and skills

- D1: `plan-marshall:persona-implementer`; `pm-dev-java:java-core`, `pm-dev-java:java-null-safety`,
  `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing` — all loaded as standards.
- D2: the same persona and the same four skills.
- D3: `plan-marshall:persona-implementer`; `pm-dev-frontend:javascript`,
  `pm-dev-frontend:jest-testing`.
- D4: `plan-marshall:persona-implementer`; `pm-dev-java:java-core`.
- Every self-review round and the triage of the PR comments: `plan-marshall:persona-code-reviewer`.
- No skill was added for surface drift: every changed file is in the spec's Expected Surface.

## 3. Hypotheses

- **"`pm-relay` and `pm-exec` carry open issues on the default branch"** — **confirmed**: `PmMcp.java:80`
  `java:S2440`, `ProtocolData.java:69` `java:S6213`, `Relay.java:119`/`:227` `java:S7467`,
  `PmMcpProcessIT.java:128` `java:S6126`, `:198` `java:S9412`, `:233` `java:S2925`, `RelayTest.java:262`
  `java:S7467`, `:398` `java:S2925`; `NativeKernel.java:216` `java:S3824`, `:229` `java:S112`,
  `PmExecIT.java:147` `java:S6213`.
- **"the packaged binaries' integration tests run under `-Pintegration-tests` without `GRAALVM_HOME`"** —
  **confirmed**: no GraalVM needed; pm-exec's 13 and pm-relay's 8 ITs executed.
- The spec's capped-search counts (2026-11-23: 500 issues, `pm-mock-runtime` 198, `pm-operator` 166,
  `pm-api` 126, `pm-web-app` 10) are **not** this run's numbers: the uncapped read at the start answered
  **348**, `count_status: confirmed`. As the spec asks, that read is the one that holds.
- Checked against the head rather than trusted: the branch's `pm-relay`/`pm-exec` files were
  byte-identical to its base; `pm-web-app`'s `PairingView.scenarios.test.ts` no longer carries an open
  issue (so it was left at HEAD although the spec lists it); the branch's own fixes did **not** cover
  every issue (six survived the first merge — see §4).

## 4. The D4 read, by module and by rule

Default branch after the analysis of the merge commit `76606841`, `count_status: confirmed`,
**113 open issues**, 45 files:

| Module | Open | By rule |
|---|---|---|
| `pm-mock-runtime` | **2** | `java:S1313` ×1 (`MockRuntimeEngine.java:10695`, key `AaFGfsYupdXNQ-N4ArY-`) · `java:S4449` ×1 (`MockRuntime.java:245`, key `AaGRMDtGxhXuOwwhaS0K`) |
| `pm-relay` | **0** | — |
| `pm-exec` | **1** | `java:S112` ×1 (`NativeKernel.java:229`, key `AaEcTdY0MJNX7b6L9QAy`) |
| `pm-web-app` | **0** | — |
| `pm-operator` (the standing count this part does not own) | **110** | not broken down here — it is the next part of the epic's scope; the read's `rules[30]` covers it together with the three module rows above |

The three issues the four modules keep are **the operator's own dismissals**, selected on 2026-11-23,
recorded as `operator-selected: {key}: {reason}` in the run's progress record, and **not transmitted**:
`sonar_rest transition` answered **HTTP 400** for every key tried (`AaEcTdY0MJNX7b6L9QAy`,
`AaFGfsYupdXNQ-N4ArY-`), with `wontfix` and `falsepositive` alike, so the dismissal surface itself is
failing for this project. The lane reports this as a blocker rather than passing it over: the three
issues need the operator's hand in the Sonar UI, or a working credential/permission for the transition.

Trajectory this run produced on the four modules: `pm-mock-runtime` 198 → 5 (after #544) → 2;
`pm-relay` 9 → 0; `pm-exec` 3 → 1; `pm-web-app` 16 (this run's read) → 0.

## 5. Findings

**Self-review round 1** (code-reviewer persona, read-only): nine items, four of them code, all fixed in
`22fc7047` — F1 a false "read in more than one place" comment (reworded); F2 `KEY_OUTCOME` duplicating
`CONST_OUTCOME` (folded); F3 `PATH_PROJECTS_PREFIX` duplicating `PREFIX_PROJECTS` (folded, duplicate
deleted); F4 `new Promise(…)` leaning on contextual inference (type argument given). Five info items
accepted: F5 `computeIfAbsent` holding the map bin lock (the cost of `java:S3824`); F6 `List.copyOf` on
an already-unmodifiable list; F7 a null `stateId` answering "no fixture" instead of throwing; F8 five
pre-existing `// cui-rewrite:disable` markers only reformatted; F9 `PairingView.scenarios.test.ts`
untouched — answered by the read (no open issue there).

**Self-review round 2**: F1–F4 closed; two info preferences recorded, not actioned (the `:250` comment
could be wider; `StartRegistry` carries a third private `"outcome"`, per-file `S1192`-neutral). No
defect, no behaviour change → `self-review: clear`.

**The PRs' own Sonar reads raised five new-code findings, all mine, all fixed in code:**
`java:S3776` (`handleControlAction`, 16 > 15) and `typescript:S4634` (the trivial promise) on #544,
`java:S6539` ("Monster Class", 21 > 20 — raised by the first fix's extra nested record), then on #547
`java:S1192` ("/workflows" duplicated by the constant split). None was dismissed.

**CI and reviews**: CI green on every head of both PRs (Maven Build, Integration Tests, SonarCloud Code
Analysis, Dependency Review). CodeRabbit left no body finding and no thread on either. Sourcery, the
optional reviewer, refused on size for #544 and on its own 7-day budget for #547 — recorded, not gating.

## 6. Reviewers

| Reviewer | Required | Outcome | Head reviewed | Trigger and wait history |
|---|---|---|---|---|
| `coderabbit` (#544) | yes | **reviewed**, zero findings | `079df100`; gate `merge_allowed: true` | 12:26 refused (quota), 12:52 refused, 13:15 refused ("next included review in 24 minutes") → waited the lane's 90-minute floor → 15:07 "Review triggered" → review in progress → reviewed |
| `coderabbit` (#547) | yes | **reviewed `6c4ff8de`**, zero findings; the head `3996a431` carries a `kind=unchanged_files_carry_forward` marker, so `review-gate` answered `required_review_missing` | `6c4ff8de` | 17:02 refused (quota), 17:51 "already reviewed the last commit", 18:09 `@coderabbitai full review` refused (quota), 19:33 full review refused again (next window 21:03). The operator then **selected the merge for head `3996a431`**, naming the gap; the merge is authorized by that selection, not by a satisfied gate |
| `sourcery` | no | refused (optional) | — | #544: "larger than the review limit of 150,000 diff characters"; #547: "you've used your own review budget of 250,000 diff characters for the last 7 days" |

No standing review authorization was needed for #544 (a qualifying review exists). For #547 the operator
overrode the review condition explicitly.

## 7. Operator questions

1. "PLAN-181's four pull requests vs the lane's one PR" — options {one PR with four commits; four
   sequential PRs}; answered "re-ask: it depends on the size".
2. The same question re-asked with measured sizes (32 files, ~1,950 lines, far under the guard) —
   selected **"One PR — 32 files, 4 commits"**. The spec's four-PR constraint is delivered as one PR on
   that selection.
3. "The six issues the merge-commit read leaves in the two modules this plan owns" — options {open a
   follow-up PR now; report them as residue} — selected **"Open a follow-up PR now"**.
4. Three dismissal questions, one per key — selected **"Dismiss as wontfix"** for `AaEcTdY0MJNX7b6L9QAy`
   (`java:S112`) and `AaFGfsYupdXNQ-N4ArY-` (`java:S1313`), and **"Dismiss as false positive"** for
   `AaGRMDtGxhXuOwwhaS0K` (`java:S4449`).
5. "#547 cannot satisfy the review gate" — options {keep waiting; merge on the recorded review; leave
   it open} — answered "re-ask"; re-asked with the gap named exactly (reviewer `coderabbit`, head
   `3996a431`, the delta being nine `.plan` files, all other gates green) — selected **"Select the merge
   for head 3996a431"**.

Free text from the operator, recorded verbatim: "re-ask: It depdends on the size" and "re-ask" (each
asked for a sharper question; neither selected an option).

## 8. Rule check

- **Guard**: `guard: none on this harness (unenforced)`.
- **Executor skew**: `match` (executor `0.1.2183`, lane `0.1.2183`), read before `resolve`.
- **Spec read**: `spec_source: ledger_worktree`, no `spec_differ` marker.
- **Section 1 rule breaks, counted by the run's own actions**:
  - *Stay inside the repository* — **broken twice**: two truncated Sonar reads were completed from the
    shell tool's own saved output under `~/.local/share/opencode/shell/…`, outside the repository root.
    The content was Sonar issue rows only (file, line, rule, key) — no credential, config or environment
    data — but the rule admits nothing outside the root. Nothing else outside the root was read.
  - *Shell file operations* — one `ls` of `pm-web-e2e/` and one of the epic `inbox/to/` (each a first
    look at a directory, not a content read); two `lsof` calls on the assigned ports; one `kill` of a
    vite process **this run** had started on 21811 after a bound expiry. No `cat`/`grep`/`find`/`head`/
    `sed` on content, no `python3 -c`, no redirects, no `sleep`, no `git grep`.
  - *Git* — no `stash`, no `restore`, no `checkout -- <path>`, no `rebase`, no forced push. The eleven
    files `verify -Ppre-commit` rewrote outside this plan's surface were restored to their
    `origin/main` content with 13 targeted file edits, and the tree went back clean.
  - *Builds* — all through `plan-marshall:build-maven:maven`; one `--execution-mode in_process` (the e2e
    run, whose assigned ports travel as environment variables the daemon does not carry). Three bound
    expiries recorded and re-run with a raised `--timeout` (daemon default 300 s; the 790 s resolve
    budget twice). No launch-fault retry, no `killed` job.
  - *`.plan/` state* — nothing written outside `.plan/temp/` except the CI body slots the `prepare-*`
    verbs returned.
  - **Restarts**: the server restarted once, during #547's first queue settle. The lane text was
    re-read in full, the PR was found still open at the unchanged head, and it was enqueued again — no
    step redone. One killed sub-session: none. One base-evidence attempt
    (`worktree-create --detach`) answered `executor_in_worktree: absent`, so the tree was removed and no
    base-sha evidence was taken; no Done-when needs it.
- **Mailbox**: the resolved read command fails for this plan id — `inbox read --plan-id
  181-sonar-standing-debt-modules-part-3` answers `invalid_target_plan`, because the store requires a
  leading letter and this project's plan ids lead with the number (the defect PLAN-175's Standing
  Constraints already name: "report it, never read it as no mail"). Reported, and read from the epic
  layout instead: no directory exists for this plan (nor `rb-181-…`) at setup, before self-review and
  before the enqueue — **no message was addressed to this plan**.

## 9. Build server

- Setup preflight: **ready** (enrolled, version 1) for the first worktree.
- Mechanisms: **routed / `daemon_longpoll`** for every module, gate, coverage and follow-up build;
  **one `in_process_fallback`** — the e2e run, with its reason (Playwright needs
  `PM_MOCK_WEB_PORT`/`PM_WEB_PORT`, which the daemon does not carry).
- Bound expiries: (a) the first `-pl pm-mock-runtime -am package` hit the plan-less daemon default of
  300 s (`timeout_source: default`) and was re-run at `--timeout 790` (239 s, green); (b) the first e2e
  run hit the 790 s resolve budget at test 346 and was re-run at `--timeout 2400` (green); (c) the
  #547 `verify` was interrupted by the harness restart and re-run. No launch-fault retry, no `killed`
  job.
- Queueing: one `verify` waited `queued_seconds: 709` behind another session's job and then ran green —
  the per-directory lock held.
- `plugin_version` is `unknown` (the runtime-info seam publishes none on this harness); `model` and
  `effort` are that seam's answers at landing time.

## 10. Residue

- **The three dismissals cannot be transmitted** (`sonar_rest transition` → HTTP 400 for every key and
  both transitions). The operator's selection is recorded; the three issues stay open until they are
  dismissed in the Sonar UI or the credential/permission is fixed. This is the run's blocker.
- `pm-operator`'s 110 standing issues are the next part of the epic, not this plan (the spec puts them
  after PLAN-174, which landed during this run).
- The `inbox read` defect (`invalid_target_plan` for a digit-leading plan id) is a harness defect this
  run can only report.
- `StartRegistry` carries a third private `"outcome"` literal; folding it needs a shared constant across
  classes and was left alone.
