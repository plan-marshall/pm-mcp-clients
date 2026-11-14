<script lang="ts">
  import GapStrip from '../../lib/templates/GapStrip.svelte';
  import type { Level } from '../../lib/templates/TriagePage.svelte';

  /**
   * A test fixture for the gap strip: the hidden levels a test hands in, names or marks, and the
   * acts the strip reports. The texts arrive as props, so the catalogue guard sees no literal copy
   * in a screen.
   */
  interface Props {
    hidden: Array<{ level: Level; index: number }>;
    names: boolean;
    label: string;
    placement?: 'start' | 'end';
    objectTitle?: string;
    onPeek?: (index: number) => void;
    onReturn?: () => void;
  }

  let {
    hidden,
    names,
    label,
    placement = 'start',
    objectTitle = '',
    onPeek = () => {},
    onReturn = () => {},
  }: Props = $props();
</script>

<GapStrip {hidden} {names} {label} {placement} {objectTitle} {onPeek} {onReturn} />
