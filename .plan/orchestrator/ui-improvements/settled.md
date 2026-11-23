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

## Landing of PLAN-20 and the running records of PLAN-77, 80, 82 and 86 (2026-10-30)

- 2026-10-30 — PLAN-77, 80, 82 and 86 recorded launched and then running on the operator's word ("all four plans are
  running"), after the emit below; four running is N=4 and no slot is free. The operator asked to persist, land everything
  and prepare a restart: the ledger was landed and a restart note heads the resume anchor. PLAN-22's follow-up PR #276 was
  merged and cleaned up on the operator's word in between (`44803a77`).
- 2026-10-30 — PLAN-20 landing analysed (PR #273, `a07a09b6`) and drained; row shipped. Nothing runs now: all four slots
  of N=4 are free and the refactor sprint the operator announced ("drain all current plans, then the refactor sprint") can
  start. Emitted at the operator's request ("analyze landed and emit"): PLAN-77 (`cui-standards-foundation`), PLAN-80
  (`web-standards-conformance`), PLAN-82 (`needs-and-find-contract`) and PLAN-86 (`shared-error-texts`). All four are
  `declarative`, determinate, with no blocking candidate and no overlap prompt (PLAN-77 and PLAN-80 share only
  `CLAUDE.md`, one file); `corpus verdicts` has `blocking_count: 0` and the four specs carry no stale verdict. PLAN-80 now
  adds a `## Web Standards` section of its own after § Code Standards, so the two `CLAUDE.md` edits cannot conflict. The
  choice rests on: the operator's order (the refactor sprint first), and PLAN-82 and PLAN-86 each unblock several staged
  plans (PLAN-82: 34, 36, 46, 49, 52, 54, 55, 83, 84; PLAN-86: 09, 30, 45). Not emitted, though ready: PLAN-17, 21, 23, 24,
  37, 85 (PLAN-17, 21, 23 overlap each other by 2 to 4 files, so they run one after the other). PLAN-78, 79 and 87 wait for
  PLAN-77. Re-grounded notes added to PLAN-21 (the PTY path of `decide`) and PLAN-36 (the `!` key is one rule).

## Landings and running records of PLAN-09 to PLAN-86 (2026-10-31)

- 2026-10-31 — PLAN-78 landing analysed (PR #288, `1651554b`, `landings/PLAN-78.md`) and drained; row shipped. Running after
  it: PLAN-16, 17 and 87, which is three of N=4, so one slot is free. Earlier the same day PLAN-82 landed (PR #284, `7d7b4d71`,
  `landings/PLAN-82.md`) and PLAN-87 was emitted and recorded running on the operator's word (its spec re-counted at HEAD
  `6372185a`: 45 classes, not 43). Emitted PLAN-46 (`web-command-palette`, WS-03, ports 20460/20461) at the operator's request
  ("analyze landed and emit"): its dependencies PLAN-39, 08, 76 and 82 are shipped, the gate reads it determinate with no
  overlap against the running PLAN-16, 17 and 87 (`blocking_count: 0`), and PLAN-82's landing unblocked it. Reading its spec
  against the accepted design found a gap: the texts the designs call for (`web.frame.search.title` and every `overview.find.*`
  row of the shared design) are in no catalogue at HEAD and in no staged spec. PLAN-46 now owns adding them (en and de, the
  design's wording) and retiring `web.frame.search.pending`; PLAN-84 reads them at its head or adds the missing ones if it runs
  first; both declare `overview.{en,de}.properties` and run one after the other. Notes were added to both specs; the
  assignment is the orchestrator's and open to the operator. Not emitted though ready: PLAN-43 (one shared file with the
  running PLAN-16) and PLAN-24 (clean); PLAN-79 is held while the terminal PLAN-17 runs. The Watch on the blocked PLAN-78 is
  closed (see Watches).
- 2026-10-31 — PLAN-78 revision message analysed (`rb-78-cui-testing-api-002.md`, written 16:03Z): the plan is NOT landed.
  PR #288 (the replacement of the closed #285, same head `0860ba13`) is open: CI green, full `verify` green, Sonar a
  confirmed `0`, every comment handled, but the required reviewer CodeRabbit has never reviewed any head of either PR (six
  triggers incl. one escalation, all "Review rate limited"; its own notice names plan capacity). The standing review
  authorization does not apply without a qualifying review. The lane is stopped before the merge and waits for the operator:
  clear the CodeRabbit account, or authorise the merge with the gap named. The row stays `running`, no landing is stamped,
  the message is archived as the observation. Recorded as a decision for the operator in the Watches. The ledger was
  landed in between ("land all and prepare to restart").
- 2026-10-31 — PLAN-16 recorded launched and then running on the operator's word ("#16 is running"): four running (16,
  17, 78, 82) is N=4 and no slot is free until one lands.
- 2026-10-31 — PLAN-09 and PLAN-78 messages analysed. PLAN-09 landed (PR #287, `5f9e60da`): row shipped. PLAN-78 did NOT
  land: its message (13:26) says PR #285 was open and unmerged, the required reviewer CodeRabbit never reviewed it (197
  files against its limit of 100, then quota refusals), and its policy was to close the PR and open a new one from the same
  branch; the live state is that #285 is closed and #288 is open on `chore/78-cui-testing-api`. The row stays `running`
  and no landing is stamped; the message is archived as the observation and the lane's later message carries the landing.
  Running after this: PLAN-17, 78 and 82, which is three of N=4, so one slot is free. Emitted PLAN-16
  (`web-time-format-and-locale`, WS-02, ports 20160/20161) at the operator's request ("analyze and emit next"): its
  dependencies PLAN-06, 09 and 76 are shipped, it has no overlap with any running plan, `blocking_count: 0`, and it is the
  next link of the web chain (PLAN-16 → 35 → 45). Not emitted though ready: PLAN-37, 87, 24, 79 (clean), PLAN-21, 23, 30 and
  85 (overlap prompts with running plans). Notes added to PLAN-16, 17 and 45 (PLAN-09's reach) and to PLAN-79 and 87 (keep
  the pull request under 100 files; restore the formatter's rewrite of main sources).
- 2026-10-31 — PLAN-09 recorded launched and then running on the operator's word ("#09 is running"): four running (09,
  17, 78, 82) is N=4 and no slot is free until one lands.
