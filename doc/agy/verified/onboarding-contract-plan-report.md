# Completion Report: `onboarding-contract` (Onboarding: Contract and Mock Runtime)

Companion to the execution plan [`onboarding-contract-plan.md`](implemented/onboarding-contract-plan.md),
worked under [`doc/agy/runbook.md`](runbook.md).
Branch: `feat/onboarding-contract`; worktree: `/Users/oliver/git/worktrees/pm-mcp-clients-onboarding-contract`.

---

## 1. Executive Summary

This work package completes the client-side contract of machine onboarding and machine settings
(`pm-api`, package `de.planmarshall.api.onboarding`) and backs the remaining cases with scenarios in
`pm-mock-runtime` (`UC-ONB-1` to `UC-ONB-10`). The onboarding state machine, the closed list of
consent kinds, the harness scan, the model catalogue, the role assignment, the plan defaults, and the
revision-guarded machine settings already existed from an earlier package; this package delivers the
remainder the work package ([`doc/plans/onboarding-contract.adoc`](../plans/implemented/onboarding-contract.adoc)) names:

- **Tokens, not money — the harness usage report.** The harness scan now carries, per harness and
  version, the figures it reports (`input`, `cache_read`, `cache_write`, `output`, `reasoning`), each
  `reported`, `not_applicable`, or `not_reported`, behind the `complete` / `partial` / `not_reported`
  summary. A figure the profile declares not applicable does not make the report partial, and no
  figure is ever a price.
- **The unknown harness's verification.** The nine stages a draft profile is run through
  (`identity` to `usage`) with their status, and the `usage` stage's result as a usage report.
- **Automatic onboarding.** The closed list of reasons of `auto.result` (`detected`, `all_supported`,
  `default`, `asked`) and the item schema of `auto.result` (aspect, value, reason).
- **Machine settings — the credential rotated or replaced, and the conflict.** The mock runtime now
  rotates a credential (dropping its old expiry), and a dedicated scenario shows the documents mode
  changed in two clients at once: the second change is refused with `conflict` and applied again on
  the current revision.
- **The case scenarios** the use cases name: each consent answered no, every chosen harness failing,
  the unknown harness, and the harness complete, partial, and not reporting its tokens.

The result is that every record and closed list of the package has a round-trip or verbatim
comparison test, and every named case scenario loads, is served by the mock runtime, and is asserted
in a test of the representation a client receives.

---

## 2. Delivered Artifacts

### 2.1 `pm-api` — contracts (`de.planmarshall.api.onboarding`)

New types (all with `jackson-core` streaming-style tree codecs, no `jackson-databind`):

| File | Role |
|---|---|
| `AutoResultReason.java` | The closed list of the automatic run's reasons; the title key of each (`onboard.auto.reason.*`) |
| `AutoResultItem.java` | One item of `auto.result` (aspect, value, reason), with a list codec |
| `UsageFigure.java` | The closed list of the token-usage figures of a run |
| `UsageFigureStatus.java` | The closed list of what a harness reports of a figure (`reported`, `not_applicable`, `not_reported`) |
| `HarnessUsageReport.java` | The per-figure usage report behind the coverage summary, held consistent and fail-closed |
| `HarnessVerificationStage.java` | The closed list of the nine stages of a draft-profile verification |
| `VerificationStageStatus.java` | The closed list of a stage's status (`passed`, `failed`, `not_run`) |
| `HarnessVerification.java` | The verification of a draft profile: the stages and the `usage` stage's report |

Modified:

| File | Change |
|---|---|
| `HarnessScanResult.java` | Carries an optional `usage` report of the harness and version (additive) |
| `package-info.java` | Names the usage report, the verification, and the automatic run's reasons and items |

### 2.2 `pm-api` — tests

| File | Coverage |
|---|---|
| `OnboardingRecordsTest.java` | Round-trip tests of the new records and closed lists (`AutoResultItem`/`AutoResultReason`, `HarnessUsageReport`/`UsageFigure`/`UsageFigureStatus`, `HarnessVerification`/`HarnessVerificationStage`/`VerificationStageStatus`, `HarnessScanResult` with usage) |
| `OnboardingCataloguesTest.java` | Verbatim comparison of every closed list with the specification (consent kinds, token-usage values and figures, the reasons of `auto.result`, the landing variants, the seven aspects, the verification stages, the documents modes) |

### 2.3 `pm-mock-runtime` — scenarios

| File | Case |
|---|---|
| `onboarding-harness-tokens-complete.json` | A harness and version reporting its tokens completely |
| `onboarding-harness-tokens-partial.json` | A harness reporting its tokens only in part, naming the missing figures |
| `onboarding-harness-tokens-not-reported.json` | A harness reporting nothing of its tokens |
| `onboarding-harness-all-failed.json` | Every chosen harness failing its check; the step comes to the none-found place |
| `onboarding-refusals.json` | Each consent answered no: a required one stays, an optional one goes on without it |
| `onboarding-unknown-harness.json` | A harness the binary does not know, added and verified, its `usage` stage failing on a named figure |
| `onboarding-settings-conflict.json` | The documents mode changed in two clients; the second change refused with `conflict` and applied again |

