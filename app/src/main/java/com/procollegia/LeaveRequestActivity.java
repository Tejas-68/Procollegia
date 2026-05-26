package com.procollegia;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class LeaveRequestActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 101;
    private TextView tvDateFrom, tvDateTo, tvAttachmentName;
    private EditText etReason;
    private Uri attachmentUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leave_request);

        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());

        tvDateFrom = findViewById(R.id.tvDateFrom);
        tvDateTo = findViewById(R.id.tvDateTo);
        etReason = findViewById(R.id.etReason);
        tvAttachmentName = findViewById(R.id.tvAttachmentName);
        LinearLayout llAttach = findViewById(R.id.llAttach);
        Button btnSubmit = findViewById(R.id.btnSubmit);

        tvDateFrom.setOnClickListener(v -> showDatePicker(tvDateFrom));
        tvDateTo.setOnClickListener(v -> showDatePicker(tvDateTo));

        llAttach.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_FILE_REQUEST);
        });

        btnSubmit.setOnClickListener(v -> {
            String reason = etReason.getText().toString().trim();
            String start = tvDateFrom.getText().toString();
            String end = tvDateTo.getText().toString();

            if (reason.isEmpty() || start.contains("Select") || end.contains("Select")) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // BUG 3 FIX: write to Firestore instead of just showing a Toast
            btnSubmit.setEnabled(false);
            submitLeaveRequest(reason, start, end, btnSubmit);
        });
    }

    // ── Firestore submit ─────────────────────────────────────────────────────

    private void submitLeaveRequest(String reason, String dateFrom, String dateTo, Button btnSubmit) {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in. Please log in again.", Toast.LENGTH_SHORT).show();
            btnSubmit.setEnabled(true);
            return;
        }
        String uid = user.getUid();

        // Fetch student name first, then write leave request
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    if (name == null) name = "Student";

                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("studentId",   uid);
                    data.put("studentName", name);
                    data.put("dateFrom",    dateFrom);
                    data.put("dateTo",      dateTo);
                    data.put("date",        dateFrom); // 'date' field used by attendance leave check
                    data.put("reason",      reason);
                    data.put("status",      "pending");
                    data.put("createdAt",   com.google.firebase.firestore.FieldValue.serverTimestamp());

                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("leaveRequests").add(data)
                            .addOnSuccessListener(ref -> {
                                Toast.makeText(this, "Leave Request Submitted Successfully!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnSubmit.setEnabled(true);
                                Toast.makeText(this, "Failed to submit: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Could not fetch your profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void showDatePicker(TextView target) {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            String date = String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d", year1, month1 + 1, dayOfMonth);
            target.setText(date);
        }, year, month, day);
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            attachmentUri = data.getData();
            // Just extracting a mock file name from URI path for UX
            String path = attachmentUri.getPath();
            if(path != null){
                int lastSlash = path.lastIndexOf('/');
                tvAttachmentName.setText(lastSlash != -1 ? path.substring(lastSlash + 1) : "Document Attached");
            } else {
                tvAttachmentName.setText("Document Attached");
            }
        }
    }
}
