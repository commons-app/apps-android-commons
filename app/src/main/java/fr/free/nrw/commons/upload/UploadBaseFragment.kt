package fr.free.nrw.commons.upload

import fr.free.nrw.commons.di.CommonsDaggerSupportFragment

/**
 * The base fragment of the fragments in upload
 */
abstract class UploadBaseFragment : CommonsDaggerSupportFragment() {
    lateinit var callback: Callback

    protected open fun onBecameVisible() = Unit

    interface Callback {
        val totalNumberOfSteps: Int
        val isWLMUpload: Boolean

        fun onNextButtonClicked(index: Int)
        fun onPreviousButtonClicked(index: Int)
        fun showProgress(shouldShow: Boolean)
        fun getIndexInViewFlipper(fragment: UploadBaseFragment?): Int
    }

    companion object {
        const val CALLBACK: String = "callback"
    }
}
