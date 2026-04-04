package com.example.willow_lotto_app.admin;

import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.willow_lotto_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Shared admin UI guards (e.g. block deep links to moderation screens).
 */
public final class AdminUiHelper {

    private AdminUiHelper() {
    }

    /**
     * @return false if the activity was finished because the user is not an admin
     */
    public static boolean requireAdminOrFinish(AppCompatActivity activity) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !AdminAccessUtil.isAdminEmail(user.getEmail())) {
            Toast.makeText(activity, R.string.admin_permission_denied, Toast.LENGTH_SHORT).show();
            activity.finish();
            return false;
        }
        return true;
    }
}
