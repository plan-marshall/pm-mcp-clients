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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One operator link of a state's operator representation: an act the state offers the operator, the
 * object {@code {id, rel, method, href, params, preselected, confirm}} of the Hypermedia Format
 * specification (Operator Links).
 * <p>
 * {@code id} follows the link identifier grammar and is unique within the state; a client and a mock
 * scenario key a link by its {@code id}, never by its relation, which several links share.
 * {@code method} and {@code href} are the local API request that takes the act
 * ({@code POST /api/v1/entries/ent-3f9a2c41/links/yes}); {@code params} is the closed schema of the
 * request body as a JSON object (an empty object where the act takes none); {@code preselected} marks
 * the link the state proposes; {@code confirm} names the message key of a confirmation the client shows
 * before it follows the link, where the state asks for one.
 *
 * @param id          the id of the link, unique within the state
 * @param rel         the closed relation of the link
 * @param method      the HTTP method that follows the link
 * @param href        the target, an absolute path of the local API
 * @param params      the closed schema of the request body, empty where the act takes none
 * @param preselected whether the state proposes this link
 * @param confirm     the message key of the confirmation the client shows, or {@code null}
 */
public record OperatorLink(
        String id,
        OperatorLinkRelation rel,
        String method,
        String href,
        Map<String, Object> params,
        boolean preselected,
        @Nullable String confirm
) {
    public static final String FIELD_ID = "id";
    public static final String FIELD_REL = "rel";
    public static final String FIELD_METHOD = "method";
    public static final String FIELD_HREF = "href";
    public static final String FIELD_PARAMS = "params";
    public static final String FIELD_PRESELECTED = "preselected";
    public static final String FIELD_CONFIRM = "confirm";
    public static final String FIELD_LINKS = "links";

    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_PATCH = "PATCH";
    public static final String METHOD_DELETE = "DELETE";

    /** The closed set of HTTP methods an operator link may carry. */
    public static final Set<String> METHODS = Set.of(
            METHOD_GET, METHOD_POST, METHOD_PUT, METHOD_PATCH, METHOD_DELETE);

    /** The link identifier grammar of the identifier registry (scope-wide or entity-addressed form). */
    private static final Pattern ID_GRAMMAR =
            Pattern.compile("^[a-z][a-z0-9_-]{0,31}(:[A-Za-z0-9][A-Za-z0-9_.-]{0,63})?$");

    private static final Set<String> KNOWN_FIELDS = Set.of(
            FIELD_ID, FIELD_REL, FIELD_METHOD, FIELD_HREF, FIELD_PARAMS, FIELD_PRESELECTED, FIELD_CONFIRM);

    public OperatorLink {
        if (!matchesIdGrammar(id)) {
            throw new IllegalArgumentException("Invalid operator link id: " + id);
        }
        Objects.requireNonNull(rel, FIELD_REL);
        if (method == null || !METHODS.contains(method)) {
            throw new IllegalArgumentException("Invalid operator link method: " + method);
        }
        if (href == null || href.isBlank()) {
            throw new IllegalArgumentException("href must not be blank in OperatorLink");
        }
        params = params == null ? Map.of() : Map.copyOf(params);
        if (confirm != null && !OperatorText.matchesMessageKeyGrammar(confirm)) {
            throw new IllegalArgumentException("Invalid confirm message key in OperatorLink: " + confirm);
        }
    }

    /**
     * @param value a candidate link id
     * @return {@code true} if the value satisfies the link identifier grammar
     */
    public static boolean matchesIdGrammar(String value) {
        return value != null && ID_GRAMMAR.matcher(value).matches();
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_ID, id);
        map.put(FIELD_REL, rel.wire());
        map.put(FIELD_METHOD, method);
        map.put(FIELD_HREF, href);
        if (!params.isEmpty()) {
            map.put(FIELD_PARAMS, params);
        }
        map.put(FIELD_PRESELECTED, preselected);
        if (confirm != null) {
            map.put(FIELD_CONFIRM, confirm);
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
     * Writes this link to a streaming generator.
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
     * @return parsed link
     * @throws IOException on format failure
     */
    public static OperatorLink fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        for (var key : map.keySet()) {
            if (!KNOWN_FIELDS.contains(key)) {
                throw new JsonFormatException("Unknown field in OperatorLink: " + key);
            }
        }
        var id = JsonTree.string(map, FIELD_ID);
        if (!matchesIdGrammar(id)) {
            throw new JsonFormatException("Invalid or missing " + FIELD_ID + " in OperatorLink: " + id);
        }
        var rel = OperatorLinkRelation.fromWireOrNull(JsonTree.string(map, FIELD_REL));
        if (rel == null) {
            throw new JsonFormatException(
                    "Unknown " + FIELD_REL + " in OperatorLink: " + JsonTree.string(map, FIELD_REL));
        }
        var method = JsonTree.string(map, FIELD_METHOD);
        if (method == null || !METHODS.contains(method)) {
            throw new JsonFormatException("Invalid " + FIELD_METHOD + " in OperatorLink: " + method);
        }
        var href = JsonTree.string(map, FIELD_HREF);
        if (href == null || href.isBlank()) {
            throw new JsonFormatException("Missing " + FIELD_HREF + " in OperatorLink");
        }
        var params = JsonTree.object(map, FIELD_PARAMS);
        var preselected = Boolean.TRUE.equals(JsonTree.bool(map, FIELD_PRESELECTED));
        var confirm = JsonTree.string(map, FIELD_CONFIRM);
        if (confirm != null && !OperatorText.matchesMessageKeyGrammar(confirm)) {
            throw new JsonFormatException("Invalid " + FIELD_CONFIRM + " in OperatorLink: " + confirm);
        }
        return new OperatorLink(id, rel, method, href, params, preselected, confirm);
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed link
     * @throws IOException on format failure
     */
    public static OperatorLink fromJson(String json) throws IOException {
        return fromMap(objectOf(JsonTree.parse(json)));
    }

    /**
     * Reads one link from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return parsed link
     * @throws IOException on format failure
     */
    public static OperatorLink readFrom(JsonParser parser) throws IOException {
        return fromMap(objectOf(JsonTree.readValue(parser)));
    }

    /**
     * @param array the {@code links} member of a state, may be {@code null}
     * @return the parsed links, empty if the member is absent
     * @throws IOException if the member is no array of link objects
     */
    public static List<OperatorLink> fromArray(Object array) throws IOException {
        if (array == null) {
            return List.of();
        }
        var list = JsonTree.asArray(array);
        if (list == null) {
            throw new JsonFormatException("Expected a JSON array of operator links");
        }
        var links = new ArrayList<OperatorLink>(list.size());
        for (var element : list) {
            var object = JsonTree.asObject(element);
            if (object == null) {
                throw new JsonFormatException("Expected an operator link object in the links array");
            }
            links.add(fromMap(object));
        }
        return List.copyOf(links);
    }

    /**
     * @param links the links of a state
     * @return the links as JSON tree maps, in order
     */
    public static List<Map<String, Object>> toMaps(List<OperatorLink> links) {
        return links.stream().map(OperatorLink::toMap).toList();
    }

    private static Map<String, Object> objectOf(Object parsed) throws IOException {
        var map = JsonTree.asObject(parsed);
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for OperatorLink");
        }
        return map;
    }
}
