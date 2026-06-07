package com.procollegia;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import androidx.annotation.Nullable;
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

import androidx.appcompat.app.AlertDialog;
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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Calendar;

public class HodTakeAttendanceActivity extends AppCompatActivity {

    private static final long COOLDOWN_MS = 5 * 60 * 1000L;
    private static final int  CUTOFF_HOUR = 17;

    private List<String> periodsList = new ArrayList<>(Arrays.asList(
        "Period 1 (08:00 – 09:00)",
        "Period 2 (09:00 – 10:00)",
        "Period 3 (10:00 – 11:00)",
        "Period 4 (11:00 – 12:00)",
        "Period 5 (13:00 – 14:00)",
        "Period 6 (14:00 – 15:00)"
    ));

    private Spinner spPeriod, spYear, spSem, spSection;
    private EditText etSearch;
    private RecyclerView rvStudents;
    private TextView tvSummary, tvBanner, tvCooldown;
    private Button btnSubmit;
    private ProgressBar pbLoading;
    private android.widget.ImageView btnConfigPeriods;

    private StudentAttendanceAdapter adapter;
    private final List<StudentAttendanceAdapter.StudentAttendance> fullList     = new ArrayList<>();
    private final List<StudentAttendanceAdapter.StudentAttendance> filteredList = new ArrayList<>();

