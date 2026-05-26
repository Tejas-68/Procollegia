package com.procollegia.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.procollegia.R;

import java.util.List;

public class ComplaintTeacherAdapter extends RecyclerView.Adapter<ComplaintTeacherAdapter.VH> {

    public static class TeacherComplaint {
        public final String id;
        public final String category;
        public final String student;
        public final String subject;
        public final String date;

        public TeacherComplaint(String id, String category, String student, String subject, String date) {
            this.id       = id;
            this.category = category;
            this.student  = student;
            this.subject  = subject;
            this.date     = date;
        }
    }

    private final List<TeacherComplaint> list;
    public ComplaintTeacherAdapter(List<TeacherComplaint> list) { this.list = list; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_complaint_teacher, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        TeacherComplaint it = list.get(pos);
        h.tvCategory.setText(it.category);
        h.tvStudent.setText("By: " + it.student);
        h.tvSubject.setText(it.subject);
        h.tvDate.setText(it.date);
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCategory, tvDate, tvStudent, tvSubject;
        VH(View v) {
            super(v);
            tvCategory = v.findViewById(R.id.tvComplaintCategory);
            tvDate     = v.findViewById(R.id.tvComplaintDate);
            tvStudent  = v.findViewById(R.id.tvComplaintStudent);
            tvSubject  = v.findViewById(R.id.tvComplaintSubject);
        }
    }
}
