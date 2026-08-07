package com.blooddonor.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Base64;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.blooddonor.R;
import com.blooddonor.activities.DonationHistoryActivity;
import com.blooddonor.activities.EditProfileActivity;
import com.blooddonor.activities.MainActivity;
import com.blooddonor.activities.NotificationsActivity;
import com.blooddonor.models.User;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private CircleImageView ivProfilePhoto;
    private TextView tvName, tvBloodGroup, tvCity,
            tvPhone, tvTotalDonations,
            tvLastDonation, tvEmail;
    private TextView tvViewHistory;        // "View History" label
    private SwitchMaterial switchAvailability;
    private Button btnEditProfile, btnLogout, btnNotifications;

    private FirebaseAuth      mAuth;
    private FirebaseFirestore db;
    private User              currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(
                R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        initViews(view);
        setupClickListeners(view);   // ← pass view here
        loadProfile();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile(); // Refresh on return from EditProfile
    }

    private void initViews(View view) {
        ivProfilePhoto     = view.findViewById(R.id.iv_profile_photo);
        tvName             = view.findViewById(R.id.tv_name);
        tvBloodGroup       = view.findViewById(R.id.tv_blood_group);
        tvCity             = view.findViewById(R.id.tv_city);
        tvPhone            = view.findViewById(R.id.tv_phone);
        tvTotalDonations   = view.findViewById(R.id.tv_total_donations);
        tvLastDonation     = view.findViewById(R.id.tv_last_donation);
        tvEmail            = view.findViewById(R.id.tv_email);
        tvViewHistory      = view.findViewById(R.id.tv_view_history);
        switchAvailability = view.findViewById(R.id.switch_availability);
        btnEditProfile     = view.findViewById(R.id.btn_edit_profile);
        btnLogout          = view.findViewById(R.id.btn_logout);
        btnNotifications   = view.findViewById(R.id.btn_notifications);
    }

    private void setupClickListeners(View view) {
        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(),
                        EditProfileActivity.class)));

        btnNotifications.setOnClickListener(v ->
                startActivity(new Intent(requireContext(),
                        NotificationsActivity.class)));

        btnLogout.setOnClickListener(v ->
                ((MainActivity) requireActivity()).logout());

        tvLastDonation.setOnClickListener(v ->
                openDonationHistory());

        tvTotalDonations.setOnClickListener(v ->
                openDonationHistory());

        if (tvViewHistory != null) {
            tvViewHistory.setOnClickListener(v ->
                    openDonationHistory());
        }

        // Donation History card
        View cardHistory = view.findViewById(R.id.card_donation_history);
        if (cardHistory != null) {
            cardHistory.setOnClickListener(v -> openDonationHistory());
        }


        // Total donations count — also opens Donation History
        tvTotalDonations.setOnClickListener(v ->
                openDonationHistory());
    }

    private void openDonationHistory() {
        startActivity(new Intent(requireContext(),
                DonationHistoryActivity.class));
    }

    private void loadProfile() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) return;

        String uid = firebaseUser.getUid();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) return;
                    if (documentSnapshot.exists()) {
                        currentUser = documentSnapshot.toObject(User.class);
                        if (currentUser != null) {
                            populateUI();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed to load profile",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void populateUI() {
        // Name
        tvName.setText(currentUser.getName() != null
                ? currentUser.getName() : "No name set");

        // Blood Group
        tvBloodGroup.setText(currentUser.getBloodGroup() != null
                ? currentUser.getBloodGroup() : "Unknown");

        // City + State combined
        String location = "";
        if (currentUser.getCity() != null)
            location += currentUser.getCity();
        if (currentUser.getState() != null)
            location += ", " + currentUser.getState();
        tvCity.setText(location.isEmpty()
                ? "Location not set" : location);

        // Phone
        tvPhone.setText(currentUser.getPhone() != null
                ? currentUser.getPhone() : "No phone");

        // Email
        tvEmail.setText(currentUser.getEmail() != null
                ? currentUser.getEmail() : "No email");

        // Total Donations — make it clickable
        int total = currentUser.getTotalDonations();
        tvTotalDonations.setText(total + " times");
        tvTotalDonations.setTextColor(
                ContextCompat.getColor(requireContext(),
                        total > 0
                                ? R.color.blood_red
                                : R.color.text_primary));

        // Last Donation Date — make it look clickable
        String lastDate = currentUser.getLastDonationDate();
        boolean hasDonated = lastDate != null
                && !lastDate.isEmpty();

        if (hasDonated) {
            tvLastDonation.setText(lastDate);
            // Make it look like a link — red + underline
            tvLastDonation.setTextColor(
                    ContextCompat.getColor(requireContext(),
                            R.color.blood_red));
            tvLastDonation.setPaintFlags(
                    tvLastDonation.getPaintFlags()
                            | Paint.UNDERLINE_TEXT_FLAG);
            // Show "View History" label
            if (tvViewHistory != null) {
                tvViewHistory.setVisibility(View.VISIBLE);
            }
        } else {
            tvLastDonation.setText("Never donated");
            tvLastDonation.setTextColor(
                    ContextCompat.getColor(requireContext(),
                            R.color.text_secondary));
            tvLastDonation.setPaintFlags(
                    tvLastDonation.getPaintFlags()
                            & ~Paint.UNDERLINE_TEXT_FLAG);
            if (tvViewHistory != null) {
                tvViewHistory.setVisibility(View.GONE);
            }
        }

        // Availability switch
        switchAvailability.setOnCheckedChangeListener(null);
        switchAvailability.setChecked(currentUser.isAvailable());
        switchAvailability.setOnCheckedChangeListener(
                (btn, checked) -> updateAvailability(checked));

        // Profile photo
        String base64 = currentUser.getProfileImageBase64();
        if (base64 != null && !base64.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(
                        bytes, 0, bytes.length);
                ivProfilePhoto.setImageBitmap(bitmap);
            } catch (Exception e) {
                ivProfilePhoto.setImageResource(R.drawable.ic_person);
            }
        } else {
            ivProfilePhoto.setImageResource(R.drawable.ic_person);
        }
    }

    private void updateAvailability(boolean available) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid())
                .update("available", available)
                .addOnSuccessListener(v -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            available
                                    ? "You are now available for donation"
                                    : "Marked as unavailable",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Update failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}