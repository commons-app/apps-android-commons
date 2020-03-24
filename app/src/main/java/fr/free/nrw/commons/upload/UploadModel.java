package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import androidx.annotation.Nullable;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.Contribution;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

@Singleton
public class UploadModel {

    private final JsonKvStore store;
    private final List<String> licenses;
    private final Context context;
    private String license;
    private final Map<String, String> licensesByName;
    private final List<UploadItem> items = new ArrayList<>();
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final SessionManager sessionManager;
    private final FileProcessor fileProcessor;
    private final ImageProcessingService imageProcessingService;
    private List<String> selectedCategories;
    private ArrayList<String> selectedDepictions;

    @Inject
    UploadModel(@Named("licenses") final List<String> licenses,
            @Named("default_preferences") final JsonKvStore store,
            @Named("licenses_by_name") final Map<String, String> licensesByName,
            final Context context,
            final SessionManager sessionManager,
            final FileProcessor fileProcessor,
            final ImageProcessingService imageProcessingService) {
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
        if (this.selectedDepictions != null) {
            this.selectedDepictions.clear();
        }
    }

    public void setSelectedCategories(List<String> selectedCategories) {
        this.selectedCategories = newListOf(selectedCategories);
    }

    /**
     * pre process a one item at a time
     */
    public Observable<UploadItem> preProcessImage(final UploadableFile uploadableFile,
            final Place place,
            final String source,
            final SimilarImageInterface similarImageInterface) {
        return Observable.just(
            createAndAddUploadItem(uploadableFile, place, source, similarImageInterface));
    }

    public Single<Integer> getImageQuality(final UploadItem uploadItem) {
        return imageProcessingService.validateImage(uploadItem);
    }

    private UploadItem createAndAddUploadItem(final UploadableFile uploadableFile,
            final Place place,
            final String source,
            final SimilarImageInterface similarImageInterface) {
        final UploadableFile.DateTimeWithSource dateTimeWithSource = uploadableFile
                .getFileCreatedDate(context);
        long fileCreatedDate = -1;
        String createdTimestampSource = "";
        if (dateTimeWithSource != null) {
            fileCreatedDate = dateTimeWithSource.getEpochDate();
            createdTimestampSource = dateTimeWithSource.getSource();
        }
        Timber.d("File created date is %d", fileCreatedDate);
        final ImageCoordinates imageCoordinates = fileProcessor
                .processFileCoordinates(similarImageInterface, uploadableFile.getFilePath());
        final UploadItem uploadItem = new UploadItem(uploadableFile.getContentUri(),
                Uri.parse(uploadableFile.getFilePath()),
                uploadableFile.getMimeType(context), source, imageCoordinates, place, fileCreatedDate,
                createdTimestampSource);
        if (place != null) {
            uploadItem.getUploadMediaDetails().set(0, new UploadMediaDetail(place));
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

    public void setSelectedLicense(final String licenseName) {
        this.license = licensesByName.get(licenseName);
        store.putString(Prefs.DEFAULT_LICENSE, license);
    }

    public Observable<Contribution> buildContributions() {
        return Observable.fromIterable(items).map(item ->
        {
            final Contribution contribution = new Contribution(item.mediaUri, null,
                    item.getFileName(), item.uploadMediaDetails.size()!=0? UploadMediaDetail.formatCaptions(item.uploadMediaDetails):new HashMap<>(),
                    UploadMediaDetail.formatList(item.uploadMediaDetails), -1,
                    null, null, sessionManager.getAuthorName(),
                    CommonsApplication.DEFAULT_EDIT_SUMMARY, new ArrayList<>(selectedDepictions), item.gpsCoords.getDecimalCoords());
            if (item.place != null) {
                contribution.setWikiDataEntityId(item.place.getWikiDataEntityId());
                contribution.setWikiItemName(item.place.getName());
                // If item already has an image, we need to know it. We don't want to override existing image later
                contribution.setP18Value(item.place.pic);
            }
            if (null == selectedCategories) {//Just a fail safe, this should never be null
                selectedCategories = new ArrayList<>();
            }
            if (selectedDepictions == null) {
                selectedDepictions = new ArrayList<>();
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

    public void deletePicture(final String filePath) {
        final Iterator<UploadItem> iterator = items.iterator();
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

    public void updateUploadItem(final int index, final UploadItem uploadItem) {
        final UploadItem uploadItem1 = items.get(index);
        uploadItem1.setMediaDetails(uploadItem.uploadMediaDetails);
    }

    public void setSelectedDepictions(final List<String> selectedDepictions) {
        this.selectedDepictions = newListOf(selectedDepictions);
    }

    @NotNull
    private <T> ArrayList<T> newListOf(final List<T> items) {
        return items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    public void useSimilarPictureCoordinates(final ImageCoordinates imageCoordinates, final int uploadItemIndex) {
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
        private List<UploadMediaDetail> uploadMediaDetails;
        private final Place place;
        private final long createdTimestamp;
        private final String createdTimestampSource;
        private final BehaviorSubject<Integer> imageQuality;
        @SuppressLint("CheckResult")
        UploadItem(final Uri originalContentUri,
                final Uri mediaUri, final String mimeType, final String source, final ImageCoordinates gpsCoords,
                final Place place,
                final long createdTimestamp,
                final String createdTimestampSource) {
            this.originalContentUri = originalContentUri;
            this.createdTimestampSource = createdTimestampSource;
            uploadMediaDetails = Collections.singletonList(new UploadMediaDetail());
            uploadMediaDetails = new ArrayList<>(Arrays.asList(new UploadMediaDetail()));
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

        public List<UploadMediaDetail> getUploadMediaDetails() {
          return uploadMediaDetails;
        }

        public long getCreatedTimestamp() {
            return createdTimestamp;
        }

        public Uri getMediaUri() {
            return mediaUri;
        }

        public int getImageQuality() {
            return this.imageQuality.getValue();
        }

        public void setImageQuality(final int imageQuality) {
            this.imageQuality.onNext(imageQuality);
        }

        public Place getPlace() {
            return place;
        }

        public void setMediaDetails(final List<UploadMediaDetail> uploadMediaDetails) {
            this.uploadMediaDetails = uploadMediaDetails;
        }

        public Uri getContentUri() {
            return originalContentUri;
        }

        @Override
        public boolean equals(@Nullable final Object obj) {
            if (!(obj instanceof UploadItem)) {
                return false;
            }
            return this.mediaUri.toString().contains(((UploadItem) (obj)).mediaUri.toString());

        }

        @Override
        public int hashCode() {
            return mediaUri.hashCode();
        }

        /**
         * Choose a filename for the media.
         * Currently, the caption is used as a filename. If several languages have been entered, the first language is used.
         */
        public String getFileName() {
            return uploadMediaDetails.get(0).getCaptionText();
        }

        public void setGpsCoords(final ImageCoordinates gpsCoords) {
            this.gpsCoords = gpsCoords;
        }

    }
}
