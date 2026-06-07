package com.procollegia;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.procollegia.fragments.StudentAcademicsFragment;
import com.procollegia.fragments.StudentAttendanceFragment;
import com.procollegia.fragments.StudentHomeFragment;

import com.procollegia.fragments.StudentSportsFragment;

public class StudentDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new StudentHomeFragment();
            } else if (itemId == R.id.nav_attendance) {
                selectedFragment = new StudentAttendanceFragment();
            } else if (itemId == R.id.nav_academics) {
                selectedFragment = new StudentAcademicsFragment();
            } else if (itemId == R.id.nav_sports) {
                selectedFragment = new StudentSportsFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new com.procollegia.fragments.SharedProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, selectedFragment)
                    .commit();
            }

            return true;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }
}
