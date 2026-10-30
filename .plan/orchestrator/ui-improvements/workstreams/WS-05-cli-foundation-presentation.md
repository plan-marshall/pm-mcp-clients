# WS-05: Terminal Client: Foundation, Interaction, Presentation

epic: ui-improvements

## Charter

The cross-cutting foundation, interaction model, and presentation of `pm-operator`: the operator gate scope, catalogue completeness and the error pattern, the display layer, footer and help, the `needs` verb, verb modes, JSON output, colour roles, ASCII fallback, the column grid, and the brand line (CLI review: Cross-Cutting S-02 to S-06, UX S-01 to S-06, Visual Design S-01 to S-05).

## Scope

- In scope: `pm-operator` texts, gate, TUI frame (`tui/`), views' rendering, Picocli wiring; `pm-api` `Display`; `doc/design/terminal-client/`.
- Out of scope: The acting dialogues themselves (WS-04); the web app.

## Plans

| Plan | Status | Lane | Notes |
|------|--------|------|-------|
| PLAN-05-cli-summary-line-and-start-script | staged | runbook | Terminal: Summary After Leaving a View, Start Script |
| PLAN-07-cli-terminal-design-docs | staged | runbook | Terminal: Presentation in the Design Documents |
| PLAN-14-cli-gate-scope | staged | runbook | Terminal: The Operator Gate on Acts Only |
| PLAN-15-cli-footer-and-help-overlay | staged | runbook | Terminal: Clean Rows, a Footer and a Help That Never Lie |
| PLAN-24-cli-proof-of-presence-decision | staged | runbook | Terminal: Decision Record for a Proof of Presence |
| PLAN-30-cli-texts-and-errors | staged | runbook | Terminal: Every Text Through the Catalogue, One Error Pattern |
| PLAN-34-cli-display-layer | staged | runbook | Terminal: Display Layer in pm-api |
| PLAN-36-cli-needs-verb | staged | runbook | Terminal: "Needs You" in Every Surface |
| PLAN-38-cli-style-and-glyphs | staged | runbook | Terminal: Style and Glyphs Classes — SUPERSEDED, moved into PLAN-44 (2026-10-30) |
| PLAN-40-verify-cli-foundation | staged | plan-marshall | Verify the Terminal Foundation — SUPERSEDED, moved into PLAN-63 (2026-10-30) |
| PLAN-42-cli-verb-modes-and-help | staged | runbook | Terminal: One Rule for Verb Modes, Help Everywhere |
| PLAN-44-cli-colour-roles-and-brand | staged | runbook | Terminal: Colour Roles Applied, Brand Line |
| PLAN-48-cli-json-output | staged | runbook | Terminal: JSON for Scripts |
| PLAN-50-cli-column-grid | staged | runbook | Terminal: One Column Grid, Say It Once |
| PLAN-56-verify-cli-interaction | staged | plan-marshall | Verify the Terminal Interaction — SUPERSEDED, moved into PLAN-63 (2026-10-30) |
| PLAN-63-verify-cli-presentation | staged | plan-marshall | Verify the Terminal Presentation and Key Tasks |

## Sequencing and Surface Notes

- PLAN-05 after PLAN-02
- PLAN-14 after PLAN-02
- PLAN-15 after PLAN-02, PLAN-05
- PLAN-24 after PLAN-14
- PLAN-30 after PLAN-21, PLAN-22, PLAN-23, PLAN-17
- PLAN-34 after PLAN-06, PLAN-26
- PLAN-36 after PLAN-13, PLAN-08, PLAN-34, PLAN-82
- PLAN-38 superseded: its deliverables are PLAN-44 deliverables 1 to 3
- PLAN-40 superseded: its checks are in PLAN-63
- PLAN-42 after PLAN-30, PLAN-36
- PLAN-44 after PLAN-07, PLAN-34, PLAN-15; PLAN-34 after PLAN-82 (served order and size)
- PLAN-48 after PLAN-42
- PLAN-50 after PLAN-44
- PLAN-56 superseded: its checks are in PLAN-63
- PLAN-63 after PLAN-07, PLAN-44, PLAN-50 and the plans of the former PLAN-40 and PLAN-56 (PLAN-14, 30, 34, 05, 24, 15, 36, 42, 48, 84, 86)
