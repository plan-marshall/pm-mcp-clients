import { writable } from 'svelte/store';

export type StartKind = 'plan' | 'epic';

export interface StartModalState {
  open: boolean;
  projectId: string;
  kind: StartKind;
}

export const startModalStore = writable<StartModalState>({
  open: false,
  projectId: '',
  kind: 'plan',
});

export function openStartWork(projectId: string, kind: StartKind = 'plan'): void {
  startModalStore.set({ open: true, projectId, kind });
}

export function closeStartWork(): void {
  startModalStore.update((state) => ({ ...state, open: false }));
}
