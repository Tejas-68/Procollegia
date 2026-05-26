package com.procollegia.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.procollegia.R;
import java.util.List;

/** Adapter for showing teams in a tournament. */
public class TeamAdapter extends RecyclerView.Adapter<TeamAdapter.VH> {

    public static class TeamItem {
        public String name;
        public int memberCount;
        public TeamItem(String n, int mc) { this.name = n; this.memberCount = mc; }
    }

    private final List<TeamItem> list;
    public TeamAdapter(List<TeamItem> l) { this.list = l; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_team_card, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        TeamItem it = list.get(pos);
        h.tvName.setText(it.name);
        h.tvCount.setText(it.memberCount + " Members");
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvCount;
        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvTeamName);
            tvCount = v.findViewById(R.id.tvTeamMembers);
        }
    }
}
