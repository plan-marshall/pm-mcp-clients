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
            "entry",
            "onboarding",
            "settings",
            "designer",
            "workspace",
            "devices",
            "overview",
            "reports",
            "decisions",
            "system",
            "projects",
            "plan_view",
            "epic_view",
            "control",
            "start"
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
        if (locale == null || locale.isBlank()) {
            return DEFAULT_LOCALE;
        }
        String clean = locale.trim().toLowerCase();
        if (SUPPORTED_LOCALES.contains(clean)) {
            return clean;
        }
        int dash = clean.indexOf('-');
        if (dash > 0) {
            String lang = clean.substring(0, dash);
            if (SUPPORTED_LOCALES.contains(lang)) {
                return lang;
            }
        }
        int underscore = clean.indexOf('_');
        if (underscore > 0) {
            String lang = clean.substring(0, underscore);
            if (SUPPORTED_LOCALES.contains(lang)) {
                return lang;
            }
        }
        return DEFAULT_LOCALE;
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
     * Checks if a message key exists in the default locale.
     *
     * @param key the message key
     * @return {@code true} if present
     */
    public static boolean hasKey(String key) {
        return hasKey(DEFAULT_LOCALE, key);
    }

    /**
     * Checks if a message key exists in the specified locale.
     *
     * @param locale the locale
     * @param key    the message key
     * @return {@code true} if present
     */
    public static boolean hasKey(String locale, String key) {
        if (key == null) return false;
        String effectiveLocale = normalizeLocale(locale);
        return getMessages(effectiveLocale, "all").containsKey(key);
    }

    /**
     * Retrieves a message template in the default locale without parameter substitution.
     *
     * @param key the message key
     * @return the message string
     */
    public static String get(String key) {
        return format(DEFAULT_LOCALE, key, Map.of());
    }

    /**
     * Formats a message template in the default locale.
     *
     * @param key    the message key
     * @param params parameter map
     * @return formatted message
     */
    public static String format(String key, Map<String, ?> params) {
        return format(DEFAULT_LOCALE, key, params);
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
        if (template.indexOf('{') < 0) {
            return template;
        }

        return evaluateTemplate(template, params != null ? params : Map.of());
    }

    private static String evaluateTemplate(String template, Map<String, ?> params) {
        var sb = new StringBuilder();
        int i = 0;
        int len = template.length();
        while (i < len) {
            int openIdx = template.indexOf('{', i);
            if (openIdx < 0) {
                sb.append(template.substring(i));
                break;
            }
            sb.append(template, i, openIdx);
            int closeIdx = findMatchingBrace(template, openIdx);
            if (closeIdx < 0) {
                sb.append(template.substring(openIdx));
                break;
            }
            String expr = template.substring(openIdx + 1, closeIdx).trim();
            int firstComma = expr.indexOf(',');
            if (firstComma < 0) {
                Object val = params.get(expr);
                sb.append(val != null ? val : "{" + expr + "}");
            } else {
                String varName = expr.substring(0, firstComma).trim();
                int secondComma = expr.indexOf(',', firstComma + 1);
                if (secondComma < 0) {
                    Object val = params.get(varName);
                    sb.append(val != null ? val : "{" + expr + "}");
                } else {
                    String type = expr.substring(firstComma + 1, secondComma).trim().toLowerCase();
                    String body = expr.substring(secondComma + 1).trim();
                    Object rawVal = params.get(varName);
                    if ("select".equals(type)) {
                        String selector = rawVal != null ? rawVal.toString() : "";
                        String selected = evaluateSelect(body, selector);
                        sb.append(evaluateTemplate(selected, params));
                    } else if ("plural".equals(type)) {
                        long count = parseCount(rawVal);
                        String selected = evaluatePlural(body, count);
                        selected = selected.replace("#", String.valueOf(count));
                        sb.append(evaluateTemplate(selected, params));
                    } else {
                        Object val = params.get(varName);
                        sb.append(val != null ? val : "{" + expr + "}");
                    }
                }
            }
            i = closeIdx + 1;
        }
        return sb.toString();
    }

    private static int findMatchingBrace(String text, int start) {
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String evaluateSelect(String body, String selector) {
        Map<String, String> cases = parseIcuCases(body);
        if (cases.containsKey(selector)) {
            return cases.get(selector);
        }
        return cases.getOrDefault("other", "");
    }

    private static String evaluatePlural(String body, long count) {
        Map<String, String> cases = parseIcuCases(body);
        String exactKey = "=" + count;
        if (cases.containsKey(exactKey)) {
            return cases.get(exactKey);
        }
        if (count == 1 && cases.containsKey("one")) {
            return cases.get("one");
        }
        return cases.getOrDefault("other", "");
    }

    private static Map<String, String> parseIcuCases(String body) {
        var result = new LinkedHashMap<String, String>();
        int i = 0;
        int len = body.length();
        while (i < len) {
            while (i < len && Character.isWhitespace(body.charAt(i))) {
                i++;
            }
            if (i >= len) {
                break;
            }
            int braceOpen = body.indexOf('{', i);
            if (braceOpen < 0) {
                break;
            }
            String caseName = body.substring(i, braceOpen).trim();
            int braceClose = findMatchingBrace(body, braceOpen);
            if (braceClose < 0) {
                break;
            }
            String caseBody = body.substring(braceOpen + 1, braceClose);
            result.put(caseName, caseBody);
            i = braceClose + 1;
        }
        return result;
    }

    private static long parseCount(Object val) {
        if (val instanceof Number n) {
            return n.longValue();
        }
        if (val != null) {
            try {
                return Long.parseLong(val.toString().trim());
            } catch (NumberFormatException _) {
                // fallback to zero
            }
        }
        return 0L;
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
