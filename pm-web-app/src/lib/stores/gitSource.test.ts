import { describe, it, expect, beforeEach } from 'vitest';
import { get } from 'svelte/store';
import { closeGitSource, gitSourceModalStore, openGitSource } from './gitSource';

describe('the git source dialogue store', () => {
  beforeEach(() => {
    gitSourceModalStore.set({ open: false, projectId: '' });
  });

  it('opens the dialogue for the project the source is added to', () => {
    // Act
    openGitSource('pm-mcp-clients');

    // Assert
    expect(get(gitSourceModalStore)).toEqual({ open: true, projectId: 'pm-mcp-clients' });
  });

  it('closes the dialogue and keeps the project it was opened for', () => {
    // Arrange
    openGitSource('pm-mcp-core');

    // Act
    closeGitSource();

    // Assert
    expect(get(gitSourceModalStore)).toEqual({ open: false, projectId: 'pm-mcp-core' });
  });
});
