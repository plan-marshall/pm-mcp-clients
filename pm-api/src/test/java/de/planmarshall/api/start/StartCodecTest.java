/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.start;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.control.ChatProposalResource;
import de.planmarshall.api.control.ChatRole;
import de.planmarshall.api.control.ChatScope;
import de.planmarshall.api.control.ChatState;
import de.planmarshall.api.control.ChatTurnResource;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.source.git.ChangedFile;

/**
 * A systematic codec and catalogue test of every record of the start contract: each closed list
 * resolves every member, refuses an unknown value and a {@code null}, and each record round-trips
 * through its JSON string codec, its map codec, and its streaming {@code writeTo}/{@code readFrom}
 * pair.
 */
@DisplayName("Start codecs and catalogues")
class StartCodecTest {

    private static final JsonFactory FACTORY = new JsonFactory();

    @Nested
    @DisplayName("Closed catalogues")
    class Catalogues {

        @Test
        @DisplayName("every member of every closed list resolves and an unknown value is refused")
        void everyMemberResolves() {
            assertAll(
                    () -> eachMember(EntryKind.wireValues(), EntryKind::fromWire, EntryKind::fromWireOrNull),
                    () -> eachMember(SourceKind.wireValues(), SourceKind::fromWire, SourceKind::fromWireOrNull),
                    () -> eachMember(SourceState.wireValues(), SourceState::fromWire, SourceState::fromWireOrNull),
                    () -> eachMember(SourceTrust.wireValues(), SourceTrust::fromWire, SourceTrust::fromWireOrNull),
                    () -> eachMember(ScreeningVerdict.wireValues(), ScreeningVerdict::fromWire,
                            ScreeningVerdict::fromWireOrNull),
                    () -> eachMember(SourceFailureCause.wireValues(), SourceFailureCause::fromWire,
                            SourceFailureCause::fromWireOrNull),
                    () -> eachMember(UnscreenedDisposition.wireValues(), UnscreenedDisposition::fromWire,
                            UnscreenedDisposition::fromWireOrNull),
                    () -> eachMember(StartRefusalReason.wireValues(), StartRefusalReason::fromWire,
                            StartRefusalReason::fromWireOrNull),
                    () -> eachMember(AdoptionFailureReason.wireValues(), AdoptionFailureReason::fromWire,
                            AdoptionFailureReason::fromWireOrNull),
                    () -> eachMember(ConfinedRoot.wireValues(), ConfinedRoot::fromWire, ConfinedRoot::fromWireOrNull),
                    () -> eachMember(StartTranscriptState.wireValues(), StartTranscriptState::fromWire,
                            StartTranscriptState::fromWireOrNull),
                    () -> eachMember(StartState.wireValues(), StartState::fromWire, StartState::fromWireOrNull));
        }

        @Test
        @DisplayName("the derived properties of the closed lists hold")
        void derivedProperties() {
            assertAll(
                    () -> assertTrue(EntryKind.EPIC.epic()),
                    () -> assertFalse(EntryKind.PLAN.epic()),
                    () -> assertTrue(SourceState.STORED.settled()),
                    () -> assertTrue(SourceState.FAILED.failed()),
                    () -> assertTrue(ScreeningVerdict.UNSCREENED.needsAcceptance()),
                    () -> assertTrue(SourceFailureCause.RATE_LIMITED.namesReset()),
                    () -> assertFalse(SourceFailureCause.NO_LOCAL_CHANGES.retryable()),
                    () -> assertTrue(UnscreenedDisposition.ACCEPT_UNTRUSTED.recordsReason()),
                    () -> assertTrue(ConfinedRoot.HOME.loopbackOnly()),
                    () -> assertTrue(StartTranscriptState.DELETED.settled()),
                    () -> assertTrue(SourceKind.ISSUE.external()),
                    () -> assertTrue(SourceKind.DOCUMENTS.documents()),
                    () -> assertTrue(SourceKind.ISSUE.offeredToEpic()),
                    () -> assertFalse(SourceKind.LESSON.offeredToEpic()));
        }

