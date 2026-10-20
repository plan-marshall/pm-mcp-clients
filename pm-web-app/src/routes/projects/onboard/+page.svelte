<script lang="ts">
  import { onMount } from 'svelte';
  import AppRouter from '../../../lib/views/AppRouter.svelte';
  import { adoptRouteFromLocation } from '../../../lib/stores/runtime';

  // `/projects/onboard` opens a project's setup; the runtime reports its states, which the
  // onboarding screen renders like the machine's.
  onMount(() => {
    adoptRouteFromLocation();
  });
</script>

<AppRouter />
