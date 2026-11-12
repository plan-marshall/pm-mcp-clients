import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { get } from 'svelte/store';
import { render, screen } from '@testing-library/svelte';
import MascotFigure, { mascotFiguresOnScreen } from './MascotFigure.svelte';
import { MASCOT_CROPS, MASCOT_POSES } from '../assets/mascot/mascotFiles';
import bust1xUrl from '../assets/mascot/mascot-bust-1x.webp?url';
import bust2xUrl from '../assets/mascot/mascot-bust-2x.webp?url';
import full1xUrl from '../assets/mascot/mascot-full-1x.webp?url';
import full2xUrl from '../assets/mascot/mascot-full-2x.webp?url';
import { blockOf, ruleOf, stylesheetOf } from '../test/stylesheet';
import { reportBox } from '../test/resizeObserver';

/**
 * `MascotFigure` (components-brand.adoc, MascotFigure): one decorative figure of the mascot by pose
 * and crop, drawn from the source files until a pose is accepted, sized by its attributes and
 * styled by its own stylesheet alone.
 */

const COMPONENT_PATH = path.resolve(__dirname, 'MascotFigure.svelte');
const ASSET_DIR = path.resolve(__dirname, '../assets/mascot');

/** The file size each derived file stays within (brand-mark-mascot.adoc, Asset Rules). */
const MAX_FILE_BYTES = 60 * 1024;

/** The source files each crop draws while no pose is accepted. */
const SOURCE_FILES = {
  full: { src1x: full1xUrl, src2x: full2xUrl },
  bust: { src1x: bust1xUrl, src2x: bust2xUrl },
} as const;

/**
 * Generated input: an object URL of the kind the app holds for the disconnected states.
 *
 * @returns a `blob:` URL with a random id
 */
function generatedObjectUrl(): string {
  return `blob:http://localhost/${crypto.randomUUID()}`;
}

