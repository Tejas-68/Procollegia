package com.procollegia.utils;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.procollegia.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Loads the department timetable image into any dashboard view (widget_timetable_viewer.xml).
 *
 * The timetable is only shown if it was uploaded TODAY. If it's stale or missing, the
 * empty state is displayed and the onMissingCallback is invoked so the caller can trigger
 * a local notification reminding teachers to upload.
 *
 * Usage:
 *   TimetableLoader.load(root.findViewById(R.id.includeTimetable), "BCA", this,
 *       () -> NotificationHelper.notifyUploadReminder(requireContext(), "BCA"));
 */
public class TimetableLoader {

    /**
     * Callback fired when no valid timetable exists for today.
     * Use this to show a local notification to teachers.
     */
    public interface OnMissingListener {
        void onTimetableMissing();
    }

    /** Load without a missing callback. */
    public static void load(View timetableRoot, String department,
                            androidx.fragment.app.Fragment fragment) {
        load(timetableRoot, department, fragment, null);
    }

    /**
     * @param timetableRoot   Inflated widget_timetable_viewer root view
     * @param department      e.g. "BCA" (case-insensitive)
     * @param fragment        Calling fragment (Glide lifecycle)
     * @param onMissing       Called if no timetable exists for today (nullable)
     */
    public static void load(View timetableRoot, String department,
                            androidx.fragment.app.Fragment fragment,
                            OnMissingListener onMissing) {
        if (timetableRoot == null || department == null || department.trim().isEmpty() || !fragment.isAdded()) {
            if (onMissing != null) onMissing.onTimetableMissing();
            return;
        }

        ImageView    ivImage  = timetableRoot.findViewById(R.id.ivTimetableImage);
        LinearLayout llEmpty  = timetableRoot.findViewById(R.id.llTimetableEmpty);
        TextView     tvUpload = timetableRoot.findViewById(R.id.tvTimetableUploaded);

        String dept  = department.trim().toUpperCase();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(new Date());

        FirebaseFirestore.getInstance()
                .collection("timetables")
                .document(dept)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!fragment.isAdded()) return;

                    boolean hasTodayTimetable = false;

                    if (doc.exists()) {
                        String imageUrl    = doc.getString("imageUrl");
                        String uploadDate  = doc.getString("uploadDate");
                        String uploadedBy  = doc.getString("uploadedBy");
                        com.google.firebase.Timestamp ts = doc.getTimestamp("uploadedAt");

                        // Only show if uploaded TODAY
                        boolean isFresh = today.equals(uploadDate);

                        if (imageUrl != null && !imageUrl.isEmpty() && isFresh) {
                            hasTodayTimetable = true;

                            llEmpty.setVisibility(View.GONE);
                            ivImage.setVisibility(View.VISIBLE);

                            Glide.with(fragment)
                                    .load(imageUrl)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .into(ivImage);

                            // Show subtle "uploaded by X" text
                            if (tvUpload != null && uploadedBy != null) {
                                String dateStr = ts != null
                                        ? new SimpleDateFormat("h:mm a", Locale.ENGLISH).format(ts.toDate())
                                        : "";
                                tvUpload.setText("by " + uploadedBy.split(" ")[0]
                                        + (dateStr.isEmpty() ? "" : " · " + dateStr));
                            }
                        }
                    }

                    if (!hasTodayTimetable) {
                        // Show empty state
                        llEmpty.setVisibility(View.VISIBLE);
                        ivImage.setVisibility(View.GONE);
                        if (tvUpload != null) tvUpload.setText("");

                        // Notify caller that timetable is missing for today
                        if (onMissing != null) onMissing.onTimetableMissing();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!fragment.isAdded()) return;
                    llEmpty.setVisibility(View.VISIBLE);
                    ivImage.setVisibility(View.GONE);
                    if (onMissing != null) onMissing.onTimetableMissing();
                });
    }
}
