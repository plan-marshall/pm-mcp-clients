# PLAN-180 landing record

epic: ui-improvements
plan: rb-180-sonar-standing-debt-pm-api-part-2 (runbook lane, harness opencode, model deepseek-v4.1-flash/high, plugin_version 0.1.2183 (amended from `unknown`), executor 0.1.2183)
pr: #543, merge `33f82210` (merge queue, 2026-11-23); 45 files, +312/-157
analysed: 2026-11-23, by sub-agent in three layers, by reading only (no build)

Message `rb-180-sonar-standing-debt-pm-api-part-2-001` (amended once): `merge_state=merged`, 3 of 4 deliverables by the lane's own reading, 3 review-body findings handled (CodeRabbit refused twice on quota, one escalated full review, three inline minors), 0 unresolved threads, Sourcery refused on its own hard quota, four operator questions answered. `total_tokens=unknown` (pointer to the 2026-10-27 Open Defect, not re-reported).

**Sonar open new issues: 0.** The lane reports 0 with `quality_gate_status=ok`. Read independently through the Sonar MCP: gate of PR 543 OK (all ratings 1, new coverage 95.9, duplication 0.0, hotspots reviewed 100%); the PR-scoped issue search returned none. The lane's default-branch read after the merge (116 open, none under `pm-api`, 348 before) was not repeated here.

## Layer 1: specified

| Deliverable | Done-when | Message | Verdict |
|---|---|---|---|
| D1 | `HttpResponse` S6878 | met | accurate: record pattern, same comparisons, one extra body clone on a matched comparison (accepted cost) |
| D1 | `JsonTree` S1168, every caller follows, no shim | met | accurate: `git grep objectOrNull` finds only ledger files; 24 main sites plus `ChatRevisionQueueTest` follow; no `fromMap` loses `throws`; absent and `null` both give empty; every `.orElse(Map.of())` meets a constructor that already mapped null to `Map.of()` |
| D1 | `RuntimeClient` S1075 not dismissed by the lane | met | met through the operator's selection (question 3, a justified source suppression); the mechanism differs from the spec's `sonar_rest transition` route because that verb cannot transmit (L3-5) |
| D1 | leftovers (`Libc.java:91`, `EpicProcess.java:51`, 33 suppression sites) | met | `EpicProcess` correct; the inventory in the PR body does not add up (L3-6) |
| D1 | `-pl pm-api -am verify`, test count not lower | met, 1689 | not checkable by reading; source gives +11 methods, not +8 (L3-4) |
| D2 | no open issue under `pm-api/src/test`, no assertion loosened | met | accurate on assertions: every removed assertion line is the same assertion rewritten, no test removed |
| D3 | no `Thread.sleep`; dependency "exactly as stated and nowhere else" | partly met as written | honest and accurate: `git grep` empty; Awaitility is in the root POM, `pm-api`, `pm-relay` and `pm-operator` (operator's selection 4, `StagedDaemon` is used by `PmMcpProcessIT` and `PmOperatorProcessIT`) |
| D3 | the landing states the approval and the POMs | met | yes |
| D4 | default branch names none under `pm-api` | met | the lane's post-merge read; not repeated here |

The hypothesis "the parent does not manage Awaitility" is refuted by the lane (the effective POM carries `version.awaitility=4.3.0`), consistent with the root pin as an override.

## Layer 2: implemented

45 files as claimed, under the 100-file limit. Outside the Expected Surface: `ChatRevisionQueueTest` (required by the signature), `pm-relay/pom.xml` and `pm-operator/pom.xml` (operator's selection), about 23 production files that follow the `JsonTree` signature, seven more test files (the spec says every open issue). The four files the message says carried no wait changed by renames only. The root POM's `jackson-core-only` allow-list gained `org.awaitility:awaitility:*:jar:test` and `org.hamcrest:hamcrest:*:jar:test`.

## Layer 3: correct (reported to the operator; no fix plan staged)

| # | Severity | Finding | Where |
|---|---|---|---|
| L3-1 | minor | `startsDetached`: the Awaitility wait is swallowed on timeout and the fixture started regardless, so the wait can never fail the test; an `IOException` of the old code is now an `UncheckedIOException` that dies in a virtual thread and fails nothing | `DetachedStartTest.java:89-104` |
| L3-2 | nit | `START_BUDGET` also times the exit-code wait; `POLL_INTERVAL` is both `pollDelay` and `pollInterval` | `DetachedStartTest.java:46-47,136` |
| L3-3 | minor | Hamcrest is admitted at `*:jar:test` (the narrowest form) and is now on the test classpath of three modules with no guard against importing it, which the repository forbids; the POM comment says "version waits" for "condition waits" | `pom.xml:77-85,163-183` |
| L3-4 | minor | the test count is not reproducible: +11 `@Test` methods in the diff, not +8; "1689 on `d20b5a71`" and on the D2 commit is odd | message § 1 |
| L3-5 | minor | the S1075 suppression is a source suppression with a reason, operator-selected, not a lane dismissal; the Javadoc was reformatted to three lines | `RuntimeClient.java:33-36` |
| L3-6 | nit | the suppression inventory of the PR body is off (36 matches in `src/main`, 35 pre-existing; 25 `unchecked`; 14 S3415 sites, not 12) | PR body |
| L3-7 | nit | `IntentJson.optionalObject` (returns `@Nullable Map`) has the same name as `JsonTree.optionalObject` (returns `Optional`); about 15 `isPresent() ? ... : null` sites | `IntentJson.java:~231` |
| L3-8 | nit | `StagedDaemon.awaitStart` throws `IllegalArgumentException` for a timeout below the 2 ms poll delay; the Javadoc claim about the interrupt flag is the lane's reading of the Awaitility sources | `StagedDaemon.java:73-87` |

Checked and fine: no `Thread.sleep` in `pm-api/src/test`; no assertion weakened, no test removed; `assertAll` correct; `JsonTreeTest.OptionalObject` carries `@EnableGeneratorController` and uses `Generators`; no Mockito, Hamcrest import, `System.out` or generic `Exception` in the diff; `@NullMarked` not regressed; no wire name, catalogue text or `src/main/resources` change.

## Residue for the plans that touch the same surface

- The `pm-operator` Sonar part (not yet staged) and PLAN-181's text: Awaitility is now declared in `pm-api`, `pm-relay` and `pm-operator`, so the `pm-operator` part may use it without a new dependency decision; `PmOperatorProcessIT.java` still has `Thread.sleep(100)` (outside this plan). PLAN-181's line 53 ("Awaitility is PLAN-180's, in `pm-api` only") is outdated; PLAN-181 has merged, so no edit.
- PLAN-183 and PLAN-184 share no file with this diff.
- Provider defects the landing reports (not this epic's code): `sonar_rest transition` cannot transmit any dismissal (JSON body to a form-encoded endpoint); `-Ppre-commit` is not idempotent on `main` (rewrites untouched files, re-inserts `cui-rewrite` markers its own gate rejects); `resolve`'s `inbox_read_command` names an invalid plan id for numbered plans; `landing-check` rejects `unknown` for `total_tokens`. Recorded in `epic.md`, owner: the plan-marshall repository.
- The D3 wording of the spec ("declared in `pm-api` only") needs a revision note if the spec is reused.
- `backup/plan-175-full` (local only) is deleted at the second of PLAN-180 and PLAN-181's landings; PLAN-181's landing message is not yet in.
