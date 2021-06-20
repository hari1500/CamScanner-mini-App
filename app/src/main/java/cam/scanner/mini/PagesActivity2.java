package cam.scanner.mini;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import cam.scanner.mini.customadapters.PagesAdapter2;
import cam.scanner.mini.localdatabase.Document;
import cam.scanner.mini.localdatabase.Page;
import cam.scanner.mini.utils.BufferedImagesHelper;
import cam.scanner.mini.utils.Constants;
import cam.scanner.mini.utils.DatabaseHelper;
import cam.scanner.mini.utils.FileHelper;
import cam.scanner.mini.utils.Utils;
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
import java.util.Objects;

public class PagesActivity2 extends AppCompatActivity {
    private Context mContext;
    private Document mDocument;
    private List<Page> mPages;
    private ViewPager mViewPager;
    private PagesAdapter2 mAdapter;
    private int requestedRetakeInd = -1;

    private static final String TAG                         = PagesActivity2.class.getCanonicalName();
    private static final String readExtStoragePermission    = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static final String writeExtStoragePermission   = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final int PDF_SAVE_CODE                  = 11;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pages2);

        mDocument = App.getCurrentDocument();
        if (mDocument == null) {
            closeActivity("Document is not available");
            return;
        }

        Intent thisIntent = getIntent();
        int currentPositon = thisIntent.getIntExtra(Constants.CURRENT_POSITION_FOR_PAGES_2_ACTIVITY_KEY, 0);

        mContext    = this;
        mPages      = DatabaseHelper.getAllPagesOfDocument(mDocument.getId());
        mAdapter    = new PagesAdapter2(mContext, mPages);
        mViewPager  = findViewById(R.id.pages2_view_pager);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(currentPositon);
    }

    private void closeActivity(String reason) {
        if (reason != null) {
            Log.v(TAG, reason);
        }

        finish();
    }

    public void onClick(@NonNull View view) {
        if (!mAdapter.isImageLoaded(mViewPager.getCurrentItem())) {
            return;
        }

        switch (view.getId()) {
            case R.id.pages2_back_button: {
                closeActivity("Pressed Back Button");
                break;
            }
            case R.id.pages2_retake_button: {
                onClickRetakeButton();
                break;
            }
            case R.id.pages2_edit_button: {
                onClickEditButton();
                break;
            }
            case R.id.pages2_save_button: {
                onClickSaveButton();
                break;
            }
            case R.id.pages2_share_button: {
                onClickShareButton();
                break;
            }
            case R.id.pages2_delete_button: {
                onClickDeleteButton();
                break;
            }
        }
    }

    private void onClickEditButton() {
        App.setCurrentPage(mPages.get(mViewPager.getCurrentItem()));

        Intent intent = new Intent(mContext, EditImageActivity1.class);
        startActivityForResult(intent, Constants.LAUNCH_EDIT_IMAGE_ACTIVITY1_FROM_PAGES_ACTIVITY2);
    }

    private void onClickShareButton() {
        FileOptionsDialogFragment dialog = new FileOptionsDialogFragment(
                getString(R.string.share_prefix),
                new FileOptionsDialogFragment.OnButtonClickListener() {
                    @Override
                    public void onButtonClick(int resource) {
                        switch (resource) {
                            case R.id.dialog_file_options_jpeg_button: {
                                onClickShareJpeg();
                                break;
                            }
                            case R.id.dialog_file_options_pdf_button: {
                                onClickSharePdf();
                                break;
                            }
                        }
                    }
                }, true, false
        );
        dialog.show(getSupportFragmentManager(), "FileOptionsDialog");
    }

    private void onClickShareJpeg() {
        Page page = mPages.get(mViewPager.getCurrentItem());
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("image/jpeg");
        ArrayList<Uri> files = new ArrayList<>();
        String file_name = String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, mDocument.getIdAsString(), page.getIdAsString());
        File file = new File(App.getContext().getFilesDir(), file_name);
        Uri uri = FileProvider.getUriForFile(App.getContext(), "cam.scanner.mini.fileprovider", file);
        files.add(uri);
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        mContext.startActivity(intent);
    }

    private void onClickSharePdf() {
        int currentPos = mViewPager.getCurrentItem();
        Page page = mPages.get(currentPos);
        com.itextpdf.text.Document document1 = new com.itextpdf.text.Document();
        String pdfFileName = App.getContext().getCacheDir().toString() + "/" + mDocument.getName() + " " + currentPos + ".pdf";
        try {
            PdfWriter.getInstance(document1, new FileOutputStream(pdfFileName));
            document1.open();
            String ImageFileName = App.getContext().getFilesDir().toString() + "/" +  String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, mDocument.getIdAsString(), page.getIdAsString());
            Image image = Image.getInstance(ImageFileName);
            float scale1 = ((document1.getPageSize().getHeight() - document1.topMargin()- document1.bottomMargin())/ image.getHeight());
            float scale2 = ((document1.getPageSize().getWidth() - document1.leftMargin() - document1.rightMargin()) / image.getWidth());
            float scaler = Math.min(scale1, scale2);
            image.scalePercent(scaler * 100);
            float x = (PageSize.A4.getWidth() - image.getScaledWidth()) / 2;
            float y = (PageSize.A4.getHeight() - image.getScaledHeight()) / 2;
            image.setAbsolutePosition(x, y);
            image.setAlignment(Image.ALIGN_CENTER);
            document1.newPage();
            document1.add(image);
            document1.close();
            File file = new File(pdfFileName);
            Uri uri = FileProvider.getUriForFile(App.getContext(), "cam.scanner.mini.fileprovider", file);
            if(uri != null){
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setType("application/pdf");
                mContext.startActivity(intent);
            }
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }
    }

    private void onClickSaveButton() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            saveDocument();
            return;
        }

        if (!Utils.isPermissionGranted(readExtStoragePermission)) {
            ActivityCompat.requestPermissions(this, new String[]{readExtStoragePermission}, Constants.READ_EXT_STORAGE_PERMISSION_REQ_CODE);
        } else if(!Utils.isPermissionGranted(writeExtStoragePermission)) {
            ActivityCompat.requestPermissions(this, new String[]{writeExtStoragePermission}, Constants.WRITE_EXT_STORAGE_PERMISSION_REQ_CODE);
        } else {
            saveDocument();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Constants.READ_EXT_STORAGE_PERMISSION_REQ_CODE) {
            if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if(ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.WRITE_EXT_STORAGE_PERMISSION_REQ_CODE);
                } else{
                    saveDocument();
                }
            } else {
                Utils.showToast(mContext, "Need Permissions for reading files", Toast.LENGTH_SHORT, Gravity.CENTER);
            }
        } else if (requestCode == Constants.WRITE_EXT_STORAGE_PERMISSION_REQ_CODE) {
            if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveDocument();
            } else {
                Utils.showToast(mContext, "Need Permissions for writing files", Toast.LENGTH_SHORT, Gravity.CENTER);
            }
        }
    }

    private void saveDocument() {
        FileOptionsDialogFragment dialog = new FileOptionsDialogFragment(
                getString(R.string.save_prefix),
                new FileOptionsDialogFragment.OnButtonClickListener() {
                    @Override
                    public void onButtonClick(int resource) {
                        switch (resource) {
                            case R.id.dialog_file_options_jpeg_button: {
                                saveImage();
                                break;
                            }
                            case R.id.dialog_file_options_pdf_button: {
                                savePdf();
                                break;
                            }
                        }
                    }
                }, true, false
        );
        dialog.show(getSupportFragmentManager(), "FileOptionsDialog");
    }

    private void saveImage() {
        Page page = mPages.get(mViewPager.getCurrentItem());
        String imageFileName = String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, page.getDocumentIdAsString(), page.getIdAsString());
        String imageSaveName = String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, mDocument.getName(), page.getIdAsString());

        File file = new File(App.getContext().getFilesDir(), imageFileName);
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
            OutputStream fos;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, imageSaveName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                Uri imageUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));
            } else {
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                File image = new File(imagesDir, imageSaveName);
                fos = new FileOutputStream(image);
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            Objects.requireNonNull(fos).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.showToast(mContext, "Saved in the downloads directory", Toast.LENGTH_SHORT);
    }

    private void savePdf() {
        Page page = mPages.get(mViewPager.getCurrentItem());
        Document document = DatabaseHelper.getDocumentById(page.getDocumentId());
        String pdfSaveName = String.format("%s_%s.pdf", (document != null) ? document.getName() : "", page.getIdAsString());
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                String pdfDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                File pdf = new File(pdfDir, pdfSaveName);
                FileOutputStream fos = new FileOutputStream(pdf);
                com.itextpdf.text.Document document1 = new com.itextpdf.text.Document();
                PdfWriter.getInstance(document1, fos);
                document1.open();
                String ImageFileName = App.getContext().getFilesDir().toString() + "/" + String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, page.getDocumentIdAsString(), page.getIdAsString());
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
                document1.close();
                Objects.requireNonNull(fos).close();
                Utils.showToast(mContext, "Saved in the downloads directory", Toast.LENGTH_SHORT);
            } else{
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/pdf");
                intent.putExtra(Intent.EXTRA_TITLE, pdfSaveName);
                startActivityForResult(intent, PDF_SAVE_CODE);
            }
        } catch (IOException | DocumentException e){
            e.printStackTrace();
        }
    }

    private void onClickDeleteButton() {
        final int currentPosition = mViewPager.getCurrentItem();

        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Warning");
        builder.setMessage("Are you sure, you wanna delete?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Deleting page and updating Document in cache
                DatabaseHelper.deletePage(mPages.get(currentPosition));
                mDocument = DatabaseHelper.getDocumentById(mDocument.getId());
                App.setCurrentDocument(mDocument);

                mPages.remove(currentPosition);
                if (mPages.size() == 0) {
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

    private void onClickRetakeButton() {
        Intent intent = new Intent(mContext, RetakeImageActivity.class);
        startActivityForResult(intent, Constants.LAUNCH_RETAKE_IMAGE_ACTIVITY_FROM_PAGES_ACTIVITY2);
        requestedRetakeInd = mViewPager.getCurrentItem();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.LAUNCH_EDIT_IMAGE_ACTIVITY1_FROM_PAGES_ACTIVITY2 && resultCode == Activity.RESULT_OK) {
            if (BufferedImagesHelper.getBufferedImages().size() > 0) {
                BufferedImagesHelper.BufferedImage bufferedImage = BufferedImagesHelper.getBufferedImages().get(0);

                Page page = App.getCurrentPage();
                DatabaseHelper.updateCorners(page, bufferedImage.getCorners());
                FileHelper.saveImage(
                        String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, page.getDocumentIdAsString(), page.getIdAsString()),
                        bufferedImage.getModifiedImage(), null
                );
                App.cacheBitmaps(page.getDocumentId(), page.getId(), null, bufferedImage.getModifiedImage());

                int currentPos = mViewPager.getCurrentItem();
                mAdapter.invalidateBitmapCache();
                mViewPager.setAdapter(null);
                mViewPager.setAdapter(mAdapter);
                mViewPager.setCurrentItem(currentPos);
            }
        }
        if (requestCode == Constants.LAUNCH_RETAKE_IMAGE_ACTIVITY_FROM_PAGES_ACTIVITY2 && resultCode == Activity.RESULT_OK) {
            Bitmap bitmap = BufferedImagesHelper.getRetakeResultantImage();
            if (bitmap != null && requestedRetakeInd >= 0 && requestedRetakeInd < mPages.size()) {
                Page page = mPages.get(requestedRetakeInd);
                App.cacheBitmaps(page.getDocumentId(), page.getId(), bitmap, bitmap);
                FileHelper.saveImage(
                        String.format(Constants.ORIGINAL_IMAGE_PATH_FORMAT, page.getDocumentIdAsString(), page.getIdAsString()),
                        bitmap, null
                );
                FileHelper.saveImage(
                        String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, page.getDocumentIdAsString(), page.getIdAsString()),
                        bitmap, null
                );
            }

            mAdapter.invalidateBitmapCache();
            mViewPager.setAdapter(null);
            mViewPager.setAdapter(mAdapter);
            mViewPager.setCurrentItem(requestedRetakeInd);
        }
        if(requestCode == PDF_SAVE_CODE && resultCode == RESULT_OK && data != null){
            Uri uri = data.getData();
            Page page = mPages.get(mViewPager.getCurrentItem());
            if (uri == null || page == null) {
                return;
            }
            try {
                ParcelFileDescriptor pfd = mContext.getContentResolver().openFileDescriptor(uri, "w");
                if (pfd == null) {
                    return;
                }
                FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());
                com.itextpdf.text.Document document1 = new com.itextpdf.text.Document();
                PdfWriter.getInstance(document1, fos);
                document1.open();
                String ImageFileName = App.getContext().getFilesDir().toString() + "/" + String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, page.getDocumentIdAsString(), page.getIdAsString());
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
                document1.close();
                Objects.requireNonNull(fos).close();
            } catch (IOException | DocumentException e) {
                e.printStackTrace();
            }
            Utils.showToast(mContext, "saved PDF", Toast.LENGTH_SHORT);
        }
    }
}
