envelope_version=1
sender_type=plan
sender_id=rb-24-cli-proof-of-presence-decision
epic=ui-improvements
kind=landing
created=2026-11-16T06:48:04Z
revision=1
amended=2026-11-16T06:48:37Z

```landing-facts
schema=landing-facts/1
plan_id=rb-24-cli-proof-of-presence-decision
epic=ui-improvements
pr=n/a
merge_state=n/a
cleanup_owed=false
deliverables_total=3
deliverables_done=0
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2135
steps=setup:not-run,implement:not-run,build-gate:not-run,self-review:not-run,pr:not-run,reviews:not-run,merge:not-run
unresolved_review_threads=unknown
review_body_findings_total=unknown
review_body_findings_handled=unknown
sonar_open_new_issues=unknown
step.sonar-roundtrip.open_issue_rules=unknown
step.sonar-roundtrip.quality_gate_status=unknown
```

guard: none on this harness (unenforced)

# PLAN-24 stop-landing — held, not emitted

The run stopped before the worktree existed. It did not stop on a budget, a build failure or a
reviewer; it stopped because the spec's own re-grounding note forbids emission and the operator
selected "keep held" after a fresh re-check.

## Why: the spec's own disposition

`PLAN-24`'s re-grounding section of 2026-11-10 (the section that supersedes its spec text) states
_both_ halves of the condition:

- "Disposition: stays HELD (the operator's), re-ground again when the L-1 milestone is planned."
- "Emit only on an operator decision recorded on L-1."

Deliverable 1's Done-when is the same bar from the other side: "The ADR exists and the pull request
records the operator's decision". Neither half holds today, so a run would have written a decision
record over an undecided requirement.

## Re-check at 2026-11-16 (what the operator asked for)

The operator reported that three other plans landed and asked for a re-check. Every fact below was
read today, against `plan-marshall-documentation` `origin/main` at `4dd67ea`, not against the
2026-11-10 note:

| Fact | State today | Where |
|---|---|---|
| L-1 deferred, no milestone | unchanged; all three boxes unchecked | `doc/later-improvements.adoc:17`, `:23-25` |
| PM-SEC-3 | unchanged; "No passphrase, operator session, or operator credential exists" | `doc/requirements/08-security.adoc:56` |
| Residual-risk paragraph | unchanged; the user proof is still deferred | `doc/requirements/08-security.adoc:59` |
| Paired-device roles / step-up | unchanged; still deferred | `doc/requirements/08-security.adoc:162` |
| Roadmap | schedules no L-1 work; cites L-1 only as a Later Improvement | `doc/roadmap.adoc:196`, `:696` |
| ADR home | still none: no `adr/` or `decisions/` directory under `doc/`; the only ADR convention is `pm-mcp-core` `doc/adr/`, cited at `doc/developer/project-setup.adoc:97` | the spec's own note at `:76-77` flags this gap and it is still open |
| Deliverable 3's surface | `doc/plans/proof-of-presence.adoc` still absent; `doc/plans/README.adoc` most recently changed by PLAN-162 (#444) | this repository |

The landed plans of the interval (#445 PLAN-149, #449 PLAN-146, #452 PLAN-150, with #446 and #450
alongside them) are web-foundation and navigation work. None of them touches `L-1`, `PM-SEC-3` or the
operator-channel requirement, so the one condition the hold names — the milestone being planned —
is exactly as unplanned as it was on 2026-11-10.

The spec's own dependency condition is already satisfied and is not what holds it: PLAN-14
(`cli-gate-scope`) landed as #277.

## Deliverables

Nothing was implemented, so no Done-when is met. Every Done-when of this spec carries
`evidence order: none`, which is the only order a held plan can carry — and `none` on a Done-when
that says nothing about failing at a sha is still a claim this run did not earn, because the work it
describes does not exist.

1. **Decision record** — not met. No ADR written; no operator decision among paired-device
   confirmation, OS user-presence check and operator-only keychain token was recorded. `order: none`.
2. **Requirement change** — not met. `PM-SEC-3` and `L-1` are unchanged; no `doc-review` run over a
   changed requirement document. `order: none`.
3. **Work package** — not met. `doc/plans/proof-of-presence.adoc` does not exist and no row was added
   to `doc/plans/README.adoc`. `order: none`.

