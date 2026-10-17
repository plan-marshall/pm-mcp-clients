import { writable } from 'svelte/store';

export interface GitSourceModalState {
  open: boolean;
  projectId: string;
}

export const gitSourceModalStore = writable<GitSourceModalState>({
  open: false,
  projectId: '',
});

export function openGitSource(projectId: string): void {
  gitSourceModalStore.set({ open: true, projectId });
}

export function closeGitSource(): void {
  gitSourceModalStore.update((state) => ({ ...state, open: false }));
}
