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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.source.git.ChangedFile;

/**
 * One source of a start as the client shows it (State Catalogue, Source Units; design <em>Starting
 * Work</em>, the list of sources): its kind, its reference, its state, its trust, the screener's
 * verdict, and, where it applies, its failure, the flagged spans, the disposition the operator chose,
 * and the changed files of a pull request.
 * <p>
 * A source is never changed once stored. Three invariants are enforced on construction and on the
 * wire:
 * <ul>
 *   <li>a source accepted while unscreened keeps the {@link SourceTrust#UNTRUSTED} mark, and an
 *       accepted flagged source does too;</li>
 *   <li>a pull request's changed files are untrusted unless the operator authored them, so a file
 *       that is neither untrusted nor operator-authored is refused;</li>
 *   <li>only a failed source carries a failure and only a flagged or unscreened source carries a
 *       verdict that needs acceptance.</li>
 * </ul>
 *
 * @param sourceId          the source's identifier within its entry
 * @param kind              the kind of the source
 * @param reference         what the source is (a description's first line, a path, an issue URL, a
 *                          lesson id)
 * @param title             the resolved title, or {@code null} while unresolved
 * @param state             the state of the source
 * @param trust             the trust of the source
 * @param verdict           the screener's verdict, or {@code null} for a source of this machine
 * @param failure           the failure of a failed source, or the cause of a screening verdict
 * @param spans             the spans the screener flagged, empty unless flagged
 * @param disposition       the disposition of a flagged or unscreened source, or {@code null}
 * @param dispositionReason the reason recorded with the disposition, or {@code null}
 * @param changedFiles      the changed files of a pull request, empty for every other kind
 * @param digest            the digest of the stored source, or {@code null} while not stored
 */
