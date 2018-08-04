package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.settings.Prefs;
import io.reactivex.Observable;

public class UploadModel {
    private static UploadItem DUMMY = new UploadItem(Uri.EMPTY, "", "", GPSExtractor.DUMMY);
    private final SharedPreferences prefs;
    private final List<String> licenses;
    private String license;
    private final Map<String, String> licensesByName;
    private List<UploadItem> items = Collections.emptyList();
    private boolean topCardState = true;
    private boolean bottomCardState = true;
    private int currentStepIndex = 0;
    private Context context;
    @Inject
    SessionManager sessionManager;

    @Inject
    UploadModel(@Named("licenses") List<String> licenses,
                @Named("default_preferences") SharedPreferences prefs,
                @Named("licenses_by_name") Map<String, String> licensesByName,
                Context context) {
        this.licenses = licenses;
        this.prefs = prefs;
        this.license = Prefs.Licenses.CC_BY_SA_3;
        this.licensesByName = licensesByName;
        this.context = context;
    }

    public void receive(List<Uri> mediaUri, String mimeType, String source) {
        items = new ArrayList<>();
        currentStepIndex = 0;
        for (int i = 0; i < mediaUri.size(); i++) {
            Uri uri = mediaUri.get(i);
            FileProcessor fp = new FileProcessor(uri, context.getContentResolver(), context);
            UploadItem e = new UploadItem(uri, mimeType, source, fp.processFileCoordinates(false));
            e.selected = (i == 0);
            e.first = (i == 0);
            items.add(e);
        }
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

    public void setBottomCardState(boolean bottomCardState) {
        this.bottomCardState = bottomCardState;
    }

    public void next() {
        markCurrentUploadVisited();
        if (currentStepIndex < items.size() + 1) {
            currentStepIndex++;
        }
        updateItemState();
    }

    public void previous() {
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
            item.error = item.title == null || item.title.trim().isEmpty();
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

    //When the EXIF modification UI is added, the selections will be realised here
    @SuppressLint("CheckResult")
    public Observable<Contribution> toContributions() {
        return Observable.fromIterable(items).map(item ->
                new Contribution(item.mediaUri, null, item.title, item.description, -1,
                        null, null, sessionManager.getCurrentAccount().name,
                        CommonsApplication.DEFAULT_EDIT_SUMMARY, item.gpsCoords.getCoords()));
    }

    @SuppressWarnings("WeakerAccess")
    static class UploadItem {
        public final Uri mediaUri;
        public final String mimeType;
        public final String source;
        public final GPSExtractor gpsCoords;

        public boolean selected = false;
        public boolean first = false;
        public String title;
        public String description;
        public boolean visited;
        public boolean error;

        UploadItem(Uri mediaUri, String mimeType, String source, GPSExtractor gpsCoords) {
            this.mediaUri = mediaUri;
            this.mimeType = mimeType;
            this.source = source;
            this.gpsCoords = gpsCoords;
        }
    }
}
