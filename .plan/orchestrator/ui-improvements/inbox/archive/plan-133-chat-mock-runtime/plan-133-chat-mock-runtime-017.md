envelope_version=1
sender_type=plan
sender_id=plan-133-chat-mock-runtime
epic=ui-improvements
kind=candidate-lesson
created=2026-11-13T12:27:14Z

# Candidate lesson: review budgets exhausted three times

**Plan:** plan-133-chat-mock-runtime (PR #424, merged as a139ec62)
**Signal:** code-review / functional-review Q-Gate process (reported by the orchestrator of this run)
**Class:** the finalize code-review loop-back budget ran out three times in this run, and each time the operator granted more rounds. The rounds were consumed by findings that arrived one or two per round (code-review rounds at 13:03, 14:26, 15:32, 19:21 and 21:10; functional-review rounds at 11:29, 12:46 and 19:00), plus three CodeRabbit and three SonarCloud waves after the push.

## Records of the class

- Code-review findings spread across rounds: 1f7597, d07ebd, 9d1f58, b6781e, 0e4812, be6610, a9e35e (round 1); d7b83a, c9482f (round 2); bdd6a0, 775633 (round 3); 77e333, 1fbda9 (round 4); b76df0 (round 5, re-observed once).
- Functional-review rounds re-raising the same accepted items: see the messages on the D8 Sonar criterion and on planned test removals.
- Three budget grants by the operator (exact grant records are in the run's decision log).

## Observation

The plan was large (a chat state machine, timing modes, fixtures, an operator client and web client touch points in one PR, 75 files in the first CodeRabbit review). Most budget was spent on classes already named in other messages of this run: state-machine edge cases, lenient validators and contract consumers left behind.

## Candidate corrective action

Size plans so one PR stays reviewable within the default budget (split the mock runtime, the operator client and the web client migration into separate plans), and make the review lens ask for a full pass over a component per round rather than stopping after the first blocking findings, so later rounds confirm fixes instead of finding new defects.
