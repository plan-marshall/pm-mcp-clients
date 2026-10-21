import { describe, it, expect } from 'vitest';
import { SCENARIOS, scenario, scenarioFetch, ScenarioMockFetch } from './scenarioMockFetch';

describe('scenarioMockFetch', () => {
  it('reads every canonical scenario from the mock runtime resources', () => {
    for (const [name, file] of Object.entries(SCENARIOS)) {
      expect(file.name, name).toBe(name);
      expect(file.states[file.start], `${name} start state`).toBeDefined();
    }
  });

  it('serves a static route representation', async () => {
    const mock = scenarioFetch('reports-and-system');

    const response = await mock.fetch('http://localhost/api/v1/reports');
    const body = (await response.json()) as { unread_count: number; items: unknown[] };

    expect(response.status).toBe(200);
    expect(body.unread_count).toBe(7);
    expect(body.items).toHaveLength(7);
  });

  it('follows a state link to its next state and answers that representation', async () => {
    const mock = scenarioFetch('reports-and-system');
    expect(mock.currentState).toBe('reports.unread');

    const response = await mock.fetch('http://localhost/api/v1/reports/read', { method: 'POST' });
    const body = (await response.json()) as { state: { state_id: string } };

    expect(mock.currentState).toBe('reports.all-read');
    expect(body.state.state_id).toBe('reports.all-read');
  });

  it('refuses a request no link and no route answers with mock_no_route', async () => {
    const mock = new ScenarioMockFetch(scenario('designer'), 'designer.removed');

    const response = await mock.fetch('http://localhost/api/v1/unknown');
    const body = (await response.json()) as { error: string; state: string };

    expect(response.status).toBe(404);
    expect(body.error).toBe('mock_no_route');
    expect(body.state).toBe('designer.removed');
  });

  it('serves the designer catalogue of the machine scope from its route', async () => {
    const mock = scenarioFetch('designer');

    const response = await mock.fetch('http://localhost/api/v1/workflows/catalogue');
    const body = (await response.json()) as { scope: string; blocks: unknown[] };

    expect(body.scope).toBe('machine');
    expect(body.blocks.length).toBeGreaterThan(0);
  });

  it('returns to the scenario start state on reset', () => {
    const mock = scenarioFetch('reports-and-system', 'reports.empty');

    mock.reset();

    expect(mock.currentState).toBe('reports.unread');
  });
});
