package fr.free.nrw.commons.media;

import static fr.free.nrw.commons.Utils.handleWebUrl;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.snackbar.Snackbar;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.bookmarks.Bookmark;
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesContentProvider;
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesDao;
import fr.free.nrw.commons.category.CategoryImagesCallback;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.utils.DownloadUtils;
import fr.free.nrw.commons.utils.ImageUtils;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import javax.inject.Inject;
import timber.log.Timber;

public class MediaDetailPagerFragment extends CommonsDaggerSupportFragment implements ViewPager.OnPageChangeListener {

    @Inject BookmarkPicturesDao bookmarkDao;

    @BindView(R.id.mediaDetailsPager) ViewPager pager;
    private Boolean editable;
    private boolean isFeaturedImage;
    MediaDetailAdapter adapter;
    private Bookmark bookmark;
    private MediaDetailProvider provider;

    public MediaDetailPagerFragment() {
        this(false, false);
    }

    @SuppressLint("ValidFragment")
    public MediaDetailPagerFragment(Boolean editable, boolean isFeaturedImage) {
        this.editable = editable;
        this.isFeaturedImage = isFeaturedImage;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_media_detail_pager, container, false);
        ButterKnife.bind(this,view);
        pager.addOnPageChangeListener(this);

        adapter = new MediaDetailAdapter(getChildFragmentManager());

        if (savedInstanceState != null) {
            final int pageNumber = savedInstanceState.getInt("current-page");
            // Adapter doesn't seem to be loading immediately.
            // Dear God, please forgive us for our sins
            view.postDelayed(() -> {
                pager.setAdapter(adapter);
                pager.setCurrentItem(pageNumber, false);

                if (getActivity() == null) {
                    Timber.d("Returning as activity is destroyed!");
                    return;
                }

                getActivity().supportInvalidateOptionsMenu();
                adapter.notifyDataSetChanged();
            }, 100);
        } else {
            pager.setAdapter(adapter);
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

                Media m = provider.getMediaAtPosition(pager.getCurrentItem());
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
                            m.getCreator(),
                            BookmarkPicturesContentProvider.uriForName(m.getFilename())
                    );
                    updateBookmarkState(menu.findItem(R.id.menu_bookmark_current_image));

                    if (m instanceof Contribution) {
                        Contribution c = (Contribution) m;
                        switch (c.getState()) {
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
            }
        }
    }

    private void updateBookmarkState(MenuItem item) {
        boolean isBookmarked = bookmarkDao.findBookmark(bookmark);
        int icon = isBookmarked ? R.drawable.ic_round_star_filled_24px : R.drawable.ic_round_star_border_24px;
        item.setIcon(icon);
    }

    public void showImage(int i) {
        Handler handler =  new Handler();
        handler.postDelayed(() -> pager.setCurrentItem(i), 5);
    }

    /**
     * The method notify the viewpager that number of items have changed.
     */
    public void notifyDataSetChanged(){
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {
        if(getActivity() == null) {
            Timber.d("Returning as activity is destroyed!");
            return;
        }
        if (i+1 >= adapter.getCount() && getContext() instanceof CategoryImagesCallback)
            ((CategoryImagesCallback) getContext()).requestMoreImages();

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

    public interface MediaDetailProvider {
        Media getMediaAtPosition(int i);

        int getTotalMediaCount();
    }

    //FragmentStatePagerAdapter allows user to swipe across collection of images (no. of images undetermined)
    private class MediaDetailAdapter extends FragmentStatePagerAdapter {

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
            return MediaDetailFragment.forMedia(i, editable, isFeaturedImage);
        }

        @Override
        public int getCount() {
            if (getActivity() == null) {
                Timber.d("Skipping getCount. Returning as activity is destroyed!");
                return 0;
            }
            return provider.getTotalMediaCount();
        }
    }
}
