package fr.free.nrw.commons.upload;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.utils.ImageUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

public class UploadModel {

    MediaWikiApi mwApi;
    private static UploadItem DUMMY = new UploadItem(Uri.EMPTY, "", "", GPSExtractor.DUMMY, "", null) {
        @Override
        public boolean isDummy() {
            return true;
        }
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

    @Inject
    SessionManager sessionManager;

    @Inject
    UploadModel(@Named("licenses") List<String> licenses,
                @Named("default_preferences") SharedPreferences prefs,
                @Named("licenses_by_name") Map<String, String> licensesByName,
                Context context,
                MediaWikiApi mwApi) {
        this.licenses = licenses;
        this.prefs = prefs;
        this.license = Prefs.Licenses.CC_BY_SA_3;
        this.licensesByName = licensesByName;
        this.context = context;
        this.mwApi = mwApi;
        this.contentResolver = context.getContentResolver();
        useExtStorage = this.prefs.getBoolean("useExternalStorage", false);
    }

    @SuppressLint("CheckResult")
    public void receive(List<Uri> mediaUri, String mimeType, String source) {
        currentStepIndex = 0;

        Observable<UploadItem> itemObservable = Observable.fromIterable(mediaUri)
                .map(this::cacheFileUpload)
                .map(filePath -> {
                    Uri uri = Uri.fromFile(new File(filePath));
                    FileProcessor fp = new FileProcessor(uri, context.getContentResolver(), context);
                    UploadItem item = new UploadItem(uri, mimeType, source, fp.processFileCoordinates(false),
                            FileUtils.getFileExt(filePath), null);
                    new DetectBadPicturesAsync(new WeakReference<>(item.imageQuality), new WeakReference<>(context),
                            new WeakReference<>(mwApi), uri).execute();
                    return item;
                });
        items = itemObservable.toList().blockingGet();
        items.get(0).selected = true;
        items.get(0).first = true;
    }

    public boolean isPreviousAvailable() {
        return currentStepIndex > 0;
    }

    public boolean isNextAvailable() {
        return currentStepIndex < (items.size() + 1);
    }

    public boolean isSubmitAvailable() {
        int count = items.size();
        boolean hasError = license == null;
        for (int i = 0; i < count; i++) {
            UploadItem item = items.get(i);
            hasError |= item.error;
        }
        return !hasError;
    }

    public int getCurrentStep() {
        return currentStepIndex + 1;
    }

    public int getStepCount() {
        return items.size() + 2;
    }

    public int getCount() {
        return items.size();
    }

    public List<UploadItem> getUploads() {
        return items;
    }

    public boolean isTopCardState() {
        return topCardState;
    }

    public void setTopCardState(boolean topCardState) {
        this.topCardState = topCardState;
    }

    public boolean isBottomCardState() {
        return bottomCardState;
    }

    public void setRightCardState(boolean rightCardState) {
        this.rightCardState = rightCardState;
    }

    public boolean isRightCardState() {
        return rightCardState;
    }

    public void setBottomCardState(boolean bottomCardState) {
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

    public void previous() {
        if (badImageSubscription != null)
            badImageSubscription.dispose();
        markCurrentUploadVisited();
        if (currentStepIndex > 0) {
            currentStepIndex--;
        }
        updateItemState();
    }

    public void jumpTo(UploadItem item) {
        currentStepIndex = items.indexOf(item);
        item.visited = true;
        updateItemState();
    }

    public UploadItem getCurrentItem() {
        return isShowingItem() ? items.get(currentStepIndex) : DUMMY;
    }

    public boolean isShowingItem() {
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

    public String getSelectedLicense() {
        return license;
    }

    public void setSelectedLicense(String licenseName) {
        this.license = licensesByName.get(licenseName);
    }

    public Observable<Contribution> buildContributions(List<String> categoryStringList) {
        return Observable.fromIterable(items).map(item ->
        {
            Contribution contribution = new Contribution(item.mediaUri, null, item.title + "." + item.fileExt,
                    Description.formatList(item.descriptions), -1,
                    null, null, sessionManager.getCurrentAccount().name,
                    CommonsApplication.DEFAULT_EDIT_SUMMARY, item.gpsCoords.getCoords());
            contribution.setWikiDataEntityId(item.wikidataEntityId);
            contribution.setCategories(categoryStringList);
            contribution.setTag("mimeType", item.mimeType);
            contribution.setSource(item.source);
            contribution.setContentProviderUri(item.mediaUri);
            return contribution;
        });
    }

    /**
     * Copy files into local storage and return file path
     *
     * @param media Uri of the file
     * @return
     */
    private String cacheFileUpload(Uri media) {
        try {
            String copyPath;
            if (useExtStorage)
                copyPath = FileUtils.createExternalCopyPathAndCopy(media, contentResolver);
            else
                copyPath = FileUtils.createCopyPathAndCopy(media, context);
            Timber.i("File path is " + copyPath);
            return copyPath;
        } catch (IOException e) {
            Timber.w(e, "Error in copying URI " + media.getPath());
        }
        return null;
    }

    public void keepPicture() {
        items.get(currentStepIndex).imageQuality.onNext(ImageUtils.Result.IMAGE_KEEP);
    }

    public void deletePicture() {
        badImageSubscription.dispose();
        items.remove(currentStepIndex).imageQuality.onComplete();
        updateItemState();
    }

    public void subscribeBadPicture(Consumer<ImageUtils.Result> consumer) {
        badImageSubscription = getCurrentItem().imageQuality.subscribe(consumer, Timber::e);
    }

    public void receiveDirect(Uri media, String mimeType, String source, String wikidataEntityIdPref, String title, String desc) {
        currentStepIndex = 0;
        items=new ArrayList<>();
        String filePath = this.cacheFileUpload(media);
        Uri uri = Uri.fromFile(new File(filePath));
        FileProcessor fp = new FileProcessor(uri, context.getContentResolver(), context);
        UploadItem item = new UploadItem(uri, mimeType, source, fp.processFileCoordinates(false),
                FileUtils.getFileExt(filePath), wikidataEntityIdPref);
        item.title.setTitleText(title);
        item.descriptions.get(0).setDescriptionText(desc);
        //TODO figure out if default descriptions in other languages exist
        item.descriptions.get(0).setLanguageCode("en");
        new DetectBadPicturesAsync(new WeakReference<>(item.imageQuality), new WeakReference<>(context),
                new WeakReference<>(mwApi), uri).execute();
        items.add(item);
        items.get(0).selected = true;
        items.get(0).first = true;
    }

    public boolean isLoggedIn() {
        Account currentAccount = sessionManager.getCurrentAccount();
        return currentAccount != null;
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
        public BehaviorSubject<ImageUtils.Result> imageQuality;
        Title title;
        List<Description> descriptions;
        public String wikidataEntityId;
        public boolean visited;
        public boolean error;

        @SuppressLint("CheckResult")
        UploadItem(Uri mediaUri, String mimeType, String source, GPSExtractor gpsCoords, String fileExt, @Nullable String wikidataEntityId) {
            title = new Title();
            descriptions = new ArrayList<>();
            descriptions.add(new Description());
            this.wikidataEntityId = wikidataEntityId;

            this.mediaUri = mediaUri;
            this.mimeType = mimeType;
            this.source = source;
            this.gpsCoords = gpsCoords;
            this.fileExt = fileExt;
            imageQuality = BehaviorSubject.createDefault(ImageUtils.Result.IMAGE_WAIT);
//                imageQuality.subscribe(iq->Timber.i("New value of imageQuality:"+ImageUtils.Result.IMAGE_OK));
        }

        public boolean isDummy() {
            return false;
        }
    }

}
