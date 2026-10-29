<script lang="ts">
  import Card from '../../lib/components/Card.svelte';

  /**
   * A test fixture that draws a `Card` inside an element a view styles as `.card` of its own. The
   * view's class is not the component: the card inside it is a surface, not a nested part.
   */
  export let title: string;
  export let body: string;
</script>

<div class="card">
  <Card {title}>
    <p>{body}</p>
  </Card>
</div>
