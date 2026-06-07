package com.procollegia.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.procollegia.R;

import java.util.HashMap;
import java.util.Map;

/**
 * HOD can configure max internal marks per year.
 * Stored in: departmentSettings/{dept}/maxMarks_{year}
 *   { ia1: 25, ia2: 25, assignment: 10 }
 *
 * Teachers and students read this doc to know the max for each component.
 */
public class HodInternalExamSubFragment extends Fragment {

    private TextView   chipY1, chipY2, chipY3;
    private EditText   etIa1, etIa2, etAssign;
    private TextView   tvTotal;
    private MaterialButton btnSave;
    private ProgressBar pb;

    private FirebaseFirestore db;
    private String hodDept = "";
    private int    selectedYear = 1;

    // Cache loaded values per year so switching chips doesn't require re-fetch every time
    private final int[][] cache = {{25, 25, 10}, {25, 25, 10}, {25, 25, 10}}; // [year-1][ia1,ia2,assign]
    private final boolean[] loaded = {false, false, false};

    public HodInternalExamSubFragment() {}

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_hod_internal_exam_sub, container, false);
        db = FirebaseFirestore.getInstance();

        chipY1   = root.findViewById(R.id.chipMaxYear1);
        chipY2   = root.findViewById(R.id.chipMaxYear2);
        chipY3   = root.findViewById(R.id.chipMaxYear3);
        etIa1    = root.findViewById(R.id.etMaxIa1);
        etIa2    = root.findViewById(R.id.etMaxIa2);
        etAssign = root.findViewById(R.id.etMaxAssignment);
        tvTotal  = root.findViewById(R.id.tvTotalMax);
        btnSave  = root.findViewById(R.id.btnSaveMaxMarks);
        pb       = root.findViewById(R.id.pbMaxMarks);

        // Live total update
        TextWatcher tw = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable e) { updateTotal(); }
        };
        etIa1.addTextChangedListener(tw);
        etIa2.addTextChangedListener(tw);
        etAssign.addTextChangedListener(tw);

        chipY1.setOnClickListener(v -> selectYear(1));
        chipY2.setOnClickListener(v -> selectYear(2));
        chipY3.setOnClickListener(v -> selectYear(3));

        btnSave.setOnClickListener(v -> saveConfig());

        // Load HOD dept then load Year 1 config by default
        loadHodDept();
        return root;
    }

    private void loadHodDept() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        pb.setVisibility(View.VISIBLE);
        db.collection("users").document(uid).get()
            .addOnSuccessListener(doc -> {
                if (!isAdded()) return;
                if (doc.exists()) {
                    hodDept = doc.getString("hodDepartment");
                    if (hodDept == null) hodDept = doc.getString("department");
                    if (hodDept == null) hodDept = "";
                }
                loadConfigForYear(1);
            })
            .addOnFailureListener(e -> {
                if (isAdded()) { pb.setVisibility(View.GONE); showMsg("Failed to load profile"); }
            });
    }

    private void selectYear(int year) {
        selectedYear = year;
        setChip(chipY1, year == 1);
        setChip(chipY2, year == 2);
        setChip(chipY3, year == 3);
        if (loaded[year - 1]) {
            // Already cached — just fill fields
            fillFields(cache[year - 1][0], cache[year - 1][1], cache[year - 1][2]);
        } else {
            loadConfigForYear(year);
        }
    }

    private void loadConfigForYear(int year) {
        if (hodDept.isEmpty()) return;
        pb.setVisibility(View.VISIBLE);
        String docId = hodDept.replaceAll("\\s+", "_");

        db.collection("departmentSettings").document(docId).get()
            .addOnSuccessListener(doc -> {
                if (!isAdded()) return;
                pb.setVisibility(View.GONE);

                int ia1 = 25, ia2 = 25, assign = 10; // defaults
                if (doc.exists()) {
                    String prefix = "maxMarks_year" + year + "_";
                    Long v1 = doc.getLong(prefix + "ia1");
                    Long v2 = doc.getLong(prefix + "ia2");
                    Long v3 = doc.getLong(prefix + "assignment");
                    if (v1 != null) ia1 = v1.intValue();
                    if (v2 != null) ia2 = v2.intValue();
                    if (v3 != null) assign = v3.intValue();
                }
                cache[year - 1][0] = ia1;
                cache[year - 1][1] = ia2;
                cache[year - 1][2] = assign;
                loaded[year - 1]   = true;
                fillFields(ia1, ia2, assign);
            })
            .addOnFailureListener(e -> {
                if (!isAdded()) return;
                pb.setVisibility(View.GONE);
                fillFields(25, 25, 10); // show defaults on error
            });
    }

    private void fillFields(int ia1, int ia2, int assign) {
        etIa1.setText(String.valueOf(ia1));
        etIa2.setText(String.valueOf(ia2));
        etAssign.setText(String.valueOf(assign));
        updateTotal();
        setChip(chipY1, selectedYear == 1);
        setChip(chipY2, selectedYear == 2);
        setChip(chipY3, selectedYear == 3);
    }

    private void updateTotal() {
        int ia1    = parseOr(etIa1,   0);
        int ia2    = parseOr(etIa2,   0);
        int assign = parseOr(etAssign, 0);
        tvTotal.setText("/ " + (ia1 + ia2 + assign));
    }

    private void saveConfig() {
        if (hodDept.isEmpty()) { showMsg("Department not loaded yet"); return; }

        int ia1    = parseOr(etIa1,   -1);
        int ia2    = parseOr(etIa2,   -1);
        int assign = parseOr(etAssign, -1);

        if (ia1 <= 0 || ia2 <= 0 || assign <= 0) {
            showMsg("All values must be greater than 0");
            return;
        }
        if (ia1 > 100 || ia2 > 100 || assign > 100) {
            showMsg("Values cannot exceed 100");
            return;
        }

        pb.setVisibility(View.VISIBLE);
        String docId  = hodDept.replaceAll("\\s+", "_");
        String prefix = "maxMarks_year" + selectedYear + "_";

        Map<String, Object> data = new HashMap<>();
        data.put(prefix + "ia1",        ia1);
        data.put(prefix + "ia2",        ia2);
        data.put(prefix + "assignment", assign);
        data.put(prefix + "total",      ia1 + ia2 + assign);
        data.put("department",          hodDept);
        data.put("updatedAt",           new java.util.Date());

        db.collection("departmentSettings").document(docId)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener(v -> {
                if (!isAdded()) return;
                pb.setVisibility(View.GONE);
                cache[selectedYear - 1][0] = ia1;
                cache[selectedYear - 1][1] = ia2;
                cache[selectedYear - 1][2] = assign;
                showMsg("Max marks saved for Year " + selectedYear);
            })
            .addOnFailureListener(e -> {
                if (!isAdded()) return;
                pb.setVisibility(View.GONE);
                showMsg("Save failed: " + e.getMessage());
            });
    }

    private void setChip(TextView chip, boolean active) {
        chip.setBackgroundResource(active ? R.drawable.bg_pill_active : R.drawable.bg_pill_inactive);
        chip.setTextColor(ContextCompat.getColor(requireContext(),
                active ? R.color.text_on_accent : R.color.text_secondary));
    }

    private int parseOr(EditText et, int fallback) {
        try { return Integer.parseInt(et.getText().toString().trim()); }
        catch (NumberFormatException e) { return fallback; }
    }

    private void showMsg(String msg) {
        if (getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
