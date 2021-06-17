package fr.free.nrw.commons.media;

import static fr.free.nrw.commons.Utils.handleWebUrl;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.snackbar.Snackbar;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.bookmarks.Bookmark;
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesContentProvider;
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesDao;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.utils.DownloadUtils;
import fr.free.nrw.commons.utils.ImageUtils;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.disposables.CompositeDisposable;
import java.util.ArrayList;
import java.util.Objects;
import javax.inject.Inject;
import timber.log.Timber;

public class MediaDetailPagerFragment extends CommonsDaggerSupportFragment implements ViewPager.OnPageChangeListener, MediaDetailFragment.Callback {

    @Inject BookmarkPicturesDao bookmarkDao;

    @Inject
    protected OkHttpJsonApiClient okHttpJsonApiClient;

    @Inject
    protected SessionManager sessionManager;

    private static CompositeDisposable compositeDisposable = new CompositeDisposable();

    @BindView(R.id.mediaDetailsPager) ViewPager pager;
    private Boolean editable;
    private boolean isFeaturedImage;
    private boolean isWikipediaButtonDisplayed;
    MediaDetailAdapter adapter;
    private Bookmark bookmark;
    private MediaDetailProvider provider;
    private boolean isFromFeaturedRootFragment;
    private int position;

    private ArrayList<Integer> removedItems=new ArrayList<Integer>();

    public void clearRemoved(){
        removedItems.clear();
    }
    public ArrayList<Integer> getRemovedItems() {
        return removedItems;
    }

    public MediaDetailPagerFragment() {
        this(false, false);
    }

    @SuppressLint("ValidFragment")
    public MediaDetailPagerFragment(Boolean editable, boolean isFeaturedImage) {
        this.editable = editable;
        this.isFeaturedImage = isFeaturedImage;
        isFromFeaturedRootFragment = false;
    }

    @SuppressLint("ValidFragment")
    public MediaDetailPagerFragment(Boolean editable, boolean isFeaturedImage, int position) {
        this.editable = editable;
        this.isFeaturedImage = isFeaturedImage;
        isFromFeaturedRootFragment = true;
        this.position = position;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_media_detail_pager, container, false);
        ButterKnife.bind(this,view);
        pager.addOnPageChangeListener(this);

