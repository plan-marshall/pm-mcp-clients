# Landing Analysis: PLAN-49 — The attention inbox as the start page

epic: ui-improvements
workstream: WS-03
pr: #461 (https://github.com/plan-marshall/pm-mcp-clients/pull/461)

> Landing record, written by `analyze` on 2026-11-17 from the inbox message
> `rb-49-web-attention-inbox-001.md` (a lead, not a fact), git, the provider, and a read-only three-layer leaf.
> The orchestrator re-read the points marked "re-read" on `e4ecd4d0`.

Merged as `e4ecd4d08c1f583374c50bb672f090ce0a56653c`, 41 files, +2373/-128. Sender `rb-49-web-attention-inbox`. Lane: runbook. Harness `opencode`, model `deepseek-v4.1-flash/high` (as stated, not corroborated).

## Deliverable Fidelity vs Spec

| Deliverable (spec, amended) | Verdict | Evidence |
|-----------------------------|---------|----------|
| D1 — attention store and `getNeedsOperator` | shipped | `attention.ts` (520 lines), `client.ts:524` `GET /api/v1/machine/needs-operator?page=N`; `attention.test.ts` pins each kind's act, the served order, the wire spellings, the kind list against `NeedsOperatorKind.java` (13 values, all `overview.needs.<kind>` keys in en and de), a page without a total. `test-first` evidenced by a failing run at the base. One amended clause is moved: `client.test.ts` was to pin "Show more, overview.needs.more", which a client call test cannot; the lane pinned it in `InboxView.test.ts`, and only "button present when `hasOlder`" (no negative case, no click) |
| D2 — what waits at `/` | shipped; evidence order `mutation-after` | `routes.ts` `inbox` route, `FIXED_ROUTE_PATHS['/']`, `?lens=waits`; `InboxView.svelte`; `inbox.spec.js` on `running-work` (five tests) fails on the mutant per the lane's log. The Done-when "an answered decision leaves" is evidenced only by the mock moving to a state where another client resolved the items: no answer is given in the UI, so the local-answer path is untested (and hides L3-1). "Then oldest" ordering is asserted nowhere (the client sorts nothing). Browser Done-when partly met: no baseline capture, post-review shots not re-taken (the lane says why) |
| D3 — the shell shows what waits | shipped | rail entry `web.rail.needs` with the total, phone bar What waits · Running work · Decisions · More, favicon/title/rail on one figure; `nav.test.ts`, `NavRail.test.ts`, `favicon.test.ts`, `routes.test.ts`; `BottomBar.test.ts` covers the selected state and the More count only, not the needs count nor `open()` routing |

**Files the diff touches that the Expected Surface does not list:** `display/projectCounts.ts`, `rememberedPlace.*`, `history.test.ts`, `pageWidth.test.ts`, the theme-readability matrix, `RowLink.*` (the spec-ordered PLAN-148 residue; the `isAppPath` guard at `RowLink.svelte:29,44` is untouched) — the lane names them; also `OverviewView.svelte`, `layout.test.ts`, `column-window.spec.js`, `decisions.spec.js`, `showroom.spec.js`, `mobile.spec.js` (suite updates the shell change implies, one line in the PR body); and, at the operator's instruction (free text "Analyze both flakes and fix"), `pm-operator` `ConsoleNoticeIT.java` and `TuiScreenTest.java`: both replace a one-shot screen sample by a wait on the produced condition (no timer pass; CLAUDE.md's rule is under Web Standards and is respected by analogy); whether the reorder at `ConsoleNoticeIT:91` removes the flake is not shown by reading.

`text-guard-baseline.json` +4 lines: one registered view `InboxView` with empty `de` and `en` arrays; no finding grandfathered.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf per landing — reason: the Dispatch Decision Rule; 41 files. The orchestrator re-read the points below on `e4ecd4d0`.
- **Verdict:** layer 1 minor; layer 2 findings (test gaps); layer 3: no blocker, **no major**, 8 minor, several info.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | the original Done-whens run on `fully-onboarded` | checked | refutes | HYPOTHESIS 1 (`:62-63`) says that fixture serves no `needs-operator`; the unamended text stays beside the amendment that replaces it; the lane followed the amendment (`running-work`) |
| L1-2 | `client.test.ts` pins "Show more" | checked | refutes | a client call test cannot pin a key or a button |
| L1-3 | a header badge reads the same count | checked | refutes | no such component at the merge commit; harmless |
| L1-4 | the three HYPOTHESIS lines | checked | confirms | `fully-onboarded` has no route; `needs_operator.changed` handled (`runtime.ts:1090`); `web.rail.needs`, `web.inbox.title` added to `common.*` |
| L1-5 | the design document is updated | checked | refutes | `README.adoc` yes; `needs-and-find.adoc:26` ("What waits · 4") and `decisions.adoc:154` ("the tab title shows the open decisions") are stale |

