package cam.scanner.mini.localdatabase;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Page page);

    @Query("SELECT * FROM page WHERE document_id = (:documentId)")
    List<Page> loadByDocumentId(long documentId);

    @Query("SELECT * FROM page WHERE document_id = (:documentId)")
    LiveData<List<Page>> loadByDocumentIdLive(long documentId);

    @Query("SELECT * FROM page WHERE id = (:id)")
    Page loadById(long id);

    @Query("DELETE FROM page WHERE id = (:id)")
    void deleteById(long id);

    @Query("DELETE FROM page WHERE document_id = (:documentId)")
    void deleteByDocumentId(long documentId);
}
