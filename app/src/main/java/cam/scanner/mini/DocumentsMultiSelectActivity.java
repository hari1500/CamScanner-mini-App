package cam.scanner.mini;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import cam.scanner.mini.R;

import cam.scanner.mini.customadapters.DocumentsMultiSelectAdapter;
import cam.scanner.mini.localdatabase.Document;
import cam.scanner.mini.utils.Constants;
import cam.scanner.mini.utils.DatabaseHelper;
import cam.scanner.mini.utils.SortComparator;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class DocumentsMultiSelectActivity extends AppCompatActivity {
    private Context mContext;
    private List<Document> mDocuments;
    private DocumentsMultiSelectAdapter mDocumentsMultiSelectAdapter;
    private RecyclerView mDocumentsRecyclerView;
    private Button mRenameButton;
    private boolean mIsSelectAllPressed;

    public static final String TAG                          = DocumentsMultiSelectActivity.class.getCanonicalName();
    private static final String readExtStoragePermission    = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static final String writeExtStoragePermission   = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final int PDF_SAVE_CODE                  = 11;
    private static long docID = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents_multi_select);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        int selectedIndex = getSelectedIndex();

        mContext                    = this;
        mDocuments                  = DatabaseHelper.getAllDocuments();
        mDocumentsRecyclerView      = findViewById(R.id.documents_multi_select_recycler_view);
        mRenameButton               = findViewById(R.id.documents_multi_select_rename);
        mIsSelectAllPressed         = false;
        sortDocuments();
        mDocumentsMultiSelectAdapter= new DocumentsMultiSelectAdapter(
                mDocuments,
                new DocumentsMultiSelectAdapter.OnItemPressedListener() {
                    @Override
                    public void onItemPressed(int positon) {
                        int numSelected = mDocumentsMultiSelectAdapter.getSelectedDocumentIds().size();
                        setTitle(String.format(Locale.getDefault(), "%d selected", numSelected));
                        if (numSelected == 1) {
                            mRenameButton.setVisibility(View.VISIBLE);
                        } else {
                            mRenameButton.setVisibility(View.GONE);
                        }
                    }
                },
                selectedIndex
        );

        if (selectedIndex >= 0 && selectedIndex < mDocuments.size()) {
            mRenameButton.setVisibility(View.VISIBLE);
            setTitle("1 selected");
        } else {
            mRenameButton.setVisibility(View.GONE);
            setTitle("0 selected");
        }
        setupRecyclerView();
    }

    private void sortDocuments() {
        switch (App.getDocumentsSortMethod()) {
            case SortComparator.METHOD_INCREASING_CREATED_TIMESTAMP: {
                Collections.sort(mDocuments, new SortComparator.SortByCreatedAscending());
                break;
            }
            case SortComparator.METHOD_DECREASING_CREATED_TIMESTAMP: {
                Collections.sort(mDocuments, new SortComparator.SortByCreatedDescending());
                break;
            }
            case SortComparator.METHOD_INCREASING_MODIFIED_TIMESTAMP: {
                Collections.sort(mDocuments, new SortComparator.SortByModifiedAscending());
                break;
            }
            case SortComparator.METHOD_DOC_NAME_A_2_Z: {
                Collections.sort(mDocuments, new SortComparator.SortByDocNameAscending());
                break;
            }
            case SortComparator.METHOD_DOC_NAME_Z_2_A: {
                Collections.sort(mDocuments, new SortComparator.SortByDocNameDescending());
                break;
            }
            case SortComparator.METHOD_DECREASING_MODIFIED_TIMESTAMP:
            default:{
                Collections.sort(mDocuments, new SortComparator.SortByModifiedDescending());
                break;
            }
        }
    }

    private int getSelectedIndex() {
        Intent thisIntent = getIntent();
        return thisIntent.getIntExtra(Constants.SELECTED_IND_FOR_DOCUMENTS_MULTI_SELECT_ACTIVITY_KEY, -1);
    }

    private void setupRecyclerView() {
        mDocumentsRecyclerView.setHasFixedSize(true);
        mDocumentsRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mDocumentsRecyclerView.setAdapter(mDocumentsMultiSelectAdapter);
    }

    private void closeActivity(String reason) {
        if (reason != null) {
            Log.v(TAG, reason);
        }
        finish();
    }

    public void onClick(@NonNull View view) {
        if (mDocumentsMultiSelectAdapter.getSelectedDocumentIds().size() == 0) {
            return;
        }
        switch (view.getId()) {
            case R.id.documents_multi_select_delete: {
                onClickDeleteButton();
                break;
            }
            case R.id.documents_multi_select_rename: {
                onClickRenameButton();
                break;
            }
            case R.id.documents_multi_select_share: {
                onClickShareButton();
                break;
            }
            case R.id.documents_multi_select_save: {
                onClickSaveButton();
                break;
            }
        }
    }

    private void onClickRenameButton() {
        if (mDocumentsMultiSelectAdapter == null) {
            return;
        }
        Set<Long> selectedDocumentIds = mDocumentsMultiSelectAdapter.getSelectedDocumentIds();
        if (selectedDocumentIds.size() != 1) {
            closeActivity("Pressed Rename Button");
            return;
        }
        long documentId = -1;
        for (Long docId : selectedDocumentIds) {
            documentId = docId;
        }
        if (documentId == -1) {
            closeActivity("Pressed Rename Button");
            return;
        }

        final Document document = DatabaseHelper.getDocumentById(documentId);
        if (document == null) {
            closeActivity("Pressed Rename Button");
            return;
        }

        final Dialog dialog = new Dialog(mContext, R.style.WideDialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_rename);
        EditText renameEditText = dialog.findViewById(R.id.dialog_rename_edit_text);
        renameEditText.setText(document.getName());

        Button renameOk = dialog.findViewById(R.id.dialog_rename_ok_button);
        renameOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText rename_editText = dialog.findViewById(R.id.dialog_rename_edit_text);
                String text = rename_editText.getText().toString();
                if(text.length() == 0){
                    rename_editText.setError("Name can't be empty");
                }
                else if(DatabaseHelper.getDocumentByName(text) != null){
                    rename_editText.setError("Documnet with same name already exists");
                }
                else {
                    DatabaseHelper.updateDocumentName(document.getId(), text);
                    closeActivity("Pressed Rename Button");
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

    private void onClickDeleteButton() {
        if (mDocumentsMultiSelectAdapter == null) {
            return;
        }
        final Set<Long> selectedDocumentIds = mDocumentsMultiSelectAdapter.getSelectedDocumentIds();
        if (selectedDocumentIds.size() <= 0) {
            return;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Warning");
        builder.setMessage("Are you sure, you wanna delete?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                for (Long docId : selectedDocumentIds) {
                    DatabaseHelper.deleteDocument(docId);
                    closeActivity("Pressed Delete Button");
                }
                closeActivity("Pressed Delete Button");
                dialog.dismiss();
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

    private void onClickShareButton() {
        if (mDocumentsMultiSelectAdapter == null) {
            return;
        }

        final Set<Long> selectedDocumentIds = mDocumentsMultiSelectAdapter.getSelectedDocumentIds();
        final List<Long> selectedDocumentsIdsOrderedList = new ArrayList<>();
        for (Document document : mDocuments) {
            if (selectedDocumentIds.contains(document.getId())) {
                selectedDocumentsIdsOrderedList.add(document.getId());
            }
        }

        FileOptionsDialogFragment dialog = new FileOptionsDialogFragment(
                getString(R.string.share_prefix),
                new FileOptionsDialogFragment.OnButtonClickListener() {
                    @Override
                    public void onButtonClick(int resource) {
                        switch (resource) {
                            case R.id.dialog_file_options_jpeg_button: {
                                ArrayList<Uri> files = new ArrayList<>();
                                for (Long docId : selectedDocumentsIdsOrderedList) {
                                    Document document = DatabaseHelper.getDocumentById(docId);
                                    if (document != null) {
                                        for (Long pageId: document.getPageIds()) {
                                            String fileName = String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, docId, pageId);
                                            File file = new File(App.getContext().getFilesDir(), fileName);
                                            Uri uri = FileProvider.getUriForFile(App.getContext(), "cam.scanner.mini.fileprovider", file);
                                            files.add(uri);
                                        }
                                    }
                                }
                                Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                                intent.setType("image/jpeg");
                                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(intent);
                                break;
                            }
                            case R.id.dialog_file_options_pdf_button: {
                                ArrayList<Uri> files = new ArrayList<>();
                                for (Long docId : selectedDocumentsIdsOrderedList) {
                                    Document document = DatabaseHelper.getDocumentById(docId);
                                    if (document != null) {
                                        try {
                                            Uri uri = Utils.createPdf(document);
                                            if (uri != null) {
                                                files.add(uri);
                                            }
                                        } catch (DocumentException | IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                                intent.setType("application/pdf");
                                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(intent);
                                break;
                            }
                            case R.id.dialog_file_options_long_image_button: {
                                ArrayList<Uri> files = new ArrayList<>();
                                for (Long docId : selectedDocumentsIdsOrderedList) {
                                    Document document = DatabaseHelper.getDocumentById(docId);
                                    if (document != null) {
                                        try {
                                            Uri uri = Utils.createLongImage(document);
                                            if (uri != null) {
                                                files.add(uri);
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
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
        if (mDocumentsMultiSelectAdapter == null) {
            return;
        }
        final Set<Long> selectedDocumentIds = mDocumentsMultiSelectAdapter.getSelectedDocumentIds();
        if (selectedDocumentIds.size() <= 0) {
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
                (selectedDocumentIds.size() == 1), true
        );
        dialog.show(getSupportFragmentManager(), "FileOptionsDialog");
    }

    private void saveImage() {
        final Set<Long> selectedDocumentIds = mDocumentsMultiSelectAdapter.getSelectedDocumentIds();
        final List<Long> selectedDocumentsIdsOrderedList = new ArrayList<>();
        for (Document document : mDocuments) {
            if (selectedDocumentIds.contains(document.getId())) {
                selectedDocumentsIdsOrderedList.add(document.getId());
            }
        }

        for(Long docId : selectedDocumentsIdsOrderedList){
            Document document = DatabaseHelper.getDocumentById(docId);
            if (document != null) {
                for (Long pageId : document.getPageIds()) {
                    String imageFileName = String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, document.getIdAsString(), pageId);
                    String imageSaveName = String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, document.getName(), pageId);
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
            }
        }
        Utils.showToast(mContext, "Saved in the downloads directory", Toast.LENGTH_SHORT);
    }

    private void savePdf() {
        final Set<Long> selectedDocumentIds = mDocumentsMultiSelectAdapter.getSelectedDocumentIds();
        final List<Long> selectedDocumentsIdsOrderedList = new ArrayList<>();
        for (Document document : mDocuments) {
            if (selectedDocumentIds.contains(document.getId())) {
                selectedDocumentsIdsOrderedList.add(document.getId());
            }
        }

        for(Long docId : selectedDocumentsIdsOrderedList){
            Document document = DatabaseHelper.getDocumentById(docId);
            if (document != null) {
                String pdfSaveName = document.getName() + ".pdf";
                try {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        String pdfDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                        File pdf = new File(pdfDir, pdfSaveName);
                        FileOutputStream fos = new FileOutputStream(pdf);
                        com.itextpdf.text.Document document1 = new com.itextpdf.text.Document();
                        PdfWriter.getInstance(document1, fos);
                        document1.open();
                        for (Long id : document.getPageIds()) {
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
                        Objects.requireNonNull(fos).close();
                        Utils.showToast(mContext, "Saved PDF in downloads directory", Toast.LENGTH_SHORT);
                    } else{
                        docID = docId;
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
        }
    }

    private void saveLongImage() {
        final Set<Long> selectedDocumentIds = mDocumentsMultiSelectAdapter.getSelectedDocumentIds();

        for(Long docId : selectedDocumentIds) {
            Document document = DatabaseHelper.getDocumentById(docId);
            if (document != null) {
                String imageSaveName = document.getName() + "_longImage.jpeg";
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
                    Utils.createLongImage(document, fos);
                    Objects.requireNonNull(fos).close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        Utils.showToast(mContext, "Saved LONG images in Downloads directory", Toast.LENGTH_SHORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PDF_SAVE_CODE && resultCode == RESULT_OK && data != null){
            Uri uri = data.getData();
            Document document = DatabaseHelper.getDocumentById(docID);
            if (uri == null || document == null) {
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
                for (Long id : document.getPageIds()) {
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
        if (item.getItemId() == R.id.multi_select_menu_all && mDocumentsMultiSelectAdapter != null) {
            mIsSelectAllPressed = !mIsSelectAllPressed;
            if (mIsSelectAllPressed) {
                item.setTitle(R.string.deselect_all);
                mDocumentsMultiSelectAdapter.selectAll();
            } else {
                item.setTitle(R.string.select_all);
                mDocumentsMultiSelectAdapter.deselectAll();
            }
            int numSelected = mDocumentsMultiSelectAdapter.getSelectedDocumentIds().size();
            setTitle(String.format(Locale.getDefault(), "%d selected", numSelected));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        closeActivity("On Press Back");
        return true;
    }
}