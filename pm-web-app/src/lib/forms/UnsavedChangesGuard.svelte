<script lang="ts">
  import type { Snippet } from 'svelte';
  import { beforeNavigate, goto } from '$app/navigation';
  import ConfirmDialog from '../components/ConfirmDialog.svelte';
  import { t } from '../i18n';
  import { createUnsavedChangesGuard } from './index';

  /**
   * The unsaved-changes guard of one form (components-fields.adoc, Submitting).
   *
   * A route change while the form holds unsaved changes is cancelled and asked through a
   * ConfirmDialog with the catalogue's texts: leaving repeats the navigation, which passes unasked
   * once, and staying keeps the page; while the question is on screen a further route change is
   * cancelled and the question keeps its target. A reload, a closed tab or a link that unloads the
   * page is asked through the browser's own `beforeunload` prompt, which no page can word. The guard asks whenever `dirty` holds, so the
   * form tells it nothing when it submits: `dirty` is measured against the last successful save.
   */
  interface Props {
    /** Whether the form holds changes that are not saved. */
    dirty: boolean;
    /** The form the guard watches. */
    children?: Snippet;
  }

  let { dirty, children }: Props = $props();

  /** Whether the question is on screen. */
  let open = $state(false);
  /** Nothing to settle: no question is on screen. */
  const UNASKED = (): void => {};
  /** Settles the question that is on screen, with `true` to leave. */
  let answer: (leave: boolean) => void = UNASKED;
  /** A repeated navigation that failed leaves the page as it is, and the guard asks again. */
  const STAYED = (): void => {};

  const guard = createUnsavedChangesGuard({
    isDirty: () => dirty,
    ask: () =>
      new Promise<boolean>((resolve) => {
        answer = resolve;
        open = true;
      }),
  });

  /** @param leave whether the operator chose to leave the page */
  function settle(leave: boolean): void {
    open = false;
    const resolve = answer;
    answer = UNASKED;
    resolve(leave);
  }

  beforeNavigate((navigation) => {
    // Unloading the page (reload, closed tab, a link to another site) is asked by the browser's own
    // prompt, which the beforeunload listener below raises.
    if (navigation.willUnload || !navigation.to) return;
    if (open) {
      // One question at a time: a further navigation while it is on screen (Back, then Forward) is
      // cancelled and the open question keeps its target.
      navigation.cancel();
      return;
    }
    if (!guard.shouldAsk()) return;
    navigation.cancel();
    const target = navigation.to.url;
    void guard.canLeave().then((leave) => {
      if (!leave) return;
      // Only the repeated navigation passes unasked; once it has settled, the guard asks again.
      guard.allowLeave();
      void goto(target)
        .finally(() => guard.endLeave())
        .catch(STAYED);
    });
  });

  /** @param event the browser's beforeunload */
  function handleBeforeUnload(event: BeforeUnloadEvent): void {
    guard.handleBeforeUnload(event);
  }

  $effect(() => {
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  });
</script>

{@render children?.()}

<ConfirmDialog
  {open}
  title={$t('web.form.unsaved.title')}
  consequences={[$t('web.form.unsaved.body')]}
  actionLabel={$t('web.form.unsaved.leave')}
  cancelLabel={$t('web.form.unsaved.stay')}
  testid="unsaved-changes-dialog"
  onConfirm={() => settle(true)}
  onClose={() => settle(false)}
/>
