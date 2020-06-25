package fr.free.nrw.commons.mwapi;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.google.gson.Gson;
import fr.free.nrw.commons.achievements.FeaturedImages;
import fr.free.nrw.commons.achievements.FeedbackResponse;
import fr.free.nrw.commons.campaigns.CampaignResponseDTO;
import fr.free.nrw.commons.explore.depictions.DepictsClient;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.model.NearbyResponse;
import fr.free.nrw.commons.nearby.model.NearbyResultItem;
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
   * This takes userName as input, which is then used to fetch the feedback/achievements statistics
   * using OkHttp and JavaRx. This function return JSONObject
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
          return new FeedbackResponse(0, 0, 0, new FeaturedImages(0, 0), 0, "");
        }


      }
      return null;
    });
  }

    public Observable<List<Place>> getNearbyPlaces(LatLng cur, String language, double radius) throws IOException {
        String wikidataQuery = FileUtils.readFromResource("/queries/nearby_query.rq");
        String query = wikidataQuery
                .replace("${RAD}", String.format(Locale.ROOT, "%.2f", radius))
                .replace("${LAT}", String.format(Locale.ROOT, "%.4f", cur.getLatitude()))
                .replace("${LONG}", String.format(Locale.ROOT, "%.4f", cur.getLongitude()))
                .replace("${LANG}", language);

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

  /**
   * Get the QIDs of all Wikidata items that are subclasses of the given Wikidata item. Example:
   * bridge -> suspended bridge, aqueduct, etc
   */
  public Observable<List<DepictedItem>> getChildDepictions(String qid, int startPosition,
      int limit) throws IOException {
    return depictedItemsFrom(sparqlQuery(qid, startPosition, limit,"/queries/subclasses_query.rq"));
  }

  /**
   * Get the QIDs of all Wikidata items that are subclasses of the given Wikidata item. Example:
   * bridge -> suspended bridge, aqueduct, etc
   */
  public Observable<List<DepictedItem>> getParentDepictions(String qid, int startPosition,
      int limit) throws IOException {
    return depictedItemsFrom(sparqlQuery(qid, startPosition, limit,
        "/queries/parentclasses_query.rq"));
  }

  private Observable<List<DepictedItem>> depictedItemsFrom(Request request) {
    return depictsClient.toDepictions(Observable.fromCallable(() -> {
      try (ResponseBody body = okHttpClient.newCall(request).execute().body()) {
        return gson.fromJson(body.string(), SparqlResponse.class);
      }
    }).doOnError(Timber::e));
  }

  @NotNull
  private Request sparqlQuery(String qid, int startPosition, int limit, String fileName) throws IOException {
    String query = FileUtils.readFromResource(fileName)
        .replace("${QID}", qid)
        .replace("${LANG}", "\"" + Locale.getDefault().getLanguage() + "\"")
        .replace("${LIMIT}",""+ limit)
        .replace("${OFFSET}",""+ startPosition);
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
