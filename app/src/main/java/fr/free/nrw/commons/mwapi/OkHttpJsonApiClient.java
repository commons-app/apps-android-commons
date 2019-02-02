package fr.free.nrw.commons.mwapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.PageTitle;
import fr.free.nrw.commons.achievements.FeaturedImages;
import fr.free.nrw.commons.achievements.FeedbackResponse;
import fr.free.nrw.commons.campaigns.CampaignResponseDTO;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.mwapi.model.MwQueryResponse;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.model.NearbyResponse;
import fr.free.nrw.commons.nearby.model.NearbyResultItem;
import fr.free.nrw.commons.upload.FileUtils;
import fr.free.nrw.commons.utils.DateUtils;
import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

@Singleton
public class OkHttpJsonApiClient {
    private static final String WIKIDATA_SPARQL_QUERY_URL = "https://query.wikidata.org/sparql";
    private final String WIKIMEDIA_CAMPAIGNS_BASE_URL =
            "https://raw.githubusercontent.com/commons-app/campaigns/master/campaigns.json";
    private final String commonsBaseUrl;

    private final OkHttpClient okHttpClient;
    private String wikiMediaToolforgeUrl = "https://tools.wmflabs.org/";
    private Gson gson;


    @Inject
    public OkHttpJsonApiClient(OkHttpClient okHttpClient,
                               String commonsBaseUrl,
                               Gson gson) {
        this.okHttpClient = okHttpClient;
        this.commonsBaseUrl = commonsBaseUrl;
        this.gson = gson;
    }

    @NonNull
    public Single<Integer> getUploadCount(String userName) {
        final String uploadCountUrlTemplate =
                wikiMediaToolforgeUrl + "urbanecmbot/commonsmisc/uploadsbyuser.py";

        HttpUrl.Builder urlBuilder = HttpUrl.parse(uploadCountUrlTemplate).newBuilder();
        urlBuilder.addQueryParameter("user", userName);
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

    /**
     * This takes userName as input, which is then used to fetch the feedback/achievements
     * statistics using OkHttp and JavaRx. This function return JSONObject
     *
     * @param userName MediaWiki user name
     * @return
     */
    public Single<FeedbackResponse> getAchievements(String userName) {
        final String fetchAchievementUrlTemplate =
                wikiMediaToolforgeUrl + "urbanecmbot/commonsmisc/feedback.py";
        return Single.fromCallable(() -> {
            String url = String.format(
                    Locale.ENGLISH,
                    fetchAchievementUrlTemplate,
                    new PageTitle(userName).getText());
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
                .parse(WIKIDATA_SPARQL_QUERY_URL)
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
            Request request = new Request.Builder().url(WIKIMEDIA_CAMPAIGNS_BASE_URL)
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
        String template = "Template:Potd/" + DateUtils.getCurrentDate();
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
}
