envelope_version=1
sender_type=orchestrator
sender_id=design-review
epic=ui-improvements
kind=finding
created=2026-11-06T16:49:01Z
revision=8
amended=2026-11-06T19:53:14Z

# Mascot: the marshal bear as illustration and as the brand icon

Sender: operator design review, 2026-11-06.

## Operator feedback
plan-marshall has a mascot: a bear in a brown cowboy hat with a gold "MARSHALL" sheriff star, a vest, and a clipboard "PLAN" with the checklist Solution ✓, Implement ✓, Test ✓, Verify ✓, Push ☐, Report ☐. Source: `plan-marshall/doc/resources/planmarshall.png` (repository `plan-marshall`; 1122 × 1402 px, RGBA with a transparent background, 1.4 MB). Task: integrate it in a spiffy way, as a background image where sensible, and give the symbol at the upper left (rail head) an iconified version. At best, variants of the mascot mark different states. Be very careful not to spoil the design.

## Current state
- **Brand mark.** `doc/design/web-app/brand-mark.adoc` (`AGREED`) defines the project's own sign: three ascending plan sheets, a baton and a node (`pm-web-app/src/lib/icons/brand/plan-marshall-mark.svg`, `currentColor`). It is used in four places:
  - the rail head: 24 px in a 28 px violet plate, beside the wordmark;
  - the pairing page: 64 px;
  - empty states: muted;
  - the favicon: `static/brand-mark.svg` and `brand-mark-attention.svg` with the attention dot, applied by `src/lib/favicon.ts`.
