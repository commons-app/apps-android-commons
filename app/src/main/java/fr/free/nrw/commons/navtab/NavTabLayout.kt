package fr.free.nrw.commons.navtab

import android.content.Context
import android.util.AttributeSet
import android.view.Menu

import com.google.android.material.bottomnavigation.BottomNavigationView
import fr.free.nrw.commons.contributions.MainActivity


class NavTabLayout : BottomNavigationView {

    constructor(context: Context) : super(context) {
        setTabViews()
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        setTabViews()
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        setTabViews()
    }

    private fun setTabViews() {
        val isLoginSkipped = (context as MainActivity)
            .applicationKvStore?.getBoolean("login_skipped")
        if (isLoginSkipped == true) {
            for (i in 0 until NavTabLoggedOut.size()) {
                val navTab = NavTabLoggedOut.of(i)
                menu.add(Menu.NONE, i, i, navTab.text()).setIcon(navTab.icon())
            }
        } else {
            for (i in 0 until NavTab.size()) {
                val navTab = NavTab.of(i)
                menu.add(Menu.NONE, i, i, navTab.text()).setIcon(navTab.icon())
            }
        }
    }
}