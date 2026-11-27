<script lang="ts">
  import type { Token, Tokens } from 'marked';
  import {
    formatExternalLabel,
    hrefForTurnRef,
    isMermaidLanguage,
    parseSafeWebUrl,
    renderMermaidBlock,
    splitTextWithRefs,
    tokenizeMarkdown,
  } from '../chat/markdown';
  import type { ChatSubject, TurnRef } from '../chat/types';
  import { t } from '../i18n';
  import Button from './Button.svelte';
  import ExternalLink from './ExternalLink.svelte';
  import Link from './Link.svelte';

  export interface ChatMarkdownProps {
    text: string;
    refs?: TurnRef[];
    subject?: ChatSubject | null;
    streaming?: boolean;
  }

  let { text, refs = [], subject = null, streaming = false }: ChatMarkdownProps = $props();

  let tokens = $derived(tokenizeMarkdown(text, streaming));

  function extractPlainText(inlineTokens: Token[] | undefined): string {
    if (!inlineTokens) return '';
    return inlineTokens
      .map((tok) => {
        if ('tokens' in tok && Array.isArray(tok.tokens)) {
          return extractPlainText(tok.tokens);
        }
        return 'text' in tok && typeof tok.text === 'string' ? tok.text : tok.raw;
      })
      .join('');
  }

  function cellAlignClass(align: 'center' | 'left' | 'right' | null): string {
    if (align === 'center') return 'chat-markdown__cell--center';
    if (align === 'right') return 'chat-markdown__cell--right';
    return 'chat-markdown__cell--left';
  }

  function copyBlock(code: string): void {
    void navigator.clipboard?.writeText(code);
  }
</script>

