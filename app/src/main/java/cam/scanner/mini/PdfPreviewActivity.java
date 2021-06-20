package cam.scanner.mini;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import cam.scanner.mini.R;

import cam.scanner.mini.customadapters.PdfImageAdapter;
import cam.scanner.mini.localdatabase.Document;
import cam.scanner.mini.utils.Constants;
import cam.scanner.mini.utils.Utils;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class PdfPreviewActivity extends AppCompatActivity {
    private Context mContext;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;

    private static final String readExtStoragePermission    = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static final String writeExtStoragePermission   = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final int PDF_SAVE_CODE                  = 11;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_preview);
        Document document = App.getCurrentDocument();
        if(document == null) {
            finish();
            return;
        }
        setTitle("Preview - " + document.getName());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext = this;
        try {
            Utils.createPdf(document);
            File pdfFile = new File(App.getContext().getCacheDir(), document.getName()+".pdf");
            parcelFileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
        }

        PdfImageAdapter pdfImageAdapter = new PdfImageAdapter(pdfRenderer, pdfRenderer.getPageCount());
        RecyclerView pdfRecyclerView = findViewById(R.id.pdf_recycler_view);
        pdfRecyclerView.setHasFixedSize(true);
        pdfRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        pdfRecyclerView.setAdapter(pdfImageAdapter);
    }

    @Override
    protected void onDestroy() {
        try {
            pdfRenderer.close();
            parcelFileDescriptor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pdf_review_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pdf_review_menu_share: {
                ArrayList<Uri> files = new ArrayList<>();
                Document document = App.getCurrentDocument();
                if (document == null) {
                    return true;
                }
                try {
                    Uri uri = Utils.createPdf(document);
                    if (uri != null) {
                        files.add(uri);
                    }
                } catch (DocumentException | IOException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                intent.setType("application/pdf");
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);

                return true;
            }
            case R.id.pdf_review_menu_save: {
                onClickSave();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void onClickSave() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            savePdf();
            return;
        }

        if (!Utils.isPermissionGranted(readExtStoragePermission)) {
            ActivityCompat.requestPermissions(this, new String[]{readExtStoragePermission}, Constants.READ_EXT_STORAGE_PERMISSION_REQ_CODE);
        } else if(!Utils.isPermissionGranted(writeExtStoragePermission)) {
            ActivityCompat.requestPermissions(this, new String[]{writeExtStoragePermission}, Constants.WRITE_EXT_STORAGE_PERMISSION_REQ_CODE);
        } else {
            savePdf();
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
                    savePdf();
                }
            } else {
                Utils.showToast(mContext, "Need Permissions for reading files", Toast.LENGTH_SHORT, Gravity.CENTER);
            }
        } else if (requestCode == Constants.WRITE_EXT_STORAGE_PERMISSION_REQ_CODE) {
            if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                savePdf();
            } else {
                Utils.showToast(mContext, "Need Permissions for writing files", Toast.LENGTH_SHORT, Gravity.CENTER);
            }
        }
    }

    private void savePdf() {
        Document document = App.getCurrentDocument();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PDF_SAVE_CODE && resultCode == RESULT_OK && data != null){
            Uri uri = data.getData();
            Document document = App.getCurrentDocument();
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
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}