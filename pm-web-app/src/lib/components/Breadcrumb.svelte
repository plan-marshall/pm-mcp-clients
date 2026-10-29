<script module lang="ts">
  /** One segment of the trail: what it says and where it leads, when it leads anywhere. */
  export interface Crumb {
    /** The visible segment, from the view's catalogue key or a formatted id. */
    label: string;
    /** The address the segment leads to; the last segment carries none. */
    href?: string;
    /**
     * Whether the label is an identifier. Only an identifier is shortened in its middle on a phone:
     * a sentence a person reads is never cut, however long it is.
     */
    id?: boolean;
  }
</script>

<script lang="ts">
  import { onMount } from 'svelte';
  import { shortenMiddle } from '../utils/shortenMiddle';

  /**
   * The trail to the page a person stands on (VD-P-05, VD-S-03): links in
   * `--color-text-secondary`, the last segment in `--color-text-primary` and marked as the current
   * page.
   *
   * A phone has little room for the trail, so a segment that is longer than a row can hold is
   * shortened in its middle — the head names the kind, the tail tells two ids apart — while a
   * desktop draws every segment in full. A trail that still does not fit wraps rather than clipping
   * a word.
   */
  export let crumbs: Crumb[] = [];
  /** The accessible name of the trail, from the view's catalogue key. */
  export let label: string;

  /** The longest segment a phone row keeps; the ellipsis is part of it. */
  const PHONE_MAX = 24;

  /** Whether the viewport is a phone, where the trail has one row. */
  let phone = false;

  onMount(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return;
    }
    const query = window.matchMedia('(max-width: 767px)');
    phone = query.matches;
    const onChange = (event: MediaQueryListEvent): void => {
      phone = event.matches;
    };
    query.addEventListener('change', onChange);
    return () => query.removeEventListener('change', onChange);
  });

  /** The rows the trail draws; the shortening follows the viewport, so the template reads a value. */
  $: rows = crumbs.map((crumb, index) => {
    // Only an identifier the view classifies as such is shortened: a label is copy until the view
    // says otherwise, and copy is never cut — a catalogue label may be one long word.
    const shown = phone && crumb.id === true ? shortenMiddle(crumb.label, PHONE_MAX) : crumb.label;
    return {
      ...crumb,
      shown,
      shortened: shown !== crumb.label,
      current: index === crumbs.length - 1,
      leads: index < crumbs.length - 1 && crumb.href !== undefined,
    };
  });
</script>

<nav class="breadcrumb" aria-label={label}>
  <ol class="breadcrumb__list">
    {#each rows as row}
      <li class="breadcrumb__item">
        {#if row.leads}
          <!-- The shortened text is the visual form; the accessible name stays the whole label. -->
          <a
            class="breadcrumb__link"
            href={row.href}
            aria-label={row.shortened ? row.label : undefined}
          >
            {row.shown}
          </a>
        {:else if row.shortened}
          <span class="breadcrumb__current" aria-current={row.current ? 'page' : undefined}>
            <span aria-hidden="true">{row.shown}</span>
            <span class="sr-only">{row.label}</span>
          </span>
        {:else}
          <span class="breadcrumb__current" aria-current={row.current ? 'page' : undefined}>
            {row.label}
          </span>
        {/if}
      </li>
    {/each}
  </ol>
</nav>

<style>
  .breadcrumb__list {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
    margin: 0;
    padding: 0;
    list-style: none;
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
  }

  .breadcrumb__item {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    min-inline-size: 0;
    overflow-wrap: anywhere;
  }

  .breadcrumb__item + .breadcrumb__item::before {
    content: '/';
    color: var(--color-text-muted);
  }

  .breadcrumb__link {
    color: var(--color-text-secondary);
    text-decoration: none;
  }

  .breadcrumb__link:hover {
    color: var(--color-text-primary);
    text-decoration: underline;
  }

  .breadcrumb__current {
    color: var(--color-text-primary);
  }
</style>
