envelope_version=1
sender_type=plan
sender_id=rb-167-specs-move-into-clients
epic=ui-improvements
kind=landing
created=2026-11-18T12:35:24Z
lifecycle=superseded
superseded_by=rb-167-specs-move-into-clients-003.md

```landing-facts
schema=landing-facts/1
plan_id=rb-167-specs-move-into-clients
epic=ui-improvements
pr=#480
merge_state=open
cleanup_owed=false
deliverables_total=6
deliverables_done=5
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2149
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:open
unresolved_review_threads=unknown
review_body_findings_total=unknown
review_body_findings_handled=unknown
sonar_open_new_issues=unknown
step.sonar-roundtrip.open_issue_rules=unknown
step.sonar-roundtrip.quality_gate_status=unknown
```

guard: none on this harness (unenforced)

# Landing — PLAN-167, resumed run (stopped before the merge)

## 1. Deliverables

- **D1 — the cut table, agreed: met.** Pull request A #473 merged as `3f912c18`; 205 rows for 205 headings, the operator confirmed the cut. Evidence order `none`.
- **D2 — the ADR of this repository: met.** `doc/adr/README.adoc`, `template.adoc` and `001-specifications-of-the-clients.adoc`; the three rules, the operator's choice (the design links to the specification), and the holder of every moved section. This run corrected the holder column to the current holder (`plan-marshall-documentation`) with the destination named separately, the family-index rule, and the CLI watch row. Evidence order `none`. Commits `4f0dc763`, `2f8bfc06`, `9994adfc`.
- **D3 — the specifications move: met.** `doc/Specification.adoc`, the three family indexes and the eight part documents; anchors kept; links rewritten. Content check recorded in the progress file: each moved document differs from its source only in link targets (plus the licensed `#bundle-content` block that stayed). The in-repository link check is clean; the authoritative `trace.py` check is D6's. Evidence order `none`. Commits `d4dbe7eb`, `070b392b`, `2880a49f`, `2f8bfc06`.
- **D4 — the implementation watch and the traceability: met.** Watch ids 9/3/4 here with their source ids unchanged; the 18 that stayed behind are named once in `doc/ImplementationWatch.adoc:43-46`. `doc/Traceability.adoc` carries the requirement entries, the four missing ones added (`PM-EXEC-1`, `PM-SEC-2`, `PM-CRED-3`), the generated anchor ids corrected and the `#bundle-content` holder corrected. Evidence order `none`. Commits `bd5b4f58`, `411981b4`.
- **D5 — the links and the instructions: met.** 25 design and planning files carry retargeted link lines; the 13 planning documents and `doc/plans/epic-clients.adoc:54` state the three homes. Evidence order `none`. Commits `dfa78093`, `5af37c61`.
- **D6 — the documentation repository's pull request: not met.** The branch `chore/167-specification-move` is pushed (76a1b4e, bc29579, 6c2fae3): the moved documents and their index rows are removed, and ~90 links are retargeted to the absolute `pm-mcp-clients` URLs. Not done: the remaining relative links inside `doc/specification/runtime-model/`, the `cli-and-security` index rows, the moved watch backlink ids, the three project skills, `CLAUDE.md`, `doc/developer/project-setup.adoc`, and the `test_trace.py` case with its failing run. Pull request C is not opened. `trace.py` was run and its result is recorded below.

## 2. Personas and skills

