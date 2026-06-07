package com.procollegia.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.procollegia.HodTakeAttendanceActivity;
import com.procollegia.R;

public class HodAttendanceSubFragment extends Fragment {

    private FirebaseFirestore db;

    public HodAttendanceSubFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_hod_attendance_sub, container, false);

        db = FirebaseFirestore.getInstance();

        root.findViewById(R.id.btnUniversalAttendance).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Loading scanner...", Toast.LENGTH_SHORT).show();
            db.collection("users").whereEqualTo("role", "Student").get()
                .addOnSuccessListener(qs -> {
                    java.util.HashMap<String, String> map = new java.util.HashMap<>();
                    for (QueryDocumentSnapshot doc : qs) {
                        String uucms = doc.getString("uucmsId");
                        String name = doc.getString("name");
                        if (uucms != null) map.put(uucms, name);
                    }
                    Intent intent = new Intent(requireContext(), com.procollegia.AttendanceScannerActivity.class);
                    intent.putExtra("uucmsMap", map);
                    startActivity(intent);
                });
        });

        root.findViewById(R.id.fabTakeAttendance).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), HodTakeAttendanceActivity.class)));

        return root;
    }
}
