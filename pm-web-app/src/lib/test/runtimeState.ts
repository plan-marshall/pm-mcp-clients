import type { RuntimeState } from '../stores/runtime';

/**
 * The runtime state a view test starts from.
 *
 * A test names only the fields it exercises; every other field keeps the value below, so a
 * field added to {@link RuntimeState} reaches the fixtures without touching every test.
 *
 * @param overrides the fields this test sets
 * @returns a complete runtime state
 */
export function makeRuntimeState(overrides: Partial<RuntimeState> = {}): RuntimeState {
  return {
    isInitialized: true,
    isPaired: true,
    isOnboarded: true,
    deviceLabel: 'test-device',
    machineName: 'test-machine',
    currentRoute: 'overview',
    currentState: null,
    isRevoked: false,
    revokedReason: '',
    isOffline: false,
    streamState: 'connected',
    isListenerClosed: false,
    isDraining: false,
    isStopped: false,
    activeStep: 1,
    completedSteps: 0,
    unreadReportsCount: 0,
    openDecisionsCount: 0,
    ...overrides,
  };
}
