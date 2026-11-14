<script lang="ts">
  import TriagePage, { type Level, type PinnedRow } from '../../lib/templates/TriagePage.svelte';

  /**
   * A test fixture for the column engine: a chain of levels, each column's body a text the test
   * hands in, the object column's body separately, and an optional pinned row. It is the position a
   * view draws itself in — not a component of the page. The texts arrive as props, so the catalogue
   * guard sees no literal copy in a screen.
   */
  interface Props {
    levels: Level[];
    label: string;
    bodies: string[];
    objectBody?: string;
    withObject?: boolean;
    pinned?: PinnedRow | null;
  }

  let { levels, label, bodies, objectBody = '', withObject = true, pinned = null }: Props = $props();

  /**
   * @param level a level of the chain
   * @returns the text of its column's body
   */
  function bodyOf(level: Level): string {
    const index = levels.findIndex((entry) => entry.key === level.key);
    return bodies[index] ?? level.title;
  }
</script>

{#snippet columnPane(level: Level)}
  <p class="fixture-column">{bodyOf(level)}</p>
{/snippet}

{#snippet objectPane(level: Level)}
  <p class="fixture-object">{objectBody || bodyOf(level)}</p>
{/snippet}

<TriagePage {levels} {label} {pinned} column={columnPane} object={withObject ? objectPane : undefined} />
