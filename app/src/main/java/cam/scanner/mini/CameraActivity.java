package cam.scanner.mini;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import cam.scanner.mini.R;

import cam.scanner.mini.components.AutoResizableFrameLayout;
import cam.scanner.mini.components.AutoResizableTextureView;
import cam.scanner.mini.utils.BufferedImagesHelper;
import cam.scanner.mini.utils.Constants;
import cam.scanner.mini.utils.RotateOrientationEventListener;
import cam.scanner.mini.utils.Utils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// TODO: Add Auto-Focus option
public class CameraActivity extends AppCompatActivity {
    private Context mContext;
    private ImageButton mSoundImageButton;
    private ImageButton mFlashImageButton;
    private ImageButton mGridImageButton;
    private ImageView mPhotoImageView;
    private ImageView mCloseImageView;
    private ImageView mRecentImageView;
    private TextView mRecentImagesCountTextView;
    private CoordinatorLayout mRecentImagesOuterLayout;
    private AutoResizableFrameLayout mCameraHolderLayout;
    private AutoResizableTextureView mTextureView;
    private ProgressBar mProgressBar;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback;
    private String mCameraId;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundHandlerThread;
    private Size mPreviewSize;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private Size mImageSize;
    private ImageReader mImageReader;
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener;
    private CameraCaptureSession mCaptureSession;
    private Integer mSensorRotation;
    private Integer mTotalRotation;
    private RotateOrientationEventListener mOrientationEventListener;
    private int mCamState;
    private boolean mFlashSupported;
    private boolean mShowGrid;
    private boolean mPlaySound;
    private boolean mShowFlash;
    private SharedPreferences mSharedPreferences;
    private boolean mReachedImageLimit;
    private boolean mManualFocusEngaged;
    private Rect mSensorArraySize;
    private Integer mMaxAFControlRegions;

    private static final String TAG                     = CameraActivity.class.getCanonicalName();
    private static final String THREAD_NAME             = TAG + "HandlerThread";
    private static final String cameraPermission        = Manifest.permission.CAMERA;
    private static final String SHOW_GRID_KEY           = TAG + "ShowGrid";
    private static final String PLAY_SOUND_KEY          = TAG + "PlaySound";
    private static final String SHOW_FLASH_KEY          = TAG + "ShowFlash";
    private static final String FOCUS_TAG               = "FOCUS_TAG";
    private static final int MAX_IMAGES_PER_READER      = 10;
    private static final int STATE_IDLE                 = 0x00;
    private static final int STATE_PREVIEW              = 0x01;
    private static final int STATE_WAIT_LOCK            = 0x02;
    private static final int STATE_WAIT_LOCK_RELEASED   = 0x03;
    private static final int CAMERA_ASPECT_RATIO_WIDTH  = 3;
    private static final int CAMERA_ASPECT_RATIO_HEIGHT = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        if (!isCameraHardwareAvailable()) {
            closeActivity("Hardware not available");
            return;
        }
        BufferedImagesHelper.clearBufferedImages();
        initVars();
        loadSettings();
//        settingTapToFocus();

