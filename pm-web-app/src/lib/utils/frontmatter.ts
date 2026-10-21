/**
 * The frontmatter of a workflow unit, read from and written into the unit's text.
 *
 * The designer shows the frontmatter as a panel kept in step with the text both ways. The text
 * stays the one source: the panel reads this parse and writes only the lines it edits, so a
 * comment, an unknown field, or a description the panel does not model survives an edit.
 */

/** One entry or exit of a unit's interface, with its description where it has one. */
export interface InterfaceItem {
  id: string;
  description?: string;
}

/** The frontmatter fields the panel edits; unknown fields are kept as they are. */
export interface FrontmatterFields {
  displayName: string;
  description: string;
  type: string;
  entries: InterfaceItem[];
  exits: InterfaceItem[];
  author: string;
  modified: string;
  release: string;
  signature: string;
  /** Fields this client does not edit, kept with their values. */
  extras: Array<[string, string]>;
}

/** A unit's text split into its header, frontmatter, and body. */
export interface ParsedUnit {
  header: string;
  /** Lines after the header, before the opening delimiter. */
  preface: string[];
  fields: FrontmatterFields;
  hasFrontmatter: boolean;
  /** The original frontmatter block, delimiters included, where the unit has one. */
  rawBlock: string[] | null;
  /** The body's lines, starting with the closing delimiter where there is frontmatter. */
  body: string[];
}

const OPEN = '---';
const SCALAR_FIELDS: Array<[keyof FrontmatterFields, string]> = [
  ['displayName', 'name'],
  ['description', 'description'],
  ['type', 'type'],
  ['author', 'author'],
  ['modified', 'modified'],
  ['release', 'release'],
  ['signature', 'signature'],
];

/** @returns the empty frontmatter the panel shows for a unit that has none */
export function emptyFrontmatter(): FrontmatterFields {
  return {
    displayName: '',
    description: '',
    type: 'subworkflow',
    entries: [],
    exits: [],
    author: '',
    modified: '',
    release: '',
    signature: '',
    extras: [],
  };
}

/** @param text the unit's text @returns the unit name from its header, or null */
export function unitNameOf(text: string): string | null {
  const header = text.split('\n', 1)[0] ?? '';
  const match = /^workflow\s+(\S+)/.exec(header.trim());
  return match?.[1] ?? null;
}

/** @param unit a parsed unit @returns the key/value line's key */
function scalarKey(line: string): string | null {
  const match = /^([A-Za-z][\w.-]*)\s*:\s?(.*)$/.exec(line);
  return match?.[1] ?? null;
}

/** @param line a line of the block @returns whether it starts an entry or an exit */
function isInterfaceLine(line: string): boolean {
  return /^(entry|exit)\s+\S+\s*$/.test(line.trim());
}

/** @param line a line of the block @returns whether it continues the line above it */
function isContinuation(line: string): boolean {
  return /^\s+\S/.test(line);
}

/** @returns the index of the opening delimiter, or -1 where there is none */
function openingDelimiter(lines: string[]): number {
  let index = 1;
  while (index < lines.length && lines[index].trim() === '') index++;
  return lines[index]?.trim() === OPEN ? index : -1;
}

/**
 * Reads a unit's text into its header, frontmatter fields, and body.
 *
 * The opening delimiter is accepted only as the first non-blank line after the header, so a `---`
 * pair inside the body is never mistaken for a frontmatter.
 *
 * @param text the unit's text
 * @returns the parsed unit
 */
