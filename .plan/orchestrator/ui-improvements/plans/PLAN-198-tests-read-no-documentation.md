# PLAN-198: Tests Read No Documentation

epic: ui-improvements
workstream: WS-01

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane (the operator, 2026-11-27: "yes. as a
> runbook lane"). It was cut after `main` went red twice from a docs-only merge: #570 (`20f971fe`, 2026-11-25) and #600 (`7e5382c8`,
> 2026-11-27). Grounded by a read-only sub-agent at `origin/main` `bac0222a`.
> This spec is SELF-SUFFICIENT. **It adds no dependency.**

## Objective

Eight test classes of this repository open design documents under `doc/design/` at run time and parse them:

- `pm-api`: `PlanViewCatalogueTest`, `OverviewCatalogueTest` and `ControlCatalogueTest`, which use the helper `SpecificationTables`. `ChatTextsCatalogueTest` and `IntentCatalogueTest` have their own readers.
- `pm-operator`: `PmOperatorTest.CommandTree`.
- `pm-web-app`: `workflowIcons.test.ts` and `vocabulary.test.ts`.

CI does not build a pull request that changes only documentation (`skip-on-docs-only`, the default of the organisation workflow). So a docs-only change to a table these tests read merges green and leaves `main` red. #600 added 7 keys to tested tables (6 `plan.*`, 1 `overview.*`) and none to the catalogue files, and every code pull request that merges `main` now fails.

pm-mcp-core solved the same break with its ADR 016 (`Schemas Are Files of the Implementing Module`, "own inputs only"). This repository's ADR 001 names ADR 016 but never applied its rule to the tests. The operator rejected the alternative, counting `doc/**` as code for the CI change filter: that is core ADR 015, which ADR 016 calls "a consequence of the tests reading documentation, not a cure".

**Operator decisions, 2026-11-27, which bind:**
- **Source of the texts:** the catalogue `.properties` files are the single source of the texts.
- **Link, not include:** the design text sections **link** their en/de catalogue files and stop restating the keys those files carry. No `include::`, no `tag=` regions, no UTF-8 normalisation of the files.
- **Texts still to come:** a key the design names that no catalogue file carries yet lives in a **"Texts to add"** table (Key | English | German) of its document. No test reads that table, and the plan that adds a key to a catalogue file deletes its row.
- **Scope:** only the **10 documents the tests read**. The other 33 documents with text tables carry no risk for `main` and stay as they are.

Line numbers are at `origin/main` `bac0222a`.

## Deliverables

