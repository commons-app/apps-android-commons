package fr.free.nrw.commons.utils;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class NearbyFABUtils {

  /*
   * Add anchors back before making them visible again.
   * */
  public static void addAnchorToBigFABs(FloatingActionButton floatingActionButton, int anchorID) {
    CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams
        (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.setAnchorId(anchorID);
    params.anchorGravity = Gravity.TOP | Gravity.RIGHT | Gravity.END;
    floatingActionButton.setLayoutParams(params);
  }

  /*
   * Add anchors back before making them visible again. Big and small fabs have different anchor
   * gravities, therefore the are two methods.
   * */
  public static void addAnchorToSmallFABs(FloatingActionButton floatingActionButton, int anchorID) {
    CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams
        (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.setAnchorId(anchorID);
    params.anchorGravity = Gravity.CENTER_HORIZONTAL;
    floatingActionButton.setLayoutParams(params);
  }

  /*
   * We are not able to hide FABs without removing anchors, this method removes anchors
   * */
  public static void removeAnchorFromFAB(FloatingActionButton floatingActionButton) {
    //get rid of anchors
    //Somehow this was the only way https://stackoverflow.com/questions/32732932
    // /floatingactionbutton-visible-for-sometime-even-if-visibility-is-set-to-gone
    CoordinatorLayout.LayoutParams param = (CoordinatorLayout.LayoutParams) floatingActionButton
        .getLayoutParams();
    param.setAnchorId(View.NO_ID);
    // If we don't set them to zero, then they become visible for a moment on upper left side
    param.width = 0;
    param.height = 0;
    floatingActionButton.setLayoutParams(param);
  }

}
