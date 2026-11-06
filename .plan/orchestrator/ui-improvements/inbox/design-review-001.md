envelope_version=1
sender_type=orchestrator
sender_id=design-review
epic=ui-improvements
kind=finding
created=2026-11-06T15:56:56Z
revision=5
amended=2026-11-06T19:53:13Z

# Rail: shorten the machine settings label and give it a cog icon

Sender: operator design review in the browser, 2026-11-06, against the mock runtime (`mock-machine`).

## Operator feedback
In the left rail, group *Machine*, the entry "Einstellungen der Maschine" is too long.
- Shorten the label to "Einstellungen" (de) / "Settings" (en). The group heading *Maschine* / *Machine* already gives the context.
- The icon is not good. Use a cog wheel or something like it.

## Findings
- **Label.** `pm-web-app/src/lib/shell/nav.ts:104-108` has the entry `settings` → `labelKey: 'web.frame.settings'`, `icon: 'nav.settings'`. The text comes from `pm-api/src/main/resources/messages/common.de.properties:100` (`Einstellungen der Maschine`) and `common.en.properties:100` (`Machine settings`). In the 232 px rail the German text is cut off ("Einstellungen der Masc…").
- **Tooltip.** `web.frame.settings.title` (`common.*.properties:101`) keeps the long form. It can stay as the explaining text.
- **Design.** `doc/design/web-app/README.adoc:138` already names the rail entry just *Settings* ("*Machine* (System, Process Designer, Settings)"). So the code is the one that differs from the design, and the short label needs no change to the design. The page title in `doc/design/web-app/machine-settings.adoc:246` and `onboarding-texts.adoc:93` ("Einstellungen der Maschine") is a different thing and can stay. The help texts that point there ("…in den Einstellungen der Maschine ändern", `common.consent.withdraw_hint`, `starting-work-sources-texts.adoc:163`, `onboarding-texts.adoc:213`) still name the right destination and stay.
- **Two settings pages.** `doc/claude_review/ux/findings.adoc:47` already notes that there are two names: "Einstellungen der Maschine" (`/settings/machine`) and "Maschinen-Einstellungen" (`/settings`, devices/listener/credentials). Under a short "Einstellungen" label that difference has to be settled once: the rail entry leads to `/settings`, and `activeNavRoute` (`nav.ts:136-152`) maps `machine-settings` onto it. PLAN-60 merges the two pages. The plan's own settings act is renamed "Plan-Einstellungen" (007), so "Einstellungen" names one thing.
- **Icon.** `pm-web-app/src/lib/icons/ui/nav-settings.svg` is a circle with eight rays. It reads as a sun (brightness or light theme), not as settings. The set (`src/lib/icons/ui/`, registry `src/lib/icons/uiIcons.ts:44,69`) has no cog. A new `nav-settings.svg` in the style of the set (24×24, `stroke="currentColor"`, `stroke-width="1.5"`, round caps and joins) replaces the content of the file. The key `nav.settings` stays, so nothing else changes. The sun is also easy to confuse with the theme choice (PLAN-85, staged, will move it into Settings "This browser").

## Affected plans
- **PLAN-60 web-settings-one-place (staged)** merges `/settings` and `/settings/machine` into one `/settings` with tabs. It owns the "two settings pages" point above and should take the short rail label and the new icon as part of its scope, or land after this small package.
- **PLAN-85 (staged)** will move the theme choice into Settings "This browser"; the sun icon must not be confused with it in the meantime.
- No other staged web plan owns the rail entry or its icon.
- PLAN-70 (light-theme-pass) screenshots every rail route in light. If the new icon lands first, it is in its evidence.

## Suggested action (the epic decides)
- A small new runbook package (`web-rail-settings-label-and-icon`): `web.frame.settings` → `Einstellungen` / `Settings`, a new cog `nav-settings.svg`, the plan's settings act `control.plan.settings` → "Plan-Einstellungen" / "Plan settings" (so "Einstellungen" names one thing, 007 §4), the snapshot/text guards updated, a browser check of the rail expanded and collapsed (tooltip) in de/en and light/dark.

## Process
This message needs **no design session** and can be implemented now as a runbook (see `design-review-010.md`). One agreed shared text changes with it: `control.plan.settings` (`shared/work-actions-texts.adoc:41-43`) becomes "Plan settings" / "Plan-Einstellungen" in the same pull request, and the terminal's plan menu (`pm-operator` `ActionMenus.java:248`, its transcript captures) is checked with it.
