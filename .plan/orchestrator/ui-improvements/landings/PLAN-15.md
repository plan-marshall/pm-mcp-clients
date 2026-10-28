# Landing Analysis: PLAN-15 — CLI footer and help overlay

epic: ui-improvements
workstream: WS-05
pr: #256 (merge commit `65d8cc5ce97c8e9fe3faf0a6c08b751256f5f695`, branch `feature/15-cli-footer-and-help-overlay`)

> Landing record for one shipped plan. Source: inbox message `rb-15-cli-footer-and-help-overlay-001.md`
> (runbook lane, harness `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2021`), analysed
> 2026-10-28 against the merged PR, the CI checks, the merge diff, the named tests and the lane's capture
> set. The message was filed with `merge_state=open` when a network outage stopped the run and was amended
> in place after the merge (`amended=2026-10-28T18:19:07Z`); the amended text is the one analysed.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Clear every row | shipped-as-specified | `TuiScreenTest.levelChangeLeavesNoResidue` exists at the merge commit (`TuiScreenTest.java:39`); `TuiLayout` and `TuiScreenDriver` are in the merge diff. The before/after capture sets exist under `.plan/temp/terminal/15-cli-footer-and-help-overlay/{before,after}`. The failing-first run, the residue text and the 120×30 / 80×24, en / de matrix are the lane's claims; not re-run here. |
| D2 Footer by priority | shipped-modified: the 80-column clause is not met as written, as the plan reports | `TuiKeyRegistry` (+356 lines) and `TuiKeyRegistryTest` are in the diff. The plan view keeps the five keys and ends `? more`. The overview and the activity fit their whole footer in 79 columns, so they end `/ filter` and show `? help` instead of `? more`; the spec clause "ends `? more`" is wider than the design, which puts `? more` only where a cell had to be left out. The code follows the design. The mutation run is the lane's claim. |
| D3 Help overlay | shipped-as-specified | `TuiCapabilityIT.helpOverlayShowsAllKeys` (line 170) and `footerHasNoResidue` (line 223) exist, parameterised per level; `TuiLayout` draws the box (`helpBox`, `rowDetails`). The six PTY cases and the mutation run are the lane's claims. |
| D4 Mockups | shipped-modified: broader than specified, by an operator decision | All 28 files of `doc/design/terminal-client/` are in the merge diff, including `README.adoc`; the message counts 22 of them as header-attribute or list-spacing edits only (the diffstat shows 3 to 12 changed lines in most). The plan asked the operator (question 3, "make the whole directory compliant") and the operator chose it. `15-overview.adoc` gained 88 lines (the footer and help-overlay section). The doc-check counts (28 files, 381 links, 27 of 28 compliant) are the lane's claims. |

**Surface delta** (computed here from the merge diff against the declared Expected Surface, because
`inbox landing-check` answered `unmeasured`, since the message carries neither set): declared 8, realized
44, state `expansion_detected`.

- Declared and realized (7): `TuiKeyRegistry`, `TuiLayout`, `TuiScreenDriver`, `TuiKeyRegistryTest`,
  `TuiScreenTest`, `TuiCapabilityIT`, `15-overview.adoc`.
- Declared, untouched (1): `TuiScreen.java`. The padding fix lives in `TuiLayout`; the plan says the same.
- Realized, never declared (37): the other 27 documents of `doc/design/terminal-client/` and its
  `README.adoc`; `pm-api/.../messages/operator.de.properties` and `operator.en.properties`;
  `EpicViewLevel`, `PlanViewLevel`, `SystemLevel`; `ReportsFullScreenTest`, `SystemFullScreenTest`,
  `ScreenCaptureIT`, `TuiLayoutTest`; and the screen-capture fixture
  `screen-captures/15-cli-footer-and-help-overlay.txt`.
- The landing says `skills added: none — no surface drift beyond the spec's modules and the terminal design
  documents`. The diff matches that for modules (`pm-operator`, `pm-api` catalogue, `doc/`); the file-level
  surface is far wider than declared.

## Metrics and Anomalies

- Tokens: `unknown` (not measured on this harness, recorded as unobserved, not as zero). `plugin_version`
  `unknown`.
