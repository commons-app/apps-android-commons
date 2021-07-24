package fr.free.nrw.commons.bookmarks.items;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import java.util.List;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;

/**
 * Tab fragment to show list of Wikidata Items
 */
public class BookmarkItemsFragment extends DaggerFragment {

    @BindView(R.id.status_message)
    TextView statusTextView;

    @BindView(R.id.loading_images_progress_bar)
    ProgressBar progressBar;

    @BindView(R.id.list_view)
    RecyclerView recyclerView;

    @BindView(R.id.parent_layout)
    RelativeLayout parentLayout;

    @Inject
    BookmarkItemsController controller;

    public static BookmarkItemsFragment newInstance() {
        return new BookmarkItemsFragment();
    }

    @Override
    public View onCreateView(
        @NonNull final LayoutInflater inflater,
        final ViewGroup container,
        final Bundle savedInstanceState
    ) {
        final View v = inflater.inflate(R.layout.fragment_bookmarks_items, container, false);
        ButterKnife.bind(this, v);
        return v;
    }

    @Override
    public void onViewCreated(final @NotNull View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        initList(requireContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        initList(requireContext());
    }

    /**
     * Get list of DepictedItem and sets to the adapter
     * @param context context
     */
    private void initList(final Context context) {
        final List<DepictedItem> depictItems = controller.loadFavoritesItems();
        final BookmarkItemsAdapter adapter = new BookmarkItemsAdapter(depictItems, context);
        recyclerView.setAdapter(adapter);
        progressBar.setVisibility(View.GONE);
        if (depictItems.isEmpty()) {
            statusTextView.setText(R.string.bookmark_empty);
            statusTextView.setVisibility(View.VISIBLE);
        } else {
            statusTextView.setVisibility(View.GONE);
        }
    }
}
