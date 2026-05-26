package com.procollegia.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.procollegia.R;

import java.util.List;

public class LeaveRequestAdapter extends RecyclerView.Adapter<LeaveRequestAdapter.VH> {

    public static class LeaveRequest {
        public final String id;
        public final String studentName;
        public final String reason;
        public final String date;
        public final String status; // pending, approved, rejected

        public LeaveRequest(String id, String studentName, String reason, String date, String status) {
            this.id = id;
            this.studentName = studentName;
            this.reason = reason;
            this.date = date;
            this.status = status;
        }
    }

    private final List<LeaveRequest> list;
    public interface OnLeaveAction { void onAction(LeaveRequest item, String action); }
    private final OnLeaveAction callback;

    public LeaveRequestAdapter(List<LeaveRequest> list, OnLeaveAction callback) {
        this.list     = list;
        this.callback = callback;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leave_request, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        LeaveRequest item = list.get(pos);
        h.tvName.setText(item.studentName);
        h.tvReason.setText(item.reason);
        h.tvDate.setText(item.date);

        h.btnApprove.setOnClickListener(v -> callback.onAction(item, "approved"));
        h.btnReject.setOnClickListener(v -> callback.onAction(item, "rejected"));
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvReason, tvDate, btnApprove, btnReject;
        VH(View v) {
            super(v);
            tvName       = v.findViewById(R.id.tvLeaveStudentName);
            tvReason     = v.findViewById(R.id.tvLeaveReason);
            tvDate       = v.findViewById(R.id.tvLeaveDate);
            btnApprove   = v.findViewById(R.id.btnApproveLeave);
            btnReject    = v.findViewById(R.id.btnRejectLeave);
        }
    }
}
