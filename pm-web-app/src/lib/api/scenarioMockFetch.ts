import designerScenario from '../../../../pm-mock-runtime/src/main/resources/scenarios/designer.json';
import fullyOnboardedScenario from '../../../../pm-mock-runtime/src/main/resources/scenarios/fully-onboarded.json';
import onboardingScenario from '../../../../pm-mock-runtime/src/main/resources/scenarios/onboarding.json';
import projectsAndStartScenario from '../../../../pm-mock-runtime/src/main/resources/scenarios/projects-and-start.json';
import reportsAndSystemScenario from '../../../../pm-mock-runtime/src/main/resources/scenarios/reports-and-system.json';
import runningWorkScenario from '../../../../pm-mock-runtime/src/main/resources/scenarios/running-work.json';

/** One link relation of a scenario state, as `05-mock-runtime.adoc#scenario-files` defines it. */
export interface ScenarioLink {
  /** The method that follows the link; `GET` when the file omits it. */
  method?: string;
  /** The path of the link, relative to the local API. */
  href?: string;
  /** The state the link leads to. */
  next: string;
  /** The status the scenario answers with. */
  status?: number;
  /** The `pm-api` error code a refusal carries. */
  error?: string;
  /** The delay before the answer; the helper answers at once so a test stays deterministic. */
  delay_ms?: number;
}

/** One timed event of a scenario state. */
export interface ScenarioEvent {
  after_ms: number;
  event: string;
  data: Record<string, unknown>;
}

/** One state of a scenario file. */
export interface ScenarioState {
  representation: Record<string, unknown>;
  links?: Record<string, ScenarioLink>;
  events?: ScenarioEvent[];
  auto?: { after_ms: number; next: string };
}

/** One static route of a scenario file. */
export interface ScenarioRoute {
  status?: number;
  representation: unknown;
  headers?: Record<string, string>;
}

/** A scenario file as `pm-mock-runtime` ships it. */
export interface ScenarioFile {
  format_version: number;
  name: string;
  description?: string;
  start: string;
  pairing_code?: string;
  routes: Record<string, ScenarioRoute>;
  states: Record<string, ScenarioState>;
}

/** The canonical scenarios the mock runtime and the web app share. */
export type ScenarioName =
  | 'onboarding'
  | 'running-work'
  | 'projects-and-start'
  | 'reports-and-system'
  | 'designer'
  | 'fully-onboarded';

/** Every canonical scenario, read from the mock runtime's own resources. */
export const SCENARIOS: Record<ScenarioName, ScenarioFile> = {
  onboarding: onboardingScenario as unknown as ScenarioFile,
  'running-work': runningWorkScenario as unknown as ScenarioFile,
  'projects-and-start': projectsAndStartScenario as unknown as ScenarioFile,
  'reports-and-system': reportsAndSystemScenario as unknown as ScenarioFile,
  designer: designerScenario as unknown as ScenarioFile,
  'fully-onboarded': fullyOnboardedScenario as unknown as ScenarioFile,
};

/**
 * @param name the canonical scenario's name
 * @returns the scenario file the mock runtime serves
 */
export function scenario(name: ScenarioName): ScenarioFile {
  return SCENARIOS[name];
}

/**
 * Answers `fetch` from a scenario file with the routing rules of
 * `05-mock-runtime.adoc#scenario-files`: a request that matches a link of the current state moves
 * to that link's next state and answers with its representation, a request that matches a static
 * route answers with the route's representation, and any other request is refused `404` with
 * `mock_no_route`, naming the state and the links it offers.
 */
export class ScenarioMockFetch {
  private state: string;

  /**
   * @param file the scenario to serve
   * @param state the state to start in, or the scenario's own start state
   */
  constructor(
    readonly file: ScenarioFile,
    state?: string,
  ) {
    this.state = state ?? file.start;
    this.requireState(this.state);
  }

  /** @returns the id of the state the helper is in */
  get currentState(): string {
    return this.state;
  }

  /** @returns the state the helper is in */
  get current(): ScenarioState {
    return this.file.states[this.state];
  }

  /** @returns the events of the current state, in the order the file declares them */
  get events(): ScenarioEvent[] {
    return this.current.events ?? [];
  }

  /**
   * @param stateId the state to move to
   */
  goto(stateId: string): void {
    this.requireState(stateId);
    this.state = stateId;
  }

  /** Moves back to the scenario's start state. */
  reset(): void {
    this.state = this.file.start;
  }

  /** A `fetch`-compatible function bound to this helper. */
  readonly fetch = async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
    const path = new URL(url, 'http://localhost').pathname;
    const method = (init?.method ?? 'GET').toUpperCase();

    const link = this.matchLink(method, path);
    if (link) {
      if (link.status !== undefined && link.status >= 400) {
        return json(
          {
            error: link.error ?? 'mock_refused',
            message: `The scenario refuses ${method} ${path}`,
            state: this.state,
          },
          link.status,
        );
      }
      this.goto(link.next);
      return json(this.current.representation, link.status ?? 200);
    }

    const route = this.file.routes[`${method} ${path}`];
    if (route) {
      return json(route.representation, route.status ?? 200, route.headers);
    }

    return json(
      {
        error: 'mock_no_route',
        message: `No link of the state ${this.state} and no route answers ${method} ${path}`,
        state: this.state,
        links: Object.keys(this.current.links ?? {}),
      },
      404,
    );
  };

  private matchLink(method: string, path: string): ScenarioLink | undefined {
    return Object.values(this.current.links ?? {}).find(
      (link) =>
        (link.method ?? 'GET').toUpperCase() === method &&
        link.href !== undefined &&
        new URL(link.href, 'http://localhost').pathname === path,
    );
  }

  private requireState(stateId: string): void {
    if (!this.file.states[stateId]) {
      throw new Error(`The scenario ${this.file.name} has no state ${stateId}`);
    }
  }
}

/**
 * @param name the canonical scenario's name
 * @param state the state to start in, or the scenario's start state
 * @returns a `fetch`-compatible helper over that scenario
 */
export function scenarioFetch(name: ScenarioName, state?: string): ScenarioMockFetch {
  return new ScenarioMockFetch(scenario(name), state);
}

function json(body: unknown, status: number, headers: Record<string, string> = {}): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json', ...headers },
  });
}
