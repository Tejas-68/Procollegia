package com.procollegia.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.procollegia.R;

import java.util.List;

public class FeedbackAdapter extends RecyclerView.Adapter<FeedbackAdapter.VH> {

    public static class FeedbackItem {
        public final String category;   // Complaint, Feedback, Suggestion, Query
        public final String recipient;  // HOD, Class Teacher, Principal
        public final String subject;
        public final String message;
        public final String date;
        public final String status;     // pending, reviewed, resolved

        public FeedbackItem(String category, String recipient, String subject,
                            String message, String date, String status) {
            this.category  = category;
            this.recipient = recipient;
            this.subject   = subject;
            this.message   = message;
            this.date      = date;
            this.status    = status;
        }
    }

    private final List<FeedbackItem> list;
    public FeedbackAdapter(List<FeedbackItem> list) { this.list = list; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feedback, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        FeedbackItem f = list.get(pos);
        h.tvCategory.setText(f.category);
        h.tvDate.setText(f.date);
        h.tvSubject.setText(f.subject);
        h.tvPreview.setText(f.message);

        // Status styling
        switch (f.status.toLowerCase()) {
            case "resolved":
                h.tvStatus.setText("  Resolved");
                h.tvStatus.setTextColor(0xFF38A169);
                break;
            case "reviewed":
                h.tvStatus.setText("  Reviewed");
                h.tvStatus.setTextColor(0xFF4A90D9);
                break;
            default:
                h.tvStatus.setText("⏳  Pending");
                h.tvStatus.setTextColor(0xFFD69E2E);
                break;
        }
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCategory, tvDate, tvSubject, tvPreview, tvStatus;
        VH(View v) {
            super(v);
            tvCategory = v.findViewById(R.id.tvFeedbackCategory);
            tvDate     = v.findViewById(R.id.tvFeedbackDate);
            tvSubject  = v.findViewById(R.id.tvFeedbackSubject);
            tvPreview  = v.findViewById(R.id.tvFeedbackPreview);
            tvStatus   = v.findViewById(R.id.tvFeedbackStatus);
        }
    }
}
