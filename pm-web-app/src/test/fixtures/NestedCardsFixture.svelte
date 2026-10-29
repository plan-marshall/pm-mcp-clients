<script lang="ts">
  import Card from '../../lib/components/Card.svelte';
  import Button from '../../lib/components/Button.svelte';

  /**
   * A test fixture that draws a card inside a card, which the design system refuses: the inner card
   * must fall back to a divider rather than draw a second surface — and must keep the acts of its
   * own head.
   */
  export let outerTitle: string;
  export let innerTitle: string;
  export let body: string;
  export let innerActLabel: string;
</script>

<Card title={outerTitle}>
  <p>{body}</p>
  <Card title={innerTitle}>
    <p>{body}</p>
    <svelte:fragment slot="actions">
      <Button variant="ghost" size="sm">{innerActLabel}</Button>
    </svelte:fragment>
  </Card>
</Card>
