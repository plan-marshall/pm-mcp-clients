/**
 * The colour maths the design-token tests measure with: sRGB decoding, OKLab and OKLCH, CIELAB under
 * D65, the CIEDE2000 colour difference and the WCAG 2.1 contrast ratio.
 *
 * A test helper with no dependency, so the measured rules of the design (`components-tags.adoc`,
 * The Three Kind Tints) stand on maths that `colourMetrics.test.ts` checks against published
 * reference values.
 */

/** A colour's three channels, each in 0..1. */
export interface Rgb {
  r: number;
  g: number;
  b: number;
}

/** A CIELAB or OKLab colour. */
export interface Lab {
  l: number;
  a: number;
  b: number;
}

/** An OKLCH colour; `h` is in degrees, 0..360. */
export interface Lch {
  l: number;
  c: number;
  h: number;
}

/** The D65 reference white in XYZ, Y normalised to 1. */
const D65_WHITE = { x: 0.95047, y: 1, z: 1.08883 };

const DEGREES = 180 / Math.PI;

/**
 * @param colour a `#rrggbb` literal
 * @returns its channels in 0..1, still gamma-encoded
 */
export function parseHex(colour: string): Rgb {
  const match = /^#([0-9a-fA-F]{6})$/.exec(colour.trim());
  if (!match) throw new Error(`Not a six-digit hex colour: ${colour}`);
  const channel = (offset: number): number => parseInt(match[1].slice(offset, offset + 2), 16) / 255;
  return { r: channel(0), g: channel(2), b: channel(4) };
}

/**
 * @param value one gamma-encoded sRGB channel in 0..1
 * @returns the linear-light channel
 */
export function srgbToLinear(value: number): number {
  return value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
}

/**
 * @param colour a `#rrggbb` literal
 * @returns its linear-light channels
 */
function linearRgb(colour: string): Rgb {
  const { r, g, b } = parseHex(colour);
  return { r: srgbToLinear(r), g: srgbToLinear(g), b: srgbToLinear(b) };
}

/**
 * @param colour a `#rrggbb` literal
 * @returns the WCAG 2.1 relative luminance
 */
