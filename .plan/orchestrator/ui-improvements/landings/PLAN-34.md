# Landing Analysis: PLAN-34 — Terminal: The Display Layer

epic: ui-improvements
workstream: WS-05
pr: #371 (https://github.com/plan-marshall/pm-mcp-clients/pull/371), merged as `40dc879e` (squash, 41 files, +756/-328; merge head `34675a2`)

> Landing record for one shipped plan. Written by the `analyze` verb (inbox scan, message
> `rb-34-cli-display-layer-001.md`, 2026-11-08) after checking the lane's claims against git, the PRs (read through
> the `ci` abstraction) and Sonar. The lane's report is a lead, never a fact.

## Deliverable Fidelity vs Spec

The lane itself reports `deliverables_done=1` of 4. It merged on the operator's "Merge now", with self-review not clear.

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Served order and the size of change | shipped-modified | Code and tests met: `OverviewFormatting.java:118,129,140` return the served order (`OverviewFormattingTest:74-105`); `PlanViewLevel.java:495-497` and `PlanCompletionLevel.java:199-206` show the change size only when one is served. Screen capture before and after: not produced. `ProcessTree` gained a `change` field (`ProcessTree.java:60`). The spec's premise that PLAN-82 already carried it was wrong. The mock serves it (`running-work.json:640`); no live producer exists in this repository |
| D2 Display | shipped-modified | `Display.duration` / `Display.time` match the rule table; `DisplayTest` tables in en and de. Screen capture: not produced. `Display.word` / `Display.title` exist but have **no production caller** |
| D3 Used everywhere | partial | See layer 2. Five other duration formatters, about 20 lone `-` placeholders, raw plan ids and wire tokens in the printed epic, the raw waiver gate (`ci_check:…`), and two clocks remain. `TuiCapabilityIT` was not extended. Captures and the Playwright regression were not produced |
| D4 The time forms in the client designs | shipped | The `git grep` over `doc/design/terminal-client` names only the rule form `2 months ago`; `pm-operator/src/main` is free of `just now|weeks? ago|yesterday`; `GermanAgeDisplayTest.germanAges` |

