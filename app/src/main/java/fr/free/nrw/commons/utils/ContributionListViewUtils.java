package fr.free.nrw.commons.utils;

import android.util.Log;
import android.view.View;

/**
 * This class includes utilities for contribution list fragment indicators, such as number of
 * uploads, notification and nearby cards and their progress bar behind them.
 */
public class ContributionListViewUtils {

    /**
     * Sets indicator and progress bar visibility according to 3 states, data is ready to display,
     * data still loading, both should be invisible because media details fragment is visible
     * @param indicator this can be numOfUploads text view, notification/nearby card views
     * @param progressBar this is the progress bar behind indicators, displays they are loading
     * @param isIndicatorReady is indicator fetched the information will be displayed
     * @param isBothInvisible true if contribution list fragment is not active (ie. Media Details Fragment is active)
     */
    public static void setIndicatorVisibility(View indicator, View progressBar, boolean isIndicatorReady, boolean isBothInvisible) {
        if (indicator!=null && progressBar!=null) {
            if (isIndicatorReady) {
                // Indicator ready, display them
                indicator.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            } else {
                if (isBothInvisible) {
                    //  Media Details Fragment is visible, hide both
                    indicator.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                } else {
                    //  Indicator is not ready, still loading
                    indicator.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
