/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.host;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import lombok.experimental.UtilityClass;

/**
 * The generic reader of the shipped client profile documents below {@code classpath:/profiles}: it
 * extracts the {@code client_id}, the {@code detection} object, and the {@code identity} object of
 * every document with the streaming API of {@code jackson-core}, and skips every other section
 * (tools, configuration, prompt extensions) by jumping over its tokens.
 * <p>
 * The reader names no harness. Which documents exist is data, the set of files below the resource
 * directory; the file name is the client id. A document that is not in the closed form of the two
 * objects is refused as a whole, never partly taken.
 */
@UtilityClass
public class ProfileDocumentReader {

    /** The resource directory of the shipped profile documents. */
    private static final String PROFILES = "profiles";

    /** The suffix of a profile document. */
    private static final String DOCUMENT_SUFFIX = ".json";

    private static final JsonFactory FACTORY = new JsonFactory();

    /**
     * One detection object: how a host is recognised in an environment.
     *
     * @param precedence       the position in the fixed detection order, lower wins
     * @param primarySignals   the signals that establish the detection, any one matches
     * @param secondarySignals the signals consulted only when no primary signal of any host matched
     */
    public record Detection(int precedence, List<HostDetection.Signal> primarySignals,
                            List<HostDetection.Signal> secondarySignals) {

        /**
         * Copies the lists.
         *
         * @param precedence       the precedence
         * @param primarySignals   the primary signals
         * @param secondarySignals the secondary signals
         */
        public Detection {
            primarySignals = List.copyOf(primarySignals);
            secondarySignals = List.copyOf(secondarySignals);
        }
    }

    /**
     * One identity object: the version probe of the host and what the host reports about itself.
     *
     * @param sessionIdVariable the variable holding the host session identifier, {@code null} if none
     * @param versionProbe      the argument vector printing the host build, {@code null} if none
     * @param versionPattern    the pattern over the probe's output, {@code null} without a probe
     * @param modelSource       where the model identity comes from
     * @param provisional       {@code true} if a value is provisional
     */
    public record Identity(String sessionIdVariable, List<String> versionProbe, String versionPattern,
                           String modelSource, boolean provisional) {

        /**
         * Copies the probe.
         *
         * @param sessionIdVariable the session ID variable
         * @param versionProbe      the version probe
         * @param versionPattern    the version pattern
         * @param modelSource       the model source
         * @param provisional       whether a value is provisional
         */
        public Identity {
            versionProbe = versionProbe == null ? null : List.copyOf(versionProbe);
        }
    }

    /**
     * One profile document reduced to its two host-detection objects.
     *
     * @param clientId  the client id, equal to the file name of the document
     * @param detection the detection object
     * @param identity  the identity object
     */
    public record Profile(String clientId, Detection detection, Identity identity) {
    }

    /**
     * Reads every shipped profile document below {@code classpath:/profiles}, in ascending
     * {@code detection.precedence}.
     *
     * @return the documents in detection precedence
     * @throws IllegalStateException if no document is found, one is not in the closed form of the two
     *                               objects, or two documents share a precedence
     */
    public static List<Profile> readShippedProfiles() {
        return readShippedProfiles(ProfileDocumentReader.class.getClassLoader());
    }

    /**
     * Reads every shipped profile document below {@code classpath:/profiles} of one class loader, in
     * ascending {@code detection.precedence}.
     *
     * @param loader the class loader holding the documents
     * @return the documents in detection precedence
     * @throws IllegalStateException if no document is found, one is not in the closed form of the two
     *                               objects, or two documents share a precedence
     */
    static List<Profile> readShippedProfiles(ClassLoader loader) {
        var documents = new TreeMap<String, Profile>();
        for (var root : profileRoots(loader)) {
            for (var name : documentNames(root)) {
                documents.computeIfAbsent(name, document -> readDocument(loader, document));
            }
        }
        if (documents.isEmpty()) {
            throw new IllegalStateException("no client profile document below classpath:/" + PROFILES);
        }
        var profiles = List.copyOf(documents.values());
        requireDistinctPrecedence(profiles);
        return profiles.stream().sorted(Comparator.comparingInt(profile -> profile.detection().precedence())).toList();
    }

    /**
     * Reads one profile document.
     *
     * @param input the UTF-8 JSON of the document
     * @return the document
     * @throws IllegalStateException if the JSON is malformed or not in the closed form of the two objects
     */
    static Profile parseDocument(InputStream input) {
        try (var parser = FACTORY.createParser(input)) {
            parser.nextToken();
            var profile = parseProfile(parser);
            if (parser.nextToken() != null) {
                throw new IllegalStateException("trailing content after the client profile document");
            }
            return profile;
        } catch (IOException e) {
            throw new IllegalStateException("malformed client profile document", e);
        }
    }

