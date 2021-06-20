package cam.scanner.mini.components;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.SearchView;

public class AdaptableSearchView extends SearchView {
    private OnSizeChangeListener mListener = null;

    public AdaptableSearchView(Context context) {
        super(context);
    }

    public AdaptableSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AdaptableSearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onActionViewExpanded() {
        super.onActionViewExpanded();

        if (mListener != null) {
            mListener.onExpanded();
        }
    }

    @Override
    public void onActionViewCollapsed() {
        super.onActionViewCollapsed();

        if (mListener != null) {
            mListener.onCollapsed();
        }
    }

    public void setOnSizeChangeListener(OnSizeChangeListener listener) {
        this.mListener = listener;
    }

    public interface OnSizeChangeListener {
        public void onExpanded();
        public void onCollapsed();
    }
}
