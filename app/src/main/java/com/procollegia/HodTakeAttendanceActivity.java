package com.procollegia;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.procollegia.adapters.StudentAttendanceAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HodTakeAttendanceActivity extends AppCompatActivity {

    private Spinner spYear, spSem, spSection;
    private EditText etSearch;
    private RecyclerView rvStudents;
    private TextView tvSummary;
    private Button btnSubmit;
    private ProgressBar pbLoading;

    private StudentAttendanceAdapter adapter;
    private final List<StudentAttendanceAdapter.StudentAttendance> fullList = new ArrayList<>();
    private final List<StudentAttendanceAdapter.StudentAttendance> filteredList = new ArrayList<>();
    
    private FirebaseFirestore db;
    private String uid;
    private boolean alreadySubmitted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_take_attendance);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "HOD_MOCK";

        spYear     = findViewById(R.id.spinnerYear);
        spSem      = findViewById(R.id.spinnerSem);
        spSection  = findViewById(R.id.spinnerSection);
        etSearch   = findViewById(R.id.etSearchStudent);
        rvStudents = findViewById(R.id.rvStudentAttendance);
        tvSummary  = findViewById(R.id.tvAttendanceSummary);
        btnSubmit  = findViewById(R.id.btnSubmitAttendance);
        pbLoading  = findViewById(R.id.pbLoading);

        rvStudents.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> submitAttendance());

        setupSpinners();
        setupSearch();

        // Temporarily removed the simulated auto-selection so HODs see all active students default to 'All Years' & 'All Sections' to verify syncing.
        // simulateHodTimetableContext();

        loadStudentsWithLeaves();
    }

    private void setupSpinners() {
        String[] years    = {"All Years", "1st Year", "2nd Year", "3rd Year"};
        String[] sems     = {"All Sems", "I Sem", "II Sem", "III Sem", "IV Sem", "V Sem", "VI Sem"};
        String[] sections = {"All Sections", "Sec A", "Sec B", "Sec C"};

        spYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, years));
        spSem.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sems));
        spSection.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sections));

        AdapterView.OnItemSelectedListener listen = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int i, long l) { filterList(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        spYear.setOnItemSelectedListener(listen);
        spSem.setOnItemSelectedListener(listen);
        spSection.setOnItemSelectedListener(listen);
    }

    private void simulateHodTimetableContext() {
        // Here we mock the logic where the HOD's scheduled timetable automatically
        // selects the class they are supposed to be teaching right now. 
        // Example: 2nd Year, IV Sem, Sec A
        spYear.setSelection(2); // 2nd Year
        spSem.setSelection(4);  // IV Sem
        spSection.setSelection(1); // Sec A
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterList(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadStudentsWithLeaves() {
        if (!fullList.isEmpty()) {
            checkLeavesForToday();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        db.collection("users").whereIn("role", java.util.Arrays.asList("Student", "student")).get()
            .addOnSuccessListener(qs -> {
                if (isFinishing() || isDestroyed()) return;
                fullList.clear();
                for (QueryDocumentSnapshot d : qs) {
                    try {
                        String name     = d.getString("name");
                        String uucms    = d.getString("uucmsId");
                        Object yearObj  = d.get("year"); 
                        String year     = (yearObj != null) ? String.valueOf(yearObj) : "";
                        String sem      = d.getString("semester");
                        String section  = d.getString("section");

                        StudentAttendanceAdapter.StudentAttendance student = new StudentAttendanceAdapter.StudentAttendance(
                                d.getId(),
                                name,
                                uucms,
                                year,
                                section != null ? section : "Sec A"
                        );
                        // We loosely bind semester dynamically or parse it 
                        // To keep it clean, we just use year/section logic + semester match below if we extend StudentAttendance model.
                        // Currently adapter doesn't store Sem, so we just add the item.
                        fullList.add(student);
                    } catch (Exception e) {}
                }
                checkLeavesForToday();
            })
            .addOnFailureListener(e -> {
                if (isFinishing() || isDestroyed()) return;
                checkLeavesForToday();
            });
    }

    private void checkLeavesForToday() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        db.collection("leaveRequests")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener(qs -> {
                if (isFinishing() || isDestroyed()) return;
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
                if (isFinishing() || isDestroyed()) return;
                pbLoading.setVisibility(View.GONE);
                filterList();
            });
    }

    private void filterList() {
        if (spYear.getSelectedItem() == null || spSem.getSelectedItem() == null || spSection.getSelectedItem() == null) return;

        String year  = spYear.getSelectedItem().toString();
        String sem   = spSem.getSelectedItem().toString(); // Currently ignored in list model but UI exists
        String sec   = spSection.getSelectedItem().toString();
        String query = etSearch.getText().toString().toLowerCase();

        filteredList.clear();
        for (StudentAttendanceAdapter.StudentAttendance s : fullList) {
            boolean matchesYear = year.equals("All Years") || (s.year != null && 
                    (s.year.equals(year) || 
                     s.year.startsWith(String.valueOf(year.charAt(0))) ||
                     (year.startsWith("1") && s.year.contains("I")) ||
                     (year.startsWith("2") && s.year.contains("II")) ||
                     (year.startsWith("3") && s.year.contains("III"))));
            
            String sSec = (s.section != null) ? s.section.toLowerCase() : "";
            boolean matchesSec = sec.equals("All Sections") || sSec.contains(sec.toLowerCase().replace("sec ", ""));

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
        int p = 0, a = 0;
        for (StudentAttendanceAdapter.StudentAttendance s : filteredList) {
            if ("P".equals(s.status)) p++;
            else if ("A".equals(s.status)) a++;
        }
        tvSummary.setText(String.format("Present: %d | Absent: %d", p, a));
    }

    private void submitAttendance() {
        if (alreadySubmitted) return;
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No students to submit.", Toast.LENGTH_SHORT).show();
            return;
        }
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // BUG 4 FIX: batch-write one record per student, with HOD override flag
        WriteBatch batch = db.batch();
        for (StudentAttendanceAdapter.StudentAttendance s : filteredList) {
            Map<String, Object> rec = new HashMap<>();
            rec.put("studentId",  s.id);
            rec.put("name",       s.name);
            rec.put("uucmsId",    s.uucmsId);
            rec.put("status",     s.status);       // "P", "A", or "L"
            rec.put("isOnLeave",  s.isOnLeave);
            rec.put("date",       date);
            rec.put("hodUid",     uid);
            rec.put("override",   true);           // HOD override flag preserved
            rec.put("timestamp",  new Date());
            batch.set(db.collection("attendanceRecords").document(), rec);
        }

        btnSubmit.setEnabled(false);
        batch.commit().addOnSuccessListener(aVoid -> {
            alreadySubmitted = true;
            btnSubmit.setText("Departmental Attendance Recorded");
            btnSubmit.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.text_muted));
            Toast.makeText(this, "Attendance Overwritten by HOD Successfully", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            btnSubmit.setEnabled(true);
            Toast.makeText(this, "Submit failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}
