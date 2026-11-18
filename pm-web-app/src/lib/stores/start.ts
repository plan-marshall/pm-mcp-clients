import { runtimeStore } from './runtime';

/** The kind of work a start opens. */
export type StartKind = 'plan' | 'epic';

/**
 * Opens the start screen for a project, or the entry that is already open.
 *
 * @param projectId the project the new work starts in
 * @param kind whether the start is a plan's or an epic's
 */
export function openStartWork(projectId: string, kind: StartKind = 'plan'): void {
  runtimeStore.update((state) => ({
    ...state,
    currentRoute: 'start',
    startProjectId: projectId,
    startEntryId: null,
    startKind: kind,
  }));
}

/**
 * Opens an entry that is already open, addressed by its id.
 *
 * @param entryId the entry
 * @param projectId the project, when the caller knows it
 */
export function openStartEntry(entryId: string, projectId?: string): void {
  runtimeStore.update((state) => ({
    ...state,
    currentRoute: 'start',
    startProjectId: projectId ?? state.startProjectId ?? null,
    startEntryId: entryId,
  }));
}