Registered in `StandardScenarios` (constants and `ALL`).

### 2.4 `pm-mock-runtime` — runtime and tests

| File | Change |
|---|---|
| `MockRuntimeEngine.java` | `credential:<key>` rotates a kept credential (new kind, old expiry dropped) as a revision-guarded settings change |
| `OnboardingScenarioTest.java` | A `Case scenarios` suite that asserts each new scenario's representation (the three token reports, the all-failed fallback, the refusals, the unknown harness and its verification, the settings conflict), plus the automatic run's result items and a credential rotation |

---

## 3. Test & Verification Results

| Suite | Result |
|---|---|
| `pm-api` unit tests (`./mvnw -pl pm-api test`) | **green** (incl. the new `OnboardingRecordsTest` cases and `OnboardingCataloguesTest`) |
| `pm-mock-runtime` unit tests (`./mvnw -pl pm-mock-runtime test`) | **230 tests, 0 failures** (incl. the `Case scenarios` suite) |
| `./mvnw verify -Ppre-commit` | see §5 |

Every new scenario is loaded through `ScenarioLoader` (with its dangling-link and operator-state
validation) and served by the mock runtime; `StandardScenariosTest` proves `ALL` lists exactly the
declared constants.

---

## 4. Domain Invariants

- **Java 25, records, package-private where possible** — every new type is an immutable record or a
  closed enum; `pm-api` still uses only `jackson-core` via `JsonTree`, proven by the `jackson-core-only`
  enforcer rule.
- **Closed lists fail closed** — an unknown consent kind, figure, figure status, reason, stage, or
  stage status is refused (`fromWire` returns `null`, `parse` throws), and the usage report refuses an
  inconsistent summary.
- **Tokens, not money** — the usage report carries counts of tokens only; `missingFigures()` derives
  the named missing figures, and no cost or price appears anywhere.
- **Revision-guarded settings** — a change against an older revision is refused with `conflict` and
  the current value, never applied.
- **Mock runtime completeness** — the new scenarios load and are served; a test per scenario asserts
  the representation a client receives.
- **No uncatalogued user-facing text** — no new message key or prose string is introduced; the
  `auto.result` reasons already had their `onboard.auto.reason.*` keys, verified by
  `OnboardingCatalogueTest`.

---

## 5. Build & Quality Evidence

```
$ ./mvnw verify -Ppre-commit
[INFO] Reactor Summary for pm-mcp-clients 0.1.0-SNAPSHOT:
[INFO] pm-api ............................................. SUCCESS [01:00 min]
[INFO] pm-exec ............................................ SUCCESS [ 12.408 s]
[INFO] pm-relay ........................................... SUCCESS [ 13.544 s]
[INFO] pm-mock-runtime .................................... SUCCESS [ 40.293 s]
[INFO] pm-operator ........................................ SUCCESS [01:06 min]
[INFO] pm-web-app ......................................... SUCCESS [ 41.008 s]
[INFO] pm-web-e2e ........................................ SUCCESS [ 12.727 s]
[INFO] BUILD SUCCESS
```

No OpenRewrite, license-header, or import-order rewrite was produced, so the committed state is the
verified state. On the pull request, the Maven build (JDK 25 and 26), the integration-tests suite,
the `cuioss-review-bot` review, and the SonarCloud analysis all pass.

---

## 6. Notes, Deviations, and Follow-ups

- **Class names.** The plan sketch (`ConsentResource`, `HarnessUsageResource`, `AutoResultResource`, …)
  names parallel `*Resource` types that would duplicate the package's existing records
  (`ConsentRecord`, `HarnessScanResult`, `MachineSettingsRecord`, …). Rather than add a second form of
  what exists, this package extends the existing naming scheme: `HarnessUsageReport`,
  `HarnessVerification`, `AutoResultItem`. The plan's intent — the usage report, the verification's
  `usage` stage, and the `auto.result` item schema — is delivered in full.
- **The operator representation.** The onboarding scenarios still use the runtime's legacy
  representation (`state` as a string, links as a map), as do every scenario but `operator-interaction`.
  Migrating the scenario corpus to `OperatorStateResource` is a contract-wide effort of its own and is
  out of this package's slice.
- **Credential rotation.** The mock runtime rotates a credential through the settings key
  `credential:<key>` (no secret is stored in the mock); the verification that follows the rotation is
  covered by the providers' verification states, already present.
- The completion report stays at `doc/agy/onboarding-contract-plan-report.md`; moving it to
  `doc/agy/verified/` is the orchestrator's job in Step 8 after the change lands on `main`.
