package cam.scanner.mini;

import android.app.Application;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.NonNull;

import cam.scanner.mini.localdatabase.Document;
import cam.scanner.mini.localdatabase.Page;
import cam.scanner.mini.utils.BufferedImagesHelper;
import cam.scanner.mini.imageprocessing.CVHelper;
import cam.scanner.mini.utils.DatabaseHelper;
import cam.scanner.mini.utils.ExifHelper;
import cam.scanner.mini.utils.SortComparator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App extends Application {
    private static Application sApplication;
    private static Document sCurrentDocument;
    private static Page sCurrentPage;
    private static Map<Long, Map<Long, Pair<Bitmap, Bitmap>>> sBitmapsCache;
    private static int sDocumentsSortMethod;

    public void onCreate() {
        super.onCreate();
        sApplication        = this;
        sCurrentDocument    = null;
        sCurrentPage        = null;
        sBitmapsCache       = new HashMap<>();
        sDocumentsSortMethod= SortComparator.METHOD_DECREASING_MODIFIED_TIMESTAMP;

        DatabaseHelper.init();
        BufferedImagesHelper.init();
        CVHelper.init();
    }

    public static Application getApplication() {
        return sApplication;
    }

    public static Context getContext() {
        return getApplication().getApplicationContext();
    }

    public static void setCurrentPage(Page page) {
        sCurrentPage = page;
    }

    public static Page getCurrentPage() {
        return sCurrentPage;
    }

    public static void setCurrentDocument(Document document) {
        sCurrentDocument = document;
    }

    public static Document getCurrentDocument() {
        return sCurrentDocument;
    }

    public static int getDocumentsSortMethod() {
        return sDocumentsSortMethod;
    }

    public static void setDocumentsSortMethod(int documentsSortMethod) {
        App.sDocumentsSortMethod = documentsSortMethod;
    }

    public static void cacheBitmaps(long documentId, long pageId, Bitmap originalBitmap, Bitmap modifiedBitmap) {
        if (sBitmapsCache.containsKey(documentId)) {
            if (sBitmapsCache.get(documentId).containsKey(pageId)) {
                Pair<Bitmap, Bitmap> previous = sBitmapsCache.get(documentId).get(pageId);

                if (originalBitmap == null) {
                    originalBitmap = previous.first;
                }
                if (modifiedBitmap == null) {
                    modifiedBitmap = previous.second;
                }
            }
            sBitmapsCache.get(documentId).put(pageId, new Pair<>(originalBitmap, modifiedBitmap));
        } else {
            HashMap<Long, Pair<Bitmap, Bitmap>> bitmapPairHashMap = new HashMap<>();
            bitmapPairHashMap.put(pageId, new Pair<>(originalBitmap, modifiedBitmap));
            sBitmapsCache.put(documentId, bitmapPairHashMap);
        }
    }

    public static Pair<Bitmap, Bitmap> retrieveBitmaps(long documentId, long pageId) {
        Pair<Bitmap, Bitmap> bitmapsPair = null;
        if (sBitmapsCache.containsKey(documentId)) {
            if (sBitmapsCache.get(documentId).containsKey(pageId)) {
                return sBitmapsCache.get(documentId).get(pageId);
            }
        }
        return bitmapsPair;
    }

    public static void removeFromCache(long documentId, long pageId) {
        if (sBitmapsCache.containsKey(documentId)) {
            sBitmapsCache.get(documentId).remove(pageId);
        }
    }

    public static void removeFromCache(long documentId) {
        sBitmapsCache.remove(documentId);
    }

    public static class ProcessGalleryData extends AsyncTask<Void, Void, Void> {
        private Intent mData;
        private ContentResolver mContentResolver;
        private OnProcessGalleryDoneListener mListener;
        private List<Bitmap> bitmaps = new ArrayList<>();
        public ProcessGalleryData(@NonNull Intent data, @NonNull ContentResolver contentResolver, OnProcessGalleryDoneListener listener) {
            mData = data;
            mContentResolver = contentResolver;
            mListener =  listener;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ClipData clipData = mData.getClipData();
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); ++i) {
                    Uri imageUri = clipData.getItemAt(i).getUri();
                    if (imageUri != null && imageUri.getPath() != null) {
                        try {
                            InputStream is = mContentResolver.openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(is);
                            bitmaps.add(ExifHelper.adjustBitmap(getContext(), bitmap, imageUri));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                Uri imageUri = mData.getData();
                if (imageUri != null && imageUri.getPath() != null) {
                    try {
                        InputStream is = mContentResolver.openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        bitmaps.add(ExifHelper.adjustBitmap(getContext(), bitmap, imageUri));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (mListener != null) {
                mListener.onProcessGalleryDone(bitmaps);
            }
        }

        public interface OnProcessGalleryDoneListener {
            public void onProcessGalleryDone(List<Bitmap> bitmaps);
        }
    }
}
