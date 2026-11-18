<script lang="ts">
  import RolesReview from './RolesReview.svelte';
  import type { ScreenProps } from './screens';

  /** The role assignment (`roles.review`, `roles.reconfigure`). */
  let { view }: ScreenProps = $props();
</script>

<RolesReview {view} />
