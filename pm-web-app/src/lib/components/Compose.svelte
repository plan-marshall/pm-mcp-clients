<script lang="ts" module>
  /** The act that replaces send while an answer runs: its label and what stopping does. */
  export interface ComposeStop {
    /** The visually hidden label of the stop act, from the catalogue. */
    label: string;
    /** Stops what runs. */
    onstop: () => void;
  }

  /** The number of lines the field grows to before it scrolls, unless the caller names another. */
  export const DEFAULT_MAX_LINES = 8;
</script>

<script lang="ts">
  import type { Snippet } from 'svelte';
  import Button from './Button.svelte';
  import { uniqueId } from '../utils/uniqueId';

  /**
   * One text area and a send act (components-fields.adoc, Compose): the field of the chat and of
   * every instruct field.
   *
   * The area grows with its text up to `maxLines` and then scrolls. Its height is a custom property
   * set from script, never a static style attribute, which the daemon's content security policy
   * blocks. `Enter` sends, `Shift+Enter` adds a line, and nothing is sent while a composition (an
   * input method, dictation) is open. The send act is the secondary `Button` with an icon and a
   * visually hidden label; while `stop` is set it becomes the stop act, and the field stays enabled,
   * so the person writes on while the answer runs.
   *
   * It owns no draft, no counter and no reference list: the frame that uses it brings those, the
   * counter and the dictation hint through `footer`, a microphone through `tools`.
   */
  interface Props {
    /** The text of the field; bindable, so the frame can fill or clear it. */
    value?: string;
    /** The visible label of the field; without it, `ariaLabel` names the field. */
    label?: string;
    /** The accessible name of the field where the frame names it visually. */
    ariaLabel?: string;
    /** An example value, never the label. */
    placeholder?: string;
    /** The visually hidden label of the send act. */
    sendLabel: string;
    /** Holds the send act back; the field stays editable. */
    disabled?: boolean;
    /** Why sending is held back; it becomes the act's description. */
    disabledReason?: string;
    /** The lines the field grows to before it scrolls. */
    maxLines?: number;
    /** Receives the text when the person sends it. */
    onsend: (text: string) => void;
    /** Turns the send act into the stop act while it is set. */
    stop?: ComposeStop | null;
    /** Acts before the send act, such as a microphone. */
    tools?: Snippet;
    /** A line under the field, such as a counter or a hint. */
    footer?: Snippet;
  }

  let {
    value = $bindable(''),
    label,
    ariaLabel,
    placeholder,
    sendLabel,
    disabled = false,
    disabledReason,
    maxLines = DEFAULT_MAX_LINES,
    onsend,
    stop = null,
    tools,
    footer,
  }: Props = $props();

  /** The legacy `keyCode` a browser reports for a key press that belongs to an input method. */
  const IME_KEY_CODE = 229;

  const fieldId = uniqueId('compose-field');

  let field: HTMLTextAreaElement | undefined = $state();
  /** Whether the text holds more lines than the field shows, so the field scrolls. */
  let overflowing = $state(false);
  /** Whether a composition is open; Safari ends it before the `Enter` that confirms it arrives. */
  let composing = false;

  /**
   * @param element the field, laid out by the browser
   * @returns the lines its text takes, wrapped lines included; 1 where nothing is laid out
   */
  function measuredLines(element: HTMLTextAreaElement): number {
    const style = getComputedStyle(element);
    const lineHeight = Number.parseFloat(style.lineHeight);
    if (!(lineHeight > 0)) {
      return 1;
    }
    const padding =
      (Number.parseFloat(style.paddingBlockStart) || 0) + (Number.parseFloat(style.paddingBlockEnd) || 0);
    // The field is measured at one line, so its scroll height is the height of its text alone.
    element.style.setProperty('--compose-lines', '1');
    return Math.max(1, Math.round((element.scrollHeight - padding) / lineHeight));
  }

  // The field takes the height of its text, up to `maxLines`, whenever the text or the limit changes.
  $effect(() => {
    const limit = Math.max(1, maxLines);
    const typedLines = value.split('\n').length;
    if (!field) {
      return;
    }
    const lines = Math.max(typedLines, measuredLines(field));
    field.style.setProperty('--compose-lines', String(Math.min(lines, limit)));
    overflowing = lines > limit;
  });

  /** Hands the text to the frame, unless sending is held back or an answer runs. */
  function send(): void {
    if (disabled || stop) {
      return;
    }
    onsend(value);
  }

  /** @param event a key press in the field */
  function onkeydown(event: KeyboardEvent): void {
    // Safari delivers the confirming `Enter` after `compositionend` with `isComposing` already false;
    // it still marks that keydown with the legacy key code 229.
    if (event.key !== 'Enter' || event.shiftKey || event.isComposing || event.keyCode === IME_KEY_CODE || composing) {
      return;
    }
    event.preventDefault();
    send();
  }

  /** The act beside the field: send, or stop while an answer runs; the focus stays in the field. */
  function act(): void {
    if (stop) {
      stop.onstop();
    } else {
      send();
    }
    field?.focus();
  }

  /** Moves the focus into the field, as the chat does when it opens. */
  export function focus(): void {
    field?.focus();
  }
