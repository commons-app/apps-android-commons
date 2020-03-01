package fr.free.nrw.commons.review;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

public class ReviewPagerAdapter extends FragmentStatePagerAdapter {
    private ReviewImageFragment[] reviewImageFragments;


    ReviewPagerAdapter(FragmentManager fm) {
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

    void updateFileInformation() {
        for (int i = 0; i < getCount(); i++) {
            ReviewImageFragment fragment = reviewImageFragments[i];
            fragment.update(i);
        }
    }

    /**
     * This function is called when an image has
     * been loaded to enable the review buttons.
     */
    public void enableButtons() {
        if (reviewImageFragments != null){
            reviewImageFragments[0].enableButtons();
        }
    }

    /**
     * This function is called when an image is being loaded
     * to disable the review buttons
     */
    public void disableButtons() {
        if (reviewImageFragments != null){
            reviewImageFragments[0].disableButtons();
        }
    }

    @Override
    public Fragment getItem(int position) {
        Bundle bundle = new Bundle();
        bundle.putInt("position", position);
        reviewImageFragments[position].setArguments(bundle);
        return reviewImageFragments[position];
    }

}
