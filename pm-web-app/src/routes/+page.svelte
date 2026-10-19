<script lang="ts">
  import { onMount } from 'svelte';
  import { adoptRouteFromLocation } from '../lib/stores/runtime';
  import AppRouter from '../lib/views/AppRouter.svelte';
  import StartWorkModal from '../lib/components/StartWorkModal.svelte';
  import LessonSourceModal from '../lib/components/LessonSourceModal.svelte';
  import GitSourceIntakeModal from '../lib/components/GitSourceIntakeModal.svelte';
  import { startModalStore, closeStartWork } from '../lib/stores/start';
  import { lessonSourceModalStore, closeLessonSource } from '../lib/stores/lessonSource';
  import { gitSourceModalStore, closeGitSource } from '../lib/stores/gitSource';

  onMount(() => {
    // The address bar is the route: a reload, a bookmark, or the browser's back returns here.
    adoptRouteFromLocation();
    const onPop = () => adoptRouteFromLocation();
    window.addEventListener('popstate', onPop);
    return () => window.removeEventListener('popstate', onPop);
  });
</script>

<AppRouter />

<StartWorkModal
  open={$startModalStore.open}
  projectId={$startModalStore.projectId}
  kind={$startModalStore.kind}
  onClose={closeStartWork}
  onStarted={() => {}}
/>

<LessonSourceModal
  open={$lessonSourceModalStore.open}
  projectId={$lessonSourceModalStore.projectId}
  onClose={closeLessonSource}
  onStarted={() => {}}
/>

<GitSourceIntakeModal
  open={$gitSourceModalStore.open}
  projectId={$gitSourceModalStore.projectId}
  onClose={closeGitSource}
  onCompleted={() => {}}
/>
