<script lang="ts">
  import DashboardPage from '../../../lib/templates/DashboardPage.svelte';

  /**
   * A control for the page-width guard's root lookup: the stylesheet carries a rule for a class no
   * element uses. The guard must report that the class is not on an element, never pass silently.
   */
</script>

<DashboardPage>
  <div class="control-real-root"></div>
</DashboardPage>

<style>
  .control-ghost-root {
    color: inherit;
  }
</style>
