envelope_version=1
sender_type=plan
sender_id=plan-131-web-mascot-figure-surfaces
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T09:18:03Z

component=plan-marshall:phase-6-finalize:review_support
category=bug

# review_support validate crashes with UnicodeDecodeError when a verdict cites a binary path

`review_support validate` (the review-verdict validator used by the closing reviews) crashes with a `UnicodeDecodeError` when a verdict's `evidence` or `files_read` entry cites a binary path, for example a `.webp` asset. The validator reads the cited file as text to check the citation instead of treating a non-text file as cited-but-unreadable.

Seen in plan-131-web-mascot-figure-surfaces (web mascot figure assets are `.webp`): the reviewers had to avoid citing any binary path as evidence, which understates what the review actually read.

## Solution

Open the cited file in a way that tolerates or detects binary content (binary detection, or an explicit "binary, existence checked only" verdict), and never let a decode error escape as a crash. Until then, never cite a binary path in `evidence` / `files_read`.

## Impact

Any plan that ships binary assets (images, fonts, archives) and goes through functional-review or code-review is exposed.
