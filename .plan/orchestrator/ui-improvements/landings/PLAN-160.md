# Landing Analysis: PLAN-160 — `config` Made to Do What the Design Says (PLAN-32 Residue)

epic: ui-improvements
workstream: WS-04
pr: #421 (https://github.com/plan-marshall/pm-mcp-clients/pull/421), squash `37f51af7` (26 files, +2026/-239), merge head `ea77e58f`

> Written by the `analyze` verb (2026-11-12) from `rb-160-cli-config-residue-001.md`, verified against git, the PR reviews and threads
> and the design. Runbook lane, opencode `deepseek-v4.1-flash/high`; executor `0.1.2106`.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Revision and conflict | shipped-as-specified | revision in the body (operator's answer to the lane's stop-and-ask), `ConfigCommand.java:623-711`, "Changed meanwhile", `k`, resend, `--no-input` 3; mock 409 `conflict`/`aspect_owned` (`MockConfigRoutes.java:161-200`); evidence `mutation-after` (disclosed) |
| D2 Rotate checks the providers | shipped-modified | runs through `ConfigAspectRun`; relies on mock-only facts and exits 0 on a failed check (finding 1); grants the consent itself (finding 2) |
| D3 Overview rows | shipped-as-specified | consents count, documents word, Paired devices from `/web/devices` + `/status`, input on the prompt line, keys sanitised; the sanitising test covers only the values path |
| D4 Key ownership | shipped (one display table left) | `AspectOwnedKeys` single write table, `credentials.store` only, tested; `ASPECT_PREFIXES` (`:124-131`) still selects the display family |
| D5 Mock | partly | plan-only inheritance fixed; "verified only from the provider state" met in letter (a `/status` flag); `fully-onboarded` reaches only `language` |
| D6 Tests, document, replies | shipped with reply defects | Generators, terminal supplier, `01-commands.adoc` toward the code (adds `.`/`?` it does not accept); #415 replies in one issue comment, one misattributed, one not in its thread |

Added-unplanned: `ConfigValueChange` (pm-api), `ConfigAspectRun.Outcome`, `AspectRunSource.lastFacts`, a mock
`GET /api/v1/credentials/{key}`, tombstone revisions, the widened `ScreenCaptureIT.settled` (all captures).

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`); it did not load the Java skills (disclosed); no write. Re-read by the
  orchestrator at `37f51af7`: `CredentialsCommand.java:240-280`, the design's `providers.failed` (`03-dialogues.adoc:84,119`,
  `05-transcripts-entry.adoc:441`). Confirmed.
- **Verdict:** no blocker. Layer 1 — D2 presumed runtime facts the catalogue does not define; D5 "the scenario's provider state" was
  under-specified. Layer 2 — 6/6 in letter; D4/D5 partly. Layer 3 — **major:** (1) rotate follows mock-only facts and exits 0 on a failed
  check; (2) the client grants and announces the `verify_providers` consent (PM-SEC-8 shape); (3) the mock's readiness is a `/status` flag
  the verify path ignores; the negative test passes when verify does nothing; (4) the PR body's counts stayed at creation values, one of 42
  threads has no reply, two #415 replies are wrong. **Minor:** HTTP 409 on a second conflict; second prefix table; one reachable aspect;
  unsynchronised mock reads; `01-commands.adoc` flags; widened capture settle; literal test input; "listener on" for an unreadable status.
- Confirmed sound: the revision form settled by asking; `served()` and tombstones; sanitising of the key paths; Sonar 0 (86.2 % new-code
  coverage); trailer and no footer.

Routing (operator, 2026-11-12, AskUserQuestion — "Fold into PLAN-30"): all of it → **PLAN-30** deliverable 7 (surface +8). PLAN-30 is now
large: lane to be re-checked at emit.

## Routing and Merge Behavior

- Review: CodeRabbit and Sourcery reviewed; 41 of 42 threads replied and resolved, thread `4550425785` resolved by the bot without a reply;
  15 body findings handled per the landing (Sourcery's item 15 only partly applied). The PR body's counts were never updated (the fourth
  recurrence of the stale-count lane defect).
- Merged by the lane via the queue (16:35Z).
- Sonar `0` per the PR's Sonar comment and the lane at `ea77e58f`.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 421; `landing` `landings/PLAN-160.md`
- [x] PLAN-30 deliverable 7 (operator)
- [x] merge clause reworded across 31 open specs (operator; CLAUDE.md/AGENTS.md #423)
- [x] message archived; resume anchor; `queue-view.md` regenerated

## Follow-Ups

- PLAN-128 (Cleanup row; its number is now conditional on the devices row) and PLAN-30 run after 160 and rebase over `ConfigCommand.java`.
- PLAN-133 (#424) rebases over `MockRuntimeEngine.java` and `fully-onboarded.json` (`providers_ready`, the settings states).
