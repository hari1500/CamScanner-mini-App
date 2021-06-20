package cam.scanner.mini.utils;

import android.graphics.Bitmap;
import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.room.Room;

import cam.scanner.mini.App;
import cam.scanner.mini.R;
import cam.scanner.mini.localdatabase.Document;
import cam.scanner.mini.localdatabase.DocumentDao;
import cam.scanner.mini.localdatabase.LocalDatabase;
import cam.scanner.mini.localdatabase.Page;
import cam.scanner.mini.localdatabase.PageDao;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseHelper {
    private static LocalDatabase mLocalDatabase = null;
    private static DocumentDao mDocumentDao = null;
    private static PageDao mPageDao = null;

    public static void init() {
        // Already existing
        if (mLocalDatabase != null) {
            return;
        }
        mLocalDatabase = Room.databaseBuilder(
                App.getContext(), LocalDatabase.class, App.getContext().getString(R.string.local_database_name)
        ).allowMainThreadQueries().build();
        mDocumentDao = mLocalDatabase.getDocumentDao();
        mPageDao = mLocalDatabase.getPageDao();
    }

    public static List<Document> getAllDocuments() {
        if (mDocumentDao == null) {
            return new ArrayList<>();
        }
        return mDocumentDao.getAll();
    }

    public static void getAllDocumentsLive(LifecycleOwner owner, Observer<List<Document>> observer) {
        if (mDocumentDao == null) {
            return;
        }
        mDocumentDao.getAllLive().observe(owner, observer);
    }

    public static void updateDocument(Document document) {
        if (mDocumentDao == null) {
            return;
        }

        DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault());
        document.setModifiedAt(dateFormat.format(Calendar.getInstance().getTime()));

        mDocumentDao.insert(document);
    }

    public static void deleteDocument(Long docId) {
        if (mDocumentDao == null || mPageDao == null) {
            return;
        }
        mDocumentDao.delete(docId);
        Document document = getDocumentById(docId);
        if (document != null) {
            for (Long pageId : document.getPageIds()) {
                deletePage(pageId, docId);
            }
        }
        App.removeFromCache(docId);
    }

    public static void updateDocumentName(Long docId, String updatedName) {
        if (mDocumentDao == null) {
            return;
        }
        Document document = mDocumentDao.loadById(docId);
        document.setName(updatedName);
        updateDocument(document);
    }

    public static Document createDocument(@NonNull String name) {
        if (mDocumentDao == null) {
            return null;
        }

        DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault());
        String createdAt = dateFormat.format(Calendar.getInstance().getTime());

        Document document = new Document(name, createdAt, createdAt, new ArrayList<Long>());
        long documentId = mDocumentDao.insert(document);
        return mDocumentDao.loadById(documentId);
    }

    public static Document getDocumentById(long documentId) {
        return mDocumentDao == null ? null : mDocumentDao.loadById(documentId);
    }

    public static Document getDocumentByName(String documentName) {
        return mDocumentDao == null ? null : mDocumentDao.loadByName(documentName);
    }

    public static void shiftPage(Document document, int fromPos, int toPos) {
        document.shiftPage(fromPos, toPos);
        updateDocument(document);
    }

    public static void createPage(Document document, Bitmap originalImage, Bitmap modifiedImage, Map<Integer, PointF> pointFMap) {
        if (mPageDao == null) {
            return;
        }
        DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault());
        String lastModifiedAt = dateFormat.format(Calendar.getInstance().getTime());

        Page page = new Page(document, lastModifiedAt, Utils.toListOfPointF(pointFMap));
        long pageId =  mPageDao.insert(page);

        document.addPage(pageId);
        document.setModifiedAt(lastModifiedAt);
        updateDocument(document);

        App.cacheBitmaps(page.getDocumentId(), pageId, originalImage, modifiedImage);
        FileHelper.saveImage(
                String.format(Constants.ORIGINAL_IMAGE_PATH_FORMAT, page.getDocumentIdAsString(), pageId),
                originalImage, null
        );
        FileHelper.saveImage(
                String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, page.getDocumentIdAsString(), pageId),
                modifiedImage, null
        );
    }

    public static void updateCorners(Page page, Map<Integer, PointF> pointFMap) {
        if (mPageDao == null) {
            return;
        }
        page.setSelectedCorners(Utils.toListOfPointF(pointFMap));
        mPageDao.insert(page);
        updateDocument(getDocumentById(page.getDocumentId()));
    }

    public static void deletePage(Page page) {
        if (mPageDao == null) {
            return;
        }

        Document document = getDocumentById(page.getDocumentId());
        if (document != null) {
            document.removePage(page.getId());
            updateDocument(document);
        }

        mPageDao.deleteById(page.getId());

        App.removeFromCache(page.getDocumentId(), page.getId());
        FileHelper.deleteImage(
                String.format(Constants.ORIGINAL_IMAGE_PATH_FORMAT, page.getDocumentIdAsString(), page.getIdAsString()), null
        );
        FileHelper.deleteImage(
                String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, page.getDocumentIdAsString(), page.getIdAsString()), null
        );
    }

    public static void deletePage(Long pageId, Long docId) {
        if (mPageDao == null) {
            return;
        }

        Document document = getDocumentById(docId);
        if (document != null) {
            document.removePage(pageId);
            updateDocument(document);
        }

        mPageDao.deleteById(pageId);

        App.removeFromCache(docId, pageId);
        FileHelper.deleteImage(
                String.format(Constants.ORIGINAL_IMAGE_PATH_FORMAT, docId, pageId), null
        );
        FileHelper.deleteImage(
                String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, docId, pageId), null
        );
    }

    public static void getAllPagesOfDocumentLive(long documentId, LifecycleOwner owner, Observer<List<Page>> observer) {
        if (mPageDao == null) {
            return;
        }

        mPageDao.loadByDocumentIdLive(documentId).observe(owner, observer);
    }

    public static List<Page> getAllPagesOfDocument(long documentId) {
        if (mPageDao == null) {
            return new ArrayList<>();
        }

        Document document = getDocumentById(documentId);
        List<Page> pages = mPageDao.loadByDocumentId(documentId);
        List<Page> sortedPages = new ArrayList<>();

        if (pages == null || document == null) {
            return sortedPages;
        }
        HashMap<Long, Page> pageHashMap = new HashMap<>();
        for (Page page : pages) {
            pageHashMap.put(page.getId(), page);
        }

        for (long pageId : document.getPageIds()) {
            if (pageHashMap.containsKey(pageId)) {
                sortedPages.add(pageHashMap.get(pageId));
            }
        }

        return sortedPages;
    }
}
