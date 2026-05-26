package com.procollegia.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.procollegia.CreateTournamentActivity;
import com.procollegia.R;
import com.procollegia.TournamentDetailActivity;
import com.procollegia.adapters.TournamentAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * P4: Teacher tournament list tab.
 * Now reads from real Firestore 'tournaments' collection instead of mock data.
 */
public class TeacherTournamentFragment extends Fragment {

    private RecyclerView rvCurrent, rvUpcoming;
    private FirebaseFirestore db;
    private final List<TournamentAdapter.TournamentItem> currentList  = new ArrayList<>();
    private final List<TournamentAdapter.TournamentItem> upcomingList = new ArrayList<>();

    public TeacherTournamentFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_teacher_tournament, container, false);

        rvCurrent  = root.findViewById(R.id.rvCurrentTournaments);
        rvUpcoming = root.findViewById(R.id.rvUpcomingTournaments);

        rvCurrent.setLayoutManager(new LinearLayoutManager(getContext()));
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
            currentList.clear();
            upcomingList.clear();

            for (QueryDocumentSnapshot d : qs) {
                String status = d.getString("status");
                TournamentAdapter.TournamentItem item = new TournamentAdapter.TournamentItem(
                        d.getId(), d.getString("name"), d.getString("type"),
                        d.getString("startDate"), d.getString("venue"), status);

                if ("Ongoing".equalsIgnoreCase(status) || "Live".equalsIgnoreCase(status)) {
                    currentList.add(item);
                } else {
                    upcomingList.add(item);
                }
            }

            TournamentAdapter.OnTourneyAction detailListener = item -> {
                Intent i = new Intent(getActivity(), TournamentDetailActivity.class);
                i.putExtra("tournamentId", item.id);
                startActivity(i);
            };

            rvCurrent.setAdapter(new TournamentAdapter(currentList, detailListener));
            rvUpcoming.setAdapter(new TournamentAdapter(upcomingList, detailListener));
        });
    }
}