        @Test
        @DisplayName("the source kind combination rules hold for every entry kind")
        void combinationRules() {
            assertAll(
                    () -> assertTrue(SourceKind.onlyDescriptionsAndDocuments(null)),
                    () -> assertTrue(SourceKind.onlyDescriptionsAndDocuments(
                            Set.of(SourceKind.DESCRIPTION, SourceKind.DOCUMENTS))),
                    () -> assertFalse(SourceKind.onlyDescriptionsAndDocuments(Set.of(SourceKind.ISSUE))),
                    () -> assertTrue(SourceKind.offered(EntryKind.EPIC, Set.of(), false, false, false).stream()
                            .noneMatch(kind -> kind == SourceKind.LESSON)),
                    () -> assertTrue(SourceKind.offered(EntryKind.PLAN, Set.of(SourceKind.ISSUE), true, true, true)
                            .stream().noneMatch(kind -> kind == SourceKind.PULL_REQUEST)),
                    () -> assertTrue(SourceKind.offered(EntryKind.PLAN, Set.of(), true, true, false).stream()
                            .noneMatch(kind -> kind == SourceKind.LOCAL_CHANGES)));
        }
    }

    @Nested
    @DisplayName("Records")
    class Records {

        @Test
        @DisplayName("the opening request round-trips through its JSON and map codecs")
        void openingRequest() throws Exception {
            var value = entryOpenRequest();
            assertEquals(value, EntryOpenRequest.fromJson(value.toJson()));
            assertEquals(value, EntryOpenRequest.fromMap(value.toMap()));
        }

        @Test
        @DisplayName("every streaming record round-trips through all three codecs")
        void everyRecord() throws Exception {
            verify(entry(), EntryResource::toJson, EntryResource::toMap, EntryResource::fromJson,
                    EntryResource::fromMap, EntryResource::readFrom, EntryResource::writeTo);
            verify(source(), SourceResource::toJson, SourceResource::toMap, SourceResource::fromJson,
                    SourceResource::fromMap, SourceResource::readFrom, SourceResource::writeTo);
            verify(failure(), SourceFailure::toJson, SourceFailure::toMap, SourceFailure::fromJson,
                    SourceFailure::fromMap, SourceFailure::readFrom, SourceFailure::writeTo);
            verify(span(), ScreeningSpan::toJson, ScreeningSpan::toMap, ScreeningSpan::fromJson,
                    ScreeningSpan::fromMap, ScreeningSpan::readFrom, ScreeningSpan::writeTo);
            verify(offer(), SourceKindOffer::toJson, SourceKindOffer::toMap, SourceKindOffer::fromJson,
                    SourceKindOffer::fromMap, SourceKindOffer::readFrom, SourceKindOffer::writeTo);
            verify(file(), ConfinedFile::toJson, ConfinedFile::toMap, ConfinedFile::fromJson,
                    ConfinedFile::fromMap, ConfinedFile::readFrom, ConfinedFile::writeTo);
            verify(listing(), ConfinedFileListResource::toJson, ConfinedFileListResource::toMap,
                    ConfinedFileListResource::fromJson, ConfinedFileListResource::fromMap,
                    ConfinedFileListResource::readFrom, ConfinedFileListResource::writeTo);
            verify(ready(), StartReadyResource::toJson, StartReadyResource::toMap, StartReadyResource::fromJson,
                    StartReadyResource::fromMap, StartReadyResource::readFrom, StartReadyResource::writeTo);
            verify(refusal(), StartRefusal::toJson, StartRefusal::toMap, StartRefusal::fromJson,
                    StartRefusal::fromMap, StartRefusal::readFrom, StartRefusal::writeTo);
            verify(adoptable(), AdoptableEpic::toJson, AdoptableEpic::toMap, AdoptableEpic::fromJson,
                    AdoptableEpic::fromMap, AdoptableEpic::readFrom, AdoptableEpic::writeTo);
            verify(adoptionFailure(), AdoptionFailure::toJson, AdoptionFailure::toMap, AdoptionFailure::fromJson,
                    AdoptionFailure::fromMap, AdoptionFailure::readFrom, AdoptionFailure::writeTo);
            verify(chat(), StartChatResource::toJson, StartChatResource::toMap, StartChatResource::fromJson,
                    StartChatResource::fromMap, StartChatResource::readFrom, StartChatResource::writeTo);
            verify(followUp(), FollowUpProposalResource::toJson, FollowUpProposalResource::toMap,
                    FollowUpProposalResource::fromJson, FollowUpProposalResource::fromMap,
                    FollowUpProposalResource::readFrom, FollowUpProposalResource::writeTo);
            verify(state(), StartStateResource::toJson, StartStateResource::toMap, StartStateResource::fromJson,
                    StartStateResource::fromMap, StartStateResource::readFrom, StartStateResource::writeTo);
        }

