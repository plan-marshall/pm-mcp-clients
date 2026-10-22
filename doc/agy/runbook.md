# Plan-Marshall MCP Clients: Development Runbook & Standard Operating Procedure

This document defines the normative operational rules, lifecycle workflow, quality gates, and skill matrices for implementing and maintaining modules in `pm-mcp-clients`.

Implementation plans (such as `doc/agy/*-plan.md`) must reference this runbook instead of duplicating procedural boilerplate.

---

## 1. Required Skills Matrix (by Work Domain)

Before implementing or reviewing tasks in specific areas of the repository, activate the required domain skills:

### 1.1 Java & Backend Modules (`pm-api`, `pm-operator`, `pm-mock-runtime`, `pm-relay`, `pm-exec`)

| Skill | Focus & Key Standards |
|---|---|
| `pm-dev-java:java-core` | Modern Java standards (Java 25+ / 26), modern language features, concurrency, performance optimization. |
| `pm-dev-java:java-security` | Inbound trust boundaries, input validation, strict file/directory permissions, secrets handling, safe subprocess execution, secure logging. |
| `pm-dev-java:junit-core` | JUnit 5 testing standards, AAA pattern, test isolation, property-based tests, test doubles, deterministic execution. |
| `pm-dev-java:cui-cui-http` | `de.cuioss:cui-http` HTTP client standards, `HttpHandler`, `HttpResult` error handling, ETag caching, retry adapters. |
| `pm-dev-java:cui-cui-logging` | Structured logging via `CuiLogger`, `LogRecord` patterns, DSL-style `LogMessages`, structured JUL formatting. |
| `pm-dev-java:cui-cui-testing` | CUI test generation utilities, contract testing for value objects, JUL log verification. |
| `pm-dev-java:arch-gate-java` | ArchUnit architectural fitness rules and structural boundary verification. |

### 1.2 Frontend & Web Modules (`pm-web-app`, `pm-web-e2e`)

| Skill | Focus & Key Standards |
|---|---|
| `pm-dev-frontend:javascript` | Modern ECMAScript/TypeScript, ES modules, DOM trust boundaries, async patterns, Web Components (Lit/Svelte). |
| `pm-dev-frontend:javascript-security` | DOM trust boundaries, XSS sink avoidance (`innerHTML`/`outerHTML`), DOMPurify sanitization, Trusted Types. |
| `pm-dev-frontend:jest-testing` | Unit testing standards, DOM mocking, async component test suites. |
| `pm-dev-frontend:lint-config` | ESLint flat config, Prettier, Stylelint integration. |
| `pm-dev-frontend:css` | Modern CSS standards, native nesting, cascade layers, Container Queries, custom properties, responsive design, WCAG accessibility. |

### 1.3 Engineering Personas & Methodologies

Plans running under this runbook execute directly within Antigravity or agent sessions without invoking Plan-Marshall orchestrator scripts or pipeline tooling (`manage-personas`, `ext-point-verify`, `TASK-*.json`, etc.).

Instead, the agent loads and adopts the following specialized personas as cognitive lenses, applying their underlying standards during the relevant plan phases:

| Persona Skill | Primary Plan Phasing & Applicability | Key Standards & Focus |
|---|---|---|
| `plan-marshall-persona-implementer` | **Implementation Tasks** (Step 2: writing & refactoring production code) | Clean code principles, Single Responsibility Principle (SRP), Command-Query Separation (CQS), immutability, refactoring triggers, and error handling (via `plan-marshall-ref-code-quality`). |
| `plan-marshall-persona-module-tester` | **Unit Testing** (Step 2: unit & module test suites) | AAA (Arrange-Act-Assert) pattern, test isolation, deterministic fixtures, boundary value analysis, coverage thresholds, avoiding test anti-patterns (via `standards/testing-methodology.md` and `testing-coverage.md`). |
| `plan-marshall-persona-integration-tester` | **Integration & E2E Testing** (Step 2: collaborator verification) | Multi-module interaction testing, real collaborators vs. test doubles, MockWebServer lifecycle, HTTP/REST endpoint contract validation, and network error resilience. |
| `plan-marshall-persona-security-expert` | **Security Review & Hardening** (Step 2: boundary validation & sensitive code) | OWASP Top 10 defenses, STRIDE threat modeling, trust boundaries, inbound input validation, secrets management, safe subprocess execution, secure logging (via `standards/*.md`). |
| `plan-marshall-persona-documenter` | **Documentation Tasks** (Step 2: docs, specs, and runbook updates) | AsciiDoc and Markdown technical documentation authoring, architecture notes, narrative clarity, doc drift prevention, and documentation accuracy. |
| `plan-marshall-persona-code-reviewer` | **Review & Triage** (Step 3: pre-commit review; Step 5: PR review & bot feedback) | Adversarial refutation of candidate findings (defect vs. preference, surplus vs. load-bearing, real vs. false-positive drift, severity sanity via `standards/adversarial-refute.md`), and the code-review spine checklist. |

