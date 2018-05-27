package fr.free.nrw.commons.review;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import fr.free.nrw.commons.mwapi.Revision;

/**
 * Created by nes on 19.05.2018.
 */

public class ReviewPagerAdapter extends FragmentStatePagerAdapter {
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
        return reviewImageFragments.length;
    }

    public void updateFileInformation(String fileName, Revision revision) {
        for (int i = 0; i < getCount(); i++) {
            ReviewImageFragment fragment = reviewImageFragments[i];
            fragment.update(i, fileName, revision);
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
