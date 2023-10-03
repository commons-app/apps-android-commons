package fr.free.nrw.commons.upload;

import android.content.Context;
import android.net.Uri;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.upload.depicts.DepictsFragment;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import java.util.ArrayList;
import java.util.Date;
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
    private final List<String> selectedCategories = new ArrayList<>();
    private final List<DepictedItem> selectedDepictions = new ArrayList<>();
    /**
     * Existing depicts which are selected
     */
    private List<String> selectedExistingDepictions = new ArrayList<>();

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
        items.clear();
        selectedCategories.clear();
        selectedDepictions.clear();
        selectedExistingDepictions.clear();
    }

    public void setSelectedCategories(List<String> selectedCategories) {
        this.selectedCategories.clear();
        this.selectedCategories.addAll(selectedCategories);
    }

    /**
     * pre process a one item at a time
     */
    public Observable<UploadItem> preProcessImage(final UploadableFile uploadableFile,
        final Place place,
        final SimilarImageInterface similarImageInterface,
        LatLng inAppPictureLocation) {
        return Observable.just(
            createAndAddUploadItem(uploadableFile, place, similarImageInterface, inAppPictureLocation));
    }

    public Single<Integer> getImageQuality(final UploadItem uploadItem, LatLng inAppPictureLocation) {
        return imageProcessingService.validateImage(uploadItem, inAppPictureLocation);
    }

    private UploadItem createAndAddUploadItem(final UploadableFile uploadableFile,
        final Place place,
        final SimilarImageInterface similarImageInterface,
        LatLng inAppPictureLocation) {
        final UploadableFile.DateTimeWithSource dateTimeWithSource = uploadableFile
                .getFileCreatedDate(context);
        long fileCreatedDate = -1;
        String createdTimestampSource = "";
        String fileCreatedDateString = "";
        if (dateTimeWithSource != null) {
            fileCreatedDate = dateTimeWithSource.getEpochDate();
            fileCreatedDateString = dateTimeWithSource.getDateString();
            createdTimestampSource = dateTimeWithSource.getSource();
        }
        Timber.d("File created date is %d", fileCreatedDate);
        final ImageCoordinates imageCoordinates = fileProcessor
                .processFileCoordinates(similarImageInterface, uploadableFile.getFilePath(),
                    inAppPictureLocation);
        final UploadItem uploadItem = new UploadItem(
            Uri.parse(uploadableFile.getFilePath()),
                uploadableFile.getMimeType(context), imageCoordinates, place, fileCreatedDate,
                createdTimestampSource,
                uploadableFile.getContentUri(),
                fileCreatedDateString);

        // If an uploadItem of the same uploadableFile has been created before, we return that.
        // This is to avoid multiple instances of uploadItem of same file passed around.
        if (items.contains(uploadItem)) {
            return items.get(items.indexOf(uploadItem));
        }

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
            String imageSHA1 = FileUtils.getSHA1(context.getContentResolver().openInputStream(item.getContentUri()));

            final Contribution contribution = new Contribution(
                item, sessionManager, newListOf(selectedDepictions), newListOf(selectedCategories), imageSHA1);

            contribution.setHasInvalidLocation(item.hasInvalidLocation());

            Timber.d("Created timestamp while building contribution is %s, %s",
                item.getCreatedTimestamp(),
                new Date(item.getCreatedTimestamp()));

            if (item.getCreatedTimestamp() != -1L) {
                contribution.setDateCreated(new Date(item.getCreatedTimestamp()));
                contribution.setDateCreatedSource(item.getCreatedTimestampSource());
                //Set the date only if you have it, else the upload service is gonna try it the other way
            }

            if (contribution.getWikidataPlace() != null) {
                if (item.isWLMUpload()) {
                    contribution.getWikidataPlace().setMonumentUpload(true);
                } else {
                    contribution.getWikidataPlace().setMonumentUpload(false);
                }
            }
            contribution.setCountryCode(item.getCountryCode());
            return contribution;
        });
    }

    public void deletePicture(final String filePath) {
        final Iterator<UploadItem> iterator = items.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getMediaUri().toString().contains(filePath)) {
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

    public void onDepictItemClicked(DepictedItem depictedItem, Media media) {
        if (media == null) {
            if (depictedItem.isSelected()) {
                selectedDepictions.add(depictedItem);
            } else {
                selectedDepictions.remove(depictedItem);
            }
        } else {
            if (depictedItem.isSelected()) {
                if (media.getDepictionIds().contains(depictedItem.getId())) {
                    selectedExistingDepictions.add(depictedItem.getId());
                } else {
                    selectedDepictions.add(depictedItem);
                }
            } else {
                if (media.getDepictionIds().contains(depictedItem.getId())) {
                    selectedExistingDepictions.remove(depictedItem.getId());
                    if (!media.getDepictionIds().contains(depictedItem.getId())) {
                        final List<String> depictsList = new ArrayList<>();
                        depictsList.add(depictedItem.getId());
                        depictsList.addAll(media.getDepictionIds());
                        media.setDepictionIds(depictsList);
                    }
                } else {
                    selectedDepictions.remove(depictedItem);
                }
            }
        }
    }

    @NotNull
    private <T> List<T> newListOf(final List<T> items) {
        return items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    public void useSimilarPictureCoordinates(final ImageCoordinates imageCoordinates, final int uploadItemIndex) {
        fileProcessor.prePopulateCategoriesAndDepictionsBy(imageCoordinates);
        items.get(uploadItemIndex).setGpsCoords(imageCoordinates);
    }

    public List<DepictedItem> getSelectedDepictions() {
        return selectedDepictions;
    }

    /**
     * Provides selected existing depicts
     *
     * @return selected existing depicts
     */
    public List<String> getSelectedExistingDepictions() {
        return selectedExistingDepictions;
    }

    /**
     * Initialize existing depicts
     *
     * @param selectedExistingDepictions existing depicts
     */
    public void setSelectedExistingDepictions(final List<String> selectedExistingDepictions) {
        this.selectedExistingDepictions = selectedExistingDepictions;
    }
}
