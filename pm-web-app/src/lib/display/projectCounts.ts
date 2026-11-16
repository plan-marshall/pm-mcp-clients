import type { ProjectSummary } from '../api/client';
import type { Translate } from './index';

/**
 * The one sentence a project's running work is named with (`overview.project.*`).
 *
 * The overview and the start page both name the work of a project, and the design requires them to
 * name it alike (`plan-overview.adoc`, A Project; `needs-and-find.adoc`, The List in the Frame), so
 * the sentence lives here and the two views call it — a fourth case or a renamed key is changed once.
 *
 * @param project the project whose work is named
 * @param translate the catalogue look-up (`$t`) a template passes, so the sentence follows a
 *   locale switch
 * @returns the sentence, or an empty string for a project that runs nothing
 */
export function projectCountsText(project: ProjectSummary, translate: Translate): string {
  const plans = Number(project.active_plans_count ?? 0);
  const epics = Number(project.active_epics_count ?? 0);
  if (project.setup_needs_operator && plans === 0) {
    return translate('overview.project.setup');
  }
  if (plans > 0 && epics > 0) {
    return translate('overview.project.counts', { plans, epics });
  }
  if (plans > 0) {
    return translate('overview.project.plans_only', { plans });
  }
  if (epics > 0) {
    return translate('overview.project.epics_only', { epics });
  }
  return '';
}
