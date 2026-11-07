package vn.cleanyourtv.dashboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * Crisp vector-style weather icon view matching the CleanYourTV Vietnamese dashboard aesthetic.
 */
public final class WeatherIconView extends View {

    public static final int TYPE_SUN = 0;
    public static final int TYPE_PARTLY_CLOUDY = 1;
    public static final int TYPE_CLOUDY = 2;
    public static final int TYPE_RAIN = 3;
    public static final int TYPE_STORM = 4;

    private int iconType = TYPE_SUN;
    private final Paint sunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cloudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path cloudPath = new Path();
    private final RectF arcRect = new RectF();

    public WeatherIconView(Context context, int initialType) {
        super(context);
        this.iconType = initialType;

        sunPaint.setStyle(Paint.Style.STROKE);
        sunPaint.setColor(0xFFF59E0B); // Warm amber-orange like screenshot
        sunPaint.setStrokeCap(Paint.Cap.ROUND);

        cloudPaint.setStyle(Paint.Style.STROKE);
        cloudPaint.setColor(0xFFD1D5DB); // Crisp silver-white
        cloudPaint.setStrokeCap(Paint.Cap.ROUND);
        cloudPaint.setStrokeJoin(Paint.Join.ROUND);

        rainPaint.setStyle(Paint.Style.STROKE);
        rainPaint.setColor(0xFF93C5FD); // Soft sky blue
        rainPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setIconType(int type) {
        if (this.iconType != type) {
            this.iconType = type;
            invalidate();
        }
    }

    public static int fromWmoCode(int code) {
        if (code == 0 || code == 1) return TYPE_SUN;
        if (code == 2) return TYPE_PARTLY_CLOUDY;
        if (code == 3 || code == 45 || code == 48) return TYPE_CLOUDY;
        if (code >= 95) return TYPE_STORM;
        return TYPE_RAIN;
    }

    public static String conditionTextFromWmo(int code) {
        if (code == 0) return "Trời quang";
        if (code == 1) return "Ít mây";
        if (code == 2) return "Có mây";
        if (code == 3) return "Nhiều mây";
        if (code == 45 || code == 48) return "Sương mù";
        if (code >= 51 && code <= 57) return "Mưa phùn";
        if (code >= 61 && code <= 67) return "Có mưa";
        if (code >= 80 && code <= 82) return "Mưa rào";
        if (code >= 95) return "Mưa dông";
        return "Trời quang";
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float size = Math.min(w, h);
        float stroke = Math.max(2.2f, size * 0.068f);
        sunPaint.setStrokeWidth(stroke);
        cloudPaint.setStrokeWidth(stroke);
        rainPaint.setStrokeWidth(stroke * 0.9f);

        float cx = w * 0.5f;
        float cy = h * 0.5f;

        if (iconType == TYPE_SUN) {
            float r = size * 0.22f;
            canvas.drawCircle(cx, cy, r, sunPaint);
            float rayInner = size * 0.31f;
            float rayOuter = size * 0.41f;
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4.0;
                float x1 = cx + (float) Math.cos(angle) * rayInner;
                float y1 = cy + (float) Math.sin(angle) * rayInner;
                float x2 = cx + (float) Math.cos(angle) * rayOuter;
                float y2 = cy + (float) Math.sin(angle) * rayOuter;
                canvas.drawLine(x1, y1, x2, y2, sunPaint);
            }
        } else if (iconType == TYPE_PARTLY_CLOUDY) {
            // Small sun at top-left/right peeking out
            float sx = w * 0.36f;
            float sy = h * 0.34f;
            float sr = size * 0.14f;
            canvas.drawCircle(sx, sy, sr, sunPaint);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4.0;
                if (a > 0.2 && a < 2.2) continue; // hidden behind cloud
                float x1 = sx + (float) Math.cos(a) * (sr * 1.35f);
                float y1 = sy + (float) Math.sin(a) * (sr * 1.35f);
                float x2 = sx + (float) Math.cos(a) * (sr * 1.85f);
                float y2 = sy + (float) Math.sin(a) * (sr * 1.85f);
                canvas.drawLine(x1, y1, x2, y2, sunPaint);
            }
            drawCloud(canvas, w * 0.54f, h * 0.58f, size * 0.62f);
        } else if (iconType == TYPE_CLOUDY) {
            drawCloud(canvas, cx, cy * 1.05f, size * 0.72f);
        } else {
            // RAIN or STORM
            drawCloud(canvas, cx, h * 0.43f, size * 0.68f);
            float dropTop = h * 0.68f;
            float dropBottom = h * 0.84f;
            float dx = size * 0.05f;
            for (int i = -1; i <= 1; i++) {
                float x = cx + i * (size * 0.16f);
                canvas.drawLine(x + dx, dropTop, x - dx, dropBottom, rainPaint);
            }
        }
    }

    private void drawCloud(Canvas canvas, float cx, float cy, float cw) {
        float ch = cw * 0.56f;
        float left = cx - cw * 0.48f;
        float right = cx + cw * 0.48f;
        float bottom = cy + ch * 0.35f;

        cloudPath.reset();
        // Flat bottom line
        cloudPath.moveTo(left + cw * 0.18f, bottom);
        cloudPath.lineTo(right - cw * 0.16f, bottom);

        // Right bump
        arcRect.set(right - cw * 0.36f, bottom - ch * 0.68f, right, bottom);
        cloudPath.arcTo(arcRect, 90, -180, false);

        // Top main dome
        arcRect.set(cx - cw * 0.26f, bottom - ch * 1.05f, cx + cw * 0.28f, bottom - ch * 0.20f);
        cloudPath.arcTo(arcRect, 15, -185, false);

        // Left bump
        arcRect.set(left, bottom - ch * 0.72f, left + cw * 0.38f, bottom);
        cloudPath.arcTo(arcRect, -70, -200, false);

        cloudPath.close();
        canvas.drawPath(cloudPath, cloudPaint);
    }
}
