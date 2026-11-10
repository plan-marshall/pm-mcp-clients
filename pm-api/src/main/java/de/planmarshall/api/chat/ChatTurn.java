/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.chat;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonTree;

/**
 * One turn of a chat's transcript as {@code GET …/chat} reads it
 * ({@code 01-processes-and-transport.adoc#chat-routes}): an operator's message, a role's reply, a
 * proposal, or a notice the runtime writes.
 * <p>
 * A turn being written is read with its text so far ({@link TurnStatus#STREAMING}); a stopped or failed
 * reply keeps its text so far, a failed one with its {@code reason} ({@code infeasible}, {@code lost},
 * {@code too_long}). A notice about the chat's subject carries the narrative {@code lines} it reports;
 * no other kind carries lines.
 *
 * @param turnId    the turn's id
 * @param kind      the turn's kind
 * @param author    {@code operator}, or the role that wrote it
 * @param status    the turn's status
 * @param format    the format of its text
 * @param untrusted whether the turn came from outside the machine
 * @param model     the model of a role's turn, or {@code null}
 * @param text      the turn's text (so far), may be empty
 * @param steps     the steps the role reported, as message keys
 * @param refs      the references the turn names
 * @param at        the instant of the turn, ISO-8601
 * @param reason    the reason of a failed turn, present exactly when the status is {@code failed}
 * @param lines     the narrative lines of a notice, empty for every other kind
 */