    private FirebaseFirestore db;
    private String uid;
    private String hodDept = "";
    private CountDownTimer cooldownTimer;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_take_attendance);

        db    = FirebaseFirestore.getInstance();
        uid   = FirebaseAuth.getInstance().getUid();
        prefs = getSharedPreferences("attendance_prefs", Context.MODE_PRIVATE);

        spPeriod         = findViewById(R.id.spinnerPeriod);
        spYear           = findViewById(R.id.spinnerYear);
        spSem            = findViewById(R.id.spinnerSem);
        spSection        = findViewById(R.id.spinnerSection);
        etSearch         = findViewById(R.id.etSearchStudent);
        rvStudents       = findViewById(R.id.rvStudentAttendance);
        tvSummary        = findViewById(R.id.tvAttendanceSummary);
        btnSubmit        = findViewById(R.id.btnSubmitAttendance);
        pbLoading        = findViewById(R.id.pbLoading);
        tvBanner         = findViewById(R.id.tvBanner);
        tvCooldown       = findViewById(R.id.tvCooldown);
        btnConfigPeriods = findViewById(R.id.btnConfigPeriods);

        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnScan).setOnClickListener(v -> startScanner());
        btnSubmit.setOnClickListener(v -> submitAttendance());
        
        btnConfigPeriods.setOnClickListener(v -> showPeriodConfigDialog());

        setupSpinners();
        setupSearch();
        checkCutoff();
        
        db.collection("users").document(uid).get().addOnSuccessListener(d -> {
            if (d.exists()) {
                hodDept = d.getString("hodDepartment");
                if (hodDept == null) hodDept = d.getString("department");
                if (hodDept == null) hodDept = "";
                loadPeriods();
                loadStudentsWithLeaves();
            }
        });
    }

    private void loadPeriods() {
        if (hodDept.isEmpty()) return;
        db.collection("departmentSettings").document(hodDept).get().addOnSuccessListener(d -> {
            if (d.exists() && d.contains("periods")) {
                List<String> fetched = (List<String>) d.get("periods");
                if (fetched != null && !fetched.isEmpty()) {
                    periodsList.clear();
                    periodsList.addAll(fetched);
                    updatePeriodSpinner();
                }
            }
        });
    }

    private void showPeriodConfigDialog() {
        EditText input = new EditText(this);
        input.setHint("e.g. Period 1 (08:00 - 09:00)");
        
        new AlertDialog.Builder(this)
            .setTitle("Add New Period")
            .setMessage("Enter the period name and time range. It will be used for auto-selection if it contains times like 'HH:mm - HH:mm'.")
            .setView(input)
            .setPositiveButton("Add", (d, w) -> {
                String val = input.getText().toString().trim();
                if (!val.isEmpty()) {
                    periodsList.add(val);
                    savePeriods();
                }
            })
            .setNeutralButton("Clear All", (d, w) -> {
                periodsList.clear();
                savePeriods();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void savePeriods() {
        if (hodDept.isEmpty()) return;
        Map<String, Object> map = new HashMap<>();
        map.put("periods", periodsList);
        db.collection("departmentSettings").document(hodDept)
            .set(map, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Periods updated", Toast.LENGTH_SHORT).show();
                updatePeriodSpinner();
            });
    }

    private void updatePeriodSpinner() {
        spPeriod.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, periodsList));
        autoSelectCurrentPeriod();
    }

    private void autoSelectCurrentPeriod() {
        try {
            int currentMinutes = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60 + Calendar.getInstance().get(Calendar.MINUTE);
            for (int i = 0; i < periodsList.size(); i++) {
                String p = periodsList.get(i);
                // Look for pattern HH:mm - HH:mm or HH:mm – HH:mm
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,2}):(\\d{2})\\s*[-–]\\s*(\\d{1,2}):(\\d{2})").matcher(p);
                if (m.find()) {
                    int startH = Integer.parseInt(m.group(1)), startM = Integer.parseInt(m.group(2));
                    int endH = Integer.parseInt(m.group(3)), endM = Integer.parseInt(m.group(4));
                    int startTotal = startH * 60 + startM;
                    int endTotal = endH * 60 + endM;
                    if (currentMinutes >= startTotal && currentMinutes <= endTotal) {
                        spPeriod.setSelection(i);
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cooldownTimer != null) cooldownTimer.cancel();
    }

    // ── Spinners ──────────────────────────────────────────────────────────────
    private void setupSpinners() {
        spPeriod.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, periodsList));
        autoSelectCurrentPeriod();
        spPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int i, long l) {
                if (cooldownTimer != null) cooldownTimer.cancel();
                checkSubmitState();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        String[] years    = {"All Years", "1", "2", "3"};
        String[] sems     = {"All Sems", "1", "2", "3", "4", "5", "6"};
        String[] sections = {"All Sections", "A", "B", "C"};

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

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { filterList(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ── Cutoff & cooldown ─────────────────────────────────────────────────────
    private boolean isPastCutoff() {
        int hour = Integer.parseInt(new SimpleDateFormat("HH", Locale.getDefault()).format(new Date()));
        return hour >= CUTOFF_HOUR;
    }

    private void checkCutoff() {
        if (isPastCutoff()) {
            tvBanner.setVisibility(View.VISIBLE);
            tvBanner.setTextColor(ContextCompat.getColor(this, R.color.accent_red));
            tvBanner.setText("Attendance is disabled after 5:00 PM");
            btnSubmit.setEnabled(false);
            btnSubmit.setText("Disabled after 5 PM");
            btnSubmit.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.text_muted));
        }
    }

    private String getCooldownKey() {
        String today  = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String period = "P" + (spPeriod.getSelectedItemPosition() + 1);
        return uid + "_hod_" + today + "_" + period;
    }

    private void checkSubmitState() {
        if (isPastCutoff()) return;
        long submitTime = prefs.getLong(getCooldownKey(), 0L);
        long now = System.currentTimeMillis();
        long elapsed = now - submitTime;

        if (submitTime > 0 && elapsed < COOLDOWN_MS) {
            lockForCooldown(COOLDOWN_MS - elapsed);
        } else if (submitTime > 0) {
            enableResubmit();
        } else {
            resetSubmitButton();
        }
    }

    private void lockForCooldown(long remainingMs) {
        btnSubmit.setEnabled(false);
        tvCooldown.setVisibility(View.VISIBLE);
        tvBanner.setVisibility(View.VISIBLE);
        tvBanner.setTextColor(ContextCompat.getColor(this, R.color.accent_orange));
        tvBanner.setText("Attendance submitted. Re-submit available after cooldown.");
        if (cooldownTimer != null) cooldownTimer.cancel();
        cooldownTimer = new CountDownTimer(remainingMs, 1000) {
            @Override public void onTick(long ms) {
                tvCooldown.setText(String.format(Locale.getDefault(),
                        "Re-submit in %d:%02d", ms / 60000, (ms % 60000) / 1000));
            }
            @Override public void onFinish() { enableResubmit(); }
        }.start();
    }

    private void enableResubmit() {
        tvCooldown.setVisibility(View.GONE);
        tvBanner.setVisibility(View.VISIBLE);
        tvBanner.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
        tvBanner.setText("Cooldown over. You can now re-submit attendance.");
        btnSubmit.setEnabled(true);
        btnSubmit.setText("Re-submit Attendance");
        btnSubmit.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.accent_blue));
    }

    private void resetSubmitButton() {
        tvCooldown.setVisibility(View.GONE);
        tvBanner.setVisibility(View.GONE);
        btnSubmit.setEnabled(true);
        btnSubmit.setText("Submit Attendance");
        btnSubmit.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.accent_blue));
    }

    // ── Student loading ───────────────────────────────────────────────────────
    private void loadStudentsWithLeaves() {
        if (!fullList.isEmpty()) { checkLeavesForToday(); return; }
        pbLoading.setVisibility(View.VISIBLE);
        db.collection("users").whereIn("role", Arrays.asList("Student", "student")).get()
            .addOnSuccessListener(qs -> {
                if (isFinishing() || isDestroyed()) return;
                fullList.clear();
                for (QueryDocumentSnapshot d : qs) {
                    try {
                        String name    = d.getString("name");
                        String uucms   = d.getString("uucmsId");
                        Object yearObj = d.get("year");
                        String year    = (yearObj != null) ? String.valueOf(yearObj) : "";
                        String section = d.getString("section");
                        fullList.add(new StudentAttendanceAdapter.StudentAttendance(
                                d.getId(), name, uucms, year,
                                section != null ? section : "A"
                        ));
                    } catch (Exception ignored) {}
                }
                checkLeavesForToday();
                checkSubmitState();
            })
            .addOnFailureListener(e -> { if (!isDestroyed()) checkLeavesForToday(); });
    }

    private void checkLeavesForToday() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        db.collection("leaveRequests").whereEqualTo("status", "approved").get()
            .addOnSuccessListener(qs -> {
                if (isFinishing() || isDestroyed()) return;
                for (QueryDocumentSnapshot d : qs) {
                    String dateFrom  = d.getString("dateFrom");
                    String dateTo    = d.getString("dateTo");
                    String studentId = d.getString("studentId");
                    if (dateFrom != null && dateTo != null
                            && today.compareTo(dateFrom) >= 0
                            && today.compareTo(dateTo) <= 0) {
                        for (StudentAttendanceAdapter.StudentAttendance s : fullList) {
                            if (s.id != null && s.id.equals(studentId)) s.isOnLeave = true;
                        }
                    }
                }
                pbLoading.setVisibility(View.GONE);
                filterList();
            })
            .addOnFailureListener(e -> {
                if (!isDestroyed()) { pbLoading.setVisibility(View.GONE); filterList(); }
            });
    }

    // ── Filter ────────────────────────────────────────────────────────────────
    private void filterList() {
        if (spYear.getSelectedItem() == null || spSection.getSelectedItem() == null) return;
        String year  = spYear.getSelectedItem().toString();
        String sec   = spSection.getSelectedItem().toString();
        String query = etSearch.getText().toString().toLowerCase().trim();

        filteredList.clear();
        for (StudentAttendanceAdapter.StudentAttendance s : fullList) {
            boolean matchY = year.equals("All Years") || (s.year != null &&
                    (s.year.equals(year) || s.year.startsWith(year.substring(0, 1))));
            String sSec = (s.section != null) ? s.section.toLowerCase() : "";
            boolean matchS = sec.equals("All Sections") ||
                    sSec.contains(sec.toLowerCase().replace("sec ", ""));
            String name = (s.name != null) ? s.name.toLowerCase() : "";
            String code = (s.uucmsId != null) ? s.uucmsId.toLowerCase() : "";
            boolean matchQ = query.isEmpty() || name.contains(query) || code.contains(query);
            if (matchY && matchS && matchQ) filteredList.add(s);
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

    // ── Submit ────────────────────────────────────────────────────────────────
    private void submitAttendance() {
        if (isPastCutoff()) {
            Toast.makeText(this, "Attendance is closed after 5 PM.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No students to submit.", Toast.LENGTH_SHORT).show();
            return;
        }
        String date   = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String period = "P" + (spPeriod.getSelectedItemPosition() + 1);

        WriteBatch batch = db.batch();
        for (StudentAttendanceAdapter.StudentAttendance s : filteredList) {
            String docId = s.id + "_" + date + "_" + period + "_hod";
            Map<String, Object> rec = new HashMap<>();
            rec.put("studentId",  s.id);
            rec.put("name",       s.name);
            rec.put("uucmsId",    s.uucmsId);
            rec.put("year",       s.year);
            rec.put("section",    s.section);
            rec.put("status",     s.status);
            rec.put("isOnLeave",  s.isOnLeave);
            rec.put("date",       date);
            rec.put("period",     period);
            rec.put("hodUid",     uid);
            rec.put("override",   true);
            rec.put("timestamp",  new Date());
            batch.set(db.collection("attendanceRecords").document(docId), rec);
        }

        btnSubmit.setEnabled(false);
        batch.commit()
            .addOnSuccessListener(aVoid -> {
                prefs.edit().putLong(getCooldownKey(), System.currentTimeMillis()).apply();

                int absentCount = 0;
                List<String> absentNames = new ArrayList<>();
                for (StudentAttendanceAdapter.StudentAttendance s : filteredList) {
                    if ("A".equals(s.status) && !s.isOnLeave) {
                        absentCount++;
                        if (absentNames.size() < 10) absentNames.add(s.name != null ? s.name : "Unknown");
                    }
                }
                showAbsentSummary(period, absentCount, absentNames);
                lockForCooldown(COOLDOWN_MS);
            })
            .addOnFailureListener(e -> {
                btnSubmit.setEnabled(true);
                Toast.makeText(this, "Submit failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

    private void showAbsentSummary(String period, int absentCount, List<String> absentNames) {
        if (isFinishing() || isDestroyed()) return;
        StringBuilder msg = new StringBuilder("Attendance submitted for ").append(period).append(".\n\n");
        if (absentCount == 0) {
            msg.append("All students are present!");
        } else {
            msg.append(absentCount).append(" student(s) absent:\n");
            for (String n : absentNames) msg.append("• ").append(n).append("\n");
            if (absentCount > absentNames.size())
                msg.append("...and ").append(absentCount - absentNames.size()).append(" more.");
            msg.append("\n\nYou can modify and re-submit after the 5-minute cooldown.");
        }
        new AlertDialog.Builder(this)
            .setTitle("Attendance Submitted")
            .setMessage(msg.toString())
            .setPositiveButton("OK", null)
            .show();
    }

    private void startScanner() {
        if (fullList.isEmpty()) {
            Toast.makeText(this, "No students to scan", Toast.LENGTH_SHORT).show();
            return;
        }
        HashMap<String, String> uucmsMap = new HashMap<>();
        for (StudentAttendanceAdapter.StudentAttendance s : fullList) {
            if (s.uucmsId != null) uucmsMap.put(s.uucmsId, s.name);
        }
        Intent intent = new Intent(this, com.procollegia.AttendanceScannerActivity.class);
        intent.putExtra("uucmsMap", uucmsMap);
        startActivityForResult(intent, 101);
    }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent d) {
        super.onActivityResult(req, res, d);
        if (req == 101 && android.app.Activity.RESULT_OK == res && d != null) {
            ArrayList<String> scanned = d.getStringArrayListExtra("scanned");
            if (scanned != null) {
                for (String uucms : scanned) {
                    for (StudentAttendanceAdapter.StudentAttendance s : fullList) {
                        if (uucms.equals(s.uucmsId)) s.status = "P";
                    }
                }
                filterList();
                Toast.makeText(this, "Marked " + scanned.size() + " students present", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
