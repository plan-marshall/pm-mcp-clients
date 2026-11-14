envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:28:12Z

# Candidate lesson: remaining script-failure notations of the run (unclassified)

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** script-failure clusters — `manage-logging signal-scan` reports 7 distinct failing notations from 18 `[ERROR] ... script_failure` lines (0 `[FAILED]`, 0 no-progress reclassifications; 8 verification_handback rows excluded as not lesson-bearing)
**Class:** script calls that exited non-zero during the run. Two of the seven notations are covered by their own messages (`phase-6-finalize:review_support`, `manage-status`); the other five were not analysed here and are passed on for orchestrator-side triage.

## Notations

- plan-marshall:build-server-client:build_server
- plan-marshall:lsp-client:lsp_client
- plan-marshall:manage-architecture:architecture
- plan-marshall:manage-findings:manage-findings
- plan-marshall:manage-solution-outline:manage-solution-outline
- (covered elsewhere) plan-marshall:phase-6-finalize:review_support, plan-marshall:manage-status:manage-status

## Observation

The individual failure lines are in the plan's work log (`manage-logging read --type work --plan-id plan-133-chat-mock-runtime`). Some may be argparse rejections (for example `manage-findings qgate list` without the required `--phase`, a rejection this capture step itself hit once) rather than script defects; this message does not decide which.

## Candidate corrective action

Triage each notation's failure lines: classify invocation errors (wrong flags, missing required flags) as agent-side lessons for the calling workflow docs, and script-side errors as defect lessons on the owning component.
