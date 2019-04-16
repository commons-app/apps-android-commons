package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.filepicker.MimeTypeMapWrapper;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.kvstore.BasicKvStore;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.utils.ImageUtils;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton
public class UploadModel {

    private static UploadItem DUMMY = new UploadItem(
            Uri.EMPTY,
            "",
            "",
            GPSExtractor.DUMMY,
            null,
            -1L, "") {
    };
    private final BasicKvStore basicKvStore;
    private final List<String> licenses;
    private String license;
    private final Map<String, String> licensesByName;
    private List<UploadItem> items = new ArrayList<>();
    private boolean topCardState = true;
    private boolean bottomCardState = true;
    private boolean rightCardState = true;
    private int currentStepIndex = 0;
    private Context context;
    private Disposable badImageSubscription;

    private SessionManager sessionManager;
    private FileProcessor fileProcessor;
    private final ImageProcessingService imageProcessingService;
    private List<String> selectedCategories;

    @Inject
    UploadModel(@Named("licenses") List<String> licenses,
                @Named("default_preferences") BasicKvStore basicKvStore,
                @Named("licenses_by_name") Map<String, String> licensesByName,
                Context context,
                SessionManager sessionManager,
                FileProcessor fileProcessor,
                ImageProcessingService imageProcessingService) {
        this.licenses = licenses;
        this.basicKvStore = basicKvStore;
        this.license = basicKvStore.getString(Prefs.DEFAULT_LICENSE, Prefs.Licenses.CC_BY_SA_3);
        this.licensesByName = licensesByName;
        this.context = context;
        this.sessionManager = sessionManager;
        this.fileProcessor = fileProcessor;
        this.imageProcessingService = imageProcessingService;
    }

    public void setSelectedCategories(List<String> selectedCategories) {
        this.selectedCategories = selectedCategories;
    }

    @SuppressLint("CheckResult")
    Observable<UploadItem> preProcessImages(List<UploadableFile> uploadableFiles,
                                            Place place,
                                            String source,
                                            SimilarImageInterface similarImageInterface) {
        return Observable.fromIterable(uploadableFiles)
                .map(uploadableFile -> getUploadItem(uploadableFile, place, source, similarImageInterface));
    }

    @SuppressLint("CheckResult")
    public Observable<UploadItem> preProcessImage(UploadableFile uploadableFile,
            Place place,
            String source,
            SimilarImageInterface similarImageInterface) {
        return Observable.just(getUploadItem(uploadableFile, place, source, similarImageInterface));
    }

    public Single<Integer> getImageQuality(UploadItem uploadItem, boolean checkTitle) {
        return imageProcessingService.validateImage(uploadItem, checkTitle);
    }