export function parseUnit(text: string): ParsedUnit {
  const lines = text.split('\n');
  const header = lines[0] ?? '';
  const fields = emptyFrontmatter();
  const openIndex = openingDelimiter(lines);
  if (openIndex === -1) {
    return {
      header,
      preface: [],
      fields,
      hasFrontmatter: false,
      rawBlock: null,
      body: lines.slice(1),
    };
  }
  const closeIndex = lines.findIndex((line, index) => index > openIndex && line.trim() === OPEN);
  if (closeIndex === -1) {
    return {
      header,
      preface: [],
      fields,
      hasFrontmatter: false,
      rawBlock: null,
      body: lines.slice(1),
    };
  }

  let currentInterface: InterfaceItem | null = null;
  for (const raw of lines.slice(openIndex + 1, closeIndex)) {
    const line = raw.trimEnd();
    const interfaceMatch = /^(entry|exit)\s+(\S+)\s*$/.exec(line.trim());
    if (interfaceMatch) {
      const item: InterfaceItem = { id: interfaceMatch[2] };
      (interfaceMatch[1] === 'entry' ? fields.entries : fields.exits).push(item);
      currentInterface = item;
      continue;
    }
    const key = scalarKey(line);
    if (key) {
      currentInterface = null;
      const value = line.slice(line.indexOf(':') + 1).trim();
      if (key === 'name') fields.displayName = value;
      else if (key === 'description') fields.description = value;
      else if (key === 'type') fields.type = value;
      else if (key === 'author') fields.author = value;
      else if (key === 'modified') fields.modified = value;
      else if (key === 'release') fields.release = value;
      else if (key === 'signature') fields.signature = value;
      else fields.extras.push([key, value]);
      continue;
    }
    if (currentInterface && line.trim().length > 0) {
      currentInterface.description = currentInterface.description
        ? `${currentInterface.description}\n${line.trim()}`
        : line.trim();
    }
  }

  return {
    header,
    preface: lines.slice(1, openIndex),
    fields,
    hasFrontmatter: true,
    rawBlock: lines.slice(openIndex, closeIndex + 1),
    body: lines.slice(closeIndex),
  };
}

/**
 * Writes fields as a fresh frontmatter block.
 *
 * @param fields the fields to write
 * @returns the block's lines, including both delimiters
 */
function serialiseFrontmatter(fields: FrontmatterFields): string[] {
  const block: string[] = [OPEN];
  block.push(`name: ${fields.displayName}`.trimEnd());
  block.push(`description: ${fields.description}`.trimEnd());
  block.push(`type: ${fields.type}`.trimEnd());
  for (const entry of fields.entries) {
    block.push(`entry ${entry.id}`.trimEnd());
    if (entry.description) block.push(`  ${entry.description}`);
  }
  for (const exit of fields.exits) {
    block.push(`exit ${exit.id}`.trimEnd());
    if (exit.description) block.push(`  ${exit.description}`);
  }
  block.push(`author: ${fields.author}`.trimEnd());
  block.push(`modified: ${fields.modified}`.trimEnd());
  block.push(`release: ${fields.release}`.trimEnd());
  if (fields.signature) block.push(`signature: ${fields.signature}`.trimEnd());
  for (const [key, value] of fields.extras) {
    block.push(`${key}: ${value}`.trimEnd());
  }
  block.push(OPEN);
  return block;
}

/**
 * Renders a parsed unit back to text.
 *
 * The frontmatter block is returned exactly as it was read; the panel's edits go through
 * {@link editFrontmatter}, which changes only the lines a field owns.
 *
 * @param unit the parsed unit
 * @returns the unit's text
 */
export function serialiseUnit(unit: ParsedUnit): string {
  if (unit.rawBlock) {
    return [unit.header, ...unit.preface, ...unit.rawBlock, ...unit.body.slice(1)].join('\n');
  }
  return [unit.header, ...unit.preface, ...unit.body].join('\n');
}

/**
 * Replaces a scalar field's line and drops the continuation lines it no longer owns.
 *
 * @param block the frontmatter body, without delimiters
 * @param key the field's wire key
 * @param value its new value
 */
