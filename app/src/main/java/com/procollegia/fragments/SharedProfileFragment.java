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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.procollegia.LoginActivity;
import com.procollegia.R;

public class SharedProfileFragment extends Fragment {

    private TextView tvProfileName, tvProfileRole, tvUucms, tvEmail, tvCurrentTheme;
    private View rowUucms, divUucms, rowEmail, rowChangePassword, rowDarkMode, btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shared_profile, container, false);

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileRole = view.findViewById(R.id.tvProfileRole);
        tvUucms = view.findViewById(R.id.tvUucms);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvCurrentTheme = view.findViewById(R.id.tvCurrentTheme);

        rowUucms = view.findViewById(R.id.rowUucms);
        divUucms = view.findViewById(R.id.divUucms);
        rowEmail = view.findViewById(R.id.rowEmail);
        rowChangePassword = view.findViewById(R.id.rowChangePassword);
        rowDarkMode = view.findViewById(R.id.rowDarkMode);
        btnLogout = view.findViewById(R.id.btnLogout);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        loadProfileData();
        setupThemeUI();

        rowEmail.setOnClickListener(v -> promptReAuthAndAction("email"));
        rowChangePassword.setOnClickListener(v -> promptReAuthAndAction("password"));
        rowDarkMode.setOnClickListener(v -> showThemeSelector());
        btnLogout.setOnClickListener(v -> logout());

        return view;
    }

    private void loadProfileData() {
        if (currentUser == null) return;

        tvEmail.setText(currentUser.getEmail());

        db.collection("users").document(currentUser.getUid()).get()
            .addOnSuccessListener(d -> {
                if (d.exists()) {
                    String name = d.getString("name");
                    String role = d.getString("role");
                    String uucms = d.getString("uucmsId");

                    tvProfileName.setText(name != null ? name : "Unknown");
                    tvProfileRole.setText(role != null ? role : "Role");

                    if ("Student".equals(role)) {
                        rowUucms.setVisibility(View.VISIBLE);
                        divUucms.setVisibility(View.VISIBLE);
                        tvUucms.setText(uucms != null ? uucms : "N/A");
                    } else {
                        rowUucms.setVisibility(View.GONE);
                        divUucms.setVisibility(View.GONE);
                    }
                }
            });
    }

    private void setupThemeUI() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE);
        int themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (themeMode == AppCompatDelegate.MODE_NIGHT_YES) {
            tvCurrentTheme.setText("Dark");
        } else if (themeMode == AppCompatDelegate.MODE_NIGHT_NO) {
            tvCurrentTheme.setText("Light");
        } else {
            tvCurrentTheme.setText("System");
        }
    }

    private void showThemeSelector() {
        String[] options = {"Light", "Dark", "System"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Theme");
        builder.setItems(options, (dialog, which) -> {
            int mode;
            if (which == 0) mode = AppCompatDelegate.MODE_NIGHT_NO;
            else if (which == 1) mode = AppCompatDelegate.MODE_NIGHT_YES;
            else mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

            SharedPreferences.Editor editor = requireActivity().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE).edit();
            editor.putInt("theme_mode", mode);
            editor.apply();

            AppCompatDelegate.setDefaultNightMode(mode);
            // Recreate activity to apply theme instantly
            requireActivity().recreate();
        });
        builder.show();
    }

    private void promptReAuthAndAction(String actionType) {
        if (currentUser == null || currentUser.getEmail() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Verify Identity");
        builder.setMessage("Please enter your current password to continue.");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Verify", (dialog, which) -> {
            String password = input.getText().toString();
            if (!password.isEmpty()) {
                AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);
                currentUser.reauthenticate(credential)
                    .addOnSuccessListener(aVoid -> {
                        if ("email".equals(actionType)) {
                            showEditEmailDialog();
                        } else if ("password".equals(actionType)) {
                            showChangePasswordDialog();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Authentication Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showEditEmailDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Update Email");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint("New Email Address");
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newEmail = input.getText().toString().trim();
            if (!newEmail.isEmpty()) {
                currentUser.updateEmail(newEmail)
                    .addOnSuccessListener(aVoid -> {
                        // Also update in Firestore
                        db.collection("users").document(currentUser.getUid()).update("email", newEmail)
                                .addOnSuccessListener(v -> {
                                    Toast.makeText(getContext(), "Email updated successfully", Toast.LENGTH_SHORT).show();
                                    tvEmail.setText(newEmail);
                                });
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update email: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Change Password");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("New Password");
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newPassword = input.getText().toString();
            if (!newPassword.isEmpty()) {
                currentUser.updatePassword(newPassword)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Password updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update password: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(getContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
