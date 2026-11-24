envelope_version=1
sender_type=plan
sender_id=rb-187-web-withdraw-decomposition-drop
epic=ui-improvements
kind=landing
created=2026-11-24T17:45:17Z

```landing-facts
schema=landing-facts/1
plan_id=rb-187-web-withdraw-decomposition-drop
epic=ui-improvements
pr=#561
merge_state=merged
cleanup_owed=false
deliverables_total=2
deliverables_done=2
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=2
review_body_findings_handled=2
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

## 1. Deliverables

PR `#561` merged into `main` at commit `d0bfd8540498886be8cad76b51dbb2ddf53837e2` (`settle: merged`). Both worktree and branch (`fix/187-web-withdraw-decomposition-drop`) were removed and pruned; main checkout updated to `d0bfd8540498886be8cad76b51dbb2ddf53837e2`.

- **Deliverable 1 — Drop leaves both decomposition views** (commits `d4b68f09`, `493d77f9`):
  - *DW 1.1* (`EpicDecompositionView.test.ts`, `DecompositionReviewView.test.ts`, `StartView.test.ts` assert no `Drop` / `Lass ihn fallen` act in `en`/`de` and wire `status: 'dropped'` stays rendered; old fake-drop tests rewritten; `"Drop the plan"` absent from `pm-web-app/src`): **met**; `order: test-first`; failing run at base `456c4cb7` in `.plan/local/plans/NO_PLAN/build-results/pm-web-app/maven-2026-11-24-121450.log` (3 failed: `DecompositionReviewView.test.ts:117`, `EpicDecompositionView.test.ts:401`, `StartView.test.ts:340`), plus self-review reactivity test-first fail in `maven-2026-11-24-134908.log` (`DecompositionReviewView.test.ts:129`); passed in `verify -pl pm-api,pm-web-app` at `493d77f9` (`maven-2026-11-24-135035.log`, Java 1689 + web 3394 tests).
  - *DW 1.2* (Agree gate counts only wire findings, no client-side dropped list): **met**; `order: none`; verified in `EpicDecompositionView.svelte:218-233`, `DecompositionReviewView.svelte:35-37,325`, `EpicDecompositionView.test.ts:415-433` (`maven-2026-11-24-135035.log`).
  - *DW 1.3* (`decomposition.spec.js` rewritten, `decomposition.spec.js` & `start.spec.js` pass under `-Pe2e` with `PM_MOCK_WEB_PORT=21870`, `PM_WEB_PORT=21871`, no `waitForTimeout`): **met**; `order: test-first`; failing run at base `456c4cb7` in `.plan/local/plans/NO_PLAN/build-results/pm-web-e2e/maven-2026-11-24-121704.log` (`decomposition.spec.js:76:3` expected 0 `Drop` buttons, received 4); passed at `493d77f9` (`maven-2026-11-24-140546.log`: `decomposition.spec.js` 4/4, `start.spec.js` 11/11; 457 passed, 75 skipped, 11 pre-existing unrelated failures matching base `456c4cb7`).
  - *DW 1.4* (Playwright MCP browser check of epic decomposition & Start review in `en`/`de`, light/dark, `1440x900` & `390x844` after last visual commit): **met**; `order: none`; 16 screenshots in `.plan/temp/playwright-mcp/{epic,start}-decomp-{en,de}-{light,dark}-{1440,390}.png` (`start-decomp-de-*` refreshed after `493d77f9`).
- **Deliverable 2 — The design says what the clients do** (commit `2be8bdce`):
  - *DW 2.1* (`starting-work.adoc` & `starting-work-texts.adoc` parity with `start.decomposition.*` and `start.gate.slow`, no shared-design decomposition Drop act): **met**; `order: none`; `asciidoc validate` (0 format violations) and full `verify -Pcoverage` at `493d77f9` (`5fdc64a627ca4cb4ae8cdccc7a0549ac`, 721s).
- **Per-head whole-repo verification**:
  - `2be8bdce`: `verify` (`9b65b6b2bf134b57be96b7b186508ebf`, 572s), `quality-gate` (`verify -Ppre-commit`, `3b292c5a69744b1298207e09a4b60a0c`, 722s), `verify -Pcoverage` (`93a27f03f0364e51bcfc5993448ad4aa`, 684s).
  - `493d77f9`: `verify -Pcoverage` (`5fdc64a627ca4cb4ae8cdccc7a0549ac`, 721s).
  - `bc6c6c2b`: `verify` (`632e038d6177473c8a17e658722a70ba`, 711s).
  - `99351b56`: `verify` (`f465696ca95b41e9bedf8b3153b56fac`, 575s).
  - `509cb79d`: `verify` (`d1c12566e1674969b1122ea6f4aea1d0`, 744s).
  - `424b60dd` (merge head): `verify --timeout 942` hit explicit bound at 943s (`448d694caa9941d886617d2f3910e037`); split gate passed all 3 parts on `424b60dd`: `quality-gate` (`01c8ca51db8146b5b2306e00c7228920`, 1250s), `test-compile` (`07ec6b609afd425482209b5da86c63a7`, 61s, queued 1098s), and `module-tests` (`df5335cf74a9469b932a693f7b485af4`, 905s, queued 13s; preceded by 1 flaky `EntryRecordsTest.runtimeEntryResourceRefusesAMalformedLegacyPort:451` failure in `d35d2d21d9494bd7931087b7d2b5569e` and 1 learned-696s bound timeout in `d6508048eee34134bde77c45515237a7`).

