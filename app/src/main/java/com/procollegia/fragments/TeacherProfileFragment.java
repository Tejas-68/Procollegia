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

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.preference.PreferenceManager;
import com.procollegia.LoginActivity;
import com.procollegia.R;

public class TeacherProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvPhone, tvDept, tvSetDeptValue;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;

    public TeacherProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_teacher_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        tvName  = root.findViewById(R.id.tvProfileName);
        tvEmail = root.findViewById(R.id.tvProfileEmail);
        tvPhone = root.findViewById(R.id.tvProfilePhone);
        tvDept  = root.findViewById(R.id.tvProfileDept);
        tvSetDeptValue = root.findViewById(R.id.tvSetDeptValue);

        setupDarkMode(root);
        loadProfileData();

        root.findViewById(R.id.btnSignOut).setOnClickListener(v -> signOut());
        root.findViewById(R.id.btnHelpSupport).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Support ticket system coming soon!", Toast.LENGTH_SHORT).show());
        root.findViewById(R.id.rowSetDept).setOnClickListener(v -> showSetDepartmentDialog());

        return root;
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

    private void loadProfileData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        tvEmail.setText(user.getEmail());

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(d -> {
                    if (d.exists()) {
                        String name = d.getString("name");
                        String dept = d.getString("department");
                        String phone = d.getString("phone");
                        if (name != null) tvName.setText(name);
                        if (phone != null) tvPhone.setText(phone);
                        
                        if (dept != null && !dept.trim().isEmpty()) {
                            tvDept.setText("Professor, " + dept + " Dept.");
                            if (tvSetDeptValue != null) tvSetDeptValue.setText(dept.toUpperCase());
                        } else {
                            tvDept.setText("Professor");
                            if (tvSetDeptValue != null) tvSetDeptValue.setText("Not Set");
                        }
                    }
                });
    }

    private void showSetDepartmentDialog() {
        String[] departments = {"BCA", "BBA", "BCOM", "BSC"};
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Department")
                .setItems(departments, (dialog, which) -> {
                    String selectedDept = departments[which];
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        db.collection("users").document(user.getUid())
                                .update("department", selectedDept)
                                .addOnSuccessListener(v -> {
                                    Toast.makeText(getContext(), "Department updated to " + selectedDept, Toast.LENGTH_SHORT).show();
                                    tvDept.setText("Professor, " + selectedDept + " Dept.");
                                    if (tvSetDeptValue != null) tvSetDeptValue.setText(selectedDept);
                                })
                                .addOnFailureListener(e -> 
                                    Toast.makeText(getContext(), "Failed to update department", Toast.LENGTH_SHORT).show());
                    }
                })
                .show();
    }

    private void signOut() {
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }
}
