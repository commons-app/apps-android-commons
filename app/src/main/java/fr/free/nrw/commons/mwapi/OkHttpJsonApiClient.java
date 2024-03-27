package fr.free.nrw.commons.mwapi;

import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.LEADERBOARD_END_POINT;
import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.UPDATE_AVATAR_END_POINT;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.Gson;
import fr.free.nrw.commons.campaigns.CampaignResponseDTO;
import fr.free.nrw.commons.explore.depictions.DepictsClient;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.model.PlaceBindings;
import fr.free.nrw.commons.nearby.model.ItemsClass;
import fr.free.nrw.commons.nearby.model.NearbyResponse;
import fr.free.nrw.commons.nearby.model.NearbyResultItem;
import fr.free.nrw.commons.profile.achievements.FeaturedImages;
import fr.free.nrw.commons.profile.achievements.FeedbackResponse;
import fr.free.nrw.commons.profile.leaderboard.LeaderboardResponse;
import fr.free.nrw.commons.profile.leaderboard.UpdateAvatarResponse;
import fr.free.nrw.commons.upload.FileUtils;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import fr.free.nrw.commons.utils.ConfigUtils;
import fr.free.nrw.commons.wikidata.model.GetWikidataEditCountResponse;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

/**
 * Test methods in ok http api client
 */
@Singleton
public class OkHttpJsonApiClient {

    private final OkHttpClient okHttpClient;
    private final DepictsClient depictsClient;
    private final HttpUrl wikiMediaToolforgeUrl;
    private final String sparqlQueryUrl;
    private final String campaignsUrl;
    private final Gson gson;


    @Inject
    public OkHttpJsonApiClient(OkHttpClient okHttpClient,
        DepictsClient depictsClient,
        HttpUrl wikiMediaToolforgeUrl,
        String sparqlQueryUrl,
        String campaignsUrl,
        Gson gson) {
        this.okHttpClient = okHttpClient;
        this.depictsClient = depictsClient;
        this.wikiMediaToolforgeUrl = wikiMediaToolforgeUrl;
        this.sparqlQueryUrl = sparqlQueryUrl;
        this.campaignsUrl = campaignsUrl;
        this.gson = gson;
    }

    /**
     * The method will gradually calls the leaderboard API and fetches the leaderboard
     *
     * @param userName username of leaderboard user
     * @param duration duration for leaderboard
     * @param category category for leaderboard
     * @param limit    page size limit for list
     * @param offset   offset for the list
     * @return LeaderboardResponse object
     */
    @NonNull
    public Observable<LeaderboardResponse> getLeaderboard(String userName, String duration,
        String category, String limit, String offset) {
        final String fetchLeaderboardUrlTemplate = wikiMediaToolforgeUrl
            + LEADERBOARD_END_POINT;
        String url = String.format(Locale.ENGLISH,
            fetchLeaderboardUrlTemplate,
            userName,
            duration,
            category,
            limit,
            offset);
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        urlBuilder.addQueryParameter("user", userName);
        urlBuilder.addQueryParameter("duration", duration);
        urlBuilder.addQueryParameter("category", category);
        urlBuilder.addQueryParameter("limit", limit);
        urlBuilder.addQueryParameter("offset", offset);
        Timber.i("Url %s", urlBuilder.toString());
        Request request = new Request.Builder()
            .url(urlBuilder.toString())
            .build();
        return Observable.fromCallable(() -> {
            Response response = okHttpClient.newCall(request).execute();
            if (response != null && response.body() != null && response.isSuccessful()) {
                String json = response.body().string();
                if (json == null) {
                    return new LeaderboardResponse();
                }
                Timber.d("Response for leaderboard is %s", json);
                try {
                    return gson.fromJson(json, LeaderboardResponse.class);
                } catch (Exception e) {
                    return new LeaderboardResponse();
                }
            }
            return new LeaderboardResponse();
        });
    }

