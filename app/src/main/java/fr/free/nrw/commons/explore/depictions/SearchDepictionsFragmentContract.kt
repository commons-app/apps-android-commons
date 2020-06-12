package fr.free.nrw.commons.explore.depictions

import fr.free.nrw.commons.explore.SearchFragmentContract
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem

/**
 * The contract with with SearchDepictionsFragment and its presenter would talk to each other
 */
interface SearchDepictionsFragmentContract {
    interface View : SearchFragmentContract.View<DepictedItem>
    interface Presenter : SearchFragmentContract.Presenter<DepictedItem>
}
