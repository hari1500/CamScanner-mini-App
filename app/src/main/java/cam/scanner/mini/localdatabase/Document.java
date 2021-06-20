package cam.scanner.mini.localdatabase;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.List;

@Entity
public class Document implements Serializable {
    @ColumnInfo(name = "id")
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "modified_at")
    private String modifiedAt;

    @ColumnInfo(name = "page_ids")
    private List<Long> pageIds;

    public Document(@NonNull String name, String createdAt, String modifiedAt, @NonNull List<Long> pageIds) {
        this.name           = name;
        this.createdAt      = createdAt;
        this.modifiedAt     = modifiedAt;
        this.pageIds        = pageIds;
    }

    public void addPage(Long pageId) {
        pageIds.add(pageId);
    }

    public void removePage(Long pageId) {
        pageIds.remove(pageId);
    }

    public void shiftPage(int fromPos, int toPos) {
        if (fromPos >= 0 && fromPos < pageIds.size() && toPos >= 0 && toPos < pageIds.size()) {
            pageIds.add(toPos, pageIds.remove(fromPos));
        }
    }

    @NonNull
    public String getName() {
        return name;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getModifiedAt() {
        return modifiedAt;
    }

    public List<Long> getPageIds() {
        return pageIds;
    }

    public long getId() {
        return id;
    }

    public String getIdAsString() {
        return String.valueOf(id);
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setModifiedAt(String modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public void setPageIds(List<Long> pageIds) {
        this.pageIds = pageIds;
    }
}
