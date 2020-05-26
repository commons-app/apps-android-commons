package fr.free.nrw.commons.explore.media

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.explore.SearchFragmentContract


interface SearchMediaFragmentContract {
    interface View : SearchFragmentContract.View<Media>
    interface Presenter : SearchFragmentContract.Presenter<Media>
}
