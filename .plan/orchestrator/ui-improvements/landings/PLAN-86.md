# Landing Analysis: PLAN-86 — Shared: One Text Set per API Error Code (Title, Cause, Fix) for Both Clients

epic: ui-improvements
workstream: WS-01
pr: #283 (merge commit `1b6c6ae37d7ea726354eeba1742323dcdd3f710c`, squash merge)

> Landing record for one shipped plan. Source: inbox message `rb-86-shared-error-texts-001.md` (runbook lane, harness
> `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2036`), analysed 2026-10-31 against the merged PR, the CI
> checks, the merge diff and the Sonar gate. The per-head build logs and the three self-review rounds are the run's
> account and are not re-run here.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 The texts and the lookup (area `error`, three keys per code in `en` and `de`, the unknown fallback, `ErrorTexts`) | shipped-as-specified, count corrected | `error.en.properties` (+187), `error.de.properties` (+188), `ErrorTexts.java` (+142), `ErrorTextsTest.java` (+142), `MessageCatalogue` registers the area, `texts-and-languages.adoc` declares it. `ErrorTextsTest` has 8 tests (every code carries title, cause and fix in both languages; no key names a non-member; the unknown fallback; the lookup for a member, an unknown wire string and a null code). `test-first`: failing run at the base (`Tests run: 8, Failures: 7`), green after. |
| D1 wording review against `texts-and-languages.adoc` | met | Recorded in the progress record with all titles in both languages; the titles are also in the PR body. |
| D1 `./mvnw -pl pm-api verify` | met | Green at the merge head (668 tests). |
| D1 open-issue read | met | `open_issue_count=0`, `open_issue_rules=none`, analysis revision = head; corroborated below. |

`deliverables_done=1` of 1 is consistent with the diff.

**The count in the spec was wrong, and the error is mine:** the spec said 43 codes ("counted by their wire literal"). The
enumeration holds **42 members**; the run counted them. My count was a text search for the literal pattern and overcounted
by one. The shipped set is
42 members plus the `unknown` fallback, which is the 43 text sets the spec named. The spec's own hypothesis (some codes
never reach a person) held for 4 of the 42 (`invalid_arguments`, `dependency_unanswered`, `cli_only`, `loopback_only`:
short, generic texts) and was refuted for the other 38, which answer an operator act or a visible state.

**Surface delta** (computed here from `git diff 1b6c6ae3^1 1b6c6ae3`, because `inbox landing-check` answers
`unmeasured`): declared 6 entries, realized 6 files. Five declared entries were changed or created (`error.en`,
`error.de`, `ErrorTexts.java`, `ErrorTextsTest.java`, `texts-and-languages.adoc`). One declared entry is untouched:
`ApiErrorCode.java` (read, not changed). One file is undeclared: `MessageCatalogue.java` (+2/−1, it has to know the new
area).

## Metrics and Anomalies

- Tokens: `unknown` (not measured on this harness, recorded as unobserved, not as zero). `plugin_version` `unknown`.
  `executor_version` `0.1.2036`.
- Anomalies:
  - **Executor skew at the start, answered in free text:** `skew` (`0.1.2032` against `0.1.2036`); the operator answered
    "already regenerated" and the re-check read `match`. Third lane in a row to report it (PLAN-80, PLAN-77, PLAN-86), all
    started before the regeneration.
  - **Every routed build failed and ran in process:** each `auto` build answered `execution_failed` (exit `-1`, 0 to 1 s,
    empty log) and the run used `--execution-mode in_process`. PLAN-77 ran its 23 builds on the daemon in the same hours,
    so the bare-`mvn` failure is not constant (open Build-server watch).
  - **A spec-level command that does not work alone:** `verify -pl pm-operator -Pintegration-tests` resolves `pm-api` from
    the installed snapshot in `~/.m2`, whose `ActivePlanSummary` had moved, and failed twice on that mismatch; with `-am` it
    passes. Recorded in the progress record. The same applies to the module commands named in the other staged specs.
  - **Self-review, three rounds** (8, 6, then the round limit with `budget-exhausted`): wording fixes in both languages,
    one glossary claim, the area declared in the shared design; one item open (R-02, below). The operator chose to open
    the PR with R-02 reported.
  - **Shell file operations by the run itself:** `ls` listings, one `which`, one `mkdir -p` and two `mv` calls that put the
    two property files aside for the failing run and back. Guard `none` on this harness, so the account is the run's own.
  - `spec_source=ledger_worktree`, no `spec_differ`.

