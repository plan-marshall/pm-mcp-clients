envelope_version=1
sender_type=plan
sender_id=rb-66-web-onboarding-frame-and-roles
epic=ui-improvements
kind=landing
created=2026-11-18T20:15:39Z

```landing-facts
schema=landing-facts/1
plan_id=rb-66-web-onboarding-frame-and-roles
epic=ui-improvements
pr=#485
merge_state=merged
cleanup_owed=false
deliverables_total=10
deliverables_done=10
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2149
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=unknown
review_body_findings_total=unknown
review_body_findings_handled=unknown
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```
guard: none on this harness (unenforced)

## 1. Deliverables

Per Emit Amendment 2026-11-18, PLAN-66 scope was partitioned across three PRs: Deliverables 1–4 merged in PR #467 (`735318df`), Deliverables 5–7 merged in PR #478 (`98ca84d0`), and Deliverables 8–10 delivered in PR #485 (`321985ac` on `main`).

- **Deliverable 1 — Frame**: met in PR #467 (`735318df`).
- **Deliverable 2 — defaults.review**: met in PR #467 (`735318df`).
- **Deliverable 3 — roles.review**: met in PR #467 (`735318df`).
- **Deliverable 4 — providers.failed**: met in PR #467 (`735318df`).
- **Deliverable 5 — harness.none**: met in PR #478 (`98ca84d0`).
- **Deliverable 6 — auto.result**: met in PR #478 (`98ca84d0`).
- **Deliverable 7 — language.choose**: met in PR #478 (`98ca84d0`).
- **Deliverable 8 — `catalogue.select`**: met.
  - Done-when: Recommendation preselected and marked `"recommended"` (`<Marker kind="recommended" />`) with reason inline; manual `TextField` per harness that cannot list its models.
  - Test name: `OnboardingView.scenarios.test.ts` (`onboarding scenario 33: catalogue.select advice and recommendation`, `scenario 34: catalogue.select manual entry for cannot-list harness`).
  - Order: `test-first`. Failing run commit `fee07a5f`, output ID `maven-2026-11-18-080330.log` (job `6264d60bf43647b498efcd84cd7a7c02`).
  - Commits: `fee07a5f`, `2fbb20d1`, `446470f4`, `9d0b5607`, `046f21bb`, `659b94b5`.
- **Deliverable 9 — `store.choose`**: met.
  - Done-when: Per option two lines: `"Protects a secret against: …"` and `"Does not protect against: …"`.
  - Test name: `OnboardingView.scenarios.test.ts` (`scenario 35: store.choose protection claims and availability`) and `AspectChoice.test.ts:30`.
  - Order: `test-first`. Failing run commit `fee07a5f`, output ID `maven-2026-11-18-085103.log` (job `4761c719df4c40d2875bea5933ad13c5`).
  - Commits: `fee07a5f`, `2fbb20d1`, `446470f4`, `9d0b5607`, `046f21bb`, `659b94b5`.
- **Deliverable 10 — `harness.choose`**: met.
  - Done-when: Monogram when no icon is fetched (stray duplicate `/>` removed); token line says `"usage complete"`, `"usage partial"`, or `"usage not reported"`.
  - Test name: `OnboardingView.scenarios.test.ts` (`scenario 36: harness.choose token usage format and monogram fallback`) and `HarnessChoice.test.ts:43`.
  - Order: `test-first`. Failing run commit `fee07a5f`, output ID `maven-2026-11-18-085612.log` (job `ac1dac28642c4b0dae24aa6845072839`).
  - Commits: `fee07a5f`, `2fbb20d1`, `446470f4`, `9d0b5607`, `046f21bb`, `659b94b5`.

