package com.procollegia.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.procollegia.R;
import com.procollegia.adapters.LeaderboardAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PtHonorScoreFragment extends Fragment {

    private EditText etSearch;
    private View llLeaderboard;
    private ScrollView svPointsEditor;
    private RecyclerView rvLeaderboard, rvSearchResults;
    
    private TextView tvStudentMeta, tvPoints, tvTier, tvCounter;
    private TextView btnMinus, btnPlus;
    private Spinner spReason;
    private EditText etNote;
    
    private FirebaseFirestore db;
    private LeaderboardAdapter leaderboardAdapter, searchAdapter;
    private final List<LeaderboardAdapter.StudentScore> leaderboardList = new ArrayList<>();
    private final List<LeaderboardAdapter.StudentScore> searchList = new ArrayList<>();
    
    private String selectedId = null;
    private int selectedScore = 500; 
    private int delta = 25; 

    public PtHonorScoreFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pt_honor_score, container, false);
        db = FirebaseFirestore.getInstance();

        etSearch       = root.findViewById(R.id.etSearchStudent);
        llLeaderboard  = root.findViewById(R.id.llLeaderboard);
        svPointsEditor = root.findViewById(R.id.svPointsEditor);
        rvLeaderboard  = root.findViewById(R.id.rvLeaderboard);
        rvSearchResults= root.findViewById(R.id.rvSearchResults);
        
        tvStudentMeta  = root.findViewById(R.id.tvStudentMeta);
        tvPoints       = root.findViewById(R.id.tvPoints);
        tvTier         = root.findViewById(R.id.tvTier);
        btnMinus       = root.findViewById(R.id.btnMinus);
        btnPlus        = root.findViewById(R.id.btnPlus);
        tvCounter      = root.findViewById(R.id.tvCounter);
        spReason       = root.findViewById(R.id.spinnerReason);
        etNote         = root.findViewById(R.id.etOptionalNote);

        setupLeaderboard();
        setupSearchList();
        setupActions();
        setupSearch();
        setupReasonSpinner();

        root.findViewById(R.id.btnBackToList).setOnClickListener(v -> showLeaderboard());
        root.findViewById(R.id.btnApplyChanges).setOnClickListener(v -> applyChanges());

        return root;
    }

    private void setupLeaderboard() {
        leaderboardAdapter = new LeaderboardAdapter(leaderboardList, this::openEditor);
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(getContext()));
        rvLeaderboard.setAdapter(leaderboardAdapter);
        fetchLeaderboard();
    }

    private void setupSearchList() {
        searchAdapter = new LeaderboardAdapter(searchList, this::openEditor);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSearchResults.setAdapter(searchAdapter);
    }

    private void fetchLeaderboard() {
        db.collection("users")
                .whereEqualTo("role", "Student")
                .orderBy("honorScore", Query.Direction.DESCENDING)
                .limit(50) 
                .get()
                .addOnSuccessListener(qs -> {
                    leaderboardList.clear();
                    for (QueryDocumentSnapshot d : qs) {
                        Long pts = d.getLong("honorScore");
                        int currentScore = (pts != null) ? pts.intValue() : 500;
                        leaderboardList.add(new LeaderboardAdapter.StudentScore(
                                d.getId(),
                                d.getString("name"),
                                d.getString("department") != null ? d.getString("department") : "BCA",
                                d.getString("uucmsId") != null ? d.getString("uucmsId") : "N/A",
                                currentScore
                        ));
                    }
                    leaderboardAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    db.collection("users")
                            .whereEqualTo("role", "Student")
                            .limit(50)
                            .get()
                            .addOnSuccessListener(qs -> {
                                leaderboardList.clear();
                                for (QueryDocumentSnapshot d : qs) {
                                    Long pts = d.getLong("honorScore");
                                    int currentScore = (pts != null) ? pts.intValue() : 500;
                                    leaderboardList.add(new LeaderboardAdapter.StudentScore(
                                            d.getId(), d.getString("name"), d.getString("department"), d.getString("uucmsId"), currentScore));
                                }
                                Collections.sort(leaderboardList, (s1, s2) -> Integer.compare(s2.score, s1.score));
                                leaderboardAdapter.notifyDataSetChanged();
                            });
                });
    }

    private void openEditor(LeaderboardAdapter.StudentScore s) {
        selectedId = s.uid;
        selectedScore = s.score;
        
        tvStudentMeta.setText(s.name + " | " + s.dept + " | Roll: " + s.roll);
        tvPoints.setText(s.score + " pts");
        updateTierBadge(s.score);
        
        llLeaderboard.setVisibility(View.GONE);
        rvSearchResults.setVisibility(View.GONE);
        svPointsEditor.setVisibility(View.VISIBLE);
        
        delta = 25;
        tvCounter.setText("25");
        etNote.setText("");
        etSearch.clearFocus();
    }

    private void showLeaderboard() {
        svPointsEditor.setVisibility(View.GONE);
        rvSearchResults.setVisibility(View.GONE);
        llLeaderboard.setVisibility(View.VISIBLE);
        fetchLeaderboard(); 
    }

    private void setupActions() {
        btnMinus.setOnClickListener(v -> { delta -= 25; updateCounterUI(); });
        btnPlus.setOnClickListener(v -> { delta += 25; updateCounterUI(); });
    }

    private void updateCounterUI() {
        tvCounter.setText(String.valueOf(delta));
        if (delta > 0) tvCounter.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_blue));
        else if (delta < 0) tvCounter.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_orange));
        else tvCounter.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 2) performSearch(s.toString());
                else {
                    rvSearchResults.setVisibility(View.GONE);
                    if (svPointsEditor.getVisibility() != View.VISIBLE) llLeaderboard.setVisibility(View.VISIBLE);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearch(String q) {
        searchList.clear();
        for (LeaderboardAdapter.StudentScore s : leaderboardList) {
            if (s.name != null && s.name.toLowerCase().contains(q.toLowerCase())) searchList.add(s);
        }
        if (!searchList.isEmpty()) {
            llLeaderboard.setVisibility(View.GONE);
            svPointsEditor.setVisibility(View.GONE);
            rvSearchResults.setVisibility(View.VISIBLE);
            searchAdapter.notifyDataSetChanged();
        } else {
            db.collection("users").whereEqualTo("role", "Student").limit(50).get()
                    .addOnSuccessListener(qs -> {
                         for (QueryDocumentSnapshot d : qs) {
                             String name = d.getString("name");
                             if (name != null && name.toLowerCase().contains(q.toLowerCase())) {
                                 Long pts = d.getLong("honorScore");
                                 int currentScore = (pts != null) ? pts.intValue() : 500;
                                 searchList.add(new LeaderboardAdapter.StudentScore(d.getId(), name, d.getString("department"), d.getString("uucmsId"), currentScore));
                             }
                         }
                         if (!searchList.isEmpty()) {
                            llLeaderboard.setVisibility(View.GONE);
                            svPointsEditor.setVisibility(View.GONE);
                            rvSearchResults.setVisibility(View.VISIBLE);
                            searchAdapter.notifyDataSetChanged();
                         }
                    });
        }
    }

    private void updateTierBadge(int score) {
        if (score > 1200) { tvTier.setText(" Diamond"); tvTier.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.accent_purple)); }
        else if (score > 800) { tvTier.setText(" Gold"); tvTier.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.accent_gold)); }
        else if (score >= 500) { tvTier.setText(" Silver"); tvTier.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.neumorph_shadow_dark)); }
        else { tvTier.setText(" Bronze"); tvTier.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.accent_orange)); }
    }

    private void setupReasonSpinner() {
        String[] rs = {"Tournament MVP", "Sportsmanship Win", "Good Conduct", "Sports Participation", "Late Arrival", "Equipment Damage", "Other"};
        spReason.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, rs));
    }

    private void applyChanges() {
        if (selectedId == null) return;
        
        long changeValue = (long) delta;
        int newScoreValue = selectedScore + delta;
        
        Map<String, Object> update = new HashMap<>();
        update.put("honorScore", FieldValue.increment(changeValue));
        
        db.collection("users").document(selectedId).update(update)
                .addOnSuccessListener(v -> {
                    Toast.makeText(getContext(), "Honor Score Updated!", Toast.LENGTH_SHORT).show();
                    logEvent(selectedId, changeValue);
                    etSearch.setText("");
                    showLeaderboard();
                })
                .addOnFailureListener(e -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("honorScore", (long) newScoreValue);
                    db.collection("users").document(selectedId).update(data)
                            .addOnSuccessListener(v -> {
                                Toast.makeText(getContext(), "Initial Points Set!", Toast.LENGTH_SHORT).show();
                                logEvent(selectedId, changeValue);
                                etSearch.setText("");
                                showLeaderboard();
                            });
                });
    }

    private void logEvent(String si, long cp) {
        Map<String, Object> log = new HashMap<>();
        log.put("studentId", si);
        log.put("points", cp);
        log.put("reason", spReason.getSelectedItem().toString());
        log.put("note", etNote.getText().toString());
        log.put("timestamp", FieldValue.serverTimestamp());
        log.put("loggedBy", "PT Admin");
        db.collection("honorEvents").add(log);
    }
}
