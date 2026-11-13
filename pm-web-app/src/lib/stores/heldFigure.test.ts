import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { get } from 'svelte/store';
import { mascotFiles } from '../assets/mascot/mascotFiles';

/**
 * The held figure of the disconnected states (brand-mark-mascot.adoc, Asset Rules, Figures of the
 * disconnected states): fetched once after the start, held as an object URL for the session, and
 * empty when the fetch did not succeed.
 */

/** The module under test, loaded fresh per test, so its one fetch of the session starts unspent. */
type HeldFigureModule = typeof import('./heldFigure');

/**
 * Generated input: an object URL of the kind the browser hands out for a blob.
 *
 * @returns a `blob:` URL with a random id
 */
function generatedObjectUrl(): string {
  return `blob:http://localhost/${crypto.randomUUID()}`;
}

/**
 * @param ok whether the origin answered with success
 * @returns a response of the origin; a successful one carries a figure file
 */
function originResponse(ok: boolean): Response {
  const file = new Blob([crypto.randomUUID()], { type: 'image/webp' });
  return { ok, status: ok ? 200 : 404, blob: async () => file } as unknown as Response;
}

describe('heldFigure', () => {
  let createObjectURL: ReturnType<typeof vi.fn<(blob: Blob) => string>>;
  let objectUrl: string;
  let module: HeldFigureModule;

  beforeEach(async () => {
    objectUrl = generatedObjectUrl();
    createObjectURL = vi.fn<(blob: Blob) => string>().mockReturnValue(objectUrl);
    // jsdom has no object URLs of its own.
    Object.defineProperty(URL, 'createObjectURL', {
      value: createObjectURL,
      configurable: true,
      writable: true,
    });
    vi.resetModules();
    module = await import('./heldFigure');
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    Reflect.deleteProperty(URL, 'createObjectURL');
  });

  it('holds no URL before the figure was fetched', () => {
    expect(get(module.heldFigure)).toBe('');
  });

  it('fetches the searching figure only once, however often it is asked', async () => {
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async () => originResponse(true));
    vi.stubGlobal('fetch', fetchMock);

    await Promise.all([module.holdFigure(), module.holdFigure()]);
    await module.holdFigure();

    expect(fetchMock).toHaveBeenCalledOnce();
    expect(fetchMock).toHaveBeenCalledWith(mascotFiles('searching', 'full').src2x);
  });

  it('holds the object URL of the fetched file after a successful fetch', async () => {
    vi.stubGlobal('fetch', vi.fn<typeof fetch>().mockImplementation(async () => originResponse(true)));

    await module.holdFigure();

    expect(createObjectURL).toHaveBeenCalledOnce();
    expect(createObjectURL.mock.calls[0][0]).toBeInstanceOf(Blob);
    expect(get(module.heldFigure)).toBe(objectUrl);
  });

  it('stays empty after a fetch that failed, and does not try again', async () => {
    const fetchMock = vi.fn<typeof fetch>().mockRejectedValue(new TypeError('network_offline'));
    vi.stubGlobal('fetch', fetchMock);

    await module.holdFigure();
    await module.holdFigure();

    expect(get(module.heldFigure)).toBe('');
    expect(createObjectURL).not.toHaveBeenCalled();
    expect(fetchMock).toHaveBeenCalledOnce();
  });

  it('stays empty after a fetch the origin answered with an error', async () => {
    vi.stubGlobal('fetch', vi.fn<typeof fetch>().mockResolvedValue(originResponse(false)));

    await module.holdFigure();

    expect(get(module.heldFigure)).toBe('');
    expect(createObjectURL).not.toHaveBeenCalled();
  });
});
