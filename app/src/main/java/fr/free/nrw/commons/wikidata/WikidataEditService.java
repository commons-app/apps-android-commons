package fr.free.nrw.commons.wikidata;

import static fr.free.nrw.commons.depictions.Media.DepictedImagesFragment.PAGE_ID_PREFIX;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.Nullable;
import com.google.gson.Gson;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.upload.UploadResult;
import fr.free.nrw.commons.upload.WikidataItem;
import fr.free.nrw.commons.upload.WikidataPlace;
import fr.free.nrw.commons.utils.ConfigUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.wikipedia.dataclient.mwapi.MwPostResponse;
import org.wikipedia.wikidata.EditClaim;
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
    private final Gson gson;

  @Inject
    public WikidataEditService(final Context context,
      final WikidataEditListener wikidataEditListener,
      @Named("default_preferences") final JsonKvStore directKvStore,
      final WikiBaseClient wikiBaseClient,
      final WikidataClient wikidataClient, final Gson gson) {
        this.context = context;
        this.wikidataEditListener = wikidataEditListener;
        this.directKvStore = directKvStore;
        this.wikiBaseClient = wikiBaseClient;
        this.wikidataClient = wikidataClient;
    this.gson = gson;
  }

  /**
   * Edits the wikibase entity by adding DEPICTS property.
   * Adding DEPICTS property requires call to the wikibase API to set tag against the entity.
   */
  @SuppressLint("CheckResult")
  private Observable<Boolean> addDepictsProperty(final String fileEntityId,
      final WikidataItem depictedItem) {

    final EditClaim data = editClaim(
        ConfigUtils.isBetaFlavour() ? "Q10" // Wikipedia:Sandbox (Q10)
        : depictedItem.getId()
    );

    return wikiBaseClient.postEditEntity(PAGE_ID_PREFIX + fileEntityId, gson.toJson(data))
        .doOnNext(success -> {
          if (success) {
            Timber.d("DEPICTS property was set successfully for %s", fileEntityId);
          } else {
            Timber.d("Unable to set DEPICTS property for %s", fileEntityId);
          }
        })
        .doOnError( throwable -> {
          Timber.e(throwable, "Error occurred while setting DEPICTS property");
          ViewUtil.showLongToast(context, throwable.toString());
        })
        .subscribeOn(Schedulers.io());
    }

  private EditClaim editClaim(final String entityId) {
    return EditClaim.from(entityId, WikidataProperties.DEPICTS.getPropertyName());
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
     * Adds label to Wikidata using the fileEntityId and the edit token, obtained from csrfTokenClient
     *
     * @param fileEntityId
     * @return
     */

    @SuppressLint("CheckResult")
    private Observable<Boolean> addCaption(final long fileEntityId, final String languageCode,
        final String captionValue) {
      return wikiBaseClient.addLabelstoWikidata(fileEntityId, languageCode, captionValue)
          .doOnNext(mwPostResponse ->  onAddCaptionResponse(fileEntityId, mwPostResponse) )
          .doOnError(throwable -> {
            Timber.e(throwable, "Error occurred while setting Captions");
            ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
          })
          .map(mwPostResponse -> mwPostResponse != null);
    }

  private void onAddCaptionResponse(Long fileEntityId, MwPostResponse response) {
    if (response != null) {
      Timber.d("Caption successfully set, revision id = %s", response);
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

  public Disposable addDepictionsAndCaptions(UploadResult uploadResult, Contribution contribution) {
    return wikiBaseClient.getFileEntityId(uploadResult)
        .doOnError(throwable -> {
          Timber.e(throwable, "Error occurred while getting EntityID to set DEPICTS property");
          ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
        })
        .subscribeOn(Schedulers.io())
        .switchMap(fileEntityId -> {
              if (fileEntityId != null) {
                Timber.d("EntityId for image was received successfully: %s", fileEntityId);
                return Observable.concat(
                    depictionEdits(contribution, fileEntityId),
                    captionEdits(contribution, fileEntityId)
                );
              } else {
                Timber.d("Error acquiring EntityId for image: %s", uploadResult);
                return Observable.empty();
              }
            }
        ).subscribe(
        success -> Timber.d("edit response: %s", success),
        throwable -> Timber.e(throwable, "posting edits failed")
    );
  }

  private Observable<Boolean> captionEdits(Contribution contribution, Long fileEntityId) {
    return Observable.fromIterable(contribution.getCaptions().entrySet())
        .concatMap(entry -> addCaption(fileEntityId, entry.getKey(), entry.getValue()));
  }

  private Observable<Boolean> depictionEdits(Contribution contribution, Long fileEntityId) {
    final ArrayList<WikidataItem> depictedItems = new ArrayList<>(contribution.getDepictedItems());
    final WikidataPlace wikidataPlace = contribution.getWikidataPlace();
    if (wikidataPlace != null) {
      depictedItems.add(wikidataPlace);
    }
    return Observable.fromIterable(depictedItems)
        .concatMap( wikidataItem -> addDepictsProperty(fileEntityId.toString(), wikidataItem));
  }
}
