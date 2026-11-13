import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import { get } from 'svelte/store';
import StartView from './StartView.svelte';
import { apiClient, type EpicDecomposition } from '../api/client';
import { ClientError } from '../api/errors';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';
import { PROJECT, entry, representation, source, sources } from './startFixtures';

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

  it('draws skeletons while the entry is still being read', async () => {
    vi.spyOn(apiClient, 'openEntry').mockReturnValue(new Promise(() => {}));
    vi.spyOn(apiClient, 'subscribeEntryEvents').mockReturnValue(() => {});

    render(StartView, { projectId: PROJECT, kind: 'plan' });

    const loading = await screen.findByTestId('start-loading');
    expect(loading.querySelector('[data-testid="skeleton"]')).not.toBeNull();
  });

  it('offers the projects when the start names none, and opens the one chosen', async () => {
    runtimeStore.set(makeRuntimeState({ currentRoute: 'start', startEntryId: 'ent-old' }));
    vi.spyOn(apiClient, 'getProjects').mockResolvedValue({
      projects: [
        { project_id: 'pm-mcp-core', display_name: 'PM-MCP core' },
        { project_id: PROJECT, display_name: 'PM-MCP clients' },
      ],
    } as Awaited<ReturnType<typeof apiClient.getProjects>>);

    render(StartView, {});

    const choice = await screen.findByTestId('start-project-choice');
    expect(choice).toHaveTextContent('Projects');
    await fireEvent.click(screen.getByRole('button', { name: 'PM-MCP core' }));

    expect(get(runtimeStore)).toMatchObject({ startProjectId: 'pm-mcp-core', startEntryId: null });
  });

  it("adopts the project of an entry opened by its id", async () => {
    arrangeOpen();
    runtimeStore.set(makeRuntimeState({ currentRoute: 'start', startProjectId: null }));

    render(StartView, { entryId: 'ent-3f9a2c41' });

    await screen.findByTestId('source-list');
    expect(get(runtimeStore).startProjectId).toBe(PROJECT);
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

    it.each([
      'reset',
      'source.added',
      'source.removed',
      'source.flagged',
      'progress.changed',
      'check.result',
      'entry.moved',
      'state.entered',
    ])(
      're-reads the entry on %s and shows what changed',
      async (event) => {
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
      },
    );

    it.each([
      'chat.turn.delta',
      'chat.turn.completed',
      'chat.proposal',
      'chat.ended',
      'entry.changed',
      'source.changed',
      'chat.turn',
    ])(
      'does not re-read the entry on %s: a chat event is the chat\'s own read, and the rest is a piece of a turn or a name no contract has',
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
      const commit = vi.spyOn(apiClient, 'commitDecomposition').mockResolvedValue(undefined as never);

      await fireEvent.click(screen.getByTestId('started-decompose'));
      expect(await screen.findByTestId('decomposition-error')).toHaveTextContent(
        'if it keeps taking this long',
      );
      expect(screen.getByTestId('start-step')).toHaveTextContent('Decomposition');

      await fireEvent.click(screen.getByTestId('decomposition-retry'));
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

    it('edits a staged plan, reads the change back, and drops a plan through a rewrite', async () => {
      await adoptEpic();
      vi.spyOn(apiClient, 'getDecomposition').mockResolvedValue(CUT);
      const edited = structuredClone(CUT);
      edited.workstreams[0].staged_plans[0].objective = 'order the cursor first';
      const instruct = vi
        .spyOn(apiClient, 'instructDecomposition')
        .mockResolvedValueOnce(edited)
        .mockResolvedValueOnce({ ...CUT, workstreams: [] });

      await fireEvent.click(screen.getByTestId('started-decompose'));
      await fireEvent.click(await screen.findByTestId('plan-edit-relay-cursor-order'));
      await fireEvent.input(screen.getByTestId('instruct-input'), {
        target: { value: 'Order the cursor first.' },
      });
      await fireEvent.click(screen.getByTestId('instruct-submit'));

      expect(instruct).toHaveBeenCalledWith(PROJECT, 'relay-hardening', {
        plan_id: 'relay-cursor-order',
        instruction: 'Order the cursor first.',
        action: 'edit',
      });
      expect(await screen.findByTestId('read-back-accept')).toBeInTheDocument();
      await fireEvent.click(screen.getByTestId('read-back-accept'));
      expect(screen.queryByTestId('read-back-accept')).toBeNull();

      await fireEvent.click(screen.getByTestId('plan-drop-relay-cursor-order'));

      expect(instruct).toHaveBeenLastCalledWith(PROJECT, 'relay-hardening', {
        plan_id: null,
        instruction: 'Drop the plan relay-cursor-order from the cut.',
        action: 'rewrite',
      });
      await vi.waitFor(() =>
        expect(screen.queryByTestId('plan-drop-relay-cursor-order')).toBeNull(),
      );
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
      await fireEvent.click(screen.getByRole('button', { name: "Continue the machine's onboarding" }));

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
      await fireEvent.click(screen.getByRole('button', { name: "Continue the project's onboarding" }));

      expect(get(runtimeStore)).toMatchObject({
        currentRoute: 'project-onboarding',
        selectedProjectId: PROJECT,
      });
    });
  });
});
