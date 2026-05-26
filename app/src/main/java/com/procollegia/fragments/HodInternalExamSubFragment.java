package com.procollegia.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.procollegia.R;

public class HodInternalExamSubFragment extends Fragment {

    public HodInternalExamSubFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_hod_internal_exam_sub, container, false);

        root.findViewById(R.id.btnScheduleNewExam).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Exam Scheduling Panel Coming Soon", Toast.LENGTH_SHORT).show();
        });

        return root;
    }
}
