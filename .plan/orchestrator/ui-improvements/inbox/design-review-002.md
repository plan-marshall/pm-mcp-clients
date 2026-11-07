envelope_version=1
sender_type=orchestrator
sender_id=design-review
epic=ui-improvements
kind=finding
created=2026-11-06T16:00:42Z
revision=1
amended=2026-11-06T19:15:19Z

# Running work: remove "Set as browser start page" together with all of its parts

Sender: operator design review in the browser, 2026-11-06, against the mock runtime (`mock-machine`).

## Operator feedback
On the overview (Running work / "Laufende Arbeit"), remove the button "Als Startseite des Browsers festlegen" ("Set as browser start page"). Remove the whole function with it: every artifact, not only the button.

## Findings: the parts of the function
The toggle is the only writer of `pm_start_route`. Nothing else sets it (`git grep pm_start_route`). It all goes:
- **Button and state:** `pm-web-app/src/lib/views/OverviewView.svelte`: the imports `rememberedStartRoute` and `setRememberedStartRoute` (:29-30), `startPageIsOverview` (:43, :194), `toggleStartPage()` (:365-368), the button `data-testid="start-page-toggle"` (:590-599), and its CSS `.start-page-toggle`, `:hover` and `--active` (:1244-1265). The heading block "Laufende Arbeit" then has no second element left. Check its layout (the title should not stay squeezed to two lines).
- **Store:** `pm-web-app/src/lib/stores/runtime.ts`: `rememberedStartRoute()` and `setRememberedStartRoute()` (:316-345), the branch `remembered` in `landingRoute()` (:308-311), and the doc comments that name `pm_start_route` (:286, :366-368 at `openStartPage`). `landingRoute` and `openStartPage` (the brand click in `NavRail.svelte:89`) stay. They land by the rule alone.
- **Texts:** `overview.start_page.set` and `overview.start_page.is_default` in `pm-api/src/main/resources/messages/overview.{de,en}.properties:75-76`, plus any catalogue/text-guard test that lists the keys.
- **Tests:** `pm-web-app/src/lib/views/OverviewView.test.ts:~350-368` (toggle test) and `pm-web-e2e/tests/overview.spec.js:172-185` ("remembers Running Work as the browser start page across a reload"). No runtime store test sets `pm_start_route`; the `openStartPage` / `landingRoute` cases in `runtime.test.ts` test the rule and stay.
- **Browsers that set it already:** pre-1.0, no shim. The key is simply no longer read. A stale `pm_start_route` in a browser's `localStorage` is harmless.

## Relation to the design and to the staged plans
- `doc/design/web-app/needs-and-find.adoc:17-21` (decision of 2026-10-31): the list of what waits at `/` becomes the start page. "A remembered place still wins" there means *where the operator left it* (`shared/plan-overview.adoc#persistence`), not this explicit toggle. Removing the toggle matches the design. No design document names the toggle or `pm_start_route` (`git grep` over `doc/design`), so no design text changes.
- **PLAN-49 (web-attention-inbox, staged)** makes `/` the start page and rewrites the landing rule. If this removal lands first, PLAN-49 starts from a `landingRoute` without the `remembered` branch. If PLAN-49 runs first, it must not take the toggle over. Either way, its spec should say that `pm_start_route` is gone.
- **PLAN-53 (start-wizard)** uses "start page" for the page that *starts a plan*. That is a different thing and not affected.
- PLAN-70 (light-theme-pass) screenshots the overview. The button will no longer be in its evidence.

## Suggested action (the epic decides)
A small runbook package (`web-overview-drop-start-page-toggle`), or one deliverable of PLAN-49 if that is staged soon: remove the parts listed above, run `npm run quality` in `pm-web-app`, run the overview e2e, and check in the browser that the heading of Running work looks right in de/en and that a fresh paired browser and the brand click land by the rule.

## Process
This message is **code-only**: it changes no design document and needs no design session. It can be implemented now (see `design-review-010.md`). Every other message of this review is designed first, in its own Claude Code session, before its implementation.
