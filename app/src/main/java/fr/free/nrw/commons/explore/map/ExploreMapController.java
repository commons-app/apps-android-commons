package fr.free.nrw.commons.explore.map;

import static fr.free.nrw.commons.utils.LengthUtils.computeDistanceBetween;
import static fr.free.nrw.commons.utils.LengthUtils.formatDistanceBetween;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION_CODES;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.util.Log;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import fr.free.nrw.commons.MapController;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.explore.ExplorePlace;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.NearbyBaseMarker;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.utils.ImageUtils;
import fr.free.nrw.commons.utils.UiUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import timber.log.Timber;

public class ExploreMapController extends MapController {
    private static final int MAX_RESULTS = 1000;
    private final ExplorePlaces explorePlaces;
    public LatLng latestSearchLocation; // Can be current and camera target on search this area button is used
    public double latestSearchRadius = 10.0; // Any last search radius except closest result search


    @Inject
    public ExploreMapController(ExplorePlaces explorePlaces) {
        this.explorePlaces = explorePlaces;
    }

    public ExplorePlacesInfo loadAttractionsFromLocation(LatLng curLatLng, LatLng searchLatLng, boolean checkingAroundCurrentLocation, boolean isFromSearchActivity, String query) {

        // TODO: check nearbyPlacesInfo in NearbyController for search this area logic

        if (searchLatLng == null) {
            Timber.d("Loading attractions explore map, but curLatLng is null");
            return null;
        }

        ExplorePlacesInfo explorePlacesInfo = new ExplorePlacesInfo();
        try {
            explorePlacesInfo.curLatLng = curLatLng;
            latestSearchLocation = searchLatLng;
            List<Media> mediaList = explorePlaces.callCommonsQuery(curLatLng, 30, isFromSearchActivity, query);
            LatLng[] boundaryCoordinates = {mediaList.get(0).getCoordinates(),   // south
                mediaList.get(0).getCoordinates(), // north
                mediaList.get(0).getCoordinates(), // west
                mediaList.get(0).getCoordinates()};// east, init with a random location


            if (curLatLng != null) {
                Timber.d("Sorting places by distance...");
                final Map<Media, Double> distances = new HashMap<>();
                for (Media media : mediaList) {
                    distances.put(media, computeDistanceBetween(media.getCoordinates(), curLatLng));
                    // Find boundaries with basic find max approach
                    if (media.getCoordinates().getLatitude() < boundaryCoordinates[0].getLatitude()) {
                        Log.d("deneme","media lat is: "+media.getCoordinates().getLatitude()+" smaller than bound lat is:"+ boundaryCoordinates[0].getLatitude());
                        boundaryCoordinates[0] = media.getCoordinates();
                    }
                    if (media.getCoordinates().getLatitude() > boundaryCoordinates[1].getLatitude()) {
                        Log.d("deneme","media lat is: "+media.getCoordinates().getLatitude()+" bigger than bound lat is:"+ boundaryCoordinates[1].getLatitude());
                        boundaryCoordinates[1] = media.getCoordinates();
                    }
                    if (media.getCoordinates().getLongitude() < boundaryCoordinates[2].getLongitude()) {
                        boundaryCoordinates[2] = media.getCoordinates();
                    }
                    if (media.getCoordinates().getLongitude() > boundaryCoordinates[3].getLongitude()) {
                        boundaryCoordinates[3] = media.getCoordinates();
                    }
                }
                /*Collections.sort(mediaList,
                    (lhs, rhs) -> {
                        double lhsDistance = distances.get(lhs);
                        double rhsDistance = distances.get(rhs);
                        return (int) (lhsDistance - rhsDistance);
                    }
                );*/
            }
            explorePlacesInfo.explorePlaceList = mediaToExplorePlace(mediaList);
            explorePlacesInfo.boundaryCoordinates = boundaryCoordinates;
            Log.d("deneme","boundary coords:"+boundaryCoordinates[0] + ", "+ boundaryCoordinates[1]+ ", "+ boundaryCoordinates[2]+ ", "+ boundaryCoordinates[3]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return explorePlacesInfo;
    }

    /**
     * Loads attractions from location for map view, we need to return Bplaces in Place data type
     * @return BaseMarkerOptions list that holds nearby places
     */
    public static List<NearbyBaseMarker> loadAttractionsFromLocationToBaseMarkerOptions(
        LatLng curLatLng,
        List<ExplorePlace> placeList,
        Context context,
        List<Place> bookmarkLocations) {
        List<NearbyBaseMarker> baseMarkerOptions = new ArrayList<>();

        if (placeList == null) {
            return baseMarkerOptions;
        }

        placeList = placeList.subList(0, Math.min(placeList.size(), MAX_RESULTS));

        VectorDrawableCompat vectorDrawable = null;
        VectorDrawableCompat vectorDrawableGreen = null;
        VectorDrawableCompat vectorDrawableGrey = null;
        VectorDrawableCompat vectorDrawableMonuments = null;
        vectorDrawable = null;
        try {
            vectorDrawable = VectorDrawableCompat.create(
                context.getResources(), R.drawable.ic_custom_map_marker, context.getTheme());
            vectorDrawableGreen = VectorDrawableCompat.create(
                context.getResources(), R.drawable.ic_custom_map_marker_green, context.getTheme());
            vectorDrawableGrey = VectorDrawableCompat.create(
                context.getResources(), R.drawable.ic_custom_map_marker_grey, context.getTheme());
            vectorDrawableMonuments = VectorDrawableCompat
                .create(context.getResources(), R.drawable.ic_custom_map_marker_monuments,
                    context.getTheme());
        } catch (Resources.NotFoundException e) {
            // ignore when running tests.
        }
        if (vectorDrawable != null) {
            Bitmap icon = UiUtils.getBitmap(vectorDrawable);
            Bitmap iconGreen = UiUtils.getBitmap(vectorDrawableGreen);
            Bitmap iconGrey = UiUtils.getBitmap(vectorDrawableGrey);
            Bitmap iconMonuments = UiUtils.getBitmap(vectorDrawableMonuments);

            for (ExplorePlace explorePlace : placeList) {
                final NearbyBaseMarker nearbyBaseMarker = new NearbyBaseMarker();
                String distance = formatDistanceBetween(curLatLng, explorePlace.location);
                explorePlace.setDistance(distance);

                nearbyBaseMarker.title(explorePlace.name);
                nearbyBaseMarker.position(
                    new com.mapbox.mapboxsdk.geometry.LatLng(
                        explorePlace.location.getLatitude(),
                        explorePlace.location.getLongitude()));
                nearbyBaseMarker.place(explorePlace);
                // TODO Glide and thumbnails here

                nearbyBaseMarker.icon(IconFactory.getInstance(context).fromBitmap(UiUtils.getBitmap(vectorDrawableGreen)));
                baseMarkerOptions.add(nearbyBaseMarker);
            }
        }

        return baseMarkerOptions;
    }

    private List<ExplorePlace> mediaToExplorePlace( List<Media> mediaList) {
        List<ExplorePlace> explorePlaceList = new ArrayList<>();
        for (Media media :mediaList) {
            explorePlaceList.add(new ExplorePlace(media.getFilename(),
                media.getFallbackDescription(),
                media.getCoordinates(),
                media.getImageUrl(),
                media.getImageUrl(),
                media.getThumbUrl()));
        }
        return explorePlaceList;
    }
}
