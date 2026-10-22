/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.interaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;

import de.planmarshall.api.config.ConfigOrigin;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

@DisplayName("Operator representation and link scheme")
class OperatorStateResourceTest {

    private static final JsonFactory FACTORY = new JsonFactory();

    private static final String STATE_JSON = """
            {
              "scope": {"kind": "plan", "id": "plan-2026-042", "project_id": "p-1", "display_name": "Demo"},
              "state": {"state_id": "remediation.choose", "node_id": "plan.finalize",
                        "kind": "operator_wait", "level": "project"},
              "progress": {"done": 2, "of": 5},
              "title": {"key": "state.title"},
              "texts": [
                {"key": "text.a", "args": {"n": 1}},
                {"verbatim": "provider said x", "source": "provider"}
              ],
              "facts": {"files_realized": 3},
              "proposal": [{"key": "plan_defaults.asking", "value": "always", "origin": "machine"}],
              "items": [
                {"id": "chk-1", "facts": {"result": "fail"},
                 "links": [{"id": "recheck", "rel": "retry", "method": "POST",
                            "href": "/api/v1/machine/onboarding/links/recheck"}]}
              ],
              "revision": 4,
              "links": [
                {"id": "yes", "rel": "next", "method": "POST",
                 "href": "/api/v1/plans/plan-2026-042/links/yes", "preselected": true,
                 "confirm": "confirm.yes"},
                {"id": "waive", "rel": "escalate", "method": "POST",
                 "href": "/api/v1/plans/plan-2026-042/links/waive", "params": {"rationale": "text[1..2000]"}}
              ]
            }
            """;

    @Test
    @DisplayName("the operator link relations are the closed specification catalogue")
    void closedLinkRelationCatalogue() {
        var relations = List.of("next", "alt", "retry", "fix", "loop_back", "submit", "escalate", "consult",
                "abort");
        assertEquals(relations, OperatorLinkRelation.wireValues());
        assertEquals(relations.size(), OperatorLinkRelation.values().length);

        assertEquals(OperatorLinkRelation.ABORT, OperatorLinkRelation.fromWire("abort"));
        assertNull(OperatorLinkRelation.fromWireOrNull("nope"));
        assertThrows(IllegalArgumentException.class, () -> OperatorLinkRelation.fromWire("nope"));
    }

    @Test
    @DisplayName("the scope kinds and text sources are closed catalogues")
    void closedScopeKindAndTextSourceCatalogues() {
        assertEquals(List.of("plan", "epic", "workspace", "machine", "entry"), OperatorScopeKind.wireValues());
        assertEquals(List.of("provider", "harness", "model", "operator", "repository"), TextSource.wireValues());
        assertEquals(OperatorScopeKind.ENTRY, OperatorScopeKind.fromWire("entry"));
        assertEquals(TextSource.HARNESS, TextSource.fromWire("harness"));
        assertNull(OperatorScopeKind.fromWireOrNull("project"));
        assertNull(TextSource.fromWireOrNull("provider_x"));
        assertTrue(OperatorScopeKind.PLAN.requiresIdentifier());
        assertFalse(OperatorScopeKind.MACHINE.requiresIdentifier());
    }

    @Test
    @DisplayName("the operator state round-trips through JSON, the map, and the streaming codec")
    void operatorStateRoundTrip() throws Exception {
        var state = OperatorStateResource.fromJson(STATE_JSON);

        assertEquals(ConfigOrigin.PROJECT, state.level());
        assertEquals(4L, state.revision());
        assertEquals("remediation.choose", state.stateId());
        assertEquals(2, state.links().size());
        assertEquals("yes", state.links().getFirst().id());
        assertTrue(state.links().getFirst().preselected());
        assertEquals("confirm.yes", state.links().getFirst().confirm());
        assertEquals("waive", state.links().get(1).id());
        assertEquals(Map.of("rationale", "text[1..2000]"), state.links().get(1).params());

        assertEquals(state, OperatorStateResource.fromJson(state.toJson()));
        assertEquals(state, OperatorStateResource.fromMap(state.toMap()));
        assertEquals(state, readStreaming(state.toJson()));
        assertEquals(state, writeStreaming(state));

        // the state fields are nested under the state object, the shape the specification describes
        var tree = JsonTree.asObject(JsonTree.parse(state.toJson()));
        var stateObject = JsonTree.object(tree, "state");
        assertEquals("remediation.choose", JsonTree.string(stateObject, "state_id"));

        // a minimal state carries only the required scope, state, and links
        var minimal = OperatorStateResource.fromJson(
                "{\"scope\":{\"kind\":\"machine\"},\"state\":{\"state_id\":\"onboarding\"},\"links\":[]}");
        assertNull(minimal.level());
        assertNull(minimal.progress());
        assertNull(minimal.revision());
        assertTrue(minimal.links().isEmpty());
    }

