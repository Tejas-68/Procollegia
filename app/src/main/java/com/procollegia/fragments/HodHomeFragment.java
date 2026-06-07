package com.procollegia.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.procollegia.R;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.AdapterView;
import androidx.core.content.ContextCompat;
import com.procollegia.TournamentDetailActivity;
import com.procollegia.adapters.TournamentAdapter;
import com.procollegia.utils.NotificationHelper;
import com.procollegia.utils.TimetableLoader;

import java.util.ArrayList;
import java.util.List;

public class HodHomeFragment extends Fragment {

    private TextView tvWelcome;
    private RecyclerView rvHighlights;
    private View root;

    private Spinner spYearAttendance, spSectionAttendance;
    private Spinner spYearInternals, spSectionInternals;
    private CircularProgressIndicator progressAttendanceAnalytics, progressInternalsAnalytics;
    private TextView tvAttendancePercentAnalytics, tvInternalsPercentAnalytics;

    private String hodDepartment;

    private final List<TournamentAdapter.TournamentItem> tournamentList = new ArrayList<>();

    public HodHomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_hod_home, container, false);
        
        tvWelcome       = root.findViewById(R.id.tvWelcome);
        rvHighlights    = root.findViewById(R.id.rvHighlights);

        // Analytics UI
        spYearAttendance = root.findViewById(R.id.spYearAttendance);
        spSectionAttendance = root.findViewById(R.id.spSectionAttendance);
        progressAttendanceAnalytics = root.findViewById(R.id.progressAttendanceAnalytics);
        tvAttendancePercentAnalytics = root.findViewById(R.id.tvAttendancePercentAnalytics);

        spYearInternals = root.findViewById(R.id.spYearInternals);
        spSectionInternals = root.findViewById(R.id.spSectionInternals);
        progressInternalsAnalytics = root.findViewById(R.id.progressInternalsAnalytics);
        tvInternalsPercentAnalytics = root.findViewById(R.id.tvInternalsPercentAnalytics);

        rvHighlights.setLayoutManager(new LinearLayoutManager(getContext()));
        
        View btnAssign = root.findViewById(R.id.btnAssignSubjects);
        if (btnAssign != null) {
            btnAssign.setOnClickListener(v -> startActivity(new Intent(getContext(), com.procollegia.HodSubjectAssignmentActivity.class)));
        }
        
        View btnUploadTimetable = root.findViewById(R.id.btnUploadTimetable);
        if (btnUploadTimetable != null) {
            btnUploadTimetable.setOnClickListener(v -> startActivity(new Intent(getContext(), com.procollegia.TimetableUploadActivity.class)));
        }
        
        View btnInternals = root.findViewById(R.id.btnManageInternals);
        if (btnInternals != null) {
            btnInternals.setOnClickListener(v -> startActivity(new Intent(getContext(), com.procollegia.TeacherInternalsActivity.class)));
        }

        setupDashboard();
        return root;
    }

    private void setupDashboard() {
        loadHodProfile();
        loadTournaments();
    }

    private void loadHodProfile() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener(d -> {
                if (!isAdded() || !d.exists()) return;
                String name = d.getString("name");
                if (name != null) tvWelcome.setText("Welcome back, Prof. " + name.split(" ")[0]);

                // Load timetable image for HOD's department; notify if missing today
                String dept = d.getString("department");
                if (dept == null) dept = d.getString("hodDepartment");
                hodDepartment = dept;
                
                setupSpinnersAndAnalytics();

                View timetableWidget = root.findViewById(R.id.includeTimetable);
                if (dept != null && timetableWidget != null) {
                    final String finalDept = dept;
                    TimetableLoader.load(timetableWidget, dept, this,
                            () -> NotificationHelper.notifyUploadReminder(getContext(), finalDept));
                }
            });
    }

    private void setupSpinnersAndAnalytics() {
        if (getContext() == null || hodDepartment == null) return;
        String[] years = {"All Years", "1st Year", "2nd Year", "3rd Year"};
        String[] sections = {"All Sections", "A", "B", "C"};

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, years);
        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, sections);

        spYearAttendance.setAdapter(yearAdapter);
        spSectionAttendance.setAdapter(sectionAdapter);
        spYearInternals.setAdapter(yearAdapter);
        spSectionInternals.setAdapter(sectionAdapter);

        AdapterView.OnItemSelectedListener attListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { fetchAttendanceAnalytics(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        spYearAttendance.setOnItemSelectedListener(attListener);
        spSectionAttendance.setOnItemSelectedListener(attListener);

        AdapterView.OnItemSelectedListener intListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { fetchInternalsAnalytics(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        spYearInternals.setOnItemSelectedListener(intListener);
        spSectionInternals.setOnItemSelectedListener(intListener);
    }

    private void fetchAttendanceAnalytics() {
        if (hodDepartment == null) return;
        String year = spYearAttendance.getSelectedItem().toString();
        String section = spSectionAttendance.getSelectedItem().toString();

        Query q = FirebaseFirestore.getInstance().collection("attendanceRecords").whereEqualTo("department", hodDepartment);
        if (!"All Years".equals(year)) q = q.whereEqualTo("year", year);
        if (!"All Sections".equals(section)) q = q.whereEqualTo("section", section);

        q.get().addOnSuccessListener(qs -> {
            if (!isAdded()) return;
            int total = qs.size();
            int present = 0;
            for (QueryDocumentSnapshot d : qs) {
                if ("P".equals(d.getString("status"))) present++;
            }
            int pct = total > 0 ? (present * 100 / total) : 0;
            tvAttendancePercentAnalytics.setText(pct + "%");
            progressAttendanceAnalytics.setProgress(pct);
            
            int color;
            if (pct >= 75) color = ContextCompat.getColor(requireContext(), R.color.accent_green);
            else if (pct >= 60) color = ContextCompat.getColor(requireContext(), R.color.accent_orange);
            else color = ContextCompat.getColor(requireContext(), R.color.accent_red);
            progressAttendanceAnalytics.setIndicatorColor(color);
        });
    }

    private void fetchInternalsAnalytics() {
        if (hodDepartment == null) return;
        String year = spYearInternals.getSelectedItem().toString();
        String section = spSectionInternals.getSelectedItem().toString();

        Query q = FirebaseFirestore.getInstance().collection("internals").whereEqualTo("department", hodDepartment);
        if (!"All Years".equals(year)) q = q.whereEqualTo("year", year);
        if (!"All Sections".equals(section)) q = q.whereEqualTo("section", section);

        q.get().addOnSuccessListener(qs -> {
            if (!isAdded()) return;
            int totalMarks = 0;
            int totalMax = 0;
            for (QueryDocumentSnapshot d : qs) {
                Long m = d.getLong("marks");
                Long mMax = d.getLong("maxMarks");
                if (m != null && mMax != null && mMax > 0) {
                    totalMarks += m.intValue();
                    totalMax += mMax.intValue();
                }
            }
            int pct = totalMax > 0 ? (totalMarks * 100 / totalMax) : 0;
            tvInternalsPercentAnalytics.setText(pct + "%");
            progressInternalsAnalytics.setProgress(pct);
        });
    }

    private void loadTournaments() {
        FirebaseFirestore.getInstance().collection("tournaments")
            .whereEqualTo("status", "Ongoing")
            .limit(2)
            .get()
            .addOnSuccessListener(qs -> {
                if (!isAdded()) return;
                tournamentList.clear();
                qs.forEach(d -> {
                    tournamentList.add(new TournamentAdapter.TournamentItem(
                        d.getId(), d.getString("name"), d.getString("type"),
                        d.getString("startDate"), d.getString("venue"),
                        d.getString("status")));
                });
                
                // If no ongoing, add mock
                if (tournamentList.isEmpty()) {
                    tournamentList.add(new TournamentAdapter.TournamentItem("t1", "Inter-Dept Cricket", "Cricket", "Mar 12 - 20", "Main Ground", "Ongoing"));
                }

                rvHighlights.setAdapter(new TournamentAdapter(tournamentList, it -> {
                    Intent i = new Intent(getActivity(), TournamentDetailActivity.class);
                    i.putExtra("tournamentId", it.id);
                    startActivity(i);
                }));
            });
    }
}
