package fr.free.nrw.commons.wikidata;

import static fr.free.nrw.commons.depictions.Media.DepictedImagesFragment.PAGE_ID_PREFIX;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.media.MediaClient;
import fr.free.nrw.commons.upload.mediaDetails.CaptionInterface;
import fr.free.nrw.commons.utils.ConfigUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.Service;
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
    private final CaptionInterface captionInterface;
    private final WikiBaseClient wikiBaseClient;
    private final WikidataClient wikidataClient;
    private final MediaClient mediaClient;
    private final CsrfTokenClient csrfTokenClient;
    private final Service service;

    @Inject
    public WikidataEditService(Context context,
                               WikidataEditListener wikidataEditListener,
                               MediaClient mediaClient,
                               @Named("default_preferences") JsonKvStore directKvStore,
                               WikiBaseClient wikiBaseClient,
                               CaptionInterface captionInterface,
                               WikidataClient wikidataClient,
                               @Named("commons-csrf") CsrfTokenClient csrfTokenClient,
                               @Named("commons-service") Service service) {
        this.context = context;
        this.wikidataEditListener = wikidataEditListener;
        this.directKvStore = directKvStore;
        this.captionInterface = captionInterface;
        this.wikiBaseClient = wikiBaseClient;
        this.mediaClient = mediaClient;
        this.wikidataClient = wikidataClient;
        this.csrfTokenClient = csrfTokenClient;
        this.service = service;
    }

    /**
     * Create a P18 claim and log the edit with custom tag
<<<<<<< HEAD
     *
     * @param wikidataEntityId
     * @param fileName
=======
     * @param wikidataEntityId a unique id of each Wikidata items
     * @param fileName name of the file we will upload
     * @param p18Value pic attribute of Wikidata item
>>>>>>> origin/master
     */
    public void createClaimWithLogging(String wikidataEntityId, String wikiItemName, String fileName, @NonNull String p18Value) {
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

        if (!p18Value.trim().isEmpty()) {
            Timber.d("Skipping creation of claim as p18Value is not empty, we won't override existing image");
            return;
        }

        editWikidataProperty(wikidataEntityId, wikiItemName, fileName);
        //editWikiBaseDepictsProperty(wikidataEntityId, fileName);
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

    /**
     * Edits the wikibase entity by adding DEPICTS property.
     * Adding DEPICTS property requires call to the wikibase API to set tag against the entity.
     *
     * @param wikidataEntityId
     * @param fileName
     */
    @SuppressLint("CheckResult")
    private void editWikiBaseDepictsProperty(String wikidataEntityId, String fileName) {
        wikiBaseClient.getFileEntityId(fileName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fileEntityId -> {
                    if (fileEntityId != null) {
                        Timber.d("EntityId for image was received successfully: %s", fileEntityId);
                        addDepictsProperty(wikidataEntityId, fileEntityId.toString());
                    } else {
                        Timber.d("Error acquiring EntityId for image: %s", fileName);
                    }
                    }, throwable -> {
                    Timber.e(throwable, "Error occurred while getting EntityID to set DEPICTS property");
                    ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
                });
    }

    @SuppressLint("CheckResult")
    private void addDepictsProperty(String entityId, String fileEntityId) {
        if (ConfigUtils.isBetaFlavour()) {
            entityId = "Q10"; // Wikipedia:Sandbox (Q10)
        }

        JsonObject value = new JsonObject();
        value.addProperty("entity-type", "item");
        value.addProperty("numeric-id", entityId.replace("Q", ""));
        value.addProperty("id", entityId);

        JsonObject dataValue = new JsonObject();
        dataValue.add("value", value);
        dataValue.addProperty("type", "wikibase-entityid");

        JsonObject mainSnak = new JsonObject();
        mainSnak.addProperty("snaktype", "value");
        mainSnak.addProperty("property", BuildConfig.DEPICTS_PROPERTY);
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
                wikiBaseClient.postEditEntity(PAGE_ID_PREFIX + fileEntityId, data))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                            if (success)
                                Timber.d("DEPICTS property was set successfully for %s", fileEntityId);
                            else
                                Timber.d("Unable to set DEPICTS property for %s", fileEntityId);
                        },
                        throwable -> {
                            Timber.e(throwable, "Error occurred while setting DEPICTS property");
                            ViewUtil.showLongToast(context, throwable.toString());
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

    /**
     * Adding captions as labels after image is successfully uploaded
     */
    @SuppressLint("CheckResult")
    public void createLabelforWikidataEntity(String wikiDataEntityId, String fileName, Map<String, String> captions) {
        Observable.fromCallable(() -> wikiBaseClient.getFileEntityId(fileName))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fileEntityId -> {
                    if (fileEntityId != null) {
                        for (Map.Entry<String, String> entry : captions.entrySet()) {
                            Map<String, String> caption = new HashMap<>();
                            caption.put(entry.getKey(), entry.getValue());
                            try {
                                wikidataAddLabels(wikiDataEntityId, fileEntityId.toString(), caption);
                            } catch (Throwable throwable) {
                                throwable.printStackTrace();
                            }
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
     * @param wikiDataEntityId entityId for the current contribution
     * @param fileEntityId
     * @param caption
     */

    @SuppressLint("CheckResult")
    private void wikidataAddLabels(String wikiDataEntityId, String fileEntityId, Map<String, String> caption) throws Throwable {
        Observable.fromCallable(() -> {
            try {
                return csrfTokenClient.getTokenBlocking();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                return null;
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(editToken  -> {
                    if (editToken  != null) {
                        Observable.fromCallable(() -> captionInterface.addLabelstoWikidata(fileEntityId, editToken, caption.get(0), caption))
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(revisionId ->
                                {
                                    if (revisionId != null) {
                                        Timber.d("Caption successfully set, revision id = %s", revisionId);
                                    } else {
                                        Timber.d("Error occurred while setting Captions, fileEntityId = %s", fileEntityId);
                                    }

                                },
                                        throwable -> {
                                            Timber.e(throwable, "Error occurred while setting Captions");
                                            ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
                                        });
                    }else {
                        Timber.d("Error acquiring EntityId for image");
                    }
                }, throwable -> {
                    Timber.e(throwable, "Error occurred while getting EntityID for the File");
                    ViewUtil.showLongToast(context, context.getString(R.string.wikidata_edit_failure));
                });
    }
}
