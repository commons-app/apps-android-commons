package fr.free.nrw.commons.review;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

public class ReviewPagerAdapter extends FragmentStatePagerAdapter {
    ReviewImageFragment[] reviewImageFragments;


    public ReviewPagerAdapter(FragmentManager fm) {
        super(fm);
        reviewImageFragments = new ReviewImageFragment[]{
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

    public void updateFileInformation(String fileName) {
        for (int i = 0; i < getCount(); i++) {
            ReviewImageFragment fragment = reviewImageFragments[i];
            fragment.update(i, fileName);
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
