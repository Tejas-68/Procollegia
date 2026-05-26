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

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.procollegia.R;
import com.procollegia.TournamentDetailActivity;
import com.procollegia.adapters.TournamentAdapter;
import com.procollegia.utils.NotificationHelper;
import com.procollegia.utils.TimetableLoader;

import java.util.ArrayList;
import java.util.List;

public class HodHomeFragment extends Fragment {

    private TextView tvWelcome;
    private BarChart attendanceChart;
    private RecyclerView rvHighlights;
    private View root;

    private final List<TournamentAdapter.TournamentItem> tournamentList = new ArrayList<>();

    public HodHomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_hod_home, container, false);
        
        tvWelcome       = root.findViewById(R.id.tvWelcome);
        attendanceChart = root.findViewById(R.id.attendanceChart);
        rvHighlights    = root.findViewById(R.id.rvHighlights);

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
        setupAttendanceChart();
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
                View timetableWidget = root.findViewById(R.id.includeTimetable);
                if (dept != null && timetableWidget != null) {
                    final String finalDept = dept;
                    TimetableLoader.load(timetableWidget, dept, this,
                            () -> NotificationHelper.notifyUploadReminder(getContext(), finalDept));
                }
            });
    }

    private void setupAttendanceChart() {
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 85f));
        entries.add(new BarEntry(1, 88f));
        entries.add(new BarEntry(2, 82f));
        entries.add(new BarEntry(3, 91f));
        entries.add(new BarEntry(4, 89f));

        BarDataSet dataSet = new BarDataSet(entries, "Attendance %");
        dataSet.setColor(getResources().getColor(R.color.accent_blue));
        dataSet.setValueTextColor(getResources().getColor(R.color.text_primary));

        BarData barData = new BarData(dataSet);
        attendanceChart.setData(barData);

        String[] months = {"Jan", "Feb", "Mar", "Apr", "May"};
        attendanceChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(months));
        attendanceChart.getXAxis().setGranularity(1f);
        attendanceChart.getXAxis().setCenterAxisLabels(false);
        attendanceChart.getDescription().setEnabled(false);
        attendanceChart.animateY(1000);
        attendanceChart.invalidate();
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
