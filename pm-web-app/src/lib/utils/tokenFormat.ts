/**
 * Token number formatting according to texts-and-languages.adoc#numbers.
 */

export function formatTokens(count: number, floor = false, locale = 'en'): string {
  if (count <= 0) return '0';

  const isDe = locale.startsWith('de');
  const thousandsSuffix = isDe ? ' Tsd.' : ' k';
  const millionsSuffix = isDe ? ' Mio.' : ' M';
  const decimalPoint = isDe ? ',' : '.';

  let formatted = '';
  if (count < 1000) {
    formatted = String(count);
  } else if (count < 10000) {
    let thousands: number;
    if (floor) {
      thousands = Math.floor(count / 100) / 10;
    } else {
      thousands = Math.round(count / 100) / 10;
    }
    const fixedStr = thousands.toFixed(1).replace('.', decimalPoint);
    formatted = `${fixedStr}${thousandsSuffix}`;
  } else if (count < 1000000) {
    let thousands: number;
    if (floor) {
      thousands = Math.floor(count / 1000);
    } else {
      thousands = Math.round(count / 1000);
    }
    formatted = `${thousands}${thousandsSuffix}`;
  } else {
    let millions: number;
    if (floor) {
      millions = Math.floor(count / 10000) / 100;
    } else {
      millions = Math.round(count / 10000) / 100;
    }
    const fixedStr = millions.toFixed(2).replace('.', decimalPoint);
    formatted = `${fixedStr}${millionsSuffix}`;
  }

  return floor ? `≥ ${formatted}` : formatted;
}

export function formatSplit(
  total: number,
  inTokens: number,
  outTokens: number,
  floor = false,
  locale = 'en'
): string {
  const isDe = locale.startsWith('de');
  const totalFormatted = formatTokens(total, floor, locale);
  const inFormatted = formatTokens(inTokens, floor, locale);
  const outFormatted = formatTokens(outTokens, floor, locale);

  const inLabel = isDe ? 'rein' : 'in';
  const outLabel = isDe ? 'raus' : 'out';

  return `${totalFormatted} (${inFormatted} ${inLabel} · ${outFormatted} ${outLabel})`;
}