export function relativeLuminance(colour: string): number {
  const { r, g, b } = linearRgb(colour);
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

/**
 * @param foreground the text or graphic colour
 * @param background the ground it stands on
 * @returns the WCAG 2.1 contrast ratio, 1..21
 */
export function contrastRatio(foreground: string, background: string): number {
  const a = relativeLuminance(foreground);
  const b = relativeLuminance(background);
  return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
}

/**
 * @param colour a `#rrggbb` literal
 * @returns the colour in OKLab (Björn Ottosson, 2020)
 */
export function toOklab(colour: string): Lab {
  const { r, g, b } = linearRgb(colour);
  const l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
  const m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
  const s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
  return {
    l: 0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s,
    a: 1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s,
    b: 0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s,
  };
}

/**
 * @param a a coordinate of the opponent axis red-green
 * @param b a coordinate of the opponent axis yellow-blue
 * @returns the hue angle in degrees, 0..360
 */
function hueDegrees(a: number, b: number): number {
  const angle = Math.atan2(b, a) * DEGREES;
  return angle < 0 ? angle + 360 : angle;
}

/**
 * @param colour a `#rrggbb` literal
 * @returns the colour in OKLCH, the hue in degrees
 */
export function toOklch(colour: string): Lch {
  const { l, a, b } = toOklab(colour);
  return { l, c: Math.hypot(a, b), h: hueDegrees(a, b) };
}

/**
 * @param first a hue in degrees
 * @param second a hue in degrees
 * @returns the shorter angle between them, 0..180
 */
export function hueDistance(first: number, second: number): number {
  const difference = Math.abs(first - second) % 360;
  return difference > 180 ? 360 - difference : difference;
}

/**
 * @param colour a `#rrggbb` literal
 * @returns the colour in CIELAB under the D65 white point
 */
export function toLab(colour: string): Lab {
  const { r, g, b } = linearRgb(colour);
  const x = (0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / D65_WHITE.x;
  const y = (0.2126729 * r + 0.7151522 * g + 0.072175 * b) / D65_WHITE.y;
  const z = (0.0193339 * r + 0.119192 * g + 0.9503041 * b) / D65_WHITE.z;
  const epsilon = 216 / 24389;
  const kappa = 24389 / 27;
  const f = (t: number): number => (t > epsilon ? Math.cbrt(t) : (kappa * t + 16) / 116);
  const fx = f(x);
  const fy = f(y);
  const fz = f(z);
  return { l: 116 * fy - 16, a: 500 * (fx - fy), b: 200 * (fy - fz) };
}

/**
 * The CIEDE2000 colour difference of two CIELAB colours, with the parametric factors at 1
 * (Sharma, Wu and Dalal, 2005).
 *
 * @param first a CIELAB colour
 * @param second a CIELAB colour
 * @returns ΔE00
 */
export function ciede2000(first: Lab, second: Lab): number {
  const radians = (degrees: number): number => degrees / DEGREES;
  const pow7 = (value: number): number => value ** 7;
  const twentyFive7 = pow7(25);

  const meanChroma = (Math.hypot(first.a, first.b) + Math.hypot(second.a, second.b)) / 2;
  const g = 0.5 * (1 - Math.sqrt(pow7(meanChroma) / (pow7(meanChroma) + twentyFive7)));
  const a1 = (1 + g) * first.a;
  const a2 = (1 + g) * second.a;
  const c1 = Math.hypot(a1, first.b);
  const c2 = Math.hypot(a2, second.b);
  const h1 = c1 === 0 ? 0 : hueDegrees(a1, first.b);
  const h2 = c2 === 0 ? 0 : hueDegrees(a2, second.b);

  const deltaL = second.l - first.l;
  const deltaC = c2 - c1;
  let deltaHue = 0;
  if (c1 * c2 !== 0) {
    deltaHue = h2 - h1;
    if (deltaHue > 180) deltaHue -= 360;
    else if (deltaHue < -180) deltaHue += 360;
  }
  const deltaH = 2 * Math.sqrt(c1 * c2) * Math.sin(radians(deltaHue) / 2);

  const meanL = (first.l + second.l) / 2;
  const meanC = (c1 + c2) / 2;
  let meanH = h1 + h2;
  if (c1 * c2 !== 0) {
    if (Math.abs(h1 - h2) <= 180) meanH = (h1 + h2) / 2;
    else if (h1 + h2 < 360) meanH = (h1 + h2 + 360) / 2;
    else meanH = (h1 + h2 - 360) / 2;
  }

  const t =
    1 -
    0.17 * Math.cos(radians(meanH - 30)) +
    0.24 * Math.cos(radians(2 * meanH)) +
    0.32 * Math.cos(radians(3 * meanH + 6)) -
    0.2 * Math.cos(radians(4 * meanH - 63));
  const deltaTheta = 30 * Math.exp(-(((meanH - 275) / 25) ** 2));
  const rc = 2 * Math.sqrt(pow7(meanC) / (pow7(meanC) + twentyFive7));
  const sl = 1 + (0.015 * (meanL - 50) ** 2) / Math.sqrt(20 + (meanL - 50) ** 2);
  const sc = 1 + 0.045 * meanC;
  const sh = 1 + 0.015 * meanC * t;
  const rt = -Math.sin(radians(2 * deltaTheta)) * rc;

  const lightness = deltaL / sl;
  const chroma = deltaC / sc;
  const hue = deltaH / sh;
  return Math.sqrt(lightness ** 2 + chroma ** 2 + hue ** 2 + rt * chroma * hue);
}

/**
 * @param first a `#rrggbb` literal
 * @param second a `#rrggbb` literal
 * @returns the CIEDE2000 difference of the two sRGB colours
 */
export function deltaE00(first: string, second: string): number {
  return ciede2000(toLab(first), toLab(second));
}
