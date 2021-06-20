package cam.scanner.mini;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import cam.scanner.mini.R;

import cam.scanner.mini.customadapters.FiltersAdapter;
import cam.scanner.mini.imageprocessing.RotateHelper;
import cam.scanner.mini.utils.BufferedImagesHelper;
import cam.scanner.mini.utils.Constants;
import com.github.chrisbanes.photoview.PhotoView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class EditImageActivity2 extends AppCompatActivity {
    private Context mContext;
    private PhotoView mImageView;
    private Button mTuneButton;
    private Button mFiltersButton;
    private RelativeLayout mTuningLayout;
    private SeekBar mBrightnessSeekBar;
    private SeekBar mContrastSeekBar;
    private RecyclerView mFiltersLayout;
    private Bitmap mImage;
    private BufferedImagesHelper.BufferedImage mBufferedImage;

    private int mNumRotations           = 0;
    private boolean mIsShowingTuning    = false;
    private boolean mIsShowingFilters   = false;

    private static final String TAG = EditImageActivity2.class.getCanonicalName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_image2);

        mBufferedImage  = null;
        mImage          = getCroppedImage();
        if (mImage == null) {
            closeActivity("Image not found");
            return;
        }

        mContext            = this;
        mImageView          = findViewById(R.id.edit_image2_image_view);
        mTuneButton         = findViewById(R.id.edit_image2_tune_button);
        mFiltersButton      = findViewById(R.id.edit_image2_filter_button);
        mTuningLayout       = findViewById(R.id.edit_image2_tuning_layout);
        mBrightnessSeekBar  = findViewById(R.id.edit_image2_brightness_seek_bar);
        mContrastSeekBar    = findViewById(R.id.edit_image2_contrast_seek_bar);
        mFiltersLayout      = findViewById(R.id.edit_image2_filters_layout);

        mImageView.setImageBitmap(mImage);
        setupSeekBars();
        setupFiltersLayout();
    }

    private Bitmap getCroppedImage() {
        if (BufferedImagesHelper.getBufferedImages().size() < 1)  {
            return null;
        }

        mBufferedImage = BufferedImagesHelper.getBufferedImages().get(0);
        return mBufferedImage.getModifiedImage();
    }

    private void setupSeekBars() {
        mContrastSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int h = mImage.getHeight(), w = mImage.getWidth();
                Mat src = new Mat(h, w, CvType.CV_8UC1);
                Utils.bitmapToMat(mImage, src);

                double a = (progress-50);
                a = a*1.5;
//                src.convertTo(src,-1,a,0);
                double f = (131.0 * (a + 127.0)) / (127 * (131 - a));
                double alpha_c = f;
                double gamma_c = 127*(1-f);
                Core.addWeighted(src,alpha_c,src,0,gamma_c,src);
                Log.v("ash",a+" "+progress+ " "+alpha_c+" "+gamma_c);
                Bitmap outputImage = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(src, outputImage);
                mImageView.setImageBitmap(outputImage);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        mBrightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double a = (progress-50);
                a/=60;
                a += 1;
//                double g = shad;

                int h = mImage.getHeight(), w = mImage.getWidth();
                Mat src = new Mat(h, w, CvType.CV_8UC1);
                Utils.bitmapToMat(mImage, src);

                double b = (progress-50)*3;
                if(b < 0){
                    Imgproc.cvtColor(src,src,Imgproc.COLOR_BGR2HSV);
                    Core.multiply(src,new Scalar(1,1,a),src);
//                Core.add(src,new Scalar(0,0,b), src);
                    Imgproc.cvtColor(src,src,Imgproc.COLOR_HSV2BGR);
                }
                else {
                    src.convertTo(src, -1, 1, b);
                }

//                Log.v("ash",a+" "+progress);
                Bitmap outputImage = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(src, outputImage);
                mImageView.setImageBitmap(outputImage);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    private void setupFiltersLayout() {
        FiltersAdapter adapter = new FiltersAdapter(mContext, mImage, new FiltersAdapter.onItemPressListener() {
            @Override
            public void onItemClick(Bitmap filteredBitmap) {
                mImage = RotateHelper.rotateImage(filteredBitmap, Constants.ANTI_CLOCK_WISE_ROTATION_DEGREES * mNumRotations);
                mImageView.setImageBitmap(mImage);
                resetTuning();
            }
        });

        mFiltersLayout.setHasFixedSize(true);
        mFiltersLayout.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
        mFiltersLayout.setAdapter(adapter);
    }

    private void closeActivity(String reason) {
        if (reason != null) {
            Log.v(TAG, reason);
        }
        finish();
    }

    public void onClick(@NonNull View view) {
        switch (view.getId()) {
            case R.id.edit_image2_back_button: {
                closeActivity("Pressed Back button");
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                break;
            }
            case R.id.edit_image2_rotate_button: {
                onClickRotate();
                break;
            }
            case R.id.edit_image2_filter_button: {
                onClickFiltersButton();
                break;
            }
            case R.id.edit_image2_tune_button: {
                onClickTuneButton();
                break;
            }
            case R.id.edit_image2_save_button: {
                onClickSaveButton();
                break;
            }
            case R.id.edit_image2_reset_button: {
                resetTuning();
                break;
            }
        }
    }

    private void onClickFiltersButton() {
        if (mIsShowingFilters) {
            hideFilters();
        } else {
            showFilters();
        }
        hideTuning();
    }

    private void showFilters() {
        mIsShowingFilters = true;
        mFiltersLayout.setVisibility(View.VISIBLE);
        mFiltersButton.setTextColor(Color.BLACK);
        mFiltersButton.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.ic_filter_black), null, null);
    }

    private void hideFilters() {
        mIsShowingFilters = false;
        mFiltersLayout.setVisibility(View.GONE);
        mFiltersButton.setTextColor(Color.WHITE);
        mFiltersButton.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.ic_filter), null, null);
    }

    private void onClickTuneButton() {
        if (mIsShowingTuning) {
            hideTuning();
        } else {
            showTuning();
        }
        hideFilters();
    }

    private void resetTuning() {
        mContrastSeekBar.setProgress(50);
        mBrightnessSeekBar.setProgress(50);
    }

    private void showTuning() {
        mIsShowingTuning = true;
        mTuningLayout.setVisibility(View.VISIBLE);
        mTuneButton.setTextColor(Color.BLACK);
        mTuneButton.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.ic_tune_black), null, null);
    }

    private void hideTuning() {
        mIsShowingTuning = false;
        mTuningLayout.setVisibility(View.GONE);
        mTuneButton.setTextColor(Color.WHITE);
        mTuneButton.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.ic_tune), null, null);
    }

    private void onClickSaveButton() {
        BufferedImagesHelper.updateImageAt(0, new BufferedImagesHelper.BufferedImage(
                mBufferedImage.getOriginalImage(), mImage, mBufferedImage.getCorners()
        ));
        Intent thisIntent = getIntent();
        setResult(Activity.RESULT_OK, thisIntent);
        closeActivity("Done Editing image 2");
    }

    private void onClickRotate() {
        mNumRotations = (mNumRotations + 1) % Constants.NUM_CORNERS_OF_POLYGON;
        mImage = RotateHelper.rotateImage(mImage, Constants.ANTI_CLOCK_WISE_ROTATION_DEGREES);
        mImageView.setImageBitmap(mImage);
        hideFilters();
        hideTuning();
    }
}