> [!IMPORTANT]
> **No Plan-Marshall Tooling/Process Dependencies**:
> When an agent loads these persona skills, it must treat them strictly as **cognitive standards and engineering methodologies**. Any references within the persona skills to Plan-Marshall internal plumbing (such as `manage-personas resolve`, `ExtensionBase`, `ext-point-verify`, or `orchestrator` pipelines) are Plan-Marshall marketplace internals and must be **ignored**. Plans driven by this runbook execute directly using the runbook steps, standard build tools (`./mvnw`), and native IDE/editor tools.

---

## 2. General Development Process (The 8-Step Discipline)

Every feature, refactoring, or bug fix must follow this sequential lifecycle:

```
[1. Worktree Creation] -> [2. Incremental Build & Impl] -> [3. Report & Plan Archival]
      (Orchestrator)                  (Worker)                         (Worker)
                                                                           |
[8. Post-Landing Verify] <- [7. Sync & Cleanup] <- [6. Review Window] <- [5. Push & PR] <- [4. Pre-Commit Verification]
      (Orchestrator)              (Worker)             (Worker)            (Worker)                 (Worker)
```

### Session Roles & Boundaries

- **Orchestrator Session**: Prepares Step 1 (worktree creation, checking off Step 1 in plan, initial branch push, dependency pre-flight) per [`doc/meta/emit_rules.adoc`](../meta/emit_rules.adoc) and completes Step 8 (post-landing 4-dimension verification, moving report to `doc/agy/verified/`, updating indexes) per [`doc/meta/verification-protocol.adoc`](../meta/verification-protocol.adoc). The orchestrator never implements production code directly.
- **Worker Session**: Executes Steps 2 through 7 inside its dedicated worktree per [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc). The worker reads `doc/agy/<topic>-plan.md` directly for its Scope, Persona Matrix, and Phased Technical Recipe—the orchestrator does not duplicate these into the launch prompt.
- **Plan Session Boundary**: The worker must ONLY author the completion report at `doc/agy/<topic>-plan-report.md` and move the plan to `doc/agy/implemented/<topic>-plan.md`. The worker **never** moves reports to `doc/agy/verified/` (strictly reserved for Step 8 by the orchestrator).

### Step 1: Isolated Git Worktree Lifecycle (Orchestrator Responsibility)

Never implement directly on `main` within the repository root workspace.

1. Ensure latest `main`:
   ```bash
   git checkout main && git pull origin main
   ```
2. Create a dedicated worktree and topic branch:
   ```bash
   git worktree add -b <branch-type>/<topic-name> /Users/oliver/git/worktrees/<worktree-name> main
   ```
   - Standard branch prefixes: `feat/`, `fix/`, `docs/`, `refactor/`, `test/`, `chore/`.
   - Work exclusively inside the created worktree directory.
3. Check off Step 1 (`- [x] 1. Isolated worktree...`) in `doc/agy/<topic>-plan.md`, commit with co-author trailer, and push branch to origin:
   ```bash
   git commit -am "chore(plan): check off step 1 in <topic>-plan.md

   Co-Authored-By: plan-marshall <noreply@cuioss.de>"
   git push -u origin <branch-type>/<topic-name>
   ```

