package com.procollegia;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CreateTournamentActivity extends AppCompatActivity {

    private EditText etName, etDesc, etVenue, etMaxTeams, etMaxPlayers, etConditions;
    private TextView btnFrom, btnTo;
    private Button btnCreate;
    private FirebaseFirestore db;
    private String dateFromValue = "", dateToValue = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_tournament);

        etName       = findViewById(R.id.etTournamentName);
        etDesc       = findViewById(R.id.etDescription);
        etVenue      = findViewById(R.id.etVenue);
        etMaxTeams   = findViewById(R.id.etMaxTeams);
        etMaxPlayers = findViewById(R.id.etMaxPlayers);
        etConditions = findViewById(R.id.etConditions);
        btnFrom      = findViewById(R.id.btnDateFrom);
        btnTo        = findViewById(R.id.btnDateTo);
        btnCreate    = findViewById(R.id.btnCreateEvent);

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnFrom.setOnClickListener(v -> pickDate(true));
        btnTo.setOnClickListener(v -> pickDate(false));

        btnCreate.setOnClickListener(v -> saveTournament());
    }

    private void pickDate(boolean from) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            String date = d + "/" + (m + 1) + "/" + y;
            if (from) {
                dateFromValue = date;
                btnFrom.setText(date);
            } else {
                dateToValue = date;
                btnTo.setText(date);
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveTournament() {
        String name = etName.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        String venue = etVenue.getText().toString().trim();
        String maxT = etMaxTeams.getText().toString().trim();
        String maxP = etMaxPlayers.getText().toString().trim();
        String cond = etConditions.getText().toString().trim();

        // Read game type from toggle button group
        com.google.android.material.button.MaterialButtonToggleGroup tg = findViewById(R.id.toggleGameType);
        String gameType = (tg.getCheckedButtonId() == R.id.btnTeam) ? "Team" : "Solo";

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(dateFromValue) || TextUtils.isEmpty(maxT)) {
            Toast.makeText(this, "Please fill required fields (Name, Date, Max Teams)", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> tourney = new HashMap<>();
        tourney.put("name", name);
        tourney.put("description", desc);
        tourney.put("venue", venue);
        tourney.put("startDate", dateFromValue);
        tourney.put("endDate", dateToValue);
        tourney.put("maxTeams", Integer.parseInt(maxT));
        tourney.put("maxPlayers", Integer.parseInt(maxP.isEmpty() ? "1" : maxP));
        tourney.put("conditions", cond);
        tourney.put("status", "Upcoming");
        tourney.put("type", "Sports");
        tourney.put("gameType", gameType);
        tourney.put("joinedCount", 0);
        tourney.put("joinedTeamsCount", 0);

        btnCreate.setEnabled(false);
        db.collection("tournaments").add(tourney)
                .addOnSuccessListener(d -> {
                    Toast.makeText(this, "Tournament Launched!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnCreate.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
