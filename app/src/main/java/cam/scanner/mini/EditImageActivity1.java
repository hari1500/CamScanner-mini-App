package cam.scanner.mini;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import cam.scanner.mini.components.CropableImageView;
import cam.scanner.mini.components.PolygonView;
import cam.scanner.mini.imageprocessing.CVHelper;
import cam.scanner.mini.imageprocessing.EdgeHelper;
import cam.scanner.mini.imageprocessing.RotateHelper;
import cam.scanner.mini.localdatabase.Page;
import cam.scanner.mini.utils.BufferedImagesHelper;
import cam.scanner.mini.utils.Constants;
import cam.scanner.mini.utils.FileHelper;
import cam.scanner.mini.utils.Utils;

import java.util.Map;

public class EditImageActivity1 extends AppCompatActivity {
    private Context mContext;
    private FrameLayout mImageHolderLayout;
    private CropableImageView mImageView;
    private ProgressBar mProgressBar;
    private PolygonView mPolygonView;
    private Page mPage;
    private Bitmap mOriginalImage;
    private Bitmap mModifiedImage;
    private int mRotation;

    private static final String TAG = EditImageActivity1.class.getCanonicalName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_image1);
        mPage = App.getCurrentPage();
        if (mPage == null) {
            closeActivity("Page not available");
            return;
        }

        mContext            = this;
        mImageHolderLayout  = findViewById(R.id.edit_image1_holder_layout);
        mImageView          = findViewById(R.id.edit_image1_image_view);
        mProgressBar        = findViewById(R.id.edit_image1_progress_bar);
        mPolygonView        = null;
        mOriginalImage      = null;
        mModifiedImage      = null;
        mRotation           = 0;

        setBitmap();
    }

    private void closeActivity(String reason) {
        if (reason != null) {
            Log.v(TAG, reason);
        }
        finish();
    }

    private void setBitmap() {
        Pair<Bitmap, Bitmap> bitmapsPair = App.retrieveBitmaps(mPage.getDocumentId(), mPage.getId());
        if (bitmapsPair != null && bitmapsPair.first != null) {
            mOriginalImage = bitmapsPair.first;
            mModifiedImage = bitmapsPair.first;
            mImageView.setImageBitmap(mOriginalImage);
            setupPolygonView();
            return;
        }

        mProgressBar.setVisibility(View.VISIBLE);
        FileHelper.loadImage(
                String.format(Constants.ORIGINAL_IMAGE_PATH_FORMAT, mPage.getDocumentIdAsString(), mPage.getIdAsString()),
                new FileHelper.OnImageLoadListener() {
                    @Override
                    public void onImageLoadComplete(Bitmap bitmap) {
                        App.cacheBitmaps(mPage.getDocumentId(), mPage.getId(), bitmap, null);
                        mOriginalImage = bitmap;
                        mModifiedImage = bitmap;
                        mImageView.setImageBitmap(mModifiedImage);
                        mProgressBar.setVisibility(View.GONE);
                        setupPolygonView();
                    }
                }
        );
    }

    private void setupPolygonView() {
        if (mModifiedImage == null || mImageView == null) {
            return;
        }
        mImageView.removeOnDrawListener();
        mImageView.setOnDrawListener(new CropableImageView.OnDrawListener() {
            @Override
            public void onDraw(int x, int y, int width, int height) {
                Map<Integer, PointF> pointFMap;
                if (mPolygonView == null) {
                    pointFMap = Utils.getPointFMap(mPage.getSelectedCorners());
                } else {
                    pointFMap = mPolygonView.getPoints();
                }
                mPolygonView = new PolygonView(mContext, x, y, width, height, null);
                mPolygonView.setPoints(pointFMap);
                mPolygonView.setBitmap(mModifiedImage);
                mImageHolderLayout.addView(mPolygonView);
            }
        });
    }

    private void resetPolygonView() {
        if (mImageHolderLayout == null || mPolygonView == null || mImageView == null) {
            return;
        }
        mImageHolderLayout.removeView(mPolygonView);
        setupPolygonView();
    }

    public void onClick(@NonNull View view) {
        if (mModifiedImage == null) {
            return;
        }

        switch (view.getId()) {
            case R.id.edit_image1_back_button: {
                closeActivity("Pressed Back button");
                break;
            }
            case R.id.edit_image1_rotate_ac_button: {
                onClickRotate(Constants.ANTI_CLOCK_WISE_ROTATION_DEGREES);
                break;
            }
            case R.id.edit_image1_rotate_c_button: {
                onClickRotate(Constants.CLOCK_WISE_ROTATION_DEGREES);
                break;
            }
            case R.id.edit_image1_auto_crop_button: {
                onClickAutoCrop();
                break;
            }
            case R.id.edit_image1_remove_crop_button: {
                mPolygonView.reset();
                break;
            }
            case R.id.edit_image1_next_button: {
                onClickNextButton();
                break;
            }
        }
    }

    private void onClickNextButton() {
        if (!PolygonView.isValidShape(mPolygonView.getPoints())) {
            displayWarning();
            return;
        }
        Bitmap croppedImage = CVHelper.applyPerspective(mModifiedImage, mPolygonView.getPoints());
//         Mat croppedImageMat = EdgeHelper.hsv_answer(mModifiedImage, false);
//        Bitmap croppedImage = Bitmap.createBitmap(croppedImageMat.width(), croppedImageMat.height(), Bitmap.Config.ARGB_8888);
//        org.opencv.android.Utils.matToBitmap(croppedImageMat, croppedImage);
        BufferedImagesHelper.clearBufferedImages();
        BufferedImagesHelper.addImageToBuffer(
                mOriginalImage, croppedImage,
                RotateHelper.rotatePolygonViewPoints(mPolygonView.getPoints(), ((360 - (mRotation % 360)) % 360))
        );

        Intent intent = new Intent(mContext, EditImageActivity2.class);
        startActivityForResult(intent, Constants.LAUNCH_EDIT_IMAGE_ACTIVITY2_FROM_EDIT_IMAGE_ACTIVITY1);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void displayWarning() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Warning");
        builder.setMessage("Cannot crop current image\nPlease change selected corners");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void onClickRotate(int degrees) {
        mRotation += degrees;
        mModifiedImage = RotateHelper.rotateImage(mModifiedImage, degrees);
        if (mPolygonView != null) {
            mPolygonView.setPoints(RotateHelper.rotatePolygonViewPoints(mPolygonView.getPoints(), degrees));
            mPolygonView.setBitmap(mModifiedImage);
        } else {
            Log.v(TAG, "PolygonView must not be null");
        }

        mImageView.setImageBitmap(mModifiedImage);
        resetPolygonView();
    }

    private void onClickAutoCrop() {
        mPolygonView.setPoints(Utils.getPointFMap(EdgeHelper.getCorners(mModifiedImage)));
        resetPolygonView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.LAUNCH_EDIT_IMAGE_ACTIVITY2_FROM_EDIT_IMAGE_ACTIVITY1 && resultCode == Activity.RESULT_OK) {
            Intent thisIntent = getIntent();
            setResult(Activity.RESULT_OK, thisIntent);
            closeActivity("Done Editing image 1");
        }
    }
}
