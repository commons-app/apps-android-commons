package fr.free.nrw.commons.upload.categories

import android.app.ProgressDialog
import android.content.Context
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.CommonsApplication.Companion.instance
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.ContributionsFragment
import fr.free.nrw.commons.media.MediaDetailFragment

class CategoryEditView :
    CategoriesContract.EditView,
    DefaultLifecycleObserver {
    internal lateinit var fragment: UploadCategoriesFragment

    /**
     * Progress Dialog for showing background process
     */
    private var progressDialog: ProgressDialog? = null

    /**
     * Gets existing categories from media
     */
    override fun getExistingCategories(): List<String>? = fragment.media?.categories

    /**
     * Returns required context
     */
    override fun getFragmentContext(): Context = fragment.requireContext()

    /**
     * Returns to previous fragment
     */
    override fun goBackToPreviousScreen() {
        fragment.fragmentManager?.popBackStack()
    }

    /**
     * Shows the progress dialog
     */
    override fun showProgressDialog() {
        progressDialog =
            ProgressDialog(fragment.requireContext())
                .apply {
                    setMessage(fragment.getString(R.string.please_wait))
                }.also {
                    it.show()
                }
    }

    /**
     * Hides the progress dialog
     */
    override fun dismissProgressDialog() {
        progressDialog?.dismiss()
    }

    /**
     * Refreshes the categories
     */
    override fun refreshCategories() {
        (fragment.parentFragment as MediaDetailFragment?)?.updateCategories()
    }

    /**
     *
     */
    override fun navigateToLoginScreen() {
        val username = fragment.sessionManager!!.userName
        val logoutListener =
            CommonsApplication.BaseLogoutListener(
                fragment.requireActivity(),
                fragment.requireActivity().getString(R.string.invalid_login_message),
                username,
            )

        instance.clearApplicationData(
            fragment.requireActivity(),
            logoutListener,
        )
    }

    /**
     * Hides the action bar while opening editing fragment
     */
    override fun onResume(owner: LifecycleOwner) {
        if (fragment.media != null) {
            fragment.onBackPressedCallback.isEnabled = true
            (fragment.requireActivity() as AppCompatActivity).supportActionBar?.hide()

            if (fragment.parentFragment?.parentFragment?.parentFragment is ContributionsFragment) {
                ((fragment.parentFragment?.parentFragment?.parentFragment) as ContributionsFragment)
                    .binding
                    ?.cardViewNearby
                    ?.visibility = View.GONE
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        fragment.onBackPressedCallback.isEnabled = false
    }

    /**
     * Shows the action bar while closing editing fragment
     */
    override fun onStop(owner: LifecycleOwner) {
        if (fragment.media != null) {
            (fragment.requireActivity() as AppCompatActivity).supportActionBar?.show()
        }
    }
}
