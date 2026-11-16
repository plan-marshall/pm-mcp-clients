<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t, currentLocale, hasMessage } from '../i18n';
  import Tabs from '../components/Tabs.svelte';
  import Button from '../components/Button.svelte';
  import RadioGroup from '../components/RadioGroup.svelte';
  import FormStatus from '../components/FormStatus.svelte';
  import ActionBar from '../components/ActionBar.svelte';
  import ConfirmDialog from '../components/ConfirmDialog.svelte';
  import LanListenerModal from '../components/LanListenerModal.svelte';
  import CredentialEntry from '../components/CredentialEntry.svelte';
  import { DEVICE_ACTS, type ActSpec } from '../actions/byState';
  import {
    apiClient,
    type ConsentRecord,
    type MachineCredentialStatus,
    type MachineSettingsRecord,
    type PairedDevice,
  } from '../api/client';
  import { isClientError, errorCodeOf } from '../api/errors';
  import ErrorState from '../components/ErrorState.svelte';
  import Skeleton from '../components/Skeleton.svelte';
  import { formatDay, formatTime } from '../display/format';
  import {
    runtimeStore,
    navigateTo,
    goBack,
    selectObjectTab,
    loadMachineSettings,
    updateMachineSetting,
    withdrawConsent,
    storeCredential,
    openOnboardingStep,
  } from '../stores/runtime';
  import { SETTINGS_TABS, type SettingsTab } from '../routes';

  /**
   * The one settings view (UX-S-12): a tab per part of what the machine holds.
   *
   * The tabs follow the design's mapping of the settings aspects onto the page: the setup carries
   * what the onboarding recorded, the access the paired devices and the network listener, the
   * credentials the store, the providers and the tokens, the consents the remembered ones, and the
   * retention the cleanup's durations. The tab is part of the address (`?tab=…`), so a reload and a
   * bookmark open the same part.
   */

  /** The catalogue key of each tab's visible label. */
  const TAB_KEYS: Record<SettingsTab, string> = {
    setup: 'web.settings.tab.setup',
    access: 'web.settings.tab.access',
    credentials: 'web.settings.tab.credentials',
    consents: 'web.settings.tab.consents',
    retention: 'web.settings.tab.retention',
  };

  /** The aspects the setup tab shows, in the design's order; the rest belong to the credentials. */
  const SETUP_ASPECTS: ReadonlyArray<string> = [
    'language',
    'harness',
    'harnesses',
    'catalogue',
    'models',
    'roles',
    'defaults',
    'plans',
  ];

  /** The aspects the credentials tab shows: the store the tokens live in, and the providers. */
  const CREDENTIAL_ASPECTS: ReadonlyArray<string> = ['store', 'credentials', 'providers'];

  /** A setting changed here that another client changed meanwhile. */
  interface ConflictState {
    key: string;
    currentValue: string;
    wantedValue: string;
    /** The cleanup categories the operator changed, which applying again sends over the current record. */
    changedFields?: CleanupDurationsRecord;
  }

  /** The cleanup durations as one record of days, `-1` for a category that is never offered. */
  type CleanupDurationsRecord = Record<string, number>;

  /** The record's keys in the design's order; the record itself decides which are shown. */
  const CLEANUP_CATEGORY_ORDER: ReadonlyArray<string> = [
    'plans_after_days',
    'epics_after_days',
    'logs_after_days',
    'reports_after_days',
    'unenrolled_after_days',
    'remind_after_days',
  ];

  /** The retention minimum of `doc/design/shared/cleanup.adoc`, refused before it is sent. */
  const MINIMUM_RETENTION_DAYS = 7;
  /** The reminder minimum of the same contract. */
  const MINIMUM_REMIND_DAYS = 1;
  /** The value a retention category carries when its items are never offered. */
  const NEVER = -1;

  $: activeTab = activeSettingsTab($runtimeStore.objectTab);
  $: tabs = SETTINGS_TABS.map((id) => ({
    id,
    label: $t(TAB_KEYS[id]),
    panelId: `settings-panel-${id}`,
  }));

  /**
   * @param tab the tab the store carries
   * @returns the tab to show: the stored one when it is one of the settings', the first otherwise
   */
  function activeSettingsTab(tab: string | null | undefined): SettingsTab {
    return (SETTINGS_TABS as ReadonlyArray<string>).includes(tab ?? '')
      ? (tab as SettingsTab)
      : 'setup';
  }

  /**
   * Shows a tab and writes it to the address, replacing the entry the view stands on.
   *
   * @param id the tab a person chose
   */
  function chooseTab(id: string): void {
    selectObjectTab(activeSettingsTab(id));
  }

  // Written once the view is on screen, so a bare `/settings` is replaced by its first tab and the
  // page has one spelling: the legacy `/settings/machine` address redirects to `?tab=setup`.
  onMount(() => {
    if ($runtimeStore.objectTab !== activeTab) {
      selectObjectTab(activeTab);
    }
  });

  // ---------------------------------------------------------------------------------------------
  // The paired devices (UC-ENT-2): the list, renaming one inline, revoking one, revoking all
  // others, and the network listener with its warning view.
  // ---------------------------------------------------------------------------------------------

  /** The paired devices of the machine. */
  let devices: PairedDevice[] = [];
  let isLoadingDevices = false;
  let loadError: string | null = null;
  /** Whether the devices read has been served at least once, so a later failure is an arrival. */
  let hasShownDevices = false;
  /** How many reads this page ran, so a failed retry is announced even before the first success. */
  let loadAttempts = 0;
  let actionError = '';
  let listenerNotice = '';
  let renamingId: string | null = null;
  let renameValue = '';
  let lanListenerOn = true;
  /** Whether the status read said how the listener stands; a failed read leaves it unknown. */
  let listenerKnown = false;
  let lanModalOpen = false;
  let pendingRevokeTarget: PairedDevice | 'others' | null = null;
  /** Whether the confirmed revoke runs; the dialog stays open and refuses a second confirm. */
  let revoking = false;

  let unsubscribeEvents: (() => void) | null = null;
  let isMounted = true;
  /** The moment the ages are measured at; a quiet timer moves it so an idle list stays true. */
  let clock = Date.now();
  let clockTimer: ReturnType<typeof setInterval> | null = null;

  /** The read in flight, aborted when a newer one starts. */
  let loadController: AbortController | null = null;
  /** The number of the newest read; an older answer never reaches the view. */
  let loadSequence = 0;

  onMount(() => {
    isMounted = true;
    unsubscribeEvents = apiClient.subscribeEvents((event, data) => {
      handleEvent(event, data);
    });
    clockTimer = setInterval(() => {
      clock = Date.now();
    }, 30_000);
    void loadData();
    void loadSettings();
  });

  onDestroy(() => {
    isMounted = false;
    loadController?.abort();
    loadController = null;
    if (clockTimer !== null) {
      clearInterval(clockTimer);
      clockTimer = null;
    }
    if (unsubscribeEvents) {
      unsubscribeEvents();
      unsubscribeEvents = null;
    }
  });

  function handleEvent(event: string, data: unknown) {
    if (event === 'device.renamed') {
      const payload = data as { device_id: string; label: string };
      if (payload?.device_id && payload?.label) {
        devices = devices.map((d) =>
          d.device_id === payload.device_id ? { ...d, label: payload.label } : d,
        );
      }
    } else if (event === 'device.revoked') {
      const payload = data as { device_id: string };
      if (payload?.device_id) {
        devices = devices.filter((d) => d.device_id !== payload.device_id);
      }
    }
  }

  /**
   * Reads the devices and the status.
   *
   * Every read carries a deadline, so a runtime that never answers ends in the error state rather
   * than in a spinner; and every read carries its number, so an answer the view no longer waits for
   * cannot replace a newer list. The read it supersedes is aborted on the way.
   */
  async function loadData() {
    if (!isMounted) return;
    const sequence = ++loadSequence;
    loadController?.abort();
    const controller = new AbortController();
    loadController = controller;
    isLoadingDevices = true;
    loadAttempts += 1;
    loadError = null;
    try {
      const signal = controller.signal;
      const [devList, status] = await Promise.all([
        apiClient.getDevices(signal),
        apiClient.getStatus(signal).catch(() => null),
      ]);
      if (!isMounted || sequence !== loadSequence) return;
      devices = Array.isArray(devList) ? devList : [];
      hasShownDevices = true;
      listenerKnown = Boolean(status?.web && typeof status.web.lan === 'boolean');
      if (listenerKnown) {
        lanListenerOn = status?.web?.lan === true;
      }
    } catch (err: unknown) {
      // The status read behind this one has nothing left to say: end it here.
      controller.abort();
      if (!isMounted || sequence !== loadSequence) return;
      loadError = errorCodeOf(err);
      devices = [];
      // Nothing says how the listener stands any more: the earlier answer is not a current one.
      listenerKnown = false;
    } finally {
      if (isMounted && sequence === loadSequence) {
        loadController = null;
        isLoadingDevices = false;
      }
    }
  }

  function isThisBrowser(device: PairedDevice): boolean {
    const currentId = apiClient.getDeviceId();
    if (currentId) return device.device_id === currentId;
    const currentLabel = $runtimeStore.deviceLabel || apiClient.getLabel();
    return Boolean(currentLabel && device.label === currentLabel);
  }

  function startRename(device: PairedDevice) {
    renamingId = device.device_id;
    renameValue = device.label;
    actionError = '';
  }

  async function saveRename(deviceId: string) {
    if (!renameValue.trim()) return;
    actionError = '';
    try {
      await apiClient.renameDevice(deviceId, renameValue.trim());
      devices = devices.map((d) =>
        d.device_id === deviceId ? { ...d, label: renameValue.trim() } : d,
      );
      renamingId = null;
    } catch {
      actionError = $t('web.settings.devices.rename_failed');
    }
  }

  function promptRevoke(target: PairedDevice | 'others') {
    actionError = '';
    pendingRevokeTarget = target;
  }

  function cancelRevoke() {
    pendingRevokeTarget = null;
  }

  /**
   * @param err the failure a revoke reported
   * @returns `true` when the request gave up on its deadline or never reached the runtime, so the
   *   runtime may still have acted
   */
  function revokeTimedOut(err: unknown): boolean {
    if (isClientError(err)) {
      return err.code === 'timeout' || err.code === 'network_offline';
    }
    const name = (err as Error)?.name;
    return name === 'TimeoutError' || name === 'AbortError';
  }

  async function confirmRevoke() {
    if (!pendingRevokeTarget || revoking) return;
    actionError = '';
    revoking = true;

    if (pendingRevokeTarget === 'others') {
      try {
        await apiClient.revokeOtherDevices();
        devices = devices.filter((d) => isThisBrowser(d));
        pendingRevokeTarget = null;
      } catch (err: unknown) {
        actionError = $t('web.settings.devices.revoke_failed');
        // The runtime may have applied the revoke before the deadline: read the list again.
        if (revokeTimedOut(err)) void loadData();
      } finally {
        revoking = false;
      }
      return;
    }

    const device = pendingRevokeTarget;
    const isCurrent = isThisBrowser(device);
    try {
      await apiClient.revokeDevice(device.device_id);
      devices = devices.filter((d) => d.device_id !== device.device_id);
      pendingRevokeTarget = null;
      if (isCurrent) {
        apiClient.clearAuth();
        runtimeStore.update((s) => ({
          ...s,
          isPaired: false,
          isRevoked: true,
          revokedReason: 'device_revoked',
          revokedInfo: {
            reason: 'device_revoked',
            revoked_by: $runtimeStore.deviceLabel,
            details: $t('web.settings.devices.revoked_this'),
          },
          currentRoute: 'revoked',
        }));
      }
    } catch (err: unknown) {
      actionError = $t('web.settings.devices.revoke_failed');
      if (revokeTimedOut(err)) void loadData();
    } finally {
      revoking = false;
    }
  }

  async function turnListenerOff() {
    actionError = '';
    try {
      await apiClient.turnOffLanListener();
      lanListenerOn = false;
      listenerNotice = $t('devices.listener_off.done');
    } catch {
      actionError = $t('web.settings.devices.listener_off_failed');
    }
  }

  /**
   * @param rawDate an ISO timestamp
   * @returns the day, in the interface language
   */
  function formatDate(rawDate?: string): string {
    return formatDay(rawDate, $currentLocale);
  }

  /**
   * @param rawDate an ISO timestamp
   * @returns its age in the unit that age has (`9 d ago`), or `since 14:02` for a moment of
   *   today, measured at the list's own clock
   */
  function formatRelative(rawDate?: string): string {
    return formatTime(rawDate, 'relative', $currentLocale, new Date(clock));
  }

  /**
   * @param rawDate an ISO timestamp
   * @returns the moment to the minute, in the interface language
   */
  function formatDateTime(rawDate?: string): string {
    return formatTime(rawDate, 'short', $currentLocale);
  }

  $: otherDevicesCount = devices.filter((d) => !isThisBrowser(d)).length;
  $: thisLabel = devices.find((d) => isThisBrowser(d))?.label ?? $runtimeStore.deviceLabel;

  /**
   * @param act a device act from the table
   * @returns the act as the bar draws it
   */
  function barAct(act: ActSpec) {
    return {
      id: act.id,
      label: $t(act.labelKey),
      advisory: act.advisoryKey ? $t(act.advisoryKey) : undefined,
      role: act.role,
      destructive: act.destructive,
    };
  }

  /**
   * @param device a paired device
   * @returns the acts the device's row offers (UX-P-04)
   */
  function deviceActs(device: PairedDevice) {
    const state = isThisBrowser(device) ? 'this_device' : 'other_device';
    return DEVICE_ACTS[state].filter((act) => act.id !== 'revoke_others').map(barAct);
  }

  /** The section's own act: revoking every other device, in its overflow. */
  $: sectionActs = DEVICE_ACTS.this_device.filter((act) => act.id === 'revoke_others').map(barAct);

  /**
   * @param device a paired device
   * @param id the act its row reported
   */
  function chooseDeviceAct(device: PairedDevice, id: string): void {
    if (id === 'rename') {
      startRename(device);
      return;
    }
    if (id === 'revoke') {
      promptRevoke(device);
    }
  }

  /** The confirmation the revoke asks for, with the name to type where the act needs one. */
  function revokeTitle(): string {
    if (pendingRevokeTarget === 'others') {
      return $t('devices.revoke.others_title', {
        count: otherDevicesCount,
        label: thisLabel ?? '',
      });
    }
    return $t('devices.revoke.title', { label: pendingRevokeTarget?.label ?? '' });
  }

  /** @returns what the revoke costs, in the words of the device it ends */
  function revokeConsequences(): string[] {
    if (pendingRevokeTarget === 'others') {
      return [$t('devices.revoke.explain', { label: thisLabel ?? '' })];
    }
    if (pendingRevokeTarget && isThisBrowser(pendingRevokeTarget)) {
      return [$t('devices.revoke.this_one')];
    }
    return [$t('devices.revoke.explain', { label: pendingRevokeTarget?.label ?? '' })];
  }

  // The title and the consequences name the target, so they follow `pendingRevokeTarget`.
  $: revokeTitleText = pendingRevokeTarget === null ? '' : revokeTitle();
  $: revokeConsequenceList = pendingRevokeTarget === null ? [] : revokeConsequences();

  // ---------------------------------------------------------------------------------------------
  // The machine's own settings (UC-ONB-10): the aspects, the documents mode, the cleanup's
  // durations, the remembered consents, the tokens and the providers.
  // ---------------------------------------------------------------------------------------------

  let isLoadingSettings = false;
  let notice = '';
  let error = '';
  let documentsMode = 'ask';
  let cleanupDays = 30;
  let cleanupRemindDays = 3;
  let conflict: ConflictState | null = null;
  let rotating: MachineCredentialStatus | null = null;
  let rotatingMode: 'rotate' | 'replace' = 'rotate';
  let rotatingSecret = '';
  let rotatingKind = '';
  let isSavingToken = false;
  let tokenNotice = '';
  let tokenError = '';

  /** Whether the last save of each form succeeded, so the form's own line can say _Saved_. */
  let documentsSaved = false;
  let cleanupSaved = false;
  /** The documents modes as the radio group draws them. */
  $: documentsOptions = ['copy', 'move', 'ask'].map((mode) => ({
    value: mode,
    label: $t(`web.settings.documents.${mode}`),
  }));

  $: settings = $runtimeStore.machineSettings;
  /** The values the machine holds, which the forms show until the operator changes one. */
  $: recordedDocuments = settings?.documents_mode || 'ask';
  $: recordedPlans = settings?.cleanup_durations?.plans_after_days ?? 30;
  $: recordedRemind = settings?.cleanup_durations?.remind_after_days ?? 3;
  $: documentsChanged = documentsMode !== recordedDocuments;
  $: plansChanged = Number(cleanupDays) !== Number(recordedPlans);
  $: remindChanged = Number(cleanupRemindDays) !== Number(recordedRemind);
  $: cleanupChanged = plansChanged || remindChanged;
  // The dependency is written out, so a settings record that arrives after the mount fills both
  // tabs: a helper call would hide it from the template's own update.
  $: setupAspects = (settings?.aspects ?? []).filter((aspect) =>
    SETUP_ASPECTS.includes(aspect.aspect),
  );
  $: credentialAspects = (settings?.aspects ?? []).filter((aspect) =>
    CREDENTIAL_ASPECTS.includes(aspect.aspect),
  );

  /** Loads the machine's settings for the panels that show them. */
  async function loadSettings(): Promise<void> {
    isLoadingSettings = true;
    const loaded = await loadMachineSettings();
    applyLocal(loaded);
    isLoadingSettings = false;
  }

  /**
   * @param aspects the aspect names a tab shows
   * @returns those aspects of the record, in the order the runtime reports them
   */
  function aspectsFor(aspects: ReadonlyArray<string>) {
    return (settings?.aspects ?? []).filter((aspect) => aspects.includes(aspect.aspect));
  }

  function applyLocal(value: MachineSettingsRecord | null): void {
    if (!value) return;
    documentsMode = value.documents_mode || 'ask';
    cleanupDays = value.cleanup_durations?.plans_after_days ?? 30;
    cleanupRemindDays = value.cleanup_durations?.remind_after_days ?? 3;
  }

  /**
   * Reads the cleanup durations a setting value carries.
   *
   * The setting travels as one record of days; an older client sends a bare number of days, which
   * is not a record and is shown as the value it is.
   *
   * @param value the setting value
   * @returns the record, or null for a value that is not one
   */
  function parseCleanupRecord(value: string): CleanupDurationsRecord | null {
    try {
      const parsed: unknown = JSON.parse(value);
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return null;
      const record: CleanupDurationsRecord = {};
      for (const [key, days] of Object.entries(parsed)) {
        if (typeof days === 'number' && Number.isFinite(days)) record[key] = days;
      }
      return record;
    } catch {
      return null;
    }
  }

  /**
   * Renders a cleanup record in the catalogue's words, one category each.
   *
   * A conflict compares two records; printing them as JSON shows the operator braces and keys
   * instead of the categories and days they chose. The record itself names the categories, so a
   * field added to it is shown rather than skipped.
   *
   * @param value the setting value
   * @returns the categories in words, or the value itself when it is not a record
   */
  function cleanupWords(value: string): string {
    const record = parseCleanupRecord(value);
    if (!record) return value;
    const known = CLEANUP_CATEGORY_ORDER;
    const keys = [
      ...known.filter((key) => record[key] !== undefined),
      ...Object.keys(record).filter((key) => !known.includes(key)),
    ];
    const words: string[] = [];
    for (const key of keys) {
      const days = record[key];
      if (days === undefined) continue;
      const kind = key.replace(/_after_days$/, '');
      if (kind === 'remind') {
        // Its label ends in "after": the sentence carries the case the bare noun would not.
        words.push($t('system.cleanup.durations.remind_at', { days }));
        continue;
      }
      const label = $t('system.cleanup.durations.kind', { kind });
      const amount = $t('system.cleanup.durations.value', {
        never: days === NEVER ? 'true' : 'false',
        days,
      });
      words.push(`${label}: ${amount}`);
    }
    return words.length > 0 ? words.join(' · ') : value;
  }

  /** @param value a cleanup record @returns the categories whose value differs from another record */
  function changedCleanupFields(
    value: CleanupDurationsRecord,
    shown: CleanupDurationsRecord | undefined,
  ): CleanupDurationsRecord {
    const changed: CleanupDurationsRecord = {};
    for (const [key, days] of Object.entries(value)) {
      if (shown?.[key] !== days) changed[key] = days;
    }
    return changed;
  }

  /**
   * Applies one setting, and opens the conflict dialogue instead of overwriting a value that
   * changed in another client.
   *
   * @param key the setting
   * @param value its new value, or null for a withdrawal
   * @param changedFields the cleanup categories the operator changed, if the value is a record
   */
  async function applySetting(
    key: string,
    value: string | null,
    changedFields?: CleanupDurationsRecord,
  ): Promise<boolean> {
    error = '';
    notice = '';
    conflict = null;
    const result = await updateMachineSetting(key, value);
    if (result?.conflict) {
      conflict = {
        key,
        currentValue: result.current_value ?? '',
        wantedValue: value ?? '',
        changedFields,
      };
      return false;
    }
    if (!result) {
      error = $t('web.settings.update_failed');
      return false;
    }
    notice = $t('web.settings.saved');
    return true;
  }

  async function saveDocumentsMode(): Promise<void> {
    const applied = await applySetting('documents_mode', documentsMode);
    documentsSaved = applied;
  }

  /** Drops the documents change and shows the value the machine holds again. */
  function resetDocumentsMode(): void {
    documentsMode = recordedDocuments;
    documentsSaved = false;
  }

  /**
   * Sends the cleanup durations the page shows: how long work is kept, and the reminder interval.
   *
   * The setting travels as one record, so both figures go in it; the categories the page does not
   * show keep the values the runtime holds, so saving the visible ones never resets them. A value
   * outside the contract (`doc/design/shared/cleanup.adoc`: at least seven days, a reminder of at
   * least one) writes nothing rather than a default the runtime would fill in.
   */
  async function saveCleanup(): Promise<void> {
    const days = Number(cleanupDays);
    const remind = Number(cleanupRemindDays);
    if (!Number.isInteger(days) || (days !== NEVER && days < MINIMUM_RETENTION_DAYS)) {
      // The contract's own sentence, with its minimum: a generic failure hides what to change.
      error = $t('system.cleanup.durations.minimum', {
        min: MINIMUM_RETENTION_DAYS,
        kind: 'plans',
      });
      notice = '';
      return;
    }
    if (!Number.isInteger(remind) || remind < MINIMUM_REMIND_DAYS) {
      error = $t('system.cleanup.durations.minimum', {
        min: MINIMUM_REMIND_DAYS,
        kind: 'remind',
      });
      notice = '';
      return;
    }
    const recorded = settings?.cleanup_durations;
    // The saved record starts from the one the runtime holds, so a category this page does not show
    // keeps its value instead of being rebuilt from a fixed list of keys.
    const durations: CleanupDurationsRecord = { ...(recorded ?? {}), ...overriddenDurations(days, remind) };
    // Only what the operator changed travels again after a conflict; the other categories keep
    // the value the other client recorded.
    const applied = await applySetting(
      'cleanup_durations',
      JSON.stringify(durations),
      changedCleanupFields(durations, recorded),
    );
    cleanupSaved = applied;
  }

  /**
   * @param days the retention the page shows
   * @param remind the reminder the page shows
   * @returns the two categories the page edits, over whatever the record carries
   */
  function overriddenDurations(days: number, remind: number): CleanupDurationsRecord {
    return { plans_after_days: days, remind_after_days: remind };
  }

  /** Drops the cleanup change and shows the durations the machine holds again. */
  function resetCleanup(): void {
    cleanupDays = recordedPlans;
    cleanupRemindDays = recordedRemind;
    cleanupSaved = false;
  }

  /** Applies the value this page wanted, on the revision the other client left behind. */
  async function applyConflictAgain(): Promise<void> {
    if (!conflict) return;
    const key = conflict.key;
    const changed = conflict.changedFields;
    let wanted = conflict.wantedValue;
    if (key === 'cleanup_durations' && changed) {
      const current = parseCleanupRecord(conflict.currentValue) ?? {};
      wanted = JSON.stringify({ ...current, ...changed });
    }
    conflict = null;
    // A second conflict keeps the changed set: the categories the operator did not touch must
    // follow the record the other client left again, not the one this resend was built from.
    const applied = await applySetting(key, wanted, changed);
    if (applied) {
      // The fields show the record the runtime now holds, so the next save does not resend a value
      // this resend deliberately kept from the other client.
      applyLocal($runtimeStore.machineSettings ?? null);
    }
  }

  /** Keeps the other client's value and closes the dialogue, without writing anything. */
  async function keepCurrentValue(): Promise<void> {
    conflict = null;
    const reloaded = await loadMachineSettings();
    if (!reloaded) {
      error = $t('web.settings.update_failed');
      return;
    }
    applyLocal(reloaded);
  }

  async function withdraw(consent: ConsentRecord): Promise<void> {
    error = '';
    notice = '';
    const result = await withdrawConsent(consent.kind);
    if (result?.conflict) {
      conflict = {
        key: `consent:${consent.kind}`,
        currentValue: result.current_value ?? '',
        wantedValue: '',
      };
      return;
    }
    if (!result) {
      error = $t('web.settings.update_failed');
      return;
    }
    notice = $t('web.settings.consents.withdrawn');
  }

  function startRotate(credential: MachineCredentialStatus, mode: 'rotate' | 'replace'): void {
    rotating = credential;
    rotatingMode = mode;
    rotatingSecret = '';
    rotatingKind = credential.kind;
    tokenNotice = '';
    tokenError = '';
  }

  function closeRotate(): void {
    rotating = null;
    rotatingSecret = '';
  }

  async function saveToken(): Promise<void> {
    if (!rotating || !rotatingSecret || isSavingToken) return;
    isSavingToken = true;
    tokenError = '';
    // A rotation keeps the recorded kind; a replacement records the chosen one.
    const kind = rotatingMode === 'replace' ? rotatingKind : null;
    const result = await storeCredential(rotating.key, rotatingSecret, kind);
    isSavingToken = false;
    if (result.ok) {
      tokenNotice = $t('web.settings.credentials.rotated', { key: rotating.key });
      rotatingSecret = '';
      rotating = null;
      if (result.kindFailed || result.refreshFailed) {
        tokenError = $t('web.settings.update_failed');
      }
    } else {
      tokenError = $t('web.settings.credentials.failed', {
        cause: result.cause || $t('web.settings.update_failed'),
      });
    }
  }

  function stepName(aspect: string): string {
    return $t(`onboard.step.${aspectKey(aspect)}`);
  }

  function aspectKey(aspect: string): string {
    const map: Record<string, string> = {
      language: 'language',
      harness: 'harnesses',
      harnesses: 'harnesses',
      catalogue: 'models',
      models: 'models',
      roles: 'roles',
      store: 'credentials',
      credentials: 'credentials',
      providers: 'providers',
      defaults: 'defaults',
      plans: 'plans',
    };
    return map[aspect] ?? aspect;
  }

  /**
   * @param credential a token's status
   * @returns the provider's name from the catalogue, or the name the runtime records
   */
  function credentialProvider(credential: MachineCredentialStatus): string {
    const key = `web.settings.credentials.provider.${credential.key}`;
    return hasMessage(key) ? $t(key) : credential.provider;
  }

  function expiryLabel(credential: MachineCredentialStatus): string {
    if (credential.expired) {
      const held = credential.held_plan ? 1 : 0;
      return $t('web.settings.providers.expired', { count: held });
    }
    if (credential.expires_in_days !== undefined) {
      return $t('web.settings.providers.expires_soon', { days: credential.expires_in_days });
    }
    return $t('web.settings.providers.no_expiry');
  }

  /**
   * The place a consent was given in.
   *
   * The runtime names the console `terminal`; a device carries the label the operator gave it, so
   * only that value is a name of its own and the console token reads as the catalogue's word.
   *
   * @param consent the remembered consent
   * @returns the word or the device label
   */
  function consentChannel(consent: { channel?: string | null }): string {
    if (!consent.channel || consent.channel === 'terminal') {
      return $t('web.settings.consents.where_terminal');
    }
    return consent.channel;
  }
