# Landing Analysis: PLAN-167 — Specifications of the client modules move into this repository

epic: ui-improvements
workstream: WS-01
pr: #480 (merged as 5353b0fd, 2026-11-18); with #473 `3f912c18` (the cut table), pm-mcp-core #501 `4680161c`, plan-marshall-documentation #192 `72ebf20c`, and in this repository #490 `ecd39923`, #497 `fe0f9805`, #500 `5b4c5ae7`

> Landing record for one plan that ran as seven pull requests in three repositories. Written by `analyze` (inbox message `rb-167-specs-move-into-clients-003`, revision 1, `landing-check` complete: false, six keys `unknown`) after verifying claims against ground truth. Message `-002` (the resumed run's stop at the merge gate) is superseded by this one.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Lane's grade | Verdict | Evidence |
|--------------------|--------------|---------|----------|
| D1 the cut table | met | met | 205 parsed rows for 205 headings, per document (89 clients / 91 plan-marshall-mcp / 25 stay); the table is deleted in #480. Minor: no row for `runtime-model/04`, `mcp-tools/01-02`, which C touched |
| D2 the ADR | met | met, wording defects (L3-6) | ADR states the three rules and the choice on `doc/design/`; holder table; `doc/design/README.adoc:10` one paragraph; #490 cites commit `3f912c18` and made the two design READMEs say what the ADR says |
| D3 the specifications move | met | met-modified (licensed) | link-only except the licensed `#bundle-content` removal and the `03-web-app.adoc` heading/breadcrumb; one small non-link addition the lane did not grade: `03-web-app.adoc:17,19` turned two `<<id>>` into `xref:` with new link text ("The Chat's Text" vs the source "The chat's text") (L3-8). Index counts match the files |
| D4 watch and traceability | met | **met-modified** | ids exact (9/3/4 moved, 18 stayed, none lost or duplicated); 57 entries 01→16 under the file that defines each; **four entries name code that does not exist** (L3-2), against the document's own rule "a line is never invented" |
| D5 links and instructions | met | **met-modified, the lane's "link lines only" is wrong for the plans** | 20 design files link-only (line comparison, 0 non-link differences); 14 of 15 plan files carry one rewritten sentence besides links — licensed for the ten named plans and `epic-clients.adoc:54`, **not** for three more (`cli-unit-validate.adoc`, `diagrams.adoc`, `start-lesson-untrusted.adoc`; `diagrams.adoc` re-read); the lane's "35 files" is right |
| D6 the documentation repository | met | met, one wrong target left (L3-3), test-first not evidenced | moved documents and rows gone; the trimmed `02-bundle-manifest.adoc` holds exactly source lines 35-44 with `#bundle-content` and `shim-skill`; the ten wrong retargets in `module-structure.adoc` corrected; skills, `CLAUDE.md`, `project-setup.adoc` name the new home; `test_trace.py` has the two named tests (13 in all); **test and extension are one commit (`ee850d0`)**, so the commit order cannot show test-first, the red run rests on the lane's progress record; the `trace.py` run itself not reproduced |
| D7 the links of `pm-mcp-core` | not fully met (clause 3) | **not met (clauses 1 and 3)** | #501 is 45 files, 96/96, link lines only except one dropped `#web-app` fragment and `xref:`→`link:` on seven lines (the lane says five); the spec counted 46 files and 106 links and the lane does not reconcile the difference; **`harness-profiles.adoc:13` still links the family index `harness-integration.adoc`, which #192 deleted (re-read)** — clause 1 is false; merge order B 15:13Z, C 15:55Z, D7 16:06Z (re-read) — clause 3 not met, as the lane says |

`deliverables_done=6` of 7 matches the table of the lane, but the table is generous at D4, D5 and D7: by the analysis D1-D3 and D6 are met, D4 and D5 met with defects, D7 not met.

## Three-Layer Analysis

Read on three layers by one read-only leaf (level 3, 42 + 30 + 19 numbered points); the orchestrator re-read M1, M2 (the four Traceability entries and the code they name), M3, the merge order, the D5 plan sentence at `5353b0fd`, and the review and Sonar numbers. By reading and by the leaf's own read-only scripts: no build, no test run, no write. **Not read by the leaf:** the progress record, the `trace.py` runs and bodies, `.claude/`-side links of the documentation repository (13 pre-existing broken), 194 `{attr}`/plan-marshall-mcp link targets, `doc/design/**` below the READMEs apart from the link-only comparison, the section-by-section text of the eight moved documents apart from the line comparison, 45 of the 57 Traceability entries, CI and build-server logs.

- **Dispatch:** one read-only leaf — seven pull requests in three repositories and a mechanical-move claim that needs a real line comparison, not a count.
- **Verdict:** no blocker. Layer 1: two findings the lane graded wrongly (D5 "link lines only", D7 clause 1), one unlicensed non-link edit. Layer 2: the mechanical claim holds for every moved document. Layer 3: three major (one wrong link in `pm-mcp-core`, four Traceability entries for code that does not exist, one wrong target in the documentation repository), reported to the operator, none staged.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | D5: all 35 files carry "link lines only" | **partly refuted** | against the lane | 14 of 15 plan files carry one rewritten sentence; three of them were not licensed by the spec's amendment; the 20 design files are link-only |
| L1-2 | D7 clause 1 met | **refuted** | against the lane | `harness-profiles.adoc:13` of `pm-mcp-core` (named by the spec) was not touched by #501 and links a deleted file (L3-1) |
| L1-3 | D7 clause 3 not met | confirmed | with the lane | B 2026-11-18T15:13:15Z, C 15:55:05Z (created 15:54:54Z, merged eleven seconds later), D7 16:06:51Z; D7 was created 15:35:41Z. The enqueue time of #501 was not read |
| L1-4 | D4 "names for each requirement the code or test, or says it has none" | **refuted for four entries** | against the lane | L3-2 |
| L1-5 | D7 "anchors kept" | **refuted at one link** | against the lane | `model-work/11-decision-records.adoc` goes from `03-web-server.adoc#web-app` to `03-web-app.adoc` without the fragment |
| L1-6 | D6 test-first | **not evidenced** | open | the commit history cannot show test before extension; the lane's red run is its own record |
| L1-7 | amendment step 6: the landing states `review-gate` and Sonar facts for D7 and C at each merge head | **left out** | against the lane | only core-CI skip lines in section 8 |
| L1-8 | the lane's seven-deliverable count and "46 files / 106 links" of the spec | not reconciled | open | #501 changed 45 files and 96 lines; the missing file is the one of L3-1 |

### Layer 2 — Implemented

- **L2-1 (re-read by the leaf, per-line comparison against the source at `79ec3a6`):** `01-cli-and-installer` (55 changed lines), `02-operator-channel` (16), `03-relay` (13), `05-mock-runtime` (10), `06-event-catalogue` (14), `01-harness-detection` (5) and the `harness-integration` watch (4) differ in link targets only; `02-bundle-manifest` differs by exactly the licensed 11-line removal; the `Source::` lines of `PM-WATCH-RUN-10`/`-11` at `5b4c5ae7` equal the source text. The mechanical constraint is kept.
- **L2-2:** watch ids at the source cli 18 / runtime-model 12 / harness 4; here 9 / 3 / 4; in the documentation repository 9 / 9 / 0; none lost, none duplicated; the 18 that stayed are named once in `doc/ImplementationWatch.adoc`.
- **L2-3:** `pm-mcp-core` at `4680161c`: the remaining links to moved paths are the two staying anchors (`harness-profiles/01-profile-schema.adoc:130`, `model-work/04-worker-launch.adoc:252`), whole-document links to watch documents that still exist, and the one of L3-1.
- **L2-4:** #490, #497, #500 change only what the lane says (the ADR citation and index rule, the two design READMEs, `Specification.adoc` text, the Traceability regrouping, the two `Source::` reverts, `harness-integration.adoc:41`); no content change to a moved document.
- **L2-5:** link check of `doc/**/*.adoc` here at `5b4c5ae7` by the leaf's own checker: 4300 link occurrences, 194 not verifiable, **one broken** (`doc/design/terminal-client/04-console.adoc:27`, pre-existing); the lane's "3654" is not reproduced (a different counting, the conclusion holds). 89 of 89 cut-table anchors resolve except the known typo row 115.
- **L2-6:** the documentation repository's broken-link set is identical before and after #192 (71 both, the leaf's checker; none targets a clients file or a moved file).

