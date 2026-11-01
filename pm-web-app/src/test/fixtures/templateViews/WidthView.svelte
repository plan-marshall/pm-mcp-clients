<script lang="ts">
  import DashboardPage from '../../../lib/templates/DashboardPage.svelte';

  /**
   * The negative control of the page-width guard: a view that sits on a template and still sets a
   * `max-width` on its page root. The guard must report it; a guard that stays green on this file
   * is not checking anything.
   */
</script>

<DashboardPage>
  <div class="control-page"></div>
</DashboardPage>

<style>
  .control-page {
    max-width: 1200px;
  }
</style>
