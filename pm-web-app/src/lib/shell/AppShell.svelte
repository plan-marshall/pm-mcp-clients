<script lang="ts">
  import { type Snippet } from 'svelte';
  import type { ThemeChoice } from '../theme';
  import NavRail from './NavRail.svelte';
  import TopBar from './TopBar.svelte';
  import BottomBar from './BottomBar.svelte';
  import CommandPalette from './CommandPalette.svelte';
  import { readRailCollapsed, writeRailCollapsed } from './nav';

  interface Props {
    /** The theme this browser chose. */
    themeChoice: ThemeChoice;
    /** Applies a theme the person picked. */
    onChooseTheme: (choice: ThemeChoice) => void;
    /** The notices above the view (a drain, a stop), when the frame has one. */
    notices?: Snippet;
    /** The view the route shows. */
    content: Snippet;
  }

  let { themeChoice, onChooseTheme, notices, content }: Props = $props();

  /** Whether the rail shows icons only; this browser keeps the choice. */
  let collapsed = $state(readRailCollapsed());

  /** Turns the rail between its full and its icon width, and keeps the choice. */
  function toggle(): void {
    collapsed = !collapsed;
    writeRailCollapsed(collapsed);
  }
</script>

<div class="app-shell">
  <NavRail {collapsed} onToggle={toggle} {themeChoice} {onChooseTheme} />

  <div class="app-shell__main">
    <TopBar />
    {#if notices}
      {@render notices()}
    {/if}
    <main class="app-body">{@render content()}</main>
  </div>

  <BottomBar {themeChoice} {onChooseTheme} />

  <CommandPalette />
</div>

<style>
  .app-shell {
    display: flex;
    flex: 1 1 auto;
    min-block-size: 100vh;
    min-inline-size: 0;
    background-color: var(--color-bg-app);
  }

  .app-shell__main {
    display: flex;
    flex-direction: column;
    flex: 1 1 auto;
    min-inline-size: 0;
    min-block-size: 100vh;
  }

  .app-body {
    flex: 1 1 auto;
    display: flex;
    flex-direction: column;
    min-inline-size: 0;
    min-block-size: 0;
  }

  @media (max-width: 767px) {
    /* The rail is hidden, so the frame is one column: the view above, the bottom bar below. */
    .app-shell {
      flex-direction: column;
      min-block-size: 100vh;
    }

    .app-shell__main {
      flex: 1 1 auto;
      min-block-size: 0;
    }
  }
</style>