    /**
     * This method will update the leaderboard user avatar
     *
     * @param username username to update
     * @param avatar   url of the new avatar
     * @return UpdateAvatarResponse object
     */
    @NonNull
    public Single<UpdateAvatarResponse> setAvatar(String username, String avatar) {
        final String urlTemplate = wikiMediaToolforgeUrl
            + UPDATE_AVATAR_END_POINT;
        return Single.fromCallable(() -> {
            String url = String.format(Locale.ENGLISH,
                urlTemplate,
                username,
                avatar);
            HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
            urlBuilder.addQueryParameter("user", username);
            urlBuilder.addQueryParameter("avatar", avatar);
            Timber.i("Url %s", urlBuilder.toString());
            Request request = new Request.Builder()
                .url(urlBuilder.toString())
                .build();
            Response response = okHttpClient.newCall(request).execute();
            if (response != null && response.body() != null && response.isSuccessful()) {
                String json = response.body().string();
                if (json == null) {
                    return null;
                }
                try {
                    return gson.fromJson(json, UpdateAvatarResponse.class);
                } catch (Exception e) {
                    return new UpdateAvatarResponse();
                }
            }
            return null;
        });
    }

    @NonNull
    public Single<Integer> getUploadCount(String userName) {
        HttpUrl.Builder urlBuilder = wikiMediaToolforgeUrl.newBuilder();
        urlBuilder
            .addPathSegments("uploadsbyuser.py")
            .addQueryParameter("user", userName);

        if (ConfigUtils.isBetaFlavour()) {
            urlBuilder.addQueryParameter("labs", "commonswiki");
        }

        Request request = new Request.Builder()
            .url(urlBuilder.build())
            .build();

        return Single.fromCallable(() -> {
            Response response = okHttpClient.newCall(request).execute();
            if (response != null && response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                if (null != responseBody) {
                    String responseBodyString = responseBody.string().trim();
                    if (!TextUtils.isEmpty(responseBodyString)) {
                        try {
                            return Integer.parseInt(responseBodyString);
                        } catch (NumberFormatException e) {
                            Timber.e(e);
                        }
                    }
                }
            }
            return 0;
        });
    }

    @NonNull
    public Single<Integer> getWikidataEdits(String userName) {
        HttpUrl.Builder urlBuilder = wikiMediaToolforgeUrl.newBuilder();
        urlBuilder
            .addPathSegments("wikidataedits.py")
            .addQueryParameter("user", userName);

        if (ConfigUtils.isBetaFlavour()) {
            urlBuilder.addQueryParameter("labs", "commonswiki");
        }

        Request request = new Request.Builder()
            .url(urlBuilder.build())
            .build();

        return Single.fromCallable(() -> {
            Response response = okHttpClient.newCall(request).execute();
            if (response != null &&
                response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                if (json == null) {
                    return 0;
                }
                // Extract JSON from response
                json = json.substring(json.indexOf('{'));
                GetWikidataEditCountResponse countResponse = gson
                    .fromJson(json, GetWikidataEditCountResponse.class);
                if (null != countResponse) {
                    return countResponse.getWikidataEditCount();
                }
            }
            return 0;
        });
    }

    /**
     * This takes userName as input, which is then used to fetch the feedback/achievements
     * statistics using OkHttp and JavaRx. This function return JSONObject
     *
     * @param userName MediaWiki user name
     * @return
     */
    public Single<FeedbackResponse> getAchievements(String userName) {
        final String fetchAchievementUrlTemplate =
            wikiMediaToolforgeUrl + (ConfigUtils.isBetaFlavour() ? "/feedback.py?labs=commonswiki"
                : "/feedback.py");
        return Single.fromCallable(() -> {
            String url = String.format(
                Locale.ENGLISH,
                fetchAchievementUrlTemplate,
                userName);
            HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
            urlBuilder.addQueryParameter("user", userName);
            Request request = new Request.Builder()
                .url(urlBuilder.toString())
                .build();
            Response response = okHttpClient.newCall(request).execute();
            if (response != null && response.body() != null && response.isSuccessful()) {
                String json = response.body().string();
                if (json == null) {
                    return null;
                }
                // Extract JSON from response
                json = json.substring(json.indexOf('{'));
                Timber.d("Response for achievements is %s", json);
                try {
                    return gson.fromJson(json, FeedbackResponse.class);
                } catch (Exception e) {
                    return new FeedbackResponse(0, 0, 0, new FeaturedImages(0, 0), 0, "");
                }


            }
            return null;
        });
    }

