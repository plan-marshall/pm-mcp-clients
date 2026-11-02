<script lang="ts">
  import TriagePage from '../../lib/templates/TriagePage.svelte';

  /**
   * A test fixture for the slotted `TriagePage`: the filter chips above the list, the list on the
   * left and the detail on the right. It is the position a triage view draws itself in — not a
   * component of the page. The texts arrive as props, so the catalogue guard sees no literal copy
   * in a screen.
   */
  export let filters: string;
  export let list: string;
  export let detail: string;
  export let selected = false;
</script>

<TriagePage {selected}>
  <span slot="filters">{filters}</span>
  <article slot="list">{list}</article>
  <article slot="detail">{detail}</article>
</TriagePage>
