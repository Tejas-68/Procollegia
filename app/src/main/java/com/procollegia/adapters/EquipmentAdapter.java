package com.procollegia.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.procollegia.R;

import java.util.ArrayList;
import java.util.List;

public class EquipmentAdapter extends RecyclerView.Adapter<EquipmentAdapter.VH> {

    public static class Equipment {
        public final String id;
        public final String name;
        public final String category;
        public final int    available;
        public final String emoji;
        public int          selectedQty; // live qty per item

        public Equipment(String id, String name, String category, int available, String emoji) {
            this.id           = id;
            this.name         = name;
            this.category     = category;
            this.available    = available;
            this.emoji        = emoji;
            this.selectedQty  = 1;
        }
    }

    public interface OnBorrowClick {
        void onBorrow(Equipment item, int qty);
    }

    private final List<Equipment>    fullList;   // original (for search filter)
    private List<Equipment>          displayList;
    private final OnBorrowClick      listener;

    public EquipmentAdapter(List<Equipment> list, OnBorrowClick listener) {
        this.fullList    = list;
        this.displayList = new ArrayList<>(list);
        this.listener    = listener;
    }

    /** Filter list by name/category search query */
    public void filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            displayList = new ArrayList<>(fullList);
        } else {
            String q = query.toLowerCase().trim();
            displayList = new ArrayList<>();
            for (Equipment e : fullList) {
                if (e.name.toLowerCase().contains(q) || e.category.toLowerCase().contains(q)) {
                    displayList.add(e);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_equipment, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Equipment e = displayList.get(pos);
        h.tvEmoji.setText(e.emoji);
        h.tvName.setText(e.name);
        h.tvCategory.setText(e.category);

        boolean inStock = e.available > 0;
        if (inStock) {
            h.tvAvail.setText(e.available + " available");
            h.tvAvail.setTextColor(0xFF48BB78);
        } else {
            h.tvAvail.setText("Out of stock");
            h.tvAvail.setTextColor(0xFFFC8181);
        }

        // Reset qty display
        h.tvQty.setText(String.valueOf(e.selectedQty));

        // Minus
        h.btnMinus.setOnClickListener(v -> {
            if (e.selectedQty > 1) {
                e.selectedQty--;
                h.tvQty.setText(String.valueOf(e.selectedQty));
            }
        });

        // Plus
        h.btnPlus.setOnClickListener(v -> {
            if (e.selectedQty < e.available) {
                e.selectedQty++;
                h.tvQty.setText(String.valueOf(e.selectedQty));
            }
        });

        // Borrow
        h.btnBorrow.setEnabled(inStock);
        h.btnBorrow.setAlpha(inStock ? 1f : 0.4f);
        h.btnBorrow.setOnClickListener(v -> {
            if (listener != null && inStock) listener.onBorrow(e, e.selectedQty);
        });
    }

    @Override
    public int getItemCount() { return displayList.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvName, tvCategory, tvAvail;
        TextView btnMinus, tvQty, btnPlus, btnBorrow;

        VH(View v) {
            super(v);
            tvEmoji    = v.findViewById(R.id.tvEquipEmoji);
            tvName     = v.findViewById(R.id.tvEquipName);
            tvCategory = v.findViewById(R.id.tvEquipCategory);
            tvAvail    = v.findViewById(R.id.tvEquipAvail);
            btnMinus   = v.findViewById(R.id.btnQtyMinus);
            tvQty      = v.findViewById(R.id.tvQty);
            btnPlus    = v.findViewById(R.id.btnQtyPlus);
            btnBorrow  = v.findViewById(R.id.btnBorrow);
        }
    }
}
