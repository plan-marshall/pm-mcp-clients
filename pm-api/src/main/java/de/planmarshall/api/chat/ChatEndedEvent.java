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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.event.EventName;
import de.planmarshall.api.json.JsonTree;

/**
 * The payload of {@code chat.ended} ({@code 06-event-catalogue.adoc#chat-events}): a chat ended, with
 * what ended it, and when the operator ended it the channel and the device where that happened.
 * <p>
 * The channel and the device belong to the operator's end only; a chat that ended any other way
 * carries neither. An absent channel stays absent and is never read as a default.
 *
 * @param chatId  the chat's id
 * @param endedBy what ended the chat
 * @param channel the channel where the operator ended it, or {@code null}
 * @param device  the device where the operator ended it, or {@code null}
 */
public record ChatEndedEvent(String chatId, EndedBy endedBy, @Nullable String channel, @Nullable String device) {

    /** The event's name in the catalogue. */
    public static final String EVENT_NAME = EventName.CHAT_ENDED.wire();

    /** The wire name of the chat's id. */
    public static final String FIELD_CHAT_ID = "chat_id";
    /** The wire name of what ended the chat. */
    public static final String FIELD_ENDED_BY = "ended_by";
    /** The wire name of the channel. */
    public static final String FIELD_CHANNEL = "channel";
    /** The wire name of the device. */
    public static final String FIELD_DEVICE = "device";

    private static final String TYPE = "ChatEndedEvent";
    private static final Set<String> KNOWN_FIELDS = Set.of(FIELD_CHAT_ID, FIELD_ENDED_BY, FIELD_CHANNEL,
            FIELD_DEVICE);

    /**
     * The closed list of what ends a chat.
     */
    public enum EndedBy {

        /** The operator ended the chat. */
        OPERATOR("operator"),

        /** The proposal the chat made was carried, and the chat ended with it. */
        PROPOSAL_CARRIED("proposal_carried"),

        /** The decision the chat is about was answered elsewhere. */
        ANSWERED_ELSEWHERE("answered_elsewhere"),

        /** The chat's subject ended. */
        SUBJECT_ENDED("subject_ended"),

        /** The entry the chat belongs to was closed. */
        ENTRY_CLOSED("entry_closed");

        private final String wireValue;

        EndedBy(String wireValue) {
            this.wireValue = wireValue;
        }

        /**
         * @return the value used on the wire
         */
        public String wireValue() {
            return wireValue;
        }

        /**
         * @param value the wire value, may be {@code null}
         * @return the member, or {@code null} when the value names none
         */
        public static @Nullable EndedBy fromWireOrNull(@Nullable String value) {
            if (value == null) {
                return null;
            }
            for (var endedBy : values()) {
                if (endedBy.wireValue.equals(value)) {
                    return endedBy;
                }
            }
            return null;
        }

        /**
         * @param value the wire value
         * @return the member
         * @throws IllegalArgumentException if the value names no member (fail-closed)
         */
        public static EndedBy fromWire(String value) {
            Objects.requireNonNull(value, "value");
            var endedBy = fromWireOrNull(value);
            if (endedBy == null) {
                throw new IllegalArgumentException("Unknown ended_by: " + value);
            }
            return endedBy;
        }

        /**
         * @return the wire values of the closed list, in declaration order
         */
        public static List<String> wireValues() {
            return Arrays.stream(values()).map(EndedBy::wireValue).toList();
        }
    }

    /**
     * @param chatId  the chat's id, not blank
     * @param endedBy what ended the chat
     * @param channel the channel, refused unless the operator ended the chat
     * @param device  the device, refused unless the operator ended the chat
     */
    public ChatEndedEvent {
        Objects.requireNonNull(chatId, FIELD_CHAT_ID);
        Objects.requireNonNull(endedBy, FIELD_ENDED_BY);
        if (chatId.isBlank()) {
            throw new IllegalArgumentException("chat_id must not be blank");
        }
        if (endedBy != EndedBy.OPERATOR && (channel != null || device != null)) {
            throw new IllegalArgumentException("only the operator's end carries a channel and a device");
        }
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_CHAT_ID, chatId);
        map.put(FIELD_ENDED_BY, endedBy.wireValue());
        if (channel != null) {
            map.put(FIELD_CHANNEL, channel);
        }
        if (device != null) {
            map.put(FIELD_DEVICE, device);
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
     * @return the parsed payload
     * @throws IOException if a member is unknown, missing, malformed, or refused by what ended the chat
     */
    public static ChatEndedEvent fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        ChatJson.requireOnlyKnown(map, KNOWN_FIELDS, TYPE);
        var chatId = ChatJson.requireText(map, FIELD_CHAT_ID, TYPE);
        var endedBy = ChatJson.requireEnum(map, FIELD_ENDED_BY, EndedBy::fromWireOrNull, TYPE);
        var channel = ChatJson.optionalText(map, FIELD_CHANNEL, TYPE);
        var device = ChatJson.optionalText(map, FIELD_DEVICE, TYPE);
        return ChatJson.construct(TYPE, () -> new ChatEndedEvent(chatId, endedBy, channel, device));
    }

    /**
     * @param json the JSON object string
     * @return the parsed payload
     * @throws IOException if the value is not an object or a member is malformed
     */
    public static ChatEndedEvent fromJson(String json) throws IOException {
        return fromMap(ChatJson.parseObject(json, TYPE));
    }
}