    @NonNull
    private UploadItem getUploadItem(UploadableFile uploadableFile,
                                     Place place,
                                     String source,
                                     SimilarImageInterface similarImageInterface) {
        fileProcessor.initFileDetails(Objects.requireNonNull(uploadableFile.getFilePath()), context.getContentResolver());
        UploadableFile.DateTimeWithSource dateTimeWithSource = uploadableFile.getFileCreatedDate(context);
        long fileCreatedDate = -1;
        String createdTimestampSource = "";
        if (dateTimeWithSource != null) {
            fileCreatedDate = dateTimeWithSource.getEpochDate();
            createdTimestampSource = dateTimeWithSource.getSource();
        }
        Timber.d("File created date is %d", fileCreatedDate);
        GPSExtractor gpsExtractor = fileProcessor.processFileCoordinates(similarImageInterface);
        UploadItem uploadItem = new UploadItem(Uri.parse(uploadableFile.getFilePath()),
                uploadableFile.getMimeType(context), source, gpsExtractor, place, fileCreatedDate,
                createdTimestampSource);
        if(place!=null){
            uploadItem.title.setTitleText(place.name);
            uploadItem.descriptions.get(0).setDescriptionText(place.getLongDescription());
            uploadItem.descriptions.get(0).setLanguageCode("en");
        }
        items.add(uploadItem);
        return uploadItem;
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

    @SuppressLint("CheckResult")
    public void next() {
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

    public UploadItem getCurrentItem() {
        return isShowingItem() ? items.get(currentStepIndex) : DUMMY;
    }

    boolean isShowingItem() {
        return currentStepIndex < items.size();
    }

    private void updateItemState() {
        Timber.d("Updating item state");
        int count = items.size();
        for (int i = 0; i < count; i++) {
            UploadItem item = items.get(i);
            item.selected = (currentStepIndex >= count || i == currentStepIndex);
            item.error = item.title == null || item.title.isEmpty();
        }
    }

    private void markCurrentUploadVisited() {
        Timber.d("Marking current upload visited");
        if (currentStepIndex < items.size() && currentStepIndex >= 0) {
            items.get(currentStepIndex).visited = true;
        }
    }

    public List<String> getLicenses() {
        return licenses;
    }

    public String getSelectedLicense() {
        return license;
    }

    public void setSelectedLicense(String licenseName) {
        this.license = licensesByName.get(licenseName);
        basicKvStore.putString(Prefs.DEFAULT_LICENSE, license);
    }

    public Observable<Contribution> buildContributions() {
        return Observable.fromIterable(items).map(item ->
        {
            Contribution contribution = new Contribution(item.mediaUri, null,
                    item.getFileName(),
                    Description.formatList(item.descriptions), -1,
                    null, null, sessionManager.getAuthorName(),
                    CommonsApplication.DEFAULT_EDIT_SUMMARY, item.gpsCoords.getCoords());
            if (item.place != null) {
                contribution.setWikiDataEntityId(item.place.getWikiDataEntityId());
            }
            contribution.setCategories(selectedCategories);
            contribution.setTag("mimeType", item.mimeType);
            contribution.setSource(item.source);
            contribution.setContentProviderUri(item.mediaUri);

            Timber.d("Created timestamp while building contribution is %s, %s",
                    item.getCreatedTimestamp(),
                    new Date(item.getCreatedTimestamp()));
            if (item.createdTimestamp != -1L) {
                contribution.setDateCreated(new Date(item.getCreatedTimestamp()));
                contribution.setDateCreatedSource(item.getCreatedTimestampSource());
                //Set the date only if you have it, else the upload service is gonna try it the other way
            }
            return contribution;
        });
    }

    void keepPicture() {
        items.get(currentStepIndex).setImageQuality(ImageUtils.IMAGE_KEEP);
    }

    void deletePicture() {
        badImageSubscription.dispose();
        updateItemState();
    }

    void subscribeBadPicture(Consumer<Integer> consumer, boolean checkTitle) {
        if (isShowingItem()) {
            badImageSubscription = getImageQuality(getCurrentItem(), checkTitle)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(consumer, Timber::e);
        }
    }

    public List<UploadItem> getItems() {
        return items;
    }

    public void updateUploadItem(int index,UploadItem uploadItem) {
        UploadItem uploadItem1 = items.get(index);
        uploadItem1.setDescriptions(uploadItem.descriptions);
        uploadItem1.setTitle(uploadItem.title);
    }

    @SuppressWarnings("WeakerAccess")
    public static class UploadItem {
        private final Uri mediaUri;
        private final String mimeType;
        private final String source;
        private final GPSExtractor gpsCoords;

        private boolean selected = false;
        private boolean first = false;
        private Title title;
        private List<Description> descriptions;
        private Place place;
        private boolean visited;
        private boolean error;
        private long createdTimestamp;
        private String createdTimestampSource;
        private BehaviorSubject<Integer> imageQuality;

        @SuppressLint("CheckResult")
        UploadItem(Uri mediaUri, String mimeType, String source, GPSExtractor gpsCoords,
                   @Nullable Place place,
                   long createdTimestamp,
                   String createdTimestampSource) {
            this.createdTimestampSource = createdTimestampSource;
            title = new Title();
            descriptions = new ArrayList<>();
            descriptions.add(new Description());
            this.place = place;
            this.mediaUri = mediaUri;
            this.mimeType = mimeType;
            this.source = source;
            this.gpsCoords = gpsCoords;
            this.createdTimestamp = createdTimestamp;
            imageQuality = BehaviorSubject.createDefault(ImageUtils.IMAGE_WAIT);
        }

        public String getCreatedTimestampSource() {
            return createdTimestampSource;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getSource() {
            return source;
        }

        public GPSExtractor getGpsCoords() {
            return gpsCoords;
        }

        public boolean isSelected() {
            return selected;
        }

        public boolean isFirst() {
            return first;
        }

        public List<Description> getDescriptions() {
            return descriptions;
        }

        public boolean isVisited() {
            return visited;
        }

        public boolean isError() {
            return error;
        }

        public long getCreatedTimestamp() {
            return createdTimestamp;
        }

        public Title getTitle() {
            return title;
        }

        public Uri getMediaUri() {
            return mediaUri;
        }

        public int getImageQuality() {
            return this.imageQuality.getValue();
        }

        public void setImageQuality(int imageQuality) {
            this.imageQuality.onNext(imageQuality);
        }

        public String getFileExt() {
            return MimeTypeMapWrapper.getExtensionFromMimeType(mimeType);
        }

        public String getFileName() {
            return title
                    != null ? Utils.fixExtension(title.toString(), getFileExt()) : null;
        }

        public Place getPlace() {
            return place;
        }

        public void setTitle(Title title) {
            this.title = title;
        }

        public void setDescriptions(List<Description> descriptions) {
            this.descriptions = descriptions;
        }
    }

}
