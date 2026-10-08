# pm-mcp-clients

The client contract of [plan-marshall-mcp](https://github.com/plan-marshall/plan-marshall-mcp) and everything that
runs outside the daemon:

| Module | What it is |
|---|---|
| `pm-api` | Client contract of the daemon's API: the socket client, the on-demand start, the runtime-token checks, the host-detection table |
| `pm-exec` | Job confinement launcher, native binary `pm-exec` |
| `pm-relay` | Harness- and model-facing CLI with the `serve` relay, native binary `pm-mcp` |
| `pm-operator` | Operator CLI, native binary `pm-operator` |

The modules are proprietary software; see [LICENSE.md](LICENSE.md). They are deployed as `SNAPSHOT` versions to the
GitHub Packages registry of the organisation `plan-marshall` on every merge to `main` and are never released; the
product consumes them from there.

A machine that builds needs a token for the registry; the setup is described in the developer documentation of
plan-marshall-mcp (`doc/developer/registry-setup.adoc`).
