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

### 1.3 Foundational & Architecture Governance

| Skill | Focus & Key Standards |
|---|---|
| `plan-marshall:persona-plan-marshall-agent` | Foundational agent rules: tool invariants, non-interactive execution discipline, avoiding document proliferation. |
| `plan-marshall:manage-architecture` | Architectural fitness functions, dependency boundaries, module layering. |
| `plan-marshall:manage-lessons` | Global lessons learned resolution, capture, and retrieval. |
| `plan-marshall:recipe-agentfile-hygiene` | Context hygiene rubric and maintenance of agent instructions. |

---

## 2. General Development Process (The 6-Step Discipline)

Every feature, refactoring, or bug fix must follow this sequential lifecycle:

```
[1. Worktree Creation] -> [2. Incremental Build & Impl] -> [3. Pre-Commit Verification]
                                                                     |
[6. Sync & Cleanup]    <- [5. 5-Min Wait & 100% Findings]  <- [4. Push & Open PR]
```

### Step 1: Isolated Git Worktree Lifecycle

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

### Step 2: Incremental Implementation & Plan Tracking

1. When working against a plan (e.g. `doc/agy/*-plan.md`), check off completed items (`- [x]`) as progress is made.
2. Build incrementally without `clean` to preserve compiler cache:
   ```bash
   ./mvnw compile -pl <module> -am
   ```
3. Test locally in the affected module using targeted tests.

### Step 3: Pre-Commit Verification (Code vs. Documentation Distinction)

Before committing changes, determine the nature of the changes:

- **Documentation-Only Changes (`doc/**`, `*.md`, `*.adoc`, inline doc revisions)**:
  - **Do NOT execute `./mvnw verify -Ppre-commit`**!
  - Running a 3–4 minute full Maven reactor build, OpenRewrite suite, and unit tests for markdown/AsciiDoc changes wastes time and compute.
  - Instead: verify document formatting, confirm internal links/references are valid, and ensure `git status` shows only documentation files modified.
  - Proceed directly to Step 4 (Commit & Pull Request Creation).

- **Code, Configuration, or Build Changes (Java, TypeScript, POMs, shell scripts, resources)**:
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
  - **Requirement**: Must complete with `BUILD SUCCESS` and a clean `git status` (no untracked artifacts or uncommitted reformatted files).

### Step 4: Commit & Pull Request Creation

1. Commit with Conventional Commits format:
   ```bash
   git add <modified-files>
   git commit -m "<type>(<scope>): <clear, concise description>"
   ```
2. Push topic branch to origin:
   ```bash
   git push -u origin <branch-type>/<topic-name>
   ```
3. Create the Pull Request via GitHub CLI:
   ```bash
   gh pr create --title "<type>(<scope>): <clear summary>" --body "<Detailed description of changes, rationale, and impacted components>"
   ```

### Step 5: Mandatory 5-Minute Review Window & 100% Findings Resolution

There is a **zero-tolerance policy** for unaddressed automated findings or skipped review bots.

1. **Mandatory 5-Minute Wait**:
   Immediately after opening a PR (or after pushing any subsequent fix commit), schedule a 300-second timer using the `schedule` tool:
   ```
   schedule(DurationSeconds=300, Prompt="Check CI status and review comments for PR #<PR_NUMBER>", TimerCondition="never")
   ```
   *Do not poll in a loop.* Wait for the notification. This window allows GitHub Actions CI, SonarCloud analysis, and AI review bots (Sourcery, CodeRabbit, cuioss-review-bot) to finish.

2. **Inspecting Checks & Reviews**:
   - Check CI status: `gh pr checks <PR_NUMBER>`
   - Inspect PR comments & reviews: `gh pr view <PR_NUMBER> --comments` and `gh api repos/<owner>/<repo>/pulls/<PR_NUMBER>/comments`

3. **100% Findings Resolution Bar**:
   - **SonarCloud**: Quality Gate must pass (0 new issues, 0 accepted issues, 0 security hotspots, code coverage meeting project threshold, 0% duplication).
   - **Sourcery AI**: All blocking/high/medium findings must be fixed in code.
   - **CodeRabbit AI**: All actionable suggestions and issue reports must be reviewed and addressed.
   - **cuioss-review-bot**: All architectural and security guidelines must be verified.

4. **Explicit Thread Replies**:
   For every inline review comment addressed, reply directly to the comment thread:
   ```bash
   gh api repos/plan-marshall/pm-mcp-clients/pulls/<PR_NUMBER>/comments/<comment_id>/replies \
     -f body="Addressed in <commit_sha> by <explanation>."
   ```

5. **Re-Review Cycle**:
   If code changes were pushed to fix review findings, schedule another 300-second timer to ensure the updated commit passes all automated re-reviews cleanly.

### Step 6: GitHub Merge Queue & Workspace Cleanup

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

Include this standardized checklist at the start of any new plan (`doc/agy/*-plan.md`):

```markdown
### Workflow Checklist (conforming to `doc/agy/runbook.md`)
- [ ] 1. Isolated worktree created on topic branch off latest `main`.
- [ ] 2. Domain skills loaded and consulted.
- [ ] 3. Incremental implementation with tests.
- [ ] 4. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for documentation-only changes).
- [ ] 5. PR opened, 5-minute review timer scheduled, 100% of bot & Sonar findings resolved.
- [ ] 6. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
```
