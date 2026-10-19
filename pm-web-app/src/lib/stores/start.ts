import { writable } from 'svelte/store';
import { runtimeStore } from './runtime';

/** The kind of work a start opens. */
export type StartKind = 'plan' | 'epic';

export interface StartModalState {
  open: boolean;
  projectId: string;
  kind: StartKind;
}

/**
 * Kept for the components that still render the start dialogue directly.
 *
 * The app itself opens the dedicated start screen (`/start`, `/projects/{id}/start`,
 * `/entries/{id}`); this store only says whether such a dialogue should be mounted on its own.
 */
export const startModalStore = writable<StartModalState>({
  open: false,
  projectId: '',
  kind: 'plan',
});

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

export function closeStartWork(): void {
  startModalStore.update((state) => ({ ...state, open: false }));
}
