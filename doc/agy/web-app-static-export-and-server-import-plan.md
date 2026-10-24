# Implementation Plan: `web-app-static-export-and-server-import` (Ship the SPA Resource JAR and Serve It from `pm-mcpd`)

This implementation plan closes the gap between the static web-app build and the core server. `pm-web-app` already builds a SvelteKit static SPA and packages it as a resource JAR, but `pm-mcp-server` has no dependency on that JAR and `WebRequestHandler` still answers every non-API `GET` with `APP_PLACEHOLDER` (“The web app is not part of this build.”).

The plan imports the web-app resource JAR into `pm-mcp-server`, serves its `META-INF/resources` tree with an SPA fallback on the web listener, preserves the existing API/MCP boundary and security headers, and adds a **server-level packaged-daemon integration test** that proves the real built app is served. The existing `DaemonProcess`/`TestRuntime`/`UdsHttp` IT infrastructure is reused; no new test harness is needed unless the packaged-daemon test cannot reach the listener.

The work spans two repositories:

| Part | Repository | Why |
|---|---|---|
| Static export proof and packaging | `pm-mcp-clients` (`pm-web-app`) | The SvelteKit static build and `META-INF/resources` packaging are owned here. |
| Dependency import, static handler, native resources, server IT | `plan-marshall-mcp` (`pm-mcp-server`) | The daemon assembly and web listener are owned here. |

