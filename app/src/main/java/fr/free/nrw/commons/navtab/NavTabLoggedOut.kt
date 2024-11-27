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
    @DrawableRes private val icon: Int
) : EnumCode {

    EXPLORE(R.string.navigation_item_explore, R.drawable.ic_globe) {
        override fun newInstance(): Fragment {
            return ExploreFragment.newInstance()
        }
    },
    BOOKMARKS(R.string.bookmarks, R.drawable.ic_round_star_border_24px) {
        override fun newInstance(): Fragment {
            return BookmarkFragment.newInstance()
        }
    },
    MORE(R.string.more, R.drawable.ic_menu_black_24dp) {
        override fun newInstance(): Fragment? {
            return null
        }
    };

    companion object {
        private val MAP: EnumCodeMap<NavTabLoggedOut> = EnumCodeMap(NavTabLoggedOut::class.java)

        @JvmStatic
        fun of(code: Int): NavTabLoggedOut {
            return MAP[code]
        }

        @JvmStatic
        fun size(): Int {
            return MAP.size()
        }
    }

    @StringRes
    fun text(): Int {
        return text
    }

    @DrawableRes
    fun icon(): Int {
        return icon
    }

    abstract fun newInstance(): Fragment?

    override fun code(): Int {
        // This enumeration is not marshalled so tying declaration order to presentation order is
        // convenient and consistent.
        return ordinal
    }
}