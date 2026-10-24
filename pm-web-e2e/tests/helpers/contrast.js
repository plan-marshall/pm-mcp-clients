/**
 * WCAG 2.1 Contrast Helper for Playwright E2E tests.
 *
 * Implements relative-luminance contrast ratio calculations, alpha compositing across
 * DOM ancestors, font-size/font-weight threshold classification, and automated page audits.
 */

/**
 * Sets the theme explicitly on the document element.
 *
 * @param {import('@playwright/test').Page} page
 * @param {'light' | 'dark'} theme
 */
export async function setTheme(page, theme) {
  await page.evaluate((t) => {
    document.documentElement.setAttribute('data-theme', t);
  }, theme);
  // Allow any CSS transitions (e.g. 0.15s transition: background-color) to complete
  await page.waitForTimeout(250);
}

/**
 * In-browser contrast analysis function passed to page.evaluate.
 * Returns an array of text elements with their computed colors, contrast ratios, and thresholds.
 */
export const IN_BROWSER_CONTRAST_ANALYZER = `
(() => {
  function parseRgb(colorStr) {
    if (!colorStr) return null;
    const str = colorStr.trim();
    if (str === 'transparent') return { r: 0, g: 0, b: 0, a: 0 };
    if (str.startsWith('#')) {
      const hex = str.slice(1);
      if (hex.length === 3 || hex.length === 4) {
        return {
          r: parseInt(hex[0] + hex[0], 16),
          g: parseInt(hex[1] + hex[1], 16),
          b: parseInt(hex[2] + hex[2], 16),
          a: hex.length === 4 ? parseInt(hex[3] + hex[3], 16) / 255 : 1,
        };
      }
      if (hex.length === 6 || hex.length === 8) {
        return {
          r: parseInt(hex.slice(0, 2), 16),
          g: parseInt(hex.slice(2, 4), 16),
          b: parseInt(hex.slice(4, 6), 16),
          a: hex.length === 8 ? parseInt(hex.slice(6, 8), 16) / 255 : 1,
        };
      }
    }
    const match = str.match(/^rgba?\\(\\s*([\\d.]+)[,\\s]+([\\d.]+)[,\\s]+([\\d.]+)(?:[,\\s/]+([\\d.%]+))?\\s*\\)$/i);
    if (!match) return null;
    let a = 1;
    if (match[4] !== undefined) {
      a = match[4].endsWith('%') ? parseFloat(match[4]) / 100 : parseFloat(match[4]);
    }
    return {
      r: Math.round(parseFloat(match[1])),
      g: Math.round(parseFloat(match[2])),
      b: Math.round(parseFloat(match[3])),
      a: isNaN(a) ? 1 : a,
    };
  }

  function compositeOver(fg, bg) {
    const a = fg.a;
    return {
      r: Math.round(fg.r * a + bg.r * (1 - a)),
      g: Math.round(fg.g * a + bg.g * (1 - a)),
      b: Math.round(fg.b * a + bg.b * (1 - a)),
      a: 1,
    };
  }

  function srgbToLinear(c) {
    const v = c / 255;
    return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
  }

  function relativeLuminance(rgb) {
    return 0.2126 * srgbToLinear(rgb.r) + 0.7152 * srgbToLinear(rgb.g) + 0.0722 * srgbToLinear(rgb.b);
  }

  function contrastRatio(rgb1, rgb2) {
    const l1 = relativeLuminance(rgb1);
    const l2 = relativeLuminance(rgb2);
    const lighter = Math.max(l1, l2);
    const darker = Math.min(l1, l2);
    return (lighter + 0.05) / (darker + 0.05);
  }

  function getEffectiveBackground(el, defaultBg) {
    let current = el;
    const layers = [];

    while (current && current !== document.documentElement) {
      const style = window.getComputedStyle(current);
      const bg = parseRgb(style.backgroundColor);
      if (bg && bg.a > 0) {
        layers.unshift(bg);
      }
      current = current.parentElement;
    }

    const docStyle = window.getComputedStyle(document.documentElement);
    const docBg = parseRgb(docStyle.backgroundColor) || defaultBg;
    let composite = { ...docBg, a: 1 };

    for (const layer of layers) {
      composite = compositeOver(layer, composite);
    }
    return composite;
  }

  function getSelector(el) {
    if (el.id) return '#' + el.id;
    const testId = el.getAttribute('data-testid');
    if (testId) return '[data-testid="' + testId + '"]';
    let path = [];
    let curr = el;
    while (curr && curr.nodeType === Node.ELEMENT_NODE && curr !== document.body) {
      let part = curr.tagName.toLowerCase();
      if (curr.getAttribute('data-testid')) {
        part = '[data-testid="' + curr.getAttribute('data-testid') + '"]';
        path.unshift(part);
        break;
      }
      if (curr.className && typeof curr.className === 'string') {
        const cls = curr.className.trim().split(/\\s+/).filter(c => !c.startsWith('svelte-'))[0];
        if (cls) part += '.' + cls;
      }
      path.unshift(part);
      curr = curr.parentElement;
    }
    return path.join(' > ') || el.tagName.toLowerCase();
  }

  return function analyzeElements(options = {}) {
    // Dynamically derive root canvas background from active computed styles
    const bodyComputed = window.getComputedStyle(document.body);
    const rootComputed = window.getComputedStyle(document.documentElement);
    const bodyBg = parseRgb(bodyComputed.backgroundColor);
    const rootBg = parseRgb(rootComputed.backgroundColor);
    const tokenBg = parseRgb(rootComputed.getPropertyValue('--color-bg-app'));

    const defaultBg = (bodyBg && bodyBg.a > 0)
      ? bodyBg
      : ((rootBg && rootBg.a > 0)
        ? rootBg
        : ((tokenBg && tokenBg.a > 0)
          ? tokenBg
          : { r: 255, g: 255, b: 255, a: 1 }));

    const results = [];
    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_ELEMENT);

    while (walker.nextNode()) {
      const el = walker.currentNode;
      if (['SCRIPT', 'STYLE', 'SVG', 'NOSCRIPT', 'TEMPLATE'].includes(el.tagName)) continue;

      // Check direct text nodes
      let directText = '';
      for (const child of el.childNodes) {
        if (child.nodeType === Node.TEXT_NODE) {
          directText += child.textContent;
        }
      }
      directText = directText.trim();
      if (!directText) continue;

      const style = window.getComputedStyle(el);
      if (style.display === 'none' || style.visibility === 'hidden') continue;
      if (parseFloat(style.opacity || '1') === 0) continue;
      if (el.getAttribute('aria-hidden') === 'true') continue;

      const rect = el.getBoundingClientRect();
      if (rect.width === 0 || rect.height === 0) continue;

      // Parse text color
      const fgColor = parseRgb(style.color);
      if (!fgColor) continue;

      const effectiveBg = getEffectiveBackground(el, defaultBg);
      const effectiveFg = fgColor.a < 1 ? compositeOver(fgColor, effectiveBg) : fgColor;

      const ratio = contrastRatio(effectiveFg, effectiveBg);

      // Determine large-text threshold
      const fontSizePx = parseFloat(style.fontSize) || 16;
      const fontWeight = parseFloat(style.fontWeight) || 400;
      const isLarge = fontSizePx >= 24 || (fontSizePx >= 18.66 && fontWeight >= 700);
      const threshold = isLarge ? 3.0 : 4.5;

      const roundedRatio = Math.round(ratio * 100) / 100;
      const selector = getSelector(el);

      results.push({
        text: directText.length > 50 ? directText.substring(0, 47) + '...' : directText,
        selector,
        tag: el.tagName.toLowerCase(),
        color: \`rgb(\${effectiveFg.r}, \${effectiveFg.g}, \${effectiveFg.b})\`,
        background: \`rgb(\${effectiveBg.r}, \${effectiveBg.g}, \${effectiveBg.b})\`,
        fontSize: style.fontSize,
        fontWeight: style.fontWeight,
        ratio: roundedRatio,
        threshold,
        passed: ratio >= threshold,
      });
    }

    return results;
  };
})()
`;

/**
 * Runs the contrast audit on the active page.
 *
 * @param {import('@playwright/test').Page} page
 * @param {object} [options]
 * @returns {Promise<{ all: Array<any>, failures: Array<any> }>}
 */
export async function auditPageContrast(page, options = {}) {
  const elements = await page.evaluate(`(${IN_BROWSER_CONTRAST_ANALYZER})(${JSON.stringify(options)})`);
  const failures = elements.filter((e) => !e.passed);
  return { all: elements, failures };
}
