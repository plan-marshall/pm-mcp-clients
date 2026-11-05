envelope_version=1
sender_type=orchestrator
sender_id=orchestrator
epic=ui-improvements
kind=finding
created=2026-11-05T06:37:48Z

# Decision for PLAN-93 deliverable 5 (e): catalogue rollback retention

The operator decided on 2026-11-05: do NOT build the retention of older catalogues for rollback (the optional item of CodeRabbit's finding on PR 291, `i18n/index.ts` ~317-318).

Reason to record in your progress record and answer in the pull request: `persistCatalogue` keeps one catalogue per locale on purpose (its docblock: "a browser keeps one catalogue per locale and never grows a library of stale packs"). The catalogue is keyed by the runtime version, and a missing cached entry is fetched from the runtime, so retention would only help when the runtime is unreachable; it costs storage growth, an eviction policy and tests for both.

Before you answer, confirm in the specification that no rule asks the browser to keep older catalogues. If one does, say so in the progress record and report it instead of answering with the reason.

The other items of deliverable 5 are unchanged.
