import { describe, it, expect, beforeEach } from 'vitest';
import { get } from 'svelte/store';
import { closeLessonSource, lessonSourceModalStore, openLessonSource } from './lessonSource';

describe('the lesson source dialogue store', () => {
  beforeEach(() => {
    lessonSourceModalStore.set({ open: false, projectId: '' });
  });

  it('opens the dialogue for the project whose lessons are listed', () => {
    // Act
    openLessonSource('pm-mcp-clients');

    // Assert
    expect(get(lessonSourceModalStore)).toEqual({ open: true, projectId: 'pm-mcp-clients' });
  });

  it('closes the dialogue and keeps the project it was opened for', () => {
    // Arrange
    openLessonSource('pm-mcp-core');

    // Act
    closeLessonSource();

    // Assert
    expect(get(lessonSourceModalStore)).toEqual({ open: false, projectId: 'pm-mcp-core' });
  });
});
