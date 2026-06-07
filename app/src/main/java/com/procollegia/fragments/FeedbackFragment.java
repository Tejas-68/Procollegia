package com.procollegia.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.procollegia.R;
import com.procollegia.adapters.FeedbackAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Feedback / Complaint fragment (loaded inside the Academics "Feedback" tab).
 *
 * Firestore schema:
 *   feedback/{id}
 *     studentId, studentName (or "Anonymous"), category, recipient,
 *     subject, message, date, status ("pending"), createdAt (timestamp)
 */
public class FeedbackFragment extends Fragment {

    // Category chips
    private TextView chipComplaint, chipFeedback, chipSuggestion, chipQuery;
    private TextView[] categoryChips;
    private String selectedCategory = "Complaint";

    // Recipient chips
    private TextView chipToHOD, chipToTeacher, chipToPrincipal;
    private TextView[] recipientChips;
    private String selectedRecipient = "HOD";

    // Form
    private EditText        etSubject, etMessage;
    private TextView        tvCharCount, btnSubmit;
    private SwitchMaterial  switchAnon;

    // History
    private View               tvPastHeader;
    private ShimmerFrameLayout shimmer;
    private RecyclerView       rvPast;

    private final List<FeedbackAdapter.FeedbackItem> pastList = new ArrayList<>();

    private FirebaseFirestore db;
    private String uid, studentName;

