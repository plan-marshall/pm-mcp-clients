<script lang="ts">
  import AppRouter from '../../lib/views/AppRouter.svelte';

  // The store decides what is shown; the route page only mounts the dispatcher, so a pairing or
  // an onboarding that moves on renders its next view without the address bar pulling it back.
</script>

<AppRouter />
