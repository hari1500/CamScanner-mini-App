package cam.scanner.mini.utils;

import android.graphics.Bitmap;
import android.graphics.PointF;

import cam.scanner.mini.components.PolygonView;

import java.util.ArrayList;
import java.util.Map;

public class BufferedImagesHelper {
    private static ArrayList<BufferedImage> mBufferedImages;
    private static Bitmap mRetakeResultantImage;

    public static void init() {
        mBufferedImages = new ArrayList<>();
        mRetakeResultantImage = null;
    }

    public static void setRetakeResultantImage(Bitmap bitmap) {
        mRetakeResultantImage = bitmap;
    }

    public static Bitmap getRetakeResultantImage() {
        return mRetakeResultantImage;
    }

    public static void clearBufferedImages() {
        mBufferedImages.clear();
    }

    public static ArrayList<BufferedImage> getBufferedImages() {
        return mBufferedImages;
    }

    public static BufferedImage getLastImage() {
        return (mBufferedImages.size() == 0) ? null : mBufferedImages.get(mBufferedImages.size() - 1);
    }

    public static void updateImageAt(int position, BufferedImage bufferedImage) {
        if (position >= 0 && position < mBufferedImages.size()) {
            mBufferedImages.set(position, bufferedImage);
        }
    }

    public static void deleteImageAt(int position) {
        if (position >= 0 && position < mBufferedImages.size()) {
            mBufferedImages.remove(position);
        }
    }

    public static void addImageToBuffer(Bitmap original, Bitmap modified) {
        mBufferedImages.add(new BufferedImage(original, modified));
    }

    public static void addImageToBuffer(Bitmap original, Bitmap modified, Map<Integer, PointF> pointFMap) {
        mBufferedImages.add(new BufferedImage(original, modified, pointFMap));
    }

    public static class BufferedImage {
        private Bitmap mOriginalImage;
        private Bitmap mModifiedImage;
        private Map<Integer, PointF> mCorners = null;
        private PolygonView mPolygonView = null;
        private int mRotation = 0;

        public BufferedImage(Bitmap originalImage, Bitmap modifiedImage, Map<Integer, PointF> corners) {
            this.mOriginalImage = originalImage;
            this.mModifiedImage = modifiedImage;
            this.mCorners       = corners;
        }

        public BufferedImage(Bitmap originalImage, Bitmap modifiedImage) {
            this.mOriginalImage = originalImage;
            this.mModifiedImage = modifiedImage;
        }

        public Bitmap getOriginalImage() {
            return mOriginalImage;
        }

        public Bitmap getModifiedImage() {
            return mModifiedImage;
        }

        public Map<Integer, PointF> getCorners() {
            return mCorners;
        }

        public void resetCorners() {
            mCorners = null;
            if (mPolygonView != null) {
                mPolygonView.reset();
            }
        }

        public PolygonView getPolygonView() {
            return mPolygonView;
        }

        public void setPolygonView(PolygonView polygonView) {
            this.mPolygonView = polygonView;
        }

        public int getRotation() {
            return mRotation;
        }

        public void setRotation(int rotation) {
            this.mRotation = rotation;
        }
    }
}
