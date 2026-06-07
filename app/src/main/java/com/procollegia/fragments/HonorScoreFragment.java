package com.procollegia.fragments;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.procollegia.R;
import com.procollegia.adapters.HonorEventAdapter;
import com.procollegia.views.GaugeView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Standalone fragment that renders inside the Honor Score tab of StudentAcademicsFragment.
 * Fetches data from Firestore:
 *   users/{uid}/honorScore  → int
 *   users/{uid}/honorHistory → array of {month, score}
 *   users/{uid}/honorEvents  → subcollection ordered by timestamp desc
 */
public class HonorScoreFragment extends androidx.fragment.app.Fragment {

    private GaugeView gaugeView;
    private TextView tvScore, tvTier;
    private ShimmerFrameLayout shimmerGauge, shimmerEvents;
    private View llGaugeContent;
    private RecyclerView rvEvents;
    private LineChart lineChart;

    private FirebaseFirestore db;
    private String uid;

    public HonorScoreFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.content_honor_score, container, false);

        gaugeView      = root.findViewById(R.id.gaugeView);
        tvScore        = root.findViewById(R.id.tvHonorScore);
        tvTier         = root.findViewById(R.id.tvTierBadge);
        shimmerGauge   = root.findViewById(R.id.shimmerGauge);
        shimmerEvents  = root.findViewById(R.id.shimmerEvents);
        llGaugeContent = root.findViewById(R.id.llGaugeContent);
        rvEvents       = root.findViewById(R.id.rvEvents);
        lineChart      = root.findViewById(R.id.lineChart);

        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        rvEvents.setNestedScrollingEnabled(false);

        // Start shimmers
        shimmerGauge.startShimmer();
        shimmerEvents.startShimmer();

        db  = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (uid != null) {
            loadHonorScore();
            loadEvents();
        } else {
            // Fallback mock data
            renderScore(720);
            renderHistory(new int[]{450, 520, 580, 630, 680, 720});
            renderEvents(mockEvents());
        }

        return root;
    }

    // ────────────────────────────────────────────────────────────────
    //  Firestore fetches
    // ────────────────────────────────────────────────────────────────

    private void loadHonorScore() {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    int score = 500; // default
                    if (doc.contains("honorScore")) {
                        Object raw = doc.get("honorScore");
                        if (raw instanceof Long)   score = ((Long) raw).intValue();
                        if (raw instanceof Integer) score = (Integer) raw;
                    }

                    // Build history from the doc as well (array field)
                    int[] history = new int[]{400, 450, 500, 520, 550, score};
                    if (doc.contains("honorHistory")) {
                        try {
                            List<?> rawList = (List<?>) doc.get("honorHistory");
                            if (rawList != null && rawList.size() == 6) {
                                history = new int[6];
                                for (int i = 0; i < 6; i++) {
                                    Object val = rawList.get(i);
                                    history[i] = val instanceof Long ? ((Long) val).intValue() : (Integer) val;
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    final int finalScore = score;
                    final int[] finalHistory = history;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            shimmerGauge.stopShimmer();
                            shimmerGauge.setVisibility(View.GONE);
                            llGaugeContent.setVisibility(View.VISIBLE);
                            renderScore(finalScore);
                            renderHistory(finalHistory);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    // Show mock data on error
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            shimmerGauge.stopShimmer();
                            shimmerGauge.setVisibility(View.GONE);
                            llGaugeContent.setVisibility(View.VISIBLE);
                            renderScore(720);
                            renderHistory(new int[]{450, 520, 580, 630, 680, 720});
                        });
                    }
                });
    }

    private void loadEvents() {
        db.collection("users").document(uid)
                .collection("honorEvents")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(6)
                .get()
                .addOnSuccessListener(qs -> {
                    List<HonorEventAdapter.HonorEvent> events = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : qs) {
                        int pts = 0;
                        Object rawPts = doc.get("points");
                        if (rawPts instanceof Long) pts = ((Long) rawPts).intValue();
                        String desc = doc.getString("description") != null
                                ? doc.getString("description") : "Event";
                        String date = doc.getString("date") != null
                                ? doc.getString("date") : "";
                        events.add(new HonorEventAdapter.HonorEvent(pts, desc, date));
                    }
                    if (events.isEmpty()) events = mockEvents();
                    final List<HonorEventAdapter.HonorEvent> finalEvents = events;

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> renderEvents(finalEvents));
                    }
                })
                .addOnFailureListener(e -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> renderEvents(mockEvents()));
                    }
                });
    }

    // ────────────────────────────────────────────────────────────────
    //  Render helpers
    // ────────────────────────────────────────────────────────────────

    private void renderScore(int score) {
        // Animate gauge needle and number counter
        ValueAnimator anim = ValueAnimator.ofInt(0, score);
        anim.setDuration(1400);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            int val = (int) a.getAnimatedValue();
            gaugeView.setScore(val);
            tvScore.setText(String.valueOf(val));
        });
        anim.start();

        // Tier badge
        if (score >= 750) {
            tvTier.setText("  GOLD TIER");
            tvTier.setBackgroundResource(R.drawable.bg_tier_gold);
        } else if (score >= 500) {
            tvTier.setText("  SILVER TIER");
            tvTier.setBackgroundResource(R.drawable.bg_tier_silver);
        } else {
            tvTier.setText("  BRONZE TIER");
            tvTier.setBackgroundResource(R.drawable.bg_tier_bronze);
        }
    }

    private void renderHistory(int[] scores) {
        String[] months = {"Sep", "Oct", "Nov", "Dec", "Jan", "Feb"};
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < scores.length; i++) {
            entries.add(new Entry(i, scores[i]));
        }

        LineDataSet ds = new LineDataSet(entries, "Honor Score");
        ds.setColor(Color.parseColor("#4A90D9"));
        ds.setCircleColor(Color.parseColor("#4A90D9"));
        ds.setLineWidth(2.5f);
        ds.setCircleRadius(4f);
        ds.setDrawValues(false);
        ds.setDrawFilled(true);
        ds.setFillColor(Color.parseColor("#4A90D9"));
        ds.setFillAlpha(40);
        ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        lineChart.setData(new LineData(ds));
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setTouchEnabled(false);
        lineChart.setBackgroundColor(Color.TRANSPARENT);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(months));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGridColor(Color.parseColor("#D1D9E6"));
        xAxis.setTextColor(Color.parseColor("#718096"));
        xAxis.setDrawAxisLine(false);

        lineChart.getAxisLeft().setGridColor(Color.parseColor("#D1D9E6"));
        lineChart.getAxisLeft().setTextColor(Color.parseColor("#718096"));
        lineChart.getAxisRight().setEnabled(false);

        lineChart.animateX(1200);
        lineChart.invalidate();
    }

    private void renderEvents(List<HonorEventAdapter.HonorEvent> events) {
        shimmerEvents.stopShimmer();
        shimmerEvents.setVisibility(View.GONE);
        rvEvents.setVisibility(View.VISIBLE);
        rvEvents.setAdapter(new HonorEventAdapter(events));
    }

    private List<HonorEventAdapter.HonorEvent> mockEvents() {
        return Arrays.asList(
                new HonorEventAdapter.HonorEvent(10, "Good Conduct",          "Feb 20"),
                new HonorEventAdapter.HonorEvent(15, "Sports Win",             "Feb 15"),
                new HonorEventAdapter.HonorEvent(-5, "Late Submission",        "Feb 10"),
                new HonorEventAdapter.HonorEvent(10, "Academic Achievement",   "Feb 5"),
                new HonorEventAdapter.HonorEvent(20, "Event Participation",    "Jan 28"),
                new HonorEventAdapter.HonorEvent(-10, "Disciplinary Action",   "Jan 20")
        );
    }
}
