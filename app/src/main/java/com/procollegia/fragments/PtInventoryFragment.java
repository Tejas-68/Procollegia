package com.procollegia.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.procollegia.R;
import com.procollegia.adapters.PtEquipmentAdapter;

import java.util.ArrayList;
import java.util.List;

public class PtInventoryFragment extends Fragment {

    private RecyclerView rvEquip;
    private FirebaseFirestore db;
    private final List<PtEquipmentAdapter.PtEquipment> equipList = new ArrayList<>();
    private PtEquipmentAdapter adapter;
    private String selectedCategory = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pt_inventory, container, false);

        rvEquip = root.findViewById(R.id.rvEquipment);
        rvEquip.setLayoutManager(new GridLayoutManager(getContext(), 2));

        db = FirebaseFirestore.getInstance();

        setupCategoryChips(root);
        loadInventory();

        root.findViewById(R.id.fabAddEquipment).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), com.procollegia.AddInventoryActivity.class));
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadInventory();
    }

    private void setupCategoryChips(View root) {
        TextView chipAll = root.findViewById(R.id.chipAll);
        TextView chipCricket = root.findViewById(R.id.chipCricket);
        TextView chipBasketball = root.findViewById(R.id.chipBasketball);
        TextView chipFootball = root.findViewById(R.id.chipFootball);
        TextView chipBadminton = root.findViewById(R.id.chipBadminton);

        TextView[] chips = {chipAll, chipCricket, chipBasketball, chipFootball, chipBadminton};

        for (int i = 0; i < chips.length; i++) {
            final TextView chip = chips[i];
            chip.setOnClickListener(v -> {
                for (TextView c : chips) {
                    c.setBackgroundResource(R.drawable.bg_pill_inactive);
                    c.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
                }
                chip.setBackgroundResource(R.drawable.bg_pill_active);
                chip.setTextColor(ContextCompat.getColor(getContext(), R.color.text_on_accent));
                selectedCategory = chip.getText().toString();
                loadInventory();
            });
        }
    }

    private void loadInventory() {
        Query q = db.collection("inventory");
        if (!selectedCategory.equals("All")) {
            q = q.whereEqualTo("category", selectedCategory);
        }

        q.get().addOnSuccessListener(qs -> {
            if (!isAdded()) return;
            if (qs.isEmpty()) {
                seedInventory();
                return;
            }
            equipList.clear();
            for (QueryDocumentSnapshot d : qs) {
                equipList.add(new PtEquipmentAdapter.PtEquipment(
                        d.getId(), 
                        d.getString("name"), 
                        d.getString("category"), 
                        d.getString("status"),
                        Math.toIntExact(d.getLong("quantity") != null ? d.getLong("quantity") : 0),
                        Math.toIntExact(d.getLong("remaining") != null ? d.getLong("remaining") : 0)
                ));
            }
            adapter = new PtEquipmentAdapter(equipList, new PtEquipmentAdapter.OnEquipAction() {
                @Override public void onIncrease(PtEquipmentAdapter.PtEquipment it) { incrementQuantity(it); }
                @Override public void onDelete(PtEquipmentAdapter.PtEquipment it) { confirmDelete(it); }
            });
            rvEquip.setAdapter(adapter);
        });
    }

    private void seedInventory() {
        String[][] baseItems = {
            {"Cricket Bat", "Cricket", "15"}, {"Cricket Ball", "Cricket", "30"},
            {"Football", "Football", "8"}, {"Basketball", "Basketball", "5"},
            {"Badminton Racket", "Badminton", "12"}, {"Volleyball", "Volleyball", "6"}
        };
        for (String[] bi : baseItems) {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("name", bi[0]);
            item.put("category", bi[1]);
            item.put("quantity", Integer.parseInt(bi[2]));
            item.put("remaining", Integer.parseInt(bi[2]));
            item.put("status", "available");
            db.collection("inventory").add(item);
        }
        Toast.makeText(getContext(), "Inventory seeded with basic items", Toast.LENGTH_SHORT).show();
        loadInventory();
    }

    private void incrementQuantity(PtEquipmentAdapter.PtEquipment it) {
        db.collection("inventory").document(it.id).update("quantity", it.quantity + 1, "remaining", it.remaining + 1)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Item quantity increased", Toast.LENGTH_SHORT).show();
                    loadInventory();
                });
    }

    private void confirmDelete(PtEquipmentAdapter.PtEquipment it) {
        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Remove Equipment?")
                .setMessage("Are you sure you want to remove " + it.name + "?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    db.collection("inventory").document(it.id).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Item removed", Toast.LENGTH_SHORT).show();
                                loadInventory();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
