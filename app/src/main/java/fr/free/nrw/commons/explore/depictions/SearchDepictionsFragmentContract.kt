package fr.free.nrw.commons.explore.depictions

import fr.free.nrw.commons.explore.PagingContract
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem

/**
 * The contract with with SearchDepictionsFragment and its presenter would talk to each other
 */
interface SearchDepictionsFragmentContract {
    interface View : PagingContract.View<DepictedItem>
    interface Presenter : PagingContract.Presenter<DepictedItem>
}
