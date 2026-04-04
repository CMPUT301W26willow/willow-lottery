package com.example.willow_lotto_app.admin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Utility class for determining whether a signed-in user has administrator access.
 * <p>
 * Responsibilities:
 * - Stores the hard-coded administrator email allow-list.
 * - Provides helper methods used by ProfileActivity and admin screens.
 * <p>
 * Current admin emails:
 * - admin1@gmail.com
 * - admin2@gmail.com
 * - admin3@gmail.com
 * - carrot@gmail.com
 * - willowtestadmin@gmail.com (dedicated test admin — create this user in Firebase Auth)
 */

public class AdminAccessUtil {
    /**
     * Hard-coded administrator email addresses.
     */
    private static final Set<String> ADMIN_EMAILS = new HashSet<>(Arrays.asList(
            "admin1@gmail.com",
            "admin2@gmail.com",
            "admin3@gmail.com",
            "carrot@gmail.com",
            "willowtestadmin@gmail.com"
    ));

    /**
     * Private constructor because this is a static utility class.
     */
    private AdminAccessUtil() {
    }

    /**
     * Returns true if the given email belongs to a hard-coded administrator.
     *
     * @param email email address to check
     * @return true if the email is in the admin allow-list
     */
    public static boolean isAdminEmail(String email) {
        if (email == null) {
            return false;
        }
        return ADMIN_EMAILS.contains(email.trim().toLowerCase(Locale.ROOT));
    }

}
