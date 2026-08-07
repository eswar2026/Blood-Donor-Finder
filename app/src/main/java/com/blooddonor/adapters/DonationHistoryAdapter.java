package com.blooddonor.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.blooddonor.R;
import com.blooddonor.models.DonationHistory;
import java.util.List;

public class DonationHistoryAdapter extends
        RecyclerView.Adapter<DonationHistoryAdapter.ViewHolder> {

    private final Context context;
    private final List<DonationHistory> list;

    public DonationHistoryAdapter(Context context,
                                  List<DonationHistory> list) {
        this.context = context;
        this.list    = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                         int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_donation_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DonationHistory item = list.get(position);

        holder.tvNumber.setText(String.valueOf(
                list.size() - position));        // count down newest first
        holder.tvBloodGroup.setText(item.getBloodGroup());
        holder.tvHospital.setText(item.getHospitalName());
        holder.tvLocation.setText(
                item.getCity() + ", " + item.getState());
        holder.tvRecipient.setText(item.getRequesterName());
        holder.tvDate.setText(item.getDonationDate());
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumber, tvBloodGroup, tvHospital,
                tvLocation, tvRecipient, tvDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumber    = itemView.findViewById(R.id.tv_number);
            tvBloodGroup = itemView.findViewById(R.id.tv_blood_group);
            tvHospital   = itemView.findViewById(R.id.tv_hospital);
            tvLocation   = itemView.findViewById(R.id.tv_location);
            tvRecipient  = itemView.findViewById(R.id.tv_recipient);
            tvDate       = itemView.findViewById(R.id.tv_date);
        }
    }
}