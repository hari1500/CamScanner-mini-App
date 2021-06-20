package cam.scanner.mini.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import cam.scanner.mini.R;

public class MagnifierView extends View {
    private Bitmap mBitmap;
    private Matrix mMatrix;
    private Paint mPaint;
    private Paint mMarkerPaint;
    private Shader mShader;
    private int mRadius;
    private int mPadding;
    private int mMarkerLen;
    private FrameLayout.LayoutParams mLayoutParams;

    public MagnifierView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mBitmap = null;
        mRadius = (int) getResources().getDimension(R.dimen.magnifier_radius);
        mPadding = (int) getResources().getDimension(R.dimen.magnifier_padding);
        mMarkerLen = (int) getResources().getDimension(R.dimen.magnifier_marker_half_length);
        mMatrix = new Matrix();
        mShader = null;
        mPaint = new Paint();
        mMarkerPaint = new Paint();
        mMarkerPaint.setColor(getResources().getColor(R.color.colorAccent));
        mMarkerPaint.setStrokeWidth(3);
        mMarkerPaint.setAntiAlias(true);

        setBackground(getResources().getDrawable(R.drawable.magnifier_background));
        setVisibility(GONE);
        setPadding(mPadding, mPadding, mPadding, mPadding);

        int margin = (int) getResources().getDimension(R.dimen.magnifier_margin);
        mLayoutParams = new FrameLayout.LayoutParams(2 * mRadius, 2 * mRadius);
        mLayoutParams.setMargins(margin, margin, margin, margin);
        setLayoutParams(mLayoutParams);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (getVisibility() == VISIBLE && mShader != null) {
            canvas.drawCircle(mRadius, mRadius, mRadius - mPadding, mPaint);
            canvas.drawLine(mRadius, mRadius - mMarkerLen, mRadius, mRadius + mMarkerLen, mMarkerPaint);
            canvas.drawLine(mRadius - mMarkerLen, mRadius, mRadius + mMarkerLen, mRadius, mMarkerPaint);
        }
    }

    public void setBitmap(Bitmap bm) {
        mBitmap = bm;
        mShader = new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        mPaint.setShader(mShader);
    }

    public void showAt(float fracX, float fracY) {
        if (mBitmap == null) {
            return;
        }
        if (getVisibility() != VISIBLE) {
            setVisibility(VISIBLE);
        }
        if (fracX <= 0.5) {
            mLayoutParams.gravity = Gravity.TOP | Gravity.END;
        } else {
            mLayoutParams.gravity = Gravity.TOP | Gravity.START;
        }
        setLayoutParams(mLayoutParams);

        int magnifierRegion = (int) getResources().getDimension(R.dimen.magnifier_region);
        float x = fracX * mBitmap.getWidth();
        float y = fracY * mBitmap.getHeight();
        RectF src = new RectF((x - magnifierRegion), (y - magnifierRegion), (x + magnifierRegion), (y + magnifierRegion));
        RectF dst = new RectF(0, 0, 2 * mRadius, 2 * mRadius);

        mMatrix.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER);
        mPaint.getShader().setLocalMatrix(mMatrix);

        invalidate();
    }

    public void hide() {
        setVisibility(GONE);
    }
}
