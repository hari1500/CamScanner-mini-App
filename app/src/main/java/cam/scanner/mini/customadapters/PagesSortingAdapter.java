package cam.scanner.mini.customadapters;

import android.graphics.Bitmap;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.List;

public class PagesSortingAdapter extends RecyclerView.Adapter<PagesSortingAdapter.CustomViewHolder> {
    private List<Page> mPages;
    private HashMap<Long, Bitmap> bitmapsCache = new HashMap<>();

    public PagesSortingAdapter(List<Page> pages) {
        mPages = pages;
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_page, parent, false);
        return (new CustomViewHolder(view));
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, final int position) {
        Page page = mPages.get(position);
        holder.indexTextView.setText(String.valueOf(position + 1));
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
            return;
        }

        Pair<Bitmap, Bitmap> bitmapsPair = App.retrieveBitmaps(page.getDocumentId(), page.getId());
        if (bitmapsPair != null && bitmapsPair.second != null) {
            holder.pageImageView.setImageBitmap(bitmapsPair.second);
            bitmapsCache.put(page.getId(), bitmapsPair.second);
            holder.imageProgressBar.setVisibility(View.GONE);
            return;
        }

        holder.pageImageView.setImageBitmap(null);
        holder.imageProgressBar.setVisibility(View.VISIBLE);
        FileHelper.loadImage(
                String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, page.getDocumentIdAsString(), page.getIdAsString()),
                new FileHelper.OnImageLoadListener() {
                    @Override
                    public void onImageLoadComplete(Bitmap bitmap) {
                        holder.imageProgressBar.setVisibility(View.GONE);
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
        CustomViewHolder(@NonNull View itemView) {
            super(itemView);

            this.pageImageView = itemView.findViewById(R.id.item_page_image_view);
            this.indexTextView = itemView.findViewById(R.id.item_page_image_index_text_view);
            this.imageProgressBar = itemView.findViewById(R.id.item_page_image_progress_bar);
        }
    }
}