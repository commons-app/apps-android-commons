package fr.free.nrw.commons;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * This adapter will be used to display fragments in a ViewPager
 */
public class ViewPagerAdapter extends FragmentPagerAdapter {
    private List<Fragment> fragmentList = new ArrayList<>();
    private List<String> fragmentTitleList = new ArrayList<>();

    public ViewPagerAdapter(FragmentManager manager) {
        super(manager);
    }

    /**
     * Constructs a ViewPagerAdapter with a specified Fragment Manager and Fragment resume behavior.
     *
     * @param manager The FragmentManager
     * @param behavior An integer which represents the behavior of non visible fragments. See
     *                 FragmentPagerAdapter.java for options.
     */
    public ViewPagerAdapter(FragmentManager manager, int behavior) {
        super(manager, behavior);
    }

    /**
     * This method returns the fragment of the viewpager at a particular position
     * @param position
     */
    @Override
    public Fragment getItem(int position) {
        return fragmentList.get(position);
    }

    /**
     * This method returns the total number of fragments in the viewpager.
     * @return size
     */
    @Override
    public int getCount() {
        return fragmentList.size();
    }

    /**
     * This method sets the fragment and title list in the viewpager
     * @param fragmentList List of all fragments to be displayed in the viewpager
     * @param fragmentTitleList List of all titles of the fragments
     */
    public void setTabData(List<Fragment> fragmentList, List<String> fragmentTitleList) {
        this.fragmentList = fragmentList;
        this.fragmentTitleList = fragmentTitleList;
    }

    /**
     * This method returns the title of the page at a particular position
     * @param position
     */
    @Override
    public CharSequence getPageTitle(int position) {
        return fragmentTitleList.get(position);
    }
}
