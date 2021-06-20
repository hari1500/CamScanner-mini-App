package cam.scanner.mini.customadapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import cam.scanner.mini.App;
import cam.scanner.mini.R;
import cam.scanner.mini.localdatabase.Page;
import cam.scanner.mini.utils.Constants;
import cam.scanner.mini.utils.FileHelper;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class PagesAdapter2 extends PagerAdapter {
    private Context mContext;
    private List<Page> mPages;
    private HashMap<Long, Bitmap> bitmapsCache = new HashMap<>();

    public PagesAdapter2(Context context, List<Page> pages) {
        mContext = context;
        mPages = pages;
    }

    @Override
    public int getCount() {
        return mPages.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, final int position) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_page2, container, false);

        final Page page         = mPages.get(position);
        PhotoView imageView     = itemView.findViewById(R.id.item_page2_image_view);
        ProgressBar progressBar = itemView.findViewById(R.id.item_page2_image_progress_bar);
        TextView indexTextView  = itemView.findViewById(R.id.item_page2_image_index_text_view);

        setBitmap(imageView, progressBar, page);
        indexTextView.setText(String.format(Locale.getDefault(), "%d / %d", position + 1, getCount()));

        container.addView(itemView);
        return itemView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    private void setBitmap(final PhotoView imageView, final ProgressBar progressBar, final Page page) {
        if (bitmapsCache.containsKey(page.getId())) {
            imageView.setImageBitmap(bitmapsCache.get(page.getId()));
            return;
        }

        Pair<Bitmap, Bitmap> bitmapsPair = App.retrieveBitmaps(page.getDocumentId(), page.getId());
        if (bitmapsPair != null && bitmapsPair.second != null) {
            imageView.setImageBitmap(bitmapsPair.second);
            bitmapsCache.put(page.getId(), bitmapsPair.second);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        FileHelper.loadImage(
                String.format(Constants.MODIFIED_IMAGE_PATH_FORMAT, page.getDocumentIdAsString(), page.getIdAsString()),
                new FileHelper.OnImageLoadListener() {
                    @Override
                    public void onImageLoadComplete(Bitmap bitmap) {
                        App.cacheBitmaps(page.getDocumentId(), page.getId(), null, bitmap);
                        bitmapsCache.put(page.getId(), bitmap);
                        imageView.setImageBitmap(bitmap);
                        progressBar.setVisibility(View.GONE);
                    }
                }
        );
    }

    public boolean isImageLoaded(int position) {
        if (position < 0 && position >= getCount()) {
            return false;
        }

        return bitmapsCache.containsKey(mPages.get(position).getId());
    }

    public void invalidateBitmapCache() {
        bitmapsCache.clear();
    }
}
