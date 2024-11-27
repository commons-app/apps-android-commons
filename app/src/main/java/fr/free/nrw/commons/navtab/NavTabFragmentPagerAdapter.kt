package fr.free.nrw.commons.navtab

import android.view.ViewGroup

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter


class NavTabFragmentPagerAdapter(
    mgr: FragmentManager
) : FragmentPagerAdapter(mgr, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private var currentFragment: Fragment? = null

    fun getCurrentFragment(): Fragment? {
        return currentFragment
    }

    override fun getItem(pos: Int): Fragment {
        return NavTab.of(pos).newInstance()!!
    }

    override fun getCount(): Int {
        return NavTab.size()
    }

    override fun setPrimaryItem(
        container: ViewGroup,
        position: Int,
        `object`: Any
    ) {
        currentFragment = `object` as Fragment
        super.setPrimaryItem(container, position, `object`)
    }
}