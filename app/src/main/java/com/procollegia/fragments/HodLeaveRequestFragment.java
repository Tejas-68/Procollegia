package com.procollegia.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.procollegia.R;
import com.procollegia.adapters.LeaveRequestAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * P3: HOD leave request approval screen.
 * Loaded inside the HOD academics / attendance section.
 * Lists pending leave requests; lets HOD approve or reject each one.
 *
 * Firestore:
 *   leaveRequests/{id}  →  studentId, studentName, dateFrom, dateTo, reason, status, createdAt
 */
public class HodLeaveRequestFragment extends Fragment {

    private RecyclerView rvLeaves;
    private TextView tvEmpty;
    private FirebaseFirestore db;
    private final List<LeaveRequestAdapter.LeaveRequest> leaveList = new ArrayList<>();
    private LeaveRequestAdapter adapter;

    public HodLeaveRequestFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_hod_leave_requests, container, false);

        rvLeaves = root.findViewById(R.id.rvLeaveRequests);
        tvEmpty  = root.findViewById(R.id.tvNoLeaves);
        rvLeaves.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();

        adapter = new LeaveRequestAdapter(leaveList, (item, action) -> processLeave(item, action));
        rvLeaves.setAdapter(adapter);

        loadPendingLeaves();

        return root;
    }

    // ── Firestore ─────────────────────────────────────────────────────────────

    private void loadPendingLeaves() {
        db.collection("leaveRequests")
                .whereEqualTo("status", "pending")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(qs -> {
                    if (!isAdded()) return;
                    leaveList.clear();
                    for (QueryDocumentSnapshot doc : qs) {
                        String dateFrom = doc.getString("dateFrom");
                        String dateTo   = doc.getString("dateTo");
                        String range    = (dateFrom != null ? dateFrom : "?")
                                        + (dateTo != null ? " – " + dateTo : "");
                        leaveList.add(new LeaveRequestAdapter.LeaveRequest(
                                doc.getId(),
                                doc.getString("studentName"),
                                doc.getString("reason"),
                                range,
                                "pending"));
                    }
                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(leaveList.isEmpty() ? View.VISIBLE : View.GONE);
                    });
                })
                .addOnFailureListener(e -> {
                    if (getActivity() != null)
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Failed to load leave requests", Toast.LENGTH_SHORT).show());
                });
    }

    /**
     * Approve or reject a leave request.
     * On approval, marks the student's attendanceRecords on that date as "L".
     */
    private void processLeave(LeaveRequestAdapter.LeaveRequest item, String newStatus) {
        db.collection("leaveRequests").document(item.id)
                .update("status", newStatus)
                .addOnSuccessListener(v -> {
                    String msg = "approved".equals(newStatus) ? "Leave approved ✓" : "Leave rejected";
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();

                    // If approved → update matching attendanceRecords to "L"
                    if ("approved".equals(newStatus)) {
                        db.collection("leaveRequests").document(item.id).get().addOnSuccessListener(doc -> {
                            String sid = doc.getString("studentId");
                            String dFrom = doc.getString("dateFrom");
                            String dTo = doc.getString("dateTo");
                            if (sid != null && dFrom != null && dTo != null) {
                                db.collection("attendanceRecords")
                                    .whereEqualTo("studentId", sid)
                                    .get()
                                    .addOnSuccessListener(qs -> {
                                        for (QueryDocumentSnapshot aDoc : qs) {
                                            String aDate = aDoc.getString("date");
                                            if (aDate != null && aDate.compareTo(dFrom) >= 0 && aDate.compareTo(dTo) <= 0) {
                                                aDoc.getReference().update("status", "L");
                                            }
                                        }
                                    });
                            }
                        });
                    }

                    // Remove from list and refresh
                    leaveList.remove(item);
                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(leaveList.isEmpty() ? View.VISIBLE : View.GONE);
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Action failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
