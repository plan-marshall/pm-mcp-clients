# WS-02: Web Foundation: Texts, Format, Design System v2

epic: ui-improvements

## Charter

The cross-cutting foundation and the design system v2 of `pm-web-app`: the text guard, the display layer, titles, theme, time and locale, tokens, icons, components, brand, and the accessibility baseline (web review: Cross-Cutting S-01 to S-05, Visual Design S-00 to S-05). Every view plan of WS-03 builds on it.

## Scope

- In scope: `pm-web-app/src/lib/display/`, `lib/components/`, `lib/icons/`, `styles/`, `routes/+layout.svelte`, `app.html`, `static/`; the guard specs of `pm-web-e2e`; `doc/design/web-app/` documents of the foundation.
- Out of scope: The rework of individual views (WS-03); the terminal client (WS-04, WS-05).

## Plans

| Plan | Status | Lane | Notes |
|------|--------|------|-------|
| PLAN-01-web-text-guard-report | staged | runbook | Web: Text Guard in Report Mode |
| PLAN-04-web-theme-and-titles | staged | runbook | Web: Theme Remembered, Titles With Their Arguments |
| PLAN-09-web-display-layer | staged | runbook | Web: Display Layer and Catalogue Fixes |
| PLAN-12-web-design-tokens-v2 | staged | runbook | Web: Design Tokens v2 and the Inter Typeface |
| PLAN-16-web-time-format-and-locale | staged | runbook | Web: One Time Format and a Full Locale Switch |
| PLAN-19-web-ui-icon-set | staged | runbook | Web: Own UI Icon Set, No Emoji |
| PLAN-25-web-core-components-controls | staged | runbook | Web: Core Components, Controls |
| PLAN-27-web-brand | staged | runbook | Web: Brand Mark, Pairing Page, Favicon |
| PLAN-29-verify-web-texts | staged | plan-marshall | Verify the Web Texts, Titles, Theme, and Time |
| PLAN-31-web-core-components-containers | staged | runbook | Web: Core Components, Containers |
| PLAN-35-web-data-components | staged | runbook | Web: Data Components |
| PLAN-37-web-accessibility-baseline | staged | runbook | Web: Accessibility Baseline and axe-core |
| PLAN-41-verify-web-design-system | staged | plan-marshall | Verify the Web Design System v2 |

## Sequencing and Surface Notes

- PLAN-09 after PLAN-06, PLAN-01
- PLAN-12 after PLAN-01
- PLAN-16 after PLAN-06, PLAN-09
- PLAN-19 after PLAN-12
- PLAN-25 after PLAN-12, PLAN-19
- PLAN-27 after PLAN-12, PLAN-19
- PLAN-29 after PLAN-01, PLAN-04, PLAN-09, PLAN-16
- PLAN-31 after PLAN-25
- PLAN-35 after PLAN-31, PLAN-16
- PLAN-37 after PLAN-31
- PLAN-41 after PLAN-12, PLAN-19, PLAN-25, PLAN-31, PLAN-35, PLAN-27, PLAN-37