Per-head test runs: none — no head was pushed, so there is no `module-tests` or `test-compile` sha to
report.

## Personas and skills

Persona applied: `plan-marshall:persona-documenter` for the spec reading and the re-grounding, since
all three deliverables are documentation work and each declares that persona. No implementation step
ran, so no deliverable's `Skills:` set was loaded for editing: `pm-documents:ref-asciidoc` and
`pm-documents:ref-documentation` were resolved and exist, but applying them to text that the hold
forbids writing would have been work without a mandate. Skills added for surface drift: none.

## Hypotheses

The spec carries no `HYPOTHESIS` line; its Claim Labels are two OBSERVED/HOLD entries and one
process clause. Their material claims were re-read at HEAD rather than inherited:

- OBSERVED (`doc/claude_review/cli/cross-cutting/plan.adoc:58-65`, `proposals.adoc:97-129`) —
  **confirmed**: CLI-CC-S-06 names the three steps, CLI-CC-P-05 names the three options cheapest
  first (paired device, OS user presence, operator-only keychain token) and marks the pair
  "later".
- HOLD (`design-audit-001`, milestone unplanned) — **confirmed today**, at
  `doc/later-improvements.adoc:17-25`; the hold is not stale.
- The 2026-11-10 gap note ("the ADR has no home") — **confirmed today**: there is no ADR directory
  in `plan-marshall-documentation`.

## Findings

- Self-review rounds: none ran, because there is no diff to review.
- CI and PR comments: none exist, because no pull request was opened.

## Reviewers

| Reviewer | Role | State |
|---|---|---|
| coderabbit | required | not triggered — no PR exists |
| sourcery | optional | not triggered — no PR exists |

No quota policy applies: nothing was asked of either reviewer, and nothing can be merged.

## Operator questions

One question was asked, in order:

1. Text: "PLAN-24's own re-grounding note (2026-11-10) says: 'Disposition: stays HELD (the
   operator's), re-ground again when the L-1 milestone is planned' and 'Emit only on an operator
   decision recorded on L-1'. Deliverable 1's Done-when also requires the pull request to record your
   decision among three options." The question carried the verified ground facts and the two
   structural facts (deliverables 1 and 2 land in the sibling repository, whose checkout is on
   `docs/next-ingestion-12`; deliverable 3 lands here), then asked how the run should proceed.
   Options, in order: "Keep it held — do not emit"; "Emit: paired-device confirmation"; "Emit: OS
   user-presence check"; "Emit: operator-only keychain token". **Selected: "Keep it held — do not
   emit".**

Free-text operator messages, listed separately as free text:

- "re-check now. three other plans did land" — an instruction to re-ground, not an answer to the
  question above and not a selection of any other option. It was carried out; its result is the
  re-check table in this report, and it did not change the disposition the operator had selected.

## Rule check

**spec_source and spec_differ.** `resolve` read the ledger worktree copy
(`.plan/local/worktrees/_orchestrator/.plan/orchestrator/ui-improvements/plans/PLAN-24-cli-proof-of-presence-decision.md`)
and carried no `spec_differ` marker, so the main checkout's copy does not say anything different.
Both the setup-time and the landing-time resolve agree.

**executor-skew.** `match`, executor `0.1.2135` and lane `0.1.2135`; nothing was asked about it. The
lane's own admonition about a stale executor template does not apply: this run executed no verb the
lane text introduced.

**Guard.** `guard: none on this harness (unenforced)` — OpenCode has no pre-tool-use hook, so no
guard log line exists for this run and "all kept" is not a claim this run may make.

**Rules this run did not keep.** The run's own shell calls are counted here, not only what a guard
would have recorded:

- _Shell: one command per call, no pipes; files read with the file tools, never with shell `ls`,
  `grep`, `head` or `sed`._ Broken repeatedly. Shell `ls` was used 8 times (epic directory, its
  `inbox` and `inbox/to`, the sibling checkout, `doc/plans`, `.plan/temp/...`, `../plan-marshall`);
  shell `grep` 3 times (`PM-SEC-3` in `08-security.adoc`, `PLAN-24` in `queue-view.md` and in
  `epic.md`); `head` in 3 pipes (`landing-facts` search, the `landings` listing, the `ls-tree` pipe);
  `sed` in 1 pipe (`git show origin/main:doc/later-improvements.adoc | sed -n`). Each had a file-tool
  equivalent — `Read`, `Glob`, `Grep` — that the rule names.
