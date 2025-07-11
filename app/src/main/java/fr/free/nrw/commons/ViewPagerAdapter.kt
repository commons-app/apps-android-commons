package fr.free.nrw.commons

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import java.util.Locale

/**
 * This adapter will be used to display fragments in a ViewPager
 */
class ViewPagerAdapter : FragmentPagerAdapter {
    private val context: Context
    private var fragmentList: List<Fragment> = emptyList()
    private var fragmentTitleList: List<String> = emptyList()

    constructor(context: Context, manager: FragmentManager) : super(manager) {
        this.context = context
    }

    constructor(context: Context, manager: FragmentManager, behavior: Int) : super(manager, behavior) {
        this.context = context
    }

    override fun getItem(position: Int): Fragment = fragmentList[position]

    override fun getPageTitle(position: Int): CharSequence = fragmentTitleList[position]

    override fun getCount(): Int = fragmentList.size

    fun setTabs(vararg titlesToFragments: Pair<Int, Fragment>) {
        // Enforce that every title must come from strings.xml and all will consistently be uppercase
        fragmentTitleList = titlesToFragments.map {
            context.getString(it.first).uppercase(Locale.ROOT)
        }
        fragmentList = titlesToFragments.map { it.second }
    }

    companion object {
        // Convenience method for Java callers, can be removed when everything is migrated
        @JvmStatic
        fun pairOf(first: Int, second: Fragment) = first to second
    }
}
