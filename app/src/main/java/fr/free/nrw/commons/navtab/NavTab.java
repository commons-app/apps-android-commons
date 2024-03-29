package fr.free.nrw.commons.navtab;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import fr.free.nrw.commons.bookmarks.BookmarkFragment;
import fr.free.nrw.commons.contributions.ContributionsFragment;
import fr.free.nrw.commons.explore.ExploreFragment;
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment;
import fr.free.nrw.commons.wikidata.model.EnumCode;
import fr.free.nrw.commons.wikidata.model.EnumCodeMap;

import fr.free.nrw.commons.R;


public enum NavTab implements EnumCode {
    CONTRIBUTIONS(R.string.contributions_fragment, R.drawable.ic_baseline_person_24) {
        @NonNull
        @Override
        public Fragment newInstance() {
            return ContributionsFragment.newInstance();
        }
    },
    NEARBY(R.string.nearby_fragment, R.drawable.ic_location_on_black_24dp) {
        @NonNull
        @Override
        public Fragment newInstance() {
            return NearbyParentFragment.newInstance();
        }
    },
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

    private static final EnumCodeMap<NavTab> MAP = new EnumCodeMap<>(NavTab.class);

    @StringRes
    private final int text;
    @DrawableRes
    private final int icon;

    @NonNull
    public static NavTab of(int code) {
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

    NavTab(@StringRes int text, @DrawableRes int icon) {
        this.text = text;
        this.icon = icon;
    }
}
