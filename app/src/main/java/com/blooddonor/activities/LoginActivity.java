package com.blooddonor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.blooddonor.R;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> loginUser());

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            // Check if email field is empty
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Please enter your email address first");
                etEmail.requestFocus();
                Toast.makeText(this,
                        "Please enter your email address in the email field first",
                        Toast.LENGTH_LONG).show();
                return;
            }

            // Check basic email format
            if (!android.util.Patterns.EMAIL_ADDRESS
                    .matcher(email).matches()) {
                etEmail.setError("Please enter a valid email address");
                etEmail.requestFocus();
                return;
            }

            // Show loading
            progressBar.setVisibility(View.VISIBLE);

            mAuth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused -> {
                        progressBar.setVisibility(View.GONE);

                        // Show a clear dialog so user knows what to do
                        new android.app.AlertDialog.Builder(
                                LoginActivity.this)
                                .setTitle("Password Reset Email Sent ✅")
                                .setMessage(
                                        "A password reset link has been sent to:\n\n"
                                                + email + "\n\n"
                                                + "Steps to reset:\n"
                                                + "1. Open your email inbox\n"
                                                + "2. Look for email from Firebase\n"
                                                + "3. Click the reset link\n"
                                                + "4. Set a new password\n"
                                                + "5. Come back and login\n\n"
                                                + "Check your spam/junk folder if you don't see it.")
                                .setPositiveButton("OK", null)
                                .show();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);

                        // Show the exact error with helpful message
                        String errorMsg = e.getMessage();
                        String userFriendlyMsg;

                        if (errorMsg != null
                                && errorMsg.contains(
                                "no user record")) {
                            userFriendlyMsg =
                                    "No account found with this email address.\n\n"
                                            + "Please check the email you entered or "
                                            + "register a new account.";
                        } else if (errorMsg != null
                                && errorMsg.contains("invalid")) {
                            userFriendlyMsg =
                                    "The email address format is invalid.\n"
                                            + "Please enter a valid email.";
                        } else if (errorMsg != null
                                && errorMsg.contains("network")) {
                            userFriendlyMsg =
                                    "No internet connection.\n"
                                            + "Please check your connection and try again.";
                        } else {
                            userFriendlyMsg =
                                    "Failed to send reset email.\n\n"
                                            + "Error: " + errorMsg;
                        }

                        new android.app.AlertDialog.Builder(
                                LoginActivity.this)
                                .setTitle("Password Reset Failed ❌")
                                .setMessage(userFriendlyMsg)
                                .setPositiveButton("OK", null)
                                .show();
                    });
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) { etEmail.setError("Email is required"); return; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Password is required"); return; }
        if (password.length() < 6) { etPassword.setError("Min 6 characters"); return; }

        setLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    setLoading(false);
                    String uid =
                            authResult.getUser().getUid();

                    // Save pending FCM token if exists
                    String pending =
                            getSharedPreferences(
                                    "fcm_prefs", MODE_PRIVATE)
                                    .getString("pending_token", null);

                    if (pending != null) {
                        com.google.firebase.firestore
                                .FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .update("fcmToken", pending)
                                .addOnSuccessListener(v ->
                                        getSharedPreferences(
                                                "fcm_prefs", MODE_PRIVATE)
                                                .edit()
                                                .remove("pending_token")
                                                .apply());
                    } else {
                        // Get fresh token
                        com.google.firebase.messaging
                                .FirebaseMessaging.getInstance()
                                .getToken()
                                .addOnSuccessListener(token -> {
                                    if (token != null) {
                                        com.google.firebase.firestore
                                                .FirebaseFirestore
                                                .getInstance()
                                                .collection("users")
                                                .document(uid)
                                                .update("fcmToken", token);
                                    }
                                });
                    }

                    startActivity(new Intent(
                            LoginActivity.this,
                            MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String error = e.getMessage();
                    if (error != null && error.contains("password")) {
                        etPassword.setError("Incorrect password");
                        etPassword.requestFocus();
                    } else if (error != null && error.contains("user")) {
                        etEmail.setError("Account not found");
                        etEmail.requestFocus();
                    } else {
                        Toast.makeText(this, "Login failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }
}
