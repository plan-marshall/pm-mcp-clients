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

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.resource.ApiErrorCode;
import de.planmarshall.api.resource.ErrorTexts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OperatorRefusedException")
@EnableGeneratorController
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
    @DisplayName("a local refusal carries its own text in every locale and exits 1")
    void constructorWithMessage() {
        var text = Generators.letterStrings(4, 12).next();

        var ex = new OperatorRefusedException(text);

        assertAll(
                            () -> assertNull(ex.verb()),
                            () -> assertNull(ex.code()),
                            () -> assertNull(ex.rawCode()),
                            () -> assertEquals(0, ex.status()),
                            () -> assertEquals(1, ex.exitCode()),
                            () -> assertEquals(text, ex.serverMessage()),
                            () -> assertEquals(text, ex.getMessage()),
                            () -> assertEquals(text, ex.message("de")));
    }

    @Test
    @DisplayName("a stack trace wrapped in a JSON message is no text for the operator")
    void jsonWrappedTraceIsNotPrinted() {
        var detail = Generators.letterStrings(4, 10).next();
        var tabTrace = "java.lang.IllegalStateException: " + detail + "\\n\\tat a.b.C.d(C.java:1)";
        var spaceTrace = "java.lang.IllegalStateException: " + detail + "\\n    at a.b.C.d(C.java:1)";

        var tab = refusal(500, "{\"message\":\"" + tabTrace + "\"}");
        var space = refusal(500, "{\"error\":\"" + spaceTrace + "\"}");

        assertAll(
                            () -> assertEquals(OperatorRefusedException.Kind.UNREADABLE, tab.kind()),
                            () -> assertNull(tab.serverMessage()),
                            () -> assertFalse(tab.getMessage().contains(detail), tab.getMessage()),
                            () -> assertEquals(OperatorRefusedException.Kind.UNREADABLE, space.kind()),
                            () -> assertNull(space.serverMessage()),
                            () -> assertFalse(space.getMessage().contains(detail), space.getMessage()));
    }

    @Test
    @DisplayName("a stack trace beside a known code keeps the code and drops the trace")
    void jsonWrappedTraceBesideAKnownCodeKeepsTheCode() {
        var detail = Generators.letterStrings(4, 10).next();
        var json = "{\"error\":\"internal_fault\",\"message\":\"java.lang.IllegalStateException: " + detail
                            + "\\n\\tat a.b.C.d(C.java:1)\"}";

        var ex = refusal(500, json);

        assertAll(
                            () -> assertEquals(ApiErrorCode.INTERNAL_FAULT, ex.code()),
                            () -> assertNull(ex.serverMessage()));
    }

    @Test
    @DisplayName("a plain sentence that starts with Error: is the message, not a trace")
    void plainErrorSentenceIsPrinted() {
        var sentence = "Error: select a configured " + Generators.letterStrings(4, 10).next() + ".";

        var ex = refusal(500, sentence);

        assertAll(
                            () -> assertEquals(OperatorRefusedException.Kind.RUNTIME, ex.kind()),
                            () -> assertEquals(sentence, ex.serverMessage()),
                            () -> assertEquals("The runtime refused: " + sentence, ex.getMessage()));
    }

    @Test
    @DisplayName("a plain body with a frame indented by four spaces is a trace")
    void plainBodyWithSpaceIndentedFrameIsATrace() {
        var detail = Generators.letterStrings(4, 10).next();

        var ex = refusal(500, "java.lang.IllegalStateException: " + detail + "\n    at a.b.C.d(C.java:1)");

        assertAll(
                            () -> assertEquals(OperatorRefusedException.Kind.UNREADABLE, ex.kind()),
                            () -> assertNull(ex.serverMessage()));
    }

    @Test
    @DisplayName("a JSON message and a body argument longer than the cap are cut")
    void jsonMessageAndArgumentAreCapped() {
        var longText = Generators.letterStrings(OperatorRefusedException.MAX_MESSAGE_LENGTH + 50,
                            OperatorRefusedException.MAX_MESSAGE_LENGTH + 150).next();
        var json = "{\"error\":\"enrolment_tampered\",\"message\":\"" + longText + "\",\"project_id\":\""
                            + longText + "\"}";

        var ex = refusal(409, json);

        var expected = longText.substring(0, OperatorRefusedException.MAX_MESSAGE_LENGTH) + "…";
        assertAll(
                            () -> assertEquals(expected, ex.serverMessage()),
                            () -> assertEquals(expected, ex.arguments().get("project_id")));
    }

    @Test
    @DisplayName("message(de) is German while getMessage() stays the English diagnostic")
    void messageFollowsTheLocale() {
        var detail = Generators.letterStrings(4, 12).next();
        var unknown = refusal(409, "{\"error\":\"not_found\",\"message\":\"" + detail + "\"}");
        var known = new OperatorRefusedException(ApiErrorCode.SCOPE_NOT_FOUND);

        assertAll(
                            () -> assertEquals("Die Runtime hat die Anfrage abgelehnt: " + detail, unknown.message("de")),
                            () -> assertEquals("The runtime refused: " + detail, unknown.getMessage()),
                            () -> assertEquals("Diesen Eintrag gibt es nicht", known.message("de")),
                            () -> assertEquals("This entry does not exist", known.getMessage()));
    }

    @Test
    @DisplayName("screenText localises a refusal and keeps the message of any other I/O failure")
    void screenTextLocalisesOnlyARefusal() {
        var text = Generators.letterStrings(4, 12).next();
        var refusal = new OperatorRefusedException(ApiErrorCode.SCOPE_NOT_FOUND);
        var other = new IOException(text);

        assertAll(
                            () -> assertEquals("Diesen Eintrag gibt es nicht", OperatorRefusedException.screenText(refusal, "de")),
                            () -> assertEquals("This entry does not exist", OperatorRefusedException.screenText(refusal, "en")),
                            () -> assertEquals(text, OperatorRefusedException.screenText(other, "de")),
                            () -> assertEquals("", OperatorRefusedException.screenText(new IOException(), "de")));
    }

    private static OperatorRefusedException refusal(int status, String body) {
        return new OperatorRefusedException("plan", new HttpResponse(status, "application/json",
                            body.getBytes(StandardCharsets.UTF_8)));
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
    @DisplayName("a 404 that names no code keeps the absent code and the message it carries")
    void parseRefusal404JsonObjectWithoutCode() {
        var detail = Generators.letterStrings(4, 12).next();
        var json = "{\"message\":\"" + detail + "\"}";
        var resp = new HttpResponse(404, "application/json", json.getBytes(StandardCharsets.UTF_8));
        var ex = new OperatorRefusedException("plan", resp);
        assertNull(ex.code(), "a code-less 404 must not be synthesized into scope_not_found");
        assertEquals(detail, ex.serverMessage());
    }

    @Test
    @DisplayName("the arguments of the body are carried with the exception")
    void argumentsAreTheScalarsOfTheBody() {
        var project = Generators.letterStrings(4, 10).next();
        var command = Generators.letterStrings(4, 10).next();
        var json = "{\"error\":\"enrolment_tampered\",\"project_id\":\"" + project + "\","
                            + "\"command\":\"" + command + "\"}";
        var resp = new HttpResponse(409, "application/json", json.getBytes(StandardCharsets.UTF_8));
        var ex = new OperatorRefusedException("plan", resp);
        assertEquals(project, ex.arguments().get("project_id"));
        assertEquals(command, ex.arguments().get("command"));
        assertFalse(ex.arguments().containsKey("error"));
    }

    @Test
    @DisplayName("a 404 whose body is no JSON object is a route the runtime does not know")
    void unparsableBodyOn404IsRouteMissing() {
        var body = "{" + Generators.letterStrings(4, 10).next();
        var resp = new HttpResponse(404, "application/json", body.getBytes(StandardCharsets.UTF_8));
        var ex = new OperatorRefusedException("plan", resp);

        assertAll(
                            () -> assertNull(ex.code()),
                            () -> assertTrue(ex.getMessage().contains("does not know the request"), ex.getMessage()));
    }

    @Test
    @DisplayName("a JSON refusal whose message names an Error or a tag is still its own message")
    void jsonMessageWithAnErrorWordIsKept() {
        var detail = Generators.letterStrings(4, 10).next();
        var json = "{\"error\":\"internal_fault\",\"message\":\"Error: " + detail + "</value>\"}";
        var resp = new HttpResponse(500, "application/json", json.getBytes(StandardCharsets.UTF_8));
        var ex = new OperatorRefusedException("plan", resp);

        assertAll(
                            () -> assertEquals(ApiErrorCode.INTERNAL_FAULT, ex.code()),
                            () -> assertEquals("Error: " + detail + "</value>", ex.serverMessage()));
    }

    @Test
    @DisplayName("a numeric argument of the body is carried too")
    void numericArgumentIsCarried() {
        var part = Generators.integers(2, 9).next();
        var json = "{\"error\":\"dependency_unanswered\",\"part\":" + part + "}";
        var resp = new HttpResponse(409, "application/json", json.getBytes(StandardCharsets.UTF_8));
        var ex = new OperatorRefusedException("answer", resp);
        assertEquals(ApiErrorCode.DEPENDENCY_UNANSWERED, ex.code());
        assertTrue(ex.arguments().containsKey("part"), "arguments: " + ex.arguments());
        assertEquals(String.valueOf(part), ex.arguments().get("part"));
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
