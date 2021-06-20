package cam.scanner.mini.imageprocessing;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;

import cam.scanner.mini.utils.Constants;

import java.util.HashMap;
import java.util.Map;

public class RotateHelper {
    public static Bitmap rotateImage(Bitmap image, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);

        return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
    }

    public static Map<Integer, PointF> rotatePolygonViewPoints(Map<Integer, PointF> pointFMap, int degrees) {
        if (pointFMap.size() != 4) {
            return pointFMap;
        }

        Map<Integer, PointF> rotatedPoints = new HashMap<>();
        if (degrees == Constants.ANTI_CLOCK_WISE_ROTATION_DEGREES) {
            rotatedPoints.put(0, new PointF(pointFMap.get(1).y, 1 - pointFMap.get(1).x));
            rotatedPoints.put(1, new PointF(pointFMap.get(3).y, 1 - pointFMap.get(3).x));
            rotatedPoints.put(2, new PointF(pointFMap.get(0).y, 1 - pointFMap.get(0).x));
            rotatedPoints.put(3, new PointF(pointFMap.get(2).y, 1 - pointFMap.get(2).x));
            return rotatedPoints;
        } else if (degrees == Constants.TWO_ROTATION_DEGREES) {
            rotatedPoints.put(0, new PointF(1 - pointFMap.get(3).x, 1 - pointFMap.get(3).y));
            rotatedPoints.put(1, new PointF(1 - pointFMap.get(2).x, 1 - pointFMap.get(2).y));
            rotatedPoints.put(2, new PointF(1 - pointFMap.get(1).x, 1 - pointFMap.get(1).y));
            rotatedPoints.put(3, new PointF(1 - pointFMap.get(0).x, 1 - pointFMap.get(0).y));
            return rotatedPoints;
        } else if (degrees == Constants.CLOCK_WISE_ROTATION_DEGREES) {
            rotatedPoints.put(0, new PointF(1 - pointFMap.get(2).y, pointFMap.get(2).x));
            rotatedPoints.put(1, new PointF(1 - pointFMap.get(0).y, pointFMap.get(0).x));
            rotatedPoints.put(2, new PointF(1 - pointFMap.get(3).y, pointFMap.get(3).x));
            rotatedPoints.put(3, new PointF(1 - pointFMap.get(1).y, pointFMap.get(1).x));
            return rotatedPoints;
        }

        return pointFMap;
    }
}
