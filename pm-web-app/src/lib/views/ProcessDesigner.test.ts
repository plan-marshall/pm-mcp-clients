import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import { get } from 'svelte/store';
import ProcessDesigner from './ProcessDesigner.svelte';
import { apiClient } from '../api/client';
import type { WorkflowCatalogue, WorkflowUnit, WorkflowValidationResult } from '../api/client';
import { runtimeStore } from '../stores/runtime';

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
    levels: [
      {
        level: 'default',
        text: 'workflow epic version 1 format 1',
        revision: 1,
        digest: 'sha256:epic',
      },
    ],
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
    vi.spyOn(apiClient, 'getProjectExplorer').mockResolvedValue({
      favourites: [],
      recent: [],
      all_projects: [
        {
          project_id: 'pm-mcp-clients',
          display_name: 'pm-mcp-clients',
          readiness: 'ready',
          needs_operator_count: 0,
          active_plans_count: 0,
          active_epics_count: 0,
          is_favourite: false,
          is_released: false,
        },
      ],
      being_added: [],
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

  it('opens with the project the route named', async () => {
    localStorage.removeItem('pm_designer_draft');
    const workflows = vi.spyOn(apiClient, 'getWorkflows');

    render(ProcessDesigner, { props: { projectId: 'pm-mcp-clients' } });

    await waitFor(() => expect(workflows).toHaveBeenCalledWith('pm-mcp-clients'));
    expect(screen.getByTestId('designer-checked')).toHaveTextContent(
      'checked against pm-mcp-clients',
    );
    await waitFor(() =>
      expect((screen.getByTestId('project-select') as HTMLSelectElement).value).toBe(
        'pm-mcp-clients',
      ),
    );
  });

  it('follows a route project change while the designer stays mounted', async () => {
    localStorage.removeItem('pm_designer_draft');
    const workflows = vi.spyOn(apiClient, 'getWorkflows');

    const { rerender } = render(ProcessDesigner, { props: { projectId: null } });
    await waitFor(() => expect(workflows).toHaveBeenCalledWith(null));

    await rerender({ projectId: 'pm-mcp-clients' });

    await waitFor(() => expect(workflows).toHaveBeenCalledWith('pm-mcp-clients'));
    expect(screen.getByTestId('designer-checked')).toHaveTextContent(
      'checked against pm-mcp-clients',
    );
    await waitFor(() =>
      expect((screen.getByTestId('project-select') as HTMLSelectElement).value).toBe(
        'pm-mcp-clients',
      ),
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

describe('ProcessDesigner intake, draft, frontmatter, and placing', () => {
  beforeEach(() => {
    vi.spyOn(apiClient, 'getWorkflows').mockResolvedValue(units);
    vi.spyOn(apiClient, 'getWorkflowCatalogue').mockResolvedValue(catalogue);
    vi.spyOn(apiClient, 'getProjectExplorer').mockResolvedValue({
      favourites: [],
      recent: [],
      all_projects: [
        {
          project_id: 'pm-mcp-clients',
          display_name: 'pm-mcp-clients',
          readiness: 'ready',
          needs_operator_count: 0,
          active_plans_count: 0,
          active_epics_count: 0,
          is_favourite: false,
          is_released: false,
        },
      ],
      being_added: [],
    });
    vi.spyOn(apiClient, 'validateWorkflow').mockResolvedValue(validResult);
    vi.spyOn(apiClient, 'placeWorkflowUnit').mockResolvedValue({
      unit: 'phase-6-finalize',
      target: 'machine',
      status: 'landed',
      steps: ['written', 'committed', 'landed'],
      author: 'Oliver Wolff',
      release: '0.9.0',
      digest: 'sha256:placed',
    });
  });

  it('opens a pasted text in the editor', async () => {
    render(ProcessDesigner);
    await waitFor(() => expect(screen.getByTestId('workflow-text')).toHaveValue(finalizeText));

    await fireEvent.click(screen.getByRole('button', { name: 'Paste text' }));
    const pasted = finalizeText.replace('phase-6-finalize', 'pasted-unit');
    await fireEvent.input(screen.getByTestId('designer-paste'), { target: { value: pasted } });
    await fireEvent.click(screen.getByTestId('designer-paste-open'));

    expect(screen.getByTestId('workflow-text')).toHaveValue(pasted);
    expect(screen.getByTestId('designer-unit')).toHaveTextContent('pasted-unit');
  });

  it('starts a draft from a template', async () => {
    render(ProcessDesigner);
    await waitFor(() => expect(screen.getByTestId('workflow-text')).toHaveValue(finalizeText));

    await fireEvent.click(screen.getByRole('button', { name: 'Start from a template' }));
    await fireEvent.click(screen.getByTestId('designer-template'));

    expect((screen.getByTestId('workflow-text') as HTMLTextAreaElement).value).toContain(
      'workflow new-unit version 1 format 1',
    );
  });

  it('restores the draft this browser kept and says when it is from', async () => {
    localStorage.setItem(
      'pm_designer_draft',
      JSON.stringify({
        text: finalizeText.replace('name: Finalize', 'name: Drafted'),
        unitName: 'phase-6-finalize',
        unitKind: 'subworkflow',
        projectId: null,
        savedAt: '2026-10-20T09:30:00Z',
      }),
    );

    render(ProcessDesigner);

    await waitFor(() =>
      expect((screen.getByTestId('workflow-text') as HTMLTextAreaElement).value).toContain(
        'name: Drafted',
      ),
    );
    expect(screen.getByTestId('designer-notice')).toHaveTextContent('Your draft from');
  });

  it('keeps the frontmatter panel in step with the text and sets date and release on edit', async () => {
    render(ProcessDesigner);
    await waitFor(() => expect(screen.getByTestId('workflow-text')).toHaveValue(finalizeText));

    expect(screen.getByTestId('frontmatter-name')).toHaveValue('Finalize');

    await fireEvent.input(screen.getByTestId('frontmatter-name'), {
      target: { value: 'Settled' },
    });

    const editor = screen.getByTestId('workflow-text') as HTMLTextAreaElement;
    expect(editor.value).toContain('name: Settled');
    expect(editor.value).toContain(`modified: ${new Date().toISOString().slice(0, 10)}`);
    expect(editor.value).toContain('release: 0.9.0');
  });

  it('drops the signature when the content changes', async () => {
    const signedText = finalizeText.replace(
      'type: subworkflow',
      'type: subworkflow\nsignature: sha256:abc',
    );
    vi.spyOn(apiClient, 'getWorkflows').mockResolvedValue([
      { ...units[0], levels: [{ ...units[0].levels[0], text: signedText }] },
      units[1],
    ]);

    render(ProcessDesigner);
    await waitFor(() => expect(screen.getByTestId('workflow-text')).toHaveValue(signedText));

    const editor = screen.getByTestId('workflow-text') as HTMLTextAreaElement;
    await fireEvent.input(editor, { target: { value: `${signedText}state extra\n` } });

    expect(editor.value).not.toContain('signature:');
    expect(screen.getByText(/The signature was removed/)).toBeInTheDocument();
  });

  it('moves the text to the state selected in the structure', async () => {
    render(ProcessDesigner);
    await waitFor(() => expect(screen.getByTestId('workflow-text')).toHaveValue(finalizeText));
    await fireEvent.click(screen.getByTestId('validate-button'));
    await waitFor(() =>
      expect(screen.getByTestId('structure-state-finalize.review')).toBeInTheDocument(),
    );

    const stateRow = screen.getByTestId('structure-state-finalize.review');
    const stateButton = stateRow.querySelector('button');
    expect(stateButton).not.toBeNull();
    await fireEvent.click(stateButton as HTMLButtonElement);

    const editor = screen.getByTestId('workflow-text') as HTMLTextAreaElement;
    const line = editor.value.slice(0, editor.selectionStart).split('\n').length;
    expect(editor.value.split('\n')[line - 1]).toContain('state finalize.review');
  });

  it('jumps to a validation finding when it is clicked', async () => {
    vi.spyOn(apiClient, 'validateWorkflow').mockResolvedValue({
      valid: false,
      stages: [{ name: 'rules', status: 'failed' }],
      findings: [
        {
          stage: 'rules',
          rule: 'INV-FIN-1',
          line: 2,
          column: 1,
          severity: 'error',
          message: 'a mandatory finalize step is missing',
        },
      ],
      structure: null,
    });

    render(ProcessDesigner);
    await waitFor(() => expect(screen.getByTestId('workflow-text')).toHaveValue(finalizeText));
    await fireEvent.click(screen.getByTestId('validate-button'));

    const findingRow = await screen.findByTestId('finding-0');
    await fireEvent.click(findingRow.querySelector('button') as HTMLButtonElement);

    const editor = screen.getByTestId('workflow-text') as HTMLTextAreaElement;
    expect(editor.value.slice(0, editor.selectionStart).split('\n')).toHaveLength(2);
  });

  it('shows the side-by-side difference before placing and lands it', async () => {
    const placeSpy = vi.spyOn(apiClient, 'placeWorkflowUnit');
    render(ProcessDesigner);
    await waitFor(() => expect(screen.getByTestId('workflow-text')).toHaveValue(finalizeText));

    await fireEvent.input(screen.getByTestId('workflow-text'), {
      target: { value: `${finalizeText}state placed\n` },
    });
    await fireEvent.click(screen.getByTestId('validate-button'));
    await waitFor(() => expect(screen.getByTestId('place-button')).not.toBeDisabled());

    await fireEvent.click(screen.getByTestId('place-button'));
    expect(screen.getByTestId('place-dialog')).toBeInTheDocument();
    await fireEvent.click(screen.getByTestId('place-compare'));

    expect(screen.getByTestId('designer-diff')).toBeInTheDocument();
    await fireEvent.click(screen.getByTestId('place-confirm'));

    await waitFor(() =>
      expect(placeSpy).toHaveBeenCalledWith(
        null,
        'phase-6-finalize',
        expect.objectContaining({ unit_name: 'phase-6-finalize' }),
      ),
    );
    expect(await screen.findByTestId('designer-place-done')).toHaveTextContent('Placed:');
  });

  it('compares again when the unit changed while placing', async () => {
    vi.spyOn(apiClient, 'placeWorkflowUnit').mockRejectedValue(new Error('conflict'));
    render(ProcessDesigner);
    await waitFor(() => expect(screen.getByTestId('workflow-text')).toHaveValue(finalizeText));

    await fireEvent.input(screen.getByTestId('workflow-text'), {
      target: { value: `${finalizeText}state placed\n` },
    });
    await fireEvent.click(screen.getByTestId('validate-button'));
    await waitFor(() => expect(screen.getByTestId('place-button')).not.toBeDisabled());
    await fireEvent.click(screen.getByTestId('place-button'));
    await fireEvent.click(screen.getByTestId('place-compare'));
    await fireEvent.click(screen.getByTestId('place-confirm'));

    expect(await screen.findByTestId('designer-place-conflict')).toHaveTextContent(
      'changed since the comparison',
    );
    expect(screen.getByTestId('designer-conflict-diff')).toBeInTheDocument();
  });

  it('is read only at phone width while Validate stays offered', async () => {
    const original = window.matchMedia;
    window.matchMedia = vi.fn().mockReturnValue({
      matches: true,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    }) as unknown as typeof window.matchMedia;

    render(ProcessDesigner);
    await waitFor(() => expect(screen.getByTestId('workflow-text')).toHaveValue(finalizeText));

    expect(screen.getByTestId('workflow-text')).toHaveAttribute('readonly');
    expect(screen.getByTestId('validate-button')).toBeInTheDocument();
    expect(screen.queryByTestId('place-button')).not.toBeInTheDocument();
    expect(screen.getByTestId('designer-phone-note')).toBeInTheDocument();

    window.matchMedia = original;
  });
});

/**
 * The designer route against a dirty draft: a refused route change must leave the project, the
 * selector, and the address bar on the loaded project, and a local selection must reach the URL.
 */
describe('ProcessDesigner route reconciliation', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
    window.history.replaceState(null, '', '/');
    runtimeStore.update((s) => ({ ...s, designerProjectId: null }));
    vi.spyOn(apiClient, 'getWorkflows').mockResolvedValue(units);
    vi.spyOn(apiClient, 'getWorkflowCatalogue').mockResolvedValue(catalogue);
    vi.spyOn(apiClient, 'getProjectExplorer').mockResolvedValue({
      favourites: [],
      recent: [],
      all_projects: [
        {
          project_id: 'project-a',
          display_name: 'Project A',
          readiness: 'ready',
          needs_operator_count: 0,
          active_plans_count: 0,
          active_epics_count: 0,
          is_favourite: false,
          is_released: false,
        },
        {
          project_id: 'project-b',
          display_name: 'Project B',
          readiness: 'ready',
          needs_operator_count: 0,
          active_plans_count: 0,
          active_epics_count: 0,
          is_favourite: false,
          is_released: false,
        },
      ],
      being_added: [],
    });
    vi.spyOn(apiClient, 'validateWorkflow').mockResolvedValue(validResult);
  });

  /** @param value the text to type into the editor */
  async function makeDirty(value: string): Promise<void> {
    await fireEvent.input(screen.getByTestId('workflow-text'), { target: { value } });
  }

  it('keeps the project, the selector, and the route when Stay refuses a dirty route change', async () => {
    const workflows = vi.spyOn(apiClient, 'getWorkflows');
    const { rerender } = render(ProcessDesigner, { props: { projectId: 'project-a' } });
    await waitFor(() => expect(workflows).toHaveBeenCalledWith('project-a'));
    await waitFor(() => expect(screen.getByTestId('workflow-text')).toHaveValue(finalizeText));
    await makeDirty(`${finalizeText}state draft\n`);

    await rerender({ projectId: 'project-b' });
    await fireEvent.click(await screen.findByRole('button', { name: 'Stay' }));

    expect((screen.getByTestId('project-select') as HTMLSelectElement).value).toBe('project-a');
    expect(window.location.pathname + window.location.search).toBe('/designer?project=project-a');
    expect(window.history).toHaveLength(1);
    expect(get(runtimeStore).designerProjectId).toBe('project-a');

    // The route returns to A, so a later route to B is a new request, not an already-adopted one.
    await rerender({ projectId: 'project-a' });
    await rerender({ projectId: 'project-b' });
    expect(await screen.findByRole('button', { name: 'Discard the draft' })).toBeInTheDocument();
  });

  it('moves the project, the selector, and the route to B when the dirty change is discarded', async () => {
    const workflows = vi.spyOn(apiClient, 'getWorkflows');
    const { rerender } = render(ProcessDesigner, { props: { projectId: 'project-a' } });
    await waitFor(() => expect(workflows).toHaveBeenCalledWith('project-a'));
    await waitFor(() => expect(screen.getByTestId('workflow-text')).toHaveValue(finalizeText));
    await makeDirty(`${finalizeText}state draft\n`);

    await rerender({ projectId: 'project-b' });
    await fireEvent.click(await screen.findByRole('button', { name: 'Discard the draft' }));

    await waitFor(() => expect(workflows).toHaveBeenCalledWith('project-b'));
    await waitFor(() =>
      expect((screen.getByTestId('project-select') as HTMLSelectElement).value).toBe('project-b'),
    );
    expect(window.location.pathname + window.location.search).toBe('/designer?project=project-b');
    expect(get(runtimeStore).designerProjectId).toBe('project-b');
  });

  it('adopts machine scope for a dirty change to a plain /designer route', async () => {
    const workflows = vi.spyOn(apiClient, 'getWorkflows');
    const { rerender } = render(ProcessDesigner, { props: { projectId: 'project-a' } });
    await waitFor(() => expect(workflows).toHaveBeenCalledWith('project-a'));
    await waitFor(() => expect(screen.getByTestId('workflow-text')).toHaveValue(finalizeText));
    await makeDirty(`${finalizeText}state draft\n`);

    await rerender({ projectId: null });
    await fireEvent.click(await screen.findByRole('button', { name: 'Discard the draft' }));

    await waitFor(() => expect(workflows).toHaveBeenCalledWith(null));
    expect(window.location.pathname + window.location.search).toBe('/designer');
    expect(screen.getByTestId('designer-checked')).toHaveTextContent(
      "checked against this machine's projects",
    );
    expect(get(runtimeStore).designerProjectId).toBeNull();
  });

  it('keeps a local selection in the route, so a rerender and a reload open it again', async () => {
    const workflows = vi.spyOn(apiClient, 'getWorkflows');
    const { rerender, unmount } = render(ProcessDesigner, {
      props: { projectId: 'project-a' },
    });
    await waitFor(() => expect(workflows).toHaveBeenCalledWith('project-a'));
    await waitFor(() => expect(screen.getByTestId('workflow-text')).toHaveValue(finalizeText));

    const select = screen.getByTestId('project-select') as HTMLSelectElement;
    await fireEvent.change(select, { target: { value: 'project-b' } });

    await waitFor(() => expect(workflows).toHaveBeenCalledWith('project-b'));
    await waitFor(() => expect(select.value).toBe('project-b'));
    expect(window.location.pathname + window.location.search).toBe('/designer?project=project-b');

    // The store follows the selection, so the same prop comes back on a rerender with no reload.
    await rerender({ projectId: 'project-b' });
    expect(screen.queryByRole('button', { name: 'Stay' })).not.toBeInTheDocument();
    expect(window.location.pathname + window.location.search).toBe('/designer?project=project-b');

    // A reload remounts the designer with the project the address bar now names.
    unmount();
    render(ProcessDesigner, { props: { projectId: 'project-b' } });
    await waitFor(() => expect(screen.getByTestId('project-select')).toHaveValue('project-b'));
  });

  it('moves the route to the project a restored draft names', async () => {
    localStorage.setItem(
      'pm_designer_draft',
      JSON.stringify({
        text: `${finalizeText}state draft\n`,
        unitName: 'phase-6-finalize',
        unitKind: 'subworkflow',
        projectId: 'project-a',
        savedAt: '2026-10-22T09:30:00Z',
      }),
    );
    const workflows = vi.spyOn(apiClient, 'getWorkflows');

    render(ProcessDesigner, { props: { projectId: 'project-b' } });

    await waitFor(() => expect(workflows).toHaveBeenCalledWith('project-a'));
    await waitFor(() =>
      expect(window.location.pathname + window.location.search).toBe('/designer?project=project-a'),
    );
    expect(get(runtimeStore).designerProjectId).toBe('project-a');
    await waitFor(() =>
      expect((screen.getByTestId('project-select') as HTMLSelectElement).value).toBe('project-a'),
    );
  });

  it('keeps the previous project and route when the new scope fails to load', async () => {
    const workflows = vi.spyOn(apiClient, 'getWorkflows').mockImplementation(async (project) => {
      if (project === 'project-b') {
        throw new Error('unreachable');
      }
      return units;
    });
    render(ProcessDesigner, { props: { projectId: 'project-a' } });
    await waitFor(() => expect(screen.getByTestId('workflow-text')).toHaveValue(finalizeText));

    const select = screen.getByTestId('project-select') as HTMLSelectElement;
    await fireEvent.change(select, { target: { value: 'project-b' } });

    await waitFor(() =>
      expect(screen.getByTestId('designer-notice')).toHaveTextContent('could not be loaded'),
    );
    expect(workflows).toHaveBeenCalledWith('project-b');
    await waitFor(() => expect(select.value).toBe('project-a'));
    expect(window.location.pathname + window.location.search).toBe('/designer?project=project-a');
    expect(get(runtimeStore).designerProjectId).toBe('project-a');
  });
});
