import type { SourceKindOffer, StartSource, StartSourceFailure } from '../api/client';
import { formatTime, type Locale } from '../display/format';

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
 * @param forge the forge's display name, for a bare number
 * @param baseUrl the forge's base URL, for a bare number
 * @returns the reference's parts, or `null` where it names none
 */
export function readForgeReference(
  reference: string,
  repository = '',
  forge = 'GitHub',
  baseUrl = 'https://github.com',
): ForgeReference | null {
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
      forge,
      repository,
      number: bare[1],
      url: `${baseUrl}/${repository}/issues/${bare[1]}`,
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

/** The forge and repository a git remote names. */
export interface RemoteInfo {
  forge: string;
  repository: string;
  baseUrl: string;
}

/**
 * Reads the forge of a remote URL.
 *
 * @param remote the remote URL
 * @returns the forge and repository, or `null` where the remote names none
 */
export function remoteInfo(remote: string): RemoteInfo | null {
  const trimmed = remote.trim();
  if (!trimmed) {
    return null;
  }
  const scp = /^[^@\s]+@([^:\s]+):([^/\s]+)\/([^/\s]+?)(?:\.git)?$/.exec(trimmed);
  if (scp?.[1] && scp[2] && scp[3]) {
    const forge = FORGES.find((candidate) => candidate.host.test(scp[1]));
    if (!forge) {
      return null;
    }
    return {
      forge: forge.forge,
      repository: `${scp[2]}/${scp[3]}`,
      baseUrl: `https://${scp[1]}`,
    };
  }
  try {
    const url = new URL(trimmed);
    const parts = url.pathname.split('/').filter(Boolean);
    if (parts.length < 2) {
      return null;
    }
    const known = FORGES.find((candidate) => candidate.host.test(url.hostname));
    if (!known) {
      return null;
    }
    const repository = `${parts[0]}/${parts[1]}`;
    return {
      forge: known.forge,
      repository: repository.endsWith('.git') ? repository.slice(0, -4) : repository,
      baseUrl: `${url.protocol}//${url.host}`,
    };
  } catch {
    return null;
  }
}

/**
 * Reads the `owner/repo` of a git remote, whichever transport it uses.
 *
 * @param remote the remote URL, e.g. `git@github.com:owner/repo.git`
 * @returns the repository, or the empty string where the remote names none
 */
export function repositoryFromRemote(remote: string): string {
  return remoteInfo(remote)?.repository ?? '';
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
export function failureMessage(
  failure: StartSourceFailure,
  locale: Locale = 'en',
): {
  key: string;
  args: Record<string, string | number>;
} {
  if (failure.cause === 'rate_limited') {
    return {
      key: 'start.cause.rate_limited',
      args: {
        time: resetTime(failure.resets_at, locale),
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
 * @param locale the interface locale
 * @returns the moment it resets, in the one time format, or an empty string where none is named
 */
export function resetTime(resetsAt?: number | null, locale: Locale = 'en'): string {
  if (!resetsAt) {
    return '';
  }
  const moment = new Date(resetsAt * 1000);
  if (Number.isNaN(moment.getTime())) {
    // An out-of-range epoch second is not an instant: nothing is shown rather than a thrown error.
    return '';
  }
  return formatTime(moment.toISOString(), 'short', locale);
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
