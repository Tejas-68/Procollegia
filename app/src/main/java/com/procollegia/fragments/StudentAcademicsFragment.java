package com.procollegia.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.procollegia.R;

import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

public class StudentAcademicsFragment extends Fragment {
    
    private TextView[] tabs;
    private FrameLayout flContent;
    private int activeTabIndex = 0;

    public StudentAcademicsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_student_academics, container, false);

        flContent = root.findViewById(R.id.flAcademicsContent);
        tabs = new TextView[]{
                root.findViewById(R.id.tabInternalMarks),
                root.findViewById(R.id.tabHonorScore),
                root.findViewById(R.id.tabTimetable),
                root.findViewById(R.id.tabFeedback)
        };

        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            tabs[i].setOnClickListener(v -> switchTab(index));
        }

        switchTab(0);
        return root;
    }

    private void switchTab(int index) {
        activeTabIndex = index;
        for (int i = 0; i < tabs.length; i++) {
            if (i == index) {
                tabs[i].setBackgroundResource(R.drawable.bg_pill_active);
                tabs[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_accent));
            } else {
                tabs[i].setBackgroundResource(R.drawable.bg_pill_inactive);
                tabs[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            }
        }

        flContent.removeAllViews();
        switch (index) {
            case 0: inflateInternalMarks(); break;
            case 1: loadFragment(new HonorScoreFragment()); break;
            case 2: loadFragment(new TimetableFragment()); break;
            case 3: loadFragment(new FeedbackFragment()); break;
        }
    }

    // -------------------------------------------------------
    //  Subject data model (mock subjects list, real marks)
    // -------------------------------------------------------
    private static final String[] SUBJECTS  = {"Web Dev", "Database", "Java", "OS"};
    
    // Fallback Mock values if no data found
    private static final int[]    AVG       = {49, 52, 47, 55};  // mock class avg
    private static final String[] BADGE     = {
            "Good Performance! Keep it up",
            "Excellent! Top of Class!",
            "Average. Focus more!",
            "Outstanding Performance!"};

    private int activeSubjectIndex = 0;

    private void loadFragment(androidx.fragment.app.Fragment fragment) {
        if (flContent.getId() == View.NO_ID) {
            flContent.setId(R.id.flAcademicsContent);
        }
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.flAcademicsContent, fragment)
                .commit();
    }

    private void inflateInternalMarks() {
        LayoutInflater.from(getContext()).inflate(R.layout.content_internal_marks, flContent, true);
        flContent.post(() -> setupSubjectTabs(flContent));
    }

    private void loadHonorScoreFragment() {
        // Give the FrameLayout a stable ID so the fragment manager can find it
        if (flContent.getId() == View.NO_ID) {
            flContent.setId(R.id.flAcademicsContent);
        }
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.flAcademicsContent, new HonorScoreFragment())
                .commit();
    }

    @SuppressWarnings("ConstantConditions")
    private void setupSubjectTabs(View root) {
        TextView[] subjectTabs = {
                root.findViewById(R.id.tabSubj1),
                root.findViewById(R.id.tabSubj2),
                root.findViewById(R.id.tabSubj3),
                root.findViewById(R.id.tabSubj4)
        };

        // Set subject names from data
        for (int i = 0; i < subjectTabs.length; i++) {
            subjectTabs[i].setText(SUBJECTS[i]);
            final int idx = i;
            subjectTabs[i].setOnClickListener(v -> {
                activeSubjectIndex = idx;
                updateSubjectPills(subjectTabs, idx);
                bindMarksData(root, idx);
            });
        }

        // Show first subject by default
        activeSubjectIndex = 0;
        updateSubjectPills(subjectTabs, 0);
        bindMarksData(root, 0);
    }

    private void updateSubjectPills(TextView[] subjectTabs, int activeIdx) {
        for (int i = 0; i < subjectTabs.length; i++) {
            if (i == activeIdx) {
                subjectTabs[i].setBackgroundResource(R.drawable.bg_pill_active);
                subjectTabs[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_accent));
            } else {
                subjectTabs[i].setBackgroundResource(R.drawable.bg_pill_inactive);
                subjectTabs[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void bindMarksData(View root, int s) {
        String subject = SUBJECTS[s];
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ? 
                     com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : "MOCK_UID";
        
        android.widget.Button btnReconsideration = root.findViewById(R.id.btnRaiseReconsideration);
        if (btnReconsideration != null) {
            btnReconsideration.setOnClickListener(v -> showReconsiderationDialog(subject));
        }

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection(com.procollegia.Constants.COL_INTERNALS)
            .whereEqualTo("studentId", uid)
            .whereEqualTo("subject", subject)
            .get()
            .addOnSuccessListener(qs -> {
                int ia1 = 0, ia2 = 0, assign = 0, attend = 0;
                if (!qs.isEmpty()) {
                    com.google.firebase.firestore.DocumentSnapshot d = qs.getDocuments().get(0);
                    ia1 = d.getLong("ia1") != null ? d.getLong("ia1").intValue() : 0;
                    ia2 = d.getLong("ia2") != null ? d.getLong("ia2").intValue() : 0;
                    assign = d.getLong("assignment") != null ? d.getLong("assignment").intValue() : 0;
                    attend = d.getLong("attendance") != null ? d.getLong("attendance").intValue() : 0;
                }
                
                int total = ia1 + ia2 + assign + attend;

                ((TextView) root.findViewById(R.id.tvIA1Marks)).setText(ia1 + "/25");
                ((TextView) root.findViewById(R.id.tvIA2Marks)).setText(ia2 + "/25");
                ((TextView) root.findViewById(R.id.tvAssignmentMarks)).setText(assign + "/10");
                ((TextView) root.findViewById(R.id.tvAttendanceMark)).setText(attend + "/10");
                ((TextView) root.findViewById(R.id.tvTotalMarks)).setText(total + "/70");

                // Class comparison bars (progress out of 100 scaled to /70)
                int yourPct = (int) ((total / 70.0) * 100);
                int avgPct  = (int) ((AVG[s]  / 70.0) * 100);

                com.google.android.material.progressindicator.LinearProgressIndicator progressYou =
                        root.findViewById(R.id.progressYou);
                com.google.android.material.progressindicator.LinearProgressIndicator progressAvg =
                        root.findViewById(R.id.progressAvg);

                if (progressYou != null) progressYou.setProgress(yourPct, true);
                if (progressAvg != null) progressAvg.setProgress(avgPct, true);

                ((TextView) root.findViewById(R.id.tvYouValue)).setText("You: " + total);
                ((TextView) root.findViewById(R.id.tvAvgValue)).setText("Class Avg: " + AVG[s]);
                ((TextView) root.findViewById(R.id.tvBadge)).setText(total > AVG[s] ? "Good Performance! Keep it up" : "Average. Focus more!");
            });
    }

    private void showReconsiderationDialog(String subject) {
        if (getContext() == null) return;
        android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint("Reason for reconsideration (e.g., IA-1 marks incorrect)");
        
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
            .setTitle("Raise Reconsideration for " + subject)
            .setView(input)
            .setPositiveButton("Submit", (dialog, which) -> {
                String reason = input.getText().toString().trim();
                if (reason.isEmpty()) return;
                
                String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ? 
                             com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : "MOCK_UID";
                             
                java.util.Map<String, Object> req = new java.util.HashMap<>();
                req.put("studentId", uid);
                req.put("subject", subject);
                req.put("reason", reason);
                req.put("status", "pending");
                req.put("date", new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date()));
                
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection(com.procollegia.Constants.COL_RECONSIDERATIONS)
                    .add(req)
                    .addOnSuccessListener(dr -> android.widget.Toast.makeText(getContext(), "Request Submitted", android.widget.Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> android.widget.Toast.makeText(getContext(), "Failed: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
