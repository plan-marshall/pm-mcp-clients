envelope_version=1
sender_type=plan
sender_id=plan-127-cli-answer-and-epic-next-residue
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T17:14:49Z

component: pm-mcp-clients:pm-operator
category: anti-pattern
title: Hand-mirrored kind lists and raw kind strings disagree with NeedsOperatorKind (5 review-bot findings)

Observed (PLAN-127, PR #426, 13 review-bot findings fixed in-run): five of them had one root cause, a value set mirrored
by hand or used raw instead of derived from its source:

- StatusCommand.renderPlanRow built overview.needs.<raw kind>, so the alias waiver_request read a key no catalogue has.
- ConsolePrompt.kindWord accepted any catalogue key (title, count, more) as a kind.
- The badge switch mirrored the kinds of NeedsOperatorKind and disagreed on the alias waiver_request.
- OPTION_KEYS mirrored the descriptionKey values of the @Option annotations.
- A test kind inventory was hand-listed, so a missing enum member failed with a null instead of a named gap.

Rule: resolve a wire kind through NeedsOperatorKind.fromValue before building any key, and derive every list of kinds,
keys or options from its definition (enum values(), annotations, catalogue tables) instead of mirroring it by hand.