## Routing and Merge Behavior

- Review: CodeRabbit (required, quota-limited) reviewed `5a65955` (two findings, both fixed in `cda6cf4e`: derive the test
  locales from `MessageCatalogue.SUPPORTED_LOCALES`, assert the null wire value for cause and fix too) and was refused on
  quota on the later heads. The merge head `d6cb2aef` differs from the reviewed head by those fixes and the `origin/main`
  merge; the merge ran under the standing review authorization after the operator said to enqueue. Sourcery (optional)
  refused on its own budget.
- CI/merge: `ci checks status --pr-number 283` is `success`, 20 checks. `origin/main` carries `1b6c6ae3`.
- Sonar: `sonar_rest gate-status --project plan-marshall_pm-mcp-clients --pr 283` answers `gate_status: OK`, six
  conditions OK (new coverage 100.0, duplicated lines 0.0, hotspots reviewed 100). A confirmed `0`, corroborated.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-86 --status shipped`
- [x] row `pr` stamped `283`
- [x] row `landing` stamped `landings/PLAN-86.md`
- [x] row `plan_marshall_plan_id` stamped `86-shared-error-texts` (the sender's derived name)
- [x] epic.md narrative reconciled against the queue rows
- [x] defects and watches opened, updated or retired (see Follow-Ups)
- [x] specs corrected: PLAN-30 (surface), PLAN-45 (surface), PLAN-09 (note)
- [x] resume anchor updated in `resume_anchor.md`
- [x] `queue-view.md` regenerated
- [x] inbox message `rb-86-shared-error-texts-001.md` archived after this record

## Follow-Ups

- **Incomplete landing facts** (`total_tokens`, `plugin_version` unknown): recurrence on the Open Defect.
- **F-02, PLAN-30's surface:** `doc/design/terminal-client/09-texts-common.adoc` (lines 169 to 208) and `03-dialogues.adoc`
  (line 266) still describe a `common.error.<code>` family and say the codes' texts belong to the flows that raise them.
  PLAN-30 adopts the terminal's refusal texts but did not declare either file; the two entries are added to its Expected
  Surface now, with a note that it rewrites those passages to point at the `error` area.
- **F-08, PLAN-45's surface:** it declared creating `ErrorTextsCompletenessTest`; that check is `ErrorTextsTest` of this
  plan. The entry is removed so the check is not built twice.
- **R-02, a decision for the operator:** the German texts use *Gate, Store, Terminal, Setup, Listener, Log* and
  *Audit-Kennung*, which are not entries of the glossary in `texts-and-languages.adoc` (that document says a new domain word
  is added there before it is used). The same words already run through dozens of German texts. Either the words are added to
  the glossary, which binds every plan's texts, or the 42 German texts are reworded. Recorded as a Watch.
- **PLAN-09 and PLAN-45 are unblocked on PLAN-86** (PLAN-45 also needs PLAN-35 and PLAN-09; PLAN-30 also needs PLAN-21, 23
  and 17). PLAN-09's note: `ErrorTexts` exists and `displayError(code)` reads the `error.<code>.*` keys; the web mock serves
  the same keys.
- **Specs whose declared files this merge changed** (re-read at HEAD before emit): `MessageCatalogue.java` is declared by
  none; `texts-and-languages.adoc` by PLAN-34 and PLAN-09 (their text rules).
- **Residue (accepted):** the tests of this plan take no input data (their values are contract literals), so no
  `Generators` appear; the redundant `static` on helper methods matches the module's `@UtilityClass` style.
- **Branch and worktree:** no `feature/86*` branch remains on the remote (`git ls-remote`) and the lane's worktree is gone
  (`git worktree list`), both checked after this record.