function writeScalar(block: string[], key: string, value: string): void {
  const index = block.findIndex((line) => new RegExp(`^${key}\\s*:`).test(line));
  if (index === -1) {
    if (value.length > 0) block.push(`${key}: ${value}`);
    return;
  }
  if (value.length === 0 && key === 'signature') {
    block.splice(index, 1);
    return;
  }
  const [firstLine, ...continuation] = value.split('\n');
  block[index] = firstLine.length > 0 ? `${key}: ${firstLine}` : `${key}:`;
  let cursor = index + 1;
  while (cursor < block.length && isContinuation(block[cursor])) {
    block.splice(cursor, 1);
  }
  for (const line of continuation) {
    block.splice(cursor, 0, `  ${line}`);
    cursor++;
  }
}

/**
 * Replaces the interface lines (entries and exits) and their descriptions.
 *
 * @param block the frontmatter body, without delimiters
 * @param fields the fields to write
 */
function writeInterface(block: string[], fields: FrontmatterFields): void {
  const first = block.findIndex((line) => isInterfaceLine(line));
  const kept: string[] = [];
  for (let index = 0; index < block.length; index++) {
    if (isInterfaceLine(block[index])) {
      index++;
      while (index < block.length && isContinuation(block[index])) index++;
      index--;
      continue;
    }
    kept.push(block[index]);
  }
  const interfaceLines: string[] = [];
  for (const entry of fields.entries) {
    interfaceLines.push(`entry ${entry.id}`);
    if (entry.description) {
      for (const line of entry.description.split('\n')) interfaceLines.push(`  ${line}`);
    }
  }
  for (const exit of fields.exits) {
    interfaceLines.push(`exit ${exit.id}`);
    if (exit.description) {
      for (const line of exit.description.split('\n')) interfaceLines.push(`  ${line}`);
    }
  }
  const position = first >= 0 ? Math.min(first, kept.length) : kept.length;
  kept.splice(position, 0, ...interfaceLines);
  block.splice(0, block.length, ...kept);
}

/**
 * Applies an edit to a unit's frontmatter and returns the new text.
 *
 * Only the lines of the fields the edit changed are rewritten; a comment, an unknown field, or a
 * description the panel does not model stays in place.
 *
 * @param text the unit's text
 * @param edit the change to the fields
 * @param create whether to add a frontmatter to a unit that has none
 * @returns the unit's text with the edit applied
 */
export function editFrontmatter(
  text: string,
  edit: (fields: FrontmatterFields) => void,
  create = false,
): string {
  const parsed = parseUnit(text);
  const fields: FrontmatterFields = {
    ...parsed.fields,
    entries: parsed.fields.entries.map((entry) => ({ ...entry })),
    exits: parsed.fields.exits.map((entry) => ({ ...entry })),
    extras: parsed.fields.extras.map(([key, value]) => [key, value]),
  };
  edit(fields);

  if (!parsed.hasFrontmatter || !parsed.rawBlock) {
    if (!create) return text;
    const block = serialiseFrontmatter(fields);
    const lines = text.split('\n');
    return [lines[0], ...block, ...lines.slice(1)].join('\n');
  }

  const block = parsed.rawBlock.slice(1, -1);
  for (const [field, key] of SCALAR_FIELDS) {
    writeScalar(block, key, String(fields[field] ?? ''));
  }
  const interfaceChanged =
    JSON.stringify(fields.entries) !== JSON.stringify(parsed.fields.entries) ||
    JSON.stringify(fields.exits) !== JSON.stringify(parsed.fields.exits);
  if (interfaceChanged) writeInterface(block, fields);

  return [parsed.header, ...parsed.preface, OPEN, ...block, ...parsed.body].join('\n');
}

/**
 * Marks the fields an edit sets: the day's date and the runtime's release, and drops a
 * signature that no longer covers the text.
 *
 * @param text the unit's text
 * @param modified the modification date, `YYYY-MM-DD`
 * @param release the runtime's release
 * @returns the unit's text with the fields set
 */
export function touchFrontmatter(text: string, modified: string, release: string): string {
  return editFrontmatter(text, (fields) => {
    fields.modified = modified;
    fields.release = release;
    fields.signature = '';
  });
}
