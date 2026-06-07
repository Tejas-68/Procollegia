package com.procollegia.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.procollegia.R;

import java.util.List;

public class StudentAttendanceAdapter extends RecyclerView.Adapter<StudentAttendanceAdapter.VH> {

    public static class StudentAttendance {
        public final String id;
        public final String name;
        public final String uucmsId;
        public final String year;    // Added for filtering
        public final String section; // Added for filtering
        public String status; // P, A, L
        public boolean isOnLeave;

        public StudentAttendance(String id, String name, String uucmsId, String year, String section) {
            this.id      = id;
            this.name    = (name != null) ? name : "Unknown";
            this.uucmsId = (uucmsId != null) ? uucmsId : "N/A";
            this.year    = (year != null) ? year : "";
            this.section = (section != null) ? section : "";
            this.status  = "P"; // Default Present
            this.isOnLeave = false;
        }
    }

    private final List<StudentAttendance> list;
    public interface OnAttendanceChanged { void onUpdate(); }
    private final OnAttendanceChanged callback;

    public StudentAttendanceAdapter(List<StudentAttendance> list, OnAttendanceChanged callback) {
        this.list     = list;
        this.callback = callback;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_attendance, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        StudentAttendance it = list.get(pos);
        h.tvName.setText(it.name);
        h.tvRoll.setText("UUCMS: " + it.uucmsId);

        if (it.isOnLeave) {
            it.status = "L";
            h.itemView.setAlpha(0.6f);
            h.llToggle.setVisibility(View.GONE);
            h.tvOnLeave.setVisibility(View.VISIBLE);
            h.tvName.setTextColor(Color.GRAY);
        } else {
            h.itemView.setAlpha(1.0f);
            h.llToggle.setVisibility(View.VISIBLE);
            h.tvOnLeave.setVisibility(View.GONE);
            h.tvName.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.text_primary));
            
            updateVisuals(h, it.status);

            h.tvP.setOnClickListener(v -> { it.status = "P"; updateVisuals(h, "P"); callback.onUpdate(); });
            h.tvA.setOnClickListener(v -> { it.status = "A"; updateVisuals(h, "A"); callback.onUpdate(); });
        }
    }

    private void updateVisuals(VH h, String status) {
        Context ctx = h.itemView.getContext();
        resetStyle(h.tvP);
        resetStyle(h.tvA);

        if (status.equals("P")) {
            applyStyle(h.tvP, ContextCompat.getColor(ctx, R.color.status_present));
        } else if (status.equals("A")) {
            applyStyle(h.tvA, ContextCompat.getColor(ctx, R.color.status_absent));
        }
    }

    private void resetStyle(TextView tv) {
        tv.setBackground(null);
        tv.setTextColor(ContextCompat.getColor(tv.getContext(), R.color.text_secondary));
    }

    private void applyStyle(TextView tv, int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        float density = tv.getResources().getDisplayMetrics().density;
        gd.setCornerRadius(16 * density); // Matches bg_neumorph_card_sm radius (16dp)
        gd.setColor(color);
        tv.setBackground(gd);
        tv.setTextColor(Color.WHITE);
    }

    /** Marks every non-leave student as the given status ("P" or "A") and refreshes. */
    public void selectAll(String status) {
        for (StudentAttendance s : list) {
            if (!s.isOnLeave) s.status = status;
        }
        notifyDataSetChanged();
        if (callback != null) callback.onUpdate();
    }

    @Override public int getItemCount() { return list.size(); }


    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvRoll, tvP, tvA, tvOnLeave;
        View llToggle;
        VH(View v) {
            super(v);
            tvName    = v.findViewById(R.id.tvStudentName);
            tvRoll    = v.findViewById(R.id.tvRollNo);
            tvP       = v.findViewById(R.id.tvStatusP);
            tvA       = v.findViewById(R.id.tvStatusA);
            tvOnLeave = v.findViewById(R.id.tvOnLeaveLabel);
            llToggle  = v.findViewById(R.id.llToggleGroup);
        }
    }
}
