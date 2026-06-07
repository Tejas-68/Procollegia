package com.procollegia;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etName, etPhone, etQualifications, etEmail, etPassword;
    private EditText etParentName, etParentPhone;
    private LinearLayout layoutStudentFields;
    private Button btnSave;
    private ImageView btnBack;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    private String currentRole = "";
    private String originalEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etQualifications = findViewById(R.id.etQualifications);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        
        layoutStudentFields = findViewById(R.id.layoutStudentFields);
        etParentName = findViewById(R.id.etParentName);
        etParentPhone = findViewById(R.id.etParentPhone);

        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> attemptSave());

        loadUserData();
    }

    private void loadUserData() {
        originalEmail = user.getEmail();
        etEmail.setText(originalEmail);

        db.collection("users").document(user.getUid()).get()
            .addOnSuccessListener(d -> {
                if (d.exists()) {
                    currentRole = d.getString("role");
                    
                    if (d.getString("name") != null) etName.setText(d.getString("name"));
                    if (d.getString("phone") != null) etPhone.setText(d.getString("phone"));
                    if (d.getString("qualifications") != null) etQualifications.setText(d.getString("qualifications"));
                    
                    if ("Student".equals(currentRole)) {
                        layoutStudentFields.setVisibility(View.VISIBLE);
                        if (d.getString("parentName") != null) etParentName.setText(d.getString("parentName"));
                        if (d.getString("parentPhone") != null) etParentPhone.setText(d.getString("parentPhone"));
                    }
                }
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to load details.", Toast.LENGTH_SHORT).show());
    }

    private void attemptSave() {
        String newName = etName.getText().toString().trim();
        String newPhone = etPhone.getText().toString().trim();
        String newQuals = etQualifications.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();
        String newPass = etPassword.getText().toString().trim();

        if (newName.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(this, "Name and Email cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean emailChanged = !newEmail.equals(originalEmail);
        boolean passChanged = !newPass.isEmpty();

        if (emailChanged || passChanged) {
            promptReauthenticate(newEmail, newPass, newName, newPhone, newQuals);
        } else {
            updateFirestoreData(newName, newPhone, newQuals, newEmail);
        }
    }

    private void promptReauthenticate(String newEmail, String newPass, String newName, String newPhone, String newQuals) {
        EditText input = new EditText(this);
        input.setHint("Current Password");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
            .setTitle("Authentication Required")
            .setMessage("Please enter your current password to update your email or password.")
            .setView(input)
            .setPositiveButton("Verify", (dialog, which) -> {
                String currentPass = input.getText().toString().trim();
                if (currentPass.isEmpty()) {
                    Toast.makeText(this, "Password required", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                AuthCredential credential = EmailAuthProvider.getCredential(originalEmail, currentPass);
                user.reauthenticate(credential).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        applyAuthChanges(newEmail, newPass, newName, newPhone, newQuals);
                    } else {
                        Toast.makeText(this, "Re-authentication failed.", Toast.LENGTH_LONG).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void applyAuthChanges(String newEmail, String newPass, String newName, String newPhone, String newQuals) {
        boolean emailChanged = !newEmail.equals(originalEmail);
        boolean passChanged = !newPass.isEmpty();

        if (passChanged) {
            user.updatePassword(newPass).addOnCompleteListener(t1 -> {
                if (!t1.isSuccessful()) {
                    Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show();
                } else if (!emailChanged) {
                    updateFirestoreData(newName, newPhone, newQuals, newEmail);
                }
            });
        }

        if (emailChanged) {
            user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(t2 -> {
                if (t2.isSuccessful()) {
                    Toast.makeText(this, "Verification email sent to " + newEmail, Toast.LENGTH_LONG).show();
                    updateFirestoreData(newName, newPhone, newQuals, newEmail);
                } else {
                    Toast.makeText(this, "Failed to update email", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateFirestoreData(String name, String phone, String quals, String email) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("qualifications", quals);
        updates.put("email", email); // Keep Firestore email in sync

        if ("Student".equals(currentRole)) {
            updates.put("parentName", etParentName.getText().toString().trim());
            updates.put("parentPhone", etParentPhone.getText().toString().trim());
        }

        db.collection("users").document(user.getUid()).update(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Error saving to database.", Toast.LENGTH_SHORT).show());
    }
}
