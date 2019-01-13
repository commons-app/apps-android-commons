package fr.free.nrw.commons.utils;

import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import timber.log.Timber;

public class FragmentUtils {

    /**
     * Utility function to check whether the fragment UI is still active or not
     * @param fragment
     * @return
     */
    public static boolean isFragmentUIActive(Fragment fragment) {
        return fragment.getActivity() != null && fragment.isAdded() && !fragment.isDetached() && !fragment.isRemoving();
    }
}
