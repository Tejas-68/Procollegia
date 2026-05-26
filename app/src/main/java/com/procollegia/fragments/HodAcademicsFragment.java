package com.procollegia.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.procollegia.R;

public class HodAcademicsFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    public HodAcademicsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_hod_academics, container, false);

        tabLayout = root.findViewById(R.id.tabLayoutAcademics);
        viewPager = root.findViewById(R.id.viewPagerAcademics);

        setupViewPager();

        return root;
    }

    private void setupViewPager() {
        AcademicsPagerAdapter adapter = new AcademicsPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Attendance"); break;
                case 1: tab.setText("Staff"); break;
                case 2: tab.setText("Internal Exam"); break;
            }
        }).attach();
    }

    private static class AcademicsPagerAdapter extends FragmentStateAdapter {

        public AcademicsPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new HodAttendanceSubFragment();
                case 1: return new HodStaffSubFragment();
                case 2: return new HodInternalExamSubFragment();
                default: return new HodAttendanceSubFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
