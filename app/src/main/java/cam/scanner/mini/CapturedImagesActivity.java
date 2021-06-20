package cam.scanner.mini;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;

import cam.scanner.mini.components.LockableViewPager;
import cam.scanner.mini.components.PolygonView;
import cam.scanner.mini.customadapters.CapturedImagesAdapter;
import cam.scanner.mini.imageprocessing.RotateHelper;
import cam.scanner.mini.utils.BufferedImagesHelper;
import cam.scanner.mini.imageprocessing.CVHelper;
import cam.scanner.mini.utils.Constants;

import java.util.ArrayList;
import java.util.Map;

public class CapturedImagesActivity extends AppCompatActivity {
    private Context mContext;
    private LockableViewPager mViewPager;
    private CapturedImagesAdapter mAdapter;
    private int requestedRetakeInd = -1;

    private static final String TAG = CapturedImagesActivity.class.getCanonicalName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captured_images);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext    = this;
        mViewPager  = findViewById(R.id.captured_images_view_pager);
        mAdapter    = new CapturedImagesAdapter(mContext, mViewPager);
        mViewPager.setAdapter(mAdapter);
    }

    public void onClick(@NonNull View view) {
        switch (view.getId()) {
            case R.id.captured_images_delete_button: {
                onClickDelete();
                break;
            }
            case R.id.captured_images_retake_button: {
                onClickRetake();
                break;
            }
            case R.id.captured_images_rotate_ac_button: {
                onClickRotate(Constants.ANTI_CLOCK_WISE_ROTATION_DEGREES);
                break;
            }
            case R.id.captured_images_rotate_c_button: {
                onClickRotate(Constants.CLOCK_WISE_ROTATION_DEGREES);
                break;
            }
            case R.id.captured_images_remove_crop_button: {
                BufferedImagesHelper.getBufferedImages().get(mViewPager.getCurrentItem()).resetCorners();
                int currentPosition = mViewPager.getCurrentItem();
                mViewPager.setAdapter(null);
                mViewPager.setAdapter(mAdapter);
                mViewPager.setCurrentItem(currentPosition);
                break;
            }
            case R.id.captured_images_done_button: {
                onClickDone();
                break;
            }
        }
    }

    private void onClickDone() {
        if(!isCornersValid()) {
            displayWarning();
            return;
        }

        final Dialog dialog = new Dialog(mContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_processing);
        dialog.setCancelable(false);
        dialog.show();

        new CropImages(
                new OnCropDoneListener() {
                    @Override
                    public void onCropDone() {
                        dialog.cancel();
                        Intent thisIntent = getIntent();
                        setResult(Activity.RESULT_OK, thisIntent);
                        closeActivity("Done modifying images");
                    }
                }
        ).execute();
    }

    private boolean isCornersValid() {
        ArrayList<BufferedImagesHelper.BufferedImage> bufferedImages = BufferedImagesHelper.getBufferedImages();
        for (int i = 0; i < bufferedImages.size(); ++i) {
            BufferedImagesHelper.BufferedImage bufferedImage = bufferedImages.get(i);
            PolygonView polygonView = bufferedImage.getPolygonView();
            if (polygonView != null && !PolygonView.isValidShape(polygonView.getPoints())) {
                return false;
            }
        }
        return true;
    }

    private void displayWarning() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Warning");
        builder.setMessage("Cannot crop current images\nPlease change selected corners");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void onClickDelete() {
        final int currentPosition = mViewPager.getCurrentItem();

        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Warning");
        builder.setMessage("Are you sure, you wanna delete?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BufferedImagesHelper.deleteImageAt(currentPosition);
                if (BufferedImagesHelper.getBufferedImages().size() == 0) {
                    closeActivity("No images to display");
                    return;
                }
                mViewPager.setAdapter(null);
                mViewPager.setAdapter(mAdapter);
                mViewPager.setCurrentItem(currentPosition == 0 ? 0 : currentPosition - 1);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void onClickRotate(int degrees) {
        int currentPos = mViewPager.getCurrentItem();
        BufferedImagesHelper.BufferedImage bufferedImage = BufferedImagesHelper.getBufferedImages().get(currentPos);

        // Rotating image
        Bitmap modfied = RotateHelper.rotateImage(bufferedImage.getModifiedImage(), degrees);

        // Rotating PolygonView
        PolygonView polygonView = bufferedImage.getPolygonView();
        Map<Integer, PointF> pointFMap = null;
        if (polygonView != null) {
            pointFMap = RotateHelper.rotatePolygonViewPoints(polygonView.getPoints(), degrees);
            polygonView.setPoints(pointFMap);
            polygonView.setBitmap(modfied);
        } else {
            Log.v(TAG, "PolygonView must not be null");
        }

        BufferedImagesHelper.BufferedImage updated = new BufferedImagesHelper.BufferedImage(
                bufferedImage.getOriginalImage(), modfied, pointFMap
        );
        updated.setPolygonView(polygonView);
        updated.setRotation(bufferedImage.getRotation() + degrees);
        BufferedImagesHelper.updateImageAt(currentPos, updated);
        mViewPager.setAdapter(null);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(currentPos);
    }

    private void onClickRetake() {
        Intent intent = new Intent(mContext, RetakeImageActivity.class);
        startActivityForResult(intent, Constants.LAUNCH_RETAKE_IMAGE_ACTIVITY_FROM_CAPTURED_IMAGES_ACTIVITY);
        requestedRetakeInd = mViewPager.getCurrentItem();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.LAUNCH_RETAKE_IMAGE_ACTIVITY_FROM_CAPTURED_IMAGES_ACTIVITY && resultCode == Activity.RESULT_OK) {
            Bitmap bitmap = BufferedImagesHelper.getRetakeResultantImage();
            if (bitmap != null && requestedRetakeInd >= 0 && requestedRetakeInd < BufferedImagesHelper.getBufferedImages().size()) {
                BufferedImagesHelper.updateImageAt(requestedRetakeInd, new BufferedImagesHelper.BufferedImage(bitmap, bitmap, null));
            }

            mViewPager.setAdapter(null);
            mViewPager.setAdapter(mAdapter);
            mViewPager.setCurrentItem(requestedRetakeInd);
        }
    }

    private void closeActivity(String reason) {
        if (reason != null) {
            Log.v(TAG, reason);
        }
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        closeActivity("Pressed back button");
        return true;
    }

    private static class CropImages extends AsyncTask<Void, Void, Void> {
        private ArrayList<Map<Integer, PointF>> pointFMapsList =  new ArrayList<>();
        private OnCropDoneListener mListener;

        CropImages(OnCropDoneListener listener) {
            mListener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ArrayList<BufferedImagesHelper.BufferedImage> bufferedImages = BufferedImagesHelper.getBufferedImages();
            for (int i = 0; i < bufferedImages.size(); ++i) {
                PolygonView polygonView = bufferedImages.get(i).getPolygonView();
                pointFMapsList.add(polygonView == null ? null : polygonView.getPoints());
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ArrayList<BufferedImagesHelper.BufferedImage> bufferedImages = BufferedImagesHelper.getBufferedImages();
            for (int i = 0; i < bufferedImages.size(); ++i) {
                BufferedImagesHelper.BufferedImage bufferedImage = bufferedImages.get(i);
                Bitmap oldModifiedImage = bufferedImage.getModifiedImage();
                Map<Integer, PointF> pointFMap = pointFMapsList.get(i);
                if (pointFMap == null) {
                    pointFMap = PolygonView.getDefaultPoints();
                }

                Bitmap modifiedImage = CVHelper.applyPerspective(oldModifiedImage, pointFMap);
                BufferedImagesHelper.updateImageAt(i, new BufferedImagesHelper.BufferedImage(
                        bufferedImage.getOriginalImage(), modifiedImage,
                        RotateHelper.rotatePolygonViewPoints(pointFMap, ((360 - (bufferedImage.getRotation() % 360)) % 360))
                ));
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (mListener != null) {
                mListener.onCropDone();
            }
        }
    }

    public interface OnCropDoneListener {
        public void onCropDone();
    }
}
