/**
 * The delivered workflow icons and the ids that resolve to them.
 *
 * The visual specification in `doc/design/web-app/workflow-visual-language.adoc` is the
 * catalogue's authority; this module is its executable mirror. Every id has one SVG under
 * `workflows/`, named by the id with dots replaced by hyphens. A view resolves an id here and
 * never builds an asset path of its own.
 */

import axisAskEveryUrl from './workflows/axis-ask-every.svg?url';
import axisAskKeyUrl from './workflows/axis-ask-key.svg?url';
import axisAskQuestionsUrl from './workflows/axis-ask-questions.svg?url';
import axisLandingFilesUrl from './workflows/axis-landing-files.svg?url';
import axisLandingMainUrl from './workflows/axis-landing-main.svg?url';
import axisLandingPrUrl from './workflows/axis-landing-pr.svg?url';
import axisMethodTestFirstUrl from './workflows/axis-method-test-first.svg?url';
import axisMethodTestsAfterUrl from './workflows/axis-method-tests-after.svg?url';
import flowAdjudicateUrl from './workflows/flow-adjudicate.svg?url';
import flowLandUrl from './workflows/flow-land.svg?url';
import flowReplanUrl from './workflows/flow-replan.svg?url';
import gitCommitUrl from './workflows/git-commit.svg?url';
import phaseExecuteUrl from './workflows/phase-execute.svg?url';
import phaseFinalizeUrl from './workflows/phase-finalize.svg?url';
import phaseInitUrl from './workflows/phase-init.svg?url';
import phaseOutlineUrl from './workflows/phase-outline.svg?url';
import phasePlanUrl from './workflows/phase-plan.svg?url';
import phaseRefineUrl from './workflows/phase-refine.svg?url';
import recipeDefaultUrl from './workflows/recipe-default.svg?url';
import remediationAnalysisUrl from './workflows/remediation-analysis.svg?url';
import remediationBuildUrl from './workflows/remediation-build.svg?url';
import remediationCiUrl from './workflows/remediation-ci.svg?url';
import remediationMergeUrl from './workflows/remediation-merge.svg?url';
import remediationQgateUrl from './workflows/remediation-qgate.svg?url';
import remediationReviewUrl from './workflows/remediation-review.svg?url';
import unitChatUrl from './workflows/unit-chat.svg?url';
import unitDecisionUrl from './workflows/unit-decision.svg?url';
import unitDecompositionUrl from './workflows/unit-decomposition.svg?url';
import unitEntityUrl from './workflows/unit-entity.svg?url';
import unitEpicUrl from './workflows/unit-epic.svg?url';
import unitLandingUrl from './workflows/unit-landing.svg?url';
import unitLessonUrl from './workflows/unit-lesson.svg?url';
import unitOnboardingUrl from './workflows/unit-onboarding.svg?url';
import unitPlanUrl from './workflows/unit-plan.svg?url';
import unitReviewUrl from './workflows/unit-review.svg?url';
import unitSetupUrl from './workflows/unit-setup.svg?url';
import unitSourceUrl from './workflows/unit-source.svg?url';
import unitStartUrl from './workflows/unit-start.svg?url';
import unitTaskUrl from './workflows/unit-task.svg?url';
import workflowDefaultUrl from './workflows/workflow-default.svg?url';

/** Every delivered icon id. The catalogue table in the visual specification must match it. */
export const WORKFLOW_ICON_IDS = Object.freeze([
  'phase.init',
  'phase.refine',
  'phase.outline',
  'phase.plan',
  'phase.execute',
  'phase.finalize',
  'flow.land',
  'flow.replan',
  'flow.adjudicate',
  'git.commit',
  'remediation.ci',
  'remediation.review',
  'remediation.analysis',
  'remediation.build',
  'remediation.merge',
  'remediation.qgate',
  'axis.landing.files',
  'axis.landing.pr',
  'axis.landing.main',
  'axis.method.test-first',
  'axis.method.tests-after',
  'axis.ask.every',
  'axis.ask.key',
  'axis.ask.questions',
  'unit.plan',
  'unit.epic',
  'unit.decomposition',
  'unit.review',
  'unit.decision',
  'unit.chat',
  'unit.onboarding',
  'unit.setup',
  'unit.start',
  'unit.source',
  'unit.task',
  'unit.entity',
  'unit.landing',
  'unit.lesson',
  'recipe.default',
  'workflow.default',
] as const);