### Step 2: Incremental Implementation & Plan Tracking

1. When working against a plan (e.g. `doc/agy/*-plan.md`), check off completed items (`- [x]`) as progress is made.
2. Load and adopt the corresponding persona for each specific task type:
   - Apply `plan-marshall-persona-implementer` when writing or refactoring production code.
   - Apply `plan-marshall-persona-module-tester` when authoring unit tests, asserting AAA structure, and verifying coverage.
   - Apply `plan-marshall-persona-integration-tester` when writing integration tests, E2E tests, or MockWebServer scenarios.
   - Apply `plan-marshall-persona-security-expert` when establishing trust boundaries, validating input, or handling credentials.
   - Apply `plan-marshall-persona-documenter` when writing or updating documentation, specifications, ADRs, or runbooks.
3. Build incrementally without `clean` to preserve compiler cache:
   ```bash
   ./mvnw compile -pl <module> -am
   ```
4. Test locally in the affected module using targeted tests.

### Step 3: Plan Completion Report & Archival

Before running the final pre-commit verification and opening the PR:

1. **Write the Final Completion Report**:
   Create a comprehensive completion report in `doc/agy/<topic>-plan-report.md` (e.g. `doc/agy/uc-ent-2-plan-report.md`).
   The report must document:
   - **Executive Summary**: Overview of the implemented use case or work package and its key outcomes.
   - **Delivered Artifacts**: Full listing of created/modified files across all modules (`pm-api`, `pm-mock-runtime`, `pm-operator`, `pm-web-app`, `pm-web-e2e`).
   - **Test & Verification Results**: Outcomes of unit tests, mock scenarios, CLI transcript tests, Vitest component tests, and Playwright E2E tests.
   - **Domain Invariants**: Verification status of all architecture invariants (Java 25, no uncatalogued strings, bilingual catalogue in EN & DE, mock runtime completeness, responsive layout down to 320px).
   - **Build & Quality Evidence**: Evidence of clean `./mvnw verify -Ppre-commit` execution.