The files outside the Expected Surface (`running-work.json`, `DesignerRegistry.java`, `ProcessTree.java`, several tests) are each explained by D1, D3 or D4.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`, `orchestrator.analyze`). Reason: every full ship gets its own leaf; 41 files.
- **Verdict:** layer 1 mixed (2 minor premise findings); layer 2 partial, which confirms the lane's own 1 of 4; layer 3 found five minor defects, including a German regression, and no blocker.
- **Re-read by the orchestrator at `40dc879e`:**
  - `Display.java:240-250` (`d MMM` for every language) and `40dc879e^` `OverviewFormatting.java:49,373` (`SHORT_DATE_DE = "d. MMM"`);
  - `StatusCommand.java:459-474` (`Instant.now()`, `Clock.systemUTC()`);
  - `ProjectWorkLevel.java:304-312` (an empty unit slot);
  - `git grep 'Display.(word|title)('` in `src/main` returns 0;
  - `QuestionsCommand.java:123-127` (the raw gate);
  - `git diff --stat 40dc879e^ 40dc879e -- epicview/` is empty;
  - `EpicCommand.java:155,191` are unchanged.

  All confirmed.

### Layer 2 — Implemented (D3 residue inventory, at `40dc879e`)

- **Other duration formatters:**
  - `PlanViewFormatting.java:50-63` (called from `PlanViewLevel:465,616,646`, `PlanCompletionLevel:81`, `WorkerActivityLevel:135`);
  - `OverviewFormatting.java:247-261` (prints 2 d as `48 h`);
  - `SystemFormatting.java:57-68` (a literal `d` in German);
  - `PlanCommand.java:473-486,1186-1193`;
  - `EpicViewFormatting.java:247-257` (`eventTime`, which formats in the system zone; `Display` uses the clock's zone).
- **Lone `-` placeholders:**
  - `CredentialsCommand:138`;
  - `PlanCommand:932,1058`;
  - `PlanSettingsDialogue:120`;
  - `RolesCommand:173,250,278,410,419,510,512,521`;
  - `StatusCommand:254,538`;
  - `PlanCompletionLevel:75,190,261`;
  - `PlanViewFormatting:131`;
  - `PlanViewLevel:642`;
  - `WorkerActivityLevel:142`.
- **Raw ids and tokens:**
  - the printed epic: `EpicCommand:280,293-308`, with English literals `running plans:` and `none`;
  - `EpicViewFormatting:114-115` wait texts;
  - the waiver gate: `QuestionsCommand:125`, `AnswerCommand:126-128,254-256,319`;
  - a raw unit id when no `unit.<id>` key exists: `ProjectWorkLevel:308-310`, `StatusCommand:453`, `EpicCommand:329-331`;
  - the unit-title lookup is copied four times instead of using `Display.title`.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `Display.java:247` | a German moment older than a week | `12 Sep.` instead of `12. Sep.`: a regression from the removed `SHORT_DATE_DE` (`d. MMM`). `OverviewFormattingTest:266-269` asserts only `contains("Okt")` | minor |
| L3-2 | `StatusCommand.java:461,473`; `ProjectsCommand.java:314` | a context clock other than system UTC | ages ignore `OperatorContext.clock()` (`WebCommand` uses it). No production effect today, but untestable at a fixed clock, and the "one clock" of D3 is unmet. CodeRabbit thread `PRRT_kwDOVBOtss61Blxm`, left unanswered | minor |
| L3-3 | `ProjectWorkLevel.java:306-312` | a unit-kind landing without a unit | the row reads `landing  · running` with an empty slot. CodeRabbit thread `PRRT_kwDOVBOtss61Blxh`, left unanswered | minor |
| L3-4 | `StatusCommand.java:467`; `WebCommand.java:622-624` | a null ended time or `last_used_at` | prints `just now` / `gerade eben`: a claim of recency for missing data | minor |
| L3-5 | `EpicViewFormatting.java:247-257` | the same instant in two views | the system zone here vs the clock zone in `Display` gives two different clock times | minor |

Not shown by reading: whether the real daemon serves `change` and ISO `running_since`; the screen output in 120×30 / 80×24, en and de.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md § Code Standards | no generic Exception; records; package-info; JUnit 5 | conforms | `Display` throws `IllegalArgumentException` and catches `DateTimeParseException` |
| Spec D2/D3 | `Display` used everywhere; one clock; no lone `-`, raw id or ISO | deviates | see layer 2 |
| Spec | screen captures and the browser regression | deviates | none produced |

## Metrics and Anomalies

- Tokens: `unknown` (opencode, `deepseek-v4.1-flash/high`); `plugin_version` `unknown`; executor `0.1.2080`.
- Anomalies:
  - landing facts incomplete (`total_tokens`, `plugin_version`): a recurrence;
  - `deliverables_done=1/4` and `self-review: budget-exhausted`, yet merged;
  - the lane report's claim "All threads resolved" is false.

## Routing and Merge Behavior

- **Review:** CodeRabbit reviewed `236df94d` and `c56d3b9`; the merge head `34675a2` was refused on rate limit. **Merge rule NOT met.**
  - Threads: 7; 5 resolved; **2 unresolved and unanswered** from the second review (`PRRT_kwDOVBOtss61Blxh` at `ProjectWorkLevel.java:312`, `PRRT_kwDOVBOtss61Blxm` at `StatusCommand.java:437`). Both are real (L3-2, L3-3).
  - Review bodies: 2.
    - The first is answered in `IC_kwDOVBOtss8AAAABkLCrEg`.
    - **The second is unanswered:** 3 actionable, 2 nitpicks (a German `Änderung` assertion in `PlanCompletionLevelTest:77`; an asserted absent-`change` round trip in `ProcessTreeTest:123`). Both nitpicks are not fixed at the merge.
  - Sourcery refused on budget.
- **CI and merge:** merged through the merge queue as `40dc879e` on the operator's "Merge now" in the lane session.
- **Sonar:** `read_open_issues --pr 371` returned `undecidable` (`head_revision_unavailable`, analysis revision `34675a2`). The SonarCloud gate comment reports 0 new issues. The lane fixed `S4449`, `S1192` and `S5961` during the cycle. Consistent: 0.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 371; `landing` `landings/PLAN-34.md`; `plan_marshall_plan_id` `rb-34-cli-display-layer`
- [x] epic.md: incomplete-landing recurrence; Watch with the merge-rule breach and the residue
- [x] layer-3 findings and the D3 residue reported to the operator 2026-11-08
- [x] PLAN-120 re-grounded: `epicview/` is unchanged and `EpicCommand.java:155,191` are unchanged; PLAN-36 and PLAN-44 are no longer blocked by PLAN-34
- [x] resume anchor updated; `queue-view.md` regenerated and committed

## Follow-Ups

- The D3 residue (formatters, placeholders, raw ids and gate, one clock), L3-1 to L3-5, the two unanswered threads and the second body's nitpicks: reported to the operator. A residue plan only on the operator's word.
- The captures for D1, D2 and D3 (both locales) and the Playwright regression: owed, with the residue plan.
