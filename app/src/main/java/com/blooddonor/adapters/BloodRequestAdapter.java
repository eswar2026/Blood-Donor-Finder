package com.blooddonor.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.blooddonor.R;
import com.blooddonor.activities.RequestDetailActivity;
import com.blooddonor.models.BloodRequest;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BloodRequestAdapter extends
        RecyclerView.Adapter<BloodRequestAdapter.ViewHolder> {

    private final Context context;

    // Not final — can be swapped when tab changes
    private List<BloodRequest> requestList;

    private final SimpleDateFormat sdf =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    // ── Click Listener Interfaces ─────────────────────────────

    public interface OnEditClickListener {
        void onEditClick(BloodRequest request);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(BloodRequest request);
    }

    private OnEditClickListener   editClickListener;
    private OnDeleteClickListener deleteClickListener;

    public void setOnEditClickListener(
            OnEditClickListener listener) {
        this.editClickListener = listener;
    }

    public void setOnDeleteClickListener(
            OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }

    // ── Constructor ───────────────────────────────────────────

    public BloodRequestAdapter(Context context,
                               List<BloodRequest> requestList) {
        this.context     = context;
        this.requestList = requestList;
    }

    // ── Update list when switching tabs ───────────────────────

    public void updateList(List<BloodRequest> newList) {
        this.requestList = newList;
        notifyDataSetChanged();
    }

    // ── Inflate item layout ───────────────────────────────────

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                         int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_blood_request, parent, false);
        return new ViewHolder(view);
    }

    // ── Bind data to each card ────────────────────────────────

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder,
                                 int position) {
        BloodRequest req = requestList.get(position);

        // ── Basic fields ──────────────────────────────────────
        holder.tvBloodGroup.setText(req.getBloodGroup());
        holder.tvRequesterName.setText(req.getRequesterName());
        holder.tvHospital.setText(req.getHospitalName());
        holder.tvLocation.setText(
                req.getCity() + ", " + req.getState());
        holder.tvUnits.setText(
                req.getUnitsNeeded() + " unit(s) needed");
        holder.tvDate.setText(
                sdf.format(new Date(req.getCreatedAt())));

        // ── Urgency chip color ────────────────────────────────
        String urgency = req.getUrgency() != null
                ? req.getUrgency() : "NORMAL";
        holder.chipUrgency.setText(urgency);

        switch (urgency) {
            case "CRITICAL":
                holder.chipUrgency
                        .setChipBackgroundColorResource(
                                R.color.blood_red);
                holder.chipUrgency.setTextColor(
                        ContextCompat.getColor(context,
                                android.R.color.white));
                break;
            case "URGENT":
                holder.chipUrgency
                        .setChipBackgroundColorResource(
                                R.color.blood_orange);
                holder.chipUrgency.setTextColor(
                        ContextCompat.getColor(context,
                                android.R.color.white));
                break;
            default:
                holder.chipUrgency
                        .setChipBackgroundColorResource(
                                R.color.chip_green);
                holder.chipUrgency.setTextColor(
                        ContextCompat.getColor(context,
                                R.color.text_primary));
                break;
        }

        // ── Status visual — dim fulfilled/closed cards ────────
        String status = req.getStatus() != null
                ? req.getStatus() : "OPEN";
        switch (status) {
            case "FULFILLED":
                holder.itemView.setAlpha(0.65f);
                break;
            case "CLOSED":
                holder.itemView.setAlpha(0.45f);
                break;
            default: // OPEN
                holder.itemView.setAlpha(1.0f);
                break;
        }

        // ── Show Edit / Delete only for owner's OPEN requests ─
        String currentUid =
                FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance()
                        .getCurrentUser().getUid()
                        : "";

        boolean isOwner = req.getRequesterId() != null
                && req.getRequesterId().equals(currentUid);
        boolean isOpen  = "OPEN".equals(status);

        if (isOwner && isOpen) {
            // Show edit and delete buttons
            holder.layoutOwnerActions.setVisibility(View.VISIBLE);
        } else {
            // Hide edit and delete buttons for others
            // or for non-OPEN requests
            holder.layoutOwnerActions.setVisibility(View.GONE);
        }

        // ── Edit button click ─────────────────────────────────
        holder.btnEdit.setOnClickListener(v -> {
            if (editClickListener != null) {
                editClickListener.onEditClick(req);
            }
        });

        // ── Delete button click ───────────────────────────────
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDeleteClick(req);
            }
        });

        // ── Contact Now button — direct dial ──────────────────
        holder.btnContact.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse(
                    "tel:" + req.getRequesterPhone()));
            context.startActivity(intent);
        });

        // ── Whole card click — open full detail screen ────────
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context,
                    RequestDetailActivity.class);
            intent.putExtra(
                    RequestDetailActivity.EXTRA_REQUEST_ID,
                    req.getRequestId());
            intent.putExtra(
                    RequestDetailActivity.EXTRA_REQUESTER_ID,
                    req.getRequesterId());
            intent.putExtra(
                    RequestDetailActivity.EXTRA_BLOOD_GROUP,
                    req.getBloodGroup());
            intent.putExtra(
                    RequestDetailActivity.EXTRA_REQUESTER_NAME,
                    req.getRequesterName());
            intent.putExtra(
                    RequestDetailActivity.EXTRA_PHONE,
                    req.getRequesterPhone());
            intent.putExtra(
                    RequestDetailActivity.EXTRA_HOSPITAL,
                    req.getHospitalName());
            intent.putExtra(
                    RequestDetailActivity.EXTRA_CITY,
                    req.getCity());
            intent.putExtra(
                    RequestDetailActivity.EXTRA_STATE,
                    req.getState());
            intent.putExtra(
                    RequestDetailActivity.EXTRA_UNITS,
                    req.getUnitsNeeded());
            intent.putExtra(
                    RequestDetailActivity.EXTRA_URGENCY,
                    req.getUrgency());
            intent.putExtra(
                    RequestDetailActivity.EXTRA_PATIENT_NAME,
                    req.getPatientName());
            intent.putExtra(
                    RequestDetailActivity.EXTRA_NOTES,
                    req.getAdditionalNotes());
            intent.putExtra(
                    RequestDetailActivity.EXTRA_CREATED_AT,
                    req.getCreatedAt());
            intent.putExtra(
                    RequestDetailActivity.EXTRA_STATUS,
                    req.getStatus());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    // ═══════════════════════════════════════════════════════════
    // ViewHolder — holds all views for one card
    // ═══════════════════════════════════════════════════════════

    static class ViewHolder extends RecyclerView.ViewHolder {

        // Card content views
        TextView    tvBloodGroup;
        TextView    tvRequesterName;
        TextView    tvHospital;
        TextView    tvLocation;
        TextView    tvUnits;
        TextView    tvDate;
        Chip        chipUrgency;
        Button      btnContact;

        // Owner-only action views
        LinearLayout layoutOwnerActions;
        ImageView    btnEdit;
        ImageView    btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            // ── Bind card content views ───────────────────────
            tvBloodGroup    = itemView.findViewById(
                    R.id.tv_blood_group);
            tvRequesterName = itemView.findViewById(
                    R.id.tv_requester_name);
            tvHospital      = itemView.findViewById(
                    R.id.tv_hospital);
            tvLocation      = itemView.findViewById(
                    R.id.tv_location);
            tvUnits         = itemView.findViewById(
                    R.id.tv_units);
            tvDate          = itemView.findViewById(
                    R.id.tv_date);
            chipUrgency     = itemView.findViewById(
                    R.id.chip_urgency);
            btnContact      = itemView.findViewById(
                    R.id.btn_contact);

            // ── Bind owner action views ───────────────────────
            layoutOwnerActions = itemView.findViewById(
                    R.id.layout_owner_actions);
            btnEdit            = itemView.findViewById(
                    R.id.btn_edit);
            btnDelete          = itemView.findViewById(
                    R.id.btn_delete);
        }
    }
}