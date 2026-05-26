package com.procollegia;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TournamentRegistrationActivity extends AppCompatActivity {

    private String tourneyId, tourneyName, gameType;
    private int maxPlayers;
    private List<EditText> memberFields = new ArrayList<>();
    private LinearLayout llMembersContainer, llTeamSection, llSoloSection;
    private EditText etTeamName;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournament_registration);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        tourneyId = getIntent().getStringExtra("id");
        tourneyName = getIntent().getStringExtra("name");
        gameType = getIntent().getStringExtra("gameType");
        maxPlayers = getIntent().getIntExtra("maxPlayers", 11);

        ((TextView) findViewById(R.id.tvTourneyNameDisplay)).setText(tourneyName);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        llTeamSection = findViewById(R.id.llTeamSection);
        llSoloSection = findViewById(R.id.llSoloSection);
        llMembersContainer = findViewById(R.id.llMembersContainer);
        etTeamName = findViewById(R.id.etTeamName);

        if ("Team".equalsIgnoreCase(gameType)) {
            llTeamSection.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvMemberHint)).setText("Add Team Members (Max " + maxPlayers + ")");
            addMemberField(); // Initial field
        } else {
            llSoloSection.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.btnAddMember).setOnClickListener(v -> {
            if (memberFields.size() < maxPlayers) {
                addMemberField();
            } else {
                Toast.makeText(this, "Maximum members reached!", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnFinalRegister).setOnClickListener(v -> submitRegistration());
    }

    private void addMemberField() {
        EditText et = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (56 * getResources().getDisplayMetrics().density));
        lp.setMargins(0, (int) (12 * getResources().getDisplayMetrics().density), 0, 0);
        et.setLayoutParams(lp);
        et.setPadding((int) (16 * getResources().getDisplayMetrics().density), 0, (int) (16 * getResources().getDisplayMetrics().density), 0);
        et.setHint("Member " + (memberFields.size() + 1) + " Name");
        et.setBackgroundResource(R.drawable.bg_neumorph_card_sm);
        et.setTextColor(getResources().getColor(R.color.text_primary));
        et.setHintTextColor(getResources().getColor(R.color.text_secondary));
        
        llMembersContainer.addView(et);
        memberFields.add(et);
    }

    private void submitRegistration() {
        if (uid == null) return;

        // Check for duplicate registration
        db.collection("tournaments").document(tourneyId)
            .collection("teams")
            .whereEqualTo("studentId", uid)
            .get()
            .addOnSuccessListener(qs -> {
                if (!qs.isEmpty()) {
                    Toast.makeText(this, "You have already registered for this tournament!", Toast.LENGTH_SHORT).show();
                } else {
                    proceedWithRegistration();
                }
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Error checking registration.", Toast.LENGTH_SHORT).show());
    }

    private void proceedWithRegistration() {
        Map<String, Object> regData = new HashMap<>();
        regData.put("tournamentId", tourneyId);
        regData.put("tournamentName", tourneyName);
        regData.put("studentId", uid);
        regData.put("gameType", gameType);
        regData.put("timestamp", FieldValue.serverTimestamp());

        if ("Team".equalsIgnoreCase(gameType)) {
            String teamName = etTeamName.getText().toString().trim();
            if (TextUtils.isEmpty(teamName)) {
                Toast.makeText(this, "Enter Team Name!", Toast.LENGTH_SHORT).show();
                return;
            }
            List<String> players = new ArrayList<>();
            for (EditText et : memberFields) {
                String p = et.getText().toString().trim();
                if (!TextUtils.isEmpty(p)) players.add(p);
            }
            if (players.isEmpty()) {
                Toast.makeText(this, "Add at least one player!", Toast.LENGTH_SHORT).show();
                return;
            }
            regData.put("teamName", teamName);
            regData.put("members", players);
            regData.put("memberCount", players.size());
        }

        db.collection("tournaments").document(tourneyId)
                .collection("teams").add(regData)
                .addOnSuccessListener(ref -> {
                    // Also update counts in parent doc
                    int pCount = "Team".equalsIgnoreCase(gameType) ? 1 : 1; // Basic increment
                    db.collection("tournaments").document(tourneyId)
                            .update("joinedCount", FieldValue.increment(1),
                                    "joinedTeamsCount", FieldValue.increment("Team".equalsIgnoreCase(gameType) ? 1 : 0));
                    
                    Toast.makeText(this, "Successfully Joined!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error joining.", Toast.LENGTH_SHORT).show());
    }
}
