import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, within } from '@testing-library/svelte';
import { get } from 'svelte/store';
import StartView from './StartView.svelte';
import { apiClient, type EpicDecomposition } from '../api/client';
import { ClientError } from '../api/errors';
import { runtimeStore } from '../stores/runtime';
import { ENTRY_REFRESH_EVENTS } from '../stores/refreshEvents';
import { makeRuntimeState } from '../test/runtimeState';
import { messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';
import { PROJECT, RECIPES, entry, offersFor, representation, source, sources } from './startFixtures';

/**
 * The start view's two patterns: the error block of a failed read with its act, the skeletons of a
 * read still in flight, and the act line of a failure the act itself reported.
 */

/**
 * @param repr the representation the entry is opened with
 * @returns nothing; the client is stubbed for one open entry
 */
function arrangeOpen(repr = sources()): void {
  vi.spyOn(apiClient, 'openEntry').mockResolvedValue(repr);
  vi.spyOn(apiClient, 'getEntry').mockResolvedValue(repr);
  vi.spyOn(apiClient, 'subscribeEntryEvents').mockReturnValue(() => {});
  vi.spyOn(apiClient, 'getProjectExplorer').mockResolvedValue({
    favourites: [],
    recent: [],
    all_projects: [],
    being_added: [],
  });
}

describe('StartView', () => {
  it("shows the sentence of the act failure's code instead of the code", async () => {
    arrangeOpen();
    vi.spyOn(apiClient, 'actEntry').mockRejectedValue(new ClientError('timeout'));

    render(StartView, { projectId: PROJECT, kind: 'plan' });

    await screen.findByTestId('source-list');
    await fireEvent.input(screen.getByTestId('description-input'), {
      target: { value: 'Add a Codeberg provider for issues' },
    });
    await fireEvent.click(screen.getByTestId('description-add'));

    const banner = await screen.findByTestId('start-action-error');
    expect(banner.textContent?.trim()).toBe(
      'The start failed: Try again; if it keeps taking this long, check that the runtime is running.',
    );
  });

  it('draws the error pattern with its act when the entry cannot be read', async () => {
    vi.spyOn(apiClient, 'getEntry').mockRejectedValue(new ClientError('network_offline'));
    vi.spyOn(apiClient, 'subscribeEntryEvents').mockReturnValue(() => {});

    render(StartView, { entryId: 'ent-3f9a2c41' });

    const error = await screen.findByTestId('start-error');
    expect(error).toHaveTextContent('The machine is not reachable');
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
  });

  it('draws the setup gate with its spinner while the entry is still being opened', async () => {
    vi.spyOn(apiClient, 'openEntry').mockReturnValue(new Promise(() => {}));
    vi.spyOn(apiClient, 'subscribeEntryEvents').mockReturnValue(() => {});

    render(StartView, { projectId: PROJECT, kind: 'plan' });

    const loading = await screen.findByTestId('start-loading');
    expect(loading).toHaveTextContent("Checking the project's setup…");
    expect(
      within(screen.getByTestId('start-gate-checking')).getByRole('status'),
    ).toBeInTheDocument();
  });

  it('bounds the setup gate at 20 seconds', async () => {
    const open = vi.spyOn(apiClient, 'openEntry').mockResolvedValue(sources());
    vi.spyOn(apiClient, 'subscribeEntryEvents').mockReturnValue(() => {});

    render(StartView, { projectId: PROJECT, kind: 'plan' });
    await screen.findByTestId('source-list');

    expect(open).toHaveBeenCalledTimes(1);
    const [, request, signal, timeoutMs] = open.mock.calls[0];
    expect(request).toEqual({ kind: 'plan' });
    expect(signal).toBeInstanceOf(AbortSignal);
    expect(timeoutMs).toBe(20_000);
  });

  it('draws the error pattern with its act when the start names no project, and leads to the projects', async () => {
    runtimeStore.set(makeRuntimeState({ currentRoute: 'start', startEntryId: null }));

    render(StartView, {});

    const pattern = await screen.findByTestId('start-no-project');
    expect(pattern).toHaveTextContent('There is no project to start work in');
    await fireEvent.click(screen.getByRole('button', { name: 'Choose a project' }));

    expect(get(runtimeStore).currentRoute).toBe('projects');
  });

  it('adopts the project of an entry opened by its id', async () => {
    arrangeOpen();
    runtimeStore.set(makeRuntimeState({ currentRoute: 'start', startProjectId: null }));

    render(StartView, { entryId: 'ent-3f9a2c41' });

    await screen.findByTestId('source-list');
    expect(get(runtimeStore).startProjectId).toBe(PROJECT);
  });

  it("closes the abandon question when another entry is addressed", async () => {
    arrangeOpen(sources());
    const rendered = render(StartView, { projectId: PROJECT, kind: 'plan' });
    await screen.findByTestId('source-list');

    await fireEvent.click(screen.getByRole('button', { name: 'More' }));
    await fireEvent.click(await screen.findByTestId('action-abandon'));
    expect(await screen.findByTestId('confirm-dialog')).toBeInTheDocument();

    await rendered.rerender({ entryId: 'ent-other', projectId: null, kind: 'plan' });

    await vi.waitFor(() => expect(screen.queryByTestId('confirm-dialog')).toBeNull());
  });

  describe('the live entry', () => {
    /** @returns the listener the view registered for the entry's events */
    function arrangeEvents(): { emit: (event: string) => void } {
      let listener: ((event: string, data: unknown) => void) | null = null;
      vi.spyOn(apiClient, 'subscribeEntryEvents').mockImplementation((_id, callback) => {
        listener = callback as (event: string, data: unknown) => void;
        return () => {};
      });
      return { emit: (event) => listener?.(event, {}) };
    }

    it.each(ENTRY_REFRESH_EVENTS)('re-reads the entry on %s and shows what changed', async (event) => {
      arrangeOpen();
      const { emit } = arrangeEvents();
      const read = vi
        .spyOn(apiClient, 'getEntry')
        .mockResolvedValue(sources({ sources: [source({ title: 'Read by the event' })] }));
      render(StartView, { projectId: PROJECT, kind: 'plan' });
      await screen.findByTestId('source-list');

      emit(event);

      expect(await screen.findByText('Read by the event')).toBeInTheDocument();
      expect(read).toHaveBeenCalledWith('ent-3f9a2c41');
    });

    it.each([
      'chat.turn.delta',
      'chat.turn.completed',
      'chat.proposal',
      'chat.ended',
      'entry.changed',
      'source.changed',
      'chat.turn',
    ])(
      "does not re-read the entry on %s: a chat event is the chat's own read, and the rest is a piece of a turn or a name no contract has",
      async (event) => {
        arrangeOpen();
        const { emit } = arrangeEvents();
        const read = vi.spyOn(apiClient, 'getEntry');
        render(StartView, { projectId: PROJECT, kind: 'plan' });
        await screen.findByTestId('source-list');

        emit(event);

        expect(read).not.toHaveBeenCalled();
      },
    );

    it('does not let an older read that completes last restore what a newer read replaced', async () => {
      arrangeOpen(sources({ sources: [source({ title: 'Shown before' })] }));
      const { emit } = arrangeEvents();
      let finishOlder: (resource: ReturnType<typeof sources>) => void = () => {};
      const older = new Promise<ReturnType<typeof sources>>((resolve) => {
        finishOlder = resolve;
      });
      const read = vi
        .spyOn(apiClient, 'getEntry')
        .mockReturnValueOnce(older)
        .mockResolvedValueOnce(sources({ sources: [source({ title: 'After the removal' })] }));
      render(StartView, { projectId: PROJECT, kind: 'plan' });
      await screen.findByText('Shown before');

      emit('source.added');
      emit('source.removed');
      expect(await screen.findByText('After the removal')).toBeInTheDocument();
      finishOlder(sources({ sources: [source({ title: 'Restored by the older read' })] }));

      await vi.waitFor(() => expect(read).toHaveBeenCalledTimes(2));
      await Promise.resolve();
      expect(screen.queryByText('Restored by the older read')).toBeNull();
      expect(screen.getByText('After the removal')).toBeInTheDocument();
    });

    it('keeps what it shows when the re-read fails', async () => {
      arrangeOpen(sources({ sources: [source({ title: 'Shown before' })] }));
      const { emit } = arrangeEvents();
      const read = vi.spyOn(apiClient, 'getEntry').mockRejectedValue(new ClientError('timeout'));
      render(StartView, { projectId: PROJECT, kind: 'plan' });
      await screen.findByText('Shown before');

      emit('source.added');

      await vi.waitFor(() => expect(read).toHaveBeenCalled());
      expect(screen.getByText('Shown before')).toBeInTheDocument();
      expect(screen.queryByTestId('start-error')).toBeNull();
    });
  });

  describe("an adopted epic's decomposition", () => {
    const CUT: EpicDecomposition = {
      epic_id: 'relay-hardening',
      status: 'check',
      revision: 1,
      workstreams: [
        {
          id: 'WS-1',
          name: 'Reconnect',
          order: 1,
          staged_plans: [
            {
              plan_id: 'relay-cursor-order',
              title: 'relay-cursor-order',
              expected_surface: ['pm-relay/src/cursor'],
              dependencies: [],
              status: 'staged',
            },
          ],
        },
      ],
      findings: [],
    };

    /** Opens the adopt step and adopts the epic, so the started step offers its decomposition. */
    async function adoptEpic(): Promise<void> {
      arrangeOpen(
        representation({
          entry: entry({
            kind: 'epic',
            state: 'start-epic.adopt',
            adoptable_epics: [{ epic_id: 'relay-hardening', title: 'Relay hardening' }],
          }),
        }),
      );
      vi.spyOn(apiClient, 'actEntry').mockResolvedValue(
        representation({
          entry: entry({
            kind: 'epic',
            state: 'start.ready',
            title: 'Relay hardening',
            plan_id: 'relay-hardening',
          }),
        }),
      );
      render(StartView, { projectId: PROJECT, kind: 'epic' });
      await fireEvent.click(await screen.findByTestId('adopt-relay-hardening'));
      await screen.findByTestId('started-title');
    }

    it('reads the cut again after a failed read, and commits it once agreed', async () => {
      await adoptEpic();
      const read = vi
        .spyOn(apiClient, 'getDecomposition')
        .mockRejectedValueOnce(new ClientError('timeout'))
        .mockResolvedValue(CUT);
      const commit = vi
        .spyOn(apiClient, 'commitDecomposition')
        .mockResolvedValue(undefined as never);

      await fireEvent.click(screen.getByTestId('started-decompose'));
      expect(await screen.findByTestId('decomposition-error')).toHaveTextContent(
        'if it keeps taking this long',
      );
      expect(screen.getByTestId('start-step')).toHaveTextContent('Decomposition');

      const retryBtn = screen.getByTestId('decomposition-retry');
      expect(retryBtn).toHaveClass('btn--secondary');
      await fireEvent.click(retryBtn);
      expect(await screen.findByTestId('decomposition-title')).toHaveTextContent(
        'relay-hardening · decomposition',
      );
      expect(read).toHaveBeenLastCalledWith(PROJECT, 'relay-hardening');

      await fireEvent.click(screen.getByTestId('agree-commit'));

      expect(commit).toHaveBeenCalledWith(PROJECT, 'relay-hardening');
      expect(await screen.findByTestId('decomposition-committed')).toHaveTextContent('committed');
    });

    it('keeps the review and names why a commit failed', async () => {
      await adoptEpic();
      vi.spyOn(apiClient, 'getDecomposition').mockResolvedValue(CUT);
      vi.spyOn(apiClient, 'commitDecomposition').mockRejectedValue(new ClientError('timeout'));

      await fireEvent.click(screen.getByTestId('started-decompose'));
      await fireEvent.click(await screen.findByTestId('agree-commit'));

      expect(await screen.findByTestId('decomposition-error')).toHaveTextContent(
        'if it keeps taking this long',
      );
      expect(screen.getByTestId('decomposition-title')).toBeInTheDocument();
      expect(screen.queryByTestId('decomposition-committed')).toBeNull();
    });

    it('edits a staged plan, reads the change back, offers no Drop act, and shows a wire-dropped plan as dropped', async () => {
      await adoptEpic();
      const cutWithDropped: EpicDecomposition = {
        ...CUT,
        workstreams: [
          {
            ...CUT.workstreams[0]!,
            staged_plans: [
              ...CUT.workstreams[0]!.staged_plans,
              {
                plan_id: 'relay-dropped',
                title: 'relay-dropped',
                expected_surface: ['pm-relay/src/dropped'],
                dependencies: [],
                status: 'dropped',
              },
            ],
          },
        ],
      };
      vi.spyOn(apiClient, 'getDecomposition').mockResolvedValue(cutWithDropped);
      const edited = structuredClone(cutWithDropped);
      edited.workstreams[0]!.staged_plans[0]!.objective = 'order the cursor first';
      const instruct = vi.spyOn(apiClient, 'instructDecomposition').mockResolvedValueOnce(edited);

      await fireEvent.click(screen.getByTestId('started-decompose'));
      await screen.findByTestId('plan-edit-relay-cursor-order');

      expect(screen.queryByRole('button', { name: 'Drop' })).toBeNull();
      expect(screen.queryByTestId('plan-drop-relay-cursor-order')).toBeNull();
      expect(screen.getByTestId('plan-relay-dropped')).toHaveTextContent('dropped');

      messagesStore.set(loadMessages('de'));
      await vi.waitFor(() =>
        expect(screen.getByTestId('plan-relay-dropped')).toHaveTextContent('entfernt'),
      );
      expect(screen.queryByRole('button', { name: 'Lass ihn fallen' })).toBeNull();
      messagesStore.set(loadMessages('en'));

      await fireEvent.click(await screen.findByTestId('plan-edit-relay-cursor-order'));
      const instructPanel = screen.getByTestId('decomposition-instruction');
      const textbox = within(instructPanel).getByRole('textbox', { name: 'What should change?' });
      await fireEvent.input(textbox, {
        target: { value: 'Order the cursor first.' },
      });
      await fireEvent.keyDown(textbox, { key: 'Enter' });

      expect(instruct).toHaveBeenCalledWith(PROJECT, 'relay-hardening', {
        plan_id: 'relay-cursor-order',
        instruction: 'Order the cursor first.',
        action: 'edit',
      });
      expect(await screen.findByTestId('read-back-accept')).toBeInTheDocument();
      await fireEvent.click(screen.getByTestId('read-back-accept'));
      expect(screen.queryByTestId('read-back-accept')).toBeNull();
    });
  });

  describe('the gate sentence and the abandon act (D0)', () => {
    it("names the gate's own bound only when the gate itself timed out", async () => {
      vi.spyOn(apiClient, 'openEntry').mockRejectedValue(new ClientError('timeout'));
      vi.spyOn(apiClient, 'subscribeEntryEvents').mockReturnValue(() => {});

      render(StartView, { projectId: PROJECT, kind: 'plan' });

      const cause = await screen.findByTestId('start-gate-cause');
      expect(cause.textContent?.trim()).toBe("The project's setup did not answer within 20 seconds.");
      // One live region for one event: the error block announces, the sentence beside it does not.
      expect(
        screen.getByTestId('start-error').querySelectorAll('[role="status"], [role="alert"]'),
      ).toHaveLength(1);
    });

    it('keeps the gate sentence off a timeout of the resume read', async () => {
      vi.spyOn(apiClient, 'getEntry').mockRejectedValue(new ClientError('timeout'));
      vi.spyOn(apiClient, 'subscribeEntryEvents').mockReturnValue(() => {});

      render(StartView, { entryId: 'ent-3f9a2c41' });

      expect(await screen.findByTestId('start-error')).toBeInTheDocument();
      expect(screen.queryByTestId('start-gate-cause')).toBeNull();
      expect(
        screen.getByTestId('start-error').querySelectorAll('[role="status"], [role="alert"]'),
      ).toHaveLength(1);
    });

    it('aborts the gate request when the view is destroyed', async () => {
      let signal: AbortSignal | undefined;
      let asked = false;
      vi.spyOn(apiClient, 'openEntry').mockImplementation((_project, _request, passed) => {
        asked = true;
        signal = passed ?? undefined;
        return new Promise(() => {});
      });
      vi.spyOn(apiClient, 'subscribeEntryEvents').mockReturnValue(() => {});

      const rendered = render(StartView, { projectId: PROJECT, kind: 'plan' });
      await screen.findByTestId('start-loading');
      expect(asked).toBe(true);

      rendered.unmount();

      expect(signal?.aborted).toBe(true);
    });

    it('keeps the route and names the failure when the abandon act fails', async () => {
      arrangeOpen();
      runtimeStore.set(makeRuntimeState({ currentRoute: 'start', startEntryId: 'ent-3f9a2c41' }));
      vi.spyOn(apiClient, 'actEntry').mockRejectedValue(new ClientError('timeout'));

      render(StartView, { projectId: PROJECT, kind: 'plan' });
      await screen.findByTestId('source-list');
      await fireEvent.click(screen.getByRole('button', { name: 'More' }));
      await fireEvent.click(await screen.findByTestId('action-abandon'));
      const dialog = await screen.findByTestId('confirm-dialog');
      await fireEvent.click(within(dialog).getByRole('button', { name: 'Abandon' }));

      expect(await screen.findByTestId('start-action-error')).toHaveTextContent(
        'if it keeps taking this long',
      );
      expect(get(runtimeStore).currentRoute).toBe('start');
    });

    it('abandons the entry and goes to the projects when the act answers', async () => {
      arrangeOpen();
      runtimeStore.set(makeRuntimeState({ currentRoute: 'start', startEntryId: 'ent-3f9a2c41' }));
      const act = vi.spyOn(apiClient, 'actEntry').mockResolvedValue(sources());

      render(StartView, { projectId: PROJECT, kind: 'plan' });
      await screen.findByTestId('source-list');
      await fireEvent.click(screen.getByRole('button', { name: 'More' }));
      await fireEvent.click(await screen.findByTestId('action-abandon'));
      const dialog = await screen.findByTestId('confirm-dialog');
      await fireEvent.click(within(dialog).getByRole('button', { name: 'Abandon' }));

      expect(act).toHaveBeenCalledWith('ent-3f9a2c41', 'abandon', {});
      await vi.waitFor(() => expect(get(runtimeStore).currentRoute).toBe('projects'));
    });

    it('names the overflow trigger away from its one item', async () => {
      arrangeOpen();
      render(StartView, { projectId: PROJECT, kind: 'plan' });
      await screen.findByTestId('source-list');

      const trigger = screen.getByRole('button', { name: 'More' });
      expect(trigger).not.toHaveAccessibleName('Abandon');
      await fireEvent.click(trigger);
      expect(await screen.findByTestId('action-abandon')).toHaveAccessibleName('Abandon');
    });

    it('names every recipe row act with its recipe', async () => {
      arrangeOpen(representation({ entry: entry({ state: 'start.recipe' }), recipes: RECIPES }));
      render(StartView, { projectId: PROJECT, kind: 'plan' });
      await screen.findByTestId('recipe-code-review');

      expect(
        screen.getByRole('button', { name: 'Choose a recipe code-review' }),
      ).toBeInTheDocument();
      expect(
        screen.getByRole('button', { name: 'Choose a recipe no-modules' }),
      ).toBeInTheDocument();
    });

    it('names why Continue cannot run yet', async () => {
      arrangeOpen(sources({ can_continue: false, kind_offers: offersFor('plan') }));
      render(StartView, { projectId: PROJECT, kind: 'plan' });
      await screen.findByTestId('source-list');

      const continued = screen.getByTestId('source-continue');
      expect(continued).toHaveAttribute('aria-disabled', 'true');
      const describedBy = continued.getAttribute('aria-describedby');
      expect(describedBy).toBeTruthy();
      expect(document.getElementById(describedBy!)).toHaveTextContent('waits for 1 source');
    });
  });

  describe('the recipe and ready steps (D1)', () => {
    it('offers one primary act on the recipe step, and it chooses the recipe', async () => {
      arrangeOpen(
        representation({ entry: entry({ state: 'start.recipe' }), recipes: RECIPES }),
      );
      const act = vi
        .spyOn(apiClient, 'actEntry')
        .mockResolvedValue(
          representation({ entry: entry({ state: 'recipe.parameters' }), recipes: RECIPES }),
        );

      render(StartView, { projectId: PROJECT, kind: 'plan' });

      const step = await screen.findByTestId('start-recipe');
      const primaries = step.querySelectorAll('.btn--primary');
      expect(primaries).toHaveLength(1);
      const primary = within(step).getByRole('button', { name: 'Choose a recipe code-review' });
      expect(primary).toHaveClass('btn--primary');

      await fireEvent.click(primary);

      expect(act).toHaveBeenCalledWith('ent-3f9a2c41', 'choose:code-review', {});
    });

    it('starts the recipe by the one primary act of the ready step', async () => {
      arrangeOpen(
        representation({
          entry: entry({ state: 'start.ready', recipe: RECIPES[0] }),
          ready: {
            title: 'A code review',
            plan_id: 'recipe-code-review-2026-10-19-14-05-00',
            id_editable: false,
            id_from_title: false,
            recipe: RECIPES[0],
            parameters: [{ ...RECIPES[0].parameters[0], value: 'deep' }],
            landing: 'landing.pull-request',
            landing_asked: false,
          },
        }),
      );
      const act = vi
        .spyOn(apiClient, 'actEntry')
        .mockResolvedValue(representation({ entry: entry({ state: 'start.ready' }) }));

      render(StartView, { projectId: PROJECT, kind: 'plan' });

      await screen.findByTestId('recipe-ready');
      expect(document.querySelectorAll('.btn--primary')).toHaveLength(1);
      const primary = screen.getByTestId('ready-start');
      expect(primary).toHaveClass('btn--primary');

      await fireEvent.click(primary);

      expect(act).toHaveBeenCalledWith('ent-3f9a2c41', 'create', { title: 'A code review' });
    });

    it('starts the recipe by Enter in the ready step, and never on an IME confirm', async () => {
      arrangeOpen(
        representation({
          entry: entry({ state: 'start.ready' }),
          ready: {
            title: 'Add a Codeberg provider for issues',
            plan_id: 'codeberg-provider-issues',
            id_editable: false,
            id_from_title: false,
            parameters: [],
            landing: 'landing.pull-request',
            landing_asked: false,
          },
        }),
      );
      const act = vi
        .spyOn(apiClient, 'actEntry')
        .mockResolvedValue(representation({ entry: entry({ state: 'start.ready' }) }));

      render(StartView, { projectId: PROJECT, kind: 'plan' });

      const title = await screen.findByTestId('ready-title');
      await fireEvent.keyDown(title, { key: 'Enter', isComposing: true });
      await fireEvent.keyDown(title, { key: 'Enter', keyCode: 229 });
      expect(act).not.toHaveBeenCalled();

      await fireEvent.keyDown(title, { key: 'Enter' });

      expect(act).toHaveBeenCalledWith('ent-3f9a2c41', 'create', {
        title: 'Add a Codeberg provider for issues',
      });
    });

    it('shows a parameter without options as its reason, with no field', async () => {
      const recipe = {
        key: 'free',
        name: 'free',
        description: 'A recipe the project offers',
        origin: 'project',
        takes_request: false,
        parameters: [
          { name: 'target', options: [], proposed: false, available: true },
        ],
      };
      arrangeOpen(
        representation({
          entry: entry({ state: 'recipe.parameters', recipe, sources: [] }),
        }),
      );
      vi.spyOn(apiClient, 'actEntry').mockResolvedValue(representation());

      render(StartView, { projectId: PROJECT, kind: 'plan' });

      const step = await screen.findByTestId('recipe-ready');
      expect(within(step).getByTestId('no-option-target')).toHaveTextContent(
        'target: none the recipe can work on; the recipe cannot run',
      );
      expect(within(step).queryByTestId('parameter-target')).toBeNull();
    });
  });

  describe('the setup gate', () => {
    it("opens the machine's onboarding when the machine is not onboarded", async () => {
      arrangeOpen(
        representation({
          entry: entry({ state: 'start.refused' }),
          refusal: {
            reason: 'machine_not_onboarded',
            need: 'providers',
            action_key: 'start.gate.continue_onboarding',
            action_href: '/onboarding',
          },
        }),
      );
      render(StartView, { projectId: PROJECT, kind: 'plan' });

      const refused = await screen.findByTestId('start-refused');
      expect(refused).toHaveTextContent('This machine is not fully onboarded: providers.');
      await fireEvent.click(
        screen.getByRole('button', { name: "Continue the machine's onboarding" }),
      );

      expect(get(runtimeStore).currentRoute).toBe('onboarding');
    });

    it("opens the project's own onboarding when the project is not ready", async () => {
      arrangeOpen(
        representation({
          entry: entry({ state: 'start.refused' }),
          refusal: {
            reason: 'project_not_ready',
            project_id: PROJECT,
            need: 'credential sonar',
            action_key: 'start.gate.continue_project',
            action_href: '/projects/pm-mcp-clients/onboarding',
          },
        }),
      );
      render(StartView, { projectId: PROJECT, kind: 'plan' });

      await screen.findByTestId('start-refused');
      await fireEvent.click(
        screen.getByRole('button', { name: "Continue the project's onboarding" }),
      );

      expect(get(runtimeStore)).toMatchObject({
        currentRoute: 'project-onboarding',
        selectedProjectId: PROJECT,
      });
    });
  });
});