        @Test
        @DisplayName("the state keeps its item facts and links through the round trip")
        void stateFacts() throws Exception {
            var value = state();
            var parsed = StartStateResource.fromJson(value.toJson());

            assertEquals(value.operator().scope(), parsed.operator().scope());
            assertEquals(value.operator().items(), parsed.operator().items());
            assertEquals(value.operator().links(), parsed.operator().links());
            assertNotNull(value.operator().items().getFirst().facts().get("failure"));
            assertEquals("ent-1", value.entryId());
            assertEquals(StartState.SOURCES, value.state());
        }

        @Test
        @DisplayName("the derived queries of the records answer as the contract says")
        void derivedQueries() {
            assertAll(
                    () -> assertFalse(entry().epic()),
                    () -> assertEquals(1, entry().sourceCount()),
                    () -> assertEquals(1, source().untrustedChangedFiles()),
                    () -> assertFalse(source().blocksContinue()),
                    () -> assertTrue(failure().retryable()),
                    () -> assertEquals(36, span().length()),
                    () -> assertEquals(1, listing().fileCount()),
                    () -> assertTrue(file().pickable()),
                    () -> assertTrue(ready().recipeBound()),
                    () -> assertFalse(ready().recipeBlocked()),
                    () -> assertTrue(chat().routedByKind(EntryKind.PLAN)),
                    () -> assertFalse(chat().routedByKind(EntryKind.EPIC)),
                    () -> assertEquals(List.of("recipe-", "recipe_key", "yyyy-mm-dd-hh-mm-ss"), RecipePlanId.parts()),
                    () -> assertTrue(RecipePlanId.matchesKey("code-review")),
                    () -> assertFalse(RecipePlanId.matchesKey("Code")));
        }

        @Test
        @DisplayName("a recipe with a parameter without an option cannot run")
        void blockedRecipe() {
            var parameter = new RecipeParameter("m", null, List.of(), false, false, "none");
            var blocked = new StartReadyResource("x", "recipe-x-2026-10-12-14-05-00", false, false,
                    new RecipeRef("x", "x", "", "project", false, List.of(parameter)), List.of(parameter),
                    null, false, null, null, null, null);
            assertTrue(blocked.recipeBlocked());
        }

