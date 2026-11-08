# PLAN-109: Web: Rail "Settings" Label, Cog Icon, and "Plan settings" for the Plan's Settings Act

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane (operator rule 2026-11-07). No design session is needed: the change is code plus one agreed
> shared text.
> This spec is SELF-SUFFICIENT: the prompt below is a one-line pointer and carries no brief.

## Objective

Operator design review of 2026-11-06 (`design-review-001.md`, archived under `inbox/archive/design-review/`): in the left
rail, group *Machine*, the entry "Einstellungen der Maschine" is cut off in the 232 px rail. Shorten the web label
`web.frame.settings` to "Einstellungen" / "Settings" (the group heading already gives the context; the tooltip
`web.frame.settings.title` keeps the long form), replace the sun-like `nav-settings.svg` (a circle with eight rays, easily
confused with the theme choice) with a cog in the style of the set, and rename the plan's own settings act
`control.plan.settings` to "Plan-Einstellungen" / "Plan settings" in the same pull request so that "Einstellungen" names one
thing (message 007 §4). The shared text is changed in the design and in both clients' catalogue; the terminal's plan menu
is changed and checked with it. Code and catalogue only: no design session.

## Deliverables

1. **Web label and cog icon**
   - What: `common.{de,en}.properties` key `web.frame.settings` → `Einstellungen` / `Settings` (line ~100; `.title` at ~101
     stays); `pm-web-app/src/lib/icons/ui/nav-settings.svg` is replaced by a cog (24×24 viewBox, `stroke="currentColor"`,
     `stroke-width="1.5"`, round caps and joins, the style of the set); the key `nav.settings` and `shell/nav.ts` stay.
     The rail entry leads to `/settings`; `activeNavRoute` (`nav.ts:136-152`) already maps `machine-settings` onto it. The
     page title "Einstellungen der Maschine" (`machine-settings.adoc:246`) and the help texts that point to the page stay.
     `doc/design/web-app/onboarding-texts.adoc` rows ~91-93 are the text-catalogue row of `web.frame.settings` itself and
     change with the key (`doc/design/web-app/README.adoc:138` already names the entry just *Settings*).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-documents:ref-asciidoc
   - Done when:
     - The e2e specs and helpers that find the rail entry by its old name use the new one: `pm-web-e2e/tests/designer.spec.js`
       (:19, :135), `showroom.spec.js` (:171), `workspace.spec.js` (:63) (`getByRole('button', {name: 'Machine settings'})`),
       `theme-readability.spec.js` (:159, `LABELS.settings`); `devices.spec.js` (:20) and `onboarding.spec.js` (:178) use
       `/Settings/i` and are checked for a second matching button under strict mode (evidence order: `test-first`)
     - `uiIcons.test.ts` passes with the new SVG (the file name is unchanged); a test or the existing registry check shows
       the key `nav.settings` still resolves (evidence order: `none`)
     - Browser check shows the rail entry fully visible (no cut-off) expanded and collapsed (tooltip) in `de` and `en`, light
       and dark, with the cog distinct from the theme choice; baseline and after screenshots recorded (evidence order: `none`)
2. **`control.plan.settings` → "Plan-Einstellungen" / "Plan settings"**
   - What: `pm-api/src/main/resources/messages/control.{de,en}.properties` key `control.plan.settings` (currently "Settings" /
     "Einstellungen"); `doc/design/shared/work-actions-texts.adoc:41-43`; the terminal's plan menu (`pm-operator`
     `ActionMenus.java:248` reads the key and needs no code change) with its tests that assert the old label:
     `ActionMenusTest.java` (:58, :78, :105, `startsWith("Settings")`) and `ActionsFullScreenTest.java` (:59, :64,
     "Settings…") expect "Plan settings…". There are no transcript files with this label. Optional doc touches that show the
     old label: `doc/design/shared/work-actions.adoc:26-50` and `doc/design/terminal-client/19-actions.adoc:56,72,73` (the
     latter belongs to the running `PLAN-97` until it lands: do not edit it before).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
   - Done when:
     - `ActionMenusTest` and `ActionsFullScreenTest` assert "Plan settings…" and pass; `ControlCatalogueTest` (key presence)
       passes unchanged (evidence order: `test-first`)
     - `pm-web-app` `byState.ts` consumers of `control.plan.settings` (six sites) render the new text through the unchanged
       key (a component test names the label) (evidence order: `test-first`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open
       (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order:
       `none`)

## Standing Constraints

- Build and test through Maven and npm only (`CLAUDE.md`); beside the session's own checks these builds pass at the head:
  `./mvnw -pl pm-api,pm-operator verify`, `./mvnw -pl pm-web-app verify`, `./mvnw -pl pm-web-e2e verify`.
- No new dependency or plugin. Input test data comes from `Generators`; expected catalogue text stays literal.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): text reaches the DOM through text-treating sinks only; a test waits on a
  condition, never on a timer.
