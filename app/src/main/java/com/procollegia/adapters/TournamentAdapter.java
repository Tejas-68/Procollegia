package com.procollegia.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.procollegia.R;

import java.util.List;

public class TournamentAdapter extends RecyclerView.Adapter<TournamentAdapter.VH> {

    public static class TournamentItem {
        public String id, name, type, venue, date, teams, prize, emoji, status, gameType;
        public int maxPlayers;
        public boolean isLive;

        // Comprehensive constructor
        public TournamentItem(String id, String n, String t, String v, String d, String tm, String pz, boolean live, String em) {
            this.id = id; this.name = n; this.type = t; this.venue = v; this.date = d;
            this.teams = tm; this.prize = pz; this.isLive = live; this.emoji = em;
            this.status = live ? "ongoing" : "upcoming";
            this.gameType = "Solo"; this.maxPlayers = 1;
        }

        // Updated constructor for PT use
        public TournamentItem(String id, String n, String t, String d, String v, String s) {
            this.id = id; this.name = n; this.type = t; this.date = d; this.venue = v; this.status = s;
            this.isLive = "ongoing".equalsIgnoreCase(s);
            this.emoji = ""; this.teams = "—"; this.prize = "Trophy";
            this.gameType = "Solo"; this.maxPlayers = 1;
        }
    }

    public interface OnTourneyAction {
        void onClick(TournamentItem item);
    }

    public interface OnJoinAction {
        void onJoin(TournamentItem item);
    }

    /** P4: long-press to delete / act on a tournament card */
    public interface OnLongClickListener {
        void onLongClick(TournamentItem item);
    }

    private final List<TournamentItem> items;
    private OnTourneyAction  listener;
    private OnJoinAction     joinListener;
    private OnLongClickListener longClickListener;
    
    public TournamentAdapter(List<TournamentItem> items) { this.items = items; }
    public TournamentAdapter(List<TournamentItem> items, OnTourneyAction listener) {
        this.items    = items;
        this.listener = listener;
    }

    public void setOnJoinListener(OnJoinAction l)           { this.joinListener      = l; }
    public void setOnLongClickListener(OnLongClickListener l) { this.longClickListener = l; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_tournament, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        TournamentItem it = items.get(pos);
        h.tvName.setText((it.emoji != null ? it.emoji + " " : "") + it.name);
        h.tvMeta.setText(it.type + " • " + it.venue);
        h.tvDate.setText(it.date);
        
        if (it.isLive || "ongoing".equalsIgnoreCase(it.status)) {
            h.tvStatus.setText("Live Now");
            h.tvStatus.setBackgroundResource(R.drawable.bg_pill_active);
        } else {
            h.tvStatus.setText("Upcoming");
            h.tvStatus.setBackgroundResource(R.drawable.bg_pill_inactive);
        }

        if (joinListener != null) {
            h.btnJoin.setVisibility(View.VISIBLE);
            h.btnJoin.setOnClickListener(v -> joinListener.onJoin(it));
        } else {
            h.btnJoin.setVisibility(View.GONE);
        }

        if (listener != null) {
            h.itemView.setOnClickListener(v -> listener.onClick(it));
        }
        if (longClickListener != null) {
            h.itemView.setOnLongClickListener(v -> {
                longClickListener.onLongClick(it);
                return true;
            });
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvMeta, tvDate, tvStatus;
        android.widget.Button btnJoin;
        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvTourneyName);
            tvMeta = v.findViewById(R.id.tvTourneyMeta);
            tvDate = v.findViewById(R.id.tvTourneyDate);
            tvStatus = v.findViewById(R.id.tvTourneyStatus);
            btnJoin = v.findViewById(R.id.btnJoinTourney);
        }
    }
}