        adapter = new MediaDetailAdapter(getChildFragmentManager());
        pager.setAdapter(adapter);
        if (savedInstanceState != null) {
            final int pageNumber = savedInstanceState.getInt("current-page");
            pager.setCurrentItem(pageNumber, false);
            getActivity().invalidateOptionsMenu();
        }
        adapter.notifyDataSetChanged();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity)getActivity()).hideTabs();
        }
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("current-page", pager.getCurrentItem());
        outState.putBoolean("editable", editable);
        outState.putBoolean("isFeaturedImage", isFeaturedImage);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            editable = savedInstanceState.getBoolean("editable");
            isFeaturedImage = savedInstanceState.getBoolean("isFeaturedImage");
        }
        setHasOptionsMenu(true);
        initProvider();
    }

    /**
     * initialise the provider, based on from where the fragment was started, as in from an activity
     * or a fragment
     */
    private void initProvider() {
        if (getParentFragment() != null) {
            provider = (MediaDetailProvider) getParentFragment();
        } else {
            provider = (MediaDetailProvider) getActivity();
        }
    }

    public MediaDetailProvider getMediaDetailProvider() {
        return provider;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (getActivity() == null) {
            Timber.d("Returning as activity is destroyed!");
            return true;
        }

        Media m = provider.getMediaAtPosition(pager.getCurrentItem());
        switch (item.getItemId()) {
            case R.id.menu_bookmark_current_image:
                boolean bookmarkExists = bookmarkDao.updateBookmark(bookmark);
                Snackbar snackbar = bookmarkExists ? Snackbar.make(getView(), R.string.add_bookmark, Snackbar.LENGTH_LONG) : Snackbar.make(getView(), R.string.remove_bookmark, Snackbar.LENGTH_LONG);
                snackbar.show();
                updateBookmarkState(item);
                return true;
            case R.id.menu_share_current_image:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, m.getDisplayTitle() + " \n" + m.getPageTitle().getCanonicalUri());
                startActivity(Intent.createChooser(shareIntent, "Share image via..."));
                return true;
            case R.id.menu_browser_current_image:
                // View in browser
                handleWebUrl(requireContext(), Uri.parse(m.getPageTitle().getMobileUri()));
                return true;
            case R.id.menu_download_current_image:
                // Download
                if (!NetworkUtils.isInternetConnectionEstablished(getActivity())) {
                    ViewUtil.showShortSnackbar(getView(), R.string.no_internet);
                    return false;
                }
                DownloadUtils.downloadMedia(getActivity(), m);
                return true;
            case R.id.menu_set_as_wallpaper:
                // Set wallpaper
                setWallpaper(m);
                return true;
            case R.id.menu_set_as_avatar:
                // Set avatar
                setAvatar(m);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Set the media as the device's wallpaper if the imageUrl is not null
     * Fails silently if setting the wallpaper fails
     * @param media
     */
    private void setWallpaper(Media media) {
        if (media.getImageUrl() == null || media.getImageUrl().isEmpty()) {
            Timber.d("Media URL not present");
            return;
        }
        ImageUtils.setWallpaperFromImageUrl(getActivity(), Uri.parse(media.getImageUrl()));
    }

    /**
     * Set the media as user's leaderboard avatar
     * @param media
     */
    private void setAvatar(Media media) {
        if (media.getImageUrl() == null || media.getImageUrl().isEmpty()) {
            Timber.d("Media URL not present");
            return;
        }
        ImageUtils.setAvatarFromImageUrl(getActivity(), media.getImageUrl(),
            Objects.requireNonNull(sessionManager.getCurrentAccount()).name,
            okHttpJsonApiClient, compositeDisposable);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!editable) { // Disable menu options for editable views
            menu.clear(); // see http://stackoverflow.com/a/8495697/17865
            inflater.inflate(R.menu.fragment_image_detail, menu);
            if (pager != null) {
                MediaDetailProvider provider = getMediaDetailProvider();
                if(provider == null) {
                    return;
                }
                final int position;
                if (isFromFeaturedRootFragment) {
                    position = this.position;
                } else {
                    position = pager.getCurrentItem();
                }

                Media m = provider.getMediaAtPosition(position);
                if (m != null) {
                    // Enable default set of actions, then re-enable different set of actions only if it is a failed contrib
                    menu.findItem(R.id.menu_browser_current_image).setEnabled(true).setVisible(true);
                    menu.findItem(R.id.menu_share_current_image).setEnabled(true).setVisible(true);
                    menu.findItem(R.id.menu_download_current_image).setEnabled(true).setVisible(true);
                    menu.findItem(R.id.menu_bookmark_current_image).setEnabled(true).setVisible(true);
                    menu.findItem(R.id.menu_set_as_wallpaper).setEnabled(true).setVisible(true);

                    // Initialize bookmark object
                    bookmark = new Bookmark(
                            m.getFilename(),
                            m.getAuthor(),
                            BookmarkPicturesContentProvider.uriForName(m.getFilename())
                    );
                    updateBookmarkState(menu.findItem(R.id.menu_bookmark_current_image));
                    final Integer contributionState = provider.getContributionStateAt(position);
                    if (contributionState != null) {
                        switch (contributionState) {
                            case Contribution.STATE_FAILED:
                            case Contribution.STATE_IN_PROGRESS:
                            case Contribution.STATE_QUEUED:
                                menu.findItem(R.id.menu_browser_current_image).setEnabled(false)
                                        .setVisible(false);
                                menu.findItem(R.id.menu_share_current_image).setEnabled(false)
                                        .setVisible(false);
                                menu.findItem(R.id.menu_download_current_image).setEnabled(false)
                                        .setVisible(false);
                                menu.findItem(R.id.menu_bookmark_current_image).setEnabled(false)
                                        .setVisible(false);
                                menu.findItem(R.id.menu_set_as_wallpaper).setEnabled(false)
                                        .setVisible(false);
                                break;
                            case Contribution.STATE_COMPLETED:
                                // Default set of menu items works fine. Treat same as regular media object
                                break;
                        }
                    }
                } else {
                    menu.findItem(R.id.menu_browser_current_image).setEnabled(false)
                            .setVisible(false);
                    menu.findItem(R.id.menu_share_current_image).setEnabled(false)
                            .setVisible(false);
                    menu.findItem(R.id.menu_download_current_image).setEnabled(false)
                            .setVisible(false);
                    menu.findItem(R.id.menu_bookmark_current_image).setEnabled(false)
                            .setVisible(false);
                    menu.findItem(R.id.menu_set_as_wallpaper).setEnabled(false)
                            .setVisible(false);
                }

                if (!sessionManager.isUserLoggedIn()) {
                    menu.findItem(R.id.menu_set_as_avatar).setVisible(false);
                }

            }
        }
    }

    private void updateBookmarkState(MenuItem item) {
        boolean isBookmarked = bookmarkDao.findBookmark(bookmark);
        if(isBookmarked) {
            if(removedItems.contains(pager.getCurrentItem())) {
                removedItems.remove(new Integer(pager.getCurrentItem()));
            }
        }
        else {
            if(!removedItems.contains(pager.getCurrentItem())) {
                removedItems.add(pager.getCurrentItem());
            }
        }
        int icon = isBookmarked ? R.drawable.menu_ic_round_star_filled_24px : R.drawable.menu_ic_round_star_border_24px;
        item.setIcon(icon);
    }

    public void showImage(int i, boolean isWikipediaButtonDisplayed) {
        this.isWikipediaButtonDisplayed = isWikipediaButtonDisplayed;
        setViewPagerCurrentItem(i);
    }

    public void showImage(int i) {
        setViewPagerCurrentItem(i);
    }

    /**
     * This function waits for the item to load then sets the item to current item
     * @param position current item that to be shown
     */
    private void setViewPagerCurrentItem(int position) {
        final Boolean[] currentItemNotShown = {true};
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while(currentItemNotShown[0]){
                    if(adapter.getCount() > position){
                        pager.setCurrentItem(position, false);
                        currentItemNotShown[0] = false;
                    }
                }
            }
        };
        new Thread(runnable).start();
    }

    /**
     * The method notify the viewpager that number of items have changed.
     */
    public void notifyDataSetChanged(){
        if (null != adapter) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {
        if(getActivity() == null) {
            Timber.d("Returning as activity is destroyed!");
            return;
        }

        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onPageSelected(int i) {
    }

    @Override
    public void onPageScrollStateChanged(int i) {
    }

    public void onDataSetChanged() {
        if (null != adapter) {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Called after the media is nominated for deletion
     *
     * @param index item position that has been nominated
     */
    @Override
    public void nominatingForDeletion(int index) {
      provider.refreshNominatedMedia(index);
    }
  
    /**
     * backButtonClicked is called on a back event in the media details pager.
     * returns true after closing the categoryEditContainer if open, implying that event was handled.
     * else returns false
     * @return
     */
    public boolean backButtonClicked(){
        return ((MediaDetailFragment)(adapter.getCurrentFragment())).hideCategoryEditContainerIfOpen();
    }

    public interface MediaDetailProvider {
        Media getMediaAtPosition(int i);

        int getTotalMediaCount();

        Integer getContributionStateAt(int position);

        // Reload media detail fragment once media is nominated
        void refreshNominatedMedia(int index);
    }

    //FragmentStatePagerAdapter allows user to swipe across collection of images (no. of images undetermined)
    private class MediaDetailAdapter extends FragmentStatePagerAdapter {

        /**
         * Keeps track of the current displayed fragment.
         */
        private Fragment mCurrentFragment;

        public MediaDetailAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            if (i == 0) {
                // See bug https://code.google.com/p/android/issues/detail?id=27526
                if(getActivity() == null) {
                    Timber.d("Skipping getItem. Returning as activity is destroyed!");
                    return null;
                }
                pager.postDelayed(() -> getActivity().invalidateOptionsMenu(), 5);
            }
            if (isFromFeaturedRootFragment) {
                return MediaDetailFragment.forMedia(position+i, editable, isFeaturedImage, isWikipediaButtonDisplayed);
            } else {
                return MediaDetailFragment.forMedia(i, editable, isFeaturedImage, isWikipediaButtonDisplayed);
            }
        }

        @Override
        public int getCount() {
            if (getActivity() == null) {
                Timber.d("Skipping getCount. Returning as activity is destroyed!");
                return 0;
            }
            return provider.getTotalMediaCount();
        }

        /**
         * Get the currently displayed fragment.
         * @return
         */
        public Fragment getCurrentFragment() {
            return mCurrentFragment;
        }

        /**
         * Called to inform the adapter of which item is currently considered to be the "primary",
         * that is the one show to the user as the current page.
         * @param container
         * @param position
         * @param object
         */
        @Override
        public void setPrimaryItem(@NonNull final ViewGroup container, final int position,
            @NonNull final Object object) {
            // Update the current fragment if changed
            if(getCurrentFragment() != object) {
                mCurrentFragment = ((Fragment)object);
            }
            super.setPrimaryItem(container, position, object);
        }
    }
}
