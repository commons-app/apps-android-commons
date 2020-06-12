package fr.free.nrw.commons.explore.categories

import fr.free.nrw.commons.explore.SearchFragmentContract

interface SearchCategoriesFragmentContract {
    interface View : SearchFragmentContract.View<String>
    interface Presenter : SearchFragmentContract.Presenter<String>
}
