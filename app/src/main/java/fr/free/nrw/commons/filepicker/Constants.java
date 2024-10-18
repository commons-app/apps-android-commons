package fr.free.nrw.commons.filepicker;

public interface Constants {
    String DEFAULT_FOLDER_NAME = "CommonsContributions";

    /**
     * Provides the request codes utilised by the FilePicker
     */
    interface RequestCodes {
        int LOCATION = 1;
        int STORAGE = 2;
    }

    /**
     * Provides locations as string for corresponding operations
     */
    interface BundleKeys {
        String FOLDER_NAME = "fr.free.nrw.commons.folder_name";
        String ALLOW_MULTIPLE = "fr.free.nrw.commons.allow_multiple";
        String COPY_TAKEN_PHOTOS = "fr.free.nrw.commons.copy_taken_photos";
        String COPY_PICKED_IMAGES = "fr.free.nrw.commons.copy_picked_images";
    }
}