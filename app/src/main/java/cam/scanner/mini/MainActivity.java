package cam.scanner.mini;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import cam.scanner.mini.R;

import cam.scanner.mini.localdatabase.Document;
import cam.scanner.mini.localdatabase.Page;
import cam.scanner.mini.utils.Constants;
import cam.scanner.mini.utils.DatabaseHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Context mContext;
    private boolean mHasDBCorrected;
    private boolean mHasSplashTimeCompleted;

    private static final String TAG = MainActivity.class.getCanonicalName();
    private static final int SPLASH_TIME_OUT = 2 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }

        mContext = this;
        mHasDBCorrected = false;
        mHasSplashTimeCompleted = false;

        correctDB();
        setupSplashTimer();
    }

    private void setupSplashTimer() {
        new Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        mHasSplashTimeCompleted = true;
                        if (mHasDBCorrected) {
                            Log.v(TAG, "Timer completed at last");
                            startActivity(new Intent(mContext, DocumentsActivity.class));
                            finish();
                        }
                    }
                }, SPLASH_TIME_OUT
        );
    }

    private void correctDB() {
        ArrayList<String> files = new ArrayList<>(Arrays.asList(fileList()));
        Log.v(TAG, files.toString());

        List<Document> allDocuments = DatabaseHelper.getAllDocuments();
        for (Document document : allDocuments) {
            List<Page> allPagesOfDocument = DatabaseHelper.getAllPagesOfDocument(document.getId());
            List<Boolean> filesAvailability = new ArrayList<>();
            for (Page page : allPagesOfDocument) {
                String originalImagePath = String.format(Constants.ORIGINAL_IMAGE_PATH_FORMAT, document.getIdAsString(), page.getIdAsString());
                String modifiedImagePath = String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, document.getIdAsString(), page.getIdAsString());

                boolean filesAvailable = files.contains(originalImagePath) && files.contains(modifiedImagePath);
                filesAvailability.add(filesAvailable);
                if (!filesAvailable) {
                    Log.v(TAG, "Not found in " + document.getName());
                    DatabaseHelper.deletePage(page.getId(), document.getId());
                }
            }

            if (!filesAvailability.contains(true)) {
                DatabaseHelper.deleteDocument(document.getId());
            }
        }

        mHasDBCorrected = true;
        if (mHasSplashTimeCompleted) {
            Log.v(TAG, "DB correction completed at last");
            startActivity(new Intent(mContext, DocumentsActivity.class));
            finish();
        }
    }
}