- **Provenance.** The provenance section of brand-mark.adoc requires original artwork with a recorded source.
- **Brand colour.** `--color-brand` is violet (`#5B4BDB`, lifted in dark). The mascot's palette is warm: orange fur, brown hat and vest, gold star, cream clipboard. Orange and violet are near-complementary, so the pair works as long as the mascot stays an illustration and violet stays the UI accent.
- **Empty states.** `components/EmptyState.svelte` (an icon at 24 px or the brand mark at 64 px, title, sentence, act; brand-mark.adoc's uses table says 24 px muted for empty states) is the one component for an empty list. Today only Reports uses it.
- **Delivery constraints.** The daemon's CSP admits `img-src 'self' blob:` only, and `vite.config.ts:122-125` sets `assetsInlineLimit: 0`, so every image must be a file of the app's own origin. A 1.4 MB PNG is too heavy for the first screen.
- **Unused space.** The rail below its last group is empty on every view (seen in the browser at 1271 × 952: about 500 px of empty rail between "Einstellungen" and the language switch).

## The design

### 1. Where the mascot appears (and where it never does)
The mascot is **decoration on calm surfaces**: screens with little text, a single message, and nothing to read closely. It is **never behind data**: no lists, logs, forms, decisions, tables, the phase tree or code. A watermark there costs contrast (PLAN-70's readability checks) and competes with state colours.

| Surface | How | Size |
|---|---|---|
| **Pairing page** (`PairingView`) | Hero illustration beside the pairing code and QR code. The first thing a new operator sees. | full figure, ≈ 280–360 px tall |
| **Onboarding welcome and done** (`OnboardingView`, first and last step) | Full figure. On "done" the clipboard's ticks fit the message "your machine is ready". | ≈ 240 px |
| **Empty states** via `EmptyState` (option `mascot`) | Head-and-shoulders crop above title + sentence + act. Used for: What waits "Nothing waits for you" (PLAN-49), no running work, no open decisions, no unread reports, no projects yet. | ≈ 120–160 px |
| **Terminal states** (`RevokedView`, `ListenerOffView`, an unknown route, connection lost) | Full figure, quieter (smaller, beside the text). | ≈ 160–200 px |
| **Rail foot (background)** | The bear **peeking up from the bottom edge of the rail**, cropped at the chest so the clipboard is hidden. A background image of the rail column above the rail foot controls (language / theme today; only the collapse control once PLAN-85 lands), low emphasis (opacity ≈ 0.18 light / 0.12 dark), purely decorative. Hidden when the rail is collapsed (64 px), when the rail's free height is below ≈ 260 px, and on the phone. | rail width, ≈ 200 px tall |

- **Rail head**: see 3.
- **Not used:** the top bar, object pages, list rows, dialogs, toasts, the favicon at 16 px (the icon of 3 is used there).

### 2. Asset rules
- **Derived files**, built once from the source and committed under `pm-web-app/src/lib/assets/mascot/`:
  - `mascot-full` (whole figure) and `mascot-bust` (head and shoulders, for empty states and the rail foot);
  - each as WebP at 1× and 2× (e.g. 360 / 720 px tall for full, 160 / 320 for bust), with a PNG fallback only if a supported browser needs it;
  - target ≤ 60 KB per file.
  - Imported through Vite so they are emitted as files of the app's origin (CSP).
- **Loading:** `loading="lazy"` and `decoding="async"`. Width and height are set, so nothing shifts on load. The pairing page loads `mascot-full` eagerly, because it is the hero there.
- **Accessibility:** the mascot is decorative (`alt=""`, `aria-hidden="true"`, CSS background for the rail foot). It is never the only carrier of a message: the empty state's title and sentence say everything.
- **Themes:** the PNG has a transparent background and dark outlines. On dark surfaces it sits on a soft radial halo (`--color-bg-subtle`) so the dark brown outline does not sink into the background. No filters that recolour it. The illustration is the same in both themes.
- **Motion:** at most one subtle entrance on the pairing page and the onboarding done step (fade/rise ≤ 300 ms), none when reduced motion is requested. The rail foot never moves.
- **Clipboard text:** the clipboard is baked-in English ("PLAN · Solution · Implement · Test · Verify · Push · Report"). As an illustration it stays untranslated, and it is never quoted as UI text. A later variant with a blank clipboard (or the product's own phases) can replace it without changing anything else.

### 3. The brand icon: two candidates, the operator chooses
The mascot gives two candidates for the icon at the upper left (rail head), on the pairing page and as the favicon. **The design package draws both and presents them side by side. The operator chooses one.** Whichever is chosen **replaces** the current abstract mark (sheets, baton, node), so there is one brand sign.

**Candidate A, the bear head.** The bear head under the cowboy hat with the star on the hat, without the clipboard. Hand-simplified into a few shapes (hat, ears, face, muzzle, star), not auto-traced.
- It carries the mascot's character and is unmistakably plan-marshall.
- At ≤ 16 px the face is too fine, so the favicon shows only the hat with the star.

**Candidate B, the clipboard.** The mascot's clipboard as the sign: a board with a clip and three ticked lines, the **clip drawn as the sheriff star** (or the star as a badge on the board), so it is the marshal's plan and not a generic clipboard.
- It says "plan" directly, keeps the idea of the current mark (plan sheets), is a simple silhouette that stays legible at 16 px, and works in one colour.
- Risk: it is close to the kind icons `kind-plan`, `kind-report` and `kind-task` (documents and checks; see the KindMark set of `design-review-003.md`). The star clip must make it clearly distinct, and the kind icons must not use a clipboard shape.
- The favicon is the same drawing at 16 px.

**Both candidates follow the same rules:**
- 24 × 24 viewBox. The **monochrome** rendering follows the SVG contract of `workflow-visual-language.adoc:26-31` (`currentColor`, no literal colour). The **colour** rendering is a documented exception to that contract and to brand-mark.adoc's "The Brand Colour" (`--color-brand`), recorded in the rewritten brand-mark.adoc.
- **Colour rendering** (mascot palette, tokens `--color-mascot-*`, the same in both themes) in the rail head without the violet plate and on the pairing page.
- **Monochrome rendering** (`currentColor`) for one-colour uses (muted glyph, forced colours / high contrast).
- The favicon keeps the attention dot (`favicon.ts`; `brand-mark-attention.svg` redrawn on the chosen base).
- The wordmark `plan-marshall` stays as it is beside the icon.

**The presentation for the decision** (one review sheet, both candidates in the same layout):
- each at 16 (favicon, with and without the attention dot), 24, 28 (rail head) and 64 px (pairing);
- in colour and monochrome;
- in light and dark;
- in the real rail head (expanded and collapsed) and in a browser tab bar beside other tabs;
- next to the kind marks of 003, to show the distance from `kind-plan`, `kind-report` and `kind-task`.

The sheet is rendered from the real SVGs on the mock runtime (screenshots), not as a mood board; for the comparison with the kind marks, D3 draws draft `kind-project` and `kind-plan` SVGs (D1 owns the final ones). The chosen candidate's SVGs are kept, the other's are discarded, and brand-mark.adoc records the choice as the design (construction, colours, uses, provenance).

### 4. Provenance
brand-mark.adoc requires a recorded source and original artwork. The mascot's origin (who drew it, how it was made) and the right to use it in the product are recorded in the rewritten brand-mark.adoc before any asset ships. The vector icon of 3 is new artwork derived from the mascot and is recorded the same way. The source PNG stays in `plan-marshall/doc/resources/`, and only the derived files are copied into `pm-web-app`.

### 5. Variants per state, and the restraint that keeps the design clean
The mascot comes as a **small, closed set of poses**. Each pose belongs to one calm-surface state:

| Pose | State | Surface |
|---|---|---|
| Waving, clipboard under the arm | Welcome / not yet paired | Pairing page, onboarding welcome |
| Clipboard held up, every box ticked (the source image, with all ticks set) | Ready / done | Onboarding done, project onboarding done (the same surface as onboarding done in §1) |
| Relaxed, leaning, clipboard down | Nothing waits / all clear | What waits empty, no open decisions, no unread reports |
| Pointing to the side (toward the act) | Empty, start here | No projects yet, no running work (the act "Start a plan" / "Add a project" beside it) |
| Looking out under the hat brim, hand above the eyes | Searching / not found / no connection | Connection lost, listener off, unknown route |
| Star badge held out, calm | Not allowed / access ended | Revoked device |

Rules (they keep the mascot from spoiling the design):
- **Only calm surfaces.** A pose appears only on the surfaces of 1. It never marks running work, a waiting plan, a failure in a list, a decision, a toast or a notice. Those states keep their state colours, words and icons.
- **Never the carrier of the state.** The title and sentence say the state. The pose only echoes it. Without the image the screen means exactly the same.
- **One mascot per screen**, never two poses side by side, and the rail-foot bear is hidden while the view shows a mascot figure or a mascot empty state, never the mascot and a large illustration together. **No waiting mark on a screen that shows a figure** (pairing, onboarding, terminal states): there the figure's surface uses the neutral `Spinner` for its own waits.
- **The rail foot and the icon never change with state.** The rail-foot bear is always the neutral peeking bust, and the rail-head / favicon icon is always the same drawing (the favicon keeps only its attention dot). A changing mascot on every screen would become a second, noisy status indicator.
- **No humour at the operator's expense on a failure.** Searching and not-allowed poses stay calm and friendly, never comic despair.
- **One consistent set:** same proportions, line weight, palette and light as the source. Drawn and reviewed together as one sheet before any pose ships. A pose that does not match the set is left out rather than shipped.
- **Closed set:** these six poses. A new pose needs a design change, not just an asset.
- **Fallback:** until a pose exists, its surface uses the source figure (full or bust). The design works with the single source image alone.

### 6. The waiting mark
- **What it is:** the chosen brand icon of 3, subtly animated.
  - Clipboard: its three lines tick one after another.
  - Bear head: the star on the hat glints.
  - Both candidates are shown animated on the review sheet of 3.
- **Where it appears:** only where the operator waits on the system:
  - the chat's working line (`design-review-009.md`);
  - long checks on screens without a figure (onboarding check steps, model tests); the pairing page and the app's first load keep the neutral `Spinner`;
- **Rules:**
  - Always with a text ("replan liest den Plan…").
  - One per screen.
  - Static with reduced motion.
  - It never marks an object's state (running plans keep their StatusLabel, 006).
- **Short waits** (a button, under about 1–2 s) keep the neutral `Spinner`, whose label comes from the catalogue instead of the literal "Loading…".
- **No mascot as the chat partner:** roles keep their neutral role glyphs.

## Agreed documents that change
- `doc/design/web-app/workflow-visual-language.adoc`: the colour exception for the brand icon.
- `doc/design/web-app/components.adoc`: `WaitingMark` (§6), `EmptyState` with the `mascot` option, `BrandMark` drawing the chosen icon (colour / monochrome; no plate).
- `doc/design/web-app/brand-mark.adoc`: becomes the brand document for the mascot icon (3), the illustration (1, 2, 4) and the poses with their rules (5).
- `doc/design/web-app/README.adoc`: frame/rail head (:138) and the rail foot background.
- `doc/design/web-app/entry.adoc` and `onboarding.adoc`: pairing hero, onboarding welcome/done illustration.

## Affected plans
- **PLAN-85 theme in browser settings (staged):** it removes theme and language from the rail foot (so the rail-foot bear sits above the rail's collapse control instead) and adds the theme / language choice to the pairing and revoked pages, where the figure appears; both layouts are checked together.
- **PLAN-49 web-attention-inbox:** its empty state "Nothing waits for you" uses `EmptyState` with the mascot.
- **PLAN-64 / PLAN-66 onboarding:** the welcome and done steps carry the full figure. Until onboarded, the header shows brand, machine, language and theme (PLAN-66), so the brand there is the chosen icon.
- **PLAN-70 light-theme-pass:** the pairing page, empty states and the rail foot are in its screenshots. The rail-foot opacity is checked against the text above it in both themes.
- **PLAN-72 token-cleanup-lint:** the `--color-mascot-*` tokens fall under the token lint.
- **PLAN-74 design-docs-closure:** closes the docs after the brand change.
- The unified navigation (`design-review-003.md`) is not affected. The KindMark tints must stay clear of the mascot's orange and gold, because the mascot appears on empty states next to kind marks.

## Suggested action (the epic decides)
1. **Design package:** rewrite brand-mark.adoc (icon, illustration, surfaces table of 1, asset rules of 2, provenance of 4), plus the document changes above, then operator review to `AGREED`. Both icon candidates of 3 (bear head, star clipboard) are drawn and shown on the review sheet of 3 for the operator's choice before AGREED, and the pose sheet of 5 is reviewed as one set on the real surfaces.
2. **Web package** (one runbook-sized package after AGREED):
   - the derived assets (source figure, bust, and each pose that exists);
   - `BrandMark` on the chosen icon and the favicons;
   - the `EmptyState` mascot option;
   - pairing hero, terminal states, rail foot background (with the collapse and height rules);
   - snapshot and e2e updates, plus a browser check in de/en and light/dark at 1280 px and on the phone.
3. The onboarding illustrations ride with PLAN-64/66, and the What-waits empty state with PLAN-49.

## Operator decisions (2026-11-06)
- **Icon:** A (bear head) or B (star clipboard) is chosen on the review sheet of 3. Both are drawn.
- **Provenance:** the mascot is the operator's own work, free to use in the product and to modify (poses, icon). The rewritten brand-mark.adoc records this.
- **Rail-foot bear:** wanted, as a reversible test. It uses a fixed opacity that passes the contrast check, and removing it touches no other screen.
- **Poses:** the D3 session writes a brief per pose (what it shows, its surface, its rules); the pose artwork, raster in the style of the source image, is supplied by the operator or by an image tool the operator names, as one set for review on the sheet. Until a pose is accepted, its surface uses the source image (full figure or bust).

## Process
The design of this message is produced **before any implementation**, in its own Claude Code session: design session **D3 (brand and mascot)** of `design-review-010.md`.
- That session sets the documents it creates or changes to `Status: DRAFT`, reviews them (`ref-documentation` docs review, `ref-asciidoc` validate and verify-links) and opens a pull request.
- The operator sets them to `AGREED` as its last commit, and the pull request is merged on the operator's word.
- Only after that merge do the implementation plans named above start or restart.

Order, document ownership, open points, what each plan waits for, and the merged per-plan instructions are in `design-review-010.md`.
