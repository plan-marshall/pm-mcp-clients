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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.resource.ApiErrorCode;
import de.planmarshall.api.resource.ErrorTexts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OperatorRefusedException")
class OperatorRefusedExceptionTest {

    @Test
    @DisplayName("constructors with ApiErrorCode populate code, rawCode, status and default message")
    void constructorsWithApiErrorCode() {
        var ex1 = new OperatorRefusedException(ApiErrorCode.SCOPE_NOT_FOUND);
        assertEquals(ApiErrorCode.SCOPE_NOT_FOUND, ex1.code());
        assertEquals(ApiErrorCode.SCOPE_NOT_FOUND.wire(), ex1.rawCode());
        assertEquals(404, ex1.status());
        assertNull(ex1.verb());
        assertNull(ex1.serverMessage());
        assertEquals(1, ex1.exitCode());
        assertEquals(ErrorTexts.title(ApiErrorCode.SCOPE_NOT_FOUND), ex1.getMessage());

        var ex2 = new OperatorRefusedException("plan", ApiErrorCode.SCOPE_NOT_FOUND);
        assertEquals("plan", ex2.verb());
        assertEquals(ApiErrorCode.SCOPE_NOT_FOUND, ex2.code());

        var ex3 = new OperatorRefusedException("plan", ApiErrorCode.SCOPE_NOT_FOUND, "custom detail");
        assertEquals("plan", ex3.verb());
        assertEquals(ApiErrorCode.SCOPE_NOT_FOUND, ex3.code());
        assertEquals("custom detail", ex3.serverMessage());
        assertEquals(ErrorTexts.title(ApiErrorCode.SCOPE_NOT_FOUND), ex3.getMessage());
    }

    @Test
    @DisplayName("constructors with message set serverMessage and default or explicit exitCode")
    void constructorsWithMessage() {
        var ex1 = new OperatorRefusedException("local refusal");
        assertNull(ex1.verb());
        assertNull(ex1.code());
        assertNull(ex1.rawCode());
        assertEquals(0, ex1.status());
        assertEquals(1, ex1.exitCode());
        assertEquals("local refusal", ex1.serverMessage());
        assertEquals("local refusal", ex1.getMessage());

        var ex2 = new OperatorRefusedException("denied", 4);
        assertEquals(4, ex2.exitCode());
        assertEquals("denied", ex2.serverMessage());
        assertEquals("denied", ex2.getMessage());
    }

    @Test
    @DisplayName("parseRefusal reads a 404 with no body and no code as a route the runtime does not know")
    void parseRefusal404EmptyBody() {
        var resp = new HttpResponse(404, "application/json", new byte[0]);
        var ex = new OperatorRefusedException("plan", resp);
        assertEquals("plan", ex.verb());
        assertEquals(404, ex.status());
        assertNull(ex.code());
        assertNull(ex.rawCode());
        assertEquals(1, ex.exitCode());
        assertTrue(ex.getMessage().contains("does not know the request"), ex.getMessage());
    }

    @Test
    @DisplayName("a 404 that names no code in a JSON object is a missing entry")
    void parseRefusal404JsonObjectWithoutCode() {
        var json = "{\"message\":\"The plan does not exist\"}";
        var resp = new HttpResponse(404, "application/json", json.getBytes(StandardCharsets.UTF_8));
        var ex = new OperatorRefusedException("plan", resp);
        assertEquals(ApiErrorCode.SCOPE_NOT_FOUND, ex.code());
        assertEquals("The plan does not exist", ex.serverMessage());
    }

    @Test
    @DisplayName("the arguments of the body are carried with the exception")
    void argumentsAreTheScalarsOfTheBody() {
        var json = "{\"error\":\"enrolment_tampered\",\"project_id\":\"project-a\",\"command\":\"accept\"}";
        var resp = new HttpResponse(409, "application/json", json.getBytes(StandardCharsets.UTF_8));
        var ex = new OperatorRefusedException("plan", resp);
        assertEquals("project-a", ex.arguments().get("project_id"));
        assertEquals("accept", ex.arguments().get("command"));
        assertFalse(ex.arguments().containsKey("error"));
    }

    @Test
    @DisplayName("a numeric argument of the body is carried too")
    void numericArgumentIsCarried() {
        var json = "{\"error\":\"dependency_unanswered\",\"part\":3}";
        var resp = new HttpResponse(409, "application/json", json.getBytes(StandardCharsets.UTF_8));
        var ex = new OperatorRefusedException("answer", resp);
        assertEquals(ApiErrorCode.DEPENDENCY_UNANSWERED, ex.code());
        assertTrue(ex.arguments().containsKey("part"), "arguments: " + ex.arguments());
        assertEquals(3, Integer.parseInt(ex.arguments().get("part").toString()));
    }

    @Test
    @DisplayName("parseRefusal handles nested error map with code and message")
    void parseRefusalNestedErrorMap() {
        var json = "{\"error\": {\"code\": \"scope_not_found\", \"message\": \"The plan does not exist\"}}";
        var resp = new HttpResponse(404, "application/json", json.getBytes(StandardCharsets.UTF_8));
        var ex = new OperatorRefusedException("plan", resp);
        assertEquals(ApiErrorCode.SCOPE_NOT_FOUND, ex.code());
        assertEquals("scope_not_found", ex.rawCode());
        assertEquals("The plan does not exist", ex.serverMessage());
        assertEquals(ErrorTexts.title(ApiErrorCode.SCOPE_NOT_FOUND), ex.getMessage());
    }

