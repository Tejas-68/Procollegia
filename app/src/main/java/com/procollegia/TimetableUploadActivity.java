package com.procollegia;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TimetableUploadActivity extends AppCompatActivity {

    private ImageView ivPreview;
    private Button btnPickImage, btnUpload;
    private ProgressBar progressBar;
    private TextView tvStatus, tvDeptInfo, tvPlaceholder;

    private Bitmap selectedBitmap;
    private Uri selectedUri;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth mAuth;
    private String teacherDepartment;
    private String teacherName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable_upload);

        db      = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        mAuth   = FirebaseAuth.getInstance();

        ivPreview    = findViewById(R.id.ivPreview);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnUpload    = findViewById(R.id.btnSave);
        progressBar  = findViewById(R.id.progressBar);
        tvStatus     = findViewById(R.id.tvStatus);
        tvDeptInfo   = findViewById(R.id.tvDeptInfo);
        tvPlaceholder= findViewById(R.id.tvPlaceholder);

        btnUpload.setEnabled(false);
        btnUpload.setText("Deploy to Students");

        // Load teacher's department to know where to save
        loadTeacherProfile();

        ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedUri = result.getData().getData();
                        try {
                            selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedUri);
                            ivPreview.setImageBitmap(selectedBitmap);
                            tvPlaceholder.setVisibility(View.GONE);
                            
                            boolean hasDept = (teacherDepartment != null && !teacherDepartment.trim().isEmpty());
                            btnUpload.setEnabled(hasDept);
                            
                            if (hasDept) {
                                tvStatus.setText("Image ready. Tap 'Deploy to Students'.");
                            } else {
                                tvStatus.setText("! Please set your department in profile first.");
                                Toast.makeText(this, "Department not found in profile", Toast.LENGTH_LONG).show();
                            }
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        btnUpload.setOnClickListener(v -> uploadTimetable());
    }

    private void loadTeacherProfile() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    teacherDepartment = doc.getString("department");
                    teacherName       = doc.getString("name");

                    // HOD uses hodDepartment if department is null
                    if (teacherDepartment == null || teacherDepartment.isEmpty()) {
                        teacherDepartment = doc.getString("hodDepartment");
                    }

                    if (teacherDepartment != null && !teacherDepartment.trim().isEmpty()) {
                        tvDeptInfo.setText("Uploading for: " + teacherDepartment.toUpperCase() + " Department");
                        if (selectedBitmap != null) {
                            btnUpload.setEnabled(true);
                            tvStatus.setText("Image ready. Tap 'Deploy to Students'.");
                        }
                    } else {
                        tvDeptInfo.setText("! Department not set in your profile.");
                        btnUpload.setEnabled(false);
                    }
                });
    }

    private void uploadTimetable() {
        if (selectedBitmap == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (teacherDepartment == null || teacherDepartment.trim().isEmpty()) {
            Toast.makeText(this, "Department missing in profile!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnUpload.setEnabled(false);
        btnPickImage.setEnabled(false);
        tvStatus.setText("Compressing and uploading timetable...");

        new Thread(() -> {
            try {
                // Resize image to prevent Base64 string from exceeding Firestore 1MB limit
                int maxWidth = 1080;
                int width = selectedBitmap.getWidth();
                int height = selectedBitmap.getHeight();
                
                if (width > maxWidth) {
                    float ratio = (float) width / height;
                    width = maxWidth;
                    height = (int) (width / ratio);
                }
                Bitmap resized = Bitmap.createScaledBitmap(selectedBitmap, width, height, true);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resized.compress(Bitmap.CompressFormat.JPEG, 60, baos); // Aggressive compression for Base64
                byte[] imageData = baos.toByteArray();
                String base64Image = android.util.Base64.encodeToString(imageData, android.util.Base64.NO_WRAP);

                runOnUiThread(() -> {
                    String dept = teacherDepartment.toUpperCase();
                    // Prefix with data URI scheme so Glide can load it automatically later
                    String dataUri = "data:image/jpeg;base64," + base64Image;
                    saveUrlToFirestore(dept, dataUri);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                    btnPickImage.setEnabled(true);
                    tvStatus.setText("Error: Processing failed: " + e.getMessage());
                    Toast.makeText(this, "Failed to process image", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void saveUrlToFirestore(String dept, String imageUrl) {
        // Format today's date as YYYY-MM-DD so we can expire old timetables
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH)
                .format(new java.util.Date());

        Map<String, Object> data = new HashMap<>();
        data.put("imageUrl",   imageUrl);
        data.put("department", dept);
        data.put("uploadedBy", teacherName != null ? teacherName : "Unknown");
        data.put("uploadedAt", com.google.firebase.Timestamp.now());
        data.put("uploadDate", today);   // Used to auto-expire after 1 day

        db.collection("timetables").document(dept)
                .set(data)
                .addOnSuccessListener(v -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Success: Timetable live for all " + dept + " students!");
                    Toast.makeText(this, "Timetable deployed to all " + dept + " students!", Toast.LENGTH_LONG).show();
                    btnPickImage.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Error: Failed to save: " + e.getMessage());
                    btnUpload.setEnabled(true);
                    btnPickImage.setEnabled(true);
                });
    }
}
