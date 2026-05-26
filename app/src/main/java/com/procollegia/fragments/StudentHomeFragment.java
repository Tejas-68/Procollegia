package com.procollegia.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.procollegia.R;
import com.procollegia.utils.TimetableLoader;

public class StudentHomeFragment extends Fragment {

    private View root;

    public StudentHomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_student_home, container, false);

        TextView tvGreeting      = root.findViewById(R.id.tvGreeting);
        ImageView ivProfile      = root.findViewById(R.id.ivProfile);
        ImageView ivNotifications = root.findViewById(R.id.ivNotifications);

        checkAlertsAndTournaments();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (!isAdded()) return;

                        if (doc.exists()) {
                            // Greeting
                            String name = doc.getString("name");
                            if (name != null) {
                                tvGreeting.setText("Welcome back, " + name.split(" ")[0]);
                            }

                            // Timetable image — loaded by department (e.g. "BCA")
                            String dept = doc.getString("department");
                            View timetableWidget = root.findViewById(R.id.includeTimetable);
                            if (dept != null && timetableWidget != null) {
                                TimetableLoader.load(timetableWidget, dept, this);
                            }

                            // Attendance card
                            FirebaseFirestore.getInstance()
                                    .collection("attendanceRecords")
                                    .whereEqualTo("studentId", currentUser.getUid())
                                    .get()
                                    .addOnSuccessListener(qs -> {
                                        int present = 0;
                                        int total = qs.size();
                                        for (QueryDocumentSnapshot d : qs) {
                                            if ("P".equals(d.getString("status"))) present++;
                                        }
                                        setupAttendanceCard(total, present);
                                    });

                            // Honor Score
                            Long honorScoreObj = doc.getLong("honorScore");
                            int honorScore = (honorScoreObj != null) ? honorScoreObj.intValue() : 500;
                            honorScore = Math.max(50, Math.min(1000, honorScore));
                            TextView tvHonorScoreValue = root.findViewById(R.id.tvHonorScoreValue);
                            if (tvHonorScoreValue != null) tvHonorScoreValue.setText(honorScore + " PTS");
                        }
                    });
        }

        ivProfile.setOnClickListener(v ->
                Toast.makeText(getContext(), "Profile feature coming soon!", Toast.LENGTH_SHORT).show());
        ivNotifications.setOnClickListener(v ->
                Toast.makeText(getContext(), "No new notifications", Toast.LENGTH_SHORT).show());

        View clAttendanceCard = root.findViewById(R.id.clAttendanceCard);
        if (clAttendanceCard != null) {
            clAttendanceCard.setOnClickListener(v -> {
                if (getActivity() != null) {
                    BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNavigationView);
                    if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_attendance);
                }
            });
        }

        return root;
    }

    private void checkAlertsAndTournaments() {
        boolean hasAnnouncements      = false;
        boolean hasCurrentTournament  = false;
        boolean hasUpcomingTournament = false;

        View llAnnouncementCard  = root.findViewById(R.id.llAnnouncementCard);
        TextView tvNoAnnouncements = root.findViewById(R.id.tvNoAnnouncements);

        if (llAnnouncementCard != null) {
            llAnnouncementCard.setVisibility(hasAnnouncements ? View.VISIBLE : View.GONE);
        }
        if (tvNoAnnouncements != null) {
            tvNoAnnouncements.setVisibility(hasAnnouncements ? View.GONE : View.VISIBLE);
        }

        View llCurrentTournament  = root.findViewById(R.id.llCurrentTournament);
        View llUpcomingTournament = root.findViewById(R.id.llUpcomingTournament);
        TextView tvNoTournaments  = root.findViewById(R.id.tvNoTournaments);

        if (llCurrentTournament != null)
            llCurrentTournament.setVisibility(hasCurrentTournament ? View.VISIBLE : View.GONE);
        if (llUpcomingTournament != null)
            llUpcomingTournament.setVisibility(hasUpcomingTournament ? View.VISIBLE : View.GONE);
        if (tvNoTournaments != null)
            tvNoTournaments.setVisibility((!hasCurrentTournament && !hasUpcomingTournament)
                    ? View.VISIBLE : View.GONE);
    }

    private void setupAttendanceCard(int totalClasses, int attendedClasses) {
        CircularProgressIndicator progressAttendance = root.findViewById(R.id.progressAttendance);
        TextView tvAttendancePercent = root.findViewById(R.id.tvAttendancePercent);
        if (progressAttendance == null || tvAttendancePercent == null) return;

        int pct = totalClasses > 0 ? (int) (((double) attendedClasses / totalClasses) * 100) : 0;
        progressAttendance.setProgress(pct);
        tvAttendancePercent.setText(pct + "%");

        int color;
        if (pct >= 75)      color = ContextCompat.getColor(requireContext(), R.color.accent_green);
        else if (pct >= 60) color = ContextCompat.getColor(requireContext(), R.color.accent_orange);
        else                color = ContextCompat.getColor(requireContext(), R.color.accent_red);
        progressAttendance.setIndicatorColor(color);
    }
}
