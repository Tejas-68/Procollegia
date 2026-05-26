package com.procollegia.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.procollegia.R;
import com.procollegia.models.TimetableSession;

import java.util.List;

public class TimetableAdapter extends RecyclerView.Adapter<TimetableAdapter.ViewHolder> {
    private List<TimetableSession> sessions;

    public TimetableAdapter(List<TimetableSession> sessions) {
        this.sessions = sessions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timetable, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimetableSession session = sessions.get(position);
        holder.tvTime.setText(session.getTime());
        holder.tvSubject.setText(session.getSubject());
        holder.tvRoom.setText(session.getRoom());
        holder.tvLecturer.setText(session.getLecturer());
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvSubject, tvRoom, tvLecturer;

        ViewHolder(View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            tvLecturer = itemView.findViewById(R.id.tvLecturer);
        }
    }
}
