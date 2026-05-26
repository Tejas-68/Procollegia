package com.procollegia.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.procollegia.R;
import com.procollegia.adapters.StudentAttendanceAdapter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TeacherAttendanceFragment extends Fragment {

    private Spinner spYear, spSection;
    private EditText etSearch;
    private RecyclerView rvStudents;
    private TextView tvSummary;
    private Button btnSubmit;
    
    private StudentAttendanceAdapter adapter;
    private final List<StudentAttendanceAdapter.StudentAttendance> fullList = new ArrayList<>();
    private final List<StudentAttendanceAdapter.StudentAttendance> filteredList = new ArrayList<>();
    private ProgressBar pbLoading;
    
    private FirebaseFirestore db;
    private String uid;
    private boolean alreadySubmitted = false;

    public TeacherAttendanceFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_teacher_attendance, container, false);

        spYear     = root.findViewById(R.id.spinnerYear);
        spSection  = root.findViewById(R.id.spinnerSection);
        etSearch   = root.findViewById(R.id.etSearchStudent);
        rvStudents = root.findViewById(R.id.rvStudentAttendance);
        tvSummary  = root.findViewById(R.id.tvAttendanceSummary);
        btnSubmit  = root.findViewById(R.id.btnSubmitAttendance);
        pbLoading  = root.findViewById(R.id.pbLoading);

        rvStudents.setLayoutManager(new LinearLayoutManager(getContext()));
        db  = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        setupSpinners();
        setupSearch();
        
        root.findViewById(R.id.btnExportExcel).setOnClickListener(v -> exportToPdf());
        btnSubmit.setOnClickListener(v -> submitAttendance());
        root.findViewById(R.id.fabCameraScanner).setOnClickListener(v -> startScanner());

        checkIfAlreadySubmitted();
        loadStudentsWithLeaves();

        return root;
    }

    private void setupSpinners() {
        String[] years    = {"All Years", "1st Year", "2nd Year", "3rd Year"};
        String[] sections = {"All Sections", "Sec A", "Sec B", "Sec C"};
        spYear.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, years));
        spSection.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, sections));

        AdapterView.OnItemSelectedListener listen = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int i, long l) { filterList(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        spYear.setOnItemSelectedListener(listen);
        spSection.setOnItemSelectedListener(listen);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterList(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void checkIfAlreadySubmitted() {
        if (uid == null) return;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String period = getCurrentPeriod();

        // BUG 4 FIX: query the correct collection where per-student records are stored
        db.collection("attendanceRecords")
                .whereEqualTo("teacherUid", uid)
                .whereEqualTo("date", today)
                .whereEqualTo("period", period)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    if (!qs.isEmpty()) {
                        alreadySubmitted = true;
                        btnSubmit.setText("Already Marked for " + period);
                        btnSubmit.setEnabled(false);
                        btnSubmit.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.text_muted));
                    }
                });
    }

    private String getCurrentPeriod() {
        int hour = Integer.parseInt(new SimpleDateFormat("HH", Locale.getDefault()).format(new Date()));
        if (hour < 10) return "Period 1";
        if (hour < 11) return "Period 2";
        if (hour < 12) return "Period 3";
        if (hour < 14) return "Period 4 (Post-Lunch)";
        return "Period 5";
    }

    private void loadStudentsWithLeaves() {
        if (!fullList.isEmpty()) {
            checkLeavesForToday();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        db.collection("users").whereIn("role", java.util.Arrays.asList("Student", "student")).get()
                .addOnSuccessListener(qs -> {
                    if (!isAdded()) return;
                    fullList.clear();
                    for (QueryDocumentSnapshot d : qs) {
                        try {
                            String name     = d.getString("name");
                            String uucms    = d.getString("uucmsId");
                            Object yearObj  = d.get("year"); 
                            String year     = (yearObj != null) ? String.valueOf(yearObj) : "";
                            String section  = d.getString("section");

                            fullList.add(new StudentAttendanceAdapter.StudentAttendance(
                                    d.getId(),
                                    name,
                                    uucms,
                                    year,
                                    section != null ? section : "Sec A"
                            ));
                        } catch (Exception e) {}
                    }
                    checkLeavesForToday();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    checkLeavesForToday();
                });
    }

    private void checkLeavesForToday() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        db.collection("leaveRequests")
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(qs -> {
                    if (!isAdded()) return;
                    for (QueryDocumentSnapshot d : qs) {
                        String dateFrom = d.getString("dateFrom");
                        String dateTo = d.getString("dateTo");
                        if (dateFrom != null && dateTo != null && today.compareTo(dateFrom) >= 0 && today.compareTo(dateTo) <= 0) {
                            String studentId = d.getString("studentId");
                            for (StudentAttendanceAdapter.StudentAttendance s : fullList) {
                                if (s.id != null && s.id.equals(studentId)) s.isOnLeave = true;
                            }
                        }
                    }
                    pbLoading.setVisibility(View.GONE);
                    filterList();
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        pbLoading.setVisibility(View.GONE);
                        filterList();
                    }
                });
    }

    private void filterList() {
        if (spYear.getSelectedItem() == null || spSection.getSelectedItem() == null) return;

        String year  = spYear.getSelectedItem().toString();
        String sec   = spSection.getSelectedItem().toString();
        String query = etSearch.getText().toString().toLowerCase();

        filteredList.clear();
        for (StudentAttendanceAdapter.StudentAttendance s : fullList) {
            // Flexible Year match (handles '1', '1st Year', 'I Year' etc)
            boolean matchesYear = year.equals("All Years") || (s.year != null && 
                    (s.year.equals(year) || 
                     s.year.startsWith(String.valueOf(year.charAt(0))) ||
                     (year.startsWith("1") && s.year.contains("I")) ||
                     (year.startsWith("2") && s.year.contains("II")) ||
                     (year.startsWith("3") && s.year.contains("III"))));
            
            // Flexible Section match (handles 'A', 'Sec A' etc)
            String sSec = (s.section != null) ? s.section.toLowerCase() : "";
            boolean matchesSec = sec.equals("All Sections") || sSec.contains(sec.toLowerCase().replace("sec ", ""));

            // Query match (Safety check: avoid NPE on name/uucmsId)
            String name = (s.name != null) ? s.name.toLowerCase() : "";
            String code = (s.uucmsId != null) ? s.uucmsId.toLowerCase() : "";
            boolean matchesQuery = query.isEmpty() || name.contains(query) || code.contains(query);

            if (matchesYear && matchesSec && matchesQuery) {
                filteredList.add(s);
            }
        }

        if (adapter == null) {
            adapter = new StudentAttendanceAdapter(filteredList, this::updateSummary);
            rvStudents.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
        updateSummary();
    }


    private void updateSummary() {
        int p=0, a=0;
        for (StudentAttendanceAdapter.StudentAttendance s : filteredList) {
            if ("P".equals(s.status)) p++;
            else if ("A".equals(s.status)) a++;
        }
        tvSummary.setText(String.format("Present: %d | Absent: %d", p, a));
    }

    private void exportToPdf() {
        if (fullList.isEmpty()) {
            Toast.makeText(getContext(), "No students to export", Toast.LENGTH_SHORT).show();
            return;
        }

        android.graphics.pdf.PdfDocument document = new android.graphics.pdf.PdfDocument();
        android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
        android.graphics.pdf.PdfDocument.Page page = document.startPage(pageInfo);
        
        android.graphics.Canvas canvas = page.getCanvas();
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(android.graphics.Color.BLACK);
        
        // Title
        paint.setTextSize(20f);
        paint.setFakeBoldText(true);
        canvas.drawText("Attendance Report", 50, 50, paint);
        
        paint.setTextSize(14f);
        paint.setFakeBoldText(false);
        String details = "Year: " + spYear.getSelectedItem() + " | Section: " + spSection.getSelectedItem();
        canvas.drawText(details, 50, 80, paint);
        
        int y = 120;
        paint.setFakeBoldText(true);
        canvas.drawText("Name", 50, y, paint);
        canvas.drawText("UUCMS ID", 250, y, paint);
        canvas.drawText("Status", 400, y, paint);
        canvas.drawText("Remarks", 470, y, paint);
        paint.setFakeBoldText(false);
        
        y += 20;
        for (StudentAttendanceAdapter.StudentAttendance s : fullList) {
            String remarks = s.isOnLeave ? "Approved Leave" : "";
            canvas.drawText(s.name != null ? s.name : "N/A", 50, y, paint);
            canvas.drawText(s.uucmsId != null ? s.uucmsId : "N/A", 250, y, paint);
            canvas.drawText(s.status, 400, y, paint);
            canvas.drawText(remarks, 470, y, paint);
            y += 20;
            
            // Simple pagination handling if needed, though for a real app we'd need multiple pages
            if (y > 800) {
                document.finishPage(page);
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }
        }
        document.finishPage(page);
        
        try {
            File file = new File(getContext().getExternalFilesDir(null), "Attendance_" + System.currentTimeMillis() + ".pdf");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();
            
            Uri u = FileProvider.getUriForFile(getContext(), "com.procollegia.fileprovider", file);
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("application/pdf");
            i.putExtra(Intent.EXTRA_STREAM, u);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i, "Export Attendance PDF"));
        } catch (IOException e) {
            Toast.makeText(getContext(), "PDF Export failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitAttendance() {
        if (alreadySubmitted) return;
        if (filteredList.isEmpty()) {
            Toast.makeText(getContext(), "No students to submit.", Toast.LENGTH_SHORT).show();
            return;
        }
        String date   = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String period = getCurrentPeriod();

        // BUG 4 FIX: batch-write one record per student to 'attendanceRecords'
        WriteBatch batch = db.batch();
        for (StudentAttendanceAdapter.StudentAttendance s : filteredList) {
            java.util.Map<String, Object> rec = new java.util.HashMap<>();
            rec.put("studentId",  s.id);
            rec.put("name",       s.name);
            rec.put("uucmsId",    s.uucmsId);
            rec.put("year",       s.year);
            rec.put("section",    s.section);
            rec.put("status",     s.status);        // "P", "A", or "L"
            rec.put("isOnLeave",  s.isOnLeave);
            rec.put("date",       date);
            rec.put("period",     period);
            rec.put("teacherUid", uid);
            rec.put("timestamp",  new Date());
            batch.set(db.collection("attendanceRecords").document(), rec);
        }

        btnSubmit.setEnabled(false);
        batch.commit().addOnSuccessListener(aVoid -> {
            alreadySubmitted = true;
            btnSubmit.setText("Submitted for " + period);
            Toast.makeText(getContext(), "Attendance Submitted Successfully", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            btnSubmit.setEnabled(true);
            Toast.makeText(getContext(), "Submit failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void startScanner() {
        if (alreadySubmitted) {
            Toast.makeText(getContext(), "Already marked for this period", Toast.LENGTH_SHORT).show();
            return;
        }
        if (androidx.core.content.ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.CAMERA) 
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 102); return;
        }
        java.util.HashMap<String, String> uucmsMap = new java.util.HashMap<>();
        for (StudentAttendanceAdapter.StudentAttendance s : fullList) {
            if (s.uucmsId != null) uucmsMap.put(s.uucmsId, s.name);
        }
        Intent intent = new Intent(getContext(), com.procollegia.AttendanceScannerActivity.class);
        intent.putExtra("uucmsMap", uucmsMap);
        startActivityForResult(intent, 101);
    }

    @Override public void onActivityResult(int req, int res, @Nullable Intent d) {
        super.onActivityResult(req, res, d);
        if (req == 101 && android.app.Activity.RESULT_OK == res && d != null) {
            ArrayList<String> scanned = d.getStringArrayListExtra("scanned");
            if (scanned != null) {
                for (String uucms : scanned) {
                    for (StudentAttendanceAdapter.StudentAttendance s : fullList) {
                        if (uucms.equals(s.uucmsId)) s.status = "P";
                    }
                }
                filterList(); // refresh
            }
        }
    }
}
