package com.procollegia.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.procollegia.R;

import java.util.List;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.VH> {

    public interface Listener {
        void onDelete(int position, String subjectName);
        void onEdit(int position, String oldName, String newName);
    }

    private final List<String> subjects;
    private final Listener listener;

    public SubjectAdapter(List<String> subjects, Listener listener) {
        this.subjects = subjects;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subject, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        String subject = subjects.get(pos);
        h.tvIndex.setText(String.valueOf(pos + 1));

        // Remove old watcher before setting text (avoid stale callbacks)
        h.etName.removeTextChangedListener(h.watcher);
        h.etName.setText(subject);

        // Start in read-only mode
        h.etName.setEnabled(false);
        h.btnEdit.setImageResource(R.drawable.ic_edit);

        // Edit toggle
        h.btnEdit.setOnClickListener(v -> {
            boolean editing = !h.etName.isEnabled();
            if (editing) {
                // Switch to edit mode
                h.etName.setEnabled(true);
                h.etName.requestFocus();
                h.etName.setSelection(h.etName.getText().length());
                h.btnEdit.setImageResource(R.drawable.ic_lock); // reuse lock as "save"
            } else {
                // Save edits
                String newName = h.etName.getText().toString().trim();
                String oldName = subjects.get(h.getAdapterPosition());
                if (!newName.isEmpty() && !newName.equals(oldName)) {
                    listener.onEdit(h.getAdapterPosition(), oldName, newName);
                }
                h.etName.setEnabled(false);
                h.btnEdit.setImageResource(R.drawable.ic_edit);
            }
        });

        // Attach text watcher for live updates
        h.watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { /* editing in place */ }
        };
        h.etName.addTextChangedListener(h.watcher);

        h.btnDelete.setOnClickListener(v -> {
            int adPos = h.getAdapterPosition();
            if (adPos != RecyclerView.NO_ID) {
                listener.onDelete(adPos, subjects.get(adPos));
            }
        });
    }

    @Override
    public int getItemCount() { return subjects.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvIndex;
        EditText etName;
        ImageView btnEdit, btnDelete;
        TextWatcher watcher;

        VH(View v) {
            super(v);
            tvIndex   = v.findViewById(R.id.tvIndex);
            etName    = v.findViewById(R.id.etSubjectName);
            btnEdit   = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}
