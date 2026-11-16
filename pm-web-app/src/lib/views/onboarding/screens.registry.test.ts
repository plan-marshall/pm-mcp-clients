import { describe, expect, it } from 'vitest';
import onboardingScenario from '../../../../../pm-mock-runtime/src/main/resources/scenarios/onboarding.json';
import { SCREEN_RULES, screenFor, screenNameFor, type OnboardingState } from './screens';

/**
 * The build check of the screen registry (UC-P-01).
 *
 * It reads every state of the mock runtime's `onboarding` scenario — the same import
 * `scenarioMockFetch.ts` uses — and fails for a state no screen claims. A state the mock can reach
 * without a screen of its own is therefore a red build, never a generic card in the running app.
 */

/** One state of the scenario, as the scenario file spells it. */
interface ScenarioState {
  representation?: {
    facts?: Record<string, unknown>;
    state?: { state_id?: string; kind?: string };
  };
}

/** The scenario's states, keyed by their id. */
const SCENARIO_STATES = (onboardingScenario as unknown as { states: Record<string, ScenarioState> })
  .states;

/**
 * The state as the runtime serves it: the facts flattened, with the state's id beside them.
 *
 * @param id the state's id
 * @returns the state's representation
 */
function stateOf(id: string): OnboardingState {
  return { ...(SCENARIO_STATES[id].representation?.facts ?? {}), state: id };
}

/**
 * @returns every id of the scenario, in the order the file lists them
 */
function scenarioStateIds(): string[] {
  return Object.keys(SCENARIO_STATES);
}

describe('the onboarding screen registry', () => {
  it('claims a screen for every state the mock scenario can reach', () => {
    const uncovered = scenarioStateIds().filter((id) => screenFor(id, stateOf(id)) === null);

    // The message names the states, so a new state of the scenario says which screen it needs.
    expect(uncovered).toEqual([]);
  });

  it('answers every state of the onboarding frame with a screen of its own', () => {
    // The frames beside the onboarding (entry, settings, the waits, pairing) are answered by the
    // fallback on purpose; every other state must have a screen of its own.
    const otherFrame = /^(entry|settings|wait|web)\./;
    const fallbackOnly = scenarioStateIds().filter(
      (id) =>
        !otherFrame.test(id) &&
        id !== 'status.only' &&
        screenNameFor(id, stateOf(id)) === 'missing',
    );

    expect(fallbackOnly).toEqual([]);
  });

  it('leaves the states of the frames beside the onboarding to the fallback', () => {
    const otherFrame = scenarioStateIds().filter((id) =>
      /^(settings|wait|web)\./.test(id),
    );

    expect(otherFrame.length).toBeGreaterThan(0);
    for (const id of otherFrame) {
      expect(screenNameFor(id, stateOf(id))).toBe('missing');
    }
  });

  it('leaves an id no screen claims to the view’s fallback', () => {
    expect(screenFor('unknown.state')).toBeNull();
    expect(screenNameFor('unknown.state')).toBe('missing');
  });

  it('sends every entry state to the fallback, by the operator’s decision of 2026-11-10', () => {
    const entryStates = scenarioStateIds().filter((id) => id.startsWith('entry.'));

    expect(entryStates.length).toBeGreaterThan(0);
    for (const id of entryStates) {
      expect(screenNameFor(id, stateOf(id))).toBe('missing');
    }
  });

  it('claims each state with one screen only, so the order of the rules is no accident', () => {
    for (const id of scenarioStateIds()) {
      const claims = SCREEN_RULES.filter((rule) => rule.claims(id, stateOf(id)));

      expect(claims).toHaveLength(1);
    }
  });

  it('names every rule once', () => {
    const ids = SCREEN_RULES.map((rule) => rule.id);

    expect(new Set(ids).size).toBe(ids.length);
  });
});
