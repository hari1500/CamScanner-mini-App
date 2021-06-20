package cam.scanner.mini.customadapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import cam.scanner.mini.R;
import cam.scanner.mini.components.CropableImageView;
import cam.scanner.mini.components.LockableViewPager;
import cam.scanner.mini.components.PolygonView;
import cam.scanner.mini.imageprocessing.EdgeHelper;
import cam.scanner.mini.utils.BufferedImagesHelper;
import cam.scanner.mini.utils.Utils;

import java.util.Locale;

public class CapturedImagesAdapter extends PagerAdapter {
    private Context mContext;
    private LockableViewPager mViewPager;
    private PolygonView.OnPointDragListener mPointDragListener;

    public CapturedImagesAdapter(Context context, LockableViewPager viewPager) {
        mContext = context;
        mViewPager = viewPager;
        mPointDragListener = new PolygonView.OnPointDragListener() {
            @Override
            public void onPointDraggingStarted() {
                mViewPager.setSwipeLocked(true);
            }

            @Override
            public void onPointDraggingCompleted() {
                mViewPager.setSwipeLocked(false);
            }
        };
    }

    @Override
    public int getCount() {
        return BufferedImagesHelper.getBufferedImages().size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, final int position) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_captured_image, container, false);

        final FrameLayout imageHolderLayout = itemView.findViewById(R.id.item_captured_image_holder_layout);
        CropableImageView imageView = itemView.findViewById(R.id.item_captured_image_cropable_image_view);
        TextView indexTextView = itemView.findViewById(R.id.item_captured_image_index_text_view);

        imageView.setImageBitmap(BufferedImagesHelper.getBufferedImages().get(position).getModifiedImage());
        imageView.setOnDrawListener(new CropableImageView.OnDrawListener() {
            @Override
            public void onDraw(int x, int y, int width, int height) {
                BufferedImagesHelper.BufferedImage bufferedImage = BufferedImagesHelper.getBufferedImages().get(position);
                PolygonView oldPolygonView = bufferedImage.getPolygonView();
                PolygonView newPolygonView = new PolygonView(mContext, x, y, width, height, mPointDragListener);
                newPolygonView.setBitmap(bufferedImage.getModifiedImage());
                if (oldPolygonView != null) {
                    newPolygonView.setPoints(oldPolygonView.getPoints());
                } else {
                    newPolygonView.setPoints(Utils.getPointFMap(EdgeHelper.getCorners(bufferedImage.getOriginalImage())));
                }
                imageHolderLayout.addView(newPolygonView);
                BufferedImagesHelper.BufferedImage updated = new BufferedImagesHelper.BufferedImage(
                        bufferedImage.getOriginalImage(), bufferedImage.getModifiedImage(), newPolygonView.getPoints()
                );
                updated.setPolygonView(newPolygonView);
                updated.setRotation(bufferedImage.getRotation());
                BufferedImagesHelper.updateImageAt(position, updated);
            }
        });
        indexTextView.setText(String.format(Locale.getDefault(), "%d / %d", position + 1, getCount()));

        container.addView(itemView);
        return itemView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }
}
