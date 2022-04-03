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
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
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
import fr.free.nrw.commons.nearby.Sitelinks;
import fr.free.nrw.commons.utils.LocationUtils;
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
    public double latestSearchRadius = 0; // Any last search radius except closest result search


    @Inject
    public ExploreMapController(ExplorePlaces explorePlaces) {
        this.explorePlaces = explorePlaces;
    }

    public ExplorePlacesInfo loadAttractionsFromLocation(LatLng curLatLng, LatLng searchLatLng, boolean checkingAroundCurrentLocation) {

        // TODO: check nearbyPlacesInfo in NearbyController for search this area logic

        if (searchLatLng == null) {
            Timber.d("Loading attractions explore map, but search is null");
            return null;
        }

        ExplorePlacesInfo explorePlacesInfo = new ExplorePlacesInfo();
        try {
            explorePlacesInfo.curLatLng = curLatLng;
            latestSearchLocation = searchLatLng;

            List<Media> mediaList = explorePlaces.callCommonsQuery(searchLatLng, 30);
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
            explorePlacesInfo.mediaList = mediaList;
            explorePlacesInfo.explorePlaceList = mediaToExplorePlace(mediaList);
            explorePlacesInfo.boundaryCoordinates = boundaryCoordinates;

            // Sets latestSearchRadius to maximum distance amoung boundaries and search location
            for (LatLng bound : boundaryCoordinates) {
                double distance = LocationUtils.commonsLatLngToMapBoxLatLng(bound).distanceTo(LocationUtils.commonsLatLngToMapBoxLatLng(latestSearchLocation));
                if (distance > latestSearchRadius) {
                    latestSearchRadius = distance;
                }
            }
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
        final List<Place> placeList,
        Context context,
        List<Place> bookmarkLocations,
        NearbyBaseMarkerThumbCallback callback,
        Marker selectedMarker,
        boolean shouldTrackPosition,
        ExplorePlacesInfo explorePlacesInfo) {
        List<NearbyBaseMarker> baseMarkerOptions = new ArrayList<>();

        if (placeList == null) {
            return baseMarkerOptions;
        }

        //placeList = placeList.subList(0, Math.min(placeList.size(), MAX_RESULTS));

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

                nearbyBaseMarker.title(explorePlace.name);
                nearbyBaseMarker.position(
                    new com.mapbox.mapboxsdk.geometry.LatLng(
                        explorePlace.location.getLatitude(),
                        explorePlace.location.getLongitude()));
                nearbyBaseMarker.place(explorePlace);

                Glide.with(context)
                    .asBitmap()
                    .load(explorePlace.getThumb())
                    .apply(new RequestOptions().override(96, 96).centerCrop().transform(new RoundedCorners(dp2px(context, 4))))
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            nearbyBaseMarker.setIcon(IconFactory.getInstance(context).fromBitmap(resource));
                            baseMarkerOptions.add(nearbyBaseMarker);
                            if (baseMarkerOptions.size() == placeList.size()) {
                                callback.onNearbyBaseMarkerThumbsReady(baseMarkerOptions, explorePlacesInfo, selectedMarker, shouldTrackPosition);
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }
                    });
            }
        }

        return baseMarkerOptions;
    }
    public static int dp2px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5F);
    }
    private List<Place> mediaToExplorePlace( List<Media> mediaList) {
        List<Place> explorePlaceList = new ArrayList<>();
        for (Media media :mediaList) {
            explorePlaceList.add(new Place(media.getFilename(),
                media.getFallbackDescription(),
                media.getCoordinates(),
                media.getCategories().toString(), // TODO make categories single string
                new Sitelinks.Builder() // TODO add sitelinks
                    .setCommonsLink(media.getPageTitle().getCanonicalUri())
                    .setWikipediaLink("")
                    .setWikidataLink("")
                    .build(),
                media.getImageUrl(),
                media.getThumbUrl()));
        }
        return explorePlaceList;
    }


    interface NearbyBaseMarkerThumbCallback {
        void onNearbyBaseMarkerThumbsReady(List<NearbyBaseMarker> baseMarkers, ExplorePlacesInfo explorePlacesInfo, Marker selectedMarker, boolean shouldTrackPosition);
    }
}
