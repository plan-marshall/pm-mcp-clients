import '@testing-library/jest-dom';
import { vi, beforeEach } from 'vitest';
import { messagesStore } from './lib/i18n';

class LocalStorageMock implements Storage {
  private store: Record<string, string> = {};

  clear(): void {
    this.store = {};
  }

  getItem(key: string): string | null {
    return this.store[key] ?? null;
  }

  setItem(key: string, value: string): void {
    this.store[key] = String(value);
  }

  removeItem(key: string): void {
    delete this.store[key];
  }

  get length(): number {
    return Object.keys(this.store).length;
  }

  key(index: number): string | null {
    return Object.keys(this.store)[index] ?? null;
  }
}

const mockStorage = new LocalStorageMock();

Object.defineProperty(globalThis, 'localStorage', {
  value: mockStorage,
  writable: true,
  configurable: true,
});

const defaultTestMessages: Record<string, string> = {
  'web.pair.page.title': 'Pair this browser',
  'web.pair.page.intro': 'A paired browser can act on this machine as you do.',
  'web.pair.page.code': 'Code',
  'web.pair.page.get_code': 'Get a code in a terminal of the machine:',
  'web.pair.page.this_machine': 'on this machine',
  'web.pair.page.other_computer': 'from another computer',
  'web.pair.page.action': 'Pair this browser',
  'web.pair.page.pairing': 'Pairing this browser …',
  'web.pair.page.invalid': 'This code does not work. Get a new one in a terminal of the machine.',
  'web.pair.page.fingerprint': 'Fingerprint SHA-256',
  'web.pair.page.fingerprint_hint': 'Compare it with the fingerprint the terminal printed.',
  'web.pair.done.paired_as': 'Paired as {label}',
  'web.pair.done.rename': 'Rename',

  'web.revoked.page.title': 'This browser is no longer paired',
  'web.revoked.page.intro':
    'A paired browser can act on this machine as you do. To pair this one again, get a code in a terminal of the machine.',
  'web.revoked.reason.device': 'It was revoked from {device} at {time}.',
  'web.revoked.reason.terminal': 'It was revoked in the terminal at {time}.',
  'web.revoked.reason.others': 'All other devices were revoked from {device} at {time}.',
  'web.revoked.reason.self': 'You revoked this browser at {time}.',
  'web.revoked.reason.idle': 'It was not used for 30 days, so its pairing expired.',
  'web.revoked.reason.unknown': 'Its pairing is no longer accepted.',

  'web.offline.title': 'This machine is not reachable over the network now',
  'web.offline.description':
    'Its network access was turned off at {time}. This browser stays paired: it works again once the network access is on (pm-operator, web lan).',
  'web.offline.unreachable':
    'plan-marshall is not reachable on this machine. Start it in a terminal: pm-operator',
  'web.offline.reconnecting': 'Reconnecting …',

  'web.frame.machine': 'Machine: {name}',
  'web.frame.settings': 'Machine settings',
  'web.frame.help': 'What is this step for?',
  'web.frame.nav.explorer': 'Projects',
  'web.frame.nav.plans': 'Plans',
  'web.frame.nav.reports': 'Reports',
  'web.frame.nav.system': 'System',
  'web.frame.nav.designer': 'Process Designer',

  'web.common.copy': 'Copy',
  'web.common.copied': 'Copied',
  'web.common.show_full': 'Show in full',
  'web.common.why': 'Why?',
  'web.common.back': 'Back',
  'web.common.close': 'Close',
  'web.common.save': 'Save',
  'web.common.cancel': 'Cancel',

  'onboard.step.language': 'Language',
  'onboard.step.harnesses': 'Harnesses',
  'onboard.step.models': 'Models',
  'onboard.step.roles': 'Roles',
  'onboard.step.credentials': 'Credentials',
  'onboard.step.providers': 'Providers',
  'onboard.step.plans': 'Plans',
  'onboard.step.done': 'Done',

  'onboard.entry.title': 'Set up this machine for plan-marshall',
  'onboard.entry.intro':
    'Seven steps. Nothing on this machine is looked at, run, or changed until you say so in that step.',
  'onboard.mode.auto': 'Decide everything for me…',
  'onboard.mode.guided': 'Begin with step 1',

  'common.consent.field.looks_for': 'Looks for',
  'common.consent.field.in': 'In',
  'common.consent.field.runs': 'Runs',
  'common.consent.field.never': 'Never',
  'common.consent.field.tokens': 'Tokens',
  'common.consent.yes': 'Yes',
  'common.consent.yes_remember': 'Yes, and remember this choice',
  'common.consent.no': 'Not now',

  'common.model.working': '{role} is working on {harness} · {model}',
  'common.model.tokens_estimate': 'about {tokens} tokens',
  'common.model.continue_without': 'Continue without the advice',
  'common.model.leave_note':
    'You can leave this page; the work continues, and the next visit shows its result.',
  'common.model.no_result': 'The steward gave no proposal: {reason}.',

  'web.settings.title': 'Machine Settings',
  'web.settings.aspects': 'Configuration Aspects',
  'web.settings.devices': 'Paired Devices',
  'web.settings.devices.label': 'Label',
  'web.settings.devices.created': 'Paired at',
  'web.settings.devices.last_used': 'Last used',
  'web.settings.devices.revoke': 'Revoke',
  'web.settings.devices.rename': 'Rename',

  'designer.view.title': 'Process Designer',
  'designer.open.title': 'Design a process',
  'designer.open.question': 'Where does the unit come from?',
  'designer.input.upload': 'Upload a file',
  'designer.input.upload_hint': 'a .pmwf file from this computer, at most {limit}',
  'designer.input.paste': 'Paste text',
  'designer.input.installed': 'Start from an installed unit',
  'designer.input.open': 'Open it',
  'designer.frontmatter.title': 'About this unit',
  'designer.frontmatter.name': 'Name',
  'designer.frontmatter.version': 'Version',
  'designer.frontmatter.title_field': 'Title',
  'designer.frontmatter.description': 'Description',
  'designer.validate.action': 'Validate',
  'designer.validate.valid': 'The process is valid.',
  'designer.validate.findings': '{count} findings',
  'designer.structure.title': 'Process Structure',
  'designer.building_blocks.title': 'Building Blocks',
  'designer.composed.title': 'Composed Process',

  'workspace.projects.title': 'Projects',
  'workspace.projects.empty': 'No enrolled projects on this machine.',
  'workspace.plans.title': 'Running & Ended Plans',
  'workspace.plans.empty': 'No plans currently active.',
  'workspace.system.title': 'System Status',
  'workspace.system.version': 'Version',
  'workspace.system.status': 'Status',
  'workspace.system.listener': 'Listener',
};

beforeEach(() => {
  mockStorage.clear();
  messagesStore.set(defaultTestMessages);
  vi.restoreAllMocks();
});