- 2026-10-31 — PLAN-86 landing analysed (PR #283, `1b6c6ae3`) and drained; row shipped. Running after it: PLAN-17, 78 and
  82, which is three of N=4, so one slot is free. Emitted PLAN-09 (`web-display-layer`, WS-02, no ports of its own beyond
  the spec's) at the operator's request ("analyze and emit next"): it is ready (PLAN-06, 01, 76 and now 86 shipped) and is the
  critical path of the web chain (PLAN-16, 35, 45 and the view plans follow it). The gate asked an overlap prompt: PLAN-09
  shares 4 catalogue files with the running PLAN-82 (`overview.*`, `plan_view.*`) and 2 with the running PLAN-17
  (`system.*`); the operator answered "PLAN-09, accept the overlap" (the other options were PLAN-37, PLAN-87 and nothing).
  `corpus verdicts` has `blocking_count: 0`. The pre-emit checks the epic had set were done: the `phase_state` drift watch is
  written into the spec (list the values the contract and mock serve, unknown goes through the fallback), the tab-title watch
  stays open and is not part of this plan, and the PLAN-06 enums are what `enumFields.ts` reads. Spec corrections from the
  PLAN-86 landing: PLAN-30 now declares `09-texts-common.adoc` and `03-dialogues.adoc` (they still describe a
  `common.error.<code>` family), PLAN-45 no longer declares a second completeness test. The German glossary question of
  PLAN-86 (R-02) is recorded as a Watch for the operator.
- 2026-10-31 — PLAN-78 recorded launched and then running on the operator's word ("#78 is running"): four running (17,
  78, 82, 86) is N=4 and no slot is free until one lands.
- 2026-10-31 — PLAN-77 landing analysed (PR #282, `e83a9ee2`) and drained; row shipped. Running after it: PLAN-17, 82 and
  86, which is three of N=4, so one slot is free. Emitted PLAN-78 (`cui-testing-api`, WS-01, no ports) at the operator's
  request ("analyze and emit next"): its only dependency PLAN-77 has landed, the spec is declarative and determinate, no
  blocking candidate, no overlap prompt (one shared file with the running PLAN-86), `blocking_count: 0`. Not PLAN-79: its
  spec says to emit it when no terminal plan is running and PLAN-17 is, and it rewrites test files that PLAN-21, 23 and the
  later terminal plans edit, so it is best run in a window without them. Not PLAN-87 (also clean): it can follow at any
  time. Ready and gate-clean but not emitted: PLAN-37, 87, 24; PLAN-79 (one shared file with PLAN-17); PLAN-21, 23 and 85
  ask an overlap prompt with a running plan. Notes were added to PLAN-78, 79 and 87 (the pilot's pattern: no `Generators`
  in field initialisers, input-free classes listed, contract values literal).
- 2026-10-31 — PLAN-17 recorded launched and then running on the operator's word ("#17 is running"): four running (77,
  82, 86, 17) is N=4 and no slot is free until one lands.
- 2026-10-31 — PLAN-80 landing analysed (PR #281, `bf8ddb31`) and drained; row shipped. Running after it: PLAN-77, 82
  and 86, which is three of N=4, so one slot is free. Emitted PLAN-17 (`cli-runtime-act-confirm`, terminal track, no ports)
  at the operator's request ("analyze and emit next"): declarative, determinate, no blocking candidate, no overlap prompt,
  `corpus verdicts` has `blocking_count: 0`; chosen because the terminal track has nothing running, it is the first link of
  the chain PLAN-17 → 26 → 32 and it feeds PLAN-30. Ready and gate-clean but not emitted: PLAN-21 and PLAN-23 (they overlap
  PLAN-17 by 2 to 4 files: after it), PLAN-24, PLAN-37; PLAN-85 now asks an overlap prompt (two files shared with the
  running PLAN-82). A re-grounded note was added to PLAN-17 (PLAN-14 and PLAN-20 changed `RuntimeCommand` and the menus).
  The PLAN-80 run's residue that the "dark" contrast audit may run under the light theme is recorded as a Watch and as
  notes in PLAN-70 and PLAN-85. The executor skew the PLAN-80 run met (`0.1.2032` against `0.1.2036`, answered by the
  operator) may recur in the landings of PLAN-77, 82 and 86.

## Landings and running records of PLAN-16 to PLAN-91 (2026-11-01)

- 2026-11-01 — PLAN-91 recorded running on the operator's word ("#91 is running"): four running (37, 43, 90, 91), N=4, no slot free.
  Next emit when a slot frees: PLAN-21 or PLAN-84 (terminal), PLAN-89 after PLAN-37.
- 2026-11-01 — PLAN-35 landing analysed (PR #302, `5650c41e`, `landings/PLAN-35.md`) in the three layers and drained; row shipped. Layer 1/2: the
  four components and the components document as specified, 7 of 7 declared files present, 24 beyond (17 pictures, 4 tests, a fixture, 2
  catalogue files), all explained; the pre-commit drift claim verified (the squash holds 31 files, none of the 178; it had landed as #298).
  Layer 3 (read, not run): sound; the numeric-alignment, density and reduced-motion tests assert markers the component writes, not CSS;
  three CSS leads not reproduced (collapsed border under a sticky head, `%` widths under `max-content`, the row-menu name composed in the
  component). Sonar: the lane read `confirmed` 0 at `eecf5a31`, my read after the merge is `undecidable`, recorded as an unverified lead.
  The merge rested on the operator's selection in the lane, not on the standing authorisation. Emitted PLAN-91 (CLI, free of the running web
  plans; gate determinate, no prompt, dependencies shipped); three run (37, 43, 90) plus PLAN-91 awaiting its start.
- 2026-11-01 — PLAN-90 recorded running on the operator's word ("#90 is running"): four running (35, 37, 43, 90), N=4, no slot free.
  Next emit: PLAN-91 when a slot frees (PLAN-89 after PLAN-37).
- 2026-11-01 — PLAN-26 landing analysed (PR #299, `be4995e5`, `landings/PLAN-26.md`) in the three layers and drained; row shipped. Layer 1:
  D1 and D3's exit codes as specified; D2's first line is NOT the specified text (`draining · 2 left` built, `draining · 2 plans finish ·
  stop in 4 min` and `restarting` specified) and the run rewrote the design (`shared/system.adoc#state-line`) to the built line; the PR
  body states `2 plans finish`, which the code, catalogue, design and test do not print. The run's reason ("the wire carries no drain
  deadline") is true of the client record only: the design lists the deadline in the status (`system.adoc:365`). Layer 2: 16 files added
  beyond a 7-entry declaration (production: a wire-name fix in `RuntimeStatus`, 49 lines of full-screen banner logic, exit 1 in four
  write paths of `WebCommand`/`ConfigCommand`); the `pm-api` `SystemSnapshot` was used as the model (the spec's new type was refuted
  correctly). Layer 3 (read, not run): no formatter defect; the full-screen lifecycle logic and the four write-verb refusals have no
  test; the new tests use literals and two carry an unused `@EnableGeneratorController`. The Sonar claim is `undecidable` after the
  merge. Lane leads about the web checked by reading: the hardcoded `retry`/`error`/`ok` words and the frozen load-failure string
  in `SystemView.svelte` are CONFIRMED (now PLAN-90 deliverable 3); `0.12 M` is REFUTED (`format.ts` gives `120 k`); `waiting for_you`
  not reproduced. Test gaps are PLAN-91 deliverable 3. Three running: PLAN-35, 37, 43; one slot free. Emitted PLAN-90
  (`web-green-baseline-and-time-residue`, WS-03, ports 20900/20901): gate determinate, 0 overlaps, no dependency open, the first of
  the staged fix plans; its green e2e baseline is what PLAN-37's third deliverable needs. Not chosen: PLAN-91 (next), PLAN-21, 84, 79,
  85, 18 (free), PLAN-24 (operator), PLAN-89 (waits for PLAN-37).
- 2026-11-01 — PLAN-46 follow-up landing analysed (PR #300, `1ce32d80`, section "Follow-up landing" of `landings/PLAN-46.md`) and
  drained together with `rb-37-web-accessibility-baseline-001.md`. The six CodeRabbit findings of PR #296 are fixed in the merged
  tree, each with a test of the matching name (read in `git show 1ce32d80:`; the failing-first logs and test counts are the run's
  account). The PR carried `skip-bot-review` and no review, on the operator's pasted word. The PLAN-46 row stays `shipped` (PR
  296): the fix is not a queue row. Sonar for PR 300 reads `undecidable` after the merge, an unverified lead. The PLAN-37 message is
  an observation (`merge_state=open`, PR #297 open, required reviewer refused on quota four times): archived, row stays
  `running`. No slot was freed by either message, so nothing new was emitted; PLAN-43 stays emitted and awaiting its start.
- 2026-11-01 — PLAN-46 landing analysed (PR #296, `d7f7ffc8`, `landings/PLAN-46.md`) and drained; row shipped. The reviewer block
  ended: CodeRabbit accepted a trigger at 11:12Z and reviewed `42aee47d` (the message does not say who triggered it), and the lane merged. The lane's message says that review had "no
  findings, no threads"; `ci pr comments` shows six inline findings (two Major: no touch close control and the arrow-key selection
  not exposed to assistive technology; four Minor) with no reply, and the merged `CommandPalette.svelte` is unchanged since that head.
  Recorded as contradicted, shipped anyway (it is merged), the findings carried as a Watch with no owner. Also landed, at the
  operator's request through the lane: PR #298 (`f5de8449`), the pre-commit import order of 178 `main` sources, the same 178 paths as
  the still-open PR #294, which is now redundant. Running after it: PLAN-26, 35, 37 (three of N=4). Emitted PLAN-43
  (`web-page-templates`, WS-03, ports 20430/20431) into the free slot: it unblocks the most plans (47, 49, 51, 52, 53, 54, 55, 58,
  59, 60, 88), dependencies PLAN-39 and 76 shipped, determinate gate with 0 overlaps against 26, 35 and 37. A re-grounded note was
  added to the spec (`SystemView` changed since staging, its own `max-width: 1200px`, the two PLAN-17 web defects are not its content,
  the e2e baseline). Not chosen: PLAN-21 and 84 (terminal, free), PLAN-23 (now asks an overlap prompt against PLAN-26), PLAN-79,
  PLAN-85 (re-read list; `routes.ts`, `AppShell` changed by PLAN-46), PLAN-18 (verify lane), PLAN-24 (operator).
- 2026-11-01 — PLAN-17 landing analysed (PR #292, `66c1495a`, `landings/PLAN-17.md`) and drained; row shipped. Running after it:
  PLAN-35, 37 and 46 (three of N=4, PLAN-46 blocked on the reviewer, PR #296). Verified against the merge: 16 files, `@Disabled`
  gone from `RuntimeStopDialogueIT`, `--when-done`/`--now` on all three acts; the surface expanded (9 added: the new
  `RuntimeActDialogue`, `StartPrompt`, `ChoicePrompt` and two design documents beside the tests; `DrainCommand` and
  `SystemLevel` declared and untouched); the Sonar claim (0 at `230fe51e`) is `undecidable` after the merge. Emitted PLAN-26
  (`cli-system-model`, WS-04, ports 20260/20261) into the free slot: its one dependency left, PLAN-17, shipped; determinate gate
  with 0 overlaps against PLAN-35, 37 and 46. Chosen over PLAN-21/23 because it opens the longest chain (26 → 34 → 36, 44 → 50;
  PLAN-33). A note was added to the spec: PLAN-17 touched `25-system.adoc` and the `system.*` catalogue the spec declares but not
  `SystemLevel`, and `SystemSnapshot` already exists in `pm-api` although the spec says it creates one. Not chosen: PLAN-21, 23 (next
  terminal plans; PLAN-30 waits for both), PLAN-24 (needs the operator), PLAN-79 (no terminal plan runs now, but it rewrites test
  files PLAN-21/23/26 add to, and is exposed to the 100-file limit), PLAN-43 and 85 (PLAN-16 re-read list), PLAN-18 (verify lane).
- 2026-11-01 — PLAN-35 recorded running on the operator's word ("#35 is running"): three running (17, 35, 46). The operator then
  answered the quota question: the CodeRabbit quota does not limit four parallel plans, because they run unattended and a queued
  or retried review costs them nothing ("So emit the fourth"). This replaces the orchestrator's own reason for holding the fourth
  slot back (see the entry below). Emitted PLAN-37 (`web-accessibility-baseline`, ports 20370/20371) into it: dependencies PLAN-31
  and 76 shipped, determinate gate with 0 overlaps against PLAN-17, 35 and 46 (`blocking_count: 0`), not among the specs PLAN-16
  touched, and its one new dependency (`@axe-core/playwright`) is the operator's decision of 2026-10-25. Not chosen: PLAN-24
  (needs the operator present), PLAN-79 (held while the terminal PLAN-17 runs), PLAN-43 and 85 (named in the PLAN-16 re-read
  list), PLAN-18 (a verify plan on the other lane). A re-grounded note was added to the spec (the e2e baseline is not known to be
  green; the pre-commit drift of PR #294).
- 2026-11-01 — PLAN-16 and PLAN-87 landings analysed (PR #291 `9890d74b`, PR #293 `418579a0`; `landings/PLAN-16.md`,
  `landings/PLAN-87.md`) and drained; both rows shipped. PLAN-46's message (`rb-46-web-command-palette-001.md`) is an
  observation, not a landing: PR #296 is open and blocked on the required reviewer; the message is archived and its row recorded
  `running` on the evidence of the open PR and the message (the operator had not said so; they are told). Running after it:
  PLAN-17 and PLAN-46, two of N=4. Emitted PLAN-35 (`web-data-components`, WS-02, ports 20350/20351) at the operator's request
  ("analyze landed and emit"): its dependencies PLAN-31, 16 and 76 are shipped, the gate reads it determinate with no overlap
  against the running PLAN-17 and PLAN-46 (`blocking_count: 0`, 0 indeterminate), it is the next link of the web chain
  (PLAN-16 -> 35 -> 45), and its claims were re-read at `origin/main` (the four components and `components.adoc` absent, the
  dev page, the README and `display/format.ts` present). A note on the pre-commit drift (PR #294 open) was added to the spec.
  One slot stays free: with only PLAN-17 and PLAN-46 running, about thirty specs read prompt-free, but the reviewer quota already
  holds PLAN-46 for hours, so the orchestrator emitted one plan rather than two; PLAN-24 (a security decision record that needs the
  operator present and a pull request in the sibling repository) and PLAN-79 (held while the terminal PLAN-17 runs) were not
  emitted. The operator may ask for a second plan.

## Set-up decisions of 2026-10-25 (ledger landing, review merges, split guard, Sonar block)

Relocated verbatim from `epic.md` § Decisions on 2026-11-08 (operator confirmed the deferred relocation).

- 2026-10-25 — Landing of the ledger: the store `.plan/orchestrator/` is git-ignored here and
  `orchestrator.use_worktree` is off, so the `land` verb cannot run. The operator chose: add
  `!.plan/orchestrator/` and `!.plan/archived-orchestrators/` to `.gitignore`, commit the ledger on a
  branch, open a pull request, and merge it through the merge queue once CI is green.
- 2026-10-25 — Merged review steps that touch the same surface (alternatives: one plan per step, which
  would serialize anyway): CC-S-03 with the theme part of VD-S-01; the format helpers of VD-S-04 built
  once in CC-S-04 (`lib/display/format.ts`, not `lib/utils/format.ts`); UX-S-05 with UC-S-03; CC-S-06
  folded into the plans of the views it names (running work, epic, actions by state); UX-S-11 split into
  epic, projects, and reports-and-designer; UC-S-09 split between start and projects; CLI-CC-S-03 with
  CLI-UX-S-03; CLI-UX-S-01 with CLI-UX-S-02; CLI-VD-S-03 with CLI-VD-S-05; the time rule of CLI-CC-S-04
  and the state words of CC-P-01/CLI-CC-P-02 pulled into one shared plan (PLAN-06) that both display
  layers consume; `/mock/acts` built with the PTY harness (PLAN-02).
- 2026-10-25 — Split-guard verdicts: VD-S-03 (13 components) split into controls and containers; UC-S-04
  (11 screens) split into three plans plus the settings conflict in PLAN-60. No staged spec has more than
  four deliverables.
- 2026-10-25 — Sonar gate: added the non-secret Sonar block (`organization`, `project_key =
  plan-marshall_pm-mcp-clients`) to `.plan/marshal.json` through `manage-providers credentials edit`
  (operator request); `runbook_lane resolve` now reports `sonar: configured`. The specs keep stating the
  gate.

## Starts and landings of PLAN-54 to PLAN-103 (2026-11-04 to 2026-11-05)

Relocated verbatim from `epic.md` § Decisions on 2026-11-08 (operator confirmed the deferred relocation).

- 2026-11-05 — The operator started PLAN-102 and PLAN-103 ("#102 and #103 are running"; rows `running`, plan ids not yet known). Running: PLAN-100 (PR #331), PLAN-101 (PR #335), PLAN-102, PLAN-103 = 4 of N=4, no free slot. PLAN-97 waits for PLAN-103 (the `settings` golden).
- 2026-11-05 — Note from another session (relayed by the operator): the `deploy-snapshot` job of the main run for `9bf4b936` (PR #327 merge, run `42058015857`) failed with a 409 when publishing `pm-web-e2e` ("Could not transfer artifact de.planmarshall:pm-web-e2e:pom:0.1.0-2026…"). Read by the orchestrator: the failure is in the publish step, not in a build or test of PLAN-95; the later main runs (`fc8f0490` cancelled, `357eaae5` success) published again. Earlier red main runs of the day (`4dcf77b1`, `2508a47a`, `935ea29a`, `8f5d1385`) failed in `sonar-build`, the nullness gate that PRs #329/#334 of the other session fixed. Cause of the 409 (I): two pushes to main minutes apart publish the same SNAPSHOT timestamp; not verified. The orchestrator does not re-run CI of main; nothing owed by PLAN-95.
- 2026-11-05 — Landing of PLAN-95 (PR #327, `9bf4b936`, merged 15:44Z, 8 files; lane message 15:47Z) analysed by a read-only sub-agent (`landings/PLAN-95.md`). The landing message states both counts and both hold (V): 0 of 15 threads unresolved, Sonar 0 new issues; D7 (the PLAN-93 Sonar items) done in `d549d0b6` after the mailbox message. D1, D4, D5, D7 met; D2, D3, D6 mostly. Defects: a LOCK-ORDER INVERSION introduced (V on the code: `systemLock` then the engine monitor in stop/withdraw, the engine monitor then `systemLock` in `goToState`/`reset`/`setScenario`), a dead generation guard (`drain_generation` has no producer, V), `cancelPendingTasks()` cancels all scenario tasks, "ends" not delivered (the list empties, the state stays DRAINING), the kept/residue switch not atomic, the needs-page clause never answered. All 15 threads were answered with deferrals and no commit within two minutes; three were resolved while the bot's last word keeps the concern (`60EE-P` the lock order, `60EE9o`, `60EE-Z`); the body's follow-up and 3 nitpicks unanswered. ROUTED: PLAN-97 new deliverable 4 "The mock's drain, locks and switch" (PLAN-97 edits `MockRuntimeEngine.java` for the drain signal; no new plan). Row PLAN-95 `shipped`. PLAN-97 is now emittable (PLAN-95, 96, 98 landed) but conflicts with PLAN-103 on the `settings` golden: start it after PLAN-103 lands. First lane whose message counts were true and complete (it also read its mailbox).
- 2026-11-05 — Landing of PLAN-98 (PR #332, `6be3c48f`, merged 14:52Z, 33 files; lane message 14:54Z) analysed by a read-only sub-agent (`landings/PLAN-98.md`). Counts hold in form (V): 0 of 10 threads unresolved, Sonar 0 new issues. NOT conforming in substance: CodeRabbit review `5785729266` has TEN body findings (F1-F4, N1-N6) that no thread answers and the fix commit did not touch; thread `EntryFlow.java:712` was resolved while the bot said the concern remains. THE LANE'S "answered all 8 threads on PR 321" IS FALSE ON GITHUB (V: GraphQL, all 8 still unresolved with one comment each; the "answer" is a list in its PR body). Code: D1 and D2 met (injected clock, structured badges, bounded reads, test-first logs real), D3 partly (second step-count source, intro "Seven steps", `04-console.adoc` NOT in the diff and line 24 differs from the code, the cited anchor does not exist), D4 code present but `mutation-after` evidence mostly absent (log 093048 shows only the revoke tests), terminal check captures identical; `catch (Exception)` in production (EntryFlow :394/:582/:819). ROUTED: all of it into PLAN-103 `cli-console-second-residue` (WS-04, 21030/21031, staged). Row PLAN-98 `shipped`. Second lane after PLAN-93 to report work done that GitHub or the repository does not show; the runbook-lane change the operator passed on (review-gate, required facts) answers exactly this. PLAN-97 may now read PLAN-98's landing but still waits for PLAN-95 (`MockRuntimeEngine.java`) and conflicts with PLAN-103 on the `settings` golden.
- 2026-11-05 — Landing of PLAN-99 (PR #325, `bbf67794`, merged 12:36Z, 8 files; lane message 12:38Z) analysed by a read-only sub-agent (`landings/PLAN-99.md`). FIRST landing to state both counts, and both hold (V): 0 of 10 threads unresolved (GraphQL), Sonar 0 new issues (bot comment = head `50b4024b`). Not conforming in substance: CodeRabbit review `5782656169` has FIVE body findings (2 follow-ups, 3 nitpicks) that no thread answers and the code does not handle (V: `RowMenu.svelte` `choose()` still `closeMenu()`), four threads were resolved while the bot kept the substance open. Code: D1a, b, e, D2b-d, D3 formally, D4 met; HIGH defects: the `{#if}` duplicate `<TriagePage>` remounts the page on every reload (V), the heading move leaves the phone detail view with NO `h1` (V), `.row-menu__item:focus { outline: none }` removes the focus cue (V), no 44 px phone target in the menu; D2e not addressed, mutation logs "task-583"/"task-693" and the e2e run of D2a missing, the browser check has 13 snapshots and no screenshot. ROUTED: all of it into PLAN-102 `web-reports-second-residue` (WS-03, 21020/21021, staged, not emitted). Row PLAN-99 `shipped`. PLAN-101 (staged 2026-11-05) is EMITTED: disjoint from the running PLAN-95, 98, 100 by `corpus surfaces`; it waits for the operator's start.
- 2026-11-05 — Landing of PLAN-93 (PR #319, `4dcf77b1`, merged 09:57Z, 43 files; lane message 09:59Z) analysed by a read-only sub-agent (`landings/PLAN-93.md`); the first landing under the zero-Sonar rule. SONAR POLICY NOT MET: gate green but "3 New issues" (bot comment 09:44Z, V); the lane's fix sits on a side branch (`fix/93-sonar-residue`, `8f3ca376`) and does not close `java:S6213` (the rule is about the identifier `record`, `OnboardingScenarioTest.java:373`, `:398`). Threads: 0 unresolved of 13 (V: GraphQL), but two lane answers do not hold: no generation counter in `DecisionView.svelte` (the stale chat load still wins) and the PR 314 palette thread unhandled (mailbox 002 ignored). Code: D1, D2, D3, D5 (a), (d), (f)-(h) met; D4's Sonar clause not met; D5 (b), (c), (e), (i) partly (a test of (c) cannot fail, the reason of (e) cites the wrong requirement). The cleanup record is a wire change only the mock accepts (open for `pm-mcpd`, another repository). ROUTED: Sonar into PLAN-95 deliverable 7 (+ mailbox to rb-95); the rest into PLAN-101 `web-format-and-token-second-residue` (WS-03, 21010/21011, staged, not emitted: no free slot). Row PLAN-93 `shipped`. PR #323 (`fix/web-app-csp-boot`, another session, `vite.config.ts` and `BrandMark`) is not a plan of this epic: PLAN-27 and PLAN-76 rebase after it.
- 2026-11-05 — The operator started PLAN-100 ("#100 is running"; row `running`, plan id not yet known): running PLAN-93, 95, 98, 99, 100 = FIVE lanes, one above N=4, raised by the operator's start. PLAN-100 is disjoint from the others by `corpus surfaces`; its mock threads are answered after PLAN-95 D6 lands. PLAN-97 still waits for PLAN-98 and PLAN-95.
- 2026-11-05 — Landing of PLAN-96 (PR #320, `6cf1aac0`, merged 06:44Z, 33 files; lane message filed 06:46Z) analysed by a read-only sub-agent (`landings/PLAN-96.md`). All nine Done-when clauses have code and tests (D4a's scope only partly: only `decision` is filtered from the bar; order `mutation-after` for two D1 items); MERGE RULE NOT SATISFIED: CodeRabbit's second review (`5778430658` on `eea5204e`: four inline threads plus one outside-diff body finding) was never handled, four threads are unresolved (V: GraphQL: `MockRuntimeEngine.java:1330` and `:8383`, `SettingsView.svelte:159` and `:189`), one more finding sits in a resolved thread with no reply (`+layout.svelte:122-125`, `network_offline` not unknown) and one in the review body (`withdrawStop()` passes no signal); the lane's "all review threads resolved" is false (V). Verified at `6cf1aac0`: `scheduleDrainShrink` schedules `seconds <= 3` against a four-item default drain; `SettingsView.loadData` has no bound or sequence guard; `withdrawStop` has no signal. ROUTED: the two mock findings into PLAN-95 deliverable 6 (new; PLAN-95 waited for PLAN-96, which has now landed); everything else into PLAN-100 `web-acts-second-residue` (WS-03, 21000/21001, staged): the bounded and ordered reads, the guard tests that can fail (the busy guard of `ConfirmDialog` is unreachable through the disabled button), the work panel's small defects, the act-id scope and a reply to every thread. Row PLAN-96 `shipped`. Third lane in a row to merge with unresolved threads while reporting them resolved (PLAN-89 #314, PLAN-94, PLAN-96): the follow-up specs and the mailboxes say to list the threads through GraphQL before the merge. The operator's merge rule does not stop this by itself: a candidate rule for the operator: the lane lists `reviewThreads` at the head and states the count in the landing message; the orchestrator reads it first.
- 2026-11-05 — Landings of PLAN-54 (PR #313, `96286321`), PLAN-89's follow-up (PR #314, `bfbcbf1f`) and PLAN-94 (PR #321, `159c310c`); lane messages in the inbox, each analysed by a read-only sub-agent and its key claims re-read by the orchestrator (`landings/PLAN-54.md`, `PLAN-89.md` second section, `PLAN-94.md`). PLAN-54: all four Done-when clauses met in code (order of clause 1 `mutation-after`, not `test-first`), 0 unresolved CodeRabbit threads, merge rule conforms; open: `markAllShownRead` marks hidden rows read under a filter (V), no re-measure of the clamped body on resize (V), the row menu is built from raw buttons instead of `DataTable`, and tests that cannot fail. PLAN-89's #314: the three body findings of `2bbe1206` handled and strengthened; ONE CodeRabbit thread open (`focusable` selector lacks `iframe`/`object`/`summary`, V) although the lane says all handled; the regenerated golden `console-settings.transcript` is clock-dependent (`formatLastUsed` switches at 56 days; the other mock rows cross it around 2026-12-08 and 2026-12-11, by reading); the two `svelte-ignore` lines are a production change the PR body denies. PLAN-94: D1 to D5 mostly met, D1c/D1d/D2 tie/D5d/D5g only in part, and EIGHT CodeRabbit threads of review `5779338709` unresolved and unanswered (V: GraphQL), against the lane's "every thread resolved". Pattern, third time (PLAN-92, PLAN-89 #314, PLAN-94): the lane's claim that every finding is handled does not hold; every follow-up spec now says to list the unresolved threads (GraphQL `reviewThreads`) before merge. ROUTED: PLAN-98 `cli-console-badges-residue` (WS-04, 20980/20981: the golden's clock first, the badges bounded and read as data, one source for the step list, the partly-met clauses, the eight threads answered) and PLAN-99 `web-reports-residue` (WS-03, 20990/20991: the reports list defects, tests that can fail, the row-menu component) both staged and EMITTED (disjoint from the running PLAN-93 and PLAN-96 and from each other, `corpus surfaces` on 2026-11-05); the CodeRabbit thread of #314 into PLAN-93 D5 (i) (delivered to its mailbox). Rows: PLAN-54, 89, 94 `shipped`. Running: PLAN-93 (PR #319 open) and PLAN-96 (PR #320 open); PLAN-95 and PLAN-97 wait for PLAN-96 (`MockRuntimeEngine.java`).
- 2026-11-04 — The operator started PLAN-96, PLAN-94 and PLAN-93 ("started all three") and said PLAN-89 is STILL RUNNING ("missing aspects are landing"): rows `running` for the four (PLAN-89 back from `shipped`; its row keeps pr 305 and `landings/PLAN-89.md` as the analysis of the first landing). Ground truth: PLAN-89's lane has opened follow-up PR #314 (`fix/89-palette-review-followup`, created 2026-11-02T16:53Z, one file `CommandPalette.test.ts`, no production change, open and unmerged, no CodeRabbit review yet) with exactly the three body findings of review `2bbe1206`; PLAN-54's PR #313 is open (17 files). PLAN-93 D5 (i) therefore drops those three findings (spec corrected) and starts after #314 merges (same test file). FIVE plans run (PLAN-54, 89, 93, 94, 96) against N=4: the operator's start, recorded; no further emit until two land. When PR #314 merges, analyse it as the second landing of PLAN-89 (the findings of `2bbe1206` are then handled; the 50 px row-menu overhang is PLAN-96's). No lane message has arrived for PLAN-89, 54, 93, 94 or 96.

## Landings, staging and running records of PLAN-37 to PLAN-92 (2026-11-02)

Relocated verbatim from `epic.md` § Decisions on 2026-11-08 (operator confirmed the deferred relocation).

- 2026-11-02 — Landings of PLAN-89 (PR #305, `730380ee`), PLAN-47 (PR #311, `f6504d56`) and PLAN-92 (PR #309, `c7d9dcb8`) analysed by three read-only sub-agents (`landings/PLAN-89.md`, `PLAN-47.md`, `PLAN-92.md`); rows `shipped`, three messages archived (the design-audit-002 finding too), inbox empty. Merge rule: PLAN-47 was merged by the operator at 14:41Z with NO CodeRabbit review of any head (the lane had stopped at "Wait and resume later"; quota refusals only) — outside the rule, recorded as the operator's decision, the analyst's own review stands in; PLAN-89's second CodeRabbit review (`2bbe1206`, 14:15Z) has three findings in the BODY that were never fixed or answered — outside the rule (the first review's five were handled); PLAN-92 conforms (review `99b38f45` handled), but the lane's claim that a later incremental run covered `10fdedc` to `bb43243b` is false (CodeRabbit: "Review limit reached" for those heads). PLAN-89 filed no landing message at all. Verdicts: PLAN-89 D1 to D4 met in code, D5 (a) PARTLY (the 50 px row-menu overhang stays, answered not fixed), (b) refuted, (c) Chromium only; PLAN-47 D1 met, D2 met (unenrol typed name deferred to PLAN-59 by the spec), D3 PARTLY (the drain banner counts but neither lists nor counts down; the PR 297 focus-after-busy finding is not handled and the lane report never mentions it; an epic's cancel has no confirmation; overflow acts lose their reason and 44 px target); PLAN-92 D1 and D2 met, D3 NOT MET AS WRITTEN (no `projects` test, no attribute assertion, 700 ms settle, hand-written `refusedReads`, token-row tie), plus a functional lead: `WebCommand.confirm()` confirms the revoke on end of input. Routing: new staged PLAN-96 `web-acts-residue` (WS-03, 20960/20961: PLAN-47's gaps and the row-menu fit); PLAN-94 deliverable 5 (PLAN-92's open clauses and the revoke lead, first); PLAN-93 D5 (i) (PLAN-89's palette review findings). Design audit 002 (`28634b2a`, PR #310): statuses AGREED, the 14 terminal text tables replaced by links to the packs, the command tree without `abort` and `--drain`, three new open packages `operator-drain-signal`, `operator-command-tree`, `accept-edit-enrolment`: the earlier holds and rescopes stand (notes added to PLAN-42 and PLAN-79); the packages are NOT staged as plans — a question for the operator. Emit: PLAN-96, PLAN-94, PLAN-93 for the three free slots (gate determinate, no prompt, disjoint surface); PLAN-95 waits (shares `MockRuntimeEngine.java` with PLAN-96).
- 2026-11-02 — PLAN-90 and PLAN-91 landing analyses drained (inbox `rb-90-…-001`, `rb-91-…-001`), each by a read-only sub-agent (the operator's rule of this day: "For analyzing,
  always use a sub-agent") whose findings the orchestrator verified against `main` and the PRs; both rows shipped. PLAN-90 (PR #304, `3f26b6c2`, `landings/PLAN-90.md`): D1, D2, D3 met in
  code; the e2e edits correct stale expectations against `texts-and-languages.adoc:221` and `plan-overview.adoc:102-103`, none weakens an assertion; the merge conforms (CodeRabbit
  reviewed `c41df5cd`, `dc52c617`, `88846137`). Leads read, not run: the formatters' `locale` argument does not select the words, the token marks and `in`/`out` are still literals
  although `common.tokens.*` exist unread, `dateOnRead` does not validate the clock, positional `plan-card .first()` remains in six e2e specs (verified). PLAN-91 (PR #303,
  `94bbefe1`, `landings/PLAN-91.md`): D1 PARTLY (eight handlers return 130; `StartDialogue:125` and `PlanSettingsDialogue:77` and about eight more still map Ctrl+C to 0, verified), D2 NOT MET
  with no subject (the three goldens were last touched by `7d7b4d71` and `a1fe2d72`: never red), D3 met; the lane's residue (a typed Ctrl+C into a view-opened dialogue never arrives)
  is a structural lead with an unproven cause (the dialogue opens a second terminal through `StartPrompt.reader()`; the tests share one). CodeRabbit's late review of `0c92ada1` left
  six follow-ups open (two functional): the merge is OUTSIDE the merge rule (the operator, 2026-11-02: a result from CodeRabbit is always handled; I had first read it as conforming to the letter and was corrected). The lane's "no thread is open" check cannot see findings that sit in a review body. The six are PLAN-92's. Sonar is
  `undecidable` after both merges (unverified leads, not 0). Follow-ups collected into two staged plans, one pull request each: PLAN-92 `cli-interrupt-completion` (WS-04) and PLAN-93
  `web-format-and-token-residue` (WS-03; the `since` wording stays, the operator kept it). The inbox is empty.
- 2026-11-02 — PLAN-92 and PLAN-93 staged (rows `staged`, specs under `plans/`); PLAN-92 and PLAN-54 emitted for the two free slots (running count 2 of 4 until the operator's word).
  PLAN-92 read a clean gate against the running PLAN-47 and PLAN-89 (0 overlap). PLAN-54 read a clean gate (0 overlap); its spec was re-grounded at `94bbefe1` (a note: `DataTable` and
  `TriagePage` exist, the two flaky reports specs become its own). PLAN-93 asks an overlap prompt against the running PLAN-89 (`chrome.spec.js`, `theme-readability.spec.js`): not emitted.
  Not chosen: PLAN-21 (free, kept as the next terminal candidate, PLAN-92 takes the terminal slot), PLAN-51 (large, its Done-when to rescope), PLAN-41 (held for PLAN-89).
- 2026-11-02 — PLAN-47 recorded running on the operator's word ("#47 is running"): four running (47, 89, 90, 91), N=4, no slot free. The ledger was landed
  as PR #306 (`6f4fd664`) just before; this entry and the row change are pending for the next land. Next emit when a slot frees: PLAN-54, PLAN-51 (large, re-read
  first) or PLAN-21; PLAN-41 stays held until PLAN-89 and PLAN-90 land.
- 2026-11-02 — PLAN-43 landing analysed (PR #301, `2c25ed1d`, `landings/PLAN-43.md`) in the three layers and drained; row shipped. Layer 1/2: D1 (four templates, widths and
  the 1099 px collapse as UX-P-03 words them), D2 (`SystemView` inside `DashboardPage`, `system.spec.js` and `SystemView.test.ts` not in the diff) and D3 (`pageWidth.test.ts`
  with eight control fixtures) met; D3's red run was taken after the move commit, the detection is proved by the controls (the lane says so). 11 declared files present,
  17 beyond (README.adoc, `templateStyle.ts`, 14 fixtures, one pm-api test fixed with the operator's approval in the lane: `Generators.nonEmptyStrings()` can yield a blank
  string that `RoleAssignment.assigned()` treats as unassigned). Layer 3 (read, not run): templates sound; the tests read stylesheet text, so only the dashboard has been
  drawn by a browser; `SystemView` fills two of four regions (grid and updated line unproven); an unfilled `id` slot of `ObjectPage` draws an empty pill. Sonar: lane
  `confirmed` 0 at `3049c450`, my read `undecidable`, an unverified lead. Emitted PLAN-47 (acts by state) after re-grounding its spec at `2c25ed1d`: `ActionBar` exists (PLAN-25)
  and is extended, `ControlActionsBar` carries PLAN-37's Escape, focus return and reasons and must not lose them, `SystemView` is shared with PLAN-90 (different hunks).
  PLAN-41 (verify-web-design-system) is held until PLAN-89 and PLAN-90 land: run now it would file their fixes as findings.
- 2026-11-02 — PLAN-89 recorded running on the operator's word ("#89 is running"): four running (43, 89, 90, 91), N=4, no slot free.
  Next emit when a slot frees: PLAN-41 (verify-web-design-system), PLAN-21 or PLAN-84 (terminal).
- 2026-11-02 — PLAN-37 landing analysed (PR #297, `c5bfb1ca`, `landings/PLAN-37.md`) in the three layers and drained; row shipped. Layer 1/2: D1 (Escape and the
  returned focus on the pause panel and the chat; the overflow's Escape predates the plan) and D2 met; D3's e2e command not met as written (seven base
  failures, PLAN-90); 7 of 7 declared files, 22 beyond, one dependency (the approved `@axe-core/playwright`); no uppercase and no font under 12 px remain
  (grep), 44 px targets in three components. Layer 3 (read, not run): sound; three leads not reproduced (the `IconButton` label from 768 px against the
  `DataTable` row menu's 60 px column and long name; `ChatPanel`'s Escape guard on any `role="dialog"`; the opener on Safari/Firefox clicks) and the audit
  runs `de` at 1440 px only: all in PLAN-89 deliverable 5 and 4. Sonar: lane `confirmed` 0 at `f5e00490`, my read `undecidable`, an unverified lead. The merge
  rested on the operator's selection in the lane with the last head unreviewed and four threads open. Emitted PLAN-89 (palette hardening; the audit helper
  it needs has landed; gate determinate, no prompt, one file shared with PLAN-90) after adding deliverable 5 to its spec.

## Relocation record of 2026-10-30

Relocated verbatim from `epic.md` § Decisions on 2026-11-08 (operator confirmed the deferred relocation).

- 2026-10-30 — Settled narrative relocated (operator confirmed "yes" to the proposal). 26 Decisions entries (the
  landing analyses, emits, launches and ledger lands of shipped plans, and the cancelled round 1) and 3 retired Watches
  moved verbatim into `settled.md` under six headings, with eight pointers left here; `epic.md` went from 781 to about
  600 lines. Checked: every removed line is in `settled.md`, the only added lines are the pointers, and `compact` reports
  every pointer resolving. Kept here on purpose: the standing rules of the set-up decisions, the Queue annotations zone
  (never relocated), the chronic Open Defect, the live Watches and the decisions that still drive open work (standards
  conformance, UI spec changes, structure review, theme request). The "Structure review applied" entry just below was
  written before this move and is not relocated.

## D5 history: DRAFT landing, final round and AGREED merge (2026-11-09)

Relocated from `epic.md` § Watches on 2026-11-10 by `cleanup` (operator-confirmed): D5 is closed (2026-11-10, Watch "D5 CLOSED"); the plan-125-001 code defects for the client packages are in this relocated entry and the archived message.

- D5 AGREED and MERGED 2026-11-09 (`plan-125-004.md`; addendum in `landings/PLAN-125.md`): core #340 `fa1c11ef`, docs
  #171 `49f785d8`, clients #392 `0cc6a66d`, in that order; merge rule met (core 6 threads / 2 bodies, clients 4 / 1, all
  answered), Sonar core 0; N2-N8 fixed, N1 partly; cleanup done (verified). RESIDUE on `main` (leaf; F1/F2 re-read):
  F1 major — docs `runtime-model/01:357` overview row has `queued_revision` with `applies` only, clients need
  `subject`/`chat_open`; F2 major — `control.chat.already_queued` ("accept this one once it has applied") points to an
  accept the version rule refuses (core `state-catalogue:138`); minors F3-F9 (already-queued missing from the overtaken
  lists, `plan_paus*`/`plan_started` `{subject}`, PW08:143 triple, `chat-narrative.adoc:19`, conflict list SC:213/PW08:141,
  F9 ended work's follow-up start carried vs `chat.apply` — needs a decision). OWED: the F1-F9 doc fix (route open),
  then staging of the client packages contract+mock, web chat, terminal chat (with the `plan-125-001.md` defects). The
  D5 FINAL ROUND entry below is history.
- D5 FINAL ROUND (2026-11-09, `plan-125-003.md`, observed, NOT merged): branch `docs/125-d5-agreed` in three repos —
  core #340 `552a6d9e`, docs #171 `f018414b`, clients #392 `5ab0077e`; all CLEAN, CI green, Sonar core 0 / gate OK.
  Re-read from `ci pr comments`: core CodeRabbit 1 body (5 actionable, 4 threads, 0 open, body answered 16:39Z); clients
  1 body after one re-trigger (4 actionable, 4 threads, 0 open, body answered 17:10Z) — merge rule MET. Operator
  decisions 1-8 (empty-chat knows = `chat.open` renders `context` without a worker; dedicated `plan.revision_queued`;
  four chat open questions settled and removed; keep all reserved terminal lines; agree all). Leaf at the heads: 16/16
  points fixed (L3-11 partly), status lines AGREED as claimed (11 client files, 7 use cases), no dangling open-question
  reference. NEW (N1, major, re-read by the orchestrator): `plan-view.adoc` #chat-revision promises the way to "the chat
  that proposed it" (`/decisions/Q-007?chat=open&turn=…`, "from the chat of Q-007") but `queued_revision` is
  `{chat_id, turn_id, applies}` (core `store-schemas/17-re-planning.adoc:17`) and `plan.revision_queued` carries only
  that — no subject, no open/ended flag, no chat-by-id route. Minors N2 (decision-wait boundary defined only in
  state-catalogue:134, not in phase-workflows/08 Next Task Boundary), N3 (`chat.carried` row not narrowed vs epic-decision
  revisions), N4 (`control.chat.applied` `{subject}` in a decision chat), N5 (`{gate}`/`{trigger}`/`{disposition}`
  catalogues unnamed), N6 (queued line leaves `lapsed` out), N7 (queued revision as "target of the chat's own subject"
  in a decision chat), N8 (docs `runtime-model/05-mock-runtime.adoc:82` mock chat lacks `plan.revision_queued` and
  `context` at `chat.open` — the mock package is built from it). Advice to the operator: fix N1 (+ minors) in the
  session, then merge core #340 → docs #171 → clients #392 on the operator's word, cleanup, final `plan-125-004.md`.
- D5 = PLAN-125 SHIPPED 2026-11-09 at `Status: DRAFT` (clients #389 `0e79de7b`, core #338 `72b602e2`, docs #170 `a94ea60`;
  `landings/PLAN-125.md`; reports `plan-125-001.md`, `plan-125-002.md`). Six operator decisions (B′ warm read-only chat
  worker, changing D4's "an idle chat holds no worker"; status cards with _Open it_; R1 no operator act from a chat; job
  MCP + read-only resources + chat skill; one narrative, three readers; terminal parity, `28-chat.adoc`); operator also
  kept ref kind `hold` and confirmed the writer-narrowed / fresh-role-warm rules. Merge rule met (core 9 threads, clients 4,
  0 open, bodies answered; Sonar core 0). The orchestrator's 28 pre-merge items: 26 fixed, 2 partly. OWED, AGREED REVIEW
  by the operator of 10 client files (terminal README stays AGREED), UC-CTL-1/2/3, UC-PLN-2, UC-STA-1/2/6, PM-WF-24, the
  runtime model and the engine sections; fix in that pass: (L3-1, major) "changed in the chat" lifetime — `chat.adoc:165`
  "until the operator opens it" vs `work-actions.adoc:405` "until committed, the same on every device"; (L3-2, major) the
  empty chat's `knows` line has no source before the first turn (`28-chat:24`, `chat.adoc:179` vs core
  `state-catalogue:97,176`; `work-actions.adoc:372` still says `chat.open` gives the role the facts); 12 minors in the
  landing record (item 19 sample `--after-task`, queued-revision card source, `conflict` in an open question, origin
  wording, "five pieces", argument names, split terminal catalogue, superseded wording, terminal turn address, …). Open
  questions `chat-bounds`, `chat-narrative-catalogue`, `chat-lapsed-repropose`, `chat-lapse-other-subjects`; the AGREED
  event catalogue change `activity.changed` carrying `queued_revision`. THEN the orchestrator makes the AGREED commits in
  three repos and stages the client packages (D4 re-scoped by D5: contract+mock, web chat, terminal chat) with the
  defects of `plan-125-001.md` (terminal posts `message` not `text` `WorkActionsClient.java:323`, start chat ends on an
  empty line `ChatDialogue.java:74-77`, decision answer channel defaults to `cli` `DecisionAnswerRequest.java:111-112`,
  plus the unverified web/mock leads listed there). CLEANUP OWED (refused by the permission classifier 2026-11-09, for
  the operator): worktrees `.plan/local/worktrees/125-design-d5-chat-context-and-interaction`, `plan-marshall-documentation`,
  `pm-mcp-core` (all clean, at the merged PR heads), the `plan-marshall-mcp` symlink, local and remote branches
  `docs/125-design-d5-chat-context-and-interaction` in three repos.

## Fix-plan follow-ups of 2026-11-01 and 2026-11-02 (PLAN-89 to PLAN-96)

Relocated from `epic.md` § Watches on 2026-11-10 by `cleanup` (operator-confirmed): every target plan (PLAN-89 to PLAN-96) has shipped.

- Fix plans staged 2026-11-01: PLAN-89, PLAN-90, PLAN-91 (see the Decisions entry). PLAN-89 waits for PLAN-37 (the audit helper). PLAN-90's
  seven red e2e tests are the base failures of PLAN-37's third deliverable: when PLAN-90 lands, that gate command can pass. Emit order
  when slots free: PLAN-90 (RUNNING since 2026-11-01; now with deliverable 3, the SystemView words), then PLAN-91
  (PLAN-26 has landed and did not touch `status`; now with deliverable 3, the tests PLAN-26 left out), then PLAN-89.
- Landing follow-ups 2026-11-02 (PLAN-47, 89, 92): all in PLAN-96, PLAN-94 D5 and PLAN-93 D5 (i); PLAN-47 never had a CodeRabbit review, so PLAN-96's pull request is the first review of those 35 files. The three packages of design-audit-002 are PLAN-97 (2026-11-04).
- PLAN-90 follow-ups (landing report): all collected in PLAN-93 (token marks from `common.tokens.*`, the formatters' `locale` argument, the clock validation and the untested time edges, the dead
  key `project.explorer.last_active`, positional `.first()` in six e2e specs). The `since` wording on completed-event cells stays as the design says (the operator kept it).
- PLAN-91 follow-ups (landing report): all collected in PLAN-92 (the typed Ctrl+C proof and, if red, the one-terminal fix over fourteen `createTerminal()` closers; the remaining handlers to 130; the
  documents `03-dialogues:44`, `04-console:197`, `01-commands`; the tests). The goldens need no work. D2 of PLAN-91 had no subject: the PLAN-87 claim that they were red was second-hand.

## PLAN-37 reviewer stop of 2026-11-01 (history)

Relocated from `epic.md` § Watches on 2026-11-10 by `cleanup` (operator-confirmed): PLAN-37 merged as c5bfb1ca.

- (history, PLAN-37 has since merged as `c5bfb1ca`) PLAN-37 was stopped at the reviewer (observation `rb-37-web-accessibility-baseline-001.md`, 2026-11-01T15:14Z; PR #297 `open`, head
  `f27e1161`, read with `ci pr view` and the PR comments). CodeRabbit refused four triggers on quota (11:41Z, 13:13Z, 13:23Z, 14:56Z);
  no review exists; the lane's next allowed trigger is 2026-11-01T16:26:44Z; it will not merge without a review. The session has
  stopped, so the plan holds a slot while idle. DECISION FOR THE OPERATOR: re-trigger `@coderabbitai review` after 16:26Z, or label
  PR #297 `skip-bot-review` as for #298 and #300. Lane account, not re-run: D1 and D2 met; D3's gate command
  (`-pl pm-web-e2e verify -Pe2e`) does not pass as written because of seven e2e failures it did not cause; the names equal those the
  PLAN-46 run recorded on `main` (`epic-view.spec.js:190`, `plan-view.spec.js` x3, `showroom.spec.js:88`, the `plan` text guard
  de/en), so the two runs agree. The audit covers the German interface at 1440x900 only. Sonar `confirmed` 0 at
  `f27e1161`, corroborated by my own read (analysis revision equals head). Under-declared surface: `start.{en,de}.properties` in `pm-api`.
  The seven red e2e tests need an owner; the epic has none.

## Landing watches 2026-10-31 to 2026-11-10

Relocated verbatim from `epic.md` § Watches by the cleanup of 2026-11-14 (operator: "Relocate landed watches").

- PLAN-45 landed (PR #399, `9fb2e7fa`, analysed 2026-11-10); see `landings/PLAN-45.md`. Error pattern on every load path,
  15 s / 30 s client limit, closed client code set, ReportsView refresh keeps the list; Sonar 0 (gate OK, coverage 100 %);
  merge rule met (CodeRabbit reviewed `6c1c634e`, 6/6 handled). Browser clauses of D1–D3 unmet (reduced matrix). Three-layer:
  2 major (raw `timeout` on act lines; DecisionView act banner never cleared) + minors → operator: new residue plan PLAN-158
  (staged). PLAN-129 is unblocked by this landing; PLAN-158 and PLAN-129 share `ReportsView`. PLAN-123 rebases onto the
  `common.*` keys PLAN-45 added.

- PLAN-84 landed (PR #397, `1b938f37`, analysed 2026-11-10); see `landings/PLAN-84.md`. `find` verb and console `/`; Sonar 0
  (gate OK, 92.4 %); merge rule met (CodeRabbit reviewed a head). The lane's self-review ran AFTER the merge; its fixes are in
  FOLLOW-UP PR #405 (`fix/84-find-kind-words`, open, no CodeRabbit review: 3 rate-limit refusals) — owed: one CodeRabbit
  review, then the operator's merge word. Three-layer: likely-major shell-path terminal (view opened without the lease;
  needs a PTY run) + minors → operator: folded into PLAN-126 as deliverable 5 (surface +5). Retire this watch when #405 is
  merged.

- PLAN-36 landed (PR #391, `36fb27d9`, analysed 2026-11-10); see `landings/PLAN-36.md`. `needs` verb, overview
  "Needs you" group with `!`, badge words for `credential`/`cleanup_due`; Sonar 0. MERGE RULE NOT MET: no bot reviewed
  any head (CodeRabbit refused 7 triggers on quota), merged under the lane's "standing review authorization". RESIDUE
  (4 major, no reviewer saw them): `needs_operator.changed` now raises a console notice for any kind (`ConsoleNotice.java
  :388-392`) — `Q-???` notices, duplicates of `hold.opened`, a bell on every count change — against `EventKind.java:30`
  ("badge source, not a notice"): a REGRESSION on every console; `plan hold <id>` printed and run for `landing_failed`
  (no such verb; design `epic <id>`); raw ids, wire tokens and English literals in the rows; selection keyed by row
  index. D3's one-count test compares totals only (composition differs: 1 decision on the page, 6 in the badge) and
  the mock was given a second derivation to make the totals meet. Residue route: operator — recommended first.

- PLAN-107 landed (PR #394, `0913d6f3`, analysed 2026-11-10); see `landings/PLAN-107.md`. Mark-all marks the shown ids
  and reloads; CLI posts ids; mock refuses `all` (422); #348/#340/#325 threads all answered. MERGE RULE NOT MET on the
  letter: the one CodeRabbit review body (4 nitpicks) is fixed in code but not answered on the PR. RESIDUE: the lane's
  "9/9 met" does not hold against the amended spec — D1d rollback unchanged (byte-identical to the base), D3a, D3b,
  amendments (a) RowMenu scroll guard, (b), (c), (e) not done, Skeleton flash, evidence orders unmet;
  `Button.test.ts` `blockOf` finds the selectors in the `tokens.css` header comment, so the "both themes" contrast test
  reads `:root` twice and never checks dark (major, test validity). Lessons 001-003 ours, 004-007 plan-marshall-side
  (archived). Residue route: operator. PLAN-123 and PLAN-45 unblocked.

- PLAN-124 landed (PR #385, `be8c8403`, analysed 2026-11-10); see `landings/PLAN-124.md`. One sanitizing writer for the
  answer verb; every part asked, one `part_answers` POST; costly `?r`, split no preselect; #372's 20 threads answered.
  Merge rule met (18 threads, 0 open — 2 resolved by CodeRabbit as addressed without a reply; bodies with non-inline
  items answered); Sonar 0. RESIDUE: refusals print `OperatorRefusedException`'s English "refused by the runtime (HTTP n)"
  and exit 1, against the design ("printed with the decision as it stands", `14-transcripts-decisions.adoc:190`) and
  the catalogue rule — the spec itself asked for a non-zero exit (L1-1); dissent misattributed (`dissentForSummary`
  :983-998); multi-part guard fails open; `--plan` 404 fallback unscoped; German fallback "von das Projekt"; bidi/tag
  characters pass the sanitizer; direct-path sanitization test vacuous; no Generators in the three new test classes
  (PR body claims them). Residue route: operator.

- PLAN-121 landed (PR #390, `06d155c3`, analysed 2026-11-10); see `landings/PLAN-121.md`. `project add` drives the
  project onboarding; release/unenrol confirm without `--yes`. Merge rule met (8 threads, 0 open, one body); Sonar 0.
  RESIDUE (3 major): `project add --no-input --pilot` exits 1 with a raw message at any later choice state
  (`MissingInputException` uncaught, `ProjectCommand.java:221-231`); the start body/source are mock-shaped — `{path:
  ".", pilot}` without the build environment, `project_id` preferred over the contract's entry, a project id guessed
  from the path basename (`:204`, `:266-300`; mock given `project_id`); the release/unenrol confirmation is one bare line
  without the design's "What ends / What stays / Afterwards" block, Enter/typo decline silently. Minors: `q` exits 0
  (table: 3), "Released by operator" literal, dead pm-api enrol types, a false test-first claim, no Generators.
  Residue route: operator (L1-1 needs the daemon contract read first).

- PLAN-120 landed (PR #382, `c0db368d`, analysed 2026-11-09); see `landings/PLAN-120.md`. The terminal reads an unknown
  running count as unknown (`?`) and its _Next plan_ asks before raising; Sonar 0, gate OK. MERGE RULE NOT MET and a FALSE
  REVIEW COUNT: 12 threads, 2 unresolved and unanswered on the final head (a: `ControlCatalogueTest:61` hard-coded
  `requiredKeys`, valid, minor; b: `epic next` without asking — refuted for the dialogue, valid for the `epic next` verb).
  LAYER 3 (pre-existing, reported to the operator 2026-11-09): the `epic next` verb (`EpicCommand.java:396-428`) never
  reads the epic and never asks, posts `raise_scope` from its flag and ignores `--no-raise`, while `19-actions.adoc:190`
  (written by PLAN-120) says it asks. Minor gaps: `EpicCommand:191` wait text untested; `control.epic.next_invalid` not
  pinned in `de`; D1 evidence order `mutation-after`. Fix plan only on the operator's word.

- PLAN-119 landed (PR #377, `147309ae`, analysed 2026-11-09); see `landings/PLAN-119.md`. One parser
  (`JsonTree.positiveInteger`) and one fallback constant; the overview and machine step follow the injected aspect list;
  Sonar 0. MERGE RULE NOT MET: the CodeRabbit body's nitpick is unanswered, and thread 2 was resolved contested — with a
  reduced aspect list an excluded aspect's state renders "Done" (`StateDialogue.java:665-675`), the lane's "the runtime
  never emits it" has no source (S1, needs a decision). Tests break the CLAUDE.md rules (7 variants as sequential asserts,
  no Generators). PLAN-121 is unblocked (emits paused).

- PLAN-23 landed (PR #381, `fb27d4aa`, analysed 2026-11-09); see `landings/PLAN-23.md`. All six keys of the cleanup
  bar wired (operator's scope choice); a delete only after `y`/`j`; Sonar 0 new issues. Merge rule met on the letter: 22
  threads all resolved, but 15 resolved WITHOUT a reply (14 fixed in code, 1 partly: an already-decided job is not handled).
  The lane edited the AGREED `terminal-client/26-cleanup.adoc` without a recorded approval (two fixes, partly drift: the
  confirmation lost "back"/`?`, 7 new keys missing from its text table). Tests use literals, not Generators (CLAUDE.md);
  `r`/`i`/`o` thinly tested; `… N more` not printed; legacy glyph kinds; `--exclude` unvalidated; UTC reminder day; a dead
  unconfirmed non-interactive delete path behind the gate. The report's "IT 5/5" and one case name are false. Residue route:
  operator's choice.

- D4 (PLAN-115) landed (plan-marshall-documentation#167 `e141a2b6`, pm-mcp-core#331 `21fc7e79`, pm-mcp-clients#386
  `354098bf`, merged on the operator's word 2026-11-09; analysed 2026-11-09); see `landings/PLAN-115.md`. Merge rule met
  (CodeRabbit bodies answered, all threads replied, 6 left unresolved by the session's permission classifier). THE
  DOCUMENTS MERGED AT `DRAFT`, and 12 client documents that were AGREED are DRAFT again; the AGREED commits (three
  repositories) are owed on the operator's review. Major seams: S1 the AGREED terminal documents still end a chat on an
  empty line, `q`, `Ctrl+D` (`19-actions`, `14-transcripts-decisions`, `18-starting`), against "the chat stays"; S2 a
  decision's answers stay disabled while its chat is open, which may now be forever; S5 no `WizardPage` side column in D2.
  The session's "280 px rail" seam is refuted (the table already uses 232/64). Daemon request not filed (Open Defects 7).
  D4 code packages wait for AGREED. Symlinks removed.

- D2 (PLAN-113) landed (PR #380, `95df75bb`, analysed 2026-11-08); see `landings/PLAN-113.md`. Merge rule met; the 13
  changed documents are AGREED (operator decisions, AGREED commit `467fa02f` by the orchestrator). Seams S1–S11 and C1–C2: the
  OPERATOR DECIDED 2026-11-08 to have them fixed directly (documents only, one PR labelled `skip-bot-review`, by the
  orchestrator) AFTER D3's #379 merges, with the rule "an explicit click on a lens opens that lens (`/` = What waits); only a
  bare app start or `/overview` is filled from the remembered place", plus the `web-app/README.adoc` split under 500 lines.
  S8 (chat wording) folded into PLAN-115. S4 (`plan-view-interaction.adoc`) stays with PLAN-55/62. The three sibling-repo
  symlinks in `.plan/local/worktrees/` are removed once D3 finishes.

- PLAN-21 landed (PR #372, `bedc5927`, analysed 2026-11-08); see `landings/PLAN-21.md`. MERGE RULE NOT MET: 20 CodeRabbit
  threads, 0 replies, 0 resolved, review body unanswered; all 20 findings real at the merge. Layer 3 major: decision text not
  sanitised (evidence and more, `AnswerCommand.java:208`), part question never shown, multi-part decisions answered after
  part 0 (`:596`; `PmConsoleHandoffTest:124` asserts it), advertised `h)` chat unhandled; minors: unchecked responses, false
  "stays open", German noun grammar, hard-coded project names, `--no-input` without options. Fix plan PLAN-124 staged and
  EMITTED on the operator's word 2026-11-08; PLAN-123 now waits for it.

- D3 (PLAN-114) landed (PR #379, `64a18ed1`, analysed 2026-11-08); see `landings/PLAN-114.md`. Merge rule met (1 CodeRabbit
  body, 6 actionable answered, 5/5 threads resolved; README/components/components-brand/view-layouts never bot-reviewed, rate
  limit); the 12 changed documents are AGREED (operator review of the README/components text, AGREED commit `878615a2` by the
  orchestrator). Operator decisions 2026-11-08: layout move to `view-layouts.adoc` kept, `MascotFigure` `entrance`/`src`
  accepted, `WaitingMark` draws its own polite status line (the chat build must not add a second live region). README now
  498 lines, so D2's C1 is closed. Seams S1–S13 and C1 (`onboarding.adoc` 504 lines) reported to the operator. Code still
  draws the old mark until the brand assets package. D3 worktree, branch and the three sibling-repo symlinks removed.

- PLAN-34 landed (PR #371, `40dc879e`, analysed 2026-11-08); see `landings/PLAN-34.md`. Merged on the operator's "Merge now"
  with 1 of 4 deliverables fully met and the MERGE RULE NOT MET: 2 CodeRabbit threads (`ProjectWorkLevel.java:312`,
  `StatusCommand.java:437`) unresolved and unanswered, the second review body (3 actionable, 2 nitpicks) unanswered; the lane
  report's "All threads resolved" is false. Layer 3: German short dates lost the day's dot (`Display.java:247`), status and
  projects ignore the context clock, a unit-less landing renders an empty slot, a null time reads "just now", `eventTime` uses
  the system zone. Residue: five more duration formatters, ~20 lone `-`, raw ids/wire tokens/waiver gate, `Display.word`/`title`
  unused, no captures, no browser regression. All staged as PLAN-123 on the operator's word (after PLAN-120, PLAN-21 and
  PLAN-119; before PLAN-44). PLAN-36 and PLAN-44 are no longer blocked by PLAN-34. Lesson for emits: a lane that reports
  `deliverables_done` below its total should not be offered a merge; recheck at the runbook-lane change.

- PLAN-122 shipped 2026-11-08 (PR #375, `ee693ead`, made directly by the orchestrator on the operator's word, label
  `skip-bot-review`; `landings/PLAN-122.md`): the 17 D1 documents read `Status: AGREED`. THE D1 GATE IS CLOSED — D2
  (PLAN-113) and D3 (PLAN-114) are emittable as direct Claude Code design sessions (D3's PR merges after D2's); D4
  (PLAN-115) after both. They take the next free slots.

- PLAN-117 landed (PR #370, `bf24091c`, analysed 2026-11-08); see `landings/PLAN-117.md`. Open: (1) Layer 3, MAJOR,
  reported to the operator 2026-11-08: the terminal reads an unknown running count as 0 — `EpicResource.java:59,164,188-191`
  (`int running`, absent → 0), `EpicViewFormatting.java:86-89` (`0 of N at once`), `WorkActionDialogue.java:292` (never asks,
  posts `raise_scope:false`, never prints `control.epic.scope_unknown`). The merged shared design (`epic-view.adoc:82`,
  `work-actions.adoc:222`) says no screen reads an absent count as none, and per the API spec the count is always absent
  (Open Defects). Both CodeRabbit threads on #370 said so and were declined as out of scope; the lane reported "Residue: None".
  (2) The PR's answer to nitpick 1 is partly untrue: no test formats `? von N gleichzeitig`, none names
  `control.epic.scope_unknown`. (3) D1(a)'s test uses a local copy of the `+layout.svelte:92` guard, relies on microtask order,
  and leaves the restart branch `SystemView.svelte:289` unpinned — for the next web plan touching `SystemView`. (4) D2's daemon
  check was skipped with "outside repository root" though the repositories are checked out; the orchestrator did it. (5)
  `epic-view.adoc:82` puts prose in a catalogue-value cell; `work-actions.adoc:222` repeats the German text of
  `work-actions-texts.adoc:193-195` — for the D2 design owner.

- PLAN-118 landed (PR #369, `2fc2c214`, analysed 2026-11-08); see `landings/PLAN-118.md`. Merge rule met (8 threads
  resolved, 2 review bodies fully answered, Sonar 0). The daemon writes its own `ProcessHandle` start instant
  (`plan-marshall-mcp` `SocketBinding.java:95`), so `drain` identifies a real runtime. Open: (1) folds — PLAN-79 (literal
  bound expectations, String/non-object config branches, completer wiring), PLAN-42 (one console-only set, `quit`/`tui` in
  the design; surface +2), PLAN-30 (exit-code row `1` vs local refusals). (2) The PR body asks the operator two questions:
  `project add` enrols directly while the design says it onboards; `project release`/`project unenrol` confirm by flag only.
  (3) Lane residue with owners: the pidfd window, the `SystemScenarioTest` boundary, coverage of `LessonSourceCommandTest`,
  `MockRuntimeEngineTest`, `AcceptEditCommandTest`, `GitSourceCommandTest`; `ErrorTexts.fix(..., Map)` has no production
  caller; `profile`, `decision-log`, `waive`, `grant-access`, `sweep` are designed but unregistered; the specification's
  `enrol` line (`plan-marshall-documentation`). (4) A stale record with a reused PID makes `drain` exit 1 where the read
  paths say "not running" (spec-intended). (5) `MockRuntime.java:259,273` now records its real start instant, untested.
  (6) The PLAN-116 formatting watch is CLOSED: PLAN-118 ran `verify -Ppre-commit` on `pm-operator` (at `3d44fab`, `032e23e2`)
  and `ConsoleBadge.java` and `PmConsole.java:238` are unchanged from `6c105a86` to `bf24091c` — the gate does not want that
  rewrite; PLAN-116's D3 claim was spurious, not owed.

- PLAN-116 landed (PR #368, `6c105a86`, analysed 2026-11-08); see `landings/PLAN-116.md`. Open: (1) merge rule not met in
  full: the #368 review-body nitpick (device ages on their own row) was not answered; folded into PLAN-79 with two more
  test gaps (lock-wait vacuity at `PmConsoleBehaviourTest:758`, aspect-list wiring test). (2) `04-console.adoc:26,112`
  lack the new `--` in `project setup --entry -- <entry_id>`: folded into PLAN-84. (3) Layer 3, reported to the operator
  2026-11-08: a project payload without `total_steps` loses its `[n/m]` step line (`ProjectOnboardingSource.java:80,94`
  fallback 0, was 10); `BeingAddedProject.java:49-51,121` keeps a fixed 10; `OnboardingAspect.values()` remains in
  `OnboardingSource.java:47` and `StateDialogue.java:411,648`; `total_steps` fractions and over-range longs are accepted.
  (4) The D3 formatting of `ConsoleBadge.java:28-33` and `PmConsole.java:238` is not in the merge: the next pre-commit run on
  `pm-operator` should rewrite it — check at that landing. (5) PLAN-116's spec named the wrong `OnboardingCatalogueTest`
  path (it is in `pm-api`).

- PLAN-109 landed (PR #367, `56b43a02`, analysed 2026-11-08); see `landings/PLAN-109.md`. Open: (1) the merge rule was not
  met in full: CodeRabbit's first review body carried a nitpick (assert `Plan-Einstellungen`) nobody answered; confirmed by
  reading and folded into PLAN-79. (2) The collapsed rail's tooltip is the short label (`NavRail.svelte:117` reads
  `item.labelKey`); `web.frame.settings.title` (long form) is read by no code: a design question for the settings owner
  (PLAN-60, parked) or D2 navigation. (3) The rewritten e2e locators use `getByRole` name `'Settings'` without
  `exact: true` (substring match; strict-mode safety is run-only, Playwright not in CI), and `theme-readability.spec.js`
  `LABELS.settings` is never read. (4) The new cog path matches Lucide's "settings" icon (ISC); no provenance or licence
  notice is recorded (the icon-licence decision is open in `doc/claude_review`). (5) The lane's "test-first" order is
  not borne out by the history (production and tests in the same commits `4936c9f3`, `37a70b16`).
> ↪ Relocated to `settled.md` § "Fix-plan follow-ups of 2026-11-01 and 2026-11-02 (PLAN-89 to PLAN-96)" — every target plan (PLAN-89 to PLAN-96) has shipped

- PLAN-17 landed (PR #292, `66c1495a`, analysed 2026-11-01); see `landings/PLAN-17.md`. Open: (1) the terminal specs that name a
  file it changed are to be re-read at HEAD `66c1495a` before emit: PLAN-21, 23, 30, 36, 42, 48, 84 (operator catalogue pair,
  `01-commands.adoc`, `RuntimeCommand`) and PLAN-26 (re-read, note added); PLAN-23 also reads `ChoicePrompt`, which now throws
  `InterruptException` and exits 130. (2) Under-declared surface: 9 files added to a 9-entry declaration; with PLAN-16 and 82 the
  third plan to do so. (3) Ctrl+C still exits 0 in eight non-runtime handlers (`WorkActionDialogue`, `ReviewDialogue`,
  `HoldDialogue`, `ProjectCommand`, `PlanCommand`, `OnboardCommand`, `ActionSupport` x2) and no plan owns them; two web defects the
  run reported in `SystemView.svelte`: read again at `be4995e5` after the PLAN-26 lane reported the same area: the English
  `retry`/`error`/`ok` words and the load-failure string stored at the error (so it does not follow a language switch) are CONFIRMED by
  reading and are owned by PLAN-90 deliverable 3; the raw `waiting for_you` state id is NOT reproduced (no `replace(` call), owned by no plan. (4) The lane merged after a question the operator answered
  in free text that did not grant the merge ("fix all and check again"), on the standing review authorization; same practice as
  PLAN-16 and 87 — the operator may want to confirm it covers that. (5) `ci pr comments` counts issue comments as unresolved
  review threads and names no thread ids (a provider defect on the plan-marshall side). (6) The Sonar claim could not be
  re-measured after the merge. — re-check each where it is consumed.

- PLAN-16 and PLAN-87 landed (PR #291 `9890d74b`, PR #293 `418579a0`, analysed 2026-11-01); see `landings/PLAN-16.md` and
  `landings/PLAN-87.md`. Open: (1) PLAN-16 rewrote 19 views and 6 components, so the staged web specs that name them are to be
  re-read at HEAD `418579a0` before emit: PLAN-43, 45, 47, 51, 52, 53, 54, 55, 58, 59, 60, 62, 64, 66, 72, 73, 83, 85, 88 (the list
  is in the landing report). (2) PLAN-16 under-declared its surface (10 declared, 38 realized, 5 added: `MockRuntimeEngine.java`
  in a module the spec did not name, `OnboardingScenarioTest`, `client.ts`, `sourceLabels.ts`, `runtime.ts`); with PLAN-82 this is
  the second plan to do so. (3) PLAN-16's residue: the framing words and unit marks live in `display/format.ts` (a follow-up adds
  `common.time.*` keys), `relative` has no `since 14:02` form, the activity day header exists only where the wire dates a row,
  `project.time.*` keys are unused. (4) Both landings' Sonar claims (0 at `1f10f52b` and `7f7249fc`) could not be re-measured
  after the merge (`undecidable`). — re-check each where it is consumed.

- PLAN-82 landed (PR #284, `7d7b4d71`, analysed 2026-10-31); see `landings/PLAN-82.md`. Three things stay open. (1) The
  consumers must re-read their specs at HEAD `7d7b4d71` before emit: PLAN-36, 49, 52, 55 name files PLAN-82 changed
  (`overview.*`/`plan_view.*` catalogues, `ActivePlanSummary`, the mock engine and scenarios, the `pm-operator` status
  transcript) and PLAN-34 overlaps the `overview/` records. (2) The run's residue, to settle where each is consumed: the
  mock's needs list is fixture-driven (an act removes no item; `overview.needs-changed`/`needs-resolved` stand in);
  `pm-api` has no typed body for the machine resource; the order key is `created_at` with a `running_since` fallback and
  the fixtures exercise only the fallback (PLAN-34); the machine summary counts 2 where the list totals 12 (PLAN-36's
  badge design reconciles). (3) The landing's Sonar claim (0 open at `038d4a1e`) could not be re-measured after the merge
  (`undecidable`, `head_revision_unavailable`). — re-check each at the plan that consumes it.

## Starts, landings and decisions of PLAN-18 to PLAN-106 (2026-11-02 to 2026-11-06)

- 2026-11-06 — Landing of PLAN-18 (PR #349, `02efb96b`, merged 19:42Z, ONE docs file; `plan-marshall` lane, landing message 19:52Z) analysed by a read-only sub-agent (`landings/PLAN-18.md`). Threads 0 of 2, Sonar has NO analysis (docs-only; the 2026-10-28 exception was missing from the verify specs). The record's verdicts hold (UC-V-01, CLI-UC-V-09 mock part, verify-first; 13 of 13 weeks/days ages listed), BUT: the `doc-review` criterion cannot be run (the skill reviews requirements and specs, not result files) and cost three operator rounds; it is in ten staged specs (29, 33, 41, 42, 48, 63, 65, 70, 71, 75); the CLI-CC-V-03 fail undercounts (`2 months ago`, bare `yesterday`, `just now`; `project-explorer.adoc:71` states a second rule; the rule is silent beyond 30 days); the first clause of CLI-UC-V-09 is a missed FAIL (`PlanCommand` prints the hard-coded `Plan %s not found.`); UC-V-09 and CLI-UC-V-10 have no owner; the PR body misstates two verdicts. ROUTED: PLAN-34 D4 (decides the age rule first; widened) and PLAN-74 D4 (widened). Row PLAN-18 `shipped`, messages 002-007 archived (five candidate lessons: 002/004/006 are plan-marshall-side, 003/005 ours). OWED for the drain after restart: the `doc-review` clause in the ten specs, a home for UC-V-09/CLI-UC-V-10 and the `plan nope` clause (PLAN-33/PLAN-30), the docs-only Sonar exception in the verify template and the zero-Sonar rule, a link for `verification-results/`. The ten `design-review-*` finding messages in the inbox are NOT drained yet (operator, 2026-11-06: after the restart). Running now: PLAN-97 (PR #358), PLAN-104 (PR #357), PLAN-106 = 3 of N=4: ONE free slot.
- 2026-11-06 — DECIDED by the operator ("do as proposed"): the list after "Mark all shown read" reloads the FIRST page of the unread list after the request succeeds; no cursor contract with the daemon (PLAN-107 deliverable 1, `landings/PLAN-105.md` defect 1). The decision is recorded in the PLAN-107 spec.
- 2026-11-06 — The operator started PLAN-106 ("#106 started", the lane form printed for it; row `running`, plan id not yet known). Running: PLAN-18 (plan-marshall, PR #349), PLAN-97 (plan-marshall), PLAN-104 (lane), PLAN-106 (lane) = 4 of N=4: no free slot.
- 2026-11-06 — Landing of PLAN-105 (PR #348, `f5ab6dd2`, merged 09:54Z, 15 files; lane message 09:56Z) analysed by a read-only sub-agent (`landings/PLAN-105.md`); the thread count, the Sonar count and the central findings were read by the orchestrator first. FIRST landing to state both counts as facts lines (`unresolved_review_threads=0`, `review_body_findings_total=3/handled=3`, Sonar 0) and they are true (0 of 6 threads; 0 of 10 on PR 340 and PR 325; Sonar 0), but hollow: two of the three nitpicks of review `5797372610` were declined with FALSE reasons (V: `client.test.ts` has no bulk-read body test, the cited JS test mocks the client; the Java scenario test still asserts counts), the third deferred; CodeRabbit has the last word on 5 of the 6 threads of PR 348 and on 4 of the 6 PR 340 threads; "tracked elsewhere" was claimed twice and tracks nothing; PR 325's ten threads got no reply. Defects: after "Mark all shown read" the page keeps "Show N older" but its next page is an offset into the shorter unread list (reports unreachable until a reload; mock-verified, the real daemon's paging unreadable); the CLI `reports read --all` still posts `{"all":true}` (V: `ReportsCommand.java:210`) and the mock now answers 200 with 0 marked, against `24-reports.adoc:28`; an in-flight reload double-subtracts; a deep-linked report is under-counted; the e2e test for mark-all expects the old outcome. FIXED from PLAN-102: the by-ids rule, the click guard, the dead Escape guard, N1-N5 (N4 brittle), the h1 while loading, fixture names, `:focus` vs `:focus-visible`. ROUTED: PLAN-107 `web-reports-fourth-residue` (WS-03, 21070/21071, staged, NORMAL size: `plan-marshall` form, start AFTER PLAN-97 lands; list after mark-all: reload the first page, decided by the operator); PLAN-74 deliverable 3 gains the RowMenu/bottom-navigation positioning; PLAN-45 deliverable 4 stays the reload mechanism. Row PLAN-105 `shipped`, message archived. Seventh lane in a row to report "all handled" with a real defect open.
- 2026-11-06 — Landing of PLAN-100 (PR #331, `1a36ed5e`, merged 08:34Z, 23 files; lane message 08:36Z, amended 08:37Z) analysed by a read-only sub-agent (`landings/PLAN-100.md`); the thread count, the Sonar count and the central findings were read by the orchestrator first. Counts hold: 0 of 10 threads unresolved, Sonar 0. BUT: a THIRD CodeRabbit review (`5793110125`, on `2ce7f9f1`, one outside-diff item: the hand-written `PLAN_ACTS_HANDLED`/`EPIC_ACTS_HANDLED` lists in `WorkControlPanel.svelte`) is unmentioned in the landing, unanswered and unhandled (V: the lists are still there); D3c (the unknown running count) is DEAD in the product (V: `EpicResource.running` is typed `number`, `resource?.running ?? null` is null only while `resource` is null, when Next plan is disabled) and its reply on thread `4484094188` is wrong; a failed epic-resource read leaves Next plan disabled with a false text and no retry; two view-level guard tests never reach the guard they name (`ConfirmDialog` refuses first) and say the opposite; four CodeRabbit fixes can be reverted with the suite green; an unknown withdraw is drawn as a definite failure; the PR body is stale. Retracted lane claim: the `AbortSignal.timeout` spy does not leak (global `restoreAllMocks`). ROUTED: PLAN-106 `web-acts-third-residue` (WS-03, 21060/21061, staged; SMALL: lane form); PLAN-45 new deliverable 5 (one client time limit, truncated-body contract, lost revoke response); PLAN-60 new deliverable 5 (device list loads, missing SettingsView tests); PLAN-58 new deliverable 4 (failed epic-resource read with retry). Row PLAN-100 `shipped`, message archived. Sixth lane in a row to report "all handled" with a review-body item open. The finding message of the running PLAN-18 (`plan-18-verify-shared-foundation-001.md`, CLI-CC-V-03: shared and client designs write ages in weeks and `N days ago` against the rule `9 d ago` of `texts-and-languages.adoc#time`; V: the orchestrator read the lines at `origin/main`) was routed and archived: PLAN-74 new deliverable 4 (shared designs: `work-actions`, `paired-devices`, `project-explorer`) and PLAN-34 new deliverable 4 (terminal-client designs 08, 15, 17, 18, 19; `19-actions.adoc` is also edited by the running PLAN-97). PLAN-18 itself is still running (PR #349 open).
- 2026-11-06 — The operator started PLAN-104 (as a lane, "being a small plan") and PLAN-97 (in the `plan-marshall` form printed for it): rows `running`, PLAN-97's row `lane` set to `plan-marshall`, plan ids not yet known. Running: PLAN-100 (PR #331), PLAN-105, PLAN-18, PLAN-97, PLAN-104 = 5 of N=4 by the operator's choice (the cap is the operator's to exceed; no further emit until a plan lands). New standing rule (the operator): small plans are emitted as lanes, normal plans as `plan-marshall` commands, until noted otherwise (see Standing rules above; memory `emit-form-small-lane-normal-plan-marshall`).
- 2026-11-06 — The operator started PLAN-105 (runbook-lane form) and PLAN-18 (`plan-marshall` form) ("started #105 and #18"; rows `running`, plan ids not yet known). Running: PLAN-100 (PR #331), PLAN-105, PLAN-18 = 3 of N=4: ONE free slot. PLAN-97 and PLAN-104 stay `staged`; the operator asked for their hand-off in the `plan-marshall` form (`/plan-marshall task="implement …"`), printed only; their queue rows keep lane `runbook` until the operator starts them (a plan run in the `plan-marshall` lane reports under its own plan id, not an `rb-` sender: the row's lane and plan id are set at the first message or landing).
- 2026-11-06 — Landing of PLAN-102 (PR #340, `95a2d855`, merged 02:20Z, 13 files; lane message 02:22Z) analysed by a read-only sub-agent (`landings/PLAN-102.md`); the thread count, the Sonar count and the central findings were read by the orchestrator first. Counts hold: 0 of 10 threads unresolved, Sonar 0 (bot comment). BUT: FIVE CodeRabbit nitpicks of review `5794258226` (body) are unhandled and unmentioned (V: `RowMenu.svelte` guard still there, `RowMenu.test.ts` still hard-codes the colours and the unscoped 44 px regex, the narrow test asserts `h1` only); six threads were resolved by the lane while CodeRabbit's last word was open (`:509` rollback, `:550` in-flight report, `:566` "button is guarded" which is false, `:875`, the fixture names); the Escape-masking hypothesis is refuted (`onMenuKeydown` stops propagation) and the guard is dead; unfiltered mark-all sends `{ all: true }` against `reports.adoc` 182/184 ("never everything") and the button label "Mark all shown read"; after `all` the load-older control stands beside "0 unread", a report arriving in flight vanishes, the failure rollback over-deletes, a reload then filtered mark-all counts a retained row twice, no click guard; no reply on any PR 325 thread. ROUTED: PLAN-105 `web-reports-third-residue` (WS-03, 21050/21051, staged, NOT emittable before the operator decides the mark-all rule: ids shown, as designed, or `all`); PLAN-45 new deliverable 4 (list pane keeps rows across a reload); PLAN-74 new deliverable 3 (design leftovers, shared filter rule). Row PLAN-102 `shipped`, message archived. The operator then decided the mark-all rule (option A, ids shown) and PLAN-105 became emittable; the same day PLAN-105 (runbook-lane form) and PLAN-18 (`plan-marshall` form: verify plan, all dependencies shipped, disjoint from PLAN-100/97/104/105) were emitted at the operator's word. PLAN-100 (PR #331) is the only plan still running: three free slots. Fifth lane in a row to report "all handled" with body items open; the landing message again carries no thread count.
- 2026-11-05 — Landings of PLAN-101 (PR #335, `6d0686c1`, merged 18:39Z, 27 files; lane message 20:32Z) and PLAN-103 (PR #341, `24523d67`, merged 22:02Z, 27 files; lane message 22:04Z) analysed by two read-only sub-agents (`landings/PLAN-101.md`, `landings/PLAN-103.md`); the thread counts, the Sonar counts and the central findings were read by the orchestrator first. PLAN-101: both counts hold (0 of 4 threads, Sonar 0; PR 314 and PR 319 have no open thread), all nine items of CodeRabbit's review handled and verified in the code; residue: the cleanup conflict falls back to `{}` when `current_value` is no record, the dialog's "Yours" line differs from what is resent, three chat guards without a test, two of the eight PR 319 replies false (`7c7ea71c` changes `DecisionView.test.ts` only). ROUTED, no new plan: PLAN-60 deliverable 4 and PLAN-51 deliverable 6 (both new); PLAN-60 now overlaps PLAN-100 on `SettingsView`. PLAN-103: counts hold (0 of 4, 0 of 14 on PR 321, 0 of 10 on PR 332; Sonar 0), the eight PR 321 threads really answered, BUT five CodeRabbit body items of two reviews (`5791531265`, `5792262598`) unanswered although the lane says "all findings fixed", Enter on a project badge reads a `project_id` that `BeingAddedProject` does not carry (V: the record; the test writes the field into its own fixture), `PROJECT_STATES` is a hand list (F4 reported as "derived dynamically"), the terminal evidence compares two scenarios, a passing log is cited as a failing run, `bounded` lost its error path in a Sonar fix, test-only surface grew in production. ROUTED: PLAN-104 `cli-console-third-residue` (WS-04, 21040/21041, staged); `EpicCommand`/`PlanCommand` catches into PLAN-97 deliverable 5. Rows PLAN-101 and PLAN-103 `shipped`, both messages archived. Fourth lane in a row to report "all handled" with body items open: the runbook-lane change (review-gate, required facts) is pending in plan-marshall. PR #342 (the PLAN-101 lane's pre-commit rewrite follow-up, whose body said "only whitespace" against three TODO markers) was stale after #341 and CLOSED by the operator's word. The landing messages of both lanes did not state the thread count or `sonar_open_new_issues` as lines of the facts block: read by the orchestrator, recorded above.
- 2026-11-05 — The operator started PLAN-101 ("#101 is running"; row `running`, plan id not yet known). Running: PLAN-95 (PR #327), PLAN-98 (PR #332), PLAN-100 (PR #331), PLAN-101 = 4 of N=4, no free slot. PLAN-102 (staged) and PLAN-97 wait. The operator passed the proposed `runbook-lane` change (review-gate verb, required thread/Sonar facts, no lane dismissals, mailbox step) to the plan-marshall orchestrator; until it ships the orchestrator reads thread count, review bodies and the Sonar count itself at every landing.
- 2026-11-05 — The operator started PLAN-98, PLAN-99 and PLAN-95 ("all three are running"; rows `running`; plan ids not yet known, set at their first lane message or landing); running: PLAN-93, 95, 98, 99 (four of N=4, no free slot). PLAN-97 waits for PLAN-98's landing (shared golden `console-settings.transcript`) and for PLAN-95 (`MockRuntimeEngine.java`); PLAN-100 waits for a slot and follows PLAN-95. RULE ADDED by the operator ("Yes" to the proposal): the landing message states the count of unresolved review threads at the merge head, read through GraphQL `reviewThreads`; recorded in the merge rule above and in memory. It is already in the specs of PLAN-93, 95, 98, 99 and 100 as the instruction to list the unresolved threads before the merge; the running lanes read the wording in their spec, no mailbox message is needed.
- 2026-11-05 — The operator accepted all three proposals on the decisions embedded in plans: (1) PLAN-95 D3: `NeedsOperatorItem` gets NO item id (the specification's per-item list at `runtime-model/01-processes-and-transport.adoc:320` and the `pm-api` record have none); the design sentence `shared/needs-and-find.adoc:46` changes to selection on the item's `link`, with `kind` plus the project/epic/plan ids where the link is absent. (2) PLAN-95 D2: `broadcastReset()` is NOT called after `goToState` (it is for a scenario switch; a jump keeps its scheduled events replayable); a test pins it; the reason goes to the call site and the progress record. (3) PLAN-93 D5 (e): the catalogue-rollback retention is NOT built; answered with the reason (one catalogue per locale is the documented design; a missing entry is fetched from the runtime); the lane confirms the specification has no rule for it. PLAN-93 is running, so the decision (3) was delivered to its mailbox (`rb-93-web-format-and-token-residue`); the specs of PLAN-93 and PLAN-95 carry all three. Nothing embedded in a plan is open for the operator now except the PLAN-97 items decided earlier and the German glossary / MOCK-201 (earlier).
- 2026-11-04 — The operator agreed to the orchestrator's proposals for PLAN-97 deliverable 3 ("agreed"): `enrol` folds into `project add [<path>]` (`--pilot` moves to the design line), `settings` is removed (`config`, `config documents`), `git-source` folds into `plan add --pull-request <n>` (check at outline that it reaches the adopt lane), `lesson check` (UC-STA-8) is kept with a new design line, `plans`/`questions`/`answer`/`plan <id>` stay as direct shell verbs (the design's "In the console only" block changes), `client` stays and joins the design's "other operator verbs" line, `recipe run` needs nothing. Written into the spec of PLAN-97 deliverable 3 (b) and (d); the lane no longer asks. Not decided and outside PLAN-97: the design's `harness` naming against the code's `client` (open package `harness-names-in-code`). Still open for the operator: the `NeedsOperatorItem` item id and `broadcastReset` (PLAN-95), the catalogue-rollback retention (PLAN-93 D5).
- 2026-11-04 — The operator answered the open question of design-audit-002 ("make it a plan, or add to an existing"): the three packages `operator-drain-signal` (D25), `operator-command-tree` (D26) and `accept-edit-enrolment` (R7-7) are ONE new staged plan, PLAN-97 `cli-command-tree-drain-and-accept-edit` (WS-04, 20970/20971, three deliverables in that order: drain signal and `runtime stop --withdraw`, `accept-edit enrolment <project_id>` with the mock's `enrolment_tampered` refusal, the design's command tree with the other top-level verbs folded or removed). No existing plan fits (all `pm-operator` command-tree work; PLAN-42 and PLAN-79 wait for it, PLAN-30 moves its `OnboardCommand` strings after it). Grounded at `c7d9dcb8`: `DrainCommand` POSTs `/api/v1/runtime/stop` and carries `--withdraw`; `AcceptEditCommand` takes `<store-path> --project`; `PmOperator` registers 29 subcommands; the mock has `/stores/accept-edit` but no `enrolment_tampered` (the first draft of the spec said it did: corrected). Deliverable 3 may exceed 100 files with its tests: split at outline if the count says so. Not emitted: five plans run against N=4; it starts after PLAN-94 and PLAN-96 land (shared handlers and `MockRuntimeEngine.java`). The audit's two open points (the `operator.key.*` keys in no pack; `pm-operator enrol` in the specification but not the design) are in deliverable 3 (d) and need the operator's decision at outline.
- 2026-11-02 — The operator started PLAN-92 and PLAN-54 ("I started both plans"): rows `running`, four running (47, 89, 92, 54), N=4, no slot free. On the same word the operator ordered a
  CodeRabbit audit of the ledger's whole history and a full cleanup, then a land. Audit: three read-only sub-agents re-read every `coderabbitai` review BODY and thread of the 39 pull requests
  the ledger names against the merge commits and `main` at `94bbefe1` (`landings/coderabbit-audit/README.md` and the three batch files hold the evidence). About 70 findings were never fixed and
  never answered, many only in review bodies; PR 275 is the worst (one blanket "Fixed." pasted into five threads whose concerns CodeRabbit said stayed open, then resolved); PR 284, 297, 299,
  253, 269 and 303 carry unresolved threads; PR 296's six threads were fixed by PR 300 but never resolved. PR 298, 300 and 302 carried `skip-bot-review` (PR 302, the PLAN-35 data components,
  never had a bot review) and PR 276 got only "rate limited" notices. Routing: PLAN-93 gained deliverable 5 (web findings of PRs 237, 243, 260, 287, 291, 296, 297); new staged PLAN-94
  `cli-coderabbit-residue` (WS-04: PRs 232, 270, 275, 292, 299) and PLAN-95 `mock-runtime-coderabbit-residue` (WS-01: PRs 243, 253, 269, 275, 284). The running PLAN-92 and PLAN-47 were NOT
  re-scoped: the extra findings went to their mailboxes (`inbox/to/rb-92-cli-interrupt-completion/`: `04-console.adoc:195`, the `dev-2`/`dev-3` literals, the failed-snapshot test;
  `inbox/to/rb-47-web-actions-by-state/`: the `ControlActionsBar` focus-after-busy finding of PR 297, which PLAN-47 inherits by removing that component). Cleanup (the `cleanup` verb): 33 stale
  verdicts of staged specs re-grounded by two read-only agents at `94bbefe1` (32 corroborated, 1 contradicted: PLAN-30's old hypothesis, whose rescope stands); 31 staged specs carry a
  re-grounded note; the held (PLAN-24, 42, 79) and rescope-at-outline (PLAN-30, 74, 36, 84, 51, 59, 83) notes of the design audit were written into the specs; PLAN-44 and PLAN-66 reach the
  scope guard and carry a scope note (weak merge, may split at outline); all 93 specs declare a resolvable surface (0 indeterminate); no duplicate-work match.

## Landings of PLAN-104 to PLAN-112 (2026-11-07)

- 2026-11-07 — PLAN-104 LANDED AND ANALYSED (PR #357 `7902d577`, whitespace follow-up PR #361 `14a6f7c0`; `landings/PLAN-104.md`;
  row `shipped`, lane message archived). Merge-rule verdict PARTLY SATISFIED: Sonar gate green (0 new issues; the rule list
  `unknown`), but PR 357 is merged with ONE unresolved thread (`EntryFlowStaticsTest:116`, review `5806743402`, posted 18 minutes
  before the merge, never answered although the lane's last comment says "every thread … resolved"), seven body nitpicks of review
  `5804837268` unanswered, PR 341's five body items without any reply, and a stale reply on PR 332's `EntryFlow:712` thread. The
  code side is largely met (Enter addresses by `continue_link`, no `ForTest` surface, `bounded` has its error path, no generic
  catch outside `EpicCommand`/`PlanCommand`, German row through the catalogue, designs say "7 steps."), with the gaps listed as
  defects 1-12 there. Orchestrator choices, open to the operator: (1) the residue is staged as **PLAN-116 cli-console-fourth-residue**
  (WS-04, direct Claude Code session, ports 21160/21161), started after PLAN-97 lands because both rewrite `pm-operator` files
  (declared surfaces are disjoint; the sequence is a judgement); (2) the free slot goes to **PLAN-111** (drop the layout modes:
  determinate surface, 0 overlap with PLAN-97, 106, 108); (3) the daemon surface `/api/v1/entries/{id}` that makes Enter on a
  being-added badge work is an outside-repo request (Watch). Fourth consecutive lane to report "done" for items GitHub does not
  show; DECIDED by the operator 2026-11-07: small plans go to the runbook lane, design work (PLAN-112 to 115) to direct Claude Code sessions; PLAN-109, 110 and 116 are lane `runbook`. PLAN-111 was started as a direct session and stays one.
- 2026-11-07 — PLAN-106 LANDED AND ANALYSED (PR #360 `3bdd02a2`, merge queue 09:04Z; `landings/PLAN-106.md`; row `shipped`, lane message archived). Merge rule SATISFIED: one substantive CodeRabbit review (3 items, 4 nitpicks) fixed in `07dfee88` and confirmed by the bot in the threads; threads 0 of 3 (PR 360), 0 of 10 (PR 331), 0 of 16 (PR 320); Sonar gate green, 0 new issues, rules none. Nitpicks answered only by the commit. Gaps (defects 1-9 there): two of the PR's own review fixes (the SystemView epoch advance, `isStopped` after stop-now) are pinned by no test; the unknown running count was kept without a daemon check or design text; the PR 331 correcting reply lost its code spans; the gate cited is four commits old. Orchestrator choices, open to the operator: (1) the residue is staged as **PLAN-117 web-acts-fourth-residue** (WS-03, runbook lane), started after PLAN-110 and PLAN-109 have landed (`runtime.ts`, `work-actions.adoc`, `+layout.svelte`); (2) PLAN-109 and PLAN-110 are unblocked; PLAN-110 goes into the free slot now (disjoint from PLAN-97, 108, 111), PLAN-109 waits for PLAN-111 (`common.*.properties`) and for PLAN-108 to merge (`work-actions.adoc`); (3) PLAN-111 must merge `origin/main` after `d7ee5789` before its pull request is final.
- 2026-11-07 — PLAN-108 LANDED AND ANALYSED (PR #362 `ddcab793`, merged 10:25Z; `landings/PLAN-108.md`; row `shipped`, inbox message archived). Merge rule SATISFIED: threads 0 of 4 (the declined StatusCommand thread was resolved by the operator's account with the PLAN-34 pointer), Sonar gate green 0 issues, CI green. The product diff is small (RowMenu clear of the bottom bar with a 390 px e2e, one shared `tokenize` rule, the age rule with rows for months and years); 28 of 45 files are test-only flake fixes at the operator's word (309 `nonEmptyStrings`→`nonBlankStrings` swaps in pm-api, `ActionSupportTest` Ctrl+C repeat, `EntryFlowStaticsTest` worker latch, which also fixes PLAN-104's unresolved thread race). Gaps: Playwright is not run in CI (`skipPlaywrightTests=true`), so the 390 px proof is session-local; the capped-menu scroll behaviour, the 30-59 day age row and the dialog z-index wording (50 vs 100) are untested or wrong; `reports.spec.js` (PLAN-105 defect 13) still fails; the terminal code (`ProjectsCommand`, `StatusCommand`, `WebCommand`) contradicts the new age rule until PLAN-34 deliverable 4. Orchestrator choices, open to the operator: (1) the residues are folded into PLAN-116 (thread reply, Ctrl+C note), PLAN-107 (scroll, wait, 30-59 day row, defect 13) and PLAN-112 (z-index wording, `disabledReason`); (2) the free slot goes to **D1 = PLAN-112** as a direct Claude Code session in worktree `112-design-d1-component-vocabulary` (it edits `terminal-client/19-actions.adoc` last, after PLAN-97 has landed); (3) the Ctrl+C window between a drawn question and `readLine` is a recorded hypothesis, not a staged bug. PLAN-111's session reports PR #363 open and green, all Done-when met except the browser check (Claude in Chrome was not connected): the operator decides whether to run the check or waive it before the merge word.
- 2026-11-07 — PLAN-110 AND PLAN-111 LANDED AND ANALYSED (PR #364 `1d8c6197`, runbook lane; PR #363 `bb30d71e`, direct session; `landings/PLAN-110.md`, `landings/PLAN-111.md`; rows `shipped`). PLAN-110: merge rule satisfied on thin evidence (no CodeRabbit review object, one clean check run; 0 threads; Sonar gate green); the spec contradicted itself (DW1 grep of the stale key vs DW2 test that must write it) and the lane split the key string. SPEC-AUTHORING RULE: a grep Done-when and a test that has to write the same literal are reconciled in the spec (exclude the test file or name the allowed literal). PLAN-111: merge rule satisfied (1 thread, fixed test-first, resolved; Sonar 0); the BROWSER CHECK was waived by the operator (Claude in Chrome was never connected; unverified: scroll position, left border); design text of `needs-and-find.adoc:57` and the layout-modes sections is false until PLAN-113 (amended, with PLAN-51 and PLAN-107). My staging error: the ports of PLAN-109/110/111 were one block below the `AGENTS.md` formula (found by the PLAN-111 session; PLAN-109 corrected, the others ran on free ports). Process finding (not this epic): `sonar_rest search` lists fixed issues (no `resolved=false`), `read_open_issues` filters correctly.
- 2026-11-07 — PLAN-97 LANDED AND ANALYSED (PR #358 `d8efe35f`, merged 14:38Z through the merge queue; `landings/PLAN-97.md`; row `shipped`, seven inbox messages archived). Merge rule MET ON THE LETTER: one CodeRabbit review, threads 0 of 24, Sonar gate green 0 new issues (86.8% coverage on new code); but the review's BODY holds twelve items beyond the threads that no reply answers (the same class of miss as PLAN-104), the first PR #356 was closed unreviewed at the operator's request, three reviews (functional, code, simplify) were closed by operator override with 13 advisory findings open in the plan's decision log, and the landing facts lack the three required lines. HIGH defect: `drain` can signal an unrelated process (`RuntimeRecord.processAlive` returns true without a start instant; `DrainCommand` then SIGTERMs a reused pid). Also: the machine-derived drain bound and the `help` verb list are untested (one verb list in three places), D4(f)/D4(i) not done, `project add` is only the old `enrol`, `project release`/`unenrol` have no interactive prompt, the design's Q1/Q3 decisions sit only in the PR body. Orchestrator choices, open to the operator: (1) the residue is staged as **PLAN-118 cli-command-tree-residue** (WS-04, runbook lane, ports 21180/21181) with the drain identity first; PLAN-116 keeps the formatting residue; (2) the three free slots go to PLAN-109 (rail Settings label; its one shared file with D1, `work-actions-texts.adoc`, does not block), PLAN-116 and PLAN-118 (they share `PmConsole.java` only); PLAN-107 waits for D1 (RowMenu `disabledReason` is D1's decision and PLAN-107 shares three files with PLAN-109), PLAN-117 waits for PLAN-109. Rule candidate: a landing whose review BODY items are unanswered is not 'handled'; the next specs state it in the Standing Constraints (already done for 116, 117, 118).
- 2026-11-07 — PLAN-112 (DESIGN D1) MERGED AND RECORDED (PR #365 `aa3a9be3`, 16:13Z, on the operator's word; `landings/PLAN-112.md`; row `shipped`, message archived). 29 threads, 0 unresolved; the session's first resolve pass closed 12 new CodeRabbit comments before reading them, it reopened and fixed or answered them in `836cf806` before the merge. THE GATE IS OPEN: the documents merged at `Status: DRAFT`; the specs of D2, D3, D4 and the waiting web plans gate on the merge WITH the documents at `AGREED`. D2 (PLAN-113) and D3 (PLAN-114) are not emitted until the operator sets `AGREED` (a small status-only pull request, which the D1 session offered) or records a waiver. Hand-offs: the terminal package (console badge words `question`/`waiver request` → `decision`/`waiver`, `PmConsole.java:741`, `OverviewFormatting.java:197-199`, `ConsolePrompt.java`) and the catalogue clean-up package (deferred reload may fetch a missing locale from a newer runtime) are to be staged from the AGREED documents.

## Landings of PLAN-130 and PLAN-132 (2026-11-11)

- 2026-11-11 — PLAN-130 landed (#414, `078b2ec5`; `landings/PLAN-130.md`): no blocker or major. Operator (AskUserQuestion):
  the BrandMark size type, the `favicon.ts` comment, the S12 screenshot spec and the waiting mark's start-frame dot → PLAN-131
  deliverable 6; the phone onboarding header without the icon → PLAN-64 (Done-when added); PLAN-147's S12 clause settled from
  the PR body; the eight candidate lessons adopted and archived (spec-authoring rules in Queue annotations; Open Defects for the
  plan-marshall-side three; the pm-web-app inventory mapping a direct-PR candidate). PLAN-159 took the freed slot.

- 2026-11-11 — PLAN-132 landed (#409, `572dc739`; `landings/PLAN-132.md`): one MAJOR (the wire form of the chat's message
  keys: the design says `chat.*`, the pm-api goldens send `control.chat.*`, 11 of them in no catalogue). Operator
  (AskUserQuestion): PLAN-133 amended before it initialised — deliverable 0 settles the key form first, deliverable 6 the pm-api
  and mock minors; `DecisionView`'s absent channel/device → PLAN-136; lessons adopted (rule 5) and archived. PLAN-133 was
  emitted on the operator's word as a fifth plan beside N=4.

## Closed Open Defects relocated on 2026-11-23 (decision 10)

Moved verbatim out of `epic.md` on 2026-11-23 (the operator, decision 10, proposal 1): each entry's last marker is final (CLOSED, FIXED, SHIPPED, SETTLED, REPLACED or SUPERSEDED) and names no open residue. A pointer stays at the origin in `epic.md`.

- **PLAN-142 landing incomplete and one false claim (2026-11-15), owner plan-marshall (runbook lane).** `inbox landing-check` on
  `rb-142-web-column-engine-001.md`: `complete: false`, `missing_keys` `total_tokens` and `plugin_version` (both `unknown`; the opencode
  harness measured no tokens and no plugin version). The landing still reconciled. The lane reported `review_body_findings_handled=18`; its own
  progress file holds 17 `finding:` lines (`inline-14` has none; all threads are resolved on the provider), so the count is recorded as a
  false claim about the disposition record. Sonar's `0` is not re-verifiable after the merge (`undecidable`, `head_revision_unavailable`).
  → CLOSED 2026-11-22 (handle-open, orchestrator, confidence 90 > threshold 80; see Decisions): PLAN-142 is `shipped` (`queue/PLAN-142.json` re-read); a landed record cannot be re-measured, the recurrence of `unknown` token and plugin keys is tracked in the Open Defect on the runbook-lane landings; Sonar's open count of a merged PR is not re-measurable.

- PLAN-126 (2026-11-12), owner plan-marshall (runbook lane), **recurring in three landings in a row** (PLAN-159, PLAN-32, PLAN-126): the
  lane reports every review-body item handled while body-only items (outside-diff, nitpick, "additional comments") have no reply and
  some are not fixed; the PR body's counts are written once and go stale. The lane must reply on the PR to each body item and re-read the
  counts at the merge head. Every staged spec's merge rule now asks for a reply to each body item (PLAN-160, the five amended specs);
  the landing analysis re-counts them every time.
  → CLOSED 2026-11-22 (handle-open, orchestrator, confidence 85 > threshold 80; see Decisions): the runbook lane's `review-gate` verb decides the merge from one provider fetch: plan-marshall `5eb2f9d9d` (#1940) and `2bf7cd83d`, both on that repository's `origin/main` (ancestry re-read); the stale PR-body count sub-item is low value.

- A lane merges with NO bot review (2026-11-10, PLAN-36 #391): CodeRabbit refused every trigger on quota and the
  runbook lane merged under its "standing review authorization", reporting "all review surfaces clear" — true only
  because nothing was reviewed. The epic's merge rule needs one CodeRabbit review of some head. Owner: plan-marshall
  (runbook lane: standing authorization must require at least one completed review, else stop for the operator).
  Recurrence check at every landing.
   → CLOSED 2026-11-18 (handle-open, orchestrator, confidence 93 > threshold 90): the lane's standing authorization now requires a qualifying review: `runbook-lane/SKILL.md` § merge gate ("never to a PR without a qualifying review", re-read at 890-897 and 930-934 of the installed 0.1.2149 copy; fix commit `280e6c404` "one handled CodeRabbit review is the review"). The recurrence check at each landing stays the orchestrator's practice, not an open item.

- 2026-11-06 — DECIDED by the operator (option A): the unfiltered "Mark all shown read" of the reports page marks the reports the list shows by their ids, never everything, as `doc/design/shared/reports.adoc` 182/184 and the label say; PLAN-102 D1c (`{ all: true }`, merged in PR #340) is amended by PLAN-105 deliverable 1 (`landings/PLAN-102.md` defect 1). PLAN-105 is emittable.
  → CLOSED 2026-11-15 (handle-open, decided by the orchestrator at confidence 95 against threshold 90; see Decisions): carried by `0913d6f3` (#394, on main); re-read `ReportsView.svelte:642-683` (`markAllShownRead` marks `matchingItems` ids only) and `client.ts:3702-3704` (`markAllReportsRead(ids)` posts `report_ids`).

- 2026-11-05 — `ActionSupportTest.leaveBackPreservesNonInterruptExitCodes` (PLAN-103 N5) duplicates `theLeaveBackMapping`; the chat sites' non-interrupt exit codes are not covered. Not routed.
  → CLOSED 2026-11-23 (the operator, decision 9 proposal 1; see Decisions): one duplicated test (`ActionSupportTest`, PLAN-103 N5), minor, never routed: accepted as residue.

- Lane landing reports state false review counts (2026-11-08, PLAN-34 and PLAN-21 in a row): PLAN-34 reported "All threads
  resolved" with 2 unresolved and unanswered; PLAN-21 reported "zero unresolved review threads, self-review clear" with all 20
  CodeRabbit threads unresolved and unanswered (none addressed). Both merged. The counts must be read from `ci pr comments`
  at the merge question, never written from memory; until the runbook-lane change (review-gate) ships, the orchestrator keeps
  re-reading threads and bodies at every landing, and new specs state the merge rule hard (PLAN-124). Owner: plan-marshall
  (runbook lane). Recurrence 2026-11-09, PLAN-120 (opencode): reported "10 findings … all 10 threads resolved"; re-read: 12
  threads, 2 unresolved and unanswered from the final head's review (`landings/PLAN-120.md`). The runbook lane has no
  `review-gate` verb yet (`runbook_lane` offers resolve, executor-skew, build-routing, build-classify, sonar-gate, sonar-facts).
   → CLOSED 2026-11-18 (handle-open, orchestrator, confidence 94 > threshold 90): the premise "no `review-gate` verb yet" is gone: `runbook-lane/SKILL.md:173` documents the `review-gate` verb and `runbook_lane.py:2859-2863` decides `unresolved_review_threads`, `body_findings_undisposed` and `thread_concern_remains` from it (re-read; fix commit `2bf7cd83d`). The recurrence list is history.

- **PLAN-145 landing record is incomplete and two claims are not re-verified (2026-11-15).** `total_tokens` and `plugin_version` are `unknown`; the
  review gate answers `review_body_findings_handled: 0` / `merge_allowed: false` / `body_findings_undisposed` for #443 although the lane's progress file
  holds ten dispositions and every thread is resolved (which store the gate reads is not settled); Sonar `read_open_issues` is `undecidable`
  (`head_revision_unavailable`), so `sonar_open_new_issues=0` stands as the lane's word.
  → CLOSED 2026-11-22 (handle-open, orchestrator, confidence 88 > threshold 80; see Decisions): plan-marshall `runbook_lane.py:5223` takes `--disposition` as argv and stores nothing (`origin/main`, re-read), which is the already recorded reason the bare `review-gate` answers `handled: 0`; the lane's progress file carries the dispositions.

- **The review gate disagrees with the lane on #443 and #444 alike (2026-11-15).** `runbook_lane review-gate` answers `review_body_findings_handled: 0`, `merge_allowed: false`,
  `body_findings_undisposed` for both, with every thread resolved and ten `finding:` lines in each progress file (the #442 reading recorded only the total, 18, so it is no sample). Which store the gate reads is
  unsettled; if it reads a findings store the runbook lane never writes, the gate cannot pass for this lane and the merge went through on the lane's own
  judgement. Sonar is `undecidable` on both. Owner: the operator, as a question for the lane's maintainers. **Update 2026-11-15:** #445 reads the same (`handled: 0` of 24, `merge_allowed: false`), from a
  different harness (`antigravity`, `gemini-2.5-pro`), so the cause is the gate or the lane's findings store, not a harness; three samples (#443, #444, #445).
  → CLOSED 2026-11-15 (handle-open, decided by the orchestrator at confidence 92 against threshold 90; see Decisions): the question "which store does the gate read" is answered, there is none. `runbook_lane.py:2604,2838,3190` and `SKILL.md:177-195,846`: the gate reads the findings from the PR's review bodies and takes the dispositions as `--disposition` argv values, nothing is stored. Re-run read-only today with each progress file's `finding:` lines passed as `--disposition`: **#444** `all_handled`, 10 of 10, `merge_allowed: true`; **#445** 24 of 24 handled, `merge_allowed: false`, reason `thread_concern_remains` on `PRRT_kwDOVBOtss63qHpK` (the thread behind `progress.md:61`); **#443** 0 of 10 handled, `body_findings_undisposed`, because PLAN-145's progress lines name the ids `1` to `10` where the gate's ids are `inline-1` to `inline-10` (#444 and #445 use the gate's form). So the earlier `handled: 0` readings of #444 and #445 came from a call made without `--disposition`, and the one real mismatch is a lane-side id form on #443. The #443 merge is shipped history; the #445 override is carried by the PLAN-149 defects entry. Not decided here: whether the runbook lane should accept a bare number or its progress template should state the id form (a question for the lane's maintainers, not a defect of this epic).

- **PLAN-172 landed (2026-11-20, #518 `8e088121`), analyzed; owner: the operator (the token decision; nothing staged).** `landings/PLAN-172.md`, one read-only leaf (message `rb-172-web-interaction-set-residue-001`, filed one minute after the merge), the points marked "re-read" re-read at `8e088121` and in `node_modules/svelte`. The interaction set's residue (25 files, +460/-26): **the lane's `deliverables_done=5` is overstated; by the spec's rule at most 3.** D2 and D3 met, D4 met with a larger scope than reported, D5 mechanical, **D1 not met as written: the spec's premise is refuted** (Svelte 5.57.2's `target_handler` already skipped a forwarded `on:click` after `stopPropagation`, `events.js:60-70`, re-read), so the `on:click` tests pass on the old code and the new `stopImmediatePropagation` is a no-op for Svelte consumers; only two raw-`addEventListener` tests fail at the base; the claimed revert run is contradicted; **nobody reported the refutation**. **The operator's decision on the `StatusLabel` token (name and value) is not evidenced**: the lane added `--state-slot-width: 8.5rem` in the control-token block (`tokens.css:63`) without a word. **Unreported:** every `Dialog` now renders under `document.body` through the new `utils/portal.ts` (a `Dialog` in a `data-theme` subtree loses the subtree's theme; a nested dialog may trap Tab; a `queueMicrotask` refocus overrides a child's autofocus), the `Toggle` spread-order change, untested `ActMenuItem.confirmTitle`/`confirmConsequences`, the coverage margin (0.14 pt again; `Dialog`, `Toggle`, `ActMenu`, `RowMenu` branches down), the focus tests that pass without the fix, false statements in the message (`ConfirmDialog.test.ts` is not in the diff, no `keydown` handler exists, six screenshots for eight combinations and none described). Sonar through the repository's provider: gate `OK`, new coverage 91.7. **The row is `shipped`; PLAN-173 is unblocked** (its spec re-read: the `tokens.css` lines after `:63` moved by +1, the `Toggle` API as read, the refuted D1 premise means the fields inherit nothing). Proposed, the operator's word only: confirm or rename the token, and fold the test and documentation fixes into an existing plan. The entry stays open until the operator decides.
  → DECIDED 2026-11-20: **(1) the token is confirmed by the operator: `--state-slot-width: 8.5rem`** (name and value; its placement in the control-token block is for the README line of deliverable 6 to settle); **(2) "yes", fold into an existing plan:** PLAN-172's test, portal and documentation findings are **deliverable 6 of the staged PLAN-156** (`web-views-modals-and-pickers`, which builds six modals on the `Dialog` PLAN-172 changed; done first): the on:click tests named what they are, the focus tests that fail without the fix, the portal and the themed subtree, the nested-dialog Tab, the autofocus, the untested `confirmTitle` API, the stale design lines, a `-Pcoverage` run at the head. PLAN-173 and PLAN-174 are RUNNING (the operator's word). The entry closes when PLAN-156 is shipped.
  → DECIDED 2026-11-22 (handle-open, orchestrator, confidence 88 > threshold 80; see Decisions): the entry STAYS OPEN, no operator decision needed; owner: PLAN-156 (running, deliverable 6; #536 open); the token decision is on `main` (`tokens.css:63`).
  → CLOSED 2026-11-23 (analyze, orchestrator, confidence 88; see Decisions): PLAN-156 shipped (#536, `b4663e27`) and carried deliverable 6: the renamed guards, the RowMenu and portal and themed-section and nested-Tab and autofocus tests, `confirmTitle` removed (`git grep` empty, leaf), the token on `main`. Residue named in `landings/PLAN-156.md`: the confirm texts are asserted by no test, the Escape yield is not pinned to its focus-keying, and the vocabulary lint cannot see `components/`.

- **PLAN-170 landed (2026-11-19, #507 `a3919eb1`), analyzed; owner: the operator (nothing staged).** `landings/PLAN-170.md`, one read-only leaf (message `rb-170-vocabulary-lint-hardening-001`), the points marked "re-read" re-read at `a3919eb1`. The vocabulary lint is hardened (3 files, +519/-126; no baseline file, `package.json` or ESLint config changed): **no blocker, no major.** D4 met; D1, D2, D3, D5 met-modified. An unparseable `.svelte` or CSS source is a finding of its own rule (`unparseable`) that exits 1 under `--error` and cannot hide behind its baseline entry; the CSS regex fallback is gone; `--write-baseline` refuses a raised count or a new file and writes nothing; the pending list is validated (`MascotFigure` and `TriagePage` leave it); the entry guard compares real paths and a real-symlink test with a matched control spawns `node`. **The lane's evidence overstates:** D2.2 is labelled `test-first` but passes at `ac538733` (re-read: the base `--write-baseline` writes exactly what the test expects); D3.1, D3.2 and D1.3 "failed at base" only because their helper is born in the test file; the tests of 1.1 (parser message and line), 2.1 (file and counts), 2.3 (error line) and 5.2 ("from the rule list") do not assert what is claimed. **Unreported:** the printed format changed (a new always-present `unparseable` column, an unparseable line without "use", the parser message in `component`) although the spec makes a format change a finding of the landing message; the flag was kept and can no longer create a baseline (the spec asked which option); the count `60` is hard-coded and the table does not state it; a `lib/dev/` fixture source was added. **CodeRabbit's inline-2 decline is wrong on substance:** the fixture check's `catch` falls back to a regex that is inverted for normal tags, swallows the parse error and accepts `<!-- <Button < -->` plus an unclosed `<div>` (re-read). Minor: `--write-baseline` reorders eight files of an unchanged baseline and is not atomic, a rename is refused as new, the entry guard exits at import time on an unresolvable path (untested), the `ruleNames` parameter of `formatTable` is dead. Review gate re-read: 0 threads, 3 body findings (same), `handled` unreadable; Sonar: no issues. **The row is `shipped`.** Proposed, the operator's word only: a small test-only follow-up (assert the claimed properties, replace the fallback regex by an error, state the creation decision, pin the rule order) as a D0 of the next web plan that touches the lint, or wait. For plan-marshall: a "test-first" row whose test passes at the base (five landings now); "failing at base" meaning "the helper does not exist"; a printed-format change left out of the landing message. The entry stays open until the operator decides the follow-up.
  → DECIDED 2026-11-20 (the same word as the PLAN-165 entry): PLAN-170's evidence gaps (the tests of 1.1, 2.1, 2.3 and 5.2 assert what they claim; the fixture check's fallback regex becomes a reported error; `--write-baseline` leaves an unchanged baseline byte-identical; the printed-format change is written in the script's header) are **deliverable 5 of the staged PLAN-153**, clauses (a) to (c). The entry closes when PLAN-153 is shipped.
  → CLOSED 2026-11-22 (handle-open, orchestrator, confidence 92 > threshold 90; see Decisions): PLAN-153 is `shipped` (#532 `9abb2958`, on `origin/main`) and its deliverable 5 carries clauses (a) to (c), re-read at `origin/main`: (a) `vocabularyLint.test.ts:263-287` asserts the parser's line and message (`line` equals `parserError.start.line`, `> 1`), `:419-440` asserts the `scripts directory does not exist` line, `:320`, `:357` and `:442` name the file and both counts, the new-file refusal and the byte-identical unchanged baseline; (b) `vocabulary.test.ts:62-69` throws `The fixture source does not parse: {message}` (no regex fallback); (c) `vocabulary-lint.mjs:11-24` states the printed format and the always-present `unparseable` column. The lane's mutation runs are its own account, not re-run here.

- **PLAN-165 landed (2026-11-19, #503 `a8ac159d`), analyzed; owner: the operator (nothing staged; one scope addition to PLAN-171).** `landings/PLAN-165.md`, one read-only leaf (message `rb-165-derive-lists-from-one-definition-001`), the points marked "re-read" re-read at `a8ac159d`. The three hand-kept lists derive from one definition each and the web module's unit coverage reaches Sonar (20 files, +1072/-81): **no blocker, no major, ten minor.** D1 (dialogue keys: `CUT_KEYS`/`PLAN_KEYS`/`READ_BACK_KEYS` and `offeredLine`, transcripts byte-identical), D2 (`CleanupKind` through an exhaustive switch) met; D3 met-modified by the operator-approved drop of `project.released` and `project.enrolled` (re-read: `EventName` holds only three project events; the mock broadcasts the two names paired with `project.changed`, so no view loses a refresh); D4 met by logs (456 e2e passed, not decidable by reading); D5 and D6 met by construction and logs, the Sonar clauses are the lane's account (no issue in the Sonar search for #503). **Weaknesses:** the four consumer tests iterate the exported sets, so a member removed from a set passes every test; the coverage floor has a 0.14 pt margin (80.14 vs 80) and the Sonar job now fails the build under it; the 99.0 % new coverage sees `.ts`/`.js` only, no `.svelte`; `sonar.test.inclusions` was extended without a word in the message; `EpicDecompositionDialogueKeysTest` has no `@EnableGeneratorController` and hand-typed input; `OverviewView.svelte:331-340` keeps its own literal list; `pm-api` still ships `ProjectReleasedEvent` and `ProjectEnrolledEvent` for names the closed catalogue lacks; an unreported e2e environment failure at 07:39; commit text of 23505a08 is false. **The row is `shipped`; PLAN-165's overlaps are released:** PLAN-169's 13 shared files are NOT touched by #503 (the overlap was refuted), PLAN-168's `CleanupCommand` lines moved by +5, PLAN-171 takes `EpicDecompositionDialogueKeysTest` as an eighth class. For plan-marshall: `inbox` address derivation from a plan id that starts with a digit (now three plans); a lane that reads logs with shell tools. The entry stays open until the operator decides whether the weaker tests (set contents not pinned) need a plan.
  → DECIDED 2026-11-20 (the operator: "yes, add it to another existing plan"): the content pins of the three exported refresh sets are part of **deliverable 5 of the staged PLAN-153** (`web-views-system-and-cleanup`, done first, clause (d)), with PLAN-170's evidence gaps. The coverage margin (0.14 pt), the `.svelte` blind spot of the Sonar figure and `OverviewView`'s own literal list stay named, NOT staged. The entry closes when PLAN-153 is shipped.
  → CLOSED 2026-11-22 (handle-open, orchestrator, confidence 92 > threshold 90; see Decisions): PLAN-153 is `shipped` (#532) and its clause (d) landed: `pm-web-app/src/lib/stores/refreshEvents.test.ts:95-120` pins the content of the project, entry and plan-process refresh sets as literal inventories (`toEqual`, re-read at `origin/main`). **The three residues this entry named and did not stage stay named here, unowned:** the 0.14 pt coverage margin, the `.svelte` blind spot of the Sonar figure, and `OverviewView.svelte`'s own literal event list.

- **PLAN-167 stopped before the merge (2026-11-18, message `rb-167-specs-move-into-clients-001`, PR B #480 open and CLEAN, 50 files); NOT a landing; owner: the operator (restart) and plan-marshall (the lane's rule breaks).** D1 (the cut table, PR A #473 `3f912c18`, 205 rows for 205 headings, confirmed by the operator) and D2 (the ADR `doc/adr/001-specifications-of-the-clients.adoc`; the design links to the specification) are met; D3 to D6 are not met: three broken links the lane introduced (`doc/implementation-watch/cli-and-security.adoc:23` and siblings with one hop too few, `doc/specification/harness-integration.adoc:26` with one too many, `runtime-model/03-relay.adoc:65` linking a part that is not local), no recorded content check, two Traceability anchors with the wrong generated id, ten planning documents under `doc/plans/` still saying the client specifications live in `plan-marshall-documentation`, and D6 half done on a local WIP branch `chore/167-specification-move` (commit `76a1b4e`, unpushed, in `../plan-marshall-documentation`): the remaining about 37 link rewrites, the three skills, `CLAUDE.md`, `project-setup.adoc`, the `trace.py` extension with `test_trace.py` (test-first) and the final trace run; the baseline is taken (`BROKEN LINKS 0`, 120 requirements, watch counts 18/12/4). PR B has 18 unresolved inline threads and 2 body findings; the lane says three are its own broken links, two ADR wording, one the Traceability anchor, one the drifted part inventory, the rest pre-existing content moved verbatim or code changes the plan forbids (`OperatorGate`, symlink-safe containment, the enforcement-weakening gate in `ClientCommand`: follow-ups for their own plans). **The lane's rule breaks, by its own count:** shell `grep | sort | uniq`, `cp`, `mv`, `mkdir`, two inline-Python deletes in the sibling repository, the step-5 self-review never dispatched, D6 left incomplete; `trace.py` crashed on `roadmap.adoc` as an interim check. Sonar: documentation-only heads, the read is `undecidable`; the operator chose "record as unknown" for PR A, PR B awaits the same decision. The main checkout is clean and the sibling repository's branch holds one WIP commit (re-read). The message stays UNCONSUMED until the lane amends it into a real landing; the row stays `running`; a resume paste is given to the operator.
  → UPDATED 2026-11-18 (the resumed run's message `rb-167-specs-move-into-clients-002`, `deliverables_done=5` of 6, `merge_state=open`, analyzed by one read-only leaf with the key points re-read; the first stopped-run message `-001` is archived as superseded): **D1 to D5 met; PR B (#480, head `9994adfc`) is a faithful mechanical move.** Verified: every moved document differs from its source (`79ec3a6`) only in link targets, apart from the removal of the 11-line `#bundle-content` block (source lines 35-45 of `02-bundle-manifest.adoc`, with `[[shim-skill]]` and the PM-SKILL-4 line), the forced framing of `03-web-app.adoc` and a plain path turned into a link in two watch items (`RUN-10`, `RUN-11`); 88 of 89 cut-table anchors resolve (row 115 is a typo in the cut table: the generated id is `…_installupdateremove`); watch ids 9, 3 and 4 exist once and the 18 that stayed are named once; 3654 links scanned, one pre-existing break (`04-console.adoc:27`); 18 of 18 threads resolved (read through GraphQL, unresolved 0), CI without a failing check, Sourcery approved; the lane's "25 files" is 35. The merge gate says no for three readings outside the content: `review-gate` `undecidable_read` (the `Minor comments` title, the plan-marshall label gap), one resolved thread where CodeRabbit "does not mark the security finding as addressed" (the specification at `01-cli-and-installer.adoc:326-328` requires `pm-operator` to refuse `--yes` for an enforcement-weakening change; the code has no change plan, no `--yes` and no `enforcement` handling in `ClientCommand`: a pre-existing spec-ahead-of-code gap that moved verbatim), and Sonar `undecidable` on a documentation-only head. **Operator decisions of 2026-11-18 (`AskUserQuestion`): authorise the enqueue of #480 (the three readings accepted; the resumed run enqueues it first); PLAN-167 gets a D7 (a third pull request in `pm-mcp-core` retargeting its 106 links in 46 files into the moved documents, link lines only, Write-Boundary amended for that only).** **A real defect in PR C (the lane did not report it, the leaf found it, re-read):** the branch `chore/167-specification-move` (3 commits, head `6c2fae3`, pushed) DELETES `doc/specification/harness-integration/02-bundle-manifest.adoc` outright (re-read: status `D`), while `#bundle-content` has no home after the move (the cut table sends it to `plan-marshall-mcp`, row 112; PR B's file has no mention of it, 0 hits; the ADR row and `Traceability.adoc:212` say it stays), so the section would exist in neither repository; and the branch newly breaks 54 links (10 are wrong retargets in `module-structure.adoc` to a document that does not move). The spec carries a Second Resume Amendment (the order: enqueue B, D7 in `pm-mcp-core`, correct D6 keeping a trimmed `02-bundle-manifest.adoc`, the `trace.py` run after B has merged, merge C, one small follow-up in this repository for the ADR's stale cut-table reference and the two design READMEs). Follow-ups named, nothing staged: the enforcement-weakening gate in `ClientCommand` and the rest of the lane's list. The row stays `running`; the message `-002` stays UNCONSUMED until the lane amends it into a real landing.
  → RE-READ 2026-11-18 late evening (the resume of this session; the message `-002` was NOT re-analyzed, a second leaf was stopped as a duplicate of the reading above): **#480 is MERGED** (`5353b0fd`), so the message's `merge_state=open` is stale; the follow-up PR **#488 (`chore/167-spec-followups`) is CLOSED, NOT merged** (closed 2026-11-18T20:09Z, head `4b7f2df9`; its CodeRabbit and Sourcery comments are rate-limit notices; the reason for the close is not verified; the branch is still on the remote); D7 (the `pm-mcp-core` link PR) and PR C were not re-read. Open question for the operator: reopen #488 or carry its three lines in a new pull request. The row stays `running`; `-002` stays the one unconsumed inbox message.
  → LANDED 2026-11-19 (the closing message `rb-167-specs-move-into-clients-003`, `merge_state=merged`, `deliverables_done=6` of 7; `-002` superseded by it; **both messages archived; the row is `shipped`**, `landings/PLAN-167.md`): seven pull requests in three repositories, #473 `3f912c18` (the cut), #480 `5353b0fd` (B), #192 `72ebf20c` (C, the documentation repository), pm-mcp-core #501 `4680161c` (D7), then #490 `ecd39923` (replaces the closed #488), #497 `fe0f9805`, #500 `5b4c5ae7`. One read-only leaf read it on three layers; the orchestrator re-read the three majors, the merge order and the review and Sonar numbers. **No blocker, three major (reported to the operator, none staged):** (1) `pm-mcp-core` `doc/specification/harness-profiles.adoc:13` links the family index `harness-integration.adoc`, which C deleted — a file #501 did not touch although the spec names it, so **D7 clause 1 is false** (clause 3, the order B < D7 < C, was B 15:13Z, C 15:55Z, D7 16:06Z and is not met, as the lane says); (2) `doc/Traceability.adoc` lines 140, 208, 271, 440 name code that does not exist (PM-WF-19 `decision-log export`, PM-SKILL-4 the bundle installer's shim skill, PM-CRED-2 `credentials migrate`, PM-TECH-4 Seatbelt), against its own rule "a line is never invented"; (3) the documentation repository's `doc/use-cases/01-onboarding.adoc:298` links the trimmed `02-bundle-manifest.adoc`, which no longer holds the schema it names. **The mechanical claim holds** (per-line comparison against `79ec3a6`: link targets only, apart from the licensed `#bundle-content` removal and the `03-web-app` framing; watch ids 9/3/4 moved and 18 stayed, none lost); **the lane's grades are generous at three points:** D5 "link lines only" is false for the plans (14 of 15 carry a rewritten sentence, three not licensed: `cli-unit-validate.adoc`, `diagrams.adoc`, `start-lesson-untrusted.adoc`), D4 "met" misses the four entries, D7 clause 1. Test-first for `test_trace.py` is not evidenced (test and extension are one commit). Review gate re-read: #480 `undecidable_read` (operator-authorised), #490 `all_handled`, #497 and #500 `body_findings_undisposed` with an author reply on every finding (the gate's counting gap); Sonar: no issues, no analysis of a documentation-only head (the lane's `unknown` stays). Minor, in the landing record: a stub `[#web-app]` in the documentation repository, `epic-clients.adoc:28`, the ADR's "keeps their paths" for the web app, "Implement in", two new link texts in `03-web-app.adoc`. The follow-ups of the lane (the daemon half's retargeting, the code follow-ups, `04-console.adoc:27`, the two `Source::` citations) stay named, not staged. Owner: the operator for the three majors (each is one link or one parenthetical; a direct documentation session in a worktree with `skip-bot-review`, not a lane plan, by the rule of 2026-11-13). The entry closes with that decision.
  → DECIDED 2026-11-19 (handle-open, threshold 90, operator chose proposal 1 of 4 at the orchestrator's confidence 75; see Decisions): the three majors are fixed as THREE SMALL DIRECT DOCUMENTATION CHANGES, one per repository, each in its own worktree with a pull request labelled `skip-bot-review`, no lane plan. Follow-up named, NOTHING staged, NOTHING started by the orchestrator: (a) `pm-mcp-core` `doc/specification/harness-profiles.adoc:13` retarget the `harness-integration.adoc` link to the clients URL `https://github.com/plan-marshall/pm-mcp-clients/blob/main/doc/specification/harness-integration.adoc` (anchors kept; check the same file for other links to deleted parts); (b) `pm-mcp-clients` `doc/Traceability.adoc` lines 140, 208, 271, 440: PM-WF-19 and PM-SKILL-4 `none in this repository`, PM-CRED-2 drop `and credentials migrate`, PM-TECH-4 drop `and Seatbelt` (Landlock stays); (c) `plan-marshall-documentation` `doc/use-cases/01-onboarding.adoc:298` retarget the `02-bundle-manifest.adoc` link to the clients URL of the moved part. The entry STAYS OPEN until the three pull requests are merged.
  → REPLACED 2026-11-20 (the operator's word "add it to an existing plan"): the three documentation fixes are **deliverable 5 of the staged PLAN-42** (`cli-verb-modes-and-help`, done first): (a) `doc/Traceability.adoc` lines 140, 208, 271, 440 here, (b) `../pm-mcp-core` `doc/specification/harness-profiles.adoc:13`, (c) `../plan-marshall-documentation` `doc/use-cases/01-onboarding.adoc:298`, each re-read as still in place on 2026-11-20; PLAN-42's Write-Boundary is amended for the two sibling repositories (link lines only, their own pull requests). The three-direct-session decision is superseded. The entry closes when PLAN-42 is shipped.

- **PLAN-30 stopped without a landing and sat as `running` for four days (2026-11-14 to 2026-11-18); parked on the operator's word of 2026-11-18; owner: the operator (resume or re-scope), the orchestrator for the stale row.** Read 2026-11-18 from the provider, git, the lane's progress file and a read-only merge simulation. **What the lane did:** two sessions on 2026-11-14 (lane text loaded 10:35 and 15:56 per `.plan/temp/rb-30-cli-texts-and-errors/progress.md`) delivered D1 to D9 test-first (the progress file names a failing run for each), one self-review round (three findings fixed, "open: none"), opened PR #438 at `8c6d3fd6` (17:52Z), triggered CodeRabbit and Sourcery (17:53, "pending"), fixed the Sonar issues (`4e7c45d6`), added unit tests for `OperatorRefusedException` and `RefusalRenderer` (`4ee1b752`) and merged `main` (`2c681d9d`, 19:55Z). The progress file ends at "review: … pending" (17:53); the last provider activity is the failed CI of `2c681d9d` at 20:14Z. **No landing message, no stop message and no inbox message ever reached the ledger:** the session simply ended. **State of #438 now:** 84 files, +3990/-551, `DIRTY` (a read-only `git merge-tree` against `origin/main` finds three content conflicts: `MessageCatalogueTest.java`, `MockRuntimeEngine.java`, `PlanCommand.java`; 13 of its files also changed on main since the merge base, the rest auto-merge), 45 commits behind and 15 ahead of main; **no review at all** (CodeRabbit and Sourcery never reviewed: 0 reviews, 0 threads); CI red at `2c681d9d` for ONE reason, SonarCloud's gate: `new_coverage` 78.3% against 80%, every other condition OK (0 open issues, duplication 0.2%; the Sonar MCP search returns no issues; the quality-gate read via the MCP). **Why the row stayed `running`:** on 2026-11-15 the operator decided to treat PLAN-30 as parked (to emit PLAN-146, which shares `MockRuntimeEngine.java` and `fully-onboarded.json` with it) and the orchestrator carried it as "row running but parked" in the anchor; no verb transitions a row without the operator's word, and on 2026-11-18 the operator's answer to the handle-open cleanup question was "leave it running". The cost: a `running` row fills an N=4 slot and sits in the disjointness gate's population, so the plans that overlap its 71 declared paths were held on a lane that was not running (today's overlaps: PLAN-42 13 files, PLAN-44 11, PLAN-48 8, PLAN-138 7, PLAN-140 5, PLAN-139 4, PLAN-165 2; PLAN-50, 63 and 79 share none). **Recorded 2026-11-18:** the row is `parked` (the operator: "PLAN-30 isn't running; only #167 and #66 are running"). **Decision waiting (nothing staged):** resume the lane on #438 (a resumed run merges main, resolves the three conflicts, lifts `new_coverage` from 78.3% to at least 80%, gets the CodeRabbit review the PR never had, and writes a landing) or re-scope PLAN-30 against everything that landed since (PLAN-142 to 164 changed `PlanCommand`, `OverviewFullScreenApp`, the operator and decisions catalogues and the mock). The orchestrator's reading: the code is done (D1 to D9) and the work still to do is a merge, one coverage gap and a review, so a resume is cheaper than a re-scope, but every day it waits more of main lands on the same files.
  → CLOSED 2026-11-22 (handle-open, orchestrator, confidence 95 > threshold 90; see Decisions): the decision the entry waited for was taken by the lane: #438 was merged (`2efcaa40`, `git log origin/main --grep '(#438)'`), the row `PLAN-30` is `shipped` (`queue/PLAN-30.json` re-read) and its landing is analyzed (`landings/PLAN-30.md`, the Open Defect `PLAN-30 landed`).

## Closed Watches relocated on 2026-11-23 (decision 10)

Moved verbatim out of `epic.md` on 2026-11-23 (the operator, decision 10, proposal 1): each entry's last marker is final (CLOSED, FIXED, SHIPPED, SETTLED, REPLACED or SUPERSEDED) and names no open residue. A pointer stays at the origin in `epic.md`.

- **Main moved outside the lanes: the third corpus review, #522 (`f707b231`, docs only, 27 files) with #536 (`pm-mcp-core`) and #198 (`plan-marshall-documentation`), merged 2026-11-20 19:05-19:08Z (read 2026-11-21); owner: the orchestrator (read) and the operator (the review's author).**
  → READ 2026-11-21 (the operator: "analyze, land and emit"; the inbox was empty): no lane file is touched: the running PLAN-173 and PLAN-174 and the emitted PLAN-175 name none of the 27 files (a search of the spec corpus: only the `CLAUDE.md` boilerplate line). **PLAN-42 deliverable 5 re-read and amended:** part (a) (four false `doc/Traceability.adoc` entries) is still in place at the new lines 122, 178, 245 and 384 (were 140, 208, 271, 440); part (b) (`pm-mcp-core` `harness-profiles.adoc:13`) is closed by #536 and part (c) (`01-onboarding.adoc`) by #198, so the deliverable is this repository only and the two sibling-repository Done-when clauses became read-only checks. PLAN-42's cited `01-commands.adoc` lines (:38 to :149) lie above the one line #522 removed (`:165`) and stand. The held specs that cite `doc/design/shared/*`, `doc/specification/runtime-model*` or `doc/Traceability.adoc` (about 30) are re-grounded at their own emit by `corpus verdicts`, not now. Nothing is transitioned or emitted from this observation; it needs no operator decision.
  → CLOSED 2026-11-22 (handle-open, orchestrator, confidence 92 > threshold 80; see Decisions): #522 `f707b231`, pm-mcp-core #536 `6d804b78` and documentation #198 `cbdfa5f` are on their repositories' `origin/main` (re-read); the entry says it needs no decision.

- **PLAN-168 reported mid-flight (2026-11-20, message `rb-168-cli-refusal-and-exit-residue-001`, PR #511 OPEN, head `bd60be5e`, 64 files, `DIRTY`); NOT a landing; no ship semantics; the row stays `running`; owner: the operator (how #511 goes on).** `landings/PLAN-168.md`, one read-only leaf; re-read at `bd60be5e`. The lane reports 4 of 6; by the plan's own rule (every Done-when clause met) the leaf's reading gives at most 2 (D1, D3). **D1 and D3 met; D2 and D5 met-modified; D4 not met (confirmed: `ExitCodesTest`, `MockRuntimeEngineTest`, `OnboardingScenarioTest`, `PicocliLanguageTest` are not in the diff); D6 not met (partly): four documents corrected, but no cleanup choice bar, no delete confirmation, no 120x30, outputs not committed.** The operator's choices are kept (A2: the texts name no audit id; B1: the code wins for `epic next`; the stream rule: the eight one-shot sites go to `err`, `ProjectCommand:127` refuted, `EntryFlow` and `PmConsole` keep `out`). **No blocker in the code; one major:** the literal guard can pass while wrong (a single-word literal such as the `plan report` labels `"outcome"`, `"reason"`, `"ran"` is not flagged by design; variables, other receivers and `new OperatorRefusedException("…")` text are not seen). Minor: three `config` paths still print a cause-less refusal to `out` (`ConfigCommand.java:147-151,1360-1363,1501-1504`, re-read), the `Failed to …` family lost its cause, a pid-less status has no test, a vacuous `assertFalse` on stdout in `PlanCommandTest`, `OperatorRefusedExceptionTest` without the controller, `epic` and `plan` differ on a code-less 404, a false "no call without a locale" (`PlanCommand.java:668,672` and about fifteen `format(key, …)` calls), `enrolment.*.failed` still prints "HTTP {status}", the cleanup-duration exit 3 → 1 beyond the spec, the claim "the runtime lists a row as launchable only when the scope has room" never confirmed in `pm-mcp-core`. **The 24 minors of CodeRabbit's review are all fixed in code at the head (read one by one), three real defects without a regression test; no reply on the PR answers them, CodeRabbit is `IN_PROGRESS` on the new head, and the gate says `undecidable_read` (the `Minor comments: 24` label gap, the second time after #480).** The head is 7 commits behind `main` with one conflict (`ControlCommandTest.java`: PLAN-171's generated ids against the PR's JSON 404 bodies; `76315c63` is not an ancestor). The PR holds no `.plan/` path (re-read by the leaf); a stray `pm-operator/.plan/` stays uncommitted in the lane's worktree; the lane admits shell file reads, `perl -pi` edits and a `python3` heredoc. **Open question for the operator:** continue the run to finish D4 and D6 (and rebase and answer the review) or merge #511 after the rebase and the answers and stage D4, D6's captures and the minors as a residue plan. The entry STAYS OPEN until #511 is merged or closed.
  → DECIDED 2026-11-20 (the operator chose option 2 of 2): **merge #511 after the rebase and the answers; D4, D6's capture clause and the leaf's minors become a residue plan.** The operator authorises the merge on the three gate readings (the unreadable `Minor comments` review body, the 28 body items answered on the PR, CodeRabbit's review of the new head) and the lane reports D4 and D6's captures as not met per clause. The orchestrator gave the operator the text to paste into the lane (conflict in `ControlCommandTest.java`, answers, the missing tests for a pid-less status and three CodeRabbit fixes, the corrections of the report's false claims). **The residue plan is NOT staged yet:** it is staged from #511's landing, after the merge, so that its claims are read at `main` and not at an unmerged head. Scope already named: D4 (`ExitCodesTest` per verb, 130, 3, 4, the three-line assertion, `RANDOM_ID` per test, `MockRuntimeEngineTest`, `OnboardingScenarioTest`, `PicocliLanguageTest`), the D6 captures (the cleanup choice bar and delete confirmation, 120x30, outputs committed), the literal guard's blind spots, the three `config` paths on `out`, the cause-less `Failed to …` family, `enrolment.*.failed` HTTP text, the locale-less calls, the vacuous assertions, the unconfirmed engine facts.
  → CLOSED 2026-11-20: **#511 was merged** (`ad4d5960`, 15:57Z); the final message `rb-168-…-002` is analyzed (`landings/PLAN-168.md`, final section) and the residue is **staged as PLAN-174** (see the Open Defect entry `PLAN-168 landed`). This Watch entry is closed.

- PLAN-33 (running, verification) filed three findings 2026-11-10 (`rb-33-verify-cli-acts-001`…`003`, at `0913d6f3`,
  drained as observed, archived): (1) `CLI-UC-V-06` — `onboard --auto --yes` has no verb (exit 2); the automatic
  onboarding is the entry dialogue's first choice since PLAN-22, so the CHECK's wording is outdated (route: the review
  doc's check text, at PLAN-33's result or PLAN-74); rest of V-06 passes. (2) `CLI-UC-V-09` — `plan nope` prints
  `Plan nope not found.` (no `No plan {id}` text; exit 1 holds) → PLAN-30; the full-screen overview in
  `reports-and-system` shows "Nothing runs…" for a REFUSED projects read (`OverviewFullScreenApp.fetchProjects:486`
  swallows, `ProjectsOverviewLevel.rows:86`; CLI-UC-F-15) → CORRECTED 2026-11-10: PLAN-30's re-grounding does NOT carry
  it ("a separate plan"); it is PLAN-126 deliverable 3. (3) `CLI-CC-V-06` — the
  waiver path (`WR-2`) has no `q`-first and no unknown-input PTY case (`AnswerDialogueIT:102-118`) → PLAN-127 (the PLAN-124
  residue plan). Expected-fail list of the PLAN-33 amendment matched (V-09); V-01/V-04/V-08 not yet reported.
  (4) 2026-11-10 `rb-33-verify-cli-acts-004` (`CLI-UC-V-04` German clause): operator ruled "exempt runtime text"; the clause
  and `CLI-CC-V-01` reworded by direct PR #402 (see Decisions); V-04 passes under the new wording. Retire this watch at the
  PLAN-33 landing once #402 is merged. PLAN-33's PR is #398 (open).
  → PLAN-33 LANDED 2026-11-10 (#398, `344515f9`): see `landings/PLAN-33.md`. The verdict corrections (V-06 stale check,
  UC-ONB-2 not verified, four `conforms` → `partial`, Totals 4/19/0/0/14) ride PR #402 (operator); the accept-edit gap is
  folded into PLAN-127. Retire this watch when #402 is merged.
  → RETIRED 2026-11-11: #402 merged `5fc36ebf` (operator's word).
  → DECIDED 2026-11-15 (handle-open, orchestrator, confidence 95 against threshold 90; see Decisions): the entry's own retirement is confirmed, `5fc36ebf` (#402) and `344515f9` (#398) are on `origin/main`; the residue it routed lives in the PLAN-30, PLAN-126 and PLAN-127 rows and specs, not in this watch.
  → CLOSED 2026-11-22 (handle-open, orchestrator, confidence 92 > threshold 80; see Decisions): #402 `5fc36ebf` and #398 `344515f9` are on `origin/main`; the rows PLAN-30, 126 and 127 are `shipped` (re-read).

- PLAN-123 STOPPED BEFORE THE MERGE (2026-11-10, `rb-123-cli-display-layer-residue-001.md`, `merge_state=open`, drained as
  observed; row stays `running`): PR #404 at `c8be43d1` (61 files), all four deliverables met by the lane's account, Sonar 0
  (PR search empty), `review-gate` 0 threads / 0 bodies — but CodeRabbit has reviewed NO head (3 × "Review rate limited",
  15:15/16:05/16:35; next allowed trigger per the lane 18:05:28), so the merge rule is not met. Owed: one CodeRabbit review
  of some head (re-trigger once; quota refusal = ask the operator), then the operator's merge word; then the full landing
  analysis (three-layer leaf) on the merge commit. Residue named by the lane: printed `plan report` hard-coded English;
  `RolesCommand` English-only; mailbox unreadable (Open Defect). PLAN-126 and PLAN-32 still wait for this merge.
  → PLAN-123 LANDED 2026-11-11 (#404, `1bdfede6`): see `landings/PLAN-123.md`; residue folded into PLAN-30 D5 and PLAN-126 D2.
  → CLOSED 2026-11-15 (handle-open, orchestrator, confidence 93 against threshold 90; see Decisions): the stopped merge it watched is carried by `1bdfede6` (#404, on `origin/main`); the residue it names is tracked by the PLAN-30 spec and row (PR #438 open) and by PLAN-126 (#416, shipped), not by this watch.

- PLAN-141 (D6) DECISIONS TAKEN, PR #403 OPEN AT DRAFT (`plan-141-001.md`, 2026-11-10, drained as observed; row stays
  `running`): head `207b62f0`, 13 design documents, CI green, links clean. Q1 plan tabs Process/Intent/Changes; Q2 epic tabs
  Plans/Intent/Landings/Reviews/Events; Q3 `plan-view-interaction.adoc` rewritten (`file=` with Changes replaces #400's
  `changes=open`); Q4 verdicts as `StatusLabel` `state.criterion.*`/`state.check.*`, new `intent-and-outline-texts.adoc`,
  chart on the project setup tab, the client contract GROWS (pm-mcp-core's intent record carries ADR relation, evidence,
  `NG-n`, `role`, `not_run` reason — the spec's hypothesis was refuted); Q5 no interim side slot, `side` on TriagePage and
  WizardPage only; Q6 remembered place without the mode, fill only on unmarked load / rail click; Q7 PLAN-85's decision
  stands (This browser holds theme, language, dictation). Operator 2026-11-10: the orchestrator reviewed #403 (one
  read-only leaf: 3 blockers, 6 majors, minors, all re-read) and, on the operator's word, fixed them on the branch
  (`e4109c2e`: README plan-tree and fill rules, Changes text per client, area `intent` registered, head counts from the
  verdict words, German `Deliverable`, chart read-only on the project's setup tab by its resource, the terminal shows no
  chart; 75 intent/state keys) and set the 12 documents AGREED (`721afd68`), pushed. CodeRabbit then REVIEWED `721afd68`
  (merge rule met): 5 minor inline findings fixed in `87288908` (glossary Outline/Role, "can flag", What waits as
  `/?lens=waits` on the operator's choice, a task address's `chat=open` is the plan's chat, selecting a part ends follow
  mode), threads replied and resolved; the additional comment (absent proposal in `OverSizeDistributionResource`) fixed in
  `cc00f135` and answered on the PR. Merge waits for the operator's word (branch behind main; the queue updates it). Not changed (reported): the plan ids PLAN-142..144/135..137 in `navigation.adoc:393`. OWED AT THE D6 LANDING (orchestrator, spec
  amendments): `doc/plans/plan-changes.adoc` address `?tab=changes&file=` (repo doc: a direct change); PLAN-55 (tab set),
  PLAN-58 (tab set), PLAN-62 (`node=` with Process only), PLAN-52 (rail rule holds), PLAN-135..137 (no interim slot; wait
  for 142, 143), PLAN-142 (side props on TriagePage/WizardPage), PLAN-143 (history.state mark, two entry points), PLAN-144
  (lens segment and Up push the unfilled list), PLAN-145 (verdict tag, texts, `NG-n`, `role`; a contract + mock step
  before it — stage or fold), PLAN-146 (chart on PLAN-59's setup tab).
  → DONE 2026-11-11 (see Decisions): amendments made, PLAN-159 staged, #413 open. Open questions recorded in the
  specs as HYPOTHESES: the engine allows 0 non-goals on the light lane while the client and design ask one or more
  (PLAN-159); the over_size route carries one role and no role parameter, the design one chart per role (PLAN-146,
  PLAN-159); the builder reason's wire name `reason` and an omitted `proposed_multiple` (no daemon route spec yet).
  → SETTLED 2026-11-12 at the PLAN-159 landing: non-goals stay ≥1 (operator; Open Defects); the one-role route is PLAN-146's;
  `reason`/`proposed_multiple` rest on the design alone (no engine or daemon schema names them).
  → CLOSED 2026-11-15 (handle-open, orchestrator, confidence 93 against threshold 90; see Decisions): the entry carries its own SETTLED line and both PRs it waited for are on `origin/main`: `8568b56e` (#403) and `ba9e82a8` (#413). The HYPOTHESES it left are recorded in the specs, not here.

- PLAN-35 follow-ups (landing report): re-read PLAN-43, 45, 51, 53, 55, 58 against the shipped components; the view plans bring the
  components in (no view calls them yet) and should assert the CSS-level behaviour where they touch it (alignment, 40/32 px rows, head border,
  `%` widths); PR #294 was closed on 2026-11-02.
  → CLOSED 2026-11-22 (handle-open, orchestrator, confidence 85 > threshold 80; see Decisions): PLAN-43, 45, 51, 53 and 58 are `shipped` (queue rows re-read); PLAN-55 was re-grounded in the cleanup of 2026-11-22 (its spec's `## Re-Grounding 2026-11-22`).

- CodeRabbit audit follow-ups (2026-11-02): all in PLAN-93 D5, PLAN-94, PLAN-95 and the two mailboxes; PLAN-94 follows PLAN-92 and PLAN-93 follows PLAN-89 (shared files). Operator decisions inside them: the
  `NeedsOperatorItem` item id (PLAN-95 D3: read the contract), the `broadcastReset` after `goToState` (PLAN-95 D2), the optional catalogue-rollback retention (PLAN-93 D5). Open reading to repeat on
  every landing: the review BODIES, not only the threads.
   → CLOSED 2026-11-18 (handle-open, orchestrator, confidence 91 > threshold 90): PLAN-93, 94 and 95 are `shipped` (#319, #321, #327; queue rows re-read), the three operator decisions inside them were taken (the 2026-11-05 Decisions entry, now in `settled.md`), and the review bodies are counted by the `review-gate` verb (`runbook-lane/SKILL.md:1042-1045`).

- PLAN-37 follow-ups (landing report): PLAN-89 deliverables 4 and 5 (the leads above and the wider audit); `SystemView.svelte` is now touched by PLAN-37 (merged),
  PLAN-90 deliverable 3 and PLAN-43 (hunks differ, rebase when either lands); the operator's call on the standing merge authorization for heads the reviewer
  never saw. The send button of the chat no longer disables itself on an empty message (the lane's choice).
  → CLOSED 2026-11-22 (handle-open, orchestrator, confidence 90 > threshold 80; see Decisions): PLAN-89 and PLAN-90 are `shipped` (queue rows re-read); the merge-authorisation sentence is superseded by `CLAUDE.md` § Git Workflow.
> ↪ Relocated to `settled.md` § "PLAN-37 reviewer stop of 2026-11-01 (history)" — PLAN-37 merged as c5bfb1ca

- PLAN-46 residue (2026-11-01, PR #296 merged): `overview.find.plain` is cited in the shared design's prose with no Texts row and no
  catalogue key (nobody owns it); the palette field has no labelled clear button (the design names no key); `/` now belongs to the
  palette, so a view's filter no longer focuses on `/` (the design assigns the key to the panel; an intended change). PLAN-46's
  message counts five files outside its declaration. — settle where the next find consumer (PLAN-84) reads the design.
  → CLOSED 2026-11-22 (handle-open, orchestrator, confidence 82 > threshold 80; see Decisions): `overview.find.plain` appears nowhere outside `.plan` (`git grep` at `origin/main` empty); the palette clear is the browser's own `type="search"` (`CommandPalette.svelte:394`); PLAN-84 is `shipped`.

- Surface under-declaration, PLAN-82 (2026-10-31): the spec declared 15 entries and the diff touched 39 files; 17 were
  never declared (all tests, the new design document, `MachineApiPaths.java` instead of the declared `MachinePaths.java`,
  and three `pm-operator` test files in a module the spec did not name). The lane's message says the surface stayed
  inside the named modules; the diff says otherwise. No staged spec collided in practice. — when a spec that changes a
  shared contract is staged, declare the tests and the sibling module whose tests the change forces (PLAN-34, 36, 84).
  → CLOSED 2026-11-22 (handle-open, orchestrator, confidence 90 > threshold 80; see Decisions): PLAN-34, 36 and 84 are `shipped` (queue rows re-read); the entry is a process lesson with no open item.

- Structure review of the staged plans, applied 2026-10-30 at the operator's word ("fix clear and weaker"); see the
  Decisions entry of that date. Left open: PLAN-84 (`find`) stays in WS-04 although it belongs with PLAN-36 in WS-05 (the
  queue verb cannot change a row's workstream; the workstream documents say so); the review finding "PLAN-59 bundles
  projects with project onboarding" was withdrawn after reading PLAN-64: the project onboarding view and the machine
  onboarding screens are different views and documents. — re-check the split at each emit of the plans it touched.
  → CLOSED 2026-11-22 (handle-open, orchestrator, confidence 88 > threshold 80; see Decisions): PLAN-84 is `shipped` (queue row re-read); the PLAN-59 bundling finding was withdrawn by the entry itself.
> ↪ Relocated to `settled.md` § "Retired Watches (2026-10-28 to 2026-10-30)" — the watch is retired, its subject is closed (PR #276 merged as `44803a77`)

- PLAN-46 review findings fixed in intent (PR #300, `1ce32d80`, 2026-11-01), but the three-layer analysis (read, not run) found
  more; see `landings/PLAN-46.md`, section "Three-layer analysis of PR #300". OPEN, for the operator: (C1) the results container is
  now a `listbox` and still holds the empty-state paragraph and the Retry button, invalid owned content in the "no match" and "failed"
  states; (C2, C3) a heading and a repeated label inside the listbox group, a constant `aria-expanded`; the design document was not
  updated in the PR although the plan's Standing Constraints require it; the filter test never asserts the selection and another test
  leaks a `scrollIntoView` stub. One more small fix PR would close them; no queue row. Earlier text: The six findings CodeRabbit filed on
  `42aee47d` (two Major: no touch close control, arrow-key selection not exposed; four Minor) are in the merged code with tests. The
  fix had no bot review (`skip-bot-review`), so the two accessibility changes (combobox with `aria-activedescendant`, close control)
  rest on the lane's self-review; PLAN-37's axe-core audit is the first independent check once it merges, if it reaches the palette
  (not established). Open: the palette's clear action is the browser's own, `type="search"`, no catalogue key; `/` belongs to the
  palette now. PR #294 is redundant (the same 178 paths landed as PR #298): close it, the operator's call. The merge authorisation
  question stays open: PLAN-17 merged after a free-text answer that did not grant it, PLAN-46 on a review whose findings were
  unhandled; PR #300 merged on the operator's word and a label.
  → CLOSED 2026-11-15 (handle-open, decided by the orchestrator at confidence 91 against threshold 90; see Decisions): the "one more small fix PR" exists and shipped as PLAN-89 `web-palette-hardening` (row `shipped`, PR #305, `landings/PLAN-89.md`). Re-read `CommandPalette.svelte` on `origin/main`: C1 fixed, the empty text and the failed-read Retry sit in `.command-palette__status` (`:477-497`), outside the listbox (`:417-418`), which holds only groups and options; C2 handled, the group heading is `role="presentation"` inside a `role="group"` named by it (`:423-428`); C3 fixed, `aria-expanded={rows.length > 0}` (`:396`), no longer constant; the `scrollIntoView` stub is restored in its test (`CommandPalette.test.ts:295-323`). PR #294 is `CLOSED`, unmerged. The merge-authorisation sentence is superseded by the repository rule (runbook-lane runs merge their own pull request once the gates hold), and PLAN-17, PLAN-46 and #300 are history. Facts kept, not defects: the palette's clear action is still the browser's own (`type="search"`, `:394`) and `/` belongs to the palette. Not re-read: the design document update and the axe audit of PLAN-89 beyond its landing message.

- Pull requests over 100 files cannot be reviewed (PLAN-78 run, 2026-10-31): CodeRabbit, the required reviewer, refuses
  more than 100 files and, once its quota is spent, every retry; Sourcery refuses above 150,000 diff characters. PLAN-78's
  first PR (197 files) was never reviewed and was closed; the second (#288) merged. The 197 came from the `pre-commit` gate
  rewriting the import grouping of 171 `pm-api` main sources (pre-existing drift on `main`, no plan owns it). PLAN-78 landed
  with 25 files (the rewrite restored from `origin/main` twice, on the operator's decision), and its message says the drift
  stays on `main` (170-odd rewrites). PLAN-87 landed (40 files) and had the operator route the rewrite into its own pull
  request: PR #294 (`style: apply the pre-commit import grouping to main sources`, label `skip-bot-review`, 177 `pm-api` sources
  and one `pm-mock-runtime` source). On 2026-11-01 the PLAN-46 lane landed the same 178 paths as PR #298 (`f5de8449`, label
  `skip-bot-review`), so the rewrite is gone from `main` and PR #294 is redundant and still open (the operator's to close). Runs
  that start from `f5de8449` or merge it forward no longer meet the rewrite (PLAN-35, 37, 26 started earlier). PLAN-79 (about 116 classes) may still need a split;
  PLAN-87's share of classes taking generated input was 40 of 45, against 25 of 73 in `pm-api`, so its estimate is a hypothesis to
  check at its outline.
  → CLOSED 2026-11-22 (handle-open, orchestrator, confidence 88 > threshold 80; see Decisions): PR #294 is `CLOSED` and unmerged (`gh pr view 294`), the rewrite landed as #298 `f5de8449`; the 100-file rule is a standing practice, not an open defect.

- Cleanup reminder interval has no write path (PLAN-09 residue, 2026-10-31): `cleanup_durations` is a single integer, so
  the reminder cannot be set from the clients; the run answered it on the PR as a server-contract gap. — no plan owns it;
  decide with the daemon repository.
   → CLOSED 2026-11-18 (handle-open, orchestrator, confidence 93 > threshold 90): the clients' write path exists: `client.ts:1428` types `cleanup_durations: Record<string, number>` and `SettingsView.svelte:814` sends the key `'cleanup_durations'` (the record's `cleanup_durations` is a per-category map, `MachineSettingsRecord.java:74`; `MachineSettingsUpdateRequest.java` names the key). Whether the real daemon accepts it stays under the "open outside this repository" entry (item (3)).

- Coverage gap of the logging test (PLAN-77 residue, 2026-10-31): the `MOCK-201` close-failure record of
  `MockRuntimeExtension` is asserted through a package-private helper, because no production path reaches that catch
  (`MockRuntime` is final and `close()` catches every `IOException`). — accept it as is, or ask for a production seam;
  the operator's call.
  → CLOSED 2026-11-15 (handle-open, operator chose proposal 1 of 3 at the orchestrator's confidence 75): accepted as it is. Re-read on `origin/main`: `MockRuntimeExtension.afterEach` (`:110-125`) has the real `catch (RuntimeException e)` that calls `logCloseFailure(e)`; `MockRuntime` is `final` (`MockRuntime.java:47`) and its `close()` (`:535`) is the only thing that could throw into it; `logCloseFailure` (`:288`) is package-private with that reason in its Javadoc, and `MockRuntimeExtensionLoggingTest.java:49-54` asserts the `MOCK-201` record through it. The record's text and identifier are covered; the one-line `catch` wiring is not run end to end. A production seam (a non-final `MockRuntime` or an injected closer) would add surface to a mock runtime for one catch line and was declined. Not checked: whether `MockRuntime.close()` can throw a `RuntimeException` at all.

- `npm run format:check` fails on 42 files of `pm-web-app` at HEAD (PLAN-80 residue; the Maven build does not run it). No
  plan owns a formatting pass, and one would touch most views. — decide whether any plan takes it; not before the view
  plans have landed.
  → CLOSED 2026-11-22 (handle-open, orchestrator, confidence 88 > threshold 80; see Decisions): `6e31ae0a` (#447) applied prettier to the two web modules on `origin/main` and answers the entry's question; `format:check` was not re-run here.

- Standards left to the operator (PLAN-77/80 analysis, 2026-10-30): `stylelint` (the `css` and `lint-config` skills expect
  it; 3 CSS files and component styles), `dependency-cruiser` (the `arch-gate-js` fitness check), `org.jspecify:jspecify`
  in `pm-api`, `pm-exec`, `pm-relay` (needed for `@NullMarked` there), `cui-test-value-objects` (only for types with
  hand-written equality; none found) and a sanitizer (no production code needs one). Each is a new dependency or plugin.
  — decide when a plan needs one; none is added by PLAN-77 to PLAN-80. `stylelint` is approved for PLAN-72 (its spec
  says so): the plan that adds it also applies the `css` and `lint-config` expectations of PLAN-80 to it.
  → CLOSED 2026-11-23 (the operator, decision 9 proposal 1; see Decisions): the standards that need a new dependency (stylelint, dependency-cruiser, jspecify, `cui-test-value-objects`, a sanitizer) are covered by the standing rule `CLAUDE.md` § Dependencies (ask first); stylelint's approval stays in PLAN-72's spec (parked).

- New specs and re-grounded specs must name the CUI skills and carry the standards Standing Constraint: Java deliverables
  `pm-dev-java:javadoc`, `pm-dev-java:java-null-safety`, `pm-dev-java-cui:cui-testing`, `pm-dev-java-cui:cui-logging`;
  web deliverables `pm-dev-frontend:javascript-security`. Specs not adapted: PLAN-24, 74 (documents only), the
  plan-marshall-lane verify plans (no Skills lines), PLAN-14, 20, 22, 39 (running) and the shipped ones.
  → CLOSED 2026-11-23 (the operator, decision 9 proposal 1; see Decisions): the skills clause for new and re-grounded specs is machine-checked: `corpus spec-skills` reports the declaration and the emit verdict at every emit.

- The CUI test standards are applied to inputs; the expected value of a contract (catalogue text, wire name, golden
  capture) stays literal. That is the orchestrator's reading of `cui-testing` ("no manual test data"), not the skill's
  text, and PLAN-78 and PLAN-79 report any class where it does not hold. — the operator may overrule it at emit.
   → CLOSED 2026-11-18 (handle-open, orchestrator, confidence 94 > threshold 90): the reading is now repository rule: `CLAUDE.md` § Code Standards ("The expected value of a contract — catalogue text, wire name, golden capture — stays literal, because it is the expectation, not input").

- `landing.failed` is accepted as an event name in the PLAN-13 tests but no `EventName` member carries it; the runtime
  sends `hold.opened` with kind `landing_failed` (PLAN-13 residue). — check where a later spec names it.
  → CLOSED 2026-11-22 (handle-open, orchestrator, confidence 90 > threshold 80; see Decisions): `EventKind.java:37-39` documents that no `EventName` member carries `landing.failed` and why the name is kept (the constant is at `:88`): deliberate, nothing to fix.

- The mock limits (`doc/claude_review/README.adoc#limits`) can make view checks `not verified` until
  PLAN-11 lands (PLAN-03 landed 2026-10-28: goto now applies a state's facts for the three scenarios, with
  four `system.*` lifecycle states still showing no view change). — re-check at the first verification
  landing.
  → CLOSED 2026-11-22 (handle-open, orchestrator, confidence 85 > threshold 80; see Decisions): PLAN-11 and PLAN-33 are `shipped` (queue rows re-read); the mock's limits are the standing documented design.

- Playwright MCP browser shared between parallel sessions (PLAN-12): another session took over its tabs,
  and a tab left open on the app port broke one e2e run. The port blocks separate the mock and the app,
  not the browser. — re-check at the next landings of browser-checking plans running side by side. If it
  recurs, give each opencode session its own Playwright MCP profile or output directory, or run only one
  browser-checking plan at a time. PLAN-03 (2026-10-28) ran its 48-pass browser matrix with an isolated
  Playwright MCP profile and reports no interference; one profile per session looks sufficient.
  → CLOSED 2026-11-23 (the operator, decision 9 proposal 1; see Decisions): the Playwright MCP browser: one profile per session sufficed (PLAN-03's 48-pass matrix); no recurrence recorded since; reopen if two browser-checking plans collide again.

- PLAN-10's run (antigravity, conversation `e291b36e…`) dirtied the main checkout on 2026-10-27 at 22:26.
  From `/Users/oliver/git/pm-mcp-clients`, not its worktree, it ran Playwright directly
  (`npx playwright --version`, then
  `npx @playwright/test test -c .plan/temp/playwright.config.js` with an ad-hoc config it wrote there).
  That left the untracked `test-results/` (`.last-run.json`: `failed`, no tests). Its browser check
  should run through the Playwright MCP server, not a hand-run Playwright from the main checkout. — check
  PLAN-10's landing for how its browser regression was evidenced; the operator removes `test-results/`.
  Recurrence 2026-10-28: the runs of PLAN-15 and PLAN-19 each stopped to ask the operator about an untracked
  `test-results/.last-run.json` in the main checkout (PLAN-15: moved to `.plan/temp/`; PLAN-19: the operator
  answered "fixed"). The file keeps reappearing in the main checkout; which run writes it is not established.
  PLAN-10's landing (2026-10-29) is silent on it and shows its browser snapshots taken through the Playwright MCP
  server (after the operator repaired the output directory), so the PLAN-10 part of this watch is closed.
  → CLOSED 2026-11-23 (the operator, decision 9 proposal 1; see Decisions): `test-results/` is gone from the main checkout (re-read) and PLAN-10, 15 and 19 shipped; the lane's rule (browser checks through the Playwright MCP, never a hand-run Playwright from the main checkout) stands in its standing constraints.

- Tab titles name the identifier (`drop-legacy-api`), not the display title (PLAN-04 residue). No
  staged plan owns a title store for the shell. — decide when PLAN-09 (`web-display-layer`) is emitted,
  or at the PLAN-29 verification (`CC-V-04`).
  → CLOSED 2026-11-23 (the operator, decision 9 proposal 1; see Decisions): tab titles come from a catalogue key through the layout (`routes/+layout.svelte:303-309`, the title waits for the catalogue and carries the attention count): the shell has its title path; PLAN-09 shipped.

- PLAN-76's deliverable 1 browser check is not evidenced. It asked for the system view showing
  `127.0.0.1:20760` while a second mock runs on `7420`; no Playwright file and no progress line exist.
  The behaviour is covered by `MockRuntimeTest` and the concurrency run. — check it in PLAN-18
  (`verify-shared-foundation`).
  → CLOSED 2026-11-18 (handle-open, operator chose proposal 1 of 4, recommended at the orchestrator's confidence 70): accepted as covered and the watch dropped, no plan. The working evidence is indirect: lanes since run concurrently on their own assigned ports without collision (PLAN-49 on 20490/20491, PLAN-52 on 20520/20521) with browser checks against them; the "covered by MockRuntimeTest" claim could not be confirmed (no hit for 20760 or 7420 in the `pm-mock-runtime` tests) and the exact system-view check was not re-read.

- Brand residue (PLAN-27, 2026-10-29): three CodeRabbit nitpicks on `favicon.test.ts` were answered, not fixed
  (stroke half-width in the dot bounds, an exact palette set, the ring on the dot circle), and `<html lang>` stays
  `en` on the German pairing page (`src/app.html` hard-codes it, nothing writes `document.documentElement.lang`).
  No staged plan owns either. — the test tightening is low value; the `lang` attribute is an accessibility defect
  for the German pages: PLAN-39 shipped (2026-10-30) without it and `src/app.html` still reads `<html lang="en">`.
  — decide which plan takes it (a view plan, or stage a one-line fix).
  → DECIDED 2026-11-15 (handle-open, operator chose proposal 1 of 4 at the orchestrator's confidence 60; the entry STAYS OPEN until the writer lands). **PLAN-152 (`web-foundation-catalogue-cleanup`, staged) takes the `<html lang>` fix.** Re-read on `origin/main`: `pm-web-app/src/app.html:2` is `<html lang="en">` and no code in `pm-web-app/src` writes `document.documentElement.lang`; no staged plan mentioned it. The writer belongs where the locale changes: a subscription on `currentLocale` in `lib/i18n/index.ts` (PLAN-152's spec already reasons about `setLocale` there, line 105) that sets `documentElement.lang`, with a test pinning `de`; the `app.html` default stays `en`. It must also hold at the first load of the pairing page, not only on a change of language (not checked how that page picks its language). Follow-up named, nothing staged and PLAN-152's spec is NOT edited from here: the clause is added to PLAN-152 when it is emitted (it is on the resume anchor's next-emit list). The three CodeRabbit nitpicks on `favicon.test.ts` (stroke half-width, an exact palette set, the ring on the dot circle) are recorded as accepted, low value, as this watch already said.
   → CLOSED 2026-11-18 (handle-open, orchestrator, confidence 95 > threshold 90): the writer landed with PLAN-152 (#468 `e9aef9a6`): `i18n/index.ts:51-55` subscribes to `currentLocale` and sets `document.documentElement.lang` (re-read at `origin/main`; its comment says the pairing page is covered through the root layout), `app.html:2` keeps `<html lang="en">` as the first-paint default, and the three favicon nitpicks were accepted as low value. See `landings/PLAN-152.md`.

- The tab icon, the header badge and the tab title all read `runtimeStore.openDecisionsCount` (PLAN-27). — PLAN-49
  switches all three to the attention total; check at its emit (note added to its spec).
   → CLOSED 2026-11-18 (handle-open, orchestrator, confidence 94 > threshold 90): PLAN-49 landed (#461): the tab title reads `$attentionStore.total` (`+layout.svelte:298`), the rail count reads it (`NavRail.svelte:76-77`) and the favicon follows the attention count (`favicon.ts:25`, fed at `+layout.svelte:323`). See `landings/PLAN-49.md`.

- Mock residue named by the PLAN-11 run (2026-10-30), accepted and not fixed: the drain-reconcile race after the event
  stream connects (needs a connection-established signal the SSE client does not expose); a drain started through
  `POST /api/v1/operator/runtime/stop` leaves the status without `drain_remaining_count`; a paused plan's synthesized
  current node is `waiting_for_operator` because `NodeState` has no paused value; the draining banner's `what` stays
  generic; the synthesized plan's activity body is the template's for a listed plan without its own fixture. — check
  where a spec reads the mock plan detail or the drain status: PLAN-20, 47, 52, 55. PLAN-39 (2026-10-30) fixed one more
  case at the operator's word: a plan listed only inside an epic's rows now resolves (`planListedByAnEpic`), takes its
  state from the row (`shipped`/`merged`/`landed` read as `done`) and names its epic.
  → CLOSED 2026-11-23 (the operator, decision 9 proposal 1; see Decisions): the mock residue was accepted and not fixed; PLAN-20, 47 and 52 shipped; a spec that reads the mock plan detail or the drain status checks it at its own outline.

- Browser regression clause on terminal-only plans (PLAN-10 residue): PLAN-10 D2 asked for web views "that render
  the changed catalogue areas (control)" but changed only `operator.*` texts, so the clause was not met. The
  same generated clause is in PLAN-05 (shipped), 17, 21, 22, 23, 26, 34 and 36. — covered by the terminal-check
  watch below: before emitting each, keep the clause only when the plan changes a catalogue the web renders.
  → CLOSED 2026-11-22 (handle-open, orchestrator, confidence 92 > threshold 80; see Decisions): PLAN-05, 10, 17, 21, 22, 23, 26, 34 and 36 are all `shipped` (queue rows re-read); the generated-clause concern for the still-staged terminal plans is the open entry on terminal-check targets.

- CodeRabbit refuses a review of the final head (PLAN-10 too, 2026-10-29: refused twice, newest reviewed head `1c77501`
  against merged head `52275941`, after three qualifying reviews; the lane merged under the standing authorization;
  the quota refusal was recorded; PLAN-25, 2026-10-29: the re-trigger after the last push returned no review at all,
  the check stayed `NEUTRAL`, an earlier qualifying review on `1d494f9` stood): PLAN-03, PLAN-15 and PLAN-19 each merged a head that no
  CodeRabbit review covers (re-triggers refused as "Review rate limited": PLAN-15 at 17:25 and 18:03, PLAN-19 at
  17:44 on 2026-10-28, the next slot given as 19:14Z); the lanes merged under the standing review authorization after an earlier qualifying review and the
  operator's word. The quota is shared by every plan running side by side. — re-check at each landing; if a
  landing has no qualifying review at all, that is a defect rather than this watch.
   → CLOSED 2026-11-18 (handle-open, orchestrator, confidence 92 > threshold 90): the operator settled the rule on 2026-11-02 (one CodeRabbit review of some head is enough; later unreviewed heads are fine) and the lane carries it: a qualifying review is required and one handled review is the review (`runbook-lane/SKILL.md:890-897,930-934`; fix commit `280e6c404`). A landing with no qualifying review at all is a defect and is recorded as one.

- Transient provider failures in the run of PLAN-15 and PLAN-19, both cleared by a retry: the CI calls answered
  `Not authenticated` (the operator answered "retry" in both runs), and PLAN-15's first `build / sonar-build`
  failed on Sonar ("Organization is not allowed to access data from non main branches") and passed on re-run
  of the same head. — re-check at the next landing; if the authentication failure recurs, find why the CLI
  session lapses.
  → CLOSED 2026-11-23 (the operator, decision 9 proposal 1; see Decisions): transient provider failures: no recurrence since 2026-10-28 and the three landings of 2026-11-23 read cleanly; reopen on a recurrence of `Not authenticated`.

- PLAN-15's 80-column footer clause was written wider than the design: `? more` appears only where a cell had
  to be left out, so the overview and the activity (whole footer fits) do not end with it. — PLAN-56
  (`verify-cli-interaction`, `CLI-UX-V-02`) takes the design, not the clause, as its criterion; check the clause
  before emitting it.
   → CLOSED 2026-11-18 (handle-open, orchestrator, confidence 94 > threshold 90): PLAN-56 is `superseded` (queue row re-read), so the clause is never emitted as written.

- `PLAN-143` mid-flight report (2026-11-15, inbox `rb-143-web-addresses-and-remembered-place-001.md`, PR #446 **open**, branch `feature/143-web-addresses-and-remembered-place`, last commit `5b1d6c28`; the branch worktree
  held uncommitted edits to `routes.ts`, `history.ts`, `runtime.ts` and `runtime.test.ts` when read, so the lane is still working): `deliverables_done=0` of 5; five of the Done-whens are "not met as written" by the lane's own
  account; self-review round 1 `budget-exhausted` with one blocker and ten majors, eight structural ones left open; CodeRabbit never reached (CI pending at the 540 s bound); the gate never run; Sonar `unknown`. Re-read
  (`parseAddress`, `routes.ts:522-538`): `parseAddress('/')` is `null` unless `?lens=waits`, so the lane's "the start fill at `/` cannot fire" holds; `/projects/onboard` is the project-onboarding route and also a
  possible project id (the design cannot tell them apart; owner PLAN-59 for the onboarding address, lead for the outline of 143's fix). Process notes (self-reported): two e2e runs went to the default ports 5173/7420 before the
  assigned 21430/21431 were used (risk to any other server on those ports); the main checkout was edited by mistake for two test files at the start of D1 and restored; the executor was at 0.1.2118 while the lane was at 0.1.2133;
  e2e runs went through `in_process_fallback` because a daemon child does not inherit the shell environment, so the build server's default routing cannot serve the ports contract (owner: operator, for the build-server and
  `runbook-lane` maintainers). Action for the next analysis: this plan's real landing, after the lane finishes; the findings above are leads, not a verdict.
  → SUPERSEDED 2026-11-15 by the landing analysis (`landings/PLAN-143.md`, the Open Defect `PLAN-143 landed`): the leads were read against the merged tree; the retained facts are in that report. Kept as history.

- `PLAN-143` follow-up (2026-11-15 19:19Z, inbox `rb-143-web-addresses-and-remembered-place-002.md`; filed as `kind: landing`, but **not a landing**: PR #446 is open, unmerged, no ship semantics; PLAN-143 stays `running`;
  `landing-check` `complete: false`, no `landing-facts` block, all 17 required keys missing, which is expected for a mid-flight report). Verified against ground truth at 19:35Z: the PR is `open`, `mergeable: conflicting`,
  `merge_state: dirty`; the remote head is `10930a04` as the message states; the lane's branch worktree already holds an unpushed `merge: origin/main into feature/…` commit (`0651b55e`, 6 ahead), so leftover 1 (the
  conflict) is being worked and the remote head has not moved yet. Unverified leads (not re-read): Sonar 0 confirmed at `f65e1341` and stale on the head; the e2e run (414 passed, ports 21430/21431); the review gate's
  `undecidable_read` on CodeRabbit's `Minor comments: 22` title (a gap in the `runbook-lane` review-gate parser, owner: operator for the `runbook-lane` maintainers, and it blocks `merge_allowed: true` for any PR CodeRabbit
  titles that way). Done-whens still 0 of 5 as written. Residue the lane names and no staged plan owns, **to be weighed as a residue plan at PLAN-143's real landing, not staged now**: (4) `factsOf` opens a remembered plan
  without a per-plan liveness read; (5) a report address outside the first list page is not resolved; (7) `fallBackFromDeletedObject` has no caller; (8) 22 minor and 2 nitpick CodeRabbit findings with two weak tests.
  Owned elsewhere: (6) `/decomposition` standalone entry (the decomposition plan, PLAN-58), (8, `file=` transition) the plan's tab control (PLAN-55, PLAN-58), (9) `/projects/onboard` ambiguity (PLAN-59). Action: the
  next analysis is PLAN-143's real landing; until then nothing is staged or transitioned from this message.
  → SUPERSEDED 2026-11-15 by the landing analysis (`landings/PLAN-143.md`, the Open Defect `PLAN-143 landed`): PLAN-143 is `shipped` (#446, `71f84bb0`); the unverified leads here were read against the merged tree (Sonar: no issue at the merged head by the Sonar search; the gate's `undecidable_read` confirmed; residue 4, 5, 7 and 8 mapped in the report's Follow-Ups). Kept as history.
