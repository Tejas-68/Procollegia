package com.procollegia.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.procollegia.R;
import com.procollegia.adapters.StudentAttendanceAdapter;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * TeacherAttendanceFragment
 *
 * Features:
 *  - Period dropdown (P1–P6).
 *  - CoordinatorLayout collapse: scroll list → header slides out, full-screen list.
 *  - "Select All Present" / "Deselect All (Absent)" toggle button.
 *  - 5 PM cutoff, 5-minute cooldown, absent-count dialog.
 *  - PDF export.
 */
public class TeacherAttendanceFragment extends Fragment {

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

    // ── Views ──
    private Spinner    spPeriod, spYear, spSection;
    private EditText   etSearch;
    private RecyclerView rvStudents;
    private TextView   tvSummary, tvCooldown, tvBanner, tvSelectAllLabel, tvSelectAllSub;
    private LinearLayout btnSelectAll, llBanner;
    private Button     btnSubmit;
    private ProgressBar pbLoading;

    // ── State ──
    private StudentAttendanceAdapter adapter;
    private final List<StudentAttendanceAdapter.StudentAttendance> fullList     = new ArrayList<>();
    private final List<StudentAttendanceAdapter.StudentAttendance> filteredList = new ArrayList<>();

    private FirebaseFirestore db;
    private String uid;
    private CountDownTimer cooldownTimer;
    private SharedPreferences prefs;

    /** true = clicking Select All marks everyone Present; false = marks everyone Absent */
    private boolean selectAllPresent = true;

