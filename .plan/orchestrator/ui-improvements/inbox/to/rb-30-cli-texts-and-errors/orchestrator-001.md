envelope_version=1
sender_type=orchestrator
sender_id=orchestrator
epic=ui-improvements
kind=finding
created=2026-11-14T11:30:12Z

# Orchestrator → PLAN-30: two items from PR #432 (analyze-fix-build-time), operator decisions 2026-11-14

PR #432 rebased onto main after #422–#429 and merged two designs in your surface. Fold both into your current work
(D2/D9 for the first, D8 for the second); re-read the files at HEAD first.

1. **`answer`: restore the fail-closed first read (operator: "Restore fail-closed read").** After #432 a failed first
   read in `AnswerCommand` is an `OperatorRefusedException` that shows the HTTP status, so
   `operator.decision.parts_unreadable` is unreachable while `DecisionCatalogueTest` still requires it. A failed or
   mismatched first read (no decision returned, or one with another id) prints `operator.decision.parts_unreadable`
   and exits 1, with no HTTP status on screen, through the same refusal path as D2. Keep #432's locale-keyed keys and
   read-before-send; EOF at any prompt still leaves with `Left Q-…` and exit 0. Test-first: a failed first read and a
   mismatched id each print the line and exit 1.
2. **Cleanup: one catalogue for one key.** `operator.cleanup.not_in_job` exists in `system.*.properties` (`{id}`) and
   in `operator.*.properties` (`{item}`); the code reads the second. Delete the stale `system.*` copy (and any test
   pinning it). `--exclude` without `--delete` now exits 2 for every form (one guard, from main): confirm it against
   `doc/design/terminal-client/26-cleanup.adoc#t-remind` and say in the pull request which way the document reads.