/** One delivered icon id. */
export type WorkflowIconId = (typeof WORKFLOW_ICON_IDS)[number];

/** The delivered asset URL of every id. */
const ICON_URLS: Readonly<Record<WorkflowIconId, string>> = Object.freeze({
  'phase.init': phaseInitUrl,
  'phase.refine': phaseRefineUrl,
  'phase.outline': phaseOutlineUrl,
  'phase.plan': phasePlanUrl,
  'phase.execute': phaseExecuteUrl,
  'phase.finalize': phaseFinalizeUrl,
  'flow.land': flowLandUrl,
  'flow.replan': flowReplanUrl,
  'flow.adjudicate': flowAdjudicateUrl,
  'git.commit': gitCommitUrl,
  'remediation.ci': remediationCiUrl,
  'remediation.review': remediationReviewUrl,
  'remediation.analysis': remediationAnalysisUrl,
  'remediation.build': remediationBuildUrl,
  'remediation.merge': remediationMergeUrl,
  'remediation.qgate': remediationQgateUrl,
  'axis.landing.files': axisLandingFilesUrl,
  'axis.landing.pr': axisLandingPrUrl,
  'axis.landing.main': axisLandingMainUrl,
  'axis.method.test-first': axisMethodTestFirstUrl,
  'axis.method.tests-after': axisMethodTestsAfterUrl,
  'axis.ask.every': axisAskEveryUrl,
  'axis.ask.key': axisAskKeyUrl,
  'axis.ask.questions': axisAskQuestionsUrl,
  'unit.plan': unitPlanUrl,
  'unit.epic': unitEpicUrl,
  'unit.decomposition': unitDecompositionUrl,
  'unit.review': unitReviewUrl,
  'unit.decision': unitDecisionUrl,
  'unit.chat': unitChatUrl,
  'unit.onboarding': unitOnboardingUrl,
  'unit.setup': unitSetupUrl,
  'unit.start': unitStartUrl,
  'unit.source': unitSourceUrl,
  'unit.task': unitTaskUrl,
  'unit.entity': unitEntityUrl,
  'unit.landing': unitLandingUrl,
  'unit.lesson': unitLessonUrl,
  'recipe.default': recipeDefaultUrl,
  'workflow.default': workflowDefaultUrl,
});

/** The bare phase token to icon id. */
const PHASE_TOKENS: Readonly<Record<string, WorkflowIconId>> = Object.freeze({
  init: 'phase.init',
  refine: 'phase.refine',
  outline: 'phase.outline',
  plan: 'phase.plan',
  execute: 'phase.execute',
  finalize: 'phase.finalize',
});

/** A phase-unit id (`phase-5-execute`) and a bare token both resolve through here. */
const PHASE_ICON_IDS: Readonly<Record<string, WorkflowIconId>> = Object.freeze({
  'phase-1-init': 'phase.init',
  'phase-2-refine': 'phase.refine',
  'phase-3-outline': 'phase.outline',
  'phase-4-plan': 'phase.plan',
  'phase-5-execute': 'phase.execute',
  'phase-6-finalize': 'phase.finalize',
  ...PHASE_TOKENS,
});