## 2. Personas and skills

- D1 implementation & tests: `plan-marshall:persona-implementer`, `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`.
- D2 design docs: `plan-marshall:persona-documenter`, `pm-documents:ref-asciidoc`, `pm-documents:ref-documentation`.
- Self-review & PR triage: `plan-marshall:persona-code-reviewer`.
- `skills added`: none.

## 3. Hypotheses

- `EpicDecompositionView` null-plan rewrite drop — confirmed at `EpicDecompositionView.svelte:237-247` (plus `:31,:123,:219-227,:402,:449-457`).
- `MockRuntimeEngine` null `plan_id` resolution to `firstOpenPlanId` + `applyRewrite` — confirmed at `MockRuntimeEngine.java:4703,4709`.
- Terminal offers no drop because contract has none — confirmed at `EpicDecompositionDialogue.java:287-289`.
- Start wizard drops through same rewrite — confirmed at `StartView.svelte:852-854,1160` and `DecompositionReviewView.svelte:22,207-215`.
- `decomposition.drop` and `start.decomposition.drop` have no Java user — confirmed (only test entries `DecompositionCatalogueTest.java:49` and `catalogue.test.ts:335`).

## 4. Findings

- **Self-review**: Round 1 (`21e061cd-e2e3-4cc0-97a1-82e18c43620c`) identified 1 pre-existing Medium i18n reactivity bug in `DecompositionReviewView.svelte` (`planDetails`/`findingText` omitted `$t`), fixed in `493d77f9`; Round 2 clear (`open: none`).
- **PR comments (`review-gate` on `424b60dd`)**: 0 review threads; 2 `coderabbit` `outside-diff` body findings (`outside-diff-1` on `DecompositionReviewView.svelte:207`, `outside-diff-2` on `EpicDecompositionView.svelte:416-417`), both answered (`#issuecomment-7134823819`) and declined (Keep/Edit stay available on wire-dropped plans to match `EpicDecompositionDialogue.java:291-308`).
- **CI**: 20/20 checks green on `424b60dd`.
- **Sonar**: `0` confirmed open new-code issues on `424b60dd`, `quality_gate_status: ok` (5/5 OK).

## 5. Reviewers

- `coderabbit` (required, `quota_limited=true`, `review_complete_after_handling=true`): triggered at `2026-11-24T15:22:26+01:00` on `493d77f9`; reviewed `493d77f9` (`PRR_kwDOVBOtss8AAAABaBL5ag`, 0 threads, 2 `outside-diff` findings handled). Exempted from re-trigger on later heads per `review_complete_after_handling`.
- `sourcery` (optional, `quota_limited=true`, `review_complete_after_handling=false`): triggered on `493d77f9` (silent after 27 min); re-triggered on `bc6c6c2b` (`2026-11-24T16:03:28+01:00`) and refused on weekly diff quota (`250,000 diff characters/7d`, next trigger `>= 2026-11-24T17:35:43+01:00`).

## 6. Operator questions

none asked.

## 7. Rule check

- `guard: none on this harness (unenforced)`
- `spec_source: ledger_worktree` (no `spec_differ`)
- `executor-skew: match` (`0.1.2183` vs `0.1.2183`)
- Mailbox checks: read 3/3 (plus pre-enqueue re-check) under `rb-187-web-withdraw-decomposition-drop` -> `mailbox_state: no_mailbox`, `delivery_state: never_delivered` (`resolve`'s `--plan-id 187-web-withdraw-decomposition-drop` form returned `error: invalid_target_plan` and was reported verbatim without modifying scripts).
- Subagents: 1 (`21e061cd-e2e3-4cc0-97a1-82e18c43620c`, 2 rounds, none killed). Context compactions resumed after full `SKILL.md` re-reads (`2026-11-24T13:38:44+01:00`, `2026-11-24T18:00:46+01:00`).
- Out-of-plan `MockRuntimeEngine.java` formatting rewritten by `-Ppre-commit` was reverted cleanly; Playwright verification screenshots were written only under `.plan/temp/playwright-mcp/`.

## 8. Build server

- Setup preflight: `build_server: ready`, `executor_in_worktree: present`.
- Routed builds recorded in `progress.md`: `13` whole-repo/split builds with `mechanism=daemon_longpoll` (`1` explicit-942s bound expiry on `448d694c` -> switched to `{build.split}`; `1` learned-696s bound expiry on `d6508048` -> succeeded with `--timeout 942`; `1` transient flaky failure in `EntryRecordsTest` on `d35d2d21` -> passed on `df5335cf`); `0` `in_process_fallback`, `0` `degraded`, `0` `launch-fault-retry`, `0` `killed`. Additionally `7` module-scoped (`pm-web-app`, `pm-api,pm-web-app`, `pm-web-e2e`) `maven run` invocations are cited by log path.
- Runtime stamp from landing `resolve`: `harness=antigravity`, `model=unknown`, `effort=unknown` (no `runtime_stamp_reason` field emitted).

## 9. Residue

- None in plan scope. Pre-existing unrelated e2e failures observed at base `456c4cb7` (`projects.spec.js:112`, `ProjectOnboardingView` in `theme-readability.spec.js`) remain untouched outside this plan. Temporary screenshots remain under gitignored `.plan/temp/playwright-mcp/`.
