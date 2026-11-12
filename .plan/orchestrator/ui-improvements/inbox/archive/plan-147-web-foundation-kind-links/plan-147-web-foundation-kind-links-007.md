envelope_version=1
sender_type=plan
sender_id=plan-147-web-foundation-kind-links
epic=ui-improvements
kind=candidate-lesson
created=2026-11-12T18:55:30Z

id=2026-11-12-18-007
component=pm-web-app
category=bug
status=active
created=2026-11-12

# Link and ExternalLink address guards accepted https:host and //host: refuse the malformed or authority form before any origin comparison

## What happened

Plan plan-147-web-foundation-kind-links (PR 420) added `Link` (app-path links only) and `ExternalLink` (web addresses only) to pm-web-app. Two address-guard bugs passed the first rounds and were each found only after a push, each costing a full review, pre-push gate (about 25 min), push, CI, Sonar and bot-review cycle:

1. Code review: `ExternalLink` accepted `https:example.org/pull/3` (scheme without the two slashes), also `http:host`, `HTTPS:host`, `https:/host` and padded variants. Fix: require `//` right after the scheme on the trimmed address, and render the trimmed address that was checked.
2. CodeRabbit: `Link` accepted `//app.invalid/settings` because it resolved the value against a placeholder origin and compared origins before refusing the authority form, so a scheme-relative address on the placeholder host passed. WHATWG parsing also turns `/\host`, and `/` followed by tab/LF/CR and then `/` or `\`, into an authority form. Fix: after trim and the leading `/` check, refuse `/^\/[\t\n\r]*[/\\]/` before any origin comparison.

## Rule

For a URL guard in pm-web-app:

- Refuse the malformed and authority forms by their syntax first; only then compare against a base or origin. A same-origin check against a placeholder base proves nothing about a scheme-relative value that names the placeholder host.
- Validate the trimmed value and render exactly that value, so the anchor carries the string that was checked.
- Test with a table, not single cases: scheme without `//`, mixed-case scheme, `//host`, `///host`, `/\host`, `/` plus tab/LF/CR before `/` or `\`, leading and trailing white space, each against both the placeholder host and a foreign host. The red run on TASK-020 showed 14 such cases failing at once while the foreign-host cases already passed, which is exactly what hides the bug.
