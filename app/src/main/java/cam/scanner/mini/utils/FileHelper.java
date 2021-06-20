package cam.scanner.mini.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import cam.scanner.mini.App;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

// TODO: Use services instead of AsyncTask
public class FileHelper {
    private static final String TAG = FileHelper.class.getCanonicalName();

    public static void saveImage(String fileName, Bitmap bitmap, OnImageSaveListener listener) {
        new SaveBitmap(fileName, bitmap, listener).execute();
    }

    public interface OnImageSaveListener {
        void onImageSaveComplete();
    }

    private static class SaveBitmap extends AsyncTask<Void, Void, Void> {
        private String mFileName;
        private Bitmap mBitmap;
        private OnImageSaveListener mListener;
        private SaveBitmap(String fileName, Bitmap bitmap, OnImageSaveListener imageSaveListener) {
            mFileName   = fileName;
            mBitmap     = bitmap;
            mListener   = imageSaveListener;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                FileOutputStream fos = App.getContext().openFileOutput(mFileName, Context.MODE_PRIVATE);
                mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
                Log.v(TAG, e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (mListener != null) {
                mListener.onImageSaveComplete();
            }
        }
    }

    public static void loadImage(String filename, OnImageLoadListener listener) {
        new LoadBitmap(filename, listener).execute();
    }

    public interface OnImageLoadListener {
        void onImageLoadComplete(Bitmap bitmap);
    }

    private static class LoadBitmap extends AsyncTask<Void, Void, Bitmap> {
        private String mFileName;
        private OnImageLoadListener mListener;
        private LoadBitmap(String fileName, OnImageLoadListener imageLoadListener) {
            mFileName   = fileName;
            mListener   = imageLoadListener;
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            Bitmap bitmap = null;
            try {
                File file = new File(App.getContext().getFilesDir(), mFileName);
                bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.v(TAG, e.toString());
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (mListener != null) {
                mListener.onImageLoadComplete(bitmap);
            }
        }
    }

    public static void deleteImage(String filename, OnImageDeleteListener listener) {
        new DeleteBitmap(filename, listener).execute();
    }

    public interface OnImageDeleteListener {
        void onImageDeleteComplete(Boolean success);
    }

    private static class DeleteBitmap extends AsyncTask<Void, Void, Boolean> {
        private String mFileName;
        private OnImageDeleteListener mListener;
        private DeleteBitmap(String fileName, OnImageDeleteListener imageDeleteListener) {
            mFileName   = fileName;
            mListener   = imageDeleteListener;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            boolean success = false;
            File file = new File(App.getContext().getFilesDir(), mFileName);
            if (file.exists()) {
                success = file.delete();
            }

            return success;
        }

        @Override
        protected void onPostExecute(Boolean b) {
            if (mListener != null) {
                mListener.onImageDeleteComplete(b);
            }
        }
    }
}
