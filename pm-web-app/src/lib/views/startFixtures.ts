import type {
  RecipeRef,
  SourceKindOffer,
  StartEntryResource,
  StartSource,
  StartState,
  StartStateResource,
} from '../api/client';

/** The project every start scenario uses. */
export const PROJECT = 'pm-mcp-clients';

/**
 * @param overrides the fields to replace
 * @returns a source of the fixture
 */
export function source(overrides: Partial<StartSource> = {}): StartSource {
  return {
    source_id: 'src-1',
    kind: 'description',
    reference: 'A description',
    state: 'stored',
    trust: 'trusted',
    spans: [],
    changed_files: [],
    ...overrides,
  };
}

/**
 * @param kind the entry's kind
 * @returns the offers the mock runtime computes for an empty list
 */
export function offersFor(kind: 'plan' | 'epic' = 'plan'): SourceKindOffer[] {
  const kinds =
    kind === 'epic'
      ? ['description', 'documents', 'issue']
      : ['description', 'documents', 'issue', 'lesson', 'local_changes', 'pull_request'];
  return kinds.map((offerKind) => ({ kind: offerKind, available: true, reason_args: {} }));
}

/** The recipes the mock runtime offers in `pm-mcp-clients`. */
export const RECIPES: RecipeRef[] = [
  {
    key: 'code-review',
    name: 'code-review',
    description: 'a structural and quality review of the current footprint',
    origin: 'domain',
    takes_request: false,
    parameters: [
      {
        name: 'depth',
        value: 'standard',
        options: ['quick', 'standard', 'deep'],
        proposed: true,
        available: true,
      },
    ],
  },
  {
    key: 'no-modules',
    name: 'no-modules',
    description: 'a recipe whose parameter has no option',
    origin: 'project',
    takes_request: false,
    parameters: [
      {
        name: 'modules',
        options: [],
        proposed: false,
        available: false,
        reason: 'none the recipe can work on',
      },
    ],
  },
];

/**
 * @param overrides the fields to replace
 * @returns an entry of the fixture
 */
export function entry(overrides: Partial<StartEntryResource> = {}): StartEntryResource {
  const kind = overrides.kind ?? 'plan';
  return {
    entry_id: 'ent-3f9a2c41',
    kind,
    project_id: PROJECT,
    state: 'start.sources',
    title: 'Start a plan in pm-mcp-clients',
    sources: [],
    kind_offers: offersFor(kind),
    can_continue: false,
    chat_available: false,
    chat_disabled_reason: 'start.chat.waits',
    ...overrides,
  };
}

/**
 * @param start the members of the start representation
 * @returns a representation as the runtime serves it
 */
export function representation(start: StartState = {}): StartStateResource {
  return {
    scope: { kind: 'workspace', project_id: PROJECT },
    state: { state_id: start.entry?.state ?? 'start.sources', kind: 'operator_wait' },
    links: [],
    start,
  };
}

/**
 * @param overrides the entry's fields to replace
 * @returns a representation of an entry in the sources step
 */
export function sources(overrides: Partial<StartEntryResource> = {}): StartStateResource {
  return representation({ entry: entry(overrides) });
}
