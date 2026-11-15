<script lang="ts">
  import StatusLabel from '../../lib/components/StatusLabel.svelte';
  import Card from '../../lib/components/Card.svelte';

  /**
   * Template fixture for StatusLabel (D6 amendment): renders StatusLabel with `fixed`
   * directly inside a Svelte 5 snippet and directly inside a component's slotted children.
   */
  export let title: string = 'dev.section.status_label';
  export let items: Array<{
    tone: 'neutral' | 'running' | 'attention' | 'danger' | 'success';
    label: string;
  }> = [
    { tone: 'success', label: 'done' },
    { tone: 'running', label: 'running' },
    { tone: 'attention', label: 'waiting for you' },
  ];
</script>

{#snippet statusItem(item: { tone: 'neutral' | 'running' | 'attention' | 'danger' | 'success'; label: string })}
  <div class="snippet-row">
    <StatusLabel tone={item.tone} label={item.label} fixed />
  </div>
{/snippet}

<div class="status-label-fixture">
  <!-- 1. Rendered inside a snippet -->
  <div class="snippet-container">
    {#each items as item}
      {@render statusItem(item)}
    {/each}
  </div>

  <!-- 2. Rendered directly inside a component's children -->
  <Card {title}>
    {#each items as item}
      <div class="children-row">
        <StatusLabel tone={item.tone} label={item.label} fixed />
      </div>
    {/each}
  </Card>
</div>
