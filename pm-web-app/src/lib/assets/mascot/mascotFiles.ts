/**
 * The derived files of the mascot figure and the one place that maps a pose to them
 * (brand-mark-mascot.adoc, Asset Rules and Poses).
 *
 * A surface names a pose and a crop, never a file: `mascotFiles` resolves them here. Until a pose is
 * accepted, every pose maps to the files of the source figure, so accepting a pose changes its row
 * below and no surface. The files are imported through Vite as files of the app's own origin; the
 * build's `assetsInlineLimit: 0` keeps any of them from becoming a `data:` URL.
 */

import bust1xUrl from './mascot-bust-1x.webp?url';
import bust2xUrl from './mascot-bust-2x.webp?url';
import full1xUrl from './mascot-full-1x.webp?url';
import full2xUrl from './mascot-full-2x.webp?url';

/** The closed set of six poses, in the order of the design: the one source of the union below. */
export const MASCOT_POSES = [
  'welcome',
  'done',
  'all-clear',
  'start-here',
  'searching',
  'access-ended',
] as const;

/** A pose of the mascot. */
export type MascotPose = (typeof MASCOT_POSES)[number];

/** The two crops: the whole figure, or head and shoulders with the top of the clipboard. */
export const MASCOT_CROPS = ['full', 'bust'] as const;

/** A crop of the mascot figure. */
export type MascotCrop = (typeof MASCOT_CROPS)[number];

/** The files of one pose in one crop, with the intrinsic size of the `1×` file. */
export interface MascotFiles {
  /** The `1×` file. */
  readonly src1x: string;
  /** The `2×` file, twice the size of the `1×` file. */
  readonly src2x: string;
  /** The intrinsic width of the `1×` file, in px. */
  readonly width: number;
  /** The intrinsic height of the `1×` file, in px. */
  readonly height: number;
}

/** The files of the source figure, which every pose draws until it is accepted. */
const SOURCE_FILES: Readonly<Record<MascotCrop, MascotFiles>> = {
  full: { src1x: full1xUrl, src2x: full2xUrl, width: 289, height: 360 },
  bust: { src1x: bust1xUrl, src2x: bust2xUrl, width: 258, height: 200 },
};

/** The files each pose draws; no pose is accepted yet, so each one draws the source figure. */
const POSE_FILES: Readonly<Record<MascotPose, Readonly<Record<MascotCrop, MascotFiles>>>> = {
  welcome: SOURCE_FILES,
  done: SOURCE_FILES,
  'all-clear': SOURCE_FILES,
  'start-here': SOURCE_FILES,
  searching: SOURCE_FILES,
  'access-ended': SOURCE_FILES,
};

/**
 * The files a surface draws for a pose in a crop.
 *
 * @param pose the pose the surface names
 * @param crop the crop the surface draws
 * @returns the files of the accepted pose, or of the source figure while the pose is not accepted
 */
export function mascotFiles(pose: MascotPose, crop: MascotCrop): MascotFiles {
  return POSE_FILES[pose][crop];
}
