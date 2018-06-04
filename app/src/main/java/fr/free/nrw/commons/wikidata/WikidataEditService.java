package fr.free.nrw.commons.wikidata;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * This class is meant to handle the Wikidata edits made through the app
 * It will talk with MediaWikiApi to make necessary API calls, log the edits and fire listeners
 * on successful edits
 */
@Singleton
public class WikidataEditService {

    private final Context context;
    private final MediaWikiApi mediaWikiApi;
    private final WikidataEditListener wikidataEditListener;
    private final SharedPreferences directPrefs;

    @Inject
    public WikidataEditService(Context context,
                               MediaWikiApi mediaWikiApi,
                               WikidataEditListener wikidataEditListener,
                               @Named("direct_nearby_upload_prefs") SharedPreferences directPrefs) {
        this.context = context;
        this.mediaWikiApi = mediaWikiApi;
        this.wikidataEditListener = wikidataEditListener;
        this.directPrefs = directPrefs;
    }

    /**
     * Create a P18 claim and log the edit with custom tag
     * @param wikidataEntityId
     * @param fileName
     */
    public void createClaimWithLogging(String wikidataEntityId, String fileName) {
        editWikidataProperty(wikidataEntityId, fileName);
    }

    /**
     * Edits the wikidata entity by adding the P18 property to it.
     * Adding the P18 edit requires calling the wikidata API to create a claim against the entity
     *
     * @param wikidataEntityId
     * @param fileName
     */
    @SuppressLint("CheckResult")
    private void editWikidataProperty(String wikidataEntityId, String fileName) {
        Timber.d("Upload successful with wiki data entity id as %s", wikidataEntityId);
        Timber.d("Attempting to edit Wikidata property %s", wikidataEntityId);
        Observable.fromCallable(() -> {
            String propertyValue = getFileName(fileName);
            return mediaWikiApi.wikidatCreateClaim(wikidataEntityId, "P18", "value", propertyValue);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(revisionId -> handleClaimResult(wikidataEntityId, revisionId), throwable -> {
                    Timber.e(throwable, "Error occurred while making claim");
                    ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
                });
    }

    private void handleClaimResult(String wikidataEntityId, String revisionId) {
        if (revisionId != null) {
            wikidataEditListener.onSuccessfulWikidataEdit();
            showSuccessToast();
            logEdit(revisionId);
        } else {
            Timber.d("Unable to make wiki data edit for entity %s", wikidataEntityId);
            ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
        }
    }

    /**
     * Log the Wikidata edit by adding Wikimedia Commons App tag to the edit
     * @param revisionId
     */
    @SuppressLint("CheckResult")
    private void logEdit(String revisionId) {
        Observable.fromCallable(() -> mediaWikiApi.addWikidataEditTag(revisionId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result) {
                        Timber.d("Wikidata edit was tagged successfully");
                    } else {
                        Timber.d("Wikidata edit couldn't be tagged");
                    }
                }, throwable -> {
                    Timber.e(throwable, "Error occurred while adding tag to the edit");
                });
    }

    /**
     * Show a success toast when the edit is made successfully
     */
    private void showSuccessToast() {
        String title = directPrefs.getString("Title", "");
        String successStringTemplate = context.getString(R.string.successful_wikidata_edit);
        String successMessage = String.format(Locale.getDefault(), successStringTemplate, title);
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
