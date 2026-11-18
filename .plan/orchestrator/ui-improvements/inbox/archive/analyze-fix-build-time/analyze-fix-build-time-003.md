envelope_version=1
sender_type=plan
sender_id=analyze-fix-build-time
epic=ui-improvements
kind=finding
created=2026-11-14T20:10:04Z

# Deferred from the review of #436 (pm-web-app)

Follows message 002. Items deferred on purpose when the web part of analyze-fix-build-time landed.

1. **Refresh-event sets derived from one definition.** `ProjectExplorerView` selects three project event
   names and `StartView` keeps `ENTRY_REFRESH_EVENTS` beside `PROJECT_EVENTS` in `stores/runtime.ts` and the
   `EventName` catalogue of pm-api; the tests repeat the lists, so a new event leaves them green. Export one
   authoritative set per stream and use it in the predicate and the parameterized test
   (`PlanView.test.ts` has the same table).
2. **`sonar.coverage.exclusions=**/*` in `pm-web-app/pom.xml`.** It predates the split and excludes every web
   source from Sonar coverage, so the LCOV report path beside it contributes nothing. Removing it puts the
   web app under the new-code coverage condition of the gate: decide the threshold first, then narrow or
   drop the exclusion.
