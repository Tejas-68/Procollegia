package com.procollegia.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.procollegia.R;
import java.util.ArrayList;
import java.util.List;

public class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.StaffViewHolder> {

    private final List<StaffItem> staffList;
    private final OnSubjectAction listener;

    public interface OnSubjectAction {
        void onAddSubject(StaffItem item, String subjectName);
        void onRemoveSubject(StaffItem item, String subjectName);
    }

    public static class StaffItem {
        public String id, name;
        public List<String> subjects;
        public boolean isExpanded;

        public StaffItem(String id, String name, List<String> subjects) {
            this.id = id;
            this.name = name;
            this.subjects = subjects;
            this.isExpanded = false;
        }

        /**
         * Returns true if this staff member is already assigned the given subject.
         * Comparison is case-insensitive and trims surrounding whitespace.
         */
        public boolean hasSubject(String subjectName) {
            if (subjectName == null || subjects == null) return false;
            String trimmed = subjectName.trim().toLowerCase();
            for (String s : subjects) {
                if (s != null && s.trim().toLowerCase().equals(trimmed)) return true;
            }
            return false;
        }

        /**
         * Returns the number of subjects currently assigned to this staff member.
         */
        public int getSubjectCount() {
            return subjects == null ? 0 : subjects.size();
        }
    }

    public StaffAdapter(List<StaffItem> staffList, OnSubjectAction listener) {
        this.staffList = staffList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StaffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_staff, parent, false);
        return new StaffViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StaffViewHolder holder, int position) {
        StaffItem item = staffList.get(position);
        holder.tvName.setText(item.name);
        holder.tvSubjectCount.setText(item.subjects.size() + " Subjects Assigned");

        // Toggle Expansion logic
        if (item.isExpanded) {
            holder.llExpandedArea.setVisibility(View.VISIBLE);
            holder.ivExpandIndicator.setRotation(0);
        } else {
            holder.llExpandedArea.setVisibility(View.GONE);
            holder.ivExpandIndicator.setRotation(180);
        }

        holder.llHeaderArea.setOnClickListener(v -> {
            boolean wasExpanded = item.isExpanded;
            for (StaffItem s : staffList) s.isExpanded = false; // Collapse all others
            item.isExpanded = !wasExpanded;
            notifyDataSetChanged(); // Simple refresh for expand/collapse
        });

        // Inflate dynamic subjects list
        holder.llSubjectsContainer.removeAllViews();
        for (String subject : item.subjects) {
            View subView = LayoutInflater.from(holder.itemView.getContext())
                    .inflate(R.layout.item_assigned_subject, holder.llSubjectsContainer, false);
            
            TextView tvSubName = subView.findViewById(R.id.tvSubjectName);
            ImageView ivRemove = subView.findViewById(R.id.ivRemoveSubject);
            
            tvSubName.setText(subject);
            ivRemove.setOnClickListener(v -> listener.onRemoveSubject(item, subject));
            
            holder.llSubjectsContainer.addView(subView);
        }

        // Add Subject Click
        holder.btnAddSubject.setOnClickListener(v -> {
            String newSub = holder.etNewSubject.getText().toString().trim();
            if (!newSub.isEmpty()) {
                listener.onAddSubject(item, newSub);
                holder.etNewSubject.setText("");
            } else {
                Toast.makeText(holder.itemView.getContext(), "Enter a subject name", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return staffList.size();
    }

    static class StaffViewHolder extends RecyclerView.ViewHolder {
        LinearLayout llHeaderArea, llExpandedArea, llSubjectsContainer;
        TextView tvName, tvSubjectCount;
        ImageView ivExpandIndicator;
        EditText etNewSubject;
        Button btnAddSubject;

        public StaffViewHolder(@NonNull View itemView) {
            super(itemView);
            llHeaderArea = itemView.findViewById(R.id.llHeaderArea);
            llExpandedArea = itemView.findViewById(R.id.llExpandedArea);
            llSubjectsContainer = itemView.findViewById(R.id.llSubjectsContainer);
            tvName = itemView.findViewById(R.id.tvStaffName);
            tvSubjectCount = itemView.findViewById(R.id.tvSubjectCount);
            ivExpandIndicator = itemView.findViewById(R.id.ivExpandIndicator);
            etNewSubject = itemView.findViewById(R.id.etNewSubject);
            btnAddSubject = itemView.findViewById(R.id.btnAddSubject);
        }
    }
}
