<script lang="ts">
  import DataTable from '../../lib/components/DataTable.svelte';
  import type { DataTableColumn } from '../../lib/components/DataTable.svelte';

  export let columns: DataTableColumn[] = [];
  export let label = '';
  export let emptyLabel = '';
  export let menuLabel: string | undefined = undefined;
</script>

<DataTable {columns} rows={[]} {label} {menuLabel}>
  <svelte:fragment slot="empty">
    <p>{emptyLabel}</p>
  </svelte:fragment>
</DataTable>
