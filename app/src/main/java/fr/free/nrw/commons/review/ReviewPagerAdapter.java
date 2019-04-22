package fr.free.nrw.commons.review;

import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

public class ReviewPagerAdapter extends FragmentStatePagerAdapter {
    ReviewImageFragment[] reviewImageFragments;

    private int mCurrentPosition = -1;

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

    // https://stackoverflow.com/a/32410274
    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        super.setPrimaryItem(container, position, object);
        if (position != mCurrentPosition) {
            Fragment fragment = (Fragment) object;
            ReviewViewPager pager = (ReviewViewPager) container;
            if (fragment != null && fragment.getView() != null) {
                mCurrentPosition = position;
                pager.measureCurrentView(fragment.getView());
            }
        }
    }

}
