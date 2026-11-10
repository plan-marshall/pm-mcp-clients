envelope_version=1
sender_type=plan
sender_id=rb-124-cli-answer-dialogue-residue
epic=ui-improvements
kind=landing
created=2026-11-09T23:45:21Z

```landing-facts
schema=landing-facts/1
plan_id=rb-124-cli-answer-dialogue-residue
epic=ui-improvements
pr=#385
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=4
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2090
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# PLAN-124 — Terminal: the Answer Dialogue, Made Correct (PLAN-21 Residue)

Branch `fix/124-cli-answer-dialogue-residue`, PR #385, merge commit `be8c8403c8d5903ac11e9a154af260162526f21a`.
The run resumed an already-implemented branch (no progress file existed) and completed the residue: review
answers on #372, the Sonar read, the reviewer cycle, and the merge on the operator's word.

## Deliverables

1. **Every decision text is cleaned before it is printed** — met.
   - Done-when (hostile text on dialogue/`--no-input`/direct): met. Test `AnswerSanitizationTest.dialogueIsClean`,
     `noInputViewIsClean`, `waiverViewIsClean`, `directAnswerIsClean`. Order `mutation-after`: reverting the sink made
     three of the four fail (job `9a01a633d4014939afab3c2e1f6ef4c1`). Commits `69156e0a`, `cdf8cfdb` (single-line sink),
     `25f76500` (security-exception message).
2. **Every part, with its question; the answer keys the design defines** — met.
   - Done-when (mock Q-012, three questions, three posted answers; direct refuses): met. Test
     `AnswerDialogueIT.threePartDecisionAsksEveryPart`. Order `mutation-after`: reverting to one part made it fail
     (job `09916bf17b9743759242e705668a03cd`). Commit `7afbd292`.
   - Done-when (costly `?r`, split no preselect, no `w`, `?2`, chat key): met. Tests `AnswerMultipartTest`
     (`costlyRecommendationHiddenUntilRequested`, `splitVotesPreselectNothing`, `voteViewShowsVotesOnOption`,
     `ownWordsNotOfferedWhenDisallowed`, `chatKeyNotAdvertised`). Order `mutation-after` for the two key tests
     (job `9a01a633d4014939afab3c2e1f6ef4c1`); the other three carry their recorded `test-first` failure
     (`maven-2026-11-08-185743.log`). Commit `7afbd292`.
3. **Responses checked, texts from the catalogue, nouns from the envelope** — met.
   - Done-when (refused withdraw / own words / `Q-999 later`): met. `AnswerRefusalTest.refusedWithdraw`,
     `refusedOwnWordsAnswer`, `laterOnUnknownDecision`. Order `test-first`; failing run `maven-2026-11-08-192948.log`.
     Commit `fa72c19b`.
   - Done-when (residue grep empty): met. Order `none`. Commit `fa72c19b`.
   - Done-when (German nouns plan/epic/entry): met. `AnswerRefusalTest.germanNouns`. Order `test-first`; failing run
     `maven-2026-11-08-192948.log`. Commit `fa72c19b`.
4. **Tests that can fail, and the 20 threads answered** — met.
   - Done-when (`ci pr comments --pr-number 372` = 20 threads replied, 0 unresolved): met. Order `none`. 20 threads
     each carry a reply and are resolved; the review body (4 items) is answered in one comment. Commits `cdf8cfdb`,
     `819372d8`.
   - Done-when (head Sonar read recorded; confirmed 0 or all dismissed): met. Order `none`. `read_open_issues`
     returned `confirmed` 0 at `c8768f1e` (the merge-group head).

Per-head test runs: `verify -Ppre-commit` green at `c8768f1e` (2941 tests); `verify -Pintegration-tests` green at
`c8768f1e` (655 tests); the `14-cli-gate-scope` capture set green at `a5ce67d6`.

## Personas and skills

- D1–D3: `plan-marshall:persona-implementer`; D4: `plan-marshall:persona-module-tester`; every self-review round and
  every PR-comment triage: `plan-marshall:persona-code-reviewer`.
- D1 skills: `pm-dev-java:java-core`, `pm-dev-java:java-security`, `pm-dev-java:junit-core`,
  `pm-dev-java-cui:cui-testing`. D2: `java-core`, `junit-core`, `cui-testing`. D3: the same plus
  `pm-documents:ref-asciidoc`. D4: `junit-core`, `junit-integration`, `cui-testing`.
- skills added: `pm-dev-java:java-core` (and the module's Java standards) for `pm-mock-runtime` and `PmOperator`,
  reason: the fix set touched production code in modules the spec's Expected Surface did not name.

## Hypotheses

- "The runtime contract answers a multi-part decision in one POST carrying every part's answer." — **confirmed**:
  `pm-api/src/main/java/de/planmarshall/api/decision/DecisionAnswerRequest.java:56` (the `part_answers` map) and the
  web app's client build the same shape.
- "The terminal has no decision-chat surface yet." — **confirmed**: `AnswerCommand.java` has no chat key or handler
  (the key is not advertised), while PLAN-115 landed only the design on `main`.

## Findings

- Self-review round 1 (persona-code-reviewer) reported: the `--plan` write paths, the dropped own-words note, the
  sink's LF/TAB, the raw stderr message, the unbacked test-first orders, the PR's failing-run ids, and the merge
  conflict. All were fixed in `cdf8cfdb`, `819372d8`, `973d2c01` (plus the merge `08ec4499`); the read-back submission,
  the interactive `later` no-act and the subject-parsing suggestions were refuted with evidence.
- Two self-review nits are reported as refuted preferences, not open defects: the unreachable English band fallback
  in `formatOptionSupport` (all four catalogue bands have keys) and the `ProcessTreeTest` whitespace rewrite the
  pre-commit profile itself forces.
- CodeRabbit reviews: `77de5dd1` (13 findings; 8 fixed, 5 answered), `dd14e13a` (3 fixed), `dd03c257` (the capture
  exit-code regression plus an outside-diff security-message finding; both fixed), `6d035ee1` (wait for a declared
  capture exit; fixed), `228b9179` (no new findings). Every thread is replied to and resolved.
- CI at every head was green; Sonar's one new-code issue on the sink line (`java:S9413`) and on the path literal
  (`java:S1192`) were fixed, not dismissed.

## Reviewers

- CodeRabbit (required, quota-limited): reviewed `77de5dd1`, `dd14e13a`, `dd03c257`, `6d035ee1`, `228b9179`. The
  current head at merge was `c8768f1e`, whose review it skipped; standing authorization applied — a qualifying review
  exists on an earlier head (`228b9179`, coverage marker `kind=reviewed`), every finding is fixed or answered and every
  thread is resolved, and it was re-triggered after the last push (refused on quota at 22:09 and again at 22:45,
  reviewed `228b9179` at 22:54, then skipped `c8768f1e`).
- Sourcery (optional): refused every trigger because the pull request exceeds its 150,000-character limit; it does
  not gate the merge.

## Operator questions

1. Asked at head `0047680c`: whether to merge with CodeRabbit's standing review at an earlier head (all findings
   handled) or wait. Options: "Merge with the standing review", "Wait for CodeRabbit on 0047680c", "Do not merge".
   Selected: **"Merge with the standing review"**.

## Rule check

- `guard: none on this harness (unenforced)` — no guard log exists for this run, so no "all kept" claim is made.
- Spec read: `spec_source=ledger_worktree`; no `spec_differ` marker.
- `executor-skew`: `match` (0.1.2080 at session start; 0.1.2090 at landing).
- Deviations recorded: several build-log reads used shell `tail`/`grep` and one skill-directory `find`
  (`/Users/oliver/.config/opencode/skills/...`), both against the file-tools/inside-the-repository rule; the public
  GitHub API was read through `fetch` to obtain the review-comment summaries that the tool output truncated; and the
  integration profile was run as `-pl pm-api,pm-mock-runtime,pm-operator` rather than the spec's `-pl pm-api,pm-operator`
  because the two-module form resolves an installed `pm-mock-runtime` snapshot that predates the decision metadata.
  No restarts, killed sub-agents or re-dispatches occurred.
- The `14-cli-gate-scope` and `ProjectCommand` changes are the pre-commit profile's own rewrites of merged `main`
  code; they were committed, never hand-edited beyond the capture-exit fix.

## Build server

- Setup preflight: `ready` (project enrolled). Every build was `daemon_longpoll`; no fallback, no retry, no `killed`
  job. `runtime_stamp_reason` was not reported.

## Residue

- The two refuted self-review nits above.
- The unbacked `test-first` orders were re-evidenced as `mutation-after`; the failing/output identifiers are the
  mutation job ids, not the original pre-fix runs.
- `plugin_version` was `unknown` at resolve, as reported.
