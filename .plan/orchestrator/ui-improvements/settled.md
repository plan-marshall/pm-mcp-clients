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
