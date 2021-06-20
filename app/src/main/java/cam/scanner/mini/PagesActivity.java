package cam.scanner.mini;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import cam.scanner.mini.R;

import cam.scanner.mini.customadapters.PagesAdapter;
import cam.scanner.mini.utils.BufferedImagesHelper;
import cam.scanner.mini.utils.Constants;
import cam.scanner.mini.utils.DatabaseHelper;
import cam.scanner.mini.localdatabase.Document;
import cam.scanner.mini.localdatabase.Page;
import cam.scanner.mini.utils.Utils;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PagesActivity extends AppCompatActivity {
    private Context mContext;
    private Document mDocument;
    private ArrayList<Page> mPages;
    private PagesAdapter mPagesAdapter;
    private RecyclerView mPagesRecyclerView;

    private static final String TAG = PagesActivity.class.getCanonicalName();
    private static final String readExtStoragePermission = Manifest.permission.READ_EXTERNAL_STORAGE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pages);
        mDocument = App.getCurrentDocument();
        if (mDocument == null) {
            closeActivity("Doc not found");
            return;
        }
        setTitle(mDocument.getName());

        mContext            = this;
        mPages              = new ArrayList<>();
        mPagesAdapter       = new PagesAdapter(mContext, mPages);
        mPagesRecyclerView  = findViewById(R.id.pages_recycler_view);

        setupRecyclerView();
        autoUpdatePages();
    }

    private void setupRecyclerView() {
        mPagesRecyclerView.setHasFixedSize(true);
        mPagesRecyclerView.setLayoutManager(new GridLayoutManager(mContext, 2));
        mPagesRecyclerView.setAdapter(mPagesAdapter);
    }

    private void closeActivity(String reason) {
        if (reason != null) {
            Log.v(TAG, reason);
        }

        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPagesAdapter != null) {
            mPages.clear();
            mPages.addAll(DatabaseHelper.getAllPagesOfDocument(mDocument.getId()));
            mPagesAdapter.invalidateBitmapCache();
            mPagesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.pages_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pages_menu_rename: {
                onClickRename();
                return true;
            }
            case R.id.pages_menu_import_images: {
                openGalleryWithPermissions();
                return true;
            }
            case R.id.pages_menu_manual_sorting: {
                startActivity(new Intent(mContext, PagesSortingActivity.class));
                return true;
            }
            case R.id.pages_menu_select: {
                startActivity(new Intent(mContext, PagesMultiSelectActivity.class));
                return true;
            }
            case R.id.pages_menu_share: {
                onClickShare();
                return true;
            }
            case R.id.pages_menu_pdf: {
                startActivity(new Intent(mContext, PdfPreviewActivity.class));
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void addPages(@NonNull View view) {
        if (view.getId() == R.id.add_pages_using_camera_button) {
            Intent intent = new Intent(mContext, CameraActivity.class);
            startActivityForResult(intent, Constants.LAUNCH_CAMERA_ACTIVITY_FROM_PAGES_ACTIVITY);
        }
    }

    private void onClickRename() {
        final Dialog dialog = new Dialog(mContext, R.style.WideDialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_rename);
        EditText renameEditText = dialog.findViewById(R.id.dialog_rename_edit_text);
        renameEditText.setText(mDocument.getName());

        Button renameOk = dialog.findViewById(R.id.dialog_rename_ok_button);
        renameOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText rename_editText = dialog.findViewById(R.id.dialog_rename_edit_text);
                String text = rename_editText.getText().toString();
                if(text.length() == 0) {
                    rename_editText.setError("Name can't be empty");
                } else if(DatabaseHelper.getDocumentByName(text) != null){
                    rename_editText.setError("Document with same name already exists");
                } else {
                    DatabaseHelper.updateDocumentName(mDocument.getId(), text);
                    setTitle(text);
                    mDocument = DatabaseHelper.getDocumentById(mDocument.getId());
                    App.setCurrentDocument(mDocument);
                    dialog.cancel();
                }
            }
        });
        Button renameCancel = dialog.findViewById(R.id.dialog_rename_cancel_button);
        renameCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });
        dialog.show();
    }

    private void onClickShare() {
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
                            case R.id.dialog_file_options_long_image_button: {
                                onClickShareLongImage();
                                break;
                            }
                        }
                    }
                }, true, true
        );
        dialog.show(getSupportFragmentManager(), "FileOptionsDialog");
    }

    private void onClickShareJpeg() {
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("image/jpeg");
        ArrayList<Uri> files = new ArrayList<>();
        for(Long id : mDocument.getPageIds()){
            String file_name = String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, mDocument.getIdAsString(), id);
            File file = new File(App.getContext().getFilesDir(), file_name);
            Uri uri = FileProvider.getUriForFile(App.getContext(), "cam.scanner.mini.fileprovider", file);
            files.add(uri);
        }
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        mContext.startActivity(intent);
    }

    private void onClickSharePdf() {
        com.itextpdf.text.Document document1 = new com.itextpdf.text.Document();
        String pdfFileName = App.getContext().getCacheDir().toString() + "/" + mDocument.getName() + ".pdf";
        try {
            PdfWriter.getInstance(document1, new FileOutputStream(pdfFileName));
            document1.open();
            for(Long id : mDocument.getPageIds()) {
                String ImageFileName = App.getContext().getFilesDir().toString() + "/" +  String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, mDocument.getIdAsString(), id);
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
            }
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

    private void onClickShareLongImage() {
        ArrayList<Uri> files = new ArrayList<>();
        try {
            Uri uri = Utils.createLongImage(mDocument);
            if (uri != null) {
                files.add(uri);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("image/jpeg");
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private void openGalleryWithPermissions() {
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.M) || Utils.isPermissionGranted(readExtStoragePermission)) {
            openGallery();
            return;
        }

        ActivityCompat.requestPermissions(this, new String[]{readExtStoragePermission}, Constants.READ_EXT_STORAGE_PERMISSION_REQ_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Constants.READ_EXT_STORAGE_PERMISSION_REQ_CODE) {
            if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Utils.showToast(mContext, "Need Permissions for loading images", Toast.LENGTH_SHORT, Gravity.CENTER);
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
        startActivityForResult(intent, Constants.LAUNCH_GALLERY_FROM_PAGES_ACTIVITY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (
                (requestCode == Constants.LAUNCH_CAMERA_ACTIVITY_FROM_PAGES_ACTIVITY ||
                        requestCode == Constants.LAUNCH_CAPTURED_IMAGES_ACTIVITY_FROM_PAGES_ACTIVITY)
                        && resultCode == Activity.RESULT_OK
        ) {
            ArrayList<BufferedImagesHelper.BufferedImage> bufferedImages = BufferedImagesHelper.getBufferedImages();
            for (BufferedImagesHelper.BufferedImage bufferedImage : bufferedImages) {
                DatabaseHelper.createPage(
                        mDocument, bufferedImage.getOriginalImage(), bufferedImage.getModifiedImage(), bufferedImage.getCorners()
                );
            }
        }
        if (requestCode == Constants.LAUNCH_GALLERY_FROM_PAGES_ACTIVITY && resultCode == Activity.RESULT_OK && data != null) {
            final Dialog dialog = new Dialog(mContext);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_processing);
            dialog.setCancelable(false);
            dialog.show();
            new App.ProcessGalleryData(data, getContentResolver(), new App.ProcessGalleryData.OnProcessGalleryDoneListener() {
                @Override
                public void onProcessGalleryDone(List<Bitmap> bitmaps) {
                    if (bitmaps.size() > 0) {
                        BufferedImagesHelper.clearBufferedImages();
                        for (Bitmap bitmap : bitmaps) {
                            BufferedImagesHelper.addImageToBuffer(bitmap, bitmap);
                        }
                        dialog.cancel();
                        Intent intent = new Intent(mContext, CapturedImagesActivity.class);
                        startActivityForResult(intent, Constants.LAUNCH_CAPTURED_IMAGES_ACTIVITY_FROM_PAGES_ACTIVITY);
                    }
                }
            }).execute();
        }
    }

    private void autoUpdatePages() {
        // Observer for getting live data
        Observer<List<Page>> pagesObserver = new Observer<List<Page>>() {
            @Override
            public void onChanged(List<Page> pages) {
                if (pages == null || pages.size() <= 0) {
                    DatabaseHelper.deleteDocument(mDocument.getId());
                    App.setCurrentDocument(null);
                    App.setCurrentPage(null);

                    Utils.showToast(mContext, "Entire Document is deleted", Toast.LENGTH_SHORT, Gravity.CENTER);
                    closeActivity("Empty Doc");
                    return;
                }
                HashMap<Long, Page> pageHashMap = new HashMap<>();
                for (Page page : pages) {
                    pageHashMap.put(page.getId(), page);
                }

                mPages.clear();
                for (long pageId : mDocument.getPageIds()) {
                    if (pageHashMap.containsKey(pageId)) {
                        mPages.add(pageHashMap.get(pageId));
                    }
                }

                mPagesAdapter.notifyDataSetChanged();
            }
        };
        DatabaseHelper.getAllPagesOfDocumentLive(mDocument.getId(), this, pagesObserver);
    }
}