### Layer 2 — Implemented

| Item | Verdict | Evidence |
|------|---------|----------|
| `attention.test.ts` first test | builds items by hand | the item path is exercised by the wire-spelling and page tests; the narrative's "7 tests" is 13 in the file |
| Show more | pinned positively only | `InboxView.test.ts:160`; `loadMore` covered at store level (`attention.test.ts:260`) |
| decisions e2e title assertion weakened | `/^\(\d+\) Decisions$/` → `/Decisions$/` (`decisions.spec.js:40`) | the non-inbox prefix rule (`+layout.svelte:305-309`) has no test that can fail |
| the PLAN-130-style catalogue claims | borne out | 13 kinds and `left`, `more`, `empty`, `since`, `count`, `title` keys in both languages; no orphan |
| the two `pm-operator` test changes | condition waits | see above; `TuiScreenTest` folds the no-residue check into the wait, so a transient residue now passes and the `assertFalse` at `:84` is redundant |

Not shown by reading: the failing-run logs under `.plan/local`, the screenshots (no baseline exists), Sonar beyond the MCP search, the runtime's paging beyond the last page.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `attention.ts:416-419,431-453` (re-read: `identityOf` keys on kind, link, project, epic, plan and `since`; `departureOf` has no way to tell a local answer from another client's); the text is `overview.needs.left` = "{kind} was resolved in another client" | the operator answers a decision in this app, or an item's `since` or `link` changes | the live region (`+layout.svelte:407-410`, sr-only) says it was "resolved in another client" when this client did it; a changed `since` counts as a leaver | minor |
| L3-2 | `runtime.ts:1229,1383` (re-read: `navigationEpoch += 1` only in `applyRoute`; the `popstate` handler `adoptRouteFromLocation` does not bump it) | click What waits or the brand with a summary read pending, press Back before the answer | the older answer passes both guards and moves the person forward again | minor |
| L3-3 | `runtime.ts:1090`, `client.ts` | the event stream drops and reconnects, a change in between | no catch-up read: list, rail count, tab title and icon stale until the next event or a remount (the decision count has the same gap) | minor |
| L3-4 | `attention.ts:110` | revoke (`runtime.ts:693-700`) | `resetAttention` is never called in production; the revoked page keeps its list, the title its count, the icon its dot | minor |
| L3-5 | `attention.ts:374,416` | a new blocking item shifts the page boundary, or two items of one kind without link and `since` | a later page is appended without dedupe: duplicate keys in the keyed `each` (`InboxView.svelte:215`); the set-based `leftOf` cannot see one of two identical items leave | minor |
| L3-6 | `attention.ts:479` | reading the already opened pages after the list shrank | one request per page by `Promise.all`; what the runtime answers beyond the last page is not in this repository: a 4xx fails the whole read, a clamp repeats rows; not one snapshot | minor, not shown by reading |
| L3-7 | `attention.ts:209,323-324` | every setup or project item | the act reads "Projects" with no argument; an item naming nothing targets `/` with the act "What waits", which re-pushes the page the person is on (a no-op button with the same accessible name as its siblings) | minor |
| L3-8 | `NavRail.svelte:158` (re-read: `aria-label` on a plain `<span>`; same in `BottomBar.svelte:140,164`) | screen reader | the count's text is `overview.needs.count` = "! {count}", a name on a generic element that is not reliably exposed and says nothing in words | minor |
| L3-9 | `overview.en.properties:39`; the `de` text | the kind words "needs a fix", "another reason" | "needs a fix was resolved in another client" reads wrongly; the German text ("steht wurde …") the same | minor (wording) |
| L3-10 | `InboxView.svelte:202` | — | the empty-state project link is built without `encodeURIComponent` while `attention.ts` encodes every id; slugs, so no exploit visible | info |
| L3-11 | the store, the route model, `rememberedPlace`, `BottomBar`, `InboxView` | — | confirmed: page handling consistent (page 1 replaces, later pages append, only the newest read writes), the complete-read departure rule as designed, the start-rule intent for the rail paths, `/` as a route sound, More's selected state, error paths keep rows and retry, no `innerHTML` or `{@html}`, no subscription to release | info |

