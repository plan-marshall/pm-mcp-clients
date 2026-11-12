envelope_version=1
sender_type=plan
sender_id=plan-132-chat-contract
epic=ui-improvements
kind=candidate-lesson
created=2026-11-11T18:21:02Z

# Candidate lesson: change-ledger rows keep pre-rebase SHAs, blocking the move from 5-execute to 6-finalize

**Source signal:** script_failure on `plan-marshall:manage-status:manage-status` (3 occurrences, 2026-11-11T14:34:19Z to 14:35:47Z).

**What happened:** The branch was rebased onto `main` during a loop-back. After that, the `kind=change` ledger rows still named the commit SHAs from before the rebase. The 5-execute to 6-finalize transition then refused with `execute_commits_unrecorded`: "10 of 15 worktree-branch commit(s) since diverging from main carry no kind=change ledger row". Running `manage-change-ledger remap` brought this down to "2 of 15". Two more rows had to be appended by hand before the transition passed.

**Possible corrective rule:** Every rebase or force-push path that rewrites branch SHAs (the self-absorb rebase, the loop-back rebase, the branch-cleanup rebase) should run the change-ledger remap itself, right after the rewrite. Today the handshake finds the stale SHAs only later. Commits the remap cannot match by patch identity (squashed or re-authored commits) should be shown together with their append commands at the rebase site, not at the phase transition.

**Evidence:** work log ERROR lines 145db3 (twice) and 4e389c, 2026-11-11T14:34-14:35Z.
