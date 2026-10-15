<script lang="ts">
  export let size = '1.25rem';
  export let label = 'Loading…';
</script>

<div class="spinner" style="--size: {size}" role="status" aria-label={label}>
  <span class="sr-only">{label}</span>
</div>

<style>
  .spinner {
    display: inline-block;
    width: var(--size);
    height: var(--size);
    border: 2px solid var(--color-border);
    border-top-color: var(--color-primary);
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }
</style>
