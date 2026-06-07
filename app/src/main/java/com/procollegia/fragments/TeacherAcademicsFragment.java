package com.procollegia.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.procollegia.R;

public class TeacherAcademicsFragment extends Fragment {

    private TextView tabAttendance, tabInternals;
    private android.widget.FrameLayout flContent;

    public TeacherAcademicsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_teacher_academics, container, false);

        tabAttendance = root.findViewById(R.id.tabAcAttendance);
        tabInternals  = root.findViewById(R.id.tabAcInternals);
        flContent     = root.findViewById(R.id.flAcademicsContent);

        tabAttendance.setOnClickListener(v -> switchTab(0));
        tabInternals.setOnClickListener(v -> switchTab(1));

        switchTab(0);
        return root;
    }

    private void switchTab(int index) {
        setTab(tabAttendance, index == 0);
        setTab(tabInternals,  index == 1);

        Fragment frag = (index == 0) ? new TeacherAttendanceFragment() : new TeacherInternalsFragment();
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.flAcademicsContent, frag)
                .commit();
    }

    private void setTab(TextView tab, boolean active) {
        tab.setBackgroundResource(active ? R.drawable.bg_pill_active : R.drawable.bg_pill_inactive);
        tab.setTextColor(ContextCompat.getColor(requireContext(),
                active ? R.color.text_on_accent : R.color.text_secondary));
    }
}