    /**
     * Make API Call to get Nearby Places
     *
     * @param cur      Search lat long
     * @param language Language
     * @param radius   Search Radius
     * @return
     * @throws Exception
     */
    @Nullable
    public List<Place> getNearbyPlaces(final LatLng cur, final String language, final double radius,
        final String customQuery)
        throws Exception {

        Timber.d("Fetching nearby items at radius %s", radius);
        Timber.d("CUSTOM_SPARQL%s", String.valueOf(customQuery != null));
        final String wikidataQuery;
        if (customQuery != null) {
            wikidataQuery = customQuery;
        } else {
            wikidataQuery = FileUtils.readFromResource(
                "/queries/radius_query_for_upload_wizard.rq");
        }
        final String query = wikidataQuery
            .replace("${RAD}", String.format(Locale.ROOT, "%.2f", radius))
            .replace("${LAT}", String.format(Locale.ROOT, "%.4f", cur.getLatitude()))
            .replace("${LONG}", String.format(Locale.ROOT, "%.4f", cur.getLongitude()))
            .replace("${LANG}", language);

        final HttpUrl.Builder urlBuilder = HttpUrl
            .parse(sparqlQueryUrl)
            .newBuilder()
            .addQueryParameter("query", query)
            .addQueryParameter("format", "json");

        final Request request = new Request.Builder()
            .url(urlBuilder.build())
            .build();

        final Response response = okHttpClient.newCall(request).execute();
        if (response.body() != null && response.isSuccessful()) {
            final String json = response.body().string();
            final NearbyResponse nearbyResponse = gson.fromJson(json, NearbyResponse.class);
            final List<NearbyResultItem> bindings = nearbyResponse.getResults().getBindings();
            final List<Place> places = new ArrayList<>();
            for (final NearbyResultItem item : bindings) {
                final Place placeFromNearbyItem = Place.from(item);
                placeFromNearbyItem.setMonument(false);
                places.add(placeFromNearbyItem);
            }
            return places;
        }
        throw new Exception(response.message());
    }

