import { describe, expect, it } from 'vitest';
import onboardingScenario from '../../../../../pm-mock-runtime/src/main/resources/scenarios/onboarding.json';
import { OTHER_FRAME_STATES, SCREEN_RULES, screenFor, screenNameFor } from './screens';

/**
 * The build check of the screen registry (UC-P-01).
 *
 * It reads every state of the mock runtime's `onboarding` scenario — the same import
 * `scenarioMockFetch.ts` uses — and fails for a state no screen claims. A state the mock can reach
 * without a screen of its own is therefore a red build, never a generic card in the running app.
 *
 * The fallback is not a catch-all: `entry.*` states land there by the operator's decision of
 * 2026-11-10, and the states of the frames beside the onboarding are named one id at a time. A new
 * state in any of those families is red until someone decides what it is.
 */

/** The scenario's states, keyed by their id. */
const SCENARIO_STATES = (
  onboardingScenario as unknown as { states: Record<string, unknown> }
).states;

/**
 * @returns every id of the scenario, in the order the file lists them
 */
function scenarioStateIds(): string[] {
  return Object.keys(SCENARIO_STATES);
}

describe('the onboarding screen registry', () => {
  it('claims a screen for every state the mock scenario can reach', () => {
    const uncovered = scenarioStateIds().filter((id) => screenFor(id) === null);

    // The message names the states, so a new state of the scenario says which screen it needs.
    expect(uncovered).toEqual([]);
  });

  it('draws every onboarding state with a designed screen, never the fallback', () => {
    const fallbackOnly = scenarioStateIds().filter(
      (id) =>
        !id.startsWith('entry.') &&
        !OTHER_FRAME_STATES.has(id) &&
        screenNameFor(id) === 'missing',
    );

    // `entry.*` is the operator's decision and the other frames own their states; every other
    // onboarding state has a screen of its own.
    expect(fallbackOnly).toEqual([]);
  });

  it('leaves an id no screen claims to the view’s fallback', () => {
    expect(screenFor('unknown.state')).toBeNull();
    expect(screenNameFor('unknown.state')).toBe('missing');
  });

  it('sends every entry state to the fallback, by the operator’s decision of 2026-11-10', () => {
    const entryStates = scenarioStateIds().filter((id) => id.startsWith('entry.'));

    expect(entryStates.length).toBeGreaterThan(0);
    for (const id of entryStates) {
      expect(screenNameFor(id)).toBe('missing');
    }
  });

  it('names the states of the frames beside the onboarding one id at a time', () => {
    // A state invented in one of those families is not claimed: the fallback list is explicit, so
    // the test above goes red and someone decides what the new state is.
    for (const id of ['settings.brand-new', 'wait.brand-new', 'web.brand-new']) {
      expect(screenFor(id)).toBeNull();
    }
  });

  it('claims each state with one screen only, so the order of the rules is no accident', () => {
    for (const id of scenarioStateIds()) {
      const claims = SCREEN_RULES.filter((rule) => rule.claims(id));

      expect(claims).toHaveLength(1);
    }
  });

  it('names every rule once', () => {
    const ids = SCREEN_RULES.map((rule) => rule.id);

    expect(new Set(ids).size).toBe(ids.length);
  });
});
