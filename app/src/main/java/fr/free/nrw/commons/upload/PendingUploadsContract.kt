package fr.free.nrw.commons.upload

import android.content.Context
import fr.free.nrw.commons.BasePresenter
import fr.free.nrw.commons.contributions.Contribution

/**
 * The contract using which the PendingUploadsFragment or FailedUploadsFragment would communicate
 * with its PendingUploadsPresenter
 */
class PendingUploadsContract {
    /**
     * Interface representing the view for uploads.
     */
    interface View

    /**
     * Interface representing the user actions related to uploads.
     */
    interface UserActionListener : BasePresenter<View> {
        /**
         * Deletes a upload.
         */
        fun deleteUpload(contribution: Contribution?, context: Context?)
    }
}