    public TeacherAttendanceFragment() {}

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_teacher_attendance, container, false);

        spPeriod       = root.findViewById(R.id.spinnerPeriod);
        spYear         = root.findViewById(R.id.spinnerYear);
        spSection      = root.findViewById(R.id.spinnerSection);
        etSearch       = root.findViewById(R.id.etSearchStudent);
        rvStudents     = root.findViewById(R.id.rvStudentAttendance);
        tvSummary      = root.findViewById(R.id.tvAttendanceSummary);
        btnSubmit      = root.findViewById(R.id.btnSubmitAttendance);
        pbLoading      = root.findViewById(R.id.pbLoading);
        tvCooldown     = root.findViewById(R.id.tvCooldown);
        llBanner       = root.findViewById(R.id.llBanner);
        tvBanner       = root.findViewById(R.id.tvBanner);
        btnSelectAll   = root.findViewById(R.id.btnSelectAll);
        tvSelectAllLabel = root.findViewById(R.id.tvSelectAllLabel);
        tvSelectAllSub   = root.findViewById(R.id.tvSelectAllSub);

        rvStudents.setLayoutManager(new LinearLayoutManager(getContext()));
        // Important: disable nested scrolling on RV so NestedScrollView handles it
        rvStudents.setNestedScrollingEnabled(false);

        db    = FirebaseFirestore.getInstance();
        uid   = FirebaseAuth.getInstance().getUid();
        prefs = requireContext().getSharedPreferences("attendance_prefs", Context.MODE_PRIVATE);

        setupSpinners();
        setupSearch();
        setupSelectAll();

        root.findViewById(R.id.btnExportExcel).setOnClickListener(v -> exportToPdf());
        btnSubmit.setOnClickListener(v -> submitAttendance());
        root.findViewById(R.id.fabCameraScanner).setOnClickListener(v -> startScanner());

        checkCutoff();
        loadStudentsWithLeaves();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cooldownTimer != null) cooldownTimer.cancel();
    }

    // ── Select All ────────────────────────────────────────────────────────────
    private void setupSelectAll() {
        updateSelectAllLabel();
        btnSelectAll.setOnClickListener(v -> {
            if (adapter == null || filteredList.isEmpty()) return;
            if (selectAllPresent) {
                adapter.selectAll("P");
            } else {
                adapter.selectAll("A");
            }
            selectAllPresent = !selectAllPresent;
            updateSelectAllLabel();
        });
    }

    private void updateSelectAllLabel() {
        if (tvSelectAllLabel == null) return;
        if (selectAllPresent) {
            tvSelectAllLabel.setText("Select All");
            tvSelectAllSub.setText("(Present)");
            tvSelectAllLabel.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.accent_blue));
        } else {
            tvSelectAllLabel.setText("Deselect All");
            tvSelectAllSub.setText("(Absent)");
            tvSelectAllLabel.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.accent_red));
        }
    }

    // ── Spinners ──────────────────────────────────────────────────────────────
    private void setupSpinners() {
        spPeriod.setAdapter(new ArrayAdapter<>(requireContext(),
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
        String[] sections = {"All Sections", "A", "B", "C"};
        spYear.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, years));
        spSection.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, sections));

        AdapterView.OnItemSelectedListener filterListen = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int i, long l) { filterList(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        spYear.setOnItemSelectedListener(filterListen);
        spSection.setOnItemSelectedListener(filterListen);
    }

    private void updatePeriodSpinner() {
        if (!isAdded()) return;
        spPeriod.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, periodsList));
        autoSelectCurrentPeriod();
    }

    private void autoSelectCurrentPeriod() {
        try {
            int currentMinutes = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) * 60 + java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE);
            for (int i = 0; i < periodsList.size(); i++) {
                String p = periodsList.get(i);
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

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { filterList(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ── 5 PM cutoff ──────────────────────────────────────────────────────────
    private boolean isPastCutoff() {
        int hour = Integer.parseInt(new SimpleDateFormat("HH", Locale.getDefault()).format(new Date()));
        return hour >= CUTOFF_HOUR;
    }

    private void checkCutoff() {
        if (isPastCutoff()) {
            showBanner("Attendance is disabled after 5:00 PM", R.color.accent_red);
            lockSubmit("Disabled after 5 PM");
        }
    }

    // ── Submit state: cooldown OR already submitted ───────────────────────────
    private String getCooldownKey() {
        String today  = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String period = selectedPeriodShort();
        return uid + "_" + today + "_" + period;
    }

    private void checkSubmitState() {
        if (isPastCutoff()) return;
        long submitTime = prefs.getLong(getCooldownKey(), 0L);
        long now        = System.currentTimeMillis();
        long elapsed    = now - submitTime;

        if (submitTime > 0 && elapsed < COOLDOWN_MS) {
            lockForCooldown(COOLDOWN_MS - elapsed);
        } else if (submitTime > 0) {
            enableResubmit();
        } else {
            resetSubmitButton();
        }
    }

    private void lockForCooldown(long remainingMs) {
        if (!isAdded()) return;
        lockSubmit("Submitted — cooldown active");
        tvCooldown.setVisibility(View.VISIBLE);
        showBanner("Attendance submitted. Re-submit after cooldown.", R.color.accent_orange);

        if (cooldownTimer != null) cooldownTimer.cancel();
        cooldownTimer = new CountDownTimer(remainingMs, 1000) {
            @Override public void onTick(long ms) {
                if (!isAdded()) return;
                tvCooldown.setText(String.format(Locale.getDefault(),
                        "Re-submit available in %d:%02d", ms / 60000, (ms % 60000) / 1000));
            }
            @Override public void onFinish() { if (isAdded()) enableResubmit(); }
        }.start();
    }

    private void enableResubmit() {
        if (!isAdded()) return;
        tvCooldown.setVisibility(View.GONE);
        showBanner("Cooldown over — you can re-submit now.", R.color.accent_green);
        unlockSubmit("Re-submit Attendance");
    }

    private void resetSubmitButton() {
        if (!isAdded()) return;
        tvCooldown.setVisibility(View.GONE);
        tvBanner.setVisibility(View.GONE);
        unlockSubmit("Submit Attendance");
    }

    // ── Banner helpers ────────────────────────────────────────────────────────
    private void showBanner(String msg, int colorRes) {
        if (!isAdded()) return;
        tvBanner.setVisibility(View.VISIBLE);
        tvBanner.setText(msg);
        tvBanner.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
    }

    private void lockSubmit(String label) {
        btnSubmit.setEnabled(false);
        btnSubmit.setText(label);
        btnSubmit.setBackgroundTintList(
                ContextCompat.getColorStateList(requireContext(), R.color.text_muted));
    }

    private void unlockSubmit(String label) {
        btnSubmit.setEnabled(true);
        btnSubmit.setText(label);
        btnSubmit.setBackgroundTintList(
                ContextCompat.getColorStateList(requireContext(), R.color.accent_blue));
    }

    // ── Student loading ───────────────────────────────────────────────────────
    private void loadStudentsWithLeaves() {
        if (!fullList.isEmpty()) { checkLeavesForToday(); return; }
        pbLoading.setVisibility(View.VISIBLE);

        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            if (!isAdded()) return;
            if (userDoc.exists()) {
                String dept = userDoc.getString("department");
                if (dept == null) dept = userDoc.getString("hodDepartment");
                if (dept != null && !dept.isEmpty()) {
                    db.collection("departmentSettings").document(dept).get().addOnSuccessListener(deptDoc -> {
                        if (deptDoc.exists() && deptDoc.contains("periods")) {
                            List<String> fetched = (List<String>) deptDoc.get("periods");
                            if (fetched != null && !fetched.isEmpty()) {
                                periodsList.clear();
                                periodsList.addAll(fetched);
                                updatePeriodSpinner();
                            }
                        }
                    });
                }
            }
            fetchStudents();
        }).addOnFailureListener(e -> fetchStudents());
    }

    private void fetchStudents() {
        db.collection("users")
            .whereIn("role", Arrays.asList("Student", "student"))
            .get()
            .addOnSuccessListener(qs -> {
                if (!isAdded()) return;
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
            .addOnFailureListener(e -> { if (isAdded()) checkLeavesForToday(); });
    }

    private void checkLeavesForToday() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        db.collection("leaveRequests")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener(qs -> {
                if (!isAdded()) return;
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
                if (isAdded()) { pbLoading.setVisibility(View.GONE); filterList(); }
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
            boolean matchYear = year.equals("All Years") || (s.year != null &&
                    (s.year.equals(year) || s.year.startsWith(year.substring(0, 1))));
            String sSec = (s.section != null) ? s.section.toLowerCase() : "";
            boolean matchSec = sec.equals("All Sections") ||
                    sSec.contains(sec.toLowerCase().replace("sec ", ""));
            String name = (s.name != null) ? s.name.toLowerCase() : "";
            String code = (s.uucmsId != null) ? s.uucmsId.toLowerCase() : "";
            boolean matchQ = query.isEmpty() || name.contains(query) || code.contains(query);

            if (matchYear && matchSec && matchQ) filteredList.add(s);
        }

        if (adapter == null) {
            adapter = new StudentAttendanceAdapter(filteredList, this::updateSummary);
            rvStudents.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
        updateSummary();

        // Reset select-all toggle when filter changes (start fresh from "Select All Present")
        selectAllPresent = true;
        updateSelectAllLabel();
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
            Toast.makeText(getContext(), "Attendance is closed after 5 PM.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (filteredList.isEmpty()) {
            Toast.makeText(getContext(), "No students to submit.", Toast.LENGTH_SHORT).show();
            return;
        }

        String date   = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String period = selectedPeriodShort();

        WriteBatch batch = db.batch();
        for (StudentAttendanceAdapter.StudentAttendance s : filteredList) {
            String docId = s.id + "_" + date + "_" + period.replace(" ", "_");
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
            rec.put("teacherUid", uid);
            rec.put("timestamp",  new Date());
            batch.set(db.collection("attendanceRecords").document(docId), rec);
        }

        btnSubmit.setEnabled(false);
        batch.commit()
            .addOnSuccessListener(aVoid -> {
                if (!isAdded()) return;
                prefs.edit().putLong(getCooldownKey(), System.currentTimeMillis()).apply();

                int absentCount = 0;
                List<String> absentNames = new ArrayList<>();
                for (StudentAttendanceAdapter.StudentAttendance s : filteredList) {
                    if ("A".equals(s.status) && !s.isOnLeave) {
                        absentCount++;
                        if (absentNames.size() < 10) absentNames.add(s.name != null ? s.name : "Unknown");
                    }
                }
                showAbsentSummary(absentCount, absentNames);
                lockForCooldown(COOLDOWN_MS);
            })
            .addOnFailureListener(e -> {
                if (!isAdded()) return;
                btnSubmit.setEnabled(true);
                Toast.makeText(getContext(), "Submit failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

    private void showAbsentSummary(int absentCount, List<String> absentNames) {
        if (!isAdded()) return;
        StringBuilder msg = new StringBuilder();
        msg.append("Attendance submitted for ").append(selectedPeriodShort()).append(".\n\n");
        if (absentCount == 0) {
            msg.append("All students are present!");
        } else {
            msg.append(absentCount).append(" student(s) absent:\n");
            for (String n : absentNames) msg.append("• ").append(n).append("\n");
            if (absentCount > absentNames.size())
                msg.append("...and ").append(absentCount - absentNames.size()).append(" more.");
            msg.append("\n\nYou can modify and re-submit after the 5-minute cooldown.");
        }
        new AlertDialog.Builder(requireContext())
            .setTitle("Attendance Submitted")
            .setMessage(msg.toString())
            .setPositiveButton("OK", null)
            .show();
    }

    private String selectedPeriodShort() {
        return "P" + (spPeriod.getSelectedItemPosition() + 1);
    }

    // ── PDF Export ────────────────────────────────────────────────────────────
    private void exportToPdf() {
        if (filteredList.isEmpty()) {
            Toast.makeText(getContext(), "No students to export", Toast.LENGTH_SHORT).show();
            return;
        }
        android.graphics.pdf.PdfDocument doc = new android.graphics.pdf.PdfDocument();
        android.graphics.pdf.PdfDocument.PageInfo pageInfo =
                new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create();
        android.graphics.pdf.PdfDocument.Page page = doc.startPage(pageInfo);
        android.graphics.Canvas canvas = page.getCanvas();
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(android.graphics.Color.BLACK);
        paint.setTextSize(20f); paint.setFakeBoldText(true);
        canvas.drawText("Attendance Report", 50, 50, paint);
        paint.setTextSize(13f); paint.setFakeBoldText(false);
        canvas.drawText("Period: " + spPeriod.getSelectedItem()
                + " | Year: " + spYear.getSelectedItem()
                + " | Sec: " + spSection.getSelectedItem(), 50, 80, paint);
        int y = 120;
        paint.setFakeBoldText(true);
        canvas.drawText("Name", 50, y, paint);
        canvas.drawText("UUCMS ID", 250, y, paint);
        canvas.drawText("Status", 420, y, paint);
        paint.setFakeBoldText(false);
        y += 24;
        for (StudentAttendanceAdapter.StudentAttendance s : filteredList) {
            canvas.drawText(s.name != null ? s.name : "N/A", 50, y, paint);
            canvas.drawText(s.uucmsId != null ? s.uucmsId : "N/A", 250, y, paint);
            canvas.drawText(s.isOnLeave ? "L(leave)" : s.status, 420, y, paint);
            y += 22;
            if (y > 800) {
                doc.finishPage(page);
                page = doc.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }
        }
        doc.finishPage(page);
        try {
            File file = new File(requireContext().getExternalFilesDir(null),
                    "Attendance_" + System.currentTimeMillis() + ".pdf");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            doc.writeTo(fos);
            doc.close();
            fos.close();
            Uri u = FileProvider.getUriForFile(requireContext(), "com.procollegia.fileprovider", file);
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("application/pdf");
            i.putExtra(Intent.EXTRA_STREAM, u);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i, "Export Attendance PDF"));
        } catch (IOException e) {
            Toast.makeText(getContext(), "PDF export failed", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Scanner ───────────────────────────────────────────────────────────────
    private void startScanner() {
        if (isPastCutoff()) {
            Toast.makeText(getContext(), "Attendance is closed after 5 PM.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 102);
            return;
        }
        HashMap<String, String> uucmsMap = new HashMap<>();
        for (StudentAttendanceAdapter.StudentAttendance s : fullList) {
            if (s.uucmsId != null) uucmsMap.put(s.uucmsId, s.name);
        }
        Intent intent = new Intent(getContext(), com.procollegia.AttendanceScannerActivity.class);
        intent.putExtra("uucmsMap", uucmsMap);
        startActivityForResult(intent, 101);
    }

    @Override
    public void onActivityResult(int req, int res, @Nullable Intent d) {
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
            }
        }
    }
}
