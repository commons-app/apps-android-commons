package fr.free.nrw.commons.filepicker;

public interface Constants {
    String DEFAULT_FOLDER_NAME = "CommonsContributions";

    /**
     * Provides the request codes utilised by the FilePicker
     */
    interface RequestCodes {
        int LOCATION = 1;
        int STORAGE = 2;
        int FILE_PICKER_IMAGE_IDENTIFICATOR = 0b1101101100; //876
        int SOURCE_CHOOSER = 1 << 15;

        int PICK_PICTURE_FROM_CUSTOM_SELECTOR = FILE_PICKER_IMAGE_IDENTIFICATOR + (1 << 10);
        int PICK_PICTURE_FROM_DOCUMENTS = FILE_PICKER_IMAGE_IDENTIFICATOR + (1 << 11);
        int PICK_PICTURE_FROM_GALLERY = FILE_PICKER_IMAGE_IDENTIFICATOR + (1 << 12);
        int TAKE_PICTURE = FILE_PICKER_IMAGE_IDENTIFICATOR + (1 << 13);
        int CAPTURE_VIDEO = FILE_PICKER_IMAGE_IDENTIFICATOR + (1 << 14);

        int RECEIVE_DATA_FROM_FULL_SCREEN_MODE = 1 << 9;
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