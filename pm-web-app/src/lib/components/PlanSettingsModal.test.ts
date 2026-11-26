import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import PlanSettingsModal from './PlanSettingsModal.svelte';
import type { PlanSettingsRecord } from '../api/client';

describe('PlanSettingsModal component', () => {
  const settings: PlanSettingsRecord = {
    plan_id: 'fix-relay',
    revision: 7,
    values: [
      {
        aspect: 'landing',
        value: 'landing.pull-request',
        origin: 'project',
        choices: ['landing.pull-request', 'landing.main', 'landing.files'],
      },
      {
        aspect: 'method',
        value: 'method.test-first',
        origin: 'machine',
        choices: ['method.test-first', 'method.tests-after'],
      },
      { aspect: 'role.implement', value: 'Claude Code - sonnet-5.5', origin: 'plan', choices: [] },
      { aspect: 'integration.git', value: 'native', origin: 'project', choices: [] },
      { aspect: 'provider.issues', value: 'GitHub', origin: 'project', choices: [] },
    ],
  };

  it('renders the plan title and groups the aspects by kind', () => {
    render(PlanSettingsModal, { props: { planId: 'fix-relay', settings, open: true } });

    expect(screen.getByTestId('settings-title')).toHaveTextContent(/Settings/);
    expect(screen.getByTestId('settings-title')).toHaveTextContent('fix-relay');
    expect(screen.getByTestId('settings-group-plan_defaults')).toBeInTheDocument();
    expect(screen.getByTestId('settings-group-roles')).toBeInTheDocument();
    expect(screen.getByTestId('settings-group-integrations')).toBeInTheDocument();
    expect(screen.getByTestId('settings-group-providers')).toBeInTheDocument();
  });

  it('shows the value of each aspect and where it comes from', () => {
    render(PlanSettingsModal, { props: { planId: 'fix-relay', settings, open: true } });

    expect(screen.getByTestId('settings-value-landing')).toHaveValue('landing.pull-request');
    expect(screen.getByTestId('settings-origin-landing')).toHaveTextContent('from the project');
    expect(screen.getByTestId('settings-origin-method')).toHaveTextContent('from the machine');
  });

  it('keeps Save disabled until a field changes', async () => {
    render(PlanSettingsModal, { props: { planId: 'fix-relay', settings, open: true } });

    const save = screen.getByTestId('settings-save');
    expect(save).toBeDisabled();

    await fireEvent.change(screen.getByTestId('settings-value-landing'), {
      target: { value: 'landing.files' },
    });

    expect(save).not.toBeDisabled();
  });

  it('marks a field that changed', async () => {
    render(PlanSettingsModal, { props: { planId: 'fix-relay', settings, open: true } });

    expect(screen.queryByTestId('settings-changed-landing')).toBeNull();

    await fireEvent.change(screen.getByTestId('settings-value-landing'), {
      target: { value: 'landing.files' },
    });

    expect(screen.getByTestId('settings-changed-landing')).toHaveTextContent('changed');
    expect(screen.queryByTestId('settings-changed-method')).toBeNull();
  });

  it('applies every changed field with the revision it was opened with', async () => {
    const onApply = vi.fn();
    render(PlanSettingsModal, { props: { planId: 'fix-relay', settings, open: true, onApply } });

    await fireEvent.change(screen.getByTestId('settings-value-landing'), {
      target: { value: 'landing.files' },
    });
    await fireEvent.click(screen.getByTestId('settings-save'));

    expect(onApply).toHaveBeenCalledTimes(1);
    expect(onApply).toHaveBeenCalledWith({
      aspect: 'landing',
      value: 'landing.files',
      revision: 7,
    });
  });

  it('offers Use the inherited value for an own value and applies the removal', async () => {
    const onApply = vi.fn();
    render(PlanSettingsModal, { props: { planId: 'fix-relay', settings, open: true, onApply } });

    expect(screen.queryByTestId('settings-inherit-landing')).toBeNull();
    const inherit = screen.getByTestId('settings-inherit-role.implement');
    expect(inherit).toHaveTextContent('Use the inherited value');

    await fireEvent.click(inherit);
    await fireEvent.click(screen.getByTestId('settings-save'));

    expect(onApply).toHaveBeenCalledWith({
      aspect: 'role.implement',
      value: null,
      revision: 7,
    });
  });

  it('applies the removal chosen through the select’s inherited option', async () => {
    const onApply = vi.fn();
    const ownValue: PlanSettingsRecord = {
      plan_id: 'fix-relay',
      revision: 7,
      values: [
        {
          aspect: 'role.implement',
          value: 'Claude Code - sonnet-5.5',
          origin: 'plan',
          choices: ['Claude Code - sonnet-5.5', 'Claude Code - opus-4.5'],
        },
      ],
    };
    render(PlanSettingsModal, {
      props: { planId: 'fix-relay', settings: ownValue, open: true, onApply },
    });

    await fireEvent.change(screen.getByTestId('settings-value-role.implement'), {
      target: { value: '' },
    });
    await fireEvent.click(screen.getByTestId('settings-save'));

    expect(onApply).toHaveBeenCalledWith({
      aspect: 'role.implement',
      value: null,
      revision: 7,
    });
  });

  it('says Saved when the runtime applied the change', async () => {
    const onApply = vi.fn().mockResolvedValue(true);
    render(PlanSettingsModal, { props: { planId: 'fix-relay', settings, open: true, onApply } });

    await fireEvent.change(screen.getByTestId('settings-value-landing'), {
      target: { value: 'landing.files' },
    });
    await fireEvent.click(screen.getByTestId('settings-save'));

    expect(await screen.findByText('Saved')).toBeInTheDocument();
  });

  it('says no Saved when the runtime refused the change', async () => {
    const onApply = vi.fn().mockResolvedValue(false);
    render(PlanSettingsModal, { props: { planId: 'fix-relay', settings, open: true, onApply } });

    await fireEvent.change(screen.getByTestId('settings-value-landing'), {
      target: { value: 'landing.files' },
    });
    await fireEvent.click(screen.getByTestId('settings-save'));

    await waitFor(() => expect(onApply).toHaveBeenCalled());
    expect(screen.queryByText('Saved')).toBeNull();
  });

  it('drops the discarded edit when the operator keeps the current value', async () => {
    const onKeepCurrent = vi.fn();
    render(PlanSettingsModal, {
      props: {
        planId: 'fix-relay',
        settings,
        open: true,
        conflict: {
          aspect: 'landing',
          currentValue: 'landing.main',
          wantedValue: 'landing.files',
          revision: 8,
        },
        onKeepCurrent,
      },
    });

    await fireEvent.change(screen.getByTestId('settings-value-landing'), {
      target: { value: 'landing.files' },
    });
    expect(screen.getByTestId('settings-save')).not.toBeDisabled();

    await fireEvent.click(screen.getByTestId('settings-conflict-keep'));

    expect(onKeepCurrent).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId('settings-save')).toBeDisabled();
  });

  it('does not keep an applied inherit pending', async () => {
    const onApply = vi.fn().mockResolvedValue(true);
    const { rerender } = render(PlanSettingsModal, {
      props: { planId: 'fix-relay', settings, open: true, onApply },
    });

    await fireEvent.click(screen.getByTestId('settings-inherit-role.implement'));
    await fireEvent.click(screen.getByTestId('settings-save'));
    await waitFor(() => expect(onApply).toHaveBeenCalled());

    await rerender({
      planId: 'fix-relay',
      open: true,
      settings: {
        ...settings,
        revision: 8,
        values: settings.values.map((entry) =>
          entry.aspect === 'role.implement'
            ? { ...entry, value: 'Claude Code - opus-4.5', origin: 'machine' }
            : entry,
        ),
      },
    });

    expect(screen.getByTestId('settings-save')).toBeDisabled();
  });

  it('calls onClose', async () => {
    const onClose = vi.fn();
    render(PlanSettingsModal, { props: { planId: 'fix-relay', settings, open: true, onClose } });

    await fireEvent.click(screen.getByTestId('settings-close-button'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('keeps exactly one Close control', () => {
    render(PlanSettingsModal, { props: { planId: 'fix-relay', settings, open: true } });

    expect(screen.getAllByRole('button', { name: /close/i })).toHaveLength(1);
  });

  it('draws the conflict’s acts as design-system buttons', () => {
    render(PlanSettingsModal, {
      props: {
        planId: 'fix-relay',
        settings,
        open: true,
        conflict: {
          aspect: 'landing',
          currentValue: 'landing.main',
          wantedValue: 'landing.files',
          revision: 8,
        },
      },
    });

    expect(screen.getByTestId('settings-conflict-keep')).toHaveClass('btn');
    expect(screen.getByTestId('settings-conflict-apply-again')).toHaveClass('btn');
  });

  it('labels a field and its options through the display layer', () => {
    render(PlanSettingsModal, { props: { planId: 'fix-relay', settings, open: true } });

    expect(screen.getByLabelText('Landing')).toHaveValue('landing.pull-request');
    expect(screen.queryByLabelText('landing')).toBeNull();
    expect(screen.getByRole('option', { name: 'Pull request' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'landing.pull-request' })).toBeNull();
    expect(screen.getByText('Role implement')).toBeInTheDocument();
  });

  it('shows a value another client changed first, with both choices', async () => {
    const onApplyAgain = vi.fn();
    const onKeepCurrent = vi.fn();
    render(PlanSettingsModal, {
      props: {
        planId: 'fix-relay',
        settings,
        open: true,
        conflict: {
          aspect: 'landing',
          currentValue: 'landing.main',
          wantedValue: 'landing.files',
          revision: 8,
        },
        onApplyAgain,
        onKeepCurrent,
      },
    });

    expect(screen.getByTestId('settings-conflict')).toHaveTextContent(
      'Changed meanwhile: it is now landing.main. Yours was not applied.',
    );
    expect(screen.getByTestId('settings-conflict-yours')).toHaveTextContent('landing.files');
    expect(screen.getByTestId('settings-conflict-current')).toHaveTextContent('landing.main');

    await fireEvent.click(screen.getByTestId('settings-conflict-apply-again'));
    expect(onApplyAgain).toHaveBeenCalledWith({
      aspect: 'landing',
      value: 'landing.files',
      revision: 8,
    });

    await fireEvent.click(screen.getByTestId('settings-conflict-keep'));
    expect(onKeepCurrent).toHaveBeenCalledTimes(1);
  });

  it('shows a refusal the runtime gave for the dialogue', () => {
    render(PlanSettingsModal, {
      props: {
        planId: 'fix-relay',
        settings,
        open: true,
        notice: 'landing.main is refused: the base branch is protected',
      },
    });

    expect(screen.getByTestId('settings-notice')).toHaveTextContent(
      'landing.main is refused: the base branch is protected',
    );
  });

  it('reseeds the fields when the runtime reports a newer revision', async () => {
    const { rerender } = render(PlanSettingsModal, {
      props: { planId: 'fix-relay', settings, open: true },
    });

    expect(screen.getByTestId('settings-value-landing')).toHaveValue('landing.pull-request');

    await rerender({
      planId: 'fix-relay',
      open: true,
      settings: {
        ...settings,
        revision: 8,
        values: settings.values.map((entry) =>
          entry.aspect === 'landing' ? { ...entry, value: 'landing.main' } : entry,
        ),
      },
    });

    expect(screen.getByTestId('settings-value-landing')).toHaveValue('landing.main');
    expect(screen.getByTestId('settings-save')).toBeDisabled();
  });
});
