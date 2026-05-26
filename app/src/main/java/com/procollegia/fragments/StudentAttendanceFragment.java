package com.procollegia.fragments;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.procollegia.R;
import com.procollegia.adapters.CalendarAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Student attendance fragment.
 * Reads real data from Firestore 'attendanceRecords' collection.
 * Calendar shows P (green), A (red), holiday/Sunday (red), future (grey).
 * Subject breakdown aggregates by period label.
 */
public class StudentAttendanceFragment extends Fragment {

    private Calendar currentDisplayedMonth;
    private View root;
    private FirebaseFirestore db;
    private String uid;

    // Keyed by "yyyy-MM-dd"
    private final Set<String> presentDates = new HashSet<>();
    private final Set<String> absentDates  = new HashSet<>();

    // period label → {present, total}
    private final Map<String, int[]> periodStats = new HashMap<>();

    public StudentAttendanceFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_student_attendance, container, false);

        currentDisplayedMonth = Calendar.getInstance();
        db  = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        root.findViewById(R.id.ivPrevMonth).setOnClickListener(v -> {
            currentDisplayedMonth.add(Calendar.MONTH, -1);
            setupCalendar();
        });
        root.findViewById(R.id.ivNextMonth).setOnClickListener(v -> {
            currentDisplayedMonth.add(Calendar.MONTH, 1);
            setupCalendar();
        });

        Button btnRequestLeave = root.findViewById(R.id.btnRequestLeave);
        btnRequestLeave.setOnClickListener(v -> {
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), com.procollegia.LeaveRequestActivity.class));
            }
        });

        if (uid != null) {
            fetchAttendanceRecords();
        } else {
            setupCalendar();
            setupSubjects(inflater);
        }

        return root;
    }

    // ── Firestore fetch ───────────────────────────────────────────────────────

    private void fetchAttendanceRecords() {
        db.collection("attendanceRecords")
                .whereEqualTo("studentId", uid)
                .get()
                .addOnSuccessListener(qs -> {
                    presentDates.clear();
                    absentDates.clear();
                    periodStats.clear();

                    for (QueryDocumentSnapshot doc : qs) {
                        String date   = doc.getString("date");   // "yyyy-MM-dd"
                        String status = doc.getString("status"); // "P", "A", "L"
                        String period = doc.getString("period"); // "Period 1" etc.

                        if (date == null || status == null) continue;

                        if ("P".equals(status) || "L".equals(status)) {
                            presentDates.add(date);
                        } else {
                            absentDates.add(date);
                        }

                        // Tally per period
                        if (period != null) {
                            int[] tally = periodStats.getOrDefault(period, new int[]{0, 0});
                            tally[1]++; // total
                            if ("P".equals(status)) tally[0]++; // present
                            periodStats.put(period, tally);
                        }
                    }

                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        setupCalendar();
                        setupSubjects(LayoutInflater.from(getContext()));
                        updateOverallAttendance();
                    });
                })
                .addOnFailureListener(e -> {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        setupCalendar();
                        setupSubjects(LayoutInflater.from(getContext()));
                    });
                });
    }

    private void updateOverallAttendance() {
        if (root == null || !isAdded()) return;

        int totalClasses = 0;
        int totalPresent = 0;

        for (int[] tally : periodStats.values()) {
            totalPresent += tally[0];
            totalClasses += tally[1];
        }

        com.google.android.material.progressindicator.CircularProgressIndicator progressAttendance = root.findViewById(R.id.progressAttendance);
        TextView tvAttendancePercent = root.findViewById(R.id.tvAttendancePercent);
        TextView tvStats = root.findViewById(R.id.tvStats);

        if (progressAttendance == null || tvAttendancePercent == null || tvStats == null) return;

        int pct = totalClasses > 0 ? (int) (((double) totalPresent / totalClasses) * 100) : 0;
        progressAttendance.setProgress(pct);
        tvAttendancePercent.setText(pct + "%");

        int absent = totalClasses - totalPresent;
        tvStats.setText(String.format(Locale.getDefault(), "Present: %d | Absent: %d", totalPresent, absent));

        int color;
        if (pct >= 75)      color = ContextCompat.getColor(requireContext(), R.color.accent_green);
        else if (pct >= 60) color = ContextCompat.getColor(requireContext(), R.color.accent_orange);
        else                color = ContextCompat.getColor(requireContext(), R.color.accent_red);
        progressAttendance.setIndicatorColor(color);
    }

    // ── Calendar ─────────────────────────────────────────────────────────────

    private void setupCalendar() {
        if (root == null || !isAdded()) return;
        TextView tvMonthYear = root.findViewById(R.id.tvMonthYear);
        RecyclerView rv = root.findViewById(R.id.rvCalendar);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 7));

        Calendar cal   = (Calendar) currentDisplayedMonth.clone();
        int year  = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);

        String[] monthNames = {"January","February","March","April","May","June",
                "July","August","September","October","November","December"};
        tvMonthYear.setText(monthNames[month] + " " + year);

        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // Sunday = 1
        int daysInMonth    = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        List<CalendarAdapter.CalendarDay> days = new ArrayList<>();
        for (int i = 0; i < firstDayOfWeek - 1; i++) days.add(new CalendarAdapter.CalendarDay("", 0));

        Calendar today = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int i = 1; i <= daysInMonth; i++) {
            cal.set(Calendar.DAY_OF_MONTH, i);
            String dateKey  = sdf.format(cal.getTime());
            int dayOfWeek   = cal.get(Calendar.DAY_OF_WEEK);
            boolean holiday = isGovtHoliday(i, month) || dayOfWeek == Calendar.SUNDAY;

            int status;
            if (holiday) {
                status = 2; // red — holiday
            } else if (presentDates.contains(dateKey)) {
                status = 1; // green — present
            } else if (absentDates.contains(dateKey)) {
                status = 2; // red — absent
            } else if (cal.after(today)) {
                status = 4; // grey — future
            } else {
                status = 0; // no record yet
            }
            days.add(new CalendarAdapter.CalendarDay(String.valueOf(i), status));
        }

        rv.setAdapter(new CalendarAdapter(days));
    }

    private boolean isGovtHoliday(int day, int month) {
        if (month == 0  && day == 1)  return true; // New Year
        if (month == 0  && day == 26) return true; // Republic Day
        if (month == 4  && day == 1)  return true; // Labour Day
        if (month == 7  && day == 15) return true; // Independence Day
        if (month == 9  && day == 2)  return true; // Gandhi Jayanti
        if (month == 11 && day == 25) return true; // Christmas
        return false;
    }

    // ── Subject breakdown ─────────────────────────────────────────────────────

    private void setupSubjects(LayoutInflater inflater) {
        if (root == null || !isAdded()) return;
        LinearLayout llSubjects = root.findViewById(R.id.llSubjectsList);
        llSubjects.removeAllViews();

        if (periodStats.isEmpty()) {
            // No real data yet: show placeholder rows
            addSubject(inflater, llSubjects, "Period 1", 0, 0, R.color.accent_green);
            addSubject(inflater, llSubjects, "Period 2", 0, 0, R.color.accent_orange);
            return;
        }

        int[] colors = {R.color.accent_green, R.color.accent_blue, R.color.accent_orange,
                        R.color.accent_red, R.color.accent_green};
        int ci = 0;
        for (Map.Entry<String, int[]> e : periodStats.entrySet()) {
            int present = e.getValue()[0];
            int total   = e.getValue()[1];
            int pct     = total > 0 ? (present * 100 / total) : 0;
            addSubject(inflater, llSubjects, e.getKey(), pct, total,
                    colors[ci % colors.length]);
            ci++;
        }
    }

    private void addSubject(LayoutInflater inflater, LinearLayout parent,
                            String name, int percent, int total, int colorRes) {
        View item = inflater.inflate(R.layout.item_subject_attendance, parent, false);

        TextView tvSubjectName = item.findViewById(R.id.tvSubjectName);
        TextView tvPercent     = item.findViewById(R.id.tvPercent);
        TextView tvDanger      = item.findViewById(R.id.tvDanger);
        View vFill             = item.findViewById(R.id.vFill);

        tvSubjectName.setText(total > 0 ? name + " (" + total + " classes)" : name);
        tvPercent.setText(percent + "%");

        if (percent < 75 && total > 0) tvDanger.setVisibility(View.VISIBLE);

        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) vFill.getLayoutParams();
        lp.matchConstraintPercentWidth = Math.max(percent / 100f, 0.02f);
        vFill.setLayoutParams(lp);

        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(8 * getResources().getDisplayMetrics().density);
        if (getContext() != null) gd.setColor(ContextCompat.getColor(getContext(), colorRes));
        vFill.setBackground(gd);

        parent.addView(item);
    }
}
