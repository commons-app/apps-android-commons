package org.wikimedia.commons.media;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;
import org.wikimedia.commons.Media;
import org.wikimedia.commons.R;

public class MediaDetailPagerFragment extends SherlockFragment {
    private ViewPager pager;
    private ShareActionProvider shareActionProvider;
    private final Boolean editable;

    public interface MediaDetailProvider {
        public Media getMediaAtPosition(int i);
        public int getTotalMediaCount();
        public void notifyDatasetChanged();
    }
    private class MediaDetailAdapter extends FragmentStatePagerAdapter {

        public MediaDetailAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            return MediaDetailFragment.forMedia(i, editable);
        }

        @Override
        public int getCount() {
            return ((MediaDetailProvider)getActivity()).getTotalMediaCount();
        }
    }

    public MediaDetailPagerFragment() {
        this(false);
    }

    public MediaDetailPagerFragment(Boolean editable) {
        this.editable = editable;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_media_detail_pager, container, false);
        pager = (ViewPager) view.findViewById(R.id.mediaDetailsPager);
        pager.setAdapter(new MediaDetailAdapter(getChildFragmentManager()));
        if(savedInstanceState != null) {
            final int pageNumber = savedInstanceState.getInt("current-page");
            // Adapter doesn't seem to be loading immediately.
            // Dear God, please forgive us for our sins
            view.postDelayed(new Runnable() {
                public void run() {
                    pager.setCurrentItem(pageNumber, false);
                }
            }, 100);
        }
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("current-page", pager.getCurrentItem());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MediaDetailProvider provider = (MediaDetailProvider)getSherlockActivity();
        Media m = provider.getMediaAtPosition(pager.getCurrentItem());
        switch(item.getItemId()) {
            case R.id.menu_share_current_image:
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear(); // see http://stackoverflow.com/a/8495697/17865
        inflater.inflate(R.menu.fragment_image_detail, menu);
    }

    public void showImage(int i) {
        pager.setCurrentItem(i);
    }
}