package fr.free.nrw.commons.navtab;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.bookmarks.BookmarkFragment;
import fr.free.nrw.commons.explore.ExploreFragment;
import fr.free.nrw.commons.wikidata.model.EnumCode;
import fr.free.nrw.commons.wikidata.model.EnumCodeMap;


public enum NavTabLoggedOut implements EnumCode {

    EXPLORE(R.string.navigation_item_explore, R.drawable.ic_globe) {
        @NonNull
        @Override
        public Fragment newInstance() {
            return ExploreFragment.newInstance();
        }
    },
    BOOKMARKS(R.string.bookmarks, R.drawable.ic_round_star_border_24px) {
        @NonNull
        @Override
        public Fragment newInstance() {
            return BookmarkFragment.newInstance();
        }
    },
    MORE(R.string.more, R.drawable.ic_menu_black_24dp) {
        @NonNull
        @Override
        public Fragment newInstance() {
            return null;
        }
    };

    private static final EnumCodeMap<NavTabLoggedOut> MAP = new EnumCodeMap<>(
        NavTabLoggedOut.class);

    @StringRes
    private final int text;
    @DrawableRes
    private final int icon;

    @NonNull
    public static NavTabLoggedOut of(int code) {
        return MAP.get(code);
    }

    public static int size() {
        return MAP.size();
    }

    @StringRes
    public int text() {
        return text;
    }

    @DrawableRes
    public int icon() {
        return icon;
    }

    @NonNull
    public abstract Fragment newInstance();

    @Override
    public int code() {
        // This enumeration is not marshalled so tying declaration order to presentation order is
        // convenient and consistent.
        return ordinal();
    }

    NavTabLoggedOut(@StringRes int text, @DrawableRes int icon) {
        this.text = text;
        this.icon = icon;
    }
}
