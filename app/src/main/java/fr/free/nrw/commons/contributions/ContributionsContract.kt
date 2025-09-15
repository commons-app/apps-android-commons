package fr.free.nrw.commons.contributions

import android.content.Context
import fr.free.nrw.commons.BasePresenter

/**
 * The contract for Contributions View & Presenter
 */
interface ContributionsContract {

    interface View {
        fun getContext(): Context?
    }

    interface UserActionListener : BasePresenter<View> {
        fun getContributionsWithTitle(uri: String): Contribution
    }
}
