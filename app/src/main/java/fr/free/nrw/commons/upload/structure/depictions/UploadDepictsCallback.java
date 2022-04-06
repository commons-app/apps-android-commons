package fr.free.nrw.commons.upload.structure.depictions;

import fr.free.nrw.commons.upload.models.depictions.DepictedItem;

/**
 * Listener to trigger callback whenever a depicts item is clicked
 */
public interface UploadDepictsCallback {
    void depictsClicked(DepictedItem item);
}
