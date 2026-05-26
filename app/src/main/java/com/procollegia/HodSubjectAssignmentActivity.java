package com.procollegia;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HodSubjectAssignmentActivity extends AppCompatActivity {

    private Spinner spTeacher, spSubject, spYear, spSection;
    private FirebaseFirestore db;
    
    private final List<String> teacherNames = new ArrayList<>();
    private final List<String> teacherUids = new ArrayList<>();
    private String hodDept = "Unknown";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_subject_assignment);

        db = FirebaseFirestore.getInstance();

        spTeacher = findViewById(R.id.spTeacher);
        spSubject = findViewById(R.id.spSubject);
        spYear = findViewById(R.id.spYear);
        spSection = findViewById(R.id.spSection);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAssign).setOnClickListener(v -> assignSubject());

        setupStaticSpinners();
        loadTeachers();
        
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        if (!currentUid.isEmpty()) {
            db.collection("users").document(currentUid).get().addOnSuccessListener(d -> {
                hodDept = d.getString("hodDepartment") != null ? d.getString("hodDepartment") : "Unknown";
            });
        }
    }

    private void setupStaticSpinners() {
        String[] subjects = {"Web Dev", "Database", "Java", "OS"};
        String[] years    = {"1st Year", "2nd Year", "3rd Year"};
        String[] sections = {"Sec A", "Sec B", "Sec C"};

        spSubject.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subjects));
        spYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, years));
        spSection.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sections));
    }

    private void loadTeachers() {
        db.collection("users").whereEqualTo("role", "Teacher").get()
            .addOnSuccessListener(qs -> {
                teacherNames.clear();
                teacherUids.clear();
                for (QueryDocumentSnapshot d : qs) {
                    teacherNames.add(d.getString("name"));
                    teacherUids.add(d.getId());
                }
                spTeacher.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, teacherNames));
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to load teachers", Toast.LENGTH_SHORT).show());
    }

    private void assignSubject() {
        if (teacherUids.isEmpty()) return;
        
        int teacherIdx = spTeacher.getSelectedItemPosition();
        if (teacherIdx < 0) return;
        
        String uid = teacherUids.get(teacherIdx);
        String name = teacherNames.get(teacherIdx);
        String subject = spSubject.getSelectedItem().toString();
        String year = spYear.getSelectedItem().toString();
        String section = spSection.getSelectedItem().toString();
        
        String docId = subject.replace(" ", "") + "_" + year.replace(" ", "") + "_" + section.replace(" ", "");
        
        Map<String, Object> data = new HashMap<>();
        data.put("teacherUid", uid);
        data.put("teacherName", name);
        data.put("subjectCode", subject.replace(" ", "").toUpperCase());
        data.put("subjectName", subject);
        data.put("department", hodDept);
        data.put("year", year);
        data.put("section", section);
        
        db.collection("subjectAssignments").document(docId).set(data)
            .addOnSuccessListener(v -> Toast.makeText(this, "Subject Assigned Successfully", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to assign", Toast.LENGTH_SHORT).show());
    }
}
