package com.procollegia.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.procollegia.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HodConfigAcademicYearFragment extends Fragment {

    private Button btnUpgradeSemester, btnUndoPromotion, btnAddHoliday;
    private LinearLayout llHolidaysList;

    private FirebaseFirestore db;
    private String hodDepartment = "";

    private List<String> holidays = new ArrayList<>();
    private Map<String, String> holidayNames = new HashMap<>();
    private long lastPromotionTimestamp = 0;

    public HodConfigAcademicYearFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_hod_config_academic_year, container, false);

        btnUpgradeSemester = root.findViewById(R.id.btnUpgradeSemester);
        btnUndoPromotion = root.findViewById(R.id.btnUndoPromotion);
        btnAddHoliday = root.findViewById(R.id.btnAddHoliday);
        llHolidaysList = root.findViewById(R.id.llHolidaysList);

        db = FirebaseFirestore.getInstance();

        btnUpgradeSemester.setOnClickListener(v -> confirmUpgradeSemester1());
        btnUndoPromotion.setOnClickListener(v -> confirmUndoPromotion());
        btnAddHoliday.setOnClickListener(v -> showAddHolidayDialog());

        loadHodDepartment();

        return root;
    }

    private void loadHodDepartment() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists() && isAdded()) {
                hodDepartment = doc.getString("hodDepartment");
                if (hodDepartment == null) hodDepartment = doc.getString("department");
                if (hodDepartment == null) hodDepartment = "";

                loadDepartmentSettings();
            }
        });
    }

    private void loadDepartmentSettings() {
        if (hodDepartment.isEmpty()) return;
        db.collection("departmentSettings").document(hodDepartment)
            .addSnapshotListener((doc, e) -> {
                if (e != null || !isAdded()) return;
                if (doc != null && doc.exists()) {
                    List<String> h = (List<String>) doc.get("holidays");
                    Map<String, String> hn = (Map<String, String>) doc.get("holidayNames");
                    Long lpt = doc.getLong("lastPromotionTimestamp");

                    if (h != null) holidays = h;
                    if (hn != null) holidayNames = hn;
                    lastPromotionTimestamp = lpt != null ? lpt : 0;

                    renderHolidays();
                    
                    long dayInMillis = 24 * 60 * 60 * 1000L;
                    if (System.currentTimeMillis() - lastPromotionTimestamp <= dayInMillis && lastPromotionTimestamp > 0) {
                        btnUndoPromotion.setVisibility(View.VISIBLE);
                    } else {
                        btnUndoPromotion.setVisibility(View.GONE);
                    }
                }
            });
    }

    private void confirmUpgradeSemester1() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Upgrade Academic Semester")
            .setMessage("Are you sure you want to upgrade ALL students in " + hodDepartment + "? 6th Sem students will become Alumni and slated for deletion.")
            .setPositiveButton("Continue", (d, w) -> confirmUpgradeSemester2())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void confirmUpgradeSemester2() {
        new AlertDialog.Builder(requireContext())
            .setTitle("FINAL WARNING")
            .setMessage("This action will shift all students to their next semester. Proceed?")
            .setPositiveButton("YES, UPGRADE", (d, w) -> performUpgrade())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void performUpgrade() {
        Toast.makeText(getContext(), "Upgrading students... Please wait", Toast.LENGTH_LONG).show();

        db.collection("users")
            .whereEqualTo("department", hodDepartment)
            .whereEqualTo("role", "Student")
            .get()
            .addOnSuccessListener(qs -> {
                WriteBatch batch = db.batch();
                List<Map<String, Object>> backupList = new ArrayList<>();
                int count = 0;

                for (DocumentSnapshot doc : qs.getDocuments()) {
                    String currentSemStr = doc.getString("semester");
                    String currentYear = doc.getString("year");
                    String currentRole = doc.getString("role");

                    if (currentSemStr != null) {
                        try {
                            int currentSem = Integer.parseInt(currentSemStr);
                            
                            Map<String, Object> backup = new HashMap<>();
                            backup.put("userId", doc.getId());
                            backup.put("oldSemester", currentSemStr);
                            backup.put("oldYear", currentYear);
                            backup.put("oldRole", currentRole);
                            backupList.add(backup);

                            if (currentSem >= 6) {
                                // Become Alumni
                                batch.update(doc.getReference(), 
                                    "role", "Alumni",
                                    "deletionDate", System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L)
                                );
                            } else {
                                int nextSem = currentSem + 1;
                                String nextYear = "1st Year";
                                if (nextSem == 3 || nextSem == 4) nextYear = "2nd Year";
                                if (nextSem == 5 || nextSem == 6) nextYear = "3rd Year";

                                batch.update(doc.getReference(), 
                                    "semester", String.valueOf(nextSem),
                                    "year", nextYear
                                );
                            }
                            count++;
                        } catch (NumberFormatException ignored) {}
                    }
                }

                if (count > 0) {
                    Map<String, Object> promoData = new HashMap<>();
                    promoData.put("lastPromotionTimestamp", System.currentTimeMillis());
                    promoData.put("lastPromotionBackup", backupList);
                    batch.update(db.collection("departmentSettings").document(hodDepartment), promoData);

                    int finalCount = count;
                    batch.commit()
                        .addOnSuccessListener(v -> Toast.makeText(getContext(), "Successfully upgraded " + finalCount + " students!", Toast.LENGTH_LONG).show())
                        .addOnFailureListener(ex -> Toast.makeText(getContext(), "Upgrade failed: " + ex.getMessage(), Toast.LENGTH_SHORT).show());
                } else {
                    Toast.makeText(getContext(), "No students found to upgrade.", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to fetch students.", Toast.LENGTH_SHORT).show());
    }

    private void confirmUndoPromotion() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Undo Promotion")
            .setMessage("Revert the last semester upgrade? This will restore students' previous semesters, years, and roles.")
            .setPositiveButton("Undo Now", (d, w) -> performUndo())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void performUndo() {
        Toast.makeText(getContext(), "Undoing promotion...", Toast.LENGTH_SHORT).show();
        db.collection("departmentSettings").document(hodDepartment).get().addOnSuccessListener(doc -> {
            List<Map<String, Object>> backupList = (List<Map<String, Object>>) doc.get("lastPromotionBackup");
            if (backupList != null && !backupList.isEmpty()) {
                WriteBatch batch = db.batch();
                for (Map<String, Object> b : backupList) {
                    String uid = (String) b.get("userId");
                    if (uid != null) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("semester", b.get("oldSemester"));
                        updates.put("year", b.get("oldYear"));
                        updates.put("role", b.get("oldRole"));
                        updates.put("deletionDate", com.google.firebase.firestore.FieldValue.delete());
                        
                        batch.update(db.collection("users").document(uid), updates);
                    }
                }
                
                Map<String, Object> resetData = new HashMap<>();
                resetData.put("lastPromotionTimestamp", com.google.firebase.firestore.FieldValue.delete());
                resetData.put("lastPromotionBackup", com.google.firebase.firestore.FieldValue.delete());
                batch.update(doc.getReference(), resetData);

                batch.commit()
                    .addOnSuccessListener(v -> Toast.makeText(getContext(), "Promotion reverted successfully.", Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Undo failed.", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(getContext(), "No backup found to undo.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddHolidayDialog() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(year, month, dayOfMonth);
            String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(sel.getTime());
            
            promptHolidayName(dateStr);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void promptHolidayName(String dateStr) {
        EditText input = new EditText(requireContext());
        input.setHint("Holiday Name (e.g. Diwali)");
        new AlertDialog.Builder(requireContext())
            .setTitle("Holiday on " + dateStr)
            .setView(input)
            .setPositiveButton("Save", (d, w) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) saveHoliday(dateStr, name);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void saveHoliday(String dateStr, String name) {
        if (!holidays.contains(dateStr)) holidays.add(dateStr);
        holidayNames.put(dateStr, name);

        Map<String, Object> data = new HashMap<>();
        data.put("holidays", holidays);
        data.put("holidayNames", holidayNames);

        db.collection("departmentSettings").document(hodDepartment)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Holiday Added", Toast.LENGTH_SHORT).show());
    }

    private void removeHoliday(String dateStr) {
        holidays.remove(dateStr);
        holidayNames.remove(dateStr);
        
        Map<String, Object> data = new HashMap<>();
        data.put("holidays", holidays);
        data.put("holidayNames", holidayNames);

        db.collection("departmentSettings").document(hodDepartment)
            .update(data)
            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Holiday Removed", Toast.LENGTH_SHORT).show());
    }

    private void renderHolidays() {
        llHolidaysList.removeAllViews();
        if (holidays.isEmpty()) {
            TextView tv = new TextView(getContext());
            tv.setText("No holidays declared yet.");
            tv.setTextColor(getResources().getColor(R.color.text_muted, null));
            llHolidaysList.addView(tv);
            return;
        }

        for (String dateStr : holidays) {
            View v = getLayoutInflater().inflate(R.layout.item_holiday, llHolidaysList, false);
            TextView tvDate = v.findViewById(R.id.tvHolidayDate);
            TextView tvName = v.findViewById(R.id.tvHolidayName);
            View btnRemove = v.findViewById(R.id.btnRemoveHoliday);

            tvDate.setText(dateStr);
            tvName.setText(holidayNames.getOrDefault(dateStr, "Holiday"));
            
            btnRemove.setOnClickListener(click -> removeHoliday(dateStr));
            llHolidaysList.addView(v);
        }
    }
}
