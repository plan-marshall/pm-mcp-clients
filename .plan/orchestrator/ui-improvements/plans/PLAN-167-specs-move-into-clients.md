# PLAN-167: Specifications of the Client Modules Move into This Repository

epic: ui-improvements
workstream: WS-01

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Written from the inbox message `doc-review-2026-11-17-001` (the doc-review session of `plan-marshall-documentation`, the operator's decision O1-A of 2026-11-17), adapted by the orchestrator on the operator's request of 2026-11-17 and checked against `plan-marshall-documentation` at `98f15fb`, `plan-marshall-mcp` at its `doc/specification-move-plan.md` (#125, merged) and `pm-mcp-core`.

## Objective

Move into `pm-mcp-clients` the specification content that the client modules (`pm-api`, `pm-relay`, `pm-operator`, `pm-exec`, `pm-mock-runtime`, `pm-web-app`, `pm-web-e2e`) implement, with its implementation watch items and the traceability of the requirements it implements, the way `pm-mcp-core` already holds its own (`doc/Specification.adoc`, `doc/specification/`, `doc/ImplementationWatch.adoc`, `doc/implementation-watch/`, `doc/Traceability.adoc`, `doc/adr/`). The operator's decision: "The spec should be completely in the implementation module. Here (documentation-repo) only the requirements should remain and point to the corresponding implementation / documentation." An implemented section stays where it is, condensed and pointing to the code (ADR 027 of `pm-mcp-core`), and is not deleted. The move is mechanical: no content change rides along, the anchors survive, every link is rewritten, and `trace.py` of the documentation repository reports `BROKEN LINKS 0` at the end.

## Deliverables

The plan carries six deliverables, the split guard's limit, and is cut into pull requests by repository and by order, which the spec licenses here: **pull request A = D1** (the cut table, merged first so the daemon half can read it), **pull request B = D2 to D5** (this repository), **pull request C = D6** (`plan-marshall-documentation`, opened beside B). Each stays under 100 files; a pull request that would exceed it is split again by document. The progress record says so.

1. **The cut table, agreed**
   - What: One table of every section (heading with its anchor) of every specification document that this move touches, each with exactly one home: `pm-mcp-clients`, `plan-marshall-mcp`, or stays in `plan-marshall-documentation` (the specifications of `pm-mcp-parent`: `module-structure.adoc` with its watch document, and `implementation-watch/cross-cutting.adoc`; the operator does not want them moved to the parent). The population is decided by the Modules table of `module-structure.adoc` through the "Specified in" column of every module, then read against the reviewer's reading (Claim Labels). A section is split at its heading, never inside a paragraph; no section is left without a home and none has two. The table is kept in this repository as `doc/specification-move-cut.md` (a plain file, deleted by the pull request that completes the move) and is the one table `plan-marshall-mcp`'s plan names as its task 2. The operator confirms the cut through the question tool before any document moves; the answer is recorded in the progress record.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - the table exists, and a count check proves its rows against the headings of the source documents: every heading of every listed document appears in exactly one row and the totals agree (evidence order: `none`; the method is recorded in the progress record: a count by the file tools, not a new program in the repository)
     - the operator's confirmation of the cut is recorded, naming the documents that are split (evidence order: `none`)
     - pull request A carries the table alone and is merged before pull request B opens (evidence order: `none`)
2. **The ADR of this repository**
   - What: `doc/adr/` with the first ADR of this repository, in the form of those of `pm-mcp-core` (`doc/adr/`, ADR 007 and 027 there): the specifications and their watch documents live in the repository that implements them; an implemented section is condensed and points to the code, never deleted; the documentation repository keeps the requirements, the use cases, the roadmap, `later-improvements`, the open questions and the articles; and **the relation of the specification to the existing `doc/design/`**: the design either becomes the specification (and the moved specification sections link to it) or links to the specification, never a second statement of the same thing. The choice is the operator's and is asked through the question tool at the outline with the evidence: how many sections of the moved documents state what a design document states (read from the cut table and the links of `doc/design/`), and what each choice costs in moved or rewritten lines.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - the ADR exists in the form of the `pm-mcp-core` ADRs, states the three rules above and the operator's choice on the relation to `doc/design/`, and names the document that holds each moved section's pointer (evidence order: `none`)
     - `doc/design/README.adoc` states the relation in one paragraph that links the ADR, and states nothing twice (evidence order: `none`)
3. **The specifications move**
   - What: `doc/Specification.adoc` and `doc/specification/` shaped like those of `pm-mcp-core`: the index with its traceability rules, and the documents of the cut table whose home is `pm-mcp-clients`, with every anchor kept and no wording changed. The reviewer's reading, for the cut to confirm or correct: `cli-and-security/01-cli-and-installer` (the relay binary, `pm-operator`, the harness bundle installer, the installation layout) and the client side of `02-operator-channel`; `runtime-model/03-relay`, `05-mock-runtime`, `06-event-catalogue` (the event catalogue is the message catalogue of `pm-api`); `harness-integration` (harness detection for the operator checks, and the bundle manifest the installer reads), unless the bundle content side belongs with the model-facing content in `plan-marshall-mcp`; and **the section `#web-app` of `cli-and-security/03-web-server`**, which the Modules table gives `pm-web-app` and `pm-web-e2e` as their specification although the file's other sections are the daemon's (a split at section boundaries). Links inside the moved documents are rewritten: to a document that stays or moves elsewhere in this repository, relative links; into `plan-marshall-documentation`, `pm-mcp-core` and `plan-marshall-mcp`, relative sibling paths counted from the file (`../../plan-marshall-documentation/doc/...`); the `Modules` sections of the moved documents keep their link back to the module rows of `module-structure.adoc`.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - the content check: every line of every moved document is found again in its new home, link lines compared after the link is rewritten; the method and its result are recorded (evidence order: `none`)
     - every anchor of the cut table's `pm-mcp-clients` rows resolves in its new file, checked by the documentation repository's `trace.py` against the checkout beside this one once D6's extension is in place, and by a link check of the moved documents until then (evidence order: `none`)
     - `doc/Specification.adoc` indexes every moved document, and the counts it states match the files (evidence order: `none`)
4. **The implementation watch and the traceability**
   - What: `doc/ImplementationWatch.adoc` and `doc/implementation-watch/` with the watch items of `implementation-watch/cli-and-security.adoc`, `runtime-model.adoc` and `harness-integration.adoc` whose anchors moved here; **watch ids are kept, never renumbered**; a watch item whose anchor is split is homed with the section it watches, and the cut table says which. `doc/Traceability.adoc` carries the `Implementation:`, `Verified by:` and `Documentation:` lines of the requirements that this repository's modules implement (the requirement modules of `plan-marshall-documentation`), where they exist, in the form of `pm-mcp-core`'s.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - every watch id of the moved items exists once, here, with the same id; none is renumbered, none is lost (a count of ids before and after, recorded) (evidence order: `none`)
     - the content check of deliverable 3 passes for the watch documents (evidence order: `none`)
     - `doc/Traceability.adoc` names, for each requirement the table's moved sections implement, the code or test it points to, or says it has none (evidence order: `none`)
5. **The links and the instructions of this repository**
   - What: Every link in this repository that points to a moved document or anchor is rewritten to the new relative path; links into the documentation repository stay relative sibling paths. About 30 files of `doc/design/`, `doc/plans/` and `CLAUDE.md` carry such links (a grep at `98f15fb`: `doc/design/README.adoc`, `web-app/chat.adoc`, `machine-settings.adoc`, `shared/system.adoc`, `shared/consents.adoc`, `terminal-client/19-actions.adoc`, `doc/plans/diagrams.adoc` among the heaviest; the lane re-reads the list at HEAD). **Link lines only**: no other line of a design document changes in this deliverable. `CLAUDE.md` of this repository states that the specifications of the client modules, their watch documents and the traceability now live here (its "Project" paragraph and the `project-setup.adoc` reading line), and no longer sends the reader to the documentation repository for them.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - a search for the old paths of every moved document finds no link in this repository (evidence order: `none`)
     - `git diff` of every design document touched by this deliverable shows link lines only (evidence order: `none`)
     - `CLAUDE.md` names the new home of the specifications, the watch documents and the traceability (evidence order: `none`)
6. **The documentation repository's pull request (beside B)**
   - What: In `plan-marshall-documentation` (its checkout beside this one, its own branch, its own pull request, its own gates per its `project-setup.adoc`): remove the moved documents and their rows in `Specification.adoc` and `ImplementationWatch.adoc` and point to this repository's index (absolute `https://github.com/plan-marshall/pm-mcp-clients/blob/main/doc/...` links); retarget every link to a moved anchor; update `CLAUDE.md`, `doc/developer/project-setup.adoc` and the project skills `traced-implementation`, `doc-review` and `ingest-ideas` for the new home; extend `.claude/skills/doc-review/scripts/trace.py` so that links, anchors, watch backlinks, traceability and index counts resolve against the `pm-mcp-clients` checkout beside it, as they do for `pm-mcp-core` (today it checks only the work packages there, with `--with-plans`), with its test (`test_trace.py`). The baseline of `trace.py` (plain and `--with-plans`) is taken on `main` of all checkouts BEFORE anything moves and saved outside the repositories. Order with the daemon half: the two halves run one after the other or share the documentation pull request; whichever merges second starts from the first one's merged state and keeps both sides' removals (the two documentation edits are on disjoint rows by the cut table).
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation, pm-dev-python:python-core, pm-dev-python:pytest-testing
   - Done when:
     - `trace.py` reports `BROKEN LINKS 0`, no undefined requirement or watch id, and watch and index counts that match, plain and with `--with-plans`, compared with the baseline: nothing new except the moved paths (evidence order: `none`; the baseline is taken before the move, the final run after it, both paths recorded)
     - `test_trace.py` covers the extension: a broken link into the `pm-mcp-clients` checkout is reported, and the matched positive control passes (evidence order: `test-first`)
     - no moved document and no index row of the cut table's `pm-mcp-clients` rows is left in `plan-marshall-documentation` (evidence order: `none`)

## Standing Constraints

- **Mechanical, nothing else (the operator).** No content change rides along in a moved document: no reworded sentence, no renumbered watch id, no moved anchor, no rewritten requirement. A needed content change is reported in the landing message as a follow-up, never made here. Link lines are the only lines that change, and each change is the path of the same target.
- **Order and the gate.** The gate of the message is **met as of 2026-11-17**: the review pull requests have landed (`pm-mcp-core` #484, `plan-marshall-documentation` #190 and #191) and the daemon half's plain plan is merged (`plan-marshall-mcp` #125, `doc/specification-move-plan.md`). The lane re-reads both at the start (`gh pr view` through the `ci` abstraction) and, if a review pull request is still open or `main` of a checkout is behind, stops and reports instead of moving a text that is about to change. The documentation edits of `pm-mcp-parent` (`module-structure.adoc` and its watch document, `implementation-watch/cross-cutting.adoc`) are NOT moved and NOT edited except for their links to moved anchors (link lines only).
- **The daemon half is not this plan's.** `runtime-model/01` and `04`, `cli-and-security/03-web-server` (except `#web-app`), the server side of `02`, `mcp-tools`, `evaluation.adoc` and their watch items belong in `plan-marshall-mcp`, which has no active orchestrator; its half is the plain plan `plan-marshall-mcp/doc/specification-move-plan.md` (a person or a run of that repository does it). This plan edits NOTHING in `plan-marshall-mcp` and in `pm-mcp-core`. The cut table of deliverable 1 is the shared table that plan's task 2 names; it reads the table from `main` of this repository once pull request A has merged.
- **Plans in flight.** The running plans that edit `doc/design/` (at the time of staging PLAN-152, PLAN-164 and PLAN-66) edit design documents this move touches by link lines only (deliverable 5): either their pull request lands before deliverable 5's, or deliverable 5 is rebased onto it; the lane merges `main` into its branch immediately before deliverable 5 and keeps both sides.
- No new dependency or plugin. No code is changed; no build gate runs on code that did not change, but the full gate of the repository's `project-setup.adoc` for a documentation-only pull request is run (read it at the start).
- Where the project analyses its pull requests with SonarCloud (`plan_marshall_pm-mcp-clients`): a documentation-only pull request carries no new-code issue to fix; the open-issue read at the head is still stated in the landing message (`sonar_open_new_issues=N`, `unknown` for a failed read, never `0`). A dismissal is made only on the operator's selection.
- Every pull request the plan opens carries the `Co-Authored-By: plan-marshall <noreply@cuioss.de>` trailer on its commits and no generator footer in its body.
- The text of the ADR and of the cut table is plain: one concept, one sentence, no marketing words.
- The lane reads the sibling checkouts (`../plan-marshall-documentation`, `../pm-mcp-core`, `../plan-marshall-mcp`) read-only, except `../plan-marshall-documentation`, where deliverable 6 works in its own branch and pull request.

## Claim Labels

- OBSERVED: the gate is met — `pm-mcp-core` #484 merged (`docs/review-2026-11-17`), `plan-marshall-documentation` #190 and #191 merged, `plan-marshall-mcp` #125 merged (`doc/specification-move-plan.md`) — read through `gh pr list` on 2026-11-17
- OBSERVED: the documents the reviewer's reading names exist in `plan-marshall-documentation` at `98f15fb` — `cli-and-security/01-cli-and-installer.adoc` (337 lines), `02-operator-channel.adoc` (154), `03-web-server.adoc` (161); `runtime-model/03-relay.adoc` (126), `05-mock-runtime.adoc` (95), `06-event-catalogue.adoc` (127); `harness-integration/01-harness-detection.adoc` (105), `02-bundle-manifest.adoc` (358); the watch documents `cli-and-security.adoc` (278), `runtime-model.adoc` (201), `harness-integration.adoc` (82) — read at `doc/specification/` and `doc/implementation-watch/`
- OBSERVED: `pm-web-app` and `pm-web-e2e` are specified in `cli-and-security/03-web-server.adoc#web-app` (`pm-web-app`), and `pm-web-e2e` in it and in `runtime-model/05-mock-runtime.adoc`, and `pm-mock-runtime` in `runtime-model/05-mock-runtime.adoc` — read at `doc/specification/module-structure.adoc` § Modules ("Specified in")
- OBSERVED: `pm-relay` and `pm-operator` are specified in `cli-and-security/01-cli-and-installer.adoc`, `02-operator-channel.adoc#operator-channel-isolation`, `03-web-server.adoc#web-server` (the `web` verbs), `harness-integration/01` and `02`, and `runtime-model.adoc` and `runtime-model/05` — read at `doc/specification/module-structure.adoc` § Modules
- OBSERVED: `pm-api` is specified in `runtime-model/01-processes-and-transport.adoc` (error codes, local API, start modes), `06-event-catalogue.adoc`, `cli-and-security/01#cli-implementation` and `#bundle-drift-notice`, `harness-integration/01#harness-detection-table` and the i18n section of `module-structure.adoc` — read at `doc/specification/module-structure.adoc` § Modules; `runtime-model/01` is a daemon-side document the reviewer assigned to `plan-marshall-mcp`, so the cut splits what `pm-api` implements from what the daemon enforces
- OBSERVED: the sections of `cli-and-security/03-web-server.adoc` named `#web-server` (daemon) and `#web-app` (client) are in one file — read at `doc/specification/module-structure.adoc` rows for `pm-mcp-server`, `pm-operator`, `pm-web-app`
- OBSERVED: `pm-mcp-core` holds its documentation as `doc/Specification.adoc`, `doc/specification/`, `doc/ImplementationWatch.adoc`, `doc/implementation-watch/`, `doc/Traceability.adoc`, `doc/adr/`, `doc/log-messages/` — read at `../pm-mcp-core/doc/`
- OBSERVED: this repository has no `doc/adr/`, no `doc/Specification.adoc`, no `doc/Traceability.adoc`; it has `doc/claude_review/`, `doc/design/`, `doc/plans/`, `doc/resources/` — read at `doc/`
- OBSERVED: about 30 files of `doc/design/`, `doc/plans/` and `CLAUDE.md` carry links into `plan-marshall-documentation` specifications — a grep for `plan-marshall-documentation/doc/specification` at HEAD of this repository on 2026-11-17 finds `doc/design/README.adoc` (2), `web-app/chat.adoc` (8), `machine-settings.adoc` (8), `shared/system.adoc` (12), `shared/consents.adoc` (5), `terminal-client/19-actions.adoc` (4), `doc/plans/diagrams.adoc` (7) and about 23 more with one to four
- OBSERVED: the plain plan of the daemon half lists the same table-first order (its task 2 "agree the section-level cut with the `pm-mcp-clients` run, as one table of sections and their target repository, before either side moves anything") — read at `../plan-marshall-mcp` `doc/specification-move-plan.md` § Tasks
- HYPOTHESIS: `harness-integration` (both parts) belongs to this repository rather than to the model-facing content of `plan-marshall-mcp` — the Modules table gives it to `pm-api` (harness-detection table), `pm-operator` (harness verbs, drift checks) and `pm-mcp-server` (bundle packaging) alike; confirm or refute at `doc/specification/harness-integration/02-bundle-manifest.adoc` § what the bundle manifest says the installer reads, and `module-structure.adoc` § model-facing content (verify-at-outline; the cut table records the answer and the operator confirms it)
- HYPOTHESIS: the requirement modules of `plan-marshall-documentation` carry `Implementation:` / `Verified by:` / `Documentation:` lines for the client modules to move into `doc/Traceability.adoc` — confirm or refute at `../plan-marshall-documentation/doc/requirements/` and `../pm-mcp-core/doc/Traceability.adoc` for the form (verify-at-outline; where none exist the document says so, it is not invented)
- Verify-first clause: the reviewer's reading was made on 2026-11-17 against the Modules table. Before the cut table is written, re-read every document above at the then-current HEAD of the three checkouts (a line count or an anchor that moved since is a finding), and re-read the Modules table's "Specified in" for every module of this repository; a part the table gives to a client module that the reviewer's reading gives to the daemon (or the reverse) is a finding in the progress record and goes to the operator in the cut's confirmation, never decided silently.

## Expected Surface

- OBSERVED: `doc/design/` — directory; link lines only (deliverable 5), about 30 files at HEAD of 2026-11-17
- OBSERVED: `doc/plans/` — directory; link lines only (`doc/plans/diagrams.adoc`, `exec-listed-read-set.adoc`, `exec-hardening.adoc` among them)
- OBSERVED: `CLAUDE.md` — present at HEAD of 2026-11-17
- HYPOTHESIS: `doc/Specification.adoc` — new (verify-at-outline against the `pm-mcp-core` form)
- HYPOTHESIS: `doc/specification/` — new directory
- HYPOTHESIS: `doc/ImplementationWatch.adoc` — new
- HYPOTHESIS: `doc/implementation-watch/` — new directory
- HYPOTHESIS: `doc/Traceability.adoc` — new
- HYPOTHESIS: `doc/adr/` — new directory
- HYPOTHESIS: `doc/specification-move-cut.md` — new, deleted by the pull request that completes the move

## Dependencies and Sequencing

- Depends on: none inside this epic. External gates (met on 2026-11-17, re-read at the start): `pm-mcp-core` #484, `plan-marshall-documentation` #190 and #191 merged; `plan-marshall-mcp` #125 merged.
- Overlaps with: the running plans that edit `doc/design/`: PLAN-152 (`doc/design/shared/decisions-texts.adoc` and the catalogue documents), PLAN-164 (the plan-changes design), PLAN-66 (`doc/design/web-app/onboarding*.adoc`). Link lines only; merge `main` before deliverable 5.
- Adjacent to: the daemon half in `plan-marshall-mcp` (its plain plan; not edited here) and `module-structure.adoc` (stays; link lines only).

## Resume Amendment 2026-11-18 (the first run stopped before the merge)

Source: the run's stopped-run message `rb-167-specs-move-into-clients-001` and `epic.md` Open Defects (the PLAN-167 entry). It supersedes nothing above; it states where the work stands and what the resumed run does, in order. A resumed run reads this section and its own progress file, and the message above is its record of the first run.

**State at the stop.** Pull request A (the cut table, D1) is merged (#473, `3f912c18`; 205 rows for 205 headings, confirmed by the operator). Pull request B (this repository, D2 to D5) is open as #480 (branch `chore/167-specs-move-into-clients-b`, 50 files, CI clean) with 18 unresolved inline threads and 2 review-body findings. D1 and D2 are met. D3 to D6 are not: the cut table is deleted and the eight part documents, the three family indexes and `doc/Specification.adoc` are in place, but the links are broken in three places, no content check is recorded, the Traceability has two wrong generated ids, ten planning documents still say the client specifications live in `plan-marshall-documentation`, and D6 is half done on the local branch `chore/167-specification-move` of `../plan-marshall-documentation` (one WIP commit `76a1b4e`, not pushed). The baseline of `trace.py` is taken (`BROKEN LINKS 0`, 120 requirements, watch counts 18, 12 and 4 for cli-and-security, runtime-model and harness-integration, `WATCH LINES WITHOUT ANCHOR 0`, plain and `--with-plans` identical): keep it, do not take it again.

**Order of the resumed run.**

1. Push the work-in-progress branch of the sibling repository first, and say so in the progress record: nothing of this plan stays unpushed in `../plan-marshall-documentation`.
2. Run the self-review (the lane's step 5) on the pull request B diff with the `persona-code-reviewer` sub-agent before any further change to the pull request. It was never run, so every Done-when verdict of the first run is the run's own and is re-checked by it.
3. Fix what the first run named, in this order, and answer each thread with the commit:
   - the three broken links: `doc/implementation-watch/cli-and-security.adoc:23` and its siblings use `../../plan-marshall-documentation` where `doc/implementation-watch/` needs one more hop; `doc/specification/harness-integration.adoc:26` uses one hop too many; `doc/specification/runtime-model/03-relay.adoc:65` links `04-sessions-and-workers.adoc` locally where it is not local (it belongs to the daemon half). Then check every relative link of the moved documents again, not only these three;
   - the two Traceability links that use `#_ownership_digest_verification_on_install_update_remove` where the generated id is `#_ownership_digest_verification_on_installupdateremove`;
   - the drifted part inventory in `doc/Specification.adoc` and the two wording points of the ADR;
   - the ten planning documents (`doc/plans/accept-edit-enrolment.adoc`, `harness-decision-log-hold-names.adoc`, `operator-command-tree.adoc`, `operator-drain-signal.adoc`, `relay-session-mode-removal.adoc`, `exec-listed-read-set.adoc`, `exec-socket-list.adoc`, `exec-hardening.adoc`, `landlock-on-wsl2.adoc`, `api-hidden-characters.adoc`) and `doc/plans/epic-clients.adoc:54`: one sentence each, link lines and that sentence only.
4. The content check of deliverable 3 is recorded: every line of every moved document is found again in its new home (link lines compared after the link is rewritten), with the method and its result in the progress record. A comparison made with the file tools counts; a count of lines alone does not.
5. The remaining threads and body findings of pull request B: fix, or answer with a stated reason. A finding that asks for a code change this plan forbids (`OperatorGate` deriving the harness signals from the `pm-api` table, symlink-safe containment of the target, the enforcement-weakening gate in `ClientCommand`) or that concerns a sentence moved verbatim by the "mechanical, nothing else" constraint is answered with that reason and listed as a follow-up in the landing message; it is not fixed here.
6. Finish deliverable 6 on the pushed branch: the remaining about 37 link rewrites, the three project skills (`traced-implementation`, `doc-review`, `ingest-ideas`), `CLAUDE.md`, `doc/developer/project-setup.adoc`, the `trace.py` extension with its `test_trace.py` (the test first, red, then the extension), and the final `trace.py` run against a checkout of this repository beside it, plain and `--with-plans`, compared with the baseline. The first run found that `trace.py` crashes on `roadmap.adoc` when it is run before the extension: the extension handles it, it is not a reason to skip the run. Open pull request C beside B.
7. The Sonar read of a documentation-only head is `undecidable` (CI skips `build / build` and `build / sonar-build`): the question tool names it and offers "enqueue and record the read as unknown" and "stop", as for pull request A (the operator chose "enqueue, record the read as unknown" there); the answer is recorded as the operator's selection. Merge B, then C, each through the gate of the lane.
8. The landing message says which of D3 to D6 each pull request carries, states the counts as `review-gate` facts at each merge head, and lists the follow-ups above. `deliverables_done` counts only fully met deliverables.

**The daemon half.** The documents this plan moved here contain links into documents that stay in `plan-marshall-documentation` until `plan-marshall-mcp`'s plan (`doc/specification-move-plan.md`) moves them (`runtime-model/01` and `04`, `cli-and-security/03` except `#web-app`, the server side of `02`, `mcp-tools`, `evaluation`). Those links point at their current home; the daemon half retargets them when it moves them. Say so in the landing message; do not edit `plan-marshall-mcp`.

**The first run broke the lane's rules (the message names them):** shell file operations, inline Python in the sibling repository, and the self-review never dispatched. The lane's rules apply unchanged to the resumed run; they are named here only so that step 2 and the file-tool reads are not skipped again.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-167-specs-move-into-clients.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests, and, where a deliverable says so (deliverable 6), the sibling repository `plan-marshall-documentation`. It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
