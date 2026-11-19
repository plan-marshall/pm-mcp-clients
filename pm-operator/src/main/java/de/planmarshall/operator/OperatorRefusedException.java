/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import java.io.IOException;
import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.resource.ApiErrorCode;
import de.planmarshall.api.resource.ErrorTexts;

/**
 * The runtime answered an operator request with an unexpected status, or a local operator check
 * was refused.
 * <p>
 * A refusal is one of four kinds, and the renderer says what each is: a refusal that carries an API
 * code, a refusal of this client or of the runtime's answer that carries no code at all (a local
 * condition), a 404 that names no code on a route the runtime does not know, and a body that is no
 * refusal this client can read at all. The kind decides the three lines; a runtime's message is a
 * detail of the first two, never the whole screen.
 */
public class OperatorRefusedException extends IOException {

    /** The longest body text a refusal carries; a longer body is no refusal of this API. */
    static final int MAX_MESSAGE_LENGTH = 200;

    /** The catalogue key of the prefix of an unknown refusal whose message the body carries. */
    static final String PREFIX_KEY = "operator.refusal.prefix";

    private static final Set<String> NON_ARGUMENTS = Set.of("error", "message", "code", "reason");
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_ERROR = "error";
    private static final String KEY_CODE = "code";

    /** What kind of refusal the exception carries. */
    enum Kind {
        /** A refusal of the API, with a code this client knows or a message beside an unknown one. */
        RUNTIME,
        /** A condition found on this machine, or in the runtime's own answer, carrying no code. */
        LOCAL,
        /** A 404 that names no code on a route the runtime does not know. */
        ROUTE_MISSING,
        /** A body that is no refusal this client can read: a page or a trace of another service. */
        UNREADABLE
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private final @Nullable String verb;
    private final int status;
    private final @Nullable ApiErrorCode code;
    private final @Nullable String rawCode;
    private final @Nullable String serverMessage;
    private final int exitCode;
    private final Kind kind;
    private final transient Map<String, String> arguments;

    /**
     * @param verb     the verb
     * @param response the response
     */
    public OperatorRefusedException(String verb, HttpResponse response) {
        this(verb, response, parseRefusal(response));
    }

    private OperatorRefusedException(String verb, HttpResponse response, ParsedRefusal parsed) {
        super(defaultMessage(parsed.kind(), parsed.code(), parsed.serverMessage()));
        this.verb = verb;
        this.status = response.status();
        this.code = parsed.code();
        this.rawCode = parsed.rawCode();
        this.serverMessage = parsed.serverMessage();
        this.exitCode = 1;
        this.kind = parsed.kind();
        this.arguments = parsed.arguments();
    }

    /**
     * @param message the refusal text, as the operator gate renders it
     */
    public OperatorRefusedException(String message) {
        this(message, 1);
    }

    /**
     * @param message  the refusal text
     * @param exitCode the process exit code
     */
    public OperatorRefusedException(String message, int exitCode) {
        super(TerminalSanitizer.sanitizeLine(message).strip());
        this.verb = null;
        this.status = 0;
        this.code = null;
        this.rawCode = null;
        this.serverMessage = TerminalSanitizer.sanitizeLine(message).strip();
        this.exitCode = exitCode;
        this.kind = Kind.LOCAL;
        this.arguments = Map.of();
    }

    /**
     * @param code the error code
     */
    public OperatorRefusedException(ApiErrorCode code) {
        this(null, code, null);
    }

    /**
     * @param verb the verb
     * @param code the error code
     */
    public OperatorRefusedException(@Nullable String verb, ApiErrorCode code) {
        this(verb, code, null);
    }

    /**
     * @param verb          the verb
     * @param code          the error code
     * @param serverMessage the message
     */
    public OperatorRefusedException(@Nullable String verb, ApiErrorCode code, @Nullable String serverMessage) {
        super(defaultMessage(Kind.RUNTIME, code, serverMessage));
        this.verb = verb;
        this.status = code.status();
        this.code = code;
        this.rawCode = code.wire();
        this.serverMessage = serverMessage != null ? TerminalSanitizer.sanitizeLine(serverMessage).strip() : null;
        this.exitCode = 1;
        this.kind = Kind.RUNTIME;
        this.arguments = Map.of();
    }

