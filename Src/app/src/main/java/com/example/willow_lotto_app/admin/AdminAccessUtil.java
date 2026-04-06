package com.example.willow_lotto_app.admin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Checks emails against a hard-coded admin allow-list. */
public class AdminAccessUtil {

    // Hard-coded admin emails; keep in sync with Firebase test accounts you use.
    private static final Set<String> ADMIN_EMAILS = new HashSet<>(Arrays.asList(
            "admin1@gmail.com",
            "admin2@gmail.com",
            "admin3@gmail.com",
            "carrot@gmail.com",
            "daisy@gmail.com",
            "willowtestadmin@gmail.com"
    ));

    private AdminAccessUtil() {
    }

    /**
     * @param email email to check; may be null
     * @return true if trimmed, lowercased email is in the admin allow-list
     */
    public static boolean isAdminEmail(String email) {
        if (email == null) {
            return false;
        }
        return ADMIN_EMAILS.contains(email.trim().toLowerCase(Locale.ROOT));
    }

}