`persona-documenter` for every deliverable (the spec's own `Persona:` line). Domain skills per deliverable as resolved: `pm-documents:ref-asciidoc` and `pm-documents:ref-documentation`; D6 adds `pm-dev-python:python-core` and `pm-dev-python:pytest-testing`. `persona-code-reviewer` for the self-review dispatch and the PR-comment triage. `skills added: none`.

## 3. Hypotheses

- **harness-integration belongs to this repository — confirmed** (first run): the manifest schema, install/update/remove and the drift are `pm-operator` and the runtime; the bundle templates are model-facing content of `plan-marshall-mcp` (`module-structure.adoc#model-facing-content:79`).
- **The requirement modules carry `Implementation:`/`Verified by:`/`Documentation:` lines — refuted** (first run): only four such lines exist inline; traceability lives in each code repository's `doc/Traceability.adoc`.

## 4. Findings

- Self-review round 1 (dispatched this run): 24 findings — the broken sibling-link depths across the moved tree, the two Traceability anchors, the missing requirement entries, the drifted part inventory, the ADR holder rows and wording, the planning-document sentences, the two prose additions in moved documents, and the missing evidence record. All fixed or explicitly answered.
- 18 review threads: 8 fixed with the commit named in the reply, 10 answered with the reason (a code change the plan forbids, or content inside a moved document the mechanical constraint forbids). All 18 replied to and resolved.
- 2 review-body findings: the part-inventory drift fixed in `2f8bfc06`; the one-mode contract of `03-relay.adoc:22` answered as a follow-up.

## 5. Reviewers

- **coderabbit (required).** Qualifying review on `070b392b` (`kind=reviewed`, covering that commit); marked review-complete-after-handling. On `9994adfc` it is rate-limited ("next included review available in 56 minutes") and it posted one thread reply at 12:15:40Z saying it does not mark the security finding as addressed.
- **sourcery (optional).** Reviewed `9994adfc`: approved. Earlier refused on its own budget.

## 6. Operator questions

1. "PR #480 (D2–D5) cannot pass the lane's merge gate … How should I proceed?" — options: stop / continue deliverable 6 then decide / authorize enqueue. Selected: **continue deliverable 6, then decide**.
   (First run: three questions — family-index layout, the ADR's relation to the design, and the cut confirmation; and the PR A Sonar question, answered "enqueue, record the read as unknown".)

## 7. Rule check

- `guard: none on this harness (unenforced)` — nothing was enforced, so nothing is claimed as kept by a guard.
- `executor-skew`: `match` (executor 0.1.2149, lane 0.1.2149).
- Spec read: `spec_source: ledger_worktree`, no `spec_differ` marker.
- Rule breaks in this session: the resolve-supplied `inbox read` command is unusable (its plan id starts with a digit and the orchestrator refuses it as `invalid_target_plan`); the mailbox was read at the plan's own channel address instead, and this is recorded as a resolve defect. One thread reply was written twice because the first draft contained a typo.

## 8. Build server

Setup preflight `ready`. One build this run: `verify -Ppre-commit` on `5af37c61`, submitted with the default routing, `mechanism daemon_longpoll`, 5245 tests, compile/lint/test green, 654 s, job `bbfbcdc2`. No fallback, no retry, no `killed` job. `build-classify` answered `buildable` (the architecture rule, fail-closed), so the gate ran on a documentation-only head.

## 9. Residue

- **PR #480 is blocked at the merge gate.** `review-gate` answers `merge_allowed: false / undecidable_read` because its parser cannot read CodeRabbit's `<picture>… Minor comments: 1` section title (retried, same); and a resolved thread's last bot comment says the concern remains. The Sonar read of this documentation-only head is `undecidable` (`analysis_revision_unavailable`). CI is green; the local gate is green; all threads are handled.
- **Deliverable 6 is incomplete** as listed under D6. Its final `trace.py` run cannot be green before PR B merges: the checkout beside the documentation repository is on `main` and does not yet contain the moved documents, so every absolute URL into it reads `missing-file`. `trace.py` also reports `pm-mcp-core`'s links into the moved paths, which this plan's Write-Boundary forbids editing.
- Follow-ups named by this run and not done here: deriving `OperatorGate`'s harness signals from the `pm-api` table; symlink-safe target containment; the enforcement-weakening gate in `ClientCommand`; the service unit in the manual upgrade procedure; nested-harness detection for markerless inner harnesses; `target_name` parent-directory rejection; the bundle-manifest example digest; the undefined `harness.probe-passes` state; the one-mode contract of the relay; the two design READMEs that still call the design the specification; the two planning-document duplications CodeRabbit named.
