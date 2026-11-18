import { describe, it, expect, beforeEach } from 'vitest';
import { get } from 'svelte/store';
import { runtimeStore } from './runtime';
import { openStartEntry, openStartWork } from './start';
import { makeRuntimeState } from '../test/runtimeState';

describe('opening the start screen', () => {
  beforeEach(() => {
    runtimeStore.set(makeRuntimeState());
  });

  it.each([
    ['a plan by default', undefined, 'plan'],
    ['a plan', 'plan', 'plan'],
    ['an epic', 'epic', 'epic'],
  ] as const)('starts %s in the project and drops an open entry', (_case, kind, expected) => {
    // Arrange: an entry of another project was open
    runtimeStore.update((state) => ({ ...state, startEntryId: 'ent-1', startProjectId: 'old' }));

    // Act
    openStartWork('pm-mcp-clients', kind);

    // Assert
    expect(get(runtimeStore)).toMatchObject({
      currentRoute: 'start',
      startProjectId: 'pm-mcp-clients',
      startEntryId: null,
      startKind: expected,
    });
  });

  it.each([
    ['the project the caller names', 'pm-mcp-core', 'pm-mcp-core'],
    ['the project already on the start screen', undefined, 'pm-mcp-clients'],
  ])('opens an open entry under %s', (_case, projectId, expected) => {
    // Arrange
    runtimeStore.update((state) => ({ ...state, startProjectId: 'pm-mcp-clients' }));

    // Act
    openStartEntry('ent-7', projectId);

    // Assert
    expect(get(runtimeStore)).toMatchObject({
      currentRoute: 'start',
      startProjectId: expected,
      startEntryId: 'ent-7',
    });
  });

  it('opens an open entry without a project when none is known', () => {
    // Arrange
    runtimeStore.update((state) => ({ ...state, startProjectId: null }));

    // Act
    openStartEntry('ent-7');

    // Assert
    expect(get(runtimeStore)).toMatchObject({ startProjectId: null, startEntryId: 'ent-7' });
  });
});
