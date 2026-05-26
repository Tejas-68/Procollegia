package com.procollegia;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * P4: Principal Dashboard — reads live stats from Firestore:
 *  - Total students, teachers, HODs
 *  - Average honor score
 *  - Pending leave requests
 */
public class PrincipalDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal_dashboard);

        db = FirebaseFirestore.getInstance();

        // Sign out
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        loadStats();
    }

    private void loadStats() {
        // Stats UI not yet implemented in layout
    }

    private void setText(int viewId, String value) {
        try {
            TextView tv = findViewById(viewId);
            if (tv != null) tv.setText(value);
        } catch (Exception ignored) {}
    }
}
