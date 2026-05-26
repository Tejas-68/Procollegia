package com.procollegia.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.procollegia.LoginActivity;
import com.procollegia.R;

public class HodProfileFragment extends Fragment {

    private TextView tvName, tvRole, tvEmail;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;

    public HodProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_hod_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        tvName = root.findViewById(R.id.tvProfileName);
        tvRole = root.findViewById(R.id.tvProfileRole);
        tvEmail = root.findViewById(R.id.tvProfileEmail);

        loadUserData();
        setupDarkMode(root);

        root.findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

        return root;
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvEmail.setText(user.getEmail());
            db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(d -> {
                    if (isAdded() && d.exists()) {
                        tvName.setText(d.getString("name"));
                        tvRole.setText("Head of Department (" + d.getString("department") + ")");
                    }
                });
        }
    }

    private void setupDarkMode(View root) {
        TextView tvCurrentTheme = root.findViewById(R.id.tvCurrentTheme);
        int themePref = prefs.getInt("theme_pref", -1);
        
        if (themePref == 1) tvCurrentTheme.setText("Light");
        else if (themePref == 2) tvCurrentTheme.setText("Dark");
        else tvCurrentTheme.setText("System");

        root.findViewById(R.id.rowDarkMode).setOnClickListener(v -> {
            String[] themes = {"System Default", "Light", "Dark"};
            int checkedItem = themePref == 1 ? 1 : (themePref == 2 ? 2 : 0);
            
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select App Theme")
                .setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
                    int newPref = -1;
                    if (which == 1) newPref = 1;
                    else if (which == 2) newPref = 2;
                    
                    prefs.edit().putInt("theme_pref", newPref).apply();
                    if (newPref == 1) tvCurrentTheme.setText("Light");
                    else if (newPref == 2) tvCurrentTheme.setText("Dark");
                    else tvCurrentTheme.setText("System");
                    
                    if (newPref == 1) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    } else if (newPref == 2) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    }
                    dialog.dismiss();
                })
                .show();
        });
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }
}