    /**
     * Retrieves nearby places based on screen coordinates and optional query parameters.
     *
     * @param screenTopRight          The top right corner of the screen (latitude, longitude).
     * @param screenBottomLeft        The bottom left corner of the screen (latitude, longitude).
     * @param language                The language for the query.
     * @param shouldQueryForMonuments Flag indicating whether to include monuments in the query.
     * @param customQuery             Optional custom SPARQL query to use instead of default
     *                                queries.
     * @return A list of nearby places.
     * @throws Exception If an error occurs during the retrieval process.
     */
    @Nullable
    public List<Place> getNearbyPlaces(
        final fr.free.nrw.commons.location.LatLng screenTopRight,
        final fr.free.nrw.commons.location.LatLng screenBottomLeft, final String language,
        final boolean shouldQueryForMonuments, final String customQuery)
        throws Exception {

        Timber.d("CUSTOM_SPARQL%s", String.valueOf(customQuery != null));

        final String wikidataQuery;
        if (customQuery != null) {
            wikidataQuery = customQuery;
        } else if (!shouldQueryForMonuments) {
            wikidataQuery = FileUtils.readFromResource("/queries/rectangle_query_for_nearby.rq");
        } else {
            wikidataQuery = FileUtils.readFromResource(
                "/queries/rectangle_query_for_nearby_monuments.rq");
        }

        final double westCornerLat = screenTopRight.getLatitude();
        final double westCornerLong = screenTopRight.getLongitude();
        final double eastCornerLat = screenBottomLeft.getLatitude();
        final double eastCornerLong = screenBottomLeft.getLongitude();

        final String query = wikidataQuery
            .replace("${LAT_WEST}", String.format(Locale.ROOT, "%.4f", westCornerLat))
            .replace("${LONG_WEST}", String.format(Locale.ROOT, "%.4f", westCornerLong))
            .replace("${LAT_EAST}", String.format(Locale.ROOT, "%.4f", eastCornerLat))
            .replace("${LONG_EAST}", String.format(Locale.ROOT, "%.4f", eastCornerLong))
            .replace("${LANG}", language);
        final HttpUrl.Builder urlBuilder = HttpUrl
            .parse(sparqlQueryUrl)
            .newBuilder()
            .addQueryParameter("query", query)
            .addQueryParameter("format", "json");

        final Request request = new Request.Builder()
            .url(urlBuilder.build())
            .build();

        final Response response = okHttpClient.newCall(request).execute();
        if (response.body() != null && response.isSuccessful()) {
            final String json = response.body().string();
            final NearbyResponse nearbyResponse = gson.fromJson(json, NearbyResponse.class);
            final List<NearbyResultItem> bindings = nearbyResponse.getResults().getBindings();
            final List<Place> places = new ArrayList<>();
            for (final NearbyResultItem item : bindings) {
                final Place placeFromNearbyItem = Place.from(item);
                if (shouldQueryForMonuments && item.getMonument() != null) {
                    placeFromNearbyItem.setMonument(true);
                } else {
                    placeFromNearbyItem.setMonument(false);
                }
                places.add(placeFromNearbyItem);
            }
            return places;
        }
        throw new Exception(response.message());
    }

    /**
     * Make API Call to get Places
     *
     * @param leftLatLng  Left lat long
     * @param rightLatLng Right lat long
     * @return
     * @throws Exception
     */
    @Nullable
    public String getPlacesAsKML(final LatLng leftLatLng, final LatLng rightLatLng)
        throws Exception {
        String kmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
            "<!--Created by Wikimedia Commons Android app -->\n" +
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
            "    <Document>";

        int increment = 1;
        double longitude = leftLatLng.getLongitude();

        while (longitude <= rightLatLng.getLongitude()) {
            double NEXT_LONGITUDE =
                (increment + longitude) >= 0.0 && (increment + longitude) <= 1.0 ? 0.0
                    : increment + longitude;

            double latitude = leftLatLng.getLatitude();

            while (latitude <= rightLatLng.getLatitude()) {
                double NEXT_LATITUDE =
                    (increment + latitude) >= 0.0 && (increment + latitude) <= 1.0 ? 0.0
                        : increment + latitude;
                List<PlaceBindings> placeBindings = runQuery(new LatLng(latitude, longitude, 0),
                    new LatLng(NEXT_LATITUDE, NEXT_LONGITUDE, 0));
                if (placeBindings != null) {
                    for (PlaceBindings item : placeBindings) {
                        if (item.getItem() != null && item.getLabel() != null && item.getClas() != null) {
                            String input = item.getLocation().getValue();
                            Pattern pattern = Pattern.compile(
                                "Point\\(([-+]?[0-9]*\\.?[0-9]+) ([-+]?[0-9]*\\.?[0-9]+)\\)");
                            Matcher matcher = pattern.matcher(input);

                            if (matcher.find()) {
                                String longStr = matcher.group(1);
                                String latStr = matcher.group(2);
                                String itemUrl = item.getItem().getValue();
                                String itemName = item.getLabel().getValue().replace("&", "&amp;");
                                String itemLatitude = latStr;
                                String itemLongitude = longStr;
                                String itemClass = item.getClas().getValue();

                                String formattedItemName =
                                    !itemClass.isEmpty() ? itemName + " (" + itemClass + ")"
                                        : itemName;

                                String kmlEntry = "\n        <Placemark>\n" +
                                    "            <name>" + formattedItemName + "</name>\n" +
                                    "            <description>" + itemUrl + "</description>\n" +
                                    "            <Point>\n" +
                                    "                <coordinates>" + itemLongitude + ","
                                    + itemLatitude
                                    + "</coordinates>\n" +
                                    "            </Point>\n" +
                                    "        </Placemark>";
                                kmlString = kmlString + kmlEntry;
                            } else {
                                Timber.e("No match found");
                            }
                        }
                    }
                }
                latitude += increment;
            }
            longitude += increment;
        }
        kmlString = kmlString + "\n    </Document>\n" +
            "</kml>\n";
        return kmlString;
    }