It operationalizes [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) under the execution discipline of [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`UC-ENT-1`](../../../plan-marshall-documentation/doc/use-cases/02-entry.adoc#UC-ENT-1) (Enter and Connect), [`UC-ONB-1`](../../../plan-marshall-documentation/doc/use-cases/01-onboarding.adoc#UC-ONB-1) (Machine Onboarding), [`UC-PLN-4`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-4) (See What Runs) |
| **Web server specification** | [`03-web-server.adoc#web-app`](../../../plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc#web-app), [`#web-proxy`](../../../plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc#web-proxy), [`#web-security-headers`](../../../plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc#web-security-headers) |
| **Current export** | [`pm-web-app/pom.xml`](../../pm-web-app/pom.xml) (`frontend-maven-plugin`, `copy-spa-resources`), [`pm-web-app/vite.config.ts`](../../pm-web-app/vite.config.ts), [`pm-web-app/build/index.html`](../../pm-web-app/build/index.html) |
| **Current server gap** | [`WebRequestHandler.java`](../../../plan-marshall-mcp/pm-mcp-server/src/main/java/de/planmarshall/mcp/server/web/WebRequestHandler.java) (`APP_PLACEHOLDER`), [`WebListener.java`](../../../plan-marshall-mcp/pm-mcp-server/src/main/java/de/planmarshall/mcp/server/web/WebListener.java), [`pm-mcp-server/pom.xml`](../../../plan-marshall-mcp/pm-mcp-server/pom.xml) |
| **Existing IT infrastructure** | [`DaemonProcess.java`](../../../plan-marshall-mcp/pm-mcp-server/src/test/java/de/planmarshall/mcp/server/test/DaemonProcess.java), [`TestRuntime.java`](../../../plan-marshall-mcp/pm-mcp-server/src/test/java/de/planmarshall/mcp/server/test/TestRuntime.java), [`UdsHttp.java`](../../../plan-marshall-mcp/pm-mcp-server/src/test/java/de/planmarshall/mcp/server/test/UdsHttp.java), [`WebListenerIT.java`](../../../plan-marshall-mcp/pm-mcp-server/src/test/java/de/planmarshall/mcp/server/web/WebListenerIT.java) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

### 1.1 Finding Analysis

| # | Finding | Root cause | Correction |
|---|---|---|---|
| **F1** | The static app is built but the daemon serves a placeholder. | `pm-mcp-server` does not depend on the `pm-web-app` artifact; `WebRequestHandler` has no static-resource branch. | Add the `pm-web-app` dependency and a `StaticWebApp` handler for `META-INF/resources`. |
| **F2** | No server-level proof that the packaged app works. | Existing `WebListenerIT` asserts only the placeholder `/` (`200`) and the API/MCP boundary; there is no assertion on the asset tree or SPA fallback. | Add `WebAppIT` that starts the packaged daemon, enables the listener, fetches `/`, an immutable JS asset and a client route, and checks API/MCP/unknown paths and security headers. |
| **F3** | The static app could serve in JVM mode but disappear from the native binary. | The handler reads classpath resources; GraalVM includes only configured resources by default. | Declare `quarkus.native.resources.includes=META-INF/resources/**` (or the Quarkus resource equivalent) and verify the native path where the host supports it. |
| **F4** | SPA routing and static assets have different fallback rules. | A blanket “return index.html for unknown GET” would answer missing assets with HTML and hide 404s. | Serve real resources first; fall back to `index.html` only for extensionless/`Accept: text/html` navigation; return `404` for missing assets and unknown API paths. |
| **F5** | The web app uses an inline `style`/CSS-mask surface and a CSP with no `style-src`. | The existing runtime CSP (`default-src 'self'`) can block inline styles in production. This is pre-existing but newly exposed by serving the app from the daemon. | Include the CSP headers in the IT assertions; if the packaged app fails in a browser due to inline styles, add a focused CSP/`style-src` fix as a follow-up rather than silently loosening the policy. |

---

## 2. Scope & Objectives

### Part A: Export the Static App as a Resource JAR (`pm-mcp-clients`)

1. Confirm the existing `pm-web-app` packaging:
   - `npm run build` produces `build/index.html` and `build/_app/immutable/**`.
   - `maven-resources-plugin` copies `build/` into `target/classes/META-INF/resources/`.
   - `maven-jar-plugin` packages that tree into `pm-web-app-*.jar`.
2. Add a **packaging verification** if the current build does not already prove it:
   - after `./mvnw -pl pm-web-app package -DskipTests`, assert the JAR contains `META-INF/resources/index.html` and at least one `META-INF/resources/_app/immutable/...` asset;
   - record the command and entries in the report.
3. Keep the app same-origin: it calls `/api/v1` and requires no CORS or API-base rewrite.
4. Do not change the web UI, pairing, or session behavior.

### Part B: Import the Artifact into the Server (`plan-marshall-mcp`)

1. Add `pm-web-app` to the dependency management of `plan-marshall-mcp` (version `${version.pm-mcp-clients}`) and as a `pm-mcp-server` dependency.
2. Make the packaged daemon include the resource JAR:
   - JVM runner and native image both carry `META-INF/resources`.
3. Add `quarkus.native.resources.includes` (or the Quarkus equivalent) for `META-INF/resources/**` so the native binary can serve the assets.
4. Keep the server’s build order explicit: the web-app artifact must be installed/published before the server consumes it. Local verification installs `pm-web-app` first; CI resolves the published SNAPSHOT.

### Part C: Serve the Static App (`pm-mcp-server`)

1. Add `StaticWebApp` (package `de.planmarshall.mcp.server.web`):
   - resource root `META-INF/resources`;
   - `index.html` entry;
   - stream a classpath resource as bytes with a content type;
   - content types for HTML, JS, CSS, JSON, SVG, PNG, ICO, WOFF/WOFF2, map, and text;
   - `Cache-Control: no-cache` for `index.html`; immutable caching for `_app/immutable/` assets; sensible fallback for other files;
   - reject traversal/encoded separators (the existing `NOT_NORMALIZED` guard remains) and never list directories.
2. Wire it into `WebRequestHandler`:
   - `/api/v1/**` → runtime API handler, unchanged;
   - `/mcp` and `/mcp/**` → `404`, unchanged;
   - other `/api/**` → `404`, unchanged;
   - `GET`/`HEAD` outside `/api/` → `StaticWebApp`:
     - existing asset → stream it;
     - missing asset with a file extension → `404`;
     - extensionless or `Accept: text/html` navigation → `index.html` (SPA fallback);
     - absent `index.html` (a server build without the resource JAR) → current `APP_PLACEHOLDER`, so the daemon still starts;
   - keep `SECURITY_HEADERS` on every response.
3. Pass the static handler from `WebListener` when constructing `WebRequestHandler`.
4. Add unit tests for the handler/static resource:
   - `GET /` → index, `text/html`;
   - `GET /_app/immutable/...js` → JS, asset body;
   - `GET /designer` → index fallback;
   - `GET /missing.js` → 404;
   - `GET /api/v1/...` → API handler (not static);
   - `/mcp` → 404;
   - traversal/encoded path → 400;
   - security headers present on static responses.

### Part D: Server-Level Integration Test (`pm-mcp-server`)

Use the existing `DaemonProcess`/`TestRuntime`/`UdsHttp` infrastructure; no new harness unless the packaged-daemon path is unavailable.

Add `pm-mcp-server/src/test/java/de/planmarshall/mcp/server/web/WebAppIT.java` (name ends with `IT`, so `-Pintegration-tests` runs it):

1. Start the packaged daemon with `DaemonProcess.startReady(base)`.
2. Open the loopback web listener with `PUT /api/v1/web {"enabled":true,"lan":false,"port":0}`.
3. With the listener port and a valid `Host` header:
   - `GET /` → `200`, `text/html`, body is the real app (contains `plan-marshall`, `_app/`, and **not** “The web app is not part of this build”).
   - Extract an immutable asset path from `index.html` (e.g. `/_app/immutable/...js`), fetch it → `200`, JavaScript content type, non-empty body.
   - `GET /designer?project=pm-mcp-clients` → `200`, same app shell (SPA fallback).
   - `GET /missing.js` → `404` (not the index fallback).
   - `GET /api/v1/status` without a device secret → `401` (API boundary; not the app).
   - `GET /mcp` → `404`.
   - `GET /api/unknown` → `404`.
   - A mismatching `Host` → `403`.
   - `Content-Security-Policy`, `X-Frame-Options`, `X-Content-Type-Options`, and `Referrer-Policy` are present on the index and asset responses.
4. Close the listener and the daemon in `finally`/`@AfterAll`.
5. If the native runner is the packaged build, the same test must pass against it; otherwise record the JVM-only result and run the native build/test separately.

The test must fail on the current `APP_PLACEHOLDER` and pass only when the real `pm-web-app` resources are on the server classpath.

### Part E: Documentation

- Update the server README/module documentation with the dependency and how the static app is served.
- Update `pm-web-app/README.md` with the export/import commands and the same-origin API expectation.
- Note the artifact dependency direction in the server POM/docs: clients publish `pm-web-app`, server consumes it.

### Non-Goals

- Changing the web UI, pairing, session, API, MCP route, or TLS behavior.
- Serving the app from a CDN or a separate static web server.
- Adding CORS or changing the API origin.
- Changing the Vite development server.
- Reactivating the default-units workflow icons or other unrelated plans.

---

## 3. Cross-Repository Isolation & Landing Order

> [!IMPORTANT]
> **Pre-flight**: before creating any worktree or branch, run `git checkout main && git pull --rebase origin main` in **every** repository involved. Never branch from stale `main`.

| Order | Repository | Worktree | Branch | Scope |
|---|---|---|---|---|
| **1** | `pm-mcp-clients` | `/Users/oliver/git/worktrees/pm-mcp-clients-web-app-static-export` | `feat/web-app-static-export` | Export/package proof, README, completion report. |
| **2** | `plan-marshall-mcp` | `/Users/oliver/git/worktrees/plan-marshall-mcp-web-app-static-export` | `feat/web-app-static-export` | Dependency, static handler, native resources, server IT. |

**Landing rules**:
- Publish/install the `pm-web-app` artifact first; then the server can resolve it.
- The server IT is the acceptance gate; it must run against the packaged daemon, not a mocked classpath.
- Each repository follows its own pre-commit and review rules; worker PRs never use `skip-bot-review`.

### Boundary Rules

- **Clients sole owner**: `pm-web-app/pom.xml` (only if the packaging check needs a fix), `pm-web-app/README.md`, the new packaging check/docs, and the clients completion report.
- **Server sole owner**: `plan-marshall-mcp/pom.xml` (dependency management), `pm-mcp-server/pom.xml`, `WebRequestHandler`, `WebListener`, new `StaticWebApp` and its tests, `WebAppIT`, `application.properties` native resource includes, server docs.
- **No touch**: `pm-api` production records, pairing/session logic, `pm-operator`, `pm-relay`, `pm-mock-runtime`, `pm-web-e2e`, TLS and security-header policy except the static response path.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktrees created in both repositories after pulling `main`:
  - `/Users/oliver/git/worktrees/pm-mcp-clients-web-app-static-export` on `feat/web-app-static-export`
  - `/Users/oliver/git/worktrees/plan-marshall-mcp-web-app-static-export` on `feat/web-app-static-export`
- [ ] 2. Domain skills & engineering personas loaded where applicable:
  - `plan-marshall-persona-implementer` & `pm-dev-java:java-core` for the server handler and dependency.
  - `plan-marshall-persona-integration-tester` for the packaged-daemon IT.
  - `plan-marshall-persona-security-expert` for static path confinement, content types, CSP, and cache headers.
  - `plan-marshall-persona-documenter` for the export/import docs.
  - `plan-marshall-persona-code-reviewer` for review triage.
- [ ] 3. Incremental implementation with tests following the phased technical recipe.
- [ ] 4. Completion report written (`doc/agy/web-app-static-export-and-server-import-plan-report.md`), plan moved to `doc/agy/implemented/`, links rebased, and dashboard/index updated.
- [ ] 5. Pre-commit passed: clients `./mvnw verify -Ppre-commit`; server `./mvnw verify -Ppre-commit` plus `-Pintegration-tests` for the IT.
- [ ] 6. PRs opened in the landing order (no `skip-bot-review`); review windows and findings resolved.
- [ ] 7. Artifact published/installed first; server PR merged; worktrees and branches cleaned up.
- [ ] 8. Post-landing verification performed; clients report moved to `doc/agy/verified/`.

> [!IMPORTANT]
> **Plan archival link rule**: this plan is written at `doc/agy/`; its links use `../meta`, `../design`, `../../../plan-marshall-mcp`, `../../../plan-marshall-documentation`, and `implemented/<name>.md`. When Step 4 moves it to `doc/agy/implemented/`, rebase every link (`../../meta`, `../../design`, `../../../../plan-marshall-mcp`, `../../../../plan-marshall-documentation`, `../runbook.md`, sibling `<name>.md`) and re-run the internal-link check.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Export proof** | `plan-marshall-persona-implementer` | `pm-dev-frontend:javascript`<br>`plan-marshall-persona-module-tester` | Static build, resource-JAR layout, packaging check, same-origin contract. |
| **Phase 2: Dependency import** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Maven dependency, build order, native resource inclusion. |
| **Phase 3: Static serving** | `plan-marshall-persona-implementer` | `plan-marshall-persona-security-expert` | Path confinement, MIME types, SPA fallback, cache, security headers. |
| **Phase 4: Server IT** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-core` | Packaged daemon, real resource JAR, API/MCP boundary, native/JVM. |
| **Phase 5: Verification/review** | `plan-marshall-persona-code-reviewer` | `plan-marshall-persona-documenter` | Cross-repo evidence, docs, report, review triage. |

---

## 6. Phased Technical Recipe

### Phase 1: Export and Verify the Static Artifact (`pm-mcp-clients`)

- [ ] **1.1 Build and inspect**:
  - `cd pm-web-app && npm ci && npm run build` (or `./mvnw -pl pm-web-app package -DskipTests`).
  - Confirm `build/index.html` and `build/_app/immutable/**`.
  - Build the JAR and assert:
    ```bash
    jar tf pm-web-app/target/pm-web-app-0.1.0-SNAPSHOT.jar \
      | grep -E 'META-INF/resources/(index.html|_app/immutable/)'
    ```
  - If the resource copy is missing, fix `pm-web-app/pom.xml` before proceeding.
- [ ] **1.2 Keep the app same-origin**:
  - Confirm no `base`/`origin` change is needed: the built app calls `/api/v1` on its own origin.
  - Do not add CORS or a server URL setting.
- [ ] **1.3 Install/publish**:
  - Local: `./mvnw -pl pm-web-app install -DskipTests`.
  - CI: the `pm-mcp-clients` SNAPSHOT publication must complete before the server build resolves `pm-web-app`.

### Phase 2: Import the Artifact (`plan-marshall-mcp`)

- [ ] **2.1 Dependency management**:
  - In `plan-marshall-mcp/pom.xml`, add `de.planmarshall:pm-web-app` at `${version.pm-mcp-clients}` beside the other clients modules.
- [ ] **2.2 Server dependency**:
  - In `pm-mcp-server/pom.xml`, add the `pm-web-app` dependency.
  - Verify `mvn dependency:tree` includes the resource JAR and that `target/classes` / the runner sees `META-INF/resources/index.html`.
- [ ] **2.3 Native resource inclusion**:
  - Add `quarkus.native.resources.includes=META-INF/resources/**` (or the Quarkus resource-config equivalent) to `application.properties`/native metadata.
  - Build the native runner where the host supports it and verify the resource is present.
- [ ] **2.4 Build order**:
  - Document that clients publish `pm-web-app` first; the server consumes the SNAPSHOT. Do not duplicate the web-app source in the server.

### Phase 3: Static Handler and Wiring (`pm-mcp-server`)

- [ ] **3.1 Add `StaticWebApp`**:
  - Resolve `META-INF/resources` via the runtime classloader (`Thread.currentThread().getContextClassLoader()` or `WebRequestHandler`'s loader).
  - `GET /` and extensionless navigation → `index.html`.
  - Existing asset → stream bytes with the correct content type.
  - Missing asset with a file extension → `404`; do not fall back to HTML.
  - `Cache-Control`: `no-cache` for `index.html`, immutable for `_app/immutable/`, a short/no-cache default otherwise.
  - Reject traversal and encoded separators; reuse the existing `NOT_NORMALIZED` guard and never list directories.
- [ ] **3.2 Wire `WebRequestHandler`**:
  - Add a `StaticWebApp` field/constructor parameter.
  - Replace the unconditional `APP_PLACEHOLDER` branch with the static handler.
  - Keep `APP_PLACEHOLDER` only for a build without the resource JAR.
  - Keep `/api/v1/**`, `/mcp`, other `/api/**`, `OPTIONS`, `Host` and `Origin` behavior unchanged.
- [ ] **3.3 Wire `WebListener`**:
  - Construct the handler with the static app; no other listener/TLS change.
- [ ] **3.4 Tests**:
  - `StaticWebAppTest` / `WebRequestHandlerTest`: index, asset, SPA fallback, missing asset, traversal, API/MCP boundary, security headers.
  - Use a small test classpath fixture or a test resource tree; the packaged IT remains the real proof.

### Phase 4: Server-Level Integration Test

- [ ] **4.1 Add `WebAppIT`** under `pm-mcp-server/src/test/java/de/planmarshall/mcp/server/web/`.
- [ ] **4.2 Reuse the IT infrastructure**:
  - `DaemonProcess.startReady(base)`; `TestRuntime.bearer(daemon.token())`; `UdsHttp` for the socket API and loopback web requests.
  - Enable the listener with `PUT /api/v1/web {"enabled":true,"lan":false,"port":0}` and close it in cleanup.
- [ ] **4.3 Assertions**:
  - `/` → `200`, `text/html`, real app shell (`plan-marshall`, `_app/`), not `APP_PLACEHOLDER`.
  - An immutable asset extracted from the index → `200`, JS content type, non-empty.
  - `/designer` → `200`, app shell (SPA fallback).
  - `/missing.js` → `404`; `/api/v1/status` without secret → `401`; `/mcp` → `404`; `/api/unknown` → `404`; wrong `Host` → `403`.
  - Security headers present on `/` and the asset.
- [ ] **4.4 Mode**:
  - Run under `-Pintegration-tests` against the packaged JVM runner.
  - Run the same test against the native runner when the build host supports it; record the result.
- [ ] **4.5 No new IT infra**: the existing `DaemonProcess`, `TestRuntime`, and `UdsHttp` are sufficient. If a new harness is genuinely needed, add a focused helper under `pm-mcp-server/src/test/java/.../test/`, not a second framework.

### Phase 5: Verification, Docs, Report

- [ ] **5.1 Client**:
  - `./mvnw -pl pm-web-app package -DskipTests` with the JAR resource check.
  - `npm test`, `npm run check`, `npm run lint`, and `./mvnw verify -Ppre-commit`.
- [ ] **5.2 Server**:
  - `./mvnw -pl pm-mcp-server -Pintegration-tests verify` (the `WebAppIT` must pass).
  - `./mvnw verify -Ppre-commit` and, where supported, the native build/test.
- [ ] **5.3 Docs**:
  - Server README: dependency, serving path, native resource requirement.
  - `pm-web-app/README.md`: export/import commands and same-origin API.
- [ ] **5.4 Report**:
  - Write `doc/agy/web-app-static-export-and-server-import-plan-report.md` with the JAR entries, server IT output, headers, asset/SPA evidence, and native status.
  - Move the plan to `implemented/`, rebase links, update the dashboard/index.

---

## 7. Acceptance Criteria

| Finding | Acceptance criterion | Evidence |
|---|---|---|
| **F1** | The built `pm-web-app` JAR contains `META-INF/resources/index.html` and at least one `_app/immutable` asset. | `jar tf` output in the report. |
| **F2** | The packaged daemon serves the real app at `/` and a client route, with `200`/`text/html`; the placeholder is absent. | `WebAppIT` on the packaged daemon. |
| **F3** | An immutable JS asset from the served index returns `200` with a JavaScript content type and non-empty body. | `WebAppIT` asset assertion. |
| **F4** | Missing assets return `404`; API/MCP/unknown API paths keep their existing status; wrong `Host` returns `403`. | `WebAppIT` negative cases. |
| **F5** | Security headers are present on the app shell and an asset. | `WebAppIT` header assertions. |
| **F6** | The native build carries `META-INF/resources` (or the native path is explicitly recorded as unsupported on the host). | Native build/resource check. |
| **F7** | Clients and server pre-commit/review gates pass; the clients SNAPSHOT is published before the server consumes it. | CI/PR evidence. |

---

## 8. Risk & Rollback Notes

- **Build order**: the server now depends on a clients artifact. A server build without the published SNAPSHOT fails; document `install`/publication order. Rollback is the single dependency line and the handler wiring.
- **Static path safety**: only `META-INF/resources` is reachable; traversal/encoded separators are rejected before resolution; directories are never listed.
- **SPA fallback**: fallback only for extensionless/HTML navigation; a missing JS/CSS asset stays `404`.
- **Native image**: classpath resource reading needs explicit native resource inclusion; test both modes where possible.
- **CSP**: the existing `default-src 'self'` policy may block inline styles in a browser. The IT verifies headers, not browser rendering; if the packaged app is visually broken by CSP, add a focused `style-src` follow-up rather than weakening the policy silently.
- **Cache**: immutable caching only for hashed `_app/immutable` assets; `index.html` stays `no-cache`. A stale index is not cached.
- **Placeholder**: kept only as a fallback for a server build without the resource JAR; the acceptance test fails if it is used.
- **No CORS**: the app and API are same-origin; adding CORS would widen the threat surface and is out of scope.
- **Rollback**: remove the dependency and restore the placeholder branch; no store or protocol format changes.
