/**
 * The registry of the onboarding's screens (UC-P-01, UX-P-08).
 *
 * Every state the onboarding frame can show resolves to one screen here: by its state id, or by
 * the pattern its id follows (`*.consent`, `harness.tokens-*`). The view holds no `{:else}` chain
 * any more — it asks this module which screen to draw, and draws the fallback only for an id no
 * screen claims.
 *
 * The registry is also a build check: `screens.registry.test.ts` reads every state of
 * `pm-mock-runtime`'s `onboarding.json` and fails for a state that resolves to nothing, so a state
 * added to the mock without a screen is a red build, never a generic card in the running app.
 *
 * Two groups are answered by the fallback on purpose, and both are named one id at a time, so a
 * state added to either group is a red build until someone decides what it is:
 *
 * - `entry.*` states have no web screen by the operator's decision of 2026-11-10; the fallback
 *   names the terminal command that shows them (PLAN-64, Operator Decisions);
 * - the states of the settings frame, the decision waits and the pairing page, listed in
 *   `OTHER_FRAME_STATES`. The onboarding frame cannot draw them, so it says so rather than
 *   guessing.
 */

import type { Component } from 'svelte';
import type { OnboardingState } from '../../api/client';
import './screens.css';

/** The runtime's onboarding state, re-exported for the screens and the tests that read it. */
export type { OnboardingState };

import MissingScreen from './MissingScreen.svelte';
import ModeScreen from './ModeScreen.svelte';
import SummaryScreen from './SummaryScreen.svelte';
import ConsentScreen from './ConsentScreen.svelte';
import CompletedScreen from './CompletedScreen.svelte';
import WorkingScreen from './WorkingScreen.svelte';
import LanguageScreen from './LanguageScreen.svelte';
import HarnessChooseScreen from './HarnessChooseScreen.svelte';
import HarnessBlockedScreen from './HarnessBlockedScreen.svelte';
import HarnessConfirmScreen from './HarnessConfirmScreen.svelte';
import ProbeFailedScreen from './ProbeFailedScreen.svelte';
import HarnessAddScreen from './HarnessAddScreen.svelte';
import TokensScreen from './TokensScreen.svelte';
import ModelsScreen from './ModelsScreen.svelte';
import RolesScreen from './RolesScreen.svelte';
import StoreScreen from './StoreScreen.svelte';
import ProvidersScreen from './ProvidersScreen.svelte';
import ProvidersFailedScreen from './ProvidersFailedScreen.svelte';
import AutoResultScreen from './AutoResultScreen.svelte';
import DefaultsScreen from './DefaultsScreen.svelte';
import StatusScreen from './StatusScreen.svelte';

/**
 * A link the state offers. A step is taken by naming the link's id on the onboarding's one step
 * resource, so the link's own href is never followed and not kept.
 */
export interface OfferedLink {
  id: string;
  isPrimary: boolean;
}

/**
 * Everything a screen reads and reports.
 *
 * The view owns the runtime conversation — the step calls, the state's fields, the catalogue — and
 * hands it to the screen as one object, so a screen is presentation and never a second client.
 */
export interface OnboardingScreenContext {
  /** The state the runtime reports, or `null` before the first read. */
  state: OnboardingState | null;
  /** The links the state offers, in the runtime's order. */
  links: OfferedLink[];
  /** Whether a step call is in flight. */
  busy: boolean;
  /** The step numbers whose summary the runtime recorded. */
  doneSteps: number[];
  /** The recorded summary of each done step, keyed by step number. */
  summaries: Record<string, string>;
  /** The machine's step numbers, 1-based. */
  machineSteps: number[];
  /** Takes the step a link names. */
  onLink: (id: string, body?: Record<string, unknown>) => void;
  /** Starts the automatic run. */
  onAuto: () => void;
  /** Goes back from a state the app cannot draw. */
  onBack: () => void;
  /** The catalogue key suffix of a step's name. */
  stepKey: (step: number) => string;
  /** A non-empty string field of the state. */
  field: (name: string) => string | undefined;
  /** An array field of the state. */
  arrayField: <T>(name: string) => T[];
  /** The label of a link, from the catalogue. */
  linkLabel: (id: string) => string;
  /** The action a consent's primary button names. */
  consentAction: () => string;
  /** The body of the completed card. */
  completedSummary: () => string;
  /** The harness names the model screen groups by. */
  harnessNames: () => Record<string, string>;
  /** The first link of the given ids the state offers. */
  linkBy: (...ids: string[]) => OfferedLink | undefined;
}

/** The props every screen takes: the view's context and nothing else. */
export interface ScreenProps {
  view: OnboardingScreenContext;
}

/** One rule of the registry: the ids it claims and the screen it draws. */
export interface ScreenRule {
  /** The screen's name, for the registry test and the progress record. */
  id: string;
  /** Whether the rule claims this state. */
  claims: (stateId: string) => boolean;
  /** The screen the rule draws. */
  screen: Component<ScreenProps>;
}

/** States whose two-state suffix the consent pattern owns. */
const CONSENT_ID = /(\.consent|-consent)$/;

/** States the runtime works in, shown with the one working screen. */
const WORKING_STATES = new Set([
  'auto.scan',
  'harness.probe',
  'harness.enrol',
  'harness.install-profile',
  'harness.verify-profile',
  'catalogue.models',
  'catalogue.advise',
  'catalogue.record',
  'roles.validate',
  'store.check',
  'store.record',
  'providers.verify',
  'providers.record',
  'github.check',
  'defaults.validate',
  'defaults.record',
]);

