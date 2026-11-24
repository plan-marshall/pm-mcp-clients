envelope_version=1
sender_type=orchestrator
sender_id=pm-mcp-core-usage-model-profiles
epic=ui-improvements
kind=finding
created=2026-11-24T12:30:00Z

# Brief from the pm-mcp-core epic (pm-mcp-core-m2-m4-v2): the five shipped profiles need `usage.model`, and a hard cross-repo predecessor waits on it

Sender: the orchestrator of the epic `pm-mcp-core-m2-m4-v2` in `pm-mcp-core`, at the operator's word of 2026-11-24 ("draft the brief for the clients repo"). Nothing in this epic was edited by the sender: this message is the only channel. It asks for one small plan; the epic decides whether, when and how.

## Why this is a request now

PLAN-V2-135 (pm-mcp-core, PR #594, head `79b6a5a8`, branch `feature/v2-135-served-model-recorded`) records the model that answered a turn. It makes `headless.usage.model` a required member of a harness profile. Its lane is done and the PR is open and **deliberately unmerged**: with the member required, every profile shipped by this repository's `pm-api` SNAPSHOT fails the validator, so five test classes of pm-mcp-core are red for that one cause (`ShippedProfilesTest`, `ShippedUsageFixturesTest` (two cases), `EndpointProfileValidatorTest.noShippedEndpointProfile`, `WorkerParametersTest`). The operator told the lane not to merge until this is done. Everything else in that PR is green and reviewed. The only thing that lets it merge is the change below, **merged here and its `SNAPSHOT` deployed** (pm-mcp-core reads it as `version.pm-mcp-clients`, root `pom.xml:56`).

## What is asked (a plan of one deliverable, optionally two)

**D1 (required). The member `usage.model` in the five shipped profiles** in `pm-api/src/main/resources/profiles/`: inside the existing `headless.usage` object, beside the seven figures, one new member `"model"`:

| Profile | `usage.model` |
|---|---|
| `claude.json` | `"/message/model"` (the pointer into the turn line the harness prints; the `usage` block is at about `:55-63`, `"result_pointer": "/result"` is the output pointer) |
| `antigravity.json` | `null` |
| `codex.json` | `null` |
| `neutral.json` | `null` |
| `opencode.json` | `null` |

The rules the validator applies (specification: pm-mcp-core `doc/specification/harness-profiles/01-profile-schema.adoc`, the schema block at `:324-328` (`["string","null"]`, listed in `required`, `additionalProperties: false`), the bullet `[[served-model]]` at `:862` and the shipped-declarations sentence at `:877`; the implementation is on #594, not yet on `main`): the member is **required** (a profile without it is refused), a string value is a **pointer** that starts with `/`, is at most 256 characters, and uses only the `~0` and `~1` escapes (RFC 6901); `null` means "this harness reports no model"; `not_applicable` (the word the seven figures use) is **refused** here, the figures stay numeric. Add the member to every profile in the same PR so none is left without it. A test of the clients repo that reads the shipped profiles (`ProfileDocumentReaderTest`, `readShippedProfiles`) should pin that each of the five carries the member and the value in the table (a test that fails on a profile without it). The values for the four `null` profiles are the specification's own (it names `/message/model` for `claude` and `null` for the others); the sender did not verify what those four harnesses print, so a later plan may replace a `null` by a pointer when a harness output is recorded (pm-mcp-core's watch item PM-WATCH-CLIENT-20 keeps that open).

**D2 (optional, may be a separate plan or the existing `plan-changes` follow-up). The `model_differs` entry's display.** The contract already exists here: `ActivityEntryKind.MODEL_DIFFERS` (`"model_differs"`), the flat members `launch_model` and `served_model` on `ActivityEntry` (as the operator chose on 2026-11-24, question 2 of the pm-mcp-core lane: flat, not nested), and the message `plan.activity.model_differs` ("answered by {served} instead of {launched}"). The pm-mcp-core server now writes such an entry when a turn reports a model other than the launch model; it is information only. What the sender cannot see from here is whether the CLI (`PlanCommand`) and the web view print both models; the epic's own `epic.md` (the line naming `PlanCommand.printActivity`) records that the CLI drops both models of a `model_differs` entry. If that is still true at the head of `main`, D2 closes it; if a merge since then settled it, drop D2 and say so. This is not a predecessor of #594; it is the display half.

## Order and what pm-mcp-core does next

1. This repository: D1 merged, `SNAPSHOT` deployed (the deploy runs on the merge to `main`).
2. pm-mcp-core: its lane re-runs the red classes against the new SNAPSHOT (`-U`), the build goes green, Sonar is read, PR #594 merges, and that plan is analyzed.
3. The assembly `plan-marshall-mcp` then wires the watch and the task row (owed there, not here).

The sender does not edit this repository's ledger beyond this message and will not edit a running plan's spec. Questions that need the operator go to the operator, not back through this inbox.

## Facts the sender re-read, and what it did not

Re-read on `origin/main` of this repository on 2026-11-24 (head `1c646489`): none of the five profiles has a `model` member under `usage`; `ActivityEntry.java:27-36` and `:86-89` carry `launch_model` and `served_model`; `ActivityEntryKind.java:28` has `MODEL_DIFFERS`; the two message packs carry `plan.activity.model_differs` (`:69`). Not read: the CLI and web renderers of the entry; the output of the four `null` harnesses.
