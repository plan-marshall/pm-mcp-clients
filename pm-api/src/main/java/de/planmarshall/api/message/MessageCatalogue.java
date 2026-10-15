/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.message;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified internationalisation message catalogue for operator clients (CLI and web app).
 * <p>
 * Loads and caches {@code classpath:/messages/<area>.<locale>.properties} resource files in UTF-8.
 */
public final class MessageCatalogue {

    /**
     * Supported locales, with {@code en} as the complete base.
     */
    public static final Set<String> SUPPORTED_LOCALES = Set.of("en", "de");

    /**
     * Supported functional areas dividing the message catalogue.
     */
    public static final List<String> SUPPORTED_AREAS = List.of(
            "common",
            "pairing",
            "onboarding",
            "settings",
            "designer",
            "workspace"
    );

    private static final String DEFAULT_LOCALE = "en";
    private static final Map<String, Map<String, String>> CACHE = new ConcurrentHashMap<>();

    private MessageCatalogue() {
        // Utility class
    }

    /**
     * Normalizes the given locale to a supported locale (defaults to "en").
     *
     * @param locale the locale to check
     * @return supported locale string
     */
    public static String normalizeLocale(String locale) {
        if (locale == null || !SUPPORTED_LOCALES.contains(locale.toLowerCase())) {
            return DEFAULT_LOCALE;
        }
        return locale.toLowerCase();
    }

    /**
     * Returns all message key-value pairs for a given locale and area.
     * <p>
     * If {@code area} is {@code "all"} or {@code null}, merges all supported areas.
     *
     * @param locale the requested locale ("en", "de")
     * @param area   the requested area, or "all"
     * @return unmodifiable map of message keys to translated text
     */
    public static Map<String, String> getMessages(String locale, String area) {
        String effectiveLocale = normalizeLocale(locale);
        String effectiveArea = area == null || area.isBlank() || "all".equalsIgnoreCase(area) ? "all" : area.toLowerCase();
        if (!"all".equals(effectiveArea) && !SUPPORTED_AREAS.contains(effectiveArea)) {
            return Map.of();
        }

        return CACHE.computeIfAbsent(effectiveArea + "." + effectiveLocale, _ -> {
            if ("all".equals(effectiveArea)) {
                var merged = new LinkedHashMap<String, String>();
                for (String a : SUPPORTED_AREAS) {
                    merged.putAll(loadAreaMessages(effectiveLocale, a));
                }
                return Collections.unmodifiableMap(merged);
            }
            return Collections.unmodifiableMap(loadAreaMessages(effectiveLocale, effectiveArea));
        });
    }

    /**
     * Returns a metadata map describing available locales and areas.
     *
     * @return map with "locales" and "areas" lists
     */
    public static Map<String, Object> getAvailableLocalesAndAreas() {
        return Map.of(
                "locales", SUPPORTED_LOCALES.stream().sorted().toList(),
                "areas", SUPPORTED_AREAS
        );
    }

    /**
     * Formats a message template by substituting named parameters in {@code {param}} format.
     *
     * @param locale the locale
     * @param key    the message key
     * @param params parameter map (optional)
     * @return formatted message or the key if not found
     */
    public static String format(String locale, String key, Map<String, ?> params) {
        String effectiveLocale = normalizeLocale(locale);
        Map<String, String> all = getMessages(effectiveLocale, "all");
        String template = all.get(key);
        if (template == null) {
            // Fall back to default locale
            template = getMessages(DEFAULT_LOCALE, "all").getOrDefault(key, key);
        }
        if (params == null || params.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", Objects.toString(entry.getValue()));
        }
        return result;
    }

    private static Map<String, String> loadAreaMessages(String locale, String area) {
        String resourcePath = "/messages/" + area + "." + locale + ".properties";
        var is = MessageCatalogue.class.getResourceAsStream(resourcePath);
        if (is == null) {
            return Collections.emptyMap();
        }

        var props = new Properties();
        try (var reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load message resource: " + resourcePath, e);
        }

        var map = new LinkedHashMap<String, String>();
        for (String key : props.stringPropertyNames()) {
            map.put(key, props.getProperty(key));
        }
        return map;
    }
}
