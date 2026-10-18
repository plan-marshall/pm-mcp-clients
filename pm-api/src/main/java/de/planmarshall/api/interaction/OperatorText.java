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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.id.DottedIdentifier;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One text the runtime gives a client to show, exactly one of two forms (Hypermedia Format, Texts of
 * the Runtime):
 * <ul>
 *   <li>a <em>message key</em> with its arguments, rendered by the client from the message catalogue
 *       of its interface language: {@code {"key": "check.login.missing", "args": {"harness": "claude"}}};
 *       or</li>
 *   <li><em>verbatim</em> text that does not come from the runtime, marked with its {@link TextSource}:
 *       {@code {"verbatim": "…", "source": "provider"}}.</li>
 * </ul>
 * An argument of a message key is a value or a verbatim text object, never a key of its own. Text of a
 * model, a provider, a harness, or a repository is untrusted and shown as plain text.
 *
 * @param key      the message key, or {@code null} in the verbatim form
 * @param args     the arguments of the message key, empty in the verbatim form
 * @param verbatim the verbatim text, or {@code null} in the key form
 * @param source   the source of the verbatim text, or {@code null} in the key form
 */
public record OperatorText(
        @Nullable String key,
        Map<String, Object> args,
        @Nullable String verbatim,
        @Nullable TextSource source
) {
    public static final String FIELD_KEY = "key";
    public static final String FIELD_ARGS = "args";
    public static final String FIELD_VERBATIM = "verbatim";
    public static final String FIELD_SOURCE = "source";

    public OperatorText {
        var hasKey = key != null;
        var hasVerbatim = verbatim != null;
        if (hasKey == hasVerbatim) {
            throw new IllegalArgumentException("OperatorText carries either a key or verbatim text, never none or both");
        }
        if (hasKey) {
            if (!matchesMessageKeyGrammar(key)) {
                throw new IllegalArgumentException("Invalid message key in OperatorText: " + key);
            }
            if (source != null) {
                throw new IllegalArgumentException("A message key carries no source in OperatorText");
            }
            args = args == null ? Map.of() : Map.copyOf(args);
        } else {
            if (source == null) {
                throw new IllegalArgumentException("Verbatim text requires a source in OperatorText");
            }
            args = Map.of();
        }
    }

    /**
     * @param key the message key
     * @return a message-key text with no arguments
     */
    public static OperatorText ofKey(String key) {
        return new OperatorText(key, Map.of(), null, null);
    }

    /**
     * @param key  the message key
     * @param args the arguments of the message key
     * @return a message-key text
     */
    public static OperatorText ofKey(String key, Map<String, Object> args) {
        return new OperatorText(key, args, null, null);
    }

    /**
     * @param text   the verbatim text
     * @param source the source of the text
     * @return a verbatim text
     */
    public static OperatorText ofVerbatim(String text, TextSource source) {
        return new OperatorText(null, Map.of(), text, source);
    }

    /**
     * @param value a candidate message key
     * @return {@code true} if the value satisfies the Message Key grammar
     */
    public static boolean matchesMessageKeyGrammar(String value) {
        return DottedIdentifier.messageKey(value);
    }

    /**
     * @return {@code true} if this text is a message key
     */
    public boolean isKey() {
        return key != null;
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        if (key != null) {
            map.put(FIELD_KEY, key);
            if (!args.isEmpty()) {
                map.put(FIELD_ARGS, args);
            }
        } else {
            map.put(FIELD_VERBATIM, verbatim);
            map.put(FIELD_SOURCE, Objects.requireNonNull(source).wire());
        }
        return map;
    }

    /**
     * Serializes to a JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this text to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * Parses from a JSON tree map.
     *
     * @param map input map
     * @return parsed text
     * @throws IOException on format failure
     */
    public static OperatorText fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var key = JsonTree.string(map, FIELD_KEY);
        var verbatim = JsonTree.string(map, FIELD_VERBATIM);
        if (key != null) {
            if (verbatim != null) {
                throw new JsonFormatException("OperatorText carries both key and verbatim");
            }
            return new OperatorText(key, JsonTree.object(map, FIELD_ARGS), null, null);
        }
        if (verbatim == null) {
            throw new JsonFormatException("OperatorText carries neither key nor verbatim");
        }
        var source = TextSource.fromWireOrNull(JsonTree.string(map, FIELD_SOURCE));
        if (source == null) {
            throw new JsonFormatException(
                    "Unknown " + FIELD_SOURCE + " in OperatorText: " + JsonTree.string(map, FIELD_SOURCE));
        }
        return new OperatorText(null, Map.of(), verbatim, source);
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed text
     * @throws IOException on format failure
     */
    public static OperatorText fromJson(String json) throws IOException {
        return fromMap(objectOf(JsonTree.parse(json)));
    }

    /**
     * Reads one text from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return parsed text
     * @throws IOException on format failure
     */
    public static OperatorText readFrom(JsonParser parser) throws IOException {
        return fromMap(objectOf(JsonTree.readValue(parser)));
    }

    private static Map<String, Object> objectOf(Object parsed) throws IOException {
        var map = JsonTree.asObject(parsed);
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for OperatorText");
        }
        return map;
    }
}