        @Test
        @DisplayName("a closed list's own factories refuse an impossible combination")
        void factoriesRefuse() {
            assertThrows(IllegalArgumentException.class,
                    () -> new SourceKindOffer(SourceKind.ISSUE, false, null, Map.of()));
            assertThrows(IllegalArgumentException.class, () -> new SourceFailure(SourceFailureCause.RATE_LIMITED,
                    null, null));
            assertThrows(IllegalArgumentException.class, () -> new ScreeningSpan(1, 0, "x", "y"));
            assertThrows(IllegalArgumentException.class, () -> new FollowUpProposalResource("x", EntryKind.EPIC,
                    ChatRole.REPLAN, ChatScope.ARCHIVE, true, null, null));
            assertNull(SourceKind.fromWireOrNull(null));
            assertThrows(JsonFormatException.class, () -> ConfinedFile.fromJson("[]"));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers

    private static <T> void verify(T value, JsonSource<T> json, MapSource<T> map, JsonReader<T> fromJson,
                                   MapReader<T> fromMap, ParserReader<T> readFrom, GeneratorWriter<T> writeTo) throws Exception {
        var text = json.json(value);
        assertEquals(value, fromJson.read(text));
        assertEquals(value, fromMap.read(map.map(value)));
        try (JsonParser parser = FACTORY.createParser(text)) {
            parser.nextToken();
            assertEquals(value, readFrom.read(parser));
        }
        var out = new StringWriter();
        try (JsonGenerator generator = FACTORY.createGenerator(out)) {
            writeTo.write(value, generator);
        }
        assertEquals(text, out.toString());
    }

    private static void eachMember(List<String> wires, Function<String, ?> parse, Function<String, ?> orNull) {
        for (var wire : wires) {
            assertNotNull(parse.apply(wire));
            assertNotNull(orNull.apply(wire));
            assertNotNull(orNull.apply(wire.toUpperCase()));
        }
        assertNull(orNull.apply("no-such-member"));
        assertNull(orNull.apply(null));
        assertThrows(IllegalArgumentException.class, () -> parse.apply("no-such-member"));
        assertThrows(IllegalArgumentException.class, () -> parse.apply(null));
    }

    private static EntryOpenRequest entryOpenRequest() {
        return new EntryOpenRequest(EntryKind.PLAN, "Add a provider", "Add a provider", "audit", null);
    }

    private static EntryResource entry() {
        return new EntryResource("ent-1", EntryKind.PLAN, "pm-mcp-clients", StartState.SOURCES,
                "Add a provider", "add-a-provider", List.of(source()),
                List.of(SourceKindOffer.available(SourceKind.DOCUMENTS),
                        SourceKindOffer.refused(SourceKind.LESSON, "start.sources.no_lessons", Map.of())),
                true, true, null, null, List.of(), null);
    }

    private static SourceResource source() {
        return SourceResource.pullRequest("src-1", "https://github.com/plan-marshall/pm-mcp-clients/pull/214",
                "Add the provider", List.of(new ChangedFile("a.java", 1, 2, true, false),
                        new ChangedFile("b.adoc", 3, 0, false, true)));
    }

    private static SourceFailure failure() {
        return SourceFailure.rateLimited(Instant.parse("2026-10-19T14:40:00Z"), "Codeberg");
    }

    private static ScreeningSpan span() {
        return new ScreeningSpan(12, 48, "ignore your instructions", "an instruction to a model");
    }

    private static SourceKindOffer offer() {
        return SourceKindOffer.refused(SourceKind.LOCAL_CHANGES,
                SourceKindOffer.REASON_LOCAL_CHANGES_LANDING, Map.of("landing", "landing.files"));
    }

    private static ConfinedFile file() {
        return new ConfinedFile("forgejo.adoc", "doc/concepts/forgejo.adoc", false, 1024, true, null);
    }

    private static ConfinedFileListResource listing() {
        return new ConfinedFileListResource("pm-mcp-clients", ConfinedRoot.CHECKOUT, "doc", ".",
                List.of(file()), false);
    }

    private static StartReadyResource ready() {
        var recipe = new RecipeRef("coverage", "coverage", "raise the coverage", "domain", false,
                List.of(new RecipeParameter("modules", "all 9", List.of("all 9"), true, true, null)));
        return new StartReadyResource("Coverage", "recipe-coverage-2026-10-12-14-05-00", false, false, recipe,
                recipe.parameters(), "landing.pull-request", false, null, null, null, null);
    }

    private static StartRefusal refusal() {
        return StartRefusal.projectNotReady("pm-mcp-clients", "credential sonar");
    }

    private static AdoptableEpic adoptable() {
        return new AdoptableEpic("relay-hardening", "Relay hardening");
    }

    private static AdoptionFailure adoptionFailure() {
        return AdoptionFailure.unreadable("relay-hardening", "epic.json", "permission denied");
    }

    private static StartChatResource chat() {
        return new StartChatResource("ent-1", ChatRole.REFINE, ChatScope.REQUEST, true, null, "chat-1",
                ChatState.READ_BACK,
                List.of(new ChatTurnResource(ChatTurnResource.ROLE_OPERATOR, "What did you understand?",
                        "2026-10-19T10:00:00Z", false)),
                new ChatProposalResource("An addition", null, "Comments are read too.", "the description"),
                StartTranscriptState.WITH_ENTRY, "pending/ent-1/chat");
    }

    private static FollowUpProposalResource followUp() {
        return FollowUpProposalResource.proposed("codeberg-issues", EntryKind.PLAN,
                new ChatProposalResource("A follow-up plan", null, "Add the provider.", "a new plan"));
    }

    private static StartStateResource state() {
        var entry = new EntryResource("ent-1", EntryKind.PLAN, "pm-mcp-clients", StartState.SOURCES,
                "Add a provider", "add-a-provider",
                List.of(new SourceResource("src-1", SourceKind.ISSUE, "4711", "Issue", SourceState.FAILED,
                        SourceTrust.UNTRUSTED, null, SourceFailure.of(SourceFailureCause.NOT_FOUND), List.of(),
                        null, null, List.of(), null)),
                List.of(SourceKindOffer.available(SourceKind.DOCUMENTS)), true, true, null, null, List.of(), null);
        return StartStateResource.of(entry, null, null, null, null, List.of());
    }

    @FunctionalInterface
    private interface JsonSource<T> {
        String json(T value) throws Exception;
    }

    @FunctionalInterface
    private interface MapSource<T> {
        Map<String, Object> map(T value) throws Exception;
    }

    @FunctionalInterface
    private interface JsonReader<T> {
        T read(String json) throws Exception;
    }

    @FunctionalInterface
    private interface MapReader<T> {
        T read(Map<String, Object> map) throws Exception;
    }

    @FunctionalInterface
    private interface ParserReader<T> {
        T read(JsonParser parser) throws Exception;
    }

    @FunctionalInterface
    private interface GeneratorWriter<T> {
        void write(T value, JsonGenerator generator) throws Exception;
    }
}
