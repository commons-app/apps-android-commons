package org.wikimedia.commons.media;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;
import org.wikimedia.commons.Media;
import org.wikimedia.commons.R;
import org.wikimedia.commons.Utils;

public class MediaDetailPagerFragment extends SherlockFragment {
    private ViewPager pager;
    private ShareActionProvider shareActionProvider;

    public interface MediaDetailProvider {
        public Media getItem(int i);
        public int getCount();
    }
    private class MediaDetailAdapter extends FragmentStatePagerAdapter {

        public MediaDetailAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Media m = ((MediaDetailProvider)getActivity()).getItem(i);
            return MediaDetailFragment.forMedia((m));
        }

        @Override
        public int getCount() {
            return ((MediaDetailProvider)getActivity()).getCount();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_media_detail_pager, container, false);
        pager = (ViewPager) view.findViewById(R.id.mediaDetailsPager);
        pager.setAdapter(new MediaDetailAdapter(getChildFragmentManager()));
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_share_current_image:
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                MediaDetailProvider provider = (MediaDetailProvider)getSherlockActivity();
                Media m = provider.getItem(pager.getCurrentItem());
                shareIntent.putExtra(Intent.EXTRA_TEXT, m.getDisplayTitle() + " " + m.getDescriptionUrl());
                startActivity(shareIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_image_detail, menu);
    }

    public void showImage(int i) {
        pager.setCurrentItem(i);

    }
}