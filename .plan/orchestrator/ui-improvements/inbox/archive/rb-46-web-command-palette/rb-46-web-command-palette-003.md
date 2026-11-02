envelope_version=1
sender_type=plan
sender_id=rb-46-web-command-palette
epic=ui-improvements
kind=landing
created=2026-11-01T15:42:23Z

```landing-facts
schema=landing-facts/1
plan_id=rb-46-web-command-palette
epic=ui-improvements
pr=#300
merge_state=merged
cleanup_owed=false
deliverables_total=6
deliverables_done=6
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2036
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:skipped,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced).

# Follow-up to PLAN-46: the six review findings of PR #296

PR #300 (`fix(web): the palette review findings of PR #296`) merged into `main` as
`1ce32d808126ac625613e8d6a786964d1d0e8e54` through the merge queue (merge-group run 41321542408,
success). Branch `feature/46-web-command-palette-review-fixes` and its remote ref deleted; the
worktree removed; the main checkout fast-forwarded to `1ce32d80`.

The preceding merge of PR #296 (as `d7f7ffc8`) rested on the belief that CodeRabbit's review of
`42aee47d` had no findings. It had filed six inline findings, all unresolved. This change fixes
exactly those six; CodeRabbit and Sourcery were not triggered (label `skip-bot-review`, on the
operator's word), and the merge rested on that label, not on a review.

## The six findings — fixed in `8e47dd0e`

| # | Finding (PR #296 comment id) | Fix | Failing test seen first | Passing after |
|---|---|---|---|---|
| 1 | Minor — acts listed before matches, so `Enter` opened a project's start route (`PRRC_kwDOVBOtss8AAAABCF6vLg`) | `rows` = matches (runtime order) then acts; the markup renders the acts after the match groups | `chrome.spec.js` "keeps the matched project ahead of its start act" — log `maven-2026-11-01-143404` (act at index 0) | log `maven-2026-11-01-144503` |
| 2 | Major — no touch-accessible close (`PRRC_kwDOVBOtss8AAAABCF6vNQ`) | labelled `Button` in the panel header, `web.common.close` ("Close"/"Schließen") → `close()` | `chrome.spec.js` "closes the panel from a labelled control on a phone" — same red log | same green log |
| 3 | Major — selection only a CSS class and `aria-current` on an unfocused button (`PRRC_kwDOVBOtss8AAAABCF6vOQ`) | field is a `combobox` with `aria-expanded` and `aria-activedescendant` naming the selected row's `id`; results are a `listbox`, rows are `option`s with `aria-selected`; Tab trap and `aria-modal` unchanged | `chrome.spec.js` "exposes the arrow-key selection as a combobox" — same red log | same green log |
| 4 | Minor — `requestedDecisionId` survived the store going `null` (`PRRC_kwDOVBOtss8AAAABCF6vRg`) | the first effect resets it when the route names no decision | `DecisionView.test.ts` "drops a pending decision when the route stops naming one" — log `maven-2026-11-01-143158` (stale `aria-current="true"`) | log `maven-2026-11-01-144246` |
| 5 | Minor — a filter hiding the requested decision consumed the request (`PRRC_kwDOVBOtss8AAAABCF6vPg`) | the request effect clears the local filter when it hides the requested decision | `DecisionView.test.ts` "clears the local filter when it hides the requested decision" — same red log | same green log |
| 6 | Minor — the scroll query ran before the card mounted (`PRRC_kwDOVBOtss8AAAABCF6vUA`) | the query runs in `tick()` after the DOM update | `DecisionView.test.ts` "scrolls to a decision whose card mounts after the request" — same red log | same green log |

## Gates at the head `8e47dd0e`

- `verify -Ppre-commit` (whole tree): green, 2527 tests (log `maven-2026-11-01-145721`).
- `verify` (whole tree): green, 2527 tests (log `maven-2026-11-01-150609`).
- `verify -Pe2e -pl pm-web-e2e`: 308 passed; all 7 palette tests green. The 7 failures are the ones
  `origin/main` already shows, recorded with the same names before the change (log
  `maven-2026-11-01-143404`) and after (log `maven-2026-11-01-144503`): `epic-view.spec.js` "writes
  the tokens as a floor…", `plan-view.spec.js` ×3 ("views plan process tree…", "navigates to epic
  view…", "discloses the token breakdown…"), `showroom.spec.js` "walks the six-phase process tree…",
  and the text guard for `plan` in `de` and `en`. Not touched.
- `-pl pm-web-app`: 89 files / 1027 tests green (unit, typecheck, lint).
- CI on the head: Maven Build, Integration Tests, Dependency Review, SonarCloud Code Analysis — all
  green.
- Sonar at the head: `confirmed`, **0** open new-code issues, no rule left open.

## Browser check (Playwright MCP, `fully-onboarded`, mock 20460 / app 20461)

- **Close control at 390 px, de and en**: the accessibility snapshot shows `button "Close"` and
  `button "Schließen"` beside the heading; clicking removes the panel. Artifacts
  `.plan/temp/playwright-mcp/46-followup-{en,de}-390-close.{png,yml}`.
- **Screen-reader-visible active descendant, de and en**: the accessibility snapshot shows
  `combobox … [expanded] [active]` + `listbox "…"` with `option "Relay Hardening" [selected]`; the DOM
  read gives `aria-activedescendant="command-palette-option-0"` naming an element with
  `role="option"` and `aria-selected="true"`. Artifacts
  `46-followup-{en,de}-390-activedescendant.{png,yml}`.
- **Acts after the matches**: `46-followup-de-390-acts-after-matches.png` shows both project matches
  first, then the two "Einen Plan in … starten" acts, the active descendant naming the first match.

## Operator questions

None asked in this follow-up. The run rests on the operator's message for PR #300: fix the six
findings, label `skip-bot-review`, do not trigger the bots, and merge once CI and Sonar are green.

## Rule check

`guard: none on this harness (unenforced)`.

- Shell file operations (read-class): `grep` over maven logs and catalogue files; `tail` over the
  test file; `git … ls-files`-free. These are rule breaks of the read-with-file-tools rule and are
  recorded as such. One `cp` was not used in this follow-up.
- Builds: all through the maven build skill, in-process (`mechanism=in_process_fallback`); none
  routed (the daemon cannot spawn maven in this environment, as recorded in the PLAN-46 landing).
- No `git stash`, `restore`, `checkout --`, `rebase` or forced push; no scratch worktree in this
  follow-up (the base was `origin/main` at `f5de8449`).

## Residue

- The seven pre-existing e2e failures remain on `main`; they belong to other plans (PLAN-16's token
  formatter and the older breadcrumb/text-guard drift).
- The palette's field clear action is still the browser's own (`type="search"`); the design names no
  catalogue key for a labelled clear.
- `/` still belongs to the palette (the design assigns it), so a view's filter no longer takes the
  focus on `/`.
