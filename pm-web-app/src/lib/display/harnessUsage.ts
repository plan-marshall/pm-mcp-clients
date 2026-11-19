/**
 * Token-reporting descriptor shared by `HarnessChoice` and `TokensScreen` so both format
 * `complete`, `partial` (with missing figures when present), and `not_reported` with one wording.
 *
 * The descriptor is the published `HarnessScanResult` / `HarnessUsageReport` (pm-api) and the
 * `harness.choose` row (`14-machine-onboarding.adoc:305`); no source publishes a `token_detail`
 * field, so both screens read it from neither and print the same line for the same fact.
 */
export interface HarnessTokenInfo {
  token_usage?: string;
  missing_figures?: string[];
  usage?: { coverage?: string; figures?: Record<string, string> };
}

type Translator = (key: string, params?: Record<string, string | number>) => string;

function formatComponent(translate: Translator, component: string): string {
  return translate('report.tokens.component', { component });
}

function missingFiguresText(translate: Translator, harness: HarnessTokenInfo): string {
  if (Array.isArray(harness.missing_figures) && harness.missing_figures.length > 0) {
    return harness.missing_figures.map((name) => formatComponent(translate, name)).join(', ');
  }
  const figures = harness.usage?.figures;
  if (figures && typeof figures === 'object') {
    const missing = Object.entries(figures)
      .filter(([, status]) => status === 'not_reported')
      .map(([name]) => formatComponent(translate, name));
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
    return translate('onboard.harness.choose.usage_complete');
  }
  if (coverage === 'partial') {
    const missing = missingFiguresText(translate, harness);
    if (missing) {
      return translate('onboard.harness.choose.usage_partial_missing', { figures: missing });
    }
    return translate('onboard.harness.choose.usage_partial');
  }
  return translate('onboard.harness.choose.usage_not_reported');
}
