package fr.free.nrw.commons;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.Place;
import java.util.List;

public abstract class MapController {

    /**
     * We pass this variable as a group of placeList and boundaryCoordinates
     */
    public class NearbyPlacesInfo {
        public List<Place> placeList; // List of nearby places
        public LatLng[] boundaryCoordinates; // Corners of nearby area
        public LatLng currentLatLng; // Current location when this places are populated
        public LatLng searchLatLng; // Search location for finding this places
        public List<Media> mediaList; // Search location for finding this places
    }

    /**
     * We pass this variable as a group of placeList and boundaryCoordinates
     */
    public class ExplorePlacesInfo {
        public List<Place> explorePlaceList; // List of nearby places
        public LatLng[] boundaryCoordinates; // Corners of nearby area
        public LatLng currentLatLng; // Current location when this places are populated
        public LatLng searchLatLng; // Search location for finding this places
        public List<Media> mediaList; // Search location for finding this places
    }
}
