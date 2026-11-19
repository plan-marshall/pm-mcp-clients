<script lang="ts">
  import Toggle from '../../lib/components/Toggle.svelte';

  export let pressed = false;
  export let label = '';
  export let icon = '';
  export let disabled = false;
  export let disabledReason: string | undefined = undefined;
  export let onToggle: (pressed: boolean) => void = () => {};
  export let onClick: () => void = () => {};
</script>

<Toggle
  {pressed}
  {label}
  {icon}
  {disabled}
  {disabledReason}
  {onToggle}
  on:click={onClick}
/>