    private static List<URL> profileRoots(ClassLoader loader) {
        var roots = new ArrayList<URL>();
        try {
            var found = loader.getResources(PROFILES);
            while (found.hasMoreElements()) {
                roots.add(found.nextElement());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot list classpath:/" + PROFILES, e);
        }
        return roots;
    }

    private static List<String> documentNames(URL root) {
        var names = new TreeSet<String>();
        // A classpath directory (development and tests) lists its files; an archive (the shipped
        // binary) lists its entries. Any other protocol names no document the reader could read.
        if ("file".equals(root.getProtocol())) {
            addDirectoryNames(root, names);
        } else if ("jar".equals(root.getProtocol())) {
            addArchiveNames(root, names);
        }
        return List.copyOf(names);
    }

    private static void addDirectoryNames(URL root, TreeSet<String> names) {
        try (var entries = Files.list(directoryOf(root))) {
            entries.map(path -> path.getFileName().toString()).filter(ProfileDocumentReader::isDocument)
                    .forEach(names::add);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot list the profile documents below " + root, e);
        }
    }

    private static Path directoryOf(URL root) {
        try {
            return Path.of(root.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("cannot resolve the profile resource directory " + root, e);
        }
    }

    private static void addArchiveNames(URL root, TreeSet<String> names) {
        try {
            var connection = (JarURLConnection) root.openConnection();
            connection.setUseCaches(false);
            var prefix = connection.getEntryName() + "/";
            try (var archive = connection.getJarFile()) {
                archive.stream().map(JarEntry::getName).filter(name -> name.startsWith(prefix))
                        .map(name -> name.substring(prefix.length())).filter(ProfileDocumentReader::isDocument)
                        .forEach(names::add);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot list the profile documents in " + root, e);
        }
    }

    private static boolean isDocument(String name) {
        return name.endsWith(DOCUMENT_SUFFIX) && !name.contains("/");
    }

    private static Profile readDocument(ClassLoader loader, String name) {
        var resource = PROFILES + "/" + name;
        try (var input = loader.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("client profile document not found: " + resource);
            }
            var profile = parseDocument(input);
            if (!name.equals(profile.clientId() + DOCUMENT_SUFFIX)) {
                throw new IllegalStateException("the file name of a profile document is its client id: " + name
                        + " declares " + profile.clientId());
            }
            return profile;
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read the client profile document " + resource, e);
        }
    }

    private static void requireDistinctPrecedence(List<Profile> profiles) {
        var precedences = new TreeSet<Integer>();
        for (var profile : profiles) {
            if (!precedences.add(profile.detection().precedence())) {
                throw new IllegalStateException(
                        "two profile documents share the detection precedence " + profile.detection().precedence());
            }
        }
    }

    private static Profile parseProfile(JsonParser parser) throws IOException {
        requireObject(parser, "a client profile document");
        String clientId = null;
        Detection detection = null;
        Identity identity = null;
        while (parser.nextToken() == JsonToken.FIELD_NAME) {
            var field = parser.currentName();
            parser.nextToken();
            switch (field) {
                case "client_id" -> clientId = readText(parser, "client_id");
                case "detection" -> detection = parseDetection(parser);
                case "identity" -> identity = parseIdentity(parser);
                default -> parser.skipChildren();
            }
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("client_id is required");
        }
        if (detection == null) {
            throw new IllegalStateException("detection is required");
        }
        if (identity == null) {
            throw new IllegalStateException("identity is required");
        }
        return new Profile(clientId, detection, identity);
    }

    private static Detection parseDetection(JsonParser parser) throws IOException {
        requireObject(parser, "detection");
        Integer precedence = null;
        List<HostDetection.Signal> primarySignals = null;
        List<HostDetection.Signal> secondarySignals = null;
        while (parser.nextToken() == JsonToken.FIELD_NAME) {
            var field = parser.currentName();
            parser.nextToken();
            switch (field) {
                case "precedence" -> precedence = readInt(parser, "detection.precedence");
                case "primary_signals" -> primarySignals = parseSignals(parser);
                case "secondary_signals" -> secondarySignals = parseSignals(parser);
                default -> parser.skipChildren();
            }
        }
        if (precedence == null || precedence < 1) {
            throw new IllegalStateException("detection.precedence must be a positive integer");
        }
        if (primarySignals == null || secondarySignals == null) {
            throw new IllegalStateException("detection.primary_signals and detection.secondary_signals are required");
        }
        return new Detection(precedence, primarySignals, secondarySignals);
    }

    private static Identity parseIdentity(JsonParser parser) throws IOException {
        requireObject(parser, "identity");
        String sessionIdVariable = null;
        List<String> versionProbe = null;
        String versionPattern = null;
        String modelSource = null;
        Boolean provisional = null;
        var probeDeclared = false;
        var patternDeclared = false;
        while (parser.nextToken() == JsonToken.FIELD_NAME) {
            var field = parser.currentName();
            parser.nextToken();
            switch (field) {
                case "session_id_variable" -> sessionIdVariable = readText(parser, "identity.session_id_variable");
                case "version_probe" -> {
                    versionProbe = readStrings(parser, "identity.version_probe");
                    probeDeclared = true;
                }
                case "version_pattern" -> {
                    versionPattern = readText(parser, "identity.version_pattern");
                    patternDeclared = true;
                }
                case "model_source" -> modelSource = readText(parser, "identity.model_source");
                case "provisional" -> provisional = readBoolean(parser, "identity.provisional");
                default -> parser.skipChildren();
            }
        }
        if (!probeDeclared || !patternDeclared) {
            throw new IllegalStateException("identity.version_probe and identity.version_pattern are required");
        }
        if (modelSource == null || modelSource.isBlank()) {
            throw new IllegalStateException("identity.model_source is required");
        }
        if (provisional == null) {
            throw new IllegalStateException("identity.provisional is required");
        }
        if ((versionProbe == null) != (versionPattern == null)) {
            throw new IllegalStateException("identity.version_pattern is null if and only if version_probe is null");
        }
        return new Identity(sessionIdVariable, versionProbe, versionPattern, modelSource, provisional);
    }

    private static List<HostDetection.Signal> parseSignals(JsonParser parser) throws IOException {
        if (parser.currentToken() != JsonToken.START_ARRAY) {
            throw new IllegalStateException("detection signals must be an array");
        }
        var signals = new ArrayList<HostDetection.Signal>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            signals.add(parseSignal(parser));
        }
        return signals;
    }

    private static HostDetection.Signal parseSignal(JsonParser parser) throws IOException {
        requireObject(parser, "a signal");
        String variable = null;
        String equals = null;
        var equalsDeclared = false;
        while (parser.nextToken() == JsonToken.FIELD_NAME) {
            var field = parser.currentName();
            parser.nextToken();
            switch (field) {
                case "variable" -> variable = readText(parser, "signal.variable");
                case "equals" -> {
                    equals = readText(parser, "signal.equals");
                    equalsDeclared = true;
                }
                default -> parser.skipChildren();
            }
        }
        if (variable == null || variable.isBlank()) {
            throw new IllegalStateException("signal.variable is required");
        }
        if (!equalsDeclared) {
            throw new IllegalStateException("signal.equals is required, null if the presence suffices");
        }
        return new HostDetection.Signal(variable, equals);
    }

    private static List<String> readStrings(JsonParser parser, String field) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }
        if (parser.currentToken() != JsonToken.START_ARRAY) {
            throw new IllegalStateException(field + " must be an array or null");
        }
        var values = new ArrayList<String>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            if (parser.currentToken() != JsonToken.VALUE_STRING) {
                throw new IllegalStateException(field + " must hold strings");
            }
            values.add(parser.getText());
        }
        if (values.isEmpty()) {
            throw new IllegalStateException(field + " must not be empty");
        }
        return values;
    }

    private static String readText(JsonParser parser, String field) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }
        if (parser.currentToken() != JsonToken.VALUE_STRING) {
            throw new IllegalStateException(field + " must be a string or null");
        }
        return parser.getText();
    }

    private static int readInt(JsonParser parser, String field) throws IOException {
        if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
            throw new IllegalStateException(field + " must be an integer");
        }
        return parser.getIntValue();
    }

    private static boolean readBoolean(JsonParser parser, String field) throws IOException {
        var token = parser.currentToken();
        if (token != JsonToken.VALUE_TRUE && token != JsonToken.VALUE_FALSE) {
            throw new IllegalStateException(field + " must be a boolean");
        }
        return token == JsonToken.VALUE_TRUE;
    }

    private static void requireObject(JsonParser parser, String what) throws IOException {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            throw new IllegalStateException(what + " must be a JSON object");
        }
    }
}
