package fr.free.nrw.commons.explore.map

/**
 * Defines actions and queries for the Explore map details bottom sheet.
 */
interface ExploreMapDetailsSheetActions {
    /**
     * Hides the bottom details sheet if it is currently displayed.
     */
    fun hideBottomDetailsSheet()

    /**
     * Checks whether the details bottom sheet is currently visible.
     *
     * @return true if visible, false otherwise
     */
    fun isDetailsBottomSheetVisible(): Boolean

    /**
     * Handles the back button event to collapse or hide the bottom sheet if expanded.
     *
     * @return true if the back press was handled by the bottom sheet, false otherwise
     */
    fun backButtonClicked(): Boolean
}
