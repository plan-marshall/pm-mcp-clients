<script lang="ts">
  import WizardPage from '../../lib/templates/WizardPage.svelte';

  /**
   * A test fixture for the slotted `WizardPage`: the step bar, the one card and the footer's
   * Back · secondary · primary. It is the position a wizard draws itself in — not a component of
   * the page. The texts arrive as props, so the catalogue guard sees no literal copy in a screen.
   */
  export let steps: string;
  export let body: string;
  export let backAct: string;
  export let secondaryAct: string;
  export let primaryAct: string;
</script>

<WizardPage>
  <span slot="steps">{steps}</span>
  <p>{body}</p>
  <button slot="back" type="button">{backAct}</button>
  <button slot="secondary" type="button">{secondaryAct}</button>
  <button slot="primary" type="button">{primaryAct}</button>
</WizardPage>
