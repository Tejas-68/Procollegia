package com.procollegia.adapters;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.procollegia.R;

import java.util.List;

public class VerticalTimetableAdapter extends RecyclerView.Adapter<VerticalTimetableAdapter.VH> {

    private final List<PeriodAdapter.TimetableItem> list;

    // Palette for accent bar colors
    private static final int[] ACCENT_COLORS = {
            Color.parseColor("#4A90D9"), Color.parseColor("#48BB78"),
            Color.parseColor("#9F7AEA"), Color.parseColor("#38B2AC"),
            Color.parseColor("#ED8936"), Color.parseColor("#FC8181")
    };

    public VerticalTimetableAdapter(List<PeriodAdapter.TimetableItem> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_period, parent, false);
        
        // Ensure ViewPager items are exactly the parent's size
        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        
        // Center the content vertically inside the ViewPager card
        if (view instanceof android.widget.LinearLayout) {
            ((android.widget.LinearLayout) view).setGravity(android.view.Gravity.CENTER_VERTICAL);
        }

        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        PeriodAdapter.TimetableItem it = list.get(pos);
        
        // Data binding
        if (it.type == PeriodAdapter.TimetableItem.TYPE_BREAK) {
            h.tvTime.setText("Break Time");
            h.tvName.setText(it.breakLabel != null ? it.breakLabel : "Rest Period");
            h.tvRoom.setVisibility(View.GONE);
            h.tvExtra.setVisibility(View.GONE);
        } else {
            h.tvTime.setText(it.startTime + " – " + it.endTime);
            h.tvName.setText(it.subject);
            h.tvRoom.setText(it.room);
            h.tvExtra.setText(it.teacher); 
            h.tvRoom.setVisibility(View.VISIBLE);
            h.tvExtra.setVisibility(View.VISIBLE);
        }

        // Apply dynamic accent color
        int color = ACCENT_COLORS[pos % ACCENT_COLORS.length];
        GradientDrawable bar = new GradientDrawable();
        bar.setColor(color);
        bar.setCornerRadius(dpToPx(12));
        h.viewAccent.setBackground(bar);

        // Adjust layout of the accent bar (make it taller/centered)
        ViewGroup.LayoutParams lp = h.viewAccent.getLayoutParams();
        lp.height = dpToPx(64);
        h.viewAccent.setLayoutParams(lp);

        // Set consistent padding (e.g. 24dp horizontal, 16dp vertical)
        h.itemView.setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16));
        
        // Ensure the background of the item is transparent so the master card shows through
        h.itemView.setBackgroundColor(Color.TRANSPARENT);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        View     viewAccent;
        TextView tvTime, tvName, tvRoom, tvExtra;
        VH(View v) {
            super(v);
            viewAccent = v.findViewById(R.id.viewAccent);
            tvTime     = v.findViewById(R.id.tvPeriodTime);
            tvName     = v.findViewById(R.id.tvPeriodName);
            tvRoom     = v.findViewById(R.id.tvPeriodRoom);
            tvExtra    = v.findViewById(R.id.tvPeriodTeacher);
        }
    }
}
