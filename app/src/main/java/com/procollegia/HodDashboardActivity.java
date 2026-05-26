package com.procollegia;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.activity.result.contract.ActivityResultContracts;
import com.procollegia.fragments.HodHomeFragment;
import com.procollegia.fragments.HodAcademicsFragment;
import com.procollegia.fragments.HodHonorScoreFragment;
import com.procollegia.fragments.HodProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HodDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_dashboard);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {}).launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        BottomNavigationView navView = findViewById(R.id.bottom_navigation);
        navView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_hod_home) {
                selectedFragment = new HodHomeFragment();
            } else if (id == R.id.nav_hod_academics) {
                selectedFragment = new HodAcademicsFragment();
            } else if (id == R.id.nav_hod_honor) {
                selectedFragment = new HodHonorScoreFragment();
            } else if (id == R.id.nav_hod_profile) {
                selectedFragment = new HodProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HodHomeFragment())
                    .commit();
        }
    }
}
