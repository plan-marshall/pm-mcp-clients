# Remediation Plan: `web-app-static-export-csp` (Let the Served SPA Boot Under Its Own CSP)

The static export and the server import are delivered, but Step 8 fails on a functional gap: the daemon serves the real SvelteKit shell with a CSP that blocks the shell's own inline bootstrap. `WebAppIT` proves the bytes and headers, not that a browser can start the app.

The client's `app.html` still renders `%sveltekit.body%` inside `<div style="display: contents">`, and the built shell carries an inline `__sveltekit_*` module bootstrapping `<script>`. The listener sends:

```text
default-src 'self'; script-src 'self'; connect-src 'self';
object-src 'none'; base-uri 'none'; frame-ancestors 'none';
require-trusted-types-for 'script'
```

There is no script hash or nonce and no `style-src` allowance, so a browser blocks the inline bootstrap and the inline style attribute. The app is served but does not start.

This plan closes that gap without weakening the policy to `'unsafe-inline'`. It is the F5 follow-up the original plan reserved.

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Parent plan** | [`implemented/web-app-static-export-and-server-import-plan.md`](implemented/web-app-static-export-and-server-import-plan.md) |
| **Completion report** | [`web-app-static-export-and-server-import-plan-report.md`](web-app-static-export-and-server-import-plan-report.md) — Step 8 FAILED |
| **Client shell** | [`pm-web-app/src/app.html`](../../pm-web-app/src/app.html) — inline `style="display: contents"` |
| **Server CSP** | `plan-marshall-mcp/pm-mcp-server/src/main/java/de/planmarshall/mcp/server/web/WebRequestHandler.java` — `SECURITY_HEADERS` |
| **Server proof** | `WebAppIT`, `WebRequestHandlerTest` |
| **Web spec** | [`03-web-server.adoc#web-security-headers`](../../../plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc#web-security-headers) |
| **Use cases** | UC-ENT-1, UC-ONB-1, UC-PLN-4 |

### 1.1 Root cause

A static SvelteKit export needs one inline script to import and start the app. The shell also wraps the body in an inline `style` attribute. The inherited CSP allows only same-origin external scripts and does not allow inline styles. The integration test checks headers and resources, so it passes while the browser refuses to execute the bootstrap.

---

## 2. Scope

### Clients (`pm-mcp-clients`, `pm-web-app`)

1. Remove the inline `style="display: contents"` wrapper from `app.html` (use a class or place `%sveltekit.body%` directly), and verify the layout in the normal Vite dev and static preview.
2. Keep the standard SvelteKit bootstrap as the only inline script; do not add a second inline script.
3. If a Trusted Types policy is required by `require-trusted-types-for 'script'`, add the smallest policy the framework needs, or record why that directive must be narrowed; do not silently drop it.

### Server (`plan-marshall-mcp`, `pm-mcp-server`)

4. Make `script-src` admit the shell's inline bootstrap without `'unsafe-inline'`:
   - preferred: compute the SHA-256 of the exact inline script at startup from the served `index.html`, cache it, and append `'sha256-…'` to `script-src`;
   - acceptable: per-response nonce injected into the shell and the CSP, if the hash cannot follow the build.
5. Leave `style-src` at `'self'` after the inline style is removed. Do not add `'unsafe-inline'`.
6. Keep every other directive (`connect-src`, `object-src`, `base-uri`, `frame-ancestors`, Trusted Types) and all existing headers unchanged.
7. Add a browser-level proof:
   - start the packaged daemon with the resource JAR;
   - load `/` in a headless browser;
   - assert the app renders a known visible element and can call `/api/v1` without a CSP violation;
   - assert the browser console has no CSP-blocked script/style.
   `WebAppIT` alone is not sufficient; it must remain as the HTTP-level proof.

### Non-Goals

- No change to the app's routes, views, API, auth, or pairing.
- No CORS, no separate static server, no CDN.
- No weakening of the CSP to `'unsafe-inline'` for scripts.
- No change to the native resource declaration or the SPA fallback.

---

## 3. Boundaries

| Repository | Worktree | Branch | Scope |
|---|---|---|---|
| `pm-mcp-clients` | `/Users/oliver/git/worktrees/pm-mcp-clients-web-app-csp` | `fix/web-app-csp` | `pm-web-app/src/app.html`, static-export docs/tests |
| `plan-marshall-mcp` | `/Users/oliver/git/worktrees/plan-marshall-mcp-web-app-csp` | `fix/web-app-csp` | `WebRequestHandler` CSP hash/nonce, `WebAppIT`, browser smoke |

**No touch**: `pm-api` records, `pm-mock-runtime`, `pm-operator`, the reports/decisions work, or any other view.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktrees created in both repositories off latest `main`:
  - `/Users/oliver/git/worktrees/pm-mcp-clients-web-app-csp` on `fix/web-app-csp`
  - `/Users/oliver/git/worktrees/plan-marshall-mcp-web-app-csp` on `fix/web-app-csp`
- [x] 2. Personas loaded: `plan-marshall-persona-implementer`, `plan-marshall-persona-integration-tester`, `plan-marshall-persona-security-expert`, `plan-marshall-persona-documenter`, `plan-marshall-persona-code-reviewer`.
- [x] 3. Implement and test §5.
- [x] 4. Report written; plan moved to `implemented/`; dashboard/index updated.
- [ ] 5. Clients `./mvnw verify -Ppre-commit`; server `./mvnw verify -Ppre-commit` and `-Pintegration-tests`.
- [ ] 6. PRs in the landing order (clients first, then server), no `skip-bot-review`.
- [ ] 7. Clients artifact published/installed before the server build; both PRs merged; worktrees cleaned.
- [ ] 8. Post-landing verification and report archival by the orchestrator.

---

## 5. Technical Recipe

- [x] **5.1 Client shell**
  - Replace the inline-style wrapper in `app.html`; keep the generated SvelteKit head and bootstrap.
  - Build and inspect `build/index.html`: one inline bootstrap script, no inline `style` attribute.
- [x] **5.2 Server CSP**
  - Read the served `index.html` once at startup; extract the exact inline script body; compute SHA-256; append `'sha256-<base64>'` to `script-src`.
  - If the shell changes at runtime, cache the header per loaded shell or compute a nonce per response.
  - Add a unit test for the hash extraction and the resulting header.
- [x] **5.3 Browser proof**
  - Start the packaged daemon and load `/` in Chromium; assert a visible app element and no CSP console error.
  - Fetch an API endpoint from the page to prove `connect-src 'self'`.
  - Repeat against the native runner when built.
- [x] **5.4 Security review**
  - Confirm no `'unsafe-inline'` was added; `object-src`, `base-uri`, `frame-ancestors`, and the other headers are unchanged.
  - Record the Trusted Types decision in the design note/README.
- [x] **5.5 Records**
  - Update the parent report’s F5/Step 8 section; move both reports only after the browser proof is green.

---

## 6. Acceptance Criteria

| Criterion | Evidence |
|---|---|
| A real browser loads the daemon-served `/` and renders the app. | Headless browser smoke against the packaged daemon. |
| The CSP permits the shell's bootstrap without `'unsafe-inline'`. | Header test and browser console clean. |
| No inline style attribute remains in the built shell. | `build/index.html` inspection. |
| API/MCP boundary, security headers, SPA fallback, and native resources are unchanged. | `WebAppIT` + browser smoke + native run when built. |
| The native runner still serves and boots the app. | Native `WebAppIT` and browser smoke. |

---

## 7. Risk & Rollback

- **Hash drift**: the SvelteKit version string changes the inline script; compute the hash from the exact shell at startup, not from a constant.
- **Trusted Types**: if the framework needs a policy, add it explicitly; do not remove the directive without a security review.
- **Nonce alternative**: a per-response nonce must be injected into the served shell and the CSP consistently; do not cache a nonce.
- **Rollback**: restore the inline-style wrapper and the static `script-src 'self'`; the browser failure returns with the same evidence.
