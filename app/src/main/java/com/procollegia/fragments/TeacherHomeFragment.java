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
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.procollegia.R;
import com.procollegia.adapters.ComplaintTeacherAdapter;
import com.procollegia.adapters.LeaveRequestAdapter;
import com.procollegia.utils.NotificationHelper;
import com.procollegia.utils.TimetableLoader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TeacherHomeFragment extends Fragment {

    private TextView tvGreeting;
    private RecyclerView rvLeave, rvComplaints;
    private View root;

    private FirebaseFirestore db;
    private String uid;

    private final List<LeaveRequestAdapter.LeaveRequest> leaveList = new ArrayList<>();
    private final List<ComplaintTeacherAdapter.TeacherComplaint> complaintList = new ArrayList<>();

    public TeacherHomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_teacher_home, container, false);

        // Bind header
        tvGreeting = root.findViewById(R.id.tvTeacherName);

        // RecyclerViews
        rvLeave      = root.findViewById(R.id.rvLeaveRequests);
        rvComplaints = root.findViewById(R.id.rvComplaints);

        View cardManageInternals = root.findViewById(R.id.cardManageInternals);
        if (cardManageInternals != null) {
            cardManageInternals.setOnClickListener(v -> {
                startActivity(new android.content.Intent(getContext(), com.procollegia.TeacherInternalsActivity.class));
            });
        }
        
        View btnUploadTimetable = root.findViewById(R.id.btnUploadTimetable);
        if (btnUploadTimetable != null) {
            btnUploadTimetable.setOnClickListener(v -> {
                startActivity(new android.content.Intent(getContext(), com.procollegia.TimetableUploadActivity.class));
            });
        }

        rvLeave.setLayoutManager(new LinearLayoutManager(getContext()));
        rvComplaints.setLayoutManager(new LinearLayoutManager(getContext()));

        // Firebase context
        db  = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (uid != null) {
            fetchTeacherDetails();
            loadLeaveRequests();
            loadComplaints();
        } else {
            loadMockData();
        }

        return root;
    }

    private void fetchTeacherDetails() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(d -> {
                    if (!isAdded()) return;
                    String name = d.getString("name");
                    if (name != null && !name.trim().isEmpty()) {
                        String firstName = name.split("\\s+")[0];
                        tvGreeting.setText("Welcome Prof. " + firstName);
                    } else {
                        tvGreeting.setText("Welcome Back");
                    }
                    // Load timetable; fire notification if none uploaded today
                    String dept = d.getString("department");
                    View timetableWidget = root.findViewById(R.id.includeTimetable);
                    if (dept != null && timetableWidget != null) {
                        final String finalDept = dept;
                        TimetableLoader.load(timetableWidget, dept, this,
                                () -> NotificationHelper.notifyUploadReminder(getContext(), finalDept));
                    }
                });
    }

    private void loadLeaveRequests() {
        // Correct collection: leaveRequests
        db.collection("leaveRequests")
                .whereIn("status", java.util.Arrays.asList("Pending", "pending")) 
                .limit(10)
                .get()
                .addOnSuccessListener(qs -> {
                    if (!isAdded()) return;
                    leaveList.clear();
                    for (QueryDocumentSnapshot d : qs) {
                        String date = d.getString("fromDate");
                        if (date == null) date = d.getString("date"); // Fallback

                        leaveList.add(new LeaveRequestAdapter.LeaveRequest(
                                d.getId(),
                                d.getString("studentName") != null ? d.getString("studentName") : "Student",
                                d.getString("reason") != null ? d.getString("reason") : "No reason provided",
                                date != null ? date : "N/A",
                                "pending"));
                    }
                    renderLeave();
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) renderLeave();
                });
    }

    private LeaveRequestAdapter leaveAdapter;

    private void renderLeave() {
        if (!isAdded() || getContext() == null) return;
        if (leaveAdapter == null) {
            leaveAdapter = new LeaveRequestAdapter(leaveList, (item, action) -> {
                db.collection("leaveRequests").document(item.id).update("status", action)
                        .addOnSuccessListener(v -> {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Leave " + action, Toast.LENGTH_SHORT).show();
                            }
                            leaveList.remove(item);
                            renderLeave();
                        });
            });
            rvLeave.setAdapter(leaveAdapter);
        } else {
            leaveAdapter.notifyDataSetChanged();
        }
    }

    private void loadComplaints() {
        // Look for feedback or complaints addressed to Teacher/Class Teacher
        db.collection("feedback")
                .whereIn("status", java.util.Arrays.asList("Pending", "pending"))
                .limit(10)
                .get()
                .addOnSuccessListener(qs -> {
                    if (!isAdded()) return;
                    complaintList.clear();
                    for (QueryDocumentSnapshot d : qs) {
                        complaintList.add(new ComplaintTeacherAdapter.TeacherComplaint(
                                d.getId(),
                                (d.getString("category") != null ? d.getString("category") : "Complaint"),
                                d.getString("studentName") != null ? d.getString("studentName") : "Anonymous",
                                d.getString("subject") != null ? d.getString("subject") : "General",
                                d.getString("date") != null ? d.getString("date") : "Today"));
                    }
                    rvComplaints.setAdapter(new ComplaintTeacherAdapter(complaintList));
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) rvComplaints.setAdapter(new ComplaintTeacherAdapter(complaintList));
                });
    }

    private void loadMockData() {
        tvGreeting.setText("Welcome, Prof. Sharma");
        addMockLeave();
        renderLeave();
        addMockComplaints();
        rvComplaints.setAdapter(new ComplaintTeacherAdapter(complaintList));
    }

    private void addMockLeave() {
        if (!leaveList.isEmpty()) return;
        leaveList.add(new LeaveRequestAdapter.LeaveRequest("m1", "Rahul Kumar", "Medical Leave (Fever)", "Mar 21", "pending"));
        leaveList.add(new LeaveRequestAdapter.LeaveRequest("m2", "Sneha Reddy", "Family Emergency", "Mar 22", "pending"));
    }

    private void addMockComplaints() {
        if (!complaintList.isEmpty()) return;
        complaintList.add(new ComplaintTeacherAdapter.TeacherComplaint("c1", "Complaint", "Rahul Kumar", "Missing equipment in lab", "Mar 18"));
        complaintList.add(new ComplaintTeacherAdapter.TeacherComplaint("c2", "Suggestion", "Ananya Singh", "Extra doubt sessions needed", "Mar 19"));
    }
}
