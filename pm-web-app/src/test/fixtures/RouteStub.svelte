<script lang="ts">
  /**
   * A stand-in for a view in the router's tests: it names the view it replaces and the props the
   * router passed, so a test reads what the dispatcher decided without loading the real view.
   */
  let { view, ...rest }: { view: string; [key: string]: unknown } = $props();
</script>

<div data-testid="route-view" data-view={view}>{JSON.stringify(rest)}</div>