    /**
     * Make API Call to get Places
     *
     * @param leftLatLng  Left lat long
     * @param rightLatLng Right lat long
     * @return
     * @throws Exception
     */
    @Nullable
    public String getPlacesAsGPX(final LatLng leftLatLng, final LatLng rightLatLng)
        throws Exception {
        String gpxString = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
            "<gpx\n" +
            " version=\"1.0\"\n" +
            " creator=\"Wikimedia Commons Android app\"\n" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            " xmlns=\"http://www.topografix.com/GPX/1/0\"\n" +
            " xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">"
            + "\n<bounds minlat=\"$MIN_LATITUDE\" minlon=\"$MIN_LONGITUDE\" maxlat=\"$MAX_LATITUDE\" maxlon=\"$MAX_LONGITUDE\"/>";

        int increment = 1;
        double longitude = leftLatLng.getLongitude();

        while (longitude <= rightLatLng.getLongitude()) {
            double NEXT_LONGITUDE =
                (increment + longitude) >= 0.0 && (increment + longitude) <= 1.0 ? 0.0
                    : increment + longitude;

            double latitude = leftLatLng.getLatitude();

            while (latitude <= rightLatLng.getLatitude()) {
                double NEXT_LATITUDE =
                    (increment + latitude) >= 0.0 && (increment + latitude) <= 1.0 ? 0.0
                        : increment + latitude;
                List<PlaceBindings> placeBindings = runQuery(new LatLng(latitude, longitude, 0),
                    new LatLng(NEXT_LATITUDE, NEXT_LONGITUDE, 0));
                if (placeBindings != null) {
                    for (PlaceBindings item : placeBindings) {
                        if (item.getItem() != null && item.getLabel() != null && item.getClas() != null) {
                            String input = item.getLocation().getValue();
                            Pattern pattern = Pattern.compile(
                                "Point\\(([-+]?[0-9]*\\.?[0-9]+) ([-+]?[0-9]*\\.?[0-9]+)\\)");
                            Matcher matcher = pattern.matcher(input);

                            if (matcher.find()) {
                                String longStr = matcher.group(1);
                                String latStr = matcher.group(2);
                                String itemUrl = item.getItem().getValue();
                                String itemName = item.getLabel().getValue().replace("&", "&amp;");
                                String itemLatitude = latStr;
                                String itemLongitude = longStr;
                                String itemClass = item.getClas().getValue();

                                String formattedItemName =
                                    !itemClass.isEmpty() ? itemName + " (" + itemClass + ")"
                                        : itemName;

                                String gpxEntry =
                                    "\n    <wpt lat=\"" + itemLatitude + "\" lon=\"" + itemLongitude
                                        + "\">\n" +
                                        "        <name>" + itemName + "</name>\n" +
                                        "        <url>" + itemUrl + "</url>\n" +
                                        "    </wpt>";
                                gpxString = gpxString + gpxEntry;

                            } else {
                                Timber.e("No match found");
                            }
                        }
                    }
                }
                latitude += increment;
            }
            longitude += increment;
        }
        gpxString = gpxString + "\n</gpx>";
        return gpxString;
    }

