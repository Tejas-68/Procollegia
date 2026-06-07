package com.procollegia.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.procollegia.R;
import com.procollegia.adapters.TeacherInternalsAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherInternalsFragment extends Fragment {

    // ── Views ──────────────────────────────────────────────────────────────────
    private TextView     chipYear1, chipYear2, chipYear3;
    private TextView     tvStepLabel, tvStudentCount, tvEmptyState;
    private LinearLayout llStep2;
    private Spinner      spSubject, spSection;
    private RecyclerView rvStudents;
    private ProgressBar  pbLoading;

    // ── Data ───────────────────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private String teacherUid;
    private String teacherDept = "";

    private final List<TeacherInternalsAdapter.InternalStudent> allStudents  = new ArrayList<>();
    private final List<TeacherInternalsAdapter.InternalStudent> studentList  = new ArrayList<>();
    private final List<String>                                  subjectNames = new ArrayList<>();
    private TeacherInternalsAdapter adapter;

    private int    selectedYear    = -1;
    private String selectedSubject = "";
    private String lastQueryKey    = "";
    private boolean allStudentsLoaded = false;

    private static final String ALL_SECTIONS = "All Sections";
    private static final String[] SECTIONS = {ALL_SECTIONS, "Sec A", "Sec B", "Sec C"};

    public TeacherInternalsFragment() {}

    // ── Lifecycle ──────────────────────────────────────────────────────────────
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_teacher_internals, container, false);

        db         = FirebaseFirestore.getInstance();
        teacherUid = FirebaseAuth.getInstance().getUid();

        chipYear1      = root.findViewById(R.id.chipYear1);
        chipYear2      = root.findViewById(R.id.chipYear2);
        chipYear3      = root.findViewById(R.id.chipYear3);
        tvStepLabel    = root.findViewById(R.id.tvStepLabel);
        tvStudentCount = root.findViewById(R.id.tvStudentCount);
        tvEmptyState   = root.findViewById(R.id.tvEmptyState);
        llStep2        = root.findViewById(R.id.llStep2);
        spSubject      = root.findViewById(R.id.spSubject);
        spSection      = root.findViewById(R.id.spSection);
        rvStudents     = root.findViewById(R.id.rvStudents);
        pbLoading      = root.findViewById(R.id.pbLoading);

        rvStudents.setLayoutManager(new LinearLayoutManager(getContext()));
        rvStudents.setNestedScrollingEnabled(false);

        // Section spinner — "All Sections" + "Sec A/B/C"
        spSection.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, SECTIONS));

        chipYear1.setOnClickListener(v -> selectYear(1));
        chipYear2.setOnClickListener(v -> selectYear(2));
        chipYear3.setOnClickListener(v -> selectYear(3));

        AdapterView.OnItemSelectedListener trigger = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int i, long l) { tryFilter(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        spSubject.setOnItemSelectedListener(trigger);
        spSection.setOnItemSelectedListener(trigger);

        loadTeacherDeptThenStudents();
        return root;
    }

    // ── Load teacher dept → then fetch all students ────────────────────────────
    private void loadTeacherDeptThenStudents() {
        if (teacherUid == null) return;
        showLoading(true);
        db.collection("users").document(teacherUid).get()
            .addOnSuccessListener(doc -> {
                if (!isAdded()) return;
                if (doc.exists()) {
                    teacherDept = doc.getString("department");
                    if (teacherDept == null) teacherDept = doc.getString("hodDepartment");
                    if (teacherDept == null) teacherDept = "";
                }
                fetchAllStudents();
            })
            .addOnFailureListener(e -> { if (isAdded()) fetchAllStudents(); });
    }

    /**
     * Fetch only STUDENTS for this department.
     * Single whereEqualTo("department") avoids composite index issues.
     * Role is checked in-memory (handles "Student" / "student" casing).
     */
    private void fetchAllStudents() {
        // Build the query — if dept is known use it, otherwise fetch all and filter by role
        com.google.firebase.firestore.Query query = db.collection("users");
        if (!teacherDept.isEmpty()) {
            query = query.whereEqualTo("department", teacherDept);
        }

        query.get()
            .addOnSuccessListener(qs -> {
                if (!isAdded()) return;
                showLoading(false);
                allStudents.clear();

                for (QueryDocumentSnapshot d : qs) {
                    try {
                        // ── Role filter (in-memory) ──
                        String role = d.getString("role");
                        if (role == null || !role.equalsIgnoreCase("student")) continue;

                        String name    = d.getString("name");
                        String uucms   = d.getString("uucmsId");
                        Object yearObj = d.get("year");
                        String year    = (yearObj != null) ? String.valueOf(yearObj).trim() : "";
                        String section = d.getString("section");
                        String dept    = d.getString("department");

                        allStudents.add(new TeacherInternalsAdapter.InternalStudent(
                                d.getId(),
                                name != null ? name : "Unknown",
                                uucms,
                                year,
                                section != null ? section.trim() : "",
                                dept    != null ? dept.trim()    : ""
                        ));
                    } catch (Exception ignored) {}
                }

                allStudentsLoaded = true;
                // If a year chip was already tapped before fetch completed, re-trigger
                if (selectedYear > 0) tryFilter();
            })
            .addOnFailureListener(e -> {
                if (isAdded()) {
                    showLoading(false);
                    showMsg("Failed to load students: " + e.getMessage());
                }
            });
    }

    // ── Year chip ──────────────────────────────────────────────────────────────
    private void selectYear(int year) {
        selectedYear = year;
        lastQueryKey = "";

        setChip(chipYear1, year == 1);
        setChip(chipYear2, year == 2);
        setChip(chipYear3, year == 3);
        tvStepLabel.setText("Year " + year + " selected");

        studentList.clear();
        updateAdapter();
        rvStudents.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.VISIBLE);
        tvEmptyState.setText("Loading subjects for Year " + year + "...");
        llStep2.setVisibility(View.GONE);

        loadSubjectsForYear(year);
    }

    private void setChip(TextView chip, boolean active) {
        chip.setBackgroundResource(active ? R.drawable.bg_pill_active : R.drawable.bg_pill_inactive);
        chip.setTextColor(ContextCompat.getColor(requireContext(),
                active ? R.color.text_on_accent : R.color.text_secondary));
    }

    // ── Load subjects ──────────────────────────────────────────────────────────
    private void loadSubjectsForYear(int year) {
        if (teacherDept.isEmpty()) {
            tvEmptyState.setText("Waiting for profile...");
            return;
        }
        String docId = teacherDept.replaceAll("\\s+", "_") + "_" + year;
        showLoading(true);

        db.collection("subjects").document(docId).get()
            .addOnSuccessListener(doc -> {
                if (!isAdded()) return;
                showLoading(false);
                subjectNames.clear();
                if (doc.exists()) {
                    Object raw = doc.get("subjectList");
                    if (raw instanceof List) {
                        for (Object item : (List<?>) raw)
                            if (item instanceof String) subjectNames.add((String) item);
                    }
                }
                if (subjectNames.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    tvEmptyState.setText("No subjects for Year " + year + ".\n"
                            + "HOD needs to add subjects in Academics → Subjects.");
                    llStep2.setVisibility(View.GONE);
                } else {
                    spSubject.setAdapter(new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_spinner_dropdown_item, subjectNames));
                    llStep2.setVisibility(View.VISIBLE);
                    tvEmptyState.setVisibility(View.GONE);
                    tvStudentCount.setText(subjectNames.size() + " subjects — pick subject and section");
                }
            })
            .addOnFailureListener(e -> {
                if (!isAdded()) return;
                showLoading(false);
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("Failed to load subjects.");
            });
    }

    // ── Try filter whenever spinners change ────────────────────────────────────
    private void tryFilter() {
        if (selectedYear < 1) return;
        if (spSubject.getSelectedItem() == null || spSection.getSelectedItem() == null) return;

        String subject = spSubject.getSelectedItem().toString().trim();
        String section = spSection.getSelectedItem().toString().trim();
        if (subject.isEmpty()) return;

        String queryKey = selectedYear + "|" + subject + "|" + section;
        if (queryKey.equals(lastQueryKey)) return;
        lastQueryKey    = queryKey;
        selectedSubject = subject;

        if (!allStudentsLoaded) {
            tvStudentCount.setText("Loading students, please wait...");
            showLoading(true);
            return;
        }

        filterAndShow(String.valueOf(selectedYear), section, subject);
    }

    /**
     * Filter allStudents in-memory:
     *
     *  Year:    chip="3"  stored="3rd Year"  → stored.startsWith("3") ✓
     *  Section: "All Sections" → skip section filter
     *           "Sec A" → stored.toLowerCase().contains("a") ✓
     *                      handles "Sec A", "sec a", "Section A", "A" all correctly
     */
    private void filterAndShow(String yearNum, String section, String subject) {
        showLoading(true);
        rvStudents.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);
        studentList.clear();

        boolean allSections = section.equals(ALL_SECTIONS);
        // Extract the section letter from spinner value: "Sec A" → "a"
        String secLetter = section.replace("Sec ", "").replace("Section ", "").trim().toLowerCase();

        for (TeacherInternalsAdapter.InternalStudent s : allStudents) {
            // ── Year match: "3rd Year".startsWith("3") ──
            if (s.year == null || !s.year.startsWith(yearNum)) continue;

            // ── Section match ──
            if (!allSections) {
                // Compare only the LAST character of the stored section
                // "Sec A" → last char = "a", "Sec B" → "b", "A" → "a"
                // DO NOT use .contains() — "sec".contains("c") is true for ALL sections!
                String stored = s.section != null ? s.section.trim() : "";
                if (stored.isEmpty()) continue;
                String storedLetter = String.valueOf(stored.charAt(stored.length() - 1)).toLowerCase();
                if (!storedLetter.equals(secLetter)) continue;
            }

            studentList.add(s);
        }

        showLoading(false);

        if (studentList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText(
                    "No students found\n"
                    + "Year " + yearNum + " · " + section + "\n"
                    + "Dept: " + teacherDept + "\n"
                    + "(" + allStudents.size() + " students loaded total)"
            );
            tvStudentCount.setText("0 students");
            return;
        }

        tvStudentCount.setText(studentList.size() + " students"
                + (allSections ? " (all sections)" : " · " + section));
        updateAdapter();
        rvStudents.setVisibility(View.VISIBLE);
        loadMaxMarksThenMarks(subject, yearNum);
    }

    // ── Load HOD max marks then existing student marks ─────────────────────────
    private void loadMaxMarksThenMarks(String subject, String yearNum) {
        if (hodDept == null) hodDept = teacherDept;
        String docId = teacherDept.replaceAll("\\s+", "_");
        String prefix = "maxMarks_year" + yearNum + "_";

        db.collection("departmentSettings").document(docId).get()
            .addOnSuccessListener(doc -> {
                if (!isAdded()) return;
                int ia1 = 25, ia2 = 25, assign = 10;
                if (doc.exists()) {
                    Long v1 = doc.getLong(prefix + "ia1");
                    Long v2 = doc.getLong(prefix + "ia2");
                    Long v3 = doc.getLong(prefix + "assignment");
                    if (v1 != null) ia1 = v1.intValue();
                    if (v2 != null) ia2 = v2.intValue();
                    if (v3 != null) assign = v3.intValue();
                }
                if (adapter != null) {
                    adapter.setMaxMarks(new TeacherInternalsAdapter.MaxMarks(ia1, ia2, assign));
                }
                loadExistingMarks(subject);
            })
            .addOnFailureListener(e -> loadExistingMarks(subject)); // use defaults on error
    }

    private String hodDept = null;

    // ── Load existing marks ────────────────────────────────────────────────────
    private void loadExistingMarks(String subject) {
        db.collection("internals")
            .whereEqualTo("subject", subject)
            .get()
            .addOnSuccessListener(qs -> {
                if (!isAdded()) return;
                Map<String, QueryDocumentSnapshot> marksMap = new HashMap<>();
                for (QueryDocumentSnapshot d : qs) {
                    String sid = d.getString("studentId");
                    if (sid != null) marksMap.put(sid, d);
                }
                for (TeacherInternalsAdapter.InternalStudent s : studentList) {
                    if (marksMap.containsKey(s.id)) {
                        QueryDocumentSnapshot d = marksMap.get(s.id);
                        s.ia1        = safeInt(d, "ia1");
                        s.ia2        = safeInt(d, "ia2");
                        s.assignment = safeInt(d, "assignment");
                    } else {
                        s.ia1 = s.ia2 = s.assignment = 0;
                    }
                }
                updateAdapter();
            })
            .addOnFailureListener(e -> { /* show students without marks */ });
    }

    // ── Save marks ─────────────────────────────────────────────────────────────
    private void saveMarks(TeacherInternalsAdapter.InternalStudent student,
                           int ia1, int ia2, int assignment) {
        String safeSubject = selectedSubject.replaceAll("[^a-zA-Z0-9]", "_");
        String safeYear    = student.year    != null ? student.year.replaceAll("\\s+", "_")    : "0";
        String safeSection = student.section != null ? student.section.replaceAll("\\s+", "_") : "X";
        String docId = student.id + "_" + safeSubject + "_" + safeYear + "_" + safeSection;

        Map<String, Object> data = new HashMap<>();
        data.put("studentId",   student.id);
        data.put("studentName", student.name);
        data.put("uucmsId",     student.uucmsId);
        data.put("subject",     selectedSubject);
        data.put("department",  teacherDept);
        data.put("year",        student.year);
        data.put("section",     student.section);
        data.put("teacherUid",  teacherUid);
        data.put("ia1",         ia1);
        data.put("ia2",         ia2);
        data.put("assignment",  assignment);
        data.put("total",       ia1 + ia2 + assignment);
        data.put("updatedAt",   new java.util.Date());

        db.collection("internals").document(docId)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener(v -> {
                student.ia1 = ia1; student.ia2 = ia2; student.assignment = assignment;
                showMsg("Saved for " + student.name);
            })
            .addOnFailureListener(e -> showMsg("Save failed: " + e.getMessage()));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private void updateAdapter() {
        if (!isAdded()) return;
        if (adapter == null) {
            adapter = new TeacherInternalsAdapter(studentList, this::saveMarks);
            rvStudents.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private void showLoading(boolean show) {
        if (pbLoading != null) pbLoading.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showMsg(String msg) {
        if (getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private int safeInt(QueryDocumentSnapshot doc, String field) {
        Long val = doc.getLong(field);
        return val != null ? val.intValue() : 0;
    }
}