public record ChatTurn(
        String turnId,
        TurnKind kind,
        String author,
        TurnStatus status,
        TurnFormat format,
        boolean untrusted,
        @Nullable String model,
        String text,
        List<MessageText> steps,
        List<TurnRef> refs,
        String at,
        @Nullable MessageText reason,
        List<NarrativeLine> lines
) {

    /** The author of the operator's own messages. */
    public static final String AUTHOR_OPERATOR = "operator";

    /** The wire name of the turn's id. */
    public static final String FIELD_TURN_ID = "turn_id";
    /** The wire name of the turn's kind. */
    public static final String FIELD_KIND = "kind";
    /** The wire name of the turn's author. */
    public static final String FIELD_AUTHOR = "author";
    /** The wire name of the turn's status. */
    public static final String FIELD_STATUS = "status";
    /** The wire name of the turn's format. */
    public static final String FIELD_FORMAT = "format";
    /** The wire name of the untrusted mark. */
    public static final String FIELD_UNTRUSTED = "untrusted";
    /** The wire name of the model. */
    public static final String FIELD_MODEL = "model";
    /** The wire name of the text. */
    public static final String FIELD_TEXT = "text";
    /** The wire name of the steps. */
    public static final String FIELD_STEPS = "steps";
    /** The wire name of the references. */
    public static final String FIELD_REFS = "refs";
    /** The wire name of the instant. */
    public static final String FIELD_AT = "at";
    /** The wire name of the failed turn's reason. */
    public static final String FIELD_REASON = "reason";
    /** The wire name of a notice's lines. */
    public static final String FIELD_LINES = "lines";

    private static final String TYPE = "ChatTurn";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_TURN_ID, FIELD_KIND, FIELD_AUTHOR,
            FIELD_STATUS, FIELD_FORMAT, FIELD_UNTRUSTED, FIELD_MODEL, FIELD_TEXT, FIELD_STEPS, FIELD_REFS,
            FIELD_AT, FIELD_REASON, FIELD_LINES);

    /**
     * Checks the turn's required members and the members its kind and status require or refuse.
     *
     * @param turnId    the id, not blank
     * @param kind      the kind
     * @param author    the author, not blank
     * @param status    the status
     * @param format    the format
     * @param untrusted the untrusted mark
     * @param model     the model, or {@code null}
     * @param text      the text, {@code null} read as empty
     * @param steps     the steps, {@code null} read as none
     * @param refs      the references, {@code null} read as none
     * @param at        the instant, not blank
     * @param reason    the reason, required for {@code failed} and refused otherwise
     * @param lines     the lines, {@code null} read as none, refused for every kind but {@code notice}
     */
    public ChatTurn {
        Objects.requireNonNull(turnId, FIELD_TURN_ID);
        Objects.requireNonNull(kind, FIELD_KIND);
        Objects.requireNonNull(author, FIELD_AUTHOR);
        Objects.requireNonNull(status, FIELD_STATUS);
        Objects.requireNonNull(format, FIELD_FORMAT);
        Objects.requireNonNull(at, FIELD_AT);
        if (turnId.isBlank() || author.isBlank() || at.isBlank()) {
            throw new IllegalArgumentException("turn_id, author and at must not be blank");
        }
        text = text == null ? "" : text;
        steps = steps == null ? List.of() : List.copyOf(steps);
        refs = refs == null ? List.of() : List.copyOf(refs);
        lines = lines == null ? List.of() : List.copyOf(lines);
        if ((status == TurnStatus.FAILED) != (reason != null)) {
            throw new IllegalArgumentException("a reason is required exactly for a failed turn");
        }
        if (kind != TurnKind.NOTICE && !lines.isEmpty()) {
            throw new IllegalArgumentException("only a notice carries lines");
        }
    }

    /**
     * @return {@code true} if this is the operator's own turn
     */
    public boolean isOperator() {
        return AUTHOR_OPERATOR.equals(author);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_TURN_ID, turnId);
        map.put(FIELD_KIND, kind.wireValue());
        map.put(FIELD_AUTHOR, author);
        map.put(FIELD_STATUS, status.wireValue());
        map.put(FIELD_FORMAT, format.wireValue());
        map.put(FIELD_UNTRUSTED, untrusted);
        if (model != null) {
            map.put(FIELD_MODEL, model);
        }
        map.put(FIELD_TEXT, text);
        map.put(FIELD_STEPS, steps.stream().map(MessageText::toMap).toList());
        map.put(FIELD_REFS, refs.stream().map(TurnRef::toMap).toList());
        map.put(FIELD_AT, at);
        if (reason != null) {
            map.put(FIELD_REASON, reason.toMap());
        }
        if (!lines.isEmpty()) {
            map.put(FIELD_LINES, lines.stream().map(NarrativeLine::toMap).toList());
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
     * @param map the input map
     * @return the parsed turn
     * @throws IOException if a member is unknown, missing, malformed, or refused by the turn's kind or
     *                     status
     */
    public static ChatTurn fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var turnId = ChatJson.requireText(map, FIELD_TURN_ID, TYPE);
        var kind = ChatJson.requireEnum(map, FIELD_KIND, TurnKind::fromWireOrNull, TYPE);
        var author = ChatJson.requireText(map, FIELD_AUTHOR, TYPE);
        var status = ChatJson.requireEnum(map, FIELD_STATUS, TurnStatus::fromWireOrNull, TYPE);
        var format = ChatJson.requireEnum(map, FIELD_FORMAT, TurnFormat::fromWireOrNull, TYPE);
        var untrusted = ChatJson.requireBoolean(map, FIELD_UNTRUSTED, TYPE);
        var model = ChatJson.optionalText(map, FIELD_MODEL, TYPE);
        var text = ChatJson.requireString(map, FIELD_TEXT, TYPE);
        var steps = ChatJson.readRecords(map, FIELD_STEPS, MessageText::fromMap, TYPE);
        var refs = ChatJson.readRecords(map, FIELD_REFS, TurnRef::fromMap, TYPE);
        var at = ChatJson.requireText(map, FIELD_AT, TYPE);
        var reason = ChatJson.optionalRecord(map, FIELD_REASON, MessageText::fromMap, TYPE);
        var lines = ChatJson.readRecords(map, FIELD_LINES, NarrativeLine::fromMap, TYPE);
        return ChatJson.construct(TYPE, () -> new ChatTurn(turnId, kind, author, status, format, untrusted,
                model, text, steps, refs, at, reason, lines));
    }

    /**
     * @param json the JSON object string
     * @return the parsed turn
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static ChatTurn fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
