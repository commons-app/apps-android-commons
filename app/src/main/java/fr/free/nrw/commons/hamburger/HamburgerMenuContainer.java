package fr.free.nrw.commons.hamburger;

import android.support.v7.app.ActionBarDrawerToggle;

public interface HamburgerMenuContainer {
    void setDrawerListener(ActionBarDrawerToggle listener);
    void toggleDrawer();
    boolean isDrawerVisible();
}