    private static String defaultMessage(Kind kind, @Nullable ApiErrorCode code, @Nullable String serverMessage) {
        if (code != null) {
            return ErrorTexts.title(code);
        }
        if (kind == Kind.ROUTE_MISSING) {
            return MessageCatalogue.format(null, "operator.refusal.route_missing.title", Map.of());
        }
        if (kind == Kind.UNREADABLE) {
            return MessageCatalogue.format(null, "operator.refusal.unreadable.title", Map.of());
        }
        if (serverMessage != null && !serverMessage.isBlank()) {
            return MessageCatalogue.format(null, PREFIX_KEY, Map.of()) + serverMessage;
        }
        return ErrorTexts.title((ApiErrorCode) null);
    }

    private record ParsedRefusal(@Nullable ApiErrorCode code, @Nullable String rawCode,
    @Nullable String serverMessage, Map<String, String> arguments, Kind kind) {
    }

    private record ExtractedFields(@Nullable String code, @Nullable String message, Map<String, String> arguments) {
    }

    /** How a body that is no JSON object reads, which decides whether it may be the message. */
    private enum BodyShape {
        /** No text at all. */
        BLANK,
        /** A page of a service between the client and the runtime: never the message. */
        MARKUP,
        /** A stack trace or a diagnostic dump: never the message. */
        TRACE,
        /** A plain sentence: the message, capped. */
        TEXT
    }

    private static ParsedRefusal parseRefusal(HttpResponse response) {
        var body = response.bodyText();
        var extracted = extractFromJson(body);
        var parsedCode = extracted.code();
        var parsedMessage = extracted.message();
        var shape = shapeOf(body);

        if (shape == BodyShape.MARKUP || shape == BodyShape.TRACE) {
            return new ParsedRefusal(null, null, null, Map.of(), Kind.UNREADABLE);
        }
        if (parsedCode == null && response.status() == 404 && !looksLikeJsonObject(body)) {
            // A route the runtime does not own answers with no name at all; it is never "this entry
            // does not exist".
            return new ParsedRefusal(null, null, null, Map.of(), Kind.ROUTE_MISSING);
        }
        if (parsedMessage == null && (parsedCode == null || ApiErrorCode.fromWireOrNull(parsedCode) == null)) {
            // A body that is no JSON object is never the message in full: a plain sentence is
            // capped, and a JSON object that names no message carries only its code.
            if (shape == BodyShape.BLANK) {
                parsedMessage = "";
            } else if (!looksLikeJsonObject(body)) {
                parsedMessage = cap(body);
            }
        }
        if (parsedCode == null && response.status() == 404) {
            parsedCode = ApiErrorCode.SCOPE_NOT_FOUND.wire();
        }
        var resolvedCode = parsedCode != null ? ApiErrorCode.fromWireOrNull(parsedCode) : null;
        var sanitizedMessage = parsedMessage != null ? TerminalSanitizer.sanitizeLine(parsedMessage).strip() : null;
        return new ParsedRefusal(resolvedCode, parsedCode, sanitizedMessage, extracted.arguments(), Kind.RUNTIME);
    }

    /**
     * @param body the body the runtime answered with
     * @return the body's shape, which decides whether it may be the message at all
     */
    private static BodyShape shapeOf(@Nullable String body) {
        if (body == null || body.isBlank()) {
            return BodyShape.BLANK;
        }
        var trimmed = body.strip();
        if (trimmed.startsWith("<") || trimmed.contains("</")) {
            return BodyShape.MARKUP;
        }
        if (containsStackTrace(trimmed)) {
            return BodyShape.TRACE;
        }
        return BodyShape.TEXT;
    }

    private static boolean containsStackTrace(String text) {
        if (text.contains("\tat ") || text.contains("\n    at ")) {
            return true;
        }
        for (var line : text.split("\n")) {
            var stripped = line.strip();
            if (stripped.endsWith("Exception") || stripped.contains("Exception:")
                                || stripped.endsWith("Error") || stripped.contains("Error:")) {
                return true;
            }
        }
        return false;
    }

