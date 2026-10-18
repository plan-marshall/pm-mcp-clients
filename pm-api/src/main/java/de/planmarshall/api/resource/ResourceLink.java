/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One link of a daemon resource of {@code /api/v1}: the request that reaches the target resource,
 * the closed {@link LinkRelation} of that target, and whether the {@code href} is a URI template a
 * client fills before it follows the link.
 *
 * @param href      the target, an absolute path of the local API
 * @param rel       the closed relation of the target
 * @param method    the HTTP method that follows the link
 * @param templated whether {@code href} is a URI template
 */
public record ResourceLink(
        String href,
        LinkRelation rel,
        String method,
        boolean templated
) {
    public static final String FIELD_HREF = "href";
    public static final String FIELD_REL = "rel";
    public static final String FIELD_METHOD = "method";
    public static final String FIELD_TEMPLATED = "templated";
    public static final String FIELD_LINKS = "links";

    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_PATCH = "PATCH";
    public static final String METHOD_DELETE = "DELETE";

    public ResourceLink {
        Objects.requireNonNull(href, "href");
        if (href.isBlank()) {
            throw new IllegalArgumentException("href must not be blank");
        }
        Objects.requireNonNull(rel, "rel");
        method = method == null || method.isBlank() ? METHOD_GET : method;
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_HREF, href);
        map.put(FIELD_REL, rel.wire());
        map.put(FIELD_METHOD, method);
        map.put(FIELD_TEMPLATED, templated);
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
    public static ResourceLink fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var href = JsonTree.string(map, FIELD_HREF);
        if (href == null || href.isBlank()) {
            throw new JsonFormatException("Missing href in ResourceLink");
        }
        var rel = LinkRelation.fromWire(JsonTree.string(map, FIELD_REL));
        if (rel == null) {
            throw new JsonFormatException(
                    "Unknown relationship in ResourceLink: " + JsonTree.string(map, FIELD_REL));
        }
        var method = JsonTree.string(map, FIELD_METHOD);
        var templated = Boolean.TRUE.equals(JsonTree.bool(map, FIELD_TEMPLATED));
        return new ResourceLink(href, rel, method, templated);
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed link
     * @throws IOException on format failure
     */
    public static ResourceLink fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            var strMap = (Map<String, Object>) map;
            return fromMap(strMap);
        }
        throw new JsonFormatException("Expected JSON object for ResourceLink");
    }

    /**
     * Reads one link from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the parsed link
     * @throws IOException on format failure
     */
    public static ResourceLink readFrom(JsonParser parser) throws IOException {
        var map = objectOf(parser, "ResourceLink");
        return fromMap(map);
    }

    /**
     * @param array the {@code links} member of a resource, may be {@code null}
     * @return the parsed links, empty if the member is absent
     * @throws IOException if the member is no array of link objects
     */
    public static List<ResourceLink> fromArray(Object array) throws IOException {
        if (array == null) {
            return List.of();
        }
        var list = JsonTree.asArray(array);
        if (list == null) {
            throw new JsonFormatException("Expected a JSON array of links");
        }
        var links = new ArrayList<ResourceLink>(list.size());
        for (var element : list) {
            var map = JsonTree.asObject(element);
            if (map == null) {
                throw new JsonFormatException("Expected a link object in the links array");
            }
            links.add(fromMap(map));
        }
        return List.copyOf(links);
    }

    /**
     * @param links the links of a resource
     * @return the links as JSON tree maps, in order
     */
    public static List<Map<String, Object>> toMaps(List<ResourceLink> links) {
        return links.stream().map(ResourceLink::toMap).toList();
    }

    static Map<String, Object> objectOf(JsonParser parser, String type) throws IOException {
        return objectOf(JsonTree.readValue(parser), type);
    }

    static Map<String, Object> objectOf(String json, String type) throws IOException {
        return objectOf(JsonTree.parse(json), type);
    }

    private static Map<String, Object> objectOf(Object parsed, String type) throws IOException {
        var map = JsonTree.asObject(parsed);
        if (map == null) {
            throw new JsonFormatException("Expected JSON object for " + type);
        }
        return map;
    }
}