    public FeedbackFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.content_feedback, container, false);

        // Category chips
        chipComplaint  = root.findViewById(R.id.chipComplaint);
        chipFeedback   = root.findViewById(R.id.chipFeedback);
        chipSuggestion = root.findViewById(R.id.chipSuggestion);
        chipQuery      = root.findViewById(R.id.chipQuery);
        categoryChips  = new TextView[]{chipComplaint, chipFeedback, chipSuggestion, chipQuery};
        String[] catLabels = {"Complaint", "Feedback", "Suggestion", "Query"};
        for (int i = 0; i < categoryChips.length; i++) {
            final String label = catLabels[i];
            categoryChips[i].setOnClickListener(v -> selectCategory(label));
        }

        // Recipient chips
        chipToHOD       = root.findViewById(R.id.chipToHOD);
        chipToTeacher   = root.findViewById(R.id.chipToTeacher);
        chipToPrincipal = root.findViewById(R.id.chipToPrincipal);
        recipientChips  = new TextView[]{chipToHOD, chipToTeacher, chipToPrincipal};
        String[] recLabels = {"HOD", "Class Teacher", "Principal"};
        for (int i = 0; i < recipientChips.length; i++) {
            final String label = recLabels[i];
            recipientChips[i].setOnClickListener(v -> selectRecipient(label));
        }

        // Form fields
        etSubject   = root.findViewById(R.id.etFeedbackSubject);
        etMessage   = root.findViewById(R.id.etFeedbackMessage);
        tvCharCount = root.findViewById(R.id.tvCharCount);
        switchAnon  = root.findViewById(R.id.switchAnonymous);
        btnSubmit   = root.findViewById(R.id.btnSubmitFeedback);

        // History views
        tvPastHeader = root.findViewById(R.id.tvPastHeader);
        shimmer      = root.findViewById(R.id.shimmerFeedback);
        rvPast       = root.findViewById(R.id.rvPastFeedback);
        rvPast.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPast.setNestedScrollingEnabled(false);

        // Char counter
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {
                int len = s.length();
                tvCharCount.setText(len + " / 500");
                tvCharCount.setTextColor(len > 450
                        ? ContextCompat.getColor(requireContext(), R.color.accent_red)
                        : ContextCompat.getColor(requireContext(), R.color.text_muted));
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Submit
        btnSubmit.setOnClickListener(v -> submitFeedback());

        // Firebase
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (uid != null) {
            fetchStudentName();
            loadPastFeedback();
        }

        return root;
    }

    // ── Chip selection ───────────────────────────────────────────────────────

    private void selectCategory(String label) {
        selectedCategory = label;
        String[] labels = {"Complaint", "Feedback", "Suggestion", "Query"};
        for (int i = 0; i < categoryChips.length; i++) {
            boolean sel = labels[i].equals(label);
            categoryChips[i].setBackgroundResource(sel
                    ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            categoryChips[i].setTextColor(ContextCompat.getColor(requireContext(),
                    sel ? R.color.accent_blue : R.color.text_secondary));
        }
    }

    private void selectRecipient(String label) {
        selectedRecipient = label;
        String[] labels = {"HOD", "Class Teacher", "Principal"};
        for (int i = 0; i < recipientChips.length; i++) {
            boolean sel = labels[i].equals(label);
            recipientChips[i].setBackgroundResource(sel
                    ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            recipientChips[i].setTextColor(ContextCompat.getColor(requireContext(),
                    sel ? R.color.accent_blue : R.color.text_secondary));
        }
    }

    // ── Submit ───────────────────────────────────────────────────────────────

    private void submitFeedback() {
        String subject = etSubject.getText().toString().trim();
        String message = etMessage.getText().toString().trim();

        if (subject.isEmpty()) {
            etSubject.setError("Please enter a subject");
            etSubject.requestFocus();
            return;
        }
        if (message.isEmpty()) {
            etMessage.setError("Please enter a message");
            etMessage.requestFocus();
            return;
        }
        if (message.length() > 500) {
            Toast.makeText(getContext(), "Message too long (max 500 chars)", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean anon = switchAnon.isChecked();
        String name  = anon ? "Anonymous" : (studentName != null ? studentName : "Student");
        String today = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());

        // Disable button to prevent double-submit
        btnSubmit.setAlpha(0.5f);
        btnSubmit.setEnabled(false);

        Map<String, Object> doc = new HashMap<>();
        doc.put("studentId",   anon ? "anonymous" : uid);
        doc.put("studentName", name);
        doc.put("category",    selectedCategory);
        doc.put("recipient",   selectedRecipient);
        doc.put("subject",     subject);
        doc.put("message",     message);
        doc.put("date",        today);
        doc.put("status",      "pending");
        doc.put("createdAt",   FieldValue.serverTimestamp());

        db.collection("feedback").add(doc)
                .addOnSuccessListener(ref -> {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(),
                                selectedCategory + " submitted successfully! ",
                                Toast.LENGTH_LONG).show();

                        // Clear form
                        etSubject.setText("");
                        etMessage.setText("");
                        tvCharCount.setText("0 / 500");
                        btnSubmit.setAlpha(1f);
                        btnSubmit.setEnabled(true);

                        // Prepend to past list
                        pastList.add(0, new FeedbackAdapter.FeedbackItem(
                                selectedCategory, selectedRecipient,
                                subject, message, today, "pending"));
                        tvPastHeader.setVisibility(View.VISIBLE);
                        rvPast.setVisibility(View.VISIBLE);
                        rvPast.getAdapter().notifyItemInserted(0);
                        rvPast.scrollToPosition(0);
                    });
                })
                .addOnFailureListener(e -> {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Submission failed. Try again.", Toast.LENGTH_SHORT).show();
                        btnSubmit.setAlpha(1f);
                        btnSubmit.setEnabled(true);
                    });
                });
    }

    // ── Past submissions ─────────────────────────────────────────────────────

    private void loadPastFeedback() {
        shimmer.startShimmer();
        tvPastHeader.setVisibility(View.VISIBLE);

        db.collection("feedback")
                .whereEqualTo("studentId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(qs -> {
                    for (QueryDocumentSnapshot d : qs) {
                        pastList.add(new FeedbackAdapter.FeedbackItem(
                                nvl(d.getString("category"),  "Feedback"),
                                nvl(d.getString("recipient"), "HOD"),
                                nvl(d.getString("subject"),   "—"),
                                nvl(d.getString("message"),   ""),
                                nvl(d.getString("date"),      ""),
                                nvl(d.getString("status"),    "pending")));
                    }
                    if (getActivity() != null)
                        getActivity().runOnUiThread(this::renderPast);
                })
                .addOnFailureListener(e -> {
                    if (getActivity() != null)
                        getActivity().runOnUiThread(this::renderPast);
                });
    }

    private void renderPast() {
        shimmer.stopShimmer();
        shimmer.setVisibility(View.GONE);
        if (!pastList.isEmpty()) {
            tvPastHeader.setVisibility(View.VISIBLE);
            rvPast.setVisibility(View.VISIBLE);
            rvPast.setAdapter(new FeedbackAdapter(pastList));
        } else {
            tvPastHeader.setVisibility(View.GONE);
        }
    }

    private void fetchStudentName() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(d -> studentName = nvl(d.getString("name"), "Student"));
    }

    private String nvl(String s, String def) { return s != null ? s : def; }
}
