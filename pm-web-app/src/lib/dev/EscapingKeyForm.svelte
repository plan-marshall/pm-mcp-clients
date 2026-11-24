<script lang="ts">
  import TextField from '../components/TextField.svelte';
  import Button from '../components/Button.svelte';
  import { t } from '../i18n';
  import { createForm } from '../forms';

  /**
   * A form whose field key is no CSS identifier: `2fa` starts with a digit, which no selector spells
   * raw. Submitting it while empty must show the reason and move the focus to its own control — the
   * case that used to throw inside `submit()` before either happened (PLAN-173 D4(b), PLAN-178 D3).
   */
  const KEY = '2fa';

  const form = createForm({ fields: { [KEY]: { initial: '', required: true } } });
</script>

<!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
<form
  class="dev__stack"
  data-testid="escaping-key-form"
  onsubmit={(event) => {
    event.preventDefault();
    void form.submit();
  }}
>
  <TextField
    value={$form.values[KEY]}
    error={$form.errors[KEY]}
    label={$t('dev.field.sample_label')}
    testid={KEY}
    oninput={(event) => form.onFieldInput(KEY, event.currentTarget.value)}
    onblur={() => form.onFieldBlur(KEY)}
  />
  <div class="dev__row dev__row--end">
    <Button type="submit" variant="primary" data-testid="escaping-key-submit">
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
