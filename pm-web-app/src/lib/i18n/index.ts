import { writable, derived, get } from 'svelte/store';
import { apiClient } from '../api/client';

export type Locale = 'en' | 'de';

export const fallbackMessages: Record<string, string> = {
  'web.offline.unreachable':
    'plan-marshall is not reachable on this machine. Start it in a terminal: pm-operator',
  'web.offline.reconnecting': 'Reconnecting …',
  'web.pair.page.title': 'Pair this browser',
  'web.pair.page.intro': 'A paired browser can act on this machine as you do.',
  'web.pair.page.code': 'Code',
  'web.pair.page.action': 'Pair this browser',
  'web.pair.page.pairing': 'Pairing this browser …',
  'entry.connection.loopback': 'Connected over loopback',
  'entry.connection.loopback.detail':
    'This browser is on the machine or reaches it through an SSH forward.',
  'entry.connection.lan': 'Connected over the network',
  'entry.connection.lan.detail': 'This browser reaches the machine over HTTPS on the network.',
  'entry.connection.devices': '{count, plural, one {# paired device} other {# paired devices}}',
  'entry.lan.title': 'Network access',
  'entry.lan.state': 'Network access: {state}',
  'entry.lan.state_on': 'open',
  'entry.lan.state_off': 'closed',
  'entry.lan.fingerprint': 'Fingerprint SHA-256',
  'entry.lan.fingerprint_hint': 'Compare it with the fingerprint the terminal printed.',
  'entry.lan.turn_off': 'Turn the network access off',
  'entry.lan.keep': 'Keep it open',
  'entry.lan.off_done': 'The network access is off; the loopback access stays open.',
  'entry.credentials.loopback_only_title': 'Available on this machine only',
  'entry.credentials.loopback_only':
    'A secret is entered on this machine only. In a terminal of the machine: {command}',
  'entry.credentials.field': 'Secret (not shown while you type)',
  'entry.credentials.save': 'Store the secret',
  'entry.credentials.stored': 'Stored on this machine.',
  'web.common.close': 'Close',
  'web.common.back': 'Back',
  'web.settings.title': 'Machine Settings',
  'web.settings.aspects': 'Configuration Aspects',
  'web.settings.devices': 'Paired Devices',
  'devices.list.title': 'Paired devices',
  'devices.list.subtitle': 'The browsers that can act on this machine',
  'devices.list.empty': 'No browser is paired.',
  'devices.list.this_one': 'this one',
  'devices.act.rename': 'Rename',
  'devices.act.label': 'Label',
  'devices.act.renamed': 'Renamed: {label}',
  'devices.act.revoke': 'Revoke…',
  'devices.act.revoke_others': 'Revoke all others',
  'devices.act.listener_off': 'Turn the network listener off',
  'devices.revoke.title': 'Revoke {label}?',
  'devices.revoke.explain': 'It is signed out at once; its open pages close with the reason.',
  'devices.revoke.this_one':
    'It is the browser you are using: its pages close and show the pairing page; a new code pairs it again.',
  'devices.revoke.others_title': 'Revoke {count} devices other than this one ({label})?',
  'devices.revoke.keep': 'Keep it',
  'devices.revoke.action': 'Revoke it',
  'devices.listener_off.done':
    'The network listener is off; the devices on the network stay paired and work again once it is on.',
  'devices.revoked.title': 'This browser is no longer paired',
  'devices.revoked.reason':
    '{reason, select, device_expired {It expired after not being used since {time}.} other {It was revoked from {by} {time}.}}',
  'devices.revoked.how_back':
    'To use the web app here again, pair it with a new code: run pm-operator in a terminal of this machine.',
  'devices.revoked.pair': 'Pair this browser',
  'overview.running.title': 'Running work',
  'overview.projects.empty': 'Nothing runs. Start a plan in a project, or enrol one.',
  'overview.project.counts':
    '{plans, plural, one {# plan} other {# plans}} · {epics, plural, one {# epic} other {# epics}}',
  'overview.project.plans_only': '{plans, plural, one {# plan} other {# plans}}',
  'overview.project.epics_only': '{epics, plural, one {# epic} other {# epics}}',
  'overview.project.setup': 'setup needs you',
  'overview.project.left': '{project} has nothing running any more.',
  'overview.view.by_plan': 'by plan',
  'overview.view.by_epic': 'by epic',
  'overview.kind.plan': 'plan',
  'overview.kind.epic': 'epic',
  'overview.group.without_epic': 'without epic',
  'overview.needs.count': '! {count}',
  'overview.needs.decision': 'decision',
  'overview.needs.waiver_request': 'waiver',
  'overview.needs.operator_wait': 'waits for you',
  'overview.needs.integrity_pause': 'integrity pause',
  'overview.needs.stalled': 'stalled',
  'overview.needs.condition': 'needs a fix',
  'overview.needs.held': 'held',
  'overview.needs.landing_failed': 'landing failed',
  'overview.needs.findings': 'new findings',
  'overview.needs.setup': 'setup',
  'overview.state.running': 'running',
  'overview.state.pausing':
    'pausing {point, select, task {after the task} other {after the phase}}',
  'overview.state.paused': 'paused',
  'overview.state.waiting': 'waiting for you',
  'overview.state.held': 'held',
  'overview.time.running': '{duration}',
  'overview.time.in_phase': 'in {phase} {duration}',
  'overview.progress.tasks': '{done} of {total} tasks',
  'overview.progress.plans': '{done} of {total} plans',
  'overview.epic.plans': '{running} running · {staged} staged',
  'overview.plan.belongs_to': 'in {epic}',
  'overview.plan.pull_request': '#{number}',
  'overview.plan.off_track': 'off track: {signal}',
  'overview.group.alongside': 'alongside',
  'overview.landing.removal':
    'removing {count, plural, one {# moved document} other {# moved documents}} from git · {state}',
  'overview.landing.unit': 'landing {unit} · {state}',
  'overview.connection.lost': 'Not connected to the runtime; reconnecting …',
  'overview.fallback.ended': '{item} has ended; showing its project.',
  'overview.ended.title': 'Ended work',
  'overview.ended.open': 'Ended work',
  'overview.ended.empty': 'Nothing in {project} has ended yet.',
  'overview.ended.outcome':
    '{outcome, select, merged {merged} no_change {no change} handed_back {handed back} returned {returned} cancelled {cancelled} abandoned {abandoned} released {released} aborted {aborted} other {ended}}',
  'overview.ended.epic_end':
    '{end, select, completed {completed} cancelled {cancelled} released {released} other {ended}}',
  'overview.ended.reason': 'Reason: {reason}',
  'overview.ended.time': 'ended {time}',
  'overview.ended.page': 'page {page} of {pages}',
  'overview.ended.newer': 'Newer',
  'overview.ended.older': 'Older',
  'overview.ended.as_it_ended': '{item} has ended ({outcome}, {time}); this is how it ended.',
  'report.list.title': 'Reports',
  'report.list.empty': 'Nothing new. Reports you have read are not shown any more.',
  'report.list.show_older': 'Show {count} older',
  'report.action.details': 'Show the details',
  'report.action.mark_read': 'Mark read',
  'report.action.mark_all_read': 'Mark all shown read',
  'report.arrival.show': 'Show them',
  'report.arrival.show_them': 'Show them',
  'web.frame.nav.reports': 'Reports',
  'web.frame.nav.decisions': 'Decisions',
  'common.actions.back': 'Back',
  'common.actions.read': 'Mark read',
  'decision.blocks.plan': 'blocks the plan',
  'decision.blocks.epic': 'blocks the epic',
  'decision.blocks.setup': "blocks the project's setup",
  'decision.blocks.entry': 'blocks the start',
  'decision.blocks.task': 'blocks one task; the plan goes on',
  'decision.blocks.none': 'blocks nothing',
  'decision.reversibility.reversible': 'can be undone',
  'decision.reversibility.costly': 'costly to undo',
  'decision.reversibility.irreversible': 'cannot be undone',
  'decision.waiting':
    '{subject, select, epic {the epic} setup {the setup of {project}} entry {the start in {project}} other {the plan}} waits for your answer, since {time}',
  'decision.part.position': 'part {n} of {total}',
  'decision.part.depends_on': 'depends on part {n}: {answer}',
  'decision.part.reopened': 'Part {n} was opened again, because part {m} changed.',
  'decision.support.votes':
    '{votes} of {total} votes · {models, plural, one {# model} other {# models}}',
  'decision.support.band.unanimous': 'unanimous',
  'decision.support.band.clear_majority': 'clear majority',
  'decision.support.band.split': 'split',
  'decision.support.band.no_consensus': 'no consensus',
  'decision.support.not_voted': 'not voted on',
  'decision.support.self_reported':
    "the model's own estimate: {value} of 100; models tend to overrate themselves",
  'decision.recommendation.by': 'Recommended by {role}: {option}',
  'decision.recommendation.on_request':
    'The recommendation is one step away, so that you weigh the options first.',
  'decision.dissent': 'Against it ({role}): {reason}',
  'decision.answer.own_words': 'in my own words',
  'decision.answer.note': 'add a note',
  'decision.answer.none_fits':
    'none fits: let {subject, select, epic {the epic} setup {the setup} entry {the start} other {the plan}} think again',
  'decision.answer.later': 'later',
  'decision.interpret.working': '{role} is reading your answer',
  'decision.interpret.unclear_bound':
    'Your words could not be read clearly enough ({reason}). Choose an option, or answer again.',
  'decision.own_words.unqualified':
    'Answering in your own words is not available: no model qualifies for {role}.',
  'decision.refused.invalid_answer': 'This answer does not fit the part: {reason}',
  'decision.refused.dependency_unanswered': 'Answer part {n} first.',
  'decision.readback.understood': 'I understood: {reading}',
  'decision.readback.condition': '{option}, provided {condition}',
  'decision.readback.own_option': 'your own answer: {words}',
  'decision.readback.delegation': 'you leave it to the plan',
  'decision.readback.confirm': 'Correct',
  'decision.readback.edit': 'edit my words',
  'decision.readback.choose': 'choose an option instead',
  'decision.clarify.asks': '{role} asks: {question}',
  'decision.confirm.title': 'Your decision',
  'decision.confirm.action': 'Decide',
  'decision.outcome.answered_elsewhere': 'answered in {channel} ({device}): {answer}',
  'decision.outcome.revised': 'The decision changed: {change}. Please answer again.',
  'decision.outcome.rethink':
    'you asked {subject, select, epic {the epic} setup {the setup of {project}} entry {the start in {project}} other {the plan}} to think again',
  'decision.outcome.asked_anew': 'asked anew after {id}',
  'decision.channel.web': 'the web app',
  'decision.channel.cli': 'the console',
  'decision.notify.ask':
    "Should the web app notify you when a plan, an epic, a project's setup, or a start asks a decision? It names the plan, the epic, the setup, or the start and the kind of decision, never its content.",
  'decision.notify.allow': 'Allow notifications',
  'decision.notify.deny': 'Not now',
  'decision.notify.title':
    '{subject, select, epic {Epic {name}} setup {The setup of {project}} entry {The start in {project}} other {{name}}} asks {kind, select, waiver {for a waiver} other {a decision}}',
  'decision.notify.tab_title': '({count}) {title}',
  'decision.waiver.title': '{plan} asks for a waiver',
  'decision.waiver.gate': 'Gate',
  'decision.waiver.result': 'Result',
  'decision.waiver.asked_by': 'Asked by',
  'decision.waiver.its_reason': 'Its reason',
  'decision.waiver.reason': 'Your reason',
  'decision.waiver.reason_required': 'Give your reason first.',
  'decision.waiver.waive': 'Waive the gate',
  'decision.waiver.decline': 'Decline',
  'decision.waiver.later': 'Later',
  'decision.answer.chat': 'chat about it first',
  'decision.chat.no_own_words':
    'this decision takes no answer in your own words: the chat explains, you choose',
  'decision.chat.proposed_answer': 'Proposed answer{part, select, none {} other {, part {part}}}',
  'decision.chat.use_words': 'Answer in these words',
  'decision.chat.stays_open': '{id} stays open; answer it once the chat has ended.',
  'decision.chat.withdrawn': '{id} is closed: the revision removed the need for it ({reason}).',
  'decision.chat.closed_elsewhere':
    '{id} was answered in {channel} ({device}); the chat has ended.',
  'decision.chat.after_chat': 'answered after a chat',
  'decision.chat.no_role':
    'This question comes from the setup itself: no planning role can chat about it.',
  'decision.chat.none_for_waiver':
    'A waiver request offers no chat: the waiver is your override, not an answer to its content.',
  'decisions.list.title': 'Decisions',
  'decisions.list.empty': 'No open decisions. Running plans will ask when a decision is needed.',
  'decisions.arrival.show_them': 'Show decision',
  'system.view.title': 'System',
  'system.runtime.title': 'Runtime',
  'system.runtime.version': 'pm-mcpd {version} · build {date} ({commit})',
  'system.runtime.available': '{version} available',
  'system.runtime.not_checked': 'newer version not checked',
  'system.runtime.checked_at': 'checked {time}',
  'system.runtime.check_now': 'Check now',
  'system.runtime.since': 'running since {time} · {uptime}',
  'system.runtime.memory': 'memory {used} in use of {max} · {resident} resident',
  'system.runtime.cpu': 'CPU {percent} %',
  'system.runtime.store': 'machine store {size} · {free} free on its disk · measured {time}',
  'system.runtime.listeners':
    'listeners socket · loopback {loopback_address} · network {lan_address}',
  'system.work.title': 'Work',
  'system.work.plans': 'plans',
  'system.work.epics': 'epics',
  'system.work.tasks': 'tasks {running} running · {queued} queued',
  'system.work.projects': '{count, plural, one {# project} other {# projects}} with running work',
  'system.work.all': 'All',
  'system.work.per_project': 'Per project',
  'system.workers.title': 'Workers',
  'system.workers.count': '{count} · {busy} busy · {idle} idle',
  'system.jobs.title': 'Jobs',
  'system.jobs.locks': 'locks {count} held',
  'system.models.title': 'Models',
  'system.models.window':
    '{window, select, hour {Last hour} today {Today} week {7 days} other {{window}}}',
  'system.models.per_model': 'Per model',
  'system.models.per_role': 'Per role',
  'system.models.none': 'No model calls in this window.',
  'system.action.stop': 'Stop…',
  'system.action.restart': 'Restart…',
  'system.action.upgrade': 'Upgrade…',
  'system.confirm.title':
    '{act, select, stop {Stop the runtime?} restart {Restart the runtime?} other {Upgrade to {version}?}}',
  'system.confirm.running': 'Running now: {what}',
  'system.confirm.nothing_runs': 'Nothing runs.',
  'system.confirm.when': 'When',
  'system.confirm.drain': 'When what runs is done',
  'system.confirm.drain_explain':
    'Nothing new starts. Each running task and job finishes, then the runtime stops.',
  'system.confirm.now': 'Now',
  'system.confirm.now_explain':
    'Running tasks are withdrawn and jobs stopped. Everything goes on from its last committed state at the next start.',
  'system.confirm.keep': 'Keep it running',
  'system.confirm.not_now': 'Not now',
  'system.stopping.line': 'Stopping when what runs is done · {count} left: {what}',
  'system.stopping.stop_now': 'Stop now',
  'system.stopped.title': 'The runtime is stopped ({who}, {time}).',
  'system.stopped.how':
    'Start it on this machine: {command}, or any verb of pm-operator. This page connects again by itself once it runs.',
  'system.stopped.copy': 'Copy the command',
  'system.restart.running': 'Restarting … the clients connect again by themselves.',
  'system.cleanup.title': 'Cleanup',
  'system.cleanup.line_due': 'due: {what} · frees {size}',
  'system.cleanup.line_none': 'nothing due · next check {time}',
  'system.cleanup.review': 'Review…',
  'system.cleanup.summary.title': 'Summary',
  'system.cleanup.summary.frees': 'frees {size}',
  'system.cleanup.details.title': 'Details',
  'system.cleanup.details.check_all': 'Check all',
  'system.cleanup.details.check_none': 'None',
  'system.cleanup.details.unchecked_note': 'Unchecked items are kept for good.',
  'system.cleanup.action.delete': 'Delete {what}…',
  'system.cleanup.action.remind': 'Ask me again in 3 days',
  'system.cleanup.action.ignore': 'Ignore',
  'system.cleanup.confirm.title': 'Delete {what}?',
  'system.cleanup.confirm.frees': 'This frees {size}. It cannot be undone.',
  'system.cleanup.confirm.stays':
    "Branches, pull requests, the remote, the checkout, and an epic's ledger in the repository stay.",
  'system.cleanup.confirm.kept':
    '{count, plural, =0 {} one {# unchecked item is kept for good.} other {# unchecked items are kept for good.}}',
  'system.cleanup.confirm.do': 'Delete',
  'system.cleanup.confirm.back': 'Back',
  'system.cleanup.deleted': 'Deleted {what}; {size} freed.',
  'system.cleanup.none': 'Nothing is due. The next check is {time}.',
  'system.cleanup.kept.title': 'Kept for good',
  'system.cleanup.kept.release': 'Release {count} selected',
  'system.cleanup.kept.release_all': 'Release all',
  'system.cleanup.kept.released':
    '{count, plural, one {# item goes} other {# items go}} back to the cleanup; due at the next check where past its duration.',
  'system.cleanup.kept.none': 'Nothing is kept for good.',
  'system.cleanup.residue.title': 'Removed by itself',
  'system.cleanup.residue.last': 'last sweep {time}',
  'system.cleanup.residue.explain':
    'Technical leftovers are removed by the runtime and never asked.',
  'system.figure.unavailable': 'not available',
  'system.figure.unavailable_reason': 'not measured on this platform',
  'project.explorer.title': 'Projects',
  'project.explorer.add': 'Add a project',
  'project.explorer.favourites': 'Favourites',
  'project.explorer.recent': 'Recent',
  'project.explorer.all': 'All projects',
  'project.explorer.by_name': 'by name',
  'project.explorer.most_used': 'most used',
  'project.explorer.filter': 'filter',
  'project.explorer.empty': 'No project is enrolled yet.',
  'project.adding.title': 'Being added',
  'project.adding.step': 'step {step} of {total}: {name}',
  'project.adding.continue': 'Continue',
  'project.adding.abandon': 'Abandon',
  'project.adding.abandon_confirm': 'Abandon adding {path}? Nothing was enrolled.',
  'project.state.ready': 'ready',
  'project.state.setup_needs': 'setup needs: {need}',
  'project.state.released': 'released',
  'project.action.running_work': 'Running work',
  'project.action.release': 'Release…',
  'project.action.unenrol': 'Unenrol…',
  'project.action.continue_setup': 'Continue the setup',
  'project.action.enrol_again': 'Enrol again',
  'project.release.title': 'Release {project}?',
  'project.release.what_ends': 'What ends',
  'project.release.ends': '{plans} plans, each ended as released and archived; {epics} epics, released with their plans and archived',
  'project.release.what_stays': 'What stays',
  'project.release.stays': "the repository: branches, worktrees, pull requests, and .marshall/ with the epics' ledgers, as they are",
  'project.release.reason': 'Reason',
  'project.release.keep': 'Keep it',
  'project.release.action_release': 'Release the project',
  'project.release.action_unenrol': 'Unenrol the project',
  'project.release.refused': '{project} cannot be unenrolled while work runs',
  'project.release.refused_hint': 'End them first, or release the project, which ends them.',
  // Plan View
  'plan.view.title': '{plan}',
  'plan.node.state.done': 'done',
  'plan.node.state.done_times': 'done \u00d7{count}',
  'plan.node.state.running': 'running',
  'plan.node.state.running_run': 'running \u00b7 run {run}',
  'plan.node.state.waiting': 'waiting for you',
  'plan.node.state.failed': 'failed',
  'plan.node.state.planned': 'planned',
  'plan.node.state.not_taken': 'not taken',
  'plan.node.state.not_in_plan': 'not in this plan',
  'plan.node.kind.phase': 'phase',
  'plan.node.kind.sub_workflow': 'sub-workflow',
  'plan.node.kind.job': 'job',
  'plan.node.kind.tasks': 'tasks',
  'plan.node.kind.waits': 'waits for you',
  'plan.node.kind.leaf': 'leaf',
  'plan.not_in_plan.group': 'not in this plan',
  'plan.not_in_plan.reason': '{node} ({reason})',
  'plan.leaf.tasks_done': '{done} of {total} tasks done',
  'plan.leaf.task':
    '{role} \u00b7 {harness} {model} \u00b7 attempt {attempt} \u00b7 {status} \u00b7 {outcome}',
  'plan.leaf.job': '{kind} \u00b7 {status} \u00b7 {duration} \u00b7 {outcome}',
  'plan.leaf.fired': 'fired {count}\u00d7: {outcomes}',
  'plan.leaf.entered': 'entered {time}, left {left}',
  'plan.path.jump': 'to where it is',
  'plan.view.changed': 'The plan changed: {event}.',
  'plan.view.ended': 'The plan has ended: {outcome}.',
  'plan.view.state': '{phase} \u00b7 {state}',
  'plan.view.holds': 'holds: {kind}',
  'plan.view.pull_request': '#{number}',
  'plan.leaf.job_waits_slot': 'waits for a build slot',
  'plan.view.tokens': 'tokens so far',
  'plan.view.tokens_none': 'none yet',
  'plan.view.tokens_detail': 'By phase and role',
  'plan.activity.title':
    '{role} \u00b7 {harness} {model} \u00b7 attempt {attempt} \u00b7 {duration}',
  'plan.activity.kind.text': 'wrote',
  'plan.activity.kind.tool_call': 'tool',
  'plan.activity.kind.tool_failed': 'failed \u00b7 {size}',
  'plan.activity.kind.file_changed': 'changed',
  'plan.activity.kind.link': 'link',
  'plan.activity.kind.question': 'asked',
  'plan.activity.kind.answer_ack': 'answer received',
  'plan.activity.server_only': 'This harness reports no steps; only what the server saw is shown.',
  'plan.activity.gap': '{count} older entries gave way',
  'plan.activity.off_track': 'off track: {signal} ({value} of {threshold}): {action}',
  'plan.prune.lane_choice': 'the land lane',
  'plan.prune.unneeded_step': 'step not needed',
  'plan.prune.not_in_plan': 'not in this plan',
  'plan.job.status.running': 'running',
  'plan.job.status.done': 'done',
  'plan.job.status.failed': 'failed',
  'plan.job.status.waits_slot': 'waits for a build slot',
  'plan.job.outcome.success': 'success',
  'plan.job.outcome.failure': 'failure',
  'plan.task.status.running': 'running',
  'plan.task.status.done': 'done',
  'plan.task.status.failed': 'failed',
  'plan.task.status.waiting': 'waiting',
  'plan.task.status.planned': 'planned',
  'plan.task.outcome.success': 'success',
  'plan.task.outcome.failure': 'failure',
  'plan.task.outcome.yielded': 'yielded',
  'plan.signal.repeating': 'repeating',
  'plan.signal.no_progress': 'no progress',
  'plan.signal.over_size': 'over size',
  'plan.signal.outside_footprint': 'outside footprint',
  'plan.signal.action.show': 'shown',
  'plan.signal.action.nudge': 'nudged',
  'plan.signal.action.yield': 'yielded',
  'plan.signal.action.restart': 'restarted',
  // Epic View
  'epic.view.title': '{epic}',
  'epic.view.status': '{status}',
  'epic.plans.running': 'running plans',
  'epic.plans.staged': 'staged plans',
  'epic.plans.none': 'no plans',
  'epic.review.title': 'completion review',
  'epic.review.ready': 'the completion review is ready',
  'epic.review.not_ready': 'completion review in progress',
  'epic.event.landed': '{plan} landed',
  'epic.event.overlap': '{plan} and {other} overlap',
  'overview.epic.event.landed_staged':
    '{plan} landed; {change, select, none {no staged plan changed} reorder {{plans} re-ordered} amend {{plans} amended} waits {a change of the staged plans waits for you} other {{plans} changed}}',
  'overview.epic.event.ended_held': '{plan} ended ({outcome}); {plans} held',
  'overview.epic.event.ended_restaged': '{plan} ended ({outcome}); staged again as {row}',
  'overview.epic.event.ended_left': '{plan} ended ({outcome}); the staged plans stay as they are',
  'overview.epic.event.started': 'started {plan} ({running} of {scope} at once)',
  'overview.epic.event.can_start': 'can start now: {plans}',
  'overview.epic.event.overlap_found':
    '{plan} and {other} changed {files, plural, one {# file} other {# files}} in common',
  'overview.epic.event.through':
    '{epic} is through: {review, select, ready {the completion review is ready} other {archiving runs its completion review}}',
  'overview.epic.event.mode': 'you switched automatic {mode, select, automatic {on} other {off}}',
  'overview.epic.sources': 'Sources',
  'overview.epic.landings': 'Landings',
  'overview.epic.findings': 'Findings',
  'overview.epic.findings_counts':
    '{new, plural, one {# new} other {# new}} \u00b7 {open, plural, one {# open} other {# open}} \u00b7 {kept, plural, one {# kept} other {# kept}}',
  'overview.epic.watches': 'Watches',
  'overview.epic.watches_counts':
    '{open, plural, one {# open} other {# open}} \u00b7 {kept, plural, one {# kept} other {# kept}}',
  'overview.epic.events': 'Events',
  'overview.epic.tokens': 'Tokens',
  'overview.epic.tokens_scope':
    "{plans, plural, one {# plan} other {# plans}} and the epic's own tasks",
  'overview.epic.tokens_detail': 'Per plan',
  'overview.epic.tokens_own': "the epic's own tasks",
  // Controlling work (UC-CTL-1..5)
  'control.actions.title': 'Actions \u00b7 {subject}',
  'control.action.disabled': '{action} is not available: {reason}',
  'control.plan.pause': 'Pause',
  'control.plan.start': 'Start',
  'control.plan.cancel': 'Cancel',
  'control.plan.abandon': 'Abandon',
  'control.plan.chat': 'Chat',
  'control.plan.settings': 'Settings',
  'control.plan.state.paused': 'paused since {time}',
  'control.plan.state.pausing':
    'pausing {point, select, task {after the task} other {after the phase}}',
  'control.plan.label.epic': 'Its epic',
  'control.plan.epic_told': '{epic} is told',
  'control.pause.title': 'Pause {plan}?',
  'control.pause.now': 'Now',
  'control.pause.after_task': 'After the current task',
  'control.pause.after_phase': 'After the current phase',
  'control.pause.keep_running': 'Keep it running',
  'control.end.title': '{action, select, cancel {Cancel} abandon {Abandon} other {Abort}} {plan}?',
  'control.end.label.reason': 'Reason',
  'control.end.keep': '{action, select, cancel {Keep it running} abandon {Keep it paused} other {Keep it held}}',
  'control.end.action':
    '{action, select, cancel {Cancel the plan} abandon {Abandon the plan} other {Abort the plan}}',
  'control.settings.title': 'Settings \u00b7 {plan}',
  'control.settings.group':
    '{group, select, plan_defaults {Plan defaults} roles {Roles} integrations {Integrations} other {Providers}}',
  'control.settings.origin':
    '{origin, select, plan {from the plan} project {from the project} team {from the team} machine {from the machine} derived {derived} other {shipped default}}',
  'control.settings.use_inherited': "Use the {level, select, project {project's} other {machine's}} value",
  'control.settings.applies':
    "Applies where {plan} next reads it; the plan's version becomes {version}.",
  'control.settings.refused': '{value} is refused: {reason}',
  'control.settings.too_late':
    'Too late: {plan} {position}; the new version would not contain where it is. Nothing changed.',
  'control.settings.epic_told': '{epic} is told of a change',
  'control.settings.close': 'Close',
  'control.settings.conflict': 'Changed meanwhile: it is now {value}. Yours was not applied.',
  'common.conflict.changed': 'Changed meanwhile: it is now {value}. Yours was not applied.',
  'common.conflict.apply_again': 'Apply yours again',
  'common.conflict.keep': 'Keep the current value',
  'common.conflict.no_input': 'Changed meanwhile: it is now {value}; --no-input stops here.',
  'control.hold.title':
    '{kind, select, stalled {{name} is stalled} condition {{name} holds: a condition} integrity_pause {{name} holds: an integrity pause} held {{name} holds: a held item} other {{name} holds: a failed landing}}',
  'control.hold.kind.stalled': 'stalled',
  'control.hold.kind.condition': 'a condition',
  'control.hold.kind.integrity_pause': 'an integrity pause',
  'control.hold.kind.held': 'a held item',
  'control.hold.kind.landing_failed': 'a failed landing',
  'control.hold.cause': 'Cause',
  'control.hold.tried': 'Tried {count, plural, one {once} other {# times}}, last {time}',
  'control.hold.resolve': 'Resolve',
  'control.hold.act.retry': 'Retry now',
  'control.hold.act.recycle': 'Recycle the worker',
  'control.hold.act.grant': 'Grant read access to {project}\u2026',
  'control.hold.act.add_credential': 'Add the credential\u2026',
  'control.hold.act.raise_budget': 'Raise the budget\u2026',
  'control.hold.integrity.file': 'File',
  'control.hold.integrity.recorded': 'recorded: {size} \u00b7 {digest}',
  'control.hold.integrity.current': 'current: {size} \u00b7 {digest}',
  'control.hold.act.accept_change': 'Accept the change\u2026',
  'control.hold.act.show_pr': 'Show the pull request #{number}',
  'control.hold.act.give_up_removal': 'Give up the removal\u2026',
  'control.hold.act.end_plan': 'End the plan\u2026',
  'control.hold.act.abort_plan': 'Abort the plan\u2026',
  'control.hold.act.cancel_epic': 'Cancel the epic\u2026',
  'control.hold.released': '{subject} goes on',
  'control.hold.still_held': 'Still held: {cause}',
  'control.hold.cleared': '{cause} cleared by itself',
  // Starting work (UC-STA-1..5)
  'start.modal.title': '{kind, select, plan {Start a plan in {project}} other {Start an epic in {project}}}',
  'start.mode.sources': 'A plan from sources',
  'start.mode.recipe': 'A recipe',
  'start.field.prompt': 'What is the work?',
  'start.field.documents': 'Documents (one path per line)',
  'start.field.issue': 'Issue number or URL',
  'start.field.title': 'Title',
  'start.action.start': '{kind, select, plan {Start the plan} other {Start the epic}}',
  'start.action.cancel': 'Cancel',
  'start.recipe.list': 'Which recipe?',
  'start.recipe.parameters': '{recipe}: its parameters',
  'start.recipe.no_option': '{parameter}: {reason}; the recipe cannot run',
  'start.recipe.loading': 'Loading recipes …',
  'start.recipe.empty': 'No recipe is offered in {project}.',
  'start.error': 'The start failed: {cause}',
  'start.sources.waits': 'waits for {count, plural, one {# source} other {# sources}}',
  'start.source.description': 'Description',
  'start.source.documents': 'Documents',
  'start.source.issue': 'Issue',
  'start.recipe.proposed': 'proposed',
  'start.started.title': '{name} has started',
  'start.started.follow': 'Follow it',
  'start.started.back': 'Back to the project',
  // Git plan sources (UC-STA-7, UC-STA-9)
  'git_sources.title': 'Git sources in {project}',
  'git_sources.mode.adopt_pr': 'Adopt a pull request',
  'git_sources.mode.local_changes': 'Local changes',
  'git_sources.tab.pr': 'Adopt a pull request',
  'git_sources.tab.local': 'Local changes',
  'git_sources.pr.field': 'Number or URL of the pull request',
  'git_sources.pr.observe': 'Pull request observed',
  'git_sources.pr.observation': '{title} by {author} · {branch}',
  'git_sources.pr.files_untrusted': '{count, plural, one {# changed file} other {# changed files}} · untrusted',
  'git_sources.pr.writable_head': 'the head branch is writable',
  'git_sources.pr.head_not_writable': 'the head branch is not writable',
  'git_sources.pr.not_open': 'the pull request is not open',
  'git_sources.pr.rate_limited': 'rate limited until {time}',
  'git_sources.pr.flagged.title': "This pull request's text may contain instructions to a model",
  'git_sources.pr.flagged.accept': 'Adopt it anyway',
  'git_sources.pr.unscreened.accept': 'Adopt it as untrusted',
  'git_sources.pr.adopt.action': 'Adopt the pull request',
  'git_sources.pr.adopt.title': 'Adopt a pull request in {project}',
  'git_sources.pr.adopted': '{name} adopted the pull request',
  'git_sources.local.title': 'Capture local changes in {project}',
  'git_sources.local.preview': '{count, plural, one {# file changed} other {# files changed}} against {branch}',
  'git_sources.local.line_additions': '+{additions}',
  'git_sources.local.line_deletions': '-{deletions}',
  'git_sources.local.eligible': 'the landing {landing} commits the snapshot',
  'git_sources.local.ineligible': "local changes cannot land: the project's landing is {landing}",
  'git_sources.local.pm_wf_25': 'local changes need a landing that commits (PM-WF-25)',
  'git_sources.local.capture': 'Capture the local changes',
  'git_sources.local.captured': '{name} captured the local changes',
  'git_sources.local.snapshot': 'snapshot commit {sha}',
  'git_sources.local.confirm': 'Capture the local changes into a snapshot commit and land them?',
  'git_sources.local.declined': 'Nothing was captured.',
  'git_sources.confirm.required': 'Confirmation required (--yes).',
  'git_sources.screening.clean': 'clean',
  'git_sources.screening.flagged': 'flagged',
  'git_sources.screening.unscreened': 'unscreened',
  'git_sources.lane.adopt': 'adopt',
  'git_sources.lane.land': 'land',
  'git_sources.lane': '{lane} lane',
  'git_sources.error': 'The git source intake failed: {cause}',
  'git_sources.actions.confirm': 'Confirm',
  'git_sources.actions.cancel': 'Cancel',
};