    private static String cap(String body) {
        var stripped = body == null ? "" : TerminalSanitizer.sanitizeLine(body).strip();
        if (stripped.length() <= MAX_MESSAGE_LENGTH) {
            return stripped;
        }
        return stripped.substring(0, MAX_MESSAGE_LENGTH).strip() + "\u2026";
    }

    private static boolean looksLikeJsonObject(@Nullable String body) {
        return body != null && body.trim().startsWith("{");
    }

    private static ExtractedFields extractFromJson(@Nullable String body) {
        if (body == null || body.isBlank() || !body.trim().startsWith("{")) {
            return new ExtractedFields(null, null, Map.of());
        }
        try {
            var parsed = JsonTree.parse(body);
            if (parsed instanceof Map<?, ?> map) {
                return extractFromMap(map);
            }
        } catch (IOException _) {
            // Not valid JSON
        }
        return new ExtractedFields(null, null, Map.of());
    }

    private static ExtractedFields extractFromMap(Map<?, ?> map) {
        var errorObj = map.get(KEY_ERROR);
        String parsedCode = null;
        if (errorObj instanceof Map<?, ?> errMap && errMap.get(KEY_CODE) instanceof String nestedCode) {
            parsedCode = nestedCode;
        } else if (errorObj instanceof String errStr) {
            parsedCode = errStr;
        }
        if (parsedCode == null && map.get(KEY_CODE) instanceof String flatCode) {
            parsedCode = flatCode;
        }
        // A message the body names is what the operator reads; a code never is.
        String parsedMessage = null;
        if (errorObj instanceof Map<?, ?> errMap && errMap.get(KEY_MESSAGE) instanceof String nestedMessage) {
            parsedMessage = nestedMessage;
        }
        if (parsedMessage == null && map.get(KEY_MESSAGE) instanceof String flatMessage) {
            parsedMessage = flatMessage;
        }
        if (parsedMessage == null && errorObj instanceof String errStr) {
            // The legacy form's string is its own text, which may be readable.
            parsedMessage = errStr;
        }
        return new ExtractedFields(parsedCode, parsedMessage, argumentsOf(map));
    }

    /**
     * Collects the body's own scalar values as the arguments of its texts: the fix line of
     * {@code enrolment_tampered} names the project id the body carries, so the renderer needs it.
     *
     * @param map the body
     * @return the arguments, never {@code null}
     */
    private static Map<String, String> argumentsOf(Map<?, ?> map) {
        var arguments = new LinkedHashMap<String, String>();
        for (var entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key) || NON_ARGUMENTS.contains(key)) {
                continue;
            }
            var value = entry.getValue();
            if (value instanceof JsonNumber(String text)) {
                arguments.put(key, text);
            } else if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                arguments.put(key, String.valueOf(value));
            }
        }
        return Map.copyOf(arguments);
    }

    /**
     * @return the resolved error code, or {@code null}
     */
    public @Nullable ApiErrorCode code() {
        return code;
    }

    /**
     * @return the raw error code string, or {@code null}
     */
    public @Nullable String rawCode() {
        return rawCode;
    }

    /**
     * @return the sanitized server message, or {@code null}
     */
    public @Nullable String serverMessage() {
        return serverMessage;
    }

    /**
     * @return the HTTP status, or 0 for local refusals
     */
    public int status() {
        return status;
    }

    /**
     * @return the verb that was attempted, or {@code null}
     */
    public @Nullable String verb() {
        return verb;
    }

    /**
     * @return the process exit code
     */
    public int exitCode() {
        return exitCode;
    }

    /**
     * @return the kind of refusal, which decides the three lines the renderer prints
     */
    Kind kind() {
        return kind;
    }

    /**
     * @return the arguments the texts of the refusal name, never {@code null}
     */
    public Map<String, String> arguments() {
        return arguments != null ? arguments : Map.of();
    }
}
