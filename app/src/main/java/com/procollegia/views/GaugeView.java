package com.procollegia.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

public class GaugeView extends View {

    private static final float START_ANGLE  = 195f;
    private static final float SWEEP_ANGLE  = 150f;
    private static final float MAX_SCORE    = 1000f;

    private final Paint arcTrackPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcFillPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickActiveP    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needleShadowP  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needleBodyP    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needleTipP     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pivotWhiteP    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pivotBlueP     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);

    private RectF ovalRect = new RectF();
    private int score = 0;

    public GaugeView(Context ctx) { super(ctx); init(); }
    public GaugeView(Context ctx, AttributeSet attrs) { super(ctx, attrs); init(); }
    public GaugeView(Context ctx, AttributeSet attrs, int d) { super(ctx, attrs, d); init(); }

    private void init() {
        arcTrackPaint.setStyle(Paint.Style.STROKE);
        arcTrackPaint.setStrokeCap(Paint.Cap.ROUND);
        arcTrackPaint.setColor(Color.parseColor("#D1D9E6"));

        arcFillPaint.setStyle(Paint.Style.STROKE);
        arcFillPaint.setStrokeCap(Paint.Cap.ROUND);

        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(2.5f);
        tickPaint.setColor(Color.parseColor("#B8BEC9"));
        tickPaint.setStrokeCap(Paint.Cap.ROUND);

        // Tick marks (active — white)
        tickActiveP.setStyle(Paint.Style.STROKE);
        tickActiveP.setStrokeWidth(2.5f);
        tickActiveP.setColor(Color.parseColor("#FFFFFF"));
        tickActiveP.setAlpha(200);
        tickActiveP.setStrokeCap(Paint.Cap.ROUND);

        // Needle shadow
        needleShadowP.setStyle(Paint.Style.STROKE);
        needleShadowP.setStrokeWidth(9f);
        needleShadowP.setColor(Color.parseColor("#40000000"));
        needleShadowP.setStrokeCap(Paint.Cap.ROUND);

        // Needle body (white)
        needleBodyP.setStyle(Paint.Style.STROKE);
        needleBodyP.setStrokeWidth(7f);
        needleBodyP.setColor(Color.WHITE);
        needleBodyP.setStrokeCap(Paint.Cap.ROUND);

        // Needle tip gradient will be applied per draw
        needleTipP.setStyle(Paint.Style.STROKE);
        needleTipP.setStrokeWidth(7f);
        needleTipP.setColor(Color.parseColor("#D0D8E8"));
        needleTipP.setStrokeCap(Paint.Cap.ROUND);

        // Pivot outer
        pivotWhiteP.setStyle(Paint.Style.FILL);
        pivotWhiteP.setColor(Color.WHITE);
        pivotWhiteP.setShadowLayer(8f, 0f, 3f, Color.parseColor("#44000000"));
        setLayerType(LAYER_TYPE_SOFTWARE, null); // needed for shadow

        // Pivot inner
        pivotBlueP.setStyle(Paint.Style.FILL);
        pivotBlueP.setColor(Color.parseColor("#4A90D9"));

        // Labels
        labelPaint.setTextSize(30f);
        labelPaint.setColor(Color.parseColor("#718096"));
        labelPaint.setFakeBoldText(false);
    }

    public void setScore(int s) {
        score = Math.max(0, Math.min(s, (int) MAX_SCORE));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w   = getWidth();
        float h   = getHeight();
        float cx  = w / 2f;
        float cy  = h * 0.62f;
        float r   = Math.min(w * 0.42f, cy * 0.85f);
        float strokeW = r * 0.14f;

        arcTrackPaint.setStrokeWidth(strokeW);
        arcFillPaint.setStrokeWidth(strokeW);

        ovalRect.set(cx - r, cy - r, cx + r, cy + r);

        // ── 1. Arc track (background) ──────────────────────────────────
        canvas.drawArc(ovalRect, START_ANGLE, SWEEP_ANGLE, false, arcTrackPaint);

        // ── 2. Gradient fill arc ────────────────────────────────────────
        float fillSweep = (score / MAX_SCORE) * SWEEP_ANGLE;

        // Build a sweep gradient aligned to our arc angles
        SweepGradient sg = new SweepGradient(cx, cy,
                new int[]{
                        Color.parseColor("#A8D4F8"),  // light blue at start
                        Color.parseColor("#4A90D9"),  // mid
                        Color.parseColor("#1A4F8A"),  // dark blue at end
                        Color.parseColor("#1A4F8A"),  // pad rest of 360
                        Color.parseColor("#A8D4F8")   // wrap
                },
                new float[]{0f, 0.35f, 0.80f, 0.81f, 1f}
        );
        Matrix gradMatrix = new Matrix();
        gradMatrix.preRotate(START_ANGLE, cx, cy);
        sg.setLocalMatrix(gradMatrix);
        arcFillPaint.setShader(sg);

        if (fillSweep > 0) {
            canvas.drawArc(ovalRect, START_ANGLE, fillSweep, false, arcFillPaint);
        }

        // ── 3. Tick marks ───────────────────────────────────────────────
        int ticks = 20;
        float innerTickR  = r - strokeW * 0.5f - 8f;
        float outerTickR  = r + strokeW * 0.5f + 8f;
        float activeLimit = (score / MAX_SCORE) * ticks;

        for (int i = 0; i <= ticks; i++) {
            float angle = (float) Math.toRadians(START_ANGLE + (SWEEP_ANGLE / ticks) * i);
            float ix = (float)(cx + innerTickR * Math.cos(angle));
            float iy = (float)(cy + innerTickR * Math.sin(angle));
            float ox = (float)(cx + outerTickR * Math.cos(angle));
            float oy = (float)(cy + outerTickR * Math.sin(angle));
            canvas.drawLine(ix, iy, ox, oy, i <= activeLimit ? tickActiveP : tickPaint);
        }

        // ── 4. Needle ───────────────────────────────────────────────────
        float needleAngleDeg = START_ANGLE + fillSweep;
        float needleAngleRad = (float) Math.toRadians(needleAngleDeg);
        float baseR   =  r * 0.18f;  // short tail behind center
        float tipR    =  r * 0.72f;  // tip length

        float bx = (float)(cx - baseR * Math.cos(needleAngleRad));
        float by = (float)(cy - baseR * Math.sin(needleAngleRad));
        float tx = (float)(cx + tipR  * Math.cos(needleAngleRad));
        float ty = (float)(cy + tipR  * Math.sin(needleAngleRad));

        // Shadow offset
        canvas.drawLine(bx + 3, by + 4, tx + 3, ty + 4, needleShadowP);
        // Tip portion (slightly grey)
        canvas.drawLine(cx, cy, tx, ty, needleTipP);
        // White body
        canvas.drawLine(bx, by, cx, cy, needleBodyP);

        // ── 5. Pivot circle ─────────────────────────────────────────────
        canvas.drawCircle(cx, cy, r * 0.115f, pivotWhiteP);
        canvas.drawCircle(cx, cy, r * 0.055f, pivotBlueP);

        // ── 6. 0 / 1000 labels ──────────────────────────────────────────
        float labelR = r + strokeW + 26f;
        float startRad  = (float) Math.toRadians(START_ANGLE);
        float endRad    = (float) Math.toRadians(START_ANGLE + SWEEP_ANGLE);

        labelPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("0",
                (float)(cx + labelR * Math.cos(startRad)),
                (float)(cy + labelR * Math.sin(startRad)) + labelPaint.getTextSize() * 0.3f,
                labelPaint);

        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("1000",
                (float)(cx + labelR * Math.cos(endRad)),
                (float)(cy + labelR * Math.sin(endRad)) + labelPaint.getTextSize() * 0.3f,
                labelPaint);
    }
}
