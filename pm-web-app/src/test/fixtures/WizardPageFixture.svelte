<script lang="ts">
  import WizardPage from '../../lib/templates/WizardPage.svelte';

  /**
   * A test fixture for the slotted `WizardPage`: the step bar, the one card, the footer's
   * Back · secondary · primary, and the side column of the chat with its acts. It is the position a
   * wizard draws itself in — not a component of the page. The texts arrive as props, so the
   * catalogue guard sees no literal copy in a screen.
   */
  interface Props {
    steps: string;
    body: string;
    backAct: string;
    secondaryAct: string;
    primaryAct: string;
    sideBody?: string;
    sideOpen?: boolean;
    onHideSide?: () => void;
  }

  let {
    steps,
    body,
    backAct,
    secondaryAct,
    primaryAct,
    sideBody = '',
    sideOpen = false,
    onHideSide = () => {},
  }: Props = $props();
</script>

{#snippet sidePane()}
  <p class="fixture-side">{sideBody}</p>
{/snippet}

<WizardPage {sideOpen} {onHideSide} side={sideBody ? sidePane : undefined}>
  <span slot="steps">{steps}</span>
  <p>{body}</p>
  <button slot="back" type="button">{backAct}</button>
  <button slot="secondary" type="button">{secondaryAct}</button>
  <button slot="primary" type="button">{primaryAct}</button>
</WizardPage>
