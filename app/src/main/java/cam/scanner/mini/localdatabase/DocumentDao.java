package cam.scanner.mini.localdatabase;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DocumentDao {
    @Query("SELECT * FROM document")
    List<Document> getAll();

    @Query("SELECT * FROM document")
    LiveData<List<Document>> getAllLive();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Document document);

    @Query("SELECT * FROM document WHERE id = (:id)")
    Document loadById(long id);

    @Query("SELECT * FROM document WHERE name = (:name)")
    Document loadByName(String name);

    @Query("DELETE FROM document WHERE id = (:id)")
    void delete(Long id);
}
