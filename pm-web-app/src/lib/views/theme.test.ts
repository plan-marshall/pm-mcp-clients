import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import fs from 'node:fs';
import path from 'node:path';
import EpicView from './EpicView.svelte';
import OverviewView from './OverviewView.svelte';
import FindingsReviewModal from '../components/FindingsReviewModal.svelte';
import {
  apiClient,
  type EpicProcess,
  type EpicResource,
  type ReviewResource,
  type ProjectOverviewResponse,
  type ProjectDetailResponse,
} from '../api/client';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';
import { contrastRatio } from '../../test/colourMetrics';
import { parseThemes, tokenOf } from '../test/stylesheet';

const mockEpic: EpicProcess = {
  epic_id: 'relay-hardening',
  project: 'pm-mcp-clients',
  title: 'relay-hardening',
  state: 'running',
  review_ready: true,
  running_plans: [],
  staged_plans: [],
  alongside_landings: [],
  tokens_so_far: {
    total: 3860000,
    input: 3520000,
    output: 340000,
    floor: false,
    tasks_total: 16,
    tasks_measured: 16,
    none_yet: false,
    not_measured: false,
  },
};

const mockEpicResource: EpicResource = {
  epic_id: 'relay-hardening',
  project_id: 'pm-mcp-clients',
  display_name: 'Relay Hardening',
  state: 'running',
  auto_start: true,
  overlap_max_files: 3,
  scope: 1,
  running: 1,
  workstreams: [
    {
      id: 'WS-1',
      name: 'Transport Core',
      open: true,
      rows: [
        {
          plan_id: 'relay-cursor-order',
          title: 'relay-cursor-order',
          status: 'running',
          wait_reason: 'none',
        },
      ],
    },
  ],
  sources: ['2 documents'],
  landings: 2,
  findings: { new: 3, open: 4, kept: 2 },
  watches: { new: 1, open: 4, kept: 0 },
  tokens: {
    total: {
      total: 3860000,
      input: 3520000,
      output: 340000,
      measured: true,
      floor: true,
      measured_tasks: 4,
      not_measured_tasks: 1,
    },
    own: {
      total: 120000,
      input: 110000,
      output: 10000,
      measured: true,
      floor: false,
      measured_tasks: 1,
      not_measured_tasks: 0,
    },
    plans: [],
    not_measured: false,
    not_measured_count: 0,
  },
  events: [],
};

const mockFindingsReview: ReviewResource = {
  review_id: 'rev-01',
  epic_id: 'relay-hardening',
  register: 'findings',
  decided: 0,
  total: 1,
  state: 'open',
  items: [
    {
      item_id: 'F-0004',
      register: 'findings',
      statement: 'Reconnect loses the last event',
      rules: ['no-event-loss'],
      origin: 'review',
      trust: 'high',
      state: 'open',
    },
  ],
};

const mockProjects: ProjectOverviewResponse = {
  projects: [
    {
      project_id: 'pm-mcp-clients',
      display_name: 'pm-mcp-clients',
      active_plans_count: 1,
      active_epics_count: 1,
      needs_operator_count: 2,
      needs_operator_summary: 'decision',
      setup_needs_operator: false,
      last_activity_at: '2026-10-16T15:36:00Z',
    },
  ],
};

const mockDetail: ProjectDetailResponse = {
  project_id: 'pm-mcp-clients',
  display_name: 'pm-mcp-clients',
  alongside_landings: [],
  plans: [
    {
      plan_id: 'fix-relay',
      title: 'Fix Relay Transport',
      current_phase: 'execute',
      phase_state: 'running',
      tasks_done: 4,
      tasks_total: 10,
      needs_operator_kind: 'decision',
      needs_operator_count: 2,
    },
  ],
  epics: [
    {
      epic_id: 'relay-hardening',
      title: 'Relay Hardening',
      running_plans_count: 1,
      staged_plans_count: 1,
      plans_done: 0,
      plans_total: 2,
      plans: [],
    },
  ],
};

