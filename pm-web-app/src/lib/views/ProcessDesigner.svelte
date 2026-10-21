<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { t } from '../i18n';
  import Button from '../components/Button.svelte';
  import BlockPalette from '../components/BlockPalette.svelte';
  import WorkflowValidationResults from '../components/WorkflowValidationResults.svelte';
  import {
    apiClient,
    type BuildingBlock,
    type WorkflowCatalogue,
    type WorkflowStructure,
    type WorkflowUnit,
    type WorkflowValidationResult,
  } from '../api/client';
  import { runtimeStore, navigateTo } from '../stores/runtime';
  import { diffLines, type LineDiff } from '../utils/lineDiff';
  import {
    editFrontmatter,
    parseUnit,
    touchFrontmatter,
    unitNameOf,
    type FrontmatterFields,
  } from '../utils/frontmatter';

  /** The key of the draft this device keeps between visits. */
  const DRAFT_KEY = 'pm_designer_draft';
  /** The key of the author this device remembers. */
  const AUTHOR_KEY = 'pm_designer_author';
  /** The largest unit text the intake reads from a chosen file. */
  const FILE_LIMIT_BYTES = 1_048_576;

  interface Draft {
    text: string;
    unitName: string;
    unitKind: string;
    projectId: string | null;
    savedAt: string;
  }

  type IntakeMode = 'installed' | 'upload' | 'paste' | 'template';
  type PlaceStage = 'idle' | 'target' | 'diff' | 'progress' | 'failed' | 'conflict' | 'done';

  let projectId = $state<string | null>(null);
  /** The project the open units were loaded for, to restore on a cancelled change. */
  let loadedProjectId = $state<string | null>(null);
  let projects = $state<Array<{ project_id: string; display_name: string }>>([]);
  let units = $state<WorkflowUnit[]>([]);
  let catalogue = $state<WorkflowCatalogue | null>(null);
  let selectedBlock = $state<BuildingBlock | null>(null);

  let unitName = $state('');
  let unitKind = $state('subworkflow');
  let text = $state('');
  let digest = $state<string | null>(null);

  let result = $state<WorkflowValidationResult | null>(null);
  let structure = $state<WorkflowStructure | null>(null);
  let outdated = $state(false);
  let isValidating = $state(false);
  let isPlacing = $state(false);
  let notice = $state('');
  let placeMessage = $state('');
  let previewOpen = $state(false);

  // Intake
  let intakeMode = $state<IntakeMode>('installed');
  let pasteText = $state('');
  let intakeError = $state('');

  // Draft and leave guard
  let dirty = $state(false);
  let touched = $state(false);
  let signatureDropped = $state(false);
  let pendingAction = $state<(() => void) | null>(null);
  let savedAuthor = $state('');

  // Editor and structure
  let textareaElement = $state<HTMLTextAreaElement | null>(null);
  let gutterElement = $state<HTMLDivElement | null>(null);
  let cursorLine = $state(1);
  let activeStateId = $state<string | null>(null);
  let isPhone = $state(false);
  let mediaQuery: MediaQueryList | null = null;

  // Placing
  let placeStage = $state<PlaceStage>('idle');
  let placeTargetProject = $state<string | null>(null);
  let placeAgainst = $state('');
  let placeDiff = $state<LineDiff | null>(null);
  let conflictDiff = $state<LineDiff | null>(null);
  let landingSteps = $state<string[]>([]);
  let landingError = $state('');

  let projectLabel = $derived(projectId ?? '');
  let checkedAgainst = $derived(
    projectId
      ? $t('designer.checked_against', { project: displayNameFor(projectId) })
      : $t('designer.checked_against_machine'),
  );

  /** The text's lines, as the editor's gutter numbers them. */
  let lines = $derived(text.split('\n'));

  /** The frontmatter as the text currently has it. */
  let frontmatter = $derived<FrontmatterFields>(parseUnit(text).fields);
  let hasFrontmatter = $derived(parseUnit(text).hasFrontmatter);

  /** The unit the target level already has, where it has one. */
  let targetUnit = $derived<WorkflowUnit | null>(
    units.find((unit) => unit.name === unitName) ?? null,
  );
  let isCompiledOnly = $derived(targetUnit?.group === 'compiled_only');
  let isReplacing = $derived(Boolean(targetUnit && targetUnit.group !== 'compiled_only'));

  /** The line range of each state, from the runtime's structure and the text. */
  let stateRanges = $derived.by<Array<{ id: string; start: number; end: number }>>(() => {
    if (!structure) return [];
    const found: Array<{ id: string; start: number }> = [];
    for (const state of structure.states) {
      const index = lines.findIndex((line) =>
        new RegExp(`^\\s*state\\s+${escapeRegExp(state.id)}\\b`).test(line),
      );
      if (index >= 0) found.push({ id: state.id, start: index });
    }
    found.sort((left, right) => left.start - right.start);
    return found.map((entry, index) => ({
      id: entry.id,
      start: entry.start,
      end: (found[index + 1]?.start ?? lines.length) - 1,
    }));
  });

  /** The lines a validation finding marks in the gutter. */
  let findingLines = $derived.by<Record<number, string>>(() => {
    const marked: Record<number, string> = {};
    for (const finding of result?.findings ?? []) {
      marked[finding.line] = finding.rule;
    }
    return marked;
  });

  /** The author prompt is offered once per device, when the unit names no author yet. */
  let needsAuthor = $derived(
    !isCompiledOnly &&
      !savedAuthor &&
      (frontmatter.author === '' || frontmatter.author === 'plan-marshall'),
  );

  onMount(() => {
    savedAuthor = readAuthor();
    if (typeof window !== 'undefined' && window.matchMedia) {
      mediaQuery = window.matchMedia('(max-width: 639px)');
      isPhone = mediaQuery.matches;
      mediaQuery.addEventListener('change', onMediaChange);
    }
    window.addEventListener('beforeunload', onBeforeUnload);
    void loadAll();
  });

  onDestroy(() => {
    mediaQuery?.removeEventListener('change', onMediaChange);
    window.removeEventListener('beforeunload', onBeforeUnload);
  });

  function onMediaChange(event: MediaQueryListEvent): void {
    isPhone = event.matches;
  }

  /**
   * Asks the browser to keep the page while a draft changed since it was placed.
   *
   * @param event the unload event
   */
  function onBeforeUnload(event: BeforeUnloadEvent): void {
    if (!dirty) return;
    event.preventDefault();
    event.returnValue = '';
  }

  /** @param value the text to escape @returns the text as a regular expression literal */
  function escapeRegExp(value: string): string {
    return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }

  function displayNameFor(id: string): string {
    const match = projects.find((project) => project.project_id === id);
    return match?.display_name ?? id;
  }

  /**
   * @param id a state's id
   * @returns the 1-based line the state starts on, or -1 where the text no longer has it
   */
  function stateRangeStart(id: string): number {
    const range = stateRanges.find((entry) => entry.id === id);
    return range ? range.start + 1 : -1;
  }

  /** @returns the author this device remembers, or an empty string */
  function readAuthor(): string {
    if (typeof window === 'undefined') return '';
    try {
      return window.localStorage.getItem(AUTHOR_KEY) ?? '';
    } catch {
      return '';
    }
  }

  /** @returns the draft this device remembers, or null */
  function readDraft(): Draft | null {
    if (typeof window === 'undefined') return null;
    try {
      const raw = window.localStorage.getItem(DRAFT_KEY);
      if (!raw) return null;
      const draft = JSON.parse(raw) as Partial<Draft>;
      if (
        typeof draft.text !== 'string' ||
        typeof draft.unitName !== 'string' ||
        draft.text.trim().length === 0
      ) {
        return null;
      }
      return {
        text: draft.text,
        unitName: draft.unitName,
        unitKind: typeof draft.unitKind === 'string' ? draft.unitKind : 'subworkflow',
        projectId: typeof draft.projectId === 'string' ? draft.projectId : null,
        savedAt: typeof draft.savedAt === 'string' ? draft.savedAt : '',
      };
    } catch {
      return null;
    }
  }

  /** Keeps the draft in this browser, so a reload shows it again. */
  function saveDraft(): void {
    if (typeof window === 'undefined') return;
    try {
      const draft: Draft = {
        text,
        unitName,
        unitKind,
        projectId,
        savedAt: new Date().toISOString(),
      };
      window.localStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
    } catch {
      // Full or disabled storage: the draft lives in the screen only.
    }
  }

  /** Drops the draft once it was placed or discarded. */
  function clearDraft(): void {
    if (typeof window === 'undefined') return;
    try {
      window.localStorage.removeItem(DRAFT_KEY);
    } catch {
      // Nothing to drop where storage is unavailable.
    }
  }

  async function loadAll(): Promise<void> {
    notice = '';
    const draft = readDraft();
    // A restored draft names the project it was written for; fetch that project's units so the
    // panel and the blocks match the text the operator sees.
    if (draft?.projectId) projectId = draft.projectId;
    loadedProjectId = projectId;
    try {
      const [unitList, catalogueData, projectData] = await Promise.all([
        apiClient.getWorkflows(projectId),
        apiClient.getWorkflowCatalogue(projectId),
        apiClient.getProjectExplorer().catch(() => null),
      ]);
      units = Array.isArray(unitList) ? unitList : [];
      catalogue = catalogueData;
      projects = (projectData?.all_projects ?? []).map((project) => ({
        project_id: project.project_id,
        display_name: project.display_name,
      }));
      selectedBlock = null;

      if (draft) {
        unitName = draft.unitName;
        unitKind = draft.unitKind;
        text = draft.text;
        digest = null;
        result = null;
        structure = null;
        dirty = true;
        touched = true;
        notice = $t('designer.draft.restored', { time: formatDraftTime(draft.savedAt) });
        return;
      }
      if (units.length > 0) {
        openUnit(units[0]);
      }
    } catch {
      notice = $t('designer.draft.restored_loading_failed');
    }
  }

  /** @param iso the saved time @returns it as the catalogue's time form */
  function formatDraftTime(iso: string): string {
    if (!iso) return '';
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) return iso;
    return date.toLocaleString();
  }

  /**
   * Opens an installed unit in the editor.
   *
   * @param unit the unit to open
   */
  function openUnit(unit: WorkflowUnit): void {
    const level =
      unit.levels.find((entry) => entry.level === unit.level_in_force) ?? unit.levels[0];
    unitName = unit.name;
    unitKind = unit.frontmatter?.type || 'subworkflow';
    text = level?.text ?? '';
    digest = level?.digest ?? null;
    result = null;
    structure = null;
    outdated = false;
    placeMessage = '';
    placeStage = 'idle';
    touched = false;
    dirty = false;
    signatureDropped = false;
    clearDraft();
  }

  /**
   * Runs a pending change after the operator chose to discard the draft, or asks first.
   *
   * @param action the change to run
   */
  function guardDraft(action: () => void): void {
    if (!dirty) {
      action();
      return;
    }
    pendingAction = action;
  }

  /** Keeps the draft and stays. */
  function stayInDesigner(): void {
    pendingAction = null;
    projectId = loadedProjectId;
  }

  /** Discards the draft and runs the change that was waiting for the answer. */
  function discardDraft(): void {
    dirty = false;
    clearDraft();
    const action = pendingAction;
    pendingAction = null;
    action?.();
  }

  /** Leaves the designer, asking first when a draft is unsaved. */
  function leaveDesigner(): void {
    navigateTo('projects');
  }

  function onTextEdited(): void {
    if (!touched) {
      touched = true;
      const parsed = parseUnit(text);
      if (parsed.hasFrontmatter) {
        const signature = parsed.fields.signature;
        text = touchFrontmatter(
          text,
          new Date().toISOString().slice(0, 10),
          catalogue?.runtime_version ?? '',
        );
        if (savedAuthor) {
          text = editFrontmatter(text, (fields) => {
            if (!fields.author || fields.author === 'plan-marshall') {
              fields.author = savedAuthor;
            }
          });
        }
        if (signature) signatureDropped = true;
      }
    }
    afterEdit();
  }

  /** Marks the result out of date, the draft changed, and keeps the draft. */
  function afterEdit(): void {
    outdated = result !== null;
    placeMessage = '';
    dirty = true;
    saveDraft();
    updateCursorFromTextarea();
  }

  function insertBlock(block: BuildingBlock): void {
    if (isCompiledOnly || isPhone) return;
    text = `${text.replace(/\s+$/, '')}\n${block.template}\n`;
    if (!touched) touched = true;
    afterEdit();
  }

  /**
   * Applies an edit of the frontmatter panel to the text.
   *
   * @param edit the change to the fields
   * @param create whether the panel is adding a frontmatter
   */
  function applyFrontmatterEdit(edit: (fields: FrontmatterFields) => void): void {
    text = editFrontmatter(text, edit, true);
    if (!touched) {
      touched = true;
      const modified = new Date().toISOString().slice(0, 10);
      const signature = parseUnit(text).fields.signature;
      text = editFrontmatter(text, (fields) => {
        fields.modified = modified;
        fields.release = catalogue?.runtime_version ?? fields.release;
        fields.signature = '';
      });
      if (signature) signatureDropped = true;
    }
    afterEdit();
  }

  /** @param key a scalar field @param value its new value */
  function setField(key: keyof FrontmatterFields, value: string): void {
    applyFrontmatterEdit((fields) => {
      if (key === 'displayName') fields.displayName = value;
      else if (key === 'description') fields.description = value;
      else if (key === 'type') fields.type = value;
      else if (key === 'author') fields.author = value;
      else if (key === 'modified') fields.modified = value;
      else if (key === 'release') fields.release = value;
    });
  }

  /** @param kind entries or exits @param raw the ids the operator typed */
  function setInterface(kind: 'entries' | 'exits', raw: string): void {
    const ids = raw
      .split(/[,\s]+/)
      .map((id) => id.trim())
      .filter((id) => id.length > 0);
    applyFrontmatterEdit((fields) => {
      const existing = kind === 'entries' ? fields.entries : fields.exits;
      const next = ids.map((id) => ({
        id,
        description: existing.find((entry) => entry.id === id)?.description,
      }));
      if (kind === 'entries') fields.entries = next;
      else fields.exits = next;
    });
  }

  /** Adds the frontmatter the unit has none of yet. */
  function addFrontmatter(): void {
    applyFrontmatterEdit((fields) => {
      if (!fields.displayName) fields.displayName = unitName;
      if (!fields.type) fields.type = unitKind;
      if (!fields.author) fields.author = savedAuthor;
      if (!fields.modified) fields.modified = new Date().toISOString().slice(0, 10);
      if (!fields.release) fields.release = catalogue?.runtime_version ?? '';
    });
  }

  /** Remembers the author once on this device and writes it into the unit. */
  function rememberAuthor(): void {
    if (typeof window !== 'undefined' && savedAuthor) {
      try {
        window.localStorage.setItem(AUTHOR_KEY, savedAuthor);
      } catch {
        // The name applies to this visit where the browser cannot remember it.
      }
    }
    setField('author', savedAuthor);
  }

  /**
   * Opens the text the operator chose in the intake.
   *
   * @param content the unit's text
   */
  function openIntakeText(content: string): void {
    intakeError = '';
    if (content.includes('\uFFFD')) {
      intakeError = $t('designer.input.not_text');
      return;
    }
    const name = unitNameOf(content);
    if (!name) {
      intakeError = $t('designer.input.no_header');
      return;
    }
    unitName = name;
    unitKind = parseUnit(content).fields.type || 'subworkflow';
    text = content;
    digest = null;
    result = null;
    structure = null;
    outdated = false;
    touched = false;
    dirty = true;
    signatureDropped = false;
    notice = '';
    saveDraft();
  }

  /** @param event the chosen file */
  async function handleFile(event: Event): Promise<void> {
    const input = event.currentTarget as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    intakeError = '';
    if (file.size > FILE_LIMIT_BYTES) {
      intakeError = $t('designer.input.file_too_large', {
        limit: $t('designer.input.limit'),
      });
      input.value = '';
      return;
    }
    try {
      const content = await file.text();
      openIntakeText(content);
    } catch {
      intakeError = $t('designer.input.read_failed');
    } finally {
      input.value = '';
    }
  }

  /** Opens a pasted text in the editor. */
  function openPastedText(): void {
    if (pasteText.trim().length === 0) return;
    openIntakeText(pasteText);
  }

  /** Starts a draft from an empty unit template. */
  function startFromTemplate(): void {
    const name = 'new-unit';
    const release = catalogue?.runtime_version ?? '';
    const modified = new Date().toISOString().slice(0, 10);
    const template = [
      `workflow ${name} version 1 format 1`,
      '---',
      `name: ${name}`,
      'description:',
      'type: subworkflow',
      `author: ${savedAuthor}`,
      `modified: ${modified}`,
      `release: ${release}`,
      '---',
      'state start',
      '  done -> done',
      '',
    ].join('\n');
    openIntakeText(template);
  }

  /**
   * @param event the textarea's event
   * @returns the 1-based line the cursor is on
   */
  function lineAtCursor(event: Event): number {
    const element = event.currentTarget as HTMLTextAreaElement;
    const position = element.selectionStart ?? 0;
    return text.slice(0, position).split('\n').length;
  }

  /** Reads the cursor's line and selects its state in the structure. */
  function updateCursorFromTextarea(): void {
    if (!textareaElement) return;
    const position = textareaElement.selectionStart ?? 0;
    cursorLine = text.slice(0, position).split('\n').length;
    activeStateId =
      stateRanges.find((range) => cursorLine - 1 >= range.start && cursorLine - 1 <= range.end)
        ?.id ?? null;
  }

  /**
   * Moves the text to a line and selects the state it belongs to.
   *
   * @param line the 1-based line
   */
  function selectLine(line: number): void {
    const bounded = Math.max(1, Math.min(line, lines.length));
    cursorLine = bounded;
    activeStateId =
      stateRanges.find((range) => bounded - 1 >= range.start && bounded - 1 <= range.end)?.id ??
      null;
    if (!textareaElement) return;
    const offset = lines.slice(0, bounded - 1).join('\n').length + (bounded > 1 ? 1 : 0);
    textareaElement.focus();
    textareaElement.setSelectionRange(offset, offset);
    const lineHeight = 1.6 * 14;
    textareaElement.scrollTop = Math.max(0, (bounded - 3) * lineHeight);
    if (gutterElement) gutterElement.scrollTop = textareaElement.scrollTop;
  }

  /**
   * Selects a structure state and moves the text to its first line.
   *
   * @param id the state's id
   */
  function selectState(id: string): void {
    const range = stateRanges.find((entry) => entry.id === id);
    if (range) selectLine(range.start + 1);
    activeStateId = id;
  }

  /** Keeps the gutter aligned with the text as it scrolls. */
  function syncGutterScroll(): void {
    if (textareaElement && gutterElement) {
      gutterElement.scrollTop = textareaElement.scrollTop;
    }
  }

  async function validate(): Promise<void> {
    isValidating = true;
    notice = '';
    try {
      result = await apiClient.validateWorkflow({
        text,
        unit_name: unitName,
        unit_kind: unitKind,
        project: projectId,
        digest,
      });
      structure = result.structure ?? structure;
      outdated = false;
    } catch {
      notice = $t('designer.validate.failed');
    } finally {
      isValidating = false;
    }
  }

  /** @returns the level label the difference compares against */
  function targetLevelLabel(project: string | null): string {
    return project ? $t('designer.level.project') : $t('designer.level.machine');
  }

  /** Opens the placement's target choice. */
  function openPlace(): void {
    if (!result?.valid || outdated) return;
    placeTargetProject = projectId;
    placeStage = 'target';
    landingError = '';
    landingSteps = [];
  }

  /**
   * Validates the target and shows the difference and the projects it reaches.
   */
  async function compareForPlacement(): Promise<void> {
    landingError = '';
    let beforeText = '';
    try {
      const targetUnits =
        placeTargetProject && placeTargetProject !== projectId
          ? await apiClient.getWorkflows(placeTargetProject)
          : units;
      const existing = targetUnits.find((unit) => unit.name === unitName);
      if (existing) {
        const level =
          existing.levels.find((entry) => entry.level === existing.level_in_force) ??
          existing.levels[0];
        beforeText = level?.text ?? '';
      } else {
        beforeText = '';
      }
    } catch {
      landingError = $t('designer.place.failed', { reason: $t('system.figure.unavailable') });
    }
    placeAgainst = targetLevelLabel(placeTargetProject);
    placeDiff = diffLines(beforeText, text);
    conflictDiff = null;
    placeStage = 'diff';
  }

  /** @returns the path a placing writes, for the progress lines */
  function targetPath(): string {
    return placeTargetProject ? `.marshall/config/${unitName}.pmwf` : `workflows/${unitName}.pmwf`;
  }

  /**
   * Places the open unit at the chosen level.
   */
  async function place(): Promise<void> {
    if (!result?.valid) return;
    isPlacing = true;
    landingError = '';
    landingSteps = [];
    placeStage = 'progress';
    try {
      const placed = await apiClient.placeWorkflowUnit(placeTargetProject, unitName, {
        text,
        unit_name: unitName,
        unit_kind: unitKind,
        author: savedAuthor || $runtimeStore.deviceLabel || null,
        digest,
      });
      landingSteps = placed.steps ?? [];
      placeMessage = $t('designer.place.result', {
        name: unitName,
        author: placed.author ?? 'you',
        date: placed.release ?? '',
        digest: placed.digest ?? '',
      });
      placeStage = 'done';
      dirty = false;
      touched = false;
      clearDraft();
      digest = placed.digest ?? digest;
      placeAgainst = targetLevelLabel(placeTargetProject);
      const placedName = unitName;
      await loadAll();
      const placedUnit = units.find((unit) => unit.name === placedName);
      if (placedUnit) openUnit(placedUnit);
      placeStage = 'done';
      placeMessage = $t('designer.place.result', {
        name: placedName,
        author: placed.author ?? 'you',
        date: placed.release ?? '',
        digest: placed.digest ?? '',
      });
    } catch (err: unknown) {
      const message = (err as Error)?.message ?? $t('system.action.failed');
      if (/conflict/i.test(message)) {
        await showConflict();
      } else {
        placeStage = 'failed';
        landingError = message;
      }
    } finally {
      isPlacing = false;
    }
  }

  /** Shows the unit as it now is against the draft, after a placing conflicted. */
  async function showConflict(): Promise<void> {
    try {
      const targetUnits =
        placeTargetProject && placeTargetProject !== projectId
          ? await apiClient.getWorkflows(placeTargetProject)
          : units;
      const existing = targetUnits.find((unit) => unit.name === unitName);
      const level =
        existing?.levels.find((entry) => entry.level === existing.level_in_force) ??
        existing?.levels[0];
      conflictDiff = diffLines(level?.text ?? '', text);
    } catch {
      conflictDiff = diffLines('', text);
    }
    placeStage = 'conflict';
  }

  /** Closes the placement dialogue and returns to the editor. */
  function closePlace(): void {
    placeStage = 'idle';
    landingError = '';
    landingSteps = [];
    conflictDiff = null;
  }

  async function removeUnit(unit: WorkflowUnit): Promise<void> {
    notice = '';
    try {
      await apiClient.removeWorkflowUnit(projectId, unit.name);
      await loadAll();
    } catch {
      notice = $t('designer.remove.failed');
    }
  }

  async function onProjectChange(): Promise<void> {
    guardDraft(() => {
      void loadAll();
    });
  }

  /** @param value a line of the diff @returns the line, or a placeholder for a gap */
  function diffText(value: string | undefined): string {
    return value ?? '';
  }
