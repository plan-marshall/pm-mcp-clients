/**
 * Token-reporting descriptor shared by `HarnessChoice` and `TokensScreen` so both format
 * `complete`, `partial` (with missing figures when present), and `not_reported` identically.
 */
export interface HarnessTokenInfo {
  token_usage?: string;
  token_detail?: string;
  missing_figures?: string[];
  usage?: { coverage?: string; figures?: Record<string, string> };
}

type Translator = (key: string, params?: Record<string, string | number>) => string;

function missingFiguresText(harness: HarnessTokenInfo): string {
  if (harness.token_detail?.trim()) return harness.token_detail.trim();
  if (Array.isArray(harness.missing_figures) && harness.missing_figures.length > 0) {
    return harness.missing_figures.join(', ');
  }
  const figures = harness.usage?.figures;
  if (figures && typeof figures === 'object') {
    const missing = Object.entries(figures)
      .filter(([, status]) => status === 'not_reported')
      .map(([name]) => name);
    if (missing.length > 0) return missing.join(', ');
  }
  return '';
}

/**
 * Formats what a harness reports of its token usage for the catalogue UI (`usage complete`,
 * `usage partial`, or `usage not reported`), avoiding dangling colons when no figures are named.
 *
 * @param translate catalogue translator function (`$t`)
 * @param harness harness scan or token-report row
 * @returns localised usage line
 */
export function harnessUsageText(translate: Translator, harness: HarnessTokenInfo): string {
  const coverage = harness.token_usage ?? harness.usage?.coverage;
  if (coverage === 'complete') {
    if (harness.token_detail?.trim()) {
      return translate('onboard.harness.tokens.complete', { figures: harness.token_detail.trim() });
    }
    return translate('onboard.harness.choose.usage_complete');
  }
  if (coverage === 'partial') {
    const missing = missingFiguresText(harness);
    if (missing) {
      return translate('onboard.harness.choose.usage_partial_missing', { figures: missing });
    }
    return translate('onboard.harness.choose.usage_partial');
  }
  return translate('onboard.harness.choose.usage_not_reported');
}
