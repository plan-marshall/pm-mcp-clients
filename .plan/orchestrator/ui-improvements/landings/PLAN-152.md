# Landing Analysis: PLAN-152 — Web Foundation: The Catalogue Clean-Up

epic: ui-improvements
workstream: WS-02
pr: #468 (https://github.com/plan-marshall/pm-mcp-clients/pull/468), follow-up #476 (https://github.com/plan-marshall/pm-mcp-clients/pull/476)

> Landing record, written by `analyze` on 2026-11-18 from the inbox message
> `rb-152-web-foundation-catalogue-cleanup-001.md` (revision 1, a lead, not a fact), git, the provider, and a read-only three-layer leaf.
> The orchestrator re-read the points marked "re-read" on `origin/main` at `98ca84d0` (which includes PLAN-66's #478, so line numbers in `i18n/index.ts` differ from the lane's).

Merged as `e9aef9a6` (#468, squash, 2026-11-17T21:32Z, 48 files, +609/-120) and `5cb2a3e4` (#476, 2026-11-17T23:55Z, 2 files, +34/-3: the one CodeRabbit finding that landed after #468 had merged). Sender `rb-152-web-foundation-catalogue-cleanup`. Lane: runbook. Harness `opencode`, model `deepseek-v4.1-flash/high` (as stated, not corroborated). The lane reports `deliverables_done=5` of 5 (D1 to D5; the `<html lang>` amendment counted in them) and `self-review: clear` (two rounds).

## Deliverable Fidelity vs Spec

| Deliverable (spec, amended) | Verdict | Evidence |
|-----------------------------|---------|----------|
| D1 — shared acts | shipped | `common.action.{back,back.title,cancel,close,details,later,retry,save}` in both packs (`common.{en,de}.properties:259-266`); the old keys gone, 20 web readers moved; the spec's grep over `pm-web-app/src` and `pm-api/src/main` is empty except the absence assertions (`catalogue.test.ts:29-55`); `catalogue.test.ts:462` and `MessageCatalogueTest.shouldRetireTheDuplicateActKeys:227` fail on a put-back key |
| D2 — kinds, filters, reasons, states | shipped, partly pinned | the find list reads `common.kind.*` (`CommandPalette.svelte:429`, re-read); the filter label is one key, `Filter {what}` / `{what} filtern` (`FilterInput.svelte` requires `label`; `DecisionView:1403`, `ReportsView:1086`, `OverviewView:852,1195`, `EpicView:938`); `overview.needs.operator_wait` is gone and the web normalises it (`attention.ts:123`); the states part is a no-op (no web reader of `overview.state.*`; the six keys stay for `pm-operator`); the key list for the terminal package is in the PR body. The reader of the kinds and three of the five filter labels are unpinned (see layer 1) |
| D3 — deferred reload | shipped | no request while a field holds unsent input, the shown locale kept, applied on release, a cached locale at once (`i18n/index.ts:547-577,487-523,628-655,64-71`; `SystemView.svelte:238-256,145`); each guard is pinned: removing a post-await guard, the supersede or the pre-await check makes an `index.test.ts` test fail (the leaf checked each) |
| D4 — documents | shipped | no retired key under `doc/design` (the leaf's grep) |
| D5 — browser evidence | not shown by reading | 33 screenshots named in the PR body |
| Amendment 2026-11-17 `<html lang>` | shipped | a subscription on `currentLocale` (`i18n/index.ts:51-55`), `app.html:2` stays `en`; pinned by `index.test.ts` (`:667,670,693`); the pairing-page test calls `initI18n()` on a fresh module and does not render the layout or the pair route |

**Files the diff touches that the Expected Surface does not list:** the spec's own consequences (20 web views and shell components that read the retired act keys, the test files of those). `pm-operator` was not edited (the lane: the readers stay). **The Expected Surface row for `app.html`** is the orchestrator's to add (the lane cannot edit the spec): the spec's emit amendment named it; the row is stale until edited.

`text-guard-baseline.json` and `ALLOWED_LITERALS` in `catalogue.test.ts` are untouched by both diffs: nothing grew.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf — reason: the Dispatch Decision Rule; 48 files and two merges. Build logs, the mutation runs and screenshots were not opened ("not shown by reading"). The orchestrator re-read the points marked below.
- **Verdict:** layer 1 minor (unpinned readers); layer 2 no contradicted claim of weight; layer 3: **no blocker, no major**, 8 minor, 2 leads.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | the find list reads the shared kind keys, pinned by a test | checked | partly | re-read: `CommandPalette.svelte:429` reads `` common.kind.${group.kind} ``; `CommandPalette.test.ts:171` only asserts the heading text is non-empty (re-read); the `$t('…')` lookup scan at `catalogue.test.ts:480` ignores template keys: a revert to `` overview.find.kind.${kind} `` would pass every unit test and render the humanised key |
| L1-2 | the filter label is pinned in every view | checked | partly | Decisions pinned (`DecisionView.test.ts:397`, `DecisionCatalogueTest.sharedFilterLabel`); Reports, Overview and Epic find the field by test id only (`ReportsView.test.ts:639`, `OverviewView.test.ts:789`, `EpicView.test.ts:753`): a wrong `what` argument goes unpinned |
| L1-3 | `catalogue.test.ts` fails on a key that no reader asks for | checked | refutes | no orphan-key test exists in the diff or at HEAD; the grep shows no orphan now; the spec text is a description, so minor |
| L1-4 | the Expected Surface names `app.html` and its test (the amendment) | checked | refutes | the lane cannot edit the spec; the orchestrator's edit is still owed (re-grounded below) |
| L1-5 | the design documents carry no retired key and the filter example is right | checked | partly | no retired key; but `view-density-and-filtering.adoc:63` still reads "(_Filter decisions_)" (re-read) and `view-layouts.adoc:18` "[Filter reports]" against the shipped "Filter Decisions", "Filter Reports" |

### Layer 2 — Implemented

| Item | Verdict | Evidence |
|------|---------|----------|
| **the retired-key reader search** (the most important check) | **clean** | the leaf ran `git grep` at `origin/main` over the whole repository for every retired key (`web.common.(back\|close\|save\|cancel\|retry)`, `common.actions.back`, `common.back\|close\|save`, `decision.waiver.later`, `decisions.filter.label`, `report.filter.label`, `overview.kind.project`, `overview.needs.operator_wait`, `overview.find.kind*`, literal and template forms) across `pm-web-app/src`, `pm-api/src/main`, `pm-operator/src/main`, `pm-mock-runtime`, `pm-web-e2e`: hits only in the absence assertions of five catalogue tests and in `.plan/orchestrator`; the dynamic readers (`CommandPalette:429`, `ReasonBadge:101`, `attention.ts:268`) resolve; `pm-operator` still reads `overview.kind.epic\|plan` (`EpicCommand:292`, `StatusCommand:229-230`, `EndedWorkLevel:170`, `ProjectWorkLevel:182,210`) and `overview.state.*` (`StatusCommand:368`, `SystemView.java:322`), and those keys are present |
| **PLAN-51's keys after the `-X theirs` merge** | **intact** | the leaf compared the key sets of `deee9290:decisions.{en,de}.properties` with `origin/main`: the only keys missing are `decision.waiver.later` and `decisions.filter.label`, the two this plan retires; `decision.outcome.answered_elsewhere`, `decision.channel.*`, `decision.notify.several`, `decision.evidence.*`, `decisions.list.since`, `decisions.list.other_group` are all present; the English diff against `deee9290` is the two removals and a replaced comment. Nothing PLAN-51 shipped was lost |
| the lane's line numbers and "the rule guards `loadCatalogueFor`" | imprecise | the lane's `index.ts:543/651/741` are the pre-#478 tree (HEAD: `setLocale:547`, `adoptRecordedLocale:675`, `initI18n:734`); only `loadCatalogueFor`'s post-await result is guarded: the request still leaves when an adopting step reaches an uncached pack during a deferral |
| "no view calls `loadMessages(area)`" | borne out | the only caller is `initI18n` with `'all'` (`index.ts:770`); the only fetches `:498`, `:636`; the only client path `client.ts:3080` |
| the three CodeRabbit fixes | pinned | `282ab891` (the `loadMessages` post-await guard; the unknown-version cache), #476 (the switch path's post-await guard): "drops a response that resolves while the reload starts waiting", "switches to the held pack of an unknown runtime version instead of fetching", "keeps the shown language when a switch answer lands…" |
| the review-finding count | differs | the message has `review_body_findings_total=1`; the PR body counts 3 reviews with one inline thread and two body findings: reporting only |

Not shown by reading: the mutation logs, the gate runs, the screenshots, the merge-queue behaviour.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `SystemView.svelte:244-256` and `i18n/index.ts:64-71` (re-read: `window.location.reload()` returns normally while the old page keeps running, then `setReloadDeferred(false)` fires the subscriber, which calls `setLocale(pending)`) | a pending language choice and a reload that returns before navigation commits; the forced "Load again" button | the old code requests the newer runtime's pack, swaps `messagesStore` and `persistCatalogue` writes the new pack under the old version key; persists if the navigation is cancelled; the comment at `:253` assumes `reload()` does not return | minor |
| L3-2 | `i18n/index.ts:628-655` and `:560-578` (re-read: `loadCatalogueFor` returns `null` without setting `pendingLocale` or calling `rememberLocaleChoice`; `setLocale` then returns) | click "de", the upgrade event arrives before the answer | the language stays `en` and the stored choice stays `en`; the comment says "the choice is made again after the reload" but nothing records it; the spec says "the choice is kept and applied"; #476's test pins only that the language is kept | minor |
| L3-3 | `SystemView.svelte:145` | navigate away from the system view | the deferral is cleared on destroy, `runtime.upgraded` is handled only there, nothing will reload the page, and a later language switch fetches the newer pack on old code; deliberate and pinned ("releases the held catalogue when the system view goes"): a design residue | minor |
| L3-4 | `i18n/index.ts:636,466` (lead) | `adoptRecordedLocale`, `adoptRecordedLocaleNow` or `initI18n` during a deferral with an uncached pack | the request leaves; only the answer is dropped; `readCatalogueVersion` (`getStatus`) is unguarded too and could re-key the held pack; practically unreachable; unpinned | minor (lead) |
| L3-5 | `BottomBar.svelte:204,247`, `NavRail.svelte:188`, `i18n/index.ts:549-554` (lead) | a pending "de" with the shown "en" | the toggle always asks for "de" again: the supersede branch is unreachable from the UI; nothing outside `i18n/index.ts` and `SystemView` reads `reloadDeferred`, so no notice | minor (lead) |
| L3-6 | `DecisionCatalogueTest.sharedFilterLabel`, `DecisionView.test.ts` | English UI | the filter labels are title-cased from the catalogue title: "Filter Decisions", "Filter Reports", "Filter Running work", "Filter Ended work", "Filter Workstreams" (two mix case); the old labels were sentence case; German is fine; no German plural or gender problem in the shared act words | minor |
| L3-7 | the readers of `common.kind.*`, `common.filter.label` | revert of the reader | unpinned (L1-1, L1-2) | minor (test) |
| L3-8 | `onboard.link.later`, `onboard.link.back`, `web.frame.live.retry`, `project.chat.close`, `project.settings.save`, `control.settings.close`, `system.cleanup.confirm.back`, `start.action.cancel` and others | — | acts still named more than once: one-key-per-concept is incomplete for acts; the spec named only the listed keys, so not a miss (readers not checked) | minor |
| L3-9 | `index.test.ts` (the title of `it('lets a later choice…')`) and the Java catalogue tests | — | the first comment glued to the opening line; literal `what` values instead of `Generators` (the expected texts are literal, which is allowed) | nit |

**Layer 3 verdict:** findings — no blocker, no major; 8 minor, 1 nit, 2 of them leads.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Web | no `innerHTML`/`{@html}`; no timer waits; ES modules | conforms | none in the added lines (the leaf) |
| CLAUDE.md Dependencies | none added | conforms | no `pom.xml` or `package.json` in the file list |
| CLAUDE.md Java | JUnit 5 only, no Mockito/Hamcrest | conforms | the catalogue tests extended |
| spec Standing | text guard baseline only shrinks | conforms | untouched |
| spec Standing | the design document is updated in the same PR | partly | L1-5 |
| spec Standing | one key per concept, one word per value | partly | L3-8 |
| spec Write-Boundary | no write under `.plan/orchestrator/` | conforms | the lane states none; the leaf found no `.plan/orchestrator` path in the file list |

**Stale design lines:** `doc/design/web-app/view-density-and-filtering.adoc:63` ("_Filter decisions_"), `doc/design/web-app/view-layouts.adoc:18` ("[Filter reports]"); `doc/design/shared/project-explorer.adoc:161` says the Projects list filter is labelled through `common.filter.label` while that list has its own `project-filter-input` and no `FilterInput` (not this plan's). `texts-and-languages.adoc:181`, `plan-overview.adoc:176` and `components-tags.adoc:203,326` are accurate.

## Metrics and Anomalies

- Tokens `0` (the lane: the measured-subset total, nothing measured), `plugin_version` `0.1.2149`, executor `0.1.2149`. `landing-check`: `complete: true`; `surface_delta` `unmeasured`.
- Self-review: two rounds (9 findings, then 3 nits), all fixed.
- **The merge ran ahead of the last review read.** `ci pr merge-queue` answered `enqueued: indeterminate — auto_merge_armed_awaiting_checks`, so GitHub's auto-merge fired as soon as CI went green on `b1d4f4bc`; CodeRabbit's third review (21:14, one outside-diff finding) was not handled before the merge. The operator chose to land it as #476. For the plan-marshall repository: a lane must treat an `indeterminate` enqueue as an armed auto-merge, re-read the review surfaces after it, and not take the enqueue answer as the gate.
- Builds: all `daemon_longpoll`; two quality gates hit the daemon's supervisory bound (1018 s `learned`, then 1093 s `explicit`) and were re-submitted with 1093 s and 1500 s (green at 790 s and 1043 s); no `killed` job. One CI job on #476 (`EntryRecordsTest.runtimeEntryResourceRefusesAMalformedLegacyPort`, a generator seed in a module this plan does not touch) was re-run and passed: the flake PLAN-66 and PLAN-51 also recorded.
- The lane's rule breaks, by its own count: shell `ls`, `cat`, `grep -n`, `cp` (8), `rm` (2) and a redirect, none changing a repository file outside `.plan/temp`; reads of the daemon's job logs outside the repository; the executor file was regenerated during the day (0.1.2135 → 0.1.2149) by something outside the run; no guard enforced.
- The screenshot matrix used a textarea appended through the page's own JS to reach the deferred-reload state and read the language toggle from the network panel: live evidence, not a substitute for the unit tests (the lane says so).
- Operator questions (two): wait 90 minutes for CodeRabbit's quota, then re-trigger; land the post-merge finding as a follow-up now.

## Routing and Merge Behavior

- Review: CodeRabbit (required, quota-limited, `review_complete_after_handling`): refused on quota at 2026-11-17T16:26Z, re-triggered after the 90-minute window, reviewed `d6a4f41e` (1 inline plus 1 outside-diff, fixed in `282ab891`), `b1d4f4bc` (1 outside-diff, fixed in #476) and `6c37bb8b` on #476 (no findings); re-read: `review-gate --pr-number 468 --required-reviewer coderabbit` shows `unresolved_review_threads=0`, `review_body_findings_total=1` (its `handled:0` is the gate lacking `--disposition`); #476: 0 threads, 0 findings. Sourcery refused on quota twice, optional.
- Sonar: `sonar_open_new_issues=0`; the Sonar MCP search for pull requests 468 and 476 returns no issues.
- Merged through the merge queue (auto-merge armed, see above).

## Reconciliation Actions

- [x] row `status` → `shipped` — `queue --transition PLAN-152 --status shipped`
- [x] row `pr` stamped — `468`
- [x] row `landing` stamped — `landings/PLAN-152.md`
- [x] row `plan_marshall_plan_id` stamped — `rb-152-web-foundation-catalogue-cleanup`
- [x] epic.md: Open Defect `PLAN-152 landed`
- [x] resume anchor and `queue-view.md`
- [x] the inbox message archived on consume

## Follow-Ups

- **A PLAN-152 residue plan** (named, NOT staged, small): the deferral's release order (L3-1), the in-flight choice (L3-2), the unmount (L3-3), the toggle's feedback (L3-5), pins for the kinds reader and the three filter labels (L1-1, L1-2), the title-cased English filter labels and the two stale design lines, the remaining duplicate act keys (L3-8). The English filter labels and the two design lines are trivial-change candidates.
- `PLAN-152`'s spec: its Expected Surface owes `pm-web-app/src/lib/i18n/index.ts` and `pm-web-app/src/app.html` (the amendment's instruction); a shipped spec is not edited further.
- **For the plan-marshall repository** (named, unverified here): an `indeterminate` merge-queue enqueue (`auto_merge_armed_awaiting_checks`) is an armed auto-merge that races the last review read; the lane has no rule for it.
- PLAN-51 and PLAN-152 overlapped four files; PLAN-51's keys all survived the merge (re-read above).
