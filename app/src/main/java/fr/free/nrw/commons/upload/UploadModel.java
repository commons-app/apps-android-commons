package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.filepicker.MimeTypeMapWrapper;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.utils.ImageUtils;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton
public class UploadModel {

    private final JsonKvStore store;
    private final List<String> licenses;
    private final Context context;
    private String license;
    private final Map<String, String> licensesByName;
    private List<UploadItem> items = new ArrayList<>();
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private SessionManager sessionManager;
    private FileProcessor fileProcessor;
    private final ImageProcessingService imageProcessingService;
    private List<String> selectedCategories;

    @Inject
    UploadModel(@Named("licenses") List<String> licenses,
            @Named("default_preferences") JsonKvStore store,
            @Named("licenses_by_name") Map<String, String> licensesByName,
            Context context,
            SessionManager sessionManager,
            FileProcessor fileProcessor,
            ImageProcessingService imageProcessingService) {
        this.licenses = licenses;
        this.store = store;
        this.license = store.getString(Prefs.DEFAULT_LICENSE, Prefs.Licenses.CC_BY_SA_3);
        this.licensesByName = licensesByName;
        this.context = context;
        this.sessionManager = sessionManager;
        this.fileProcessor = fileProcessor;
        this.imageProcessingService = imageProcessingService;
    }

    /**
     * cleanup the resources, I am Singleton, preparing for fresh upload
     */
    public void cleanUp() {
        compositeDisposable.clear();
        fileProcessor.cleanup();
        this.items.clear();
        if (this.selectedCategories != null) {
            this.selectedCategories.clear();
        }
    }

    public void setSelectedCategories(List<String> selectedCategories) {
        if (null == selectedCategories) {
            selectedCategories = new ArrayList<>();
        }
        this.selectedCategories = selectedCategories;
    }


    /**
     * pre process a one item at a time
     */
    public Observable<UploadItem> preProcessImage(UploadableFile uploadableFile,
            Place place,
            String source,
            SimilarImageInterface similarImageInterface) {
        return Observable.just(getUploadItem(uploadableFile, place, source, similarImageInterface));
    }

    public Single<Integer> getImageQuality(UploadItem uploadItem) {
        return imageProcessingService.validateImage(uploadItem);
    }

    private UploadItem getUploadItem(UploadableFile uploadableFile,
            Place place,
            String source,
            SimilarImageInterface similarImageInterface) {
        UploadableFile.DateTimeWithSource dateTimeWithSource = uploadableFile
                .getFileCreatedDate(context);
        long fileCreatedDate = -1;
        String createdTimestampSource = "";
        if (dateTimeWithSource != null) {
            fileCreatedDate = dateTimeWithSource.getEpochDate();
            createdTimestampSource = dateTimeWithSource.getSource();
        }
        Timber.d("File created date is %d", fileCreatedDate);
        ImageCoordinates imageCoordinates = fileProcessor
                .processFileCoordinates(similarImageInterface, uploadableFile.getFilePath());
        UploadItem uploadItem = new UploadItem(uploadableFile.getContentUri(),
                Uri.parse(uploadableFile.getFilePath()),
                uploadableFile.getMimeType(context), source, imageCoordinates, place, fileCreatedDate,
                createdTimestampSource);
        if (place != null) {
            uploadItem.title.setTitleText(place.name);
            if(uploadItem.descriptions.isEmpty()) {
                uploadItem.descriptions.add(new Description());
            }
            uploadItem.descriptions.get(0).setDescriptionText(place.getLongDescription());
            uploadItem.descriptions.get(0).setLanguageCode("en");
        }
        if (!items.contains(uploadItem)) {
            items.add(uploadItem);
        }
        return uploadItem;
    }

    public int getCount() {
        return items.size();
    }

    public List<UploadItem> getUploads() {
        return items;
    }

    public List<String> getLicenses() {
        return licenses;
    }

