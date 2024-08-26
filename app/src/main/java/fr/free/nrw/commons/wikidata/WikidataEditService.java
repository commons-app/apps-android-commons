package fr.free.nrw.commons.wikidata;


import static fr.free.nrw.commons.media.MediaClientKt.PAGE_ID_PREFIX;

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
import fr.free.nrw.commons.wikidata.model.DataValue;
import fr.free.nrw.commons.wikidata.model.DataValue.ValueString;
import fr.free.nrw.commons.wikidata.model.EditClaim;
import fr.free.nrw.commons.wikidata.model.RemoveClaim;
import fr.free.nrw.commons.wikidata.model.Snak_partial;
import fr.free.nrw.commons.wikidata.model.Statement_partial;
import fr.free.nrw.commons.wikidata.model.WikiBaseMonolingualTextValue;
import fr.free.nrw.commons.wikidata.mwapi.MwPostResponse;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import timber.log.Timber;

/**
 * This class is meant to handle the Wikidata edits made through the app It will talk with MediaWiki
 * Apis to make the necessary calls, log the edits and fire listeners on successful edits
 */
@Singleton
public class WikidataEditService {

    public static final String COMMONS_APP_TAG = "wikimedia-commons-app";

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
     * Edits the wikibase entity by adding DEPICTS property. Adding DEPICTS property requires call
     * to the wikibase API to set tag against the entity.
     */
    @SuppressLint("CheckResult")
    private Observable<Boolean> addDepictsProperty(
        final String fileEntityId,
        final List<String> depictedItems
    ) {
        final EditClaim data = editClaim(
            ConfigUtils.isBetaFlavour() ? Collections.singletonList("Q10")
                // Wikipedia:Sandbox (Q10)
                : depictedItems
        );

        return wikiBaseClient.postEditEntity(PAGE_ID_PREFIX + fileEntityId, gson.toJson(data))
            .doOnNext(success -> {
                if (success) {
                    Timber.d("DEPICTS property was set successfully for %s", fileEntityId);
                } else {
                    Timber.d("Unable to set DEPICTS property for %s", fileEntityId);
                }
            })
            .doOnError(throwable -> {
                Timber.e(throwable, "Error occurred while setting DEPICTS property");
                ViewUtil.showLongToast(context, throwable.toString());
            })
            .subscribeOn(Schedulers.io());
    }

    /**
     * Takes depicts ID as a parameter and create a uploadable data with the Id
     * and send the data for POST operation
     *
     * @param fileEntityId ID of the file
     * @param depictedItems IDs of the selected depict item
     * @return Observable<Boolean>
     */
    @SuppressLint("CheckResult")
    public Observable<Boolean> updateDepictsProperty(
        final String fileEntityId,
        final List<String> depictedItems
    ) {
        final String entityId = PAGE_ID_PREFIX + fileEntityId;
        final List<String> claimIds = getDepictionsClaimIds(entityId);

        final RemoveClaim data = removeClaim( /* Please consider removeClaim scenario for BetaDebug */
            ConfigUtils.isBetaFlavour() ? Collections.singletonList("Q10")
                // Wikipedia:Sandbox (Q10)
                : claimIds
        );

        return wikiBaseClient.postDeleteClaims(entityId, gson.toJson(data))
            .doOnError(throwable -> {
                Timber.e(throwable, "Error occurred while removing existing claims for DEPICTS property");
                ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
            }).switchMap(success-> {
                if(success) {
                    Timber.d("DEPICTS property was deleted successfully");
                    return addDepictsProperty(fileEntityId, depictedItems);
                } else {
                    Timber.d("Unable to delete DEPICTS property");
                    return Observable.empty();
                }
            });
    }

    @SuppressLint("CheckResult")
    private List<String> getDepictionsClaimIds(final String entityId) {
        return wikiBaseClient.getClaimIdsByProperty(entityId, WikidataProperties.DEPICTS.getPropertyName())
            .subscribeOn(Schedulers.io())
            .blockingFirst();
    }

