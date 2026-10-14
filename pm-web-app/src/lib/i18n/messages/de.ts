export const de: Record<string, string> = {
  // Web pairing
  'web.pair.page.title': 'Kopple diesen Browser',
  'web.pair.page.intro': 'Ein gekoppelter Browser kann auf dieser Maschine handeln wie du.',
  'web.pair.page.code': 'Code',
  'web.pair.page.get_code': 'Hol dir einen Code in einem Terminal der Maschine:',
  'web.pair.page.this_machine': 'auf dieser Maschine',
  'web.pair.page.other_computer': 'von einem anderen Computer',
  'web.pair.page.action': 'Kopple diesen Browser',
  'web.pair.page.pairing': 'Dieser Browser wird gekoppelt …',
  'web.pair.page.invalid':
    'Dieser Code funktioniert nicht. Hol dir einen neuen in einem Terminal der Maschine.',
  'web.pair.page.fingerprint': 'Fingerabdruck SHA-256',
  'web.pair.page.fingerprint_hint':
    'Vergleiche ihn mit dem Fingerabdruck, den das Terminal ausgegeben hat.',
  'web.pair.done.paired_as': 'Gekoppelt als {label}',
  'web.pair.done.rename': 'Benenne um',

  // Revocation
  'web.revoked.page.title': 'Dieser Browser ist nicht mehr gekoppelt',
  'web.revoked.page.intro':
    'Ein gekoppelter Browser kann auf dieser Maschine handeln wie du. Um ihn erneut zu koppeln, hole einen Code im Terminal.',
  'web.revoked.reason.device': 'Er wurde um {time} von {device} aus entfernt.',
  'web.revoked.reason.terminal': 'Er wurde um {time} im Terminal entfernt.',
  'web.revoked.reason.others': 'Alle anderen Geräte wurden um {time} von {device} aus entfernt.',
  'web.revoked.reason.self': 'Du hast diesen Browser um {time} entfernt.',
  'web.revoked.reason.idle': 'Er wurde 30 Tage nicht genutzt und ist abgelaufen.',
  'web.revoked.reason.unknown': 'Seine Kopplung wird nicht mehr akzeptiert.',

  // Offline / Listener off
  'web.offline.title': 'Diese Maschine ist derzeit über das Netzwerk nicht erreichbar',
  'web.offline.description':
    'Der Netzwerkzugriff wurde um {time} ausgeschaltet. Dieser Browser bleibt gekoppelt: er funktioniert wieder, sobald der Netzwerkzugriff aktiv ist (pm-operator, web lan).',
  'web.offline.unreachable':
    'plan-marshall ist auf dieser Maschine nicht erreichbar. Starte es im Terminal: pm-operator',
  'web.offline.reconnecting': 'Verbinde erneut …',

  // Frame & Navigation
  'web.frame.machine': 'Maschine: {name}',
  'web.frame.settings': 'Einstellungen der Maschine',
  'web.frame.help': 'Wofür ist dieser Schritt?',
  'web.frame.nav.explorer': 'Projekte',
  'web.frame.nav.plans': 'Pläne',
  'web.frame.nav.reports': 'Berichte',
  'web.frame.nav.system': 'System',
  'web.frame.nav.designer': 'Prozess-Designer',

  // Common UI
  'web.common.copy': 'Kopiere',
  'web.common.copied': 'Kopiert',
  'web.common.show_full': 'Zeige alles',
  'web.common.why': 'Warum?',
  'web.common.back': 'Zurück',
  'web.common.close': 'Schließen',
  'web.common.save': 'Speichern',
  'web.common.cancel': 'Abbrechen',

  // Onboarding steps
  'onboard.step.language': 'Sprache',
  'onboard.step.harnesses': 'Harnesses',
  'onboard.step.models': 'Modelle',
  'onboard.step.roles': 'Rollen',
  'onboard.step.credentials': 'Zugangsdaten',
  'onboard.step.providers': 'Provider',
  'onboard.step.plans': 'Pläne',
  'onboard.step.done': 'Fertig',

  // Onboarding entry & mode
  'onboard.entry.title': 'Maschine für plan-marshall einrichten',
  'onboard.entry.intro':
    'Sieben Schritte. Auf dieser Maschine wird nichts angesehen, ausgeführt oder geändert, bevor du es in dem Schritt erlaubst.',
  'onboard.mode.auto': 'Entscheide alles für mich…',
  'onboard.mode.guided': 'Mit Schritt 1 beginnen',

  // Consent
  'common.consent.field.looks_for': 'Sucht nach',
  'common.consent.field.in': 'In',
  'common.consent.field.runs': 'Führt aus',
  'common.consent.field.never': 'Niemals',
  'common.consent.field.tokens': 'Tokens',
  'common.consent.yes': 'Ja',
  'common.consent.yes_remember': 'Ja, und Auswahl merken',
  'common.consent.no': 'Nicht jetzt',

  // Model working
  'common.model.working': '{role} arbeitet auf {harness} · {model}',
  'common.model.tokens_estimate': 'ca. {tokens} Tokens',
  'common.model.continue_without': 'Ohne Vorschlag fortfahren',
  'common.model.leave_note':
    'Du kannst diese Seite verlassen; die Arbeit läuft im Hintergrund weiter.',
  'common.model.no_result': 'Der Steward gab keinen Vorschlag: {reason}.',

  // Machine settings
  'web.settings.title': 'Maschinen-Einstellungen',
  'web.settings.aspects': 'Konfigurations-Aspekte',
  'web.settings.devices': 'Gekoppelte Geräte',
  'web.settings.devices.label': 'Bezeichnung',
  'web.settings.devices.created': 'Gekoppelt am',
  'web.settings.devices.last_used': 'Zuletzt genutzt',
  'web.settings.devices.revoke': 'Entfernen',
  'web.settings.devices.rename': 'Umbenennen',

  // Process Designer
  'designer.view.title': 'Prozess-Designer',
  'designer.open.title': 'Einen Prozess entwerfen',
  'designer.open.question': 'Woher kommt die Einheit?',
  'designer.input.upload': 'Lade eine Datei hoch',
  'designer.input.upload_hint': 'eine .pmwf-Datei von diesem Computer, höchstens {limit}',
  'designer.input.paste': 'Füge Text ein',
  'designer.input.installed': 'Starte mit einer installierten Einheit',
  'designer.input.open': 'Öffne sie',
  'designer.frontmatter.title': 'Über diese Einheit',
  'designer.frontmatter.name': 'Name',
  'designer.frontmatter.version': 'Version',
  'designer.frontmatter.title_field': 'Titel',
  'designer.frontmatter.description': 'Beschreibung',
  'designer.validate.action': 'Validieren',
  'designer.validate.valid': 'Der Prozess ist gültig.',
  'designer.validate.findings': '{count} Befunde',
  'designer.structure.title': 'Prozess-Struktur',
  'designer.building_blocks.title': 'Bausteine',
  'designer.composed.title': 'Zusammengesetzter Prozess',

  // Workspace
  'workspace.projects.title': 'Projekte',
  'workspace.projects.empty': 'Keine registrierten Projekte auf dieser Maschine.',
  'workspace.plans.title': 'Laufende & beendete Pläne',
  'workspace.plans.empty': 'Derzeit keine aktiven Pläne.',
  'workspace.system.title': 'Systemstatus',
  'workspace.system.version': 'Version',
  'workspace.system.status': 'Status',
  'workspace.system.listener': 'Listener',
};
