package fr.free.nrw.commons.bookmarks.locations;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.nearby.Place;

@Singleton
public class BookmarkLocationListController {

    private MediaWikiApi mediaWikiApi;

    @Inject
    public BookmarkLocationListController(MediaWikiApi mediaWikiApi) {
        this.mediaWikiApi = mediaWikiApi;
    }

    public List<Place> loadFavoritesLocations() {
        List<Place> places = new ArrayList<>();
        /*places.add(new Place(
                "Anstalten Luleå",
                Place.Label.UNKNOWN,
                "bâtiment",
                Uri.parse("http://commons.wikimedia.org/wiki/Special:FilePath/BSicon%20BUILDING.svg"),
                new LatLng(65.62138889f,22.1325f, 1f),
                "",
                null
        ));*/
        // TODO Call Dao to retrieve Places from DB
        return places;
    }
}
