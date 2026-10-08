# CLAUDE.md

Guidance for Claude Code (claude.ai/code) when working in this repository.

## Project

`pm-mcp-clients` holds the client contract of plan-marshall-mcp (PM-MCP) and everything that runs outside the
daemon: `pm-api`, the job launcher `pm-exec`, the relay `pm-relay` (binary `pm-mcp`) and the operator CLI
`pm-operator`. The daemon `pm-mcpd` and the engine are not here: they live in `plan-marshall/plan-marshall-mcp` and
`plan-marshall/pm-mcp-core`.

The only listing of the repositories, the modules, their responsibilities and allowed dependencies is the Module
Structure Specification of the product (`doc/specification/module-structure.adoc` in
`plan-marshall/plan-marshall-mcp`, later in `plan-marshall/plan-marshall-documentation`); name modules from there and
never repeat the listing here. All project documentation (requirements, specifications, implementation watch,
roadmap, concept, developer and user documentation) lives there, not in this repository.

## Build

- Compile: `./mvnw compile`
- Quality gate: `./mvnw verify -Ppre-commit` (rewrites files: licence headers, OpenRewrite recipes, import order;
  review every resulting diff and commit it, then run the full verify again)
- Full verify: `./mvnw verify`
- Coverage: `./mvnw verify -Pcoverage` (minimum 80% instruction and branch coverage per module)
- Integration tests: `./mvnw verify -Pintegration-tests`
- Native binaries and their integration tests: `./mvnw verify -Pnative,integration-tests` with `GRAALVM_HOME` and
  `JAVA_HOME` set to a GraalVM 25 installation
- Always build and test through Maven and JUnit; never run `javac` directly or write ad-hoc verifier classes.
- The compiler runs with `failOnWarning`: fix deprecations and warnings, don't suppress them.
- `.mvn/maven.config` passes `.mvn/settings.xml` (the organisation's package registry, no token) as global
  settings; the token is the server `plan-marshall` of `~/.m2/settings.xml`.

## Dependencies and Versions

- Parent `de.planmarshall:pm-mcp-parent`, resolved from the organisation's registry. It supplies the Java release,
  the managed third-party versions, the plugin management, the quality-gate recipes and the deployment target.
- Never add a dependency or a plugin without asking the user first.
- Pre-1.0: no deprecation cycles, no backward-compatibility shims.

## Dependency Rules (enforced by the build)

The enforcer executions of the root POM fail the build for a violation; `BuildGuardsIT` of `pm-api` proves each of
them with a fixture project below `src/guard-controls`:

- No module depends on a module of the product other than `pm-api`, and none on Quarkus. `pm-relay` and
  `pm-operator` depend on `pm-api` only and never on each other.
- `pm-exec` depends on no other module of the product.
- `pm-api` depends on the streaming API of `jackson-core` only, never on `jackson-databind`.
- No module carries model-facing content: nothing below `workflows/`, `roles/`, `bundles/` or `skills/` in a JAR.
  Test fixtures are written for the test, never copied from the content of the product.

## Code Standards

- Java 25, Lombok (`@UtilityClass`, `@Value`, `@Builder`), prefer records, `var` for obvious types, final fields,
  package-private over public where possible. No Quarkus, no CDI.
- Every package has a `package-info.java` with Javadoc; every public type is documented.
- Never catch or throw generic `Exception`/`RuntimeException` in production code.
- No `System.out`/`System.err` except where a CLI writes its own output.
- JUnit 5 only (`@DisplayName`, `@Nested`, AAA, `@ParameterizedTest` for 3+ variants). Forbidden: Mockito,
  PowerMock, Hamcrest.
- Behaviour of the packaged binaries belongs in `*IT` tests (run with `-Pintegration-tests`, natively with
  `-Pnative`). The CLI modules run against the stub daemon of the test scope of `pm-api`; the binaries against the
  real `pm-mcpd` are covered by `pm-e2e` in `plan-marshall/plan-marshall-mcp`.
- `pm-exec`'s Linux-only kernel class `LinuxCalls` is excluded from the JaCoCo report and check on macOS only
  (profile `macos-coverage` in its POM); on Linux it counts in full.

## Publishing Only to the Organisation's Registry

The product is proprietary. Artifacts go to the GitHub Packages registry of the organisation `plan-marshall`
(`https://maven.pkg.github.com/plan-marshall/pm-mcp-clients`) and nowhere else, never to Maven Central or another
registry. The modules are deployed as `SNAPSHOT` versions on every merge to `main` and are never released. The
deployment target, its pin and the build check that guards it come from the parent POM; `pm.repository` in the root
POM names this repository. No workflow receives a Sonatype or GPG credential. Never weaken any of this, and never
add a deployment target, without the user's explicit decision.

## Git Workflow

`main` is protected by rulesets and merges go through the merge queue; direct pushes to `main` are not allowed.
Branch, commit, push, open a pull request, wait for the checks, answer and resolve every review comment. Do not
merge without the user's word. Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`.

CI: reusable workflows of `cuioss/cuioss-organization`, pinned by full SHA with a version comment; configuration in
`.github/project.yml`.
