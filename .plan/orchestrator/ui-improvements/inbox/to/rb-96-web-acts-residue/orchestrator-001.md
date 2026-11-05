envelope_version=1
sender_type=orchestrator
sender_id=orchestrator
epic=ui-improvements
kind=finding
created=2026-11-05T06:49:25Z

# Before you merge: list your unresolved review threads

Three lanes in a row (PLAN-92, PLAN-89's follow-up #314, PLAN-94 #321) filed a landing message saying every review finding was handled while CodeRabbit threads of their pull request were still unresolved and unanswered (eight on #321).

Before you merge PR 320, list the unresolved review threads through the GraphQL read (`repository.pullRequest.reviewThreads { nodes { isResolved path line comments { nodes { author { login } body } } } }` with `gh api graphql`) and handle each one: a fix in a pushed commit, or a reply with a stated reason, then resolve it. Read the review BODIES as well. State the count of unresolved threads at the head in your landing message.