public record SourceResource(
        String sourceId,
        SourceKind kind,
        String reference,
        @Nullable String title,
        SourceState state,
        SourceTrust trust,
        @Nullable ScreeningVerdict verdict,
        @Nullable SourceFailure failure,
        List<ScreeningSpan> spans,
        @Nullable UnscreenedDisposition disposition,
        @Nullable String dispositionReason,
        List<ChangedFile> changedFiles,
        @Nullable String digest
) {

    public static final String FIELD_SOURCE_ID = "source_id";
    public static final String FIELD_KIND = "kind";
    public static final String FIELD_REFERENCE = "reference";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_STATE = "state";
    public static final String FIELD_TRUST = "trust";
    public static final String FIELD_VERDICT = "verdict";
    public static final String FIELD_FAILURE = "failure";
    public static final String FIELD_SPANS = "spans";
    public static final String FIELD_DISPOSITION = "disposition";
    public static final String FIELD_DISPOSITION_REASON = "disposition_reason";
    public static final String FIELD_CHANGED_FILES = "changed_files";
    public static final String FIELD_DIGEST = "digest";

    private static final Set<String> KNOWN_FIELDS = Set.of(
            FIELD_SOURCE_ID, FIELD_KIND, FIELD_REFERENCE, FIELD_TITLE, FIELD_STATE, FIELD_TRUST,
            FIELD_VERDICT, FIELD_FAILURE, FIELD_SPANS, FIELD_DISPOSITION, FIELD_DISPOSITION_REASON,
            FIELD_CHANGED_FILES, FIELD_DIGEST);

    public SourceResource {
        Objects.requireNonNull(sourceId, FIELD_SOURCE_ID);
        Objects.requireNonNull(kind, FIELD_KIND);
        Objects.requireNonNull(reference, FIELD_REFERENCE);
        Objects.requireNonNull(state, FIELD_STATE);
        Objects.requireNonNull(trust, FIELD_TRUST);
        spans = spans == null ? List.of() : List.copyOf(spans);
        changedFiles = changedFiles == null ? List.of() : List.copyOf(changedFiles);
        requireFailureFitsState(state, verdict, failure);
        requireVerdictFitsState(state, verdict, spans);
        requireDispositionRecordsItsReason(disposition, dispositionReason);
        if (disposition != null && disposition.recordsReason() && trust != SourceTrust.UNTRUSTED) {
            throw new IllegalArgumentException("A source accepted while unscreened keeps the untrusted mark");
        }
        if (verdict != null && verdict.needsAcceptance() && trust != SourceTrust.UNTRUSTED) {
            throw new IllegalArgumentException(
                    "A flagged or unscreened source keeps the untrusted mark: " + verdict.wireValue());
        }
        if (kind.external() && disposition == null && verdict == null
                && (state == SourceState.STORED || state == SourceState.FLAGGED || state == SourceState.UNSCREENED)) {
            throw new IllegalArgumentException("An external source stored without acceptance names its verdict");
        }
        for (var file : changedFiles) {
            if (!file.untrusted() && !file.operatorAuthored()) {
                throw new IllegalArgumentException(
                        "A pull request's changed file is untrusted unless the operator authored it: " + file.path());
            }
        }
    }

    private static void requireFailureFitsState(SourceState state, @Nullable ScreeningVerdict verdict,
                                                @Nullable SourceFailure failure) {
        if (failure != null && !state.failed() && verdict == null) {
            throw new IllegalArgumentException(
                    "Only a failed source or a screening verdict carries a failure: " + state.wireValue());
        }
        if (failure != null && verdict == ScreeningVerdict.CLEAN) {
            throw new IllegalArgumentException("A clean source carries no cause");
        }
        if (state.failed() && failure == null) {
            throw new IllegalArgumentException("A failed source names its cause");
        }
    }

    private static void requireVerdictFitsState(SourceState state, @Nullable ScreeningVerdict verdict,
                                                List<ScreeningSpan> spans) {
        if (!spans.isEmpty() && verdict != ScreeningVerdict.FLAGGED) {
            throw new IllegalArgumentException("Only a flagged source carries flagged spans");
        }
        if (verdict == ScreeningVerdict.FLAGGED && state != SourceState.FLAGGED
                && state != SourceState.STORED && state != SourceState.COPIED && state != SourceState.MOVED) {
            throw new IllegalArgumentException(
                    "A flagged source is flagged, stored, or accepted: " + state.wireValue());
        }
    }

    private static void requireDispositionRecordsItsReason(@Nullable UnscreenedDisposition disposition,
                                                           @Nullable String dispositionReason) {
        if (disposition != null && disposition.recordsReason()
                && (dispositionReason == null || dispositionReason.isBlank())) {
            throw new IllegalArgumentException("accept_untrusted records the operator's reason");
        }
    }

    /**
     * @param sourceId  the source's identifier
     * @param kind      the kind
     * @param reference the reference
     * @return a trusted source of this machine, stored
     */
    public static SourceResource local(String sourceId, SourceKind kind, String reference) {
        return new SourceResource(sourceId, kind, reference, null, SourceState.STORED, SourceTrust.TRUSTED,
                null, null, List.of(), null, null, List.of(), null);
    }

    /**
     * @param sourceId  the source's identifier
     * @param reference the pull request's URL
     * @param title     the pull request's title
     * @param files     the changed files, each untrusted unless the operator authored it
     * @return a stored pull request source
     */
    public static SourceResource pullRequest(String sourceId, String reference, @Nullable String title,
                                             List<ChangedFile> files) {
        return new SourceResource(sourceId, SourceKind.PULL_REQUEST, reference, title, SourceState.STORED,
                SourceTrust.UNTRUSTED, ScreeningVerdict.CLEAN, null, List.of(), null, null, files, null);
    }

    /**
     * @return the number of changed files that count as untrusted
     */
    public int untrustedChangedFiles() {
        return (int) changedFiles.stream().filter(ChangedFile::untrusted).count();
    }

    /**
     * @return {@code true} if the source still blocks <em>Continue</em>
     */
    public boolean blocksContinue() {
        return !state.settled() && state != SourceState.FLAGGED && state != SourceState.UNSCREENED
                && disposition == null;
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_SOURCE_ID, sourceId);
        map.put(FIELD_KIND, kind.wireValue());
        map.put(FIELD_REFERENCE, reference);
        if (title != null) {
            map.put(FIELD_TITLE, title);
        }
        map.put(FIELD_STATE, state.wireValue());
        map.put(FIELD_TRUST, trust.wireValue());
        if (verdict != null) {
            map.put(FIELD_VERDICT, verdict.wireValue());
        }
        if (failure != null) {
            map.put(FIELD_FAILURE, failure.toMap());
        }
        if (!spans.isEmpty()) {
            map.put(FIELD_SPANS, spans.stream().map(ScreeningSpan::toMap).toList());
        }
        if (disposition != null) {
            map.put(FIELD_DISPOSITION, disposition.wireValue());
        }
        if (dispositionReason != null) {
            map.put(FIELD_DISPOSITION_REASON, dispositionReason);
        }
        if (!changedFiles.isEmpty()) {
            map.put(FIELD_CHANGED_FILES, changedFiles.stream().map(ChangedFile::toMap).toList());
        }
        if (digest != null) {
            map.put(FIELD_DIGEST, digest);
        }
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this source to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * @param map the input map
     * @return the parsed source
     * @throws IOException if a required field is missing, a closed value is unknown, or an invariant
     *                     is broken
     */
    public static SourceResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        StartJson.requireOnlyKnown(map, KNOWN_FIELDS, "SourceResource");
        var sourceId = JsonTree.string(map, FIELD_SOURCE_ID);
        var reference = JsonTree.string(map, FIELD_REFERENCE);
        if (sourceId == null || reference == null) {
            throw new JsonFormatException(
                    "Missing required fields for SourceResource: source_id, reference");
        }
        var kind = SourceKind.fromWireOrNull(JsonTree.string(map, FIELD_KIND));
        if (kind == null) {
            throw new JsonFormatException("Unknown " + FIELD_KIND + " in SourceResource: "
                    + JsonTree.string(map, FIELD_KIND));
        }
        var state = SourceState.fromWireOrNull(JsonTree.string(map, FIELD_STATE));
        if (state == null) {
            throw new JsonFormatException("Unknown " + FIELD_STATE + " in SourceResource: "
                    + JsonTree.string(map, FIELD_STATE));
        }
        var trust = SourceTrust.fromWireOrNull(JsonTree.string(map, FIELD_TRUST));
        if (trust == null) {
            throw new JsonFormatException("Unknown " + FIELD_TRUST + " in SourceResource: "
                    + JsonTree.string(map, FIELD_TRUST));
        }
        var verdictWire = JsonTree.string(map, FIELD_VERDICT);
        var verdict = verdictWire != null ? ScreeningVerdict.fromWireOrNull(verdictWire) : null;
        if (verdictWire != null && verdict == null) {
            throw new JsonFormatException("Unknown " + FIELD_VERDICT + " in SourceResource: " + verdictWire);
        }
        var dispositionWire = JsonTree.string(map, FIELD_DISPOSITION);
        var disposition = dispositionWire != null ? UnscreenedDisposition.fromWireOrNull(dispositionWire) : null;
        if (dispositionWire != null && disposition == null) {
            throw new JsonFormatException(
                    "Unknown " + FIELD_DISPOSITION + " in SourceResource: " + dispositionWire);
        }
        var failureMap = JsonTree.objectOrNull(map, FIELD_FAILURE, "SourceResource");
        try {
            return new SourceResource(sourceId, kind, reference, JsonTree.string(map, FIELD_TITLE), state,
                    trust, verdict, failureMap != null ? SourceFailure.fromMap(failureMap) : null,
                    readSpans(map), disposition, JsonTree.string(map, FIELD_DISPOSITION_REASON),
                    readChangedFiles(map), JsonTree.string(map, FIELD_DIGEST));
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid SourceResource: " + e.getMessage(), e);
        }
    }

    /**
     * @param json the JSON object string
     * @return the parsed source
     * @throws IOException if the value is not an object or a field is malformed
     */
    public static SourceResource fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for SourceResource");
        }
        return fromMap(obj);
    }

    /**
     * Reads one source from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed source
     * @throws IOException on format failure
     */
    public static SourceResource readFrom(JsonParser parser) throws IOException {
        var map = JsonTree.asObject(JsonTree.readValue(parser));
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for SourceResource");
        }
        return fromMap(map);
    }

    private static List<ScreeningSpan> readSpans(Map<String, Object> map) throws IOException {
        return StartJson.readEach(StartJson.readObjects(map, FIELD_SPANS), ScreeningSpan::fromMap);
    }

    private static List<ChangedFile> readChangedFiles(Map<String, Object> map) throws IOException {
        return StartJson.readEach(StartJson.readObjects(map, FIELD_CHANGED_FILES), ChangedFile::fromMap);
    }
}
