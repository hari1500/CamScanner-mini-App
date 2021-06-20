package cam.scanner.mini.localdatabase;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {Document.class, Page.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class LocalDatabase extends RoomDatabase {
    public abstract DocumentDao getDocumentDao();
    public abstract PageDao getPageDao();
}