    @Test
    @DisplayName("an operator link round-trips and refuses an unknown relation or method")
    void operatorLinkRoundTrip() throws Exception {
        var link = new OperatorLink("yes_remember", OperatorLinkRelation.NEXT, OperatorLink.METHOD_POST,
                "/api/v1/machine/onboarding/links/yes_remember", Map.of(), true, "confirm.remember");

        assertEquals(link, OperatorLink.fromJson(link.toJson()));
        assertEquals(link, OperatorLink.fromMap(link.toMap()));
        assertEquals(link, readLinkStreaming(link.toJson()));
        assertEquals(link, writeLinkStreaming(link));

        assertThrows(JsonFormatException.class,
                () -> OperatorLink.fromJson(
                        "{\"id\":\"yes\",\"rel\":\"nope\",\"method\":\"POST\",\"href\":\"/api/v1/x\"}"));
        assertThrows(JsonFormatException.class,
                () -> OperatorLink.fromJson(
                        "{\"id\":\"yes\",\"rel\":\"next\",\"method\":\"FETCH\",\"href\":\"/api/v1/x\"}"));
        assertThrows(JsonFormatException.class,
                () -> OperatorLink.fromJson(
                        "{\"id\":\"Yes\",\"rel\":\"next\",\"method\":\"POST\",\"href\":\"/api/v1/x\"}"));
        assertThrows(JsonFormatException.class,
                () -> OperatorLink.fromJson(
                        "{\"id\":\"yes\",\"rel\":\"next\",\"method\":\"POST\",\"href\":\"/api/v1/x\",\"extra\":1}"));
        assertThrows(JsonFormatException.class,
                () -> OperatorLink.fromJson(
                        "{\"id\":\"yes\",\"rel\":\"next\",\"method\":\"POST\",\"href\":\"/api/v1/x\",\"confirm\":\"nodot\"}"));
        // an external, protocol-relative, or relative target can never leave the local API boundary
        assertThrows(JsonFormatException.class,
                () -> OperatorLink.fromJson(
                        "{\"id\":\"yes\",\"rel\":\"next\",\"method\":\"POST\",\"href\":\"https://evil.example/x\"}"));
        assertThrows(JsonFormatException.class,
                () -> OperatorLink.fromJson(
                        "{\"id\":\"yes\",\"rel\":\"next\",\"method\":\"POST\",\"href\":\"//evil.example/x\"}"));
        // a wrong-typed preselected or params is refused, never coerced or dropped
        assertThrows(JsonFormatException.class,
                () -> OperatorLink.fromJson(
                        "{\"id\":\"yes\",\"rel\":\"next\",\"method\":\"POST\",\"href\":\"/api/v1/x\",\"preselected\":\"yes\"}"));
        assertThrows(JsonFormatException.class,
                () -> OperatorLink.fromJson(
                        "{\"id\":\"yes\",\"rel\":\"next\",\"method\":\"POST\",\"href\":\"/api/v1/x\",\"params\":[]}"));
    }

