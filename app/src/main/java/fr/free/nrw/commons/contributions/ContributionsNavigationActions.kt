package fr.free.nrw.commons.contributions

/** Navigation actions exposed by the contributions screen to its host activity. */
interface ContributionsNavigationActions {
    /** Scrolls the contributions list to its first item. */
    fun scrollToTop()

    /** Returns to the list when media details consume the back button. */
    fun backButtonClicked(): Boolean
}
