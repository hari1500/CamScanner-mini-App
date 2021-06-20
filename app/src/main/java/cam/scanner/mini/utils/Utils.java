package cam.scanner.mini.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.net.Uri;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import cam.scanner.mini.App;
import cam.scanner.mini.components.PolygonView;
import cam.scanner.mini.localdatabase.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Utils {
    public static Map<Integer, PointF> getPointFMap(List<PointF> corners) {
        Map<Integer, PointF> pointFMap;
        if (corners != null && corners.size() == Constants.NUM_CORNERS_OF_POLYGON) {
            pointFMap = PolygonView.getOrderedPoints(corners);
            if (!PolygonView.isValidShape(pointFMap)) {
                pointFMap = PolygonView.getDefaultPoints();
            }
        } else {
            pointFMap = PolygonView.getDefaultPoints();
        }
        return pointFMap;
    }

    public static List<PointF> toListOfPointF(Map<Integer, PointF> pointFMap) {
        if (pointFMap == null || pointFMap.size() != Constants.NUM_CORNERS_OF_POLYGON) {
            return null;
        }

        List<PointF> pointFList = new ArrayList<>();
        for (int i = 0; i < Constants.NUM_CORNERS_OF_POLYGON; ++i) {
            pointFList.add(pointFMap.get(i));
        }
        return pointFList;
    }

    public static void showToast(Context context, String text, int duration, int gravity) {
        Toast toast = Toast.makeText(context, text, duration);
        toast.setGravity(gravity, 0, 0);
        toast.show();
    }

    public static void showToast(Context context, String text, int duration) {
        Toast.makeText(context, text, duration).show();
    }

    public static boolean isPermissionGranted(@NonNull String permission) {
        return ContextCompat.checkSelfPermission(App.getContext(), permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static Uri createPdf(Document document) throws DocumentException, IOException {
        String pdfFileName = App.getContext().getCacheDir().toString() + "/" + document.getName() + ".pdf";
        File file = new File(pdfFileName);
        if(file.exists()) {
            file.delete();
        }
        com.itextpdf.text.Document document1 = new com.itextpdf.text.Document();
        PdfWriter.getInstance(document1, new FileOutputStream(pdfFileName));
        document1.open();
        for(Long id : document.getPageIds()) {
            String ImageFileName = App.getContext().getFilesDir().toString() + "/" + String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, document.getIdAsString(), id);
            Image image = Image.getInstance(ImageFileName);
            float scale1 = ((document1.getPageSize().getHeight() - document1.topMargin() - document1.bottomMargin()) / image.getHeight());
            float scale2 = ((document1.getPageSize().getWidth() - document1.leftMargin() - document1.rightMargin()) / image.getWidth());
            float scaler = Math.min(scale1, scale2);
            image.scalePercent(scaler * 100);
            float x = (PageSize.A4.getWidth() - image.getScaledWidth()) / 2;
            float y = (PageSize.A4.getHeight() - image.getScaledHeight()) / 2;
            image.setAbsolutePosition(x, y);
            image.setAlignment(Image.ALIGN_CENTER);
            document1.newPage();
            document1.add(image);
        }
        document1.close();
        File pdfFile = new File(pdfFileName);
        return FileProvider.getUriForFile(App.getContext(), "cam.scanner.mini.fileprovider", pdfFile);
    }

    public static Uri createLongImage(final Document document) throws IOException {
        List<Bitmap> parts = new ArrayList<>();
        int totalHeight = 0, maxWidth = 0;
        for(final Long pageId : document.getPageIds()){
            Pair<Bitmap, Bitmap> bitmaps = App.retrieveBitmaps(document.getId(), pageId);
            if(bitmaps == null){
                String fileName = String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, document.getIdAsString(), pageId);
                File file = new File(App.getContext().getFilesDir(), fileName);
                Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
                App.cacheBitmaps(document.getId(), pageId, null, bitmap);
                bitmaps =  App.retrieveBitmaps(document.getId(), pageId);
            }
            parts.add(bitmaps.second);
            totalHeight += bitmaps.second.getHeight();
            maxWidth = Math.max(maxWidth,bitmaps.second.getWidth());
        }
        Bitmap result = Bitmap.createBitmap(maxWidth, totalHeight, parts.get(0).getConfig());
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        int height = 0;
        for(Bitmap bitmap : parts){
            canvas.drawBitmap(bitmap, (maxWidth - bitmap.getWidth()) >> 1, height, paint);
            height += bitmap.getHeight();
        }
        String imageFileName = document.getName() + ".jpeg";
        File file = new File(App.getContext().getCacheDir(), imageFileName);
        FileOutputStream fos = new FileOutputStream(file);
        result.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        fos.close();
        return FileProvider.getUriForFile(App.getContext(), "cam.scanner.mini.fileprovider", file);
    }

    public static Uri createLongImage(List<Long> pageIds, Long docId) throws IOException {
        List<Bitmap> parts = new ArrayList<>();
        int totalHeight = 0, maxWidth = 0;
        Document document = DatabaseHelper.getDocumentById(docId);
        for(final Long pageId : pageIds){
            Pair<Bitmap, Bitmap> bitmaps = App.retrieveBitmaps(docId, pageId);
            if(bitmaps == null){
                String fileName = String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, docId, pageId);
                File file = new File(App.getContext().getFilesDir(), fileName);
                Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
                App.cacheBitmaps(docId, pageId, null, bitmap);
                bitmaps =  App.retrieveBitmaps(docId, pageId);
            }
            parts.add(bitmaps.second);
            totalHeight += bitmaps.second.getHeight();
            maxWidth = Math.max(maxWidth,bitmaps.second.getWidth());
        }
        Bitmap result = Bitmap.createBitmap(maxWidth, totalHeight, parts.get(0).getConfig());
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        int height = 0;
        for(Bitmap bitmap : parts){
            canvas.drawBitmap(bitmap, (maxWidth - bitmap.getWidth()) >> 1, height, paint);
            height += bitmap.getHeight();
        }
        String imageFileName = (document != null ? document.getName() : pageIds.size()) + ".jpeg";
        File file = new File(App.getContext().getCacheDir(), imageFileName);
        FileOutputStream fos = new FileOutputStream(file);
        result.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        fos.close();
        return FileProvider.getUriForFile(App.getContext(), "cam.scanner.mini.fileprovider", file);
    }

    public static void createLongImage(final Document document, OutputStream fos) throws IOException {
        List<Bitmap> parts = new ArrayList<>();
        int totalHeight = 0, maxWidth = 0;
        for(final Long pageId : document.getPageIds()){
            Pair<Bitmap, Bitmap> bitmaps = App.retrieveBitmaps(document.getId(), pageId);
            if(bitmaps == null){
                String fileName = String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, document.getIdAsString(), pageId);
                File file = new File(App.getContext().getFilesDir(), fileName);
                Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
                App.cacheBitmaps(document.getId(), pageId, null, bitmap);
                bitmaps =  App.retrieveBitmaps(document.getId(), pageId);
            }
            parts.add(bitmaps.second);
            totalHeight += bitmaps.second.getHeight();
            maxWidth = Math.max(maxWidth,bitmaps.second.getWidth());
        }
        Bitmap result = Bitmap.createBitmap(maxWidth, totalHeight, parts.get(0).getConfig());
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        int height = 0;
        for(Bitmap bitmap : parts) {
            canvas.drawBitmap(bitmap, (maxWidth - bitmap.getWidth()) >> 1, height, paint);
            height += bitmap.getHeight();
        }
        result.compress(Bitmap.CompressFormat.JPEG, 100, fos);
    }

    public static void createLongImage(List<Long> pageIds, Long docId, OutputStream fos) throws IOException {
        List<Bitmap> parts = new ArrayList<>();
        int totalHeight = 0, maxWidth = 0;
        for(final Long pageId : pageIds) {
            Pair<Bitmap, Bitmap> bitmaps = App.retrieveBitmaps(docId, pageId);
            if(bitmaps == null) {
                String fileName = String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, docId, pageId);
                File file = new File(App.getContext().getFilesDir(), fileName);
                Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
                App.cacheBitmaps(docId, pageId, null, bitmap);
                bitmaps =  App.retrieveBitmaps(docId, pageId);
            }
            parts.add(bitmaps.second);
            totalHeight += bitmaps.second.getHeight();
            maxWidth = Math.max(maxWidth,bitmaps.second.getWidth());
        }
        Bitmap result = Bitmap.createBitmap(maxWidth, totalHeight, parts.get(0).getConfig());
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        int height = 0;
        for(Bitmap bitmap : parts) {
            canvas.drawBitmap(bitmap, (maxWidth - bitmap.getWidth()) >> 1, height, paint);
            height += bitmap.getHeight();
        }
        result.compress(Bitmap.CompressFormat.JPEG, 100, fos);
    }
}
