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
        flContent.post(() -> {
            setupSubjectTabs(flContent);
            listenForTimetable(flContent);
        });
    }

    private void listenForTimetable(View root) {
        android.widget.Button btnDownload = root.findViewById(R.id.btnDownloadTimetable);
        android.widget.Button btnRoom = root.findViewById(R.id.btnDownloadRoomAllotment);
        if (btnDownload == null) return;

        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(user.getUid())
            .get().addOnSuccessListener(doc -> {
                String dept = doc.getString("department");
                if (dept == null) return;

                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("departmentSettings").document(dept)
                    .addSnapshotListener((d, e) -> {
                        if (e != null || !isAdded()) return;
                        if (d != null && d.exists()) {
                            Boolean active = d.getBoolean("isInternalsActive");
                            if (active != null && active) {
                                String ttUrl = d.getString("internalsTimetableUrl");
                                String ttType = d.getString("timetableFileType");
                                if (ttUrl != null) {
                                    btnDownload.setVisibility(View.VISIBLE);
                                    btnDownload.setOnClickListener(v -> openDoc(ttUrl, ttType));
                                } else {
                                    btnDownload.setVisibility(View.GONE);
                                }

                                String roomUrl = d.getString("roomAllotmentUrl");
                                String roomType = d.getString("roomAllotmentFileType");
                                if (roomUrl != null && btnRoom != null) {
                                    btnRoom.setVisibility(View.VISIBLE);
                                    btnRoom.setOnClickListener(v -> openDoc(roomUrl, roomType));
                                } else if (btnRoom != null) {
                                    btnRoom.setVisibility(View.GONE);
                                }
                            } else {
                                btnDownload.setVisibility(View.GONE);
                                if (btnRoom != null) btnRoom.setVisibility(View.GONE);
                            }
                        }
                    });
            });
    }

    private void openDoc(String dataUrl, String fileType) {
        if ("pdf".equals(fileType)) {
            openPdfDoc(dataUrl);
        } else {
            showImageDocDialog(dataUrl);
        }
    }

    private void showImageDocDialog(String dataUrl) {
        try {
            String base64 = dataUrl.substring(dataUrl.indexOf(",") + 1);
            byte[] decoded = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length);

            android.widget.ImageView iv = new android.widget.ImageView(getContext());
            iv.setImageBitmap(bitmap);
            iv.setAdjustViewBounds(true);

            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Internals Timetable")
                .setView(iv)
                .setPositiveButton("Close", null)
                .show();
        } catch (Exception e) {
            android.widget.Toast.makeText(getContext(), "Failed to load image", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void openPdfDoc(String dataUrl) {
        try {
            String base64 = dataUrl;
            if (base64.contains(",")) base64 = base64.substring(base64.indexOf(",") + 1);
            byte[] pdfBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);

            java.io.File cacheDir = requireContext().getCacheDir();
            java.io.File pdfFile = new java.io.File(cacheDir, "internals_doc.pdf");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(pdfFile);
            fos.write(pdfBytes);
            fos.close();

            android.net.Uri pdfUri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                pdfFile
            );

            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                android.widget.Toast.makeText(getContext(), "No PDF viewer installed on this device.", android.widget.Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            android.widget.Toast.makeText(getContext(), "Failed to open PDF: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        }
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
        com.google.firebase.auth.FirebaseUser currentUser =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String uid = currentUser.getUid();
        com.google.firebase.firestore.FirebaseFirestore db =
                com.google.firebase.firestore.FirebaseFirestore.getInstance();

        android.widget.Button btnReconsideration = root.findViewById(R.id.btnRaiseReconsideration);
        if (btnReconsideration != null) {
            btnReconsideration.setOnClickListener(v -> showReconsiderationDialog(subject));
        }

        // Step 1: load student profile to get dept + year
        db.collection("users").document(uid).get()
            .addOnSuccessListener(profile -> {
                if (!isAdded()) return;
                String dept     = profile.getString("department");
                Object yearObj  = profile.get("year");
                String yearStr  = (yearObj != null) ? String.valueOf(yearObj).trim() : "";
                // Extract digit: "3rd Year" → "3"
                String yearNum  = yearStr.replaceAll("[^0-9]", "");
                if (yearNum.isEmpty()) yearNum = "1";
                if (dept == null) dept = "";

                final String finalDept = dept;
                final String finalYearNum = yearNum;

                // Step 2: load HOD max marks config
                String settingsDocId = dept.replaceAll("\\s+", "_");
                String prefix = "maxMarks_year" + yearNum + "_";

                db.collection("departmentSettings").document(settingsDocId).get()
                    .addOnSuccessListener(settings -> {
                        if (!isAdded()) return;
                        int maxIa1 = 25, maxIa2 = 25, maxAssign = 10;
                        if (settings.exists()) {
                            Long v1 = settings.getLong(prefix + "ia1");
                            Long v2 = settings.getLong(prefix + "ia2");
                            Long v3 = settings.getLong(prefix + "assignment");
                            if (v1 != null) maxIa1   = v1.intValue();
                            if (v2 != null) maxIa2   = v2.intValue();
                            if (v3 != null) maxAssign = v3.intValue();
                        }
                        int totalMax = maxIa1 + maxIa2 + maxAssign;

                        final int fMaxIa1 = maxIa1, fMaxIa2 = maxIa2,
                                  fMaxAssign = maxAssign, fTotalMax = totalMax;

                        // Step 3: load actual marks from internals collection
                        db.collection(com.procollegia.Constants.COL_INTERNALS)
                            .whereEqualTo("studentId", uid)
                            .whereEqualTo("subject", subject)
                            .get()
                            .addOnSuccessListener(qs -> {
                                if (!isAdded()) return;
                                int ia1 = 0, ia2 = 0, assign = 0;
                                if (!qs.isEmpty()) {
                                    com.google.firebase.firestore.DocumentSnapshot d =
                                            qs.getDocuments().get(0);
                                    if (d.getLong("ia1")        != null) ia1    = d.getLong("ia1").intValue();
                                    if (d.getLong("ia2")        != null) ia2    = d.getLong("ia2").intValue();
                                    if (d.getLong("assignment") != null) assign = d.getLong("assignment").intValue();
                                }

                                int total = ia1 + ia2 + assign;

                                // Update UI with real max marks
                                TextView tvIa1    = root.findViewById(R.id.tvIA1Marks);
                                TextView tvIa2    = root.findViewById(R.id.tvIA2Marks);
                                TextView tvAssign = root.findViewById(R.id.tvAssignmentMarks);
                                TextView tvTotal  = root.findViewById(R.id.tvTotalMarks);

                                if (tvIa1    != null) tvIa1.setText(ia1    + "/" + fMaxIa1);
                                if (tvIa2    != null) tvIa2.setText(ia2    + "/" + fMaxIa2);
                                if (tvAssign != null) tvAssign.setText(assign + "/" + fMaxAssign);
                                if (tvTotal  != null) tvTotal.setText(total  + "/" + fTotalMax);

                                // Hide attendance row if it exists (removed from schema)
                                View attendRow = root.findViewById(R.id.tvAttendanceMark);
                                if (attendRow != null) attendRow.setVisibility(View.GONE);

                                // Progress bars
                                int yourPct = fTotalMax > 0 ? (int) ((total / (double) fTotalMax) * 100) : 0;
                                int avgPct  = fTotalMax > 0 ? (int) ((AVG[s] / (double) fTotalMax) * 100) : 0;

                                com.google.android.material.progressindicator.LinearProgressIndicator pYou =
                                        root.findViewById(R.id.progressYou);
                                com.google.android.material.progressindicator.LinearProgressIndicator pAvg =
                                        root.findViewById(R.id.progressAvg);
                                if (pYou != null) pYou.setProgress(yourPct, true);
                                if (pAvg != null) pAvg.setProgress(avgPct, true);

                                TextView tvYou   = root.findViewById(R.id.tvYouValue);
                                TextView tvAvg   = root.findViewById(R.id.tvAvgValue);
                                TextView tvBadge = root.findViewById(R.id.tvBadge);
                                if (tvYou   != null) tvYou.setText("You: " + total + "/" + fTotalMax);
                                if (tvAvg   != null) tvAvg.setText("Class Avg: " + AVG[s]);
                                if (tvBadge != null) tvBadge.setText(
                                        total > AVG[s] ? "Good Performance! Keep it up" : "Average. Focus more!");
                            });
                    })
                    .addOnFailureListener(e -> {
                        // If settings load fails, show marks with defaults
                        db.collection(com.procollegia.Constants.COL_INTERNALS)
                            .whereEqualTo("studentId", uid)
                            .whereEqualTo("subject", subject)
                            .get()
                            .addOnSuccessListener(qs -> {
                                if (!isAdded() || qs.isEmpty()) return;
                                com.google.firebase.firestore.DocumentSnapshot d = qs.getDocuments().get(0);
                                int ia1 = d.getLong("ia1") != null ? d.getLong("ia1").intValue() : 0;
                                int ia2 = d.getLong("ia2") != null ? d.getLong("ia2").intValue() : 0;
                                int assign = d.getLong("assignment") != null ? d.getLong("assignment").intValue() : 0;
                                int total = ia1 + ia2 + assign;
                                TextView tvIa1 = root.findViewById(R.id.tvIA1Marks);
                                TextView tvIa2 = root.findViewById(R.id.tvIA2Marks);
                                TextView tvAs  = root.findViewById(R.id.tvAssignmentMarks);
                                TextView tvTot = root.findViewById(R.id.tvTotalMarks);
                                if (tvIa1 != null) tvIa1.setText(ia1    + "/25");
                                if (tvIa2 != null) tvIa2.setText(ia2    + "/25");
                                if (tvAs  != null) tvAs.setText(assign  + "/10");
                                if (tvTot != null) tvTot.setText(total  + "/60");
                            });
                    });
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
