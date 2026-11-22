envelope_version=1
sender_type=plan
sender_id=plan-175-sonar-standing-debt
epic=ui-improvements
kind=candidate-lesson
created=2026-11-22T14:35:28Z

## Candidate lesson: plan the pull-request split at outline time when the diff will exceed the review bot's file limit

Source: Q-Gate finding 79fbc5 context and the finalize decision records of PLAN-175 (operator PR cut).

What happened: the outline put 8 deliverables on one branch. The whole branch reached 154 files, above the 100-file limit beyond which CodeRabbit does not review. At finalize the operator cut the branch into three PRs (P1 = pm-api production code, 86 files, the one this run finalized; P2 = pm-api tests, Awaitility, JsonTree, stacked on P1; P3 = mock-runtime, web-app, relay, exec, independent). Consequences: seven deliverables (D2 to D8) had to be retired from the functional review by recorded operator decisions (fourteen criteria-integrity retirement records), the PR title had to be re-derived against the cut scope, P2 and P3 are opened outside this run, and the full history lives on a local backup branch.

Suggested rule: when an outline's expected surface is large (here scope_estimate multi_module, 178 distinct paths at init), plan the PR split at outline time — one plan or one PR per independently reviewable group of deliverables, each under the review bot's file limit — rather than discovering the limit at finalize. This matches the project memory note on splitting PRs above 100 files, which applied here but was not acted on until finalize.

Evidence: decision log of plan-175-sonar-standing-debt, entries f44d82 (PR cut), 17ea05 to c1bcd4 (retirements), 8bc18d (PR title re-derived), 382a7b (scope estimate at init).
