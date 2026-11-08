/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.display;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.message.MessageCatalogue;

/**
 * The display layer of the terminal client: the one place that turns a server value into the word,
 * the title, the time and the duration the operator reads
 * ({@code doc/design/shared/texts-and-languages.adoc}).
 *
 * <p>It is the terminal's half of the one rule the web app renders through its own display layer.
 * A caller never prints a raw state id, a raw id without a title, an ISO string or a second
 * duration format of its own; every one of them goes through this class.
 *
 * <p>The words and the unit marks are catalogue texts of the area {@code common}
 * ({@code state.*} and {@code common.time.*}); the figures and the calendar data come from
 * {@code java.time} and {@link Locale}.
 */
public final class Display {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
    private static final DateTimeFormatter SECOND = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);

    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long MINUTES_PER_HOUR = 60L;
    private static final long HOURS_PER_DAY = 24L;
    private static final long DAYS_PER_MONTH = 30L;
    private static final long DAYS_PER_YEAR = 365L;

    /**
     * The three renderings of a moment the design defines
     * ({@code texts-and-languages.adoc#time}): the age a list shows, the moment itself to the
     * minute, and the clock to the second.
     */
    public enum TimeStyle {
        /** The distance to now in the unit that distance has ({@code 5 min ago}). */
        RELATIVE,
        /** The moment itself to the minute ({@code today 14:02}, {@code 12 Sep, 09:10}). */
        SHORT,
        /** The moment to the second ({@code 14:02:11}). */
        CLOCK
    }

    private Display() {
    }

    /**
     * Renders the display word of a server value through the shared {@code state.<kind>.<value>}
     * key.
     *
     * @param locale the interface locale
     * @param kind   the kind of value ({@code plan}, {@code task}, {@code worker}, ...)
     * @param value  the raw server value
     * @return the localized word
     * @throws IllegalArgumentException when the catalogue has no word for the value, so a raw state
     *                                  id can never reach the operator
     */
    public static String word(String locale, String kind, @Nullable String value) {
        if (kind == null || kind.isBlank() || value == null || value.isBlank()) {
            throw new IllegalArgumentException("A display word needs a kind and a value");
        }
        var key = "state." + kind + "." + value;
        if (!MessageCatalogue.hasKey(locale, key)) {
            throw new IllegalArgumentException("No display word for the server value " + key);
        }
        return MessageCatalogue.format(locale, key, Map.of());
    }

    /**
     * Renders the title of a value, and never its id alone: a value without a title is a defect of
     * the data, not a reason to fall back to the id.
     *
     * <p>A workflow unit id whose {@code unit.<id>} key exists renders that key's plain name; any
     * other value without a title is refused.
     *
     * @param locale the interface locale
     * @param title  the value's title, or {@code null}
     * @param id     the value's id, used for the refusal message and for a unit key
     * @return the title
     * @throws IllegalArgumentException when the value carries no title and no unit text, so a raw
     *                                  id can never reach the operator
     */
    public static String title(String locale, @Nullable String title, String id) {
        if (title != null && !title.isBlank()) {
            return title;
        }
        var unitKey = "unit." + id;
        if (MessageCatalogue.hasKey(locale, unitKey)) {
            return MessageCatalogue.format(locale, unitKey, Map.of());
        }
        throw new IllegalArgumentException("A value is never shown by its id alone: " + id);
    }

    /**
     * Renders a moment as the age a list shows, following the rule of
     * {@code texts-and-languages.adoc#time}: {@code just now}, {@code 5 min ago}, {@code 9 d ago},
     * {@code last month}, {@code 3 years ago}. It is the rendering {@code Display.time} gives by
     * default and the one an age in a list or a detail uses.
     *
     * @param locale  the interface locale
     * @param instant the moment, or {@code null}
     * @param clock   the clock "now" means
     * @return the rendered age, or an empty string for {@code null}
     */
    public static String time(String locale, @Nullable Instant instant, Clock clock) {
        if (instant == null) {
            return "";
        }
        return age(locale, Math.max(0L, Duration.between(instant, clock.instant()).toMillis()));
    }

    /**
     * Renders a moment in one of the three styles the design defines, so a detail can show the
     * moment itself and an activity stream can show the clock.
     *
     * @param locale  the interface locale
     * @param instant the moment, or {@code null}
     * @param style   the rendering the view needs
     * @param clock   the clock "now" means
     * @return the rendering, or an empty string for {@code null}
     */
    public static String time(String locale, @Nullable Instant instant, TimeStyle style, Clock clock) {
        if (instant == null) {
            return "";
        }
        var now = clock.instant();
        var zone = clock.getZone();
        return switch (style) {
            case RELATIVE -> time(locale, instant, clock);
            case CLOCK -> SECOND.format(instant.atZone(zone));
            case SHORT -> shortMoment(locale, instant, now, zone);
        };
    }

    /**
     * Renders a moment given as an ISO-8601 timestamp. A value that is not an instant is returned
     * as it arrived, so no caller writes a second parse of its own.
     *
     * @param locale the interface locale
     * @param iso    the ISO-8601 timestamp, or {@code null}
     * @param clock  the clock "now" means
     * @return the rendered age, an empty string for {@code null}, or the unchanged non-instant
     */
    public static String time(String locale, @Nullable String iso, Clock clock) {
        if (iso == null || iso.isBlank()) {
            return "";
        }
        try {
            return time(locale, Instant.parse(iso.trim()), clock);
        } catch (DateTimeParseException _) {
            return iso;
        }
    }

    /**
     * Renders a span as the design's three duration rows, with the unit marks of the interface
     * language ({@code s}, {@code min}, {@code h}, and {@code d} / {@code T}).
     *
     * @param locale  the interface locale
     * @param seconds the span in seconds, negative treated as zero
     * @return {@code 30 s}, {@code 25 min}, {@code 2 h 10 min}, or {@code 9 d 1 h}
     */
    public static String duration(String locale, long seconds) {
        long totalSeconds = Math.max(0L, seconds);
        if (totalSeconds < SECONDS_PER_MINUTE) {
            return MessageCatalogue.format(locale, "common.time.unit.second", Map.of("count", totalSeconds));
        }
        long minutes = totalSeconds / SECONDS_PER_MINUTE;
        if (minutes < MINUTES_PER_HOUR) {
            return MessageCatalogue.format(locale, "common.time.unit.minute", Map.of("count", minutes));
        }
        long hours = minutes / MINUTES_PER_HOUR;
        if (hours < HOURS_PER_DAY * 2) {
            long rest = minutes % MINUTES_PER_HOUR;
            return rest > 0L
                    ? MessageCatalogue.format(locale, "common.time.unit.hour_minute",
                    Map.of("hours", hours, "minutes", rest))
                    : MessageCatalogue.format(locale, "common.time.unit.hour", Map.of("count", hours));
        }
        long days = hours / HOURS_PER_DAY;
        long restHours = hours % HOURS_PER_DAY;
        return restHours > 0L
                ? MessageCatalogue.format(locale, "common.time.unit.day_hour",
                Map.of("days", days, "hours", restHours))
                : MessageCatalogue.format(locale, "common.time.unit.day", Map.of("count", days));
    }

    private static String age(String locale, long millis) {
        if (millis < Duration.ofMinutes(1).toMillis()) {
            return MessageCatalogue.format(locale, "common.time.just_now", Map.of());
        }
        long minutes = millis / Duration.ofMinutes(1).toMillis();
        if (minutes < MINUTES_PER_HOUR) {
            return MessageCatalogue.format(locale, "common.time.minutes", Map.of("count", minutes));
        }
        long hours = minutes / MINUTES_PER_HOUR;
        if (hours < HOURS_PER_DAY) {
            return MessageCatalogue.format(locale, "common.time.hours", Map.of("count", hours));
        }
        long days = hours / HOURS_PER_DAY;
        if (days < DAYS_PER_MONTH) {
            return MessageCatalogue.format(locale, "common.time.days", Map.of("count", days));
        }
        if (days < DAYS_PER_YEAR) {
            return MessageCatalogue.format(locale, "common.time.months", Map.of("count", days / DAYS_PER_MONTH));
        }
        return MessageCatalogue.format(locale, "common.time.years", Map.of("count", days / DAYS_PER_YEAR));
    }

    private static String shortMoment(String locale, Instant instant, Instant now, ZoneId zone) {
        var day = LocalDate.ofInstant(instant, zone);
        var today = LocalDate.ofInstant(now, zone);
        long distance = ChronoUnit.DAYS.between(day, today);
        var time = CLOCK.format(instant.atZone(zone));
        if (distance == 0L) {
            return MessageCatalogue.format(locale, "common.time.today", Map.of()) + " " + time;
        }
        if (distance == 1L) {
            return MessageCatalogue.format(locale, "common.time.yesterday", Map.of()) + " " + time;
        }
        if (distance > 1L && distance < 7L) {
            var weekday = DateTimeFormatter.ofPattern("EEE", javaLocale(locale))
                    .format(instant.atZone(zone));
            return weekday.replace(".", "") + " " + time;
        }
        var dayMonth = DateTimeFormatter.ofPattern("d MMM", javaLocale(locale))
                .format(instant.atZone(zone));
        return dayMonth + ", " + time;
    }

    private static Locale javaLocale(String locale) {
        return locale != null && locale.toLowerCase(Locale.ROOT).startsWith("de") ? Locale.GERMAN : Locale.ENGLISH;
    }
}
