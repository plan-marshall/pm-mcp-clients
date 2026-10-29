envelope_version=1
sender_type=plan
sender_id=rb-27-web-brand
epic=ui-improvements
kind=landing
created=2026-10-29T15:06:26Z

```landing-facts
schema=landing-facts/1
plan_id=rb-27-web-brand
epic=ui-improvements
pr=#262
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=3
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2032
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# PLAN-27 — Web: Brand Mark, Pairing Page, Favicon

Merged as `4c39c0f8a2b6bb52b98a05a110ad5a64de7ccb7a` (PR #262, `feature/27-web-brand`).

## 1. Deliverables

**D1 — BrandMark.** Met. The mark draws at `64` px for the pairing page and, with `plate`, at `24` px inside the rail head's `28` px brand-coloured square; the header keeps `24`.
- Done-when "unit tests for both sizes", order `test-first` for the plate/rail test and `mutation-after` for the `64` px half (operator-accepted substitution): test-first red at `a4be10a1`, log `pm-web-app/maven-2026-10-28-203716.log` (`Unable to find [data-testid="brand-mark-plate"]`); the `64` px test passed against the unfixed code, so it rests on a mutation run at `217e35e6`, log `pm-web-app/maven-2026-10-29-063900.log` (`expected '24' to be '64'`), mutation reverted, tree clean. Commit `4c049a01`.
- Done-when "browser check", order `none`: met. 8 baseline and 8 after targets (en/de × light/dark × 1440×900/390×844) plus the paired state under `.plan/temp/playwright-mcp/27-web-brand-*`; the after set was re-shot on 2026-10-29 so it carries the final caption.
  - Limitation recorded: the 28 px plate is visible in no artifact — the rail head arrives with `web-app-shell` (PLAN-39); the square is covered by `BrandMark.test.ts`. The favicon dot cannot appear in a page screenshot either; it is evidenced by the icon `href` read in the browser (`/brand-mark-attention.svg`, decisions badge at 3 after pairing) and by `favicon.test.ts`.

**D2 — Pairing page.** Met. The card carries the mark at `64` px and the wordmark, the code at `--text-3xl` monospace, and the pairing URL as a QR once the code is complete.
- Done-when "`pairing.spec.js` finds the mark, the code and the QR", order `test-first`: met. Red at `4c049a01`, log `pm-web-e2e/maven-2026-10-28-204108.log` (295 passed, 1 failed at `pairing.spec.js:35`); green at `7481711e`, log `…-204838.log` (296 passed). Commit `7481711e`.
- Done-when "browser check", order `none`: met, same artifact set; every target shows `data-size=64`, the wordmark, a 36 px monospace code and the QR, with no horizontal overflow at 390 px; German renders German text.
- The QR now appears on every listener; the catalogue caption no longer promises a same-network scan and points at the machine's network address on the local address (operator decision, see §6).

**D3 — Favicon and design.** Met. `src/lib/favicon.ts` switches the tab icon by the attention count (`brand-mark-attention.svg` above zero), wired in `+layout.svelte` to the same count the header badge and the tab title carry; `brand-mark.adoc` states the brand colour and the four uses.
- Done-when "a unit test toggles the favicon dot from the attention count", order `test-first`: met. Red at `7481711e`, log `pm-web-app/maven-2026-10-28-205510.log` (`Failed to resolve import "./favicon"`); green in `…-205739.log` (791 tests). Commit `217e35e6`.
- Done-when "browser check", order `none`: met (same artifacts; the icon flips after pairing, see D1's limitation).
- Done-when "the open-issue read at the pull request head", order `none`: met — `sonar read_open_issues` at the merge head `916198a`: `count_status: confirmed`, `open_issue_count: 0` (`analysis_revision` = head), `sonar-gate` `merge_allowed: true` (`no_open_issues`); `rules: none`.

Per-head test runs (the project publishes no `module-tests` / `test-compile` canonical, so the gate canonical is the run): quality-gate + `verify` green at `a7b07d0`, `23403bc`, `290ae99`, `b2941110` and `916198a` (2208 tests after the first main merge; 792 vitest tests in the web app at the end); `pm-web-e2e` green at `a5dc20b`, `2cdf0c0`, `721a2a2`, `fb008bf`, `23403bc`, `290ae99`, `b2941110` and `916198a` (296 passed each). One pre-existing `GitSourceIntakeModal` 5 s timeout flaked once at `b2941110` and passed on the re-run; one `designer.spec.js` draft test flaked once at `a5dc20b` and passed on the re-run.

## 2. Personas and skills

- D1 and D3: `plan-marshall:persona-implementer`; D2: `plan-marshall:persona-integration-tester`. Every self-review round and the PR-comment triage: `plan-marshall:persona-code-reviewer`.
- Domain skills loaded per the spec: `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing` (D1, D3) and `pm-dev-frontend:javascript` (D2). No skill was dropped.
- Surface drift, recorded: `pm-api/src/main/resources/messages/pairing.{en,de}.properties` (the QR caption, by the operator's decision), `pm-web-app/src/routes/+layout.svelte` (the favicon wiring), `pm-web-app/src/lib/favicon.{ts,test.ts}` and `static/brand-mark-attention.svg` (D3), `PairingView.scenarios.test.ts` (the removed loopback guard), and the design-document repairs `doc/design/web-app/{README,theme-readability,entry}.adoc` plus the two brand diagrams. Skills added for that drift: none needed — all are in the already-loaded frontend domains.

## 3. Hypotheses

The spec carries no `HYPOTHESIS` lines. Its one `OBSERVED` claim (VD-S-05, VD-P-08) is corroborated: `doc/claude_review/visual-design/plan.adoc:46-48` and `proposals.adoc:205-215`, and every Expected Surface file existed at `c4dc9bb4`.

## 4. Findings and dispositions

- Self-review round 1: (A) QR on loopback under a false caption — resolved by the operator's decision, captions rewritten; (B) the attention dot left the `2..22` construction box — dot moved inside; (C) the recorded commit of the D2 red run was wrong — corrected to `4c049a01`; (D) the `64` px test was not fail-first — mutation-after evidence recorded; doc contradictions fixed (README "mask", theme-readability allowlist, diagram colours).
- Round 2: F2 theme-readability line naming only the PairingQr allowlist — fixed; F3 empty-state row read as a current use — marked as arriving with its own plan; F4 entry.adoc lacked the brand block and QR — bullets and sketch added; F5 the browser artifacts predated the caption commit — all re-shot; F6 the caption assumed a printed network address — retargeted to "the network address of this machine"; F8 baked hexes asserted against literals — now read from the token catalogue.
- Round 3: entry.adoc's QR cross-reference and the sketch itself — fixed; `ascii_diagrams check` reports 0 misaligned. Nothing else open.
- CodeRabbit, head `a7b07d0`: 2 trivial nitpicks (QR payload not asserted; attention dot asserted as text) — fixed in `fb008bf`.
- CodeRabbit, head `23403bc`: 1 trivial nitpick (dot test assets not derived from the icon constants) — fixed in `290ae99`.
- CodeRabbit, head `b294111`: 2 trivial nitpicks (dot geometry; painted brand colours) — fixed in `916198a`.
- CodeRabbit, head `916198a`: 3 trivial nitpicks (stroke half-width in the dot bounds; exact attention palette; ring on the dot circle) — **answered** in PR comment `6500029686` with the reason that each is a preference inside assertions that already prove the same thing about the shipped assets, and carried as one follow-up item (§9). The review itself records "mergeable with minor test improvements as follow-up".
- CI: one earlier `sonar-build` run failed with "Organization is not allowed to access data from non main branches" (provider permission, not the change); the parallel run on the same head and every later run passed.
- `gh` authentication was lost once mid-run (~13:05Z); after the operator restored it, CI/Sonar/PR reads resumed and the merge proceeded.

## 5. Reviewers

| Reviewer | Required | State | Head reviewed | History |
|---|---|---|---|---|
| coderabbit | yes (quota-limited) | reviewed | `916198a` (current head at merge) | triggered 08:30Z, 08:49Z (quota refusals), 10:22Z (review of `a7b07d0` at 10:29Z), 11:07Z (quota refusal), 11:38Z (review of `23403bc` at 11:44Z), 12:24Z (quota refusal), 14:42Z (review of `916198a` at 14:51Z) |
| sourcery | no (quota-limited) | refused | — | triggered 08:30:58Z and 08:50:27Z; hard quota ("250,000 diff characters", next in ~4 days); optional, does not gate |

No review thread was ever opened (`reviewThreads: 0`). All findings are answered or fixed as listed in §4. `sourceCommitId`/`coveredCommitId` = `916198a`, `kind=reviewed`.

## 6. Operator questions

1. "The executor `.plan/execute-script.py` (0.1.2021) is older than the runbook-lane text it would run (0.1.2028), so the lane stops as blocked before `resolve`…" — options: regenerate / stop as blocked; **selected "Regenerate the executor, then continue"**.
2. "The plan requires `/pair` to show the mark, wordmark, code and QR… the QR encodes `window.location.origin`, unreachable from a phone on loopback, under the caption 'Scan this code with a phone on the same network'" — options: QR everywhere + truthful caption / restore the loopback guard / keep as-is; **selected "QR everywhere, caption tells the truth"**.
3. "D1's unit-test Done-when declares `test-first`; the 64 px test was green before the fix; a mutation-after red exists" — options: accept the mutation-after evidence / report not met; **selected "Accept the mutation-after evidence"**.
4. "PR #262 is ready to merge under the standing authorization…" — options: enqueue now / wait for a review of the current head; **selected "Enqueue the merge now"**.
5. Free text "retry" (twice) after the `gh` authentication loss — retried; the third attempt succeeded once the operator had restored the credentials. Not a merge authorization and not treated as one.

## 7. Rule check

guard: none on this harness (unenforced) — no pre-tool-use hook exists for opencode here, so no guard-log count is claimed.

Own actions checked against §1:
- Shell file operations: one `grep` over build logs and one `grep` for durations were issued early on (the log-open calls §4 prescribes); afterwards files were read with the read/grep tools. No shell `cat`/`ls`/`find`/`head`/`sed` was used on repository files.
- Builds: every build went through the resolved `maven` canonical; all ran `--execution-mode in_process` because the one routed attempt at the start answered `execution_failed` with an empty log. Both `verify` runs at `916198a` are recorded; the first hit the resolved 856 s bound (`timeout_used_seconds: 856`, `timeout_source: explicit`) and the re-run with a raised `--timeout 1800` passed, as §4 prescribes for a bound expiry.
- CI: every action went through `ci` (no `gh`/`glab` directly). One malformed `ci pr comments --slot 0 … || true` invocation was issued by mistake; it printed nothing and changed nothing.
- Nine operator-facing messages, four of them question-tool calls with selections (listed in §6), two of them free-text "retry"/"try again", and the model never changed, so the stamp stays `deepseek-v4.1-flash/high`.
- Spec read: `spec_source: ledger_worktree` at landing time (the epic's ledger copy, which is the same text this run implemented), `spec_differ` not reported.
- `executor-skew`: `skew` at the start (`0.1.2021` vs `0.1.2028`), regenerated with the operator's agreement, then `match`; the landing-time resolve reports executor `0.1.2032` (a later sync), which the run did not execute against.

## 8. Build server

Setup preflight: `ready` (registered). Every build of this run ran in-process: the single routed attempt answered `execution_failed` (the daemon's `mvn` produced an empty log), so the lane recorded the degradation and used `--execution-mode in_process` throughout. No `killed` job. `runtime_stamp_reason`: none reported; `plugin_version` is `unknown` from the resolve result (the runtime stamp does not carry it here), everything else was measured.

## 9. Residue

- **Follow-up item:** tighten `favicon.test.ts` as the `916198a` review suggests — stroke half-width in the dot bounds, an exact palette set, and the white ring asserted on the dot circle itself (`cr-comment:v1:b6726aef9b1edd244dffb0e3`, `cr-comment:v1:bf2be272a34ad834e0d8a512`, `cr-comment:v1:427de3848c6599cf3e5259a7`).
- **PLAN-49 dependency:** the favicon's attention count is `runtimeStore.openDecisionsCount` because the attention inbox (`lib/stores/attention.ts`) belongs to PLAN-49; when that lands, the badge, the tab title and the tab icon should all read the inbox total.
- **PLAN-39 dependency:** the rail head must render `<BrandMark size={24} plate />` for the 28 px square to appear; the component and its test are ready.
- **Empty states** (Objective, no deliverable): the mark in `--color-text-muted` is recorded in `brand-mark.adoc` as arriving with the empty states' own plan (PLAN-35).
- **Pre-existing, untouched:** `<html lang>` stays `en` on the German pairing page (`src/app.html` hard-codes it and nothing writes `document.documentElement.lang`).
- **Optional reviewer:** Sourcery never reviewed (hard quota until ~2026-11-02); it is optional and did not gate.
