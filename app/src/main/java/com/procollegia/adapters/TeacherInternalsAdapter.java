package com.procollegia.adapters;

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

    public static class InternalStudent {
        public String id;
        public String name;
        public String uucmsId;
        public String year;
        public String section;
        public int ia1 = 0;
        public int ia2 = 0;
        public int assignment = 0;
        public int attendance = 0;

        public InternalStudent(String id, String name, String uucmsId, String year, String section) {
            this.id = id;
            this.name = name;
            this.uucmsId = uucmsId;
            this.year = year;
            this.section = section;
        }
    }

    public interface OnSaveListener {
        void onSave(InternalStudent student, int ia1, int ia2, int assignment, int attendance);
    }

    private final List<InternalStudent> students;
    private final OnSaveListener saveListener;

    public TeacherInternalsAdapter(List<InternalStudent> students, OnSaveListener saveListener) {
        this.students = students;
        this.saveListener = saveListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_internal_marks, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        InternalStudent s = students.get(pos);
        h.tvName.setText(s.name != null ? s.name : "Unknown");
        h.tvUucms.setText(s.uucmsId != null ? s.uucmsId : "N/A");

        h.etIa1.setText(s.ia1 > 0 ? String.valueOf(s.ia1) : "");
        h.etIa2.setText(s.ia2 > 0 ? String.valueOf(s.ia2) : "");
        h.etAssign.setText(s.assignment > 0 ? String.valueOf(s.assignment) : "");
        h.etAttend.setText(s.attendance > 0 ? String.valueOf(s.attendance) : "");

        h.btnSave.setOnClickListener(v -> {
            int ia1 = parseInt(h.etIa1.getText().toString());
            int ia2 = parseInt(h.etIa2.getText().toString());
            int assign = parseInt(h.etAssign.getText().toString());
            int attend = parseInt(h.etAttend.getText().toString());
            
            if (saveListener != null) {
                saveListener.onSave(s, ia1, ia2, assign, attend);
            }
        });
    }

    private int parseInt(String str) {
        if (str == null || str.trim().isEmpty()) return 0;
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvUucms;
        EditText etIa1, etIa2, etAssign, etAttend;
        Button btnSave;

        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvStudentName);
            tvUucms = v.findViewById(R.id.tvUucmsId);
            etIa1 = v.findViewById(R.id.etIa1);
            etIa2 = v.findViewById(R.id.etIa2);
            etAssign = v.findViewById(R.id.etAssign);
            etAttend = v.findViewById(R.id.etAttend);
            btnSave = v.findViewById(R.id.btnSaveMarks);
        }
    }
}
