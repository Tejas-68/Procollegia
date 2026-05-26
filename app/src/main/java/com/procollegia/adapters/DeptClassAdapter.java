package com.procollegia.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;
import com.procollegia.R;
import java.util.List;

public class DeptClassAdapter extends RecyclerView.Adapter<DeptClassAdapter.ClassViewHolder> {

    private final List<ClassItem> classList;

    public static class ClassItem {
        public String id, name, attendance;
        public int progress;
        public ClassItem(String id, String name, String attendance, int progress) {
            this.id = id;
            this.name = name;
            this.attendance = attendance;
            this.progress = progress;
        }
    }

    public DeptClassAdapter(List<ClassItem> classList) {
        this.classList = classList;
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dept_class, parent, false);
        return new ClassViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        ClassItem item = classList.get(position);
        holder.tvName.setText(item.name);
        holder.tvAttendance.setText(item.attendance);
        holder.pbSyllabus.setProgress(item.progress);
        holder.tvSyllabusProgress.setText(item.progress + "% Covered");
        
        // Dynamic coloring for attendance
        try {
            int attVal = Integer.parseInt(item.attendance.replace("%", ""));
            if (attVal < 75) {
                holder.tvAttendance.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_red));
            } else {
                holder.tvAttendance.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_green));
            }
        } catch (NumberFormatException e) {
            holder.tvAttendance.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));
        }
    }

    @Override
    public int getItemCount() {
        return classList.size();
    }

    static class ClassViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAttendance, tvSyllabusProgress;
        ProgressBar pbSyllabus;
        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvClassName);
            tvAttendance = itemView.findViewById(R.id.tvClassAttendance);
            tvSyllabusProgress = itemView.findViewById(R.id.tvSyllabusProgress);
            pbSyllabus = itemView.findViewById(R.id.pbSyllabus);
        }
    }
}
