import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import {
  ADR_RELATIONS,
  BUILDER_RESOLUTIONS,
  BUILDER_SORTS,
  BUILDER_STATES,
  CRITERION_STATUSES,
  DONE_WHEN_VERDICTS,
  FILE_INTENTS,
  INTENT_DECIDERS,
} from './client';

/**
 * The client's closed vocabularies are the contract's: each list is held to the `pm-api` enum it
 * mirrors, in that enum's declaration order. An enum constant the client does not know would be
 * refused at the wire, so this guard fails the build before that refusal reaches an operator.
 */

const INTENT_DIR = path.resolve(
  __dirname,
  '../../../../pm-api/src/main/java/de/planmarshall/api/intent',
);

/**
 * @param file the enum's source file
 * @returns the wire values of its constants, in declaration order
 */
function wireValuesOf(file: string): string[] {
  const source = fs.readFileSync(path.join(INTENT_DIR, file), 'utf-8');
  return Array.from(source.matchAll(/^\s+[A-Z][A-Z0-9_]*\("([a-z_-]+)"\)/gm)).map(
    (match) => match[1],
  );
}

describe('the intent readers accept exactly the contract vocabularies', () => {
  it('holds every list to the pm-api enum it mirrors, in declaration order', () => {
    expect(wireValuesOf('CriterionStatus.java')).toEqual([...CRITERION_STATUSES]);
    expect(wireValuesOf('IntentDecider.java')).toEqual([...INTENT_DECIDERS]);
    expect(wireValuesOf('BuilderCheckResource.java')).toEqual([...BUILDER_RESOLUTIONS]);
    expect(wireValuesOf('DeliverableRecord.java')).toEqual([...FILE_INTENTS]);
    expect(wireValuesOf('DoneWhenCheck.java')).toEqual([...DONE_WHEN_VERDICTS]);
  });

  it('holds both closed lists of a builder question apart: its sort and its state', () => {
    const questionValues = wireValuesOf('BuilderQuestion.java');

    expect(questionValues.slice(0, BUILDER_SORTS.length)).toEqual([...BUILDER_SORTS]);
    expect(questionValues.slice(BUILDER_SORTS.length)).toEqual([...BUILDER_STATES]);
    // `answered` is the one value that names both a sort and a state, by the contract's design.
    expect(BUILDER_SORTS).toContain('answered');
    expect(BUILDER_STATES).toContain('answered');  });

  it('reads the ADR relations of the plan intent record', () => {
    const source = fs.readFileSync(path.join(INTENT_DIR, 'PlanIntentResource.java'), 'utf-8');
    const relations = Array.from(
      source.matchAll(/^\s+[A-Z]+\("(follows|contradicts)"\)/gm),
    ).map((match) => match[1]);

    expect(relations).toEqual([...ADR_RELATIONS]);
  });
});
