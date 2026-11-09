envelope_version=1
sender_type=plan
sender_id=rb-34-cli-display-layer
epic=ui-improvements
kind=landing
created=2026-11-08T16:05:14Z

```landing-facts
schema=landing-facts/1
plan_id=rb-34-cli-display-layer
epic=ui-improvements
pr=#371
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=1
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2080
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

## Deliverables

### D1 — the served order and the size of change
- "A test in `pm-operator`: an activity event changes no row's position; the plan view and the completion level show the size, and no line without one" — **met**, order `mutation-after` (spec asked `test-first`). Tests `OverviewFormattingTest.sortProjectsKeepsServedOrder` / `sortPlansKeepsServedOrder` / `sortEpicsKeepsServedOrder`, `PlanViewLevelTest.changeSizeLine`, `PlanCompletionLevelTest.changeSizeLine`. Failing run `dbc440d4b4df4171b1073e763f0810a0.log` (expected served order, got the old sort; change rows absent). Commit `b807375`.
- "Terminal check (screen capture) before and after" — **not met**: not produced.
- Deliverable not fully met.

### D2 — Display
- "Table tests of `Display.duration` and `Display.time` in both languages" — **met**, order `test-first`. Tests `DisplayTest.durationTable`, `DisplayTest.timeTable`, `DisplayTest.shortStyle`. Failing run `6bbd19e6af01425f97efecc72572c5b7.log` (`Symbol: Variable Display` at `DisplayTest.java:49`). Commit `31b7e85` (+ formatter `d1dccdf`).
- "Terminal check (screen capture) both locales" — **not met**: not produced.
- Deliverable not fully met.

### D3 — used everywhere
- "`TuiCapabilityIT` and transcripts: no `remediation.ci-failure`, `ci_check:`, `(step current)`, lone `-`, link name or ISO time" — **partially met**, order `mutation-after`. `status.transcript` and the epic/overview now render the unit title (`CI failure`), not the raw id (`86fd7eb`, `3dac728`, `6cf6816`, `218f602`); the lone `-` placeholders and some second duration formatters remain.
- "Terminal screen capture", "Browser regression (Playwright)" — **not met**: not produced.
- "Open-issue read at the PR head" — **met**: confirmed `0` at `34675a2`.
- Deliverable not fully met.

### D4 — the time forms in the client designs
- "`git grep` over `doc/design/terminal-client` names no form the rule lacks" — **met**, order `none`: only `2 months ago` remains, a rule form.
- "Open-issue read at the PR head" — **met**: confirmed `0` at `34675a2`.
- Amendment: "`pm-operator/src/main` free of `just now|weeks? ago|yesterday`" — **met**, order `none`: grep empty.
- Amendment: "German-session test for `status` and `projects`" — **met**, order `mutation-after` (spec asked `test-first`). `GermanAgeDisplayTest.germanAges`; failing run `c5201b42a691447fb29cc193a2eaf54b.log` (`gerade eben` vs `just now`). Commits `a25690c`, `236df94`.
- Deliverable fully met.

## Personas and skills
- D1: `persona-implementer`; skills `pm-dev-java:java-core`, `java-lombok`, `javadoc`, `java-null-safety`, `junit-core`, `pm-dev-java-cui:cui-testing`, `cui-logging`.
- D2: `persona-implementer`; same skill set.
- D3: `persona-integration-tester`; skills `pm-dev-java:junit-integration`, `junit-core`, `pm-dev-java-cui:cui-testing`.
- D4: `persona-documenter`; skills `pm-documents:ref-asciidoc`, `pm-documents:ref-documentation`.
- Self-review and PR-comment triage: `persona-code-reviewer`.
- skills added: none (no surface drift beyond the Java/documentation skills named).

## Hypotheses
- Re-grounding "no `Display` class in `pm-api`" — **confirmed** at the start (absent); created.
- "`OverviewFormatting` still sorts by `lastActivityAt`" — **confirmed** (`OverviewFormatting.java:124-176` before the change); removed.
- "English-only age strings in `ProjectsCommand:317-327`, `StatusCommand:478-485`, `WebCommand:623-643`" — **confirmed**; routed through `Display.time`.
- "The client designs still show the old forms" — **confirmed**; rewritten.

## Findings
- Self-review round 1: blockers open (D3 "used everywhere" incomplete: `Display.word`/`title` unused, second duration formatters, lone `-`); fixed the doc/code drift in `15-overview.adoc`, the raw unit ids in `status`/`epic`/`overview`, and the CodeRabbit nits. Open: the D3 blocker and the terminal/browser evidence. `self-review` not clear.
- CodeRabbit (review on `c56d3b9`): 5 actionable + 3 nitpicks + 2 additional. Fixed: sanitize the landing title (`218f602`), session locale + full-screen unit title (`6cf6816`), test-label/row-level/round-trip nits (`6cf6816`, `34675a2`). Answered with a reason: mock `projects-and-start` order (PLAN-82 surface), `TranscriptNormalizer` explicit patterns, `ProcessTree` live producer / partial `ChangeSize` (PLAN-82 contract). All threads resolved.
- CI: `build (26)` failed at `6cf6816` because `ActionsFullScreenTest` selected the old raw landing text; fixed in `c56d3b9`.
- Sonar: 2 issues at `236df94d` (`S4449`, `S1192`) fixed in `cfd9f98`; `S5961` at `34675a2` fixed by splitting the test. Final read: `0` confirmed.

## Reviewers
- coderabbit (required, quota-limited): reviewed `236df94d` and `c56d3b9`; findings fixed or answered; re-triggered after `34675a2`, refused with "Review rate limited" (recorded). Standing review authorization applied; operator selected "Merge now".
- sourcery (optional): triggered; refused on its 7-day diff-character budget.

## Operator questions
- "PLAN-34 (PR #371) is ready to merge…" — options: "Merge now", "Do not merge yet"; selected "Merge now".

## Rule check
- `guard: none on this harness (unenforced)`.
- Shell file operations used: `cp`/`rm` for the test-first evidence under `.plan/temp/` (allowed), and `grep`/`tail` on build logs and source several times — the lane asks for the file tools for reads/searches; those are the run's own deviations.
- `git worktree remove` (not the git-workflow verb), `prune-local-and-remote-ref`, `switch-and-pull` used for cleanup.
- `executor-skew`: `match` (0.1.2080 / 0.1.2080).
- `spec_source`: `ledger_worktree`; no `spec_differ` marker.
- Restarts: none; no killed sub-agent; one self-review sub-agent.

## Build server
- setup preflight: `ready`.
- mechanism: `daemon_longpoll` for every build; no `in_process_fallback`.
- one bound expiry: the 3-module `test` hit the daemon's 300 s bound (`timeout_used_seconds=300`, `timeout_source=default`), then the modules ran separately. Several `queued` waits from a busy shared worktree (up to 723 s). No launch-fault retry.
- `runtime_stamp_reason`: none.

## Residue
- D3's terminal screen-capture set and the Playwright browser regression were not produced; the two D1/D2 terminal captures likewise.
- `Display.word`/`Display.title` have no production callers yet; second duration formatters (`PlanViewFormatting.duration`, `SystemFormatting.uptime`, `OverviewFormatting.formatSeconds`) and remaining lone `-` placeholders are not migrated.
- `ProcessTree`'s `change` is parsed but the live process producer is outside this repository; the mock serves it.
