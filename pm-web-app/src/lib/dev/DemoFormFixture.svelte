<script lang="ts">
  import TextField from '../components/TextField.svelte';
  import TextArea from '../components/TextArea.svelte';
  import FormStatus, { type FormStatusTone } from '../components/FormStatus.svelte';
  import Button from '../components/Button.svelte';
  import { t } from '../i18n';
  import {
    createForm,
    handleFormKeyDown,
    handleTextAreaKeyDown,
    handleInlineEditKeyDown,
  } from '../forms';
  import UnsavedChangesGuard from '../forms/UnsavedChangesGuard.svelte';

  /** The title field is driven by the form module: its value, its blur check and its reason. */
  const form = createForm({ fields: { title: { initial: '', required: true } } });

  let reason = $state('');
  /** The reason as last saved, the baseline its unsaved changes are measured against. */
  let savedReason = $state('');
  /** The number of the latest save handed to the form. */
  let latestSave = 0;
  /** The number of the newest save that has succeeded; only a newer success moves `savedReason`. */
  let appliedSave = 0;
  let submitted = $state(false);
  let cancelled = $state(false);
  /** Whether the title differs from its baseline, followed through the form's store. */
  let titleDirty = $state(false);

  $effect(() =>
    form.subscribe(() => {
      titleDirty = form.isDirty();
    }),
  );

  const dirty = $derived(titleDirty || reason !== savedReason);

  const isValid = $derived($form.values.title.trim().length > 0 && reason.trim().length > 0);

  const statusMessage = $derived.by(() => {
    if (submitted) return $t('dev.field.form_submitted');
    if (cancelled) return $t('dev.field.form_cancelled');
    if (!isValid) return $t('common.field.reason_required');
    return null;
  });

  const statusTone = $derived.by((): FormStatusTone => {
    if (submitted) return 'success';
    if (cancelled) return 'neutral';
    if (!isValid) return 'danger';
    return 'neutral';
  });

  /**
   * Marks the form saved only once the form module reports the save as succeeded. The reason saved
   * is the one submitted, not one typed while the save was pending, and the guard is told nothing:
   * it asks whenever the form differs from what was saved. As in the form module, the newest save
   * that has succeeded holds the saved reason: an older success that resolves after a newer one
   * leaves it alone.
   */
  function handleSubmit(): void {
    if (!isValid) return;
    const submittedReason = reason;
    const sequence = ++latestSave;
    void form.submit().then((saved) => {
      if (!saved || sequence <= appliedSave) return;
      appliedSave = sequence;
      savedReason = submittedReason;
      submitted = true;
      cancelled = false;
    });
  }

  function handleCancel() {
    cancelled = true;
    submitted = false;
  }
</script>

<UnsavedChangesGuard {dirty}>
  <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
  <form
    class="dev__stack"
    data-testid="demo-form-fixture"
    onsubmit={(e) => {
      e.preventDefault();
      handleSubmit();
    }}
    onkeydown={(e) => {
      handleFormKeyDown(e, handleSubmit);
      handleInlineEditKeyDown(e, handleCancel);
    }}
  >
    <TextField
      value={$form.values.title}
      error={$form.errors.title}
      label={$t('dev.field.sample_label')}
      placeholder={$t('dev.field.sample_placeholder')}
      testid="demo-form-text"
      oninput={(e) => form.onFieldInput('title', e.currentTarget.value)}
      onblur={() => form.onFieldBlur('title')}
    />
    <TextArea
      bind:value={reason}
      label={$t('dev.field.sample_label')}
      placeholder={$t('dev.field.reason_placeholder')}
      testid="demo-form-textarea"
      onkeydown={(e) => {
        handleTextAreaKeyDown(e, handleSubmit);
      }}
    />
    <FormStatus message={statusMessage} tone={statusTone} />
    <div class="dev__row dev__row--end">
      <Button type="button" variant="ghost" onclick={handleCancel} testid="demo-form-cancel">
        {$t('common.action.cancel')}
      </Button>
      <Button
        type="submit"
        variant="primary"
        disabled={!isValid}
        disabledReason={!isValid ? $t('common.field.reason_required') : undefined}
        testid="demo-form-submit"
      >
        {$t('common.action.save')}
      </Button>
    </div>
  </form>
</UnsavedChangesGuard>

<style>
  .dev__stack {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
  }

  .dev__row {
    display: flex;
    align-items: center;
    gap: var(--space-3);
  }

  .dev__row--end {
    justify-content: flex-end;
  }
</style>
