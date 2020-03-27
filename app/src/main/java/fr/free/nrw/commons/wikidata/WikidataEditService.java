package fr.free.nrw.commons.wikidata;

import static fr.free.nrw.commons.depictions.Media.DepictedImagesFragment.PAGE_ID_PREFIX;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.upload.UploadResult;
import fr.free.nrw.commons.upload.WikidataItem;
import fr.free.nrw.commons.upload.WikidataPlace;
import fr.free.nrw.commons.utils.ConfigUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.wikipedia.dataclient.mwapi.MwPostResponse;
import timber.log.Timber;

/**
 * This class is meant to handle the Wikidata edits made through the app
 * It will talk with MediaWiki Apis to make the necessary calls, log the edits and fire listeners
 * on successful edits
 */
@Singleton
public class WikidataEditService {

    private static final String COMMONS_APP_TAG = "wikimedia-commons-app";
    private static final String COMMONS_APP_EDIT_REASON = "Add tag for edits made using Android Commons app";

    private final Context context;
    private final WikidataEditListener wikidataEditListener;
    private final JsonKvStore directKvStore;
  private final WikiBaseClient wikiBaseClient;
    private final WikidataClient wikidataClient;

  @Inject
    public WikidataEditService(final Context context,
      final WikidataEditListener wikidataEditListener,
      @Named("default_preferences") final JsonKvStore directKvStore,
      final WikiBaseClient wikiBaseClient,
      final WikidataClient wikidataClient) {
        this.context = context;
        this.wikidataEditListener = wikidataEditListener;
        this.directKvStore = directKvStore;
        this.wikiBaseClient = wikiBaseClient;
        this.wikidataClient = wikidataClient;
  }