    @Test
    @DisplayName("parseRefusal handles flat map with code and message")
    void parseRefusalFlatMap() {
        var json = "{\"code\": \"scope_not_found\", \"message\": \"Plan missing\"}";
        var resp = new HttpResponse(400, "application/json", json.getBytes(StandardCharsets.UTF_8));
        var ex = new OperatorRefusedException("plan", resp);
        assertEquals(ApiErrorCode.SCOPE_NOT_FOUND, ex.code());
        assertEquals("scope_not_found", ex.rawCode());
        assertEquals("Plan missing", ex.serverMessage());
    }

    @Test
    @DisplayName("parseRefusal handles string error with unknown code")
    void parseRefusalStringErrorUnknown() {
        var json = "{\"error\": \"arbitrary server error\"}";
        var resp = new HttpResponse(500, "application/json", json.getBytes(StandardCharsets.UTF_8));
        var ex = new OperatorRefusedException("plan", resp);
        assertNull(ex.code());
        assertEquals("arbitrary server error", ex.rawCode());
        assertEquals("arbitrary server error", ex.serverMessage());
        assertTrue(ex.getMessage().contains("The runtime refused: arbitrary server error"));
    }

    @Test
    @DisplayName("parseRefusal handles string error with known code")
    void parseRefusalStringErrorKnown() {
        var json = "{\"error\": \"scope_not_found\"}";
        var resp = new HttpResponse(404, "application/json", json.getBytes(StandardCharsets.UTF_8));
        var ex = new OperatorRefusedException("plan", resp);
        assertEquals(ApiErrorCode.SCOPE_NOT_FOUND, ex.code());
        assertEquals("scope_not_found", ex.rawCode());
    }

    @Test
    @DisplayName("parseRefusal handles non-JSON response body")
    void parseRefusalNonJson() {
        var body = "plain text error";
        var resp = new HttpResponse(502, "text/plain", body.getBytes(StandardCharsets.UTF_8));
        var ex = new OperatorRefusedException("plan", resp);
        assertNull(ex.code());
        assertNull(ex.rawCode());
        assertEquals("plain text error", ex.serverMessage());
        assertTrue(ex.getMessage().contains("plain text error"));
    }

    @Test
    @DisplayName("defaultMessage uses fallback title when code is absent and serverMessage is blank")
    void defaultMessageFallback() {
        var resp = new HttpResponse(500, "application/json", null);
        var ex = new OperatorRefusedException("plan", resp);
        assertNull(ex.code());
        assertEquals("", ex.serverMessage());
        assertEquals(ErrorTexts.title((ApiErrorCode) null), ex.getMessage());
    }

    @Test
    @DisplayName("serverMessage is sanitized against ANSI sequences")
    void sanitizesServerMessage() {
        var json = "{\"error\": \"\\u001B[31mred error\\u001B[0m\"}";
        var resp = new HttpResponse(500, "application/json", json.getBytes(StandardCharsets.UTF_8));
        var ex = new OperatorRefusedException("plan", resp);
        assertEquals("red error", ex.serverMessage());
    }

    @Test
    @DisplayName("a blank body and a malformed JSON object are no refusal the body can name")
    void blankAndMalformedBodies() {
        var blank = new OperatorRefusedException("plan", new HttpResponse(503, "application/json",
                            "   ".getBytes(StandardCharsets.UTF_8)));
        var malformed = new OperatorRefusedException("plan", new HttpResponse(500, "application/json",
                            "{\"error\":".getBytes(StandardCharsets.UTF_8)));

        assertAll(
                            () -> assertEquals(ErrorTexts.title((ApiErrorCode) null), blank.getMessage()),
                            () -> assertEquals("", blank.serverMessage()),
                            () -> assertNull(malformed.code()),
                            () -> assertNull(malformed.serverMessage(), "a JSON body never reaches the operator"),
                            () -> assertEquals(ErrorTexts.title((ApiErrorCode) null), malformed.getMessage()));
    }

    @Test
    @DisplayName("an unknown code beside a message shows the message, never the raw wire token")
    void unknownCodeReadsTheMessage() {
        var json = "{\"error\": \"not_found\", \"message\": \"Question Q-999 not found.\"}";
        var resp = new HttpResponse(404, "application/json", json.getBytes(StandardCharsets.UTF_8));
        var ex = new OperatorRefusedException("answer Q-999", resp);

        assertAll(
                            () -> assertNull(ex.code()),
                            () -> assertEquals("not_found", ex.rawCode()),
                            () -> assertEquals("Question Q-999 not found.", ex.serverMessage()),
                            () -> assertEquals("The runtime refused: Question Q-999 not found.", ex.getMessage()));
    }

    @Test
    @DisplayName("a JSON object without a message shows no body at all")
    void jsonObjectWithoutMessageShowsNoBody() {
        var resp = new HttpResponse(500, "application/json", "{}".getBytes(StandardCharsets.UTF_8));
        var ex = new OperatorRefusedException("plan", resp);

        assertAll(
                            () -> assertNull(ex.serverMessage()),
                            () -> assertEquals(ErrorTexts.title((ApiErrorCode) null), ex.getMessage()));
    }
}