Per-head test runs against pushed heads:
- `9d0b5607`: `verify -Ppre-commit` passed (`5245` Surefire + `169` Vitest files / `2997` tests), `verify` passed (`5245` tests), `pm-web-e2e` passed (`456` passed, `75` skipped).
- `046f21bb`: `verify -Ppre-commit` passed (`5245` Surefire + `169` Vitest files / `3003` tests), `verify` passed (`5245` tests), `pm-web-e2e` passed (`456` passed, `75` skipped).
- `07e48541`: `verify -Ppre-commit` passed (`5248` tests), `verify` passed (`5248` tests), `pm-web-e2e` passed (`456` passed, `75` skipped).
- `659b94b5`: `verify -Ppre-commit` passed (`5248` tests, 958s), `verify` passed (`5248` tests, 601s), `pm-web-e2e` passed (`881s`).
- `e52d0ba3`: `verify -Ppre-commit` passed (`5248` tests, 915s), `verify` passed (`5248` tests, 727s).

## 2. Personas and Skills

- **Personas applied**:
  - Setup: `plan-marshall:persona-plan-marshall-agent`
  - Implementation & unit tests (D8–D10): `plan-marshall:persona-implementer`, `plan-marshall:persona-module-tester`
  - E2E and visual browser checks: `plan-marshall:persona-integration-tester`
  - Self-review rounds 1–4 & PR comment triage: `plan-marshall:persona-code-reviewer`
  - Documentation updates: `plan-marshall:persona-documenter`
  - Security copy and trust boundaries: `plan-marshall:persona-security-expert`
- **Domain skills**:
  - Declared: `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`, `pm-dev-frontend:javascript-security`
  - Skills added: none.

## 3. Hypotheses

- **D8 Hypothesis (`ModelsChoice.svelte` / `catalogue.select`)**: CONFIRMED (`ModelsChoice.svelte:59-180`). Preselected all models unconditionally instead of only recommendations, lacked `<Marker kind="recommended" />`, showed no reason, and lacked `TextField` for manual input when harness cannot list models.
- **D9 Hypothesis (`AspectChoice.svelte` / `store.choose`)**: CONFIRMED (`AspectChoice.svelte:48-158`). Rendered store choices without the two required protection lines. Added `"Protects a secret against: …"` and `"Does not protect against: …"` backed by i18n properties and fallback defaults.
- **D10 Hypothesis (`HarnessChoice.svelte` / `harness.choose`)**: CONFIRMED (`HarnessChoice.svelte:94-159`). Stray duplicate `/>` tag on icon element removed; token summary format aligned to `"usage complete"`, `"usage partial"`, or `"usage not reported"`.

## 4. Findings

- **Self-review Round 1** (`2d049d8f`): 7 findings (duplicate re-add/sole-model group preservation, German formal `Sie`/`Ihr` -> `du`/`dein`, store fallback/button disable, `token_detail` gating). Fixed in `2fbb20d1`.
- **Self-review Round 2** (`cb346030`): 6 findings (localize partial missing figures, frozen empty array, recommendation filtering guard, mobile button wrap, doc wireframes). Fixed in `446470f4`.
- **Self-review Round 3** (`c20d6b35`): 2 test findings (`svelte-check` store type, token figure assertions). Fixed in `9d0b5607`.
- **Self-review Round 4** (`a502c491`): CLEAN.
- **CodeRabbit Review 1 (`PRR_kwDOVBOtss8AAAABYwfO9A`)**: 8 inline threads + 1 minor + 1 nitpick. Fixed in `046f21bb`, all threads resolved, summary comment posted.
- **CodeRabbit Review 2 (`PRR_kwDOVBOtss8AAAABYyyl0Q`)**: 4 inline threads + 2 outside-diff + 3 nitpicks. Fixed in `659b94b5`, all threads resolved, summary comment posted.
- **CI / Build findings**: GitHub Actions Maven Central transient 429 rate limit in run `44348785560`; rerun succeeded cleanly in run `44348785443` and merge run `44367638939`.

## 5. Reviewers

