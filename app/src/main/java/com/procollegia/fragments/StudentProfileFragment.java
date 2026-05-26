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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.procollegia.LoginActivity;
import com.procollegia.R;

public class StudentProfileFragment extends Fragment {

    private TextView tvName, tvEmail;
    private TextView tvStatSem, tvStatAttendance, tvStatHonor;
    private TextView tvRollNo, tvDept, tvSemester, tvSection;
    private TextView tvPhone, tvParentPhone;
    private TextView btnLogout;

    public StudentProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_student_profile, container, false);

        // Bind views
        tvName          = root.findViewById(R.id.tvProfileName);
        tvEmail         = root.findViewById(R.id.tvProfileEmail);
        tvStatSem       = root.findViewById(R.id.tvStatSem);
        tvStatAttendance= root.findViewById(R.id.tvStatAttendance);
        tvStatHonor     = root.findViewById(R.id.tvStatHonor);
        tvRollNo        = root.findViewById(R.id.tvRollNo);
        tvDept          = root.findViewById(R.id.tvDept);
        tvSemester      = root.findViewById(R.id.tvSemester);
        tvSection       = root.findViewById(R.id.tvSection);
        tvPhone         = root.findViewById(R.id.tvPhone);
        tvParentPhone   = root.findViewById(R.id.tvParentPhone);
        btnLogout       = root.findViewById(R.id.btnLogout);

        // Notifications switch
        SwitchMaterial swNotif = root.findViewById(R.id.switchNotifications);
        swNotif.setOnCheckedChangeListener((btn, checked) ->
                Toast.makeText(getContext(),
                        checked ? "Notifications enabled" : "Notifications disabled",
                        Toast.LENGTH_SHORT).show());

        // App Theme row
        TextView tvCurrentTheme = root.findViewById(R.id.tvCurrentTheme);
        SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext());
        
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

        // Change password row
        root.findViewById(R.id.rowChangePassword).setOnClickListener(v ->
                sendPasswordReset());

        // About row
        root.findViewById(R.id.rowAbout).setOnClickListener(v ->
                Toast.makeText(getContext(), "ProCollegia v1.0 — Built with ❤️", Toast.LENGTH_SHORT).show());

        // Sign out
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Load data
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tvEmail.setText(user.getEmail());
            loadProfile(user.getUid());
        }

        return root;
    }

    private void loadProfile(String uid) {
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (getActivity() == null || !isAdded()) return;
                    getActivity().runOnUiThread(() -> bindDoc(doc));
                });
    }

    private void bindDoc(DocumentSnapshot doc) {
        // Header
        String name = doc.getString("name");
        if (name != null) tvName.setText(name);

        // Academic info
        String roll = doc.getString("uucmsId");
        if (roll != null) tvRollNo.setText(roll);

        String dept = doc.getString("department");
        if (dept != null) tvDept.setText(dept);

        String sem = doc.getString("semester");
        if (sem != null) {
            tvSemester.setText(sem + " Semester");
            tvStatSem.setText(ordinal(sem));
        }

        String section = doc.getString("section");
        if (section != null) tvSection.setText("Section " + section);

        // Contact
        String phone = doc.getString("phone");
        if (phone != null) tvPhone.setText(phone);

        String parent = doc.getString("parentPhone");
        if (parent != null) tvParentPhone.setText(parent);

        // Quick stats
        Object attendance = doc.get("attendancePercent");
        if (attendance != null) tvStatAttendance.setText(attendance + "%");

        Object honor = doc.get("honorScore");
        if (honor != null) tvStatHonor.setText(String.valueOf(honor));
    }

    private void sendPasswordReset() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        FirebaseAuth.getInstance()
                .sendPasswordResetEmail(user.getEmail())
                .addOnSuccessListener(v ->
                        Toast.makeText(getContext(),
                                "Password reset email sent to " + user.getEmail(),
                                Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed to send reset email.", Toast.LENGTH_SHORT).show());
    }

    /** "4" → "4th", "1" → "1st", etc. */
    private String ordinal(String sem) {
        try {
            int n = Integer.parseInt(sem.replaceAll("[^0-9]", ""));
            String[] suf = {"th","st","nd","rd"};
            int idx = (n % 100 >= 11 && n % 100 <= 13) ? 0 : Math.min(n % 10, 3);
            return n + suf[idx];
        } catch (Exception e) {
            return sem;
        }
    }
}
