package cam.scanner.mini.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import cam.scanner.mini.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PolygonView extends FrameLayout {
    protected Context mContext;
    private Paint mPaint;
    private ImageView mPointer1;
    private ImageView mPointer2;
    private ImageView mPointer3;
    private ImageView mPointer4;
    private ImageView mMidPointer13;
    private ImageView mMidPointer12;
    private ImageView mMidPointer34;
    private ImageView mMidPointer24;
    private PolygonView mPolygonView;
    private Drawable circle;
    private int circleHalfWidth;
    private int circleHalfHeight;
    private Rect mBounds;
    private OnPointDragListener mListener;
    private MagnifierView mMagnifierView;
    private Bitmap mBitmap;

    public PolygonView(Context context, int x, int y, int width, int height, @Nullable OnPointDragListener listener) {
        super(context);
        mContext        = context;
        mListener       = listener;
        mBounds         = new Rect(x, y, x + width, y + height);
        mPolygonView    = this;
        mBitmap         = null;
        circle          = mContext.getResources().getDrawable(R.drawable.ic_circle);
        circleHalfWidth = circle.getIntrinsicWidth() >> 1;
        circleHalfHeight= circle.getIntrinsicHeight() >> 1;
        init();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        int x = mBounds.left, y = mBounds.top;
        int width = mBounds.right - mBounds.left, height = mBounds.bottom - mBounds.top;
        mPointer1       = getImageView(x, y);
        mPointer2       = getImageView(x + width, y);
        mPointer3       = getImageView(x, height + y);
        mPointer4       = getImageView(x + width, height + y);
        mMidPointer12   = getImageView(x, width / 2 + y);
        mMidPointer24   = getImageView(x, height / 2 + y);
        mMidPointer34   = getImageView(x, height / 2 + y);
        mMidPointer13   = getImageView(x, height / 2 + y);

        mMidPointer12.setOnTouchListener(new MidPointTouchListenerImpl(mPointer1, mPointer2));
        mMidPointer24.setOnTouchListener(new MidPointTouchListenerImpl(mPointer2, mPointer4));
        mMidPointer34.setOnTouchListener(new MidPointTouchListenerImpl(mPointer3, mPointer4));
        mMidPointer13.setOnTouchListener(new MidPointTouchListenerImpl(mPointer1, mPointer3));

        mMagnifierView = new MagnifierView(mContext);

        addView(mPointer1);
        addView(mPointer2);
        addView(mPointer3);
        addView(mPointer4);
        addView(mMidPointer12);
        addView(mMidPointer24);
        addView(mMidPointer34);
        addView(mMidPointer13);
        addView(mMagnifierView);
        initPaint();
    }

    public void reset() {
        setPoints(getDefaultPoints());
        requestLayout();
    }

    @Override
    protected void attachViewToParent(View child, int index, ViewGroup.LayoutParams params) {
        super.attachViewToParent(child, index, params);
    }

    private void initPaint() {
        mPaint = new Paint();
        mPaint.setColor(getColorResource());
        mPaint.setStrokeWidth(2);
        mPaint.setAntiAlias(true);
    }

    public Map<Integer, PointF> getPoints() {
        List<PointF> points = new ArrayList<>();
        int width = mBounds.right - mBounds.left;
        int height = mBounds.bottom - mBounds.top;
        points.add(new PointF((mPointer1.getX() + circleHalfWidth - mBounds.left) / width, (mPointer1.getY() + circleHalfHeight - mBounds.top) / height));
        points.add(new PointF((mPointer2.getX() + circleHalfWidth - mBounds.left) / width, (mPointer2.getY() + circleHalfHeight - mBounds.top) / height));
        points.add(new PointF((mPointer3.getX() + circleHalfWidth - mBounds.left) / width, (mPointer3.getY() + circleHalfHeight - mBounds.top) / height));
        points.add(new PointF((mPointer4.getX() + circleHalfWidth - mBounds.left) / width, (mPointer4.getY() + circleHalfHeight - mBounds.top) / height));

        return getOrderedPoints(points);
    }

    public static Map<Integer, PointF> getDefaultPoints() {
        Map<Integer, PointF> pointFMap = new HashMap<>();
        pointFMap.put(0, new PointF(0, 0));
        pointFMap.put(1, new PointF(1, 0));
        pointFMap.put(2, new PointF(0, 1));
        pointFMap.put(3, new PointF(1, 1));

        return pointFMap;
    }

    public static Map<Integer, PointF> getOrderedPoints(List<PointF> points) {
        PointF centerPoint = new PointF();
        int size = points.size();
        for (PointF pointF : points) {
            centerPoint.x += pointF.x / size;
            centerPoint.y += pointF.y / size;
        }
        Map<Integer, PointF> orderedPoints = new HashMap<>();
        for (PointF pointF : points) {
            int index = -1;
            if (pointF.x < centerPoint.x && pointF.y < centerPoint.y) {
                index = 0;
            } else if (pointF.x > centerPoint.x && pointF.y < centerPoint.y) {
                index = 1;
            } else if (pointF.x < centerPoint.x && pointF.y > centerPoint.y) {
                index = 2;
            } else if (pointF.x > centerPoint.x && pointF.y > centerPoint.y) {
                index = 3;
            }
            orderedPoints.put(index, pointF);
        }
        return orderedPoints;
    }

    public void setPoints(Map<Integer, PointF> pointFMap) {
        if (pointFMap.size() == 4) {
            setPointsCoordinates(pointFMap);
        }
    }

    private void setPointsCoordinates(Map<Integer, PointF> pointFMap) {
        int width = mBounds.right - mBounds.left;
        int height = mBounds.bottom - mBounds.top;

        mPointer1.setX((pointFMap.get(0).x * width) - circleHalfWidth + mBounds.left);
        mPointer1.setY((pointFMap.get(0).y * height) - circleHalfHeight + mBounds.top);

        mPointer2.setX((pointFMap.get(1).x * width) - circleHalfWidth + mBounds.left);
        mPointer2.setY((pointFMap.get(1).y * height) - circleHalfHeight + mBounds.top);

        mPointer3.setX((pointFMap.get(2).x * width) - circleHalfWidth + mBounds.left);
        mPointer3.setY((pointFMap.get(2).y * height) - circleHalfHeight + mBounds.top);

        mPointer4.setX((pointFMap.get(3).x * width) - circleHalfWidth + mBounds.left);
        mPointer4.setY((pointFMap.get(3).y * height) - circleHalfHeight + mBounds.top);

        mPaint.setColor(getColorResource());
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        canvas.drawLine(mPointer1.getX() + (mPointer1.getWidth() * 0.5f), mPointer1.getY() + (mPointer1.getHeight() * 0.5f), mPointer3.getX() + (mPointer3.getWidth() * 0.5f), mPointer3.getY() + (mPointer3.getHeight() * 0.5f), mPaint);
        canvas.drawLine(mPointer1.getX() + (mPointer1.getWidth() * 0.5f), mPointer1.getY() + (mPointer1.getHeight() * 0.5f), mPointer2.getX() + (mPointer2.getWidth() * 0.5f), mPointer2.getY() + (mPointer2.getHeight() * 0.5f), mPaint);
        canvas.drawLine(mPointer2.getX() + (mPointer2.getWidth() * 0.5f), mPointer2.getY() + (mPointer2.getHeight() * 0.5f), mPointer4.getX() + (mPointer4.getWidth() * 0.5f), mPointer4.getY() + (mPointer4.getHeight() * 0.5f), mPaint);
        canvas.drawLine(mPointer3.getX() + (mPointer3.getWidth() * 0.5f), mPointer3.getY() + (mPointer3.getHeight() * 0.5f), mPointer4.getX() + (mPointer4.getWidth() * 0.5f), mPointer4.getY() + (mPointer4.getHeight() * 0.5f), mPaint);
        mMidPointer13.setX(mPointer3.getX() - ((mPointer3.getX() - mPointer1.getX()) * 0.5f));
        mMidPointer13.setY(mPointer3.getY() - ((mPointer3.getY() - mPointer1.getY()) * 0.5f));
        mMidPointer24.setX(mPointer4.getX() - ((mPointer4.getX() - mPointer2.getX()) * 0.5f));
        mMidPointer24.setY(mPointer4.getY() - ((mPointer4.getY() - mPointer2.getY()) * 0.5f));
        mMidPointer34.setX(mPointer4.getX() - ((mPointer4.getX() - mPointer3.getX()) * 0.5f));
        mMidPointer34.setY(mPointer4.getY() - ((mPointer4.getY() - mPointer3.getY()) * 0.5f));
        mMidPointer12.setX(mPointer2.getX() - ((mPointer2.getX() - mPointer1.getX()) * 0.5f));
        mMidPointer12.setY(mPointer2.getY() - ((mPointer2.getY() - mPointer1.getY()) * 0.5f));
    }

    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
    }

    @SuppressLint("ClickableViewAccessibility")
    private ImageView getImageView(int x, int y) {
        ImageView imageView = new ImageView(mContext);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        imageView.setLayoutParams(layoutParams);
        imageView.setImageDrawable(circle);
        imageView.setX(x - circleHalfWidth);
        imageView.setY(y - circleHalfHeight);
        imageView.setAlpha(0.6f);
        imageView.setOnTouchListener(new TouchListenerImpl());
        return imageView;
    }

    private class MidPointTouchListenerImpl implements OnTouchListener {
        PointF DownPT = new PointF(); // Record Mouse Position When Pressed Down
        PointF StartPT = new PointF(); // Record Start Position of 'img'

        private ImageView mainPointer1;
        private ImageView mainPointer2;

        private boolean isMoving = false;
        int width = mBounds.right - mBounds.left;
        int height = mBounds.bottom - mBounds.top;

        public MidPointTouchListenerImpl(ImageView mainPointer1, ImageView mainPointer2) {
            this.mainPointer1 = mainPointer1;
            this.mainPointer2 = mainPointer2;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int eid = event.getAction();
            switch (eid) {
                case MotionEvent.ACTION_MOVE:
                    PointF mv = new PointF(event.getX() - DownPT.x, event.getY() - DownPT.y);

                    if (Math.abs(mainPointer1.getX() - mainPointer2.getX()) > Math.abs(mainPointer1.getY() - mainPointer2.getY())) {
                        if (((mainPointer2.getY() + mv.y + circleHalfHeight <= mBounds.bottom) && (mainPointer2.getY() + mv.y + circleHalfHeight >= mBounds.top))) {
                            v.setX((int) (StartPT.y + mv.y));
                            StartPT = new PointF(v.getX(), v.getY());
                            mainPointer2.setY((int) (mainPointer2.getY() + mv.y));
                        }
                        if (((mainPointer1.getY() + mv.y + circleHalfHeight <= mBounds.bottom) && (mainPointer1.getY() + mv.y + circleHalfHeight >= mBounds.top))) {
                            v.setX((int) (StartPT.y + mv.y));
                            StartPT = new PointF(v.getX(), v.getY());
                            mainPointer1.setY((int) (mainPointer1.getY() + mv.y));
                        }
                    } else {
                        if ((mainPointer2.getX() + mv.x + circleHalfWidth <= mBounds.right) && (mainPointer2.getX() + mv.x + circleHalfWidth >= mBounds.left)) {
                            v.setX((int) (StartPT.x + mv.x));
                            StartPT = new PointF(v.getX(), v.getY());
                            mainPointer2.setX((int) (mainPointer2.getX() + mv.x));
                        }
                        if ((mainPointer1.getX() + mv.x + circleHalfWidth <= mBounds.right) && (mainPointer1.getX() + mv.x + circleHalfWidth >= mBounds.left)) {
                            v.setX((int) (StartPT.x + mv.x));
                            StartPT = new PointF(v.getX(), v.getY());
                            mainPointer1.setX((int) (mainPointer1.getX() + mv.x));
                        }
                    }
                    break;
                case MotionEvent.ACTION_DOWN:
                    DownPT.x = event.getX();
                    DownPT.y = event.getY();
                    StartPT = new PointF(v.getX(), v.getY());
                    break;
                case MotionEvent.ACTION_UP:
                    mPaint.setColor(getColorResource());
                    break;
                default:
                    break;
            }
            if (eid == MotionEvent.ACTION_MOVE) {
                if (!isMoving) {
                    isMoving = true;
                    if (mListener != null) {
                        mListener.onPointDraggingStarted();
                    }
                    if (mBitmap != null) {
                        mMagnifierView.setBitmap(mBitmap);
                    }
                }
                mMagnifierView.showAt(
                        ((((mainPointer1.getX() + mainPointer2.getX()) / 2) + circleHalfWidth - mBounds.left) / width),
                        ((((mainPointer1.getY() + mainPointer2.getY()) / 2) + circleHalfHeight - mBounds.top) / height)
                );
            } else {
                if (isMoving) {
                    isMoving = false;
                    if (mListener != null) {
                        mListener.onPointDraggingCompleted();
                    }
                    mMagnifierView.hide();
                }
            }
            mPolygonView.invalidate();
            return true;
        }
    }

    private int getColorResource() {
        if (isValidShape(getPoints())) {
            return getResources().getColor(R.color.cropAcceptableColor);
        }
        return getResources().getColor(R.color.cropRejectedColor);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    public static boolean isValidShape(Map<Integer, PointF> pointFMap) {
        List<PointF> pointFList = new ArrayList<>();
        if (pointFMap.size() == 4) {
            pointFList.add(pointFMap.get(0));
            pointFList.add(pointFMap.get(1));
            pointFList.add(pointFMap.get(2));
            pointFList.add(pointFMap.get(3));
        }

        Map<Integer, PointF> orderedPoints = getOrderedPoints(pointFList);
        if (orderedPoints.size() != 4) {
            return false;
        }

        double a0 = getAngle(orderedPoints.get(0), orderedPoints.get(1), orderedPoints.get(2));
        double a1 = getAngle(orderedPoints.get(1), orderedPoints.get(3), orderedPoints.get(0));
        double a2 = getAngle(orderedPoints.get(2), orderedPoints.get(0), orderedPoints.get(3));
        double a3 = getAngle(orderedPoints.get(3), orderedPoints.get(2), orderedPoints.get(1));

        return (a0 >= 45 && a0 <= 135) && (a1 >= 45 && a1 <= 135) && (a2 >= 45 && a2 <= 135) && (a3 >= 45 && a3 <= 135);
    }

    private static double getAngle(PointF a, PointF b, PointF c) {
        PointF l1 = new PointF(a.x - b.x, a.y - b.y);
        PointF l2 = new PointF(a.x - c.x, a.y - c.y);

        float dotProduct = l1.x * l2.x + l1.y * l2.y;
        double l1Length = Math.sqrt(l1.x * l1.x + l1.y * l1.y);
        double l2Length = Math.sqrt(l2.x * l2.x + l2.y * l2.y);

        return Math.toDegrees(Math.acos(dotProduct / (l1Length * l2Length)));
    }

    private class TouchListenerImpl implements OnTouchListener {
        PointF DownPT = new PointF(); // Record Mouse Position When Pressed Down
        PointF StartPT = new PointF(); // Record Start Position of 'img'

        private boolean isMoving = false;
        int width = mBounds.right - mBounds.left;
        int height = mBounds.bottom - mBounds.top;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int eid = event.getAction();
            switch (eid) {
                case MotionEvent.ACTION_MOVE:
                    PointF mv = new PointF(event.getX() - DownPT.x, event.getY() - DownPT.y);
                    if (
                            (StartPT.x + mv.x + circleHalfWidth <= mBounds.right) &&
                            (StartPT.y + mv.y + circleHalfHeight <= mBounds.bottom) &&
                            (StartPT.x + mv.x + circleHalfWidth >= mBounds.left) &&
                            (StartPT.y + mv.y + circleHalfHeight >= mBounds.top)
                    ) {
                        v.setX((int) (StartPT.x + mv.x));
                        v.setY((int) (StartPT.y + mv.y));
                        StartPT = new PointF(v.getX(), v.getY());
                    }
                    break;
                case MotionEvent.ACTION_DOWN:
                    DownPT.x = event.getX();
                    DownPT.y = event.getY();
                    StartPT = new PointF(v.getX(), v.getY());
                    break;
                case MotionEvent.ACTION_UP:
                    mPaint.setColor(getColorResource());
                    break;
                default:
                    break;
            }
            if (eid == MotionEvent.ACTION_MOVE) {
                if (!isMoving) {
                    isMoving = true;
                    if (mListener != null) {
                        mListener.onPointDraggingStarted();
                    }
                    if (mBitmap != null) {
                        mMagnifierView.setBitmap(mBitmap);
                    }
                }
                mMagnifierView.showAt((v.getX() + circleHalfWidth - mBounds.left) / width, (v.getY() + circleHalfHeight - mBounds.top) / height);
            } else {
                if (isMoving) {
                    isMoving = false;
                    if (mListener != null) {
                        mListener.onPointDraggingCompleted();
                    }
                    mMagnifierView.hide();
                }
            }

            mPolygonView.invalidate();
            return true;
        }
    }

    public interface OnPointDragListener {
        public void onPointDraggingStarted();
        public void onPointDraggingCompleted();
    }
}
