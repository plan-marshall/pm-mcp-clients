<script lang="ts">
  import ConsentBox from '../../components/ConsentBox.svelte';
  import { t } from '../../i18n';
  import type { ScreenProps } from './screens';

  /**
   * Every consent state (`<step>.consent`, `<step>.<action>-consent`): what the runtime looks for,
   * where, what it runs, what it never does and the tokens it expects (shared/consents.adoc).
   */
  let { view }: ScreenProps = $props();

  /** Takes the accept link; "remember" answers the same step with the remembered flag. */
  function accept(remember: boolean): void {
    const target = remember
      ? (view.linkBy('remember', 'yes') ?? view.links[view.links.length - 1])
      : (view.linkBy('yes', 'remember') ?? view.links[view.links.length - 1]);
    if (target) view.onLink(target.id, { remember });
  }

  /** Takes the decline link; a state that offers none keeps its last link as the way out. */
  function decline(): void {
    const target = view.linkBy('no', 'decline') ?? view.links[0];
    if (target) view.onLink(target.id, { remember: false });
  }

  /** The third answer a state may offer: allow the probing, refuse the model's advice. */
  const noAdvice = $derived(view.linkBy('no_advice'));
</script>

<ConsentBox
  title={view.field('summary') ?? $t('common.consent.yes')}
  looksFor={view.field('looks_for')}
  inDir={view.field('in')}
  runs={view.field('runs')}
  never={view.field('never')}
  tokens={view.field('tokens')}
  canRemember={view.state?.kind !== 'auto_run' &&
    view.state?.kind !== 'verify_harness_profile'}
  busy={view.busy}
  primaryLabel={view.consentAction()}
  declineWithoutAdviceLabel={noAdvice ? view.linkLabel(noAdvice.id) : undefined}
  onDeclineWithoutAdvice={noAdvice ? () => view.onLink(noAdvice.id) : undefined}
  onAccept={accept}
  onDecline={decline}
/>
