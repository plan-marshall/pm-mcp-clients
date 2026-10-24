# Completion Report: `web-app-static-export-and-server-import`

## Executive Summary

`pm-web-app` already built a SvelteKit SPA and packaged it as a resource JAR, but `pm-mcpd` answered every
non-API web request with the `APP_PLACEHOLDER` page. This plan closes that gap across two repositories:

- **Clients (`pm-mcp-clients`, `pm-web-app`)** — the packaging is verified by the build itself: a `verify`
  execution asserts that the JAR carries `META-INF/resources/index.html` and at least one
  `META-INF/resources/_app/immutable/...` asset, and fails the build otherwise. The module README documents
  the export, the inspection command, the install order, and the same-origin API expectation.
- **Server (`plan-marshall-mcp`)** — `pm-mcp-server` now depends on the `pm-web-app` resource JAR (version
  `${version.pm-mcp-clients}`), serves its `META-INF/resources` tree with SPA fallback through a new
  `StaticWebApp`, keeps the API/MCP boundary and every security header, declares the resources for the native
  image, and proves the whole path with `WebAppIT` against the packaged daemon.

The static handler serves real files first, falls back to `index.html` only for a navigation, keeps a missing
asset a `404`, caches only the content-hashed `_app/immutable/` files as immutable, and answers with the old
placeholder only when the resource JAR is absent from the build.

## Delivered Artifacts

### `pm-mcp-clients` — `pm-web-app`

- `pm-web-app/pom.xml` — added the `maven-antrun-plugin` verification (`verify-spa-resources`). It counts
  `META-INF/resources/index.html` and `META-INF/resources/_app/immutable/**` inside the built JAR and fails
  the build when either is missing; it echoes the packaged counts on success. No other build change was
  needed: the existing `copy-spa-resources` and `maven-jar-plugin` already produce the resource layout.
- `pm-web-app/README.md` — new section **Static Export and Daemon Serving**: the build commands, the `jar tf`
  inspection command, the `install` order for the server build, and the same-origin `/api/v1` contract (no
  API base URL, no CORS, no separate static server).
- `doc/agy/web-app-static-export-and-server-import-plan-report.md` — this report.
- The plan archived at `doc/agy/implemented/web-app-static-export-and-server-import-plan.md` with every
  relative link rebased; the dashboard (`doc/agy/README.md`) and the archive index
  (`doc/agy/implemented/README.md`) updated.