**Layer 3 verdict:** findings — no blocker, no major; 9 minor, 2 info.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Web | no `innerHTML`/`{@html}`; no timer waits; ES modules | conforms | no hit in the added lines |
| CLAUDE.md Dependencies | none added | conforms | no `pom.xml` or `package.json` in the diff |
| CLAUDE.md Java | no Mockito/Hamcrest, no generic exception, JUnit 5 | conforms | the two `pm-operator` diffs add a JUnit poll helper only |
| spec Standing | the design document is updated in the same PR | deviates | L1-5 |
| spec Standing | `RowLink` `isAppPath` guard intact; v2 components only | conforms | `RowLink.svelte:29,44` |
| spec Standing | text guard baseline only shrinks | conforms | one registered view, no finding |
| spec Standing | values through the display layer | not shown by reading | plan, epic and project titles are rendered as text directly |
| spec Write-Boundary | no write under `.plan/orchestrator/` | conforms | re-read: `git show --stat e4ecd4d0 -- .plan` is empty |

## Metrics and Anomalies

- Tokens `unknown`; `plugin_version` `unknown`; executor `0.1.2135`. `landing-check`: `complete: false`, `missing_keys` `total_tokens`, `plugin_version`.
- The merge queue dequeued #461 twice (`ConsoleNoticeIT.decisionOpenedNotice`, then `TuiScreenTest.levelChangeLeavesNoResidue`); the operator answered "re-enqueue", then "Analyze both flakes and fix"; the third attempt merged.
- Builds: six daemon-routed, twelve in-process; the routed e2e hit the daemon's learned bound (600 s) and then failed at the Playwright step (an unset ports environment in the daemon): a defect for the plan-marshall repository, not read here.
- The lane used `grep`, `sed`, `tail`, `wc` in the shell a few times and one `rm -f`; the browser pane was shared with another session and capturing stopped.
- The mailbox read address with a digit-leading plan id is refused (`invalid_target_plan`): the fourth lane to report it.

## Routing and Merge Behavior

- Review: CodeRabbit, three reviews, 15 findings (13 fixed in `7845d71b`, the rest in `5699ca0a`, `f80f9b14`); re-read: `review-gate --pr-number 461 --required-reviewer coderabbit` shows `unresolved_review_threads=0`, `review_body_findings_total=15` (its `handled:0` is the gate lacking `--disposition`). Conforms to the standing rule.
- Sonar: `sonar_open_new_issues=0`; the Sonar MCP search for pull request 461 returns no issues; three rounds (2 → 3 → 1 → 0) fixed by the lane.
- `.plan`: #461 carries no file under `.plan` (re-read).

## Reconciliation Actions

- [x] row `status` → `shipped` — `queue --transition PLAN-49 --status shipped`
- [x] row `pr` stamped — `461`
- [x] row `landing` stamped — `landings/PLAN-49.md`
- [x] row `plan_marshall_plan_id` stamped — `rb-49-web-attention-inbox`
- [x] epic.md: Open Defect `PLAN-49 landed`
- [x] resume anchor and `queue-view.md`
- [x] the inbox message archived on consume

## Follow-Ups

- **A PLAN-49 residue plan** (named, NOT staged): the local-answer notice (L3-1), the Back race (L3-2), a catch-up read on reconnect (L3-3), `resetAttention` on revoke (L3-4), dedupe of later pages (L3-5), distinct acts for setup and project items (L3-7), the count's accessible name (L3-8), the notice wording in both languages (L3-9), `needs-and-find.adoc:26` and `decisions.adoc:154`, the tests (negative Show more, the bar's needs count and routing, the non-inbox title prefix, "then oldest").
- PLAN-51 and PLAN-152 lose their overlap with PLAN-49 (still four files each with the running PLAN-52); PLAN-85 loses its `BottomBar` overlap with PLAN-49 but waits for PLAN-144.
- The two `pm-operator` timing tests: a re-run of `ConsoleNoticeIT` is the only proof the reorder removes the flake.
