<script lang="ts">
  export let size = '1.25rem';
  export let label = 'Loading…';
</script>

<div class="spinner" data-size={size} role="status" aria-label={label}>
  <span class="sr-only">{label}</span>
</div>

<style>
  .spinner {
    display: inline-block;
    width: 1.25rem;
    height: 1.25rem;
    border: 2px solid var(--color-border);
    border-top-color: var(--color-primary);
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  .spinner[data-size='1rem'] {
    width: 1rem;
    height: 1rem;
  }

  .spinner[data-size='1.25rem'] {
    width: 1.25rem;
    height: 1.25rem;
  }

  .spinner[data-size='2rem'] {
    width: 2rem;
    height: 2rem;
  }

  .spinner[data-size='2.5rem'] {
    width: 2.5rem;
    height: 2.5rem;
  }

  .spinner[data-size='3rem'] {
    width: 3rem;
    height: 3rem;
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }
</style>