1. **ADR 002: tests read only the inputs of this repository's modules**
 - What:
   - `doc/adr/002-tests-read-no-documentation.adoc` adopts core ADR 016's "own inputs only" for the clients:
     - A test reads files of a module of this repository: its class path, or the `src/` of another module, as `catalogue-fixture.ts` reads the `pm-api` catalogue files.
     - A test never reads a file below `doc/`.
   - The ADR records:
     - the two incidents (#570, #600);
     - the rejected alternative (`paths-code-extra`, core ADR 015);
     - the operator's decisions above (the catalogue files are the source; link, not include; "Texts to add");
     - that the docs-only build skip stays as it is.
   - The ADR names the 8 retired readers.
   - List the ADR in `doc/adr/README.adoc`. Amend ADR 001 where it names core ADR 016 (`001-specifications-of-the-clients.adoc:193`) to point to ADR 002.
 - Persona: plan-marshall:persona-documenter
 - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
 - Done when:
   - ADR 002 exists, is listed in `doc/adr/README.adoc`, names the 8 readers and the three decisions, and the AsciiDoc validators report 0 errors and 0 broken links over `doc/adr/` (evidence order: `none`).
2. **The `pm-api` catalogue tests stop reading documents**
 - What:
   - Delete `pm-api/src/test/java/de/planmarshall/api/message/SpecificationTables.java`.
   - Each of the five catalogue tests keeps what it can check without a document.
   - Its existing checks that need no document stay. They are named in the Claim Labels.
   - What each test drops or re-homes:
     - **`PlanViewCatalogueTest`:**
       - Drop the doc ⊆ pack check (`:74-82`) and the key-set equality of `plan.changes.*` (`:101-114`).
       - Keep the 7 verbatim `plan.changes.*` en/de texts (`:83-89`) as literal expectations in the test.
     - **`OverviewCatalogueTest`:**
       - Drop the doc ⊆ pack check (`:96-135`).
       - Drop `PENDING_RENAME` (`:91`): `overview.needs.integrity_hold` moves to the "Texts to add" table of `plan-overview.adoc` (deliverable 4).
     - **`ControlCatalogueTest`:**
       - Drop the doc ⊆ pack check (`:47-74`).
       - Keep the `control.*`/`common.*` prefix rule as a check over the files.
     - **`ChatTextsCatalogueTest`:**
       - Drop its table reader (`:46-129`, `:173-185`).
       - Keep the verbatim texts it guarded only where the test already states them literally. Otherwise the en/de parity of `control.chat.*` and `decision.chat.*` remains.
     - **`IntentCatalogueTest`:**
       - Drop its document reader (`:43-44`, `:226-252`) and the doc-equality checks (`:52-102`).
       - Keep a test that the `intent` area's en and de key sets are equal and non-empty, and that `common` carries the 7 verdict words `state.criterion.*`/`state.check.*` in both languages (`:198-220`, as literals).
   - **Re-home the one useful check that would be lost:** every key of every catalogue area carries the same ICU argument names in en and de. Today `IntentCatalogueTest:88-102` checks this only for `intent`, computed from the document. Put the pack-wide test in `MessageCatalogueTest`, or beside it.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java:java-core
 - Done when:
   - A test asserts that every key of every catalogue area carries the same ICU argument names in en and de. It fails when one argument is dropped from one German text, e.g. `{head}` from `plan.changes.title` (evidence order: `mutation-after`).
   - A test asserts that the `intent` en and de key sets are equal and non-empty, and that `common` carries the 7 `state.criterion.*`/`state.check.*` words in both packs. It fails on a key removed from one pack (evidence order: `mutation-after`).
   - The 7 `plan.changes.*` texts are asserted as literal expectations. The test fails on one changed German text (evidence order: `mutation-after`).
   - `SpecificationTables.java` is deleted, and `git grep -nE '"doc"|\.\./doc|doc/design|\.adoc"' -- pm-api/src/test` has no hit that opens a file. The PR shows the grep (evidence order: `none`).
3. **The `pm-operator` and `pm-web-app` readers stop reading documents**
 - What:
   - **`PmOperatorTest.CommandTree`** (`:191-283`):
     - Its expected verb set becomes a literal set in the test: the 24 verbs of `PmOperator.java:45-70`. That is a contract expectation, allowed by `CLAUDE.md`.
     - The walk to `01-commands.adoc` (`:253-263`) and the parse of its open-items bullet (`:223-238`) go.
     - `foldedVerbIsUnknown` (`:285-294`) and the `--help` check (`:296+`) stay.
     - Optionally fix the Javadoc of `PmOperator.java:37`.
   - **`workflowIcons.test.ts`:**
     - Stops reading `workflow-visual-language.adoc` (`:17-20`, `:40`, `:65-79`).
     - It checks code invariants instead: every value of `PHASE_TOKENS`, `PHASE_ICON_IDS` and `UNIT_ICON_IDS` (`workflowIcons.ts:143-209`; export them, or iterate through the resolvers) is in `WORKFLOW_ICON_IDS` (`:52-95`), and resolves through `phaseIconId`/`unitIconId`.
     - The asset, fallback and SVG-lint checks (`:50-63`, `:81-131`) stay.
   - **`vocabulary.test.ts`:**
     - Stops reading `components.adoc` (`:13`, `:104-135`).
     - The grouped vocabulary (8 groups, 60 names) and `PENDING_LIST` (`:23-30`) become a literal in the test. Same pattern as the command tree; no new production file.
     - Every non-pending name has a `.svelte` source, a `.test.ts` and a `/dev/components` fixture (`:177-228`).
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - A test asserts that `PmOperator`'s registered verb names equal the test's literal set of 24. It fails when one subcommand class is removed from `PmOperator.java:45-70`, e.g. `FindCommand.class` (evidence order: `mutation-after`).
   - A test asserts that every value of the three icon maps is in `WORKFLOW_ICON_IDS` and resolves through its resolver. It fails on a map value outside the id list (evidence order: `mutation-after`).
   - A test asserts that every non-pending name of the literal vocabulary has a source, a test and a `/dev/components` fixture. It fails when one fixture is deleted (evidence order: `mutation-after`).
   - `git grep -nE 'doc/design|\.\./doc|components\.adoc|workflow-visual-language\.adoc|01-commands\.adoc' -- pm-operator/src/test pm-web-app/src` has no hit that opens a file. The PR shows the grep (evidence order: `none`).
4. **The 10 design documents link to the code**
 - What:
   - **The 7 catalogue documents:** `shared/plan-view.adoc`, `shared/plan-overview.adoc`, `terminal-client/15-overview.adoc`, `shared/work-actions-texts.adoc`, `shared/work-actions-chat-texts.adoc`, `shared/decisions-texts.adoc`, `shared/intent-and-outline-texts.adoc`.
     - Each text section keeps its anchor (`[#texts]` and the others) and names and links its en/de catalogue files (`pm-api/src/main/resources/messages/<area>.{en,de}.properties`).
     - It drops every `|Key |English |German` row whose key those files carry at the head.
     - A row whose key no file carries moves to a **"Texts to add"** table (Key | English | German) under that section, with one sentence: no test reads it, and the plan that adds the key deletes the row.
     - This covers the 7 keys of #600, unless the running fix has added them to the files by then, and `overview.needs.integrity_hold`.
   - **`web-app/workflow-visual-language.adoc`:**
     - `:164`, the `#update` steps `:684-695` and `#sources-of-truth` `:697-720` name `pm-web-app/src/lib/icons/workflowIcons.ts` as the authority for the ids and the maps, and link it.
     - The Meaning, Family, Asset and Status columns stay design text.
     - The `// icon-catalogue:start..end` and `// unit-icon-map:start..end` markers go.
   - **`web-app/components.adoc#vocabulary`:** stays design text. One sentence says the test carries its own copy as the expectation.
   - **`terminal-client/01-commands.adoc`:** stays prose (options, `[op]` marks and aspects are design content). The open-items bullet `:165` stops being test input.
   - The keys paragraph of `doc/design/shared/texts-and-languages.adoc` states the rule. Correct its area list where it disagrees with the file names (`plan` vs `plan_view`, `report` vs `reports`).
 - Persona: plan-marshall:persona-documenter
 - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
 - Done when:
   - No `|Key |English |German` row of the 7 catalogue documents restates a key their catalogue files carry at the head. Each of their text sections links its en/de files. Every key they named that no file carries sits in a "Texts to add" table. A script-free check in the PR lists these per document (evidence order: `none`).
   - The AsciiDoc validators report 0 errors and 0 broken links over the 10 documents. Each link to a catalogue file or to `workflowIcons.ts` resolves at the head (evidence order: `none`).
   - The landing message lists the keys of every "Texts to add" table it created, as residue for the plans that implement them (evidence order: `none`).

## Standing Constraints

- **Build and test through Maven only** (`CLAUDE.md`). At the head:
  - `./mvnw -pl pm-api,pm-operator verify`
  - `./mvnw -pl pm-web-app verify`
  - the full `./mvnw verify`
  - `./mvnw verify -Ppre-commit`
- **`main` may be red when the run starts, from #600:**
  - The red is `PlanViewCatalogueTest`, 6 keys: `plan.node.state.running_run_of`, `plan.leaf.job_running`, `plan.leaf.ci_wait`, `plan.view.replanned`, `plan.timeline.title`, `plan.timeline.open`.
  - And `OverviewCatalogueTest`, 1 key: `overview.needs.overdue`.
  - A running plan adds those keys on the operator's word. If it has merged, rebase on it. If not, this plan turns `main` green by itself, because it removes the readers and moves the 7 keys to "Texts to add".
  - **Never add catalogue texts to make a test pass:** the texts belong to the plans that implement them.
- **No new dependency or plugin.** Pre-1.0: no shim.
- **No change to `.github/`.** In particular no `paths-code-extra` and no change to `skip-on-docs-only` (the operator, 2026-11-27).
- **No catalogue file is edited:** no tag regions, no UTF-8 normalisation, no key added or removed.
- **Java:** Java 25, JUnit 5, input data from `Generators` with `@EnableGeneratorController`. Literal expectations (verb names, catalogue texts, icon ids) stay literal: they are the contract.
- **Web:** ES modules; no timer wait in a test.
- **ZERO Sonar findings** on the new code of the pull request (SonarCloud `plan-marshall_pm-mcp-clients`), read at the head after the push. State `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`). No `NOSONAR`.
- **The merge rule** (`CLAUDE.md`, Git Workflow):
  - Every review thread is replied to and resolved, and every review-body item answered.
  - Some head has had one CodeRabbit review.
  - Build and Sonar are green.
  - The PR body states the counts from `ci pr comments`.
  - Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; no generator footer.
  - If CodeRabbit refuses on quota, wait out the window and trigger it once more. Ask the operator only if it still refuses.
