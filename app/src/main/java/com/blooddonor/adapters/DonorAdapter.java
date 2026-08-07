package com.blooddonor.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.blooddonor.R;
import com.blooddonor.activities.DonorProfileActivity;
import com.blooddonor.models.User;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import com.google.android.material.chip.Chip;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.List;

public class DonorAdapter extends RecyclerView.Adapter<DonorAdapter.ViewHolder> {

    private final Context context;
    private final List<User> donorList;

    public DonorAdapter(Context context, List<User> donorList) {
        this.context = context;
        this.donorList = donorList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_donor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User donor = donorList.get(position);

        holder.tvName.setText(donor.getName());
        holder.tvBloodGroup.setText(donor.getBloodGroup());
        holder.tvCity.setText(donor.getCity() + ", " + donor.getState());
        holder.tvDonations.setText(donor.getTotalDonations() + " donations");
        holder.chipAvailability.setText(donor.isAvailable() ? "Available" : "Unavailable");
        holder.chipAvailability.setChipBackgroundColorResource(
                donor.isAvailable() ? R.color.chip_green : R.color.chip_red);

        if (donor.getProfileImageBase64() != null && !donor.getProfileImageBase64().isEmpty()) {
            byte[] decodedBytes = Base64.decode(donor.getProfileImageBase64(), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            holder.ivPhoto.setImageBitmap(bitmap);
        } else {
            holder.ivPhoto.setImageResource(R.drawable.ic_person);
        }
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DonorProfileActivity.class);
            intent.putExtra(DonorProfileActivity.EXTRA_USER_ID, donor.getUserId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return donorList.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivPhoto;
        TextView tvName, tvBloodGroup, tvCity, tvDonations;
        Chip chipAvailability;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto          = itemView.findViewById(R.id.iv_photo);
            tvName           = itemView.findViewById(R.id.tv_name);
            tvBloodGroup     = itemView.findViewById(R.id.tv_blood_group);
            tvCity           = itemView.findViewById(R.id.tv_city);
            tvDonations      = itemView.findViewById(R.id.tv_donations);
            chipAvailability = itemView.findViewById(R.id.chip_availability);
        }
    }
}
