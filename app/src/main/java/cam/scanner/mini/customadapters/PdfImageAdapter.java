package cam.scanner.mini.customadapters;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import cam.scanner.mini.R;

public class PdfImageAdapter extends RecyclerView.Adapter<PdfImageAdapter.CustomViewHolder> {
    private PdfRenderer pdfRenderer;
    private int number_of_pages;

    public PdfImageAdapter( PdfRenderer pdfRenderer, int number_of_pages) {
        this.pdfRenderer = pdfRenderer;
        this.number_of_pages = number_of_pages;
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf_image, parent, false);
        return (new PdfImageAdapter.CustomViewHolder(view));
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
        PdfRenderer.Page currentPage = pdfRenderer.openPage(position);
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        holder.pdfImageView.setImageBitmap(bitmap);
        currentPage.close();
    }

    @Override
    public int getItemCount() {
        return number_of_pages;
    }

    static class CustomViewHolder extends RecyclerView.ViewHolder {
        ImageView pdfImageView;

        CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            this.pdfImageView = itemView.findViewById(R.id.item_pdf_image_view);
        }
    }
}