    public String getSelectedLicense() {
        return license;
    }

    public void setSelectedLicense(String licenseName) {
        this.license = licensesByName.get(licenseName);
        store.putString(Prefs.DEFAULT_LICENSE, license);
    }

    public Observable<Contribution> buildContributions() {
        return Observable.fromIterable(items).map(item ->
        {
            Contribution contribution = new Contribution(item.mediaUri, null,
                    item.getFileName(),
                    Description.formatList(item.descriptions), -1,
                    null, null, sessionManager.getAuthorName(),
                    CommonsApplication.DEFAULT_EDIT_SUMMARY, item.gpsCoords.getDecimalCoords(),
                    getMapFromDescriptions(item.descriptions));
            if (item.place != null) {
                contribution.setWikiDataEntityId(item.place.getWikiDataEntityId());
                // If item already has an image, we need to know it. We don't want to override existing image later
                contribution.setP18Value(item.place.pic);
            }
            if (null == selectedCategories) {//Just a fail safe, this should never be null
                selectedCategories = new ArrayList<>();
            }
            contribution.setCategories(selectedCategories);
            contribution.setTag("mimeType", item.mimeType);
            contribution.setSource(item.source);
            contribution.setContentProviderUri(item.mediaUri);
            contribution.setDateUploaded(new Date());

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

    private HashMap<String, String> getMapFromDescriptions(List<Description> descriptions) {
        HashMap<String, String> descriptionMap = new HashMap<>();
        for (Description description : descriptions) {
            descriptionMap.put(description.getLanguageCode(), description.getDescriptionText());
        }
        return descriptionMap;
    }

    public void deletePicture(String filePath) {
        Iterator<UploadItem> iterator = items.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().mediaUri.toString().contains(filePath)) {
                iterator.remove();
                break;
            }
        }
        if (items.isEmpty()) {
            cleanUp();
        }
    }

    public List<UploadItem> getItems() {
        return items;
    }

    public void updateUploadItem(int index, UploadItem uploadItem) {
        UploadItem uploadItem1 = items.get(index);
        uploadItem1.setDescriptions(uploadItem.descriptions);
        uploadItem1.setTitle(uploadItem.title);
    }

    public void useSimilarPictureCoordinates(ImageCoordinates imageCoordinates, int uploadItemIndex) {
        fileProcessor.useImageCoords(imageCoordinates);
        items.get(uploadItemIndex).setGpsCoords(imageCoordinates);
    }

    @SuppressWarnings("WeakerAccess")
    public static class UploadItem {

        private final Uri originalContentUri;
        private final Uri mediaUri;
        private final String mimeType;
        private final String source;
        private ImageCoordinates gpsCoords;

        public void setGpsCoords(ImageCoordinates gpsCoords) {
            this.gpsCoords = gpsCoords;
        }

        private Title title;
        private List<Description> descriptions;
        private Place place;
        private long createdTimestamp;
        private String createdTimestampSource;
        private BehaviorSubject<Integer> imageQuality;

        @SuppressLint("CheckResult")
        UploadItem(Uri originalContentUri,
                Uri mediaUri, String mimeType, String source, ImageCoordinates gpsCoords,
                Place place,
                long createdTimestamp,
                String createdTimestampSource) {
            this.originalContentUri = originalContentUri;
            this.createdTimestampSource = createdTimestampSource;
            title = new Title();
            descriptions = new ArrayList<>();
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

        public String getSource() {
            return source;
        }

        public ImageCoordinates getGpsCoords() {
            return gpsCoords;
        }

        public List<Description> getDescriptions() {
            return descriptions;
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

        public Uri getContentUri() {
            return originalContentUri;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof UploadItem)) {
                return false;
            }
            return this.mediaUri.toString().contains(((UploadItem) (obj)).mediaUri.toString());

        }

        @Override
        public int hashCode() {
            return mediaUri.hashCode();
        }
    }

}
