/**
 * Stacking layer tokens (PLAN-149, components.adoc § #layers).
 *
 * Single runtime source of truth for stacking layers, verified against
 * tokens.css by tokens.test.ts.
 */
export const LAYER_TOKENS = [
  { name: 'sticky', token: '--layer-sticky', value: 1 },
  { name: 'bar', token: '--layer-bar', value: 20 },
  { name: 'menu', token: '--layer-menu', value: 30 },
  { name: 'toast', token: '--layer-toast', value: 90 },
  { name: 'dialog', token: '--layer-dialog', value: 100 },
] as const;

export type LayerTokenDefinition = (typeof LAYER_TOKENS)[number];
export type LayerName = LayerTokenDefinition['name'];
