package com.procollegia.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.procollegia.R;
import com.procollegia.adapters.ReturnRequestAdapter;
import com.procollegia.adapters.TournamentAdapter;

import java.util.ArrayList;
import java.util.List;

public class PtHomeFragment extends Fragment {

    private TextView tvGreeting, tvBorrowedCount, tvPendingReturns;
    private RecyclerView rvTournaments, rvReturnRequests;
    private FirebaseFirestore db;
    private String uid;

    private final List<ReturnRequestAdapter.ReturnRequest> returnList = new ArrayList<>();
    private final List<TournamentAdapter.TournamentItem> tournamentList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pt_home, container, false);

        tvGreeting      = root.findViewById(R.id.tvGreeting);
        tvBorrowedCount = root.findViewById(R.id.tvBorrowedCount);
        tvPendingReturns = root.findViewById(R.id.tvPendingReturns);
        rvTournaments    = root.findViewById(R.id.rvOngoingTournaments);
        rvReturnRequests = root.findViewById(R.id.rvReturnRequests);

        rvTournaments.setLayoutManager(new LinearLayoutManager(getContext()));
        rvReturnRequests.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (uid != null) {
            setupDashboard();
        }

        return root;
    }

    private void setupDashboard() {
        fetchAdminDetails();
        loadBorrowedCount();
        loadOngoingTournaments();
        loadReturnRequests();
    }

    private void fetchAdminDetails() {
        db.collection("users").document(uid).get().addOnSuccessListener(d -> {
            if (isAdded() && d.exists()) {
                String name = d.getString("name");
                if (name != null) tvGreeting.setText("Welcome Prof. " + name.split(" ")[0]);
            }
        });
    }

    private void loadBorrowedCount() {
        // Fetch count from borrowRequests where status is 'active' or 'return_requested'
        db.collection("borrowRequests").whereIn("status", java.util.Arrays.asList("active", "return_requested")).get().addOnSuccessListener(qs -> {
            if (isAdded()) tvBorrowedCount.setText(String.valueOf(qs.size()));
        });
    }

    private void loadOngoingTournaments() {
        db.collection("tournaments").whereIn("status", java.util.Arrays.asList("Ongoing", "ongoing", "upcoming", "Upcoming")).limit(3).get().addOnSuccessListener(qs -> {
            if (!isAdded()) return;
            tournamentList.clear();
            for (QueryDocumentSnapshot d : qs) {
                tournamentList.add(new TournamentAdapter.TournamentItem(
                        d.getId(), d.getString("name"), d.getString("gameType") != null ? d.getString("gameType") : d.getString("type"),
                        d.getString("startDate"), d.getString("venue"),
                        d.getString("status")));
            }
            rvTournaments.setAdapter(new TournamentAdapter(tournamentList, it -> {
                Intent i = new Intent(getActivity(), com.procollegia.TournamentDetailActivity.class);
                i.putExtra("tournamentId", it.id);
                startActivity(i);
            }));
        }).addOnFailureListener(e -> {
            if (isAdded()) Toast.makeText(getContext(), "Failed to load tournaments", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadReturnRequests() {
        db.collection("borrowRequests").whereEqualTo("status", "return_requested").get().addOnSuccessListener(qs -> {
            if (!isAdded()) return;
            returnList.clear();
            tvPendingReturns.setText(String.valueOf(qs.size()));

            for (QueryDocumentSnapshot d : qs) {
                returnList.add(new ReturnRequestAdapter.ReturnRequest(
                        d.getId(),
                        d.getString("studentName"),
                        d.getString("equipmentName"),
                        d.getString("equipmentId"),   // needed for stock restore
                        d.getString("borrowDate")));
            }

            rvReturnRequests.setAdapter(new ReturnRequestAdapter(returnList, req -> {
                approveReturn(req);
            }));
        });
    }

    private void approveReturn(ReturnRequestAdapter.ReturnRequest req) {
        db.collection("borrowRequests").document(req.id).update("status", "returned")
            .addOnSuccessListener(aVoid -> {
                if (req.equipmentId != null && !req.equipmentId.isEmpty()) {
                    db.collection("inventory").document(req.equipmentId)
                            .update("remaining", FieldValue.increment(1));
                }

                Toast.makeText(getContext(), "Return Approved — stock restored", Toast.LENGTH_SHORT).show();
                loadReturnRequests(); // refresh list
                loadBorrowedCount();  // refresh count
            })
            .addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Failed to approve: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
