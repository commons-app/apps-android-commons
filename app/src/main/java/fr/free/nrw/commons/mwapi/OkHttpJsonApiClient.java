package fr.free.nrw.commons.mwapi;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.achievements.FeaturedImages;
import fr.free.nrw.commons.achievements.FeedbackResponse;
import fr.free.nrw.commons.campaigns.CampaignResponseDTO;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.model.NearbyResponse;
import fr.free.nrw.commons.nearby.model.NearbyResultItem;
import fr.free.nrw.commons.upload.FileUtils;
import fr.free.nrw.commons.utils.CommonsDateUtil;
import fr.free.nrw.commons.utils.ConfigUtils;
import fr.free.nrw.commons.wikidata.model.GetWikidataEditCountResponse;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import timber.log.Timber;

/**
 * Test methods in ok http api client
 */
@Singleton
public class OkHttpJsonApiClient {
    private static final String THUMB_SIZE = "640";

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
                GetWikidataEditCountResponse countResponse = gson.fromJson(json, GetWikidataEditCountResponse.class);
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
                wikiMediaToolforgeUrl + (ConfigUtils.isBetaFlavour() ? "/feedback.py?labs=commonswiki" : "/feedback.py");
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
        String date = CommonsDateUtil.getIso8601DateFormatShort().format(new Date());
        Timber.d("Current date is %s", date);
        String template = "Template:Potd/" + date;
        return getMedia(template, true);
    }

    /**
     * Fetches Media object from the imageInfo API
     *
     * @param titles       the tiles to be searched for. Can be filename or template name
     * @param useGenerator specifies if a image generator parameter needs to be passed or not
     * @return
     */
    public Single<Media> getMedia(String titles, boolean useGenerator) {
        HttpUrl.Builder urlBuilder = HttpUrl
                .parse(commonsBaseUrl)
                .newBuilder()
                .addQueryParameter("action", "query")
                .addQueryParameter("format", "json")
                .addQueryParameter("formatversion", "2")
                .addQueryParameter("titles", titles);

        if (useGenerator) {
            urlBuilder.addQueryParameter("generator", "images");
        }

        Request request = new Request.Builder()
                .url(appendMediaProperties(urlBuilder).build())
                .build();

        return Single.fromCallable(() -> {
            Response response = okHttpClient.newCall(request).execute();
            if (response.body() != null && response.isSuccessful()) {
                String json = response.body().string();
                MwQueryResponse mwQueryPage = gson.fromJson(json, MwQueryResponse.class);
                if (mwQueryPage.success() && mwQueryPage.query().firstPage() != null) {
                    return Media.from(mwQueryPage.query().firstPage());
                }
            }
            return null;
        });
    }



    /**
     * Whenever imageInfo is fetched, these common properties can be specified for the API call
     * https://www.mediawiki.org/wiki/API:Imageinfo
     *
     * @param builder
     * @return
     */
    private HttpUrl.Builder appendMediaProperties(HttpUrl.Builder builder) {
        builder.addQueryParameter("prop", "imageinfo")
                .addQueryParameter("iiprop", "url|extmetadata")
                .addQueryParameter("iiurlwidth", THUMB_SIZE)
                .addQueryParameter("iiextmetadatafilter", "DateTime|Categories|GPSLatitude|GPSLongitude|ImageDescription|DateTimeOriginal|Artist|LicenseShortName|LicenseUrl");

        String language = Locale.getDefault().getLanguage();
        if (!StringUtils.isBlank(language)) {
            builder.addQueryParameter("iiextmetadatalanguage", language);
        }

        return builder;
    }
}
