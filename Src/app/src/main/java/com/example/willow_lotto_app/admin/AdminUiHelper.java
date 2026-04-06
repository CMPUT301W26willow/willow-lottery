package com.example.willow_lotto_app.admin;

import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.willow_lotto_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** Finishes the screen if the current user is not an allow-listed admin. */
public final class AdminUiHelper {

    private AdminUiHelper() {
    }

    /**
     * @param activity host activity to finish if the user is not an allow-listed admin
     * @return false if the activity was finished; true if the current user may proceed
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
