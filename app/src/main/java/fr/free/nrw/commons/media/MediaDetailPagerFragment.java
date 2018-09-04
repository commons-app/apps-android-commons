package fr.free.nrw.commons.media;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.category.CategoryDetailsActivity;
import fr.free.nrw.commons.category.CategoryImagesActivity;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.contributions.ContributionsActivity;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.SearchActivity;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.utils.ImageUtils;
import timber.log.Timber;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.Context.DOWNLOAD_SERVICE;
import static android.content.Intent.ACTION_VIEW;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.widget.Toast.LENGTH_SHORT;

public class MediaDetailPagerFragment extends CommonsDaggerSupportFragment implements ViewPager.OnPageChangeListener {

    @Inject
    MediaWikiApi mwApi;
    @Inject
    SessionManager sessionManager;
    @Inject
    @Named("default_preferences")
    SharedPreferences prefs;

    @BindView(R.id.mediaDetailsPager)
    ViewPager pager;
    private Boolean editable;
    private boolean isFeaturedImage;
    MediaDetailAdapter adapter;

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

                if(getActivity() == null) {
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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(getActivity() == null) {
            Timber.d("Returning as activity is destroyed!");
            return true;
        }
        MediaDetailProvider provider = (MediaDetailProvider) getActivity();
        Media m = provider.getMediaAtPosition(pager.getCurrentItem());
        switch (item.getItemId()) {
            case R.id.menu_share_current_image:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, m.getDisplayTitle() + " \n" + m.getFilePageTitle().getCanonicalUri());
                startActivity(Intent.createChooser(shareIntent, "Share image via..."));
                return true;
            case R.id.menu_browser_current_image:
                // View in browser
                Intent viewIntent = new Intent();
                viewIntent.setAction(ACTION_VIEW);
                viewIntent.setData(m.getFilePageTitle().getMobileUri());
                //check if web browser available
                if(viewIntent.resolveActivity(getActivity().getPackageManager()) != null){
                    startActivity(viewIntent);
                } else {
                    Toast toast = Toast.makeText(getContext(), getString(R.string.no_web_browser), LENGTH_SHORT);
                    toast.show();
                }

                return true;
            case R.id.menu_download_current_image:
                // Download
                downloadMedia(m);
                return true;
            case R.id.menu_set_as_wallpaper:
                // Set wallpaper
                setWallpaper(m);
                return true;
            case R.id.menu_retry_current_image:
                // Retry
                ((ContributionsActivity) getActivity()).retryUpload(pager.getCurrentItem());
                getActivity().getSupportFragmentManager().popBackStack();
                return true;
            case R.id.menu_cancel_current_image:
                // todo: delete image
                ((ContributionsActivity) getActivity()).deleteUpload(pager.getCurrentItem());
                getActivity().getSupportFragmentManager().popBackStack();
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
        if(media.getImageUrl() == null || media.getImageUrl().isEmpty()) {
            Timber.d("Media URL not present");
            return;
        }
        ImageUtils.setWallpaperFromImageUrl(getActivity(), Uri.parse(media.getImageUrl()));
    }

