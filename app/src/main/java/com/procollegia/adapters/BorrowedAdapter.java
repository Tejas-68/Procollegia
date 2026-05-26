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

public class BorrowedAdapter extends RecyclerView.Adapter<BorrowedAdapter.VH> {

    public static class BorrowedItem {
        public final String docId;
        public final String name;
        public final String emoji;
        public final String borrowDate;
        // "active" | "return_requested" | "returned"
        public String status;

        public BorrowedItem(String docId, String name, String emoji,
                            String borrowDate, String status) {
            this.docId      = docId;
            this.name       = name;
            this.emoji      = emoji;
            this.borrowDate = borrowDate;
            this.status     = status;
        }
    }

    public interface OnReturnClick { void onReturn(BorrowedItem item, int pos); }

    private final List<BorrowedItem> list;
    private final OnReturnClick      listener;

    public BorrowedAdapter(List<BorrowedItem> list, OnReturnClick listener) {
        this.list     = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_borrowed, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        BorrowedItem item = list.get(pos);
        h.tvEmoji.setText(item.emoji);
        h.tvName.setText(item.name);
        h.tvDate.setText("Borrowed: " + item.borrowDate);

        switch (item.status) {
            case "active":
                h.tvStatus.setText("Active");
                h.tvStatus.setBackgroundResource(R.drawable.bg_badge_green);
                h.tvStatus.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.accent_green));
                h.btnReturn.setVisibility(View.VISIBLE);
                h.btnReturn.setText("Return");
                h.btnReturn.setAlpha(1f);
                h.btnReturn.setOnClickListener(v -> { if (listener != null) listener.onReturn(item, pos); });
                break;

            case "return_requested":
                h.tvStatus.setText("Return Pending ⏳");
                h.tvStatus.setBackgroundResource(R.drawable.bg_badge_green);
                h.tvStatus.setTextColor(0xFFD69E2E);
                h.btnReturn.setVisibility(View.VISIBLE);
                h.btnReturn.setText("Pending");
                h.btnReturn.setAlpha(0.5f);
                h.btnReturn.setEnabled(false);
                break;

            case "returned":
                h.tvStatus.setText("Returned ✓");
                h.tvStatus.setBackgroundResource(R.drawable.bg_badge_green);
                h.tvStatus.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.text_muted));
                h.btnReturn.setVisibility(View.GONE);
                break;

            default:
                h.tvStatus.setText(item.status);
                h.btnReturn.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    public void updateStatus(int pos, String newStatus) {
        list.get(pos).status = newStatus;
        notifyItemChanged(pos);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvName, tvDate, tvStatus, btnReturn;
        VH(View v) {
            super(v);
            tvEmoji   = v.findViewById(R.id.tvBorrowedEmoji);
            tvName    = v.findViewById(R.id.tvBorrowedName);
            tvDate    = v.findViewById(R.id.tvBorrowedDate);
            tvStatus  = v.findViewById(R.id.tvBorrowedStatus);
            btnReturn = v.findViewById(R.id.btnReturn);
        }
    }
}
