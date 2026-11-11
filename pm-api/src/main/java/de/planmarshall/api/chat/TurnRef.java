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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonTree;

/**
 * A reference a turn or a narrative line names ({@code 01-processes-and-transport.adoc#chat-routes}):
 * {@code {kind, id}} for a task, a file with its line ({@code path:line}), a decision's address, a
 * plan or a hold, and {@code {kind: act, act, subject}} for an operator act the role names, which a
 * client leads the operator to and never carries out ({@code state-catalogue.adoc#chat-names-acts}).
 *
 * @param kind    the reference's kind
 * @param id      the referenced id, present for every kind but {@link RefKind#ACT}
 * @param act     the act's name, present for {@link RefKind#ACT} alone
 * @param subject the subject the act is offered on, present for {@link RefKind#ACT} alone
 */
public record TurnRef(RefKind kind, @Nullable String id, @Nullable String act, @Nullable ChatSubject subject) {

    /** The wire name of the reference's kind. */
    public static final String FIELD_KIND = "kind";
    /** The wire name of the referenced id. */
    public static final String FIELD_ID = "id";
    /** The wire name of the act. */
    public static final String FIELD_ACT = "act";
    /** The wire name of the act's subject. */
    public static final String FIELD_SUBJECT = "subject";

    private static final String TYPE = "TurnRef";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_KIND, FIELD_ID, FIELD_ACT, FIELD_SUBJECT);

    /**
     * @param kind    the kind
     * @param id      the id, required for every kind but {@code act}, refused for {@code act}
     * @param act     the act, required for {@code act}, refused otherwise
     * @param subject the act's subject, required for {@code act}, refused otherwise
     */
    public TurnRef {
        Objects.requireNonNull(kind, FIELD_KIND);
        if (kind == RefKind.ACT) {
            if (act == null || act.isBlank() || subject == null) {
                throw new IllegalArgumentException("a reference of the kind act requires act and subject");
            }
            if (id != null) {
                throw new IllegalArgumentException("a reference of the kind act carries no id");
            }
        } else {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("a reference of the kind " + kind.wireValue() + " requires id");
            }
            if (act != null || subject != null) {
                throw new IllegalArgumentException(
                        "a reference of the kind " + kind.wireValue() + " carries no act and no subject");
            }
        }
    }

    /**
     * @param kind the kind, not {@link RefKind#ACT}
     * @param id   the referenced id
     * @return the reference
     */
    public static TurnRef of(RefKind kind, String id) {
        return new TurnRef(kind, id, null, null);
    }

    /**
     * @param act     the act's name
     * @param subject the subject the act is offered on
     * @return the reference of the kind {@code act}
     */
    public static TurnRef ofAct(String act, ChatSubject subject) {
        return new TurnRef(RefKind.ACT, null, act, subject);
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_KIND, kind.wireValue());
        if (id != null) {
            map.put(FIELD_ID, id);
        }
        if (act != null) {
            map.put(FIELD_ACT, act);
        }
        if (subject != null) {
            map.put(FIELD_SUBJECT, subject.toMap());
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
     * @return the parsed reference
     * @throws IOException if a member is unknown, missing for its kind, or present against its kind
     */
    public static TurnRef fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var kind = ChatJson.requireEnum(map, FIELD_KIND, RefKind::fromWireOrNull, TYPE);
        var id = ChatJson.optionalText(map, FIELD_ID, TYPE);
        var act = ChatJson.optionalText(map, FIELD_ACT, TYPE);
        var subject = ChatJson.optionalRecord(map, FIELD_SUBJECT, ChatSubject::fromMap, TYPE);
        return ChatJson.construct(TYPE, () -> new TurnRef(kind, id, act, subject));
    }

    /**
     * @param json the JSON object string
     * @return the parsed reference
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static TurnRef fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
