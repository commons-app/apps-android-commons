package fr.free.nrw.commons.explore.media

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.explore.PagingContract


interface SearchMediaFragmentContract {
    interface View : PagingContract.View<Media>
    interface Presenter : PagingContract.Presenter<Media>
}
