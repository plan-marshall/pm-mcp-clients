<script lang="ts">
  import { t } from '../i18n';
  import Dialog from './Dialog.svelte';
  import Button from './Button.svelte';

  /**
   * The question that asks for the read of another repository's issues (`shared/starting-work.adoc`
   * § #issue): the design `Dialog` whose title is the question and whose three answers are the
   * runtime's acts. The narrowest grant is the primary act, the wider one secondary, the refusal the
   * ghost act — the design names no role for these three answers, so the choice is the lane's and it
   * is recorded in the pull request.
   */
  export let open = false;
  export let repository = '';
  export let onGrant: (scope: 'once' | 'project') => void = () => {};
  export let onRefuse: () => void = () => {};

  function grant(scope: 'once' | 'project'): void {
    onGrant(scope);
  }
</script>

<Dialog
  {open}
  title={$t('start.issue.grant.title', { repository })}
  onClose={onRefuse}
  testid="grant-modal"
>
  <svelte:fragment slot="footer">
    <Button variant="ghost" data-testid="grant-no" on:click={onRefuse}>
      {$t('start.issue.grant.no')}
    </Button>
    <Button variant="secondary" data-testid="grant-project" on:click={() => grant('project')}>
      {$t('start.issue.grant.project')}
    </Button>
    <Button variant="primary" data-testid="grant-once" on:click={() => grant('once')}>
      {$t('start.issue.grant.once')}
    </Button>
  </svelte:fragment>
</Dialog>
