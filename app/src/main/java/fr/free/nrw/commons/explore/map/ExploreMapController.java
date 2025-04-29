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
    public LatLng latestSearchLocation;         // Last search center
    public LatLng currentLocation;              // User’s current location
    public double latestSearchRadius = 0;       // Last search radius
    public double currentLocationSearchRadius = 0; // Radius when searching around current location

    @Inject
    public ExploreMapController(ExploreMapCalls explorePlaces) {
        this.exploreMapCalls = explorePlaces;
    }

    /**
     * Load attractions around a given location and compute boundaries.
     */
    public ExplorePlacesInfo loadAttractionsFromLocation(
            LatLng currentLatLng,
            LatLng searchLatLng,
            boolean checkingAroundCurrentLocation
    ) {
        if (searchLatLng == null) {
            Timber.d("Loading attractions explore map, but search is null");
            return null;
        }

        ExplorePlacesInfo explorePlacesInfo = new ExplorePlacesInfo();
        try {
            explorePlacesInfo.currentLatLng = currentLatLng;
            latestSearchLocation = searchLatLng;

            List<Media> mediaList = exploreMapCalls.callCommonsQuery(searchLatLng);
            LatLng[] boundaryCoordinates = {
                mediaList.get(0).getCoordinates(),  // south
                mediaList.get(0).getCoordinates(),  // north
                mediaList.get(0).getCoordinates(),  // west
                mediaList.get(0).getCoordinates()   // east
            };

            // Compute distances and update boundaries
            Timber.d("Sorting places by distance...");
            Map<Media, Double> distances = new HashMap<>();
            for (Media media : mediaList) {
                distances.put(media, computeDistanceBetween(media.getCoordinates(), searchLatLng));

                LatLng coords = media.getCoordinates();
                if (coords.getLatitude() < boundaryCoordinates[0].getLatitude()) {
                    boundaryCoordinates[0] = coords;
                }
                if (coords.getLatitude() > boundaryCoordinates[1].getLatitude()) {
                    boundaryCoordinates[1] = coords;
                }
                if (coords.getLongitude() < boundaryCoordinates[2].getLongitude()) {
                    boundaryCoordinates[2] = coords;
                }
                if (coords.getLongitude() > boundaryCoordinates[3].getLongitude()) {
                    boundaryCoordinates[3] = coords;
                }
            }

            explorePlacesInfo.mediaList = mediaList;
            explorePlacesInfo.explorePlaceList = PlaceUtils.mediaToExplorePlace(mediaList);
            explorePlacesInfo.boundaryCoordinates = boundaryCoordinates;

            // Compute latestSearchRadius as the max distance from search center
            for (LatLng bound : boundaryCoordinates) {
                double distance = LocationUtils.calculateDistance(
                    bound.getLatitude(),
                    bound.getLongitude(),
                    searchLatLng.getLatitude(),
                    searchLatLng.getLongitude()
                );
                if (distance > latestSearchRadius) {
                    latestSearchRadius = distance;
                }
            }

            // If searching around current location, capture that state
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
     * Convert a list of Place objects into BaseMarker options for displaying on the map.
     */
    public static List<BaseMarker> loadAttractionsFromLocationToBaseMarkerOptions(
            LatLng currentLatLng,
            final List<Place> placeList,
            Context context,
            NearbyBaseMarkerThumbCallback callback,
            ExplorePlacesInfo explorePlacesInfo
    ) {
        List<BaseMarker> baseMarkerList = new ArrayList<>();

        if (placeList == null) {
            return baseMarkerList;
        }

        VectorDrawableCompat vectorDrawable = null;
        try {
            vectorDrawable = VectorDrawableCompat.create(
                context.getResources(),
                R.drawable.ic_custom_map_marker_dark,
                context.getTheme()
            );
        } catch (Resources.NotFoundException ignored) {
            // ignore when running tests.
        }

        if (vectorDrawable != null) {
            for (Place explorePlace : placeList) {
                final BaseMarker baseMarker = new BaseMarker();
                String distance = formatDistanceBetween(currentLatLng, explorePlace.location);
                explorePlace.setDistance(distance);

                // Use caption if available, otherwise derive title from filename
                if (explorePlace.caption != null && !explorePlace.caption.isEmpty()) {
                    baseMarker.setTitle(explorePlace.caption);
                } else {
                    baseMarker.setTitle(
                        explorePlace.name.substring(
                            5,
                            explorePlace.name.lastIndexOf(".")
                        )
                    );
                }

                baseMarker.setPosition(
                    new fr.free.nrw.commons.location.LatLng(
                        explorePlace.location.getLatitude(),
                        explorePlace.location.getLongitude(),
                        0
                    )
                );
                baseMarker.setPlace(explorePlace);

                // Load thumbnail asynchronously
                Glide.with(context)
                     .asBitmap()
                     .load(explorePlace.getThumb())
                     .placeholder(R.drawable.image_placeholder_96)
                     .apply(new RequestOptions().override(96, 96).centerCrop())
                     .into(new CustomTarget<Bitmap>() {
                         @Override
                         public void onResourceReady(
                             @NonNull Bitmap resource,
                             @Nullable Transition<? super Bitmap> transition
                         ) {
                             baseMarker.setIcon(ImageUtils.addRedBorder(resource, 6, context));
                             baseMarkerList.add(baseMarker);
                             if (baseMarkerList.size() == placeList.size()) {
                                 callback.onNearbyBaseMarkerThumbsReady(
                                     baseMarkerList,
                                     explorePlacesInfo
                                 );
                             }
                         }

                         @Override
                         public void onLoadCleared(@Nullable Drawable placeholder) {
                             // no-op
                         }

                         @Override
                         public void onLoadFailed(@Nullable Drawable errorDrawable) {
                             super.onLoadFailed(errorDrawable);
                             baseMarker.fromResource(context, R.drawable.image_placeholder_96);
                             baseMarkerList.add(baseMarker);
                             if (baseMarkerList.size() == placeList.size()) {
                                 callback.onNearbyBaseMarkerThumbsReady(
                                     baseMarkerList,
                                     explorePlacesInfo
                                 );
                             }
                         }
                     });
            }
        }

        return baseMarkerList;
    }

    /**
     * Callback interface for when all marker thumbnails are ready.
     */
    public interface NearbyBaseMarkerThumbCallback {
        void onNearbyBaseMarkerThumbsReady(
            List<BaseMarker> baseMarkers,
            ExplorePlacesInfo explorePlacesInfo
        );
    }
}
