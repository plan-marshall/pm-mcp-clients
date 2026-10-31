envelope_version=1
sender_type=plan
sender_id=rb-09-web-display-layer
epic=ui-improvements
kind=landing
created=2026-10-31T15:28:02Z

```landing-facts
schema=landing-facts/1
plan_id=rb-09-web-display-layer
epic=ui-improvements
pr=#287
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=4
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

**D1 — Display module.** Met.
- Unit tests per function, de and en, including an unknown value — met, order `test-first`:
  `pm-web-app/src/lib/display/index.test.ts` (17 tests). Failing run `maven-2026-10-31-114620.log`
  (`Failed to resolve import "./index"`, commit `2991b39^`); green at `2991b39` and at every later
  head (`maven-2026-10-31-142629.log`, `…-1459…`).
- Browser check on `/system`, `/system/cleanup`, overview, `/reports`, a completion report, an
  epic, `/start`, `/settings/machine` — met, order `none`: 64 screenshots
  `rb-09-<target>-<locale>-<theme>-<width>-{before,after,final}.png` and 16 accessibility
  snapshots `rb-09-<target>-<locale>-final.yml` under `.plan/temp/playwright-mcp/`. Before: system
  (en) "1 waiting for_you", "tasks 4 running", "Build slots 1 of 2 slots", "listeners socket";
  machine (de) "Früge mich nach Tage", ISO times; cleanup "ended ended 12 Sep"; completion
  "MERGED", `github.com`. Final: system (de) "1 wartet auf dich", "4 laufen", "1 von 2 Slots",
  "Socket ·"; machine (de) "Beendete Arbeit [30] Tage aufbewahren", "Erinnerung nach [3] Tagen";
  cleanup "beendet 12 Sep"; completion (de) "ZUSAMMENGEFÜHRT", "fertig"; overview (de) "läuft",
  "wartet auf dich", "angehalten", "vom Weg ab: wiederholend". Across all 64 combinations no raw
  enum, state id or placeholder; `github.com` sits inside a code element the guard skips; the ISO
  times CC-F-04 owns remain and are in the baseline.

**D2 — Enum guard.** Met.
- Fails on `SystemView.svelte` before the views change, passes after — met, order `test-first`:
  failing run `maven-2026-10-31-115111.log` lists `lib/views/SystemView.svelte:
  w.state.toLowerCase()`; green at `6d53ea2` and at every later head. Three control tests prove
  the rule catches a bare read, an optional chain, a nullish fallback, a `||` fallback, a mixed
  `displayState(...) + node.state`, `String(node.state)` and an `aria-label` binding.
- Browser check — met as for D1 (same snapshots and screenshots).

**D3 — Views through the display layer.** Met.
- The text-guard baseline loses every raw-enum and state-id entry of these views — met, order
  `none`: `maven-2026-10-31-124343.log` lists exactly those removals; the baseline shrank in
  `6d53ea2`; the guard reports `SystemView 0`, `CleanupView 0`, `OverviewView (de)
  foreign-word: ago`, `PlanView`, `ReportsView`, `CompletionReportView`, `MachineSettingsView`
  only `<iso-time>`, and the e2e is green at `647d2370` and at the final head (307 tests).
- Browser check — met as for D1.

**D4 — Catalogue fixes.** Met.
- A catalogue test asserts each corrected string in both locales — met, order `test-first`:
  failing run `maven-2026-10-31-120255.log` (old texts); green at `add1ec5` and at the final head.
- Browser check — met as for D1.
- The open-issue read at the pull request head is recorded — met, order `none`: `sonar
  read_open_issues` on `d5fd44e8` returned `count_status: confirmed`, `open_issue_count: 0`; the
  gate reports `merge_allowed: true`.

Per-head test runs: `-pl pm-web-app test` green at `2991b39`, `6d53ea2`, `add1ec5`, `39e09faf`,
`647d2370`, `d5fd44e8`; `-pl pm-api,pm-operator` and `-pl pm-api,pm-mock-runtime,pm-operator`
green at `add1ec5`/`647d2370`; `-pl pm-web-e2e verify -Pe2e` green at `647d2370` (307 passed) and
at `d5fd44e8` (307 passed); full `verify` green at `1b6c6ae3`-based heads `647d2370` and
`d5fd44e8`.

## Personas and skills

- Persona applied per step: `plan-marshall:persona-implementer` for D1–D4 (the spec's own
  `Persona:` line), `plan-marshall:persona-code-reviewer` for the three self-review rounds and for
  triaging the PR comments.
- Domain skills per deliverable: D1/D2/D3 `pm-dev-frontend:javascript`, `pm-dev-frontend:css`,
  `pm-dev-frontend:jest-testing`, `pm-dev-frontend:javascript-security`; D4
  `pm-dev-java:java-core`, `pm-dev-java:java-lombok`, `pm-dev-java:javadoc`,
  `pm-dev-java:java-null-safety`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`,
  `pm-dev-java-cui:cui-logging`.
- Loaded as standards in this session: `pm-dev-frontend:javascript`,
  `pm-dev-frontend:jest-testing`, `plan-marshall:persona-implementer`. The remaining declared
  skills were resolved by `resolve` (they exist) but were not loaded into the session; the work
  followed the standards in `CLAUDE.md` for them.
- `skills added: none named beyond the spec`; surface drift beyond the spec's Expected Surface:
  `pm-api` catalogue files (`common`, `decisions`, `onboarding`, `settings`, `system`,
  `plan_view`, `start`), `pm-web-app/src/lib/i18n/index.ts` (`hasMessage`),
  `pm-operator` (`StartCommand`, `StartDialogue`, `SystemLevel`, transcript),
  `pm-mock-runtime` (scenarios, `MockRuntimeEngine`), `pm-web-e2e` (baseline, two specs),
  `doc/design` (four text tables), reason: each follows from a deliverable (D1's vocabulary and
  lookup, D3's fixture titles, D4's texts and the console keys the shared messages were split
  into).

