package cam.scanner.mini;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import cam.scanner.mini.components.AdaptableSearchView;
import cam.scanner.mini.customadapters.DocumentsAdapter;
import cam.scanner.mini.utils.BufferedImagesHelper;
import cam.scanner.mini.utils.Constants;
import cam.scanner.mini.utils.DatabaseHelper;
import cam.scanner.mini.localdatabase.Document;
import cam.scanner.mini.utils.SortComparator;
import cam.scanner.mini.utils.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class DocumentsActivity extends AppCompatActivity {
    private Context mContext;
    private List<Document> mDocuments;
    private List<Document> mDocumentsShow;
    private DocumentsAdapter mDocumentsAdapter;
    private SharedPreferences mSharedPreferences;
    private int sortMethod;

    private static final String TAG = DocumentsActivity.class.getCanonicalName();
    private static final String readExtStoragePermission = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static final String SORT_METHOD_KEY = TAG + "SortMethod";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents);

        mContext            = this;
        mDocuments          = new ArrayList<>();
        mDocumentsShow      = new ArrayList<>();
        mDocumentsAdapter   = new DocumentsAdapter(mContext, mDocumentsShow);

        RecyclerView documentsRecyclerView = findViewById(R.id.documents_recycler_view);
        documentsRecyclerView.setHasFixedSize(true);
        documentsRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        documentsRecyclerView.setAdapter(mDocumentsAdapter);

        loadSortMethod();
        autoUpdateDocuments();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDocumentsAdapter != null) {
            mDocumentsAdapter.invalidateBitmapCache();
            mDocumentsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSortMethod();
    }

    private void loadSortMethod() {
        mSharedPreferences = getPreferences(Context.MODE_PRIVATE);
        sortMethod = mSharedPreferences.getInt(SORT_METHOD_KEY, SortComparator.METHOD_DECREASING_MODIFIED_TIMESTAMP);
        App.setDocumentsSortMethod(sortMethod);
    }

    private void saveSortMethod() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(SORT_METHOD_KEY, sortMethod);
        editor.apply();
    }

    private void autoUpdateDocuments() {
        // Observer for getting live data
        Observer<List<Document>> documentsObserver = new Observer<List<Document>>() {
            @Override
            public void onChanged(List<Document> documents) {
                mDocuments.clear();
                mDocuments.addAll(documents);
                updateDocumentsShow();
            }
        };
        DatabaseHelper.getAllDocumentsLive(this, documentsObserver);
    }

    private void updateDocumentsShow() {
        mDocumentsShow.clear();
        for(Document d: mDocuments) {
            mDocumentsShow.add(d);
        }
        sortDocumentShow();
    }

    private void sortDocumentShow() {
        switch (sortMethod) {
            case SortComparator.METHOD_INCREASING_MODIFIED_TIMESTAMP: {
                Collections.sort(mDocumentsShow, new SortComparator.SortByModifiedAscending());
                break;
            }
            case SortComparator.METHOD_DECREASING_CREATED_TIMESTAMP: {
                Collections.sort(mDocumentsShow, new SortComparator.SortByCreatedDescending());
                break;
            }
            case SortComparator.METHOD_INCREASING_CREATED_TIMESTAMP: {
                Collections.sort(mDocumentsShow, new SortComparator.SortByCreatedAscending());
                break;
            }
            case SortComparator.METHOD_DOC_NAME_A_2_Z: {
                Collections.sort(mDocumentsShow, new SortComparator.SortByDocNameAscending());
                break;
            }
            case SortComparator.METHOD_DOC_NAME_Z_2_A: {
                Collections.sort(mDocumentsShow, new SortComparator.SortByDocNameDescending());
                break;
            }
            case SortComparator.METHOD_DECREASING_MODIFIED_TIMESTAMP:
            default:  {
                Collections.sort(mDocumentsShow, new SortComparator.SortByModifiedDescending());
                break;
            }
        }
        if (mDocumentsAdapter != null) {
            mDocumentsAdapter.notifyDataSetChanged();
        }
    }

    public void createNewDoc(@NonNull View view) {
        if (view.getId() == R.id.new_document_using_camera_button) {
            Intent intent = new Intent(mContext, CameraActivity.class);
            startActivityForResult(intent, Constants.LAUNCH_CAMERA_ACTIVITY_FROM_DOCUMENTS_ACTIVITY);
        }
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
                (requestCode == Constants.LAUNCH_CAMERA_ACTIVITY_FROM_DOCUMENTS_ACTIVITY ||
                        requestCode == Constants.LAUNCH_CAPTURED_IMAGES_ACTIVITY_FROM_DOCUMENTS_ACTIVITY)
                && resultCode == Activity.RESULT_OK
        ) {
            Document document = DatabaseHelper.createDocument(getNewDocumentName());
            ArrayList<BufferedImagesHelper.BufferedImage> bufferedImages = BufferedImagesHelper.getBufferedImages();
            for (BufferedImagesHelper.BufferedImage bufferedImage : bufferedImages) {
                DatabaseHelper.createPage(
                        document, bufferedImage.getOriginalImage(), bufferedImage.getModifiedImage(), bufferedImage.getCorners()
                );
            }
            App.setCurrentDocument(document);
            startActivity(new Intent(mContext, PagesActivity.class));
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
                        startActivityForResult(intent, Constants.LAUNCH_CAPTURED_IMAGES_ACTIVITY_FROM_DOCUMENTS_ACTIVITY);
                    }
                }
            }).execute();
        }
    }

    private String getNewDocumentName() {
        String documentName = "New Doc ";

        DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_FILE_NAME, Locale.getDefault());
        String createdAt = dateFormat.format(Calendar.getInstance().getTime());

        documentName += createdAt;

        Random random = new Random();
        while (!isDocNameAvailable(documentName)) {
            documentName += random.nextInt();
        }

        return documentName;
    }

    private boolean isDocNameAvailable(String name) {
        for (Document document : mDocuments) {
            if (document.getName().equals(name)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.documents_menu, menu);

        final MenuItem searchMenuItem = menu.findItem(R.id.documents_menu_search);
        AdaptableSearchView searchView = (AdaptableSearchView) searchMenuItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint("Search Here!");
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    mDocumentsShow.clear();
                    for(Document d: mDocuments) {
                        if(d.getName().toLowerCase().trim().contains(newText.toLowerCase().trim())){
                            mDocumentsShow.add(d);
                        }
                    }
                    sortDocumentShow();
                    return true;
                }
            });
            searchView.setOnSizeChangeListener(new AdaptableSearchView.OnSizeChangeListener() {
                @Override
                public void onExpanded() {
                    searchMenuItem.setVisible(false);
                }

                @Override
                public void onCollapsed() {
                    searchMenuItem.setVisible(true);
                }
            });
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.documents_menu_sort: {
                showRadioButtonDialog();
                return true;
            }
            case R.id.documents_menu_import_images: {
                openGalleryWithPermissions();
                return true;
            }
            case R.id.documents_menu_search: {
                return true;
            }
            case R.id.documents_menu_select: {
                if (mDocuments.size() > 0) {
                    startActivity(new Intent(mContext, DocumentsMultiSelectActivity.class));
                }
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showRadioButtonDialog() {
        final Dialog dialog = new Dialog(mContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_sort);

        RadioGroup rg = dialog.findViewById(R.id.dialog_sort_radio_group);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.dialog_sort_modified_time_dec_radio_button: {
                        sortMethod = SortComparator.METHOD_DECREASING_MODIFIED_TIMESTAMP;
                        Collections.sort(mDocumentsShow, new SortComparator.SortByModifiedDescending());
                        break;
                    }
                    case R.id.dialog_sort_modified_time_inc_radio_button: {
                        sortMethod = SortComparator.METHOD_INCREASING_MODIFIED_TIMESTAMP;
                        Collections.sort(mDocumentsShow, new SortComparator.SortByModifiedAscending());
                        break;
                    }
                    case R.id.dialog_sort_creation_time_dec_radio_button: {
                        sortMethod = SortComparator.METHOD_DECREASING_CREATED_TIMESTAMP;
                        Collections.sort(mDocumentsShow, new SortComparator.SortByCreatedDescending());
                        break;
                    }
                    case R.id.dialog_sort_creation_time_inc_radio_button: {
                        sortMethod = SortComparator.METHOD_INCREASING_CREATED_TIMESTAMP;
                        Collections.sort(mDocumentsShow, new SortComparator.SortByCreatedAscending());
                        break;
                    }
                    case R.id.dialog_sort_document_name_inc_radio_button: {
                        sortMethod = SortComparator.METHOD_DOC_NAME_A_2_Z;
                        Collections.sort(mDocumentsShow, new SortComparator.SortByDocNameAscending());
                        break;
                    }
                    case R.id.dialog_sort_document_name_dec_radio_button: {
                        sortMethod = SortComparator.METHOD_DOC_NAME_Z_2_A;
                        Collections.sort(mDocumentsShow, new SortComparator.SortByDocNameDescending());
                        break;
                    }
                }
                App.setDocumentsSortMethod(sortMethod);
                mDocumentsAdapter.notifyDataSetChanged();
                dialog.cancel();
            }
        });
        RadioButton radioButton = dialog.findViewById(getSortRadioButtonId());
        radioButton.setChecked(true);
        dialog.show();
    }

    private int getSortRadioButtonId() {
        switch (sortMethod) {
            case SortComparator.METHOD_DECREASING_MODIFIED_TIMESTAMP:
                return R.id.dialog_sort_modified_time_dec_radio_button;
            case SortComparator.METHOD_INCREASING_MODIFIED_TIMESTAMP:
                return R.id.dialog_sort_modified_time_inc_radio_button;
            case SortComparator.METHOD_DECREASING_CREATED_TIMESTAMP:
                return R.id.dialog_sort_creation_time_dec_radio_button;
            case SortComparator.METHOD_INCREASING_CREATED_TIMESTAMP:
                return R.id.dialog_sort_creation_time_inc_radio_button;
            case SortComparator.METHOD_DOC_NAME_A_2_Z:
                return R.id.dialog_sort_document_name_inc_radio_button;
            case SortComparator.METHOD_DOC_NAME_Z_2_A:
                return R.id.dialog_sort_document_name_dec_radio_button;
            default:
                return R.id.dialog_sort_modified_time_dec_radio_button;
        }
    }

    @Override
    public void onBackPressed() {
        // moving task to background
        moveTaskToBack(true);
    }
}
