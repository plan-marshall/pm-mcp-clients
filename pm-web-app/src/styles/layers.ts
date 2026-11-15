/**
 * Stacking layer tokens (PLAN-149, components.adoc § #layers).
 *
 * Single runtime source of truth for stacking layers, verified against
 * tokens.css by tokens.test.ts.
 */
export interface LayerTokenDefinition {
  readonly name: string;
  readonly token: string;
  readonly value: number;
}

export const LAYER_TOKENS: readonly LayerTokenDefinition[] = Object.freeze([
  { name: 'sticky', token: '--layer-sticky', value: 1 },
  { name: 'bar', token: '--layer-bar', value: 20 },
  { name: 'menu', token: '--layer-menu', value: 30 },
  { name: 'toast', token: '--layer-toast', value: 90 },
  { name: 'dialog', token: '--layer-dialog', value: 100 },
]);

export type LayerName = 'sticky' | 'bar' | 'menu' | 'toast' | 'dialog';
