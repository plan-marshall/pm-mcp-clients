envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:26:53Z

# Candidate lesson: formatter artefacts re-indent already committed lines

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** finalize process (reported by the orchestrator of this run)
**Class:** the quality-gate build (`./mvnw verify -Ppre-commit`) rewrites files (licence headers, OpenRewrite recipes, import order, formatting). Run over files a fix round touched, it also re-indented lines that were already committed and unrelated to the fix, which put formatter-only hunks into fix commits and into the diff the review bots re-reviewed.

## Records of the class

- Formatter-only re-indentation hunks on committed lines appeared in fix-round commits of this run (no single finding hash; observed by the orchestrator while landing the fix rounds).

## Observation

Formatter-only hunks inflate the reviewed diff, invite new bot comments on unchanged code, and make it harder to check that a fix commit contains only its fix.

## Candidate corrective action

Run the pre-commit formatter before the first push of the branch so committed lines are already in formatter shape, and in fix rounds review the formatter's diff: commit formatter-only changes as their own commit, separate from the fix.
