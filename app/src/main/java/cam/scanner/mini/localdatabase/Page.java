package cam.scanner.mini.localdatabase;

import android.graphics.PointF;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity
public class Page {
    @ColumnInfo(name = "id")
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "document_id")
    private long documentId;

    @ColumnInfo(name = "last_modified_at")
    private String lastModifiedAt;

    /**
     * not stored in any specific order
     */
    @ColumnInfo(name = "selected_corners")
    private List<PointF> selectedCorners;

    public Page(long id) {
        this.id = id;
    }

    public Page(Document document, String lastModifiedAt, List<PointF> selectedCorners) {
        this.documentId         = document.getId();
        this.lastModifiedAt     = lastModifiedAt;
        this.selectedCorners    = selectedCorners;
    }

    public long getDocumentId() {
        return documentId;
    }

    public String getDocumentIdAsString() {
        return String.valueOf(documentId);
    }

    public String getLastModifiedAt() {
        return lastModifiedAt;
    }

    public List<PointF> getSelectedCorners() {
        return selectedCorners;
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

    public void setDocumentId(long documentId) {
        this.documentId = documentId;
    }

    public void setLastModifiedAt(String lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public void setSelectedCorners(List<PointF> selectedCorners) {
        this.selectedCorners = selectedCorners;
    }
}
