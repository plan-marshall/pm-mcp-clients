<script lang="ts">
  import DashboardPage from '../../../lib/templates/DashboardPage.svelte';

  /**
   * A control for the page-width guard's text read: the root carries the word `max-width` in a CSS
   * comment and as a custom property, but sets no cap. The guard must not report it.
   */
</script>

<DashboardPage>
  <div class="control-comment-root"></div>
</DashboardPage>

<style>
  .control-comment-root {
    /* max-width: 1200px is deliberately not set */
    --max-width: 1200px;
  }
</style>
