package fr.free.nrw.commons;


import androidx.appcompat.app.ActionBar;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(ActionBar.class)
public class ShadowActionBar {

  private boolean showHomeAsUp;

  public boolean getShowHomeAsUp() {
    return showHomeAsUp;
  }

  @Implementation
  void setDisplayHomeAsUpEnabled(final boolean showHomeAsUp) {
    this.showHomeAsUp = showHomeAsUp;
  }
}
