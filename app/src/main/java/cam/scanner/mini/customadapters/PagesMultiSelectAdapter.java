package cam.scanner.mini.customadapters;

import android.graphics.Bitmap;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import cam.scanner.mini.App;
import cam.scanner.mini.R;
import cam.scanner.mini.localdatabase.Page;
import cam.scanner.mini.utils.Constants;
import cam.scanner.mini.utils.FileHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PagesMultiSelectAdapter extends RecyclerView.Adapter<PagesMultiSelectAdapter.CustomViewHolder> {
    private List<Page> mPages;
    private OnItemPressedListener mListener;
    private HashMap<Long, Bitmap> bitmapsCache = new HashMap<>();
    private Set<Long> mSelectedPageIds = new HashSet<>();

    public PagesMultiSelectAdapter(List<Page> pages, OnItemPressedListener listener, int selectedIndex) {
        mPages = pages;
        mListener = listener;
        if (selectedIndex >= 0 && selectedIndex < mPages.size()) {
            mSelectedPageIds.add(mPages.get(selectedIndex).getId());
        }
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_page, parent, false);
        return (new CustomViewHolder(view));
    }

    @Override
    public void onBindViewHolder(@NonNull final CustomViewHolder holder, final int position) {
        final Page page = mPages.get(position);
        holder.indexTextView.setText(String.valueOf(position + 1));
        if (holder.checkBox.getVisibility() != View.VISIBLE) {
            holder.checkBox.setVisibility(View.VISIBLE);
        }
        holder.checkBox.setChecked(mSelectedPageIds.contains(page.getId()));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.checkBox.isChecked()) {
                    mSelectedPageIds.remove(page.getId());
                } else {
                    mSelectedPageIds.add(page.getId());
                }
                notifyItemChanged(position);
                if (mListener != null) {
                    mListener.onItemPressed(position);
                }
            }
        });
        setBitmap(holder, page, position);
    }

    @Override
    public int getItemCount() {
        return mPages.size();
    }

    private void setBitmap(final CustomViewHolder holder, final Page page, final int postion) {
        if (bitmapsCache.containsKey(page.getId())) {
            holder.pageImageView.setImageBitmap(bitmapsCache.get(page.getId()));
            holder.imageProgressBar.setVisibility(View.GONE);
            holder.itemView.setClickable(true);
            return;
        }

        Pair<Bitmap, Bitmap> bitmapsPair = App.retrieveBitmaps(page.getDocumentId(), page.getId());
        if (bitmapsPair != null && bitmapsPair.second != null) {
            holder.pageImageView.setImageBitmap(bitmapsPair.second);
            bitmapsCache.put(page.getId(), bitmapsPair.second);
            holder.imageProgressBar.setVisibility(View.GONE);
            holder.itemView.setClickable(true);
            return;
        }

        holder.pageImageView.setImageBitmap(null);
        holder.itemView.setClickable(false);
        holder.imageProgressBar.setVisibility(View.VISIBLE);
        FileHelper.loadImage(
                String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, page.getDocumentIdAsString(), page.getIdAsString()),
                new FileHelper.OnImageLoadListener() {
                    @Override
                    public void onImageLoadComplete(Bitmap bitmap) {
                        holder.imageProgressBar.setVisibility(View.GONE);
                        holder.itemView.setClickable(true);
                        if (bitmap == null) {
                            return;
                        }
                        App.cacheBitmaps(page.getDocumentId(), page.getId(), null, bitmap);
                        bitmapsCache.put(page.getId(), bitmap);
                        notifyItemChanged(postion);
                    }
                }
        );
    }

    static class CustomViewHolder extends RecyclerView.ViewHolder {
        ImageView pageImageView;
        TextView indexTextView;
        ProgressBar imageProgressBar;
        View itemView;
        CheckBox checkBox;
        CustomViewHolder(@NonNull View itemView) {
            super(itemView);

            this.itemView           = itemView;
            this.pageImageView      = itemView.findViewById(R.id.item_page_image_view);
            this.indexTextView      = itemView.findViewById(R.id.item_page_image_index_text_view);
            this.imageProgressBar   = itemView.findViewById(R.id.item_page_image_progress_bar);
            this.checkBox           = itemView.findViewById(R.id.item_page_check_box);
        }
    }

    public interface OnItemPressedListener {
        public void onItemPressed(int positon);
    }

    public Set<Long> getSelectedPageIds() {
        return mSelectedPageIds;
    }

    public void selectAll() {
        mSelectedPageIds.clear();
        for (int i = 0; i < mPages.size(); ++i) {
            mSelectedPageIds.add(mPages.get(i).getId());
        }
        if (mListener != null) {
            mListener.onItemPressed(-1);
        }
        notifyDataSetChanged();
    }

    public void deselectAll() {
        mSelectedPageIds.clear();
        if (mListener != null) {
            mListener.onItemPressed(-1);
        }
        notifyDataSetChanged();
    }
}