- _Executor only: never the `git` `grep` subcommand._ Broken 4 times: `git grep` over
  `doc/roadmap.adoc`/`later-improvements.adoc`/`08-security.adoc`, over
  `doc/developer`/`doc/requirements`/`doc/specification`/`doc/concepts`, and twice scoped to a single
  file, plus one `git ls-tree … | grep`. The same searches were available through the `Grep` tool.
- _Stay inside the repository._ Two read-only `ls` calls named `../plan-marshall-documentation` and
  `../plan-marshall`. Both are sanctioned in substance — the spec's Write-Boundary names the sibling
  repository and the spec's own source citations live there — but the calls should have gone through
  the file tools like every other read. No credential file, config directory or environment dump was
  read.

Nothing else in section 1 was broken: no repository file was created or edited, nothing was staged
or committed, nothing was written under `.plan/` outside `.plan/temp/` and this landing, the spec and
the orchestrator ledger were not edited, and no `.plan/` ledger path was written by hand.

**Restarts and re-dispatches:** none. No sub-agent was killed; no self-review round was dispatched
(nothing to review).

## Build server

Setup preflight state: `ready` (the epic's project is enrolled and the daemon answers). Builds run:
none — the build gate was never reached, so there is no `daemon_longpoll` or `in_process_fallback`
count and no fallback to report. No `killed` job exists. `runtime_stamp_reason` was not present; the
stamp is fully read except `plugin_version`, which the runtime-info seam answers as `unknown` on this
install.

## Residue

1. **The HOLD is the plan's, not the lane's.** Re-grounding date to carry forward is 2026-11-16;
   the next re-check belongs to whoever re-grounds the epic when the L-1 milestone is planned.
2. **The gap at `:76-77` is still open and it is not a lane defect.** The ADR has no home in
   `plan-marshall-documentation`; the only ADR convention in the product is `pm-mcp-core/doc/adr/`
   (ADR 016 cited at `doc/developer/project-setup.adoc:97`). Whoever emits this plan must decide
   where the decision record lands before writing it — `pm-mcp-core/doc/adr/` is the only candidate
   that exists today.
3. **Two repositories, one lane.** Deliverables 1 and 2 land in `plan-marshall-documentation` through
   its own pull request while deliverable 3 lands here, so a future run needs two worktrees and two
   PRs; the lane's worktree/PR/merge machinery is anchored to this repository only. That is a
   limitation of the emission, not something to work around silently at emit time.
4. **The spec's option set and L-1's differ.** `proposals.adoc:124-127` offers paired-device
   confirmation, OS user presence and an operator-only keychain token; `later-improvements.adoc:23-25`
   offers an operator passphrase, OS user presence and device roles with step-up. OS user presence is
   the only option both name. The operator decision this plan waits for has to reconcile them, and the
   requirement change has to say which entry text wins.
5. **Inbox read defect.** `resolve`'s `inbox_read_command` cannot be used for this plan: the plan id
   `24-cli-proof-of-presence-decision` begins with a digit and the reader rejects it with
   `invalid_target_plan` (pattern `^[a-z][a-z0-9-]*$`). The mailbox directory is keyed by sender
   (`inbox/to/{sender_id}`), and `inbox/to/rb-24-cli-proof-of-presence-decision` does not exist, so
   this run read no mail — but it could not look either, and this landing reports that rather than
   calling the mailbox empty. The same pattern will break every numeric-first plan id.
6. **The landing reads INCOMPLETE at six required keys — by design, and named.** `inbox
   landing-check` on this message answers `complete: false` and names `total_tokens`,
   `plugin_version`, `unresolved_review_threads`, `review_body_findings_total`,
   `review_body_findings_handled` and `sonar_open_new_issues`. Each is `unknown` for a reason this
   run can state: no phase of work ran, so no token total was measured; the runtime-info seam
   answers `plugin_version` as `unknown` on this install and the resolve result is the only sanctioned
   source for it; and no PR ever existed, so the three review numbers and the Sonar count had no head
   to be read from, while `n/a` is not a legal value for the first three nor for a Sonar-configured
   project. Writing zeros would have bought `complete: true` at the price of four false claims. The
   six gaps are the honest shape of a run stopped before its first commit.
