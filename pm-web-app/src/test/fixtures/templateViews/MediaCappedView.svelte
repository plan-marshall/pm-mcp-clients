<script lang="ts">
  import DashboardPage from '../../../lib/templates/DashboardPage.svelte';

  /**
   * A control for the page-width guard's rule sweep: the base rule sets no cap, a media query adds
   * one. The guard must report the media-query cap instead of reading only the first rule.
   */
</script>

<DashboardPage>
  <div class="control-media-root"></div>
</DashboardPage>

<style>
  .control-media-root {
    color: inherit;
  }

  @media (max-width: 900px) {
    .control-media-root {
      max-width: 900px;
    }
  }
</style>