- Pull request [#200](https://github.com/plan-marshall/pm-mcp-clients/pull/200), branch
  `feat/web-app-static-export`.

### `plan-marshall-mcp` — `pm-mcp-server`

- `pom.xml` — `pm-web-app` added to `dependencyManagement` at `${version.pm-mcp-clients}`, beside the other
  clients modules.
- `pm-mcp-server/pom.xml` — the `pm-web-app` dependency, with the dependency direction documented.
- `pm-mcp-server/src/main/resources/application.properties` —
  `quarkus.native.resources.includes=pm-mcp/tools/*.json,META-INF/resources/**`.
- `pm-mcp-server/src/main/java/de/planmarshall/mcp/server/web/StaticWebApp.java` **(new)** — resolves the
  app below `META-INF/resources` through the runtime class loader; extension-to-content-type mapping; SPA
  fallback decision; cache directives; confinement (backslash, percent-encoding, empty/dot segments are
  refused, directories are never served or listed); `Buffer` bodies for the Vert.x response.
- `WebRequestHandler.java` — `StaticWebApp` field and constructor parameter; `GET`/`HEAD` outside `/api/`
  now stream the app or the shell; missing asset `404`; `APP_PLACEHOLDER` only for a build without the
  resource JAR; every response keeps the security headers; `/api/v1/**`, `/mcp`, other `/api/**`, `Host`,
  `Origin`, and `OPTIONS` behaviour unchanged.
- `WebListener.java` — constructs the handler with the static app (the injected listener uses the context
  class loader; a package-private constructor takes a test fixture).
- `README.md` — new section **Web app**: routing table, cache rules, the clients→server dependency
  direction and publish order, the native-resource requirement, `WebAppIT` as the proof, and the known CSP
  limitation of the current SvelteKit shell.
- `pm-mcp-server/src/test/java/de/planmarshall/mcp/server/web/StaticWebAppTest.java` **(new)** — 35 cases:
  index and content types, cache directives, SPA fallback, missing assets, traversal/encoded/backslash
  paths, directories, an absent resource JAR, non-absolute paths, and a fixture-completeness walk.
- `pm-mcp-server/src/test/java/de/planmarshall/mcp/server/web/WebRequestHandlerTest.java` **(new)** — 10
  cases over a real Vert.x listener: index, immutable asset, shell, `Accept: text/html` fallback, missing
  asset, API/MCP boundary, `400` confinement, `HEAD`, the placeholder of a JAR-less build, and the `Host` /
  `OPTIONS` guard.
- `pm-mcp-server/src/test/java/de/planmarshall/mcp/server/web/WebAppIT.java` **(new)** — the packaged-daemon
  proof (two tests).
- Pull request [plan-marshall/plan-marshall-mcp#104](https://github.com/plan-marshall/plan-marshall-mcp/pull/104),
  branch `feat/web-app-static-export`.

## Test & Verification Results

### Clients: packaging proof

```text
$ ./mvnw -pl pm-web-app package -DskipTests
[INFO] Copying 40 resources from build to target/classes/META-INF/resources
[INFO] Building jar: .../pm-web-app/target/pm-web-app-0.1.0-SNAPSHOT.jar
[INFO] BUILD SUCCESS
```

The `verify-spa-resources` check is bound to `verify`, so it reports its counts there (the full
`./mvnw verify -Ppre-commit` below runs the same execution):

```text
$ ./mvnw -pl pm-web-app verify -Dmaven.test.skip=true -Dskip.lint=true -Dskip.typecheck=true
[WARNING]      [echo] pm-web-app: packaged 1 index.html and 37 immutable asset(s) under META-INF/resources.
[INFO] BUILD SUCCESS
```

The JAR inspection records:

```text
$ jar tf pm-web-app/target/pm-web-app-0.1.0-SNAPSHOT.jar | grep -E '^META-INF/resources/(index\.html|_app/immutable/.+\.(js|css))$' | head -6
META-INF/resources/_app/immutable/assets/0.CIpBYpR4.css
META-INF/resources/_app/immutable/assets/AppRouter.BlzYb664.css
META-INF/resources/_app/immutable/assets/gitSource.DrBJD4dC.css
META-INF/resources/_app/immutable/chunks/-d_arnM_.js
META-INF/resources/_app/immutable/chunks/Bjy-W4x2.js
META-INF/resources/_app/immutable/chunks/Bv8o_ktL.js

index.html: 1
immutable files: 37
total META-INF/resources entries: 47
```

The app is confirmed same-origin: `ApiClient` defaults its `baseUrl` to the empty string and calls
`/api/v1/...` on the origin that served it (`src/lib/api/client.ts`), so no base/origin change, CORS, or
server URL setting was added.

### Server: unit tests

```text
$ ./mvnw -pl pm-mcp-server test
[INFO] Tests run: 143, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

This includes the architecture guard `AssemblyHoldsWiringOnlyTest` (the static handler references a Vert.x
type, so it does not count as framework-free logic in the assembly) and the existing Quarkus web-listener
tests, which now see the real resource JAR on the test classpath.

### Server: packaged-daemon integration test (`WebAppIT`, JVM runner)

```text
$ ./mvnw -pl pm-mcp-server -Pintegration-tests verify -Dit.test=WebAppIT -DskipTests
[INFO] Running Packaged web app served by the packaged daemon
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Measured evidence (`target/verification-results/web-app-static-export.json`):

| Figure | Value |
|---|---|
| `GET /` | `200`, `text/html; charset=utf-8`, real shell (`plan-marshall`, `_app/`), placeholder absent |
| Immutable asset extracted from the served index | `/_app/immutable/entry/start.DP1uF47u.js`, `200`, `text/javascript; charset=utf-8`, 522 bytes, `Cache-Control: public, max-age=31536000, immutable` |
| `GET /designer?project=pm-mcp-clients` | `200`, `text/html; charset=utf-8`, app shell (SPA fallback) |
| `GET /missing.js` | `404` (not the index, not the placeholder) |
| `GET /api/v1/status` without a device secret | `401` |
| `GET /mcp` | `404` |
| `GET /api/unknown` | `404` |
| Mismatching `Host` | `403` |
| `GET /%2e%2e/secret` | `400` |
| Security headers on `/`, the asset, and the client route | `Content-Security-Policy`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer` |
| `HEAD /` | `200`, `text/html`, no body |

### Native image

`setup: GraalVM CE 25.0.4 (native-image 25.0.4.1.1), macOS aarch64.`

```text
$ JAVA_HOME=$HOME/.sdkman/candidates/java/graalce-25 GRAALVM_HOME=... \
    ./mvnw -pl pm-mcp-server -Pnative package -DskipTests
[INFO] [io.quarkus.deployment.QuarkusAugmentor] Quarkus augmentation completed in 118677ms
Finished generating 'pm-mcp-server-0.1.1-SNAPSHOT-runner' in 1m 52s.
   28,06MiB (52,60%) for image heap:  402.984 objects and 83 resources
[INFO] BUILD SUCCESS
```

The image carries the resource tree: `quarkus.native.resources.includes=...,META-INF/resources/**` puts
`META-INF/resources` into the image (the build reports 83 embedded resources), and the same `WebAppIT` passes
against the native runner:

```text
$ ./mvnw -pl pm-mcp-server -Pintegration-tests failsafe:integration-test failsafe:verify -Dit.test=WebAppIT
[INFO] Running Packaged web app served by the packaged daemon
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

`target/verification-results/web-app-static-export.json` records `"runner": "native"`, `"index_status":
200`, `"asset_status": 200`, `"designer_status": 200`, `"missing_asset_status": 404`, `"pass": true`.
The JVM runner and the native runner therefore both serve the real packaged app.

## Domain Invariants

| Invariant | Status |
|---|---|
| Java 25 / configured release | Unchanged; the module compiles under the repository release. |
| No uncatalogued strings | No user-facing string was added; the placeholder is the existing constant. |
| Bilingual catalogue (EN & DE) | Not touched: no UI text changed. |
| Mock runtime completeness | Not touched: `pm-mock-runtime` is outside the footprint. |
| Responsive layout down to 320 px | Not touched: no view changed. |
| Assembly holds wiring only (`PM-IMPL-1`) | `AssemblyHoldsWiringOnlyTest` passes. |
| Clients publish, server consumes | `pom.xml` dependency management, the server dependency, and both READMEs state the direction and the install order. |
| Static path confinement | The handler's `NOT_NORMALIZED` guard plus `StaticWebApp`'s percent/backslash/dot/empty-segment refusal; directories are never served or listed. |
| API/MCP boundary | Unchanged: `/api/v1/**` to the runtime, `/mcp` and other `/api/**` `404`, proven in the unit and integration tests. |
| Security headers on every response | Unchanged and asserted on the static responses. |

## Build & Quality Evidence

```text
# clients
$ ./mvnw verify -Ppre-commit
[INFO] pm-mcp-clients ..................................... SUCCESS [ 10.645 s]
[INFO] pm-api ............................................. SUCCESS [01:49 min]
[INFO] pm-exec ............................................ SUCCESS [ 15.177 s]
[INFO] pm-mock-runtime .................................... SUCCESS [01:03 min]
[INFO] pm-relay ........................................... SUCCESS [ 13.775 s]
[INFO] pm-operator ........................................ SUCCESS [04:29 min]
[INFO] pm-web-app ......................................... SUCCESS [ 50.589 s]
[INFO] pm-web-e2e ......................................... SUCCESS [ 12.130 s]
[INFO] BUILD SUCCESS   (9:04 min)

# server
$ ./mvnw verify -Ppre-commit
[INFO] pm-mcp-server ...................................... SUCCESS [02:11 min]
[INFO] pm-e2e ............................................. SUCCESS [ 16.189 s]
[INFO] BUILD SUCCESS   (2:48 min; 143 unit tests)

# server integration tests, packaged daemon
$ ./mvnw -pl pm-mcp-server -Pintegration-tests failsafe:integration-test failsafe:verify -Dit.test=WebAppIT
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

The clients pre-commit gate reformatted one pre-existing line in
`pm-mock-runtime/src/test/java/de/planmarshall/mock/PlanViewScenarioTest.java` (indentation only); it is
committed with this change, as the runbook requires. No other file outside the footprint changed.

## Finding-by-Finding Acceptance

| Finding | Acceptance criterion | Evidence |
|---|---|---|
| **F1** | The built Jar carries `META-INF/resources/index.html` and at least one `_app/immutable` asset. | `jar tf` output and the new `verify-spa-resources` build check (1 index, 37 immutable assets). |
| **F2** | The packaged daemon serves the real app at `/` and a client route, `200`/`text/html`, placeholder absent. | `WebAppIT.shouldServePackagedApp` (JVM runner). |
| **F3** | An immutable JS asset from the served index returns `200`, a JavaScript content type, non-empty body. | `WebAppIT` asset assertion (`text/javascript`, 522 bytes). |
| **F4** | Missing assets `404`; API/MCP/unknown API keep their status; wrong `Host` `403`. | `WebAppIT` negative cases. |
| **F5** | Security headers present on the shell and an asset. | `WebAppIT` header assertions. |
| **F6** | The native build carries `META-INF/resources`, or the native path is recorded as unsupported on the host. | See **Native image**. |
| **F7** | Clients and server gates pass; the clients SNAPSHOT is published before the server consumes it. | See **Build & Quality Evidence** and the landing order below. |

## Known Limitation: CSP and the SvelteKit Shell (F5 follow-up)

The web listener sends `default-src 'self'; script-src 'self'; ...` on every response. The current
SvelteKit `index.html` boots through an inline `<script>` (the `__sveltekit_*` bootstrap) and carries an
inline `style="display: contents"` attribute, both of which that policy blocks in a browser. `WebAppIT`
proves that the daemon serves the shell and the assets with the headers; it does not (and cannot) prove
browser rendering. This is the pre-existing CSP finding F5 in the plan and is deliberately **not** fixed
here: the plan requires a focused follow-up (a hash/nonce on the shell or a shell without inline
script/style) instead of silently loosening the policy. The server README states the limitation.

## Landing Order & Publication

1. `pm-mcp-clients`: `./mvnw verify -Ppre-commit`, merge of PR
   [#200](https://github.com/plan-marshall/pm-mcp-clients/pull/200), and the snapshot deployment of
   `pm-web-app` on `main`.
2. `plan-marshall-mcp`: the server build resolves `de.planmarshall:pm-web-app:0.1.0-SNAPSHOT` from the
   organisation registry; its own `./mvnw verify -Ppre-commit` and `-Pintegration-tests` gates pass after
   the clients deployment (PR
   [plan-marshall/plan-marshall-mcp#104](https://github.com/plan-marshall/plan-marshall-mcp/pull/104)).

No store, protocol, pairing, session, UI, or TLS behaviour changed.
