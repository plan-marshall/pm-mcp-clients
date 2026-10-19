# Completion Report: `web-onboarding` (Onboarding and Start in the Web App)

Governed by [`doc/agy/runbook.md`](../runbook.md). The plan is archived at
[`doc/agy/implemented/web-onboarding-plan.md`](web-onboarding-plan.md); the work package at
[`doc/plans/implemented/web-onboarding.adoc`](../../plans/implemented/web-onboarding.adoc).

## Executive Summary

The `web-onboarding` work package delivers the onboarding route, the entry and pairing pages, the
machine settings, and the paired-devices management of `pm-web-app`, rendered from the states
`pm-mock-runtime` reports and covered by view tests against those scenarios.

The contract (`onboarding-contract`, `entry-contract`) and a first, partly generic rendering already
existed. This package closed the gaps against the settled design (`doc/design/web-app/`) and the
package's *Done When*:

- the app now has the routes the plan names — `/onboarding`, `/entry`, `/pair`, `/settings/machine`
  (and `/settings`) — and the address bar follows the store route, so a reload, a bookmark, or the
  browser's back returns to the same page;
- the onboarding renders each state as a screen of its own (start, consents, language, harness
  choose with token reporting, harness blocked, harness confirm, model select, roles, credentials,
  providers, plan defaults, the automatic run's result) instead of a generic hypermedia card, and
  the done steps are read-only summaries that can be opened from the step bar;
- the pairing page presents the pairing URL as a QR code and, on the network listener, the
  certificate fingerprint; the loopback credential entry, the connection heartbeat, and the offline
  banner are wired;
- the machine settings render all seven aspects, the remembered consents with their withdrawal, the
  documents mode, the cleanup durations, the tokens with rotate/replace, the providers' state, and
  the paired devices; a value changed in another client is refused with a **409 conflict dialogue**
  (*Apply yours again* / *Keep the current value*);
- the paired devices (UC-ENT-2) list, rename inline, revoke one, and revoke all others, and the
  browser of a device whose network listener was turned off gets the page that says why;
- **100% of the strings come from the message catalogue** (English and German), with a guard test
  that fails on any user-facing literal in these screens;
- **tokens, never money**: every figure is a token count or a floor estimate (`formatTokens`), and a
  test asserts that no currency sign or price appears.

## Delivered Artifacts

### Routes (`pm-web-app/src/routes/`)
- `onboarding/+page.svelte`, `entry/+page.svelte`, `pair/+page.svelte`, `settings/+page.svelte`,
  `settings/machine/+page.svelte` — one dispatcher (`AppRouter.svelte`) behind every route.
- `+page.svelte` and `+layout.svelte` — the route is adopted from the address bar, followed back on
  `popstate`, and mirrored into the address bar from the store (never across a pairing code still in
  the fragment); the un-onboarded machine stays on the onboarding on every route.

### Views and components (`pm-web-app/src/lib/`)
- `views/AppRouter.svelte` — the single route dispatcher.
- `views/OnboardingView.svelte` (rewritten) — one screen per reported state, the model-at-work card,
  the read-only completed-step summaries, and the step bar's read-only step view.
- `components/LanguageChoice.svelte`, `HarnessChoice.svelte`, `ModelsChoice.svelte`,
  `AspectChoice.svelte` (roles, store, providers, plan defaults), `ConsentBox.svelte` (rewritten to
  the shared pattern: fields, remember choice, *this run only*, the action's own label).
- `components/PairingQr.svelte` — the pairing URL as a locally-drawn QR code (no network request).
- `components/StepBar.svelte` (rewritten) — done steps open their read-only summary.
- `components/ConnectionStatusBanner.svelte` — heartbeat, listener, and device-count refresh.
- `views/MachineSettingsView.svelte` (rewritten) — aspects, consents, documents mode, cleanup
  durations, tokens with rotate/replace (loopback only), the 409 conflict dialogue, and the devices
  summary.
- `views/SettingsView.svelte` (rewritten) — the paired devices list with inline rename, individual
  revocation, revoke-all-others, and the network listener; `CredentialEntry`, `LanListenerModal`.
- `views/ListenerOffView.svelte` — the page of a device whose network listener was turned off.
- `routes.ts` — the one route model shared by the store, the router, and the views.
- `stores/runtime.ts` — the route model, deep-link adoption, `completedSteps`, the listener-closed
  state, and the credential store helper.
- `components/choice-screen.css` — the shared layout of the choice screens.

### Message catalogue (`pm-api/src/main/resources/messages/`)
- `onboarding.{en,de}.properties` — the start screen's step list, the choice screens, the consent
  actions, the completed summary, and the link labels.
- `settings.{en,de}.properties` — providers, the conflict dialogue, a token rotated or replaced, and
  the devices texts.
- `entry.{en,de}.properties` — the listener-off page.
- `pairing.{en,de}.properties` — the QR label and hint, and the code example.

Both packs keep the same keys (asserted by `MessageCatalogueTest` and `OnboardingCatalogueTest`).

### Mock runtime (`pm-mock-runtime/src/`)
- `MockRuntimeEngine.java` — `GET /api/v1/onboarding/state` also reports `step_summaries`, the
  recorded summary of each done step, so the web app's read-only summaries come from the runtime.
- `OnboardingScenarioTest.java` — a test for the reported step summaries.

### Tests (`pm-web-app/src/lib/`)
- `views/OnboardingView.scenarios.test.ts` — the start screen, consents (remember / once-only), the
  model at work, language, harness choose (tokens, disabled rows, default), a blocked harness,
  model allow/deny, the automatic result, the completed read-only summary, the step-bar read-only
  view, and the bilingual catalogue.
- `views/MachineSettingsView.scenarios.test.ts` — all seven aspects, consent withdrawal, the 409
  conflict applied again and kept, the expired token, rotate/replace, documents mode, durations.
- `views/PairingView.scenarios.test.ts` — code entry, invalid code, pairing, revoked reason, the
  LAN fingerprint, and the QR presentation.
- `views/Devices.scenarios.test.ts` — the device list, rename, revoke one, revoke all others,
  revoking this browser, the listener off, and the listener-off page.
- `i18n/catalogue.test.ts` — English/German key parity, the keys this package renders, and the
  guard that fails on any user-facing literal in the screens.
- `routes.test.ts` — the route model.
- `i18n/testMessages.ts` — the test loader for the real `pm-api` packs (with `\uXXXX` decoding).

### E2E (`pm-web-e2e/tests/`)
- `onboarding.spec.js` — the onboarding route, the consent loop, the harness list, the automatic
  run, phone-width overflow, the dark theme, and the machine settings route.
- `entry.spec.js`, `pairing.spec.js` — the connection banner, the LAN modal, the credential entry,
  the fragment pairing, and the fingerprint (scenario resets for the single-use code).

## Test & Verification Results

- `pm-web-app` unit/view tests: **201 passed** across 38 files (`npm test`).
- `pm-web-app` lint: clean (`npm run lint`).
- In-scope Playwright E2E (`onboarding`, `entry`, `pairing`, `devices`): **20 passed**.
- `pm-mock-runtime` onboarding scenarios: **18 passed**.
- `pm-api` catalogue tests: **20 passed**.
- `./mvnw verify -Ppre-commit`: **BUILD SUCCESS** for all modules.

## Domain Invariants

- **Java 25 / SvelteKit 3**: unchanged toolchain; the new dependency is `qrcode-generator`, used
  only in the browser to draw the QR locally.
- **No uncatalogued strings**: asserted for the screens this package owns; a user-facing literal
  fails `i18n/catalogue.test.ts`.
- **Bilingual catalogue (EN & DE)**: every added key exists in both packs; parity is asserted in
  the JS guard test and in the Java `MessageCatalogueTest`.
- **Mock runtime completeness**: the runtime now reports the done steps' summaries; the scenario
  tests cover it.
- **Tokens, never money**: figures use `formatTokens`; the model/consent screens show no currency,
  asserted in `OnboardingView.scenarios.test.ts`.
- **Responsive down to phone width**: a 375 px viewport asserts no horizontal overflow; the header,
  the step bar, the completed-row and detail grids, the devices table, and the modals reflow; the
  dark theme is asserted from `--color-bg-app`.

## Build & Quality Evidence

`./mvnw verify -Ppre-commit` completed with `BUILD SUCCESS`; `git status` after the run shows only
the intended source and documentation changes, with no reformatted or generated artifacts.

## Notes

`doc/plans/web-onboarding.adoc` was moved to `doc/plans/implemented/` per the runbook; its
references in `doc/plans/README.adoc`, `doc/design/web-app/README.adoc`, and the two archived plans
that name it were updated. The completion report stays at `doc/agy/web-onboarding-plan-report.md`
for the orchestrator session to verify and move (Step 8).

The legacy `workspace` E2E spec and a few pre-existing specs fail against a shared mock runtime for
reasons that predate this package (single-use pairing codes and order-dependent scenario state);
they are outside this package's scope and are unchanged here.