    private List<PlaceBindings> runQuery(final LatLng currentLatLng, final LatLng nextLatLng)
        throws IOException {

        final String wikidataQuery = FileUtils.readFromResource("/queries/places_query.rq");
        final String query = wikidataQuery
            .replace("${LONGITUDE}",
                String.format(Locale.ROOT, "%.2f", currentLatLng.getLongitude()))
            .replace("${LATITUDE}", String.format(Locale.ROOT, "%.4f", currentLatLng.getLatitude()))
            .replace("${NEXT_LONGITUDE}",
                String.format(Locale.ROOT, "%.4f", nextLatLng.getLongitude()))
            .replace("${NEXT_LATITUDE}",
                String.format(Locale.ROOT, "%.4f", nextLatLng.getLatitude()));

        final HttpUrl.Builder urlBuilder = HttpUrl
            .parse(sparqlQueryUrl)
            .newBuilder()
            .addQueryParameter("query", query)
            .addQueryParameter("format", "json");

        final Request request = new Request.Builder()
            .url(urlBuilder.build())
            .build();

        final Response response = okHttpClient.newCall(request).execute();
        if (response.body() != null && response.isSuccessful()) {
            final String json = response.body().string();
            final ItemsClass item = gson.fromJson(json, ItemsClass.class);
            return item.getResults().getBindings();
        } else {
            return null;
        }
    }

    /**
     * Make API Call to get Nearby Places Implementation does not expects a custom query
     *
     * @param cur      Search lat long
     * @param language Language
     * @param radius   Search Radius
     * @return
     * @throws Exception
     */
    @Nullable
    public List<Place> getNearbyPlaces(final LatLng cur, final String language, final double radius)
        throws Exception {
        return getNearbyPlaces(cur, language, radius, null);
    }

    /**
     * Get the QIDs of all Wikidata items that are subclasses of the given Wikidata item. Example:
     * bridge -> suspended bridge, aqueduct, etc
     */
    public Single<List<DepictedItem>> getChildDepictions(String qid, int startPosition,
        int limit) throws IOException {
        return depictedItemsFrom(
            sparqlQuery(qid, startPosition, limit, "/queries/subclasses_query.rq"));
    }

    /**
     * Get the QIDs of all Wikidata items that are subclasses of the given Wikidata item. Example:
     * bridge -> suspended bridge, aqueduct, etc
     */
    public Single<List<DepictedItem>> getParentDepictions(String qid, int startPosition,
        int limit) throws IOException {
        return depictedItemsFrom(sparqlQuery(qid, startPosition, limit,
            "/queries/parentclasses_query.rq"));
    }

    private Single<List<DepictedItem>> depictedItemsFrom(Request request) {
        return depictsClient.toDepictions(Single.fromCallable(() -> {
            try (ResponseBody body = okHttpClient.newCall(request).execute().body()) {
                return gson.fromJson(body.string(), SparqlResponse.class);
            }
        }).doOnError(Timber::e));
    }

    @NotNull
    private Request sparqlQuery(String qid, int startPosition, int limit, String fileName)
        throws IOException {
        String query = FileUtils.readFromResource(fileName)
            .replace("${QID}", qid)
            .replace("${LANG}", "\"" + Locale.getDefault().getLanguage() + "\"")
            .replace("${LIMIT}", "" + limit)
            .replace("${OFFSET}", "" + startPosition);
        HttpUrl.Builder urlBuilder = HttpUrl
            .parse(sparqlQueryUrl)
            .newBuilder()
            .addQueryParameter("query", query)
            .addQueryParameter("format", "json");
        return new Request.Builder()
            .url(urlBuilder.build())
            .build();
    }

    public Single<CampaignResponseDTO> getCampaigns() {
        return Single.fromCallable(() -> {
            Request request = new Request.Builder().url(campaignsUrl)
                .build();
            Response response = okHttpClient.newCall(request).execute();
            if (response != null && response.body() != null && response.isSuccessful()) {
                String json = response.body().string();
                if (json == null) {
                    return null;
                }
                return gson.fromJson(json, CampaignResponseDTO.class);
            }
            return null;
        });
    }
}
