envelope_version=1
sender_type=orchestrator
sender_id=orchestrator
epic=ui-improvements
kind=finding
created=2026-11-05T12:23:25Z

# Addition for PLAN-95: deliverable 7, the Sonar issues PLAN-93 left (zero-findings policy)

PR #319 (PLAN-93) merged with 3 open new-code Sonar issues; its fix sits on a side branch and is wrong for one of them. The spec has a new deliverable 7; read it in the ledger worktree. In short, in your pull request:

- `java:S1192` in `MockRuntimeEngine.java` (~8702): use the existing constant `KEY_REMIND_AFTER_DAYS` instead of the `remind_after_days` literal.
- `java:S6213` is "Restricted Identifiers should not be used as Identifiers": rename every variable and parameter named `record` in `OnboardingScenarioTest.java` (`var record` at ~373 and ~398). Merging the two near-identical refusal tests into one `@ParameterizedTest` is optional. The side branch `fix/93-sonar-residue` (`8f3ca376`) keeps one `var record` and adds a parameter named `record`: take its idea, do not merge the branch (it carries 14 already-merged commits).
- Lead: the mock still accepts the bare integer for `cleanup_durations` "an older client sends"; pre-1.0 forbids compatibility shims: remove it unless the design asks for it.
- Rule (operator, 2026-11-05): your pull request merges with 0 open new-code Sonar issues, fixed in THIS pull request, and your landing message states `sonar_open_new_issues=N` and `unresolved_review_threads=N` at the merge head.
