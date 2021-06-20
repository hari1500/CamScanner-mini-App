package cam.scanner.mini.imageprocessing;

import android.graphics.Bitmap;
import android.graphics.PointF;

import cam.scanner.mini.App;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Math.pow;

public class CVHelper {
    public static void init() {
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, App.getContext(), null);
        }
    }

    /**
     *
     * @param image original image
     * @param orderedPointFMap size must be 4
     *                         Order of points is [top_left,top_right,bottom_left,bottom_right]
     * @return modified image after applying perspective transformation
     */
    public static Bitmap applyPerspective(Bitmap image, Map<Integer, PointF> orderedPointFMap) {
        if (orderedPointFMap.size() != 4) {
            return image;
        }
        ArrayList<myHandle> sourcePoints = new ArrayList<>();
        sourcePoints.add(new myHandle(orderedPointFMap.get(0).x * image.getWidth(), orderedPointFMap.get(0).y * image.getHeight()));
        sourcePoints.add(new myHandle(orderedPointFMap.get(1).x * image.getWidth(), orderedPointFMap.get(1).y * image.getHeight()));
        sourcePoints.add(new myHandle(orderedPointFMap.get(3).x * image.getWidth(), orderedPointFMap.get(3).y * image.getHeight()));
        sourcePoints.add(new myHandle(orderedPointFMap.get(2).x * image.getWidth(), orderedPointFMap.get(2).y * image.getHeight()));

        // sourcePoints are  expected  to be clockwise ordered
        // [top_left,top_right,bottom_right,bottom_left]
        // getting the size of the output image
        // TODO: determine the dst_width, dst_height
        double dst_width = Math.max(sourcePoints.get(0).distanceFrom(sourcePoints.get(1)), sourcePoints.get(3).distanceFrom(sourcePoints.get(2)));
        double dst_height = Math.max(sourcePoints.get(0).distanceFrom(sourcePoints.get(3)), sourcePoints.get(1).distanceFrom(sourcePoints.get(2)));
//        double dst_width = image.getWidth(), dst_height = image.getHeight();
        //determining point sets to get the transformation matrix
        List<Point> srcPts = new ArrayList<org.opencv.core.Point>();
        for (myHandle ball : sourcePoints) {
            srcPts.add(new org.opencv.core.Point((ball.getX()), ball.getY()));
        }

        List<Point> dstPoints = new ArrayList<Point>();
        dstPoints.add(new org.opencv.core.Point(0, 0));
        dstPoints.add(new org.opencv.core.Point(dst_width - 1, 0));
        dstPoints.add(new org.opencv.core.Point(dst_width - 1, dst_height - 1));
        dstPoints.add(new org.opencv.core.Point(0, dst_height));

        Mat srcMat = Converters.vector_Point2f_to_Mat(srcPts);
        Mat dstMat = Converters.vector_Point2f_to_Mat(dstPoints);

        //getting the transformation matrix
        Mat perspectiveTransformation = Imgproc.getPerspectiveTransform(srcMat, dstMat);

        //getting the input matrix from the given bitmap
        Mat inputMat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC1);

        Utils.bitmapToMat(image, inputMat);

        //getting the output matrix with the previously determined sizes
        Mat outputMat = new Mat((int) dst_height, (int) dst_width, CvType.CV_8UC1);

        //applying the transformation
        Imgproc.warpPerspective(inputMat, outputMat, perspectiveTransformation, new Size(dst_width, dst_height));


        Bitmap outputBitmap = Bitmap.createBitmap((int) dst_width, (int) dst_height, Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(outputMat, outputBitmap);

        return outputBitmap;
    }

    public static class myHandle {
        float x,y;

        public myHandle(float xcord, float ycord) {
            x = xcord;
            y = ycord;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public int distanceFrom(myHandle point) {
            return (int) Math.sqrt(pow((point.getX() - x),2) + pow((point.getY() - y),2));
        }
    }
}
