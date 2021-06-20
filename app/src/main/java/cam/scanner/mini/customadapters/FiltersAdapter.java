package cam.scanner.mini.customadapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import cam.scanner.mini.R;
import cam.scanner.mini.imageprocessing.Filters;
import com.google.android.material.shape.MaterialShapeDrawable;

import java.util.HashMap;
import java.util.Map;

public class FiltersAdapter extends RecyclerView.Adapter<FiltersAdapter.CustomViewHolder> {
    private Context mContext;
    private Bitmap mOrignialImage;
    private Filters.Type[] mFilterTypes;
    private onItemPressListener mListener;
    private Map<Filters.Type, Bitmap> filteredImagesCache;
    private int selectedItem;

    public FiltersAdapter(Context context, Bitmap bitmap, onItemPressListener listener) {
        mContext            = context;
        mOrignialImage      = bitmap;
        mListener           = listener;
        mFilterTypes        = Filters.Type.values();
        filteredImagesCache = new HashMap<>();
        selectedItem        = -1;
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_filter, parent, false);
        return (new CustomViewHolder(view));
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, final int position) {
        Filters.Type type = mFilterTypes[position];
        holder.nameTextView.setText(Filters.getFilterName(type));

        final Bitmap filteredImage;
        if (filteredImagesCache.containsKey(type)) {
            filteredImage = filteredImagesCache.get(type);
        } else {
            filteredImage = Filters.getFilteredImage(mOrignialImage, type);
            filteredImagesCache.put(type, filteredImage);
        }
        holder.imageView.setImageBitmap(filteredImage);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onItemClick(filteredImage);
                }
                selectedItem = position;
                notifyDataSetChanged();
            }
        });
        if (selectedItem == position) {
            MaterialShapeDrawable shapeDrawable = new MaterialShapeDrawable();
            shapeDrawable.setFillColor(ContextCompat.getColorStateList(mContext, android.R.color.transparent));
            shapeDrawable.setStroke(2.0f, ContextCompat.getColor(mContext, R.color.colorAccent));
            ViewCompat.setBackground(holder.itemView, shapeDrawable);
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() {
        return mFilterTypes.length;
    }

    static class CustomViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        ImageView imageView;
        View itemView;
        CustomViewHolder(@NonNull View itemView) {
            super(itemView);

            this.itemView = itemView;
            this.nameTextView = itemView.findViewById(R.id.item_filter_name_text_view);
            this.imageView = itemView.findViewById(R.id.item_filter_image_view);
        }
    }

    public interface onItemPressListener {
        public void onItemClick(Bitmap filteredBitmap);
    }
}
