package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.utils.BitmapRegionDecoderWrapper;
import fr.free.nrw.commons.utils.ImageUtils;
import fr.free.nrw.commons.utils.ImageUtilsWrapper;
import fr.free.nrw.commons.utils.StringUtils;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

public class UploadModel {

    private MediaWikiApi mwApi;
    private static UploadItem DUMMY = new UploadItem(
            Uri.EMPTY,
            "",
            "",
            GPSExtractor.DUMMY,
            "",
            null,
            -1L) {
    };
    private final SharedPreferences prefs;
    private final List<String> licenses;
    private String license;
    private final Map<String, String> licensesByName;
    private List<UploadItem> items = new ArrayList<>();
    private boolean topCardState = true;
    private boolean bottomCardState = true;
    private boolean rightCardState = true;
    private int currentStepIndex = 0;
    private Context context;
    private ContentResolver contentResolver;
    private boolean useExtStorage;
    private Disposable badImageSubscription;

    private SessionManager sessionManager;
    private Uri currentMediaUri;
    private FileUtilsWrapper fileUtilsWrapper;
    private ImageUtilsWrapper imageUtilsWrapper;
    private BitmapRegionDecoderWrapper bitmapRegionDecoderWrapper;
    private FileProcessor fileProcessor;

    @Inject
    UploadModel(@Named("licenses") List<String> licenses,
                @Named("default_preferences") SharedPreferences prefs,
                @Named("licenses_by_name") Map<String, String> licensesByName,
                Context context,
                MediaWikiApi mwApi,
                SessionManager sessionManager,
                FileUtilsWrapper fileUtilsWrapper,
                ImageUtilsWrapper imageUtilsWrapper,
                BitmapRegionDecoderWrapper bitmapRegionDecoderWrapper,
                FileProcessor fileProcessor) {
        this.licenses = licenses;
        this.prefs = prefs;
        this.license = prefs.getString(Prefs.DEFAULT_LICENSE, Prefs.Licenses.CC_BY_SA_3);
        this.bitmapRegionDecoderWrapper = bitmapRegionDecoderWrapper;
        this.licensesByName = licensesByName;
        this.context = context;
        this.mwApi = mwApi;
        this.contentResolver = context.getContentResolver();
        this.sessionManager = sessionManager;
        this.fileUtilsWrapper = fileUtilsWrapper;
        this.fileProcessor = fileProcessor;
        this.imageUtilsWrapper = imageUtilsWrapper;
        useExtStorage = this.prefs.getBoolean("useExternalStorage", false);
    }

    @SuppressLint("CheckResult")
    void receive(List<Uri> mediaUri, String mimeType, String source, SimilarImageInterface similarImageInterface) {
        initDefaultValues();
        Observable<UploadItem> itemObservable = Observable.fromIterable(mediaUri)
                .map(media -> {
                    currentMediaUri = media;
                    return cacheFileUpload(media);
                })
                .map(filePath -> {
                    long fileCreatedDate = getFileCreatedDate(currentMediaUri);
                    Uri uri = Uri.fromFile(new File(filePath));
                    fileProcessor.initFileDetails(filePath, context.getContentResolver());
                    UploadItem item = new UploadItem(uri, mimeType, source, fileProcessor.processFileCoordinates(similarImageInterface),
                            fileUtilsWrapper.getFileExt(filePath), null, fileCreatedDate);
                    Single.zip(
                            Single.fromCallable(() ->
                                    fileUtilsWrapper.getFileInputStream(filePath))
                                    .map(fileUtilsWrapper::getSHA1)
                                    .map(mwApi::existingFile)
                                    .map(b -> b ? ImageUtils.IMAGE_DUPLICATE : ImageUtils.IMAGE_OK),
                            Single.fromCallable(() ->
                                    fileUtilsWrapper.getFileInputStream(filePath))
                                    .map(file -> bitmapRegionDecoderWrapper.newInstance(file, false))
                                    .map(imageUtilsWrapper::checkIfImageIsTooDark), //Returns IMAGE_DARK or IMAGE_OK
                            (dupe, dark) -> dupe | dark)
                            .observeOn(Schedulers.io())
                            .subscribe(item.imageQuality::onNext, Timber::e);
                    return item;
                });
        items = itemObservable.toList().blockingGet();
        items.get(0).selected = true;
        items.get(0).first = true;
    }

