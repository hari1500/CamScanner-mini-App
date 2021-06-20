package cam.scanner.mini.imageprocessing;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class Filters {
    public enum Type {
        ORIGINAL, BLACK_AND_WHITE, BLACK_AND_WHITE_2, LIGHTEN, GRAY
    }

    public static String getFilterName(Type type) {
        switch (type) {
            case ORIGINAL: {
                return "Original";
            }
            case BLACK_AND_WHITE: {
                return "B&W I";
            }
            case BLACK_AND_WHITE_2: {
                return "B&W II";
            }
            case LIGHTEN: {
                return "Lighten";
            }
            case GRAY: {
                return "Gray";
            }
            default:{
                return "";
            }
        }
    }

    public static Bitmap getFilteredImage(Bitmap bitmap, Type type) {
        switch (type) {
            case ORIGINAL: {
                return bitmap;
            }
            case BLACK_AND_WHITE: {
                return filter_magic(bitmap);
            }
            case BLACK_AND_WHITE_2: {
                return filter_otsu(bitmap);
            }
            case LIGHTEN: {
                return filter_lighten(bitmap);
            }
            case GRAY: {
                return filter_gray(bitmap);
            }
            default:{
                return bitmap;
            }
        }
    }

    public static Bitmap filter_otsu(Bitmap image) {
        int h = image.getHeight(), w = image.getWidth();
        Mat src = new Mat(h, w, CvType.CV_8UC1);
        Utils.bitmapToMat(image, src);

        Log.v("ash", "otsu");
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(src, src, new Size(5,5), 0);
        Imgproc.threshold(src, src, 0, 255, Imgproc.THRESH_BINARY+Imgproc.THRESH_OTSU);
//        Imgproc.adaptiveThreshold(src, src, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
//                Imgproc.THRESH_BINARY, 11,2);
//        Imgproc.GaussianBlur(src,src, new Size(3,3), 0);
        Bitmap outputImage = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(src, outputImage);
        return outputImage;
    }

    public static Bitmap filter_gray(Bitmap image) {
        int h = image.getHeight(), w = image.getWidth();

        Mat src = new Mat(h, w, CvType.CV_8UC1);
        Utils.bitmapToMat(image, src);

        Log.v("ash", "gray");
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(src, src, new Size(3,3), 0);

        Bitmap outputImage = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, outputImage);
        return outputImage;
    }

    public static Bitmap filter_lighten(Bitmap image) {
        int h = image.getHeight(), w = image.getWidth();

        Mat src = new Mat(h, w, CvType.CV_8UC1);
        Utils.bitmapToMat(image, src);
        Mat dst = new Mat(h, w, CvType.CV_8UC1);

        Log.v("ash", "lighten");
        Imgproc.GaussianBlur(src, src, new Size(3,3), 0);
        src.convertTo(dst,-1,1,50);

        Bitmap outputImage = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, outputImage);
        return outputImage;
    }

    public static Bitmap filter_magic(Bitmap image){
        Log.v("ash", "hey");
        int h = image.getHeight(), w = image.getWidth();

        Mat src = new Mat(h, w, CvType.CV_8UC1);
        Utils.bitmapToMat(image, src);
        Log.v("ash", "magic");

        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(src, src, new Size(7,7), 0);

        Imgproc.adaptiveThreshold(src, src, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 99,15);
        Bitmap outputImage = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, outputImage);
        return outputImage;
    }

}