/** States whose design is a read-only summary: the runtime reports what happened, in its words. */
const STATUS_STATES = new Set(['harness.none', 'providers.credentials', 'providers.verified', 'sonar.key']);

/**
 * The states of the frames beside the onboarding, which the onboarding frame answers with the
 * fallback. Named one id at a time: a state added to one of those families has to be added here
 * deliberately, and until it is, the registry test is red.
 */
export const OTHER_FRAME_STATES = new Set([
  'settings.choose',
  'settings.store-credential',
  'settings.verify-providers',
  'settings.conflict',
  'settings.withdraw',
  'settings.documents-mode',
  'settings.plan-defaults',
  'settings.cleanup-durations',
  'settings.record',
  'wait.choose',
  'wait.check',
  'wait.moved',
  'wait.cancelled',
  'web.pair',
  'web.paired',
  'status.only',
]);

/**
 * The registry, in order: the first rule that claims a state draws it.
 *
 * A rule is a pattern where the states share their design (a consent is a consent, whatever step
 * asks it) and an id where the state is one screen of its own. No rule is a catch-all: an id no
 * rule names is a screen nobody designed, and the test says so.
 */
export const SCREEN_RULES: readonly ScreenRule[] = [
  {
    id: 'completed',
    claims: (stateId) => stateId === 'onboard.completed',
    screen: CompletedScreen,
  },
  {
    id: 'consent',
    claims: (stateId) => CONSENT_ID.test(stateId),
    screen: ConsentScreen,
  },
  {
    id: 'summary',
    claims: (stateId) => stateId === 'onboard.summary',
    screen: SummaryScreen,
  },
  {
    id: 'mode',
    claims: (stateId) => stateId === 'onboard.mode' || stateId === 'onboard.entry',
    screen: ModeScreen,
  },
  {
    id: 'language',
    claims: (stateId) => stateId === 'language.choose' || stateId === 'language.reconfigure',
    screen: LanguageScreen,
  },
  {
    id: 'harness-choose',
    claims: (stateId) =>
      stateId === 'harness.choose' ||
      stateId === 'harness.not-logged-in' ||
      stateId === 'harness.all-failed' ||
      stateId === 'harness.reconfigure',
    screen: HarnessChooseScreen,
  },
  {
    id: 'harness-blocked',
    claims: (stateId) => stateId === 'harness.blocked',
    screen: HarnessBlockedScreen,
  },
  {
    id: 'harness-confirm',
    claims: (stateId) => stateId === 'harness.confirm' || stateId === 'harness.add-confirm',
    screen: HarnessConfirmScreen,
  },
  {
    id: 'harness-probe-failed',
    claims: (stateId) => stateId === 'harness.probe-failed',
    screen: ProbeFailedScreen,
  },
  {
    id: 'harness-add',
    claims: (stateId) => stateId === 'harness.add' || stateId === 'harness.unknown',
    screen: HarnessAddScreen,
  },
  {
    id: 'harness-tokens',
    claims: (stateId) => stateId.startsWith('harness.tokens-'),
    screen: TokensScreen,
  },
  {
    id: 'catalogue',
    claims: (stateId) => stateId === 'catalogue.select' || stateId === 'catalogue.reconfigure',
    screen: ModelsScreen,
  },
  {
    id: 'roles',
    claims: (stateId) => stateId === 'roles.review' || stateId === 'roles.reconfigure',
    screen: RolesScreen,
  },
  {
    id: 'store',
    claims: (stateId) => stateId === 'store.choose' || stateId === 'store.reconfigure',
    screen: StoreScreen,
  },
  {
    id: 'providers',
    claims: (stateId) => stateId === 'providers.select' || stateId === 'providers.reconfigure',
    screen: ProvidersScreen,
  },
  {
    id: 'providers-failed',
    claims: (stateId) => stateId === 'providers.failed',
    screen: ProvidersFailedScreen,
  },
  {
    id: 'defaults',
    claims: (stateId) => stateId === 'defaults.review' || stateId === 'defaults.reconfigure',
    screen: DefaultsScreen,
  },
  {
    id: 'auto-result',
    claims: (stateId) => stateId === 'auto.result',
    screen: AutoResultScreen,
  },
  {
    id: 'working',
    claims: (stateId) => WORKING_STATES.has(stateId),
    screen: WorkingScreen,
  },
  {
    id: 'status',
    claims: (stateId) => STATUS_STATES.has(stateId),
    screen: StatusScreen,
  },
  {
    id: 'missing',
    claims: (stateId) => stateId.startsWith('entry.') || OTHER_FRAME_STATES.has(stateId),
    screen: MissingScreen,
  },
];

/**
 * The screen that draws a state, or `null` when no screen claims it.
 *
 * A `null` is the state the app has no screen for: the view draws the fallback, which never prints
 * the state's id as content.
 *
 * @param stateId the state's id, as the runtime reports it
 * @returns the screen, or `null` for a state no screen claims
 */
export function screenFor(stateId: string): Component<ScreenProps> | null {
  for (const rule of SCREEN_RULES) {
    if (rule.claims(stateId)) return rule.screen;
  }
  return null;
}

/**
 * @param stateId the state's id, as the runtime reports it
 * @returns the name of the screen drawing the state, or `missing` for one no screen claims
 */
export function screenNameFor(stateId: string): string {
  for (const rule of SCREEN_RULES) {
    if (rule.claims(stateId)) return rule.id;
  }
  return 'missing';
}
