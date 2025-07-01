package org.telegram.ui.Profile;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Efficient metaball renderer: draws two morphing circles ("blobs") that merge smoothly as they approach,
 * using a single filled path (no overdrawing).
 *
 * Optimized to reuse geometry objects (no new allocations during draw).
 */
public final class MetaBallRenderer {

    private static final Paint PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path  PATH  = new Path();

    private static final float HANDLE_SIZE = 2.4f;

    // ── Reused geometry objects ──
    private static final RectF OVAL1 = new RectF();
    private static final RectF OVAL2 = new RectF();

    // Arrays used as mutable (x, y) point buffers
    private static final float[] p1 = new float[2];
    private static final float[] p2 = new float[2];
    private static final float[] p3 = new float[2];
    private static final float[] p4 = new float[2];
    private static final float[] h1 = new float[2];
    private static final float[] h2 = new float[2];
    private static final float[] h3 = new float[2];
    private static final float[] h4 = new float[2];

    static {
        PAINT.setStyle(Paint.Style.FILL);
    }

    private MetaBallRenderer() {}

    /**
     * Draws two animated circles with a smooth liquid-like connector as they approach.
     * All geometry is computed in-place to minimize allocations.
     */
    public static void drawMorph(Canvas canvas,
                                 float cx1, float cy1, float r1,
                                 float cx2, float cy2, float r2,
                                 int color) {
        PAINT.setColor(color);
        PATH.reset();

        final float dx = cx2 - cx1;
        final float dy = cy2 - cy1;
        final float d  = (float) Math.hypot(dx, dy);
        final float maxDistance = (r1 + r2) * 1.2f;

        // ── CASE A: Too far → no connection ──
        if (d > maxDistance) {
            PATH.addCircle(cx1, cy1, r1, Path.Direction.CW);
            PATH.addCircle(cx2, cy2, r2, Path.Direction.CW);
            canvas.drawPath(PATH, PAINT);
            return;
        }

        // ── CASE B: One fully contains the other → draw the larger only ──
        if (d <= Math.abs(r1 - r2)) {
            float bigCx = r1 >= r2 ? cx1 : cx2;
            float bigCy = r1 >= r2 ? cy1 : cy2;
            float bigR  = r1 >= r2 ? r1  : r2;
            PATH.addCircle(bigCx, bigCy, bigR, Path.Direction.CW);
            canvas.drawPath(PATH, PAINT);
            return;
        }

        // ── CASE C: Construct connector ──

        // Compute dynamic blending factor (ease-out curve)
        final float minDistance = Math.abs(r1 - r2);
        float t = (d - minDistance) / (maxDistance - minDistance);
        t = clamp(t, 0f, 1f);
        final float V = 1f - t * t;

        final float theta     = (float) Math.atan2(dy, dx);
        final float halfPI    = (float) (Math.PI * 0.5);
        final float maxSpread = (float) Math.acos(clamp((r1 - r2) / d, -1f, 1f));

        // Angles for tangent points
        final float u1 = (float) Math.acos(clamp((r1*r1 + d*d - r2*r2) / (2f * r1 * d), -1, 1));
        final float u2 = (float) Math.acos(clamp((r2*r2 + d*d - r1*r1) / (2f * r2 * d), -1, 1));

        final float a1 = theta + u1 + (maxSpread - u1) * V;
        final float a2 = theta - u1 - (maxSpread - u1) * V;
        final float a3 = theta + (float) Math.PI - u2 - ((float) Math.PI - u2 - maxSpread) * V;
        final float a4 = theta - (float) Math.PI + u2 + ((float) Math.PI - u2 - maxSpread) * V;

        // Tangent points on the circles
        polar(cx1, cy1, a1, r1, p1);
        polar(cx1, cy1, a2, r1, p2);
        polar(cx2, cy2, a3, r2, p3);
        polar(cx2, cy2, a4, r2, p4);

        // Bezier control handle lengths
        final float totalR  = r1 + r2;
        final float d2Base  = Math.min(V * HANDLE_SIZE, distance(p1, p3) / totalR);
        final float d2      = d2Base * Math.min(1f, (d * 2f) / totalR);
        final float r1h     = r1 * d2;
        final float r2h     = r2 * d2;

        // Control points for cubic Beziers
        polar(p1[0], p1[1], a1 - halfPI, r1h, h1);
        polar(p2[0], p2[1], a2 + halfPI, r1h, h2);
        polar(p3[0], p3[1], a3 + halfPI, r2h, h3);
        polar(p4[0], p4[1], a4 - halfPI, r2h, h4);

        // Define oval bounds for arcTo
        OVAL1.set(cx1 - r1, cy1 - r1, cx1 + r1, cy1 + r1);
        OVAL2.set(cx2 - r2, cy2 - r2, cx2 + r2, cy2 + r2);

        // Build connector path (bridge + circle arcs)
        PATH.moveTo(p1[0], p1[1]);
        PATH.cubicTo(h1[0], h1[1], h3[0], h3[1], p3[0], p3[1]);
        PATH.arcTo(OVAL2, deg(a3), sweepCW(a3, a4), false);
        PATH.cubicTo(h4[0], h4[1], h2[0], h2[1], p2[0], p2[1]);
        PATH.arcTo(OVAL1, deg(a2), sweepCW(a2, a1), false);
        PATH.close();

        // Add full circles (to ensure consistent fill)
        PATH.addCircle(cx1, cy1, r1, Path.Direction.CW);
        PATH.addCircle(cx2, cy2, r2, Path.Direction.CW);

        canvas.drawPath(PATH, PAINT);
    }

    // ────────────────────────────────────────────────────────────────
    //  Helper methods
    // ────────────────────────────────────────────────────────────────

    private static float clamp(float v, float min, float max) {
        return v < min ? min : Math.min(v, max);
    }

    private static float distance(float[] a, float[] b) {
        return (float) Math.hypot(b[0] - a[0], b[1] - a[1]);
    }

    /** Writes polar coordinate to (out[0], out[1]) */
    private static void polar(float cx, float cy, float angle, float radius, float[] out) {
        out[0] = cx + radius * (float) Math.cos(angle);
        out[1] = cy + radius * (float) Math.sin(angle);
    }

    private static float deg(float rad) {
        return (float) Math.toDegrees(rad);
    }

    /** Computes clockwise angular sweep from start to end (in radians → degrees). */
    private static float sweepCW(float start, float end) {
        float diff = end - start;
        while (diff < 0f) diff += Math.PI * 2f;
        return (float) Math.toDegrees(diff);
    }
}

