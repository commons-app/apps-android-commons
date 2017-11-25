package fr.free.nrw.commons.utils;

import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import timber.log.Timber;

public class FragmentUtils {

    public static boolean addAndCommitFragmentWithImmediateExecution(
            @NonNull FragmentManager fragmentManager,
            @IdRes int containerViewId,
            @NonNull Fragment fragment) {
        if (fragment.isAdded()) {
            Timber.w("Could not add fragment. The fragment is already added.");
            return false;
        }
        try {
            fragmentManager.beginTransaction()
                    .add(containerViewId, fragment)
                    .commitNow();
            return true;
        } catch (IllegalStateException e) {
            Timber.e(e, "Could not add & commit fragment. "
                    + "Did you mean to call commitAllowingStateLoss?");
        }
        return false;
    }
}
