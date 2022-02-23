package fr.free.nrw.commons.explore.map;

import static fr.free.nrw.commons.utils.LengthUtils.computeDistanceBetween;
import static fr.free.nrw.commons.utils.LengthUtils.formatDistanceBetween;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.util.Log;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

public class ExploreWithQueryMapController extends MapController {
    private static final int MAX_RESULTS = 1000;
    private final ExploreWithQueryPlaces exploreWithQueryPlaces;
    public static LatLng latestSearchLocation; // Can be current and camera target on search this area button is used
    public static double latestSearchRadius = 10.0; // Any last search radius except closest result search
    public static double farthestDistance;


    @Inject
    public ExploreWithQueryMapController(ExploreWithQueryPlaces exploreWithQueryPlaces) {
        this.exploreWithQueryPlaces = exploreWithQueryPlaces;
    }

    public ExplorePlacesInfo loadAttractionsFromLocation(LatLng curLatLng, LatLng searchLatLng, boolean checkingAroundCurrentLocation) {

        // TODO: check nearbyPlacesInfo in NearbyController for search this area logic

        if (searchLatLng == null) {
            Timber.d("Loading attractions explore map, but curLatLng is null");
            return null;
        }

        ExplorePlacesInfo explorePlacesInfo = new ExplorePlacesInfo();
        try {
            explorePlacesInfo.curLatLng = curLatLng;
            latestSearchLocation = searchLatLng;
            List<Media> mediaList = exploreWithQueryPlaces.callCommonsQuery(searchLatLng, 30);

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
                Collections.sort(mediaList,
                    (lhs, rhs) -> {
                        double lhsDistance = distances.get(lhs);
                        double rhsDistance = distances.get(rhs);
                        return (int) (lhsDistance - rhsDistance);
                    }
                );
            }

            Log.d("nesli2","mediaList" + mediaList);
            explorePlacesInfo.explorePlaceList = mediaToExplorePlace(mediaList);
            explorePlacesInfo.boundaryCoordinates = boundaryCoordinates;
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("nesli2","error happened"+e.toString());
        }
        //Log.d("nesli2","nearby places info" + explorePlacesInfo.explorePlaceList.get(0));
        return explorePlacesInfo;
    }

    /**
     * Loads attractions from location for map view, we need to return BaseMarkerOption data type.
     *
     * @param curLatLng users current location
     * @param placeList list of nearby places in Place data type
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
                // Check if string is only spaces or empty, if so place doesn't have any picture
                /*ImageView imageView = new ImageView(context);

                try {
                    Drawable drawable = Glide.with(context).load(explorePlace.thumb).thumbnail(0.3f).submit().get();
                    Log.d("nesli2", "setting nearby base marker for " + nearbyBaseMarker.getMarker().getTitle() + " and pic is " + explorePlace.thumb + " and resource is " + drawable);
                    nearbyBaseMarker.setIcon(IconFactory.getInstance(context).fromBitmap(UiUtils.drawableToBitmap(drawable)));

                        baseMarkerOptions.add(nearbyBaseMarker);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/


                /*Glide.with(context).asBitmap().load(explorePlace.thumb).into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        //imageView.setImageBitmap(resource);
                        //nearbyBaseMarker.icon(IconFactory.getInstance(context)
                          //  .fromBitmap(resource));
                        Log.d("nesli2", "setting nearby base marker for " + nearbyBaseMarker.getMarker().getTitle() + " and pic is " + explorePlace.thumb + " and resource is " + resource);
                        nearbyBaseMarker.setIcon(IconFactory.getInstance(context)
                            .fromBitmap(resource));
                        baseMarkerOptions.add(nearbyBaseMarker);
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });*/

                nearbyBaseMarker.icon(IconFactory.getInstance(context).fromBitmap(UiUtils.getBitmap(vectorDrawableGreen)));
                baseMarkerOptions.add(nearbyBaseMarker);

                /*if (!explorePlace.pic.trim().isEmpty()) {
                    if (iconGreen != null) {
                        nearbyBaseMarker.icon(IconFactory.getInstance(context)
                            .fromBitmap(thumnail));
                    }
                }else {
                    nearbyBaseMarker.icon(IconFactory.getInstance(context)
                        .fromBitmap(thumnail));
                }*/
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
