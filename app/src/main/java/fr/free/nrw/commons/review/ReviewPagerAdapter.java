package fr.free.nrw.commons.review;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by nes on 19.05.2018.
 */

public class ReviewPagerAdapter extends FragmentStatePagerAdapter {
    private int currentPosition;
    ReviewOutOfContextFragment reviewOutOfContextFragment;


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
                currentPosition = 0;
                return ReviewOutOfContextFragment.init(position, ReviewController.fileName);
            case 1: // Fragment # 1 - This will show image
                currentPosition = 1;
                return ReviewLicenceViolationFragment.init(position, ReviewController.fileName);
            default:// Fragment # 2-9 - Will show list
                currentPosition = 2;
                return ReviewCategoryMissuseFragment.init(position, ReviewController.fileName);
        }
    }

}