</script>

<div class="compose">
  {#if label}
    <label class="compose__label" for={fieldId}>{label}</label>
  {/if}
  <div class="compose__row">
    <textarea
      bind:this={field}
      bind:value
      id={fieldId}
      class="compose__field"
      class:compose__field--scrolls={overflowing}
      rows="1"
      aria-label={label ? undefined : ariaLabel}
      {placeholder}
      {onkeydown}
      oncompositionstart={() => (composing = true)}
      oncompositionend={() => (composing = false)}
    ></textarea>
    {#if tools}
      <div class="compose__tools">{@render tools()}</div>
    {/if}
    <Button
      variant="secondary"
      icon={stop ? 'act.stop' : 'act.send'}
      disabled={stop ? false : disabled}
      disabledReason={stop ? undefined : disabledReason}
      onclick={act}
    >
      <span class="sr-only">{stop ? stop.label : sendLabel}</span>
    </Button>
  </div>
  {#if footer}
    <div class="compose__footer">{@render footer()}</div>
  {/if}
</div>

<style>
  .compose {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .compose__label {
    color: var(--color-text-primary);
    font-size: var(--text-sm);
    font-weight: var(--weight-medium);
    line-height: var(--line-height-sm);
  }

  .compose__row {
    display: flex;
    align-items: flex-end;
    gap: var(--space-2);
  }

  /* One line is as high as a md Button (34 px); the text adds lines of the base line height. */
  .compose__field {
    flex: 1 1 auto;
    min-inline-size: 0;
    box-sizing: content-box;
    block-size: calc(var(--compose-lines, 1) * var(--line-height-base));
    padding-block: calc((34px - 2px - var(--line-height-base)) / 2);
    padding-inline: var(--space-3);
    border: 1px solid var(--color-border-strong);
    border-radius: var(--radius-md);
    background-color: var(--color-bg-surface);
    color: var(--color-text-primary);
    font-size: var(--text-base);
    line-height: var(--line-height-base);
    resize: none;
    overflow-y: hidden;
  }

  .compose__field--scrolls {
    overflow-y: auto;
  }

  .compose__field::placeholder {
    color: var(--color-text-muted);
  }

  .compose__tools {
    display: inline-flex;
    align-items: center;
    gap: var(--space-1);
  }

  .compose__footer {
    color: var(--color-text-secondary);
    font-size: var(--text-xs);
    line-height: var(--line-height-xs);
  }

  /* A phone gets the 44 px target, as the Button beside the field does. */
  @media (max-width: 767px) {
    .compose__field {
      padding-block: calc((44px - 2px - var(--line-height-base)) / 2);
    }
  }
</style>