    @SuppressLint("CheckResult")
    void receiveDirect(Uri media, String mimeType, String source, String wikidataEntityIdPref, String title, String desc, SimilarImageInterface similarImageInterface, String wikidataItemLocation) {
        initDefaultValues();
        long fileCreatedDate = getFileCreatedDate(media);
        String filePath = this.cacheFileUpload(media);
        Uri uri = Uri.fromFile(new File(filePath));
        fileProcessor.initFileDetails(filePath, context.getContentResolver());
        UploadItem item = new UploadItem(uri, mimeType, source, fileProcessor.processFileCoordinates(similarImageInterface),
                fileUtilsWrapper.getFileExt(filePath), wikidataEntityIdPref, fileCreatedDate);
        item.title.setTitleText(title);
        item.descriptions.get(0).setDescriptionText(desc);
        //TODO figure out if default descriptions in other languages exist
        item.descriptions.get(0).setLanguageCode("en");
        Single.zip(
                Single.fromCallable(() ->
                        fileUtilsWrapper.getFileInputStream(filePath))
                        .map(fileUtilsWrapper::getSHA1)
                        .map(mwApi::existingFile)
                        .map(b -> b ? ImageUtils.IMAGE_DUPLICATE : ImageUtils.IMAGE_OK),
                Single.fromCallable(() -> filePath)
                        .map(fileUtilsWrapper::getGeolocationOfFile)
                        .map(geoLocation -> imageUtilsWrapper.checkImageGeolocationIsDifferent(geoLocation, wikidataItemLocation))
                        .map(r -> r ? ImageUtils.IMAGE_GEOLOCATION_DIFFERENT : ImageUtils.IMAGE_OK),
                Single.fromCallable(() ->
                        fileUtilsWrapper.getFileInputStream(filePath))
                        .map(file -> bitmapRegionDecoderWrapper.newInstance(file, false))
                        .map(imageUtilsWrapper::checkIfImageIsTooDark), //Returns IMAGE_DARK or IMAGE_OK
                (dupe, wrongGeo, dark) -> dupe | wrongGeo | dark).subscribe(item.imageQuality::onNext);
        items.add(item);
        items.get(0).selected = true;
        items.get(0).first = true;
    }

    private void initDefaultValues() {
        currentStepIndex = 0;
        topCardState = true;
        bottomCardState = true;
        rightCardState = true;
        items = new ArrayList<>();
    }

    /**
     * Get file creation date from uri from all possible content providers
     *
     * @param media
     * @return
     */
    private long getFileCreatedDate(Uri media) {
        try {
            Cursor cursor = contentResolver.query(media, null, null, null, null);
            if (cursor == null) {
                return -1;//Could not fetch last_modified
            }
            //Content provider contracts for opening gallery from the app and that by sharing from gallery from outside are different and we need to handle both the cases
            int lastModifiedColumnIndex = cursor.getColumnIndex("last_modified");//If gallery is opened from in app
            if (lastModifiedColumnIndex == -1) {
                lastModifiedColumnIndex = cursor.getColumnIndex("datetaken");
            }
            //If both the content providers do not give the data, lets leave it to Jesus
            if (lastModifiedColumnIndex == -1) {
                return -1L;
            }
            cursor.moveToFirst();
            return cursor.getLong(lastModifiedColumnIndex);
        } catch (Exception e) {
            return -1;////Could not fetch last_modified
        }
    }

    boolean isPreviousAvailable() {
        return currentStepIndex > 0;
    }

    boolean isNextAvailable() {
        return currentStepIndex < (items.size() + 1);
    }

    boolean isSubmitAvailable() {
        int count = items.size();
        boolean hasError = license == null;
        for (int i = 0; i < count; i++) {
            UploadItem item = items.get(i);
            hasError |= item.error;
        }
        return !hasError;
    }

    int getCurrentStep() {
        return currentStepIndex + 1;
    }

    int getStepCount() {
        return items.size() + 2;
    }

    public int getCount() {
        return items.size();
    }

    public List<UploadItem> getUploads() {
        return items;
    }

    boolean isTopCardState() {
        return topCardState;
    }

    void setTopCardState(boolean topCardState) {
        this.topCardState = topCardState;
    }

    boolean isBottomCardState() {
        return bottomCardState;
    }

    void setRightCardState(boolean rightCardState) {
        this.rightCardState = rightCardState;
    }

    boolean isRightCardState() {
        return rightCardState;
    }

    void setBottomCardState(boolean bottomCardState) {
        this.bottomCardState = bottomCardState;
    }

    public void next() {
        if (badImageSubscription != null)
            badImageSubscription.dispose();
        markCurrentUploadVisited();
        if (currentStepIndex < items.size() + 1) {
            currentStepIndex++;
        }
        updateItemState();
    }

    void setCurrentTitleAndDescriptions(Title title, List<Description> descriptions) {
        setCurrentUploadTitle(title);
        setCurrentUploadDescriptions(descriptions);
    }

    private void setCurrentUploadTitle(Title title) {
        if (currentStepIndex < items.size() && currentStepIndex >= 0) {
            items.get(currentStepIndex).title = title;
        }
    }

    private void setCurrentUploadDescriptions(List<Description> descriptions) {
        if (currentStepIndex < items.size() && currentStepIndex >= 0) {
            items.get(currentStepIndex).descriptions = descriptions;
        }
    }

