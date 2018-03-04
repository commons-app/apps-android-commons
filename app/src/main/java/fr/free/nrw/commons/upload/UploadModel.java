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
    private int currentStepIndex = 0;

    @Inject
    UploadModel() {
    }

    public void receive(List<Uri> mediaUri, String mimeType, String source) {
        items = new ArrayList<>();
        currentStepIndex = 0;
        for (int i = 0; i < mediaUri.size(); i++) {
            Uri uri = mediaUri.get(i);
            UploadItem e = new UploadItem(uri, mimeType, source);
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

    @SuppressWarnings("WeakerAccess")
    static class UploadItem {
        public boolean selected = false;
        public boolean first = false;
        public final Uri mediaUri;
        public final String mimeType;
        public final String source;
        public String title;
        public String description;
        public boolean visited;
        public boolean error;

        UploadItem(Uri mediaUri, String mimeType, String source) {
            this.mediaUri = mediaUri;
            this.mimeType = mimeType;
            this.source = source;
        }
    }
}
