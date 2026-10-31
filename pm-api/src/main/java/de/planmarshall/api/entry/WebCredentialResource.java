/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.entry;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * A credential entry as the web app carries it: the key, whether the entry is present, the store
 * that holds it, and that a secret may be entered on the loopback listener only
 * (03-web-server.adoc#web-credentials, UC-ONB-8). The secret itself is never part of this
 * representation: it travels in the body of one write and never leaves the runtime.
 *
 * @param credentialKey the credential key, e.g. {@code github}
 * @param status        the entry's status
 * @param store         the store that holds it, or {@code null} when the status names none
 * @param loopbackOnly  whether a secret may be entered on the loopback listener only
 */
public record WebCredentialResource(
        String credentialKey,
        Status status,
        @Nullable Store store,
        boolean loopbackOnly
) {

    public static final String FIELD_CREDENTIAL_KEY = "credential_key";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_STORE = "store";
    public static final String FIELD_LOOPBACK_ONLY = "loopback_only";

    /**
     * The closed status of a credential entry.
     */
    public enum Status {

        /** The entry exists in the chosen store. */
        PRESENT("present"),

        /** No entry of that key exists. */
        NOT_FOUND("not_found"),

        /** The entry was written by the request that answered with this status. */
        STORED("stored");

        private final String wire;

        Status(String wire) {
            this.wire = wire;
        }

        /**
         * @return the wire representation of this status
         */
        public String wire() {
            return wire;
        }

        /**
         * Resolves a wire value to its status.
         *
         * @param wire the wire representation
         * @return the status, or {@code null} when the value names none
         */
        public static Status fromWireOrNull(String wire) {
            if (wire == null) {
                return null;
            }
            for (var status : values()) {
                if (status.wire.equalsIgnoreCase(wire)) {
                    return status;
                }
            }
            return null;
        }

        /**
         * @return the wire values of the closed list
         */
        public static List<String> wireValues() {
            return Arrays.stream(values()).map(Status::wire).toList();
        }
    }

    /**
     * The closed list of credential stores.
     */
    public enum Store {

        /** The macOS keychain. */
        KEYCHAIN("keychain"),

        /** The freedesktop secret service. */
        SECRET_SERVICE("secret-service"),

        /** The runtime's private credential file. */
        FILE("file");

        private final String wire;

        Store(String wire) {
            this.wire = wire;
        }

        /**
         * @return the wire representation of this store
         */
        public String wire() {
            return wire;
        }

        /**
         * Resolves a wire value to its store.
         *
         * @param wire the wire representation
         * @return the store, or {@code null} when the value names none
         */
        public static Store fromWireOrNull(String wire) {
            if (wire == null) {
                return null;
            }
            for (var store : values()) {
                if (store.wire.equalsIgnoreCase(wire)) {
                    return store;
                }
            }
            return null;
        }

        /**
         * @return the wire values of the closed list
         */
        public static List<String> wireValues() {
            return Arrays.stream(values()).map(Store::wire).toList();
        }
    }

    public WebCredentialResource {
        Objects.requireNonNull(credentialKey, FIELD_CREDENTIAL_KEY);
        Objects.requireNonNull(status, FIELD_STATUS);
    }

    /**
     * @param credentialKey the key of a written entry
     * @return the resource of an entry the request just stored
     */
    public static WebCredentialResource stored(String credentialKey) {
        return new WebCredentialResource(credentialKey, Status.STORED, null, true);
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_CREDENTIAL_KEY, credentialKey);
        map.put(FIELD_STATUS, status.wire());
        if (store != null) {
            map.put(FIELD_STORE, store.wire());
        }
        map.put(FIELD_LOOPBACK_ONLY, loopbackOnly);
        return map;
    }

    /**
     * @return compact JSON
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Writes this resource to a streaming generator.
     *
     * @param generator the generator
     * @throws IOException on write failure
     */
    public void writeTo(JsonGenerator generator) throws IOException {
        JsonTree.writeValue(generator, toMap());
    }

    /**
     * Parses a credential entry resource from a JSON tree map.
     *
     * @param map the input map
     * @return the resource
     * @throws IOException on a missing or unknown member
     */
    public static WebCredentialResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var key = JsonTree.string(map, FIELD_CREDENTIAL_KEY);
        if (key == null) {
            throw new JsonFormatException("Missing credential_key in WebCredentialResource");
        }
        var statusWire = JsonTree.string(map, FIELD_STATUS);
        var status = Status.fromWireOrNull(statusWire);
        if (status == null) {
            throw new JsonFormatException("Missing or unknown status in WebCredentialResource: " + statusWire);
        }
        var storeWire = JsonTree.string(map, FIELD_STORE);
        var store = Store.fromWireOrNull(storeWire);
        if (storeWire != null && store == null) {
            throw new JsonFormatException("Unknown store in WebCredentialResource: " + storeWire);
        }
        var loopbackOnly = JsonTree.bool(map, FIELD_LOOPBACK_ONLY);
        return new WebCredentialResource(key, status, store, loopbackOnly == null || loopbackOnly);
    }

    /**
     * Parses a credential entry resource from a JSON string.
     *
     * @param json the JSON string
     * @return the resource
     * @throws IOException on parse failure
     */
    public static WebCredentialResource fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for WebCredentialResource");
        }
        return fromMap(obj);
    }

    /**
     * Reads one credential entry resource from a streaming parser positioned at its first token.
     *
     * @param parser the parser
     * @return the resource
     * @throws IOException on parse failure
     */
    public static WebCredentialResource readFrom(JsonParser parser) throws IOException {
        var obj = JsonTree.asObject(JsonTree.readValue(parser));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for WebCredentialResource");
        }
        return fromMap(obj);
    }
}
