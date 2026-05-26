package com.procollegia.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.procollegia.R;

import java.util.List;

public class PeriodAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // ── Data models ───────────────────────────────────────────────────────────

    public static class TimetableItem {
        public static final int TYPE_PERIOD = 0;
        public static final int TYPE_BREAK  = 1;

        public final int    type;
        // Period fields
        public final String subject;
        public final String teacher;
        public final String room;
        public final String startTime;
        public final String endTime;

        // Break field
        public final String breakLabel;

        /** Period constructor */
        public TimetableItem(String subject, String teacher, String room,
                             String startTime, String endTime) {
            this.type       = TYPE_PERIOD;
            this.subject    = subject;
            this.teacher    = teacher;
            this.room       = room;
            this.startTime  = startTime;
            this.endTime    = endTime;
            this.breakLabel = null;
        }

        /** Break constructor */
        public TimetableItem(String breakLabel) {
            this.type       = TYPE_BREAK;
            this.breakLabel = breakLabel;
            this.subject = this.teacher = this.room = this.startTime = this.endTime = null;
        }
    }

    // Keep the old Period alias so TimetableFragment doesn't break
    public static class Period extends TimetableItem {
        public Period(String subject, String teacher, String room,
                      String startTime, String endTime) {
            super(subject, teacher, room, startTime, endTime);
        }
    }

    // Accent colors cycling per period (matching the reference image palette)
    private static final int[] ACCENT_COLORS = {
            Color.parseColor("#4A90D9"), // blue
            Color.parseColor("#48BB78"), // green
            Color.parseColor("#9F7AEA"), // purple
            Color.parseColor("#38B2AC"), // teal
            Color.parseColor("#ED8936"), // orange
            Color.parseColor("#FC8181"), // red-pink
    };

    private final List<TimetableItem> items;

    public PeriodAdapter(List<TimetableItem> items) {
        this.items = items;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TimetableItem.TYPE_BREAK) {
            return new BreakVH(inflater.inflate(R.layout.item_break, parent, false));
        }
        return new PeriodVH(inflater.inflate(R.layout.item_period, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TimetableItem item = items.get(position);

        if (item.type == TimetableItem.TYPE_BREAK) {
            ((BreakVH) holder).tvLabel.setText(item.breakLabel);
            return;
        }

        PeriodVH h = (PeriodVH) holder;

        // Assign accent color — cycle through palette based on overall ordinal among only periods
        int periodOrdinal = 0;
        for (int i = 0; i < position; i++) {
            if (items.get(i).type == TimetableItem.TYPE_PERIOD) periodOrdinal++;
        }
        int color = ACCENT_COLORS[periodOrdinal % ACCENT_COLORS.length];

        // Apply rounded accent bar color
        GradientDrawable bar = new GradientDrawable();
        bar.setColor(color);
        bar.setCornerRadius(12f);
        h.viewAccent.setBackground(bar);

        h.tvTime.setText(item.startTime + " – " + item.endTime);
        h.tvName.setText(item.subject);
        h.tvRoom.setText(item.room);
        h.tvTeacher.setText(item.teacher);
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ── ViewHolders ───────────────────────────────────────────────────────────

    static class PeriodVH extends RecyclerView.ViewHolder {
        View     viewAccent;
        TextView tvTime, tvName, tvRoom, tvTeacher;

        PeriodVH(View v) {
            super(v);
            viewAccent = v.findViewById(R.id.viewAccent);
            tvTime     = v.findViewById(R.id.tvPeriodTime);
            tvName     = v.findViewById(R.id.tvPeriodName);
            tvRoom     = v.findViewById(R.id.tvPeriodRoom);
            tvTeacher  = v.findViewById(R.id.tvPeriodTeacher);
        }
    }

    static class BreakVH extends RecyclerView.ViewHolder {
        TextView tvLabel;
        BreakVH(View v) {
            super(v);
            tvLabel = v.findViewById(R.id.tvBreakLabel);
        }
    }
}