## Hypotheses

- "Views put API fields into the markup and no single display function exists" — **confirmed**
  (`pm-web-app/src/lib/views/SystemView.svelte:951,955-956` printed `w.role` and
  `w.state.toLowerCase()`; no `lib/display` existed).
- "The strings of CC-F-05 live in the shared catalogue, not in the views" — **confirmed** for the
  catalogue rows (`start.de.properties:176` duplicated the web key; `settings.de.properties:100`
  misspelled; `start.started.title` unquoted), **refuted** for the cleanup row (the mock served
  `ended_at: "ended 12 Sep"`, so the view added a second "ended") and for the system row (each
  message carried its own label).

## Findings

Self-review round 1 (10 findings): fixed the epic state, the three overview plan states, the
inspector signal, the guard's `$t` exemption, three stale design docs and the ProjectOnboarding
comment; refuted `{w.role}` (a plain configured string, documented in `enumFields.ts`); kept and
disclosed the `github.com` shrink; added the missing per-target snapshots; `displayError` has no
caller because no view prints an error code yet (the spec requires the function).

Self-review round 2 (4 findings): fixed the waiting plan reading "unknown", the raw off-track
action, the guard's `?.`/`??` blind spot with controls, and the missing change-map vocabulary.

Self-review round 3: clear of blocking findings; the German action words now follow
`plan.signal.action.*` and the consent channel reads the catalogue word.

PR #287 review (CodeRabbit, 7 inline findings + 1 follow-up + 3 nitpicks): the `gap` activity
word, the helper allowlist, the `displayState` stripping, the `||` decision, the aria-name scan,
the locale reactivity, the unit-id chip and `NodeNarrative`'s node kinds were fixed in
`d5fd44e8`; the cleanup-reminder follow-up was answered as a server-contract gap this plan does not
own (posted on the PR); its threads are marked "Addressed in commit d5fd44e8". Sourcery refused on
its own quota and is optional.

CI: green at `647d2370` and at `d5fd44e8`. Sonar: 0 new-code issues at `d5fd44e8`.

## Reviewers

- **coderabbit** (required, quota-limited): reviewed `647d2370` (`kind=reviewed`,
  `final_review_risk_coverage` names `647d2370`); the current head is `d5fd44e8`, so the standing
  review authorization applies — a qualifying review exists, every one of its findings is fixed or
  answered, its threads are "Addressed in commit d5fd44e8", it was re-triggered once after the last
  push and refused with "Review rate limited" (recorded), and every other gate condition holds on
  `d5fd44e8`. Trigger history: `@coderabbitai review` at 14:05 local, review at 14:19 on
  `647d2370`, `@coderabbitai review` again at 15:31 on `d5fd44e8`, refused 15:35.
- **sourcery** (optional, quota-limited): refused on its own quota ("250,000 diff characters for
  the last 7 days; another review in 2 days 8 hours" this attempt). It does not gate the merge.

## Operator questions

1. Asked at the merge gate: "Merge PR #287?" with the facts of the final head; options "Merge now
   (standing authorization)", "Wait for CodeRabbit on the new head", "Stop before the merge".
   Selected: **"Merge now (standing authorization)"**.

No free-text operator messages were received.

## Rule check

- Guard: `guard: none on this harness (unenforced)`; the resolve result reported `guard: none`, so
  no guard-log line exists for this run and none is claimed.
- Shell/file rules: one command per call except one `| rg` pipeline used twice while reading the PR
  comment list (a violation of the no-pipes rule, recorded here); no heredocs, no `&&`, no `;`.
  Files were read and edited with the file tools throughout.
- Builds: every build went through the resolved `build` commands with an absolute
  `--project-dir`. One early routed build used a relative `--project-dir` and resolved to the main
  checkout (recorded, `maven-2026-10-31-114006.log`); every later call used the absolute path. No
  build was killed; the two long ones were re-attached with `build_server wait` as the client
  requires.
- CI: every CI action went through `plan-marshall:tools-integration-ci:ci`; no `gh`/`glab` call.
- `.plan/` state: only `.plan/temp/` was written, plus the `ci` body slots under
  `.plan/local/plans/NO_PLAN/work/ci-bodies/` and this one inbox message. The spec and the
  orchestrator tree were never edited.
- Repo state: every edit was made inside the worktree; the main checkout was only read (and pulled
  by the sanctioned `switch-and-pull` at cleanup).
- Spec read: `spec_source: ledger_worktree`; `spec_differ: true` — the main checkout's copy says
  something else and was not read (worktree-first default; the ledger copy is authoritative).
- `executor-skew`: `match` (executor 0.1.2036 = lane 0.1.2036).

## Build server

- Setup preflight: `ready`; nothing degraded, no `killed` job, no fallback used.
- `runtime_stamp_reason` was not carried; `plugin_version` is `unknown` and is reported as such.

## Residue

- ISO times remain in `/reports`, the completion report and `/settings/machine`; the text-guard
  baseline keeps those `iso-time` entries, which CC-F-04 (PLAN-16) owns.
- The cleanup reminder interval still has no write path (`cleanup_durations` is a single integer);
  answered on the PR as a server-contract gap.
- `state.node_kind.*` sits beside the older `plan.node.kind.*` family; both are used and neither
  contradicts the other, but nothing keeps them in step.
- `displayError` has no production caller until the error pattern of PLAN-45 lands.
