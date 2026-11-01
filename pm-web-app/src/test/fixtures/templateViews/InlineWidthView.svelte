<script lang="ts">
  import DashboardPage from '../../../lib/templates/DashboardPage.svelte';

  /**
   * The second control of the page-width guard: a view that sits on a template and caps its page
   * root with `max-inline-size` instead of `max-width`. The guard must report it too.
   */
</script>

<DashboardPage>
  <div class="control-inline-page"></div>
</DashboardPage>

<style>
  .control-inline-page {
    max-inline-size: 1200px;
  }
</style>