2. **Move Completed Plan to `implemented/`**:
   Move the initial plan from `doc/agy/<topic>-plan.md` into the `doc/agy/implemented/` subdirectory:
   ```bash
   mv doc/agy/<topic>-plan.md doc/agy/implemented/
   ```
   Update [`doc/agy/implemented/README.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/README.md) to record the implemented plan and link to its completion report.

   > [!IMPORTANT]
   > **Plan Session Boundary**:
   > The executing plan session must ONLY author the completion report at `doc/agy/<topic>-plan-report.md` and move the completed plan to `doc/agy/implemented/<topic>-plan.md`.
   > **DO NOT** move the completion report into `doc/agy/verified/` during plan execution!
   > Moving the completion report to `doc/agy/verified/` is strictly the responsibility of the orchestrator session in **Step 8** after changes land on `main` and pass post-landing verification analysis.

3. **Roadmap Archival (`doc/plans/`)**:
   If the plan completes an active work package in `doc/plans/`, mark it as `done` in `doc/plans/README.adoc` and move the package file to `doc/plans/implemented/`.

### Step 4: Pre-Commit Verification (Code vs. Documentation Distinction)

Before committing changes, determine the nature of the changes:

- **Standalone Documentation-Only Changes (`doc/**`, `*.md`, `*.adoc`, excluding inline code edits in `.java` / `.ts`)**:
  - **Do NOT execute `./mvnw verify -Ppre-commit`**!
  - Running a 3–4 minute full Maven reactor build, OpenRewrite suite, and unit tests for markdown/AsciiDoc changes wastes time and compute.
  - Instead: verify document formatting, confirm internal links/references are valid, and ensure `git status` shows only documentation files modified.
  - Proceed directly to Step 5 (Commit & Pull Request Creation).

- **Code, Configuration, or Build Changes (Java, TypeScript, POMs, shell scripts, resources, or inline Javadocs)**:
  - Execute the full repository pre-commit gate:
    ```bash
    ./mvnw verify -Ppre-commit
    ```
  - **What this validates**:
    - OpenRewrite recipes (code formatting, imports, style normalization).
    - License header verification and formatting.
    - Checkstyle and Enforcer rules (`clients-depend-on-pm-api-only`, `no-model-facing-content`, etc.).
    - Javadoc generation and validation.
    - Complete unit test suite across all modules in the reactor.
  - **The OpenRewrite output is part of the change**:
    - The recipes run in write mode. Every file the gate reformats — import order, text-block indentation, test-method names, and the other normalizations of the parent's recipe set — is committed together with the change that triggered the gate.
    - **Never revert the reformatting to keep the diff small.** A reverted reformat leaves the repository baseline behind the recipe set, so the next gate run rewrites the same files and the worker has to revert them again. The first change after a parent or recipe bump therefore carries the whole baseline reformat.
    - Commit the reformatted files in the same PR as the change; the requirement below is met when the committed tree is the tree the gate built.
  - **Requirement**: Must complete with `BUILD SUCCESS` and a clean `git status` (no untracked artifacts or uncommitted reformatted files).

### Step 5: Commit & Pull Request Creation

1. Commit with Conventional Commits format and the mandatory co-author trailer:
   ```bash
   git add <modified-files>
   git commit -m "<type>(<scope>): <clear, concise description>

   Co-Authored-By: plan-marshall <noreply@cuioss.de>"
   ```
2. Push topic branch to origin:
   ```bash
   git push -u origin <branch-type>/<topic-name>
   ```
3. Create the Pull Request via GitHub CLI:
   ```bash
   gh pr create --title "<type>(<scope>): <clear summary>" --body "<Detailed description of changes, rationale, and impacted components>"
   ```

   > [!IMPORTANT]
   > **The `skip-bot-review` Invariant**:
   > - **Worker / Implementation PRs**: **NEVER** apply `--label "skip-bot-review"`. All worker PRs modifying code, tests, build configurations, resources, or mock scenarios must undergo full automated reviews by all review bots (CodeRabbit, Sourcery, cuioss-review-bot) and SonarCloud analysis. All findings must be resolved or refuted via `persona-code-reviewer`.
   > - **Orchestrator Housekeeping PRs**: The label `--label "skip-bot-review"` is **STRICTLY RESERVED** for pure documentation/housekeeping PRs submitted by the orchestrator session (e.g., archiving verified completion reports, updating plan indexes, or landing runbook updates).

### Step 6: Review Window & 100% Findings Resolution

There is a **zero-tolerance policy** for unaddressed automated findings or skipped review bots on worker implementation PRs. Workers must NEVER use `skip-bot-review` to bypass review bots.

1. **Mandatory 5-Minute Wait**:
   Immediately after opening a PR (or after pushing any subsequent fix commit), schedule a 300-second timer using the `schedule` tool:
   ```
   schedule(DurationSeconds=300, Prompt="Check CI status and review comments for PR #<PR_NUMBER>", TimerCondition="never")
   ```
   *Do not poll in a loop.* Wait for the notification. This window allows GitHub Actions CI, SonarCloud analysis, and AI review bots (Sourcery, CodeRabbit, cuioss-review-bot) to finish.
   If checks or bots are still running when the timer fires, repeat the wait until all checks are complete.

2. **Inspecting Checks & Reviews**:
   - Check CI status: `gh pr checks <PR_NUMBER>`
   - Inspect PR comments & reviews: `gh pr view <PR_NUMBER> --comments` and `gh api repos/<owner>/<repo>/pulls/<PR_NUMBER>/comments`

3. **100% Findings Resolution Bar (Evaluated via `plan-marshall-persona-code-reviewer`)**:
   - Apply the adversarial-refute lenses from `standards/adversarial-refute.md` (distinguish genuine defects from style preferences, verify whether flagged structure is truly surplus or load-bearing, confirm documentation drift against current sources of truth).
   - **SonarCloud**: Quality Gate must pass (0 new issues, 0 accepted issues, 0 security hotspots, code coverage meeting project threshold, 0% duplication).
   - **Sourcery AI**: All blocking/high/medium findings must be fixed in code.
   - **CodeRabbit AI**: All actionable suggestions and issue reports must be reviewed and addressed.
   - **cuioss-review-bot**: All architectural and security guidelines must be verified.

4. **Explicit Thread Replies**:
   For every inline review comment addressed, reply directly to the comment thread:
   ```bash
   gh api repos/plan-marshall/pm-mcp-clients/pulls/<PR_NUMBER>/comments \
     -f body="Addressed in <commit_sha> by <explanation>." \
     -F in_reply_to=<comment_id>
   ```

5. **Re-Review Cycle**:
   If code changes were pushed to fix review findings, schedule another 300-second timer to ensure the updated commit passes all automated re-reviews cleanly.

### Step 7: GitHub Merge Queue & Workspace Cleanup

The repository's `main` branch is protected by GitHub Rulesets and uses GitHub's Merge Queue with squash merging.

1. Enqueue PR for merge:
   ```bash
   gh pr merge <PR_NUMBER> --auto --squash
   ```
2. Monitor merge queue entry:
   ```bash
   gh api graphql -f query='query { repository(owner:"plan-marshall", name:"pm-mcp-clients") { pullRequest(number: <PR_NUMBER>) { state mergeQueueEntry { position state } } } }'
   ```
3. Once merge-queue checks complete and the PR state transitions to `MERGED`:
4. Synchronize the primary repository workspace:
   ```bash
   git checkout main && git pull origin main
   ```
5. Clean up the isolated worktree and local branch:
   ```bash
   git worktree remove /Users/oliver/git/worktrees/<worktree-name>
   git branch -D <branch-type>/<topic-name>
   ```

### Step 8: Post-Landing Verification & Report Archival (Orchestrator Responsibility)

> [!NOTE]
> This step is executed exclusively by the orchestrator / coordinating session on `main` after a plan's PR has landed, never by the executing plan sessions.

Once changes have landed on `main` and the repository workspace is synchronized:

1. **Conduct 4-Pillar Verification Analysis (Governed by [`doc/meta/verification-protocol.adoc`](../meta/verification-protocol.adoc))**:
   Do not re-execute Maven reactor builds or tests (functional compilation, tests, and static checks were already proven by CI and pre-commit). Instead, rigorously execute the active 4-pillar audit defined in [`doc/meta/verification-protocol.adoc`](../meta/verification-protocol.adoc):
   - **Pillar 1: Specification & Contract Conformance Matrix**: Trace implemented capabilities against upstream `.adoc` specifications, use cases, wire contracts, and closed enums; verify zero stubs or temporary placeholders.
   - **Pillar 2: Architectural & Code Hygiene Anti-Pattern Scans**: Automated module-specific checks (zero `fallbackMessages`, zero uncatalogued strings, closed mock schema enforcement, terminal output sanitizer boundary, pure Jackson streaming).
   - **Pillar 3: Subprocess Lifecycle & Cross-Module Handshake Pre-Flight**: Smoke-test live lifecycles (`start.sh`, `stop.sh`, port bindings, standard scenarios, SSE reset/replay).
   - **Pillar 4: Independent Diff & AST Verification**: Adversarial inspection of git diff against base, verifying module boundary discipline, commit trailers, and absence of accidental regressions.

2. **Move Verified Report to `verified/`**:
   Move the companion completion report from `doc/agy/<topic>-plan-report.md` into `doc/agy/verified/`:
   ```bash
   mv doc/agy/<topic>-plan-report.md doc/agy/verified/
   ```
   Update [`doc/agy/verified/README.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/verified/README.md) and [`doc/agy/implemented/README.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/README.md) to record the verified landing.


---

## 3. Shell & Scripting Invariants

All shell scripts (`start.sh`, `stop.sh`, utilities) must comply with these portability and runtime invariants:

### 3.1 macOS Bash 3.2 & `nounset` (`set -u`) Compatibility

macOS defaults to GNU Bash 3.2. When `set -u` is enabled, expanding an empty array `"${arr[@]}"` triggers `arr[@]: unbound variable`.

- **Rule**: Never expand empty arrays directly under `set -u`.
- **Approved Idiom**: Use the canonical parameter substitution idiom:
  ```bash
  FINAL_ARGS+=(${PASSED_ARGS[@]+"${PASSED_ARGS[@]}"})
  ```
  - If `PASSED_ARGS` is empty, `${PASSED_ARGS[@]+...}` expands to nothing without evaluating the unset array.
  - If `PASSED_ARGS` contains elements, it expands to `"${PASSED_ARGS[@]}"`, preserving quotes and whitespace per element.

### 3.2 Repository-Root Maven Anchoring

When lifecycle scripts run from inside subdirectories (e.g. `cd pm-mock-runtime && ./start.sh`), Maven uses the current working directory as the reactor root, which breaks `-pl <module>` and skips `.mvn/` configuration discovery.

- **Rule**: Always execute `./mvnw` within a subshell anchored to `$REPO_ROOT`:
  ```bash
  (cd "$REPO_ROOT" && ./mvnw compile dependency:build-classpath -Dmdep.outputFile="$TMP_CP" -pl <module> -am)
  ```
- Retain `-am` during compilation and classpath generation to ensure reactor dependencies (e.g. `pm-api`) are resolved without requiring `mvn install`.

### 3.3 Atomic File Publication & Clean Up Traps

Generated classpath and configuration files must never be written directly to live paths. A failed build could leave a corrupt or partial file behind.

- **Approved Pattern**:
  ```bash
  TMP_CP="$CP_FILE.tmp.$$"
  trap 'rm -f "$TMP_CP"' EXIT INT TERM
  (cd "$REPO_ROOT" && ./mvnw compile dependency:build-classpath -Dmdep.outputFile="$TMP_CP" -pl <module> -am)
  mv "$TMP_CP" "$CP_FILE"
  trap - EXIT INT TERM
  ```

### 3.4 Runtime Artifact Locations

- Process IDs (`*.pid`) and logs (`*.log`) must be stored in the module's `target/` directory (e.g. `pm-mock-runtime/target/pm-mock-runtime.pid`), never in the repository root or module root.
- Clean up any legacy root dotfiles (e.g. `.pm-mock-runtime.pid`) if encountered.

---

## 4. Plan Template Checklist

Include this standardized checklist in any new plan (`doc/agy/*-plan.md`):

```markdown
### Workflow Checklist (conforming to `doc/agy/runbook.md`)
- [ ] 1. Isolated worktree created on topic branch off latest `main`.
- [ ] 2. Domain skills & engineering personas loaded where applicable:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for trust boundaries & input validation
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation with tests following the phased technical recipe.
- [ ] 4. Final completion report written (`doc/agy/<topic>-plan-report.md`), initial plan moved to `doc/agy/implemented/<topic>-plan.md`, and package marked done in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation).
- [ ] 6. PR opened (NEVER use `skip-bot-review` on worker PRs; reserved for orchestrator docs PRs), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/<topic>-plan-report.md` by orchestrator session.
```

---

## 5. Canonical Worker Launch Prompt Standard

Because every implementation plan (`doc/agy/<topic>-plan.md`) already defines its scope, isolation boundaries, persona matrix, and phased recipe, orchestrators launch workers using the lean two-liner standard codified in [`doc/meta/emit_rules.adoc`](../meta/emit_rules.adoc):

1. **Working Directory Navigation:**
   ```bash
   cd /Users/oliver/git/worktrees/<worktree-name>
   ```

2. **Canonical Worker Prompt to Paste:**
   ```text
   Execute the implementation plan at `doc/agy/<topic>-plan.md` conforming strictly to `doc/agy/runbook.md`. Step 1 (worktree allocation) is already complete. Load required cognitive personas and proceed from Step 2 onwards.
   ```

> [!IMPORTANT]
> **Anti-Duplication Invariant**: The orchestrator must never summarize or copy-paste plan sections (scope, persona matrix, technical recipes, checklists) into the launch prompt. The plan file is the sole authoritative specification.