export const currentLocale = writable<Locale>('en');
export const messagesStore = writable<Record<string, string>>({});

function skipWhitespace(text: string, startIndex: number): number {
  let i = startIndex;
  while (i < text.length && /\s/.test(text[i])) {
    i++;
  }
  return i;
}

function readBraceBlock(body: string, startIndex: number): { text: string; nextIndex: number } {
  let depth = 1;
  let i = startIndex;
  while (i < body.length && depth > 0) {
    if (body[i] === '{') {
      depth++;
    } else if (body[i] === '}') {
      depth--;
    }
    i++;
  }
  return { text: body.slice(startIndex, i - 1), nextIndex: i };
}

function parseSelectBranches(body: string): Record<string, string> {
  const branches: Record<string, string> = {};
  let i = 0;
  while (i < body.length) {
    i = skipWhitespace(body, i);
    if (i >= body.length) break;

    const keyStart = i;
    while (i < body.length && /[a-zA-Z0-9_-]/.test(body[i])) {
      i++;
    }
    const key = body.slice(keyStart, i);

    i = skipWhitespace(body, i);
    if (i >= body.length || body[i] !== '{') break;

    const block = readBraceBlock(body, i + 1);
    branches[key] = block.text;
    i = block.nextIndex;
  }
  return branches;
}

