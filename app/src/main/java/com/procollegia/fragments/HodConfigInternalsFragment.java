package com.procollegia.fragments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.procollegia.R;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class HodConfigInternalsFragment extends Fragment {

    private Button btnUploadTimetable, btnUploadRoomAllotment, btnEndInternals, btnSaveMaxMarks;
    private TextView tvInternalsStatus;
    private Spinner spMaxMarksYear;
    private EditText etIA1Max, etIA2Max, etAssignmentMax;

    private FirebaseFirestore db;
    private String hodDepartment = "";
    private boolean isInternalsActive = false;

    private ActivityResultLauncher<Intent> ttImagePicker, ttPdfPicker;
    private ActivityResultLauncher<Intent> roomImagePicker, roomPdfPicker;

    public HodConfigInternalsFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

        ttImagePicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            if (r.getResultCode() == Activity.RESULT_OK && r.getData() != null) uploadFile(r.getData().getData(), true, "image");
        });
        ttPdfPicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            if (r.getResultCode() == Activity.RESULT_OK && r.getData() != null) uploadFile(r.getData().getData(), true, "pdf");
        });
        roomImagePicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            if (r.getResultCode() == Activity.RESULT_OK && r.getData() != null) uploadFile(r.getData().getData(), false, "image");
        });
        roomPdfPicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            if (r.getResultCode() == Activity.RESULT_OK && r.getData() != null) uploadFile(r.getData().getData(), false, "pdf");
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_hod_config_internals, container, false);

        btnUploadTimetable = root.findViewById(R.id.btnUploadTimetable);
        btnUploadRoomAllotment = root.findViewById(R.id.btnUploadRoomAllotment);
        btnEndInternals = root.findViewById(R.id.btnEndInternals);
        tvInternalsStatus = root.findViewById(R.id.tvInternalsStatus);

        spMaxMarksYear = root.findViewById(R.id.spMaxMarksYear);
        etIA1Max = root.findViewById(R.id.etIA1Max);
        etIA2Max = root.findViewById(R.id.etIA2Max);
        etAssignmentMax = root.findViewById(R.id.etAssignmentMax);
        btnSaveMaxMarks = root.findViewById(R.id.btnSaveMaxMarks);

        if (getContext() != null) {
            String[] years = {"All Years", "1st Year", "2nd Year", "3rd Year"};
            spMaxMarksYear.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, years));
        }

        btnUploadTimetable.setOnClickListener(v -> showTypeDialog(true));
        btnUploadRoomAllotment.setOnClickListener(v -> showTypeDialog(false));
        btnEndInternals.setOnClickListener(v -> endInternals());
        btnSaveMaxMarks.setOnClickListener(v -> saveMaxMarks());

        loadHodDepartment();

        return root;
    }

    private void showTypeDialog(boolean isTimetable) {
        new AlertDialog.Builder(requireContext())
            .setTitle(isTimetable ? "Upload Timetable" : "Upload Room Allotment")
            .setPositiveButton("Image", (d, w) -> {
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                if (isTimetable) ttImagePicker.launch(i); else roomImagePicker.launch(i);
            })
            .setNeutralButton("PDF", (d, w) -> {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("application/pdf");
                if (isTimetable) ttPdfPicker.launch(Intent.createChooser(i, "Select PDF"));
                else roomPdfPicker.launch(Intent.createChooser(i, "Select PDF"));
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void uploadFile(Uri uri, boolean isTimetable, String type) {
        if (getActivity() == null) return;
        Toast.makeText(getContext(), "Uploading...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                String dataUri = "";
                if (type.equals("image")) {
                    Bitmap bmp = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), uri);
                    int w = 1024;
                    int h = Math.round(w / (bmp.getWidth() / (float) bmp.getHeight()));
                    Bitmap resized = Bitmap.createScaledBitmap(bmp, w, h, false);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    dataUri = "data:image/jpeg;base64," + android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP);
                } else {
                    ContentResolver cr = getActivity().getContentResolver();
                    InputStream is = cr.openInputStream(uri);
                    if (is == null) throw new Exception("Cannot open PDF");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = is.read(buffer)) != -1) baos.write(buffer, 0, len);
                    is.close();
                    if (baos.size() > 900_000) {
                        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "PDF too large (max 900KB)", Toast.LENGTH_LONG).show());
                        return;
                    }
                    dataUri = "data:application/pdf;base64," + android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP);
                }

                String finalUri = dataUri;
                getActivity().runOnUiThread(() -> saveToFirestore(finalUri, isTimetable, type));

            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    private void saveToFirestore(String dataUri, boolean isTimetable, String fileType) {
        if (hodDepartment.isEmpty()) return;
        Map<String, Object> data = new HashMap<>();
        data.put("isInternalsActive", true);
        if (isTimetable) {
            data.put("internalsTimetableUrl", dataUri);
            data.put("timetableFileType", fileType);
        } else {
            data.put("roomAllotmentUrl", dataUri);
            data.put("roomAllotmentFileType", fileType);
        }

        db.collection("departmentSettings").document(hodDepartment)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Uploaded Successfully!", Toast.LENGTH_SHORT).show());
    }

    private void endInternals() {
        if (hodDepartment.isEmpty()) return;
        new AlertDialog.Builder(requireContext())
            .setTitle("End Internals?")
            .setMessage("Remove internals from student and teacher dashboards?")
            .setPositiveButton("End Now", (d, w) -> {
                Map<String, Object> data = new HashMap<>();
                data.put("isInternalsActive", false);
                data.put("internalsTimetableUrl", com.google.firebase.firestore.FieldValue.delete());
                data.put("timetableFileType", com.google.firebase.firestore.FieldValue.delete());
                data.put("roomAllotmentUrl", com.google.firebase.firestore.FieldValue.delete());
                data.put("roomAllotmentFileType", com.google.firebase.firestore.FieldValue.delete());
                db.collection("departmentSettings").document(hodDepartment).update(data)
                    .addOnSuccessListener(v -> Toast.makeText(getContext(), "Internals Ended", Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void saveMaxMarks() {
        if (hodDepartment.isEmpty()) return;
        String year = spMaxMarksYear.getSelectedItem().toString();
        String ia1 = etIA1Max.getText().toString().trim();
        String ia2 = etIA2Max.getText().toString().trim();
        String assign = etAssignmentMax.getText().toString().trim();

        if (ia1.isEmpty() && ia2.isEmpty() && assign.isEmpty()) {
            Toast.makeText(getContext(), "Enter at least one max mark", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        if (!ia1.isEmpty()) data.put("ia1Max", Integer.parseInt(ia1));
        if (!ia2.isEmpty()) data.put("ia2Max", Integer.parseInt(ia2));
        if (!assign.isEmpty()) data.put("assignmentMax", Integer.parseInt(assign));

        db.collection("departmentSettings").document(hodDepartment)
            .collection("maxMarks").document(year)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Max Marks Saved for " + year, Toast.LENGTH_SHORT).show();
                etIA1Max.setText(""); etIA2Max.setText(""); etAssignmentMax.setText("");
            });
    }

    private void loadHodDepartment() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists() && isAdded()) {
                hodDepartment = doc.getString("hodDepartment");
                if (hodDepartment == null) hodDepartment = doc.getString("department");
                if (hodDepartment == null) hodDepartment = "";

                db.collection("departmentSettings").document(hodDepartment)
                    .addSnapshotListener((d, e) -> {
                        if (e != null || !isAdded()) return;
                        if (d != null && d.exists()) {
                            Boolean active = d.getBoolean("isInternalsActive");
                            isInternalsActive = active != null && active;
                            
                            tvInternalsStatus.setVisibility(isInternalsActive ? View.VISIBLE : View.GONE);
                            btnEndInternals.setVisibility(isInternalsActive ? View.VISIBLE : View.GONE);
                        }
                    });
            }
        });
    }
}
