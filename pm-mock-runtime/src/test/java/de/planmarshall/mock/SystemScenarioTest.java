/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.system.CleanupJob;
import de.planmarshall.api.system.CleanupJobState;
import de.planmarshall.api.system.RuntimeLifecycleState;
import de.planmarshall.api.system.SystemSnapshot;

@DisplayName("SystemScenario")
@EnabledOnOs({OS.MAC, OS.LINUX})
class SystemScenarioTest {

    @Test
    @DisplayName("loads system scenario and returns full system snapshot with default hour window")
    void shouldLoadScenarioAndReturnSystemSnapshot() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.SYSTEM)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            assertEquals(StandardScenarios.REPORTS_AND_SYSTEM, runtime.currentScenarioName());
            assertEquals("system.running", runtime.currentStateName());

            var res = client.send(HttpRequest.get("/api/v1/system").withHeader("Authorization", auth));
            assertEquals(200, res.status());

            var snapshot = SystemSnapshot.fromJson(res.bodyText());
            assertNotNull(snapshot.runtime());
            assertEquals("0.9.1", snapshot.runtime().version());
            assertEquals(4.0, snapshot.runtime().cpuPercent());
            assertEquals("127.0.0.1:7420", snapshot.runtime().loopbackListener());
            assertTrue(snapshot.runtime().networkListener().contains("192.168.1.20:7421"));

            assertNotNull(snapshot.work());
            assertEquals(3, snapshot.work().plansByState().get("running"));
            assertEquals(4, snapshot.work().tasksRunning());

            assertEquals(5, snapshot.workers().size());
            assertEquals("planner", snapshot.workers().get(0).role());

            assertNotNull(snapshot.jobs());
            assertEquals(3, JsonTree.integer(snapshot.jobs(), "locks_held"));

            assertEquals(3, snapshot.models().size());
            var opus = snapshot.models().get(0);
            assertEquals("claude:opus-4.5", opus.harnessAndModel());
            assertEquals(42, opus.callsCount());
            assertEquals(2020000L, opus.tokensTotal());

            assertNotNull(snapshot.cleanup());
            assertEquals(12, snapshot.cleanup().plansCount());
        }
    }

    @Test
    @DisplayName("filters system snapshot by window and project query parameters")
    void shouldFilterSystemSnapshotByWindowAndProject() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.SYSTEM)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var resToday = client.send(HttpRequest.get("/api/v1/system?window=today&project=pm-mcp-clients").withHeader("Authorization", auth));
            assertEquals(200, resToday.status());

            var snapToday = SystemSnapshot.fromJson(resToday.bodyText());
            assertEquals(1, snapToday.work().activeProjectsCount());
            assertEquals(2, snapToday.work().tasksRunning());

            var opusToday = snapToday.models().get(0);
            assertEquals(164, opusToday.callsCount());
            assertEquals(7890000L, opusToday.tokensTotal());

            var res7d = client.send(HttpRequest.get("/api/v1/system?window=7d").withHeader("Authorization", auth));
            assertEquals(200, res7d.status());
            var snap7d = SystemSnapshot.fromJson(res7d.bodyText());
            var opus7d = snap7d.models().get(0);
            assertEquals(980, opus7d.callsCount());
            assertEquals(48200000L, opus7d.tokensTotal());
        }
    }

    @Test
    @DisplayName("serves release notes for known and unknown versions")
    void shouldServeReleaseNotes() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.SYSTEM)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            var res = client.send(HttpRequest.get("/api/v1/system/release-notes?version=0.10.0").withHeader("Authorization", auth));
            assertEquals(200, res.status());
            assertTrue(res.bodyText().contains("pm-mcpd 0.10.0 Release Notes"));

            var res404 = client.send(HttpRequest.get("/api/v1/system/release-notes?version=99.0.0").withHeader("Authorization", auth));
            assertEquals(404, res404.status());
        }
    }

    @Test
    @DisplayName("manages runtime lifecycle: stop drain, withdraw, stop now, upgrade")
    void shouldManageRuntimeLifecycle() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.SYSTEM)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            // 1. Drain stop
            var drainBody = JsonTree.write(Map.of("when", "drain", "then", "exit"));
            var resDrain = client.send(HttpRequest.post("/api/v1/operator/runtime/stop").withHeader("Authorization", auth).withBody(drainBody));
            assertEquals(200, resDrain.status());

            var resStatus = client.send(HttpRequest.get("/api/v1/status").withHeader("Authorization", auth));
            var statusMap = JsonTree.asObject(JsonTree.parse(resStatus.bodyText()));
            assertEquals("draining", statusMap.get("lifecycle_state"));
            assertEquals("the console", statusMap.get("draining_by"));
            assertEquals(true, statusMap.get("can_withdraw"));

            // 2. Withdraw drain
            var resWithdraw = client.send(HttpRequest.delete("/api/v1/operator/runtime/stop").withHeader("Authorization", auth));
            assertEquals(200, resWithdraw.status());

            var resStatusRunning = client.send(HttpRequest.get("/api/v1/status").withHeader("Authorization", auth));
            var statusRunning = JsonTree.asObject(JsonTree.parse(resStatusRunning.bodyText()));
            assertEquals("running", statusRunning.get("lifecycle_state"));

            // 3. Second withdraw fails with 409
            var resWithdrawFail = client.send(HttpRequest.delete("/api/v1/operator/runtime/stop").withHeader("Authorization", auth));
            assertEquals(409, resWithdrawFail.status());

            // 4. Stop now
            var stopNowBody = JsonTree.write(Map.of("when", "now", "then", "restart"));
            var resStopNow = client.send(HttpRequest.post("/api/v1/operator/runtime/stop").withHeader("Authorization", auth).withBody(stopNowBody));
            assertEquals(200, resStopNow.status());

            // 5. Upgrade
            runtime.engine().reset();
            var upgradeBody = JsonTree.write(Map.of("version", "0.10.0"));
            var resUpgrade = client.send(HttpRequest.post("/api/v1/operator/runtime/upgrade").withHeader("Authorization", auth).withBody(upgradeBody));
            assertEquals(200, resUpgrade.status());
            assertEquals(RuntimeLifecycleState.UPGRADING, runtime.engine().lifecycleState());

            var resWithdrawUpgrade = client.send(HttpRequest.delete("/api/v1/operator/runtime/upgrade").withHeader("Authorization", auth));
            assertEquals(200, resWithdrawUpgrade.status());
            assertEquals(RuntimeLifecycleState.RUNNING, runtime.engine().lifecycleState());

            // 6. Upgrade check
            var resCheck = client.send(HttpRequest.post("/api/v1/operator/runtime/upgrade/check").withHeader("Authorization", auth));
            assertEquals(200, resCheck.status());
            var checkMap = JsonTree.asObject(JsonTree.parse(resCheck.bodyText()));
            assertEquals("0.10.0", checkMap.get("version"));
            assertEquals(true, checkMap.get("available"));
        }
    }

    @Test
    @DisplayName("manages cleanup job: get open job, postpone, ignore, delete, and kept items")
    void shouldManageCleanupJob() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.SYSTEM)) {
            var client = new UnixSocketHttpClient(runtime.socketPath());
            var auth = "Bearer " + runtime.token();

            // 1. Get open cleanup job
            var resJob = client.send(HttpRequest.get("/api/v1/system/cleanup").withHeader("Authorization", auth));
            assertEquals(200, resJob.status());
            var job = CleanupJob.fromJson(resJob.bodyText());
            assertEquals("cln-0012", job.jobId());
            assertEquals(CleanupJobState.OPEN, job.state());
            assertEquals(3, job.projectItems().size());

            // 2. Kept items list and update
            var resKept = client.send(HttpRequest.get("/api/v1/system/cleanup/kept").withHeader("Authorization", auth));
            assertEquals(200, resKept.status());
            var keptList = JsonTree.asArray(JsonTree.parse(resKept.bodyText()));
            assertEquals(4, keptList.size());

            var putKeptBody = JsonTree.write(Map.of("item_ids", List.of("token-usage", "relay-auth")));
            var resPutKept = client.send(HttpRequest.put("/api/v1/system/cleanup/kept").withHeader("Authorization", auth).withBody(putKeptBody));
            assertEquals(200, resPutKept.status());
            var putKeptList = JsonTree.asArray(JsonTree.parse(resPutKept.bodyText()));
            assertEquals(2, putKeptList.size());

            // 3. Remind cleanup job
            var resRemind = client.send(HttpRequest.post("/api/v1/system/cleanup/cln-0012/remind").withHeader("Authorization", auth));
            assertEquals(200, resRemind.status());
            var remindMap = JsonTree.asObject(JsonTree.parse(resRemind.bodyText()));
            assertEquals("postponed", remindMap.get("status"));
            assertEquals(3, JsonTree.integer(remindMap, "remind_after_days"));

            // 4. Ignore cleanup job after reset
            runtime.engine().reset();
            var resIgnore = client.send(HttpRequest.post("/api/v1/system/cleanup/cln-0012/ignore").withHeader("Authorization", auth));
            assertEquals(200, resIgnore.status());
            var ignoreMap = JsonTree.asObject(JsonTree.parse(resIgnore.bodyText()));
            assertEquals("done", ignoreMap.get("status"));
            assertEquals("ignore", ignoreMap.get("decision"));

            // 5. Delete cleanup job after reset
            runtime.engine().reset();
            var deleteBody = JsonTree.write(Map.of("unchecked_ids", List.of("token-usage")));
            var resDelete = client.send(HttpRequest.post("/api/v1/system/cleanup/cln-0012/delete").withHeader("Authorization", auth).withBody(deleteBody));
            assertEquals(200, resDelete.status());
            var deleteMap = JsonTree.asObject(JsonTree.parse(resDelete.bodyText()));
            assertEquals("deleted", deleteMap.get("status"));
            assertEquals(1872127584L, JsonTree.number(deleteMap, "freed_bytes").longValue());

            // Cleanup job is now done, so GET /api/v1/system/cleanup returns open=false
            var resNoJob = client.send(HttpRequest.get("/api/v1/system/cleanup").withHeader("Authorization", auth));
            assertEquals(200, resNoJob.status());
            var noJobMap = JsonTree.asObject(JsonTree.parse(resNoJob.bodyText()));
            assertNotEquals(Boolean.TRUE, noJobMap.get("open"));
            assertNotNull(noJobMap.get("next_check_at"));
            assertNotNull(noJobMap.get("durations"));
            assertNotNull(noJobMap.get("residue"));

            // 6. Delete refused while stopping
            runtime.engine().reset();
            client.send(HttpRequest.post("/api/v1/operator/runtime/stop").withHeader("Authorization", auth));
            var resRefused = client.send(HttpRequest.post("/api/v1/system/cleanup/cln-0012/delete").withHeader("Authorization", auth));
            assertEquals(409, resRefused.status());
        }
    }
}
