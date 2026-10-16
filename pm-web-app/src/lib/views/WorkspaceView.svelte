<script lang="ts">
  import { onMount } from 'svelte';
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import { apiClient, type StatusResponse } from '../api/client';
  import { runtimeStore, navigateTo } from '../stores/runtime';

  type Tab = 'projects' | 'plans' | 'system';
  let activeTab: Tab = 'projects';
  let statusData: StatusResponse | null = null;
  let isLoading = false;
  let loadError = false;

  interface Project {
    id: string;
    name: string;
    repo: string;
    branch: string;
    plansCount: number;
  }

  interface Plan {
    id: string;
    title: string;
    project: string;
    phase: string;
    state: string;
  }

  let projects: Project[] = [];
  let plans: Plan[] = [];

  onMount(async () => {
    isLoading = true;
    loadError = false;
    try {
      statusData = await apiClient.getStatus();
      if (Array.isArray(statusData.projects) && statusData.projects.length > 0) {
        projects = statusData.projects as Project[];
      } else {
        projects = [
          {
            id: 'prj-1',
            name: 'pm-mcp-clients',
            repo: 'https://github.com/plan-marshall/pm-mcp-clients',
            branch: 'main',
            plansCount: 1,
          },
        ];
      }
      plans = [
        {
          id: 'PLN-001',
          title: 'Web Application Foundation',
          project: 'pm-mcp-clients',
          phase: 'phase-5-execute',
          state: 'running',
        },
      ];
    } catch {
      loadError = true;
      statusData = null;
    } finally {
      isLoading = false;
    }
  });
</script>

<div class="workspace">
  <div class="workspace__tabs">
    <button
      class="tab-btn"
      class:tab-btn--active={activeTab === 'projects'}
      on:click={() => (activeTab = 'projects')}
    >
      {$t('workspace.projects.title')}
    </button>
    <button
      class="tab-btn"
      class:tab-btn--active={activeTab === 'plans'}
      on:click={() => (activeTab = 'plans')}
    >
      {$t('workspace.plans.title')}
    </button>
    <button
      class="tab-btn"
      class:tab-btn--active={activeTab === 'system'}
      on:click={() => (activeTab = 'system')}
    >
      {$t('workspace.system.title')}
    </button>
  </div>

  <div class="workspace__content">
    {#if activeTab === 'projects'}
      <div class="card">
        <div class="card__header">
          <h2 class="card__title">{$t('workspace.projects.title')}</h2>
          <Button variant="secondary" on:click={() => navigateTo('designer')}>
            {$t('designer.view.title')}
          </Button>
        </div>
        {#if projects.length === 0}
          <p class="empty-text">{$t('workspace.projects.empty')}</p>
        {:else}
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Repository</th>
                  <th>Branch</th>
                  <th>Plans</th>
                </tr>
              </thead>
              <tbody>
                {#each projects as prj}
                  <tr>
                    <td><strong>{prj.name}</strong></td>
                    <td class="mono">{prj.repo}</td>
                    <td class="mono">{prj.branch}</td>
                    <td>{prj.plansCount}</td>
                  </tr>
                {/each}
              </tbody>
            </table>
          </div>
        {/if}
      </div>
    {:else if activeTab === 'plans'}
      <div class="card">
        <div class="card__header">
          <h2 class="card__title">{$t('workspace.plans.title')}</h2>
        </div>
        {#if plans.length === 0}
          <p class="empty-text">{$t('workspace.plans.empty')}</p>
        {:else}
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Title</th>
                  <th>Project</th>
                  <th>Phase</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {#each plans as plan}
                  <tr>
                    <td class="mono"><strong>{plan.id}</strong></td>
                    <td>{plan.title}</td>
                    <td>{plan.project}</td>
                    <td><span class="badge badge--phase">{plan.phase}</span></td>
                    <td><span class="badge badge--success">{plan.state}</span></td>
                  </tr>
                {/each}
              </tbody>
            </table>
          </div>
        {/if}
      </div>
    {:else if activeTab === 'system'}
      <div class="card">
        <div class="card__header">
          <h2 class="card__title">{$t('workspace.system.title')}</h2>
        </div>
        {#if loadError}
          <p class="empty-text error-text">Unable to load system status from runtime.</p>
        {:else}
          <dl class="system-stats">
            <div class="system-row">
              <dt>{$t('workspace.system.status')}</dt>
              <dd><span class="badge badge--success">{statusData?.status || 'ready'}</span></dd>
            </div>
            <div class="system-row">
              <dt>{$t('workspace.system.version')}</dt>
              <dd class="mono">{statusData?.version || '0.1.0-SNAPSHOT'}</dd>
            </div>
            <div class="system-row">
              <dt>{$t('workspace.system.listener')}</dt>
              <dd class="mono">{statusData?.listener || 'tcp/loopback'}</dd>
            </div>
            <div class="system-row">
              <dt>Machine Name</dt>
              <dd class="mono">
                {typeof statusData?.machine === 'string'
                  ? statusData.machine
                  : statusData?.machine?.name || $runtimeStore.machineName}
              </dd>
            </div>
          </dl>
        {/if}
      </div>
    {/if}
  </div>
</div>

<style>
  .workspace {
    max-width: 1000px;
    margin: 0 auto;
    padding: var(--space-6) var(--space-4);
  }

  .workspace__tabs {
    display: flex;
    gap: var(--space-2);
    border-bottom: 1px solid var(--color-border);
    margin-bottom: var(--space-6);
  }

  .tab-btn {
    padding: var(--space-3) var(--space-5);
    font-size: var(--text-base);
    font-weight: 500;
    color: var(--color-text-secondary);
    border-bottom: 2px solid transparent;
    transition: all 0.15s ease-in-out;
  }

  .tab-btn:hover {
    color: var(--color-text-primary);
  }

  .tab-btn--active {
    color: var(--color-primary);
    border-bottom-color: var(--color-primary);
    font-weight: 600;
  }

  .card {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-6);
    box-shadow: var(--shadow-sm);
  }

  .card__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--space-6);
  }

  .card__title {
    font-size: var(--text-xl);
    font-weight: 700;
  }

  .table-wrap {
    overflow-x: auto;
  }

  .data-table {
    width: 100%;
    border-collapse: collapse;
    font-size: var(--text-sm);
  }

  .data-table th,
  .data-table td {
    padding: var(--space-3) var(--space-4);
    text-align: left;
    border-bottom: 1px solid var(--color-border-subtle);
  }

  .data-table th {
    background-color: var(--color-bg-subtle);
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .badge {
    display: inline-block;
    padding: 2px 8px;
    font-size: var(--text-xs);
    font-weight: 600;
    border-radius: var(--radius-sm);
  }

  .badge--success {
    background-color: var(--color-success-subtle);
    color: var(--color-success);
    border: 1px solid var(--color-success-border);
  }

  .badge--phase {
    background-color: var(--color-primary-subtle);
    color: var(--color-primary);
  }

  .system-stats {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  .system-row {
    display: grid;
    grid-template-columns: 200px 1fr;
    font-size: var(--text-sm);
  }

  .system-row dt {
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .empty-text {
    color: var(--color-text-muted);
    font-style: italic;
  }
</style>
