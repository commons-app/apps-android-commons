package fr.free.nrw.commons.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.util.DateUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.achievements.FeaturedImages;
import fr.free.nrw.commons.achievements.FeedbackResponse;
import fr.free.nrw.commons.campaigns.CampaignResponseDTO;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.model.NearbyResponse;
import fr.free.nrw.commons.nearby.model.NearbyResultItem;
import fr.free.nrw.commons.upload.FileUtils;
import fr.free.nrw.commons.wikidata.model.GetWikidataEditCountResponse;
import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

@Singleton
public class OkHttpJsonApiClient {
    private final OkHttpClient okHttpClient;
    private final HttpUrl wikiMediaToolforgeUrl;
    private final String sparqlQueryUrl;
    private final String campaignsUrl;
    private final String commonsBaseUrl;
    private Gson gson;


    @Inject
    public OkHttpJsonApiClient(OkHttpClient okHttpClient,
                               HttpUrl wikiMediaToolforgeUrl,
                               String sparqlQueryUrl,
                               String campaignsUrl,
                               String commonsBaseUrl,
                               Gson gson) {
        this.okHttpClient = okHttpClient;
        this.wikiMediaToolforgeUrl = wikiMediaToolforgeUrl;
        this.sparqlQueryUrl = sparqlQueryUrl;
        this.campaignsUrl = campaignsUrl;
        this.commonsBaseUrl = commonsBaseUrl;
        this.gson = gson;
    }

    @NonNull
    public Single<Integer> getUploadCount(String userName) {
        HttpUrl.Builder urlBuilder = wikiMediaToolforgeUrl.newBuilder();
        urlBuilder
                .addPathSegments("/uploadsbyuser.py")
                .addQueryParameter("user", userName);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        return Single.fromCallable(() -> {
            Response response = okHttpClient.newCall(request).execute();
            if (response != null && response.isSuccessful()) {

                return Integer.parseInt(response.body().string().trim());
            }
            return 0;
        });
    }

    @NonNull
    public Single<Integer> getWikidataEdits(String userName) {
        HttpUrl.Builder urlBuilder = wikiMediaToolforgeUrl.newBuilder();
        urlBuilder
                .addPathSegments("/wikidataedits.py")
                .addQueryParameter("user", userName);
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
                GetWikidataEditCountResponse countResponse = gson.fromJson(json, GetWikidataEditCountResponse.class);
                return countResponse.getWikidataEditCount();
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
                wikiMediaToolforgeUrl + "/feedback.py";
        return Single.fromCallable(() -> {
            String url = String.format(
                    Locale.ENGLISH,
                    fetchAchievementUrlTemplate,
                    userName);
            HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
            urlBuilder.addQueryParameter("user", userName);
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
                Timber.d("Response for achievements is %s", json);
                try {
                    return gson.fromJson(json, FeedbackResponse.class);
                } catch (Exception e) {
                    return new FeedbackResponse("", 0, 0, 0, new FeaturedImages(0, 0), 0, "", 0);
                }


            }
            return null;
        });
    }

    public Observable<List<Place>> getNearbyPlaces(LatLng cur, String lang, double radius) throws IOException {
        String wikidataQuery = FileUtils.readFromResource("/queries/nearby_query.rq");
        String query = wikidataQuery
                .replace("${RAD}", String.format(Locale.ROOT, "%.2f", radius))
                .replace("${LAT}", String.format(Locale.ROOT, "%.4f", cur.getLatitude()))
                .replace("${LONG}", String.format(Locale.ROOT, "%.4f", cur.getLongitude()))
                .replace("${LANG}", lang);

        HttpUrl.Builder urlBuilder = HttpUrl
                .parse(sparqlQueryUrl)
                .newBuilder()
                .addQueryParameter("query", query)
                .addQueryParameter("format", "json");

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        return Observable.fromCallable(() -> {
            Response response = okHttpClient.newCall(request).execute();
            if (response != null && response.body() != null && response.isSuccessful()) {
                String json = response.body().string();
                if (json == null) {
                    return new ArrayList<>();
                }
                NearbyResponse nearbyResponse = gson.fromJson(json, NearbyResponse.class);
                List<NearbyResultItem> bindings = nearbyResponse.getResults().getBindings();
                List<Place> places = new ArrayList<>();
                for (NearbyResultItem item : bindings) {
                    places.add(Place.from(item));
                }
                return places;
            }
            return new ArrayList<>();
        });
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

    /**
     * The method returns the picture of the day
     *
     * @return Media object corresponding to the picture of the day
     */
    @Nullable
    public Single<Media> getPictureOfTheDay() {
        String template = "Template:Potd/" + DateUtil.getIso8601DateFormatShort().format(new Date());
        HttpUrl.Builder urlBuilder = HttpUrl
                .parse(commonsBaseUrl)
                .newBuilder()
                .addQueryParameter("action", "query")
                .addQueryParameter("generator", "images")
                .addQueryParameter("format", "json")
                .addQueryParameter("titles", template)
                .addQueryParameter("prop", "imageinfo")
                .addQueryParameter("iiprop", "url|extmetadata");

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        return Single.fromCallable(() -> {
            Response response = okHttpClient.newCall(request).execute();
            if (response != null && response.body() != null && response.isSuccessful()) {
                String json = response.body().string();
                if (json == null) {
                    return null;
                }
                MwQueryResponse mwQueryPage = gson.fromJson(json, MwQueryResponse.class);
                return Media.from(mwQueryPage.query().firstPage());
            }
            return null;
        });
    }

    /**
     * This method takes search keyword as input and returns a list of  Media objects filtered using search query
     * It uses the generator query API to get the images searched using a query, 25 at a time.
     * @param query keyword to search images on commons
     * @return
     */
    @Nullable
    public Single<List<Media>> searchImages(String query, int offset) {
        HttpUrl.Builder urlBuilder = HttpUrl
                .parse(commonsBaseUrl)
                .newBuilder()
                .addQueryParameter("action", "query")
                .addQueryParameter("generator", "search")
                .addQueryParameter("format", "json")
                .addQueryParameter("gsrwhat", "text")
                .addQueryParameter("gsrnamespace", "6")
                .addQueryParameter("gsrlimit", "25")
                .addQueryParameter("gsroffset", String.valueOf(offset))
                .addQueryParameter("gsrsearch", query)
                .addQueryParameter("prop", "imageinfo")
                .addQueryParameter("iiprop", "url|extmetadata");

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        return Single.fromCallable(() -> {
            Response response = okHttpClient.newCall(request).execute();
            List<Media> mediaList = new ArrayList<>();
            if (response.body() != null && response.isSuccessful()) {
                String json = response.body().string();
                MwQueryResponse mwQueryResponse = gson.fromJson(json, MwQueryResponse.class);
                if (mwQueryResponse.query() == null || mwQueryResponse.query().pages() == null) {
                    return mediaList;
                }
                for (MwQueryPage page : mwQueryResponse.query().pages()) {
                    mediaList.add(Media.from(page));
                }
            }
            return mediaList;
        });
    }
}
