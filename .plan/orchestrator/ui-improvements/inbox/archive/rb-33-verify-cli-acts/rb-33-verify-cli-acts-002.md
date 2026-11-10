envelope_version=1
sender_type=plan
sender_id=rb-33-verify-cli-acts
epic=ui-improvements
kind=finding
created=2026-11-10T07:37:59Z

# Finding: `CLI-UC-V-09` fails on both of its clauses

**Check:** `CLI-UC-V-09` of `doc/claude_review/cli/use-cases/verification.adoc` — "`plan nope` shows 'No plan nope' and
exits `1`. The full-screen overview in `reports-and-system` never shows 'Nothing runs' for a refused request."

**Verdict:** `fail` (both clauses).

**Evidence:** run on commit `0913d6f3`, capture set `PLAN-33`, artifacts under `pm-operator/target/screens/PLAN-33/`.

* The refusal text is `Plan nope not found.`, not `No plan nope`
  (`plan-nope-120x30-en.stdout.txt`; German `plan-nope-120x30-de.stdout.txt`). `plan nope --print` exits `1`, so the exit
  clause holds; the catalogue (`pm-api/src/main/resources/messages/`) holds no `No plan {id}` text.
* The full-screen overview in `reports-and-system` prints `Nothing runs. Start a plan in a project, or enrol one.`
  (`overview-refused-120x30-en.txt`). That scenario serves no `/api/v1/projects` route, so the read is refused;
  `OverviewFullScreenApp.fetchProjects` (`OverviewFullScreenApp.java:486`) swallows the failure and leaves the list
  empty, and `ProjectsOverviewLevel.rows` (`ProjectsOverviewLevel.java:86`) renders the empty state. An error still
  looks like an empty state, which is the defect `CLI-UC-F-15` named.

**Plans concerned:** the overview defect belongs to PLAN-20 (`cli-actions-by-state`), whose deliverable owns the
overview; the refusal text belongs to PLAN-30, which has not landed (the plan the spec's Amendment names for it).

**Nothing was fixed:** this plan verifies only.
