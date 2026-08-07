package com.blooddonor.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.blooddonor.R;
import com.blooddonor.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yalantis.ucrop.UCrop;
import de.hdodenhof.circleimageview.CircleImageView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class EditProfileActivity extends AppCompatActivity {

    private CircleImageView ivProfilePhoto;
    private ImageView ivCameraOverlay;
    private MaterialButton btnChangePhoto, btnRemovePhoto, btnSave;
    private EditText etName, etPhone, etCity, etState,
            etAddress, etLastDonation;
    private Spinner spinnerBloodGroup;
    private Switch switchAvailable, switchIsDonor;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private User currentUser;

    // Tracks the state of the photo
    private String currentBase64 = "";     // what is currently saved
    private String newBase64 = "";         // what the user just picked/cropped
    private boolean photoRemoved = false;  // user tapped Remove Photo

    private final String[] BLOOD_GROUPS = {
            "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
    };

    // ── Launchers ─────────────────────────────────────────────

    // Step 1: Pick image from gallery
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) startCrop(uri);
                    });

    // Step 2: Receive cropped result from UCrop
    private final ActivityResultLauncher<Intent> cropLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null) {
                            Uri croppedUri = UCrop.getOutput(result.getData());
                            if (croppedUri != null) {
                                applyCroppedImage(croppedUri);
                            }
                        } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                            Toast.makeText(this,
                                    "Crop failed. Please try again.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Profile");
        }

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        initViews();
        setupSpinner();
        setupClickListeners();
        loadCurrentUser();
    }

    private void initViews() {
        ivProfilePhoto  = findViewById(R.id.iv_profile_photo);
        ivCameraOverlay = findViewById(R.id.iv_camera_overlay);
        btnChangePhoto  = findViewById(R.id.btn_change_photo);
        btnRemovePhoto  = findViewById(R.id.btn_remove_photo);
        btnSave         = findViewById(R.id.btn_save);
        etName          = findViewById(R.id.et_name);
        etPhone         = findViewById(R.id.et_phone);
        etCity          = findViewById(R.id.et_city);
        etState         = findViewById(R.id.et_state);
        etAddress       = findViewById(R.id.et_address);
        etLastDonation  = findViewById(R.id.et_last_donation);
        spinnerBloodGroup = findViewById(R.id.spinner_blood_group);
        switchAvailable = findViewById(R.id.switch_available);
        switchIsDonor   = findViewById(R.id.switch_is_donor);
        progressBar     = findViewById(R.id.progress_bar);
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, BLOOD_GROUPS);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // Tapping the photo or camera overlay opens the photo menu
        ivProfilePhoto.setOnClickListener(v -> showPhotoMenu());
        ivCameraOverlay.setOnClickListener(v -> showPhotoMenu());
        btnChangePhoto.setOnClickListener(v -> showPhotoMenu());

        // Remove photo
        btnRemovePhoto.setOnClickListener(v -> removePhoto());

        // Save profile
        btnSave.setOnClickListener(v -> saveProfile());
    }

    // ── Photo Menu ────────────────────────────────────────────

    private void showPhotoMenu() {
        boolean hasPhoto = !currentBase64.isEmpty()
                || !newBase64.isEmpty();

        String[] options = hasPhoto
                ? new String[]{"Choose from Gallery", "Remove Photo", "Cancel"}
                : new String[]{"Choose from Gallery", "Cancel"};

        new AlertDialog.Builder(this)
                .setTitle("Profile Photo")
                .setItems(options, (dialog, which) -> {
                    if (hasPhoto) {
                        if (which == 0) openGallery();
                        else if (which == 1) removePhoto();
                    } else {
                        if (which == 0) openGallery();
                    }
                })
                .show();
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    // ── Crop ─────────────────────────────────────────────────

    private void startCrop(Uri sourceUri) {
        // Create a temp file in cache for the cropped output
        File outputFile = new File(
                getCacheDir(), "cropped_profile_" + System.currentTimeMillis() + ".jpg");
        Uri destinationUri = Uri.fromFile(outputFile);

        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(true);          // circular crop overlay
        options.setShowCropFrame(false);
        options.setShowCropGrid(false);
        options.setCompressionQuality(80);
        options.setToolbarColor(ContextCompat.getColor(this, R.color.blood_red));
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.blood_red_dark));
        options.setToolbarWidgetColor(ContextCompat.getColor(this, R.color.white));
        options.setToolbarTitle("Crop Photo");
        options.setHideBottomControls(false);

        Intent cropIntent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)          // square = circle crop
                .withMaxResultSize(300, 300)    // max 300x300 pixels
                .withOptions(options)
                .getIntent(this);

        cropLauncher.launch(cropIntent);
    }

    private void applyCroppedImage(Uri croppedUri) {
        try {
            InputStream inputStream =
                    getContentResolver().openInputStream(croppedUri);
            if (inputStream == null) return;

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            // Convert to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);
            byte[] bytes = baos.toByteArray();
            newBase64 = Base64.encodeToString(bytes, Base64.DEFAULT);

            // Show in UI
            ivProfilePhoto.setImageBitmap(bitmap);
            photoRemoved = false;

            // Show remove button since user now has a photo
            btnRemovePhoto.setVisibility(View.VISIBLE);

            Toast.makeText(this,
                    "Photo cropped. Tap Save to apply.",
                    Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(this,
                    "Failed to process image: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ── Remove Photo ──────────────────────────────────────────

    private void removePhoto() {
        new AlertDialog.Builder(this)
                .setTitle("Remove Photo")
                .setMessage("Are you sure you want to remove your profile photo? Your profile will show a default person icon.")
                .setPositiveButton("Remove", (dialog, which) -> {
                    // Reset everything
                    newBase64     = "";
                    currentBase64 = "";
                    photoRemoved  = true;

                    // Show default person icon
                    ivProfilePhoto.setImageResource(R.drawable.ic_person);

                    // Hide remove button
                    btnRemovePhoto.setVisibility(View.GONE);

                    Toast.makeText(this,
                            "Photo removed. Tap Save to apply.",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Load Profile ──────────────────────────────────────────

    private void loadCurrentUser() {
        String uid = mAuth.getCurrentUser().getUid();
        setLoading(true);
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    setLoading(false);
                    currentUser = doc.toObject(User.class);
                    if (currentUser != null) populateFields();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Failed to load profile",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void populateFields() {
        etName.setText(currentUser.getName());
        etPhone.setText(currentUser.getPhone());
        etCity.setText(currentUser.getCity());
        etState.setText(currentUser.getState());
        etAddress.setText(currentUser.getAddress());
        etLastDonation.setText(currentUser.getLastDonationDate());
        switchAvailable.setChecked(currentUser.isAvailable());
        switchIsDonor.setChecked(currentUser.isDonor());

        // Set blood group spinner
        for (int i = 0; i < BLOOD_GROUPS.length; i++) {
            if (BLOOD_GROUPS[i].equals(currentUser.getBloodGroup())) {
                spinnerBloodGroup.setSelection(i);
                break;
            }
        }

        // Load existing profile photo
        String existingBase64 = currentUser.getProfileImageBase64();
        if (existingBase64 != null && !existingBase64.isEmpty()) {
            currentBase64 = existingBase64;
            try {
                byte[] decoded = Base64.decode(existingBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(
                        decoded, 0, decoded.length);
                ivProfilePhoto.setImageBitmap(bitmap);
                btnRemovePhoto.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                ivProfilePhoto.setImageResource(R.drawable.ic_person);
            }
        } else {
            currentBase64 = "";
            ivProfilePhoto.setImageResource(R.drawable.ic_person);
            btnRemovePhoto.setVisibility(View.GONE);
        }
    }

    // ── Save Profile ──────────────────────────────────────────

    private void saveProfile() {
        String name  = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String city  = etCity.getText().toString().trim();
        String state = etState.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name required"); return;
        }
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Phone required"); return;
        }
        if (TextUtils.isEmpty(city)) {
            etCity.setError("City required"); return;
        }
        if (TextUtils.isEmpty(state)) {
            etState.setError("State required"); return;
        }

        setLoading(true);

        String uid = mAuth.getCurrentUser().getUid();

        // Decide which Base64 to save
        String photoToSave;
        if (photoRemoved) {
            photoToSave = "";           // empty = no photo = default icon
        } else if (!newBase64.isEmpty()) {
            photoToSave = newBase64;    // user picked and cropped a new photo
        } else {
            photoToSave = currentBase64; // unchanged — keep existing
        }

        currentUser.setName(name);
        currentUser.setPhone(phone);
        currentUser.setCity(city);
        currentUser.setState(state);
        currentUser.setAddress(etAddress.getText().toString().trim());
        currentUser.setLastDonationDate(
                etLastDonation.getText().toString().trim());
        currentUser.setBloodGroup(
                spinnerBloodGroup.getSelectedItem().toString());
        currentUser.setAvailable(switchAvailable.isChecked());
        currentUser.setDonor(switchIsDonor.isChecked());
        currentUser.setProfileImageBase64(photoToSave);
        currentUser.setUpdatedAt(System.currentTimeMillis());

        db.collection("users").document(uid)
                .set(currentUser)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Profile updated successfully!",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Update failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}