describe('Theme Component Readability Tests (Light & Dark)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    runtimeStore.set(
      makeRuntimeState({
        isInitialized: true,
        isPaired: true,
        isOnboarded: true,
        deviceLabel: 'test-device',
        machineName: 'test-mac',
        selectedEpicId: 'relay-hardening',
      }),
    );
    vi.spyOn(apiClient, 'getEpicProcess').mockResolvedValue(mockEpic);
    vi.spyOn(apiClient, 'getEpicResource').mockResolvedValue(mockEpicResource);
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'getProjects').mockResolvedValue(mockProjects);
    vi.spyOn(apiClient, 'getProjectDetail').mockResolvedValue(mockDetail);
    vi.spyOn(apiClient, 'getEndedWork').mockResolvedValue({
      project_id: 'pm-mcp-clients',
      page: 1,
      total_pages: 1,
      has_newer: false,
      has_older: false,
      items: [],
    });
  });

  beforeAll(() => {
    const tokensCss = fs.readFileSync(path.resolve(__dirname, '../../styles/tokens.css'), 'utf8');
    const baseCss = fs.readFileSync(path.resolve(__dirname, '../../styles/base.css'), 'utf8');
    const epicView = fs.readFileSync(path.resolve(__dirname, './EpicView.svelte'), 'utf8');
    const epicStyleMatch = epicView.match(/<style>([\s\S]*?)<\/style>/);
    const epicStyle = epicStyleMatch ? epicStyleMatch[1] : '';

    const styleEl = document.createElement('style');
    styleEl.textContent = `${tokensCss}\n${baseCss.replace(/@import[^;]+;/g, '')}\n${epicStyle}`;
    document.head.appendChild(styleEl);
  });

  for (const theme of ['light', 'dark'] as const) {
    describe(`under data-theme="${theme}"`, () => {
      beforeEach(() => {
        document.documentElement.setAttribute('data-theme', theme);
      });

      it('renders the epic findings action button ("Prüfen" / Review) with the primary action role', async () => {
        render(EpicView);

        // The findings and watches reviews live on the Reviews tab of the epic's object column.
        await fireEvent.click(await screen.findByRole('tab', { name: /Reviews|Prüfungen/ }));

        const reviewBtn = await screen.findByTestId('open-findings-review');
        expect(reviewBtn).toBeInTheDocument();
        expect(reviewBtn.tagName).toBe('BUTTON');
        expect(reviewBtn).toHaveClass('review-open');

        // Review button must not carry inline black or un-themed colors
        const style = window.getComputedStyle(reviewBtn);
        expect(style.display).not.toBe('none');
        expect(style.visibility).not.toBe('hidden');

        // Verify computed foreground uses the primary action token rather than un-themed black buttontext
        expect(style.color).toBe('var(--color-primary)');

        // Contrast verification: read the real pair (--color-primary -> --color-brand on --color-bg-surface)
        // from tokens.css in the active theme palette (and system-dark for dark).
        const tokensCss = fs.readFileSync(
          path.resolve(__dirname, '../../styles/tokens.css'),
          'utf8',
        );
        const themes = parseThemes(tokensCss);
        expect(tokenOf(themes.root, '--color-primary')).toBe('var(--color-brand)');

        const palettes =
          theme === 'dark' ? [themes.dark, themes.mediaDark] : [themes.root, themes.light];
        for (const palette of palettes) {
          const contrast = contrastRatio(
            tokenOf(palette, '--color-brand'),
            tokenOf(palette, '--color-bg-surface'),
          );
          expect(contrast).toBeGreaterThanOrEqual(4.5);
        }

        // Verify the watches review button as well
        const watchesBtn = screen.getByTestId('open-watches-review');
        expect(watchesBtn).toBeInTheDocument();
        expect(watchesBtn).toHaveClass('review-open');
      });

      it('renders attention and epic badges in OverviewView with theme roles', async () => {
        render(OverviewView);

        const attentionBadge = await screen.findByText('! 2');
        expect(attentionBadge).toBeInTheDocument();
        expect(attentionBadge).toHaveClass('badge--attention');

        const attentionStyle = window.getComputedStyle(attentionBadge);
        expect(attentionStyle.display).not.toBe('none');
      });

      it('renders the review as the design dialog of the theme', () => {
        render(FindingsReviewModal, {
          props: {
            epicId: 'relay-hardening',
            open: true,
            review: mockFindingsReview,
            onClose: vi.fn(),
          },
        });

        const modal = screen.getByTestId('review-modal');

        expect(modal).toHaveClass('dialog');
        expect(modal.closest('.dialog-backdrop')).not.toBeNull();
      });
    });
  }
});
