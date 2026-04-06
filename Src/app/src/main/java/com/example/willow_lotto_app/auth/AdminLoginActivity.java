package com.example.willow_lotto_app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.admin.AdminAccessUtil;
import com.example.willow_lotto_app.admin.AdminDashboardActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Sample admin entry from the main login screen: email + password via Firebase Auth,
 * then opens {@link AdminDashboardActivity} only if the account is on the allow-list.
 */
public class AdminLoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextInputEditText emailEdit;
    private TextInputEditText passwordEdit;
    private MaterialButton adminAccessButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        mAuth = FirebaseAuth.getInstance();

        emailEdit = findViewById(R.id.admin_email_edit);
        passwordEdit = findViewById(R.id.admin_password_edit);
        adminAccessButton = findViewById(R.id.adminAccessButton);

        Button backButton = findViewById(R.id.adminBackButton);
        backButton.setOnClickListener(v -> {
            startActivity(new Intent(AdminLoginActivity.this, LoginActivity.class));
            finish();
        });

        adminAccessButton.setOnClickListener(v -> attemptAdminSignIn());
    }

    private void attemptAdminSignIn() {
        String email = emailEdit.getText() != null ? emailEdit.getText().toString().trim() : "";
        String password = passwordEdit.getText() != null ? passwordEdit.getText().toString() : "";

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.admin_login_missing_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        adminAccessButton.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    adminAccessButton.setEnabled(true);
                    if (!task.isSuccessful()) {
                        Toast.makeText(this, R.string.admin_login_failed, Toast.LENGTH_LONG).show();
                        return;
                    }
                    String signedInEmail = mAuth.getCurrentUser() != null
                            ? mAuth.getCurrentUser().getEmail()
                            : null;
                    if (!AdminAccessUtil.isAdminEmail(signedInEmail)) {
                        mAuth.signOut();
                        Toast.makeText(this, R.string.admin_permission_denied, Toast.LENGTH_LONG).show();
                        return;
                    }
                    Intent intent = new Intent(AdminLoginActivity.this, AdminDashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
    }
}
