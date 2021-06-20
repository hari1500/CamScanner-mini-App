package cam.scanner.mini.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class CropableImageView extends AppCompatImageView {
    private boolean mDidDraw = false;
    private OnDrawListener mOnDrawListener = null;

    public CropableImageView(Context context) {
        super(context);
    }

    public CropableImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CropableImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (getDrawable() == null) {
            return;
        }

        float[] imageMatrix = new float[9];
        getImageMatrix().getValues(imageMatrix);
        int x = (int) (getX() + getPaddingLeft() + imageMatrix[Matrix.MTRANS_X]);
        int y = (int) (getY() + getPaddingTop() + imageMatrix[Matrix.MTRANS_Y]);

        Rect imageRect = getDrawable().getBounds();
        int width = (int) ((imageRect.right - imageRect.left) * imageMatrix[Matrix.MSCALE_X]);
        int height = (int) ((imageRect.bottom - imageRect.top) * imageMatrix[Matrix.MSCALE_Y]);

        if (!mDidDraw && mOnDrawListener !=  null)  {
            mOnDrawListener.onDraw(x, y, width, height);
            mDidDraw = true;
        }
    }

    public void setOnDrawListener(OnDrawListener onDrawListener) {
        this.mOnDrawListener = onDrawListener;

        requestLayout();
    }

    public void removeOnDrawListener() {
        this.mOnDrawListener = null;
        mDidDraw = false;
    }

    public interface OnDrawListener {
        public void onDraw(int x, int y, int width, int height);
    }
}
