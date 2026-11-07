envelope_version=1
sender_type=plan
sender_id=plan-97-cli-command-tree-drain-and-accept-edit
epic=ui-improvements
kind=candidate-lesson
created=2026-11-07T14:48:18Z

## Candidate lesson: a made-up commit SHA was appended to the append-only change ledger and cannot be remapped

- **Source**: PLAN-97 (plan-97-cli-command-tree-drain-and-accept-edit), change ledger of the plan.
- **Suggested component**: `plan-marshall:manage-change-ledger`.
- **Suggested category**: bug.

### What happened

- An entry with a commit SHA that does not exist in the repository was appended to the plan's change ledger. The SHA was written from memory, not read from git.
- The ledger is append-only and has no verb to remap or correct an entry's SHA, so the wrong value stays in the record.

### Rule worth considering

- Never type a commit SHA into a ledger entry: read it from git (`git rev-parse HEAD` or the commit verb's own output) in the same step that appends the entry.
- The ledger should verify that a supplied SHA resolves in the repository before appending, and offer an append-only correction record (a remap entry that supersedes the bad SHA) so a mistake can be fixed without breaking the append-only rule.