### Layer 3 — Correct

Reported to the operator at once, never fixed inline.

| Id | Sev | Where | Defect | How verified |
|----|-----|-------|--------|--------------|
| L3-1 | **major** | `pm-mcp-core` `doc/specification/harness-profiles.adoc:13` | links `../../../plan-marshall-documentation/doc/specification/harness-integration.adoc`, the family index #192 deleted: a missing file in the repository that was meant to be link-clean | re-read at `4680161c`; the file is absent at docs `72ebf20c` |
| L3-2 | **major** | `doc/Traceability.adoc:140, 208, 271, 440` (here) | PM-WF-19 names `pm-operator` (`decision-log export`), PM-SKILL-4 `pm-operator` (the harness bundle installer and its shim skill), PM-CRED-2 `credentials migrate`, PM-TECH-4 `pm-exec` (Landlock **and Seatbelt**); a search of `pm-operator`/`pm-exec` main at `5b4c5ae7` finds no decision-log verb, no `migrate`, no `shim`, no Seatbelt (`Confinement.java` says macOS has no Landlock; `Os.java:20` "process group only") | re-read; Landlock itself is present |
| L3-3 | **major** | docs `doc/use-cases/01-onboarding.adoc:298` | the link to `harness-integration/02-bundle-manifest.adoc` now resolves to the trimmed file, which no longer holds the schema it is named for; the second amendment asked for the moved parts to be retargeted | re-read at `72ebf20c`; invisible to `trace.py` |
| L3-4 | minor | docs `03-web-server.adoc:140-145` | a stub `[#web-app]` heading stays; `roadmap.adoc:749`, `module-structure.adoc:224,229` still link the stub, so the id exists in both repositories | the leaf |
| L3-5 | minor | `doc/plans/epic-clients.adoc:28` | still says an accepted design is the specification of its client's implementation, against ADR 001 | the leaf |
| L3-6 | minor | ADR lines 44-45, 121-123, 145 | the moved parts "keep their paths" is false for the web app; the cut table is "deleted by the pull request that completed the move" (#480 deleted it, C and D7 came after); an unwrapped line | the leaf |
| L3-7 | minor | docs `specification/cli-and-security.adoc:63` | "Implement in" where the source says "Implemented in"; `project-setup.adoc` paragraph inserted between a sentence ending "what its setup is:" and its table | the leaf |
| L3-8 | minor | `03-web-app.adoc:17,19` | two new link texts, one in a different case from the source bullet | the leaf |
| L3-9 | minor | `doc/claude_review/cli/use-cases/proposals.adoc:132`; `Specification.adoc:21` | a historic note still says the specification lives in the documentation repository; an unwrapped long line | the leaf |
| L3-10 | minor | the `Source::` lines of `PM-WATCH-RUN-10`/`-11` (`runtime-model.adoc:53`) | plain text naming `doc/specification/evaluation.adoc`, a path that resolves only in the documentation repository; named by the lane, restored to the source text on purpose | the leaf |

### Conformance to the Project's Own Rules

| Rule | Verdict | Evidence |
|------|---------|----------|
| mechanical, nothing else | kept for the moved documents; **broken for three plan files (unlicensed sentence) and `03-web-app` link texts** | L1-1, L3-8 |
| watch ids kept, never renumbered | kept | L2-2 |
| the self-review before further change to #480 | **not kept**, said by the lane | the review ran after the merges; its six findings were all fixed in #490, #497, #500 |
| merge order B < D7 < C | **not kept**, said by the lane | L1-3 |
| no content change in the documentation repository's pull request beyond links | kept as far as read | L2-6 |
| a line is never invented (Traceability) | **broken** at four entries | L3-2 |

## Metrics and Anomalies

- Tokens: `unknown` (harness `opencode`, model `deepseek-v4.1-flash/high`, effort high, plugin version `unknown`, executor 0.1.2149): recorded as unobserved, not zero.
- Anomalies: C merged eleven seconds after it was created, before D7 was enqueued; the lane read build-server job logs outside the repository root and ran two shell file operations (from its own message, not checked); builds queued behind other sessions' jobs for 412-546 s and hit the daemon's 795 s bound twice.

## Routing and Merge Behavior

- Review gate, re-read with `runbook_lane review-gate --pr-number N`: **#480** `undecidable_read` (the CodeRabbit `Minor comments: 1` title, the known label gap; the operator authorised the merge on 2026-11-18); **#490** `all_handled`, 0 threads, 0 body findings (lane: reviewed, no actionable comment, **same**); **#497** `unresolved_review_threads=0`, `review_body_findings_total=2`, `handled=0` — the two findings are inline threads with an author reply each (fixed in `19cc0e03`; answered with the mechanical constraint), the gate reads them as undisposed because it counts no reply as a disposition (**the lane's "fixed or answered" holds, the gate's `body_findings_undisposed` is the known counting gap**); **#500** the same shape, one finding fixed in `053d7736`. For #192 and #501 the lane gives no gate facts and none were re-read (other repositories).
- Sonar: the lane gives `unknown` for every head (no analysis of a documentation-only head). Re-read through the Sonar search for the pull requests 480, 490, 497, 500: no issues. That is no analysed finding, not a measured 0: the lane's `unknown` stays the honest value.
- CI/merge: all five pull requests of this repository merged through the merge queue; checks not opened. CodeRabbit refused on quota three times before #490; its marker-bearing summary at `4b7f2df9` is the qualifying review. #488 stays closed unmerged as the superseded first attempt.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-167 --status shipped`
- [x] row `pr` stamped 480; row `landing` stamped `landings/PLAN-167.md`; row `plan_marshall_plan_id` stamped `167-specs-move-into-clients`
- [x] inbox: `-003` archived as consumed; `-002` archived as superseded by `-003`
- [x] epic.md: the PLAN-167 Open Defect entries amended; a new entry for this landing
- [x] resume anchor, `queue-view.md`, decision log

## Follow-Ups

- Layer 3 reported to the operator at once, none staged: L3-1 (one link in `pm-mcp-core`), L3-2 (four Traceability entries), L3-3 (one use-case link). Each is one line in a document; none is a code change. The place of each fix is the operator's call: L3-1 is a link line in `pm-mcp-core` (the same form as #501), L3-3 a link line in the documentation repository, L3-2 four parentheticals here; by the standing rule of 2026-11-13 a mechanical documentation fix is a direct session in a worktree with `skip-bot-review`, not a staged lane plan.
- The lane's named follow-ups, carried over and not staged: the daemon half's retargeting (four `plan-marshall-mcp` work packages still link the moved paths, read true by the leaf); the corpus-wide `#_…` anchor form; the code follow-ups (the enforcement-weakening gate in `ClientCommand`, `OperatorGate` deriving the harness signals from the `pm-api` table, symlink-safe target containment, the service unit in the manual upgrade procedure, nested-harness detection for markerless inner harnesses, `target_name` parent-directory rejection, the bundle-manifest example digest, the undefined `harness.probe-passes` state, the one-mode contract of the relay); the pre-existing `04-console.adoc:27`; the two `Source::` citations (the operator may ask for the link back).
- For plan-marshall: `review-gate` counts an author reply to an inline finding as undisposed (`body_findings_undisposed`, now also on #497 and #500; the gate and the lane disagree); the label gap (`undecidable_read`) for CodeRabbit's `Minor comments` title; landing narratives that claim test-first the commit history cannot show (now four landings: PLAN-66, 30, 151, 167); `landing-check` `unknown` tokens and plugin version; the mailbox read refusing a plan id with a leading digit.
