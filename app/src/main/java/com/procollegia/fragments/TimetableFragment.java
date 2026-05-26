package com.procollegia.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.procollegia.R;
import com.procollegia.utils.TimetableLoader;

public class TimetableFragment extends Fragment {

    public TimetableFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.content_timetable, container, false);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (!isAdded()) return;

                        if (doc.exists()) {
                            String dept = doc.getString("department");
                            View timetableWidget = root.findViewById(R.id.includeTimetable);
                            if (dept != null && timetableWidget != null) {
                                TimetableLoader.load(timetableWidget, dept, this);
                            }
                        }
                    });
        }

        return root;
    }
}