/** The known unit ids, including the catalogue's separator aliases. */
const UNIT_ICON_IDS: Readonly<Record<string, WorkflowIconId>> = Object.freeze({
  land: 'flow.land',
  'git.commit_and_push': 'git.commit',
  'git.commit-and-push': 'git.commit',
  'git-commit-and-push': 'git.commit',
  'remediation.ci-failure': 'remediation.ci',
  'remediation.review': 'remediation.review',
  'remediation.analysis': 'remediation.analysis',
  'remediation.build-failure': 'remediation.build',
  'remediation.merge-conflict': 'remediation.merge',
  'remediation.quality-gate': 'remediation.qgate',
  replan: 'flow.replan',
  'flow.replan': 'flow.replan',
  'adjudicate.decisions': 'flow.adjudicate',
  'adjudicate-decisions': 'flow.adjudicate',
  'landing.files': 'axis.landing.files',
  'landing.pull-request': 'axis.landing.pr',
  'landing.pr': 'axis.landing.pr',
  'landing.main': 'axis.landing.main',
  'method.test-first': 'axis.method.test-first',
  'method.tests-after': 'axis.method.tests-after',
  'ask.every-phase': 'axis.ask.every',
  'ask.key-points': 'axis.ask.key',
  'ask.questions-only': 'axis.ask.questions',
  plan: 'unit.plan',
  epic: 'unit.epic',
  decomposition: 'unit.decomposition',
  review: 'unit.review',
  decision: 'unit.decision',
  chat: 'unit.chat',
  onboarding: 'unit.onboarding',
  'project-setup': 'unit.setup',
  setup: 'unit.setup',
  start: 'unit.start',
  bootstrap: 'unit.start',
  source: 'unit.source',
  'worker-task': 'unit.task',
  task: 'unit.task',
  entity: 'unit.entity',
  lessons: 'unit.lesson',
  lesson: 'unit.lesson',
  landing: 'unit.landing',
  'git.land-unit': 'unit.landing',
  'git.land-removal': 'unit.landing',
  'recipe.default': 'recipe.default',
});

/** @param id a candidate icon id @returns whether the registry delivers it */
export function isWorkflowIconId(id: string): id is WorkflowIconId {
  return (WORKFLOW_ICON_IDS as readonly string[]).includes(id);
}

/**
 * Resolves an id to its delivered asset.
 *
 * An unknown or empty id is not an error here: the schema refuses an empty frontmatter value,
 * while a future id from a newer runtime still renders through the neutral fallback.
 *
 * @param id the icon id
 * @returns the asset URL, or the fallback's when the id is unknown
 */
export function workflowIconUrl(id: string | null | undefined): string {
  const key = (id ?? '').trim();
  return isWorkflowIconId(key) ? ICON_URLS[key] : ICON_URLS['workflow.default'];
}

/**
 * @param value a bare phase token or a phase-unit id
 * @returns the phase icon, or the fallback when the value is not a phase
 */
export function phaseIconId(value: string | null | undefined): WorkflowIconId {
  const key = (value ?? '').trim().toLowerCase();
  const exact = PHASE_ICON_IDS[key];
  return exact ?? 'workflow.default';
}

/** @param name a remediation unit id @returns its category icon */
function remediationIconId(name: string): WorkflowIconId {
  if (name.includes('ci')) return 'remediation.ci';
  if (name.includes('review')) return 'remediation.review';
  if (name.includes('analy')) return 'remediation.analysis';
  if (name.includes('build')) return 'remediation.build';
  if (name.includes('merge') || name.includes('conflict')) return 'remediation.merge';
  if (name.includes('qgate') || name.includes('quality')) return 'remediation.qgate';
  return 'remediation.build';
}

/**
 * Resolves a unit id to the icon the catalogue records for it.
 *
 * A unit's own frontmatter is the authority where it has one; this function fills the gaps for
 * phase badges and compiled units, and never reads a file.
 *
 * @param unitName the unit id
 * @returns the catalogue icon, or the fallback for an unknown unit
 */
export function unitIconId(unitName: string | null | undefined): WorkflowIconId {
  const key = (unitName ?? '').trim().toLowerCase();
  if (key.length === 0) return 'workflow.default';
  const exact = UNIT_ICON_IDS[key] ?? PHASE_ICON_IDS[key];
  if (exact) return exact;
  if (key.startsWith('phase-')) return phaseIconId(key);
  if (key.startsWith('remediation')) return remediationIconId(key);
  if (key.startsWith('landing.')) {
    if (key.includes('pull') || key.includes('pr')) return 'axis.landing.pr';
    if (key.includes('main')) return 'axis.landing.main';
    return 'axis.landing.files';
  }
  if (key.startsWith('method.')) {
    return key.includes('test-first') ? 'axis.method.test-first' : 'axis.method.tests-after';
  }
  if (key.startsWith('ask.')) {
    if (key.includes('every')) return 'axis.ask.every';
    if (key.includes('key')) return 'axis.ask.key';
    return 'axis.ask.questions';
  }
  if (key.startsWith('entity')) return 'unit.entity';
  if (key.startsWith('source')) return 'unit.source';
  if (key.startsWith('git.land')) return 'unit.landing';
  return 'workflow.default';
}
