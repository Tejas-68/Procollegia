package com.procollegia.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.procollegia.CreateTournamentActivity;
import com.procollegia.R;
import com.procollegia.adapters.TournamentAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * PT Admin tournament management.
 * P4: Added long-press → delete tournament with confirmation dialog.
 */
public class PtTournamentFragment extends Fragment {

    private RecyclerView rvOngoing, rvUpcoming;
    private FirebaseFirestore db;
    private final List<TournamentAdapter.TournamentItem> ongoingList  = new ArrayList<>();
    private final List<TournamentAdapter.TournamentItem> upcomingList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pt_tournament, container, false);

        rvOngoing  = root.findViewById(R.id.rvOngoing);
        rvUpcoming = root.findViewById(R.id.rvUpcoming);

        rvOngoing.setLayoutManager(new LinearLayoutManager(getContext()));
        rvUpcoming.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();

        root.findViewById(R.id.fabAddTournament).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), CreateTournamentActivity.class)));

        loadTournaments();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTournaments();
    }

    private void loadTournaments() {
        db.collection("tournaments").get().addOnSuccessListener(qs -> {
            if (!isAdded()) return;
            ongoingList.clear();
            upcomingList.clear();

            for (QueryDocumentSnapshot d : qs) {
                String status = d.getString("status");
                TournamentAdapter.TournamentItem it = new TournamentAdapter.TournamentItem(
                        d.getId(), d.getString("name"), d.getString("gameType"),
                        d.getString("startDate"), d.getString("venue"), status);

                if ("ongoing".equalsIgnoreCase(status) || "live".equalsIgnoreCase(status)) {
                    ongoingList.add(it);
                } else {
                    upcomingList.add(it);
                }
            }

            // Click → detail; Long-press → delete confirmation
            TournamentAdapter.OnTourneyAction detailAction = item -> {
                Intent i = new Intent(getActivity(), com.procollegia.TournamentDetailActivity.class);
                i.putExtra("tournamentId", item.id);
                startActivity(i);
            };

            TournamentAdapter ongoingAdapter  = new TournamentAdapter(ongoingList, detailAction);
            TournamentAdapter upcomingAdapter = new TournamentAdapter(upcomingList, detailAction);

            // P4: long-press delete wired via item's setOnLongClickListener in adapter
            ongoingAdapter.setOnLongClickListener(item -> showOptions(item, ongoingList, ongoingAdapter));
            upcomingAdapter.setOnLongClickListener(item -> showOptions(item, upcomingList, upcomingAdapter));

            rvOngoing.setAdapter(ongoingAdapter);
            rvUpcoming.setAdapter(upcomingAdapter);
        });
    }

    private void showOptions(TournamentAdapter.TournamentItem item,
                             List<TournamentAdapter.TournamentItem> list,
                             TournamentAdapter adapter) {
        String[] options = "upcoming".equalsIgnoreCase(item.status) 
                           ? new String[]{"Start Tournament", "Delete Tournament"}
                           : new String[]{"Delete Tournament"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Tournament Options")
                .setItems(options, (dialog, which) -> {
                    String selected = options[which];
                    if (selected.equals("Start Tournament")) {
                        db.collection("tournaments").document(item.id).update("status", "ongoing")
                                .addOnSuccessListener(v -> {
                                    Toast.makeText(getContext(), "Tournament Started!", Toast.LENGTH_SHORT).show();
                                    loadTournaments();
                                });
                    } else if (selected.equals("Delete Tournament")) {
                        confirmDelete(item, list, adapter);
                    }
                })
                .show();
    }

    private void confirmDelete(TournamentAdapter.TournamentItem item,
                               List<TournamentAdapter.TournamentItem> list,
                               TournamentAdapter adapter) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Tournament")
                .setMessage("Delete \"" + item.name + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    db.collection("tournaments").document(item.id).delete()
                            .addOnSuccessListener(v -> {
                                list.remove(item);
                                adapter.notifyDataSetChanged();
                                Toast.makeText(getContext(), "Tournament deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
