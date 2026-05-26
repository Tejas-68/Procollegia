package com.procollegia.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.procollegia.R;
import java.util.List;

/** Adapter for sports equipment in the PT Admin inventory. */
public class PtEquipmentAdapter extends RecyclerView.Adapter<PtEquipmentAdapter.VH> {

    public interface OnEquipAction {
        void onIncrease(PtEquipment equip);
        void onDelete(PtEquipment equip);
    }

    public static class PtEquipment {
        public String id, name, category, status;
        public int quantity, remaining;

        public PtEquipment(String id, String name, String cat, String stat, int q, int rem) {
            this.id = id; this.name = name; this.category = cat; this.status = stat;
            this.quantity = q; this.remaining = rem;
        }
    }

    private final List<PtEquipment> list;
    private final OnEquipAction listener;

    public PtEquipmentAdapter(List<PtEquipment> list, OnEquipAction listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_equipment_pt, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PtEquipment it = list.get(position);
        holder.tvName.setText(it.name);
        holder.tvQty.setText("Stock: " + it.quantity);
        holder.tvStatus.setText(it.status);

        // Visual feedback based on status
        if ("Borrowed".equalsIgnoreCase(it.status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_pill_active); // Yellowish if possible, else just blue
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_pill_active); 
        }

        holder.btnIncrease.setOnClickListener(v -> listener.onIncrease(it));
        // Add long click to delete
        holder.itemView.setOnLongClickListener(v -> {
            listener.onDelete(it);
            return true;
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus, tvQty, btnIncrease;
        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvEquipName);
            tvStatus = v.findViewById(R.id.tvEquipStatus);
            tvQty = v.findViewById(R.id.tvQuantity);
            btnIncrease = v.findViewById(R.id.btnIncreaseQty);
        }
    }
}
