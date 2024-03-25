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
import fr.free.nrw.commons.BaseMarker;
import fr.free.nrw.commons.MapController;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LatLng;
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
    public ExploreMapController(ExploreMapCalls explorePlaces) {
        this.exploreMapCalls = explorePlaces;
    }

    /**
     * Takes location as parameter and returns ExplorePlaces info that holds currentLatLng, mediaList,
     * explorePlaceList and boundaryCoordinates
     *
     * @param currentLatLng                     is current geolocation
     * @param searchLatLng                  is the location that we want to search around
     * @param checkingAroundCurrentLocation is a boolean flag. True if we want to check around
     *                                      current location, false if another location
     * @return explorePlacesInfo info that holds currentLatLng, mediaList, explorePlaceList and
     * boundaryCoordinates
     */
    public ExplorePlacesInfo loadAttractionsFromLocation(LatLng currentLatLng, LatLng searchLatLng,
        boolean checkingAroundCurrentLocation) {

        if (searchLatLng == null) {
            Timber.d("Loading attractions explore map, but search is null");
            return null;
        }

        ExplorePlacesInfo explorePlacesInfo = new ExplorePlacesInfo();
        try {
            explorePlacesInfo.currentLatLng = currentLatLng;
            latestSearchLocation = searchLatLng;

            List<Media> mediaList = exploreMapCalls.callCommonsQuery(searchLatLng);
            LatLng[] boundaryCoordinates = {mediaList.get(0).getCoordinates(),   // south
                mediaList.get(0).getCoordinates(), // north
                mediaList.get(0).getCoordinates(), // west
                mediaList.get(0).getCoordinates()};// east, init with a random location

            if (searchLatLng != null) {
                Timber.d("Sorting places by distance...");
                final Map<Media, Double> distances = new HashMap<>();
                for (Media media : mediaList) {
                    distances.put(media,
                        computeDistanceBetween(media.getCoordinates(), searchLatLng));
                    // Find boundaries with basic find max approach
                    if (media.getCoordinates().getLatitude()
                        < boundaryCoordinates[0].getLatitude()) {
                        boundaryCoordinates[0] = media.getCoordinates();
                    }
                    if (media.getCoordinates().getLatitude()
                        > boundaryCoordinates[1].getLatitude()) {
                        boundaryCoordinates[1] = media.getCoordinates();
                    }
                    if (media.getCoordinates().getLongitude()
                        < boundaryCoordinates[2].getLongitude()) {
                        boundaryCoordinates[2] = media.getCoordinates();
                    }
                    if (media.getCoordinates().getLongitude()
                        > boundaryCoordinates[3].getLongitude()) {
                        boundaryCoordinates[3] = media.getCoordinates();
                    }
                }
            }
            explorePlacesInfo.mediaList = mediaList;
            explorePlacesInfo.explorePlaceList = PlaceUtils.mediaToExplorePlace(mediaList);
            explorePlacesInfo.boundaryCoordinates = boundaryCoordinates;

            // Sets latestSearchRadius to maximum distance among boundaries and search location
            for (LatLng bound : boundaryCoordinates) {
                double distance = LocationUtils.calculateDistance(bound.getLatitude(),
                    bound.getLongitude(), searchLatLng.getLatitude(), searchLatLng.getLongitude());
                if (distance > latestSearchRadius) {
                    latestSearchRadius = distance;
                }
            }

            // Our radius searched around us, will be used to understand when user search their own location, we will follow them
            if (checkingAroundCurrentLocation) {
                currentLocationSearchRadius = latestSearchRadius;
                currentLocation = currentLatLng;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return explorePlacesInfo;
    }

    /**
     * Loads attractions from location for map view, we need to return places in Place data type
     *
     * @return baseMarkerOptions list that holds nearby places with their icons
     */
    public static List<BaseMarker> loadAttractionsFromLocationToBaseMarkerOptions(
        LatLng currentLatLng,
        final List<Place> placeList,
        Context context,
        NearbyBaseMarkerThumbCallback callback,
        ExplorePlacesInfo explorePlacesInfo) {
        List<BaseMarker> baseMarkerList = new ArrayList<>();

        if (placeList == null) {
            return baseMarkerList;
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
                final BaseMarker baseMarker = new BaseMarker();
                String distance = formatDistanceBetween(currentLatLng, explorePlace.location);
                explorePlace.setDistance(distance);

                baseMarker.setTitle(
                    explorePlace.name.substring(5, explorePlace.name.lastIndexOf(".")));
                baseMarker.setPosition(
                    new fr.free.nrw.commons.location.LatLng(
                        explorePlace.location.getLatitude(),
                        explorePlace.location.getLongitude(), 0));
                baseMarker.setPlace(explorePlace);

                Glide.with(context)
                    .asBitmap()
                    .load(explorePlace.getThumb())
                    .placeholder(R.drawable.image_placeholder_96)
                    .apply(new RequestOptions().override(96, 96).centerCrop())
                    .into(new CustomTarget<Bitmap>() {
                        // We add icons to markers when bitmaps are ready
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource,
                            @Nullable Transition<? super Bitmap> transition) {
                            baseMarker.setIcon(
                                ImageUtils.addRedBorder(resource, 6, context));
                            baseMarkerList.add(baseMarker);
                            if (baseMarkerList.size()
                                == placeList.size()) { // if true, we added all markers to list and can trigger thumbs ready callback
                                callback.onNearbyBaseMarkerThumbsReady(baseMarkerList,
                                    explorePlacesInfo);
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }

                        // We add thumbnail icon for images that couldn't be loaded
                        @Override
                        public void onLoadFailed(@Nullable final Drawable errorDrawable) {
                            super.onLoadFailed(errorDrawable);
                            baseMarker.fromResource(context, R.drawable.image_placeholder_96);
                            baseMarkerList.add(baseMarker);
                            if (baseMarkerList.size()
                                == placeList.size()) { // if true, we added all markers to list and can trigger thumbs ready callback
                                callback.onNearbyBaseMarkerThumbsReady(baseMarkerList,
                                    explorePlacesInfo);
                            }
                        }
                    });
            }
        }
        return baseMarkerList;
    }

    interface NearbyBaseMarkerThumbCallback {

        // Callback to notify thumbnails of explore markers are added as icons and ready
        void onNearbyBaseMarkerThumbsReady(List<BaseMarker> baseMarkers,
            ExplorePlacesInfo explorePlacesInfo);
    }
}
