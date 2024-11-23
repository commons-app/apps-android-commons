package fr.free.nrw.commons.utils

import androidx.fragment.app.Fragment

object FragmentUtils {

    /**
     * Utility function to check whether the fragment UI is still active or not
     * @param fragment
     * @return Boolean
     */
    @JvmStatic
    fun isFragmentUIActive(fragment: Fragment?): Boolean {
        return fragment != null &&
                fragment.activity != null &&
                fragment.isAdded &&
                !fragment.isDetached &&
                !fragment.isRemoving
    }
}
