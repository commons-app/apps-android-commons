package fr.free.nrw.commons.wikidata;

import android.annotation.SuppressLint;
import android.content.Context;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.actions.PageEditClient;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.media.MediaClient;
import fr.free.nrw.commons.upload.mediaDetails.CaptionInterface;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
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

    private final static String COMMONS_APP_TAG = "wikimedia-commons-app";
    private final static String COMMONS_APP_EDIT_REASON = "Add tag for edits made using Android Commons app";

    private final Context context;
    private final WikidataEditListener wikidataEditListener;
    private final JsonKvStore directKvStore;
    private final CaptionInterface captionInterface;
    private final WikiBaseClient wikiBaseClient;
    private final WikidataClient wikidataClient;
    private final PageEditClient wikiDataPageEditClient;
    private final MediaClient mediaClient;

    @Inject
    public WikidataEditService(Context context,
                               WikidataEditListener wikidataEditListener, MediaClient mediaClient,
                               @Named("default_preferences") JsonKvStore directKvStore, WikiBaseClient wikiBaseClient, CaptionInterface captionInterface, WikidataClient wikidataClient, @Named("wikidata-page-edit") PageEditClient wikiDataPageEditClient) {
        this.context = context;
        this.wikidataEditListener = wikidataEditListener;
        this.directKvStore = directKvStore;
        this.captionInterface = captionInterface;
        this.wikiBaseClient = wikiBaseClient;
        this.wikiDataPageEditClient = wikiDataPageEditClient;
        this.mediaClient = mediaClient;
        this.wikidataClient = wikidataClient;
    }

    /**
     * Create a P18 claim and log the edit with custom tag
     *
     * @param wikidataEntityId
     * @param fileName
     */
    public void createClaimWithLogging(String wikidataEntityId, String fileName) {
        if (wikidataEntityId == null) {
            Timber.d("Skipping creation of claim as Wikidata entity ID is null");
            return;
        }

        if (fileName == null) {
            Timber.d("Skipping creation of claim as fileName entity ID is null");
            return;
        }

        if (!(directKvStore.getBoolean("Picture_Has_Correct_Location", true))) {
            Timber.d("Image location and nearby place location mismatched, so Wikidata item won't be edited");
            return;
        }

        // TODO Wikidata Sandbox (Q4115189) for test purposes
        //wikidataEntityId = "Q4115189";
        editWikidataProperty(wikidataEntityId, fileName);
        editWikiBasePropertyP180(wikidataEntityId, fileName);
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

        String propertyValue = getFileName(fileName);

        Timber.d(propertyValue);
        wikidataClient.createClaim(wikidataEntityId, "P18", "value", propertyValue)
                .flatMap(revisionId -> {
                    if (revisionId != -1) {
                        return wikiDataPageEditClient.addEditTag(revisionId, COMMONS_APP_TAG, COMMONS_APP_EDIT_REASON);
                    }
                    throw new RuntimeException("Unable to edit wikidata item");
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(revisionId -> handleClaimResult(wikidataEntityId, String.valueOf(revisionId)), throwable -> {
                    Timber.e(throwable, "Error occurred while making claim");
                    ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
                });
    }


    /**
     * Edits the wikibase entity by adding the P180 property to it.
     * Adding the P180 requires call to the wikibase API to set tag against the entity.
     *
     * @param wikidataEntityId
     * @param fileName
     */
    @SuppressLint("CheckResult")
    private void editWikiBasePropertyP180(String wikidataEntityId, String fileName) {
        wikiBaseClient.getFileEntityId(fileName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fileEntityId -> {
                    if (fileEntityId != null) {
                        addPropertyP180(wikidataEntityId, fileEntityId.toString());
                        Timber.d("EntityId for image was received successfully");
                    } else {
                        Timber.d("Error acquiring EntityId for image");
                    }
                }, throwable -> {
                    Timber.e(throwable, "Error occurred while getting EntityID for P180 tag");
                    ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
                });
    }

    @SuppressLint("CheckResult")
    private void addPropertyP180(String entityId, String fileEntityId) {

        JsonObject value = new JsonObject();
        value.addProperty("entity-type", "item");
        value.addProperty("numeric-id", entityId.replace("Q", ""));
        value.addProperty("id", entityId);

        JsonObject dataValue = new JsonObject();
        dataValue.add("value", value);
        dataValue.addProperty("type", "wikibase-entityid");

        JsonObject mainSnak = new JsonObject();
        mainSnak.addProperty("snaktype", "value");
        mainSnak.addProperty("property", "P180");
        mainSnak.add("datavalue", dataValue);

        JsonObject claim = new JsonObject();
        claim.add("mainsnak", mainSnak);
        claim.addProperty("type", "statement");
        claim.addProperty("rank", "preferred");

        JsonArray claims = new JsonArray();
        claims.add(claim);

        JsonObject jsonData = new JsonObject();
        jsonData.add("claims", claims);

        String data = jsonData.toString();

        Observable.defer((Callable<ObservableSource<Boolean>>) () ->
                wikiBaseClient.postEditEntity("M" + fileEntityId, data))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                            if (success)
                                Timber.d("Property P180 set successfully for %s", fileEntityId);
                            else
                                Timber.d("Unable to set property P180 for %s", fileEntityId);
                        },
                        throwable -> {
                            Timber.e(throwable, "Error occurred while setting P180 tag");
                            ViewUtil.showLongToast(context, throwable.toString());
                        });
    }

    private void handleClaimResult(String wikidataEntityId, String revisionId) {
        if (revisionId != null) {
            if (wikidataEditListener != null) {
                wikidataEditListener.onSuccessfulWikidataEdit();
            }
            showSuccessToast();
        } else {
            Timber.d("Unable to make wiki data edit for entity %s", wikidataEntityId);
            ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
        }
    }


    /**
     * Show a success toast when the edit is made successfully
     */
    private void showSuccessToast() {
        String caption = directKvStore.getString("Title", "");
        String successStringTemplate = context.getString(R.string.successful_wikidata_edit);
        @SuppressLint({"StringFormatInvalid", "LocalSuppress"}) String successMessage = String.format(Locale.getDefault(), successStringTemplate, caption);
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

    public void createLabelforWikidataEntity(String wikiDataEntityId, String fileName, HashMap<String, String> captions) {
        mediaClient.getMedia(fileName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(media -> media.getPageId())
                .subscribe(fileEntityId -> {
                    if (fileEntityId != null) {
                        Iterator iter = captions.keySet().iterator();
                        while(iter.hasNext()) {
                            Map.Entry entry = (Map.Entry) iter.next();
                            wikidataAddLabels(wikiDataEntityId, fileEntityId, entry.getKey().toString(), entry.getValue().toString());

                        }
                    }else {
                        Timber.d("Error acquiring EntityId for image");
                    }
                }, throwable -> {
                    Timber.e(throwable, "Error occurred while getting EntityID for Q24 tag");
                    ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
                });
    }

    private void wikidataAddLabels(String wikiDataEntityId, String fileEntityId, String key, String value) {
        Observable.fromCallable(() -> captionInterface.addLabelstoWikidata(fileEntityId,"", key.substring(1,key.length()-1), value.substring(1,value.length()-1)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(revisionId -> Timber.d("Property Q24 set successfully for %s", revisionId),
                        throwable -> {
                            Timber.e(throwable, "Error occurred while setting Q24 tag");
                            ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
                        });
    }
}
