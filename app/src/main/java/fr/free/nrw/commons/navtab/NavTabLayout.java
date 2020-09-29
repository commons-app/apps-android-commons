package fr.free.nrw.commons.navtab;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Menu;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import fr.free.nrw.commons.contributions.MainActivity;


public class NavTabLayout extends BottomNavigationView {

  public NavTabLayout(Context context) {
    super(context);
    setTabViews();
  }

  public NavTabLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    setTabViews();
  }

  public NavTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setTabViews();
  }

  private void setTabViews() {
    if (((MainActivity)getContext()).applicationKvStore.getBoolean("login_skipped") == true) {
      for (int i = 0; i < NavTabLoggedOut.size(); i++) {
        NavTabLoggedOut navTab = NavTabLoggedOut.of(i);
        getMenu().add(Menu.NONE, i, i, navTab.text()).setIcon(navTab.icon());
      }
    } else {
      for (int i = 0; i < NavTab.size(); i++) {
        NavTab navTab = NavTab.of(i);
        getMenu().add(Menu.NONE, i, i, navTab.text()).setIcon(navTab.icon());
      }
    }
  }
}
