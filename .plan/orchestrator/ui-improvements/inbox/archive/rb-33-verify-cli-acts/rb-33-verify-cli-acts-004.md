envelope_version=1
sender_type=plan
sender_id=rb-33-verify-cli-acts
epic=ui-improvements
kind=finding
created=2026-11-10T09:28:53Z

# Finding: `CLI-UC-V-04` fails on its German clause

**Check:** `CLI-UC-V-04` of `doc/claude_review/cli/use-cases/verification.adoc` — "… The German run has no English word
outside the glossary."

**Verdict:** `fail` (every other clause passes: two options with votes, the dissent, the evidence hint, the prompt, Enter
answers and prints `✓ Q-007 answered: Rebase onto main`; `answer WR-2` asks `y`/`d`/`l`).

**Evidence:** run on commit `0913d6f3`, capture `pm-operator/target/screens/PLAN-21/answer-q007-prompt-120x30-de.txt` (and
its 80 × 24 arm). The client's own chrome is German (`Frage`, `blockiert den Plan`, `zeigt die Belege`, `Stimmen`,
`Empfohlen von`, `Dagegen`, `weitere`, `später`), but the screen also carries English sentences: `The change conflicts
with main in RelayServer.java.`, `How should the conflict be resolved?`, the option labels (`Merge main into the branch`,
`Rebase onto main`, `Stop the plan and ask me later`), `Keeps the history linear.`, `the branch was pushed; a rebase
rewrites what others pulled.`, `(both versions of the method)`. Those are not glossary terms, so the clause fails as
written.

**Plan concerned:** PLAN-21 (`cli-answer-dialogue`), whose dialogue renders the decision, and PLAN-11
(`mock-cli-fixes`), whose fixture supplies the English decision payload (`Q-007` in the `running-work` scenario). Which
of the two the clause means is a decision the plan cannot take: either the payload is expected to be in the screen's
language (then the mock fixture and the runtime's decision texts are the subject), or the check exempts runtime-supplied
text (then the check's wording needs that exemption recorded). The spec's Amendment listed this clause as an expected
`fail` and said "verify against the text"; the text is English.

**Nothing was fixed:** this plan verifies only.
