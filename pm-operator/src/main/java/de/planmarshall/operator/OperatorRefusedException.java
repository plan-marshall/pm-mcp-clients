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
import java.util.Map;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.resource.ApiErrorCode;
import de.planmarshall.api.resource.ErrorTexts;

/**
 * The runtime answered an operator request with an unexpected status, or a local operator check
 * was refused.
 */
public class OperatorRefusedException extends IOException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final @Nullable String verb;
    private final int status;
    private final @Nullable ApiErrorCode code;
    private final @Nullable String rawCode;
    private final @Nullable String serverMessage;
    private final int exitCode;

    /**
     * @param verb     the verb
     * @param response the response
     */
    public OperatorRefusedException(String verb, HttpResponse response) {
        this(verb, response, parseRefusal(response));
    }

    private OperatorRefusedException(String verb, HttpResponse response, ParsedRefusal parsed) {
        super(defaultMessage(parsed.code(), parsed.serverMessage()));
        this.verb = verb;
        this.status = response.status();
        this.code = parsed.code();
        this.rawCode = parsed.rawCode();
        this.serverMessage = parsed.serverMessage();
        this.exitCode = 1;
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
        super(defaultMessage(code, serverMessage));
        this.verb = verb;
        this.status = code.status();
        this.code = code;
        this.rawCode = code.wire();
        this.serverMessage = serverMessage != null ? TerminalSanitizer.sanitizeLine(serverMessage).strip() : null;
        this.exitCode = 1;
    }

    private static String defaultMessage(@Nullable ApiErrorCode code, @Nullable String serverMessage) {
        if (code != null) {
            return ErrorTexts.title(code);
        }
        if (serverMessage != null && !serverMessage.isBlank()) {
            return "The runtime refused: " + serverMessage;
        }
        return ErrorTexts.title((ApiErrorCode) null);
    }

    private record ParsedRefusal(@Nullable ApiErrorCode code, @Nullable String rawCode, @Nullable String serverMessage) {
    }

    private record ExtractedFields(@Nullable String code, @Nullable String message) {
    }

    private static ParsedRefusal parseRefusal(HttpResponse response) {
        var body = response.bodyText();
        var extracted = extractFromJson(body);
        var parsedCode = extracted.code();
        var parsedMessage = extracted.message();

        if (parsedMessage == null && (parsedCode == null || ApiErrorCode.fromWireOrNull(parsedCode) == null)) {
            // A body that is no JSON object is its own message. A JSON object that names no message
            // carries only its code, and a code is a machine token, never a text for the operator.
            parsedMessage = body != null && body.trim().startsWith("{") ? null : body;
        }
        if (parsedCode == null && response.status() == 404) {
            parsedCode = ApiErrorCode.SCOPE_NOT_FOUND.wire();
        }
        ApiErrorCode resolvedCode = parsedCode != null ? ApiErrorCode.fromWireOrNull(parsedCode) : null;
        String sanitizedMessage = parsedMessage != null ? TerminalSanitizer.sanitizeLine(parsedMessage).strip() : null;
        return new ParsedRefusal(resolvedCode, parsedCode, sanitizedMessage);
    }

    private static ExtractedFields extractFromJson(@Nullable String body) {
        if (body == null || body.isBlank() || !body.trim().startsWith("{")) {
            return new ExtractedFields(null, null);
        }
        try {
            var parsed = JsonTree.parse(body);
            if (parsed instanceof Map<?, ?> map) {
                return extractFromMap(map);
            }
        } catch (IOException _) {
            // Not valid JSON
        }
        return new ExtractedFields(null, null);
    }

    private static ExtractedFields extractFromMap(Map<?, ?> map) {
        var errorObj = map.get("error");
        String parsedCode = null;
        if (errorObj instanceof Map<?, ?> errMap && errMap.get("code") instanceof String nestedCode) {
            parsedCode = nestedCode;
        } else if (errorObj instanceof String errStr) {
            parsedCode = errStr;
        }
        if (parsedCode == null && map.get("code") instanceof String flatCode) {
            parsedCode = flatCode;
        }
        // A message the body names is what the operator reads; a code never is.
        String parsedMessage = null;
        if (errorObj instanceof Map<?, ?> errMap && errMap.get("message") instanceof String nestedMessage) {
            parsedMessage = nestedMessage;
        }
        if (parsedMessage == null && map.get("message") instanceof String flatMessage) {
            parsedMessage = flatMessage;
        }
        if (parsedMessage == null && errorObj instanceof String errStr) {
            // The legacy form's string is its own text, which may be readable.
            parsedMessage = errStr;
        }
        return new ExtractedFields(parsedCode, parsedMessage);
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
}