- Every changed text exists in both languages (`de` / `en`); catalogue parity is kept.
- Ports of this plan: mock web port `21090`, app port `21091` (`AGENTS.md` § Ports formula `20000 + 10 × 109`; corrected 2026-11-07, the staged value was one block too low and PLAN-110 runs on this block by its own spec: start this plan only after PLAN-110 has merged). Pass them to every mock, app
  and e2e start; other plans run at the same time, so never stop or reuse a process this worktree did not start.
- Browser check of the rendered output following `AGENTS.md` § Browser checks (the session's browser tool; if none is
  available the browser Done-when clause is NOT met — a blocker reported as such, never replaced by a unit test). Check `de`
  and `en`, dark and light, at 1440 × 900 and 390 × 844; the visible text shows no raw key.
- **Sequencing:** start after the running `PLAN-106` has landed (it lists `control.{de,en}.properties` in its surface), or
  merge `main` and rebase before landing. Do not build in parallel with `PLAN-111` (both edit `common.{de,en}.properties`,
  lines ~100-101 against ~238-244: no textual conflict, but a rebase) or with `PLAN-107` without a rebase.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The pull request is merged only on the operator's word; commits end with `Co-Authored-By: plan-marshall
  <noreply@cuioss.de>`; the pull request body carries no generator footer.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`): an open Sonar issue is
  fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` and transmitted through
  `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`.
  The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed.

## Claim Labels

- OBSERVED: `pm-web-app/src/lib/shell/nav.ts:104-109` has the entry `settings` with `labelKey: 'web.frame.settings'` and
  `icon: 'nav.settings'`; `common.de.properties:100` is "Einstellungen der Maschine" and `common.en.properties:100`
  "Machine settings"; `nav-settings.svg` is a circle with eight rays; `uiIcons.ts:44,69` registers it — verified by a
  read-only check at HEAD 60b4bfa5 on 2026-11-06
  - verdict: corroborated | checked_at: aa3a9be3694ff072e1ffb09be87a5569da274ecc | by: ui-improvements/cleanup | rescoped: n/a | evidence: nav.ts:103-109 labelKey web.frame.settings icon nav.settings; common.de:100 / common.en:100 Einstellungen der Maschine / Machine settings; nav-settings.svg circle with 8 rays; uiIcons.ts:44,69 register it
- OBSERVED: `control.plan.settings` is "Settings" / "Einstellungen" in `control.{en,de}.properties:16`; `ActionMenus.java:248`
  reads it; the only other consumer is `byState.ts` (six sites, key unchanged) — verified at HEAD 60b4bfa5 on 2026-11-06
  - verdict: corroborated | checked_at: aa3a9be3694ff072e1ffb09be87a5569da274ecc | by: ui-improvements/cleanup | rescoped: n/a | evidence: control.{en,de}.properties:16 Settings/Einstellungen; ActionMenus.java:248 reads the key; byState.ts six sites (67,73,79,87,93,98)
- OBSERVED: `web.frame.settings` has no consumer outside the web rail and `onboarding-texts.adoc:91-93` — verified at HEAD
  60b4bfa5 on 2026-11-06
  - verdict: contradicted | checked_at: 56b43a02404b95023b16dc4304afe6998767f8bd | by: ui-improvements/analyze | rescoped: no | evidence: web.frame.settings also reaches the phone More sheet: BottomBar.svelte:158 renders $t(item.labelKey) for nav.ts:106; harmless, premise incomplete (three-layer analysis of the PLAN-109 landing)
- OBSERVED: no snapshot files exist (`__snapshots__`, `.snap`); the terminal asserts the plan-menu label in plain tests, not in
  transcript captures — verified at HEAD 60b4bfa5 on 2026-11-06 (the message said "transcript captures": refuted)
  - verdict: corroborated | checked_at: aa3a9be3694ff072e1ffb09be87a5569da274ecc | by: ui-improvements/cleanup | rescoped: n/a | evidence: no __snapshots__ or .snap files in pm-web-app
- HYPOTHESIS: `devices.spec.js:20` and `onboarding.spec.js:178` (`/Settings/i`) match more than one button once the rail entry
  is "Settings" (strict-mode violation) — inferred, not run; confirm/refute by running both specs (verify-at-outline)
  - verdict: unverifiable | checked_at: aa3a9be3694ff072e1ffb09be87a5569da274ecc | by: ui-improvements/cleanup | rescoped: n/a | evidence: the strict-mode clash of devices.spec:20 and onboarding.spec:178 needs a Playwright run
- HYPOTHESIS: `uiIcons.test.ts` checks asset names against the registry only, not SVG content — confirm/refute at
  `pm-web-app/src/lib/icons/uiIcons.test.ts` (verify-at-outline)
  - verdict: contradicted | checked_at: aa3a9be3694ff072e1ffb09be87a5569da274ecc | by: ui-improvements/cleanup | rescoped: yes | evidence: uiIcons.test.ts checks SVG content (24 grid, 1.5 stroke, currentColor, round caps, forbidden tags): the new cog must satisfy it; work-actions-texts.adoc:41-43, work-actions.adoc:26-50 and 19-actions.adoc:59,75-76 already say Plan settings (PLAN-112), so the doc edits for the plan act are dropped
- Verify-first clause: re-read the named lines at the current HEAD before editing; `PLAN-106` (running) and `PLAN-60`/`PLAN-85`
  (staged) edit neighbouring files.

## Expected Surface

- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD 60b4bfa5
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD 60b4bfa5
- OBSERVED: `pm-api/src/main/resources/messages/control.de.properties` — present at HEAD 60b4bfa5
- OBSERVED: `pm-api/src/main/resources/messages/control.en.properties` — present at HEAD 60b4bfa5
- OBSERVED: `pm-web-app/src/lib/icons/ui/nav-settings.svg` — present at HEAD 60b4bfa5
- OBSERVED: `pm-web-app/src/lib/icons/uiIcons.test.ts` — present at HEAD 60b4bfa5 (run; edit only if it checks content)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/actions/ActionMenusTest.java` — present at HEAD 60b4bfa5
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ActionsFullScreenTest.java` — present at HEAD 60b4bfa5
- OBSERVED: `pm-web-e2e/tests/designer.spec.js` — present at HEAD 60b4bfa5
- OBSERVED: `pm-web-e2e/tests/showroom.spec.js` — present at HEAD 60b4bfa5
- OBSERVED: `pm-web-e2e/tests/workspace.spec.js` — present at HEAD 60b4bfa5
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` — present at HEAD 60b4bfa5
- OBSERVED: `pm-web-e2e/tests/devices.spec.js` — present at HEAD 60b4bfa5 (check only)
- OBSERVED: `pm-web-e2e/tests/onboarding.spec.js` — present at HEAD 60b4bfa5 (check only)
- OBSERVED: `doc/design/web-app/onboarding-texts.adoc` — present at HEAD 60b4bfa5

