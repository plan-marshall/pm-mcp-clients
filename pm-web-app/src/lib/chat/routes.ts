import type { ChatTarget } from './types';

export interface ResolvedChatRoutes {
  subjectPath: string;
  chatPath: string;
  pagePath: (beforeTurnId: string) => string;
  turnsPath: string;
  stopPath: (turnId: string) => string;
  acceptPath: string;
  rejectPath: string;
  endPath: string;
  streamPath: string;
}

function resolveSubjectAndStream(target: ChatTarget): {
  subjectPath: string;
  streamPath: string;
} {
  switch (target.kind) {
    case 'plan': {
      const base = `/api/v1/plans/${encodeURIComponent(target.id)}`;
      return { subjectPath: base, streamPath: `${base}/events` };
    }
    case 'epic': {
      const base = `/api/v1/epics/${encodeURIComponent(target.id)}`;
      return { subjectPath: base, streamPath: `${base}/events` };
    }
    case 'entry': {
      const base = `/api/v1/entries/${encodeURIComponent(target.id)}`;
      return { subjectPath: base, streamPath: `${base}/events` };
    }
    case 'decision': {
      const qid = encodeURIComponent(target.id);
      if (!target.scope) {
        return {
          subjectPath: `/api/v1/decisions/${qid}`,
          streamPath: '/api/v1/events',
        };
      }
      switch (target.scope.kind) {
        case 'plan': {
          const scopeBase = `/api/v1/plans/${encodeURIComponent(target.scope.id)}`;
          return {
            subjectPath: `${scopeBase}/decisions/${qid}`,
            streamPath: `${scopeBase}/events`,
          };
        }
        case 'epic': {
          const scopeBase = `/api/v1/epics/${encodeURIComponent(target.scope.id)}`;
          return {
            subjectPath: `${scopeBase}/decisions/${qid}`,
            streamPath: `${scopeBase}/events`,
          };
        }
        case 'entry': {
          const scopeBase = `/api/v1/entries/${encodeURIComponent(target.scope.id)}`;
          return {
            subjectPath: `${scopeBase}/decisions/${qid}`,
            streamPath: `${scopeBase}/events`,
          };
        }
        case 'workspace': {
          const scopeBase = `/api/v1/projects/${encodeURIComponent(target.scope.projectId)}/workspace`;
          return {
            subjectPath: `${scopeBase}/decisions/${qid}`,
            streamPath: `${scopeBase}/events`,
          };
        }
        case 'machine':
          return {
            subjectPath: `/api/v1/machine/decisions/${qid}`,
            streamPath: '/api/v1/machine/events',
          };
      }
    }
  }
}

/**
 * Resolves the B1 `ChatRoutes` endpoints and subject SSE stream path for a target.
 *
 * @param target the subject whose chat is addressed
 * @returns resolved HTTP paths and stream path
 */
export function chatRoutesFor(target: ChatTarget): ResolvedChatRoutes {
  const { subjectPath, streamPath } = resolveSubjectAndStream(target);
  const chatPath = `${subjectPath}/chat`;
  const turnsPath = `${chatPath}/turns`;
  return {
    subjectPath,
    chatPath,
    pagePath: (beforeTurnId: string) => `${chatPath}?before=${encodeURIComponent(beforeTurnId)}`,
    turnsPath,
    stopPath: (turnId: string) => `${turnsPath}/${encodeURIComponent(turnId)}/stop`,
    acceptPath: `${chatPath}/proposal/accept`,
    rejectPath: `${chatPath}/proposal/reject`,
    endPath: `${chatPath}/end`,
    streamPath,
  };
}

/**
 * @param target the chat target
 * @returns stable `localStorage` key for holding unsent drafts for that subject's chat
 */
export function storageKeyForDraft(target: ChatTarget): string {
  if (target.kind === 'decision' && target.scope) {
    const scopePart =
      target.scope.kind === 'machine'
        ? 'machine'
        : target.scope.kind === 'workspace'
          ? `workspace:${target.scope.projectId}`
          : `${target.scope.kind}:${target.scope.id}`;
    return `pm_chat_draft:decision:${scopePart}:${target.id}`;
  }
  return `pm_chat_draft:${target.kind}:${target.id}`;
}
