<script lang="ts">
  import IconButton from '../../lib/components/IconButton.svelte';

  export let disabled = false;
  export let disabledReason: string | undefined = undefined;
  export let label = '';
  export let onClick: () => void = () => {};
</script>

<IconButton
  icon="act.copy"
  {label}
  {disabled}
  {disabledReason}
  on:click={onClick}
/>
