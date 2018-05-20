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
    ReviewImageFragment[] reviewImageFragments;


    public ReviewPagerAdapter(FragmentManager fm) {
        super(fm);
        reviewImageFragments = new ReviewImageFragment[] {
            new ReviewImageFragment(),
            new ReviewImageFragment(),
            new ReviewImageFragment(),
            new ReviewImageFragment()
        };
    }

    @Override
    public int getCount() {
        return 4;
    }

    public void updateFilename() {
        for (int i = 0; i < getCount(); i++) {
            ReviewImageFragment fragment = reviewImageFragments[i];
            fragment.update(i, ReviewController.fileName);
        }
    }

    public void updateCategories() {
        ReviewImageFragment categoryFragment = reviewImageFragments[ReviewImageFragment.CATEGORY];
        categoryFragment.updateCategories(ReviewController.categories);
    }

    @Override
    public Fragment getItem(int position) {
        Bundle bundle = new Bundle();
        bundle.putInt("position", position);
        reviewImageFragments[position].setArguments(bundle);
        return reviewImageFragments[position];
    }

}