- **Write-set discipline:** the main checkout is never edited. Work only in the session's own worktree and branch.
- **Running plans:** whichever merges second merges `main` and keeps both changes. Never touch another plan's branch. The shared files:
  - PLAN-135 (running) may create `Chat`, `ChatTurn`, `ChatMarkdown` and take them out of `vocabulary.test.ts`'s `PENDING_LIST`.
  - PLAN-62 (running) may add a row to `workflow-visual-language.adoc`, and may touch `plan-view.adoc` and `plan_view.*.properties`.
- **The landing message states every claim against the merged tree:** file names, counts of tests and keys.

## Claim Labels

- OBSERVED: eight test classes open a file below `doc/` at run time; every other `doc/` string in the test sources is a comment, a fixture value or a read of another module's `src/` — read at `pm-api/src/test/java/de/planmarshall/api/message/SpecificationTables.java:108-113`, `IntentCatalogueTest.java:43-44`, `ChatTextsCatalogueTest.java:46-88`, `pm-operator/src/test/java/de/planmarshall/operator/PmOperatorTest.java:253-263`, `pm-web-app/src/lib/icons/workflowIcons.test.ts:17-20`, `pm-web-app/src/lib/vocabulary.test.ts:13`
- OBSERVED: CI skips `build` for a docs-only pull request and for a docs-only merge group; #600's `build / build` and `sonar-build` were SKIPPED and `build / conclusion` SUCCESS — read at `gh pr view 600 --json statusCheckRollup`; cuioss-organization `reusable-maven-build.yml` v0.46.0 `:41-55,314-360`
- OBSERVED: what stays testable without the documents already exists: en/de key parity (`MessageCatalogueTest.java:48-90`, `catalogue.test.ts:222-232`), the per-test enum coverage and literal format tests — read at those lines
- OBSERVED: no test checks ICU argument parity across all areas; only `IntentCatalogueTest:88-102` does, for `intent`, from the document — read at `MessageCatalogueTest.java`, `catalogue.test.ts`
- OBSERVED: the design text tables have the shape `[cols="3,3,3",options="header"]` `|Key |English |German` — read at `doc/design/shared/plan-overview.adoc:173-187`
- HYPOTHESIS: the 7 #600 keys are still absent from the packs when the run starts — confirm/refute at `pm-api/src/main/resources/messages/plan_view.en.properties`, `overview.en.properties` at the run's base

