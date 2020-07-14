package fr.free.nrw.commons.wikidata;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * This class is meant to handle the Wikidata edits made through the app
 * It will talk with MediaWiki Apis to make the necessary calls, log the edits and fire listeners
 * on successful edits
 */
@Singleton
public class WikidataEditService {

    private final static String COMMONS_APP_TAG = "wikimedia-commons-app";
    private final static String COMMONS_APP_EDIT_REASON = "Add tag for edits made using Android Commons app";

    private final Context context;
    private final WikidataEditListener wikidataEditListener;
    private final JsonKvStore directKvStore;
    private final WikidataClient wikidataClient;

    @Inject
    WikidataEditService(Context context,
                        WikidataEditListener wikidataEditListener,
                        @Named("default_preferences") JsonKvStore directKvStore,
                        WikidataClient wikidataClient) {
        this.context = context;
        this.wikidataEditListener = wikidataEditListener;
        this.directKvStore = directKvStore;
        this.wikidataClient = wikidataClient;
    }

    /**
     * Create a P18 claim and log the edit with custom tag
     * @param wikidataEntityId a unique id of each Wikidata items
     * @param fileName name of the file we will upload
     * @param p18Value pic attribute of Wikidata item
     */
    public void createClaimWithLogging(String wikidataEntityId, String wikiItemName, String fileName, String p18Value) {
        if (wikidataEntityId == null) {
            Timber.d("Skipping creation of claim as Wikidata entity ID is null");
            return;
        }

        if (fileName == null) {
            Timber.d("Skipping creation of claim as fileName entity ID is null");
            return;
        }

        if (p18Value != null && !p18Value.trim().isEmpty()) {
            Timber.d("Skipping creation of claim as p18Value is not empty, we won't override existing image");
            return;
        }

        editWikidataProperty(wikidataEntityId, wikiItemName, fileName);
    }

    /**
     * Edits the wikidata entity by adding the P18 property to it.
     * Adding the P18 edit requires calling the wikidata API to create a claim against the entity
     *
     * @param wikidataEntityId
     * @param fileName
     */
    @SuppressLint("CheckResult")
    private void editWikidataProperty(String wikidataEntityId, String wikiItemName, String fileName) {
        Timber.d("Upload successful with wiki data entity id as %s", wikidataEntityId);
        Timber.d("Attempting to edit Wikidata property %s", wikidataEntityId);

        String propertyValue = getFileName(fileName);

        Timber.d("Entity id is %s and property value is %s", wikidataEntityId, propertyValue);
        wikidataClient.createClaim(wikidataEntityId, propertyValue)
                .flatMap(revisionId -> {
                    if (revisionId != -1) {
                        return wikidataClient.addEditTag(revisionId, COMMONS_APP_TAG, COMMONS_APP_EDIT_REASON);
                    }
                    throw new RuntimeException("Unable to edit wikidata item");
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(revisionId -> handleClaimResult(wikidataEntityId, wikiItemName, String.valueOf(revisionId)), throwable -> {
                    Timber.e(throwable, "Error occurred while making claim");
                    ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
                });
    }

    private void handleClaimResult(String wikidataEntityId, String wikiItemName, String revisionId) {
        if (revisionId != null) {
            if (wikidataEditListener != null) {
                wikidataEditListener.onSuccessfulWikidataEdit();
            }
            showSuccessToast(wikiItemName);
        } else {
            Timber.d("Unable to make wiki data edit for entity %s", wikidataEntityId);
            ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
        }
    }

    /**
     * Show a success toast when the edit is made successfully
     */
    private void showSuccessToast(String wikiItemName) {
        String successStringTemplate = context.getString(R.string.successful_wikidata_edit);
        String successMessage = String.format(Locale.getDefault(), successStringTemplate, wikiItemName);
        ViewUtil.showLongToast(context, successMessage);
    }

    /**
     * Formats and returns the filename as accepted by the wiki base API
     * https://www.mediawiki.org/wiki/Wikibase/API#wbcreateclaim
     *
     * @param fileName
     * @return
     */
    private String getFileName(String fileName) {
        fileName = String.format("\"%s\"", fileName.replace("File:", ""));
        Timber.d("Wikidata property name is %s", fileName);
        return fileName;
    }
}