function resolveIcuSelect(result: string, params: Record<string, string | number>): string {
  const selectIdx = result.indexOf(', select,');
  if (selectIdx === -1) return result;

  const openBrace = result.lastIndexOf('{', selectIdx);
  if (openBrace === -1) return result;

  const varName = result.slice(openBrace + 1, selectIdx).trim();
  const val = String(params[varName] ?? '');

  let depth = 0;
  let closeBrace = -1;
  for (let i = openBrace; i < result.length; i++) {
    if (result[i] === '{') depth++;
    else if (result[i] === '}') {
      depth--;
      if (depth === 0) {
        closeBrace = i;
        break;
      }
    }
  }

  if (closeBrace === -1) return result;

  const body = result.slice(selectIdx + ', select,'.length, closeBrace);
  const branches = parseSelectBranches(body);
  const replacement = branches[val] ?? branches['other'] ?? '';
  return result.slice(0, openBrace) + replacement + result.slice(closeBrace + 1);
}

function resolveIcuPlural(result: string, params: Record<string, string | number>): string {
  const pluralIdx = result.indexOf(', plural,');
  if (pluralIdx === -1) return result;

  const openBrace = result.lastIndexOf('{', pluralIdx);
  if (openBrace === -1) return result;

  const varName = result.slice(openBrace + 1, pluralIdx).trim();
  const count = Number(params[varName] ?? 0);

  let depth = 0;
  let closeBrace = -1;
  for (let i = openBrace; i < result.length; i++) {
    if (result[i] === '{') depth++;
    else if (result[i] === '}') {
      depth--;
      if (depth === 0) {
        closeBrace = i;
        break;
      }
    }
  }

  if (closeBrace === -1) return result;

  const body = result.slice(pluralIdx + ', plural,'.length, closeBrace);
  const branches = parseSelectBranches(body);
  const exactKey = `=${count}`;
  let replacement = branches[exactKey];
  if (replacement === undefined) {
    if (count === 1 && branches['one'] !== undefined) {
      replacement = branches['one'];
    } else {
      replacement = branches['other'] ?? '';
    }
  }
  replacement = replacement.replace(/#/g, String(count));
  return result.slice(0, openBrace) + replacement + result.slice(closeBrace + 1);
}

export function formatString(template: string, params?: Record<string, string | number>): string {
  if (!params) return template;
  let processed = template;
  while (processed.includes(', select,')) {
    const next = resolveIcuSelect(processed, params);
    if (next === processed) break;
    processed = next;
  }
  while (processed.includes(', plural,')) {
    const next = resolveIcuPlural(processed, params);
    if (next === processed) break;
    processed = next;
  }
  return processed.replace(/\{(\w+)\}/g, (match, key) => {
    if (key in params) {
      return String(params[key]);
    }
    return match;
  });
}

export async function loadMessages(locale: Locale, area = 'all'): Promise<void> {
  try {
    const msgs = await apiClient.getMessages(locale, area);
    if (area === 'all') {
      messagesStore.set(msgs);
    } else {
      messagesStore.update((curr) => ({ ...curr, ...msgs }));
    }
  } catch {
    // Retain existing messages or fallbacks on failure
  }
}

export async function setLocale(locale: Locale): Promise<void> {
  currentLocale.set(locale);
  if (typeof window !== 'undefined') {
    localStorage.setItem('pm_locale', locale);
  }
  await loadMessages(locale, 'all');
}

export async function initI18n(): Promise<void> {
  let initialLocale: Locale = 'en';
  if (typeof window !== 'undefined') {
    const saved = localStorage.getItem('pm_locale') as Locale | null;
    if (saved === 'en' || saved === 'de') {
      initialLocale = saved;
    } else if (navigator?.language?.startsWith('de')) {
      initialLocale = 'de';
    }
  }
  currentLocale.set(initialLocale);
  await loadMessages(initialLocale, 'all');
}

export function translate(
  locale: Locale,
  key: string,
  params?: Record<string, string | number>,
): string {
  const allMessages = get(messagesStore);
  const template = allMessages[key] || fallbackMessages[key] || key;
  return formatString(template, params);
}

export const t = derived(
  [currentLocale, messagesStore],
  ([, $messages]) =>
    (key: string, params?: Record<string, string | number>): string => {
      const template = $messages[key] || fallbackMessages[key] || key;
      return formatString(template, params);
    },
);
