package com.procollegia.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.procollegia.R;
import com.procollegia.adapters.SubjectAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TeacherSubjectsFragment
 *
 * Lets a teacher manage subjects by year.
 *
 * Firestore schema:
 *   Collection: "subjects"
 *   Doc ID:  "{department}_{year}"      e.g.  "BCA_1"
 *   Fields:  subjectList: List<String>  (ordered list of subject names)
 *
 * On every save/delete/edit, the teacher's own `assignedSubjects` field on their
 * users doc is also refreshed — so the Internals spinner stays in sync automatically.
 */
public class TeacherSubjectsFragment extends Fragment implements SubjectAdapter.Listener {

    // ── Views ──
    private TextView   chipYear1, chipYear2, chipYear3, tvSubjectInfo, tvEmpty;
    private EditText   etNewSubject;
    private Button     btnAdd;
    private RecyclerView rvSubjects;
    private ProgressBar  pbSubjects;

    // ── State ──
    private final List<String> subjectList = new ArrayList<>();
    private SubjectAdapter adapter;

    private FirebaseFirestore db;
    private String teacherUid;
    private String teacherDept = "";
    private int    selectedYear = 1; // 1, 2 or 3

    public TeacherSubjectsFragment() {}

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_teacher_subjects, container, false);

        db         = FirebaseFirestore.getInstance();
        teacherUid = FirebaseAuth.getInstance().getUid();

        chipYear1     = root.findViewById(R.id.chipYear1);
        chipYear2     = root.findViewById(R.id.chipYear2);
        chipYear3     = root.findViewById(R.id.chipYear3);
        tvSubjectInfo = root.findViewById(R.id.tvSubjectInfo);
        tvEmpty       = root.findViewById(R.id.tvEmpty);
        etNewSubject  = root.findViewById(R.id.etNewSubject);
        btnAdd        = root.findViewById(R.id.btnAddSubject);
        rvSubjects    = root.findViewById(R.id.rvSubjects);
        pbSubjects    = root.findViewById(R.id.pbSubjects);

        rvSubjects.setLayoutManager(new LinearLayoutManager(getContext()));

        // Year chip clicks
        chipYear1.setOnClickListener(v -> selectYear(1));
        chipYear2.setOnClickListener(v -> selectYear(2));
        chipYear3.setOnClickListener(v -> selectYear(3));

        // Add subject button
        btnAdd.setOnClickListener(v -> addSubject());

        // Allow pressing Done on keyboard to add
        etNewSubject.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addSubject();
                return true;
            }
            return false;
        });

        // Load teacher's department first, then load subjects
        loadTeacherDept();

        return root;
    }

    // ── Load teacher department ───────────────────────────────────────────────
    private void loadTeacherDept() {
        if (teacherUid == null) return;
        showLoading(true);

        db.collection("users").document(teacherUid).get()
            .addOnSuccessListener(doc -> {
                if (!isAdded()) return;
                showLoading(false);
                if (doc.exists()) {
                    teacherDept = doc.getString("department");
                    if (teacherDept == null) teacherDept = doc.getString("hodDepartment");
                    if (teacherDept == null) teacherDept = "";
                }
                selectYear(1); // default to Year 1
            })
            .addOnFailureListener(e -> {
                showLoading(false);
                showMsg("Could not load teacher profile: " + e.getMessage());
            });
    }

    // ── Year selector ─────────────────────────────────────────────────────────
    private void selectYear(int year) {
        selectedYear = year;
        updateYearChips();
        tvSubjectInfo.setText("Subjects for Year " + year
                + (teacherDept.isEmpty() ? "" : "  (" + teacherDept + ")"));
        loadSubjectsForYear(year);
    }

    private void updateYearChips() {
        TextView[] chips = {chipYear1, chipYear2, chipYear3};
        for (int i = 0; i < chips.length; i++) {
            boolean active = (i + 1) == selectedYear;
            chips[i].setBackgroundResource(active ? R.drawable.bg_pill_active : R.drawable.bg_pill_inactive);
            chips[i].setTextColor(ContextCompat.getColor(requireContext(),
                    active ? R.color.text_on_accent : R.color.text_secondary));
        }
    }

    // ── Load subjects from Firestore ──────────────────────────────────────────
    private void loadSubjectsForYear(int year) {
        if (teacherDept.isEmpty()) return;
        showLoading(true);
        subjectList.clear();
        updateAdapter();

        String docId = docId(year);
        db.collection("subjects").document(docId).get()
            .addOnSuccessListener(doc -> {
                if (!isAdded()) return;
                showLoading(false);
                if (doc.exists()) {
                    Object raw = doc.get("subjectList");
                    if (raw instanceof List) {
                        for (Object item : (List<?>) raw) {
                            if (item instanceof String) subjectList.add((String) item);
                        }
                    }
                }
                updateAdapter();
                updateEmptyState();
            })
            .addOnFailureListener(e -> {
                showLoading(false);
                updateEmptyState();
            });
    }

    // ── Add subject ───────────────────────────────────────────────────────────
    private void addSubject() {
        String name = etNewSubject.getText().toString().trim();
        if (name.isEmpty()) {
            etNewSubject.setError("Enter a subject name");
            return;
        }
        // Duplicate check (case-insensitive)
        for (String s : subjectList) {
            if (s.equalsIgnoreCase(name)) {
                showMsg("\"" + name + "\" already exists");
                return;
            }
        }

        subjectList.add(name);
        etNewSubject.setText("");
        updateAdapter();
        updateEmptyState();
        saveSubjectList();
    }

    // ── SubjectAdapter.Listener ───────────────────────────────────────────────
    @Override
    public void onDelete(int position, String subjectName) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Remove Subject")
            .setMessage("Remove \"" + subjectName + "\" from Year " + selectedYear + "?")
            .setPositiveButton("Remove", (d, w) -> {
                subjectList.remove(position);
                updateAdapter();
                updateEmptyState();
                saveSubjectList();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public void onEdit(int position, String oldName, String newName) {
        if (newName.isEmpty()) {
            showMsg("Subject name cannot be empty");
            return;
        }
        // Duplicate check
        for (int i = 0; i < subjectList.size(); i++) {
            if (i != position && subjectList.get(i).equalsIgnoreCase(newName)) {
                showMsg("\"" + newName + "\" already exists");
                return;
            }
        }
        subjectList.set(position, newName);
        saveSubjectList();
        showMsg("Renamed to \"" + newName + "\"");
    }

    // ── Save to Firestore ─────────────────────────────────────────────────────
    /**
     * Saves the current subjectList for the selected year under:
     *   subjects/{dept}_{year}  →  subjectList: [...]
     *
     * Also syncs to the teacher's own assignedSubjects array by merging
     * all subjects across all 3 years (so internals spinner stays up to date).
     */
    private void saveSubjectList() {
        if (teacherDept.isEmpty()) return;

        String docId = docId(selectedYear);
        Map<String, Object> data = new HashMap<>();
        data.put("subjectList", new ArrayList<>(subjectList));
        data.put("department",  teacherDept);
        data.put("year",        String.valueOf(selectedYear));
        data.put("updatedBy",   teacherUid);

        db.collection("subjects").document(docId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener(v -> {
                showMsg("Saved!");
                // Sync teacher's assignedSubjects so Internals spinner is up to date
                syncAssignedSubjects();
            })
            .addOnFailureListener(e -> showMsg("Save failed: " + e.getMessage()));
    }

    /**
     * Reads subjects from all 3 year docs for this department and merges them
     * into the teacher's own `assignedSubjects` array field.
     */
    private void syncAssignedSubjects() {
        if (teacherUid == null || teacherDept.isEmpty()) return;

        List<String> merged = new ArrayList<>();

        // Fetch all 3 year docs, then merge
        int[] pending = {3}; // count down

        for (int yr = 1; yr <= 3; yr++) {
            final int y = yr;
            db.collection("subjects").document(docId(y)).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Object raw = doc.get("subjectList");
                        if (raw instanceof List) {
                            for (Object item : (List<?>) raw) {
                                String s = String.valueOf(item);
                                if (!merged.contains(s)) merged.add(s);
                            }
                        }
                    }
                    pending[0]--;
                    if (pending[0] == 0) {
                        // All 3 fetched — update teacher doc
                        db.collection("users").document(teacherUid)
                            .update("assignedSubjects", merged)
                            .addOnFailureListener(e -> { /* non-critical — silent fail */ });
                    }
                })
                .addOnFailureListener(e -> {
                    pending[0]--;
                    if (pending[0] == 0 && !merged.isEmpty()) {
                        db.collection("users").document(teacherUid)
                            .update("assignedSubjects", merged);
                    }
                });
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    /** Firestore doc ID for subjects collection */
    private String docId(int year) {
        return teacherDept.replaceAll("\\s+", "_") + "_" + year;
    }

    private void updateAdapter() {
        if (!isAdded()) return;
        if (adapter == null) {
            adapter = new SubjectAdapter(subjectList, this);
            rvSubjects.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private void updateEmptyState() {
        if (!isAdded()) return;
        boolean empty = subjectList.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvSubjects.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showLoading(boolean show) {
        if (pbSubjects != null) pbSubjects.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showMsg(String msg) {
        if (getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
