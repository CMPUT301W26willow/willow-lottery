package com.example.willow_lotto_app.admin;

import java.util.Locale;

/** Normalizes search text and does case-insensitive substring checks. */
public final class AdminSearchTextUtil {

    private AdminSearchTextUtil() {
    }

    /**
     * @param raw raw search input; may be null
     * @return trimmed lowercased string, or empty string if {@code raw} is null
     */
    public static String normalizeQuery(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * @param haystack text to search; may be null
     * @param needleLower substring to find, already normalized (e.g. via {@link #normalizeQuery(String)})
     * @return true if {@code needleLower} is empty, or if {@code haystack} contains {@code needleLower} case-insensitively
     */
    public static boolean containsNormalized(String haystack, String needleLower) {
        if (needleLower.isEmpty()) {
            return true;
        }
        if (haystack == null || haystack.isEmpty()) {
            return false;
        }
        return haystack.toLowerCase(Locale.ROOT).contains(needleLower);
    }
}
