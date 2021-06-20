package cam.scanner.mini.customadapters;

import android.graphics.Bitmap;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import cam.scanner.mini.App;
import cam.scanner.mini.R;
import cam.scanner.mini.localdatabase.Document;
import cam.scanner.mini.utils.Constants;
import cam.scanner.mini.utils.FileHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DocumentsMultiSelectAdapter extends RecyclerView.Adapter<DocumentsMultiSelectAdapter.CustomViewHolder> {
    private List<Document> mDocuments;
    private OnItemPressedListener mListener;
    private HashMap<Long, Bitmap> bitmapsCache = new HashMap<>();
    private Set<Long> mSelectedDocumentIds = new HashSet<>();

    public DocumentsMultiSelectAdapter(List<Document> documents, OnItemPressedListener listener, int selectedIndex) {
        mDocuments = documents;
        mListener = listener;
        if (selectedIndex >= 0 && selectedIndex < mDocuments.size()) {
            mSelectedDocumentIds.add(mDocuments.get(selectedIndex).getId());
        }
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_document, parent, false);
        return (new CustomViewHolder(view));
    }

    @Override
    public void onBindViewHolder(@NonNull final CustomViewHolder holder, final int position) {
        final Document document = mDocuments.get(position);
        int numPages = document.getPageIds().size();
        holder.nameTextView.setText(document.getName());
        holder.timestampTextView.setText(String.format(
                "%s %s", document.getModifiedAt().substring(0, 16), document.getModifiedAt().substring(20).toLowerCase()
        ));
        holder.numPagesTextView.setText(String.format(
                Locale.getDefault(), "%d image%s", numPages, numPages == 1 ? "" : "s"
        ));
        if (holder.checkBox.getVisibility() != View.VISIBLE) {
            holder.checkBox.setVisibility(View.VISIBLE);
        }
        holder.checkBox.setChecked(mSelectedDocumentIds.contains(document.getId()));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.checkBox.isChecked()) {
                    mSelectedDocumentIds.remove(document.getId());
                } else {
                    mSelectedDocumentIds.add(document.getId());
                }
                notifyItemChanged(position);
                if (mListener != null) {
                    mListener.onItemPressed(position);
                }
            }
        });
        setBitmap(holder, document, position);
    }

    private void setBitmap(final CustomViewHolder holder, final Document document, final int postion) {
        if (document.getPageIds().size() <= 0) {
            return;
        }
        final Long pageId = document.getPageIds().get(0);
        if (bitmapsCache.containsKey(pageId)) {
            holder.thumbnailImageView.setImageBitmap(bitmapsCache.get(pageId));
            return;
        }

        Pair<Bitmap, Bitmap> bitmapsPair = App.retrieveBitmaps(document.getId(), pageId);
        if (bitmapsPair != null && bitmapsPair.second != null) {
            holder.thumbnailImageView.setImageBitmap(bitmapsPair.second);
            bitmapsCache.put(pageId, bitmapsPair.second);
            return;
        }

        holder.thumbnailImageView.setImageBitmap(null);
        FileHelper.loadImage(
                String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, document.getIdAsString(), String.valueOf(pageId)),
                new FileHelper.OnImageLoadListener() {
                    @Override
                    public void onImageLoadComplete(Bitmap bitmap) {
                        if (bitmap == null) {
                            return;
                        }
                        App.cacheBitmaps(document.getId(), pageId, null, bitmap);
                        bitmapsCache.put(pageId, bitmap);
                        notifyItemChanged(postion);
                    }
                }
        );
    }

    @Override
    public int getItemCount() {
        return mDocuments.size();
    }

    static class CustomViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView timestampTextView;
        TextView numPagesTextView;
        ImageView thumbnailImageView;
        CheckBox checkBox;
        View itemView;
        CustomViewHolder(@NonNull View itemView) {
            super(itemView);

            this.itemView           = itemView;
            this.nameTextView       = itemView.findViewById(R.id.item_document_name_text_view);
            this.timestampTextView  = itemView.findViewById(R.id.item_document_timestamp_text_view);
            this.numPagesTextView   = itemView.findViewById(R.id.item_document_num_pages_text_view);
            this.thumbnailImageView = itemView.findViewById(R.id.item_document_thumbnail_image_view);
            this.checkBox           = itemView.findViewById(R.id.item_document_check_box);
        }
    }

    public interface OnItemPressedListener {
        public void onItemPressed(int positon);
    }

    public Set<Long> getSelectedDocumentIds() {
        return mSelectedDocumentIds;
    }

    public void selectAll() {
        mSelectedDocumentIds.clear();
        for (int i = 0; i < mDocuments.size(); ++i) {
            mSelectedDocumentIds.add(mDocuments.get(i).getId());
        }
        if (mListener != null) {
            mListener.onItemPressed(-1);
        }
        notifyDataSetChanged();
    }

    public void deselectAll() {
        mSelectedDocumentIds.clear();
        if (mListener != null) {
            mListener.onItemPressed(-1);
        }
        notifyDataSetChanged();
    }
}

