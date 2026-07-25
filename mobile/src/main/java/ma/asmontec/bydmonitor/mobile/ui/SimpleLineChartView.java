package ma.asmontec.bydmonitor.mobile.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import java.util.Collections;
import java.util.List;

/** Petit graphique linéaire dessiné sur Canvas, sans bibliothèque externe. */
public final class SimpleLineChartView extends View {

    private List<Float> values = Collections.emptyList();
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint();

    public SimpleLineChartView(Context context) {
        super(context);
        linePaint.setColor(UiKit.ACCENT);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(UiKit.dp(context, 2));

        fillPaint.setColor(UiKit.ACCENT);
        fillPaint.setAlpha(40);
        fillPaint.setStyle(Paint.Style.FILL);

        gridPaint.setColor(UiKit.TEXT_MUTED);
        gridPaint.setAlpha(60);
        gridPaint.setStrokeWidth(1f);
    }

    public void setValues(List<Float> newValues) {
        this.values = newValues == null ? Collections.emptyList() : newValues;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        canvas.drawLine(0, h / 2f, w, h / 2f, gridPaint);

        if (values.size() < 2) {
            return;
        }

        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (float v : values) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        if (max - min < 0.001f) {
            max = min + 1f;
        }

        Path line = new Path();
        Path fill = new Path();
        float stepX = w / (float) (values.size() - 1);

        for (int i = 0; i < values.size(); i++) {
            float x = i * stepX;
            float normalized = (values.get(i) - min) / (max - min);
            float y = h - normalized * h;
            if (i == 0) {
                line.moveTo(x, y);
                fill.moveTo(x, h);
                fill.lineTo(x, y);
            } else {
                line.lineTo(x, y);
                fill.lineTo(x, y);
            }
        }
        fill.lineTo(w, h);
        fill.close();

        canvas.drawPath(fill, fillPaint);
        canvas.drawPath(line, linePaint);
    }
}
