envelope_version=1
sender_type=orchestrator
sender_id=orchestrator
epic=ui-improvements
kind=finding
created=2026-11-02T10:58:15Z

# Amendment for PLAN-92 from the CodeRabbit audit of 2026-11-02

Three findings of CodeRabbit on PR #303 (your predecessor PLAN-91) sit outside your spec's text but inside its subject. They are open on `main` at `94bbefe1`; handle them in your pull request or answer each with a reason in the progress record. Keep the pull request under 100 files.

1. `doc/design/terminal-client/04-console.adoc:195-196` (unresolved thread, Minor). The document still says an error in a view-opened dialogue "leaves the terminal in the normal screen; the view is not re-entered, and the console reports the error at the prompt". CodeRabbit says the code re-enters the alternate screen and shows the message in the status row. Read the code, then make the sentence true. Deliverable 2 already corrects line 197 of the same document.
2. `pm-operator/src/test/java/de/planmarshall/operator/WebDevicesCommandTest.java:404-407` (unresolved thread, Minor). `@CsvSource` hard-codes the device ids `dev-2` and `dev-3` as command input. The project rule is generated input (`Generators`); the expected value of a contract stays literal.
3. `SystemFullScreenTest` has no test with a failing `/api/v1/system` and a successful status (no `systemCode` switch), so the status read after a failed snapshot in `refreshSystem` is untested (Trivial). Add a switch for the system response, reset it in `setUp`, and a test that fails the system endpoint, sets the status body to the draining form and asserts the line `draining · 5 left` after `r`.

Also: read CodeRabbit's review BODIES as well as the threads before you merge. PR #303 merged with six findings that sat in a review body only.
