package fr.free.nrw.commons.explore.categories

import fr.free.nrw.commons.explore.PagingContract

interface SearchCategoriesFragmentContract {
    interface View : PagingContract.View<String>
    interface Presenter : PagingContract.Presenter<String>
}
