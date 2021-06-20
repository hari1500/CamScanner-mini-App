package cam.scanner.mini;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import cam.scanner.mini.R;

import cam.scanner.mini.customadapters.PagesMultiSelectAdapter;
import cam.scanner.mini.localdatabase.Document;
import cam.scanner.mini.localdatabase.Page;
import cam.scanner.mini.utils.Constants;
import cam.scanner.mini.utils.DatabaseHelper;
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
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class PagesMultiSelectActivity extends AppCompatActivity {
    private Context mContext;
    private Document mDocument;
    private List<Page> mPages;
    private PagesMultiSelectAdapter mPagesMultiSelectAdapter;
    private RecyclerView mPagesRecyclerView;
    private boolean mIsSelectAllPressed;

    public static final String TAG                          = PagesMultiSelectActivity.class.getCanonicalName();
    private static final String readExtStoragePermission    = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static final String writeExtStoragePermission   = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final int PDF_SAVE_CODE                  = 11;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pages_multi_select);
        mDocument = App.getCurrentDocument();
        if (mDocument == null) {
            closeActivity("Doc not available");
            return;
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        int selectedIndex = getSelectedIndex();

        mContext                = this;
        mPages                  = DatabaseHelper.getAllPagesOfDocument(mDocument.getId());
        mPagesRecyclerView      = findViewById(R.id.pages_multi_select_recycler_view);
        mIsSelectAllPressed     = false;
        mPagesMultiSelectAdapter= new PagesMultiSelectAdapter(
                mPages,
                new PagesMultiSelectAdapter.OnItemPressedListener() {
                    @Override
                    public void onItemPressed(int positon) {
                        setTitle(String.format(Locale.getDefault(), "%d selected", mPagesMultiSelectAdapter.getSelectedPageIds().size()));
                    }
                },
                selectedIndex
        );

        setTitle(String.format(Locale.getDefault(), "%d selected", (selectedIndex >= 0 && selectedIndex < mPages.size()) ? 1 : 0));
        setupRecyclerView();
    }

    private int getSelectedIndex() {
        Intent thisIntent = getIntent();
        return thisIntent.getIntExtra(Constants.SELECTED_IND_FOR_PAGES_MULTI_SELECT_ACTIVITY_KEY, -1);
    }

    private void setupRecyclerView() {
        mPagesRecyclerView.setHasFixedSize(true);
        mPagesRecyclerView.setLayoutManager(new GridLayoutManager(mContext, 2));
        mPagesRecyclerView.setAdapter(mPagesMultiSelectAdapter);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END, 0
        ) {
            int dragFrom = -1;
            int dragTo = -1;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                if(dragFrom == -1) {
                    dragFrom =  fromPosition;
                }
                dragTo = toPosition;

                Objects.requireNonNull(recyclerView.getAdapter()).notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                if (dragFrom >= 0 && dragFrom < mPages.size() && dragTo >= 0 && dragTo < mPages.size()) {
                    mPages.add(dragTo, mPages.remove(dragFrom));
                    if (dragFrom > dragTo) {
                        mPagesMultiSelectAdapter.notifyItemRangeChanged(dragTo, dragFrom - dragTo + 1);
                    } else {
                        mPagesMultiSelectAdapter.notifyItemRangeChanged(dragFrom, dragTo - dragFrom + 1);
                    }

                    DatabaseHelper.shiftPage(mDocument, dragFrom, dragTo);
                    App.setCurrentDocument(mDocument);
                }
                dragFrom = -1;
                dragTo = -1;
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(mPagesRecyclerView);
    }

    private void closeActivity(String reason) {
        if (reason != null) {
            Log.v(TAG, reason);
        }
        finish();
    }

    public void onClick(@NonNull View view) {
        switch (view.getId()) {
            case R.id.pages_multi_select_delete: {
                onClickDelete();
                break;
            }
            case R.id.pages_multi_select_share: {
                onClickShare();
                break;
            }
            case R.id.pages_multi_select_save: {
                onClickSaveButton();
                break;
            }
        }
    }

    private void onClickDelete() {
        if (mPagesMultiSelectAdapter == null) {
            return;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Warning");
        builder.setMessage("Are you sure, you wanna delete?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Set<Long> selectedPageIds = mPagesMultiSelectAdapter.getSelectedPageIds();
                for (Long pageId : selectedPageIds) {
                    DatabaseHelper.deletePage(pageId, mDocument.getId());
                }

                mDocument = DatabaseHelper.getDocumentById(mDocument.getId());
                App.setCurrentDocument(mDocument);
                closeActivity("On Pressed Delete");
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

    private void onClickShare() {
        if (mPagesMultiSelectAdapter == null) {
            return;
        }

        Set<Long> selectedPageIds = mPagesMultiSelectAdapter.getSelectedPageIds();
        final List<Long> selectedPagesIdsOrderedList = new ArrayList<>();
        for (Page page : mPages) {
            if (selectedPageIds.contains(page.getId())) {
                selectedPagesIdsOrderedList.add(page.getId());
            }
        }

        FileOptionsDialogFragment dialog = new FileOptionsDialogFragment(
                getString(R.string.share_prefix),
                new FileOptionsDialogFragment.OnButtonClickListener() {
                    @Override
                    public void onButtonClick(int resource) {
                        switch (resource) {
                            case R.id.dialog_file_options_jpeg_button: {
                                Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                                intent.setType("image/jpeg");
                                ArrayList<Uri> files = new ArrayList<>();
                                for(Long id : selectedPagesIdsOrderedList) {
                                    String file_name = String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, mDocument.getIdAsString(), id);
                                    File file = new File(App.getContext().getFilesDir(), file_name);
                                    Uri uri = FileProvider.getUriForFile(App.getContext(), "cam.scanner.mini.fileprovider", file);
                                    files.add(uri);
                                }
                                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                mContext.startActivity(intent);
                                break;
                            }
                            case R.id.dialog_file_options_pdf_button: {
                                com.itextpdf.text.Document document1 = new com.itextpdf.text.Document();
                                String pdfFileName = App.getContext().getCacheDir().toString() + "/" + mDocument.getName() + ".pdf";
                                try {
                                    PdfWriter.getInstance(document1, new FileOutputStream(pdfFileName));
                                    document1.open();
                                    for(Long id : selectedPagesIdsOrderedList) {
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

                                break;
                            }
                            case R.id.dialog_file_options_long_image_button: {
                                ArrayList<Uri> files = new ArrayList<>();
                                try {
                                    Uri uri = Utils.createLongImage(selectedPagesIdsOrderedList, mDocument.getId());
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
                                break;
                            }
                        }
                    }
                }, true, true
        );
        dialog.show(getSupportFragmentManager(), "FileOptionsDialog");
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
        if (mPagesMultiSelectAdapter == null || mPagesMultiSelectAdapter.getSelectedPageIds().size() <= 0) {
            return;
        }
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
                            case R.id.dialog_file_options_long_image_button: {
                                saveLongImage();
                                break;
                            }
                        }
                    }
                },
                true, true
        );
        dialog.show(getSupportFragmentManager(), "FileOptionsDialog");
    }

    private void saveImage() {
        Set<Long> selectedPageIds = mPagesMultiSelectAdapter.getSelectedPageIds();
        final List<Long> selectedPagesIdsOrderedList = new ArrayList<>();
        for (Page page : mPages) {
            if (selectedPageIds.contains(page.getId())) {
                selectedPagesIdsOrderedList.add(page.getId());
            }
        }

        for (Long pageId : selectedPagesIdsOrderedList) {
            String imageFileName = String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, mDocument.getIdAsString(), pageId);
            String imageSaveName = String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, mDocument.getName(), pageId);
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
        }
        Utils.showToast(mContext, "Saved in the downloads directory", Toast.LENGTH_SHORT);
    }

    private void savePdf() {
        Set<Long> selectedPageIds = mPagesMultiSelectAdapter.getSelectedPageIds();
        final List<Long> selectedPagesIdsOrderedList = new ArrayList<>();
        for (Page page : mPages) {
            if (selectedPageIds.contains(page.getId())) {
                selectedPagesIdsOrderedList.add(page.getId());
            }
        }

        String pdfSaveName = mDocument.getName() + ".pdf";
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                String pdfDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                File pdf = new File(pdfDir, pdfSaveName);
                FileOutputStream fos = new FileOutputStream(pdf);
                com.itextpdf.text.Document document1 = new com.itextpdf.text.Document();
                PdfWriter.getInstance(document1, fos);
                document1.open();
                for (Long id : selectedPagesIdsOrderedList) {
                    String ImageFileName = App.getContext().getFilesDir().toString() + "/" + String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, mDocument.getIdAsString(), id);
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

    private void saveLongImage() {
        Set<Long> selectedPageIds = mPagesMultiSelectAdapter.getSelectedPageIds();
        final List<Long> selectedPagesIdsOrderedList = new ArrayList<>();
        for (Page page : mPages) {
            if (selectedPageIds.contains(page.getId())) {
                selectedPagesIdsOrderedList.add(page.getId());
            }
        }

        String imageSaveName = mDocument.getName() + "_longImage.jpeg";
        try {
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
            Utils.createLongImage(selectedPagesIdsOrderedList, mDocument.getId(), fos);
            Objects.requireNonNull(fos).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.showToast(mContext, "Saved LONG image in Downloads directory", Toast.LENGTH_SHORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PDF_SAVE_CODE && resultCode == RESULT_OK && data != null){
            Uri uri = data.getData();
            Set<Long> selectedPageIds = mPagesMultiSelectAdapter.getSelectedPageIds();
            final List<Long> selectedPagesIdsOrderedList = new ArrayList<>();
            for (Page page : mPages) {
                if (selectedPageIds.contains(page.getId())) {
                    selectedPagesIdsOrderedList.add(page.getId());
                }
            }

            if (uri == null || selectedPagesIdsOrderedList.size() <= 0) {
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
                for (Long id : selectedPagesIdsOrderedList) {
                    String ImageFileName = App.getContext().getFilesDir().toString() + "/" + String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, mDocument.getIdAsString(), id);
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
                Objects.requireNonNull(fos).close();
            } catch (IOException | DocumentException e) {
                e.printStackTrace();
            }
            Utils.showToast(mContext, "saved PDF", Toast.LENGTH_SHORT);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.multi_select_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.multi_select_menu_all && mPagesMultiSelectAdapter != null) {
            mIsSelectAllPressed = !mIsSelectAllPressed;
            if (mIsSelectAllPressed) {
                item.setTitle(R.string.deselect_all);
                mPagesMultiSelectAdapter.selectAll();
            } else {
                item.setTitle(R.string.select_all);
                mPagesMultiSelectAdapter.deselectAll();
            }
            setTitle(String.format(Locale.getDefault(), "%d selected", mPagesMultiSelectAdapter.getSelectedPageIds().size()));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        closeActivity("On Press Back");
        return true;
    }
}
