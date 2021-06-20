package cam.scanner.mini.imageprocessing;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;

import androidx.annotation.NonNull;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.abs;
import static java.util.Collections.sort;

public class EdgeHelper {
    public static double getAngle(Point pt1, Point pt2, Point pt0)
    {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1*dx2 + dy1*dy2)/Math.sqrt((dx1*dx1 + dy1*dy1)*(dx2*dx2 + dy2*dy2) + 1e-10);
    }

    public static int findLargestSquare(List<MatOfPoint> squares) {
        if (squares.size() == 0)
            return -1;
        int max_width = 0;
        int max_height = 0;
        int max_square_idx = 0;
        int currentIndex = 0;
        for (MatOfPoint square : squares) {
            Rect rectangle = Imgproc.boundingRect(square);
            if (rectangle.width >= max_width && rectangle.height >= max_height) {
                max_width = rectangle.width;
                max_height = rectangle.height;
                max_square_idx = currentIndex;
            }
            currentIndex++;
        }
        return max_square_idx;
    }

    // Point-to-point distance
    public static double getSpacePointToPoint(Point p1, Point p2) {
        double a = p1.x - p2.x;
        double b = p1.y - p2.y;
        return Math.sqrt(a * a + b * b);
    }

    // The intersection of two straight lines
    public static Point computeIntersect(double[] a, double[] b) {
        if (a.length != 4 || b.length != 4)
            throw new ClassFormatError();
        double x1 = a[0], y1 = a[1], x2 = a[2], y2 = a[3], x3 = b[0], y3 = b[1], x4 = b[2], y4 = b[3];
        double d = ((x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4));
        if (d != 0) {
            Point pt = new Point();
            pt.x = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
            pt.y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
            return pt;
        }
        else
            return new Point(-1, -1);
    }

    public static Mat hsv_answer(@NonNull Bitmap img, Boolean flag) {
        // flag false, return the contour drawing image
        // else return the thresholded image
        int w = img.getWidth(), h = img.getHeight();
        Mat inputMat = new Mat(h, w, CvType.CV_8UC1);
        Mat inputMatCopy = new Mat(h,w, CvType.CV_8UC1);
        Utils.bitmapToMat(img, inputMat);

        List<Mat> hsv_channel = new ArrayList<>();
        Mat gray = new Mat(h, w, CvType.CV_8UC1);
        Imgproc.cvtColor(inputMat, gray, Imgproc.COLOR_BGR2GRAY);


        // TODO: Find better edge detection methods
        gray = pre_proc1(gray);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(gray.clone(), contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                return Double.compare(abs(Imgproc.contourArea(o2)), abs(Imgproc.contourArea(o1)));
            }
        });

