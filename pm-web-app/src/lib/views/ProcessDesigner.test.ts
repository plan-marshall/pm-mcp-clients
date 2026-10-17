import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import ProcessDesigner from './ProcessDesigner.svelte';
import { apiClient } from '../api/client';
import type {
  WorkflowCatalogue,
  WorkflowUnit,
  WorkflowValidationResult,
} from '../api/client';

const finalizeText = [
  'workflow phase-6-finalize version 5 format 1',
  '---',
  'name: Finalize',
  'type: subworkflow',
  'entry start',
  'exit merged',
  '---',
  'state finalize.review',
  '  done -> finalize.archive-plan',
  '',
].join('\n');

const units: WorkflowUnit[] = [
  {
    name: 'phase-6-finalize',
    group: 'project_owned',
    level_in_force: 'project',
    frontmatter: {
      display_name: 'Finalize',
      description: 'Settles the plan.',
      type: 'subworkflow',
      entries: ['start'],
      exits: ['merged'],
      author: 'Oliver Wolff',
      release: '0.9.0',
      signed: false,
    },
    levels: [{ level: 'project', text: finalizeText, revision: 4, digest: 'sha256:pf6-p4' }],
  },
  {
    name: 'epic',
    group: 'compiled_only',
    level_in_force: 'default',
    levels: [{ level: 'default', text: 'workflow epic version 1 format 1', revision: 1, digest: 'sha256:epic' }],
  },
];

const catalogue: WorkflowCatalogue = {
  scope: 'pm-mcp-clients',
  runtime_version: '0.9.0',
  digest: 'sha256:cat',
  blocks: [
    {
      kind: 'sub_workflow',
      name: 'git.commit_and_push',
      summary: 'Commits the changes and pushes the branch.',
      template: 'call git.commit_and_push entry commit',
      parameters: ['message_mode enum'],
      constraints: ['a push remote'],
      outcomes: ['done (success)', 'stepped_out'],
      used_in: ['the call clause of a state'],
      compiled_only: false,
    },
  ],
};

const validResult: WorkflowValidationResult = {
  valid: true,
  stages: [
    { name: 'parse', status: 'passed' },
    { name: 'rules', status: 'passed' },
    { name: 'invariants', status: 'passed' },
    { name: 'composition', status: 'passed' },
  ],
  findings: [],
  structure: {
    display_name: 'Finalize',
    entries: ['start'],
    exits: ['merged'],
    states: [
      {
        id: 'finalize.review',
        kind: 'state',
        outcomes: ['done -> finalize.archive-plan'],
        calls: [],
      },
    ],
  },
  composed: {
    plan_id: 'plan',
    title: 'If a plan started now',
    project: { project_id: 'pm-mcp-clients' },
    current_phase: 'execute',
    phase_state: 'planned',
    needs_operator: false,
    instance_version: 1,
    tokens_so_far: {
      total: 0,
      input: 0,
      output: 0,
      floor: false,
      tasks_total: 0,
      tasks_measured: 0,
      none_yet: true,
      not_measured: false,
    },
    nodes: [
      {
        node_id: 'phase-1-init',
        title: 'phase-1-init',
        kind: 'phase',
        state: 'planned',
        run_count: 1,
        current_run: 1,
        duration_seconds: 0,
        sub_nodes: [],
        leaf_tasks: [],
      },
    ],
    current_position_path: [],
    duration_seconds: 0,
  },
};

