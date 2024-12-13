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
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.databinding.FragmentBookmarksItemsBinding;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import java.util.List;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;

/**
 * Tab fragment to show list of bookmarked Wikidata Items
 */
public class BookmarkItemsFragment extends DaggerFragment {

    private FragmentBookmarksItemsBinding binding;

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
        binding = FragmentBookmarksItemsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(final @NotNull View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
        binding.listView.setAdapter(adapter);
        binding.loadingImagesProgressBar.setVisibility(View.GONE);
        if (depictItems.isEmpty()) {
            binding.statusMessage.setText(R.string.bookmark_empty);
            binding.statusMessage.setVisibility(View.VISIBLE);
        } else {
            binding.statusMessage.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