describe('MascotFigure', () => {
  const source = fs.readFileSync(COMPONENT_PATH, 'utf-8');
  const stylesheet = stylesheetOf(COMPONENT_PATH);

  describe('the files', () => {
    it.each(MASCOT_POSES.flatMap((pose) => MASCOT_CROPS.map((crop) => [pose, crop] as const)))(
      'draws the source files for the pose %s in the crop %s',
      (pose, crop) => {
        render(MascotFigure, { props: { pose, crop, height: 160 } });
        const figure = screen.getByTestId('mascot-figure');

        expect(figure.getAttribute('src')).toBe(SOURCE_FILES[crop].src1x);
        expect(figure.getAttribute('srcset')).toBe(
          `${SOURCE_FILES[crop].src1x} 1x, ${SOURCE_FILES[crop].src2x} 2x`,
        );
        expect(figure.getAttribute('data-pose')).toBe(pose);
        expect(figure.getAttribute('data-crop')).toBe(crop);
      },
    );

    it('draws the full figure when no crop is named', () => {
      render(MascotFigure, { props: { pose: 'welcome', height: 360 } });

      expect(screen.getByTestId('mascot-figure').getAttribute('data-crop')).toBe('full');
    });

    it.each(['mascot-full-1x.webp', 'mascot-full-2x.webp', 'mascot-bust-1x.webp', 'mascot-bust-2x.webp'])(
      'keeps %s within 60 KB',
      (file) => {
        expect(fs.statSync(path.join(ASSET_DIR, file)).size).toBeLessThanOrEqual(MAX_FILE_BYTES);
      },
    );
  });

  describe('the size', () => {
    it.each([
      ['full', 360, 289],
      ['full', 288, 231],
      ['full', 200, 161],
      ['bust', 200, 258],
      ['bust', 160, 206],
      ['bust', 120, 155],
    ] as const)('sets the %s crop at %i px tall to %i px wide', (crop, height, width) => {
      render(MascotFigure, { props: { pose: 'all-clear', crop, height } });
      const figure = screen.getByTestId('mascot-figure');

      expect(figure).toHaveAttribute('height', String(height));
      expect(figure).toHaveAttribute('width', String(width));
    });
  });

  describe('the loading', () => {
    it('loads lazily and decodes asynchronously by default', () => {
      render(MascotFigure, { props: { pose: 'all-clear', crop: 'bust', height: 140 } });
      const figure = screen.getByTestId('mascot-figure');

      expect(figure).toHaveAttribute('loading', 'lazy');
      expect(figure).toHaveAttribute('decoding', 'async');
    });

    it('loads at once when eager, for the pairing hero', () => {
      render(MascotFigure, { props: { pose: 'welcome', height: 320, eager: true } });
      const figure = screen.getByTestId('mascot-figure');

      expect(figure).toHaveAttribute('loading', 'eager');
      expect(figure).not.toHaveAttribute('decoding');
    });
  });

  describe('the decorative image', () => {
    it('carries an empty alt text and is hidden from assistive technology', () => {
      render(MascotFigure, { props: { pose: 'searching', height: 180 } });
      const figure = screen.getByTestId('mascot-figure');

      expect(figure).toHaveAttribute('alt', '');
      expect(figure).toHaveAttribute('aria-hidden', 'true');
      expect(screen.queryByRole('img')).toBeNull();
    });
  });

  describe('the held object URL', () => {
    it('draws the given object URL and nothing else', () => {
      const objectUrl = generatedObjectUrl();
      render(MascotFigure, { props: { pose: 'searching', height: 180, src: objectUrl } });
      const figure = screen.getByTestId('mascot-figure');

      expect(figure).toHaveAttribute('src', objectUrl);
      expect(figure).not.toHaveAttribute('srcset');
    });

    it('draws nothing and keeps no space for an empty object URL', () => {
      const { container } = render(MascotFigure, { props: { pose: 'searching', height: 180, src: '' } });

      expect(screen.queryByTestId('mascot-figure')).toBeNull();
      expect(container.querySelector('img')).toBeNull();
      expect(container.textContent).toBe('');
    });
  });

  describe('the content security policy', () => {
    it.each([undefined, generatedObjectUrl()])('sets no style attribute and no data: URL, src %s', (src) => {
      const { container } = render(MascotFigure, {
        props: { pose: 'welcome', height: 320, entrance: true, src },
      });
      const figure = screen.getByTestId('mascot-figure');

      expect(container.querySelectorAll('[style]')).toHaveLength(0);
      expect(figure.getAttribute('src')).not.toMatch(/^data:/);
      expect(figure.getAttribute('srcset') ?? '').not.toContain('data:');
    });

    it('carries no literal colour and no raw HTML in the component', () => {
      expect(source).not.toMatch(/#[0-9a-fA-F]{3,8}\b/);
      expect(source).not.toMatch(/\b(rgb|rgba|hsl|hsla)\(/);
      expect(source).not.toContain('{@html');
      expect(source).not.toContain('data:');
    });
  });

  describe('the stylesheet', () => {
    it('sets the dark-theme halo of --color-bg-subtle by its own class', () => {
      expect(ruleOf(stylesheet, ":global([data-theme='dark']) .mascot-figure")).toMatch(
        /background-image:\s*radial-gradient\([^;]*var\(--color-bg-subtle\)/,
      );
    });

    it('fades and rises the figure in once within 300 ms with entrance', () => {
      render(MascotFigure, { props: { pose: 'welcome', height: 320, entrance: true } });

      expect(screen.getByTestId('mascot-figure')).toHaveClass('mascot-figure--entrance');
      expect(ruleOf(stylesheet, '.mascot-figure--entrance')).toMatch(
        /animation:\s*mascot-figure-entrance\s+300ms\b[^;]*;/,
      );
      expect(ruleOf(stylesheet, '.mascot-figure--entrance')).not.toMatch(/infinite/);
      const keyframes = blockOf(stylesheet, '@keyframes mascot-figure-entrance');
      expect(blockOf(keyframes, 'from')).toMatch(/opacity:\s*0\s*;/);
      expect(blockOf(keyframes, 'from')).toMatch(/transform:\s*translateY\(/);
    });

    it('draws no entrance without the prop', () => {
      render(MascotFigure, { props: { pose: 'all-clear', crop: 'bust', height: 140 } });

      expect(screen.getByTestId('mascot-figure')).not.toHaveClass('mascot-figure--entrance');
    });

    it('drops the entrance where reduced motion is asked for', () => {
      // jsdom computes no media, so the rule is read from the component's stylesheet.
      const reduced = blockOf(stylesheet, '@media (prefers-reduced-motion: reduce)');

      expect(ruleOf(reduced, '.mascot-figure--entrance')).toMatch(/animation:\s*none\s*;/);
    });
  });

  describe('the figures on screen', () => {
    it('counts a figure while it has a box, and no longer once it is destroyed', () => {
      const before = get(mascotFiguresOnScreen);
      const { unmount } = render(MascotFigure, { props: { pose: 'welcome', height: 320 } });

      reportBox(screen.getByTestId('mascot-figure'), 257, 320);
      expect(get(mascotFiguresOnScreen)).toBe(before + 1);

      unmount();
      expect(get(mascotFiguresOnScreen)).toBe(before);
    });

    it('does not count a mounted figure that has no box, as under display: none', () => {
      const before = get(mascotFiguresOnScreen);

      render(MascotFigure, { props: { pose: 'welcome', height: 320 } });

      // A hidden figure is reported by no observer, as a browser reports none for it.
      expect(screen.getByTestId('mascot-figure')).toBeInTheDocument();
      expect(get(mascotFiguresOnScreen)).toBe(before);
    });

    it('stops counting a figure that loses its box and counts it again once it is shown', () => {
      const before = get(mascotFiguresOnScreen);
      render(MascotFigure, { props: { pose: 'all-clear', crop: 'bust', height: 160 } });
      const figure = screen.getByTestId('mascot-figure');
      reportBox(figure, 124, 160);

      // The page narrows below its container query, or the figure's ancestor is hidden.
      reportBox(figure, 0, 0);
      expect(get(mascotFiguresOnScreen)).toBe(before);

      // The page widens again, or the ancestor is shown.
      reportBox(figure, 124, 160);
      expect(get(mascotFiguresOnScreen)).toBe(before + 1);
    });

    it('counts a figure once however often its box changes size', () => {
      const before = get(mascotFiguresOnScreen);
      render(MascotFigure, { props: { pose: 'all-clear', crop: 'bust', height: 160 } });
      const figure = screen.getByTestId('mascot-figure');

      reportBox(figure, 124, 160);
      reportBox(figure, 100, 129);

      expect(get(mascotFiguresOnScreen)).toBe(before + 1);
    });

    it('does not count down a figure destroyed while it had no box', () => {
      const before = get(mascotFiguresOnScreen);
      const { unmount } = render(MascotFigure, { props: { pose: 'welcome', height: 320 } });
      const figure = screen.getByTestId('mascot-figure');
      reportBox(figure, 257, 320);
      reportBox(figure, 0, 0);

      unmount();

      expect(get(mascotFiguresOnScreen)).toBe(before);
      // The destroyed figure's observer is disconnected and reports nothing any longer.
      expect(reportBox(figure, 257, 320)).toBe(0);
    });

    it('does not count a figure that draws nothing', () => {
      const before = get(mascotFiguresOnScreen);

      render(MascotFigure, { props: { pose: 'searching', height: 180, src: '' } });

      expect(get(mascotFiguresOnScreen)).toBe(before);
    });
  });
});
