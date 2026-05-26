package com.procollegia.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

public class PtProfileFragment extends Fragment {

    private TextView tvName, tvRole;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;

    public PtProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pt_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        tvName = root.findViewById(R.id.tvProfileName);
        tvRole = root.findViewById(R.id.tvProfileRole);

        setupDarkMode(root);
        loadProfileData();

        root.findViewById(R.id.btnEditName).setOnClickListener(v -> showEditNameDialog());
        root.findViewById(R.id.btnChangePassword).setOnClickListener(v -> showChangePasswordDialog());
        root.findViewById(R.id.btnSignOut).setOnClickListener(v -> signOut());

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

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(d -> {
                    if (d.exists()) {
                        String name = d.getString("name");
                        if (name != null) tvName.setText(name);
                        tvRole.setText("Physical Training Admin");
                    }
                });
    }

    private void showEditNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Update My Name");
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(tvName.getText().toString());
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                db.collection("users").document(mAuth.getUid()).update("name", newName)
                        .addOnSuccessListener(v -> {
                            tvName.setText(newName);
                            Toast.makeText(getContext(), "Name updated!", Toast.LENGTH_SHORT).show();
                        });
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Update Password");
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Enter new password");
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newPass = input.getText().toString().trim();
            if (newPass.length() >= 6) {
                mAuth.getCurrentUser().updatePassword(newPass)
                        .addOnSuccessListener(v -> Toast.makeText(getContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } else {
                Toast.makeText(getContext(), "Min 6 characters required", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void signOut() {
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }
}
