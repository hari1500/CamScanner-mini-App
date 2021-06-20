package cam.scanner.mini.components;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class AutoResizableFrameLayout extends FrameLayout {
    private Context mContext;
    private DisplayGridView mDisplayGridView;

    private int mPreferredRatioWidth    = -1;
    private int mPreferredRatioHeight   = -1;
    private boolean showGrid            = false;

    public AutoResizableFrameLayout(Context context) {
        this(context, null);
    }

    public AutoResizableFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoResizableFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
        postInvalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mDisplayGridView == null) {
            mDisplayGridView = new DisplayGridView(mContext, getWidth(), getHeight());
            addView(mDisplayGridView);
        }
        mDisplayGridView.setShowGrid(showGrid);
        bringChildToFront(mDisplayGridView);

        super.dispatchDraw(canvas);
    }

    public void setAspectRatio(int width, int height) {
        mPreferredRatioWidth = width;
        mPreferredRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (mPreferredRatioWidth > 0 && mPreferredRatioHeight > 0) {
            if (width < height * mPreferredRatioWidth / mPreferredRatioHeight) {
                height = width * mPreferredRatioHeight / mPreferredRatioWidth;
            } else {
                width = height * mPreferredRatioWidth / mPreferredRatioHeight;
            }
        }
        setMeasuredDimension(width, height);
    }
}