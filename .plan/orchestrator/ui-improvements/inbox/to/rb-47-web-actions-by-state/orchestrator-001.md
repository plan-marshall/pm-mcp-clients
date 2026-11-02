envelope_version=1
sender_type=orchestrator
sender_id=orchestrator
epic=ui-improvements
kind=finding
created=2026-11-02T10:58:16Z

# Finding for PLAN-47 from the CodeRabbit audit of 2026-11-02

CodeRabbit's last review on PR #297 (PLAN-37, `c5bfb1ca`) left an unresolved Minor finding on `ControlActionsBar.svelte` (~118) that was never handled and that your deliverable 3 inherits, because it removes `ControlActionsBar` and carries its behaviour over to `ActionBar`:

When a confirm sets `busy`, every action button is disabled. `closeDialog`'s focus fallback selects only `button:not([disabled])` (`[data-testid="control-actions"] button:not([disabled])`), so the focus stays on `body` and is not restored when `busy` clears. The behaviour you carry over must restore the focus to the invoking control (or the first enabled control) after `busy` clears, and a test must fail without it.

Also read CodeRabbit's review BODIES as well as the threads before you merge; findings that sit only in a body were missed by the earlier checks.
