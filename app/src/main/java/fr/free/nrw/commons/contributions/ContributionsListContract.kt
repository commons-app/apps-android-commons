package fr.free.nrw.commons.contributions

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import fr.free.nrw.commons.BasePresenter

/**
 * The contract for Contributions list View & Presenter
 */
class ContributionsListContract {
    interface View {
        fun showWelcomeTip(numberOfUploads: Boolean)

        fun showProgress(shouldShow: Boolean)

        fun showNoContributionsUI(shouldShow: Boolean)
    }

    interface UserActionListener : BasePresenter<View> {
        fun refreshList(swipeRefreshLayout: SwipeRefreshLayout?)
    }
}
