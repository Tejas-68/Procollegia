package com.procollegia.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.procollegia.HodConfigurationActivity;
import com.procollegia.R;

public class HodAcademicsFragment extends Fragment {

    private TextView tabAcAttendance, tabAcInternals;
    private View btnHodConfig;

    public HodAcademicsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_hod_academics, container, false);

        tabAcAttendance = root.findViewById(R.id.tabAcAttendance);
        tabAcInternals = root.findViewById(R.id.tabAcInternals);
        btnHodConfig = root.findViewById(R.id.btnHodConfig);

        tabAcAttendance.setOnClickListener(v -> switchTab(0));
        tabAcInternals.setOnClickListener(v -> switchTab(1));

        btnHodConfig.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), HodConfigurationActivity.class));
        });

        switchTab(0);

        return root;
    }

    private void switchTab(int index) {
        tabAcAttendance.setBackgroundResource(index == 0 ? R.drawable.bg_pill_active : R.drawable.bg_pill_inactive);
        tabAcAttendance.setTextColor(ContextCompat.getColor(requireContext(), index == 0 ? R.color.text_on_accent : R.color.text_secondary));

        tabAcInternals.setBackgroundResource(index == 1 ? R.drawable.bg_pill_active : R.drawable.bg_pill_inactive);
        tabAcInternals.setTextColor(ContextCompat.getColor(requireContext(), index == 1 ? R.color.text_on_accent : R.color.text_secondary));

        Fragment target = (index == 0) ? new HodAttendanceSubFragment() : new TeacherInternalsFragment();

        getChildFragmentManager()
            .beginTransaction()
            .replace(R.id.flAcademicsContent, target)
            .commit();
    }
}
