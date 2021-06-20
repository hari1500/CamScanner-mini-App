package cam.scanner.mini.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

public class AutoResizableTextureView extends TextureView {
    private int mPreferredRatioWidth    = -1;
    private int mPreferredRatioHeight   = -1;

    public AutoResizableTextureView(Context context) {
        this(context, null);
    }

    public AutoResizableTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoResizableTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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
