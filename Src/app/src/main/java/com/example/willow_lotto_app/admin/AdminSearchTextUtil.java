package com.example.willow_lotto_app.admin;

import java.util.Locale;

/**
 * Normalizes admin search input for case-insensitive matching.
 */
public final class AdminSearchTextUtil {

    private AdminSearchTextUtil() {
    }

    public static String normalizeQuery(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

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
