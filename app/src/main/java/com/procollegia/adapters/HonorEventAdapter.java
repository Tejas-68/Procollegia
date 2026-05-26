package com.procollegia.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.procollegia.R;

import java.util.List;

public class HonorEventAdapter extends RecyclerView.Adapter<HonorEventAdapter.EventVH> {

    public static class HonorEvent {
        public final int points;
        public final String description;
        public final String date;

        public HonorEvent(int points, String description, String date) {
            this.points = points;
            this.description = description;
            this.date = date;
        }
    }

    private final List<HonorEvent> events;

    public HonorEventAdapter(List<HonorEvent> events) {
        this.events = events;
    }

    @NonNull
    @Override
    public EventVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_honor_event, parent, false);
        return new EventVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EventVH h, int pos) {
        HonorEvent e = events.get(pos);
        boolean isPositive = e.points >= 0;

        h.tvArrow.setText(isPositive ? "↑" : "↓");
        h.tvArrow.setTextColor(ContextCompat.getColor(h.itemView.getContext(),
                isPositive ? R.color.accent_green : R.color.accent_red));

        String sign = isPositive ? "+" : "";
        h.tvPoints.setText(sign + e.points + " pts");
        h.tvPoints.setTextColor(ContextCompat.getColor(h.itemView.getContext(),
                isPositive ? R.color.accent_green : R.color.accent_red));

        h.tvDesc.setText(" — " + e.description);
        h.tvDate.setText(e.date);
    }

    @Override
    public int getItemCount() { return events.size(); }

    static class EventVH extends RecyclerView.ViewHolder {
        TextView tvArrow, tvPoints, tvDesc, tvDate;

        EventVH(View itemView) {
            super(itemView);
            tvArrow  = itemView.findViewById(R.id.tvEventArrow);
            tvPoints = itemView.findViewById(R.id.tvEventPoints);
            tvDesc   = itemView.findViewById(R.id.tvEventDesc);
            tvDate   = itemView.findViewById(R.id.tvEventDate);
        }
    }
}
