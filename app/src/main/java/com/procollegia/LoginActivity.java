package com.procollegia;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private android.widget.TextView tvRegister, tvForgotPassword;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> loginUser());
        
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void showForgotPasswordDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter Email or UUCMS ID");
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("Enter your Email or UUCMS ID to receive a password reset link.")
            .setView(input)
            .setPositiveButton("Send Reset Link", (dialog, which) -> {
                String inputStr = input.getText().toString().trim();
                if (inputStr.isEmpty()) {
                    Toast.makeText(this, "Please enter your Email or ID", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                progressBar.setVisibility(View.VISIBLE);
                if (!inputStr.contains("@")) {
                    FirebaseFirestore.getInstance().collection("users")
                        .whereEqualTo("uucmsId", inputStr)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(qs -> {
                            if (!qs.isEmpty()) {
                                String email = qs.getDocuments().get(0).getString("email");
                                if (email != null && !email.isEmpty()) {
                                    sendResetEmail(email);
                                } else {
                                    showLoginError("No email associated with this UUCMS ID");
                                }
                            } else {
                                showLoginError("UUCMS ID not found");
                            }
                        })
                        .addOnFailureListener(e -> showLoginError("Lookup failed: " + e.getMessage()));
                } else {
                    sendResetEmail(inputStr);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void sendResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "Password reset email sent to " + email, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Error sending reset email: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
    }

    private void loginUser() {
        String emailOrUucms = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (emailOrUucms.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter all details", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        if (!emailOrUucms.contains("@")) {
            // Treat as UUCMS ID, lookup email
            FirebaseFirestore.getInstance().collection("users")
                .whereEqualTo("uucmsId", emailOrUucms)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    if (!qs.isEmpty()) {
                        String email = qs.getDocuments().get(0).getString("email");
                        if (email != null && !email.isEmpty()) {
                            performFirebaseAuth(email, password);
                        } else {
                            showLoginError("No email associated with this UUCMS ID");
                        }
                    } else {
                        showLoginError("UUCMS ID not found");
                    }
                })
                .addOnFailureListener(e -> showLoginError("Lookup failed: " + e.getMessage()));
        } else {
            performFirebaseAuth(emailOrUucms, password);
        }
    }

    private void performFirebaseAuth(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String uid = task.getResult().getUser().getUid();
                    FirebaseFirestore.getInstance().collection("users").document(uid).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            progressBar.setVisibility(View.GONE);
                            btnLogin.setEnabled(true);

                            if (documentSnapshot.exists()) {
                                String role = documentSnapshot.getString("role");
                                Toast.makeText(LoginActivity.this, "Welcome Back", Toast.LENGTH_SHORT).show();
                                
                                Intent intent;
                                if ("Teacher".equals(role)) {
                                    intent = new Intent(LoginActivity.this, TeacherDashboardActivity.class);
                                } else if ("HOD".equals(role)) {
                                    intent = new Intent(LoginActivity.this, HodDashboardActivity.class);
                                } else if ("PT Admin".equals(role)) {
                                    intent = new Intent(LoginActivity.this, PtAdminDashboardActivity.class);
                                } else if ("Principal".equals(role)) {
                                    intent = new Intent(LoginActivity.this, PrincipalDashboardActivity.class);
                                } else {
                                    intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
                                }
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "User details not found. Please register.", Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                            }
                        })
                        .addOnFailureListener(e -> showLoginError("Error fetching details: " + e.getMessage()));
                } else {
                    showLoginError("Error: " + task.getException().getMessage());
                }
            });
    }

    private void showLoginError(String message) {
        progressBar.setVisibility(View.GONE);
        btnLogin.setEnabled(true);
        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
    }
}
