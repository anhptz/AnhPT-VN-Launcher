package vn.cleanyourtv.dashboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/**
 * Draws crisp vector icons for the top-right status bar (Notification Bell with Badge,
 * Wi-Fi with connection state, LAN/Ethernet with connection state, Wallpaper frame,
 * System Settings Gear) and the bottom-right "All Apps" grid icon.
 */
public final class StatusIconView extends View {

    public static final int ICON_BELL = 1;
    public static final int ICON_WIFI = 2;
    public static final int ICON_LAN = 3;
    public static final int ICON_SETTINGS = 4;
    public static final int ICON_ALL_APPS = 5;
    public static final int ICON_WALLPAPER = 6;

    private final int iconType;
    private boolean connected = false;
    private int badgeCount = 0;

    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgeBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    public StatusIconView(Context context, int iconType) {
        super(context);
        this.iconType = iconType;

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(0xE6FFFFFF);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(0xE6FFFFFF);

        badgeBgPaint.setStyle(Paint.Style.FILL);
        badgeBgPaint.setColor(0xFFEF4444);

        badgeTextPaint.setColor(0xFFFFFFFF);
        badgeTextPaint.setTextAlign(Paint.Align.CENTER);
        badgeTextPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
    }

    public void setConnected(boolean connected) {
        if (this.connected != connected) {
            this.connected = connected;
            invalidate();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void setBadgeCount(int count) {
        if (this.badgeCount != count) {
            this.badgeCount = Math.max(0, count);
            invalidate();
        }
    }

    public int getBadgeCount() {
        return badgeCount;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float size = Math.min(w, h);
        float stroke = Math.max(2.0f, size * 0.085f);
        strokePaint.setStrokeWidth(stroke);

        if (iconType == ICON_BELL) {
            drawBell(canvas, w, h, size, stroke);
        } else if (iconType == ICON_WIFI) {
            drawWifi(canvas, w, h, size, stroke);
        } else if (iconType == ICON_LAN) {
            drawLan(canvas, w, h, size, stroke);
        } else if (iconType == ICON_SETTINGS) {
            drawSettingsGear(canvas, w, h, size, stroke);
        } else if (iconType == ICON_ALL_APPS) {
            drawAllAppsGrid(canvas, w, h, size);
        } else if (iconType == ICON_WALLPAPER) {
            drawWallpaperIcon(canvas, w, h, size, stroke);
        }
    }

    private void drawBell(Canvas canvas, float w, float h, float size, float stroke) {
        strokePaint.setColor(0xF2FFFFFF);
        fillPaint.setColor(0xF2FFFFFF);

        float cx = w * 0.46f;
        float topY = h * 0.20f;
        float bottomY = h * 0.72f;
        float bellW = size * 0.56f;

        path.reset();
        rect.set(cx - bellW * 0.38f, topY, cx + bellW * 0.38f, topY + bellW * 0.72f);
        path.addArc(rect, 180, 180);
        path.lineTo(cx + bellW * 0.48f, bottomY);
        path.lineTo(cx - bellW * 0.48f, bottomY);
        path.close();
        canvas.drawPath(path, strokePaint);

        rect.set(cx - bellW * 0.14f, bottomY, cx + bellW * 0.14f, bottomY + bellW * 0.24f);
        canvas.drawArc(rect, 0, 180, false, strokePaint);

        // Numeric badge in top-right
        float bx = w * 0.75f;
        float by = h * 0.26f;
        float br = size * 0.24f;
        badgeBgPaint.setColor(badgeCount > 0 ? 0xFFEF4444 : 0xFF475569);
        canvas.drawCircle(bx, by, br, badgeBgPaint);

        String txt = badgeCount > 9 ? "9+" : String.valueOf(badgeCount);
        badgeTextPaint.setTextSize(br * 1.25f);
        Paint.FontMetrics fm = badgeTextPaint.getFontMetrics();
        float ty = by - (fm.ascent + fm.descent) * 0.5f;
        canvas.drawText(txt, bx, ty, badgeTextPaint);
    }

    private void drawWifi(Canvas canvas, float w, float h, float size, float stroke) {
        int activeColor = connected ? 0xFF22C55E : 0xFF64748B;
        strokePaint.setColor(connected ? 0xF2FFFFFF : 0xFF64748B);
        fillPaint.setColor(activeColor);

        float cx = w * 0.48f;
        float baseY = h * 0.74f;

        float[] radii = {size * 0.18f, size * 0.33f, size * 0.48f};
        for (float r : radii) {
            rect.set(cx - r, baseY - r, cx + r, baseY + r);
            canvas.drawArc(rect, 225, 90, false, strokePaint);
        }
        canvas.drawCircle(cx, baseY, stroke * 0.75f, fillPaint);

        if (!connected) {
            strokePaint.setColor(0xFFEF4444);
            canvas.drawLine(w * 0.22f, h * 0.22f, w * 0.74f, h * 0.76f, strokePaint);
        }

        badgeBgPaint.setColor(connected ? 0xFF22C55E : 0xFFEF4444);
        canvas.drawCircle(w * 0.80f, h * 0.24f, size * 0.11f, badgeBgPaint);
    }

    private void drawLan(Canvas canvas, float w, float h, float size, float stroke) {
        strokePaint.setColor(connected ? 0xF2FFFFFF : 0xFF64748B);
        strokePaint.setStrokeWidth(stroke * 0.85f);

        float cx = w * 0.48f;
        float topBoxW = size * 0.22f;
        float topBoxH = size * 0.17f;

        rect.set(cx - topBoxW * 0.5f, h * 0.18f, cx + topBoxW * 0.5f, h * 0.18f + topBoxH);
        canvas.drawRoundRect(rect, 2f, 2f, strokePaint);

        float busY = h * 0.50f;
        float leftX = cx - size * 0.26f;
        float rightX = cx + size * 0.26f;
        canvas.drawLine(cx, h * 0.18f + topBoxH, cx, busY, strokePaint);
        canvas.drawLine(leftX, busY, rightX, busY, strokePaint);

        float botTop = h * 0.62f;
        canvas.drawLine(leftX, busY, leftX, botTop, strokePaint);
        canvas.drawLine(rightX, busY, rightX, botTop, strokePaint);

        rect.set(leftX - topBoxW * 0.5f, botTop, leftX + topBoxW * 0.5f, botTop + topBoxH);
        canvas.drawRoundRect(rect, 2f, 2f, strokePaint);

        rect.set(rightX - topBoxW * 0.5f, botTop, rightX + topBoxW * 0.5f, botTop + topBoxH);
        canvas.drawRoundRect(rect, 2f, 2f, strokePaint);

        if (!connected) {
            strokePaint.setColor(0xFFEF4444);
            canvas.drawLine(w * 0.20f, h * 0.22f, w * 0.74f, h * 0.76f, strokePaint);
        }

        badgeBgPaint.setColor(connected ? 0xFF22C55E : 0xFFEF4444);
        canvas.drawCircle(w * 0.80f, h * 0.24f, size * 0.11f, badgeBgPaint);
    }

    private void drawWallpaperIcon(Canvas canvas, float w, float h, float size, float stroke) {
        strokePaint.setColor(0xF2FFFFFF);
        strokePaint.setStrokeWidth(stroke * 0.9f);

        float padX = w * 0.17f;
        float padY = h * 0.21f;
        rect.set(padX, padY, w - padX, h - padY);
        canvas.drawRoundRect(rect, size * 0.10f, size * 0.10f, strokePaint);

        // Sun circle inside frame
        fillPaint.setColor(0xFFFBBF24);
        canvas.drawCircle(w * 0.36f, h * 0.39f, size * 0.075f, fillPaint);

        // Mountain peaks
        path.reset();
        path.moveTo(padX + stroke * 0.5f, h - padY - stroke * 0.4f);
        path.lineTo(w * 0.42f, h * 0.52f);
        path.lineTo(w * 0.55f, h * 0.65f);
        path.lineTo(w * 0.68f, h * 0.45f);
        path.lineTo(w - padX - stroke * 0.5f, h - padY - stroke * 0.4f);
        canvas.drawPath(path, strokePaint);
    }

    private void drawSettingsGear(Canvas canvas, float w, float h, float size, float stroke) {
        strokePaint.setColor(0xF2FFFFFF);
        float cx = w * 0.5f;
        float cy = h * 0.5f;
        float innerR = size * 0.14f;
        float outerR = size * 0.27f;
        float toothOuter = size * 0.36f;

        canvas.drawCircle(cx, cy, innerR, strokePaint);
        canvas.drawCircle(cx, cy, outerR, strokePaint);

        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0;
            float x1 = cx + (float) Math.cos(a) * outerR;
            float y1 = cy + (float) Math.sin(a) * outerR;
            float x2 = cx + (float) Math.cos(a) * toothOuter;
            float y2 = cy + (float) Math.sin(a) * toothOuter;
            canvas.drawLine(x1, y1, x2, y2, strokePaint);
        }
    }

    private void drawAllAppsGrid(Canvas canvas, float w, float h, float size) {
        fillPaint.setColor(0xFFE2E8F0);
        float gridSpan = size * 0.58f;
        float startX = (w - gridSpan) * 0.5f;
        float startY = (h - gridSpan) * 0.5f;
        float cell = gridSpan * 0.26f;
        float step = (gridSpan - cell) * 0.5f;
        float r = cell * 0.28f;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                float x = startX + col * step;
                float y = startY + row * step;
                if (row == 1 && col == 1) {
                    fillPaint.setColor(0xFF60A5FA);
                } else {
                    fillPaint.setColor(0xFFE2E8F0);
                }
                rect.set(x, y, x + cell, y + cell);
                canvas.drawRoundRect(rect, r, r, fillPaint);
            }
        }
    }
}
