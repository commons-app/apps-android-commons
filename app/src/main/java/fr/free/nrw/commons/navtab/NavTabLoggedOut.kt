package fr.free.nrw.commons.navtab

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import fr.free.nrw.commons.R
import fr.free.nrw.commons.bookmarks.BookmarkFragment
import fr.free.nrw.commons.explore.ExploreFragment
import fr.free.nrw.commons.wikidata.model.EnumCode
import fr.free.nrw.commons.wikidata.model.EnumCodeMap

enum class NavTabLoggedOut(
    @StringRes private val text: Int,
    @DrawableRes private val icon: Int,
) : EnumCode {
    EXPLORE(R.string.navigation_item_explore, R.drawable.ic_globe) {
        override fun newInstance(): Fragment = ExploreFragment.newInstance()
    },
    BOOKMARKS(R.string.bookmarks, R.drawable.ic_round_star_border_24px) {
        override fun newInstance(): Fragment = BookmarkFragment.newInstance()
    },
    MORE(R.string.more, R.drawable.ic_menu_black_24dp) {
        override fun newInstance(): Fragment? = null
    }, ;

    companion object {
        private val MAP: EnumCodeMap<NavTabLoggedOut> = EnumCodeMap(NavTabLoggedOut::class.java)

        @JvmStatic
        fun of(code: Int): NavTabLoggedOut = MAP[code]

        @JvmStatic
        fun size(): Int = MAP.size()
    }

    @StringRes
    fun text(): Int = text

    @DrawableRes
    fun icon(): Int = icon

    abstract fun newInstance(): Fragment?

    override fun code(): Int {
        // This enumeration is not marshalled so tying declaration order to presentation order is
        // convenient and consistent.
        return ordinal
    }
}
