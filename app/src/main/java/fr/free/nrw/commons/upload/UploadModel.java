package fr.free.nrw.commons.upload;

import android.net.Uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class UploadModel {
    private static UploadItem DUMMY = new UploadItem(Uri.EMPTY, "", "");
    private List<UploadItem> items = Collections.emptyList();
    private boolean topCardState = true;
    private boolean bottomCardState = true;
    private int currentStep = 1;

    @Inject
    UploadModel() {
    }

    public void receive(List<Uri> mediaUri, String mimeType, String source) {
        items = new ArrayList<>();
        currentStep = 1;
        for (int i = 0; i < mediaUri.size(); i++) {
            Uri uri = mediaUri.get(i);
            UploadItem e = new UploadItem(uri, mimeType, source);
            e.selected = (i == 0);
            e.first = (i == 0);
            items.add(e);
        }
    }

    public boolean isPreviousAvailable() {
        return currentStep > 1;
    }

    public boolean isNextAvailable() {
        return currentStep < (items.size() + 2);
    }

    public int getCurrentStep() {
        return currentStep;
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
        if (currentStep < items.size() + 2) {
            currentStep++;
        }
        updateSelectedState();
    }

    public void previous() {
        if (currentStep > 1) {
            currentStep--;
        }
        updateSelectedState();
    }

    public void jumpTo(UploadItem item) {
        int index = items.indexOf(item);
        currentStep = index + 1;
        updateSelectedState();
    }

    public UploadItem getCurrentItem() {
        return isShowingItem() ? items.get(currentStep - 1) : DUMMY;
    }

    public boolean isShowingItem() {
        return currentStep <= items.size();
    }

    private void updateSelectedState() {
        int count = items.size();
        for (int i = 0; i < count; i++) {
            UploadItem uploadItem = items.get(i);
            uploadItem.selected = (currentStep > count || i == currentStep - 1);
        }
    }

    public static class UploadItem {
        public boolean selected = false;
        public boolean first = false;
        public final Uri mediaUri;
        public final String mimeType;
        public final String source;
        public String title;
        public String description;

        public UploadItem(Uri mediaUri, String mimeType, String source) {
            this.mediaUri = mediaUri;
            this.mimeType = mimeType;
            this.source = source;
        }
    }
}
