package com.procollegia.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.procollegia.R;
import com.procollegia.adapters.BorrowedAdapter;
import com.procollegia.adapters.EquipmentAdapter;
import com.procollegia.adapters.TournamentAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentSportsFragment extends Fragment {

    // ── Views ────────────────────────────────────────────────────────────────
    private TextView[]  tabs;
    private FrameLayout flContent;

    // Cached inflated sub-views (so we don't re-fetch on tab-switch)
    private View vTournaments, vBorrow, vMyItems;

    // ── Data ─────────────────────────────────────────────────────────────────
    private final List<TournamentAdapter.TournamentItem> tournaments = new ArrayList<>();
    private final List<EquipmentAdapter.Equipment>    equipment    = new ArrayList<>();
    private final List<BorrowedAdapter.BorrowedItem>  borrowed     = new ArrayList<>();

    private EquipmentAdapter equipmentAdapter;
    private BorrowedAdapter  borrowedAdapter;

    private boolean tournamentsFetched = false;
    private boolean equipmentFetched   = false;
    private boolean borrowedFetched    = false;

    // ── Auth ─────────────────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private String uid, studentName;

    public StudentSportsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_student_sports, container, false);

        flContent = root.findViewById(R.id.flSportsContent);
        tabs = new TextView[]{
                root.findViewById(R.id.tabTournaments),
                root.findViewById(R.id.tabBorrow),
                root.findViewById(R.id.tabMyItems)
        };

        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            tabs[i].setOnClickListener(v -> switchTab(idx));
        }

        db  = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (uid != null) fetchStudentName();

        // Start on Tournaments tab
        switchTab(0);
        return root;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Tab switching
    // ────────────────────────────────────────────────────────────────────────

    private void switchTab(int idx) {
        // Update pill styles
        for (int i = 0; i < tabs.length; i++) {
            if (i == idx) {
                tabs[i].setBackgroundResource(R.drawable.bg_pill_active);
                tabs[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_accent));
            } else {
                tabs[i].setBackgroundResource(R.drawable.bg_pill_inactive);
                tabs[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            }
        }

        flContent.removeAllViews();
        switch (idx) {
            case 0: showTournamentsTab(); break;
            case 1: showBorrowTab();      break;
            case 2: showMyItemsTab();     break;
        }
    }

    // ── Tournaments tab ──────────────────────────────────────────────────────

    private void showTournamentsTab() {
        if (vTournaments == null) {
            vTournaments = LayoutInflater.from(getContext())
                    .inflate(R.layout.content_sports_tournaments, flContent, false);
        }
        flContent.addView(vTournaments);

        if (!tournamentsFetched) {
            tournamentsFetched = true;
            ShimmerFrameLayout shimmer = vTournaments.findViewById(R.id.shimmerTournaments);
            shimmer.startShimmer();
            loadMockTournaments(); // pre-load mock; replace with Firestore below
            fetchTournaments(shimmer);
        }
    }

    private void fetchTournaments(ShimmerFrameLayout shimmer) {
        if (uid == null) { renderTournaments(shimmer); return; }

        db.collection("tournaments").get()
                .addOnSuccessListener(qs -> {
                    if (!qs.isEmpty()) {
                        tournaments.clear();
                        for (QueryDocumentSnapshot doc : qs) {
                            TournamentAdapter.TournamentItem item = new TournamentAdapter.TournamentItem(
                                    doc.getId(),
                                    nvl(doc.getString("name"), "Tournament"),
                                    nvl(doc.getString("gameType"), "Sport"),
                                    nvl(doc.getString("venue"), "Campus"),
                                    nvl(doc.getString("startDate"),  "TBD"),
                                    doc.contains("joinedCount") && doc.contains("maxTeams") ? doc.getLong("joinedCount") + "/" + doc.getLong("maxTeams") : "—",
                                    "Trophy",
                                    "ongoing".equalsIgnoreCase(doc.getString("status")),
                                    "🏆");
                            
                            item.gameType = nvl(doc.getString("gameType"), "Solo");
                            item.maxPlayers = doc.contains("maxPlayers") ? doc.getLong("maxPlayers").intValue() : 1;
                            tournaments.add(item);
                        }
                    }
                    if (getActivity() != null)
                        getActivity().runOnUiThread(() -> renderTournaments(shimmer));
                })
                .addOnFailureListener(e -> {
                    if (getActivity() != null)
                        getActivity().runOnUiThread(() -> renderTournaments(shimmer));
                });
    }

    private void renderTournaments(ShimmerFrameLayout shimmer) {
        shimmer.stopShimmer();
        shimmer.setVisibility(View.GONE);
        RecyclerView rv   = vTournaments.findViewById(R.id.rvTournaments);
        View         empty = vTournaments.findViewById(R.id.llNoTournaments);
        if (tournaments.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
        } else {
            rv.setVisibility(View.VISIBLE);
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            rv.setNestedScrollingEnabled(false);
            
            TournamentAdapter adapter = new TournamentAdapter(tournaments);
            adapter.setOnJoinListener(it -> {
                android.content.Intent intent = new android.content.Intent(getActivity(), com.procollegia.TournamentRegistrationActivity.class);
                intent.putExtra("id", it.id);
                intent.putExtra("name", it.name);
                intent.putExtra("gameType", it.gameType);
                intent.putExtra("maxPlayers", it.maxPlayers);
                startActivity(intent);
            });
            rv.setAdapter(adapter);
        }
    }

    // ── Borrow tab ───────────────────────────────────────────────────────────

    private void showBorrowTab() {
        if (vBorrow == null) {
            vBorrow = LayoutInflater.from(getContext())
                    .inflate(R.layout.content_sports_borrow, flContent, false);
        }
        flContent.addView(vBorrow);

        if (!equipmentFetched) {
            equipmentFetched = true;
            ShimmerFrameLayout shimmer = vBorrow.findViewById(R.id.shimmerEquipment);
            shimmer.startShimmer();
            loadMockEquipment();
            fetchEquipment(shimmer);
        }

        // Wire search bar (always re-wire in case view was re-added)
        EditText etSearch = vBorrow.findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (equipmentAdapter != null) equipmentAdapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchEquipment(ShimmerFrameLayout shimmer) {
        if (uid == null) { renderEquipment(shimmer); return; }

        // BUG 1 FIX: read from 'inventory' — the same collection PT Admin writes to
        db.collection("inventory").get()
                .addOnSuccessListener(qs -> {
                    if (!qs.isEmpty()) {
                        equipment.clear();
                        for (QueryDocumentSnapshot doc : qs) {
                            // PT Admin stores stock count in 'remaining', not 'available'
                            int avail = 0;
                            Object raw = doc.get("remaining");
                            if (raw instanceof Long) avail = ((Long) raw).intValue();
                            equipment.add(new EquipmentAdapter.Equipment(
                                    doc.getId(),
                                    nvl(doc.getString("name"), "Equipment"),
                                    nvl(doc.getString("category"), "Sports"),
                                    avail,
                                    nvl(doc.getString("emoji"), "🏅")));
                        }
                    }
                    if (getActivity() != null)
                        getActivity().runOnUiThread(() -> renderEquipment(shimmer));
                })
                .addOnFailureListener(e -> {
                    if (getActivity() != null)
                        getActivity().runOnUiThread(() -> renderEquipment(shimmer));
                });
    }

    private void renderEquipment(ShimmerFrameLayout shimmer) {
        shimmer.stopShimmer();
        shimmer.setVisibility(View.GONE);
        RecyclerView rv = vBorrow.findViewById(R.id.rvEquipment);
        rv.setVisibility(View.VISIBLE);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setNestedScrollingEnabled(false);
        equipmentAdapter = new EquipmentAdapter(equipment, this::onBorrowClicked);
        rv.setAdapter(equipmentAdapter);
    }

    // ── My Items tab ─────────────────────────────────────────────────────────

    private void showMyItemsTab() {
        if (vMyItems == null) {
            vMyItems = LayoutInflater.from(getContext())
                    .inflate(R.layout.content_sports_myitems, flContent, false);
        }
        flContent.addView(vMyItems);

        if (!borrowedFetched) {
            borrowedFetched = true;
            fetchMyBorrows();
        } else {
            renderBorrowed();
        }
    }

    private void fetchMyBorrows() {
        if (uid == null) { renderBorrowed(); return; }

        db.collection("borrowRequests")
                .whereEqualTo("studentId", uid)
                .whereNotEqualTo("status", "returned")
                .get()
                .addOnSuccessListener(qs -> {
                    for (QueryDocumentSnapshot doc : qs) {
                        borrowed.add(new BorrowedAdapter.BorrowedItem(
                                doc.getId(),
                                nvl(doc.getString("equipmentName"),  "Equipment"),
                                nvl(doc.getString("equipmentEmoji"), "🏅"),
                                nvl(doc.getString("borrowDate"),     "—"),
                                nvl(doc.getString("status"),         "active")));
                    }
                    if (getActivity() != null)
                        getActivity().runOnUiThread(this::renderBorrowed);
                })
                .addOnFailureListener(e -> {
                    if (getActivity() != null)
                        getActivity().runOnUiThread(this::renderBorrowed);
                });
    }

    private void renderBorrowed() {
        if (vMyItems == null) return;
        RecyclerView rv    = vMyItems.findViewById(R.id.rvBorrowed);
        View         empty = vMyItems.findViewById(R.id.llNoBorrows);
        if (borrowed.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
        } else {
            rv.setVisibility(View.VISIBLE);
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            rv.setNestedScrollingEnabled(false);
            borrowedAdapter = new BorrowedAdapter(borrowed, this::onReturnClicked);
            rv.setAdapter(borrowedAdapter);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Borrow flow
    // ────────────────────────────────────────────────────────────────────────

    private void onBorrowClicked(EquipmentAdapter.Equipment item, int qty) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Borrow " + qty + "x " + item.name + "?")
                .setMessage("A PT Admin will track this borrow request.")
                .setPositiveButton("Confirm", (d, w) -> submitBorrow(item, qty))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitBorrow(EquipmentAdapter.Equipment item, int qty) {
        String today = new SimpleDateFormat("MMM dd", Locale.getDefault()).format(new Date());
        Map<String, Object> req = new HashMap<>();
        req.put("studentId",      uid);
        req.put("studentName",    studentName != null ? studentName : "Student");
        req.put("equipmentId",    item.id);
        req.put("equipmentName",  item.name);
        req.put("equipmentEmoji", item.emoji);
        req.put("quantity",       qty);
        req.put("borrowDate",     today);
        req.put("status",         "active");
        req.put("createdAt",      FieldValue.serverTimestamp());

        db.collection("borrowRequests").add(req)
                .addOnSuccessListener(ref -> {
                    // BUG 1 FIX: decrement 'remaining' in 'inventory', not 'available' in 'equipment'
                    db.collection("inventory").document(item.id)
                            .update("remaining", FieldValue.increment(-qty));

                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(),
                                qty + "x " + item.name + " borrowed!", Toast.LENGTH_SHORT).show();
                        // Add to My Items list
                        BorrowedAdapter.BorrowedItem bi = new BorrowedAdapter.BorrowedItem(
                                ref.getId(), item.name + (qty > 1 ? " ×" + qty : ""),
                                item.emoji, today, "active");
                        borrowed.add(bi);
                        if (borrowedAdapter != null) {
                            borrowedAdapter.notifyItemInserted(borrowed.size() - 1);
                        }
                        // Switch to My Items to show it
                        switchTab(2);
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed. Try again.", Toast.LENGTH_SHORT).show());
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Return flow
    // ────────────────────────────────────────────────────────────────────────

    private void onReturnClicked(BorrowedAdapter.BorrowedItem item, int pos) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Request Return?")
                .setMessage("Send return request for " + item.name +
                        "?\nA PT Admin will verify and approve.")
                .setPositiveButton("Send Request", (d, w) -> submitReturn(item, pos))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitReturn(BorrowedAdapter.BorrowedItem item, int pos) {
        db.collection("borrowRequests").document(item.docId)
                .update("status", "return_requested",
                        "returnRequestedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(v -> {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(),
                                "Return request sent! Awaiting PT approval.", Toast.LENGTH_LONG).show();
                        if (borrowedAdapter != null) borrowedAdapter.updateStatus(pos, "return_requested");
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to send.", Toast.LENGTH_SHORT).show());
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Mock data
    // ────────────────────────────────────────────────────────────────────────

    private void loadMockTournaments() {
        if (!tournaments.isEmpty()) return;
        tournaments.add(new TournamentAdapter.TournamentItem("S1", "Inter-College Cricket",  "Cricket",    "College Ground", "Mar 20","8","Trophy",  true,  "🏏"));
        tournaments.add(new TournamentAdapter.TournamentItem("S2", "Basketball Championship","Basketball", "Sports Hall",    "Apr 05","6","Medal",   false, "🏀"));
        tournaments.add(new TournamentAdapter.TournamentItem("S3", "Badminton Open",         "Badminton",  "Indoor Court",   "Apr 15","12","Certificate",false,"🏸"));
    }

    private void loadMockEquipment() {
        if (!equipment.isEmpty()) return;
        equipment.addAll(Arrays.asList(
                new EquipmentAdapter.Equipment("1","Basketball",       "Ball Sports",   5, "🏀"),
                new EquipmentAdapter.Equipment("2","Cricket Bat",      "Cricket",       3, "🏏"),
                new EquipmentAdapter.Equipment("3","Badminton Racket", "Racket Sports", 8, "🏸"),
                new EquipmentAdapter.Equipment("4","Football",         "Ball Sports",   4, "⚽"),
                new EquipmentAdapter.Equipment("5","Table Tennis Bat", "TT",            0, "🏓"),
                new EquipmentAdapter.Equipment("6","Volleyball",       "Ball Sports",   2, "🏐"),
                new EquipmentAdapter.Equipment("7","Skipping Rope",    "Fitness",       10,"🪢"),
                new EquipmentAdapter.Equipment("8","Boxing Gloves",    "Combat",        1, "🥊")
        ));
    }

    private void fetchStudentName() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> studentName = nvl(doc.getString("name"), "Student"));
    }

    private String nvl(String s, String def) { return s != null ? s : def; }
}