    public void previous() {
        if (badImageSubscription != null)
            badImageSubscription.dispose();
        markCurrentUploadVisited();
        if (currentStepIndex > 0) {
            currentStepIndex--;
        }
        updateItemState();
    }

    void jumpTo(UploadItem item) {
        currentStepIndex = items.indexOf(item);
        item.visited = true;
        updateItemState();
    }

    UploadItem getCurrentItem() {
        return isShowingItem() ? items.get(currentStepIndex) : DUMMY;
    }

    boolean isShowingItem() {
        return currentStepIndex < items.size();
    }

    private void updateItemState() {
        int count = items.size();
        for (int i = 0; i < count; i++) {
            UploadItem item = items.get(i);
            item.selected = (currentStepIndex >= count || i == currentStepIndex);
            item.error = item.title == null || item.title.isEmpty();
        }
    }

    private void markCurrentUploadVisited() {
        if (currentStepIndex < items.size() && currentStepIndex >= 0) {
            items.get(currentStepIndex).visited = true;
        }
    }

    public List<String> getLicenses() {
        return licenses;
    }

    String getSelectedLicense() {
        return license;
    }

    void setSelectedLicense(String licenseName) {
        this.license = licensesByName.get(licenseName);
        prefs.edit().putString(Prefs.DEFAULT_LICENSE, license).commit();
    }

    Observable<Contribution> buildContributions(List<String> categoryStringList) {
        return Observable.fromIterable(items).map(item ->
        {
            Contribution contribution = new Contribution(item.mediaUri, null, item.title + "." + item.fileExt,
                    Description.formatList(item.descriptions), -1,
                    null, null, sessionManager.getAuthorName(),
                    CommonsApplication.DEFAULT_EDIT_SUMMARY, item.gpsCoords.getCoords());
            contribution.setWikiDataEntityId(item.wikidataEntityId);
            contribution.setCategories(categoryStringList);
            contribution.setTag("mimeType", item.mimeType);
            contribution.setSource(item.source);
            contribution.setContentProviderUri(item.mediaUri);
            if (item.createdTimestamp != -1L) {
                contribution.setDateCreated(new Date(item.createdTimestamp));
                //Set the date only if you have it, else the upload service is gonna try it the other way
            }
            return contribution;
        });
    }

    /**
     * Copy files into local storage and return file path
     * If somehow copy fails, it returns the original path
     * @param media Uri of the file
     * @return path of the enw file
     */
    private String cacheFileUpload(Uri media) {
        String finalFilePath;
        try {
            String copyFilePath = fileUtilsWrapper.createCopyPathAndCopy(useExtStorage, media, contentResolver, context);
            Timber.i("Copied file path is %s", copyFilePath);
            finalFilePath = copyFilePath;
        } catch (Exception e) {
            Timber.w(e, "Error in copying URI %s. Using original file path instead", media.getPath());
            finalFilePath = media.getPath();
        }

        if (StringUtils.isNullOrWhiteSpace(finalFilePath)) {
            finalFilePath = media.getPath();
        }
        return finalFilePath;
    }

    void keepPicture() {
        items.get(currentStepIndex).imageQuality.onNext(ImageUtils.IMAGE_KEEP);
    }

    void deletePicture() {
        badImageSubscription.dispose();
        items.remove(currentStepIndex).imageQuality.onComplete();
        updateItemState();
    }

    void subscribeBadPicture(Consumer<Integer> consumer) {
        badImageSubscription = getCurrentItem().imageQuality.subscribe(consumer, Timber::e);
    }

    public List<UploadItem> getItems() {
        return items;
    }

    @SuppressWarnings("WeakerAccess")
    static class UploadItem {
        public final Uri mediaUri;
        public final String mimeType;
        public final String source;
        public final GPSExtractor gpsCoords;

        public boolean selected = false;
        public boolean first = false;
        public String fileExt;
        public BehaviorSubject<Integer> imageQuality;
        Title title;
        List<Description> descriptions;
        public String wikidataEntityId;
        public boolean visited;
        public boolean error;
        public long createdTimestamp;

        @SuppressLint("CheckResult")
        UploadItem(Uri mediaUri, String mimeType, String source, GPSExtractor gpsCoords, String fileExt, @Nullable String wikidataEntityId, long createdTimestamp) {
            title = new Title();
            descriptions = new ArrayList<>();
            descriptions.add(new Description());
            this.wikidataEntityId = wikidataEntityId;
            this.mediaUri = mediaUri;
            this.mimeType = mimeType;
            this.source = source;
            this.gpsCoords = gpsCoords;
            this.fileExt = fileExt;
            imageQuality = BehaviorSubject.createDefault(ImageUtils.IMAGE_WAIT);
            this.createdTimestamp = createdTimestamp;
        }
    }

}
