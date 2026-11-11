envelope_version=1
sender_type=orchestrator
sender_id=orchestrator
epic=ui-improvements
kind=finding
created=2026-11-11T08:44:28Z

# Advisory for PLAN-158: the residue of PLAN-129 in ReportsView (operator decision 2026-11-11)

PLAN-129 merged as #406 (`d1a9e63a`) while you run. Its landing analysis (`landings/PLAN-129.md` of the epic) left the
residue below, all minor, in files you already change. The operator routed it to you. Re-read every line at your HEAD
(you will have merged `d1a9e63a`); fix what is real, and name in your landing message what you fixed and what you did
not (with the reason). Each item needs a test that fails before the fix.

## In `pm-web-app/src/lib/views/ReportsView.svelte` (lines at `d1a9e63a`)

1. **A failed single Mark read forgets a counted read** (`markRead` catch, `:506-510`): with `retain`, a `report.read` for
   the row arriving while the request is pending counts it read, and the failure then deletes `retainedReadIds[item.id]`
   whatever `countedReadIds` says, so the row returns to unread although the runtime read it. Apply the rule PLAN-129
   gave mark-all (`:577`: a counted read stays read). `doc/design/shared/reports.adoc#read` says "a single Mark read that
   fails returns its row to unread": make the text say the counted-read exception.
2. **A counted-read row can outlive its detail** (`flushRetainedReads` `:243-246` skips pending ids; mark-all's catch
   keeps the marker at `:577`; `finally` clears `pendingReadIds` at `:585-586`): if the detail closes while a mark-all
   runs and the act fails, nothing drops that row afterwards. Flush the retained rows once the request settles.
3. **No response-order guard in `loadReports`** (`:389-432`): a reload in flight before the POST can answer after the
   post-success reload (`:572`) and put the older page back. Ignore a response older than the latest request.
4. **`pendingReadIds` is a Set** (`:585-588`): mark-all's `finally` clears ids a concurrent single `markRead` still holds.
   Count per id, or keep the two pending sets apart.
5. **A mark-all that empties the list draws the Skeleton** (`:568` sets the list empty, `:572` reloads, so `:394`/`:946`
   draw it): add the test PLAN-129's spec asked for (no Skeleton on a mark-all reload), and keep the empty state instead.

## Tests

6. `ReportsView.test.ts:2056-2108` ("keeps a reload in flight before the mark-all response…") rejects the request, so
   the success path (the response must not subtract a read the reload already counted) is never run: add the success arm.
7. `RowMenu.test.ts:553-563`: the `scrollTop` assertions cannot fail in jsdom; keep the `maxBlockSize`/class assertions
   and drop the vacuous ones, or assert what jsdom can observe.
8. `ReportsCommandTest.java:121-164`: no negative assertion for the older-page hint (`ReportsCommand.java:245`): assert
   that `reports read --all` with `has_older=false` prints no hint. The class takes hand-written input and has no
   `@EnableGeneratorController` (CLAUDE.md): draw the ids and counts from `Generators`.

Files beyond your declared surface: `pm-web-app/src/lib/components/RowMenu.test.ts`,
`pm-operator/src/main/java/de/planmarshall/operator/ReportsCommand.java`,
`pm-operator/src/test/java/de/planmarshall/operator/ReportsCommandTest.java`, `doc/design/shared/reports.adoc` — name them
in your landing as added scope.
