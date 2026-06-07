package com.procollegia;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.procollegia.adapters.TeamAdapter;

import java.util.ArrayList;
import java.util.List;

public class TournamentDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvJoinedStudents, tvJoinedTeams, tvMaxPlayers;
    private RecyclerView rvTeams;
    private FirebaseFirestore db;
    private String tourneyId;
    private int maxTeams = 0, maxPlayers = 0;
    private View llManagementButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournament_detail);

        tvTitle = findViewById(R.id.tvDetailTitle);
        tvJoinedStudents = findViewById(R.id.tvJoinedStudents);
        tvJoinedTeams = findViewById(R.id.tvJoinedTeams);
        tvMaxPlayers = findViewById(R.id.tvMaxPlayers);
        rvTeams = findViewById(R.id.rvTeams);
        llManagementButtons = findViewById(R.id.llManagementButtons);
        llManagementButtons.setVisibility(View.GONE);

        rvTeams.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        tourneyId = getIntent().getStringExtra("tournamentId");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        if (tourneyId != null) {
            loadTournamentStats();
            loadTeams();
            checkPermissions();
        }

        findViewById(R.id.btnIncTeams).setOnClickListener(v -> adjustLimit("maxTeams", maxTeams + 1));
        findViewById(R.id.btnDecTeams).setOnClickListener(v -> { if(maxTeams > 1) adjustLimit("maxTeams", maxTeams - 1); });
        findViewById(R.id.btnIncPlayers).setOnClickListener(v -> adjustLimit("maxPlayers", maxPlayers + 1));
        findViewById(R.id.btnDecPlayers).setOnClickListener(v -> { if(maxPlayers > 1) adjustLimit("maxPlayers", maxPlayers - 1); });
    }

    private void checkPermissions() {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
                String role = userDoc.getString("role");
                db.collection("tournaments").document(tourneyId).get().addOnSuccessListener(d -> {
                    String type = d.getString("type");
                    boolean canEdit = false;
                    if ("Sports".equalsIgnoreCase(type)) {
                        if ("PT Admin".equalsIgnoreCase(role)) canEdit = true;
                    } else {
                        if ("Teacher".equalsIgnoreCase(role) || "HOD".equalsIgnoreCase(role) || "Principal".equalsIgnoreCase(role)) canEdit = true;
                    }
                    llManagementButtons.setVisibility(canEdit ? View.VISIBLE : View.GONE);
                });
            });
        }
    }

    private void loadTournamentStats() {
        db.collection("tournaments").document(tourneyId).addSnapshotListener((d, e) -> {
            if (d != null && d.exists()) {
                tvTitle.setText(d.getString("name"));
                
                long joined = d.getLong("joinedCount") != null ? d.getLong("joinedCount") : 0;
                long teamCount = d.getLong("joinedTeamsCount") != null ? d.getLong("joinedTeamsCount") : 0;
                maxTeams = Math.toIntExact(d.getLong("maxTeams") != null ? d.getLong("maxTeams") : 0);
                maxPlayers = Math.toIntExact(d.getLong("maxPlayers") != null ? d.getLong("maxPlayers") : 0);

                tvMaxPlayers.setText(String.valueOf(maxPlayers));
                tvJoinedStudents.setText(joined + " / " + (maxTeams * maxPlayers));
                tvJoinedTeams.setText(teamCount + " / " + maxTeams);
            }
        });
    }

    private void loadTeams() {
        // Assuming teams are in a subcollection 'teams' under the tournament
        db.collection("tournaments").document(tourneyId).collection("teams")
                .get().addOnSuccessListener(qs -> {
                    List<TeamAdapter.TeamItem> teamList = new ArrayList<>();
                    for (QueryDocumentSnapshot d : qs) {
                        String name = d.getString("teamName");
                        if (name == null) name = d.getString("name"); // fallback
                        if (name == null) name = "Unknown";
                        teamList.add(new TeamAdapter.TeamItem(
                                name,
                                Math.toIntExact(d.getLong("memberCount") != null ? d.getLong("memberCount") : 0)
                        ));
                    }
                    rvTeams.setAdapter(new TeamAdapter(teamList));
                });
    }

    private void adjustLimit(String field, int newVal) {
        db.collection("tournaments").document(tourneyId).update(field, newVal)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Capacity increased!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
