package com.blooddonor.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.blooddonor.utils.InAppNotificationManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.blooddonor.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class RequestDetailActivity extends AppCompatActivity {

    public static final String EXTRA_REQUEST_ID     = "request_id";
    public static final String EXTRA_BLOOD_GROUP    = "blood_group";
    public static final String EXTRA_REQUESTER_NAME = "requester_name";
    public static final String EXTRA_REQUESTER_ID   = "requester_id";
    public static final String EXTRA_PHONE          = "phone";
    public static final String EXTRA_HOSPITAL       = "hospital";
    public static final String EXTRA_CITY           = "city";
    public static final String EXTRA_STATE          = "state";
    public static final String EXTRA_UNITS          = "units";
    public static final String EXTRA_URGENCY        = "urgency";
    public static final String EXTRA_PATIENT_NAME   = "patient_name";
    public static final String EXTRA_NOTES          = "notes";
    public static final String EXTRA_CREATED_AT     = "created_at";
    public static final String EXTRA_STATUS         = "status";

    private TextView tvBloodGroup, tvUrgency, tvRequesterName,
            tvPatientName, tvHospital, tvLocation,
            tvUnits, tvNotes, tvDate, tvStatus, tvFulfilled;
    private Button btnCall, btnWhatsApp;
    private com.google.android.material.button.MaterialButton btnDonated;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String requestId;
    private String requesterId;
    private String phone;
    private String status;
    private String bloodGroup;
    private String requesterName;
    private String hospital;
    private String city;
    private String state;

    private final SimpleDateFormat sdf =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
    private final SimpleDateFormat dateSdf =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Request Details");
        }

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        
        requestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);
        if (requestId != null && !requestId.isEmpty() && getIntent().getStringExtra(EXTRA_BLOOD_GROUP) == null) {
            // Only ID passed (from notification), load full data from Firestore
            loadDataFromFirestore(requestId);
        } else {
            // Data passed via extras
            loadData();
        }
    }

    private void loadDataFromFirestore(String id) {
        btnDonated.setEnabled(false);
        db.collection("blood_requests").document(id).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Request not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    
                    requesterId   = doc.getString("requesterId");
                    phone         = doc.getString("requesterPhone");
                    status        = doc.getString("status");
                    bloodGroup    = doc.getString("bloodGroup");
                    requesterName = doc.getString("requesterName");
                    hospital      = doc.getString("hospitalName");
                    city          = doc.getString("city");
                    state         = doc.getString("state");
                    
                    tvBloodGroup.setText(bloodGroup);
                    tvUrgency.setText(doc.getString("urgency"));
                    tvRequesterName.setText(requesterName);
                    tvPatientName.setText(doc.getString("patientName"));
                    tvHospital.setText(hospital);
                    tvLocation.setText(city + ", " + state);
                    
                    Long units = doc.getLong("unitsNeeded");
                    tvUnits.setText((units != null ? units : 1) + " unit(s) needed");
                    
                    tvNotes.setText(doc.getString("additionalNotes"));
                    
                    Long createdAt = doc.getLong("createdAt");
                    tvDate.setText(createdAt != null ? sdf.format(new Date(createdAt)) : "Unknown");
                    
                    updateStatusUI(status);
                    setupButtons();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void setupButtons() {
        btnDonated.setEnabled(true);
        // Call button
        btnCall.setOnClickListener(v -> {
            if (phone != null && !phone.isEmpty()) {
                Intent call = new Intent(Intent.ACTION_DIAL);
                call.setData(Uri.parse("tel:" + phone));
                startActivity(call);
            }
        });

        // WhatsApp button
        btnWhatsApp.setOnClickListener(v -> {
            try {
                Intent wa = new Intent(Intent.ACTION_VIEW);
                wa.setData(Uri.parse(
                        "https://api.whatsapp.com/send?phone=91"
                                + phone
                                + "&text=Hi, I saw your blood request for "
                                + bloodGroup
                                + " at " + hospital
                                + ". I can help!"));
                startActivity(wa);
            } catch (Exception e) {
                Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
            }
        });

        // I Donated button
        btnDonated.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) return;
            if (user.getUid().equals(requesterId)) {
                Toast.makeText(this, "You cannot mark your own request as fulfilled", Toast.LENGTH_SHORT).show();
                return;
            }
            showDonationConfirmDialog(user.getUid());
        });
    }

    private void initViews() {
        tvBloodGroup    = findViewById(R.id.tv_blood_group);
        tvUrgency       = findViewById(R.id.tv_urgency);
        tvRequesterName = findViewById(R.id.tv_requester_name);
        tvPatientName   = findViewById(R.id.tv_patient_name);
        tvHospital      = findViewById(R.id.tv_hospital);
        tvLocation      = findViewById(R.id.tv_location);
        tvUnits         = findViewById(R.id.tv_units);
        tvNotes         = findViewById(R.id.tv_notes);
        tvDate          = findViewById(R.id.tv_date);
        tvStatus        = findViewById(R.id.tv_status);
        tvFulfilled     = findViewById(R.id.tv_fulfilled);
        btnCall         = findViewById(R.id.btn_call);
        btnWhatsApp     = findViewById(R.id.btn_whatsapp);
        btnDonated      = findViewById(R.id.btn_donated);
    }

    private void loadData() {
        Intent i = getIntent();

        requestId               = i.getStringExtra(EXTRA_REQUEST_ID);
        requesterId             = i.getStringExtra(EXTRA_REQUESTER_ID);
        phone                   = i.getStringExtra(EXTRA_PHONE);
        status                  = i.getStringExtra(EXTRA_STATUS);
        bloodGroup              = i.getStringExtra(EXTRA_BLOOD_GROUP);
        requesterName           = i.getStringExtra(EXTRA_REQUESTER_NAME);
        hospital                = i.getStringExtra(EXTRA_HOSPITAL);
        city                    = i.getStringExtra(EXTRA_CITY);
        state                   = i.getStringExtra(EXTRA_STATE);
        int units               = i.getIntExtra(EXTRA_UNITS, 1);
        String urgency          = i.getStringExtra(EXTRA_URGENCY);
        String patientName      = i.getStringExtra(EXTRA_PATIENT_NAME);
        String notes            = i.getStringExtra(EXTRA_NOTES);
        long createdAt          = i.getLongExtra(EXTRA_CREATED_AT, 0);

        tvBloodGroup.setText(bloodGroup);
        tvUrgency.setText(urgency);
        tvRequesterName.setText(requesterName);
        tvPatientName.setText(
                (patientName != null && !patientName.isEmpty())
                        ? patientName : "Not specified");
        tvHospital.setText(hospital);
        tvLocation.setText(city + ", " + state);
        tvUnits.setText(units + " unit(s) needed");
        tvNotes.setText(
                (notes != null && !notes.isEmpty())
                        ? notes : "No additional notes");
        tvDate.setText(createdAt > 0
                ? sdf.format(new Date(createdAt)) : "Unknown");

        // Show status
        updateStatusUI(status);
        setupButtons();
    }

    private void showDonationConfirmDialog(String donorUid) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Confirm Donation")
                .setMessage("Are you sure you donated blood for this request?\n\n" +
                        "This will:\n" +
                        "• Mark the request as Fulfilled\n" +
                        "• Add 1 to your total donations\n" +
                        "• Save today as your last donation date\n" +
                        "• Notify the requester")
                .setPositiveButton("Yes, I Donated", (dialog, which) ->
                        markAsFulfilled(donorUid))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void markAsFulfilled(String donorUid) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        // Notify requester their blood request is fulfilled
        if (requesterId != null && !requesterId.isEmpty()) {
            db.collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(donorDoc -> {
                        String donorName = donorDoc.getString("name");
                        if (donorName == null) donorName = "A donor";

                        String title = "✅ Blood Request Fulfilled!";
                        String body = donorName + " donated " + bloodGroup
                                + " blood for your request! Thank you!";

                        // Save to requester's history
                        InAppNotificationManager.saveNotification(
                                db, requesterId, title, body, "DONOR_FOUND", requestId);

                        // Send push notification to requester
                        db.collection("users").document(requesterId).get()
                                .addOnSuccessListener(requesterDoc -> {
                                    String token = requesterDoc.getString("fcmToken");
                                    if (token != null && !token.isEmpty()) {
                                        com.blooddonor.utils.FcmNotificationSender
                                                .sendNotification(
                                                        getApplicationContext(),
                                                        token,
                                                        title,
                                                        body,
                                                        "DONOR_FOUND",
                                                        requestId);
                                    }
                                });
                    });
        }

        if (requestId == null || requestId.isEmpty()) {
            Toast.makeText(this, "Request ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        btnDonated.setEnabled(false);
        btnDonated.setText("Saving...");

        String todayDate = dateSdf.format(new Date());

        db.collection("blood_requests")
                .document(requestId)
                .update(
                        "status",    "FULFILLED",
                        "donorId",   donorUid,
                        "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(unused -> {

                    // Update donor stats AND save history record
                    updateDonorStats(
                            donorUid, todayDate,
                            bloodGroup, hospital,
                            city, state,
                            requesterName, phone);

                    updateStatusUI("FULFILLED");
                    Toast.makeText(this,
                            "Thank you! 🩸 Donation recorded.\n" +
                                    "Check your Donation History in Profile.",
                            Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    btnDonated.setEnabled(true);
                    btnDonated.setText("✅  I Donated Blood for This Request");
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateDonorStats(String donorUid,
                                  String todayDate,
                                  String bloodGroup,
                                  String hospitalName,
                                  String city,
                                  String state,
                                  String requesterName,
                                  String requesterPhone) {

        // Step A — Increment totalDonations and set lastDonationDate
        db.collection("users").document(donorUid)
                .get()
                .addOnSuccessListener(doc -> {
                    long currentTotal = 0;
                    if (doc.exists()
                            && doc.getLong("totalDonations") != null) {
                        currentTotal = doc.getLong("totalDonations");
                    }
                    long newTotal = currentTotal + 1;

                    db.collection("users").document(donorUid)
                            .update(
                                    "totalDonations",   newTotal,
                                    "lastDonationDate", todayDate,
                                    "updatedAt",
                                    System.currentTimeMillis())
                            .addOnSuccessListener(u -> {
                                // Step B — Save to donation_history sub-collection
                                saveDonationHistoryRecord(
                                        donorUid, todayDate,
                                        bloodGroup, hospitalName,
                                        city, state,
                                        requesterName, requesterPhone);
                            });
                });
    }

    private void saveDonationHistoryRecord(
            String donorUid,
            String todayDate,
            String bloodGroup,
            String hospitalName,
            String city,
            String state,
            String requesterName,
            String requesterPhone) {

        Map<String, Object> record = new java.util.HashMap<>();
        record.put("requestId",      requestId);
        record.put("requesterName",  requesterName);
        record.put("requesterPhone", requesterPhone);
        record.put("bloodGroup",     bloodGroup);
        record.put("hospitalName",   hospitalName);
        record.put("city",           city);
        record.put("state",          state);
        record.put("donationDate",   todayDate);
        record.put("createdAt",      System.currentTimeMillis());

        db.collection("users")
                .document(donorUid)
                .collection("donation_history")
                .add(record);
    }

    private void updateStatusUI(String currentStatus) {
        if ("FULFILLED".equals(currentStatus)) {
            // Hide donate button, show fulfilled message
            btnDonated.setVisibility(View.GONE);
            tvFulfilled.setVisibility(View.VISIBLE);
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText("✅  FULFILLED");
            tvStatus.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.blood_green));
        } else if ("CLOSED".equals(currentStatus)) {
            btnDonated.setVisibility(View.GONE);
            tvFulfilled.setVisibility(View.VISIBLE);
            tvFulfilled.setText("🔒 This request has been closed");
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText("CLOSED");
            tvStatus.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.text_secondary));
        } else {
            // OPEN — show the donate button
            btnDonated.setVisibility(View.VISIBLE);
            tvFulfilled.setVisibility(View.GONE);
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText("🔴  OPEN");
            tvStatus.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.blood_red));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
