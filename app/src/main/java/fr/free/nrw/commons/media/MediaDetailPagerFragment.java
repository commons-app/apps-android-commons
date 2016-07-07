package fr.free.nrw.commons.media;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.EventLog;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.contributions.ContributionsActivity;

public class MediaDetailPagerFragment extends Fragment implements ViewPager.OnPageChangeListener {
    private ViewPager pager;
    private Boolean editable;
    private CommonsApplication app;

    public MediaDetailPagerFragment() {
        this(false);
    }

    @SuppressLint("ValidFragment")
    public MediaDetailPagerFragment(Boolean editable) {
        this.editable = editable;
    }

    public interface MediaDetailProvider {
        public Media getMediaAtPosition(int i);
        public int getTotalMediaCount();
        public void notifyDatasetChanged();
        public void registerDataSetObserver(DataSetObserver observer);
        public void unregisterDataSetObserver(DataSetObserver observer);
    }

    //FragmentStatePagerAdapter allows user to swipe across collection of images (no. of images undetermined)
    private class MediaDetailAdapter extends FragmentStatePagerAdapter {

        public MediaDetailAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            if(i == 0) {
                // See bug https://code.google.com/p/android/issues/detail?id=27526
                pager.postDelayed(new Runnable() {
                    public void run() {
                        getActivity().supportInvalidateOptionsMenu();
                    }
                }, 5);
            }
            return fr.free.nrw.commons.media.MediaDetailFragment.forMedia(i, editable);
        }

        @Override
        public int getCount() {
            return ((MediaDetailProvider)getActivity()).getTotalMediaCount();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_media_detail_pager, container, false);
        pager = (ViewPager) view.findViewById(R.id.mediaDetailsPager);
        pager.setOnPageChangeListener(this);
        if(savedInstanceState != null) {
            final int pageNumber = savedInstanceState.getInt("current-page");
            // Adapter doesn't seem to be loading immediately.
            // Dear God, please forgive us for our sins
            view.postDelayed(new Runnable() {
                public void run() {
                    pager.setAdapter(new MediaDetailAdapter(getChildFragmentManager()));
                    pager.setCurrentItem(pageNumber, false);
                    getActivity().supportInvalidateOptionsMenu();
                }
            }, 100);
        } else {
            pager.setAdapter(new MediaDetailAdapter(getChildFragmentManager()));
        }
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("current-page", pager.getCurrentItem());
        outState.putBoolean("editable", editable);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            editable = savedInstanceState.getBoolean("editable");
        }
        app = (CommonsApplication)getActivity().getApplicationContext();
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MediaDetailProvider provider = (MediaDetailProvider)getActivity();
        Media m = provider.getMediaAtPosition(pager.getCurrentItem());
        switch(item.getItemId()) {
            case R.id.menu_share_current_image:
                EventLog.schema(CommonsApplication.EVENT_SHARE_ATTEMPT)
                        .param("username", app.getCurrentAccount().name)
                        .param("filename", m.getFilename())
                        .log();
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, m.getDisplayTitle() + " " + m.getDescriptionUrl());
                startActivity(shareIntent);
                return true;
            case R.id.menu_browser_current_image:
                Intent viewIntent = new Intent();
                viewIntent.setAction(Intent.ACTION_VIEW);
                viewIntent.setData(Uri.parse(m.getDescriptionUrl()));
                startActivity(viewIntent);
                return true;
            case R.id.menu_download_current_image:
                downloadMedia(m);
                return true;
            case R.id.menu_retry_current_image:
                // Is this... sane? :)
                ((ContributionsActivity)getActivity()).retryUpload(pager.getCurrentItem());
                getActivity().getSupportFragmentManager().popBackStack();
                return true;
            case R.id.menu_cancel_current_image:
                // todo: delete image
                ((ContributionsActivity)getActivity()).deleteUpload(pager.getCurrentItem());
                getActivity().getSupportFragmentManager().popBackStack();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Start the media file downloading to the local SD card/storage.
     * The file can then be opened in Gallery or other apps.
     *
     * @param m
     */
    private void downloadMedia(Media m) {
        String imageUrl = m.getImageUrl(),
               fileName = m.getFilename();
        // Strip 'File:' from beginning of filename, we really shouldn't store it
        fileName = fileName.replaceFirst("^File:", "");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // Gingerbread DownloadManager has no HTTPS support...
            // Download file over HTTP, there'll be no credentials
            // sent so it should be safe-ish.
            imageUrl = imageUrl.replaceFirst("^https://", "http://");
        }
        Uri imageUri = Uri.parse(imageUrl);

        DownloadManager.Request req = new DownloadManager.Request(imageUri);
        //These are not the image title and description fields, they are download descs for notifications
        req.setDescription(getString(R.string.app_name));
        req.setTitle(m.getDisplayTitle());
        req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Modern Android updates the gallery automatically. Yay!
            req.allowScanningByMediaScanner();

            // On HC/ICS/JB we can leave the download notification up when complete.
            // This allows folks to open the file directly in gallery viewer.
            // But for some reason it fails on Honeycomb (Google TV). Sigh.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            }
        }

        final DownloadManager manager = (DownloadManager)getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        final long downloadId = manager.enqueue(req);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // For Gingerbread compatibility...
            BroadcastReceiver onComplete = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // Check if the download has completed...
                    Cursor c = manager.query(new DownloadManager.Query()
                            .setFilterById(downloadId)
                            .setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL | DownloadManager.STATUS_FAILED)
                    );
                    if (c.moveToFirst()) {
                        int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        Log.d("Commons", "Download completed with status " + status);
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            // Force Gallery to index the new file
                            Uri mediaUri = Uri.parse("file://" + Environment.getExternalStorageDirectory());
                            getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, mediaUri));

                            // todo: show a persistent notification?
                        }
                    } else {
                        Log.d("Commons", "Couldn't get download status for some reason");
                    }
                    getActivity().unregisterReceiver(this);
                }
            };
            getActivity().registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(!editable) { // Disable menu options for editable views
            menu.clear(); // see http://stackoverflow.com/a/8495697/17865
            inflater.inflate(R.menu.fragment_image_detail, menu);
            if(pager != null) {
                MediaDetailProvider provider = (MediaDetailProvider)getActivity();
                Media m = provider.getMediaAtPosition(pager.getCurrentItem());
                if(m != null) {
                    // Enable default set of actions, then re-enable different set of actions only if it is a failed contrib
                    menu.findItem(R.id.menu_retry_current_image).setEnabled(false).setVisible(false);
                    menu.findItem(R.id.menu_cancel_current_image).setEnabled(false).setVisible(false);
                    menu.findItem(R.id.menu_browser_current_image).setEnabled(true).setVisible(true);
                    menu.findItem(R.id.menu_share_current_image).setEnabled(true).setVisible(true);
                    menu.findItem(R.id.menu_download_current_image).setEnabled(true).setVisible(true);

                    if(m instanceof Contribution) {
                        Contribution c = (Contribution)m;
                        switch(c.getState()) {
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
                    return;
                }
            }
        }
    }

    public void showImage(int i) {
        pager.setCurrentItem(i);
    }

    public void onPageScrolled(int i, float v, int i2) {
        getActivity().supportInvalidateOptionsMenu();
    }

    public void onPageSelected(int i) {
    }

    public void onPageScrollStateChanged(int i) {

    }
}