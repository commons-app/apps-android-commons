package fr.free.nrw.commons.depictions.Media

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.explore.PagingContract

/**
 * Contract with which DepictedImagesFragment and its presenter will talk to each other
 */
interface DepictedImagesContract {
    interface View : PagingContract.View<Media>
    interface Presenter : PagingContract.Presenter<Media>
}
