package com.procollegia.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.procollegia.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HodConfigAttendanceFragment extends Fragment {

    private TextView tvSemStartDate;
    private Spinner spYear1Sec, spYear2Sec, spYear3Sec;
    private TextView tvYear1Pct, tvYear2Pct, tvYear3Pct;
    private ProgressBar pbYear1, pbYear2, pbYear3;

    private FirebaseFirestore db;
    private String hodDepartment = "";
    private String semStartDateStr = "";
    private List<String> holidays = new ArrayList<>();
    
    private List<String> periodsList = new ArrayList<>(Arrays.asList(
        "Period 1 (08:00 – 09:00)",
        "Period 2 (09:00 – 10:00)",
        "Period 3 (10:00 – 11:00)",
        "Period 4 (11:00 – 12:00)",
        "Period 5 (13:00 – 14:00)",
        "Period 6 (14:00 – 15:00)"
    ));

    private List<QueryDocumentSnapshot> allAttendanceRecords = new ArrayList<>();

    public HodConfigAttendanceFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_hod_config_attendance, container, false);

        tvSemStartDate = root.findViewById(R.id.tvSemStartDate);
        
        spYear1Sec = root.findViewById(R.id.spYear1Sec);
        spYear2Sec = root.findViewById(R.id.spYear2Sec);
        spYear3Sec = root.findViewById(R.id.spYear3Sec);
        
        tvYear1Pct = root.findViewById(R.id.tvYear1Pct);
        tvYear2Pct = root.findViewById(R.id.tvYear2Pct);
        tvYear3Pct = root.findViewById(R.id.tvYear3Pct);
        
        pbYear1 = root.findViewById(R.id.pbYear1);
        pbYear2 = root.findViewById(R.id.pbYear2);
        pbYear3 = root.findViewById(R.id.pbYear3);

        db = FirebaseFirestore.getInstance();

        setupSpinners();

        root.findViewById(R.id.llSemStartDate).setOnClickListener(v -> showDatePicker());
        root.findViewById(R.id.llManagePeriods).setOnClickListener(v -> showPeriodConfigDialog());

        loadHodDepartment();

        return root;
    }

    private void setupSpinners() {
        if (getContext() == null) return;
        List<String> sections = java.util.Arrays.asList("All Sections", "Sec A", "Sec B", "Sec C");
        ArrayAdapter<String> sAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, sections);
        
        spYear1Sec.setAdapter(sAdapter);
        spYear2Sec.setAdapter(sAdapter);
        spYear3Sec.setAdapter(sAdapter);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { calculateAnalytics(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };

        spYear1Sec.setOnItemSelectedListener(listener);
        spYear2Sec.setOnItemSelectedListener(listener);
        spYear3Sec.setOnItemSelectedListener(listener);
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        if (!semStartDateStr.isEmpty()) {
            try {
                Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(semStartDateStr);
                if (d != null) cal.setTime(d);
            } catch (Exception ignored) {}
        }

        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(year, month, dayOfMonth);
            String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(sel.getTime());
            saveSemStartDate(dateStr);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadHodDepartment() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists() && isAdded()) {
                hodDepartment = doc.getString("hodDepartment");
                if (hodDepartment == null) hodDepartment = doc.getString("department");
                if (hodDepartment == null) hodDepartment = "";
                
                // CRITICAL FIX: Make sure the department matches the normalized ID string without spaces
                hodDepartment = hodDepartment.replaceAll("\\s+", "_");
                
                loadDepartmentSettings();
            }
        });
    }

    private void loadDepartmentSettings() {
        if (hodDepartment.isEmpty()) return;
        db.collection("departmentSettings").document(hodDepartment).get().addOnSuccessListener(doc -> {
            if (doc.exists() && isAdded()) {
                semStartDateStr = doc.getString("semStartDate");
                if (semStartDateStr == null) semStartDateStr = "";
                
                tvSemStartDate.setText(semStartDateStr.isEmpty() ? "Not Set" : semStartDateStr);
                
                List<String> fetchedPeriods = (List<String>) doc.get("periods");
                if (fetchedPeriods != null && !fetchedPeriods.isEmpty()) {
                    periodsList.clear();
                    periodsList.addAll(fetchedPeriods);
                }
                
                List<String> h = (List<String>) doc.get("holidays");
                if (h != null) holidays = h;

                fetchAttendanceRecords();
            }
        });
    }

    private void saveSemStartDate(String dateStr) {
        if (hodDepartment.isEmpty()) return;
        Map<String, Object> data = new HashMap<>();
        data.put("semStartDate", dateStr);
        
        db.collection("departmentSettings").document(hodDepartment)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                semStartDateStr = dateStr;
                tvSemStartDate.setText(dateStr);
                Toast.makeText(getContext(), "Semester Start Date Updated", Toast.LENGTH_SHORT).show();
                calculateAnalytics();
            });
    }

    private void fetchAttendanceRecords() {
        if (hodDepartment.isEmpty()) return;
        db.collection("attendanceRecords").whereEqualTo("department", hodDepartment).get()
            .addOnSuccessListener(qs -> {
                allAttendanceRecords.clear();
                for (QueryDocumentSnapshot d : qs) {
                    allAttendanceRecords.add(d);
                }
                calculateAnalytics();
            });
    }

    private void calculateAnalytics() {
        if (!isAdded() || semStartDateStr.isEmpty()) {
            resetUI();
            return;
        }

        int totalWorkingDays = calculateWorkingDays(semStartDateStr, holidays);
        if (totalWorkingDays <= 0) {
            resetUI();
            return;
        }

        // We want to calculate percentage for Year 1, Year 2, Year 3 based on the selected section.
        // Percentage = (Average Distinct Days Present for students in that group) / Total Working Days * 100

        String sec1 = spYear1Sec.getSelectedItem().toString().replace("Sec ", "").trim();
        String sec2 = spYear2Sec.getSelectedItem().toString().replace("Sec ", "").trim();
        String sec3 = spYear3Sec.getSelectedItem().toString().replace("Sec ", "").trim();

        updateRow(1, sec1, totalWorkingDays, tvYear1Pct, pbYear1);
        updateRow(2, sec2, totalWorkingDays, tvYear2Pct, pbYear2);
        updateRow(3, sec3, totalWorkingDays, tvYear3Pct, pbYear3);
    }

    private void resetUI() {
        tvYear1Pct.setText("0%"); pbYear1.setProgress(0);
        tvYear2Pct.setText("0%"); pbYear2.setProgress(0);
        tvYear3Pct.setText("0%"); pbYear3.setProgress(0);
    }

    private void updateRow(int yearNum, String secFilter, int totalWorkingDays, TextView tvPct, ProgressBar pb) {
        // Map: studentId -> Set of distinct dates present
        Map<String, Set<String>> studentPresentDays = new HashMap<>();
        Set<String> allStudentsInGroup = new HashSet<>();

        String yearStr = yearNum + "st Year";
        if (yearNum == 2) yearStr = "2nd Year";
        if (yearNum == 3) yearStr = "3rd Year";

        for (QueryDocumentSnapshot d : allAttendanceRecords) {
            String y = d.getString("year");
            String s = d.getString("section");
            String studentId = d.getString("studentId");
            String date = d.getString("date");
            String status = d.getString("status");

            if (y == null || !y.startsWith(String.valueOf(yearNum))) continue;
            
            if (!secFilter.equals("All Sections")) {
                if (s == null) continue;
                String normalizedS = s.toLowerCase().replace("sec ", "").replace("section ", "").trim();
                if (!normalizedS.equalsIgnoreCase(secFilter)) continue;
            }

            if (studentId != null) {
                allStudentsInGroup.add(studentId);
                if ("P".equals(status) && date != null && isDateAfterStart(date)) {
                    if (!studentPresentDays.containsKey(studentId)) {
                        studentPresentDays.put(studentId, new HashSet<>());
                    }
                    studentPresentDays.get(studentId).add(date);
                }
            }
        }

        if (allStudentsInGroup.isEmpty()) {
            tvPct.setText("0%");
            pb.setProgress(0);
            return;
        }

        long totalDaysPresentAllStudents = 0;
        for (String studentId : allStudentsInGroup) {
            if (studentPresentDays.containsKey(studentId)) {
                totalDaysPresentAllStudents += studentPresentDays.get(studentId).size();
            }
        }

        // Average present days per student in this group
        double avgPresentDays = (double) totalDaysPresentAllStudents / allStudentsInGroup.size();
        int pct = (int) Math.round((avgPresentDays / totalWorkingDays) * 100);
        if (pct > 100) pct = 100;

        tvPct.setText(pct + "%");
        pb.setProgress(pct);
    }

    public static int calculateWorkingDays(String startStr, List<String> holidays) {
        if (startStr.isEmpty()) return 0;
        try {
            Date start = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(startStr);
            if (start == null) return 0;
            
            Calendar startCal = Calendar.getInstance();
            startCal.setTime(start);
            
            Calendar endCal = Calendar.getInstance(); 
            // End date is today
            
            if (startCal.after(endCal)) return 0;
            
            int workingDays = 0;
            Calendar current = (Calendar) startCal.clone();
            
            Set<String> holidaySet = holidays != null ? new HashSet<>(holidays) : new HashSet<>();
            
            while (!current.after(endCal)) {
                int dayOfWeek = current.get(Calendar.DAY_OF_WEEK);
                // Exclude Sunday
                if (dayOfWeek != Calendar.SUNDAY) {
                    String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(current.getTime());
                    if (!holidaySet.contains(dateStr)) {
                        workingDays++;
                    }
                }
                current.add(Calendar.DAY_OF_MONTH, 1);
            }
            
            return workingDays;
            
        } catch (ParseException e) {
            return 0;
        }
    }

    private void showPeriodConfigDialog() {
        if (getContext() == null) return;
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_manage_periods, null);
        
        RecyclerView rvPeriods = view.findViewById(R.id.rvPeriods);
        EditText etPeriodName = view.findViewById(R.id.etPeriodName);
        TextView tvStartTime = view.findViewById(R.id.tvStartTime);
        TextView tvEndTime = view.findViewById(R.id.tvEndTime);
        ImageView btnAddPeriod = view.findViewById(R.id.btnAddPeriod);
        Button btnDone = view.findViewById(R.id.btnDone);

        PeriodsAdapter adapter = new PeriodsAdapter();
        rvPeriods.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPeriods.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setView(view)
            .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View.OnClickListener timePickerListener = v -> {
            TextView tv = (TextView) v;
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(requireContext(), (view1, hourOfDay, minute) -> {
                String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                tv.setText(timeStr);
                tv.setTextColor(getResources().getColor(R.color.text_primary));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        };

        tvStartTime.setOnClickListener(timePickerListener);
        tvEndTime.setOnClickListener(timePickerListener);

        btnAddPeriod.setOnClickListener(v -> {
            String name = etPeriodName.getText().toString().trim();
            String start = tvStartTime.getText().toString();
            String end = tvEndTime.getText().toString();

            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a period name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (start.equals("Start Time") || end.equals("End Time")) {
                Toast.makeText(getContext(), "Please select start and end times", Toast.LENGTH_SHORT).show();
                return;
            }

            String finalPeriod = name + " (" + start + " - " + end + ")";
            periodsList.add(finalPeriod);
            adapter.notifyItemInserted(periodsList.size() - 1);
            
            // Reset fields
            etPeriodName.setText("");
            tvStartTime.setText("Start Time");
            tvStartTime.setTextColor(getResources().getColor(R.color.text_secondary));
            tvEndTime.setText("End Time");
            tvEndTime.setTextColor(getResources().getColor(R.color.text_secondary));
            
            savePeriods();
            rvPeriods.scrollToPosition(periodsList.size() - 1);
        });

        btnDone.setOnClickListener(v -> {
            String name = etPeriodName.getText().toString().trim();
            String start = tvStartTime.getText().toString();
            String end = tvEndTime.getText().toString();

            // Auto-add if the user filled out the fields but forgot to press the '+' button
            if (!name.isEmpty() && !start.equals("Start Time") && !end.equals("End Time")) {
                String finalPeriod = name + " (" + start + " - " + end + ")";
                periodsList.add(finalPeriod);
                savePeriods();
            }
            
            dialog.dismiss();
        });

        dialog.show();
    }

    private class PeriodsAdapter extends RecyclerView.Adapter<PeriodsAdapter.PeriodViewHolder> {
        @NonNull
        @Override
        public PeriodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_period, parent, false);
            return new PeriodViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PeriodViewHolder holder, int position) {
            String period = periodsList.get(position);
            holder.tvPeriodName.setText(period);
            holder.btnDeletePeriod.setOnClickListener(v -> {
                int adapterPos = holder.getAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION) {
                    periodsList.remove(adapterPos);
                    notifyItemRemoved(adapterPos);
                    savePeriods();
                }
            });
        }

        @Override
        public int getItemCount() {
            return periodsList.size();
        }

        class PeriodViewHolder extends RecyclerView.ViewHolder {
            TextView tvPeriodName;
            ImageView btnDeletePeriod;

            public PeriodViewHolder(@NonNull View itemView) {
                super(itemView);
                tvPeriodName = itemView.findViewById(R.id.tvPeriodName);
                btnDeletePeriod = itemView.findViewById(R.id.btnDeletePeriod);
            }
        }
    }

    private void savePeriods() {
        if (hodDepartment.isEmpty()) return;
        Map<String, Object> map = new HashMap<>();
        map.put("periods", periodsList);
        db.collection("departmentSettings").document(hodDepartment)
            .set(map, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Periods updated successfully", Toast.LENGTH_SHORT).show();
            });
    }

    private boolean isDateAfterStart(String dateStr) {
        if (semStartDateStr.isEmpty()) return true;
        return dateStr.compareTo(semStartDateStr) >= 0;
    }
}
