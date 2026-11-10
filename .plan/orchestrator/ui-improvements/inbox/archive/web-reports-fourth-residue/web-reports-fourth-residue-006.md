envelope_version=1
sender_type=plan
sender_id=web-reports-fourth-residue
epic=ui-improvements
kind=candidate-lesson
created=2026-11-10T06:48:33Z

component=plan-marshall:tools-script-executor
category=anti-pattern

# Candidate lesson: Classify an invocation failure before retrying — script shape and environment are not code defects

- **Source**: plan web-reports-fourth-residue, work + script-execution logs. Eight distinct failing script notations across the run.

## What happened

Two different failure classes were recorded beside genuine work:

1. **Argparse rejections (`exit_code: 2`)** — the call shape, not the work, was wrong. Observed shapes: `manage-architecture` was rejected seven times across `which-module --path`, `derived`, `resolve --command module-tests`, `resolve --command quality-gate`, and `find --pattern`; `manage-change-ledger query` was passed `--plan-id`, which the script does not declare; `build-npm` was called with the invented verb `resolve-test-scope`; `ci pr merge-queue` was rejected; `git-workflow switch-and-pull` omitted the required `--base`; `manage-findings qgate list` omitted the required `--phase`; a `manage-status` call was rejected for an unrecognized argument.
2. **Environment failure (`exit_code: -1`, `error: execution_failed`)** — `build-maven:maven` reported `OS error: [Errno 1] Operation not permitted` after ~647 s while running `verify -pl pm-operator -am`. That is a sandbox/permission signal about the launching context, not a statement about the code.

Treating either class as a build defect leads to editing code to satisfy a tool that never looked at it.

## Rule worth considering

Before changing anything after a script failure, read the `failure_kind`: `argparse_rejection` means quote the subcommand and flags from the script's own `--help` / canonical invocation block (and note that `--plan-id` placement is per-script — some scripts declare it before the verb, some after, and some do not declare it at all); an OS-level `execution_failed` means re-run through the sanctioned build path and verify the launching context, never edit source in response. Both classes are recorded as `script_failure` rows, so the marker alone is not evidence of a code defect.
