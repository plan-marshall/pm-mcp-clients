envelope_version=1
sender_type=plan
sender_id=rb-66-web-onboarding-frame-and-roles
epic=ui-improvements
kind=landing
created=2026-11-18T04:45:16Z

```landing-facts
schema=landing-facts/1
plan_id=rb-66-web-onboarding-frame-and-roles
epic=ui-improvements
pr=#478
merge_state=merged
cleanup_owed=false
deliverables_total=10
deliverables_done=2
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2149
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=9
review_body_findings_handled=9
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

## Deliverables

The Emit Amendment 2026-11-17 binds this run: "5 to 7 first, then 8 to 10". **D1–D4 shipped in #467** and
**D8–D10 remain open** (`catalogue.select`, `store.choose`, `harness.choose`). `deliverables_done` counts only this
pull request's deliverables.

**D5 `harness.none`**

- *A screen test on `harness.none`: no `Machine · harness.none` text* — **met**, order `test-first`. Failing run
  `17020f32` (test-only commit), `.plan/local/plans/NO_PLAN/build-results/pm-web-app/maven-2026-11-17-204055.log`,
  2 failed / 2919 passed (`harness-none-failures` not found); passed `5139b125`, `maven-2026-11-17-205111.log`.
  Caveat: the clause's own literal is inert — nothing in the app ever printed `Machine · <id>` — the test earns its
  red through the per-harness assertions.
- *Browser check on `harness.none`* — **met for the published shape**, order `none`. 16 files
  `plan-66-harness-none-{before,after}-{en,de}-{light,dark}-{1440,390}.{snap.yml,png}`. Before: a title and three
  buttons. After: each failed harness with its answer and fix, the "none passed" sentence and the fix line. **The
  path and the check's name are a contract gap**: neither `pm-api` nor any in-repo contract names them, they were
  removed from the mock, and the screen renders them only when a runtime sends them (the screen test pins that
  branch). `onboarding.adoc` now says "where the runtime reports them".

**D6 `auto.result`**

- *A screen test on `auto.result`* — **met**, order `test-first`. Same failing log (button `/^change /i` not found);
  passed `5139b125`.
- *Browser check on `auto.result`* — **met**, order `none`. 16 files `plan-66-auto-result-*`. Before: three columns,
  no act, humanized wire labels. After: seven rows, *Change* per row in the catalogue's words (`Change` / `Ändern`,
  accessible `Change Language` / `Sprache ändern`), and the *Continue* footer. PLAN-64's residue is fixed with it:
  `auto.run` and the `.working`/`.probe` suffix rules, and per-link labels on the working screen.

**D7 `language.choose`**

- *`onboarding.spec.js`: after "Use Deutsch for both" the next screen is German* — **behaviour met; order `not met as
  written`**. The spec names `test-first`; the fix was committed (`8c7dca66`) before the test (`d35a1e52`), and both
  earlier reds were test bugs. The recorded red is therefore `mutation-after`: `097e3721` reverts the adoption as a
  commit, the suite fails there (`.plan/local/plans/NO_PLAN/build-results/pm-web-e2e/maven-2026-11-17-235804.log`,
  `onboarding.spec.js:161`, 1 failed / 454 passed), `5dbe96c7` re-applies it. A base-commit red was attempted and is
  a **blocker**: the detached `66-…-base` worktree answered `executor_in_worktree: absent`, which the lane says is
  not evidence; it was removed. The reviewer's round-2 and round-3 verification both refused `test-first` here.
- *Browser check on `language.choose`* — **met**, order `none`. 16 files
  `plan-66-language-choose-*` (the screen itself, unchanged) plus 16 `plan-66-language-next-{before,after}-*` for the
  screen the deliverable actually changes: before, recording German leaves the next screen English; after, it turns
  German (frame, page title, acts), and recording English leaves it English.

Per-head test runs: `verify -pl pm-web-app` 2941 passed; `verify -Ppre-commit` BUILD SUCCESS on all eight modules at
`3a097f43`; `verify -pl pm-web-e2e -Pe2e` 455 passed / 75 skipped at `3a097f43` and at the merge head.

## Personas and skills

Per step: the implementer persona for D5 and D6, the integration-tester persona for D7 (the spec's own `Persona:`
lines); the code-reviewer persona for the three self-review rounds and for triaging every PR comment.

Per deliverable, the domain skills the spec named: D5 `pm-dev-frontend:javascript`, `:css`, `:jest-testing`,
`:javascript-security`; D6 the same four; D7 `pm-dev-frontend:javascript`, `:javascript-security`. All were loaded as
standards. No skill was added for surface drift: the run touched no module the Expected Surface did not name.

## Hypotheses

- D5: `harness.none` drew the generic status card from its summary alone — **confirmed**, `screens.ts:143`
  (`STATUS_STATES`), `StatusScreen.svelte:13-54`.
- D6: `auto.result` drew a three-column table with no per-row act — **confirmed**, `AutoResultScreen.svelte:16-33`,
  and the mock state offered only `done`.
- D7: `stepOnboarding` never adopted the recorded language, so the screen after recording kept the old one —
  **confirmed**, `runtime.ts:1716-1725` (only `applyOnboardingState`).

## Findings

Self-review round 1 (13 findings): the catalogue's `change` link instead of a mock-only `reconfigure:<aspect>` shape,
PLAN-64's residue, the missing link labels, the duplicated aspect map, the unreachable footer link, the unguarded
"looked for" sentence, the duplicated key, non-existent keys in the texts doc, stale comments, surplus mock facts —
all fixed in `2dee1a3d`, `793b07d7`, `926a3aac`.

Self-review round 2 (3 blocking, 2 major, 8 minor): B3 (no green unit run) **refuted** — the gate log
`default/maven-2026-11-18-002446.log` ran the whole tree, 2923 tests; B1/B2 **reported as contract gaps** (see
above); B4 (D7's order) **accepted** and re-recorded honestly as `mutation-after`; the rest fixed in `926a3aac` and
the evidence re-taken.

Self-review round 3 (no further code defect beyond F1/F2/F4 and minor cleanups): F1 (D5's five facts) → the design now
says "where the runtime reports them", the gap is reported; F2 (English runtime `summary` on a German screen) →
**refuted**: the summary is the runtime's own text, the mock localizes nothing, and every screen of the registry
renders it verbatim; F4 (the `change:<aspect>` spelling) → the design no longer claims it as the catalogue's, and it
is reported as a gap; F5 (German accessible name) → `onboard.auto.result.change_label` added; F6/F7 (raw ids as
labels) → humanized; F8 → a registry test for the suffix rules; F9 → the doc row made truthful; F10 → the diff is the
answer.

PR comments (CodeRabbit, 9 review-body findings, all handled):

- `inline-1` — *Later sends nothing* → the doc says it sends a step for `later` (`63f628c3`).
- `inline-2` — footer *Change* buttons indistinguishable → labelled per aspect (`63f628c3`).
- `inline-3` — a late adoption could be lost → the bound now limits only the wait, the read applies late (`63f628c3`).
- `inline-4` — the automatic result's edit dead-ends at `settings.choose` → seven `*.reconfigure-result` states return
  to `auto.result`, claimed by the same screens, titled in both locales (`6baa911a`, `3a097f43`).
- `inline-5` — a pending adoption survives a later record → `adoptRecordedLocaleNow` bumps the switch generation
  (`6baa911a`).
- `inline-6` — a chosen language is lost where storage refuses → the choice also lives in memory for the visit
  (`6baa911a`).
- `inline-7` — a saved response could overwrite a newer state → an applied-state revision guards both paths
  (`6baa911a`).
- `inline-8` — no resume line beside *Later* → `onboard.later.hint` rendered with that link (`6baa911a`).
- `inline-9` — the e2e asserted eventual German only → the language step now waits for the adoption unbounded, so the
  next screen cannot paint first (`6baa911a`).

Sourcery: refused on quota (optional reviewer), recorded; the gate never depended on it. No CI failure went
unhandled: the two red CI runs were the local gate before a fix, each followed by a green run.

## Reviewers

- **coderabbit** (required, `quota_limited`, `review_complete_after_handling`): reviewed `e606c1db` (3 findings) and,
  on the requested full review, `63f628c3` (6 findings). All nine are handled and every thread answered with a commit
  and resolved. No re-trigger was needed on later heads: a qualifying review exists and every finding of it was
  handled, so the standing credit applies (`review-gate` confirms a qualifying review: `required_reviewers` reports
  it and the gate answers `all_handled`). `@coderabbitai full review` was posted once to discharge a bot comment that
  the gate read as a standing concern; the bot's own reply closed it.
- **sourcery** (optional): refused on its review budget of 250,000 diff characters for 7 days — a quota refusal, not
  a review; the run did not wait for it.

## Operator questions

None asked. No block required an operator decision: the split is the spec's own licence, the reviewers answered, and
the two evidence gaps (D5's `path`/`check`, D7's order) are reported here rather than papered over.

## Rule check

- **Breaches of my own:**
  - one shell call used a heredoc (`python3 - <<'PY'` to change `rel` values in the mock); the edit tool would have
    done it, and the file tools were used everywhere else.
  - build logs were read with shell `grep`/`sed` throughout; the lane asks for the file tools, and the logs are files.
    No repository source was read or searched that way.
  - `git show origin/main:<path> | sed` resolved the merge conflict's other side; that side is repository source read
    through a pipe.
  - five e2e runs were dispatched `--execution-mode in_process` rather than with the default routing: the daemon's
    child env is a whitelist (`PATH`, `HOME`, `LANG`, …) without `PM_MOCK_WEB_PORT`/`PM_WEB_PORT`, so a routed run
    would have used the operator's own 7420/5173. Reported, not silent.
- **Guard**: `guard: none` on this harness (unenforced); `.plan/temp/guard.log` has no line for this run, and the run
  reports no "all kept".
- **Spec read**: `spec_source: ledger_worktree` at setup, with `spec_differ: true` (the main checkout's copy differed).
  The main checkout's copy was what the session first read; the ledger's copy — amended mid-run with the Emit
  Amendment — then bound, and the two are identical now.
- **executor-skew**: `match`, executor 0.1.2149, lane 0.1.2149.
- **One restart, no re-dispatch**: after the round-2 findings the self-review sub-agent was dispatched twice more
  (rounds 2 and 3); each report is above. No sub-agent was killed.

## Build server

- Setup preflight: **ready** (the project is enrolled, the daemon answered the handshake).
- Builds by mechanism: **daemon_longpoll 20**, **in_process_fallback 11**. Every non-e2e build was routed and none
  degraded or retried; the eleven e2e runs are the deliberate in-process ones above (port-pinned).
- No `killed` job; the daemon's queue delayed several builds (`queued_seconds` up to 873) and none was lost.
- `runtime_stamp_reason`: none carried; `plugin_version` is `unknown` in every resolve.

## Residue

- **D8–D10 remain open** (`catalogue.select`, `store.choose`, `harness.choose`) — the next pull request of the
  amendment's split.
- **Contract gaps for the operator**: `harness.none`'s `checks[]` and its `path`/`check`; the per-row
  `change:<aspect>` id shape; the "looked for" facts. None of them is stated by `pm-api`, and the engine's state
  catalogue is outside this repository (not read). The new `*.reconfigure-result` states are the mock's, built so the
  automatic result's edit returns to the result.
- **`resolve`'s inbox command cannot read this plan's mailbox**: `inbox read --plan-id
  66-web-onboarding-frame-and-roles` is refused `invalid_target_plan` (the plan id starts with a digit; the verb
  requires `^[a-z][a-z0-9-]*$`). The three reads the lane asks for all failed the same way; the epic queue was
  readable and carried nothing addressed to this plan. A delivery to this plan's mailbox would have been invisible.
- D7's `test-first` order is not recoverable in this history; the operator may count it or not.