## Expected Surface

- OBSERVED: `doc/adr/002-tests-read-no-documentation.adoc` — absent at `bac0222a`; created by this plan
- OBSERVED: `doc/adr/README.adoc` — present at `bac0222a`
- OBSERVED: `doc/adr/001-specifications-of-the-clients.adoc` — present at `bac0222a`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/SpecificationTables.java` — present at `bac0222a`; deleted by this plan
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/PlanViewCatalogueTest.java` — present at `bac0222a`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/OverviewCatalogueTest.java` — present at `bac0222a`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/ControlCatalogueTest.java` — present at `bac0222a`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/ChatTextsCatalogueTest.java` — present at `bac0222a`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/IntentCatalogueTest.java` — present at `bac0222a`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/MessageCatalogueTest.java` — present at `bac0222a`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PmOperatorTest.java` — present at `bac0222a`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PmOperator.java` — present at `bac0222a` (Javadoc `:37` only, optional)
- OBSERVED: `pm-web-app/src/lib/icons/workflowIcons.test.ts` — present at `bac0222a`
- OBSERVED: `pm-web-app/src/lib/icons/workflowIcons.ts` — present at `bac0222a` (exports only)
- OBSERVED: `pm-web-app/src/lib/vocabulary.test.ts` — present at `bac0222a`
- OBSERVED: `doc/design/shared/plan-view.adoc` — present at `bac0222a`
- OBSERVED: `doc/design/shared/plan-overview.adoc` — present at `bac0222a`
- OBSERVED: `doc/design/terminal-client/15-overview.adoc` — present at `bac0222a`
- OBSERVED: `doc/design/shared/work-actions-texts.adoc` — present at `bac0222a`
- OBSERVED: `doc/design/shared/work-actions-chat-texts.adoc` — present at `bac0222a`
- OBSERVED: `doc/design/shared/decisions-texts.adoc` — present at `bac0222a`
- OBSERVED: `doc/design/shared/intent-and-outline-texts.adoc` — present at `bac0222a`
- OBSERVED: `doc/design/web-app/workflow-visual-language.adoc` — present at `bac0222a`
- OBSERVED: `doc/design/web-app/components.adoc` — present at `bac0222a`
- OBSERVED: `doc/design/terminal-client/01-commands.adoc` — present at `bac0222a`
- OBSERVED: `doc/design/shared/texts-and-languages.adoc` — present at `bac0222a`

## Dependencies and Sequencing

- Depends on: none. It can run while `main` is red: it removes the red's cause.
- Overlaps with, under the overlap rule (2026-11-25), all below 5 code files:
  - PLAN-135 (running): `vocabulary.test.ts` (`PENDING_LIST`).
  - PLAN-62 (running): none in code; `workflow-visual-language.adoc` and `plan-view.adoc` are doc-only.
  - PLAN-70 and PLAN-71 (running): none.
- Residue for the plans that implement texts: every key of a "Texts to add" table, as listed in the landing message.

## Amendment 2026-11-27 (PLAN-71 landed, #599 `3f0af941`)

**This section binds over the body where they differ.**

- #599 carried the operator-directed commit `2f7759a0`, which added the 7 #600 keys to `plan_view.{en,de}` and `overview.{en,de}`. `main` is green at `3f0af941`.
- The HYPOTHESIS "the 7 #600 keys are still absent" is **refuted**.
- The 7 keys stay in the catalogue files. Their design rows become links like every other key the files carry, not "Texts to add" rows.
- In the 7 catalogue documents, the only key a tested table names that no file carries is `overview.needs.integrity_hold`. It goes to "Texts to add" in `plan-overview.adoc`. Re-check at the run's base.
- The standing constraint "`main` may be red when the run starts" is obsolete. The base is green. Rebase on `3f0af941` or later.

## Hand-Off Command

Lane: `runbook` (the operator, 2026-11-27).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-198-tests-read-no-documentation.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source, tests, design documents and ADRs. It creates and
edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger
write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` §
Ledger Write-Boundary).