</script>

<div class="designer" data-testid="process-designer">
  <header class="designer__head">
    <div class="designer__head-left">
      <h1 class="designer__title" data-testid="designer-title">{$t('designer.view.title')}</h1>
      <span class="designer__unit" data-testid="designer-unit">{unitName || '—'}</span>
      <span class="designer__checked" data-testid="designer-checked">{checkedAgainst}</span>
    </div>
    <div class="designer__head-right">
      <select
        class="designer__project"
        bind:value={projectId}
        onchange={onProjectChange}
        data-testid="project-select"
        aria-label={$t('designer.open.question')}
      >
        <option value={null}>{$t('designer.level.machine')}</option>
        {#each projects as project (project.project_id)}
          <option value={project.project_id}>{project.display_name}</option>
        {/each}
      </select>
      <Button
        variant="primary"
        loading={isValidating}
        on:click={validate}
        data-testid="validate-button"
      >
        {$t('designer.validate.action')}
      </Button>
      {#if !isPhone}
        <Button
          variant="secondary"
          disabled={!result?.valid || outdated || isPlacing || isCompiledOnly}
          loading={isPlacing}
          on:click={openPlace}
          data-testid="place-button"
        >
          {$t('designer.place.action')}
        </Button>
      {/if}
      <Button
        variant="ghost"
        on:click={() => guardDraft(leaveDesigner)}
        data-testid="designer-back"
      >
        {$t('designer.back')}
      </Button>
    </div>
  </header>

  {#if notice}
    <p class="designer__notice" role="alert" data-testid="designer-notice">{notice}</p>
  {/if}
  {#if placeMessage}
    <p class="designer__placed" data-testid="designer-placed">{placeMessage}</p>
  {/if}
  {#if signatureDropped}
    <p class="designer__note" role="status">{$t('designer.frontmatter.signature_dropped')}</p>
  {/if}
  {#if isPhone}
    <p class="designer__note" role="status" data-testid="designer-phone-note">
      {$t('designer.phone.note')}
    </p>
  {/if}

  {#if !isPhone}
    <section class="designer__intake" aria-labelledby="designer-intake-title">
      <h2 id="designer-intake-title" class="designer__intake-title">
        {$t('designer.open.title')}
      </h2>
      <div class="designer__intake-modes" role="group" aria-label={$t('designer.open.question')}>
        <button
          type="button"
          class="intake-mode"
          class:intake-mode--active={intakeMode === 'installed'}
          onclick={() => {
            intakeMode = 'installed';
          }}
        >
          {$t('designer.input.installed')}
        </button>
        <button
          type="button"
          class="intake-mode"
          class:intake-mode--active={intakeMode === 'upload'}
          onclick={() => {
            intakeMode = 'upload';
          }}
        >
          {$t('designer.input.upload')}
        </button>
        <button
          type="button"
          class="intake-mode"
          class:intake-mode--active={intakeMode === 'paste'}
          onclick={() => {
            intakeMode = 'paste';
          }}
        >
          {$t('designer.input.paste')}
        </button>
        <button
          type="button"
          class="intake-mode"
          class:intake-mode--active={intakeMode === 'template'}
          onclick={() => {
            intakeMode = 'template';
          }}
        >
          {$t('designer.input.template')}
        </button>
      </div>

      {#if intakeMode === 'upload'}
        <div class="intake-panel">
          <label class="intake-label" for="designer-file">{$t('designer.input.upload')}</label>
          <input
            id="designer-file"
            class="intake-file"
            type="file"
            accept=".pmwf,.txt,text/plain"
            onchange={handleFile}
            data-testid="designer-file-input"
          />
          <span class="text-muted text-xs">
            {$t('designer.input.upload_hint', { limit: $t('designer.input.limit') })}
          </span>
        </div>
      {:else if intakeMode === 'paste'}
        <div class="intake-panel">
          <label class="intake-label" for="designer-paste">{$t('designer.input.paste')}</label>
          <textarea
            id="designer-paste"
            class="intake-paste"
            bind:value={pasteText}
            spellcheck="false"
            aria-label={$t('designer.input.paste')}
            data-testid="designer-paste"
          ></textarea>
          <Button variant="secondary" onclick={openPastedText} data-testid="designer-paste-open">
            {$t('designer.input.open')}
          </Button>
        </div>
      {:else if intakeMode === 'template'}
        <div class="intake-panel">
          <p class="text-muted text-sm">{$t('designer.input.template_hint')}</p>
          <Button variant="secondary" onclick={startFromTemplate} data-testid="designer-template">
            {$t('designer.input.open')}
          </Button>
        </div>
      {:else}
        <p class="text-muted text-sm">{$t('designer.input.installed_hint')}</p>
      {/if}

      {#if intakeError}
        <p class="designer__notice" role="alert" data-testid="designer-intake-error">
          {intakeError}
        </p>
      {/if}
    </section>
  {/if}

  <div class="designer__workspace">
    <aside class="designer__units">
      <h2 class="designer__units-title">{$t('designer.input.installed')}</h2>
      {#if units.length === 0}
        <p class="designer__units-empty">—</p>
      {:else}
        <ul class="designer__units-list">
          {#each units as unit (unit.name)}
            <li
              class="designer__unit-row"
              class:designer__unit-row--active={unit.name === unitName}
            >
              <button
                type="button"
                class="designer__unit-open"
                onclick={() => guardDraft(() => openUnit(unit))}
                data-testid={`unit-open-${unit.name}`}
              >
                {unit.name}
              </button>
              <span class="designer__unit-level">
                {$t(`designer.level.${unit.level_in_force}`)}
              </span>
              {#if unit.group === 'compiled_only'}
                <span class="designer__unit-compiled" data-testid={`unit-compiled-${unit.name}`}>
                  {$t('designer.installed.compiled_only')}
                </span>
              {:else}
                <Button
                  variant="ghost"
                  on:click={() => removeUnit(unit)}
                  data-testid={`unit-remove-${unit.name}`}
                >
                  {$t('designer.remove.action')}
                </Button>
              {/if}
            </li>
          {/each}
        </ul>
      {/if}
      {#if !isPhone && !isCompiledOnly}
        <BlockPalette
          {catalogue}
          selected={selectedBlock}
          onInsert={insertBlock}
          onSelect={(block) => (selectedBlock = block)}
        />
      {/if}
    </aside>

    <section class="designer__text-pane">
      <div class="designer__text-head">
        <h2 class="designer__pane-title">{$t('designer.pane.text')}</h2>
        <span class="text-muted text-xs">{cursorLine} / {lines.length}</span>
      </div>
      <div class="designer__editor-wrap">
        <div
          class="designer__gutter"
          aria-hidden="true"
          bind:this={gutterElement}
          data-testid="designer-gutter"
        >
          {#each lines as _, index}
            <button
              type="button"
              class="gutter-line"
              class:gutter-line--active={cursorLine === index + 1}
              class:gutter-line--finding={Boolean(findingLines[index + 1])}
              tabindex="-1"
              onclick={() => selectLine(index + 1)}
            >
              {#if findingLines[index + 1]}✗{/if}{index + 1}
            </button>
          {/each}
        </div>
        <textarea
          class="designer__editor"
          bind:this={textareaElement}
          bind:value={text}
          oninput={onTextEdited}
          onclick={updateCursorFromTextarea}
          onkeyup={updateCursorFromTextarea}
          onscroll={syncGutterScroll}
          readonly={isPhone || isCompiledOnly}
          spellcheck="false"
          data-testid="workflow-text"
          aria-label={$t('designer.pane.text')}
        ></textarea>
      </div>
    </section>

    <aside class="designer__structure" data-testid="structure-pane">
      {#if hasFrontmatter}
        <section class="frontmatter" aria-labelledby="frontmatter-title">
          <h2 id="frontmatter-title" class="designer__structure-title">
            {$t('designer.frontmatter.title')}
          </h2>

          {#if needsAuthor && !isPhone}
            <div class="frontmatter__author-ask">
              <label class="frontmatter__label" for="designer-author">
                {$t('designer.frontmatter.author')}
              </label>
              <input
                id="designer-author"
                class="frontmatter__input"
                bind:value={savedAuthor}
                data-testid="designer-author-input"
              />
              <Button
                variant="secondary"
                onclick={rememberAuthor}
                data-testid="designer-author-save"
              >
                {$t('designer.frontmatter.author_save')}
              </Button>
            </div>
          {/if}

          <label class="frontmatter__row">
            <span class="frontmatter__label">{$t('designer.frontmatter.name')}</span>
            <input
              class="frontmatter__input"
              value={frontmatter.displayName}
              readonly={isPhone || isCompiledOnly}
              oninput={(event) => setField('displayName', event.currentTarget.value)}
              data-testid="frontmatter-name"
            />
          </label>
          <label class="frontmatter__row">
            <span class="frontmatter__label">{$t('designer.frontmatter.description')}</span>
            <input
              class="frontmatter__input"
              value={frontmatter.description}
              readonly={isPhone || isCompiledOnly}
              oninput={(event) => setField('description', event.currentTarget.value)}
              data-testid="frontmatter-description"
            />
          </label>
          <label class="frontmatter__row">
            <span class="frontmatter__label">{$t('designer.frontmatter.type')}</span>
            <input
              class="frontmatter__input"
              value={frontmatter.type}
              readonly={isCompiledOnly || isReplacing}
              oninput={(event) => setField('type', event.currentTarget.value)}
              data-testid="frontmatter-type"
            />
          </label>
          {#if isReplacing}
            <p class="frontmatter__kept text-muted text-xs">
              {$t('designer.frontmatter.interface_kept')}
            </p>
          {/if}
          <label class="frontmatter__row">
            <span class="frontmatter__label">{$t('designer.frontmatter.entries')}</span>
            <input
              class="frontmatter__input"
              value={frontmatter.entries.map((entry) => entry.id).join(', ')}
              readonly={isCompiledOnly || isReplacing}
              oninput={(event) => setInterface('entries', event.currentTarget.value)}
              data-testid="frontmatter-entries"
            />
          </label>
          <label class="frontmatter__row">
            <span class="frontmatter__label">{$t('designer.frontmatter.exits')}</span>
            <input
              class="frontmatter__input"
              value={frontmatter.exits.map((entry) => entry.id).join(', ')}
              readonly={isCompiledOnly || isReplacing}
              oninput={(event) => setInterface('exits', event.currentTarget.value)}
              data-testid="frontmatter-exits"
            />
          </label>
          <label class="frontmatter__row">
            <span class="frontmatter__label">{$t('designer.frontmatter.author_label')}</span>
            <input
              class="frontmatter__input"
              value={frontmatter.author}
              readonly={isPhone || isCompiledOnly}
              oninput={(event) => setField('author', event.currentTarget.value)}
              data-testid="frontmatter-author"
            />
          </label>
          <label class="frontmatter__row">
            <span class="frontmatter__label">{$t('designer.frontmatter.modified')}</span>
            <input
              class="frontmatter__input"
              value={frontmatter.modified}
              readonly={isPhone || isCompiledOnly}
              oninput={(event) => setField('modified', event.currentTarget.value)}
              data-testid="frontmatter-modified"
            />
          </label>
          <label class="frontmatter__row">
            <span class="frontmatter__label">{$t('designer.frontmatter.release')}</span>
            <input
              class="frontmatter__input"
              value={frontmatter.release}
              readonly={isPhone || isCompiledOnly}
              oninput={(event) => setField('release', event.currentTarget.value)}
              data-testid="frontmatter-release"
            />
          </label>
          <p class="frontmatter__signature text-muted text-xs">
            {$t('designer.frontmatter.signature')}:
            {frontmatter.signature || $t('designer.frontmatter.signature_none')}
          </p>
        </section>
      {:else}
        <div class="frontmatter__missing">
          <p class="text-muted text-sm">{$t('designer.frontmatter.add')}</p>
          <Button variant="secondary" onclick={addFrontmatter} data-testid="frontmatter-add">
            {$t('designer.frontmatter.add_action')}
          </Button>
        </div>
      {/if}

      <h2 class="designer__structure-title">{$t('designer.pane.structure')}</h2>
      {#if structure}
        <p class="designer__structure-name">{structure.display_name || unitName}</p>
        {#if structure.entries.length > 0}
          <p class="designer__structure-io">
            {structure.entries.map((entry) => `→ ${entry}`).join('  ')}
          </p>
        {/if}
        {#if structure.exits.length > 0}
          <p class="designer__structure-io">
            {structure.exits.map((exit) => `◂ ${exit}`).join('  ')}
          </p>
        {/if}
        <ul class="designer__structure-states">
          {#each structure.states as state (state.id)}
            <li
              class="designer__structure-state"
              class:designer__structure-state--active={activeStateId === state.id}
              class:designer__structure-state--finding={Boolean(
                findingLines[stateRangeStart(state.id)],
              )}
              data-testid={`structure-state-${state.id}`}
            >
              <button
                type="button"
                class="designer__structure-open"
                onclick={() => selectState(state.id)}
              >
                <span class="designer__structure-state-id">{state.id}</span>
                {#if state.calls.length > 0}
                  <span class="designer__structure-call">↳ {state.calls.join(', ')}</span>
                {/if}
                {#each state.outcomes as outcome}
                  <span class="designer__structure-outcome">{outcome}</span>
                {/each}
              </button>
            </li>
          {/each}
        </ul>
      {:else}
        <p class="designer__structure-empty">—</p>
      {/if}
    </aside>
  </div>

  <WorkflowValidationResults
    {result}
    {projectLabel}
    {outdated}
    onPreview={() => (previewOpen = true)}
    onSelectLine={selectLine}
  />

  {#if previewOpen && result?.composed}
    <div class="designer__preview" data-testid="composed-preview">
      <div class="designer__preview-head">
        <h2 class="designer__preview-title">
          {$t('designer.preview.title', { project: projectLabel || $t('designer.level.machine') })}
        </h2>
        <Button variant="ghost" on:click={() => (previewOpen = false)} data-testid="preview-close">
          {$t('web.common.close')}
        </Button>
      </div>
      <ol class="designer__preview-nodes">
        {#each result.composed.nodes as node (node.node_id)}
          <li class="designer__preview-node" data-testid={`preview-node-${node.node_id}`}>
            <span class="designer__preview-node-title">{node.title}</span>
            <span class="designer__preview-node-kind">{$t('plan.node.kind.phase')}</span>
            <span class="designer__preview-node-state">{$t('plan.node.state.planned')}</span>
          </li>
        {/each}
      </ol>
    </div>
  {/if}
</div>

{#if pendingAction}
  <div class="designer__modal" role="dialog" aria-modal="true" aria-labelledby="leave-title">
    <div class="designer__modal-card">
      <h2 id="leave-title" class="designer__modal-title">{$t('designer.leave.question')}</h2>
      <div class="designer__modal-actions">
        <Button variant="secondary" onclick={stayInDesigner}>{$t('designer.leave.stay')}</Button>
        <Button variant="danger" onclick={discardDraft}>{$t('designer.leave.discard')}</Button>
      </div>
    </div>
  </div>
{/if}

{#if placeStage !== 'idle'}
  <div
    class="designer__modal"
    role="dialog"
    aria-modal="true"
    aria-labelledby="place-title"
    data-testid="place-dialog"
  >
    <div class="designer__modal-card designer__modal-card--wide">
      {#if placeStage === 'target'}
        <h2 id="place-title" class="designer__modal-title">
          {$t('designer.place.question', { unit: unitName })}
        </h2>
        <fieldset class="designer__targets">
          <label class="designer__target">
            <input
              type="radio"
              name="placeTarget"
              checked={placeTargetProject === null}
              onchange={() => {
                placeTargetProject = null;
              }}
            />
            <span>
              <strong>{$t('designer.place.machine')}</strong>
              <span class="text-muted text-sm">{$t('designer.place.machine_hint')}</span>
            </span>
          </label>
          {#each projects as project (project.project_id)}
            <label class="designer__target">
              <input
                type="radio"
                name="placeTarget"
                checked={placeTargetProject === project.project_id}
                onchange={() => {
                  placeTargetProject = project.project_id;
                }}
              />
              <span>
                <strong>{$t('designer.place.project', { project: project.display_name })}</strong>
              </span>
            </label>
          {/each}
        </fieldset>
        <p class="text-muted text-sm">{$t('designer.place.order')}</p>
        <div class="designer__modal-actions">
          <Button variant="secondary" onclick={closePlace}>{$t('designer.back')}</Button>
          <Button variant="primary" onclick={compareForPlacement} data-testid="place-compare">
            {$t('designer.place.compare')}
          </Button>
        </div>
      {:else if placeStage === 'diff' && placeDiff}
        <h2 id="place-title" class="designer__modal-title">
          {$t('designer.confirm.title', {
            unit: unitName,
            target: placeTargetProject ? 'project' : 'machine',
            project: placeTargetProject ?? '',
          })}
        </h2>
        <p class="text-muted text-sm">
          {$t('designer.confirm.against', { level: placeAgainst })} ·
          {$t('designer.confirm.lines', {
            added: placeDiff.added,
            removed: placeDiff.removed,
          })}
        </p>
        <div class="designer__diff" data-testid="designer-diff">
          {#each placeDiff.rows as row}
            <div class="diff-row diff-row--{row.kind}">
              <span class="diff-no">{row.leftNo ?? ''}</span>
              <pre class="diff-line">{diffText(row.left)}</pre>
              <span class="diff-no">{row.rightNo ?? ''}</span>
              <pre class="diff-line">{diffText(row.right)}</pre>
            </div>
          {/each}
        </div>
        <p class="text-muted text-sm">
          {$t('designer.confirm.projects', {
            projects:
              projects
                .filter(
                  (project) => !placeTargetProject || project.project_id === placeTargetProject,
                )
                .map((project) => project.display_name)
                .join(', ') || $t('designer.level.machine'),
            others: placeTargetProject ? Math.max(projects.length - 1, 0) : 0,
          })}
        </p>
        {#if placeDiff.added === 0 && placeDiff.removed === 0}
          <p class="designer__note">{$t('designer.place.unchanged')}</p>
        {/if}
        <div class="designer__modal-actions">
          <Button variant="secondary" onclick={closePlace}>{$t('designer.back')}</Button>
          {#if placeDiff.added > 0 || placeDiff.removed > 0}
            <Button
              variant="primary"
              loading={isPlacing}
              onclick={place}
              data-testid="place-confirm"
            >
              {$t('designer.confirm.place', {
                target: placeTargetProject ? 'project' : 'machine',
                project: placeTargetProject ?? '',
              })}
            </Button>
          {/if}
        </div>
      {:else if placeStage === 'progress'}
        <h2 id="place-title" class="designer__modal-title">
          {$t('designer.place.progress', { unit: unitName })}
        </h2>
        {#if landingSteps.length === 0}
          <p class="text-muted text-sm">{$t('designer.place.starting')}</p>
        {/if}
        <ul class="designer__landing" data-testid="designer-landing">
          {#each landingSteps as step}
            <li class="designer__landing-step">
              <span aria-hidden="true">✓</span>
              {$t('designer.place.step', {
                step,
                path: targetPath(),
                commit: '',
              })}
            </li>
          {/each}
        </ul>
        <div class="designer__modal-actions">
          {#if !isPlacing}
            <Button variant="secondary" onclick={closePlace}>{$t('web.common.close')}</Button>
          {/if}
        </div>
      {:else if placeStage === 'failed'}
        <h2 id="place-title" class="designer__modal-title">{$t('designer.place.action')}</h2>
        <p class="designer__notice" role="alert" data-testid="designer-place-failed">
          {$t('designer.place.failed', { reason: landingError })}
        </p>
        <div class="designer__modal-actions">
          <Button variant="secondary" onclick={closePlace}>{$t('web.common.close')}</Button>
          <Button variant="primary" loading={isPlacing} onclick={place} data-testid="place-retry">
            {$t('designer.place.retry')}
          </Button>
        </div>
      {:else if placeStage === 'conflict' && conflictDiff}
        <h2 id="place-title" class="designer__modal-title">{$t('designer.place.action')}</h2>
        <p class="designer__notice" role="alert" data-testid="designer-place-conflict">
          {$t('designer.place.conflict', { unit: unitName })}
        </p>
        <div class="designer__diff designer__diff--conflict" data-testid="designer-conflict-diff">
          {#each conflictDiff.rows as row}
            <div class="diff-row diff-row--{row.kind}">
              <span class="diff-no">{row.leftNo ?? ''}</span>
              <pre class="diff-line">{diffText(row.left)}</pre>
              <span class="diff-no">{row.rightNo ?? ''}</span>
              <pre class="diff-line">{diffText(row.right)}</pre>
            </div>
          {/each}
        </div>
        <div class="designer__modal-actions">
          <Button variant="secondary" onclick={closePlace}>{$t('web.common.close')}</Button>
          <Button variant="primary" onclick={compareForPlacement} data-testid="place-compare-again">
            {$t('designer.place.compare')}
          </Button>
        </div>
      {:else if placeStage === 'done'}
        <h2 id="place-title" class="designer__modal-title">{$t('designer.place.action')}</h2>
        <p class="designer__placed" data-testid="designer-place-done">{placeMessage}</p>
        <div class="designer__modal-actions">
          <Button variant="primary" onclick={closePlace}>{$t('web.common.close')}</Button>
        </div>
      {/if}
    </div>
  </div>
{/if}

<style>
  .designer {
    display: flex;
    flex-direction: column;
    height: 100vh;
    background-color: var(--color-bg-app);
  }

  .designer__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-4);
    padding: var(--space-3) var(--space-6);
    background-color: var(--color-bg-surface);
    border-bottom: 1px solid var(--color-border);
    flex-wrap: wrap;
  }

  .designer__head-left {
    display: flex;
    align-items: baseline;
    gap: var(--space-3);
    flex-wrap: wrap;
  }

  .designer__title {
    font-size: var(--text-lg);
    font-weight: 700;
  }

  .designer__unit {
    font-family: var(--font-mono, monospace);
    font-size: var(--text-sm);
    color: var(--color-text-primary);
  }

  .designer__checked {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .designer__head-right {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  .designer__project {
    padding: var(--space-2);
    font-size: var(--text-sm);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-app);
    color: var(--color-text-primary);
  }

  .designer__notice {
    margin: 0;
    padding: var(--space-2) var(--space-6);
    background-color: var(--color-danger-subtle);
    color: var(--color-danger);
    font-size: var(--text-sm);
  }

  .designer__note,
  .designer__placed {
    margin: 0;
    padding: var(--space-2) var(--space-6);
    background-color: var(--color-success-subtle);
    color: var(--color-success);
    font-size: var(--text-sm);
  }

  .designer__note {
    background-color: var(--color-warning-subtle);
    color: var(--color-warning);
  }

  .designer__intake {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    padding: var(--space-2) var(--space-6) var(--space-3);
    background-color: var(--color-bg-surface);
    border-bottom: 1px solid var(--color-border);
  }

  .designer__intake-title {
    font-size: var(--text-sm);
    font-weight: 700;
    margin: 0;
  }

  .designer__intake-modes {
    display: flex;
    gap: var(--space-1);
    flex-wrap: wrap;
  }

  .intake-mode {
    padding: var(--space-1) var(--space-3);
    font-size: var(--text-sm);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background: transparent;
    color: var(--color-text-secondary);
    cursor: pointer;
  }

  .intake-mode--active {
    background-color: var(--color-primary);
    border-color: var(--color-primary);
    color: #ffffff;
  }

  .intake-panel {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    flex-wrap: wrap;
  }

  .intake-label {
    font-size: var(--text-sm);
    font-weight: 600;
  }

  .intake-paste {
    flex: 1;
    min-width: 240px;
    min-height: 4rem;
    padding: var(--space-2);
    font-family: var(--font-mono, monospace);
    font-size: var(--text-sm);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-app);
    color: var(--color-text-primary);
  }

  .designer__workspace {
    flex: 1;
    display: grid;
    grid-template-columns: 320px 1fr 360px;
    gap: 1px;
    background-color: var(--color-border);
    overflow: hidden;
  }

  .designer__units {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    padding: var(--space-3);
    background-color: var(--color-bg-surface);
    overflow-y: auto;
  }

  .designer__units-title,
  .designer__pane-title {
    font-size: var(--text-sm);
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--color-text-secondary);
  }

  .designer__units-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
  }

  .designer__unit-row {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    border-radius: var(--radius-sm);
    padding: 2px;
  }

  .designer__unit-row--active {
    background-color: var(--color-primary-subtle);
  }

  .designer__unit-open {
    flex: 1;
    text-align: left;
    font-family: var(--font-mono, monospace);
    font-size: var(--text-sm);
    background: none;
    border: none;
    cursor: pointer;
    color: var(--color-text-primary);
  }

  .designer__unit-level,
  .designer__unit-compiled {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }

  .designer__unit-compiled {
    font-style: italic;
  }

  .designer__text-pane {
    display: flex;
    flex-direction: column;
    min-width: 0;
    background-color: var(--color-bg-surface);
    overflow: hidden;
  }

  .designer__text-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--space-1) var(--space-3);
    border-bottom: 1px solid var(--color-border-subtle);
  }

  .designer__editor-wrap {
    flex: 1;
    display: grid;
    grid-template-columns: auto 1fr;
    overflow: hidden;
    background-color: var(--color-bg-inset);
  }

  .designer__gutter {
    display: flex;
    flex-direction: column;
    padding: var(--space-3) var(--space-1);
    overflow: hidden;
    text-align: right;
    font-family: var(--font-mono, monospace);
    font-size: var(--text-sm);
    line-height: 1.6;
    color: var(--color-text-muted);
    user-select: none;
  }

  .gutter-line {
    background: none;
    border: none;
    padding: 0 var(--space-1);
    font: inherit;
    color: inherit;
    cursor: pointer;
    white-space: pre;
  }

  .gutter-line--active {
    color: var(--color-text-primary);
    font-weight: 700;
  }

  .gutter-line--finding {
    color: var(--color-danger);
  }

  .designer__editor {
    width: 100%;
    height: 100%;
    padding: var(--space-3);
    border: none;
    outline: none;
    resize: none;
    font-family: var(--font-mono, monospace);
    font-size: var(--text-sm);
    line-height: 1.6;
    background-color: var(--color-bg-inset);
    color: var(--color-text-primary);
  }

  .designer__structure {
    padding: var(--space-3);
    background-color: var(--color-bg-surface);
    overflow-y: auto;
  }

  .designer__structure-title {
    font-size: var(--text-sm);
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--color-text-secondary);
    margin-bottom: var(--space-2);
  }

  .designer__structure-name {
    font-weight: 600;
    color: var(--color-text-primary);
    margin: 0 0 var(--space-1) 0;
  }

  .designer__structure-io {
    font-family: var(--font-mono, monospace);
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    margin: 0 0 var(--space-1) 0;
  }

  .designer__structure-states {
    list-style: none;
    margin: var(--space-2) 0 0 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .designer__structure-state {
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding-left: var(--space-2);
    border-left: 2px solid var(--color-border);
  }

  .designer__structure-state--active {
    border-left-color: var(--color-primary);
    background-color: var(--color-primary-subtle);
  }

  .designer__structure-state--finding {
    border-left-color: var(--color-danger);
  }

  .designer__structure-open {
    display: flex;
    flex-direction: column;
    gap: 2px;
    background: none;
    border: none;
    padding: 0;
    text-align: left;
    cursor: pointer;
    font: inherit;
    color: inherit;
  }

  .designer__structure-state-id {
    font-family: var(--font-mono, monospace);
    font-size: var(--text-sm);
  }

  .designer__structure-call,
  .designer__structure-outcome {
    font-family: var(--font-mono, monospace);
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
  }

  .designer__structure-empty,
  .designer__units-empty {
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }

  .frontmatter {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    padding-bottom: var(--space-3);
    margin-bottom: var(--space-3);
    border-bottom: 1px solid var(--color-border);
  }

  .frontmatter__row {
    display: grid;
    grid-template-columns: 90px 1fr;
    align-items: center;
    gap: var(--space-2);
  }

  .frontmatter__label {
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    font-weight: 600;
  }

  .frontmatter__input {
    width: 100%;
    padding: var(--space-1);
    font-size: var(--text-xs);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-app);
    color: var(--color-text-primary);
  }

  .frontmatter__author-ask {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    padding: var(--space-2);
    border: 1px solid var(--color-warning-border);
    border-radius: var(--radius-sm);
    background-color: var(--color-warning-subtle);
    margin-bottom: var(--space-2);
  }

  .frontmatter__missing {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
    padding-bottom: var(--space-3);
    margin-bottom: var(--space-3);
    border-bottom: 1px solid var(--color-border);
  }

  .designer__preview {
    border-top: 1px solid var(--color-border);
    background-color: var(--color-bg-subtle);
    padding: var(--space-3) var(--space-6);
    max-height: 40vh;
    overflow-y: auto;
  }

  .designer__preview-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--space-2);
  }

  .designer__preview-title {
    font-size: var(--text-sm);
    font-weight: 700;
  }

  .designer__preview-nodes {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
  }

  .designer__preview-node {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    padding: var(--space-1) var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    background-color: var(--color-bg-surface);
    font-size: var(--text-xs);
  }

  .designer__preview-node-kind,
  .designer__preview-node-state {
    color: var(--color-text-muted);
  }

  .designer__modal {
    position: fixed;
    inset: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: var(--space-4);
    z-index: 100;
  }

  .designer__modal-card {
    background-color: var(--color-bg-surface);
    border-radius: var(--radius-lg);
    padding: var(--space-6);
    width: 100%;
    max-width: 520px;
    max-height: 90vh;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
  }

  .designer__modal-card--wide {
    max-width: 1000px;
  }

  .designer__modal-title {
    font-size: var(--text-lg);
    font-weight: 700;
    margin: 0;
  }

  .designer__modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: var(--space-3);
    flex-wrap: wrap;
  }

  .designer__targets {
    border: none;
    padding: 0;
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  .designer__target {
    display: flex;
    align-items: flex-start;
    gap: var(--space-2);
    padding: var(--space-2);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
  }

  .designer__diff {
    display: flex;
    flex-direction: column;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-sm);
    overflow: auto;
    max-height: 50vh;
    font-family: var(--font-mono, monospace);
    font-size: var(--text-xs);
  }

  .diff-row {
    display: grid;
    grid-template-columns: 3rem 1fr 3rem 1fr;
    gap: var(--space-2);
    padding: 0 var(--space-2);
    white-space: pre;
  }

  .diff-row--add {
    background-color: var(--color-success-subtle);
  }

  .diff-row--remove {
    background-color: var(--color-danger-subtle);
  }

  .diff-row--change {
    background-color: var(--color-warning-subtle);
  }

  .diff-no {
    color: var(--color-text-muted);
    text-align: right;
  }

  .diff-line {
    margin: 0;
    overflow-wrap: anywhere;
    white-space: pre-wrap;
  }

  .designer__landing {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    font-size: var(--text-sm);
  }

  .designer__landing-step {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }

  @media (max-width: 900px) {
    .designer__workspace {
      grid-template-columns: 1fr;
      overflow-y: auto;
    }
  }

  @media (max-width: 639px) {
    .designer__head-right {
      flex-wrap: wrap;
    }

    .designer__intake {
      padding: var(--space-2) var(--space-3);
    }

    .designer__workspace {
      grid-template-columns: 1fr;
    }

    .diff-row {
      grid-template-columns: 2rem 1fr;
    }

    .diff-row .diff-no:nth-child(3),
    .diff-row .diff-line:nth-child(4) {
      display: none;
    }
  }
</style>
