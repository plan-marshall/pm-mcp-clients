envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:26:33Z

# Candidate lesson: baseline merge conflicts with a chat removed elsewhere

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** script-failure cluster and finalize process (reported by the orchestrator of this run)
**Class:** while this plan rebuilt the chat in the mock runtime, `main` moved on (the recent history shows #420, #421, #422 and #425 just before this merge; which of them caused the conflict is not recorded here). The baseline merge into the plan branch conflicted with the old engine chat this plan removed, i.e. a change on `main` still touched the surface this plan retired.

## Records of the class

- The baseline merge conflict itself (engine chat removed by this plan vs. edits on main), resolved during finalize.
- Knock-on effects recorded separately: the PR left CONFLICTING while CI looked green (separate message), and the branch-cleanup merge gate refusing with `merge_candidate_not_gated` after the baseline merge (separate message).

## Observation

The epic ran several plans touching chat-adjacent code in parallel. A plan that retires a surface (the engine/start-state chat) is a conflict magnet for every sibling still editing that surface.

## Candidate corrective action

At epic staging, mark a plan that removes or replaces a shared surface as exclusive for that surface's files, and either sequence siblings that touch it after it lands or merge the baseline into the retiring plan early and repeatedly rather than once at finalize.
