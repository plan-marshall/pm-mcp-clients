envelope_version=1
sender_type=plan
sender_id=web-reports-fourth-residue
epic=ui-improvements
kind=candidate-lesson
created=2026-11-10T06:48:15Z

component=pm-operator
category=bug

# Candidate lesson: A narrowed flag must be re-described on every surface that documents it

- **Source**: plan web-reports-fourth-residue, PR #394. Q-Gate findings 957fd1 (`doc/design/terminal-client/24-reports.adoc:20`, code-review round 1), feb322 (`doc/design/terminal-client/01-commands.adoc:88`, round 3), 75a298 (`pm-operator/.../ReportsCommand.java:198` help text, round 3).

## What happened

`reports read --all` changed meaning: it now fetches the first reports page and submits those ids, instead of marking every unread report. The implementation and its tests moved together, but three surfaces kept the old promise:

1. The design page `24-reports.adoc` still described `--all` as every unread report — found in code-review round 1.
2. The command catalogue `01-commands.adoc` still said `--all` marks the last listing — found in round 3.
3. The CLI help string in `ReportsCommand.java` still advertised marking all unread reports — found in round 3.

Each stale sentence was a separate review round. The same two surfaces (help text and catalogue) were only re-read after the first two were fixed.

## Rule worth considering

When a flag's semantics narrow, treat every surface that states the old semantics as part of the same change: the argparse help string, the command catalogue, the design page, and any user-facing label. Grep the flag name and the old promised phrase across docs and source in one pass, and add a test that asserts the help/catalogue wording matches the implemented behaviour. A behaviour change plus a doc sweep is one deliverable; a behaviour change alone leaves a false promise that review will find one round at a time.
