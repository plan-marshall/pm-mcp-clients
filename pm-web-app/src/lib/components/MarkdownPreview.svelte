<script lang="ts">
  /**
   * A plain-text preview of the operator's Markdown.
   *
   * It never renders HTML: headings, list items, fenced code and paragraphs become elements with
   * their text as Svelte text nodes, so a description can carry no markup that the browser runs.
   */
  interface Block {
    kind: 'heading' | 'paragraph' | 'item' | 'code';
    level: number;
    text: string;
  }

  export let value = '';

  function blocksOf(markdown: string): Block[] {
    const blocks: Block[] = [];
    let inCode = false;
    for (const rawLine of markdown.split('\n')) {
      const line = rawLine.trimEnd();
      if (line.trim().startsWith('```')) {
        inCode = !inCode;
        continue;
      }
      if (inCode) {
        blocks.push({ kind: 'code', level: 0, text: rawLine });
        continue;
      }
      const heading = /^(#{1,6})\s+(.*)$/.exec(line);
      if (heading?.[1]) {
        blocks.push({ kind: 'heading', level: heading[1].length, text: heading[2] ?? '' });
        continue;
      }
      const item = /^\s*[-*+]\s+(.*)$/.exec(line);
      if (item?.[1]) {
        blocks.push({ kind: 'item', level: 0, text: item[1] });
        continue;
      }
      if (line.trim()) {
        blocks.push({ kind: 'paragraph', level: 0, text: line });
      }
    }
    return blocks;
  }

  $: blocks = blocksOf(value);
</script>

<div class="markdown-preview" data-testid="markdown-preview">
  {#each blocks as block}
    {#if block.kind === 'heading'}
      <p class="markdown-preview__heading" data-level={block.level}>{block.text}</p>
    {:else if block.kind === 'item'}
      <p class="markdown-preview__item">• {block.text}</p>
    {:else if block.kind === 'code'}
      <pre class="markdown-preview__code">{block.text}</pre>
    {:else}
      <p class="markdown-preview__paragraph">{block.text}</p>
    {/if}
  {/each}
</div>

<style>
  .markdown-preview {
    display: flex;
    flex-direction: column;
    gap: var(--space-1, 0.25rem);
    padding: var(--space-2, 0.5rem);
    border: 1px solid var(--color-border, #d1d5db);
    border-radius: var(--radius-md, 0.5rem);
    background-color: var(--color-bg-subtle, #f9fafb);
    max-height: 16rem;
    overflow-y: auto;
  }

  .markdown-preview p,
  .markdown-preview pre {
    margin: 0;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
  }

  .markdown-preview__heading {
    font-weight: 700;
  }

  .markdown-preview__heading[data-level='1'] {
    font-size: var(--text-lg, 1.125rem);
  }

  .markdown-preview__item {
    padding-left: var(--space-2, 0.5rem);
  }

  .markdown-preview__code {
    font-family: var(--font-mono, monospace);
    font-size: var(--text-sm, 0.875rem);
  }
</style>
