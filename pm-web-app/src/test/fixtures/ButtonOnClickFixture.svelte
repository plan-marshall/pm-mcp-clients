<script lang="ts">
  import Button from '../../lib/components/Button.svelte';

  export let disabled = false;
  export let disabledReason: string | undefined = undefined;
  export let label = '';
  export let onClick: () => void = () => {};
</script>

<Button {disabled} {disabledReason} on:click={onClick}>
  {label}
</Button>