</script>

<div class="settings-view" data-testid="settings-view">
  <div class="settings-view__header">
    <h1 class="settings-view__title">{$t('web.settings.title')}</h1>
    <div class="settings-view__actions">
      <Button
        variant="secondary"
        on:click={() => navigateTo('designer')}
        data-testid="open-designer-btn"
      >
        {$t('designer.view.title')}
      </Button>
      <Button
        variant="secondary"
        on:click={() => {
          if (!goBack()) {
            navigateTo('projects');
          }
        }}
        data-testid="settings-back-button"
      >
        {$t('web.common.back')}
      </Button>
    </div>
  </div>

  <Tabs {tabs} active={activeTab} label={$t('web.settings.tabs.label')} onSelect={chooseTab} />

  {#if error}
    <p class="error-text" role="alert">{error}</p>
  {/if}
  {#if notice}
    <p class="notice-text">{notice}</p>
  {/if}

  {#if conflict}
    <div class="conflict" role="alert" data-testid="conflict-dialog">
      <h2 class="conflict__title">{$t('web.settings.conflict.title')}</h2>
      <p class="conflict__line">
        {$t('web.settings.conflict.current', { value: cleanupWords(conflict.currentValue) })}
      </p>
      <p class="conflict__line">
        {$t('web.settings.conflict.yours', { value: cleanupWords(conflict.wantedValue) })}
      </p>
      <div class="conflict__actions">
        <Button variant="ghost" on:click={keepCurrentValue} data-testid="conflict-keep">
          {$t('web.settings.conflict.keep')}
        </Button>
        <Button variant="primary" on:click={applyConflictAgain} data-testid="conflict-apply">
          {$t('web.settings.conflict.apply_again')}
        </Button>
      </div>
    </div>
  {/if}

  {#if activeTab === 'setup'}
    <div
      class="settings-panel"
      id="settings-panel-setup"
      role="tabpanel"
      aria-label={$t('web.settings.tab.setup')}
      data-testid="settings-panel-setup"
      tabindex="0"
    >
      {#if isLoadingSettings}
        <p class="loading-text">{$t('web.settings.devices.loading')}</p>
      {:else if !settings}
        <p class="empty-text">{$t('web.settings.aspects.empty')}</p>
      {:else}
        <section class="settings-section">
          <h2 class="settings-section__title">{$t('web.settings.aspects.title')}</h2>
          <ul class="aspect-list" data-testid="aspect-list">
            {#each setupAspects as aspect}
              <li class="aspect-row" data-aspect={aspect.aspect}>
                <span class="aspect-row__name">
                  {aspect.step}. {stepName(aspect.aspect)}
                </span>
                <span class="aspect-row__value">{aspect.summary}</span>
                <Button
                  variant="ghost"
                  on:click={() => openOnboardingStep(aspect.step)}
                  aria-label={`${$t('web.settings.aspects.change')} ${stepName(aspect.aspect)}`}
                  data-testid={`change-aspect-${aspect.aspect}`}
                >
                  {$t('web.settings.aspects.change')}
                </Button>
              </li>
            {/each}
          </ul>
          <p class="hint-text">{$t('web.settings.aspects.project_note')}</p>
        </section>

        <section class="settings-section" data-testid="documents-section">
          <RadioGroup
            legend={$t('web.settings.documents.title')}
            name="documents_mode"
            bind:value={documentsMode}
            options={documentsOptions}
            testid="documents-choice"
          />
          <p class="hint-text">{$t('web.settings.documents.loopback_only')}</p>
          {#if documentsChanged}
            <p class="changed-mark" data-testid="documents-changed">
              {$t('web.settings.form.changed')}
            </p>
          {/if}
          <div class="form-footer">
            <div data-testid="documents-form-status">
              <FormStatus
                message={documentsChanged
                  ? ''
                  : documentsSaved
                    ? $t('web.settings.saved')
                    : $t('web.settings.form.unchanged')}
                tone={documentsSaved && !documentsChanged ? 'success' : 'neutral'}
              />
            </div>
            <Button
              variant="ghost"
              disabled={!documentsChanged}
              on:click={resetDocumentsMode}
              data-testid="documents-reset"
            >
              {$t('web.settings.form.reset')}
            </Button>
            <Button
              variant="primary"
              disabled={!documentsChanged}
              on:click={saveDocumentsMode}
              data-testid="save-documents"
            >
              {$t('web.common.save')}
            </Button>
          </div>
        </section>
      {/if}
    </div>
  {:else if activeTab === 'access'}
    <div
      class="settings-panel"
      id="settings-panel-access"
      role="tabpanel"
      aria-label={$t('web.settings.tab.access')}
      data-testid="settings-panel-access"
      tabindex="0"
    >
      <section class="settings-section" data-testid="paired-devices-section">
        <div class="section-header">
          <div>
            <h2 class="settings-section__title">{$t('devices.list.title')}</h2>
            <p class="settings-section__subtitle">{$t('devices.list.subtitle')}</p>
          </div>
          {#if otherDevicesCount > 0}
            <div data-testid="devices-section-acts">
              <ActionBar acts={sectionActs} onAct={() => promptRevoke('others')} />
            </div>
          {/if}
        </div>

        <div class="devices-card">
          {#if actionError}
            <p class="error-text" role="alert">{actionError}</p>
          {/if}
          {#if isLoadingDevices}
            <Skeleton shape="card" rows={3} label={$t('web.settings.devices.loading')} />
          {:else if loadError !== null}
            <ErrorState
              code={loadError}
              actionLabel={$t('common.action.retry')}
              onAct={() => loadData()}
              titleLevel={3}
              announce={hasShownDevices || loadAttempts > 1}
            />
          {:else if devices.length === 0}
            <p class="empty-text" data-testid="empty-devices">{$t('devices.list.empty')}</p>
          {:else}
            <table class="devices-table" data-testid="devices-table">
              <thead>
                <tr>
                  <th>{$t('web.settings.devices.column_device')}</th>
                  <th>{$t('web.settings.devices.column_paired')}</th>
                  <th>{$t('web.settings.devices.column_last_used')}</th>
                  <th>{$t('web.settings.devices.column_action')}</th>
                </tr>
              </thead>
              <tbody>
                {#each devices as device}
                  <tr data-testid={`device-row-${device.device_id}`}>
                    <td>
                      {#if renamingId === device.device_id}
                        <div class="rename-inline">
                          <input
                            type="text"
                            bind:value={renameValue}
                            class="rename-input"
                            data-testid={`rename-input-${device.device_id}`}
                            aria-label={$t('web.settings.devices.new_label')}
                          />
                          <Button
                            variant="primary"
                            on:click={() => saveRename(device.device_id)}
                            data-testid={`save-rename-${device.device_id}`}
                          >
                            {$t('web.common.save')}
                          </Button>
                          <Button variant="ghost" on:click={() => (renamingId = null)}>
                            {$t('web.settings.devices.cancel')}
                          </Button>
                        </div>
                      {:else}
                        <strong class="device-label">{device.label}</strong>
                        {#if isThisBrowser(device)}
                          <span class="badge-current" data-testid="this-browser-badge">
                            {$t('devices.list.this_one')}
                          </span>
                        {/if}
                      {/if}
                    </td>
                    <td class="text-secondary">
                      {$t('devices.list.paired', { date: formatDate(device.created_at) })}
                      {#if device.paired_from}
                        · {$t('devices.list.from', { from: device.paired_from })}
                      {/if}
                    </td>
                    <td class="text-secondary" title={formatDateTime(device.last_used_at)}>
                      <span class="visually-hidden">{formatDateTime(device.last_used_at)}</span>
                      {$t('devices.list.last_used', { time: formatRelative(device.last_used_at) })}
                    </td>
                    <td>
                      <ActionBar
                        acts={deviceActs(device)}
                        onAct={(id) => chooseDeviceAct(device, id)}
                      />
                    </td>
                  </tr>
                {/each}
              </tbody>
            </table>
          {/if}
        </div>

        <p class="terminal-note">{$t('devices.list.terminal_note')}</p>

        <div class="listener-bar" data-testid="listener-bar">
          <span class="listener-status" data-testid="listener-status">
            {$t(listenerKnown ? 'devices.list.listener' : 'devices.list.listener_unknown', {
              state: lanListenerOn ? 'on' : 'off',
            })}.
          </span>
          <div class="listener-actions">
            <Button
              variant="ghost"
              on:click={() => (lanModalOpen = true)}
              data-testid="lan-details-button"
            >
              {$t('entry.lan.title')}
            </Button>
            {#if listenerKnown && lanListenerOn}
              <Button
                variant="secondary"
                on:click={turnListenerOff}
                data-testid="listener-off-button"
              >
                {$t('devices.act.listener_off')}
              </Button>
            {/if}
          </div>
        </div>
        {#if listenerNotice}
          <p class="notice-text" data-testid="listener-notice">{listenerNotice}</p>
        {/if}
      </section>
    </div>
  {:else if activeTab === 'credentials'}
    <div
      class="settings-panel"
      id="settings-panel-credentials"
      role="tabpanel"
      aria-label={$t('web.settings.tab.credentials')}
      data-testid="settings-panel-credentials"
      tabindex="0"
    >
      {#if isLoadingSettings}
        <p class="loading-text">{$t('web.settings.devices.loading')}</p>
      {:else if !settings}
        <p class="empty-text">{$t('web.settings.aspects.empty')}</p>
      {:else}
        <section class="settings-section">
          <h2 class="settings-section__title">{$t('web.settings.aspects.title')}</h2>
          <ul class="aspect-list" data-testid="aspect-list-credentials">
            {#each credentialAspects as aspect}
              <li class="aspect-row" data-aspect={aspect.aspect}>
                <span class="aspect-row__name">
                  {aspect.step}. {stepName(aspect.aspect)}
                </span>
                <span class="aspect-row__value">{aspect.summary}</span>
                <Button
                  variant="ghost"
                  on:click={() => openOnboardingStep(aspect.step)}
                  aria-label={`${$t('web.settings.aspects.change')} ${stepName(aspect.aspect)}`}
                  data-testid={`change-aspect-${aspect.aspect}`}
                >
                  {$t('web.settings.aspects.change')}
                </Button>
              </li>
            {/each}
          </ul>
          <p class="hint-text">{$t('web.settings.aspects.project_note')}</p>
        </section>

        <section class="settings-section" data-testid="credentials-section">
          <h2 class="settings-section__title">{$t('web.settings.credentials.title')}</h2>
          {#if settings.credentials.length === 0}
            <p class="empty-text">{$t('web.settings.credentials.none')}</p>
          {:else}
            <ul class="credential-list">
              {#each settings.credentials as credential}
                <li class="credential-row" data-credential={credential.key}>
                  <span class="credential-row__main">
                    {credentialProvider(credential)} · {credential.kind}
                  </span>
                  <span class="credential-row__expiry" class:warn={credential.expired}>
                    {expiryLabel(credential)}
                  </span>
                  <Button
                    variant="ghost"
                    on:click={() => startRotate(credential, 'rotate')}
                    data-testid={`rotate-${credential.key}`}
                  >
                    {$t('web.settings.credentials.rotate')}
                  </Button>
                  <Button
                    variant="ghost"
                    on:click={() => startRotate(credential, 'replace')}
                    data-testid={`replace-${credential.key}`}
                  >
                    {$t('web.settings.credentials.replace')}
                  </Button>
                </li>
              {/each}
            </ul>
          {/if}

          {#if tokenNotice}
            <p class="notice-text" data-testid="token-notice">{tokenNotice}</p>
          {/if}
          {#if tokenError}
            <p class="error-text" role="alert">{tokenError}</p>
          {/if}
        </section>

        <section class="settings-section">
          <h2 class="settings-section__title">{$t('onboard.step.credentials')}</h2>
          <CredentialEntry
            credentialKey="github"
            label={$t('web.settings.credentials.provider.github')}
          />
        </section>
      {/if}
    </div>
  {:else if activeTab === 'consents'}
    <div
      class="settings-panel"
      id="settings-panel-consents"
      role="tabpanel"
      aria-label={$t('web.settings.tab.consents')}
      data-testid="settings-panel-consents"
      tabindex="0"
    >
      {#if isLoadingSettings}
        <p class="loading-text">{$t('web.settings.devices.loading')}</p>
      {:else if !settings}
        <p class="empty-text">{$t('web.settings.aspects.empty')}</p>
      {:else}
        <section class="settings-section" data-testid="consents-section">
          <h2 class="settings-section__title">{$t('web.settings.consents.title')}</h2>
          {#if settings.consents.length === 0}
            <p class="empty-text" data-testid="no-consents">{$t('web.settings.consents.none')}</p>
          {:else}
            <ul class="consent-list">
              {#each settings.consents as consent}
                <li class="consent-row" data-consent={consent.kind}>
                  <span>{$t(`common.consent.kind.${consent.kind}`)}</span>
                  <span class="consent-row__where">
                    {formatTime(consent.granted_at, 'short', $currentLocale)} · {consentChannel(
                      consent,
                    )}
                  </span>
                  {#if consent.project}
                    <span class="consent-row__where">
                      {consent.project}
                      {#if consent.script_count !== undefined}
                        · {$t('web.settings.consents.scripts', { count: consent.script_count })}
                      {/if}
                    </span>
                  {/if}
                  <Button
                    variant="ghost"
                    on:click={() => withdraw(consent)}
                    data-testid={`withdraw-${consent.kind}`}
                  >
                    {$t('web.settings.consents.withdraw')}
                  </Button>
                </li>
              {/each}
            </ul>
          {/if}
        </section>
      {/if}
    </div>
  {:else}
    <div
      class="settings-panel"
      id="settings-panel-retention"
      role="tabpanel"
      aria-label={$t('web.settings.tab.retention')}
      data-testid="settings-panel-retention"
      tabindex="0"
    >
      {#if isLoadingSettings}
        <p class="loading-text">{$t('web.settings.devices.loading')}</p>
      {:else if !settings}
        <p class="empty-text">{$t('web.settings.aspects.empty')}</p>
      {:else}
        <section class="settings-section" data-testid="cleanup-section">
          <h2 class="settings-section__title">{$t('web.settings.cleanup.title')}</h2>
          <div class="field-row">
            <label class="field-label" for="cleanup-days">
              {$t('web.settings.cleanup.keep')}
            </label>
            <input
              id="cleanup-days"
              type="number"
              bind:value={cleanupDays}
              data-testid="cleanup-days"
            />
            <span class="field-unit">{$t('web.settings.cleanup.days')}</span>
            {#if plansChanged}
              <span class="changed-mark" data-testid="cleanup-changed-plans">
                {$t('web.settings.form.changed')}
              </span>
            {/if}
          </div>
          <div class="field-row">
            <label class="field-label" for="cleanup-remind-days">
              {$t('web.settings.cleanup.remind')}
            </label>
            <input
              id="cleanup-remind-days"
              type="number"
              min="1"
              bind:value={cleanupRemindDays}
              data-testid="cleanup-remind-days"
            />
            <span class="field-unit">{$t('web.settings.cleanup.days_after')}</span>
            {#if remindChanged}
              <span class="changed-mark" data-testid="cleanup-changed-remind">
                {$t('web.settings.form.changed')}
              </span>
            {/if}
          </div>
          <p class="hint-text">{$t('web.settings.cleanup.hint')}</p>
          <div class="form-footer">
            <div data-testid="cleanup-form-status">
              <FormStatus
                message={cleanupChanged
                  ? ''
                  : cleanupSaved
                    ? $t('web.settings.saved')
                    : $t('web.settings.form.unchanged')}
                tone={cleanupSaved && !cleanupChanged ? 'success' : 'neutral'}
              />
            </div>
            <Button
              variant="ghost"
              disabled={!cleanupChanged}
              on:click={resetCleanup}
              data-testid="cleanup-reset"
            >
              {$t('web.settings.form.reset')}
            </Button>
            <Button
              variant="primary"
              disabled={!cleanupChanged}
              on:click={saveCleanup}
              data-testid="save-cleanup"
            >
              {$t('web.settings.cleanup.save')}
            </Button>
          </div>
        </section>
      {/if}
    </div>
  {/if}

  {#if lanModalOpen}
    <LanListenerModal onClose={() => (lanModalOpen = false)} onChanged={loadData} />
  {/if}

  <ConfirmDialog
    open={pendingRevokeTarget !== null}
    title={revokeTitleText}
    consequences={revokeConsequenceList}
    actionLabel={$t('devices.revoke.action')}
    cancelLabel={$t('devices.revoke.keep')}
    typedName={pendingRevokeTarget === 'others' ? (thisLabel ?? '') : undefined}
    busy={revoking}
    onConfirm={confirmRevoke}
    onClose={cancelRevoke}
  />

  {#if rotating}
    <div class="modal-backdrop" role="dialog" aria-modal="true" data-testid="rotate-modal">
      <div class="modal-card">
        <h2 class="modal-title">
          {$t(
            rotatingMode === 'replace'
              ? 'web.settings.credentials.replace_title'
              : 'web.settings.credentials.rotate_title',
            { key: credentialProvider(rotating) },
          )}
        </h2>
        <dl class="rotate-details">
          <div class="rotate-details__row">
            <dt>
              {$t('web.settings.credentials.entry', {
                key: credentialProvider(rotating),
                provider: rotating.provider,
              })}
            </dt>
            <dd>
              {$t(
                rotatingMode === 'replace'
                  ? 'web.settings.credentials.replace_explain'
                  : 'web.settings.credentials.rotate_explain',
              )}
            </dd>
          </div>
          {#if rotatingMode === 'replace'}
            <div class="rotate-details__row">
              <dt>
                <label class="rotate-label" for="replace-kind">
                  {$t('web.settings.credentials.kind')}
                </label>
              </dt>
              <dd>
                <input
                  id="replace-kind"
                  type="text"
                  class="rotate-input"
                  bind:value={rotatingKind}
                  data-testid="replace-kind"
                />
              </dd>
            </div>
          {:else}
            <div class="rotate-details__row">
              <dt>{$t('web.settings.credentials.kind')}</dt>
              <dd>{rotating.kind}</dd>
            </div>
          {/if}
          <div class="rotate-details__row">
            <dt>
              {$t('web.settings.credentials.now', {
                kind: rotating.kind,
                expiry: expiryLabel(rotating),
              })}
            </dt>
            <dd></dd>
          </div>
        </dl>

        <label class="rotate-label" for="rotate-secret">
          {$t('web.settings.credentials.secret')}
        </label>
        <input
          id="rotate-secret"
          type="password"
          class="rotate-input"
          autocomplete="off"
          bind:value={rotatingSecret}
          data-testid="rotate-secret"
        />
        <p class="hint-text">{$t('web.settings.credentials.secret_hint')}</p>
        <p class="hint-text">
          {$t('web.settings.credentials.lan', {
            command: `pm-operator credentials rotate ${rotating.key}`,
          })}
        </p>

        <div class="modal-actions">
          <Button variant="ghost" on:click={closeRotate} data-testid="rotate-cancel">
            {$t('web.settings.back')}
          </Button>
          <Button
            variant="primary"
            disabled={!rotatingSecret}
            loading={isSavingToken}
            on:click={saveToken}
            data-testid="rotate-save"
          >
            {$t('web.settings.credentials.store')}
          </Button>
        </div>
      </div>
    </div>
  {/if}
</div>

<style>
  .visually-hidden {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
    border: 0;
  }

  .settings-view {
    max-width: 64rem;
    margin: 0 auto;
    padding: var(--space-8) var(--space-4);
  }

  .settings-view__header {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-3);
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--space-6);
  }

  .settings-view__title {
    font-size: var(--text-2xl);
    font-weight: 700;
    color: var(--color-text-primary);
  }

  .settings-view__actions {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .settings-panel {
    margin-top: var(--space-6);
  }

  .settings-panel:focus-visible {
    outline: none;
    box-shadow: var(--focus-ring);
  }

  .settings-section {
    margin-bottom: var(--space-8);
  }

  .section-header {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-3);
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--space-4);
  }

  .settings-section__title {
    font-size: var(--text-lg);
    font-weight: 600;
    color: var(--color-text-primary);
    margin-bottom: var(--space-3);
  }

  .settings-section__subtitle {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .aspect-list,
  .consent-list,
  .credential-list {
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .aspect-row,
  .consent-row,
  .credential-row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-3);
    padding: var(--space-3) var(--space-4);
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
  }

  .aspect-row__name {
    font-weight: 600;
    min-width: 9rem;
  }

  .aspect-row__value,
  .consent-row__where,
  .credential-row__main {
    flex: 1 1 14rem;
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
  }

  .credential-row__expiry {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .devices-card {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    overflow-x: auto;
  }

  .devices-table {
    width: 100%;
    border-collapse: collapse;
    font-size: var(--text-sm);
  }

  .devices-table th,
  .devices-table td {
    padding: var(--space-3) var(--space-4);
    text-align: left;
    border-bottom: 1px solid var(--color-border-subtle);
  }

  .devices-table th {
    background-color: var(--color-bg-subtle);
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .device-label {
    color: var(--color-text-primary);
  }

  .text-secondary {
    color: var(--color-text-secondary);
  }

  .badge-current {
    display: inline-block;
    padding: 2px 6px;
    font-size: var(--text-xs);
    background-color: var(--color-primary-subtle);
    color: var(--color-primary);
    border-radius: var(--radius-sm);
    margin-left: var(--space-2);
    font-weight: 500;
  }

  .rename-inline {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
  }

  .rename-input {
    padding: var(--space-1) var(--space-2);
    font-size: var(--text-sm);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-surface);
    color: var(--color-text-primary);
  }

  .terminal-note {
    margin-top: var(--space-2);
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .listener-bar {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-3);
    align-items: center;
    justify-content: space-between;
    margin-top: var(--space-4);
    padding: var(--space-3) var(--space-4);
    background-color: var(--color-bg-subtle);
    border-radius: var(--radius-md);
    border: 1px solid var(--color-border-subtle);
  }

  .listener-status {
    font-size: var(--text-sm);
    color: var(--color-text-primary);
  }

  .listener-actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
  }

  .notice-text {
    font-size: var(--text-sm);
    color: var(--color-primary);
    margin-top: var(--space-2);
  }

  .error-text {
    color: var(--color-danger);
    font-size: var(--text-sm);
    padding: var(--space-3) var(--space-4);
    margin: 0;
  }

  .empty-text,
  .loading-text {
    padding: var(--space-6) var(--space-4);
    text-align: center;
    color: var(--color-text-secondary);
  }

  .field-row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
    margin-bottom: var(--space-3);
  }

  .field-label {
    font-size: var(--text-sm);
    font-weight: var(--weight-medium);
    color: var(--color-text-primary);
  }

  .field-row input[type='number'] {
    width: 5rem;
    min-height: var(--control-height);
    padding: var(--space-1) var(--space-2);
    border: 1px solid var(--control-border);
    border-radius: var(--control-radius);
    background-color: var(--control-bg);
    color: var(--color-text-primary);
  }

  .field-unit {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .changed-mark {
    font-size: var(--text-xs);
    font-weight: var(--weight-medium);
    color: var(--color-primary);
    background-color: var(--color-primary-subtle);
    border-radius: var(--radius-sm);
    padding: 1px var(--space-2);
    margin: 0;
  }

  .form-footer {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: flex-end;
    gap: var(--space-2);
    margin-top: var(--space-3);
  }

  .form-footer > :first-child {
    margin-inline-end: auto;
  }

  .hint-text {
    margin-top: var(--space-2);
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .warn {
    color: var(--color-warning);
  }

  .conflict {
    padding: var(--space-4);
    margin-top: var(--space-4);
    margin-bottom: var(--space-6);
    background-color: var(--color-warning-subtle);
    border: 1px solid var(--color-warning-border);
    border-radius: var(--radius-md);
  }

  .conflict__title {
    font-size: var(--text-base);
    font-weight: 600;
    color: var(--color-warning);
    margin-bottom: var(--space-2);
  }

  .conflict__line {
    font-size: var(--text-sm);
    color: var(--color-text-primary);
  }

  .conflict__actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: var(--space-3);
    margin-top: var(--space-3);
  }

  .modal-backdrop {
    position: fixed;
    inset: 0;
    background-color: var(--color-overlay);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: var(--space-4);
    z-index: 100;
  }

  .modal-card {
    background-color: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    padding: var(--space-6);
    max-width: 34rem;
    width: 100%;
    max-height: 90vh;
    overflow-y: auto;
    box-shadow: var(--shadow-md);
  }

  .modal-title {
    font-size: var(--text-lg);
    font-weight: 600;
    color: var(--color-text-primary);
    margin-bottom: var(--space-4);
  }

  .rotate-details {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    margin-bottom: var(--space-4);
  }

  .rotate-details__row {
    display: grid;
    grid-template-columns: 1fr;
    gap: var(--space-1);
    font-size: var(--text-sm);
  }

  .rotate-details__row dt {
    color: var(--color-text-secondary);
    font-weight: 600;
  }

  .rotate-details__row dd {
    color: var(--color-text-primary);
  }

  .rotate-label {
    display: block;
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--color-text-secondary);
    margin-bottom: var(--space-1);
  }

  .rotate-input {
    width: 100%;
    padding: var(--space-2) var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-app);
    color: var(--color-text-primary);
  }

  .modal-actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: var(--space-3);
    margin-top: var(--space-4);
  }

  @media (max-width: 768px) {
    .settings-view {
      padding: var(--space-4) var(--space-3);
    }

    .aspect-row__name {
      min-width: 0;
    }

    .devices-table th:nth-child(2),
    .devices-table td:nth-child(2),
    .devices-table th:nth-child(3),
    .devices-table td:nth-child(3) {
      display: none;
    }

    .conflict__actions,
    .modal-actions {
      flex-direction: column-reverse;
      align-items: stretch;
    }
  }
</style>
