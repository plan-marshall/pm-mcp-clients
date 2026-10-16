/**
 * Token number formatting according to texts-and-languages.adoc#numbers.
 */

function formatWithDecimals(
  val: number,
  factor: number,
  decimals: number,
  floor: boolean,
  decimalPoint: string,
): string {
  const rounded = floor ? Math.floor(val * factor) / factor : Math.round(val * factor) / factor;
  return rounded.toFixed(decimals).replace('.', decimalPoint);
}

export function formatTokens(count: number, floor = false, locale = 'en'): string {
  if (count <= 0) return '0';

  const isDe = locale.startsWith('de');
  const thousandsSuffix = isDe ? ' Tsd.' : ' k';
  const millionsSuffix = isDe ? ' Mio.' : ' M';
  const decimalPoint = isDe ? ',' : '.';

  let formatted: string;
  if (count < 1000) {
    formatted = String(count);
  } else if (count < 10000) {
    formatted = `${formatWithDecimals(count / 1000, 10, 1, floor, decimalPoint)}${thousandsSuffix}`;
  } else if (count < 1000000) {
    const thousands = floor ? Math.floor(count / 1000) : Math.round(count / 1000);
    formatted = `${thousands}${thousandsSuffix}`;
  } else {
    formatted = `${formatWithDecimals(count / 1000000, 100, 2, floor, decimalPoint)}${millionsSuffix}`;
  }

  return floor ? `≥ ${formatted}` : formatted;
}

export function formatSplit(
  total: number,
  inTokens: number,
  outTokens: number,
  floor = false,
  locale = 'en',
): string {
  const isDe = locale.startsWith('de');
  const totalFormatted = formatTokens(total, floor, locale);
  const inFormatted = formatTokens(inTokens, floor, locale);
  const outFormatted = formatTokens(outTokens, floor, locale);

  const inLabel = isDe ? 'rein' : 'in';
  const outLabel = isDe ? 'raus' : 'out';

  return `${totalFormatted} (${inFormatted} ${inLabel} · ${outFormatted} ${outLabel})`;
}
