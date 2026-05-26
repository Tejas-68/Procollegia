package com.procollegia;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Theme is handled in ProCollegiaApp globally.

        setContentView(R.layout.activity_splash);

        // Minimum splash delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Check real Firebase Session
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            routeToRole(documentSnapshot.getString("role"));
                        } else {
                            // Document missing, re-login required
                            FirebaseAuth.getInstance().signOut();
                            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Network error? Don't sign out, just retry login
                        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                        finish();
                    });
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }
        }, 1500);
    }

    private void routeToRole(String role) {
        Intent intent;
        if ("Teacher".equals(role)) {
            intent = new Intent(this, TeacherDashboardActivity.class);
        } else if ("PT Admin".equals(role)) {
            intent = new Intent(this, PtAdminDashboardActivity.class);
        } else if ("Principal".equals(role)) {
            intent = new Intent(this, PrincipalDashboardActivity.class);
        } else if ("HOD".equals(role)) {
            intent = new Intent(this, HodDashboardActivity.class);
        } else {
            intent = new Intent(this, StudentDashboardActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