- **`coderabbitai`** (required, quota-limited: true, review_complete_after_handling: true):
  - Qualifying reviews on record: `PRR_kwDOVBOtss8AAAABYwfO9A` on `9d0b5607` and `PRR_kwDOVBOtss8AAAABYyyl0Q` on `07e48541`.
  - All 12 inline review threads replied to and resolved.
  - All body comments addressed and summarized in PR comments `IC_kwDOVBOtss8AAAABoA1JAQ` and `IC_kwDOVBOtss8AAAABoFT1vA`.
- **`sourcery-ai`** (optional): weekly quota limit reached notice posted (`IC_kwDOVBOtss8AAAABn-0gzw`); skipped.

## 6. Operator Questions

1. **Merge Authorization Question**:
   - Text: "CodeRabbit last reviewed head 07e48541 (not current head e52d0ba392c862333ce7e1a35f8139da8e468d87) and review-gate reports undecidable_read due to CodeRabbit's `<picture>` markup in the summary header. CI, verify, and Sonar are all green on current head e52d0ba392c862333ce7e1a35f8139da8e468d87. How do you wish to proceed?"
   - Options:
     1. "Authorize enqueue of PR #485 into merge queue on the recorded evidence (all 12 CodeRabbit review threads resolved, body comments addressed in 046f21bb and 659b94b5, verify and CI green on head e52d0ba392c862333ce7e1a35f8139da8e468d87, Sonar gate OK with 0 open issues, review-gate returning undecidable_read due to parser picture tag limitation)."
     2. "Stop run without merging."
   - Operator selected: Option 1 (Authorize enqueue).

## 7. Rule Check

- `guard: none on this harness (unenforced)`
- Section 1 rules: maintained throughout; zero shell file ops used for code inspection, no unauthorized tool bypasses.
- Spec read facts: `spec_source: ledger_worktree`.
- Executor skew: `match` (`0.1.2149`).

## 8. Build Server

- Setup preflight: `ready` (`registered: true`).
- Build routing: `execution_mode: auto`, resolved to `daemon_longpoll`.
- Daemon builds executed:
  - Job `6264d60bf43647b498efcd84cd7a7c02`: `failed` (test-first red run D8)
  - Job `f808d6b8090248c5a133617adc1672e7`: `success` (green run D8)
  - Job `4761c719df4c40d2875bea5933ad13c5`: `failed` (test-first red run D9)
  - Job `d8b928c858fd47d09039b59c4dd3f435`: `success` (green run D9)
  - Job `ac1dac28642c4b0dae24aa6845072839`: `failed` (test-first red run D10)
  - Job `ad5b6e0e878a4d05bd720bf7eaa0ec3e`: `success` (green run D10)
  - Job `43a9e7d8ee9942eabd0748bbfd5eb6f5`: `success` (quality-gate initial)
  - Job `f777ed190276475d845ce94d791aea8a`: `success` (verify initial)
  - Job `658a7a9aa6d941beaccd019059b0d186`: `success` (pm-web-e2e initial)
  - Job `19bd493fe30640889edfe0654e52fddf`: `success` (quality-gate pre-PR)
  - Job `441755dc91c146e4b12c62daa9d9cea4`: `success` (verify pre-PR)
  - Job `e0d849b156dc4a919f2181d72e34bad6`: `success` (pm-web-e2e pre-PR)
  - Job `cb225662a5334de7b24d4d9202107b3b`: `success` (quality-gate post-review 2)
  - Job `f30187cb0bfe4151a85992a3056b5625`: `success` (verify post-review 2)
  - Job `f96bfd8ceaa74d92a74de24da7040f42`: `success` (quality-gate post-main-merge, 915s)
  - Job `3e17877206c048f0be57e67060121b2c`: `success` (verify post-main-merge, 727s)
- Fallbacks: none. All builds ran via `daemon_longpoll`.

## 9. Residue

- None. All 10 deliverables of PLAN-66 are complete and landed on `main`.
- Worktree removed and feature branch deleted both locally and remotely.
- Root checkout clean and synchronized on `main` at commit `321985ac7a7868799acbc6bbd45d04a3b517e455`.
