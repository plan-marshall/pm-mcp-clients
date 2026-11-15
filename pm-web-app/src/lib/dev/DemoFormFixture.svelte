<script lang="ts">
  import TextField from '../components/TextField.svelte';
  import TextArea from '../components/TextArea.svelte';
  import FormStatus, { type FormStatusTone } from '../components/FormStatus.svelte';
  import Button from '../components/Button.svelte';
  import { t } from '../i18n';
  import {
    handleFormKeyDown,
    handleTextAreaKeyDown,
    handleInlineEditKeyDown,
  } from '../forms';

  let text = $state('');
  let reason = $state('');
  let submitted = $state(false);
  let cancelled = $state(false);

  const isValid = $derived(text.trim().length > 0 && reason.trim().length > 0);

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

  function handleSubmit() {
    if (isValid) {
      submitted = true;
      cancelled = false;
    }
  }

  function handleCancel() {
    cancelled = true;
    submitted = false;
  }
</script>

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
    bind:value={text}
    label={$t('dev.field.sample_label')}
    placeholder={$t('dev.field.sample_placeholder')}
    testid="demo-form-text"
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
      {$t('web.common.cancel')}
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
