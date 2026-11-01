<script lang="ts">
  import TriagePage from '../../lib/templates/TriagePage.svelte';

  /**
   * A test fixture for a `TriagePage` a view fills without filter chips: the list and the detail
   * only. The template must not draw the filters row for it. The texts arrive as props, so the
   * catalogue guard sees no literal copy in a screen.
   */
  export let list: string;
  export let detail: string;
</script>

<TriagePage>
  <article slot="list">{list}</article>
  <article slot="detail">{detail}</article>
</TriagePage>
