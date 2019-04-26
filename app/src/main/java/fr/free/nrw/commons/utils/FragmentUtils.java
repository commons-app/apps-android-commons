package fr.free.nrw.commons.utils;

import androidx.fragment.app.Fragment;

public class FragmentUtils {

    /**
     * Utility function to check whether the fragment UI is still active or not
     * @param fragment
     * @return
     */
    public static boolean isFragmentUIActive(Fragment fragment) {
        return fragment!=null && fragment.getActivity() != null && fragment.isAdded() && !fragment.isDetached() && !fragment.isRemoving();
    }
}
