<script lang="ts">
  import DashboardPage from '../../../lib/templates/DashboardPage.svelte';

  /**
   * The third control of the page-width guard: a view that sits on a template and caps its page
   * root with an inline `style` instead of a stylesheet rule. The guard must report it too.
   */
</script>

<DashboardPage>
  <div class="control-inline-style" style="max-width: 1200px"></div>
</DashboardPage>

<style>
  .control-inline-style {
    color: inherit;
  }
</style>
