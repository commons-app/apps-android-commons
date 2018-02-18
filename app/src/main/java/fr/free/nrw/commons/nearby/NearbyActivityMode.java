package fr.free.nrw.commons.nearby;

import android.support.annotation.DrawableRes;

import fr.free.nrw.commons.R;

enum NearbyActivityMode {
    MAP(R.drawable.ic_list_white_24dp),
    LIST(R.drawable.ic_map_white_24dp);

    @DrawableRes
    private final int icon;

    NearbyActivityMode(int icon) {
        this.icon = icon;
    }

    @DrawableRes
    public int getIcon() {
        return icon;
    }

    public NearbyActivityMode toggle() {
        return isMap() ? LIST : MAP;
    }

    public boolean isMap() {
        return MAP.equals(this);
    }
}