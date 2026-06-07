package com.procollegia;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.procollegia.adapters.StudentAttendanceAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PtAttendanceActivity extends AppCompatActivity {

    private Spinner spYear, spSection;
    private EditText etSearch;
    private RecyclerView rvStudents;
    private TextView tvSummary, tvSelectAllLabel, tvSelectAllSub;
    private LinearLayout btnSelectAll;
    private Button btnSubmit;
    private ProgressBar pbLoading;

    private StudentAttendanceAdapter adapter;
    private final List<StudentAttendanceAdapter.StudentAttendance> fullList = new ArrayList<>();
    private final List<StudentAttendanceAdapter.StudentAttendance> filteredList = new ArrayList<>();

    private FirebaseFirestore db;
    private String uid;
    private boolean selectAllPresent = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pt_attendance);

        spYear = findViewById(R.id.spinnerYear);
        spSection = findViewById(R.id.spinnerSection);
        etSearch = findViewById(R.id.etSearchStudent);
        rvStudents = findViewById(R.id.rvStudentAttendance);
        tvSummary = findViewById(R.id.tvAttendanceSummary);
        btnSubmit = findViewById(R.id.btnSubmitAttendance);
        pbLoading = findViewById(R.id.pbLoading);
        btnSelectAll = findViewById(R.id.btnSelectAll);
        tvSelectAllLabel = findViewById(R.id.tvSelectAllLabel);
        tvSelectAllSub = findViewById(R.id.tvSelectAllSub);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnExportExcel).setOnClickListener(v -> exportToPdf());
        findViewById(R.id.fabCameraScanner).setOnClickListener(v -> startScanner());
        btnSubmit.setOnClickListener(v -> submitAttendance());

        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        rvStudents.setNestedScrollingEnabled(false);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        setupSpinners();
        setupSearch();
        setupSelectAll();
        loadStudents();
    }

    private void setupSpinners() {
        List<String> years = Arrays.asList("All Years", "1st Year", "2nd Year", "3rd Year");
        ArrayAdapter<String> yAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, years);
        spYear.setAdapter(yAdapter);

        List<String> sections = Arrays.asList("All Sections", "Sec A", "Sec B", "Sec C");
        ArrayAdapter<String> sAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sections);
        spSection.setAdapter(sAdapter);

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { filterList(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        spYear.setOnItemSelectedListener(filterListener);
        spSection.setOnItemSelectedListener(filterListener);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterList(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupSelectAll() {
        btnSelectAll.setOnClickListener(v -> {
            for (StudentAttendanceAdapter.StudentAttendance s : filteredList) {
                if (!s.isOnLeave) s.status = selectAllPresent ? "P" : "A";
            }
            if (adapter != null) adapter.notifyDataSetChanged();
            updateSummary();

            selectAllPresent = !selectAllPresent;
            if (selectAllPresent) {
                tvSelectAllLabel.setText("Select All");
                tvSelectAllSub.setText("(Present)");
                tvSelectAllLabel.setTextColor(ContextCompat.getColor(this, R.color.accent_blue));
            } else {
                tvSelectAllLabel.setText("Deselect All");
                tvSelectAllSub.setText("(Absent)");
                tvSelectAllLabel.setTextColor(ContextCompat.getColor(this, R.color.accent_orange));
            }
        });
    }

    private void loadStudents() {
        pbLoading.setVisibility(View.VISIBLE);
        db.collection("users")
                .whereEqualTo("role", "Student")
                .get()
                .addOnSuccessListener(qs -> {
                    fullList.clear();
                    for (QueryDocumentSnapshot d : qs) {
                        StudentAttendanceAdapter.StudentAttendance sa = new StudentAttendanceAdapter.StudentAttendance(
                                d.getId(),
                                d.getString("name"),
                                d.getString("uucmsId"),
                                d.getString("year"),
                                d.getString("section")
                        );
                        // No leave logic for PT for now, just normal attendance
                        sa.isOnLeave = false; 
                        fullList.add(sa);
                    }
                    filterList();
                    pbLoading.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load students", Toast.LENGTH_SHORT).show();
                });
    }

    private void filterList() {
        filteredList.clear();
        String selectedYear = spYear.getSelectedItem().toString();
        String yearNum = selectedYear.equals("All Years") ? "" : selectedYear.substring(0, 1);
        
        String selSec = spSection.getSelectedItem().toString();
        String secLetter = selSec.equals("All Sections") ? "" : selSec.replace("Sec ", "").trim().toLowerCase();
        
        String query = etSearch.getText().toString().toLowerCase().trim();

        for (StudentAttendanceAdapter.StudentAttendance s : fullList) {
            // Year check
            if (!yearNum.isEmpty() && (s.year == null || !s.year.startsWith(yearNum))) continue;
            
            // Section check (handle 'c' substring bug correctly by comparing exactly or char match)
            if (!secLetter.isEmpty()) {
                if (s.section == null) continue;
                String stSec = s.section.trim().toLowerCase();
                if (stSec.startsWith("sec ")) stSec = stSec.replace("sec ", "");
                if (stSec.startsWith("section ")) stSec = stSec.replace("section ", "");
                if (!stSec.equals(secLetter)) continue;
            }

            // Search query
            if (!query.isEmpty()) {
                boolean matchName = s.name != null && s.name.toLowerCase().contains(query);
                boolean matchUucms = s.uucmsId != null && s.uucmsId.toLowerCase().contains(query);
                if (!matchName && !matchUucms) continue;
            }

            filteredList.add(s);
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
        int pres = 0, abs = 0;
        for (StudentAttendanceAdapter.StudentAttendance s : filteredList) {
            if ("P".equals(s.status)) pres++;
            else abs++;
        }
        tvSummary.setText("Present: " + pres + " | Absent: " + abs);
    }

    private void startScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 102);
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
        if (req == 101 && RESULT_OK == res && d != null) {
            ArrayList<String> scanned = d.getStringArrayListExtra("scanned");
            if (scanned != null) {
                for (String uucms : scanned) {
                    for (StudentAttendanceAdapter.StudentAttendance s : fullList) {
                        if (uucms.equals(s.uucmsId)) s.status = "P";
                    }
                }
                filterList(); // refresh UI
            }
        }
    }

    private void submitAttendance() {
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No students to mark", Toast.LENGTH_SHORT).show();
            return;
        }

        int absCount = 0;
        for (StudentAttendanceAdapter.StudentAttendance s : filteredList) {
            if (!"P".equals(s.status) && !s.isOnLeave) absCount++;
        }

        if (absCount > 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Submission")
                    .setMessage(absCount + " students are marked ABSENT. Are you sure you want to submit?")
                    .setPositiveButton("Submit", (d, w) -> executeSubmit())
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            executeSubmit();
        }
    }

    private void executeSubmit() {
        pbLoading.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        WriteBatch batch = db.batch();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        
        for (StudentAttendanceAdapter.StudentAttendance s : filteredList) {
            if (s.isOnLeave) continue;
            
            Map<String, Object> record = new HashMap<>();
            record.put("studentId", s.id);
            record.put("name", s.name);
            record.put("uucmsId", s.uucmsId);
            record.put("year", s.year);
            record.put("section", s.section);
            record.put("status", s.status);
            record.put("date", date);
            record.put("ptAdminUid", uid);
            record.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

            // unique ID to prevent duplicates for same student on same day
            String docId = s.id + "_" + date + "_pt";
            batch.set(db.collection("ptAttendanceRecords").document(docId), record);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    pbLoading.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Attendance Saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    pbLoading.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void exportToPdf() {
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No students to export", Toast.LENGTH_SHORT).show();
            return;
        }
        android.graphics.pdf.PdfDocument doc = new android.graphics.pdf.PdfDocument();
        android.graphics.pdf.PdfDocument.PageInfo pageInfo =
                new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create();
        android.graphics.pdf.PdfDocument.Page page = doc.startPage(pageInfo);
        android.graphics.Canvas canvas = page.getCanvas();
        android.graphics.Paint paint = new android.graphics.Paint();
        
        paint.setColor(android.graphics.Color.BLACK);
        paint.setTextSize(20f); 
        paint.setFakeBoldText(true);
        canvas.drawText("PT Attendance Report", 50, 50, paint);
        
        paint.setTextSize(13f); 
        paint.setFakeBoldText(false);
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        canvas.drawText("Date: " + date
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
            canvas.drawText(s.status, 420, y, paint);
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
            File file = new File(getExternalFilesDir(null), "PT_Attendance_" + System.currentTimeMillis() + ".pdf");
            FileOutputStream fos = new FileOutputStream(file);
            doc.writeTo(fos);
            doc.close();
            fos.close();
            
            Uri u = FileProvider.getUriForFile(this, "com.procollegia.fileprovider", file);
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("application/pdf");
            i.putExtra(Intent.EXTRA_STREAM, u);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i, "Export Attendance PDF"));
        } catch (IOException e) {
            Toast.makeText(this, "PDF export failed", Toast.LENGTH_SHORT).show();
        }
    }
}