{#snippet inlineNodes(inlineTokens: Token[] | undefined)}
  {#if inlineTokens}
    {#each inlineTokens as token (token)}
      {#if token.type === 'strong'}
        <strong>{@render inlineNodes((token as Tokens.Strong).tokens)}</strong>
      {:else if token.type === 'em'}
        <em>{@render inlineNodes((token as Tokens.Em).tokens)}</em>
      {:else if token.type === 'codespan'}
        <code class="chat-markdown__inline-code">{(token as Tokens.Codespan).text}</code>
      {:else if token.type === 'br'}
        <br />
      {:else if token.type === 'link'}
        {@const linkToken = token as Tokens.Link}
        {@const url = parseSafeWebUrl(linkToken.href)}
        {#if url}
          <ExternalLink
            href={url.href}
            label={formatExternalLabel(extractPlainText(linkToken.tokens) || linkToken.text, url)}
          />
        {:else}
          {linkToken.raw}
        {/if}
      {:else if token.type === 'image'}
        {@const imageToken = token as Tokens.Image}
        {@const url = parseSafeWebUrl(imageToken.href)}
        {#if url}
          <ExternalLink
            href={url.href}
            label={formatExternalLabel(imageToken.text || imageToken.href, url)}
          />
        {:else}
          {imageToken.raw}
        {/if}
      {:else if token.type === 'text'}
        {@const textToken = token as Tokens.Text}
        {#if 'tokens' in textToken && Array.isArray(textToken.tokens) && textToken.tokens.length > 0}
          {@render inlineNodes(textToken.tokens)}
        {:else}
          {#each splitTextWithRefs(textToken.text, refs) as seg (seg)}
            {#if seg.kind === 'ref' && seg.ref}
              <Link
                look="chip"
                href={hrefForTurnRef(seg.ref, subject)}
                label={seg.ref.id ?? seg.text}
              />
            {:else}
              {seg.text}
            {/if}
          {/each}
        {/if}
      {:else if token.type === 'escape'}
        {(token as Tokens.Escape).text}
      {:else}
        {token.raw}
      {/if}
    {/each}
  {/if}
{/snippet}

{#snippet blockNode(token: Token)}
  {#if token.type === 'space'}
    <!-- Ignored whitespace separator between blocks -->
  {:else if token.type === 'paragraph'}
    <p class="chat-markdown__p">{@render inlineNodes((token as Tokens.Paragraph).tokens)}</p>
  {:else if token.type === 'blockquote'}
    <blockquote class="chat-markdown__quote">
      {#each (token as Tokens.Blockquote).tokens as inner (inner)}
        {@render blockNode(inner)}
      {/each}
    </blockquote>
  {:else if token.type === 'list'}
    {@const listToken = token as Tokens.List}
    {#if listToken.ordered}
      <ol
        class="chat-markdown__list"
        start={typeof listToken.start === 'number' && listToken.start !== 1
          ? listToken.start
          : undefined}
      >
        {#each listToken.items as item (item)}
          <li class="chat-markdown__list-item">
            {#each item.tokens as inner (inner)}
              {@render blockNode(inner)}
            {/each}
          </li>
        {/each}
      </ol>
    {:else}
      <ul class="chat-markdown__list">
        {#each listToken.items as item (item)}
          <li class="chat-markdown__list-item">
            {#each item.tokens as inner (inner)}
              {@render blockNode(inner)}
            {/each}
          </li>
        {/each}
      </ul>
    {/if}
  {:else if token.type === 'text'}
    {@const textBlock = token as Tokens.Text}
    {#if 'tokens' in textBlock && Array.isArray(textBlock.tokens)}
      {@render inlineNodes(textBlock.tokens)}
    {:else}
      {#each splitTextWithRefs(textBlock.text, refs) as seg (seg)}
        {#if seg.kind === 'ref' && seg.ref}
          <Link
            look="chip"
            href={hrefForTurnRef(seg.ref, subject)}
            label={seg.ref.id ?? seg.text}
          />
        {:else}
          {seg.text}
        {/if}
      {/each}
    {/if}
  {:else if token.type === 'code'}
    {@const codeToken = token as Tokens.Code}
    {@const mermaid = isMermaidLanguage(codeToken.lang) ? renderMermaidBlock(codeToken.text) : null}
    {@const langLabel = mermaid ? mermaid.lang : (codeToken.lang?.trim() ?? '')}
    {@const codeBody = mermaid ? mermaid.code : codeToken.text}
    <div class="chat-markdown__code-block">
      <div class="chat-markdown__code-head">
        <span class="chat-markdown__code-lang">{langLabel || 'text'}</span>
        <Button variant="ghost" size="sm" icon="act.copy" onclick={() => copyBlock(codeBody)}>
          {$t('web.common.copy')}
        </Button>
      </div>
      <pre class="chat-markdown__pre"><code>{codeBody}</code></pre>
    </div>
  {:else if token.type === 'table'}
    {@const tableToken = token as Tokens.Table}
    <div class="chat-markdown__table-wrap">
      <table class="chat-markdown__table">
        <thead>
          <tr>
            {#each tableToken.header as headerCell, idx (idx)}
              <th class={cellAlignClass(tableToken.align[idx] ?? null)}>
                {@render inlineNodes(headerCell.tokens)}
              </th>
            {/each}
          </tr>
        </thead>
        <tbody>
          {#each tableToken.rows as row, rowIdx (rowIdx)}
            <tr>
              {#each row as cell, colIdx (colIdx)}
                <td class={cellAlignClass(tableToken.align[colIdx] ?? null)}>
                  {@render inlineNodes(cell.tokens)}
                </td>
              {/each}
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  {:else}
    <!-- html and any token outside the closed subset render strictly as source text -->
    <p class="chat-markdown__raw">{token.raw}</p>
  {/if}
{/snippet}

<div class="chat-markdown" data-testid="chat-markdown">
  {#each tokens as token (token)}
    {@render blockNode(token)}
  {/each}
</div>

<style>
  .chat-markdown {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    color: var(--color-text-primary);
    font-size: var(--text-base);
    line-height: var(--line-height-normal);
    overflow-wrap: anywhere;
  }

  .chat-markdown__p,
  .chat-markdown__raw {
    margin: 0;
  }

  .chat-markdown__raw {
    white-space: pre-wrap;
  }

  .chat-markdown__inline-code {
    font-family: var(--font-mono);
    font-size: 0.9em;
    padding: 0 var(--space-1);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-subtle);
  }

  .chat-markdown__quote {
    margin: 0;
    padding: var(--space-1) var(--space-3);
    border-left: 3px solid var(--color-border);
    color: var(--color-text-secondary);
  }

  .chat-markdown__list {
    margin: 0;
    padding-left: var(--space-4);
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .chat-markdown__code-block {
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-subtle);
    overflow: hidden;
  }

  .chat-markdown__code-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--space-1) var(--space-2);
    border-bottom: 1px solid var(--color-border);
  }

  .chat-markdown__code-lang {
    font-family: var(--font-mono);
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .chat-markdown__pre {
    margin: 0;
    padding: var(--space-2) var(--space-3);
    overflow-x: auto;
    font-family: var(--font-mono);
    font-size: var(--text-sm);
    color: var(--color-text-primary);
  }

  .chat-markdown__table-wrap {
    overflow-x: auto;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
  }

  .chat-markdown__table {
    width: max-content;
    min-width: 100%;
    border-collapse: collapse;
    font-size: var(--text-sm);
  }

  .chat-markdown__table th,
  .chat-markdown__table td {
    padding: var(--space-1) var(--space-2);
    border-bottom: 1px solid var(--color-border);
  }

  .chat-markdown__cell--left {
    text-align: left;
  }

  .chat-markdown__cell--center {
    text-align: center;
  }

  .chat-markdown__cell--right {
    text-align: right;
  }
</style>
