package fr.free.nrw.commons.review;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by nes on 19.05.2018.
 */

public class ReviewPagerAdapter extends FragmentStatePagerAdapter {

    public ReviewPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0: // Fragment # 0 - This will show image
                return ReviewOutOfContextFragment.init(position);
            case 1: // Fragment # 1 - This will show image
                return ReviewOutOfContextFragment.init(position);
            default:// Fragment # 2-9 - Will show list
                return ReviewOutOfContextFragment.init(position);
        }
    }
}
