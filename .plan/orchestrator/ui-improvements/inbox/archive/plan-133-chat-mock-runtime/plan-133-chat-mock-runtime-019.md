envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:27:34Z

# Candidate lesson: git-workflow commit refusal on an unstaged tree reported as FETCH_FAILURE

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** script-failure cluster (reported by the orchestrator of this run)
**Component:** plan-marshall:workflow-integration-git (git-workflow commit verb)
**Class:** a script reports a failure under the wrong error class. The commit verb refused because the working tree had unstaged changes, but returned the error as `FETCH_FAILURE`, which reads as a network or remote problem and sends the caller to the wrong recovery.

## Records of the class

- git-workflow commit, called with an unstaged working tree, returned `FETCH_FAILURE`.

## Observation

A misclassified error costs a diagnosis round: the agent first investigates the remote before finding the local tree state.

## Candidate corrective action

The commit verb checks the working-tree state before any fetch and returns a dedicated error (for example `unstaged_changes`, naming the paths) when it refuses for that reason; `FETCH_FAILURE` is reserved for a failed fetch.
