import { describe, it, expect } from 'vitest';
import { get } from 'svelte/store';
import { createViewLayout, readLayout, writeLayout, layoutStorageKey } from './viewLayout';

describe('viewLayout', () => {
  it('defaults to comfortable when the browser holds no choice', () => {
    expect(readLayout('reports')).toBe('comfortable');
    expect(layoutStorageKey('reports')).toBe('pm_view_layout_reports');
  });

  it('persists a choice under the view-specific key', () => {
    writeLayout('decisions', 'compact');
    expect(localStorage.getItem('pm_view_layout_decisions')).toBe('compact');
    expect(readLayout('decisions')).toBe('compact');
  });

  it('falls back for an unknown stored value instead of throwing', () => {
    localStorage.setItem('pm_view_layout_reports', 'enormous');
    expect(readLayout('reports')).toBe('comfortable');
    expect(readLayout('reports', 'compact')).toBe('compact');
  });

  it('creates a store that reads, updates, and persists its layout', () => {
    writeLayout('epic', 'split');
    const layout = createViewLayout('epic');
    expect(get(layout)).toBe('split');

    layout.set('compact');
    expect(localStorage.getItem('pm_view_layout_epic')).toBe('compact');

    layout.update(() => 'comfortable');
    expect(get(layout)).toBe('comfortable');
    expect(localStorage.getItem('pm_view_layout_epic')).toBe('comfortable');
  });
});
