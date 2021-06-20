package cam.scanner.mini.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class DisplayGridView extends View {
    private boolean showGrid = false;
    int width, height;

    public DisplayGridView(Context context, int width, int height) {
        super(context);
        this.width = width;
        this.height = height;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint paint = new Paint();
        if (showGrid) {
            paint.setAntiAlias(true);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);

            float thirdWidth = width * 1.0f / 3;
            float thirdHeight = height * 1.0f / 3;

            canvas.drawLine(thirdWidth, 0, thirdWidth, height, paint);
            canvas.drawLine(2 * thirdWidth, 0, 2 * thirdWidth, height, paint);
            canvas.drawLine(0, thirdHeight, width, thirdHeight, paint);
            canvas.drawLine(0, 2 * thirdHeight, width, 2 * thirdHeight, paint);
        }

        super.onDraw(canvas);
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
        postInvalidate();
    }
}
