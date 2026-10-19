import type { SourceKindOffer, StartSource, StartSourceFailure } from '../api/client';

/**
 * The parts of an issue or pull-request reference the client shows before the runtime fetches it.
 */
export interface ForgeReference {
  /** The forge's display name, e.g. `Codeberg`. */
  forge: string;
  /** The repository the reference names, e.g. `forgejo/forgejo`. */
  repository: string;
  /** The number the reference names. */
  number: string;
  /** The URL of the reference, which is what the runtime stores. */
  url: string;
}

/** The forges this machine knows, and how their references are read. */
const FORGES: ReadonlyArray<{ host: RegExp; forge: string }> = [
  { host: /(^|\.)github\.com$/i, forge: 'GitHub' },
  { host: /(^|\.)gitlab\.com$/i, forge: 'GitLab' },
  { host: /(^|\.)codeberg\.org$/i, forge: 'Codeberg' },
];

/**
 * Reads the forge and repository of an issue or pull-request URL.
 *
 * @param reference a URL, or a bare number when `repository` is given
 * @param repository the project's own repository, for a bare number
 * @returns the reference's parts, or `null` where it names none
 */
export function readForgeReference(reference: string, repository = ''): ForgeReference | null {
  const trimmed = reference.trim();
  if (!trimmed) {
    return null;
  }
  const bare = /^#?(\d+)$/.exec(trimmed);
  if (bare?.[1]) {
    if (!repository) {
      return null;
    }
    return {
      forge: 'GitHub',
      repository,
      number: bare[1],
      url: `https://github.com/${repository}/issues/${bare[1]}`,
    };
  }
  let url: URL;
  try {
    url = new URL(trimmed);
  } catch {
    return null;
  }
  if (url.protocol !== 'https:' && url.protocol !== 'http:') {
    return null;
  }
  const known = FORGES.find((candidate) => candidate.host.test(url.hostname));
  if (!known) {
    return null;
  }
  const parts = url.pathname.split('/').filter(Boolean);
  const marker = parts.findIndex((part) => part === 'issues' || part === 'pull');
  const number = /^\d+$/.exec((marker >= 0 ? parts[marker + 1] : parts[2]) ?? '');
  if (parts.length < 3 || !number?.[0]) {
    return null;
  }
  return {
    forge: known.forge,
    repository: `${parts[0]}/${parts[1]}`,
    number: number[0],
    url: trimmed,
  };
}

/**
 * @param reference the reference the operator typed
 * @param projectRepository the project's own repository
 * @returns the repository a reference names, or `''` where it names none
 */
export function repositoryOf(reference: string, projectRepository = ''): string {
  return readForgeReference(reference, projectRepository)?.repository ?? '';
}

/**
 * The message key and arguments a failed source shows.
 *
 * @param failure the failure
 * @returns the catalogue key and its arguments
 */
export function failureMessage(failure: StartSourceFailure): {
  key: string;
  args: Record<string, string | number>;
} {
  if (failure.cause === 'rate_limited') {
    return {
      key: 'start.cause.rate_limited',
      args: {
        time: resetTime(failure.resets_at),
        credential: failure.detail ? 'present' : 'none',
        forge: failure.detail ?? '',
      },
    };
  }
  if (failure.cause === 'no_credential') {
    return {
      key: 'start.cause.no_credential',
      args: { forge: failure.detail ?? '' },
    };
  }
  if (failure.cause === 'screening_failed') {
    return {
      key: 'start.cause.screening_failed',
      args: { cause: failure.detail ?? '' },
    };
  }
  if (failure.cause === 'too_large') {
    return {
      key: 'start.cause.too_large',
      args: { size: failure.detail ?? '' },
    };
  }
  return { key: `start.cause.${failure.cause}`, args: {} };
}

/**
 * @param resetsAt the epoch second a rate limit resets, if any
 * @returns the time of day the client shows, or its own clock where none is named
 */
export function resetTime(resetsAt?: number | null): string {
  if (!resetsAt) {
    return '';
  }
  return new Date(resetsAt * 1000).toLocaleTimeString(undefined, {
    hour: '2-digit',
    minute: '2-digit',
  });
}

/**
 * @param source the source
 * @returns the text of a settled source's state, or the empty string for another state
 */
export function settledLabel(source: StartSource): string {
  if (source.kind === 'local_changes' && source.state === 'stored') {
    return '';
  }
  return source.state;
}

/**
 * The reason an offer is refused, translated from the catalogue.
 *
 * @param offer the offer
 * @param translate the catalogue's translation function
 * @returns the translated reason, or the empty string for an available offer
 */
export function offerReason(
  offer: SourceKindOffer,
  translate: (key: string, params?: Record<string, string | number>) => string,
): string {
  if (!offer.reason_key) {
    return '';
  }
  return translate(offer.reason_key, offer.reason_args ?? {});
}
