package fr.free.nrw.commons.explore.map;

import static fr.free.nrw.commons.utils.LengthUtils.computeDistanceBetween;
import static fr.free.nrw.commons.utils.LengthUtils.formatDistanceBetween;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import fr.free.nrw.commons.MapController;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.NearbyBaseMarker;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.utils.ImageUtils;
import fr.free.nrw.commons.utils.LocationUtils;
import fr.free.nrw.commons.utils.PlaceUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import timber.log.Timber;

public class ExploreMapController extends MapController {
    private final ExploreMapCalls exploreMapCalls;
    public LatLng latestSearchLocation; // Can be current and camera target on search this area button is used
    public LatLng currentLocation; // current location of user
    public double latestSearchRadius = 0; // Any last search radius
    public double currentLocationSearchRadius = 0; // Search radius of only searches around current location


    @Inject
    public ExploreMapController(ExploreMapCalls exploreMapCalls) {
        this.exploreMapCalls = exploreMapCalls;
    }

    /**
     * Takes location as parameter and returns ExplorePlaces info that holds curLatLng, mediaList, explorePlaceList and boundaryCoordinates
     * @param curLatLng is current geolocation
     * @param searchLatLng is the location that we want to search around
     * @param checkingAroundCurrentLocation is a boolean flag. True if we want to check around current location, false if another location
     * @return explorePlacesInfo info that holds curLatLng, mediaList, explorePlaceList and boundaryCoordinates
     */
    public PlacesInfo loadAttractionsFromLocation(LatLng curLatLng, LatLng searchLatLng, boolean checkingAroundCurrentLocation) {

        if (searchLatLng == null) {
            Timber.d("Loading attractions explore map, but search is null");
            return null;
        }

        PlacesInfo explorePlacesInfo = new PlacesInfo();
        try {
            explorePlacesInfo.curLatLng = curLatLng;
            explorePlacesInfo.searchLatLng = searchLatLng;
            latestSearchLocation = searchLatLng;

            List<Media> mediaList = exploreMapCalls.callCommonsQuery(searchLatLng);

            if (mediaList == null || mediaList.isEmpty()) {
                // This will be handled on the fragment with error message saying explore places couldn't fetched
                return null;
            }

            LatLng[] boundaryCoordinates = calculateBoundaryCoordinates(mediaList, searchLatLng);
            explorePlacesInfo.mediaList = mediaList;
            explorePlacesInfo.placeList = PlaceUtils.mediaToExplorePlace(mediaList);
            explorePlacesInfo.boundaryCoordinates = boundaryCoordinates;

            // Sets latestSearchRadius to maximum distance among boundaries and search location
            for (LatLng bound : boundaryCoordinates) {
                double distance = LocationUtils.commonsLatLngToMapBoxLatLng(bound).distanceTo(LocationUtils.commonsLatLngToMapBoxLatLng(latestSearchLocation));
                if (distance > latestSearchRadius) {
                    latestSearchRadius = distance;
                }
            }

            // Our radius searched around us, will be used to understand when user search their own location, we will follow them
            if (checkingAroundCurrentLocation) {
                currentLocationSearchRadius = latestSearchRadius;
                currentLocation = curLatLng;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return explorePlacesInfo;
    }

    public LatLng[] calculateBoundaryCoordinates(List<Media> mediaList, LatLng searchLatLng) {
        if (mediaList == null || mediaList.isEmpty()) {
            return null;
        } else {
            LatLng[] boundaryCoordinates = {mediaList.get(0).getCoordinates(),   // south
                mediaList.get(0).getCoordinates(), // north
                mediaList.get(0).getCoordinates(), // west
                mediaList.get(0).getCoordinates()};// east, init with a random location

            if (searchLatLng != null) {
                Timber.d("Sorting places by distance...");
                final Map<Media, Double> distances = new HashMap<>();
                for (Media media : mediaList) {
                    distances.put(media, computeDistanceBetween(media.getCoordinates(), searchLatLng));
                    // Find boundaries with basic find max approach
                    if (media.getCoordinates().getLatitude() < boundaryCoordinates[0].getLatitude()) {
                        boundaryCoordinates[0] = media.getCoordinates();
                    }
                    if (media.getCoordinates().getLatitude() > boundaryCoordinates[1].getLatitude()) {
                        boundaryCoordinates[1] = media.getCoordinates();
                    }
                    if (media.getCoordinates().getLongitude() < boundaryCoordinates[2].getLongitude()) {
                        boundaryCoordinates[2] = media.getCoordinates();
                    }
                    if (media.getCoordinates().getLongitude() > boundaryCoordinates[3].getLongitude()) {
                        boundaryCoordinates[3] = media.getCoordinates();
                    }
                }
            }
            return boundaryCoordinates;
        }
    }

    /**
     * Loads attractions from location for map view, we need to return places in Place data type
     * @return baseMarkerOptions list that holds nearby places with their icons
     */
    public List<NearbyBaseMarker> loadAttractionsFromLocationToBaseMarkerOptions(
        LatLng curLatLng,
        final List<Place> placeList,
        Context context,
        NearbyBaseMarkerThumbCallback callback,
        Marker selectedMarker,
        boolean shouldTrackPosition,
        PlacesInfo explorePlacesInfo) {
        List<NearbyBaseMarker> baseMarkerOptions = new ArrayList<>();

        if (placeList == null) {
            return baseMarkerOptions;
        }

        VectorDrawableCompat vectorDrawable = null;
        try {
            vectorDrawable = VectorDrawableCompat.create(
                context.getResources(), R.drawable.ic_custom_map_marker, context.getTheme());

        } catch (Resources.NotFoundException e) {
            // ignore when running tests.
        }
        if (vectorDrawable != null) {
            for (Place explorePlace : placeList) {
                final NearbyBaseMarker nearbyBaseMarker = new NearbyBaseMarker();
                String distance = formatDistanceBetween(curLatLng, explorePlace.location);
                explorePlace.setDistance(distance);

                nearbyBaseMarker.title(explorePlace.name.substring(5, explorePlace.name.lastIndexOf(".")));
                nearbyBaseMarker.position(
                    new com.mapbox.mapboxsdk.geometry.LatLng(
                        explorePlace.location.getLatitude(),
                        explorePlace.location.getLongitude()));
                nearbyBaseMarker.place(explorePlace);

                Glide.with(context)
                    .asBitmap()
                    .load(explorePlace.getThumb())
                    .placeholder(R.drawable.image_placeholder_96)
                    .apply(new RequestOptions().override(96, 96).centerCrop())
                    .into(new CustomTarget<Bitmap>() {
                        // We add icons to markers when bitmaps are ready
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            nearbyBaseMarker.setIcon(IconFactory.getInstance(context).fromBitmap(
                                ImageUtils.addRedBorder(resource, 6, context)));
                            baseMarkerOptions.add(nearbyBaseMarker);
                            if (baseMarkerOptions.size() == placeList.size()) { // if true, we added all markers to list and can trigger thumbs ready callback
                                callback.onNearbyBaseMarkerThumbsReady(baseMarkerOptions, explorePlacesInfo, selectedMarker, shouldTrackPosition);
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }

                        // We add thumbnail icon for images that couldn't be loaded
                        @Override
                        public void onLoadFailed(@Nullable final Drawable errorDrawable) {
                            super.onLoadFailed(errorDrawable);
                            nearbyBaseMarker.setIcon(IconFactory.getInstance(context).fromResource(R.drawable.image_placeholder_96));
                            baseMarkerOptions.add(nearbyBaseMarker);
                            if (baseMarkerOptions.size() == placeList.size()) { // if true, we added all markers to list and can trigger thumbs ready callback
                                callback.onNearbyBaseMarkerThumbsReady(baseMarkerOptions, explorePlacesInfo, selectedMarker, shouldTrackPosition);
                            }
                        }
                    });
            }
        }
        return baseMarkerOptions;
    }

    interface NearbyBaseMarkerThumbCallback {
        // Callback to notify thumbnails of explore markers are added as icons and ready
        void onNearbyBaseMarkerThumbsReady(List<NearbyBaseMarker> baseMarkers, PlacesInfo explorePlacesInfo, Marker selectedMarker, boolean shouldTrackPosition);
    }
}