- Anomalies:
  - A network outage stopped the first run before the push; the operator chose "retry" (ten attempts failed,
    the run stopped), and a resume pushed, merged and amended the message. The GitHub CLI was unauthenticated
    for a short time and cleared by a retry.
  - Self-review ended at its limit: three rounds, `self-review: budget-exhausted`; the operator chose to
    proceed to the PR.
  - The first `build / sonar-build` of `3fe48cf` failed on Sonar infrastructure ("Organization is not allowed
    to access data from non main branches"); the re-run of the same head passed.
  - The one routed build failed because the daemon invoked a bare `mvn` that is not on its PATH; every later
    build ran `in_process`.
  - Six operator questions in all (executor skew, dirty main checkout, D4 scope, self-review limit, outage,
    unauthenticated CLI).

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `3fe48cf` (4 actionable comments, 1 nitpick) and `f11fb4c` (one outside-diff
  finding); all fixed and answered. Its re-triggers after the later pushes were refused for rate limit, so the
  merged head `f9befd65` carries no CodeRabbit review; the lane applied the standing review authorization.
  Sourcery refused on its quota and does not gate.
- CI/merge: `ci pr view 256` reads `state: merged`, merge commit `65d8cc5c…`, the commit on `origin/main` right
  after `4efa0314` (PLAN-19's merge `cc5794c9` followed it). All 30 checks on #256 are `pass` or `skipping`,
  including `build / build (25)`, `build (26)`, `quarkus-alignment`, `sonar-build`, `integration-tests /
  test` and `build / conclusion`. Merged through the queue only.
- Sonar: the landing reports `open_issue_count=0`, `open_issue_rules=none`. Measured at the merged PR:
  `sonar_rest gate-status --pr 256` is `OK` (new coverage 91.5 %, duplication 0.0 %, ratings 1), the
  SonarCloud check passes, and the Sonar MCP search for the PR returns no issues. Corroborated, no anomaly.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-15 --status shipped`
- [x] row `pr` stamped `256`
- [x] row `landing` stamped `landings/PLAN-15.md`
- [x] row `plan_marshall_plan_id` stamped `15-cli-footer-and-help-overlay` (the sender's derived name)
- [x] epic.md narrative reconciled against the queue rows
- [x] defects and watches opened, updated or retired (see Follow-Ups)
- [x] resume anchor updated in `resume_anchor.md`
- [x] `queue-view.md` regenerated

## Follow-Ups

- **Incomplete landing facts** (`total_tokens`, `plugin_version` unknown): recurrence on the Open Defect.
- **Surface under-declared; the catalogue is shared with the plan in flight.** PLAN-15 changed
  `operator.de.properties` and `operator.en.properties` in `pm-api` without declaring them. PLAN-10 (launched)
  declares both, and so do PLAN-13, 14, 17, 21, 23, 30, 36 and 42. Recorded as a Watch; PLAN-10's lane merges
  the new catalogue on its own landing.
- **Staged specs that declare files PLAN-15 changed** are PLAN-17 (`SystemLevel`, `25-system.adoc`), PLAN-26
  (`25-system.adoc`), PLAN-20 (`19-actions.adoc`), PLAN-21, 23, 30, 32, 36, 42 and 48 (terminal documents), and PLAN-13 (`04-console.adoc`).
  Their premises were written against the old footer and the old document headers; they are to be re-read at
  HEAD before they are emitted. No spec was edited.
- **Dependents now unblocked on PLAN-15**: PLAN-38 (also needs PLAN-34) and PLAN-56 (also needs PLAN-30, 36, 42,
  48).
- **D2's 80-column clause** is wider than the design (`? more` only where a cell was left out). Open point for
  the CLI verification plan PLAN-56, whose `CLI-UX-V-02` clause checks help overlay and footer by priority:
  take the design as the criterion, not the clause as written.
- **Pre-existing, untouched**: the `-` placeholder in the plan view's task row
  (`running · - · 6 min`), and the `deprecated_xref` false positive in `19-actions.adoc` that keeps one
  document out of the validator's compliant set.
- **Process deviations of the run** (shell file reads and file operations in the lane; `mkdir`, `cp -R`, `mv`,
  `rm -f`): recorded here only. The guard was `none` on this harness, so nothing enforced them.
