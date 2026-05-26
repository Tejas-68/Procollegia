package com.procollegia.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.procollegia.R;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.VH> {

    public interface OnStudentClickListener {
        void onStudentClick(StudentScore s);
    }

    public static class StudentScore {
        public String uid, name, dept, roll;
        public int score;
        public StudentScore(String u, String n, String d, String r, int s) {
            uid=u; name=n; dept=d; roll=r; score=s;
        }
    }

    private final List<StudentScore> students;
    private final OnStudentClickListener listener;

    public LeaderboardAdapter(List<StudentScore> students, OnStudentClickListener listener) {
        this.students = students;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_leaderboard_student, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        StudentScore s = students.get(pos);
        h.tvRank.setText("#" + (pos + 1));
        h.tvName.setText(s.name);
        h.tvMeta.setText(s.dept + " | Roll: " + s.roll);
        h.tvScore.setText(s.score + " pts");
        
        h.itemView.setOnClickListener(v -> listener.onStudentClick(s));
    }

    @Override public int getItemCount() { return students.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvRank, tvName, tvMeta, tvScore;
        VH(View v) {
            super(v);
            tvRank = v.findViewById(R.id.tvRank);
            tvName = v.findViewById(R.id.tvStudentName);
            tvMeta = v.findViewById(R.id.tvStudentMeta);
            tvScore= v.findViewById(R.id.tvStudentScore);
        }
    }
}
