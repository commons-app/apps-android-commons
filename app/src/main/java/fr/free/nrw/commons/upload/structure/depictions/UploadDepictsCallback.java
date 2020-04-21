package fr.free.nrw.commons.upload.structure.depictions;

/**
 * Listener to trigger callback whenever a depicts item is clicked
 */
public interface UploadDepictsCallback {
    void depictsClicked(DepictedItem item);

    void fetchThumbnailUrlForEntity(String entityId,int position);
}
