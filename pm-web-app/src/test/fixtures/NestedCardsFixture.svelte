<script lang="ts">
  import Card from '../../lib/components/Card.svelte';

  /**
   * A test fixture that draws a card inside a card, which the design system refuses: the inner card
   * must fall back to a divider rather than draw a second surface.
   */
  export let outerTitle: string;
  export let innerTitle: string;
  export let body: string;
</script>

<Card title={outerTitle}>
  <p>{body}</p>
  <Card title={innerTitle}>
    <p>{body}</p>
  </Card>
</Card>
