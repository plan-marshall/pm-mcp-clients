# Settled narrative: ui-improvements

Entries moved verbatim out of `epic.md` on 2026-10-30 (operator confirmed the relocation). Each stays reachable through a pointer left at its origin. The subject of every entry here is closed (a shipped plan, a finished drain or emit, a retired watch); standing rules, open defects, live watches and the annotation zone stay in `epic.md`.

## Decision of 2026-10-26: round 1 cancelled and re-launched

- 2026-10-26 — Round 1 cancelled and re-launched: the runbook lane failed on the opencode install
  (bundle resolution, `executor-skew`, empty architecture); after the lane fix PLAN-01, PLAN-02 and
  PLAN-04 were launched again (PRs #226, #229).

## Landings of PLAN-02 and PLAN-04 (2026-10-27)

- 2026-10-27 — PLAN-02 landing analysed (`landings/PLAN-02.md`). The two baseline defects it found (a
  `-` placeholder in the plan view's task line, a relative "today" in the status header) belong to
  PLAN-34 (`cli-display-layer`), not to PLAN-04/PLAN-16 as the message says; PLAN-34 already covers
  them, so nothing is folded. The undeclared files it touched (`pm-mock-runtime/README.adoc`, the
  test-scope `tui/AnsiScreen*`, `VirtualScreenTerminal`) collided with nothing.
- 2026-10-27 — PLAN-04 landing analysed (`landings/PLAN-04.md`). Spec correction across the corpus: the
  standing constraint named `./mvnw -pl pm-web-e2e verify -Pintegration-tests`, which activates no profile
  in `pm-web-e2e` and skips Playwright silently; the suite's profile is `-Pe2e`. Replaced in the 27 staged
  web specs (PLAN-01 running and PLAN-04 shipped left as they are). The files PLAN-04 touched beyond its
  declared surface (`lib/theme.ts`, `routeTitles.test.ts`, the `plan_view`/`epic_view`/`start` message
  areas, `workspace.spec.js`, five design docs) collided with nothing in flight.

## Landings of PLAN-01, PLAN-76, PLAN-07 and PLAN-06 (2026-10-27 to 2026-10-28)

- 2026-10-27 — PLAN-01 landing analysed (`landings/PLAN-01.md`). Its unmet clauses are spec wording, not
  missing code, so the gaps are folded into PLAN-73 (`web-text-guard-failing`), deliverable 1:
  - the guard does not scan chips, aria labels or glyphs;
  - CC-F-05 has no rule;
  - split tokens are not seen;
  - the browser clause becomes "every violation the guard lists appears in the snapshot".

  PLAN-73's Expected Surface is updated in the same act: the three guard files exist now, and
  `text-guard.spec.js` is added. The port collision with PLAN-03 cost PLAN-01 several hours, which
  confirms PLAN-76.
- 2026-10-27 — PLAN-76 landing analysed (`landings/PLAN-76.md`), and the operator's hold on new launches
  lifted. Emitted three plans, one per track, all with port blocks and disjoint from PLAN-03 and from each
  other: PLAN-06 (`shared-display-vocabulary`), PLAN-07 (`cli-terminal-design-docs`) and PLAN-12
  (`web-design-tokens-v2`).
  - With PLAN-03, that is four in flight, one above N=3, at the operator's request ("three if sensible").
    It is sensible because the port blocks remove the collision that motivated the limit.
  - Not chosen: PLAN-08 (same track as PLAN-06); PLAN-10, PLAN-13 and PLAN-14, which share
    `operator.{de,en}.properties` with each other; PLAN-15, which shares `15-overview.adoc` with PLAN-07.
- 2026-10-27 — PLAN-07 landing analysed (`landings/PLAN-07.md`). Emitted PLAN-10 (`cli-choice-prompt-pause`)
  into the terminal slot it freed. It comes first in order among the ready terminal plans and gates PLAN-20
  and PLAN-28.
  - Corrected before the emit, under the terminal-check watch: deliverable 1 (`ChoicePrompt`) gets no
    terminal check of its own, because it reaches the screen only through deliverable 2's dialogues.
  - Deliverable 2 now enables `CancelDialogueIT` as well as `PauseDialogueIT` (both name PLAN-10 since
    PLAN-02), and its capture covers the cancel dialogue.
  - Expected Surface: `PauseDialogueIT` relabelled present, `CancelDialogueIT` added.
  - Not chosen: PLAN-13 and PLAN-14 (share the operator catalogue with PLAN-10); PLAN-15 (next terminal
    candidate); PLAN-08 (Sonar watch open).
- 2026-10-28 — PLAN-06 landing analysed (`landings/PLAN-06.md`). Two operator decisions in its run change
  the shared contract, and later plans build on them:
  - The `state.<kind>.<value>` and `unit.<id>` keys are a documented carve-out from the three-part key
    rule of `texts-and-languages.adoc`.
  - `pm-api` gained the closed sets `EpicState`, `LandingState`, `CleanupKind`, `ModelStatus` and
    `GateKind`, and `PlanActivity` gained `STAGED`/`ENDED`. Before, five of the eleven state kinds existed
    only as literals in the mock and the views.

  The display layers PLAN-09 and PLAN-34 read these enums; their specs predate them, so each is checked
  against them before it is emitted.

## Emits, launches and landings from PLAN-15 to PLAN-13 (2026-10-28 to 2026-10-30)

- 2026-10-28 — Emitted PLAN-15 (`cli-footer-and-help-overlay`) into the slot PLAN-06 freed (four in
  flight, as before).
  - The terminal-check watch applied: deliverables 2 (footer by priority) and 3 (help overlay) had the
    plan's shared residue target, which shows neither change. Each now names its own observable: the
    80-column footer keeping `a`, `Enter`, `Esc`, `?`, `q` and ending `? more`; and `?` opening the
    grouped box with `Esc close`.
  - Not chosen: PLAN-08 (the docs-only Sonar watch needs the operator's decision); PLAN-09 (check
    against the new enums and decide the tab-title watch first); PLAN-11 (5 files shared with the
    in-flight PLAN-03); PLAN-13 and PLAN-14 (share the operator catalogue with PLAN-10).
- 2026-10-28 — PLAN-12 landing analysed (`landings/PLAN-12.md`). Emitted PLAN-19 (`web-ui-icon-set`) into
  the web slot it freed. Its dependencies PLAN-12 and PLAN-76 are shipped, it is disjoint from PLAN-03,
  PLAN-10 and PLAN-15, and it carries PLAN-12's emoji residue. PLAN-09 was not chosen: the PLAN-06 enum
  check and the tab-title watch must be settled before its emit.
- 2026-10-28 — PLAN-15 and PLAN-19 landings analysed (`landings/PLAN-15.md`, `landings/PLAN-19.md`). With PLAN-10
  alone in flight, two of the three slots were free. Emitted PLAN-08 (`use-case-documents`, shared track) and
  PLAN-25 (`web-core-components-controls`, web track, ports 20250/20251). Both pass the gate: PLAN-08 depends on
  nothing and PLAN-25 on PLAN-12, 19 and 76 (all shipped); each shares no file with PLAN-10 or with the other;
  every verdict admits; every surface is declarative. PLAN-08 was held back before only for the docs-only Sonar
  watch, which was decided earlier today. No overlap prompt was asked.
  - Not chosen: PLAN-09 (the PLAN-06 enum check and the tab-title watch are still owed, and PLAN-19 changed five of
    its views); PLAN-11 (re-read against HEAD and add the e2e specs and the transcript to its Expected Surface
    first); PLAN-13 and PLAN-14 (each shares both operator catalogue files with PLAN-10, so the overlap prompt
    would fire); PLAN-27 (ready, but the web track is taken by PLAN-25).
- 2026-10-28 — PLAN-08, PLAN-25 and PLAN-27 recorded launched on the operator's confirmation (four launched with
  PLAN-10, one above N=3 by the operator's choice).
- 2026-10-29 — PLAN-10 landing analysed (`landings/PLAN-10.md`). Row `shipped` (PR #257), message archived. No plan
  emitted: three plans (08, 25, 27) stay launched, which is N. Next in line when a slot frees: PLAN-11, or the
  terminal plans PLAN-13 and PLAN-14 now that the operator catalogue is free (re-read at HEAD first).
- 2026-10-29 — PLAN-25 landing analysed (`landings/PLAN-25.md`). Row `shipped` (PR #260), message archived. No plan
  emitted: two plans (08, 27) stay launched, so one slot is free; the choice of what takes it is left to the
  operator: PLAN-31 (ready, but the web track is taken by PLAN-27 until it lands), PLAN-11 (mock track), or
  PLAN-13 / PLAN-14 (terminal track, free now that PLAN-10 shipped; they overlap on the operator catalogue).
- 2026-10-29 — Ledger landed as PR #263 (merge `a2f4d2c4`): PLAN-10 and PLAN-25 landings, the launch records of
  PLAN-08, 25 and 27, the anchor and this file.
- 2026-10-29 — Second PLAN-01 landing message analysed (`landings/PLAN-01.md`, addendum). It comes from the
  resumed run of the same plan after the server outage, not from a re-run of a shipped plan: PR #237 was merged
  by the operator during the outage, no new PR, no leftover branch or worktree. The row stays `shipped`. Its
  round-3 findings were folded into PLAN-73's deliverable 1; no plan emitted.
- 2026-10-29 — PLAN-08 and PLAN-27 recorded running on the operator's word (PLAN-08 had been recorded launched
  from a command emitted, with no proof of a live session; the operator found the session; PR #259 open, CI green).
- 2026-10-29 — Emitted PLAN-31 (`web-core-components-containers`, ports 20310/20311) and PLAN-11 (`mock-cli-fixes`,
  ports 20110/20111) at the operator's request. With PLAN-08 and PLAN-27 running that makes four in flight, one
  above N=3, by the operator's choice (the same exception as 2026-10-28). Gate: both specs are declarative with a
  determinate comparison, no blocking candidate and no overlap prompt (`corpus cross-check`); the four surfaces
  are pairwise disjoint (PLAN-08 `doc/design/`, PLAN-27 brand files, PLAN-31 containers and the dev page, PLAN-11
  the mock). PLAN-31 shares the web track with PLAN-27 on the operator's choice; the files are disjoint, but both
  plans may touch `pm-web-app` in the same period. Both dependencies are shipped (PLAN-31: 25, 76; PLAN-11: 03,
  76). Specs re-read at HEAD `a2f4d2c4` and corrected before the emit: PLAN-31 (the dev page exists, created by
  PLAN-25; the controls it reuses; the `dev.*` keys) and PLAN-11 (`kind="item"` is at `MockRuntimeEngine.java:7708`,
  not 7663; claim 1 re-confirmed: still one plan copy for any plan id, no `/api/v1/config` or `/roles` route).
  Not added to PLAN-11: the two stale mock epic routes (`actions/cancel` ignores `with_plans`, `actions/next`
  is a no-op); the operator did not decide it, so they stay a watch. Not recorded as launched: that waits for the
  operator's confirmation that the commands were started.
- 2026-10-29 — Ledger landed as PR #264 (`53718209`). PLAN-31 and PLAN-11 recorded launched, then running, on the
  operator's word.
- 2026-10-29 — PLAN-08 landing analysed (`landings/PLAN-08.md`). Row `shipped` (PR #259), message archived. Emitted
  PLAN-13 (`cli-event-contract`; terminal plan, no ports assigned in the spec) at the operator's request ("emit the next"): terminal track,
  free since PLAN-10 shipped. With PLAN-27, PLAN-31 and PLAN-11 running that is four in flight, one above N=3 by the
  operator's choice (third time). Gate: declarative, determinate, no blocking candidate, no overlap prompt
  (`corpus cross-check`); its only shared files are the operator catalogue pair with staged plans (PLAN-14, 17, 21,
  23), none in flight. Dependency PLAN-02 shipped. Spec re-read at HEAD eba84c17: hypothesis still holds
  (`ConsoleNotice.java:293`, `PmConsole.java:448`); a re-grounded note added (PLAN-10 and PLAN-15 changed the
  catalogue, PLAN-15 changed `04-console.adoc`). PLAN-14 not chosen: same catalogue pair, so not both at once.
  Not recorded as launched.
- 2026-10-29 — PLAN-27 landing analysed (PR #262, `4c39c0f8`) and drained; row shipped. Running after it: PLAN-31,
  PLAN-11 and PLAN-13, which is N=3 with no free slot. Emitted PLAN-20 (`cli-actions-by-state`, terminal) at the
  operator's request ("emit the next"), which makes four in flight if it is started: one above N=3 by the operator's
  choice (fourth time). The ready set (every dependency shipped) is PLAN-09, 14, 17, 20, 21, 22, 23. Gate:
  declarative, determinate, no blocking candidate, no overlap prompt for PLAN-20 (`corpus cross-check`: no shared file
  with anything). Not chosen: PLAN-14, 17 (each shares the operator catalogue pair with PLAN-13; the cross-check asks
  an overlap prompt); PLAN-21, 22, 23 (the catalogue pair too, and the same terminal track); PLAN-09 (two checks
  still open: the PLAN-06 enums with the tab-title watch, and the five views PLAN-19 and PLAN-25 changed). PLAN-20
  re-read at HEAD 4c39c0f8: half is done (hold acts first exist; the decision-first entry of a waiting plan does not),
  so a re-grounded note scopes it. Its one undeclared overlap with PLAN-13 is the new catalogue text (different
  section of the pair), named in the note. Recorded launched and then running on the operator's word the same day
  ("i started #20"): four running (31, 11, 13, 20).
- 2026-10-29 — PLAN-31 landing analysed (PR #266, `0351ce78`) and drained; row shipped. Running after it: PLAN-11,
  PLAN-13 and PLAN-20, which is N=3 with no free slot. Emitted PLAN-39 (`web-app-shell`, web track) at the operator's
  request ("emit the next"); started, it makes four in flight, one above N=3 by the operator's choice (fifth time).
  The ready set is PLAN-09, 14, 17, 21, 22, 23, 37, 39. Gate for PLAN-39: declarative, determinate, no blocking
  candidate, no overlap prompt, no shared file with anything (`corpus cross-check`); dependencies PLAN-31, 27, 04, 76
  shipped; the web track is free; ports 20390/20391 in the spec. It is the critical path of the web chain (the shell
  gates the view plans). Spec re-read at HEAD 0351ce78: a re-grounded note lists the PLAN-31 components it can use,
  what `+layout.svelte` holds today and that the four `system.*` lifecycle states are not part of it (the machine
  pill is neutral). Not chosen: PLAN-37 (adds `@axe-core/playwright`, an operator-decided dependency, and edits
  `WorkControlPanel`/`ChatPanel`; wait for a free slot), PLAN-09 (two checks still open), PLAN-14, 17, 21, 23 (overlap
  prompt on the operator catalogue with PLAN-13), PLAN-22. Recorded launched and then running on the operator's word
  ("I started the plan"). The operator set `parallelization_scope` to 4 in the same message: four running (11, 13, 20,
  39) is now N, not above it, and no slot is free until one lands.
- 2026-10-30 — PLAN-11 landing analysed (PR #269, `bae3a8c8`) and drained; row shipped. Running after it: PLAN-13,
  PLAN-20 and PLAN-39, which is three of N=4, so one slot is free. Emitted PLAN-22 (`cli-state-dialogue-onboarding`,
  terminal track) at the operator's request ("emit next"). The ready set is PLAN-09, 14, 17, 21, 22, 23, 37. Gate for
  PLAN-22: declarative (13 entries), determinate, no blocking candidate, no overlap prompt, no shared file with any
  gate candidate (`corpus cross-check`); dependencies PLAN-10 and PLAN-76 shipped; ports 20220/20221 in the spec. Its
  catalogue sections (`onboarding.*`, `projects.*`) are not the ones PLAN-13 (`operator.console.*`) and PLAN-20
  (`operator.actions.*`) edit. Spec re-read at HEAD bae3a8c8: the two dialogue ITs already exist `@Disabled` (PLAN-02),
  so a re-grounded note and two corrected Expected Surface lines say "enable", not "create". Not chosen: PLAN-14, 17,
  21, 23 (overlap prompt on the operator catalogue with PLAN-13), PLAN-37 (adds `@axe-core/playwright`, an
  operator-decided dependency; also shares `workspace.spec.js` with PLAN-39), PLAN-09 (two checks still open).
  Recorded launched and then running on the operator's word the same day ("#22 is running"): four running (13, 20,
  39, 22) is N=4, and no slot is free until one lands.
- 2026-10-30 — PLAN-13 landing analysed (PR #270, `7e2b4b61`) and drained; row shipped. Running after it: PLAN-20,
  PLAN-22 and PLAN-39, which is three of N=4, so one slot is free. Emitted PLAN-14 (`cli-gate-scope`, terminal track)
  at the operator's request ("emit next"). With PLAN-13 shipped the operator catalogue is held by no running plan, so
  PLAN-14, 17, 21 and 23 now read a clean gate (`corpus cross-check`: declarative, determinate, no blocking candidate,
  no overlap prompt, no shared file with a running plan); PLAN-36 still needs PLAN-34; PLAN-09 now overlaps PLAN-22 on
  `onboarding.*` (overlap prompt); PLAN-37 shares `workspace.spec.js` with PLAN-39 and needs the axe decision. PLAN-14
  chosen as the lowest ready terminal plan with a single dependency (PLAN-02, shipped); no ports in the spec. Spec
  re-read at HEAD 7e2b4b61: `PmConsole` and `PtySession` changed under it (PLAN-13), the hypothesis holds
  (`QuestionsCommand:54`, `ReportsCommand:65`, `ProjectsCommand:68` call `gate().verify()` unconditionally), and
  `OperatorGateScopeTest` is still absent; a re-grounded note says so. The overlap of the
  running PLAN-22 with PLAN-13's `EntryFlow` change is named in the PLAN-13 landing; no mailbox note sent. Recorded launched and then running on the operator's word the same day ("plan is running"): four running (20, 22, 39, 14) is N=4, and no slot is free until one lands.

## Landings of PLAN-39, PLAN-22 and PLAN-14 (2026-10-30)

- 2026-10-30 — PLAN-39 landing analysed (PR #274, `44b051ff`) and drained; row shipped. The same drain found PLAN-22's
  PR #275 (`fa178dec`) already on `main` with no inbox message yet: PLAN-22 stays `running` until its landing message
  arrives. Running after this drain: PLAN-14, 20 and 22, which is three of N=4, so one slot is free (two once PLAN-22's
  landing is drained; see the PLAN-22 entry below). Nothing emitted: the operator's standing order is to drain the running plans before the refactor
  sprint, so the emit waits for the operator's pick (gate reads at HEAD `44b051ff`: PLAN-77, 80 and 37 clean; PLAN-17,
  21 and 23 now ask an overlap prompt with PLAN-14 on the operator catalogue; PLAN-09 with PLAN-22 on `onboarding.*`;
  PLAN-79 with 14, 20 and 22). The web shell now exists, so PLAN-46 (palette), PLAN-49 (inbox item, `+layout`),
  PLAN-45 and PLAN-57 (`client.ts`), PLAN-37 (`workspace.spec.js`) and PLAN-52 and PLAN-75 (`mobile.spec.js`) read against
  the new layout; their gates are clean for the running set. The PLAN-80 note that PLAN-39 edits `+layout.svelte`
  is now past. The refactor sprint (PLAN-77, 80, then 78, 79) is unaffected.
- 2026-10-30 — PLAN-22 landing analysed (PR #275, `fa178dec`) and drained; row shipped. Running after it: PLAN-14 and
  PLAN-20, which is two of N=4, so two slots are free. Nothing emitted (same order as above: the operator picks).
  Gate reads at HEAD `44b051ff` with PLAN-22 gone from the running set: PLAN-09 is now clean (its `onboarding.*`
  overlap prompt is gone) and PLAN-37, 77, 78, 80 stay clean; PLAN-17, 21, 23, 30, 36 and 79 still ask an overlap prompt
  with PLAN-14 or PLAN-20 on the operator catalogue, the capture sets or `pm-operator` tests. PLAN-14 and PLAN-20 rebase
  over `fa178dec` (`PmConsole`, `dialogue/`, `operator.*`); no mailbox note sent (offered). The follow-up PR #276 is
  recorded as a Watch for the operator.
- 2026-10-30 — PLAN-14 landing analysed (PR #277, `5348e89e`) and drained; row shipped. Only PLAN-20 runs now (one of
  N=4, three slots free). Nothing emitted: the operator's order stands (drain the running plan, then the refactor sprint).
  Gate reads at HEAD `5348e89e` with PLAN-14 gone from the running set: PLAN-09, 17, 21, 23, 30, 36, 37, 77, 78 and 80 are
  clean with no overlap prompt, and PLAN-79 shares one file with PLAN-20 (`ActionMenusTest`) and asks no prompt (below the
  threshold). PLAN-20 (running) must rebase over `19-actions.adoc` and `WorkActionsClient`; no mailbox note sent (offered).
  Re-grounded notes added to PLAN-42 (`plan <id> --print` now exists) and PLAN-23 (the whole `cleanup` verb is an operator
  act; `26-cleanup.adoc` rewritten). The `PM_OPERATOR_DEV_ALLOW_AGENT` question stays an operator decision.

## Retired Watches (2026-10-28 to 2026-10-30)

- Retired watch (2026-10-30): UC-PLN-7 and UC-PLN-8 were `DRAFT` (agreed by the operator in PR #259). They are `AGREED`
  since `plan-marshall-documentation` PR #135 (`use-cases/03-plans.adoc`, both `Status:: AGREED`), which also moved the
  `Changes to the Specification` content into PM-ARCH-11 and the runtime-model specification, and PR #278 added the
  `credential` and `cleanup due` kinds to the shared and the console design. Both drafts' follow-ups are done in the
  documents; the code is owned by PLAN-81 to PLAN-84 and PLAN-36, 46, 49.
- Closed-PR branch left behind (retired watch, 2026-10-28): `feature/03-mock-scenario-states` (the closed PR #250's
  head, `8cf8cfb`) outlived the PLAN-03 landing, which reports `cleanup_owed=false`; the lane pruned only the `-b`
  branch. Deleted from the remote on the operator's word after checking that no open PR used it and that
  merge commit `4efa0314` contains every file it changed (plus later fixes). Open point that stays: the lane's
  cleanup does not treat a closed PR's branch as owed; re-check at the next landing that re-opens a PR.
- Retired watch (2026-10-30): the mock wrote a kept cleanup item with `kind="item"`, outside `CleanupKind` (PLAN-06
  residue). PLAN-11 (PR #269) fixed it in its deliverable 2 and `FixtureConsistencyTest.keptItemKindIsFromTheClosedSet`
  now pins it.
- Follow-up PR #276 of PLAN-22 is open (`fix/22-cli-state-dialogue-followup`, mergeable, clean; the run reports CI and
  Sonar green at `eebbeeb6` and CodeRabbit refused on quota twice). It fixes the German `--auto` line that repeats the
  runtime's English summary and two duplicated catalogue keys in `onboarding.{en,de}.properties`. PLAN-22 shipped
  without it, no plan owns it, and nothing merges without the operator's word. — ask the operator to merge or close it;
  PLAN-14 and PLAN-20 (both running) declare the operator catalogue and `PmConsole` and rebase over `fa178dec` either way.
  Resolved 2026-10-30 at the operator's word ("merge and cleanup"): PR #276 merged through the merge queue as `44803a77`
  (merge group run green); the remote branch was deleted by the queue; the local worktree
  `.plan/local/worktrees/22-cli-state-dialogue-followup` and branch were removed after checking the worktree was clean
  and that the only difference to `main` in the touched files was PLAN-14's later gate parameter.
