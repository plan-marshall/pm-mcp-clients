/**
 * The frontmatter of a workflow unit, read from and written into the unit's text.
 *
 * The designer shows the frontmatter as a panel kept in step with the text both ways. The text
 * stays the one source: the panel reads this parse and writes its fields back as lines, so an
 * edit in either place is the same edit.
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
  /** Fields this client does not edit, kept in their original order. */
  extras: Array<[string, string]>;
}

/** A unit's text split into its header, frontmatter, and body. */
export interface ParsedUnit {
  header: string;
  /** Lines after the header, before the opening delimiter. */
  preface: string[];
  fields: FrontmatterFields;
  hasFrontmatter: boolean;
  /** The body's lines, starting with the closing delimiter where there is frontmatter. */
  body: string[];
}

const OPEN = '---';

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

/**
 * Reads a unit's text into its header, frontmatter fields, and body.
 *
 * A text without a frontmatter parses with empty fields and `hasFrontmatter: false`; the panel
 * then offers to add one.
 *
 * @param text the unit's text
 * @returns the parsed unit
 */
export function parseUnit(text: string): ParsedUnit {
  const lines = text.split('\n');
  const header = lines[0] ?? '';
  const fields = emptyFrontmatter();
  const openIndex = lines.findIndex((line, index) => index > 0 && line.trim() === OPEN);
  if (openIndex === -1) {
    return { header, preface: lines.slice(1), fields, hasFrontmatter: false, body: [] };
  }
  const closeIndex = lines.findIndex((line, index) => index > openIndex && line.trim() === OPEN);
  if (closeIndex === -1) {
    return { header, preface: lines.slice(1), fields, hasFrontmatter: false, body: [] };
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
      currentInterface.description = line.trim();
    }
  }

  return {
    header,
    preface: lines.slice(1, openIndex),
    fields,
    hasFrontmatter: true,
    body: lines.slice(closeIndex),
  };
}

/**
 * Writes parsed fields back as the frontmatter block.
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
 * @param unit the parsed unit, with the fields to write
 * @returns the unit's text
 */
export function serialiseUnit(unit: ParsedUnit): string {
  const block = unit.hasFrontmatter ? serialiseFrontmatter(unit.fields) : [];
  return [unit.header, ...unit.preface, ...block, ...unit.body].join('\n');
}

/**
 * Applies an edit to a unit's frontmatter and returns the new text.
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
  const unit: ParsedUnit = { ...parsed, fields: { ...parsed.fields } };
  edit(unit.fields);
  if (!unit.hasFrontmatter && create) {
    unit.hasFrontmatter = true;
  }
  return serialiseUnit(unit);
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
