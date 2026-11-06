envelope_version=1
sender_type=plan
sender_id=plan-18-verify-shared-foundation
epic=ui-improvements
kind=finding
created=2026-11-06T07:46:41Z

# Finding: CLI-CC-V-03 (rule part) fails

**Check** (doc/claude_review/cli/cross-cutting/verification.adoc, CLI-CC-V-03, step CLI-CC-S-04):
"`Display.duration` and `Display.time` pass their table tests in both languages. The overview and the plan view show
the same age for one plan." PLAN-18 ran the rule part only, with the pass criterion: the time and duration rule is
specified once, in `doc/design/shared/texts-and-languages.adoc`, and nowhere restated differently.

**Verdict:** fail (PLAN-18, verified at pm-mcp-clients main 92c18515, 2026-11-06).

**Evidence** (row CLI-CC-V-03 of `doc/claude_review/verification-results/verify-shared-foundation.adoc`, commit
9a429567 on `feature/plan-18-verify-shared-foundation`):

- The rule is specified once: `texts-and-languages.adoc#time` (lines 127-194). An age in a list is `9 d ago` /
  `vor 9 T`; the units are `s`, `min`, `h`, `d`; there is no unit for weeks. No second rule section exists.
- Shared design documents write ages in forms the rule does not allow:
  - `doc/design/shared/work-actions.adoc` line 332 (review wireframe): `Origin      landing of relay-reconnect (#211) · 2 days ago` (the rule gives `2 d ago`).
  - `doc/design/shared/paired-devices.adoc` line 24 (list wireframe): `last used 5 weeks ago` (the rule gives `35 d ago`).
  - `doc/design/shared/project-explorer.adoc`: one more age in weeks (found by content search; line not read).
- The client designs carry the same forms: terminal-client `15-overview.adoc` (4), `17-projects.adoc` (2),
  `18-starting.adoc` (1), `08-transcripts-later.adoc` (1) in weeks; `19-actions.adoc` (2) in days.
- `doc/design/shared/cleanup.adoc` matches are retention settings in days, not rendered times; not counted.

**Concerns:** the shared time rule of the client designs (CLI-CC-S-04). PLAN-18 did not establish which landed plan
wrote the differing examples. Candidates to route it to: PLAN-34 `cli-display-layer` (staged; brings
`Display.time` / `Display.duration`) and PLAN-74 `web-design-docs-closure` (staged; closes the design documents).

**Fix scope (not done by PLAN-18):** rewrite the listed ages in the rule's form (`2 d ago`, `35 d ago`, ...), or, if
weeks are wanted, add the unit to the rule in `texts-and-languages.adoc#time` first.
