/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.control;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.json.JsonFormatException;

/**
 * The review of an epic's register: the defaults of a review that names only itself, the open items
 * it shows, and the identifiers its codec requires.
 */
@DisplayName("ReviewResource")
@EnableGeneratorController
class ReviewResourceTest {

    private static String text() {
        return Generators.nonBlankStrings().next();
    }

    private static ReviewItemResource item(String id, String state) {
        return new ReviewItemResource(id, null, text(), List.of(), text(), null, state, null);
    }

    @Test
    @DisplayName("should read a review that names only itself as an open, empty findings review")
    void shouldDefaultAMinimalReview() throws Exception {
        var reviewId = text();
        var epicId = text();

        var review = ReviewResource.fromMap(Map.of("review_id", reviewId, "epic_id", epicId));

        assertAll(
                () -> assertEquals("findings", review.register()),
                () -> assertEquals("open", review.state()),
                () -> assertTrue(review.items().isEmpty()),
                () -> assertEquals(0, review.decided()),
                () -> assertEquals(0, review.total()),
                () -> assertNull(review.draftedPlan()),
                () -> assertFalse(review.toMap().containsKey("drafted_plan")),
                () -> assertFalse(review.toMap().containsKey("read_back")),
                () -> assertEquals(review, ReviewResource.fromJson(review.toJson())));
    }

    @Test
    @DisplayName("should replace a blank register and state and absent items of a constructed review")
    void shouldDefaultBlankMembers() {
        var blank = " ".repeat(Generators.integers(0, 3).next());

        var review = new ReviewResource(text(), text(), blank, null, 0, 0, blank, null, null);

        assertAll(
                () -> assertEquals("findings", review.register()),
                () -> assertEquals("open", review.state()),
                () -> assertTrue(review.items().isEmpty()));
    }

    @ParameterizedTest(name = "without {0}")
    @ValueSource(strings = {"review_id", "epic_id"})
    @DisplayName("should refuse a review without its id or its epic")
    void shouldRefuseAReviewWithoutItsIds(String missing) {
        var map = new HashMap<String, Object>(Map.of("review_id", text(), "epic_id", text()));
        map.remove(missing);

        assertThrows(JsonFormatException.class, () -> ReviewResource.fromMap(map));
    }

    @Test
    @DisplayName("should show only the undecided items as open and find an item by its id")
    void shouldFilterOpenItemsAndFindAnItem() {
        var decidedState = Arrays.stream(ReviewDisposition.values())
                .filter(disposition -> disposition != ReviewDisposition.OPEN)
                .findFirst().orElseThrow().value();
        var decided = item("F-0001", decidedState);
        var open = item("F-0002", ReviewDisposition.OPEN.value());

        var review = new ReviewResource(text(), text(), "findings", List.of(decided, open), 1, 2, "open", null,
                null);

        assertAll(
                () -> assertEquals(List.of(open), review.openItems()),
                () -> assertEquals(open, review.item("F-0002")),
                () -> assertNull(review.item("F-0003")));
    }
}
