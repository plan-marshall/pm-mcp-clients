<script lang="ts">
  import ObjectPage from '../../lib/templates/ObjectPage.svelte';

  /**
   * A test fixture for the slotted `ObjectPage`: the parts of the head, the attention strip, the
   * tabs and the body. It is the position an object view draws itself in — not a component of the
   * page. The texts arrive as props, so the catalogue guard sees no literal copy in a screen.
   */
  export let kind: string;
  export let path: string;
  export let title: string;
  export let idChip: string;
  export let state: string;
  export let primaryAct: string;
  export let secondaryAct: string;
  export let overflowAct: string;
  export let attention: string;
  export let tabs: string;
  export let body: string;
</script>

<ObjectPage>
  <span slot="kind">{kind}</span>
  <span slot="path">{path}</span>
  <span slot="title">{title}</span>
  <span slot="id">{idChip}</span>
  <span slot="state">{state}</span>
  <button slot="primary" type="button">{primaryAct}</button>
  <button slot="secondary" type="button">{secondaryAct}</button>
  <button slot="overflow" type="button">{overflowAct}</button>
  <p slot="attention">{attention}</p>
  <span slot="tabs">{tabs}</span>
  <p>{body}</p>
</ObjectPage>
