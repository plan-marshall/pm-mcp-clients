/**
 * The delivered UI icons and the names that resolve to them.
 *
 * The iconography proposal VD-P-06 of `doc/claude_review/visual-design/proposals.adoc` is the
 * set's authority, drawn on the contract of `doc/design/web-app/workflow-visual-language.adoc`:
 * a 24 px grid, a 1.5 px stroke, round caps, and `currentColor` only. Every name has one SVG under
 * `ui/`, named by the name with dots replaced by hyphens. A view resolves a name here and never
 * builds an asset path of its own.
 */

import actBackUrl from './ui/act-back.svg?url';
import actCancelUrl from './ui/act-cancel.svg?url';
import actChatUrl from './ui/act-chat.svg?url';
import actCleanUrl from './ui/act-clean.svg?url';
import actCopyUrl from './ui/act-copy.svg?url';
import actEditUrl from './ui/act-edit.svg?url';
import actExternalUrl from './ui/act-external.svg?url';
import actFavouriteOnUrl from './ui/act-favourite-on.svg?url';
import actFavouriteUrl from './ui/act-favourite.svg?url';
import actMoreUrl from './ui/act-more.svg?url';
import actPauseUrl from './ui/act-pause.svg?url';
import actRefreshUrl from './ui/act-refresh.svg?url';
import actSearchUrl from './ui/act-search.svg?url';
import actStartUrl from './ui/act-start.svg?url';
import kindDecisionUrl from './ui/kind-decision.svg?url';
import kindDocumentUrl from './ui/kind-document.svg?url';
import kindEntityUrl from './ui/kind-entity.svg?url';
import kindEpicUrl from './ui/kind-epic.svg?url';
import kindHarnessUrl from './ui/kind-harness.svg?url';
import kindIssueUrl from './ui/kind-issue.svg?url';
import kindLessonUrl from './ui/kind-lesson.svg?url';
import kindPlanUrl from './ui/kind-plan.svg?url';
import kindPullRequestUrl from './ui/kind-pull-request.svg?url';
import kindReportUrl from './ui/kind-report.svg?url';
import kindTaskUrl from './ui/kind-task.svg?url';
import navDecisionsUrl from './ui/nav-decisions.svg?url';
import navDesignerUrl from './ui/nav-designer.svg?url';
import navInboxUrl from './ui/nav-inbox.svg?url';
import navLessonsUrl from './ui/nav-lessons.svg?url';
import navProjectsUrl from './ui/nav-projects.svg?url';
import navReportsUrl from './ui/nav-reports.svg?url';
import navRunningUrl from './ui/nav-running.svg?url';
import navSettingsUrl from './ui/nav-settings.svg?url';
import navSourcesUrl from './ui/nav-sources.svg?url';
import navSystemUrl from './ui/nav-system.svg?url';
import stateAlertUrl from './ui/state-alert.svg?url';
import stateCheckUrl from './ui/state-check.svg?url';
import stateClockUrl from './ui/state-clock.svg?url';
import stateDotUrl from './ui/state-dot.svg?url';
import stateErrorUrl from './ui/state-error.svg?url';
import stateLockUrl from './ui/state-lock.svg?url';

/**
 * Every delivered UI icon name.
 *
 * The families are the ones VD-P-06 groups: the navigation, the acts, the states, and the kinds of
 * thing the interface names. A view names one of these; it never takes an id from the runtime.
 */
export const UI_ICON_IDS = Object.freeze([
  'nav.inbox',
  'nav.running',
  'nav.projects',
  'nav.decisions',
  'nav.reports',
  'nav.system',
  'nav.designer',
  'nav.settings',
  'nav.lessons',
  'nav.sources',
  'act.start',
  'act.pause',
  'act.cancel',
  'act.chat',
  'act.search',
  'act.copy',
  'act.more',
  'act.back',
  'act.external',
  'act.edit',
  'act.refresh',
  'act.favourite',
  'act.favourite-on',
  'act.clean',
  'state.check',
  'state.dot',
  'state.alert',
  'state.clock',
  'state.lock',
  'state.error',
  'kind.plan',
  'kind.epic',
  'kind.decision',
  'kind.report',
  'kind.harness',
  'kind.document',
  'kind.issue',
  'kind.pull-request',
  'kind.lesson',
  'kind.task',
  'kind.entity',
] as const);

/** One delivered UI icon name. */
export type UiIconId = (typeof UI_ICON_IDS)[number];

/** The delivered asset URL of every name. */
const ICON_URLS: Readonly<Record<UiIconId, string>> = Object.freeze({
  'nav.inbox': navInboxUrl,
  'nav.running': navRunningUrl,
  'nav.projects': navProjectsUrl,
  'nav.decisions': navDecisionsUrl,
  'nav.reports': navReportsUrl,
  'nav.system': navSystemUrl,
  'nav.designer': navDesignerUrl,
  'nav.settings': navSettingsUrl,
  'nav.lessons': navLessonsUrl,
  'nav.sources': navSourcesUrl,
  'act.start': actStartUrl,
  'act.pause': actPauseUrl,
  'act.cancel': actCancelUrl,
  'act.chat': actChatUrl,
  'act.search': actSearchUrl,
  'act.copy': actCopyUrl,
  'act.more': actMoreUrl,
  'act.back': actBackUrl,
  'act.external': actExternalUrl,
  'act.edit': actEditUrl,
  'act.refresh': actRefreshUrl,
  'act.favourite': actFavouriteUrl,
  'act.favourite-on': actFavouriteOnUrl,
  'act.clean': actCleanUrl,
  'state.check': stateCheckUrl,
  'state.dot': stateDotUrl,
  'state.alert': stateAlertUrl,
  'state.clock': stateClockUrl,
  'state.lock': stateLockUrl,
  'state.error': stateErrorUrl,
  'kind.plan': kindPlanUrl,
  'kind.epic': kindEpicUrl,
  'kind.decision': kindDecisionUrl,
  'kind.report': kindReportUrl,
  'kind.harness': kindHarnessUrl,
  'kind.document': kindDocumentUrl,
  'kind.issue': kindIssueUrl,
  'kind.pull-request': kindPullRequestUrl,
  'kind.lesson': kindLessonUrl,
  'kind.task': kindTaskUrl,
  'kind.entity': kindEntityUrl,
});

/** @param name a candidate icon name @returns whether the set delivers it */
export function isUiIconId(name: string): name is UiIconId {
  return (UI_ICON_IDS as readonly string[]).includes(name);
}

/**
 * Resolves a name to its delivered asset.
 *
 * Unlike a runtime-supplied workflow id, a UI icon name is written by the view, so a name the set
 * does not deliver is a programming error rather than a future id. It answers `null` and the view
 * draws nothing, because there is no neutral member of this set to fall back to.
 *
 * @param name the icon name
 * @returns the asset URL, or `null` for a name the set does not deliver
 */
export function uiIconUrl(name: string | null | undefined): string | null {
  const key = (name ?? '').trim();
  return isUiIconId(key) ? ICON_URLS[key] : null;
}
