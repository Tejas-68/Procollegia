package com.procollegia.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.procollegia.R;

import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private List<CalendarDay> days;

    public static class CalendarDay {
        public String day;
        public int status; // 0=empty, 1=present, 2=absent, 3=holiday

        public CalendarDay(String day, int status) {
            this.day = day;
            this.status = status;
        }
    }

    public CalendarAdapter(List<CalendarDay> days) {
        this.days = days;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        CalendarDay d = days.get(position);
        holder.tvDay.setText(d.day);
        
        if (d.status == 0) {
            holder.tvDay.setBackgroundResource(0);
            holder.tvDay.setTextColor(Color.TRANSPARENT);
        } else {
            holder.tvDay.setTextColor(Color.WHITE);
            if (d.status == 1) {
                holder.tvDay.setBackgroundResource(R.drawable.bg_day_present);
            } else if (d.status == 2) {
                holder.tvDay.setBackgroundResource(R.drawable.bg_day_absent);
            } else if (d.status == 3) {
                holder.tvDay.setBackgroundResource(R.drawable.bg_day_holiday);
            } else if (d.status == 4) {
                holder.tvDay.setBackgroundResource(0);
                holder.tvDay.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_muted));
            }
        }
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay;
        public CalendarViewHolder(View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
        }
    }
}
