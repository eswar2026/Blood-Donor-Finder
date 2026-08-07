package com.blooddonor.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.blooddonor.R;
import com.blooddonor.models.User;
import com.google.firebase.firestore.FirebaseFirestore;
import de.hdodenhof.circleimageview.CircleImageView;

public class DonorProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "user_id";

    private CircleImageView ivProfilePhoto;
    private TextView tvName, tvBloodGroup, tvCity, tvState, tvPhone,
            tvTotalDonations, tvLastDonation, tvAvailability;
    private Button btnCall, btnSMS, btnWhatsApp;
    private ProgressBar progressBar;
    private View cardContact;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donor_profile);

        db = FirebaseFirestore.getInstance();
        initViews();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Donor Profile");
        }
        String userId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (userId != null) loadDonorProfile(userId);
    }

    private void initViews() {
        ivProfilePhoto = findViewById(R.id.iv_profile_photo);
        tvName = findViewById(R.id.tv_name);
        tvBloodGroup = findViewById(R.id.tv_blood_group);
        tvCity = findViewById(R.id.tv_city);
        tvState = findViewById(R.id.tv_state);
        tvPhone = findViewById(R.id.tv_phone);
        tvTotalDonations = findViewById(R.id.tv_total_donations);
        tvLastDonation = findViewById(R.id.tv_last_donation);
        tvAvailability = findViewById(R.id.tv_availability);
        btnCall = findViewById(R.id.btn_call);
        btnSMS = findViewById(R.id.btn_sms);
        btnWhatsApp = findViewById(R.id.btn_whatsapp);
        progressBar = findViewById(R.id.progress_bar);
        cardContact = findViewById(R.id.card_contact);
    }

    private void loadDonorProfile(String userId) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    progressBar.setVisibility(View.GONE);
                    User user = doc.toObject(User.class);
                    if (user != null) populateUI(user);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void populateUI(User user) {
        tvName.setText(user.getName());
        tvBloodGroup.setText(user.getBloodGroup());
        tvCity.setText(user.getCity());
        tvState.setText(user.getState());
        tvPhone.setText(user.getPhone());
        tvTotalDonations.setText(String.valueOf(user.getTotalDonations()));
        tvLastDonation.setText(user.getLastDonationDate() != null
                ? user.getLastDonationDate() : "Never");
        tvAvailability.setText(user.isAvailable() ? "Available" : "Not Available");
        tvAvailability.setTextColor(ContextCompat.getColor(this, user.isAvailable()
                ? R.color.blood_green : R.color.blood_red));

        if (user.getProfileImageBase64() != null && !user.getProfileImageBase64().isEmpty()) {
            byte[] decodedBytes = Base64.decode(user.getProfileImageBase64(), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            ivProfilePhoto.setImageBitmap(bitmap);
        }

        String phone = user.getPhone();
        btnCall.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            startActivity(intent);
        });

        btnSMS.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + phone));
            intent.putExtra("sms_body", "Hi, I need blood donation. Can you help?");
            startActivity(intent);
        });

        btnWhatsApp.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://api.whatsapp.com/send?phone=91" + phone
                        + "&text=Hi, I need blood donation. Can you help?"));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}