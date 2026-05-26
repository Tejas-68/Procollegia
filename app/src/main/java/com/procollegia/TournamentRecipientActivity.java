package com.procollegia;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * P3: Tournament recipient selection.
 * Reads matching student count live from Firestore users collection.
 * On submit, writes a tournamentInvitations document with the filter criteria.
 */
public class TournamentRecipientActivity extends AppCompatActivity {

    private TextView tvStatus;
    private ChipGroup chipGroupYear, chipGroupDept, chipGroupSection;
    private FirebaseFirestore db;
    private String uid;

    // The tournament ID passed from CreateTournamentActivity (optional)
    private String tournamentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournament_recipient);

        db  = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        tournamentId = getIntent().getStringExtra("tournamentId");

        tvStatus       = findViewById(R.id.tvSelectionStatus);
        chipGroupYear  = findViewById(R.id.chipGroupYear);
        chipGroupDept  = findViewById(R.id.chipGroupDept);
        chipGroupSection = findViewById(R.id.chipGroupSection);

        setupChips();

        findViewById(R.id.btnBackRecipient).setOnClickListener(v -> finish());

        findViewById(R.id.btnSubmitTournament).setOnClickListener(v -> {
            String year    = getSelectedChipLabel(chipGroupYear);
            String dept    = getSelectedChipLabel(chipGroupDept);
            String section = getSelectedChipLabel(chipGroupSection);

            if (year == null && dept == null && section == null) {
                Toast.makeText(this, "Please select at least one filter", Toast.LENGTH_SHORT).show();
                return;
            }
            submitInvitation(year, dept, section);
        });
    }

    // ── Chip helpers ──────────────────────────────────────────────────────────

    private void setupChips() {
        ChipGroup.OnCheckedChangeListener listener = (group, checkedId) -> refreshCount();
        chipGroupYear.setOnCheckedChangeListener(listener);
        chipGroupDept.setOnCheckedChangeListener(listener);
        chipGroupSection.setOnCheckedChangeListener(listener);
    }

    /**
     * Counts students from Firestore matching the selected filters and updates tvStatus.
     */
    private void refreshCount() {
        String year    = getSelectedChipLabel(chipGroupYear);
        String dept    = getSelectedChipLabel(chipGroupDept);
        String section = getSelectedChipLabel(chipGroupSection);

        com.google.firebase.firestore.Query query = db.collection("users")
                .whereEqualTo("role", "Student");

        if (year    != null) query = query.whereEqualTo("year",       year);
        if (dept    != null) query = query.whereEqualTo("department", dept);
        if (section != null) query = query.whereEqualTo("section",    section);

        query.get().addOnSuccessListener(qs ->
                tvStatus.setText("Selected: " + qs.size() + " students")
        ).addOnFailureListener(e ->
                tvStatus.setText("Could not count students")
        );
    }

    /** Returns the label of the checked chip in a group, or null if none. */
    private String getSelectedChipLabel(ChipGroup group) {
        int id = group.getCheckedChipId();
        if (id == -1) return null;
        Chip chip = group.findViewById(id);
        return chip != null ? chip.getText().toString() : null;
    }

    // ── Firestore submit ──────────────────────────────────────────────────────

    private void submitInvitation(String year, String dept, String section) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Map<String, Object> doc = new HashMap<>();
        doc.put("tournamentId", tournamentId != null ? tournamentId : "");
        doc.put("createdBy",    uid);
        doc.put("year",         year    != null ? year    : "All");
        doc.put("department",   dept    != null ? dept    : "All");
        doc.put("section",      section != null ? section : "All");
        doc.put("date",         today);
        doc.put("status",       "sent");
        doc.put("createdAt",    FieldValue.serverTimestamp());

        db.collection("tournamentInvitations").add(doc)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Tournament invitations sent successfully!", Toast.LENGTH_LONG).show();
                    Intent i = new Intent(this, TeacherDashboardActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send invitations: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
