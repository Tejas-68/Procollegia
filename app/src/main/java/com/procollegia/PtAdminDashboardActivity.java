package com.procollegia;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.procollegia.fragments.PtHomeFragment;
import com.procollegia.fragments.PtInventoryFragment;
import com.procollegia.fragments.PtTournamentFragment;
import com.procollegia.fragments.PtHonorScoreFragment;
import com.procollegia.fragments.PtProfileFragment;

public class PtAdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pt_admin_dashboard);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new PtHomeFragment();
            } else if (itemId == R.id.nav_inventory) {
                selectedFragment = new PtInventoryFragment();
            } else if (itemId == R.id.nav_tournament) {
                selectedFragment = new PtTournamentFragment();
            } else if (itemId == R.id.nav_honor) {
                selectedFragment = new PtHonorScoreFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new PtProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            }
            return true;
        });

        // Load the home fragment by default
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }
}
