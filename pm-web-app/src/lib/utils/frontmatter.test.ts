import { describe, it, expect } from 'vitest';
import {
  editFrontmatter,
  parseUnit,
  serialiseUnit,
  touchFrontmatter,
  unitNameOf,
} from './frontmatter';

const unit = [
  'workflow phase-6-finalize version 5 format 1',
  '---',
  'name: Finalize',
  'description: Settles the plan.',
  'type: subworkflow',
  'entry start',
  '  The execute phase ended.',
  'exit merged',
  'author: Oliver Wolff',
  'modified: 2026-10-14',
  'release: 0.9.0',
  'signature: sha256:abc',
  'custom: kept',
  '---',
  'state finalize.review',
  '',
].join('\n');

describe('frontmatter', () => {
  it('reads the header, the interface, and the fields', () => {
    expect(unitNameOf(unit)).toBe('phase-6-finalize');

    const parsed = parseUnit(unit);
    expect(parsed.hasFrontmatter).toBe(true);
    expect(parsed.fields.displayName).toBe('Finalize');
    expect(parsed.fields.type).toBe('subworkflow');
    expect(parsed.fields.entries).toEqual([
      { id: 'start', description: 'The execute phase ended.' },
    ]);
    expect(parsed.fields.exits).toEqual([{ id: 'merged' }]);
    expect(parsed.fields.extras).toEqual([['custom', 'kept']]);
  });

  it('round-trips a unit without losing its body or unknown fields', () => {
    const parsed = parseUnit(unit);
    const rendered = serialiseUnit(parsed);
    expect(rendered).toContain('custom: kept');
    expect(rendered).toContain('state finalize.review');
    expect(parseUnit(rendered).fields.signature).toBe('sha256:abc');
  });

  it('writes a field edit into the text', () => {
    const edited = editFrontmatter(unit, (fields) => {
      fields.displayName = 'Settled';
    });
    expect(edited).toContain('name: Settled');
    expect(edited).toContain('state finalize.review');
  });

  it('adds a frontmatter to a unit that has none', () => {
    const bare = 'workflow new-unit version 1 format 1\nstate start\n';
    expect(parseUnit(bare).hasFrontmatter).toBe(false);

    const added = editFrontmatter(
      bare,
      (fields) => {
        fields.displayName = 'New';
        fields.type = 'recipe';
      },
      true,
    );

    const parsed = parseUnit(added);
    expect(parsed.hasFrontmatter).toBe(true);
    expect(parsed.fields.displayName).toBe('New');
    expect(added).toContain('state start');
  });

  it('sets the modification date and release on an edit and drops the signature', () => {
    const touched = touchFrontmatter(unit, '2026-10-21', '0.10.0');
    const fields = parseUnit(touched).fields;
    expect(fields.modified).toBe('2026-10-21');
    expect(fields.release).toBe('0.10.0');
    expect(fields.signature).toBe('');
    expect(touched).not.toContain('signature:');
  });
});
