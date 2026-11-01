<script lang="ts">
  import DashboardPage from '../../../lib/templates/DashboardPage.svelte';

  /**
   * A control for the page-width guard's scope: the root carries no cap, a child carries an inline
   * `max-width`. The guard must ignore the child, because a capped part is not a capped page.
   */
</script>

<DashboardPage>
  <div class="control-child-root">
    <span style="max-width: 1200px"></span>
  </div>
</DashboardPage>

<style>
  .control-child-root {
    color: inherit;
  }
</style>
