package com.procollegia;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.procollegia.fragments.HodConfigAcademicYearFragment;
import com.procollegia.fragments.HodConfigAttendanceFragment;
import com.procollegia.fragments.HodConfigInternalsFragment;
import com.procollegia.fragments.HodConfigStaffsFragment;

public class HodConfigurationActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_configuration);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tabLayout = findViewById(R.id.tabLayoutConfig);
        viewPager = findViewById(R.id.viewPagerConfig);

        ConfigPagerAdapter adapter = new ConfigPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Attendance"); break;
                case 1: tab.setText("Internals"); break;
                case 2: tab.setText("Subjects"); break;
                case 3: tab.setText("Academic Year"); break;
                case 4: tab.setText("Staffs"); break;
            }
        }).attach();
    }

    private static class ConfigPagerAdapter extends FragmentStateAdapter {
        public ConfigPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new HodConfigAttendanceFragment();
                case 1: return new HodConfigInternalsFragment();
                case 2: return new com.procollegia.fragments.TeacherSubjectsFragment();
                case 3: return new HodConfigAcademicYearFragment();
                case 4: return new HodConfigStaffsFragment();
                default: return new HodConfigAttendanceFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 5;
        }
    }
}
