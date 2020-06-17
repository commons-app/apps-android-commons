package fr.free.nrw.commons.depictions.Media

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.explore.SearchFragmentContract

/**
 * Contract with which DepictedImagesFragment and its presenter will talk to each other
 */
interface DepictedImagesContract {
    interface View : SearchFragmentContract.View<Media>
    interface Presenter : SearchFragmentContract.Presenter<Media>
}