    @Test
    @DisplayName("a text is either a message key with arguments or verbatim text with a source")
    void operatorTextBothForms() throws Exception {
        var key = OperatorText.ofKey("check.login.missing", Map.of("harness", "claude"));
        var verbatim = OperatorText.ofVerbatim("claude: not logged in", TextSource.HARNESS);

        assertTrue(key.isKey());
        assertFalse(verbatim.isKey());
        assertEquals(key, OperatorText.fromJson(key.toJson()));
        assertEquals(verbatim, OperatorText.fromJson(verbatim.toJson()));
        assertEquals(verbatim, OperatorText.fromMap(verbatim.toMap()));

        assertThrows(JsonFormatException.class, () -> OperatorText.fromJson("{\"key\":\"a.b\",\"verbatim\":\"x\"}"));
        assertThrows(JsonFormatException.class, () -> OperatorText.fromJson("{\"source\":\"provider\"}"));
        assertThrows(JsonFormatException.class,
                () -> OperatorText.fromJson("{\"verbatim\":\"x\",\"source\":\"nope\"}"));
        assertThrows(JsonFormatException.class,
                () -> OperatorText.fromJson("{\"key\":\"a.b\",\"args\":[],\"extra\":1}"));
        assertThrows(IllegalArgumentException.class, () -> OperatorText.ofKey("nodot"));
        assertThrows(IllegalArgumentException.class, () -> new OperatorText(null, Map.of(), null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new OperatorText("a.b", Map.of(), "x", TextSource.MODEL));
    }

    @Test
    @DisplayName("a state refuses an unknown field, a duplicate link id, and an invalid level")
    void stateFailsClosed() {
        assertThrows(JsonFormatException.class,
                () -> OperatorStateResource.fromJson(
                        "{\"scope\":{\"kind\":\"machine\"},\"state\":{\"state_id\":\"onboarding\"},\"links\":[],\"nope\":1}"));
        assertThrows(JsonFormatException.class,
                () -> OperatorStateResource.fromJson(
                        "{\"state\":{\"state_id\":\"onboarding\"},\"links\":[]}"));
        assertThrows(JsonFormatException.class,
                () -> OperatorStateResource.fromJson(
                        "{\"scope\":{\"kind\":\"project\"},\"state\":{\"state_id\":\"onboarding\"},\"links\":[]}"));
        assertThrows(JsonFormatException.class,
                () -> OperatorStateResource.fromJson(
                        "{\"scope\":{\"kind\":\"machine\"},\"state\":{\"state_id\":\"Onboarding\"},\"links\":[]}"));
        assertThrows(JsonFormatException.class,
                () -> OperatorStateResource.fromJson(
                        "{\"scope\":{\"kind\":\"machine\"},\"state\":{\"state_id\":\"onboarding\",\"nope\":1},\"links\":[]}"));
        // done > of is a format error, not a bare IllegalArgumentException
        assertThrows(JsonFormatException.class,
                () -> OperatorStateResource.fromJson(
                        "{\"scope\":{\"kind\":\"machine\"},\"state\":{\"state_id\":\"onboarding\"},"
                                + "\"progress\":{\"done\":4,\"of\":3},\"links\":[]}"));
        // two links of the same id, the item link included, are refused
        assertThrows(IllegalArgumentException.class,
                () -> new OperatorStateResource(
                        new OperatorScope(OperatorScopeKind.MACHINE, null, null, null),
                        "onboarding", null, null, null, null, null,
                        List.of(), Map.of(), List.of(), List.of(), null,
                        List.of(new OperatorLink("yes", OperatorLinkRelation.NEXT, "POST", "/api/v1/x",
                                        Map.of(), false, null),
                                new OperatorLink("yes", OperatorLinkRelation.ALT, "POST", "/api/v1/y",
                                        Map.of(), false, null))));
        // a level a state does not write (the default or a derived one) is refused
        assertThrows(IllegalArgumentException.class,
                () -> new OperatorStateResource(
                        new OperatorScope(OperatorScopeKind.MACHINE, null, null, null),
                        "onboarding", null, null, ConfigOrigin.DEFAULT, null, null,
                        List.of(), Map.of(), List.of(), List.of(), null, List.of()));
    }

    @Test
    @DisplayName("the scope, progress, and proposal round-trip")
    void supportingRecordsRoundTrip() throws Exception {
        var scope = new OperatorScope(OperatorScopeKind.EPIC, "epic-1", "p-1", "Demo");
        var progress = new OperatorProgress(1, 3);
        var proposal = new OperatorProposal("plan_defaults.asking", "always", ConfigOrigin.MACHINE);
        var item = new OperatorItem("chk-1", Map.of("result", "pass"), List.of());

        assertEquals(scope, OperatorScope.fromMap(scope.toMap()));
        assertEquals(progress, OperatorProgress.fromMap(progress.toMap()));
        assertEquals(proposal, OperatorProposal.fromMap(proposal.toMap()));
        assertEquals(item, OperatorItem.fromMap(item.toMap()));

        assertThrows(IllegalArgumentException.class, () -> new OperatorProgress(4, 3));
        assertThrows(IllegalArgumentException.class, () -> new OperatorProgress(1, 0));
        // a plan scope requires an id; the machine scope carries none
        assertThrows(IllegalArgumentException.class,
                () -> new OperatorScope(OperatorScopeKind.PLAN, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new OperatorScope(OperatorScopeKind.MACHINE, "m", null, null));
        assertThrows(JsonFormatException.class,
                () -> OperatorScope.fromMap(JsonTree.asObject(JsonTree.parse("{\"kind\":\"epic\"}"))));
    }

    private static OperatorStateResource readStreaming(String json) throws IOException {
        try (var parser = FACTORY.createParser(json)) {
            return OperatorStateResource.readFrom(parser);
        }
    }

    private static OperatorStateResource writeStreaming(OperatorStateResource state) throws IOException {
        var out = new StringWriter();
        try (var generator = FACTORY.createGenerator(out)) {
            state.writeTo(generator);
        }
        assertFalse(out.toString().isBlank());
        return OperatorStateResource.fromJson(out.toString());
    }

    private static OperatorLink readLinkStreaming(String json) throws IOException {
        try (var parser = FACTORY.createParser(json)) {
            return OperatorLink.readFrom(parser);
        }
    }

    private static OperatorLink writeLinkStreaming(OperatorLink link) throws IOException {
        var out = new StringWriter();
        try (var generator = FACTORY.createGenerator(out)) {
            link.writeTo(generator);
        }
        return OperatorLink.fromJson(out.toString());
    }
}
