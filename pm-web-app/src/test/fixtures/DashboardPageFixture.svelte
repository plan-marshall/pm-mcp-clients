<script lang="ts">
  import DashboardPage from '../../lib/templates/DashboardPage.svelte';

  /**
   * A test fixture for the slotted `DashboardPage`: the title, the time window switch, the
   * "updated" line and the grid of cards. It is the position a dashboard view draws itself in —
   * not a component of the page. The texts arrive as props, so the catalogue guard sees no
   * literal copy in a screen.
   */
  export let title: string;
  export let windowLabel: string;
  export let updated: string;
  export let firstCard: string;
  export let secondCard: string;
</script>

<DashboardPage>
  <h1 slot="title">{title}</h1>
  <div slot="controls"><button type="button">{windowLabel}</button></div>
  <span slot="updated">{updated}</span>
  <article>{firstCard}</article>
  <article>{secondCard}</article>
</DashboardPage>
