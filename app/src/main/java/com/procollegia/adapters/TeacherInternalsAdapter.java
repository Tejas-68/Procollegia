package com.procollegia.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.procollegia.R;

import java.util.List;

public class TeacherInternalsAdapter extends RecyclerView.Adapter<TeacherInternalsAdapter.VH> {

    // ── Data model ─────────────────────────────────────────────────────────────
    public static class InternalStudent {
        public String id;
        public String name;
        public String uucmsId;
        public String year;
        public String section;
        public String department;
        public int ia1        = 0;
        public int ia2        = 0;
        public int assignment = 0;

        public InternalStudent(String id, String name, String uucmsId,
                               String year, String section, String department) {
            this.id         = id;
            this.name       = name != null ? name : "Unknown";
            this.uucmsId    = uucmsId;
            this.year       = year;
            this.section    = section;
            this.department = department != null ? department : "";
        }
    }

    // ── Max marks config (set by HOD, defaults to 25/25/10) ───────────────────
    public static class MaxMarks {
        public final int ia1;
        public final int ia2;
        public final int assignment;

        public MaxMarks(int ia1, int ia2, int assignment) {
            this.ia1        = ia1;
            this.ia2        = ia2;
            this.assignment = assignment;
        }

        public int total() { return ia1 + ia2 + assignment; }

        public static MaxMarks defaults() { return new MaxMarks(25, 25, 10); }
    }

    // ── Callback ───────────────────────────────────────────────────────────────
    public interface OnSaveListener {
        void onSave(InternalStudent student, int ia1, int ia2, int assignment);
    }

    // ── State ──────────────────────────────────────────────────────────────────
    private final List<InternalStudent> students;
    private final OnSaveListener        saveListener;
    private MaxMarks                    maxMarks;

    public TeacherInternalsAdapter(List<InternalStudent> students,
                                   OnSaveListener saveListener) {
        this.students    = students;
        this.saveListener = saveListener;
        this.maxMarks    = MaxMarks.defaults();
    }

    /** Call this after loading max marks from Firestore to refresh all cards. */
    public void setMaxMarks(MaxMarks maxMarks) {
        this.maxMarks = maxMarks;
        notifyDataSetChanged();
    }

    // ── RecyclerView ───────────────────────────────────────────────────────────
    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_internal_marks, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        InternalStudent s = students.get(pos);

        h.tvName.setText(s.name);
        h.tvUucms.setText(s.uucmsId != null ? s.uucmsId : "N/A");

        // Dynamic labels reflecting HOD-configured max marks
        h.tvLabelIa1.setText("IA-1\n(/" + maxMarks.ia1 + ")");
        h.tvLabelIa2.setText("IA-2\n(/" + maxMarks.ia2 + ")");
        h.tvLabelAssign.setText("Assign\n(/" + maxMarks.assignment + ")");

        // Total badge
        h.tvTotal.setText("0/" + maxMarks.total());
        setEditText(h.etIa1,   s.ia1);
        setEditText(h.etIa2,   s.ia2);
        setEditText(h.etAssign, s.assignment);
        updateTotal(h, s.ia1, s.ia2, s.assignment);

        // Live total watcher
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int a, int b, int cnt) {}
            @Override public void onTextChanged(CharSequence c, int a, int b, int cnt) {}
            @Override public void afterTextChanged(Editable e) {
                int ia1    = clamp(h.etIa1.getText().toString(),   maxMarks.ia1);
                int ia2    = clamp(h.etIa2.getText().toString(),   maxMarks.ia2);
                int assign = clamp(h.etAssign.getText().toString(), maxMarks.assignment);
                updateTotal(h, ia1, ia2, assign);
            }
        };

        // Clear old watchers via tag trick
        removePreviousWatcher(h.etIa1);
        removePreviousWatcher(h.etIa2);
        removePreviousWatcher(h.etAssign);

        h.etIa1.addTextChangedListener(watcher);
        h.etIa2.addTextChangedListener(watcher);
        h.etAssign.addTextChangedListener(watcher);

        h.btnSave.setOnClickListener(v -> {
            int ia1    = validate(h.etIa1,   maxMarks.ia1,        "IA-1");
            int ia2    = validate(h.etIa2,   maxMarks.ia2,        "IA-2");
            int assign = validate(h.etAssign, maxMarks.assignment, "Assignment");
            if (ia1 < 0 || ia2 < 0 || assign < 0) return;
            if (saveListener != null) saveListener.onSave(s, ia1, ia2, assign);
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private void setEditText(EditText et, int value) {
        et.setText(value > 0 ? String.valueOf(value) : "");
    }

    private void updateTotal(VH h, int ia1, int ia2, int assign) {
        int total = ia1 + ia2 + assign;
        int max   = maxMarks.total();
        h.tvTotal.setText(total + "/" + max);

        double pct = max > 0 ? (double) total / max : 0;
        int color;
        if (pct >= 0.75)      color = 0xFF27AE60; // green
        else if (pct >= 0.50) color = 0xFFE67E22; // orange
        else                  color = 0xFFE74C3C; // red
        h.tvTotal.setTextColor(color);
    }

    private int validate(EditText et, int max, String label) {
        String raw = et.getText().toString().trim();
        if (raw.isEmpty()) return 0;
        try {
            int val = Integer.parseInt(raw);
            if (val < 0 || val > max) {
                et.setError(label + " max is " + max);
                return -1;
            }
            return val;
        } catch (NumberFormatException e) {
            et.setError("Invalid number");
            return -1;
        }
    }

    private int clamp(String str, int max) {
        if (str == null || str.trim().isEmpty()) return 0;
        try { return Math.min(Math.max(0, Integer.parseInt(str.trim())), max); }
        catch (NumberFormatException e) { return 0; }
    }

    private void removePreviousWatcher(EditText et) {
        // Tag stores a marker so old watchers don't fire after rebind
        et.setTag(null);
    }

    @Override public int getItemCount() { return students.size(); }

    // ── ViewHolder ─────────────────────────────────────────────────────────────
    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvUucms, tvTotal;
        TextView tvLabelIa1, tvLabelIa2, tvLabelAssign;
        EditText etIa1, etIa2, etAssign;
        Button   btnSave;

        VH(View v) {
            super(v);
            tvName       = v.findViewById(R.id.tvStudentName);
            tvUucms      = v.findViewById(R.id.tvUucmsId);
            tvTotal      = v.findViewById(R.id.tvTotal);
            tvLabelIa1   = v.findViewById(R.id.tvLabelIa1);
            tvLabelIa2   = v.findViewById(R.id.tvLabelIa2);
            tvLabelAssign= v.findViewById(R.id.tvLabelAssign);
            etIa1        = v.findViewById(R.id.etIa1);
            etIa2        = v.findViewById(R.id.etIa2);
            etAssign     = v.findViewById(R.id.etAssign);
            btnSave      = v.findViewById(R.id.btnSaveMarks);
        }
    }
}
