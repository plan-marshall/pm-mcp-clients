<script lang="ts">
  import DataTable from '../../lib/components/DataTable.svelte';
  import type { DataTableColumn } from '../../lib/components/DataTable.svelte';

  export let columns: DataTableColumn[] = [];
  export let label = '';
  export let emptyLabel = '';
</script>

<DataTable {columns} rows={[]} {label}>
  <svelte:fragment slot="empty">
    <p>{emptyLabel}</p>
  </svelte:fragment>
</DataTable>