//        Bitmap outputBitmap = Bitmap.createBitmap((int) w, (int) h, Bitmap.Config.ARGB_8888);

        List<MatOfPoint> squares = new ArrayList<>();
        List<MatOfPoint> hulls = new ArrayList<>();
        MatOfInt hull = new MatOfInt();
        MatOfPoint2f approx = new MatOfPoint2f();
        approx.convertTo(approx, CvType.CV_32F);
        int x = 0;

        for (MatOfPoint contour: contours) {
            // Convex hull of border
            Imgproc.convexHull(contour, hull);

            // Calculating new outline points with convex hull
            Point[] contourPoints = contour.toArray();
            int[] indices = hull.toArray();
            List<Point> newPoints = new ArrayList<>();
            for (int index : indices) {
                newPoints.add(contourPoints[index]);
            }
            MatOfPoint2f contourHull = new MatOfPoint2f();
            contourHull.fromList(newPoints);

            // Polygon fitting convex hull border (less accurate fitting at this point)
            Imgproc.approxPolyDP(contourHull, approx, Imgproc.arcLength(contourHull, true)*0.02, true);

            x++;
            // Drawing only the top 5 area contours/polygon fits
            if(x == 5) break;

            // To draw the contours directly
            Imgproc.drawContours(inputMat, contours, contours.indexOf(contour), new Scalar(255,255,255), 10);

            Log.v("Rohan", "Contour Index, Area: " + contours.indexOf(contour) + " " + Imgproc.contourArea(contour));
            MatOfPoint points = new MatOfPoint( approx.toArray() );

            MatOfPoint largestOutline = new  MatOfPoint();
            approx.convertTo(largestOutline, CvType.CV_32S);

            List<MatOfPoint> largestList = new ArrayList<>();
            largestList.add(largestOutline);

            // To draw the polygon fit
            Imgproc.drawContours(inputMatCopy, largestList, 0,  new Scalar(255, 0, 255), 2);
        }

        if(flag) { // used in the getCorners function to determine corners
            return gray;
        }
        else { // to be appeared at modified image screen
//            return gray; // the h channel after thresholding
//            return inputMatCopy; // to check the polygon fits
            return inputMat; // to check the contour drawings
        }

    }





    public static Mat pre_proc1(Mat originalMat){
        int w = originalMat.cols(), h = originalMat.rows();
        Mat dst = new Mat(h, w, CvType.CV_8UC1);
//        Utils.bitmapToMat(img, originalMat);

        Mat morph_kernel = new Mat(new Size(10, 10), CvType.CV_8UC1, new Scalar(255));

        Log.v("ash", "blur");
//        Imgproc.GaussianBlur(originalMat, dst, new Size(7, 7),0);
//        Imgproc.bilateralFilter(originalMat,dst,7,75,75);
        Imgproc.medianBlur(originalMat,dst,7);
        Core.normalize(dst, originalMat, 0, 255, Core.NORM_MINMAX);

        Log.v("ash", "trunc");
        // step 2.
        // As most papers are bright in color, we can use truncation to make it uniformly bright.
//        Imgproc.threshold(originalMat,originalMat, 160,255,Imgproc.THRESH_TOZERO);
//        Imgproc.threshold(originalMat,originalMat,180,255,Imgproc.THRESH_TRUNC);

//        Log.v("ash", "canny");
        // step 3.
        // After above preprocessing, canny edge detection can now work much better.
        Imgproc.Canny(originalMat, originalMat, 70, 200,3,false);
//        Imgproc.threshold(originalMat, originalMat,70,255,Imgproc.THRESH_OTSU);

        Log.v("ash", "thresh");
        // step 4.
        // Cutoff the remaining weak edges
        Imgproc.threshold(originalMat,originalMat,155,255,Imgproc.THRESH_TOZERO);

        Log.v("ash", "morph");
        // step 5.
        // Closing - closes small gaps. Completes the edges on canny image; AND also reduces stringy lines near edge of paper.
        Imgproc.morphologyEx(originalMat, originalMat, Imgproc.MORPH_CLOSE, morph_kernel, new Point(-1,-1),2);
        Mat morph = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.dilate(originalMat, originalMat, morph, new Point(-1, -1), 2, 1, new Scalar(1));
        return originalMat;
    }



    public static List<PointF> getCorners(@NonNull Bitmap img){
        int w = img.getWidth(), h = img.getHeight();
        Log.v("ash", "getCorners");
        Mat hc = hsv_answer(img, true);
        Log.v("ash", "Corners");
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();


        Imgproc.findContours(hc, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        List<MatOfPoint> squares = new ArrayList<>();
        List<MatOfPoint> hulls = new ArrayList<>();
        MatOfInt hull = new MatOfInt();
        MatOfPoint2f approx = new MatOfPoint2f();
        approx.convertTo(approx, CvType.CV_32F);
//        Log.d("Rohan", "Contours number: " + contours.size());
//        int ff = 0, fs = 0, ft = 0;
        for (MatOfPoint contour: contours) {
            // Convex hull of border
            Imgproc.convexHull(contour, hull);

            // Calculating new outline points with convex hull
            Point[] contourPoints = contour.toArray();
            int[] indices = hull.toArray();
            List<Point> newPoints = new ArrayList<>();
            for (int index : indices) {
                newPoints.add(contourPoints[index]);
            }
            MatOfPoint2f contourHull = new MatOfPoint2f();
            contourHull.fromList(newPoints);

            // Polygon fitting convex hull border (less accurate fitting at this point)
            Imgproc.approxPolyDP(contourHull, approx, Imgproc.arcLength(contourHull, true)*0.1, true);

            // A convex quadrilateral with an area greater than a certain threshold and a quadrilateral with angles close to right angles is selected
            MatOfPoint approxf1 = new MatOfPoint();
            approx.convertTo(approxf1, CvType.CV_32S);
//            if(approx.rows() == 4) ff++;
//            if(abs(Imgproc.contourArea(approx)) > 40000) fs++;
//            if( Imgproc.isContourConvex(approxf1)) ft++;
            if (approx.rows() == 4 && abs(Imgproc.contourArea(approx)) > 100000 &&
                    Imgproc.isContourConvex(approxf1)) {
//                double maxCosine = 0, minCosine = 1;
//                for (int j = 2; j < 5; j++) {
//                    double cosine = abs(getAngle(approxf1.toArray()[j%4], approxf1.toArray()[j-2], approxf1.toArray()[j-1]));
//                    maxCosine = Math.max(maxCosine, cosine);
//                    minCosine = Math.min(minCosine, cosine);
//                }
//                // The angle is about 72 degrees
//                if (maxCosine < 0.705 && minCosine > -0.705) {
                    MatOfPoint tmp = new MatOfPoint();
                    contourHull.convertTo(tmp, CvType.CV_32S);
                    squares.add(approxf1);
                    hulls.add(tmp);
//                }
            }
        }

//        Log.d("Rohan", "ff count: " + ff);
//        Log.d("Rohan", "fs count: " + fs);
//        Log.d("Rohan", "ft count: " + ft);
        Log.d("Rohan", "Squares count: " + squares.size());
        Log.d("Rohan", "Hulls count: " + hulls.size());
        List<Point> corners = new ArrayList<>();
        int index = findLargestSquare(squares);
        if(index==-1){
            return point2pointF(corners, img.getWidth(), img.getHeight());
        }
        MatOfPoint largest_square = squares.get(index);
        MatOfPoint contourHull = hulls.get(index);
        MatOfPoint2f tmp = new MatOfPoint2f();
        contourHull.convertTo(tmp, CvType.CV_32F);
        Imgproc.approxPolyDP(tmp, approx, 3, true);
        List<Point> newPointList = new ArrayList<>();
        double maxL = Imgproc.arcLength(approx, true) * 0.02;
        for (Point p : approx.toArray()) {
            if (!(getSpacePointToPoint(p, largest_square.toList().get(0)) > maxL &&
                    getSpacePointToPoint(p, largest_square.toList().get(1)) > maxL &&
                    getSpacePointToPoint(p, largest_square.toList().get(2)) > maxL &&
                    getSpacePointToPoint(p, largest_square.toList().get(3)) > maxL)) {
                newPointList.add(p);
            }
        }

        // Find the remaining vertex links with four edges larger than 2 * maxL as the four edges of a quadrilateral object
        List<double[]> lines = new ArrayList<>();
        for (int i = 0; i < newPointList.size(); i++) {
            Point p1 = newPointList.get(i);
            Point p2 = newPointList.get((i+1) % newPointList.size());
            if (getSpacePointToPoint(p1, p2) > 2 * maxL) {
                lines.add(new double[]{p1.x, p1.y, p2.x, p2.y});
            }
        }
        // Calculates the intersection of two adjacent edges of the four edges, the four vertices of the object
        for (int i = 0; i < lines.size(); i++) {
            Point corner = computeIntersect(lines.get(i),lines.get((i+1) % lines.size()));
            corners.add(corner);
        }
        return point2pointF(corners, img.getWidth(), img.getHeight());
    }

    public static List<PointF> point2pointF(List<Point> corners, int width, int height){
        List<PointF> points = new ArrayList<>();
        for(Point p : corners){
            points.add(new PointF(((float)p.x / width), ((float)p.y / height)));
        }
        return points;
    }
}
