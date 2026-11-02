<script lang="ts">
  import WizardPage from '../../lib/templates/WizardPage.svelte';

  /**
   * A test fixture for a `WizardPage` a view fills without a footer: the step bar and the card
   * only. The template must not draw an empty footer row for it. The texts arrive as props, so the
   * catalogue guard sees no literal copy in a screen.
   */
  export let steps: string;
  export let body: string;
</script>

<WizardPage>
  <span slot="steps">{steps}</span>
  <p>{body}</p>
</WizardPage>
