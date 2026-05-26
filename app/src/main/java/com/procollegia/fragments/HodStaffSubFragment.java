package com.procollegia.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.procollegia.R;
import com.procollegia.adapters.StaffAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HodStaffSubFragment extends Fragment {

    private RecyclerView rvStaff;
    private StaffAdapter staffAdapter;
    private final List<StaffAdapter.StaffItem> staffList = new ArrayList<>();
    private FirebaseFirestore db;

    public HodStaffSubFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_hod_staff_sub, container, false);
        db = FirebaseFirestore.getInstance();

        rvStaff = root.findViewById(R.id.rvStaffList);
        setupStaffList();

        root.findViewById(R.id.btnAddStaff).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Onboarding Panel Coming Soon", Toast.LENGTH_SHORT).show();
        });

        return root;
    }

    private void setupStaffList() {
        staffAdapter = new StaffAdapter(staffList, new StaffAdapter.OnSubjectAction() {
            @Override
            public void onAddSubject(StaffAdapter.StaffItem item, String subjectName) {
                if (!item.subjects.contains(subjectName)) {
                    item.subjects.add(subjectName);
                    updateSubjectsInDatabase(item.id, item.subjects);
                    staffAdapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Added " + subjectName, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Subject already exists", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onRemoveSubject(StaffAdapter.StaffItem item, String subjectName) {
                if (item.subjects.contains(subjectName)) {
                    item.subjects.remove(subjectName);
                    updateSubjectsInDatabase(item.id, item.subjects);
                    staffAdapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Removed " + subjectName, Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        rvStaff.setLayoutManager(new LinearLayoutManager(getContext()));
        rvStaff.setAdapter(staffAdapter);

        fetchStaff();
    }

    private void updateSubjectsInDatabase(String uid, List<String> updatedSubjects) {
        // Only attempt to update if we have a real UID
        if (uid != null && !uid.startsWith("MOCK_")) {
            db.collection("users").document(uid).update("assignedSubjects", updatedSubjects)
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to sync changes", Toast.LENGTH_SHORT).show());
        }
    }

    private void fetchStaff() {
        staffList.clear();
        db.collection("users")
                .whereEqualTo("role", "Teacher")
                .get()
                .addOnSuccessListener(qs -> {
                    if (!isAdded()) return;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot d : qs) {
                        List<String> subjects = new ArrayList<>();
                        Object subObj = d.get("assignedSubjects");
                        if (subObj instanceof List) {
                            for (Object o : (List<?>) subObj) {
                                if (o instanceof String) subjects.add((String) o);
                            }
                        } else if (subObj instanceof String) {
                            subjects.add((String) subObj);
                        }

                        staffList.add(new StaffAdapter.StaffItem(
                                d.getId(),
                                d.getString("name"),
                                subjects
                        ));
                    }
                    if (staffList.isEmpty()) {
                        // Professional Mocks if DB empty
                        staffList.add(new StaffAdapter.StaffItem("MOCK_1", "Dr. Sameer Khan", new ArrayList<>(Arrays.asList("Computer Networks", "DBMS"))));
                        staffList.add(new StaffAdapter.StaffItem("MOCK_2", "Prof. Ananya Rao", new ArrayList<>(Arrays.asList("C Programming", "Data Structures"))));
                    }
                    staffAdapter.notifyDataSetChanged();
                });
    }
}