    private EditClaim editClaim(final List<String> entityIds) {
        return EditClaim.from(entityIds, WikidataProperties.DEPICTS.getPropertyName());
    }

    private RemoveClaim removeClaim(final List<String> claimIds) {
        return RemoveClaim.from(claimIds);
    }

    /**
     * Show a success toast when the edit is made successfully
     */
    private void showSuccessToast(final String wikiItemName) {
        final String successStringTemplate = context.getString(R.string.successful_wikidata_edit);
        final String successMessage = String
            .format(Locale.getDefault(), successStringTemplate, wikiItemName);
        ViewUtil.showLongToast(context, successMessage);
    }

    /**
     * Adds label to Wikidata using the fileEntityId and the edit token, obtained from
     * csrfTokenClient
     *
     * @param fileEntityId
     * @return
     */
    @SuppressLint("CheckResult")
    private Observable<Boolean> addCaption(final long fileEntityId, final String languageCode,
        final String captionValue) {
        return wikiBaseClient.addLabelsToWikidata(fileEntityId, languageCode, captionValue)
            .doOnNext(mwPostResponse -> onAddCaptionResponse(fileEntityId, mwPostResponse))
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

    public Long createClaim(@Nullable final WikidataPlace wikidataPlace, final String fileName,
        final Map<String, String> captions) {
        if (!(directKvStore.getBoolean("Picture_Has_Correct_Location", true))) {
            Timber
                .d("Image location and nearby place location mismatched, so Wikidata item won't be edited");
            return null;
        }
        return addImageAndMediaLegends(wikidataPlace, fileName, captions);
    }

    public Long addImageAndMediaLegends(final WikidataItem wikidataItem, final String fileName,
        final Map<String, String> captions) {
        final Snak_partial p18 = new Snak_partial("value",
            WikidataProperties.IMAGE.getPropertyName(),
            new ValueString(fileName.replace("File:", "")));

        final List<Snak_partial> snaks = new ArrayList<>();
        for (final Map.Entry<String, String> entry : captions.entrySet()) {
            snaks.add(new Snak_partial("value",
                WikidataProperties.MEDIA_LEGENDS.getPropertyName(), new DataValue.MonoLingualText(
                new WikiBaseMonolingualTextValue(entry.getValue(), entry.getKey()))));
        }

        final String id = wikidataItem.getId() + "$" + UUID.randomUUID().toString();
        final Statement_partial claim = new Statement_partial(p18, "statement", "normal", id,
            Collections.singletonMap(WikidataProperties.MEDIA_LEGENDS.getPropertyName(), snaks),
            Arrays.asList(WikidataProperties.MEDIA_LEGENDS.getPropertyName()));

        return wikidataClient.setClaim(claim, COMMONS_APP_TAG).blockingSingle();
    }

    public void handleImageClaimResult(final WikidataItem wikidataItem, final Long revisionId) {
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

    public Observable<Boolean> addDepictionsAndCaptions(
        final UploadResult uploadResult,
        final Contribution contribution
    ) {
        return wikiBaseClient.getFileEntityId(uploadResult)
            .doOnError(throwable -> {
                Timber
                    .e(throwable, "Error occurred while getting EntityID to set DEPICTS property");
                ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
            })
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
            );
    }

    private Observable<Boolean> captionEdits(Contribution contribution, Long fileEntityId) {
        return Observable.fromIterable(contribution.getMedia().getCaptions().entrySet())
            .concatMap(entry -> addCaption(fileEntityId, entry.getKey(), entry.getValue()));
    }

    private Observable<Boolean> depictionEdits(Contribution contribution, Long fileEntityId) {
        final List<String> depictIDs = new ArrayList<>();
        for (final WikidataItem wikidataItem :
            contribution.getDepictedItems()) {
            depictIDs.add(wikidataItem.getId());
        }
        return addDepictsProperty(fileEntityId.toString(), depictIDs);
    }
}