    /**
     * Start the media file downloading to the local SD card/storage.
     * The file can then be opened in Gallery or other apps.
     *
     * @param m Media file to download
     */
    private void downloadMedia(Media m) {
        String imageUrl = m.getImageUrl(), fileName = m.getFilename();

        if (imageUrl == null
                || fileName == null
                || getContext() ==  null
                || getActivity() == null) {
            Timber.d("Skipping download media as either imageUrl %s or filename %s activity is null", imageUrl, fileName);
            return;
        }

        // Strip 'File:' from beginning of filename, we really shouldn't store it
        fileName = fileName.replaceFirst("^File:", "");

        Uri imageUri = Uri.parse(imageUrl);

        DownloadManager.Request req = new DownloadManager.Request(imageUri);
        //These are not the image title and description fields, they are download descs for notifications
        req.setDescription(getString(R.string.app_name));
        req.setTitle(m.getDisplayTitle());
        req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        // Modern Android updates the gallery automatically. Yay!
        req.allowScanningByMediaScanner();
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(getContext(), READ_EXTERNAL_STORAGE)
                        != PERMISSION_GRANTED
                && getView() != null) {
            Snackbar.make(getView(), R.string.read_storage_permission_rationale,
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok,
                    view -> ActivityCompat.requestPermissions(getActivity(),
                            new String[]{READ_EXTERNAL_STORAGE}, 1)).show();
        } else {
            DownloadManager systemService = (DownloadManager) getActivity().getSystemService(DOWNLOAD_SERVICE);
            if (systemService != null) {
                systemService.enqueue(req);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!editable) { // Disable menu options for editable views
            menu.clear(); // see http://stackoverflow.com/a/8495697/17865
            inflater.inflate(R.menu.fragment_image_detail, menu);
            if (pager != null) {
                MediaDetailProvider provider = (MediaDetailProvider) getActivity();
                if(provider == null) {
                    return;
                }

                Media m = provider.getMediaAtPosition(pager.getCurrentItem());
                if (m != null) {
                    // Enable default set of actions, then re-enable different set of actions only if it is a failed contrib
                    menu.findItem(R.id.menu_retry_current_image).setEnabled(false).setVisible(false);
                    menu.findItem(R.id.menu_cancel_current_image).setEnabled(false).setVisible(false);
                    menu.findItem(R.id.menu_browser_current_image).setEnabled(true).setVisible(true);
                    menu.findItem(R.id.menu_share_current_image).setEnabled(true).setVisible(true);
                    menu.findItem(R.id.menu_download_current_image).setEnabled(true).setVisible(true);

                    if (m instanceof Contribution ) {
                        Contribution c = (Contribution) m;
                        switch (c.getState()) {
                            case Contribution.STATE_FAILED:
                                menu.findItem(R.id.menu_retry_current_image).setEnabled(true).setVisible(true);
                                menu.findItem(R.id.menu_cancel_current_image).setEnabled(true).setVisible(true);
                                menu.findItem(R.id.menu_browser_current_image).setEnabled(false).setVisible(false);
                                menu.findItem(R.id.menu_share_current_image).setEnabled(false).setVisible(false);
                                menu.findItem(R.id.menu_download_current_image).setEnabled(false).setVisible(false);
                                break;
                            case Contribution.STATE_IN_PROGRESS:
                            case Contribution.STATE_QUEUED:
                                menu.findItem(R.id.menu_retry_current_image).setEnabled(false).setVisible(false);
                                menu.findItem(R.id.menu_cancel_current_image).setEnabled(false).setVisible(false);
                                menu.findItem(R.id.menu_browser_current_image).setEnabled(false).setVisible(false);
                                menu.findItem(R.id.menu_share_current_image).setEnabled(false).setVisible(false);
                                menu.findItem(R.id.menu_download_current_image).setEnabled(false).setVisible(false);
                                break;
                            case Contribution.STATE_COMPLETED:
                                // Default set of menu items works fine. Treat same as regular media object
                                break;
                        }
                    }
                }
            }
        }
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
        if (i+1 >= adapter.getCount()){
            try{
                ((CategoryImagesActivity) getContext()).requestMoreImages();
            }catch (Exception e){
                e.printStackTrace();
            }
            try{
                ((CategoryDetailsActivity) getContext()).requestMoreImages();
            }catch (Exception e){
                e.printStackTrace();
            }
            try{
                ((SearchActivity) getContext()).requestMoreImages();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onPageSelected(int i) {
    }

    @Override
    public void onPageScrollStateChanged(int i) {
    }

    public interface MediaDetailProvider {
        Media getMediaAtPosition(int i);

        int getTotalMediaCount();

        void notifyDatasetChanged();

        void registerDataSetObserver(DataSetObserver observer);

        void unregisterDataSetObserver(DataSetObserver observer);
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
                pager.postDelayed(() -> getActivity().supportInvalidateOptionsMenu(), 5);
            }
            return MediaDetailFragment.forMedia(i, editable, isFeaturedImage);
        }

        @Override
        public int getCount() {
            if(getActivity() == null) {
                Timber.d("Skipping getCount. Returning as activity is destroyed!");
                return 0;
            }
            return ((MediaDetailProvider) getActivity()).getTotalMediaCount();
        }
    }
}