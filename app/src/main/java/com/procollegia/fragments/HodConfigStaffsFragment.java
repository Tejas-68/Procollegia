package com.procollegia.fragments;

import android.os.Bundle;
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
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.procollegia.R;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HodConfigStaffsFragment extends Fragment {

    private LinearLayout llStaffList;
    private View btnAddStaff;
    private FirebaseFirestore db;
    private String hodDepartment = "";

    public HodConfigStaffsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_hod_config_staffs, container, false);

        llStaffList = root.findViewById(R.id.llStaffList);
        btnAddStaff = root.findViewById(R.id.btnAddStaff);
        db = FirebaseFirestore.getInstance();

        btnAddStaff.setOnClickListener(v -> promptAddStaff());

        loadHodDepartment();

        return root;
    }

    private void loadHodDepartment() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists() && isAdded()) {
                hodDepartment = doc.getString("hodDepartment");
                if (hodDepartment == null) hodDepartment = doc.getString("department");
                if (hodDepartment == null) hodDepartment = "";

                fetchStaffs();
            }
        });
    }

    private void fetchStaffs() {
        if (hodDepartment.isEmpty()) return;
        db.collection("users")
            .whereEqualTo("role", "Teacher")
            .whereEqualTo("department", hodDepartment)
            .addSnapshotListener((qs, e) -> {
                if (e != null || !isAdded()) return;
                llStaffList.removeAllViews();

                if (qs == null || qs.isEmpty()) {
                    TextView tv = new TextView(getContext());
                    tv.setText("No staff members found.");
                    tv.setTextColor(getResources().getColor(R.color.text_muted, null));
                    llStaffList.addView(tv);
                    return;
                }

                for (DocumentSnapshot doc : qs.getDocuments()) {
                    View v = getLayoutInflater().inflate(R.layout.item_hod_manage_staff, llStaffList, false);
                    TextView tvName = v.findViewById(R.id.tvStaffName);
                    TextView tvEmail = v.findViewById(R.id.tvStaffEmail);
                    View btnEdit = v.findViewById(R.id.btnEditStaff);
                    View btnRemove = v.findViewById(R.id.btnRemoveStaff);

                    String name = doc.getString("name");
                    String email = doc.getString("email");
                    String uucms = doc.getString("uucmsId");
                    
                    tvName.setText(name != null ? name : "Unknown Staff");
                    tvEmail.setText(email != null ? email : (uucms != null ? uucms : "No ID"));

                    btnEdit.setOnClickListener(click -> promptEditStaff(doc.getId(), name, email));
                    btnRemove.setOnClickListener(click -> promptRemoveStaff(doc.getId(), name));

                    llStaffList.addView(v);
                }
            });
    }

    private void promptAddStaff() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText etName = new EditText(requireContext());
        etName.setHint("Staff Full Name");
        layout.addView(etName);

        EditText etEmail = new EditText(requireContext());
        etEmail.setHint("Email Address (Optional)");
        layout.addView(etEmail);

        new AlertDialog.Builder(requireContext())
            .setTitle("Onboard New Faculty")
            .setMessage("Note: If the staff registers through the app using this email, they will automatically sync with this account.")
            .setView(layout)
            .setPositiveButton("Add Staff", (d, w) -> {
                String name = etName.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                if (!name.isEmpty()) createStaffRecord(name, email);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void createStaffRecord(String name, String email) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        if (!email.isEmpty()) data.put("email", email);
        data.put("role", "Teacher");
        data.put("department", hodDepartment);
        data.put("createdAt", System.currentTimeMillis());

        // We create a document with a mock ID.
        // In a real scenario, we might use Firebase Functions to create an Auth user.
        String mockUid = "STAFF_" + UUID.randomUUID().toString().substring(0, 8);
        
        db.collection("users").document(mockUid).set(data)
            .addOnSuccessListener(v -> Toast.makeText(getContext(), "Staff Added", Toast.LENGTH_SHORT).show());
    }

    private void promptEditStaff(String uid, String currentName, String currentEmail) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText etName = new EditText(requireContext());
        etName.setText(currentName);
        etName.setSelection(currentName != null ? currentName.length() : 0);
        etName.setHint("Staff Full Name");
        layout.addView(etName);

        EditText etEmail = new EditText(requireContext());
        etEmail.setText(currentEmail);
        etEmail.setHint("Email Address (Optional)");
        layout.addView(etEmail);

        new AlertDialog.Builder(requireContext())
            .setTitle("Edit Staff Details")
            .setView(layout)
            .setPositiveButton("Save", (d, w) -> {
                String newName = etName.getText().toString().trim();
                String newEmail = etEmail.getText().toString().trim();
                if (!newName.isEmpty()) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("name", newName);
                    if (!newEmail.isEmpty()) updates.put("email", newEmail);
                    db.collection("users").document(uid).update(updates)
                        .addOnSuccessListener(v -> Toast.makeText(getContext(), "Details Updated", Toast.LENGTH_SHORT).show());
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void promptRemoveStaff(String uid, String name) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Remove Staff")
            .setMessage("Are you sure you want to remove " + name + " from the department?")
            .setPositiveButton("Remove", (d, w) -> {
                // We just remove the department assignment so they don't show up here,
                // or we could delete the document if it's a mock.
                if (uid.startsWith("STAFF_") || uid.startsWith("MOCK_")) {
                    db.collection("users").document(uid).delete();
                } else {
                    db.collection("users").document(uid).update("department", com.google.firebase.firestore.FieldValue.delete());
                }
                Toast.makeText(getContext(), "Staff Removed", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
