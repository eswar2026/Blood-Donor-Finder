package com.blooddonor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.blooddonor.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText etName, etEmail, etPhone,
            etPassword, etConfirmPassword, etCity, etState;
    private Spinner spinnerBloodGroup;
    private CheckBox cbIsDonor;
    private Button btnRegister;
    private ProgressBar progressBar;
    private TextView tvLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final String[] BLOOD_GROUPS = {
            "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        initViews();
        setupSpinner();

        btnRegister.setOnClickListener(v -> registerUser());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void initViews() {
        etName            = findViewById(R.id.et_name);
        etEmail           = findViewById(R.id.et_email);
        etPhone           = findViewById(R.id.et_phone);
        etPassword        = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etCity            = findViewById(R.id.et_city);
        etState           = findViewById(R.id.et_state);
        spinnerBloodGroup = findViewById(R.id.spinner_blood_group);
        cbIsDonor         = findViewById(R.id.cb_is_donor);
        btnRegister       = findViewById(R.id.btn_register);
        progressBar       = findViewById(R.id.progress_bar);
        tvLogin           = findViewById(R.id.tv_login);
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                BLOOD_GROUPS);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(adapter);
    }

    private void registerUser() {

        // ── Read all fields ──────────────────────────────────
        String name            = etName.getText().toString().trim();
        String email           = etEmail.getText().toString().trim();
        String phone           = etPhone.getText().toString().trim();
        String password        = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String city            = etCity.getText().toString().trim();
        String state           = etState.getText().toString().trim();
        String bloodGroup      = spinnerBloodGroup.getSelectedItem().toString();

        // ── Validation ───────────────────────────────────────
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(phone) || phone.length() != 10) {
            etPhone.setError("Enter a valid 10-digit phone number");
            etPhone.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(city)) {
            etCity.setError("City is required");
            etCity.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(state)) {
            etState.setError("State is required");
            etState.requestFocus();
            return;
        }

        setLoading(true);
        Log.d(TAG, "Starting registration for: " + email);

        // ── Step 1: Create Firebase Auth account ─────────────
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {

                    String uid = authResult.getUser().getUid();
                    Log.d(TAG, "Auth created. UID = " + uid);
                    Toast.makeText(this,
                            "Account created! Saving profile...",
                            Toast.LENGTH_SHORT).show();

                    // ── Step 2: Save user data to Firestore ──────
                    saveUserToFirestore(
                            uid, name, email, phone,
                            bloodGroup, city, state,
                            cbIsDonor.isChecked()
                    );
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e(TAG, "Auth failed: " + e.getMessage());
                    Toast.makeText(this,
                            "Registration failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void saveUserToFirestore(
            String uid,
            String name,
            String email,
            String phone,
            String bloodGroup,
            String city,
            String state,
            boolean isDonor) {

        Log.d(TAG, "Saving to Firestore. UID = " + uid);

        // Use HashMap — most reliable way to save to Firestore
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId",           uid);
        userData.put("name",             name);
        userData.put("email",            email);
        userData.put("phone",            phone);
        userData.put("bloodGroup",       bloodGroup);
        userData.put("city",             city);
        userData.put("state",            state);
        userData.put("address",          "");
        userData.put("latitude",         0.0);
        userData.put("longitude",        0.0);
        userData.put("available",        true);
        userData.put("donor",            isDonor);
        userData.put("totalDonations",   0);
        userData.put("lastDonationDate", "");
        userData.put("profileImageBase64", "");
        userData.put("fcmToken",         "");
        userData.put("createdAt",        System.currentTimeMillis());
        userData.put("updatedAt",        System.currentTimeMillis());

        db.collection("users")
                .document(uid)
                .set(userData)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Log.d(TAG, "Firestore save SUCCESS");
                    Toast.makeText(this,
                            "Welcome, " + name + "! Profile saved.",
                            Toast.LENGTH_LONG).show();
                    startActivity(
                            new Intent(this, MainActivity.class));
                    finishAffinity();

                })

                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e(TAG, "Firestore save FAILED: " + e.getMessage());

                    // Show the exact error so we know what went wrong
                    Toast.makeText(this,
                            "Profile save failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
        btnRegister.setText(loading ? "Please wait..." : "CREATE ACCOUNT");
    }
}
