<script lang="ts">
  import Card from '../../lib/components/Card.svelte';
  import Button from '../../lib/components/Button.svelte';

  /**
   * A test fixture for the slotted `Card`: a head with its title and one act, and a body. It is the
   * position a view draws the card in — not a component of the page.
   */
  export let title: string;
  export let body: string;
  export let actLabel: string;
</script>

<Card {title}>
  <p>{body}</p>
  <svelte:fragment slot="actions">
    <Button variant="ghost">{actLabel}</Button>
  </svelte:fragment>
</Card>