        mTextureView.setAspectRatio(CAMERA_ASPECT_RATIO_WIDTH, CAMERA_ASPECT_RATIO_HEIGHT);
        mCameraHolderLayout.setAspectRatio(CAMERA_ASPECT_RATIO_WIDTH, CAMERA_ASPECT_RATIO_HEIGHT);
        mCameraHolderLayout.setShowGrid(mShowGrid);
        mGridImageButton.setImageResource(mShowGrid ? R.drawable.ic_grid : R.drawable.ic_grid_off);
        mSoundImageButton.setImageResource(mPlaySound ? R.drawable.ic_sound : R.drawable.ic_sound_off);
        mFlashImageButton.setImageResource(mShowFlash ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);
    }

    private void initVars() {
        mContext                    = this;
        mSoundImageButton           = findViewById(R.id.camera_sound_image_button);
        mFlashImageButton           = findViewById(R.id.camera_flash_image_button);
        mGridImageButton            = findViewById(R.id.camera_grid_image_button);
        mPhotoImageView             = findViewById(R.id.camera_take_photo_image_view);
        mCloseImageView             = findViewById(R.id.camera_close_image_view);
        mRecentImageView            = findViewById(R.id.camera_recent_image_view);
        mRecentImagesCountTextView  = findViewById(R.id.camera_recent_images_count_text_view);
        mRecentImagesOuterLayout    = findViewById(R.id.camera_recent_images_outer_layout);
        mCameraHolderLayout         = findViewById(R.id.camera_middle_layout);
        mTextureView                = findViewById(R.id.camera_texture_view);
        mProgressBar                = findViewById(R.id.camera_progress_bar);
        mSurfaceTextureListener     = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                initCam(width, height);
                connectCam();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.v(TAG, "onSurfaceTextureSizeChanged");
                configurePreview();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        };
        mCameraManager              = null;
        mCameraDeviceStateCallback  = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                mCameraDevice = camera;
                showPreview();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                closeCamera(camera);
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                closeCamera(camera);
                Log.v(TAG, "error code: " + error);
            }

            private void closeCamera(@NonNull CameraDevice camera) {
                camera.close();
                mCameraDevice = null;
            }
        };
        mCameraId                   = null;
        mBackgroundHandler          = null;
        mBackgroundHandlerThread    = null;
        mPreviewSize                = null;
        mImageSize                  = null;
        mImageReader                = null;
        mOnImageAvailableListener   = new ImageReader.OnImageAvailableListener() {
            private Bitmap getBitmapFromImage(Image image) {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);

                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                Matrix matrix = new Matrix();
                matrix.postRotate(getTotalRotation(mOrientationEventListener.getOrientation()));

                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }

            @Override
            public void onImageAvailable(ImageReader reader) {
                mCamState = STATE_WAIT_LOCK_RELEASED;
                final Bitmap bitmap = getBitmapFromImage(reader.acquireLatestImage());
                BufferedImagesHelper.addImageToBuffer(bitmap, bitmap);
                if (BufferedImagesHelper.getBufferedImages().size() == MAX_IMAGES_PER_READER) {
                    mReachedImageLimit = true;
                    Utils.showToast(mContext, "Only "+ MAX_IMAGES_PER_READER + " images can be taken at once!!!", Toast.LENGTH_SHORT, Gravity.CENTER);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mRecentImagesOuterLayout.setVisibility(View.VISIBLE);
                        mRecentImageView.setImageBitmap(bitmap);
                        mRecentImagesCountTextView.setText(String.valueOf(BufferedImagesHelper.getBufferedImages().size()));
                        mProgressBar.setVisibility(View.GONE);
                        showPreview();
                    }
                });
            }
        };
        mCaptureSession             = null;
        mSensorRotation             = null;
        mTotalRotation              = null;
        mOrientationEventListener   = new RotateOrientationEventListener(mContext) {
            @Override
            public void onRotateChanged(int startDeg, int endDeg) {
                rotateView(mCloseImageView, startDeg, endDeg);
                rotateView(mPhotoImageView, startDeg, endDeg);
                rotateView(mRecentImagesOuterLayout, startDeg, endDeg);
            }
        };
        mCamState                   = STATE_IDLE;
        mFlashSupported             = false;
        mReachedImageLimit          = false;
        mManualFocusEngaged         = false;
        mSensorArraySize            = null;
        mMaxAFControlRegions        = -1;
    }

    private void closeActivity(String reason) {
        Log.v(TAG, reason);
        finish();
    }

    private void loadSettings() {
        mSharedPreferences = getPreferences(Context.MODE_PRIVATE);
        mShowGrid = mSharedPreferences.getBoolean(SHOW_GRID_KEY, false);
        mPlaySound= mSharedPreferences.getBoolean(PLAY_SOUND_KEY, false);
        mShowFlash= mSharedPreferences.getBoolean(SHOW_FLASH_KEY, false);
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(SHOW_GRID_KEY, mShowGrid);
        editor.putBoolean(PLAY_SOUND_KEY, mPlaySound);
        editor.putBoolean(SHOW_FLASH_KEY, mShowFlash);
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        enableOrientationListener();

        mReachedImageLimit = (BufferedImagesHelper.getBufferedImages().size() == MAX_IMAGES_PER_READER);
        BufferedImagesHelper.BufferedImage bufferedImage = BufferedImagesHelper.getLastImage();
        if (bufferedImage == null) {
            mRecentImagesOuterLayout.setVisibility(View.GONE);
        } else {
            mRecentImagesOuterLayout.setVisibility(View.VISIBLE);
            mRecentImageView.setImageBitmap(bufferedImage.getOriginalImage());
            mRecentImagesCountTextView.setText(String.valueOf(BufferedImagesHelper.getBufferedImages().size()));
        }

        if (mTextureView.isAvailable()) {
            initCam(mTextureView.getWidth(), mTextureView.getHeight());
            connectCam();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause");
        closeCam();
        disableOrientationListener();
        stopBackgroundThread();
        saveSettings();
        super.onPause();
    }

    /** Camera related */
    @SuppressWarnings("SuspiciousNameCombination")
    private void initCam(int width, int height) {
        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        CameraCharacteristics characteristics = null;
        if (mCameraManager == null) {
            closeActivity("Camera Manager not found");
            return;
        }

        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                Integer camFacing = characteristics.get(CameraCharacteristics.LENS_FACING);

                if (camFacing != null && camFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraId = cameraId;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mCameraId == null || characteristics == null) {
            closeActivity("Camera Id not found");
            return;
        }

        int rotatedWidth = width, rotatedHeight = height;
        mSensorRotation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        mTotalRotation = getTotalRotation(getDisplayRotation());
        if (mTotalRotation == 90 || mTotalRotation == 270) {
            rotatedWidth = height;
            rotatedHeight = width;
        }
        Log.v(TAG, "Dimens: " + width + " " + height + " " + rotatedWidth + " " + rotatedHeight);

        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map != null) {
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
            mImageSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.JPEG), rotatedWidth, rotatedHeight);
            mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, MAX_IMAGES_PER_READER);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
            Log.v(TAG, "mPreviewSize: " + mPreviewSize + " mImageSize: " + mImageSize);
        }

        Boolean flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        mFlashSupported = flashAvailable == null ? false : flashAvailable;

        mSensorArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        mMaxAFControlRegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
    }

    @SuppressLint("MissingPermission")
    private void connectCam() {
        if (mCameraManager == null) {
            return;
        }

        configurePreview();
        try {
            if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.M) || Utils.isPermissionGranted(cameraPermission)) {
                mCameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
                return;
            }

            ActivityCompat.requestPermissions(this, new String[]{cameraPermission}, Constants.CAMERA_PERMISSION_REQ_CODE);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCam() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }

    /** Preview related */
    private void showPreview() {
        mPhotoImageView.setEnabled(!mReachedImageLimit);
        if (mPreviewSize == null || mCameraDevice == null || mImageReader == null) {
            Utils.showToast(mContext, "Problem in showing preview", Toast.LENGTH_SHORT);
            return;
        }

        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(
                    Arrays.asList(previewSurface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mCaptureSession = session;
                            try {
                                setFlash(mCaptureRequestBuilder);
                                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                mCaptureSession.setRepeatingRequest(
                                        mCaptureRequestBuilder.build(),
                                        new CameraCaptureSession.CaptureCallback() {
                                            @Override
                                            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                                                super.onCaptureCompleted(session, request, result);
                                            }
                                        },
                                        mBackgroundHandler
                                );
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Utils.showToast(mContext, "Unable to load preview", Toast.LENGTH_SHORT);
                        }
                    },
                    null
            );
            mCamState = STATE_PREVIEW;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void configurePreview() {
        if (mTextureView == null || mPreviewSize == null) {
            return;
        }

        int displayRot = getDisplayRotation();
        int viewWidth = mTextureView.getWidth();
        int viewHeight = mTextureView.getHeight();
        int prevWidth = mPreviewSize.getWidth();
        int prevHeight = mPreviewSize.getHeight();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF prevRect = new RectF(0, 0, prevWidth, prevHeight);
        float viewCenterX = viewRect.centerX();
        float viewCenterY = viewRect.centerY();
        float prevCenterX = prevRect.centerX();
        float prevCenterY = prevRect.centerY();

        Matrix matrix = new Matrix();
        if (displayRot == Surface.ROTATION_90 || displayRot == Surface.ROTATION_270) {
            prevRect.offset(viewCenterX - prevCenterX, viewCenterY - prevCenterY);
            matrix.setRectToRect(viewRect, prevRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(((float) viewHeight) / prevHeight, ((float) viewWidth) / prevWidth);
            matrix.postScale(scale, scale, viewCenterX, viewCenterY);
            matrix.postRotate(90 * (displayRot - 2), viewCenterX, viewCenterY);
        } else if (displayRot == Surface.ROTATION_180) {
            matrix.postRotate(180, viewCenterX, viewCenterY);
        }
        mTextureView.setTransform(matrix);
    }

    /** Permissions related */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Constants.CAMERA_PERMISSION_REQ_CODE) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Utils.showToast(mContext, "Need Camera Permissions", Toast.LENGTH_SHORT, Gravity.CENTER);
                closeActivity("Need Camera Permissions");
            }
        }
    }

    /** OnClick related */
    public void onClick(@NonNull View view) {
        switch (view.getId()) {
            case R.id.camera_sound_image_button: {
                mPlaySound = !mPlaySound;
                mSoundImageButton.setImageResource(mPlaySound ? R.drawable.ic_sound : R.drawable.ic_sound_off);
                break;
            }
            case R.id.camera_flash_image_button: {
                mShowFlash = !mShowFlash;
                if (mCamState == STATE_PREVIEW) {
                    showPreview();
                }
                mFlashImageButton.setImageResource(mShowFlash ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);
                break;
            }
            case R.id.camera_grid_image_button: {
                mShowGrid = !mShowGrid;
                mCameraHolderLayout.setShowGrid(mShowGrid);
                mGridImageButton.setImageResource(mShowGrid ? R.drawable.ic_grid : R.drawable.ic_grid_off);
                break;
            }
            case R.id.camera_take_photo_image_view: {
                if (mPlaySound) {
                    new MediaActionSound().play(MediaActionSound.SHUTTER_CLICK);
                }
                startStillCaptureRequest();
                break;
            }
            case R.id.camera_close_image_view: {
                displayWarningForClose();
                break;
            }
            case R.id.camera_recent_images_inner_layout: {
                Intent intent = new Intent(mContext, CapturedImagesActivity.class);
                startActivityForResult(intent, Constants.LAUNCH_CAPTURED_IMAGES_ACTIVITY_FROM_CAMERA_ACTIVITY);
                break;
            }
        }
    }

    private void startStillCaptureRequest() {
        if (mReachedImageLimit) {
            Utils.showToast(mContext, "Only "+ MAX_IMAGES_PER_READER + " images can be taken at once!!!", Toast.LENGTH_SHORT, Gravity.CENTER);
            return;
        }
        if (mCameraDevice == null || mImageReader == null || mCaptureSession == null) {
            return;
        }

        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());

            setFlash(mCaptureRequestBuilder);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            mCaptureSession.capture(mCaptureRequestBuilder.build(), null, null);
            mCaptureSession.stopRepeating();
            mCamState = STATE_WAIT_LOCK;
            mProgressBar.setVisibility(View.VISIBLE);
            mPhotoImageView.setEnabled(false);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void displayWarningForClose() {
        if (BufferedImagesHelper.getBufferedImages().size() > 0) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("Warning");
            builder.setMessage("Captured images will be lost\nAre you sure to close?");
            builder.setPositiveButton("Discard", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    closeActivity("Voluntary close");
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
        } else {
            closeActivity("Voluntary close");
        }
    }

    private void setFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            if (mShowFlash) {
                requestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            } else {
                requestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void settingTapToFocus() {
        mTextureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int actionMasked = event.getActionMasked();
                if (actionMasked != MotionEvent.ACTION_DOWN) {
                    return false;
                }
                if (mManualFocusEngaged || mMaxAFControlRegions == null || mSensorArraySize == null || mCaptureRequestBuilder == null || mCaptureSession == null) {
                    return true;
                }

                int x, y;
                if (mTotalRotation == 90 || mTotalRotation == 270) {
                    x = (int)((event.getY() / (float)v.getHeight()) * (float)mSensorArraySize.width());
                    y = (int)((event.getX() / (float)v.getWidth())  * (float)mSensorArraySize.height());
                } else {
                    x = (int)((event.getX() / (float)v.getWidth())  * (float)mSensorArraySize.width());
                    y = (int)((event.getY() / (float)v.getHeight()) * (float)mSensorArraySize.height());
                }
                int halfTouchWidth = 150, halfTouchHeight = 150;

                MeteringRectangle focusAreaTouch = new MeteringRectangle(
                        Math.max(x - halfTouchWidth,  0),
                        Math.max(y - halfTouchHeight, 0),
                        halfTouchWidth  * 2,
                        halfTouchHeight * 2,
                        MeteringRectangle.METERING_WEIGHT_MAX - 1
                );

                CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                        mManualFocusEngaged = false;

                        if (request.getTag() == FOCUS_TAG) {
                            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, null);
                            showPreview();
                        }
                    }

                    @Override
                    public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                        super.onCaptureFailed(session, request, failure);

                        mManualFocusEngaged = false;
                        showPreview();
                    }
                };

                try {
                    mCaptureSession.stopRepeating();

                    mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                    mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                    mCaptureSession.capture(mCaptureRequestBuilder.build(), captureCallback, mBackgroundHandler);

                    if (mMaxAFControlRegions >= 1) {
                        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{focusAreaTouch});
                    }
                    mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
                    mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
                    mCaptureRequestBuilder.setTag(FOCUS_TAG);

                    mCaptureSession.capture(mCaptureRequestBuilder.build(), captureCallback, mBackgroundHandler);
                    mManualFocusEngaged = true;
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }

                return false;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.LAUNCH_CAPTURED_IMAGES_ACTIVITY_FROM_CAMERA_ACTIVITY && resultCode == Activity.RESULT_OK) {
            Intent thisIntent = getIntent();
            setResult(Activity.RESULT_OK, thisIntent);
            closeActivity("Done getting images");
        }
    }

    @Override
    public void onBackPressed() {
        displayWarningForClose();
    }

    /** Background thread related */
    private void startBackgroundThread() {
        if (mBackgroundHandlerThread != null) {
            stopBackgroundThread();
        }
        mBackgroundHandlerThread = new HandlerThread(THREAD_NAME);
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (mBackgroundHandlerThread == null) {
            return;
        }

        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /** dimensions, rotation and orientation related */
    public Integer getDisplayRotation() {
        return getWindowManager().getDefaultDisplay().getRotation();
    }

    private int getTotalRotation(int rotation) {
        if (mSensorRotation == null) {
            mSensorRotation = 0;
        }

        return (360 + mSensorRotation + (rotation * 90)) % 360;
    }

    private void enableOrientationListener() {
        if (mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable();
        }
    }

    private void disableOrientationListener() {
        mOrientationEventListener.disable();
    }

    private void rotateView(@NonNull View view, int startDeg, int endDeg) {
        view.setRotation(startDeg);
        view.animate().rotation(endDeg).start();
    }

    /** Static classes and functions */
    @SuppressLint("UnsupportedChromeOsCameraSystemFeature")
    public static boolean isCameraHardwareAvailable() {
        return App.getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    public static class CompareSizeByArea implements Comparator<Size> {
        @Override
        public int compare(Size o1, Size o2) {
            return Long.signum((long) o1.getWidth() * o1.getHeight() - (long) o2.getWidth() * o2.getHeight());
        }
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        List<Size> bigEnough = new ArrayList<>();
        for (Size choice : choices) {
            if (
                    (choice.getHeight() == choice.getWidth() * height / width) &&
                            (choice.getWidth() >= width) && (choice.getHeight() >= height)
            ) {
                bigEnough.add(choice);
            }
        }

        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        }
        return choices[0];
    }
}
