package cam.scanner.mini;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;

import cam.scanner.mini.R;

import cam.scanner.mini.customadapters.PagesSortingAdapter;
import cam.scanner.mini.localdatabase.Document;
import cam.scanner.mini.localdatabase.Page;
import cam.scanner.mini.utils.DatabaseHelper;

import java.util.List;
import java.util.Objects;

public class PagesSortingActivity extends AppCompatActivity {
    private Context mContext;
    private Document mDocument;
    private List<Page> mPages;
    private PagesSortingAdapter mPagesSortingAdapter;
    private RecyclerView mPagesRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pages_sorting);
        mDocument = App.getCurrentDocument();
        if (mDocument == null) {
            finish();
            return;
        }
        setTitle(String.format("Sorting - %s", mDocument.getName()));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mContext            = this;
        mPages              = DatabaseHelper.getAllPagesOfDocument(mDocument.getId());
        mPagesSortingAdapter= new PagesSortingAdapter(mPages);
        mPagesRecyclerView  = findViewById(R.id.pages_sorting_recycler_view);

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        mPagesRecyclerView.setHasFixedSize(true);
        mPagesRecyclerView.setLayoutManager(new GridLayoutManager(mContext, 2));
        mPagesRecyclerView.setAdapter(mPagesSortingAdapter);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END, 0
        ) {
            int dragFrom = -1;
            int dragTo = -1;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                if(dragFrom == -1) {
                    dragFrom =  fromPosition;
                }
                dragTo = toPosition;

                Objects.requireNonNull(recyclerView.getAdapter()).notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                if (dragFrom >= 0 && dragFrom < mPages.size() && dragTo >= 0 && dragTo < mPages.size()) {
                    mPages.add(dragTo, mPages.remove(dragFrom));
                    if (dragFrom > dragTo) {
                        mPagesSortingAdapter.notifyItemRangeChanged(dragTo, dragFrom - dragTo + 1);
                    } else {
                        mPagesSortingAdapter.notifyItemRangeChanged(dragFrom, dragTo - dragFrom + 1);
                    }

                    DatabaseHelper.shiftPage(mDocument, dragFrom, dragTo);
                }
                dragFrom = -1;
                dragTo = -1;
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(mPagesRecyclerView);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
