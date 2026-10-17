import { writable } from 'svelte/store';

export interface LessonSourceModalState {
  open: boolean;
  projectId: string;
}

export const lessonSourceModalStore = writable<LessonSourceModalState>({
  open: false,
  projectId: '',
});

export function openLessonSource(projectId: string): void {
  lessonSourceModalStore.set({ open: true, projectId });
}

export function closeLessonSource(): void {
  lessonSourceModalStore.update((state) => ({ ...state, open: false }));
}
