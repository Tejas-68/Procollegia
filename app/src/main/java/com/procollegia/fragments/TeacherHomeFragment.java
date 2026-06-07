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
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.procollegia.R;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.AdapterView;
import androidx.core.content.ContextCompat;
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

    private Spinner spYearAttendance, spSectionAttendance;
    private Spinner spYearInternals, spSectionInternals;
    private CircularProgressIndicator progressAttendanceAnalytics, progressInternalsAnalytics;
    private TextView tvAttendancePercentAnalytics, tvInternalsPercentAnalytics;

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

        // Analytics UI
        spYearAttendance = root.findViewById(R.id.spYearAttendance);
        spSectionAttendance = root.findViewById(R.id.spSectionAttendance);
        progressAttendanceAnalytics = root.findViewById(R.id.progressAttendanceAnalytics);
        tvAttendancePercentAnalytics = root.findViewById(R.id.tvAttendancePercentAnalytics);

        spYearInternals = root.findViewById(R.id.spYearInternals);
        spSectionInternals = root.findViewById(R.id.spSectionInternals);
        progressInternalsAnalytics = root.findViewById(R.id.progressInternalsAnalytics);
        tvInternalsPercentAnalytics = root.findViewById(R.id.tvInternalsPercentAnalytics);

        View btnUploadTimetable = root.findViewById(R.id.btnUploadTimetable);
        if (btnUploadTimetable != null) {
            btnUploadTimetable.setOnClickListener(v -> {
                startActivity(new android.content.Intent(getContext(), com.procollegia.TimetableUploadActivity.class));
            });
        }

        // Internals Docs UI Elements
        View llInternalsDocs = root.findViewById(R.id.llInternalsDocs);
        View btnViewInternalsTimetable = root.findViewById(R.id.btnViewInternalsTimetable);
        View btnViewRoomAllotment = root.findViewById(R.id.btnViewRoomAllotment);

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
            setupSpinnersAndAnalytics();
            loadInternalsDocs(llInternalsDocs, btnViewInternalsTimetable, btnViewRoomAllotment);
        } else {
            loadMockData();
        }

        return root;
    }

    private void loadInternalsDocs(View llInternalsDocs, View btnTimetable, View btnRoom) {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            String dept = doc.getString("department");
            if (dept == null) return;
            
            db.collection("departmentSettings").document(dept).addSnapshotListener((dset, e) -> {
                if (e != null || dset == null || !dset.exists()) return;
                
                Boolean isActive = dset.getBoolean("isInternalsActive");
                if (isActive != null && isActive) {
                    llInternalsDocs.setVisibility(View.VISIBLE);
                    
                    String ttUrl = dset.getString("internalsTimetableUrl");
                    String ttType = dset.getString("timetableFileType");
                    if (ttUrl != null) {
                        btnTimetable.setVisibility(View.VISIBLE);
                        btnTimetable.setOnClickListener(v -> openDoc(ttUrl, ttType));
                    } else {
                        btnTimetable.setVisibility(View.GONE);
                    }

                    String roomUrl = dset.getString("roomAllotmentUrl");
                    String roomType = dset.getString("roomAllotmentFileType");
                    if (roomUrl != null) {
                        btnRoom.setVisibility(View.VISIBLE);
                        btnRoom.setOnClickListener(v -> openDoc(roomUrl, roomType));
                    } else {
                        btnRoom.setVisibility(View.GONE);
                    }
                } else {
                    llInternalsDocs.setVisibility(View.GONE);
                }
            });
        });
    }

    private void openDoc(String dataUrl, String fileType) {
        if ("pdf".equals(fileType)) {
            try {
                String base64 = dataUrl;
                if (base64.contains(",")) base64 = base64.substring(base64.indexOf(",") + 1);
                byte[] pdfBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);

                java.io.File cacheDir = requireContext().getCacheDir();
                java.io.File pdfFile = new java.io.File(cacheDir, "internals_doc.pdf");
                java.io.FileOutputStream fos = new java.io.FileOutputStream(pdfFile);
                fos.write(pdfBytes);
                fos.close();

                android.net.Uri pdfUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    pdfFile
                );

                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(pdfUri, "application/pdf");
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    android.widget.Toast.makeText(getContext(), "No PDF viewer installed on this device.", android.widget.Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                android.widget.Toast.makeText(getContext(), "Failed to open PDF: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
            }
        } else {
            try {
                String base64 = dataUrl.substring(dataUrl.indexOf(",") + 1);
                byte[] decoded = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length);

                android.widget.ImageView iv = new android.widget.ImageView(getContext());
                iv.setImageBitmap(bitmap);
                iv.setAdjustViewBounds(true);

                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Internals Document")
                    .setView(iv)
                    .setPositiveButton("Close", null)
                    .show();
            } catch (Exception e) {
                android.widget.Toast.makeText(getContext(), "Failed to load image", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
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

    private void setupSpinnersAndAnalytics() {
        if (getContext() == null) return;
        String[] years = {"All Years", "1st Year", "2nd Year", "3rd Year"};
        String[] sections = {"All Sections", "A", "B", "C"};

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, years);
        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, sections);

        spYearAttendance.setAdapter(yearAdapter);
        spSectionAttendance.setAdapter(sectionAdapter);
        spYearInternals.setAdapter(yearAdapter);
        spSectionInternals.setAdapter(sectionAdapter);

        AdapterView.OnItemSelectedListener attListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { fetchAttendanceAnalytics(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        spYearAttendance.setOnItemSelectedListener(attListener);
        spSectionAttendance.setOnItemSelectedListener(attListener);

        AdapterView.OnItemSelectedListener intListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { fetchInternalsAnalytics(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        spYearInternals.setOnItemSelectedListener(intListener);
        spSectionInternals.setOnItemSelectedListener(intListener);
    }

    private void fetchAttendanceAnalytics() {
        String year = spYearAttendance.getSelectedItem().toString();
        String section = spSectionAttendance.getSelectedItem().toString();

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            String dept = doc.getString("department");
            if (dept == null) return;
            
            db.collection("departmentSettings").document(dept).get().addOnSuccessListener(dset -> {
                String semStart = dset.getString("semStartDate");
                List<String> holidays = (List<String>) dset.get("holidays");
                
                int workingDays = com.procollegia.fragments.HodConfigAttendanceFragment.calculateWorkingDays(semStart, holidays);
                if (workingDays <= 0) {
                    resetAttendanceAnalytics();
                    return;
                }

                // Instead of only teacher's attendance, if they want class analytics, or if they want teacher's analytics:
                // We'll keep it scoped to teacher if needed, or entire dept. Let's scope to teacher's classes but calculate based on working days?
                // Actually the prompt says "apply that logic in the teacher dashboard too".
                // We'll fetch teacher's records and calculate distinct days present per student.
                
                Query q = db.collection("attendanceRecords").whereEqualTo("teacherUid", uid);
                if (!"All Years".equals(year)) q = q.whereEqualTo("year", year.substring(0, 1) + "st Year"); // Rough match or just year
                // Let's refine the query
                q.get().addOnSuccessListener(qs -> {
                    if (!isAdded()) return;
                    
                    java.util.Set<String> allStudents = new java.util.HashSet<>();
                    java.util.Map<String, java.util.Set<String>> studentPresentDays = new java.util.HashMap<>();
                    
                    String yrNum = "All Years".equals(year) ? "" : year.substring(0, 1);
                    String secLet = "All Sections".equals(section) ? "" : section.replace("Sec ", "").trim();

                    for (QueryDocumentSnapshot d : qs) {
                        String sId = d.getString("studentId");
                        String date = d.getString("date");
                        String status = d.getString("status");
                        String y = d.getString("year");
                        String s = d.getString("section");
                        
                        if (!yrNum.isEmpty() && (y == null || !y.startsWith(yrNum))) continue;
                        if (!secLet.isEmpty()) {
                            if (s == null) continue;
                            String stSec = s.toLowerCase().replace("sec ", "").replace("section ", "").trim();
                            if (!stSec.equalsIgnoreCase(secLet)) continue;
                        }

                        if (sId != null) {
                            allStudents.add(sId);
                            if ("P".equals(status) && date != null) {
                                if (!studentPresentDays.containsKey(sId)) studentPresentDays.put(sId, new java.util.HashSet<>());
                                studentPresentDays.get(sId).add(date);
                            }
                        }
                    }

                    if (allStudents.isEmpty()) {
                        resetAttendanceAnalytics();
                        return;
                    }

                    long totalPresentDays = 0;
                    for (String sId : allStudents) {
                        if (studentPresentDays.containsKey(sId)) {
                            totalPresentDays += studentPresentDays.get(sId).size();
                        }
                    }

                    double avgPresent = (double) totalPresentDays / allStudents.size();
                    int pct = (int) Math.round((avgPresent / workingDays) * 100);
                    if (pct > 100) pct = 100;

                    tvAttendancePercentAnalytics.setText(pct + "%");
                    progressAttendanceAnalytics.setProgress(pct);
                    
                    int color;
                    if (pct >= 75) color = ContextCompat.getColor(requireContext(), R.color.accent_green);
                    else if (pct >= 60) color = ContextCompat.getColor(requireContext(), R.color.accent_orange);
                    else color = ContextCompat.getColor(requireContext(), R.color.accent_red);
                    progressAttendanceAnalytics.setIndicatorColor(color);
                });
            });
        });
    }

    private void resetAttendanceAnalytics() {
        tvAttendancePercentAnalytics.setText("0%");
        progressAttendanceAnalytics.setProgress(0);
        progressAttendanceAnalytics.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.accent_red));
    }

    private void fetchInternalsAnalytics() {
        String year = spYearInternals.getSelectedItem().toString();
        String section = spSectionInternals.getSelectedItem().toString();

        Query q = db.collection("internals").whereEqualTo("teacherUid", uid);
        if (!"All Years".equals(year)) q = q.whereEqualTo("year", year);
        if (!"All Sections".equals(section)) q = q.whereEqualTo("section", section);

        q.get().addOnSuccessListener(qs -> {
            if (!isAdded()) return;
            int totalMarks = 0;
            int totalMax = 0;
            for (QueryDocumentSnapshot d : qs) {
                Long m = d.getLong("marks");
                Long mMax = d.getLong("maxMarks");
                if (m != null && mMax != null && mMax > 0) {
                    totalMarks += m.intValue();
                    totalMax += mMax.intValue();
                }
            }
            int pct = totalMax > 0 ? (totalMarks * 100 / totalMax) : 0;
            tvInternalsPercentAnalytics.setText(pct + "%");
            progressInternalsAnalytics.setProgress(pct);
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
