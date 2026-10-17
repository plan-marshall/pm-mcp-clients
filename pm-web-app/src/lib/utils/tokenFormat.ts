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

export function formatBytes(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  const kb = bytes / 1024;
  if (kb < 1024) {
    return `${Math.round(kb)} KB`;
  }
  const mb = kb / 1024;
  if (mb < 1024) {
    return `${Math.round(mb)} MB`;
  }
  const gb = mb / 1024;
  if (gb >= 10) {
    return `${Math.round(gb)} GB`;
  }
  const rounded = Math.round(gb * 10) / 10;
  if (rounded === Math.floor(rounded)) {
    return `${rounded} GB`;
  }
  return `${rounded.toFixed(1)} GB`;
}

export function formatDurationSeconds(seconds: number): string {
  if (seconds <= 0) return '0 min';
  const mins = Math.floor(seconds / 60);
  const hours = Math.floor(mins / 60);
  if (hours > 0) {
    const remMins = mins % 60;
    return remMins > 0 ? `${hours} h ${remMins} min` : `${hours} h`;
  }
  if (mins > 0) {
    return `${mins} min`;
  }
  return `${seconds} s`;
}