describe('ProcessDesigner view', () => {
  beforeEach(() => {
    vi.spyOn(apiClient, 'getWorkflows').mockResolvedValue(units);
    vi.spyOn(apiClient, 'getWorkflowCatalogue').mockResolvedValue(catalogue);
    vi.spyOn(apiClient, 'getProjects').mockResolvedValue({
      projects: [
        {
          project_id: 'pm-mcp-clients',
          display_name: 'pm-mcp-clients',
          active_plans_count: 0,
          active_epics_count: 0,
          needs_operator_count: 0,
          setup_needs_operator: false,
        },
      ],
    });
    vi.spyOn(apiClient, 'validateWorkflow').mockResolvedValue(validResult);
    vi.spyOn(apiClient, 'placeWorkflowUnit').mockResolvedValue({
      unit: 'phase-6-finalize',
      target: 'pm-mcp-clients',
      status: 'landed',
      steps: ['written', 'committed', 'landed'],
      catalogue_digest: 'sha256:pm-mcp-clients-r2',
      author: 'Oliver Wolff',
      release: '0.9.0',
      digest: 'sha256:placed',
    });
    vi.spyOn(apiClient, 'removeWorkflowUnit').mockResolvedValue({
      unit: 'phase-6-finalize',
      target: 'pm-mcp-clients',
      status: 'landed',
      steps: ['written', 'committed', 'landed'],
    });
  });

  it('renders the title and opens the first installed unit in the editor', async () => {
    render(ProcessDesigner);

    expect(screen.getByTestId('designer-title')).toHaveTextContent('Process designer');

    await waitFor(() => {
      expect(screen.getByTestId('workflow-text')).toHaveValue(finalizeText);
    });
    expect(screen.getByTestId('designer-unit')).toHaveTextContent('phase-6-finalize');
    expect(screen.getByTestId('designer-checked')).toHaveTextContent(
      "checked against this machine's projects",
    );
  });

  it('marks a compiled-only unit and offers no removal for it', async () => {
    render(ProcessDesigner);

    await waitFor(() => expect(screen.getByTestId('unit-compiled-epic')).toBeInTheDocument());
    expect(screen.queryByTestId('unit-remove-epic')).not.toBeInTheDocument();
    expect(screen.getByTestId('unit-remove-phase-6-finalize')).toBeInTheDocument();
  });

  it('validates the text and enables placing for a valid unit', async () => {
    const validateSpy = vi.spyOn(apiClient, 'validateWorkflow');

    render(ProcessDesigner);
    await waitFor(() => expect(screen.getByTestId('workflow-text')).toHaveValue(finalizeText));

    await fireEvent.click(screen.getByTestId('validate-button'));

    await waitFor(() => expect(screen.getByTestId('validation-valid')).toBeInTheDocument());
    expect(validateSpy).toHaveBeenCalledWith(
      expect.objectContaining({ unit_name: 'phase-6-finalize', unit_kind: 'subworkflow' }),
    );
    expect(screen.getByTestId('place-button')).not.toBeDisabled();
  });

  it('inserts a building block at the cursor', async () => {
    render(ProcessDesigner);
    await waitFor(() => expect(screen.getByTestId('workflow-text')).toHaveValue(finalizeText));

    await fireEvent.click(screen.getByTestId('block-insert-git.commit_and_push'));

    const editor = screen.getByTestId('workflow-text') as HTMLTextAreaElement;
    expect(editor.value).toContain('call git.commit_and_push entry commit');
  });

  it('shows a validation finding with its rule, line, and column', async () => {
    vi.spyOn(apiClient, 'validateWorkflow').mockResolvedValue({
      valid: false,
      stages: [
        { name: 'parse', status: 'passed' },
        { name: 'rules', status: 'failed' },
        { name: 'invariants', status: 'not_run' },
        { name: 'composition', status: 'not_run' },
      ],
      findings: [
        {
          stage: 'rules',
          rule: 'INV-FIN-1',
          line: 42,
          column: 3,
          severity: 'error',
          message: 'a mandatory finalize step is missing',
        },
      ],
      structure: null,
    });

    render(ProcessDesigner);
    await waitFor(() => expect(screen.getByTestId('workflow-text')).toHaveValue(finalizeText));

    await fireEvent.click(screen.getByTestId('validate-button'));

    await waitFor(() => expect(screen.getByTestId('validation-summary')).toBeInTheDocument());
    expect(screen.getByTestId('validation-summary')).toHaveTextContent('1 finding');
    expect(screen.getByTestId('validation-findings')).toHaveTextContent(
      'INV-FIN-1 · line 42, column 3: a mandatory finalize step is missing',
    );
    expect(screen.getByTestId('place-button')).toBeDisabled();
  });
});