## Dependencies and Sequencing

- Depends on: `PLAN-106` (running; `control.*` properties)
- Overlaps with: `PLAN-111` (`common.*.properties`), `PLAN-107` (`common.*.properties`, different keys), `PLAN-70` and `PLAN-85`
  (`theme-readability.spec.js`; staged and gated), `PLAN-60` (owns the settings pages and merges `/settings` and
  `/settings/machine`; starts after this plan), `PLAN-97` (`terminal-client/19-actions.adoc`, optional touch only)
- Adjacent to: `PLAN-112` (D1 changes `control.plan.settings` only if this plan has not landed it)

## Re-grounding 2026-11-07 (cleanup)

Checked at `aa3a9be3`. The shared documents are DONE by PLAN-112 (D1): `work-actions-texts.adoc:41-43` reads "Plan settings…" / "Plan-Einstellungen…", `work-actions.adoc:26-50` and `19-actions.adoc:59,75-76` already say "Plan settings…": drop those doc edits and the PLAN-97 restriction on `19-actions.adoc`. Still to do: the catalogue keys, the web rail (`nav.ts:103-109`, `nav-settings.svg`, `uiIcons.ts`), the two Java tests, `onboarding-texts.adoc:91-93` (`web.frame.settings`), the six `byState.ts` sites. The new cog must satisfy `uiIcons.test.ts` (24 grid, 1.5 stroke, `currentColor`, round caps, no forbidden tags). The strict-mode clash of `devices.spec:20` and `onboarding.spec:178` needs a run (unverifiable by reading).

## Hand-Off Command

Lane: `runbook` (run with opencode) — a small plan (operator rule 2026-11-07: small plans go to the runbook lane, design work to direct Claude Code sessions). Start it after `PLAN-106` has landed. 

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-109-web-rail-settings-label-and-icon.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source, tests and design documents. It creates and edits
NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message — the orchestrator owns every other
ledger write — and reports its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole
sanctioned write mechanism are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.