    /**
     * Edits the wikibase entity by adding DEPICTS property.
     * Adding DEPICTS property requires call to the wikibase API to set tag against the entity.
     * @param uploadResult
     * @param depictedItem
     */
    @SuppressLint("CheckResult")
    private void editWikidataDepictsProperty(final UploadResult uploadResult, final WikidataItem depictedItem) {
        wikiBaseClient.getFileEntityId(uploadResult)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fileEntityId -> {
                    if (fileEntityId != null) {
                        Timber.d("EntityId for image was received successfully: %s", fileEntityId);
                        addDepictsProperty(fileEntityId.toString(), depictedItem);
                    } else {
                        Timber.d("Error acquiring EntityId for image: %s", uploadResult);
                    }
                    }, throwable -> {
                    Timber.e(throwable, "Error occurred while getting EntityID to set DEPICTS property");
                    ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
                });
    }

    @SuppressLint("CheckResult")
    private void addDepictsProperty(final String fileEntityId, final WikidataItem depictedItem) {
      // Wikipedia:Sandbox (Q10)
      final String data = depictionJson(ConfigUtils.isBetaFlavour() ?"Q10" : depictedItem.getId());

      Observable.defer((Callable<ObservableSource<Boolean>>) () ->
                wikiBaseClient.postEditEntity(PAGE_ID_PREFIX + fileEntityId, data))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                            if (success) {
                              Timber.d("DEPICTS property was set successfully for %s", fileEntityId);
                            } else {
                              Timber.d("Unable to set DEPICTS property for %s", fileEntityId);
                            }
                        },
                        throwable -> {
                            Timber.e(throwable, "Error occurred while setting DEPICTS property");
                            ViewUtil.showLongToast(context, throwable.toString());
                        });
    }

  @NotNull
  private String depictionJson(final String entityId) {
    final JsonObject value = new JsonObject();
    value.addProperty("entity-type", "item");
    value.addProperty("numeric-id", entityId.replace("Q", ""));
    value.addProperty("id", entityId);

    final JsonObject dataValue = new JsonObject();
    dataValue.add("value", value);
    dataValue.addProperty("type", "wikibase-entityid");

    final JsonObject mainSnak = new JsonObject();
    mainSnak.addProperty("snaktype", "value");
    mainSnak.addProperty("property", WikidataProperties.DEPICTS.getPropertyName());
    mainSnak.add("datavalue", dataValue);

    final JsonObject claim = new JsonObject();
    claim.add("mainsnak", mainSnak);
    claim.addProperty("type", "statement");
    claim.addProperty("rank", "preferred");

    final JsonArray claims = new JsonArray();
    claims.add(claim);

    final JsonObject jsonData = new JsonObject();
    jsonData.add("claims", claims);

    return jsonData.toString();
  }

  /**
     * Show a success toast when the edit is made successfully
     */
    private void showSuccessToast(final String wikiItemName) {
        final String successStringTemplate = context.getString(R.string.successful_wikidata_edit);
        final String successMessage = String.format(Locale.getDefault(), successStringTemplate, wikiItemName);
        ViewUtil.showLongToast(context, successMessage);
    }

    /**
     * Adding captions as labels after image is successfully uploaded
     */
    @SuppressLint("CheckResult")
    public void createCaptions(final UploadResult uploadResult, final Map<String, String> captions) {
        wikiBaseClient.getFileEntityId(uploadResult)
                .subscribeOn(Schedulers.io())
                .subscribe(fileEntityId -> {
                    if (fileEntityId != null) {
                        for (final Map.Entry<String, String> entry : captions.entrySet()) {
                          addCaption(fileEntityId, entry.getKey(), entry.getValue());
                        }
                    } else {
                        Timber.d("Error acquiring EntityId for image");
                    }
                }, throwable -> {
                    Timber.e(throwable, "Error occurred while getting EntityID for the file");
                    ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
                });
    }

    /**
     * Adds label to Wikidata using the fileEntityId and the edit token, obtained from csrfTokenClient
     *
     * @param fileEntityId
     */

    @SuppressLint("CheckResult")
    private void addCaption(final Long fileEntityId, final String languageCode,
        final String captionValue) {
      wikiBaseClient.addLabelstoWikidata(fileEntityId, languageCode, captionValue)
          .subscribe(mwPostResponse -> onAddCaptionResponse(fileEntityId, mwPostResponse),
              throwable -> {
                Timber.e(throwable, "Error occurred while setting Captions");
                ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
              }
          );
    }

  private void onAddCaptionResponse(Long fileEntityId, MwPostResponse revisionId) {
    if (revisionId != null) {
      Timber.d("Caption successfully set, revision id = %s", revisionId);
    } else {
      Timber.d("Error occurred while setting Captions, fileEntityId = %s", fileEntityId);
    }
  }

  public void createImageClaim(@Nullable final WikidataPlace wikidataPlace, final UploadResult imageUpload) {
    if (!(directKvStore.getBoolean("Picture_Has_Correct_Location", true))) {
      Timber.d("Image location and nearby place location mismatched, so Wikidata item won't be edited");
      return;
    }
    editWikidataImageProperty(wikidataPlace, imageUpload);
  }

  @SuppressLint("CheckResult")
  private void editWikidataImageProperty(final WikidataItem wikidataItem, final UploadResult imageUpload) {
    wikidataClient.createImageClaim(wikidataItem, String.format("\"%s\"", imageUpload.getFilename()))
        .flatMap(revisionId -> {
          if (revisionId != -1) {
            return wikidataClient.addEditTag(revisionId, COMMONS_APP_TAG, COMMONS_APP_EDIT_REASON);
          }
          throw new RuntimeException("Unable to edit wikidata item");
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(revisionId -> handleImageClaimResult(wikidataItem, String.valueOf(revisionId)), throwable -> {
          Timber.e(throwable, "Error occurred while making claim");
          ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
        });
  }

  private void handleImageClaimResult(final WikidataItem wikidataItem, final String revisionId) {
    if (revisionId != null) {
      if (wikidataEditListener != null) {
        wikidataEditListener.onSuccessfulWikidataEdit();
      }
      showSuccessToast(wikidataItem.getName());
    } else {
      Timber.d("Unable to make wiki data edit for entity %s", wikidataItem);
      ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
    }
  }

  public void createDepictsProperty(final UploadResult uploadResult, final WikidataItem depictedItem) {
    editWikidataDepictsProperty(uploadResult, depictedItem);
  }
}
