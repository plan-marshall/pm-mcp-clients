<script lang="ts">
  import DashboardPage from '../../../lib/templates/DashboardPage.svelte';

  /**
   * A control for the page-width guard's directive read: the root caps its width with a
   * `style:max-width` directive rather than a stylesheet rule or a `style` attribute.
   */
</script>

<DashboardPage>
  <div class="control-directive-root" style:max-width={'900px'}></div>
</DashboardPage>

<style>
  .control-directive-root {
    color: inherit;
  }
</style>
