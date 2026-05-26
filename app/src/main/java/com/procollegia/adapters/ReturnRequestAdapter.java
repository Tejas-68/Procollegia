package com.procollegia.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.procollegia.R;
import java.util.List;

/** Adapter for showing equipment return requests in PT Dashboard. */
public class ReturnRequestAdapter extends RecyclerView.Adapter<ReturnRequestAdapter.VH> {

    private final List<ReturnRequest> list;
    private final OnRequestAction listener;

    public interface OnRequestAction {
        void onApprove(ReturnRequest request);
    }

    public static class ReturnRequest {
        public String id, studentName, equipmentName, equipmentId, date;
        public ReturnRequest(String id, String studentName, String equipmentName,
                             String equipmentId, String date) {
            this.id            = id;
            this.studentName   = studentName;
            this.equipmentName = equipmentName;
            this.equipmentId   = equipmentId;
            this.date          = date;
        }
    }

    public ReturnRequestAdapter(List<ReturnRequest> list, OnRequestAction listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_return_request, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ReturnRequest req = list.get(position);
        holder.tvStudent.setText(req.studentName);
        holder.tvEquipment.setText(req.equipmentName);
        holder.tvDate.setText("Requested " + req.date);
        
        holder.btnApprove.setOnClickListener(v -> listener.onApprove(req));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvStudent, tvEquipment, tvDate;
        View btnApprove;
        VH(View v) {
            super(v);
            tvStudent = v.findViewById(R.id.tvStudentName);
            tvEquipment = v.findViewById(R.id.tvEquipmentName);
            tvDate = v.findViewById(R.id.tvRequestDate);
            btnApprove = v.findViewById(R.id.btnApproveReturn);
        }
    }
}
