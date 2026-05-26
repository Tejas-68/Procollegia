package com.procollegia;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.procollegia.adapters.TeacherInternalsAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherInternalsActivity extends AppCompatActivity {

    private Spinner spSubject, spYear, spSection;
    private RecyclerView rvStudents;
    private TeacherInternalsAdapter adapter;
    private final List<TeacherInternalsAdapter.InternalStudent> fullList = new ArrayList<>();
    private final List<TeacherInternalsAdapter.InternalStudent> filteredList = new ArrayList<>();
    
    private FirebaseFirestore db;
    private String[] mockSubjects = {"Web Dev", "Database", "Java", "OS"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_internals);

        db = FirebaseFirestore.getInstance();

        spSubject = findViewById(R.id.spSubject);
        spYear = findViewById(R.id.spYear);
        spSection = findViewById(R.id.spSection);
        rvStudents = findViewById(R.id.rvStudents);
        rvStudents.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupSpinners();
        loadStudents();
    }

    private void setupSpinners() {
        String[] years    = {"All Years", "1st Year", "2nd Year", "3rd Year"};
        String[] sections = {"All Sections", "Sec A", "Sec B", "Sec C"};

        spSubject.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mockSubjects));
        spYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, years));
        spSection.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sections));

        AdapterView.OnItemSelectedListener listen = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int i, long l) { filterList(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        spSubject.setOnItemSelectedListener(listen);
        spYear.setOnItemSelectedListener(listen);
        spSection.setOnItemSelectedListener(listen);
    }

    private void loadStudents() {
        db.collection("users").whereEqualTo("role", "Student").get()
            .addOnSuccessListener(qs -> {
                if (isFinishing() || isDestroyed()) return;
                fullList.clear();
                for (QueryDocumentSnapshot d : qs) {
                    String name = d.getString("name");
                    String uucms = d.getString("uucmsId");
                    Object yearObj = d.get("year");
                    String year = yearObj != null ? String.valueOf(yearObj) : "1st Year";
                    String section = d.getString("section") != null ? d.getString("section") : "Sec A";
                    fullList.add(new TeacherInternalsAdapter.InternalStudent(d.getId(), name, uucms, year, section));
                }
                filterList();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to load students", Toast.LENGTH_SHORT).show());
    }

    private void filterList() {
        if (spSubject.getSelectedItem() == null || spYear.getSelectedItem() == null || spSection.getSelectedItem() == null) return;
        
        String subject = spSubject.getSelectedItem().toString();
        String year = spYear.getSelectedItem().toString();
        String sec = spSection.getSelectedItem().toString();

        filteredList.clear();
        for (TeacherInternalsAdapter.InternalStudent s : fullList) {
            boolean matchesYear = year.equals("All Years") || s.year.contains(year) || s.year.startsWith(year.substring(0, 1));
            boolean matchesSec = sec.equals("All Sections") || s.section.contains(sec.replace("Sec ", ""));
            
            if (matchesYear && matchesSec) {
                filteredList.add(s);
            }
        }
        
        loadMarksForSubject(subject);
    }

    private void loadMarksForSubject(String subject) {
        if (filteredList.isEmpty()) {
            updateAdapter();
            return;
        }
        
        // Reset all marks first
        for (TeacherInternalsAdapter.InternalStudent s : filteredList) {
            s.ia1 = 0; s.ia2 = 0; s.assignment = 0; s.attendance = 0;
        }

        db.collection("internals").whereEqualTo("subject", subject).get()
            .addOnSuccessListener(qs -> {
                for (QueryDocumentSnapshot d : qs) {
                    String studentId = d.getString("studentId");
                    for (TeacherInternalsAdapter.InternalStudent s : filteredList) {
                        if (s.id.equals(studentId)) {
                            Long ia1 = d.getLong("ia1");
                            Long ia2 = d.getLong("ia2");
                            Long assign = d.getLong("assignment");
                            Long attend = d.getLong("attendance");
                            if (ia1 != null) s.ia1 = ia1.intValue();
                            if (ia2 != null) s.ia2 = ia2.intValue();
                            if (assign != null) s.assignment = assign.intValue();
                            if (attend != null) s.attendance = attend.intValue();
                        }
                    }
                }
                updateAdapter();
            });
    }

    private void updateAdapter() {
        if (adapter == null) {
            adapter = new TeacherInternalsAdapter(filteredList, this::saveMarks);
            rvStudents.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private void saveMarks(TeacherInternalsAdapter.InternalStudent student, int ia1, int ia2, int assignment, int attendance) {
        String subject = spSubject.getSelectedItem().toString();
        String docId = student.id + "_" + subject.replace(" ", "");
        
        Map<String, Object> data = new HashMap<>();
        data.put("studentId", student.id);
        data.put("studentName", student.name);
        data.put("subject", subject);
        data.put("ia1", ia1);
        data.put("ia2", ia2);
        data.put("assignment", assignment);
        data.put("attendance", attendance);
        
        db.collection("internals").document(docId).set(data)
            .addOnSuccessListener(v -> Toast.makeText(this, "Saved marks for " + student.name, Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show());
    }
}
